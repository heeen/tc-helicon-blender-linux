#!/usr/bin/env python3
"""Resilient JTAG flash writer — reconnects after each sector.

Designed for flaky JTAG connections. Starts fresh OpenOCD for each sector,
so a JTAG link drop only loses one sector (retried automatically).

Usage:
    python3 firmware/jtag_flash_resilient.py                          # flash restored image
    python3 firmware/jtag_flash_resilient.py --ref firmware/blender_spi_patched.bin
    python3 firmware/jtag_flash_resilient.py --start-sector 17        # resume from sector 17
    python3 firmware/jtag_flash_resilient.py --speed 500              # slower JTAG clock
"""

import argparse
import os
import signal
import socket
import struct
import subprocess
import sys
import time
from pathlib import Path

FIRMWARE_DIR = Path(__file__).parent
OPENOCD_CFG = FIRMWARE_DIR.parent / "jtag" / "miolink-dice3-openocd.cfg"
DRIVER_BIN = FIRMWARE_DIR / "sram_flash_driver.bin"

# SRAM layout (must match sram_flash_driver.c)
DATA_BUF = 0x2B000
DRIVER_CODE = 0x2C000
MAILBOX = 0x2E000

# Mailbox offsets (must match sram_flash_driver.c)
MB_STATUS = 0     # 0=idle, 2=done_ok, 0xFF=error
MB_COMMAND = 4    # 1=erase, 2=write
MB_SPI_ADDR = 8
MB_PROGRESS = 12
MB_ERRORS = 16
MB_LAST_SR = 20

CMD_ERASE = 1
CMD_WRITE = 2
CMD_READ = 3
CMD_VERIFY = 4
CMD_BP_CLEAR = 5
ST_IDLE = 0
ST_DONE_OK = 2
ST_ERROR = 0xFF


def find_non_empty_sectors(spi_image: bytes, sector_size: int = 0x1000):
    """Find sectors that contain non-0xFF data."""
    sectors = []
    for addr in range(0, len(spi_image), sector_size):
        sector = spi_image[addr:addr + sector_size]
        if any(b != 0xFF for b in sector):
            non_ff = sum(1 for b in sector if b != 0xFF)
            sectors.append((addr, non_ff))
    return sectors


class OpenOCDSession:
    """Manages a single OpenOCD session for one sector write."""

    def __init__(self, speed: int = 1000):
        self.speed = speed
        self.proc = None
        self.sock = None

    def __enter__(self):
        self.start()
        return self

    def __exit__(self, *args):
        self.stop()

    def start(self):
        self.proc = subprocess.Popen(
            ["openocd", "-f", str(OPENOCD_CFG)],
            stdout=subprocess.DEVNULL,
            stderr=subprocess.DEVNULL,
        )
        time.sleep(2)
        self.sock = socket.create_connection(("localhost", 6666), timeout=10)
        self.cmd(f"adapter speed {self.speed}")

    def stop(self):
        if self.sock:
            try:
                self.cmd("resume")
            except Exception:
                pass
            self.sock.close()
            self.sock = None
        if self.proc:
            self.proc.terminate()
            self.proc.wait(timeout=5)
            self.proc = None

    def cmd(self, c: str, timeout: float = 10) -> str:
        self.sock.settimeout(timeout)
        self.sock.sendall((c + "\x1a").encode())
        buf = b""
        while b"\x1a" not in buf:
            buf += self.sock.recv(4096)
        return buf.decode().strip("\x1a").strip()

    def halt(self):
        return self.cmd("halt")

    def resume(self):
        return self.cmd("resume")

    def _send_cmd(self, cmd: int, spi_addr: int, wait_s: float) -> int:
        """Send mailbox command, resume, wait, halt, return status."""
        self.cmd(f"mww {MAILBOX + MB_SPI_ADDR:#x} {spi_addr:#x}")
        self.cmd(f"mww {MAILBOX + MB_STATUS:#x} 0")
        self.cmd(f"mww {MAILBOX + MB_PROGRESS:#x} 0")
        self.cmd(f"mww {MAILBOX + MB_ERRORS:#x} 0")
        self.cmd(f"mww {MAILBOX + MB_COMMAND:#x} {cmd}")
        self.resume()
        time.sleep(wait_s)
        self.halt()
        return int(self.cmd(f"mdw {MAILBOX + MB_STATUS:#x}").split()[-1], 16)

    def _read_errors(self) -> int:
        return int(self.cmd(f"mdw {MAILBOX + MB_ERRORS:#x}").split()[-1], 16)

    def write_sector(self, spi_addr: int, data: bytes) -> bool:
        """Erase + write + verify one sector. Returns True on success."""
        self.halt()

        # Load driver to SRAM
        self.cmd(f"load_image {DRIVER_BIN} {DRIVER_CODE:#x} bin")

        # Clear mailbox
        for i in range(8):
            self.cmd(f"mww {MAILBOX + i * 4:#x} 0")

        # Invalidate I-cache so CPU sees fresh driver code
        self.cmd("arm mcr 15 0 7 5 0 0")

        # Set PC to driver entry, SVC mode, resume
        self.cmd(f"reg pc {DRIVER_CODE:#x}")
        self.cmd("reg cpsr 0xd3")
        self.resume()
        time.sleep(0.3)

        # Load sector data to SRAM buffer
        tmp = f"/tmp/jtag_sector_{spi_addr:05x}.bin"
        with open(tmp, "wb") as f:
            f.write(data)

        self.halt()
        self.cmd(f"load_image {tmp} {DATA_BUF:#x} bin")

        # Clear block protection — must run before any erase/write
        self._send_cmd(CMD_BP_CLEAR, 0, 0.5)

        # Erase
        status = self._send_cmd(CMD_ERASE, spi_addr, 1.5)
        if status == ST_ERROR:
            os.unlink(tmp)
            return False

        # Write
        status = self._send_cmd(CMD_WRITE, spi_addr, 4)
        if status != ST_DONE_OK:
            os.unlink(tmp)
            return False

        # Verify — driver reads flash back and compares against DATA_BUF on-device
        status = self._send_cmd(CMD_VERIFY, spi_addr, 2)
        mismatches = self._read_errors()
        os.unlink(tmp)

        if status != ST_DONE_OK or mismatches != 0:
            return False

        return True


def main():
    p = argparse.ArgumentParser(description="Resilient JTAG flash writer")
    p.add_argument("--ref", default=str(FIRMWARE_DIR / "blender_spi_flash_restored.bin"))
    p.add_argument("--start-sector", type=int, default=0, help="Resume from this sector index")
    p.add_argument("--speed", type=int, default=1000, help="JTAG clock in kHz")
    p.add_argument("--retries", type=int, default=3)
    args = p.parse_args()

    spi_image = Path(args.ref).read_bytes()
    sectors = find_non_empty_sectors(spi_image)
    print(f"Reference: {args.ref} ({len(spi_image)} bytes, {len(sectors)} non-empty sectors)")
    print(f"JTAG speed: {args.speed} kHz, retries: {args.retries}")

    # Kill any existing OpenOCD
    subprocess.run(["pkill", "-x", "openocd"], capture_output=True)
    time.sleep(1)

    ok = 0
    fail = 0

    for idx, (addr, non_ff) in enumerate(sectors):
        if idx < args.start_sector:
            continue

        sector_data = spi_image[addr:addr + 0x1000]
        success = False

        for attempt in range(args.retries):
            try:
                with OpenOCDSession(speed=args.speed) as ocd:
                    label = f"[{idx + 1}/{len(sectors)}] {addr:#07x} ({non_ff} bytes)"
                    if attempt:
                        label += f" retry {attempt + 1}"
                    print(label, end=" → ", flush=True)
                    if ocd.write_sector(addr, sector_data):
                        print("erase+write+verify OK")
                        success = True
                        break
                    else:
                        errs = ocd._read_errors()
                        print(f"FAIL (mismatches={errs})")
            except Exception as e:
                print(f"JTAG ERROR: {e}")
                subprocess.run(["pkill", "-x", "openocd"], capture_output=True)
                time.sleep(2)
                # Reset USB on MioLink probe
                try:
                    import usb.core
                    dev = usb.core.find(idVendor=0x2E8A, idProduct=0x000C)
                    if dev:
                        dev.reset()
                        time.sleep(2)
                except Exception:
                    pass

        if success:
            ok += 1
        else:
            fail += 1
            print(f"  FAILED after {args.retries} attempts — skipping {addr:#x}")

    print(f"\nDone: {ok} OK, {fail} failed out of {len(sectors)} sectors")
    if fail == 0:
        print("SUCCESS — power cycle the device now.")
    else:
        print(f"WARNING: {fail} sectors failed. Re-run with --start-sector to retry.")


if __name__ == "__main__":
    main()
