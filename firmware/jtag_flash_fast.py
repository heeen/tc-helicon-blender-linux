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
SR_TRACE_IDX = 0x2E07C
SR_TRACE_BUF = 0x2E080
SECTOR_LIST = 0x2F100
IMAGE_BASE = 0x2F900
SAFE_IMAGE_END = 0x50000  # empirically reliable upper bound outside TCAT-BOOT

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


def decode_last_sr(last_sr, detail=0):
    tag = last_sr & 0xF0000000
    addr = last_sr & 0x0FFFFFFF
    if tag == 0xA0000000:
        return f"erase fail @ 0x{addr:05x} (detail=0x{detail:x})"
    if tag == 0xB0000000:
        return f"write fail @ 0x{addr:05x} (detail=0x{detail:x})"
    if tag == 0xC0000000:
        idx = (last_sr >> 16) & 0x0FFF
        exp = (last_sr >> 8) & 0xFF
        act = last_sr & 0xFF
        return f"verify mismatch first@+0x{idx:03x} exp=0x{exp:02x} act=0x{act:02x}"
    if tag == 0xD0000000:
        return f"verify read fail @ 0x{addr:05x}"
    if tag == 0xE0000000:
        idx = (last_sr >> 16) & 0x0FFF
        exp = (last_sr >> 8) & 0xFF
        act = last_sr & 0xFF
        return f"write self-check fail +0x{idx:03x} exp=0x{exp:02x} act=0x{act:02x}"
    return f"last_sr=0x{last_sr:x}"


def dump_sr_trace(o):
    evt_names = {
        0x01: "bp_clear_pre",
        0x02: "bp_clear_post",
        0x10: "erase_wren",
        0x11: "erase_done",
        0x20: "write_wren",
        0x21: "write_first",
        0x22: "write_mid",
        0x23: "write_wrdi",
        0x24: "write_done",
        0x25: "write_pair_sr",
        0x26: "write_wel_lost",
    }
    tmp_idx = "/tmp/jtag_sr_trace_idx.bin"
    tmp_buf = "/tmp/jtag_sr_trace_buf.bin"
    try:
        o.halt()
        o.cmd(f"dump_image {tmp_idx} {SR_TRACE_IDX:#x} 4", timeout=30)
        o.cmd(f"dump_image {tmp_buf} {SR_TRACE_BUF:#x} {32*4}", timeout=30)
        o.resume()
        idx = int.from_bytes(Path(tmp_idx).read_bytes(), "little") & 0x1F
        raw = Path(tmp_buf).read_bytes()
    finally:
        for p in (tmp_idx, tmp_buf):
            try:
                os.unlink(p)
            except FileNotFoundError:
                pass
    words = [int.from_bytes(raw[i:i+4], "little") for i in range(0, len(raw), 4)]
    ordered = words[idx:] + words[:idx]
    rendered = []
    for w in ordered:
        if w == 0:
            continue
        evt = (w >> 24) & 0xFF
        sr = w & 0xFF
        rendered.append(f"{evt_names.get(evt, hex(evt))}:sr=0x{sr:02x}")
    return rendered[-8:] if rendered else []


def dump_buf_signature(o, addr, size=32):
    """Dump first bytes of an SRAM buffer for diagnostics."""
    tmp = f"/tmp/jtag_sig_{addr:x}.bin"
    halted = False
    try:
        o.halt()
        halted = True
        o.cmd(f"dump_image {tmp} {addr:#x} {size}", timeout=30)
        p = Path(tmp)
        if not p.exists():
            return "<dump failed: file not created>"
        data = p.read_bytes()
        if not data:
            return "<dump failed: empty>"
    finally:
        if halted:
            try:
                o.resume()
            except Exception:
                pass
        try:
            os.unlink(tmp)
        except FileNotFoundError:
            pass
    return " ".join(f"{b:02x}" for b in data)


def validate_sram_upload(o, batch_sectors, image_data, sector_list):
    """Read back SRAM payload/list and verify host->SRAM upload integrity."""
    list_dump = "/tmp/jtag_flash_list_readback.bin"
    image_dump = "/tmp/jtag_flash_image_readback.bin"
    try:
        o.cmd(f"dump_image {list_dump} {SECTOR_LIST:#x} {len(sector_list)}", timeout=30)
        o.cmd(f"dump_image {image_dump} {IMAGE_BASE:#x} {len(image_data)}", timeout=120)
        list_rb = Path(list_dump).read_bytes()
        image_rb = Path(image_dump).read_bytes()
    finally:
        for p in (list_dump, image_dump):
            try:
                os.unlink(p)
            except FileNotFoundError:
                pass

    if list_rb != sector_list:
        for i, (a, b) in enumerate(zip(list_rb, sector_list)):
            if a != b:
                print(f"UPLOAD ERROR: sector list mismatch @+0x{i:x}: got=0x{a:02x} exp=0x{b:02x}")
                return False
        if len(list_rb) != len(sector_list):
            print(f"UPLOAD ERROR: sector list size mismatch {len(list_rb)} != {len(sector_list)}")
            return False

    if image_rb != image_data:
        for i, (a, b) in enumerate(zip(image_rb, image_data)):
            if a != b:
                print(f"UPLOAD ERROR: image mismatch @+0x{i:x}: got=0x{a:02x} exp=0x{b:02x}")
                break
        # Try to map mismatch back to sector/offset for easier triage.
        mismatch = next((i for i, (a, b) in enumerate(zip(image_rb, image_data)) if a != b), None)
        if mismatch is not None:
            sec_idx = mismatch // SECTOR_SIZE
            off = mismatch % SECTOR_SIZE
            if sec_idx < len(batch_sectors):
                spi_addr = batch_sectors[sec_idx][0]
                print(f"  maps to batch sector {sec_idx} SPI 0x{spi_addr:05x} +0x{off:03x}")
        return False

    print("  Upload validation OK: SRAM sector list + image payload match host data")
    return True


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


def host_reboot_style_teardown(o):
    """Mirror firmware/reboot_common.c before loading SRAM driver: USB reset,
    LED + flash SPI quiesce, full DMA reset, VIC pending clear. Complements
    on-device pre_flash_teardown() when firmware was running."""
    # ChipIdea USB + TCAT extensions (usb_hw_reset)
    o.mww(0x90000014, 0xFFFFFFFF)
    cmd = o.mdw(0x90000008)
    cmd = (cmd & ~1) | 2  # clear RS, assert controller reset
    o.mww(0x90000008, cmd)
    for _ in range(2000):
        if (o.mdw(0x90000008) & 2) == 0:
            break
    o.mww(0x90000014, 0xFFFFFFFF)
    o.mww(0x90000018, 0)
    v = o.mdw(0x90000008)
    v &= ~(1 | (1 << 13) | (1 << 14))
    o.mww(0x90000008, v)
    o.mww(0x90000024, 0)
    o.mww(0x90000028, 0)
    o.mww(0x90000800, 2)
    o.mww(0x90000818, 0xFFFFFFFF)
    o.mww(0x9000081C, 0)
    o.mww(0x90000834, 0)
    o.mww(0x90000810, 0)
    o.mww(0x90000814, 0)
    # spi_ip_block_quiesce(LED_SPI)
    for a in (
        0xCF000010,
        0xCF000008,
        0xCF00002C,
        0xCF00004C,
        0xCF000050,
        0xCF000054,
        0xCF000000,
        0xCF000004,
        0xCF000018,
        0xCF000034,
    ):
        o.mww(a, 0)
    o.mww(0xCB000020, 0)
    o.mww(0xCF000014, 0xFF)
    # spi_ip_block_quiesce(flash SPI)
    for a in (
        0xCC000010,
        0xCC000008,
        0xCC00002C,
        0xCC00004C,
        0xCC000050,
        0xCC000054,
        0xCC000000,
        0xCC000004,
        0xCC000018,
        0xCC000034,
    ):
        o.mww(a, 0)
    # dma_engine_full_reset
    o.mww(0x80000008, 0)
    o.mww(0x80000010, 0x0F)
    for ch in range(4):
        o.mww(0x80000030 + ch * 4, 1)
    for ch in range(4):
        base = 0x80000100 + ch * 0x20
        o.mww(base + 0x10, 0)
        o.mww(base + 0x0C, 0)
        o.mww(base + 0x08, 0)
        o.mww(base + 0x00, 0)
        o.mww(base + 0x04, 0)
    o.mww(0xFFFFF014, 0xFFFFFFFF)
    o.mww(0xFFFFF01C, 0xFFFFFFFF)


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
    p.add_argument("--validate-upload-only", action="store_true",
                   help="Load sector list/image to SRAM and verify readback, then exit")
    p.add_argument("--allow-high-sram", action="store_true",
                   help="Allow image payload above 0x50000 SRAM boundary")
    p.add_argument("--no-host-teardown", action="store_true",
                   help="Skip host-side reboot-style USB/DMA/SPI/VIC teardown "
                   "(driver still runs on-device pre_flash_teardown)")
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
    sram_ceiling = 0x80000
    if not args.allow_high_sram:
        sram_ceiling = min(sram_ceiling, SAFE_IMAGE_END)
    max_image_bytes = sram_ceiling - IMAGE_BASE
    if max_image_bytes < SECTOR_SIZE:
        raise SystemExit("ERROR: SRAM payload window too small for one sector")
    max_sectors_per_batch = max_image_bytes // SECTOR_SIZE
    num_batches = (len(sectors) + max_sectors_per_batch - 1) // max_sectors_per_batch
    print(f"  SRAM budget: {max_image_bytes // 1024} KB "
          f"→ {max_sectors_per_batch} sectors/batch, {num_batches} batch(es)")
    if not args.allow_high_sram:
        print("  Using conservative SRAM ceiling at 0x50000 (override with --allow-high-sram)")

    # Start OpenOCD
    subprocess.run(["pkill", "-x", "openocd"], capture_output=True)
    time.sleep(1)

    o = OpenOCD(speed=args.speed)
    t0 = time.monotonic()
    total_errors = 0
    sectors_done = 0

    try:
        o.halt()

        if not args.no_host_teardown:
            print("\nReboot-style host teardown (USB/DMA/SPI/VIC)...")
            host_reboot_style_teardown(o)
        else:
            print("\nDisabling USB (minimal)...")
            o.mww(0x90000008, 0)
            o.mww(0x90000014, 0xFFFFFFFF)

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

            if args.validate_upload_only:
                print("Validating upload path (host -> SRAM)...")
                if not validate_sram_upload(o, batch_sectors, bytes(image_data), bytes(sector_list)):
                    raise SystemExit(2)
                # Validate only the first batch unless user explicitly narrows with --limit.
                print("Upload-path validation complete; exiting without flash.")
                raise SystemExit(0)

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
            # IMPORTANT: never halt mid-CMD_FLASH_ALL. Add a large guard band
            # before first status check to avoid in-flight DMA/SPI corruption.
            initial_wait = max(est_seconds * 1.8, est_seconds + 10, 20)
            print(f"  Flashing {batch_count} sectors (~{est_seconds:.0f}s, "
                  f"{'recovery' if recovery_mode else 'fast'} mode)...")
            print(f"  Waiting {initial_wait:.0f}s before first status check (halt-safe guard)...")
            time.sleep(initial_wait)

            # Check status — should be done. If not, wait more.
            max_flash_wait = max(initial_wait * 4.0, 180.0)
            while True:
                o.halt()
                status = o.mdw(MAILBOX + MB_STATUS)
                progress = o.mdw(MAILBOX + MB_PROGRESS)
                errors = o.mdw(MAILBOX + MB_ERRORS)
                total = o.mdw(MAILBOX + MB_TOTAL)
                last_sr = o.mdw(MAILBOX + MB_LAST_SR)
                reserved = o.mdw(MAILBOX + 28)
                o.resume()

                elapsed = time.monotonic() - t_flash
                global_progress = sectors_done + progress

                if elapsed > max_flash_wait:
                    print(f"  DRIVER STUCK: status=0x{status:x} progress={progress}/{total} "
                          f"errors={errors} last_sr=0x{last_sr:x} reserved=0x{reserved:x} "
                          f"after {elapsed:.0f}s")
                    tr = dump_sr_trace(o)
                    if tr:
                        print(f"  SR trace: {' | '.join(tr)}")
                    print(f"  Readback[0:32] = {dump_buf_signature(o, READBACK_BUF)}")
                    print(f"  Expected[0:32] = {dump_buf_signature(o, DATA_BUF)}")
                    # Dump CPU registers + SPI/DMA/Timer state to pinpoint hang location.
                    o.halt()
                    try:
                        print("  --- CPU registers ---")
                        print(o.cmd("reg"))
                        print("  --- SPI controller (0xCC000000+0x40) ---")
                        print(o.cmd("mdw 0xCC000000 16"))
                        print("  --- DMA global (0x80000000+) ---")
                        print(o.cmd("mdw 0x80000000 8"))
                        print("  --- DMA ch0 (0x80000100+) ---")
                        print(o.cmd("mdw 0x80000100 8"))
                        print("  --- Timer0 ---")
                        print(o.cmd("mdw 0xC2000000 4"))
                    finally:
                        try:
                            o.resume()
                        except Exception:
                            pass
                    raise SystemExit(124)

                if status == ST_DONE_OK:
                    print(f"  Done: {progress}/{total} sectors, {errors} errors "
                          f"({elapsed:.0f}s, {decode_last_sr(last_sr, reserved)})")
                    if errors > 0:
                        tr = dump_sr_trace(o)
                        if tr:
                            print(f"  SR trace: {' | '.join(tr)}")
                    if ((last_sr & 0xF0000000) in (0xC0000000, 0xE0000000)) and errors > 0:
                        if (last_sr & 0xF0000000) == 0xC0000000:
                            print(f"  Verify readback[0:4] = {reserved & 0xFF:02x} "
                                  f"{(reserved >> 8) & 0xFF:02x} "
                                  f"{(reserved >> 16) & 0xFF:02x} "
                                  f"{(reserved >> 24) & 0xFF:02x}")
                        print(f"  Readback[0:32] = {dump_buf_signature(o, READBACK_BUF)}")
                        print(f"  Expected[0:32] = {dump_buf_signature(o, DATA_BUF)}")
                    break
                if status == ST_ERROR:
                    print(f"  DRIVER ERROR: {progress}/{total} sectors, "
                          f"{errors} errors ({decode_last_sr(last_sr, reserved)})")
                    tr = dump_sr_trace(o)
                    if tr:
                        print(f"  SR trace: {' | '.join(tr)}")
                    if ((last_sr & 0xF0000000) in (0xC0000000, 0xE0000000)) and errors > 0:
                        if (last_sr & 0xF0000000) == 0xC0000000:
                            print(f"  Verify readback[0:4] = {reserved & 0xFF:02x} "
                                  f"{(reserved >> 8) & 0xFF:02x} "
                                  f"{(reserved >> 16) & 0xFF:02x} "
                                  f"{(reserved >> 24) & 0xFF:02x}")
                        print(f"  Readback[0:32] = {dump_buf_signature(o, READBACK_BUF)}")
                        print(f"  Expected[0:32] = {dump_buf_signature(o, DATA_BUF)}")
                    if not args.ignore_errors:
                        total_errors += errors
                        sectors_done += batch_count
                        raise SystemExit(1)
                    break
                if status == ST_IDLE and progress >= total and total > 0:
                    print(f"  Done: {progress}/{total} sectors, {errors} errors "
                          f"({elapsed:.0f}s, {decode_last_sr(last_sr, reserved)})")
                    if errors > 0:
                        tr = dump_sr_trace(o)
                        if tr:
                            print(f"  SR trace: {' | '.join(tr)}")
                    if ((last_sr & 0xF0000000) in (0xC0000000, 0xE0000000)) and errors > 0:
                        if (last_sr & 0xF0000000) == 0xC0000000:
                            print(f"  Verify readback[0:4] = {reserved & 0xFF:02x} "
                                  f"{(reserved >> 8) & 0xFF:02x} "
                                  f"{(reserved >> 16) & 0xFF:02x} "
                                  f"{(reserved >> 24) & 0xFF:02x}")
                        print(f"  Readback[0:32] = {dump_buf_signature(o, READBACK_BUF)}")
                        print(f"  Expected[0:32] = {dump_buf_signature(o, DATA_BUF)}")
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
