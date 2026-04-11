#!/usr/bin/env python3
"""DICE3 hardware exploration — register scans for flash driver optimization.

Connects to OpenOCD via TCL, halts CPU, dumps registers from:
  - SPI flash controller (0xCC000000)
  - DMA engine (0x80000000)
  - Interrupt controller candidates (0xFFFFF000, 0x40000000)
  - Timer/counter (0xC2000000)
  - Exception vectors (0x00000000)
  - Unknown peripherals (0xC9000000, 0xCB000000)

Usage:
    # Start OpenOCD first, or let the script start it:
    python3 firmware/explore_hardware.py
    python3 firmware/explore_hardware.py --no-start-openocd  # if already running
"""

import argparse
import socket
import subprocess
import sys
import time
from pathlib import Path

FIRMWARE_DIR = Path(__file__).parent
OPENOCD_CFG = FIRMWARE_DIR.parent / "jtag" / "miolink-dice3-openocd.cfg"


class OpenOCD:
    def __init__(self, start=True, speed=1000):
        self.proc = None
        if start:
            subprocess.run(["pkill", "-x", "openocd"], capture_output=True)
            time.sleep(1)
            self.proc = subprocess.Popen(
                ["openocd", "-f", str(OPENOCD_CFG)],
                stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL,
            )
            time.sleep(3)
        self.sock = socket.create_connection(("localhost", 6666), timeout=30)
        self.cmd(f"adapter speed {speed}")

    def close(self):
        try:
            self.cmd("resume")
        except Exception:
            pass
        self.sock.close()
        if self.proc:
            self.proc.terminate()
            self.proc.wait(timeout=5)

    def cmd(self, c, timeout=30):
        self.sock.settimeout(timeout)
        self.sock.sendall((c + "\x1a").encode())
        buf = b""
        while b"\x1a" not in buf:
            buf += self.sock.recv(4096)
        return buf.decode().strip("\x1a").strip()

    def halt(self):
        self.cmd("halt")

    def resume(self):
        self.cmd("resume")

    def mdw(self, addr):
        """Read one 32-bit word. Returns int or None on error."""
        try:
            r = self.cmd(f"mdw {addr:#010x}")
            # Output: "0xADDRESS: VALUE"
            return int(r.split()[-1], 16)
        except Exception:
            return None

    def mdw_bulk(self, addr, count):
        """Read count 32-bit words starting at addr. Returns list of (addr, val)."""
        results = []
        try:
            r = self.cmd(f"mdw {addr:#010x} {count}")
            # OpenOCD outputs: "0xADDR: VAL1 VAL2 VAL3 ..."
            # May span multiple lines for large counts
            for line in r.strip().split("\n"):
                line = line.strip()
                if not line or ":" not in line:
                    continue
                parts = line.split(":")
                base = int(parts[0].strip(), 16)
                vals = parts[1].strip().split()
                for i, v in enumerate(vals):
                    results.append((base + i * 4, int(v, 16)))
        except Exception as e:
            print(f"  [ERROR reading {addr:#010x} x{count}]: {e}")
        return results

    def read_reg_range(self, base, count, name=""):
        """Read a range of registers and return as list of (offset, value)."""
        print(f"\n{'=' * 70}")
        print(f"  {name} — {base:#010x} to {base + count * 4 - 4:#010x} ({count} words)")
        print(f"{'=' * 70}")
        results = self.mdw_bulk(base, count)
        return results


def print_reg_table(results, base, known_regs=None):
    """Pretty-print register dump with known names."""
    if known_regs is None:
        known_regs = {}
    for addr, val in results:
        offset = addr - base
        name = known_regs.get(offset, "")
        marker = ""
        if val == 0:
            marker = "  (zero)"
        elif val == 0xFFFFFFFF:
            marker = "  (all-ones)"
        elif val == 0xDEADBEEF:
            marker = "  (DEADBEEF)"
        print(f"  +{offset:03X} [{addr:#010x}] = {val:#010x}  {name}{marker}")


def scan_spi_controller(o):
    """Scan SPI flash controller at 0xCC000000."""
    known = {
        0x00: "CTRL",
        0x04: "LEN",
        0x08: "EN",
        0x0C: "???",
        0x10: "CS",
        0x14: "CLK",
        0x18: "CLRINT",
        0x1C: "???",
        0x20: "???",
        0x24: "???",
        0x28: "STAT",
        0x2C: "DMAGO",
        0x30: "???",
        0x34: "ERR",
        0x38: "???",
        0x3C: "???",
        0x40: "???",
        0x44: "???",
        0x48: "???",
        0x4C: "DMAMD",
        0x50: "DMACFG0",
        0x54: "DMACFG1",
        0x58: "???",
        0x5C: "???",
        0x60: "DATA",
        0x64: "???",
        0x68: "???",
        0x6C: "???",
        0x70: "RXDMA",
        0x74: "???",
        0x78: "???",
        0x7C: "???",
        0x80: "TXDMA",
    }
    results = o.read_reg_range(0xCC000000, 64, "SPI Flash Controller (0xCC000000)")
    print_reg_table(results, 0xCC000000, known)

    # Also scan 0xCC000100-0xCC0001FF for any extended registers
    results2 = o.read_reg_range(0xCC000100, 64, "SPI Flash Controller extended (0xCC000100)")
    non_zero = [(a, v) for a, v in results2 if v != 0]
    if non_zero:
        print("  Non-zero extended registers:")
        print_reg_table(non_zero, 0xCC000100)
    else:
        print("  (all zeros)")

    return results


def scan_spi_led(o):
    """Scan SPI LED controller at 0xCF000000 for comparison."""
    results = o.read_reg_range(0xCF000000, 34, "SPI LED Controller (0xCF000000) — for comparison")
    known = {
        0x00: "CTRL", 0x04: "LEN", 0x08: "EN", 0x10: "CS",
        0x14: "CLK", 0x18: "CLRINT", 0x28: "STAT", 0x2C: "DMAGO",
        0x34: "ERR", 0x4C: "DMAMD", 0x50: "DMACFG0", 0x54: "DMACFG1",
        0x60: "DATA", 0x70: "RXDMA", 0x80: "TXDMA",
    }
    print_reg_table(results, 0xCF000000, known)
    return results


def scan_dma_engine(o):
    """Scan DMA engine at 0x80000000."""
    known_global = {
        0x00: "???",
        0x04: "???",
        0x08: "EN",
        0x0C: "???",
        0x10: "ICLR",
        0x14: "ISTAT",
        0x18: "???",
        0x1C: "???",
        0x20: "???",
        0x24: "???",
        0x28: "???",
        0x2C: "???",
        0x30: "CH0CLR",
        0x34: "CH1CLR",
        0x38: "CH2CLR",
        0x3C: "CH3CLR",
    }

    # Global registers
    results = o.read_reg_range(0x80000000, 32, "DMA Engine — Global (0x80000000)")
    print_reg_table(results, 0x80000000, known_global)

    # Per-channel registers (4 channels, 0x20 each, starting at 0x100)
    ch_known = {
        0x00: "SRC", 0x04: "DST", 0x08: "NXT",
        0x0C: "CFG", 0x10: "TRG", 0x14: "???", 0x18: "???", 0x1C: "???",
    }
    for ch in range(4):
        base = 0x80000100 + ch * 0x20
        results = o.read_reg_range(base, 8, f"DMA Channel {ch} ({base:#010x})")
        print_reg_table(results, base, ch_known)

    return results


def scan_interrupt_controller(o):
    """Try to find the interrupt controller."""
    print(f"\n{'=' * 70}")
    print(f"  Interrupt Controller Search")
    print(f"{'=' * 70}")

    # ARM926EJ-S exception vectors at 0x00000000
    print("\n  Exception vectors (0x00000000-0x0000001F):")
    vectors = o.mdw_bulk(0x00000000, 8)
    vec_names = ["RESET", "UNDEF", "SWI", "PREFETCH_ABORT",
                 "DATA_ABORT", "RESERVED", "IRQ", "FIQ"]
    for (addr, val), name in zip(vectors, vec_names):
        # Decode ARM branch: if top byte is 0xEA, it's a B instruction
        if (val >> 24) == 0xEA:
            offset = val & 0x00FFFFFF
            if offset & 0x800000:
                offset |= 0xFF000000  # sign extend
            target = addr + 8 + (offset << 2)
            print(f"  [{addr:#010x}] {name:16s} = {val:#010x}  → B {target:#010x}")
        else:
            print(f"  [{addr:#010x}] {name:16s} = {val:#010x}")

    # Follow IRQ vector to find handler
    irq_vec = vectors[6][1] if len(vectors) > 6 else None
    if irq_vec and (irq_vec >> 24) == 0xEA:
        offset = irq_vec & 0x00FFFFFF
        if offset & 0x800000:
            offset |= 0xFF000000
        irq_handler = 0x18 + 8 + (offset << 2)
        print(f"\n  IRQ handler at {irq_handler:#010x} — reading first 8 words:")
        handler_words = o.mdw_bulk(irq_handler, 8)
        for addr, val in handler_words:
            # Look for LDR pc, [rN, #imm] pattern that reads VIC vector address
            print(f"    [{addr:#010x}] = {val:#010x}")

    # Standard VIC location for ARM926
    print("\n  Trying standard VIC at 0xFFFFF000:")
    vic_results = o.mdw_bulk(0xFFFFF000, 16)
    all_zero = all(v == 0 for _, v in vic_results)
    all_ff = all(v == 0xFFFFFFFF for _, v in vic_results)
    if all_zero or all_ff:
        print(f"    (all {'zeros' if all_zero else 'ones'} — likely unmapped)")
    else:
        vic_known = {
            0x00: "IRQ_STATUS", 0x04: "FIQ_STATUS", 0x08: "RAW_STATUS",
            0x0C: "INT_SELECT", 0x10: "INT_ENABLE", 0x14: "INT_EN_CLR",
            0x18: "SOFT_INT", 0x1C: "SOFT_INT_CLR",
            0x20: "PROTECTION", 0x24: "SW_PRIORITY_MASK",
            0x30: "VECT_ADDR",
        }
        print_reg_table(vic_results, 0xFFFFF000, vic_known)

    # Try 0x40000000 (unknown peripheral that reads 0x800000)
    print("\n  Unknown peripheral at 0x40000000:")
    unk_results = o.read_reg_range(0x40000000, 32, "Unknown (0x40000000)")
    non_zero = [(a, v) for a, v in unk_results if v != 0]
    if non_zero:
        print_reg_table(non_zero, 0x40000000)
    else:
        print("  (all zeros)")

    # Try common TCAT/DICE interrupt controller locations
    for candidate in [0xC0000000, 0xC1000000, 0xC3000000, 0xC6000000,
                      0xC7000000, 0xC8000000, 0xCA000000]:
        try:
            val = o.mdw(candidate)
            if val is not None and val != 0 and val != 0xFFFFFFFF:
                print(f"\n  Candidate at {candidate:#010x}: {val:#010x}")
                extra = o.mdw_bulk(candidate, 16)
                non_zero = [(a, v) for a, v in extra if v != 0 and v != 0xFFFFFFFF]
                if non_zero:
                    print_reg_table(non_zero, candidate)
        except Exception:
            pass


def scan_timer(o):
    """Scan timer/counter at 0xC2000000."""
    results = o.read_reg_range(0xC2000000, 32, "Timer/Counter (0xC2000000)")
    print_reg_table(results, 0xC2000000)

    # Check if any register is a free-running counter (read twice, compare)
    print("\n  Free-running counter check (read same offsets twice):")
    for offset in [0x00, 0x04, 0x08, 0x0C, 0x10, 0x14, 0x18, 0x1C]:
        addr = 0xC2000000 + offset
        v1 = o.mdw(addr)
        v2 = o.mdw(addr)
        if v1 != v2:
            print(f"    +{offset:03X}: {v1:#010x} → {v2:#010x}  *** COUNTER ***")
        else:
            if v1 != 0:
                print(f"    +{offset:03X}: {v1:#010x} (static)")

    return results


def scan_clock_pll(o):
    """Scan clock/PLL registers at 0xC5000000 (beyond UART)."""
    # UART is at +0x00 (TX data) and +0x18 (status)
    # PLL registers reportedly at +0x24-0x30
    results = o.read_reg_range(0xC5000000, 32, "UART + Clock/PLL (0xC5000000)")
    known = {
        0x00: "UART_TX_DATA",
        0x18: "UART_STATUS",
        0x24: "PLL_CFG?",
        0x28: "PLL_CFG2?",
        0x2C: "CLK_DIV?",
        0x30: "CLK_STATUS?",
    }
    print_reg_table(results, 0xC5000000, known)
    return results


def scan_unknown_peripherals(o):
    """Scan 0xC9000000 and 0xCB000000."""
    for base, name in [(0xC9000000, "Unknown C9"), (0xCB000000, "Unknown CB")]:
        results = o.read_reg_range(base, 32, f"{name} ({base:#010x})")
        non_zero = [(a, v) for a, v in results if v != 0]
        if non_zero:
            print_reg_table(non_zero, base)
        else:
            print("  (all zeros)")


def scan_cp15(o):
    """Read key CP15 registers for cache/MMU state."""
    print(f"\n{'=' * 70}")
    print(f"  CP15 System Control (via ARM MCR/MRC)")
    print(f"{'=' * 70}")

    cp15_regs = [
        ("c0 c0 0", "Main ID"),
        ("c0 c0 1", "Cache Type"),
        ("c1 c0 0", "Control (MMU/cache enables)"),
        ("c1 c0 1", "Auxiliary Control"),
    ]
    for crn_crm_op2, name in cp15_regs:
        try:
            r = o.cmd(f"arm mrc 15 0 {crn_crm_op2}")
            val = int(r.strip().split()[-1], 0) if r.strip() else None
            if val is not None:
                print(f"  {name:35s} = {val:#010x}")
                if "Control" in name and "Auxiliary" not in name:
                    print(f"    MMU={'ON' if val & 1 else 'OFF'}, "
                          f"D-cache={'ON' if val & 4 else 'OFF'}, "
                          f"I-cache={'ON' if val & (1<<12) else 'OFF'}, "
                          f"Write-buf={'ON' if val & 8 else 'OFF'}")
        except Exception as e:
            print(f"  {name:35s} = ERROR: {e}")


def main():
    p = argparse.ArgumentParser(description="DICE3 hardware register exploration")
    p.add_argument("--no-start-openocd", action="store_true",
                   help="Don't start OpenOCD (use existing instance)")
    p.add_argument("--speed", type=int, default=1000, help="JTAG clock kHz")
    args = p.parse_args()

    o = OpenOCD(start=not args.no_start_openocd, speed=args.speed)

    try:
        o.halt()
        print("CPU halted.")

        # Read PC and CPSR for context
        pc = o.cmd("reg pc")
        cpsr = o.cmd("reg cpsr")
        print(f"  {pc.strip()}")
        print(f"  {cpsr.strip()}")

        scan_cp15(o)
        scan_spi_controller(o)
        scan_spi_led(o)
        scan_dma_engine(o)
        scan_interrupt_controller(o)
        scan_timer(o)
        scan_clock_pll(o)
        scan_unknown_peripherals(o)

        print(f"\n{'=' * 70}")
        print("  Exploration complete. Resuming CPU.")
        print(f"{'=' * 70}")

    finally:
        o.close()


if __name__ == "__main__":
    main()
