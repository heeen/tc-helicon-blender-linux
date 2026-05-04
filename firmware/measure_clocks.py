#!/usr/bin/env python3
"""Measure DICE3 actual CPU clock frequency via JTAG + SRAM stub.

The earlier attempt at reading TIMER_COUNT directly via JTAG was
unreliable: the timer reload (500k) is small enough that wraps happen
during OpenOCD round-trips, and JTAG halt may or may not stop the
timer (the docs disagree with empirical behavior).

This script avoids that by running a fixed-cycle counting loop on the
target CPU itself and measuring the elapsed wall time externally:

    stub:
      loop: subs r0, r0, #1   ; 1 cycle
            bne  loop          ; 3 cycles taken
      str  CAFEBABE, [r1]
      bx   lr

  4 cycles per iteration. Host:
    1. Load stub at SRAM 0x2B000.
    2. Set r0 = iters, r1 = signal addr, lr = trap-loop addr, pc = stub.
    3. Capture t0 = time.monotonic(), resume CPU.
    4. Poll for signal == 0xCAFEBABE.
    5. Capture t1 = time.monotonic().
    6. cpu_freq = (4 * iters) / (t1 - t0)

Halt-detect overhead is in the polling, not in the measurement: the
stub itself runs continuously between t0 and t1 with no JTAG
interaction, so the wall-time measurement is accurate to within the
poll interval (~10 ms) of the stub's true completion. Choose iters
large enough that stub runtime >> poll interval.

Run with --mode crystal/pll/both. Crystal (~12 MHz) is the anchor:
the firmware constant pool stores 0x17D78400 = 400_000_000 (PLL VCO
= 12 MHz × 100 / 3) which can only be true if crystal is exactly 12
MHz. This is the same anchor `pll_clock_init` uses for the AHB freq
constant 200_000_000 = Fvco / 2.
"""
from __future__ import annotations
import sys, time, argparse
from pathlib import Path

sys.path.insert(0, str(Path(__file__).parent))
from jtag_flash_v2 import OpenOCD, ensure_crystal_clock, ensure_pll_clock

# Stub layout. Each iteration is 4 cycles (subs 1 + bne taken 3) on
# ARM926EJ-S. Total stub cycles ≈ 4 × iters + small overhead (~6 ops).
STUB_WORDS = [
    0xE2500001,  # subs r0, r0, #1
    0x1AFFFFFD,  # bne -3 (back to subs)
    0xE59F2004,  # ldr r2, [pc, #4]   ; load CAFEBABE literal (at 0x14)
    0xE5812000,  # str r2, [r1]
    0xE12FFF1E,  # bx lr
    0x00000000,  # padding
    0xCAFEBABE,  # literal — loaded by ldr above (PC+8 from ldr at 0x08 → 0x14? wait)
]
# Recompute: ldr at offset 0x08, PC for ldr = 0x08 + 8 = 0x10.
# offset #4 from PC 0x10 = 0x14. STUB_WORDS[5] is at 0x14. So literal must
# be at index 5, NOT index 6. Let me fix:
STUB_WORDS = [
    0xE2500001,  # 0x00  subs r0, r0, #1
    0x1AFFFFFD,  # 0x04  bne -3
    0xE59F2004,  # 0x08  ldr r2, [pc, #4]   → reads from 0x08+8+4 = 0x14
    0xE5812000,  # 0x0C  str r2, [r1]
    0xE12FFF1E,  # 0x10  bx lr
    0xCAFEBABE,  # 0x14  literal
]
CYCLES_PER_ITER = 4
STUB_ADDR = 0x2B000
RETURN_TRAP_ADDR = 0x2B080
SIGNAL_ADDR = 0x2B100

CRYSTAL_HZ = 12_000_000           # firmware-anchored: pll_clock_init's stored 0x17D78400 = 400 MHz only fits 12 MHz × 100 / 3


def encode_stub() -> bytes:
    return b"".join(w.to_bytes(4, "little") for w in STUB_WORDS)


def measure_cpu_freq(o: OpenOCD, iters: int, poll_interval_s: float = 0.02) -> tuple[float, dict]:
    stub = encode_stub()
    tmp = "/tmp/cpu_freq_stub.bin"
    Path(tmp).write_bytes(stub)
    o.halt()
    # Trap loop at RETURN_TRAP_ADDR ("b ." = 0xEAFFFFFE)
    o.mww(RETURN_TRAP_ADDR, 0xEAFFFFFE)
    o.mww(SIGNAL_ADDR, 0)
    o.load_image(tmp, STUB_ADDR)
    o.cmd(f"reg r0 {iters}")
    o.cmd(f"reg r1 0x{SIGNAL_ADDR:x}")
    o.cmd(f"reg lr 0x{RETURN_TRAP_ADDR:x}")
    o.cmd(f"reg pc 0x{STUB_ADDR:x}")
    o.cmd("reg cpsr 0xd3")
    t0 = time.monotonic()
    o.resume()

    deadline = t0 + max(60.0, iters * 4 / 1e6 * 5)   # 5× expected at 1 MHz min
    poll_count = 0
    while time.monotonic() < deadline:
        time.sleep(poll_interval_s)
        try:
            o.halt()
            poll_count += 1
            sig = o.mdw(SIGNAL_ADDR)
            if sig == 0xCAFEBABE:
                t1 = time.monotonic()
                o.resume()
                wall = t1 - t0
                cycles = CYCLES_PER_ITER * iters
                return cycles / wall, {
                    "iters": iters,
                    "cycles": cycles,
                    "wall_s": wall,
                    "polls": poll_count,
                    "poll_interval_s": poll_interval_s,
                }
            o.resume()
        except Exception as e:
            continue
    return 0.0, {"error": f"stub never signaled within {deadline - t0:.1f}s",
                 "iters": iters, "polls": poll_count}


def report_cpu(o: OpenOCD, label: str, iters: int):
    """Measure stub-iters-per-second. The ABSOLUTE Hz value is uncalibrated
    (ARM926EJ-S branch-refill cycles aren't exactly 4); divide by the
    crystal-mode result to get a reliable CPU-clock ratio."""
    pll = o.mdw(0xC900000C)
    print(f"\n=== {label}  (PLL_OUTPUT=0x{pll:04x}) ===")
    rate, dbg = measure_cpu_freq(o, iters=iters)
    if "error" in dbg:
        print(f"  CPU stub failed: {dbg['error']}  (polls={dbg['polls']})")
        return None
    print(f"  CPU stub: {dbg['iters']:>11,} iters in {dbg['wall_s']*1000:.2f} ms wall  "
          f"(polls={dbg['polls']}, ±{dbg['poll_interval_s']*1000:.0f} ms uncertainty)")
    print(f"  Stub rate: {dbg['iters']/dbg['wall_s']/1e6:.3f} M-iters/s  "
          f"(uncalibrated abs. cycles assuming 4/iter: {rate/1e6:.3f} MHz)")
    return dbg['iters'] / dbg['wall_s']  # iters per second — ratio-comparable


def main():
    p = argparse.ArgumentParser()
    p.add_argument("--mode", choices=("as-is", "crystal", "pll", "both"), default="both")
    p.add_argument("--speed", type=int, default=1000)
    p.add_argument("--cpu-iters", type=int, default=10_000_000,
                   help="Iterations for CPU stub. 10M iters × 4 cyc = 40M cycles "
                        "→ 0.2s at 200 MHz, 3.3s at 12 MHz.")
    args = p.parse_args()

    o = OpenOCD(speed=args.speed)
    try:
        if args.mode == "as-is":
            report_cpu(o, "AS-IS", args.cpu_iters)
        elif args.mode == "crystal":
            ensure_crystal_clock(o)
            report_cpu(o, "CRYSTAL CPU", args.cpu_iters)
        elif args.mode == "pll":
            ensure_pll_clock(o)
            report_cpu(o, "PLL CPU", args.cpu_iters)
        else:
            ensure_pll_clock(o)
            r_pll = report_cpu(o, "PLL CPU", args.cpu_iters)
            ensure_crystal_clock(o)
            r_crystal = report_cpu(o, "CRYSTAL CPU", args.cpu_iters // 8)  # crystal slower, smaller count
            ensure_pll_clock(o)
            r_pll2 = report_cpu(o, "PLL CPU again (sanity)", args.cpu_iters)
            if r_pll and r_crystal:
                ratio = r_pll / r_crystal
                derived_pll = CRYSTAL_HZ * ratio
                print(f"\n=== Cross-check (ratio-anchored to 12 MHz crystal) ===")
                print(f"  Stub rate at PLL:     {r_pll/1e6:.3f} M-iters/s")
                print(f"  Stub rate at crystal: {r_crystal/1e6:.3f} M-iters/s")
                print(f"  PLL / crystal ratio:  {ratio:.3f}")
                print(f"  Expected if AHB = 200 MHz: {200/12:.3f}")
                print(f"  Derived PLL CPU clock: {derived_pll/1e6:.2f} MHz "
                      f"(crystal × ratio = 12 × {ratio:.3f})")
                # Tolerance: ±10% covers typical crystal tolerance (50ppm) +
                # JTAG poll-interval uncertainty (~5% per measurement) +
                # any pipeline-cycle slop in the stub.
                if abs(derived_pll/1e6 - 200) < 20:
                    print(f"  ✓ Consistent with Ghidra finding (AHB = Fvco/2 = 200 MHz)")
                else:
                    print(f"  ⚠ Diverges from 200 MHz expectation by {derived_pll/1e6 - 200:+.1f} MHz")
    finally:
        o.close()


if __name__ == "__main__":
    main()
