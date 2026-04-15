#!/usr/bin/env python3 -u
"""Fast autonomous JTAG flash writer.

Loads the entire SPI image + sector list to SRAM in one bulk transfer,
then the on-device driver iterates all sectors: erase → write → verify.
Host just polls progress. ~50s for full 122-sector image vs 16 min previously.

Usage:
    python3 firmware/jtag_flash_fast.py
    python3 firmware/jtag_flash_fast.py --ref firmware/blender_spi_patched.bin
    python3 firmware/jtag_flash_fast.py --speed 1000
    python3 firmware/jtag_flash_fast.py --device-verify
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
CMD_FLASH_NO_VERIFY = 9
CMD_READ = 3
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


def send_cmd(o, cmd, spi_addr=0):
    o.halt()
    for i in range(7):
        o.mww(MAILBOX + i * 4, 0)
    o.mww(MAILBOX + MB_SPI_ADDR, spi_addr)
    o.mww(MAILBOX + MB_COMMAND, cmd)
    o.resume()


def wait_cmd(o, timeout=60):
    t0 = time.monotonic()
    while True:
        time.sleep(0.2)
        o.halt()
        st = o.mdw(MAILBOX + MB_STATUS)
        er = o.mdw(MAILBOX + MB_ERRORS)
        o.resume()
        if st == ST_DONE_OK:
            return (True, er)
        if st == ST_ERROR:
            return (False, er)
        if time.monotonic() - t0 > timeout:
            return (False, 0xDEAD)


def main():
    p = argparse.ArgumentParser(description="Fast autonomous JTAG flash writer")
    p.add_argument("--ref", default=str(FIRMWARE_DIR / "blender_spi_flash_restored.bin"))
    p.add_argument("--speed", type=int, default=10000)
    p.add_argument("--reboot", action="store_true",
                   help="Attempt soft reboot after flashing")
    p.add_argument("--ignore-errors", action="store_true",
                   help="Continue on errors instead of aborting")
    p.add_argument("--device-verify", action="store_true",
                   help="Deprecated alias: on-device verify is now default")
    p.add_argument("--no-verify", action="store_true",
                   help="Skip on-device verify (use CMD_FLASH_NO_VERIFY)")
    p.add_argument("--limit", type=int, default=0,
                   help="Limit to first N sectors (0 = all)")
    args = p.parse_args()

    spi = Path(args.ref).read_bytes()

    # Find non-empty sectors
    sectors = []
    for addr in range(0, len(spi), SECTOR_SIZE):
        sec = spi[addr:addr + SECTOR_SIZE]
        non_ff = sum(1 for b in sec if b != 0xFF)
        if non_ff > 0:
            sectors.append((addr, non_ff))

    if args.limit > 0:
        sectors = sectors[:args.limit]
    total_data = len(sectors) * SECTOR_SIZE
    print(f"Reference: {args.ref}")
    print(f"  {len(sectors)} non-empty sectors, {total_data // 1024} KB data")
    print(f"  Speed: {args.speed} kHz")

    # Calculate batch size based on available SRAM
    max_image_bytes = 0x80000 - IMAGE_BASE  # SRAM end - image start
    max_sectors_per_batch = max_image_bytes // SECTOR_SIZE
    num_batches = (len(sectors) + max_sectors_per_batch - 1) // max_sectors_per_batch
    print(f"  SRAM budget: {max_image_bytes // 1024} KB "
          f"→ {max_sectors_per_batch} sectors/batch, {num_batches} batch(es)")

    # Start OpenOCD
    subprocess.run(["pkill", "-x", "openocd"], capture_output=True)
    time.sleep(1)

    o = OpenOCD(speed=args.speed)
    t0 = time.monotonic()
    total_errors = 0
    sectors_done = 0

    try:
        o.halt()

        # Disable USB to prevent its DMA corrupting SRAM during load.
        print("\nDisabling USB...")
        o.mww(0x90000008, 0)           # USB controller stop (RS=0)
        o.mww(0x90000014, 0xFFFFFFFF)  # USB flush all endpoints

        # Load driver once (driver configures PLL internally if needed)
        print("Loading driver...")
        o.load_image(str(DRIVER_BIN), DRIVER_CODE)

        # Invalidate caches, set PC to driver, resume
        for i in range(8):
            o.mww(MAILBOX + i * 4, 0)
        o.cmd("arm mcr 15 0 7 5 0 0")
        o.cmd("arm mcr 15 0 7 6 0 0")
        o.cmd(f"reg pc {DRIVER_CODE:#x}")
        o.cmd("reg cpsr 0xd3")
        o.resume()
        time.sleep(0.3)

        # Verify driver is idle and check detected mode
        o.halt()
        status = o.mdw(MAILBOX + MB_STATUS)
        last_sr = o.mdw(MAILBOX + MB_LAST_SR)
        if status != ST_IDLE:
            print(f"WARNING: driver status={status} after init")
        if last_sr != 0xFF and last_sr != 0:
            print(f"Driver ready (SR=0x{last_sr:02X})")
        elif last_sr == 0:
            print(f"Driver ready (SR=0x00, BP cleared)")
        else:
            print(f"WARNING: RDSR returned 0xFF — flash may not respond")
        recovery_mode = True   # timer-based delays, ~0.5s/sector at any clock
        o.resume()

        # Process in batches
        for batch_idx in range(num_batches):
            batch_start = batch_idx * max_sectors_per_batch
            batch_end = min(batch_start + max_sectors_per_batch, len(sectors))
            batch_sectors = sectors[batch_start:batch_end]
            batch_count = len(batch_sectors)

            if num_batches > 1:
                print(f"\n── Batch {batch_idx + 1}/{num_batches}: "
                      f"sectors {batch_start + 1}-{batch_end} of {len(sectors)} ──")

            # Pack this batch's sector data contiguously
            image_data = bytearray()
            sector_list = bytearray()
            for addr, _ in batch_sectors:
                offset = len(image_data)
                image_data.extend(spi[addr:addr + SECTOR_SIZE])
                sector_list.extend(struct.pack("<II", addr, offset))
            sector_list.extend(struct.pack("<II", 0xFFFFFFFF, 0))

            # Write temp files
            img_tmp = "/tmp/jtag_flash_image.bin"
            list_tmp = "/tmp/jtag_flash_list.bin"
            with open(img_tmp, "wb") as f:
                f.write(image_data)
            with open(list_tmp, "wb") as f:
                f.write(sector_list)

            # Halt driver, load data, resume
            o.halt()

            print(f"Loading sector list ({batch_count} entries)...")
            o.load_image(list_tmp, SECTOR_LIST)

            print(f"Loading image data ({len(image_data) // 1024} KB)...")
            t_load = time.monotonic()
            o.load_image(img_tmp, IMAGE_BASE)
            dt = time.monotonic() - t_load
            rate = len(image_data) / dt / 1024
            print(f"  Loaded in {dt:.1f}s ({rate:.1f} KB/s)")

            os.unlink(img_tmp)
            os.unlink(list_tmp)

            # Clear mailbox and send flash command
            # Default to on-device verify via CMD_FLASH_ALL.
            # Use --no-verify to force flash-only path.
            flash_cmd = CMD_FLASH_NO_VERIFY if args.no_verify else CMD_FLASH_ALL
            o.mww(MAILBOX + MB_STATUS, 0)
            o.mww(MAILBOX + MB_PROGRESS, 0)
            o.mww(MAILBOX + MB_ERRORS, 0)
            o.mww(MAILBOX + MB_TOTAL, 0)
            o.mww(MAILBOX + MB_COMMAND, flash_cmd)
            o.resume()

            # Wait for autonomous flash to complete.
            # IMPORTANT: do NOT halt the CPU during flash — any JTAG halt
            # corrupts in-flight DMA transfers (RDSR polling, verify reads).
            # Wait the full estimated time, then check once.
            t_flash = time.monotonic()
            if recovery_mode:
                secs_per_sector = 0.6   # timer-based ~20µs/pair, ~0.5s/sector + DMA overhead
            else:
                secs_per_sector = 0.15  # DMA TX + fixed delays at 196MHz PLL

            est_seconds = batch_count * secs_per_sector
            print(f"  Flashing {batch_count} sectors (~{est_seconds:.0f}s, "
                  f"{'recovery' if recovery_mode else 'fast'} mode)...")
            time.sleep(est_seconds)

            # Check status — should be done. If not, wait more.
            while True:
                o.halt()
                status = o.mdw(MAILBOX + MB_STATUS)
                progress = o.mdw(MAILBOX + MB_PROGRESS)
                errors = o.mdw(MAILBOX + MB_ERRORS)
                total = o.mdw(MAILBOX + MB_TOTAL)
                last_sr = o.mdw(MAILBOX + MB_LAST_SR)
                o.resume()

                elapsed = time.monotonic() - t_flash
                global_progress = sectors_done + progress

                if status == ST_DONE_OK:
                    print(f"  Done: {progress}/{total} sectors, {errors} errors "
                          f"({elapsed:.0f}s, last_sr=0x{last_sr:x})")
                    break
                if status == ST_ERROR:
                    print(f"  DRIVER ERROR: {progress}/{total} sectors, "
                          f"{errors} errors (last_sr=0x{last_sr:x})")
                    if not args.ignore_errors:
                        total_errors += errors
                        sectors_done += batch_count
                        raise SystemExit(1)
                    break
                if status == ST_IDLE and progress >= total and total > 0:
                    print(f"  Done: {progress}/{total} sectors, {errors} errors "
                          f"({elapsed:.0f}s, last_sr=0x{last_sr:x})")
                    break

                # Not done yet — wait more without halting
                remaining = (total - progress) * secs_per_sector if total > 0 else 30
                wait = max(remaining * 1.5, 10)
                print(f"  [{global_progress}/{len(sectors)}] {progress}/{total} done, "
                      f"waiting {wait:.0f}s more...")
                time.sleep(wait)

            total_errors += errors
            sectors_done += batch_count

            if errors > 0 and not args.ignore_errors:
                print(f"ABORTING: {errors} errors in batch (use --ignore-errors to continue)")
                break

            if not args.no_verify:
                verify_tmp = "/tmp/jtag_flash_verify.bin"
                verify_errors = 0
                print(f"  Host verify: reading back {batch_count} sectors...")
                t_verify = time.monotonic()
                for i, (addr, _) in enumerate(batch_sectors):
                    expected = spi[addr:addr + SECTOR_SIZE]
                    send_cmd(o, CMD_READ, addr)
                    ok, er = wait_cmd(o, timeout=30)
                    if not ok:
                        print(f"    READ FAIL @ 0x{addr:05x} (err=0x{er:x})")
                        verify_errors += 1
                        if not args.ignore_errors:
                            break
                        continue

                    o.halt()
                    o.cmd(
                        f"dump_image {verify_tmp} {READBACK_BUF:#x} {SECTOR_SIZE}",
                        timeout=30,
                    )
                    o.resume()
                    actual = Path(verify_tmp).read_bytes()
                    mismatches = sum(1 for a, b in zip(actual, expected) if a != b)
                    if mismatches > 0:
                        print(f"    MISMATCH @ 0x{addr:05x}: {mismatches} bytes")
                        verify_errors += 1
                        if not args.ignore_errors:
                            break
                    elif (i + 1) % 16 == 0:
                        print(f"    verified {i + 1}/{batch_count}")

                try:
                    os.unlink(verify_tmp)
                except FileNotFoundError:
                    pass

                dt_verify = time.monotonic() - t_verify
                if verify_errors == 0:
                    print(f"  Host verify passed ({dt_verify:.1f}s)")
                else:
                    print(f"  Host verify found {verify_errors} error(s) ({dt_verify:.1f}s)")
                    total_errors += verify_errors
                    if not args.ignore_errors:
                        print("ABORTING: host verification failed")
                        break

            dt_batch = time.monotonic() - t_flash
            if num_batches > 1:
                print(f"  Batch {batch_idx + 1} done: {batch_count} sectors in "
                      f"{dt_batch:.1f}s ({dt_batch/batch_count:.2f}s/sector)")

        dt_total = time.monotonic() - t0

        print(f"\n{'=' * 60}")
        if total_errors == 0:
            print(f"SUCCESS: {sectors_done} sectors, 0 errors")
        else:
            print(f"WARNING: {sectors_done} sectors, {total_errors} ERRORS")
        print(f"Total time: {dt_total:.1f}s ({dt_total/sectors_done:.2f}s/sector)")
        print(f"{'=' * 60}")
        # Restore SPI/DMA to clean state
        print("Restoring SPI/DMA to clean state...")
        o.halt()
        o.mww(MAILBOX + MB_STATUS, 0)
        o.mww(MAILBOX + MB_COMMAND, CMD_SPI_CLEANUP)
        o.resume()
        time.sleep(0.5)

        if total_errors == 0 and args.reboot:
            print("Attempting soft reboot...")
            o.halt()
            # Upload reboot stub to work area and execute it.
            # The stub handles USB disconnect, SPI/DMA teardown, cache
            # invalidation, and jumps to bootloader at 0x4F000.
            reboot_bin = FIRMWARE_DIR / "reboot_stub.bin"
            if not reboot_bin.exists():
                print(f"ERROR: {reboot_bin} not found, skipping reboot")
            else:
                REBOOT_ADDR = 0x2B000
                # Get entry point from ELF (uart_puts may precede reboot_entry)
                reboot_elf = FIRMWARE_DIR / "reboot_stub.elf"
                if reboot_elf.exists():
                    with open(reboot_elf, "rb") as f:
                        f.seek(0x18)
                        entry = struct.unpack("<I", f.read(4))[0]
                else:
                    entry = REBOOT_ADDR
                o.load_image(str(reboot_bin), REBOOT_ADDR)
                o.cmd("arm mcr 15 0 7 5 0 0")  # invalidate I-cache
                o.cmd(f"reg pc {entry:#x}")
                o.cmd("reg cpsr 0xd3")
            o.resume()
            time.sleep(2)  # let reboot stub run before closing OpenOCD
            print("Reboot triggered. Watch UART for TCAT-BOOT.")
        elif total_errors == 0:
            print("Power cycle the device now.")

    finally:
        o.close()


if __name__ == "__main__":
    main()
