#!/usr/bin/env python3
"""Fast autonomous JTAG flash writer.

Loads the entire SPI image + sector list to SRAM in one bulk transfer,
then the on-device driver iterates all sectors: erase → write → verify.
Host just polls progress. ~50s for full 122-sector image vs 16 min previously.

Usage:
    python3 firmware/jtag_flash_fast.py
    python3 firmware/jtag_flash_fast.py --ref firmware/blender_spi_patched.bin
    python3 firmware/jtag_flash_fast.py --speed 1000
"""

import argparse
import os
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
READBACK_BUF = 0x2E100
SECTOR_LIST = 0x2F100
IMAGE_BASE = 0x2F900

SECTOR_SIZE = 0x1000

# Mailbox offsets
MB_STATUS = 0
MB_COMMAND = 4
MB_SPI_ADDR = 8
MB_PROGRESS = 12
MB_ERRORS = 16
MB_LAST_SR = 20
MB_TOTAL = 24

CMD_FLASH_ALL = 6
CMD_SPI_CLEANUP = 7
ST_IDLE = 0
ST_RUNNING = 1
ST_DONE_OK = 2
ST_ERROR = 0xFF


class OpenOCD:
    def __init__(self, speed=1000):
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

    def load_image(self, path, addr):
        r = self.cmd(f"load_image {path} {addr:#x} bin", timeout=120)
        return r


def main():
    p = argparse.ArgumentParser(description="Fast autonomous JTAG flash writer")
    p.add_argument("--ref", default=str(FIRMWARE_DIR / "blender_spi_flash_restored.bin"))
    p.add_argument("--speed", type=int, default=1000)
    p.add_argument("--reboot", action="store_true",
                   help="Attempt soft reboot via ROM after flashing")
    args = p.parse_args()

    spi = Path(args.ref).read_bytes()

    # Find non-empty sectors
    sectors = []
    for addr in range(0, len(spi), SECTOR_SIZE):
        sec = spi[addr:addr + SECTOR_SIZE]
        non_ff = sum(1 for b in sec if b != 0xFF)
        if non_ff > 0:
            sectors.append((addr, non_ff))

    total_data = len(sectors) * SECTOR_SIZE
    print(f"Reference: {args.ref}")
    print(f"  {len(sectors)} non-empty sectors, {total_data // 1024} KB data")
    print(f"  Speed: {args.speed} kHz")

    # Pack sector data contiguously
    image_data = bytearray()
    sector_list = bytearray()
    for addr, _ in sectors:
        offset = len(image_data)
        image_data.extend(spi[addr:addr + SECTOR_SIZE])
        sector_list.extend(struct.pack("<II", addr, offset))
    # Terminator
    sector_list.extend(struct.pack("<II", 0xFFFFFFFF, 0))

    print(f"  Image blob: {len(image_data)} bytes")
    print(f"  Sector list: {len(sector_list)} bytes ({len(sectors)} entries)")

    # Check fit
    if IMAGE_BASE + len(image_data) > 0x80000:
        print(f"ERROR: image too large for SRAM ({IMAGE_BASE + len(image_data):#x} > 0x80000)")
        sys.exit(1)

    # Write temp files
    img_tmp = "/tmp/jtag_flash_image.bin"
    list_tmp = "/tmp/jtag_flash_list.bin"
    with open(img_tmp, "wb") as f:
        f.write(image_data)
    with open(list_tmp, "wb") as f:
        f.write(sector_list)

    # Start OpenOCD
    subprocess.run(["pkill", "-x", "openocd"], capture_output=True)
    time.sleep(1)

    o = OpenOCD(speed=args.speed)
    t0 = time.monotonic()

    try:
        o.halt()

        # Disable USB to prevent its DMA corrupting SRAM during load.
        # Do NOT disable DMA engine or SPI — the flash driver needs them.
        print("\nDisabling USB...")
        o.mww(0x90000008, 0)           # USB controller stop (RS=0)
        o.mww(0x90000014, 0xFFFFFFFF)  # USB flush all endpoints

        # Load driver
        print("Loading driver...")
        o.load_image(str(DRIVER_BIN), DRIVER_CODE)

        # Load sector list
        print("Loading sector list...")
        o.load_image(list_tmp, SECTOR_LIST)

        # Load bulk image data
        print(f"Loading image data ({len(image_data) // 1024} KB)...")
        t_load = time.monotonic()
        o.load_image(img_tmp, IMAGE_BASE)
        dt = time.monotonic() - t_load
        rate = len(image_data) / dt / 1024
        print(f"  Loaded in {dt:.1f}s ({rate:.1f} KB/s)")

        # Clear mailbox
        for i in range(8):
            o.mww(MAILBOX + i * 4, 0)

        # Invalidate caches, set PC to driver, resume
        o.cmd("arm mcr 15 0 7 5 0 0")
        o.cmd("arm mcr 15 0 7 6 0 0")
        o.cmd(f"reg pc {DRIVER_CODE:#x}")
        o.cmd("reg cpsr 0xd3")
        o.resume()
        time.sleep(0.3)

        # Verify driver is idle
        o.halt()
        status = o.mdw(MAILBOX + MB_STATUS)
        if status != ST_IDLE:
            print(f"WARNING: driver status={status} after init")
        o.resume()

        # Send CMD_FLASH_ALL
        print(f"\nFlashing {len(sectors)} sectors autonomously...")
        o.halt()
        o.mww(MAILBOX + MB_STATUS, 0)
        o.mww(MAILBOX + MB_PROGRESS, 0)
        o.mww(MAILBOX + MB_ERRORS, 0)
        o.mww(MAILBOX + MB_TOTAL, 0)
        o.mww(MAILBOX + MB_COMMAND, CMD_FLASH_ALL)
        o.resume()

        # Poll progress
        t_flash = time.monotonic()
        last_progress = -1
        while True:
            time.sleep(2)
            o.halt()
            status = o.mdw(MAILBOX + MB_STATUS)
            progress = o.mdw(MAILBOX + MB_PROGRESS)
            errors = o.mdw(MAILBOX + MB_ERRORS)
            total = o.mdw(MAILBOX + MB_TOTAL)
            o.resume()

            if progress != last_progress:
                elapsed = time.monotonic() - t_flash
                if progress > 0 and total > 0:
                    eta = elapsed / progress * (total - progress)
                    print(f"  [{progress}/{total}] errors={errors} "
                          f"({elapsed:.0f}s elapsed, ~{eta:.0f}s remaining)")
                else:
                    print(f"  [{progress}/{total}] errors={errors}")
                last_progress = progress

            if status == ST_DONE_OK:
                break
            if status == ST_ERROR:
                print(f"DRIVER ERROR after {progress}/{total} sectors, {errors} errors")
                break
            if status == ST_IDLE:
                # Command was consumed, check if it completed
                if progress >= total and total > 0:
                    break
                print(f"  (idle, progress={progress}/{total})")

        dt_flash = time.monotonic() - t_flash
        dt_total = time.monotonic() - t0

        print(f"\n{'=' * 60}")
        if errors == 0:
            print(f"SUCCESS: {total} sectors, 0 errors")
        else:
            print(f"WARNING: {total} sectors, {errors} ERRORS")
        print(f"Flash time: {dt_flash:.1f}s ({dt_flash/total:.2f}s/sector)")
        print(f"Total time: {dt_total:.1f}s (including {dt_total - dt_flash:.1f}s JTAG load)")
        print(f"{'=' * 60}")
        # Restore SPI/DMA to clean state
        print("Restoring SPI/DMA to clean state...")
        o.halt()
        o.mww(MAILBOX + MB_STATUS, 0)
        o.mww(MAILBOX + MB_COMMAND, CMD_SPI_CLEANUP)
        o.resume()
        time.sleep(0.5)

        if errors == 0 and args.reboot:
            print("Attempting soft reboot...")
            o.halt()
            # Full USB controller reset (ChipIdea RST bit self-clears)
            o.mww(0x90000014, 0xFFFFFFFF)   # ENDPTFLUSH all
            val = o.mdw(0x90000008)
            o.mww(0x90000008, (val & ~1))   # clear RS → disconnect
            o.mww(0x90000008, (val | 2))    # set RST → full reset
            time.sleep(0.01)                # RST self-clears in <1ms
            # Reset SPI to XIP mode (same as software_reboot)
            o.mww(0xCC000008, 1)            # SPI EN = 1 (power-on)
            o.mww(0xCC000010, 2)            # SPI CS = 2 (power-on)
            o.mww(0xCC000014, 0x38)         # SPI CLK = XIP mode
            # Invalidate caches
            o.cmd("arm mcr 15 0 7 5 0 0")
            o.cmd("arm mcr 15 0 7 6 0 0")
            # Jump to TCAT bootloader (not ROM — bootloader handles SPI init)
            o.cmd("reg pc 0x4F000")
            o.cmd("reg cpsr 0xd3")
            o.resume()
            print("Reboot triggered. Watch UART for TCAT-BOOT.")
        elif errors == 0:
            print("Power cycle the device now.")

    finally:
        os.unlink(img_tmp)
        os.unlink(list_tmp)
        o.close()


if __name__ == "__main__":
    main()
