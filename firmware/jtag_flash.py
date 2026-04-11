#!/usr/bin/env python3
"""Unified JTAG flash writer for TC Helicon Blender.

Uses sram_flash_driver_v2 with batched autonomous flash-all.
Proven primitives: RDSR-polled erase, DMA AAI write, on-device verify.

Usage:
    python3 firmware/jtag_flash.py                                    # flash restored image
    python3 firmware/jtag_flash.py --ref firmware/blender_spi_patched.bin
    python3 firmware/jtag_flash.py --speed 1000 --reboot
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
DRIVER_BIN = FIRMWARE_DIR / "sram_flash_driver_v2.bin"

# SRAM layout (must match sram_flash_driver_v2.c)
DRIVER_CODE = 0x2C000
MB = 0x2E000
SECTOR_LIST = 0x2F100
IMAGE_BASE = 0x2F900
MAX_BATCH = 32  # limited by safe SRAM range for OpenOCD load_image (0x2F900-0x4FFFF)
SECTOR_SIZE = 0x1000

ST_IDLE = 0
ST_RUNNING = 1
ST_DONE = 2
ST_ERROR = 0xFF

# Mailbox field offsets
F_STATUS = 0
F_COMMAND = 4
F_SPI_ADDR = 8
F_PROGRESS = 12
F_ERRORS = 16
F_LAST_SR = 20
F_TOTAL = 24
F_FAIL_ADDR = 28


class OpenOCD:
    def __init__(self, speed=1000):
        self.proc = subprocess.Popen(
            ["openocd", "-f", str(OPENOCD_CFG)],
            stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL,
        )
        time.sleep(4)
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
        return int(self.cmd(f"mdw {addr:#x}").split()[-1], 16)

    def load_image(self, path, addr):
        self.cmd(f"load_image {path} {addr:#x} bin", timeout=120)


def send_cmd(o, command, spi_addr=0):
    """Send a mailbox command. Driver must be halted."""
    o.mww(MB + F_STATUS, 0)
    o.mww(MB + F_SPI_ADDR, spi_addr)
    o.mww(MB + F_PROGRESS, 0)
    o.mww(MB + F_ERRORS, 0)
    o.mww(MB + F_FAIL_ADDR, 0)
    o.mww(MB + F_COMMAND, command)  # write LAST


def poll_done(o, label, timeout_s=60, interval=1.0):
    """Poll until status is DONE or ERROR. Returns (status, errors, progress)."""
    deadline = time.monotonic() + timeout_s
    last_print = 0
    while time.monotonic() < deadline:
        time.sleep(interval)
        o.halt()
        status = o.mdw(MB + F_STATUS)
        progress = o.mdw(MB + F_PROGRESS)
        errors = o.mdw(MB + F_ERRORS)
        total = o.mdw(MB + F_TOTAL)
        fail = o.mdw(MB + F_FAIL_ADDR)
        o.resume()

        if status == ST_DONE:
            return status, errors, progress
        if status == ST_ERROR:
            print(f"  {label}: ERROR at {fail:#x}, errors={errors:#x}")
            return status, errors, progress
        # ST_IDLE or ST_RUNNING — keep polling
        now = time.monotonic()
        if now - last_print >= 5:
            if total > 0:
                print(f"  [{progress}/{total}] {now - (deadline - timeout_s):.0f}s")
            last_print = now

    print(f"  {label}: TIMEOUT after {timeout_s}s")
    return ST_ERROR, 0xDEAD, 0


def main():
    p = argparse.ArgumentParser(description="JTAG flash writer (v2)")
    p.add_argument("--ref", default=str(FIRMWARE_DIR / "blender_spi_flash_restored.bin"))
    p.add_argument("--speed", type=int, default=1000)
    p.add_argument("--reboot", action="store_true", help="Soft reboot after flashing")
    args = p.parse_args()

    spi = Path(args.ref).read_bytes()

    # Find non-empty sectors
    sectors = []
    for addr in range(0, len(spi), SECTOR_SIZE):
        sec = spi[addr:addr + SECTOR_SIZE]
        if any(b != 0xFF for b in sec):
            sectors.append(addr)

    total_data = len(sectors) * SECTOR_SIZE
    print(f"Image: {args.ref}")
    print(f"  {len(sectors)} sectors, {total_data // 1024} KB")
    print(f"  JTAG speed: {args.speed} kHz")

    subprocess.run(["pkill", "-x", "openocd"], capture_output=True)
    time.sleep(1)

    o = OpenOCD(speed=args.speed)
    t0 = time.monotonic()

    try:
        o.halt()

        # Disable USB to prevent its DMA corrupting SRAM
        o.mww(0x90000008, 0)
        o.mww(0x90000014, 0xFFFFFFFF)

        # Disable XIP: the TCAT bootloader maps SPI flash into the SRAM
        # address space (SPI_CLK=0x38). With XIP active, writes to SRAM
        # addresses in the XIP window are silently dropped.
        # Set SPI_CLK=2 (DMA mode, not XIP) to break the mapping while
        # keeping the SPI clock functional for the flash driver.
        o.mww(0xCC000008, 0)    # SPI EN = 0
        o.mww(0xCC000010, 0)    # SPI CS = 0
        o.mww(0xCC000014, 2)    # SPI CLK = 2 (DMA mode, breaks XIP)

        # Wait for in-flight DMA/XIP to settle
        time.sleep(0.2)

        # Load driver
        print("\nLoading driver...")
        o.load_image(str(DRIVER_BIN), DRIVER_CODE)
        for i in range(8):
            o.mww(MB + i * 4, 0)
        o.cmd("arm mcr 15 0 7 5 0 0")
        o.cmd("arm mcr 15 0 7 6 0 0")
        o.cmd(f"reg pc {DRIVER_CODE:#x}")
        o.cmd("reg cpsr 0xd3")
        o.resume()
        time.sleep(0.5)
        o.halt()
        assert o.mdw(MB) == ST_IDLE, "Driver failed to start"
        print("  Driver OK")
        o.resume()

        # JEDEC ID
        print("JEDEC ID check...")
        o.halt()
        send_cmd(o, 7)
        o.resume()
        time.sleep(2)
        o.halt()
        jedec = o.mdw(MB + F_LAST_SR)
        status = o.mdw(MB + F_STATUS)
        o.resume()
        print(f"  JEDEC: {jedec:#08x} (expect 0xBF2541)")
        if jedec != 0xBF2541:
            print("  ERROR: Wrong chip! Aborting.")
            return

        # Process in batches
        total_ok = 0
        num_batches = (len(sectors) + MAX_BATCH - 1) // MAX_BATCH

        for batch_idx in range(num_batches):
            batch_start = batch_idx * MAX_BATCH
            batch = sectors[batch_start:batch_start + MAX_BATCH]
            print(f"\n=== Batch {batch_idx+1}/{num_batches}: {len(batch)} sectors ===")

            # Pack data
            image_data = bytearray()
            sector_list = bytearray()
            for addr in batch:
                offset = len(image_data)
                image_data.extend(spi[addr:addr + SECTOR_SIZE])
                sector_list.extend(struct.pack("<II", addr, offset))
            sector_list.extend(struct.pack("<II", 0xFFFFFFFF, 0))

            # Write to temp files
            list_tmp = "/tmp/jtag_slist.bin"
            img_tmp = "/tmp/jtag_sdata.bin"
            with open(list_tmp, "wb") as f:
                f.write(sector_list)
            with open(img_tmp, "wb") as f:
                f.write(image_data)

            # Load to SRAM — halt CPU and disable XIP first
            o.halt()
            o.mww(0xCC000008, 0)    # SPI EN = 0 (break XIP)
            o.mww(0xCC000014, 2)    # SPI CLK = 2 (DMA mode)
            time.sleep(0.1)
            t_load = time.monotonic()
            print(f"  Loading {len(image_data)//1024} KB...")
            o.load_image(list_tmp, SECTOR_LIST)
            o.load_image(img_tmp, IMAGE_BASE)
            dt_load = time.monotonic() - t_load
            print(f"  Loaded in {dt_load:.1f}s")

            # Verify SRAM data integrity (spot-check first sector in batch)
            first_sector_data = spi[batch[0]:batch[0] + 16]
            for i in range(0, 16, 4):
                expected = struct.unpack_from("<I", first_sector_data, i)[0]
                actual = o.mdw(IMAGE_BASE + i)
                if actual != expected:
                    print(f"  SRAM CORRUPTION at IMAGE_BASE+{i:#x}: "
                          f"got {actual:#010x} expected {expected:#010x}")
                    print(f"  Aborting — SRAM is being overwritten by DMA")
                    return
            print(f"  SRAM verified OK")

            # Send FLASH_ALL
            send_cmd(o, 6)
            o.resume()

            # Poll with 3s interval, 20 min timeout per batch
            t_batch = time.monotonic()
            status, errors, progress = poll_done(
                o, f"Batch {batch_idx+1}",
                timeout_s=len(batch) * 15,  # ~15s per sector max
                interval=3.0,
            )

            dt_batch = time.monotonic() - t_batch
            os.unlink(list_tmp)
            os.unlink(img_tmp)

            if status == ST_DONE and errors == 0:
                total_ok += len(batch)
                print(f"  OK: {len(batch)} sectors in {dt_batch:.1f}s "
                      f"({dt_batch/len(batch):.1f}s/sector)")
            else:
                fail = 0
                try:
                    o.halt()
                    fail = o.mdw(MB + F_FAIL_ADDR)
                    o.resume()
                except Exception:
                    pass
                print(f"  FAILED at {fail:#x}, errors={errors:#x}")
                print(f"  Aborting.")
                break

        dt_total = time.monotonic() - t0
        print(f"\n{'='*60}")
        print(f"{'SUCCESS' if total_ok == len(sectors) else 'PARTIAL'}: "
              f"{total_ok}/{len(sectors)} sectors")
        print(f"Total time: {dt_total:.1f}s")
        print(f"{'='*60}")

        if total_ok == len(sectors) and args.reboot:
            print("\nSoft reboot...")
            o.halt()
            # USB hw reset
            o.mww(0x90000014, 0xFFFFFFFF)
            o.mww(0x90000008, 2)  # RST
            time.sleep(0.01)
            # SPI to XIP
            o.mww(0xCC000008, 1)
            o.mww(0xCC000010, 2)
            o.mww(0xCC000014, 0x38)
            o.cmd("arm mcr 15 0 7 5 0 0")
            o.cmd("arm mcr 15 0 7 6 0 0")
            o.cmd("reg pc 0x4F000")
            o.cmd("reg cpsr 0xd3")
            o.resume()
            print("Reboot triggered — check UART")
        elif total_ok == len(sectors):
            print("Power cycle the device now.")

    finally:
        o.close()


if __name__ == "__main__":
    main()
