#!/usr/bin/env python3
"""Measure DICE3 clocks and timer frequencies via JTAG + on-device stubs.

Two independent stubs sample on-CPU, avoiding JTAG-halt-stops-Timer
unreliability:

  STUB A — long countdown for absolute CPU MHz via wall clock
    subs+bne loop, writes 0xCAFEBABE when done. Host times the resume
    →signal interval externally. Only the PLL/crystal RATIO is
    quantitatively reliable; the absolute MHz from this stub is
    uncalibrated (assumes 4 cycles/iter, ARM926EJ-S branch refill
    makes it closer to 8). Anchored against crystal = 12 MHz (firmware
    constant 0x17D78400 = 400 MHz only valid at exactly 12 MHz).

  STUB B — short countdown with Timer0 sampling for Timer0 freq
    Same body, but reads TIMER_COUNT before and after into r4/r5,
    stores them. Iter count is small so the loop time stays well
    below one TIMER_RELOAD wrap period. From the stub we get exactly
    `delta_ticks per N CPU cycles`; combined with the absolute CPU
    MHz from STUB A, we derive Timer0 frequency without any JTAG
    halt-during-measurement contamination.

Run each stub at PLL and crystal CPU. Reported:
  - PLL CPU clock and PLL/crystal ratio (vs Ghidra-claimed 200 MHz)
  - Timer0 frequency at PLL and crystal (tests whether Timer0 is
    PLL-derived, crystal-derived, or on a third oscillator)
"""
from __future__ import annotations
import sys, time, argparse
from pathlib import Path

sys.path.insert(0, str(Path(__file__).parent))
from jtag_flash_v2 import OpenOCD, ensure_crystal_clock, ensure_pll_clock

CRYSTAL_HZ = 12_000_000

# ── STUB A: long countdown for wall-clock CPU rate ─────────────────
# subs+bne loop; signals 0xCAFEBABE when done.
STUB_A_WORDS = [
    0xE2500001,  # 0x00  subs r0, r0, #1
    0x1AFFFFFD,  # 0x04  bne -3
    0xE59F2004,  # 0x08  ldr r2, [pc, #4]   → loads from PC+8+4 = 0x14
    0xE5812000,  # 0x0C  str r2, [r1]
    0xE12FFF1E,  # 0x10  bx lr
    0xCAFEBABE,  # 0x14  literal
]

# ── STUB B: short countdown + Timer0 sampling ──────────────────────
# Reads TIMER_COUNT before and after the loop; stores t0 at +4, t1 at +8,
# signals 0xCAFEBABE at +0. Loop body identical to STUB A (4 cycles/iter
# nominal, real ~8 with branch refill).
#
# Layout (offsets):
#   0x00  ldr r3, [pc, #0x20]   → loads &TIMER_COUNT from 0x28
#   0x04  ldr r4, [r3]          ; t0 = TIMER_COUNT
#   0x08  subs r0, r0, #1       ; loop body start
#   0x0C  bne -3                  ; back to 0x08
#   0x10  ldr r5, [r3]          ; t1 = TIMER_COUNT (after loop)
#   0x14  str r4, [r1, #4]      ; signal+4 = t0
#   0x18  str r5, [r1, #8]      ; signal+8 = t1
#   0x1C  ldr r2, [pc, #8]      ; loads CAFEBABE from PC+8+8 = 0x2C
#   0x20  str r2, [r1, #0]      ; signal+0 = CAFEBABE
#   0x24  bx lr
#   0x28  0xC2000004            ; literal: TIMER_COUNT addr
#   0x2C  0xCAFEBABE            ; literal: signal value
STUB_B_WORDS = [
    0xE59F3020,  # 0x00  ldr r3, [pc, #0x20]
    0xE5934000,  # 0x04  ldr r4, [r3]
    0xE2500001,  # 0x08  subs r0, r0, #1
    0x1AFFFFFD,  # 0x0C  bne -3
    0xE5935000,  # 0x10  ldr r5, [r3]
    0xE5814004,  # 0x14  str r4, [r1, #4]
    0xE5815008,  # 0x18  str r5, [r1, #8]
    0xE59F2008,  # 0x1C  ldr r2, [pc, #8]
    0xE5812000,  # 0x20  str r2, [r1, #0]
    0xE12FFF1E,  # 0x24  bx lr
    0x00000000,  # 0x28  -- placeholder, filled with TIMER_COUNT addr
    0xC2000004,  # 0x2C  literal: TIMER_COUNT addr (PC+8 from ldr at 0x00 → 0x28; off by 4)
    0xCAFEBABE,  # 0x30  literal: CAFEBABE (PC+8 from ldr at 0x1C → 0x2C; off by 4)
]
# Fix offsets: ldr r3 at 0x00 reads PC+0x20 = 0x08+0x20 = 0x28
# ldr r2 at 0x1C reads PC+8 = 0x24+8 = 0x2C
# So literals must be at 0x28 (TIMER_COUNT) and 0x2C (CAFEBABE).
# Words at indices 10 (0x28) and 11 (0x2C):
STUB_B_WORDS = [
    0xE59F3020,  # 0x00
    0xE5934000,  # 0x04
    0xE2500001,  # 0x08
    0x1AFFFFFD,  # 0x0C
    0xE5935000,  # 0x10
    0xE5814004,  # 0x14
    0xE5815008,  # 0x18
    0xE59F2008,  # 0x1C
    0xE5812000,  # 0x20
    0xE12FFF1E,  # 0x24
    0xC2000004,  # 0x28  TIMER_COUNT addr
    0xCAFEBABE,  # 0x2C  signal value
]

CYCLES_PER_ITER = 4
STUB_ADDR = 0x2B000
RETURN_TRAP_ADDR = 0x2B080
SIGNAL_ADDR = 0x2B100
TIMER_RELOAD = 0xC2000000
TIMER_COUNT  = 0xC2000004


def _encode(words):
    return b"".join(w.to_bytes(4, "little") for w in words)


def _run_stub(o: OpenOCD, stub_bytes: bytes, iters: int,
              poll_interval_s: float = 0.02, max_seconds: float = 60.0
              ) -> tuple[float, dict]:
    tmp = "/tmp/clock_stub.bin"
    Path(tmp).write_bytes(stub_bytes)
    o.halt()
    o.mww(RETURN_TRAP_ADDR, 0xEAFFFFFE)  # b . — trap loop
    # Clear signal area: [signal, t0, t1]
    o.mww(SIGNAL_ADDR + 0, 0)
    o.mww(SIGNAL_ADDR + 4, 0)
    o.mww(SIGNAL_ADDR + 8, 0)
    o.load_image(tmp, STUB_ADDR)
    o.cmd(f"reg r0 {iters}")
    o.cmd(f"reg r1 0x{SIGNAL_ADDR:x}")
    o.cmd(f"reg lr 0x{RETURN_TRAP_ADDR:x}")
    o.cmd(f"reg pc 0x{STUB_ADDR:x}")
    o.cmd("reg cpsr 0xd3")
    t0 = time.monotonic()
    o.resume()
    deadline = t0 + max_seconds
    polls = 0
    while time.monotonic() < deadline:
        time.sleep(poll_interval_s)
        try:
            o.halt()
            polls += 1
            sig = o.mdw(SIGNAL_ADDR)
            if sig == 0xCAFEBABE:
                t1 = time.monotonic()
                # Read t0/t1 sampled by stub (only meaningful for STUB_B)
                stub_t0 = o.mdw(SIGNAL_ADDR + 4)
                stub_t1 = o.mdw(SIGNAL_ADDR + 8)
                o.resume()
                return t1 - t0, {
                    "iters": iters,
                    "wall_s": t1 - t0,
                    "polls": polls,
                    "stub_t0": stub_t0,
                    "stub_t1": stub_t1,
                }
            o.resume()
        except Exception:
            continue
    return 0.0, {"error": f"no signal within {max_seconds}s", "polls": polls}


def measure_cpu_rate(o: OpenOCD, iters: int) -> tuple[float, dict]:
    """STUB A: returns iters/sec rate (wall-clock anchored)."""
    wall, dbg = _run_stub(o, _encode(STUB_A_WORDS), iters)
    if "error" in dbg:
        return 0.0, dbg
    return iters / wall, dbg


def measure_timer_via_stub(o: OpenOCD, iters: int) -> tuple[int, int, dict]:
    """STUB B: returns (delta_ticks, iters, debug). delta_ticks measured
    on-CPU between two TIMER_COUNT reads bracketing the stub loop."""
    wall, dbg = _run_stub(o, _encode(STUB_B_WORDS), iters)
    if "error" in dbg:
        return 0, 0, dbg
    o.halt()
    reload_v = o.mdw(TIMER_RELOAD)
    o.resume()
    t0, t1 = dbg["stub_t0"], dbg["stub_t1"]
    # TIMER_COUNT decrements; t1 should be < t0 if no wrap.
    if t1 <= t0:
        delta = t0 - t1
        wraps = 0
    else:
        delta = t0 + (reload_v - t1)
        wraps = 1
    dbg["reload"] = reload_v
    dbg["wraps"] = wraps
    dbg["delta_ticks"] = delta
    return delta, iters, dbg


def report_state(o: OpenOCD, label: str, args):
    pll = o.mdw(0xC900000C)
    print(f"\n=== {label}  (PLL_OUTPUT=0x{pll:04x}) ===")

    # STUB A: CPU stub-rate (long run for wall-clock accuracy).
    rate, dbg_a = measure_cpu_rate(o, args.cpu_iters)
    if "error" in dbg_a:
        print(f"  STUB A failed: {dbg_a['error']}")
        return None
    print(f"  STUB A: {dbg_a['iters']:>11,} iters in {dbg_a['wall_s']*1000:8.2f} ms wall  "
          f"(polls={dbg_a['polls']}, ±{20:.0f} ms)")
    print(f"          rate = {rate/1e6:7.3f} M-iters/s")

    # STUB B: Timer0 sampling (short run to avoid wrap).
    delta, iters_b, dbg_b = measure_timer_via_stub(o, args.timer_iters)
    if "error" in dbg_b:
        print(f"  STUB B failed: {dbg_b['error']}")
        return {"rate": rate}
    cycles = CYCLES_PER_ITER * iters_b
    cycles_per_tick = cycles / delta if delta > 0 else 0
    print(f"  STUB B: {iters_b:>11,} iters → {delta} Timer0 ticks  "
          f"(reload={dbg_b['reload']}, wraps_in_burst={dbg_b['wraps']}, "
          f"stub t0={dbg_b['stub_t0']}, t1={dbg_b['stub_t1']})")
    print(f"          cycles-per-tick assumed (4/iter): {cycles_per_tick:.3f}")
    # Combine: ratio of stub-iters to Timer0-ticks gives Timer0 freq from
    # the wall-clock-anchored stub rate, independent of cycles/iter.
    iters_per_tick = iters_b / delta if delta > 0 else 0
    timer_freq = rate / iters_per_tick if iters_per_tick > 0 else 0
    print(f"          iters-per-tick = {iters_per_tick:.4f}  →  "
          f"Timer0 freq = {timer_freq/1e6:7.3f} MHz")
    return {"rate": rate, "timer_freq": timer_freq, "iters_per_tick": iters_per_tick}


def main():
    p = argparse.ArgumentParser()
    p.add_argument("--mode", choices=("as-is", "crystal", "pll", "both"), default="both")
    p.add_argument("--speed", type=int, default=1000)
    p.add_argument("--cpu-iters", type=int, default=5_000_000,
                   help="STUB A iters (long-run, wall-clock CPU rate). "
                        "5M ≈ 0.2s at PLL.")
    p.add_argument("--timer-iters", type=int, default=10_000,
                   help="STUB B iters (short-run, Timer0 sampling). "
                        "10k iters × 4 cycles ≈ 200µs at 200 MHz CPU.")
    args = p.parse_args()

    o = OpenOCD(speed=args.speed)
    try:
        results = {}
        if args.mode == "as-is":
            results["as-is"] = report_state(o, "AS-IS", args)
        elif args.mode == "crystal":
            ensure_crystal_clock(o)
            results["crystal"] = report_state(o, "CRYSTAL CPU", args)
        elif args.mode == "pll":
            ensure_pll_clock(o)
            results["pll"] = report_state(o, "PLL CPU", args)
        else:  # both
            ensure_pll_clock(o)
            results["pll"] = report_state(o, "PLL CPU", args)
            # Crystal is ~17× slower; reduce iters proportionally so wall time stays manageable.
            args_crystal = argparse.Namespace(**vars(args))
            args_crystal.cpu_iters = max(1, args.cpu_iters // 8)
            args_crystal.timer_iters = max(1, args.timer_iters // 8)
            ensure_crystal_clock(o)
            results["crystal"] = report_state(o, "CRYSTAL CPU", args_crystal)
            ensure_pll_clock(o)
            results["pll2"] = report_state(o, "PLL CPU again (sanity)", args)

            r_pll = results.get("pll", {})
            r_crystal = results.get("crystal", {})
            if r_pll and r_crystal:
                ratio = r_pll["rate"] / r_crystal["rate"]
                derived_cpu = CRYSTAL_HZ * ratio
                print(f"\n=== Cross-check (12 MHz crystal anchor) ===")
                print(f"  CPU PLL/crystal stub rate ratio: {ratio:.3f}")
                print(f"  Derived PLL CPU = 12 MHz × {ratio:.3f} = {derived_cpu/1e6:.2f} MHz")
                print(f"  Expected if AHB = Fvco/2 = 200 MHz: ratio {200/12:.3f}")
                if abs(derived_cpu/1e6 - 200) < 20:
                    print(f"  ✓ Consistent with Ghidra finding (AHB = 200 MHz)")
                else:
                    print(f"  ⚠ Diverges: {derived_cpu/1e6 - 200:+.1f} MHz")

                if r_pll.get("timer_freq") and r_crystal.get("timer_freq"):
                    tf_pll = r_pll["timer_freq"]
                    tf_crystal = r_crystal["timer_freq"]
                    timer_ratio = tf_pll / tf_crystal
                    print(f"\n  Timer0 freq @ PLL CPU:     {tf_pll/1e6:6.3f} MHz")
                    print(f"  Timer0 freq @ crystal CPU: {tf_crystal/1e6:6.3f} MHz")
                    print(f"  Timer0 ratio (PLL/crystal): {timer_ratio:.3f}")
                    if abs(timer_ratio - 1) < 0.10:
                        print(f"  ✓ Timer0 is on a FIXED clock (not PLL-derived)")
                    elif abs(timer_ratio - ratio) < 1:
                        print(f"  → Timer0 tracks CPU clock ⇒ shares PLL/crystal source")
                    else:
                        print(f"  ⚠ Timer0 ratio doesn't match either fixed or CPU-tracking pattern")
    finally:
        o.close()


if __name__ == "__main__":
    main()
