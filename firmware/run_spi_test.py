#!/usr/bin/env python3
"""Run SPI behavior experiments on DICE3 via JTAG.

Loads explore_spi_test.bin to SRAM, executes, reads results struct.

Usage:
    python3 firmware/run_spi_test.py
"""

import socket
import struct
import subprocess
import sys
import time
from pathlib import Path

FIRMWARE_DIR = Path(__file__).parent
OPENOCD_CFG = FIRMWARE_DIR.parent / "jtag" / "miolink-dice3-openocd.cfg"
TEST_BIN = FIRMWARE_DIR / "explore_spi_test.bin"

DRIVER_CODE = 0x2C000
RESULT_BASE = 0x2E000
RESULT_SIZE = 0x80  # struct results size


class OpenOCD:
    def __init__(self, speed=1000):
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

    def mww(self, addr, val):
        self.cmd(f"mww {addr:#x} {val:#x}")

    def mdw(self, addr):
        r = self.cmd(f"mdw {addr:#x}")
        return int(r.split()[-1], 16)

    def mdw_bulk(self, addr, count):
        results = []
        r = self.cmd(f"mdw {addr:#x} {count}")
        for line in r.strip().split("\n"):
            if ":" not in line:
                continue
            parts = line.split(":")
            base = int(parts[0].strip(), 16)
            for i, v in enumerate(parts[1].strip().split()):
                results.append(int(v, 16))
        return results

    def load_image(self, path, addr):
        return self.cmd(f"load_image {path} {addr:#x} bin", timeout=60)


def main():
    o = OpenOCD(speed=1000)

    try:
        o.halt()

        # Disable USB to prevent DMA interference
        o.mww(0x90000008, 0)
        o.mww(0x90000014, 0xFFFFFFFF)

        # Load test binary
        print(f"Loading {TEST_BIN.name} ({TEST_BIN.stat().st_size} bytes)...")
        o.load_image(str(TEST_BIN), DRIVER_CODE)

        # Clear result area
        for i in range(RESULT_SIZE // 4):
            o.mww(RESULT_BASE + i * 4, 0)

        # Set PC and run
        o.cmd("arm mcr 15 0 7 5 0 0")  # invalidate I-cache
        o.cmd("arm mcr 15 0 7 6 0 0")  # invalidate D-cache
        o.cmd(f"reg pc {DRIVER_CODE:#x}")
        o.cmd("reg cpsr 0xd3")
        o.resume()

        # Poll for completion (magic = 0x54455354)
        print("Running experiments...")
        for attempt in range(60):
            time.sleep(1)
            o.halt()
            magic = o.mdw(RESULT_BASE)
            if magic == 0x54455354:
                break
            print(f"  waiting... (magic={magic:#010x})")
            o.resume()
        else:
            print("TIMEOUT — test did not complete in 60s")
            # Read partial results anyway

        # Read results
        words = o.mdw_bulk(RESULT_BASE, RESULT_SIZE // 4)

        print()
        print("=" * 70)
        print("  SPI Behavior Experiment Results")
        print("=" * 70)

        magic = words[0]
        test_mask = words[1]
        print(f"  Magic: {magic:#010x} ({'OK' if magic == 0x54455354 else 'INCOMPLETE'})")
        print(f"  Tests run: {test_mask:#04x}")

        # Test 5: Timer (run first for calibration)
        if test_mask & 0x10:
            print()
            print("  --- Test 5: Timer Calibration ---")
            print(f"  Reload value:      {words[24]}")
            print(f"  Control register:  {words[25]:#x}")
            print(f"  100 iter cycles:   {words[26]}")
            print(f"  1000 iter cycles:  {words[27]}")
            if words[26] > 0:
                per_iter = words[27] / 1000.0
                per_100 = words[26] / 100.0
                print(f"  Cycles/iteration:  {per_iter:.1f} (from 1000), {per_100:.1f} (from 100)")
            print(f"  Counter2 delta:    {words[28]}")
            print(f"  DMA TX 1-byte:     {words[29]} cycles")
            print(f"  PIO TX 1-byte:     {words[30]} cycles")

        # Test 1: DMA re-trigger
        if test_mask & 0x01:
            print()
            print("  --- Test 1: DMA Re-trigger ---")
            print(f"  Full setup OK:     {words[2]}")
            retrig = words[3]
            print(f"  Re-trigger OK:     {retrig & 1} (with EN toggle), {(retrig >> 1) & 1} (no EN toggle)")
            print(f"  Full setup cycles: {words[4]}")
            print(f"  Re-trigger cycles: {words[5]}")
            if words[4] > 0 and words[5] > 0:
                speedup = words[4] / words[5]
                print(f"  Speedup:           {speedup:.1f}x")

        # Test 2: SPI_STAT timing
        if test_mask & 0x02:
            print()
            print("  --- Test 2: SPI_STAT Timing ---")
            print(f"  STAT after DMA:    {words[6]:#x}")
            print(f"  STAT poll iters:   {words[7]} (until bit 0 clears)")
            print(f"  DMA poll iters:    {words[8]} (until ISTAT fires)")

        # Test 3: RDSR reliability
        if test_mask & 0x04:
            print()
            print("  --- Test 3: RDSR Reliability ---")
            print(f"  Successful reads:  {words[9]}/10")
            print(f"  0xFF count:        {words[20]}")
            vals = words[10:20]
            for i, v in enumerate(vals):
                status = ""
                if v == 0xFF:
                    status = " (FAIL — 0xFF)"
                elif v == 0xFE:
                    status = " (TIMEOUT)"
                elif v & 1:
                    status = " (BUSY)"
                else:
                    status = f" (OK: SR={v:#04x})"
                print(f"    [{i}] = {v:#04x}{status}")

        # Test 4: PIO AAI
        if test_mask & 0x08:
            print()
            print("  --- Test 4: PIO AAI ---")
            pio_ok = words[21]
            print(f"  DMA AAI pair 1:    {'OK' if pio_ok & 1 else 'FAIL'} (byte0={words[22]:#04x}, byte1={words[23]:#04x})")
            print(f"  PIO AAI pair 2:    {'OK' if pio_ok & 2 else 'FAIL'}")
            if pio_ok & 2:
                print("  *** PIO TX WORKS for AAI subsequent pairs! ***")
            else:
                print("  PIO TX does NOT work for AAI — must use DMA")

        print()
        print("=" * 70)

    finally:
        o.close()


if __name__ == "__main__":
    main()
