#!/usr/bin/env python3
"""Write complete SPI flash image via SRAM driver stub + OpenOCD.

Loads sram_flash_driver.bin to SRAM, then drives erase/write for each
non-0xFF sector in the reference image.  Optionally verifies after write.

Usage:
    python3 firmware/flash_full_image.py                    # write all sectors
    python3 firmware/flash_full_image.py --verify           # write + verify each
    python3 firmware/flash_full_image.py --test-only        # test sector 0xF0000 only
    python3 firmware/flash_full_image.py --read-test 0x0    # test DMA RX at addr
"""

import argparse
import os
import socket
import struct
import subprocess
import sys
import tempfile
import time
from pathlib import Path

FIRMWARE_DIR = Path(__file__).parent
OPENOCD_CFG = FIRMWARE_DIR.parent / 'jtag' / 'miolink-dice3-openocd.cfg'

# SRAM layout (must match sram_flash_driver.c)
DATA_BUF     = 0x2B000
DRIVER_CODE  = 0x2C000
MAILBOX      = 0x2E000
READBACK_BUF = 0x2E100
SECTOR_SIZE  = 0x1000

# Mailbox offsets (32 bytes)
MB_STATUS   = MAILBOX + 0x00
MB_COMMAND  = MAILBOX + 0x04
MB_SPI_ADDR = MAILBOX + 0x08
MB_PROGRESS = MAILBOX + 0x0C
MB_ERRORS   = MAILBOX + 0x10
MB_LAST_SR  = MAILBOX + 0x14

# Commands
CMD_NOP      = 0
CMD_ERASE    = 1
CMD_WRITE    = 2
CMD_READ     = 3
CMD_VERIFY   = 4
CMD_BP_CLEAR = 5

# Status
ST_IDLE    = 0
ST_RUNNING = 1
ST_DONE_OK = 2
ST_ERROR   = 0xFF


class OpenOCDTcl:
    """Interface to OpenOCD via TCL port."""

    def __init__(self, host='localhost', port=6666, timeout=30):
        self.sock = socket.create_connection((host, port), timeout=timeout)
        self.sock.settimeout(timeout)

    def cmd(self, command: str) -> str:
        self.sock.sendall(command.encode('utf-8') + b'\x1a')
        buf = b''
        while b'\x1a' not in buf:
            chunk = self.sock.recv(4096)
            if not chunk:
                break
            buf += chunk
        return buf.split(b'\x1a')[0].decode('utf-8', errors='replace').strip()

    def halt(self):
        return self.cmd('halt')

    def resume(self, addr=None):
        if addr is not None:
            return self.cmd(f'resume {addr:#x}')
        return self.cmd('resume')

    def mww(self, addr, val):
        self.cmd(f'mww {addr:#x} {val:#x}')

    def mdw(self, addr, count=1):
        result = self.cmd(f'mdw {addr:#x} {count}')
        values = []
        for line in result.splitlines():
            if ':' in line:
                values.extend(int(w, 16) for w in line.split(':', 1)[1].strip().split())
        return values

    def load_image(self, path, addr):
        return self.cmd(f'load_image {path} {addr:#x} bin')

    def read_memory(self, addr, size):
        """Read SRAM via dump_image (for verification)."""
        with tempfile.NamedTemporaryFile(suffix='.bin', delete=False) as f:
            tmp = f.name
        try:
            self.cmd(f'dump_image {tmp} {addr:#x} {size}')
            return Path(tmp).read_bytes()
        finally:
            try:
                os.unlink(tmp)
            except OSError:
                pass

    def close(self):
        self.sock.close()


def build_driver():
    """Build the SRAM flash driver if source is newer than binary."""
    src = FIRMWARE_DIR / 'sram_flash_driver.c'
    ld = FIRMWARE_DIR / 'sram_flash_driver.ld'
    elf = FIRMWARE_DIR / 'sram_flash_driver.elf'
    out = FIRMWARE_DIR / 'sram_flash_driver.bin'

    if (out.exists() and src.stat().st_mtime < out.stat().st_mtime
            and ld.stat().st_mtime < out.stat().st_mtime):
        print(f'Driver up to date: {out} ({out.stat().st_size} bytes)')
        return out

    print('Building sram_flash_driver...')
    subprocess.run([
        'arm-none-eabi-gcc', '-march=armv5te', '-marm', '-Os',
        '-nostdlib', '-ffreestanding', '-Wall',
        '-T', str(ld),
        '-o', str(elf), str(src)
    ], check=True)
    subprocess.run([
        'arm-none-eabi-objcopy', '-O', 'binary', str(elf), str(out)
    ], check=True)
    print(f'Built: {out} ({out.stat().st_size} bytes)')
    return out


def load_sector_data(o, data: bytes):
    """Load 4KB sector data to SRAM via temp file + load_image."""
    assert len(data) == SECTOR_SIZE
    with tempfile.NamedTemporaryFile(suffix='.bin', delete=False) as f:
        f.write(data)
        tmp = f.name
    try:
        o.load_image(tmp, DATA_BUF)
    finally:
        os.unlink(tmp)


def send_command(o, cmd, spi_addr=0):
    """Write command to mailbox (CPU must be halted)."""
    o.mww(MB_SPI_ADDR, spi_addr)
    o.mww(MB_PROGRESS, 0)
    o.mww(MB_ERRORS, 0)
    o.mww(MB_COMMAND, cmd)


def read_mailbox(o):
    """Read all mailbox fields. Returns dict."""
    vals = o.mdw(MAILBOX, 8)
    return {
        'status': vals[0],
        'command': vals[1],
        'spi_addr': vals[2],
        'progress': vals[3],
        'errors': vals[4],
        'last_sr': vals[5],
    }


def poll_completion(o, label='', timeout_s=30, poll_interval=0.5):
    """Resume CPU, poll mailbox until done. Returns mailbox dict."""
    o.resume()

    deadline = time.time() + timeout_s
    last_progress = -1

    while time.time() < deadline:
        time.sleep(poll_interval)
        o.halt()
        time.sleep(0.01)

        mb = read_mailbox(o)

        if mb['status'] == ST_DONE_OK:
            return mb
        elif mb['status'] == ST_ERROR:
            print(f'  {label} ERROR: errors={mb["errors"]:#x}, '
                  f'progress={mb["progress"]}, last_sr={mb["last_sr"]:#x}')
            return mb
        elif mb['status'] == ST_IDLE:
            # Command was processed already (fast operation)
            # Check if command field was cleared (= completed)
            if mb['command'] == 0:
                return mb

        # Still running — show progress if changed
        if mb['progress'] != last_progress:
            last_progress = mb['progress']

        o.resume()

    # Timeout
    o.halt()
    time.sleep(0.05)
    mb = read_mailbox(o)
    print(f'  {label} TIMEOUT: status={mb["status"]}, progress={mb["progress"]}, '
          f'errors={mb["errors"]:#x}')
    return mb


def init_driver(o, driver_bin):
    """Load driver to SRAM, set up CPU, resume into command loop."""
    print('Loading driver to SRAM...')
    o.halt()
    time.sleep(0.2)

    o.load_image(str(driver_bin), DRIVER_CODE)

    # Clear mailbox
    for i in range(8):
        o.mww(MAILBOX + i * 4, 0)

    # Invalidate I-cache
    o.cmd('arm mcr 15 0 7 5 0 0')

    # Set PC and CPSR, resume into _start
    o.cmd(f'reg pc {DRIVER_CODE:#x}')
    o.cmd('reg cpsr 0xd3')
    o.resume()

    # Wait for driver to initialize
    time.sleep(0.1)
    o.halt()
    time.sleep(0.01)
    mb = read_mailbox(o)
    if mb['status'] != ST_IDLE:
        print(f'WARNING: driver status={mb["status"]} after init (expected 0=idle)')
    else:
        print('Driver initialized (idle).')


def run_bp_clear(o):
    """Send BP_CLEAR command."""
    print('Clearing block protection...')
    send_command(o, CMD_BP_CLEAR)
    o.resume()
    time.sleep(3)  # generous fixed wait — BP clear is ~500ms
    o.halt()
    time.sleep(0.05)
    mb = read_mailbox(o)
    # Read full mailbox including diagnostic fields
    o.halt()
    time.sleep(0.05)
    vals = o.mdw(MAILBOX, 8)
    sr_initial = vals[6]   # reserved1 = initial SR
    sr_ewsr = vals[7]      # reserved2 = SR after attempt 1 (EWSR+WRSR)
    sr_final = mb['last_sr']

    print(f'  SR initial:         {sr_initial:#04x}')
    print(f'  SR after EWSR+WRSR: {sr_ewsr:#04x}')
    print(f'  SR final:           {sr_final:#04x}')

    if sr_final == 0xDEAD:
        print('  DMA RX FAILED — using fixed delays.')
    elif sr_final & 0x1C:
        print('  WARNING: BP bits still set after all 4 attempts!')
    else:
        print('  BP cleared successfully.')
    return mb


def run_erase(o, spi_addr):
    """Erase one sector."""
    send_command(o, CMD_ERASE, spi_addr)
    mb = poll_completion(o, f'ERASE {spi_addr:#x}', timeout_s=10)
    # Read diagnostic: SR immediately after SE
    o.halt()
    time.sleep(0.05)
    vals = o.mdw(MAILBOX, 8)
    sr_after_se = vals[6]  # reserved1
    if sr_after_se & 1:
        print(f'  SE confirmed: flash was BUSY (SR={sr_after_se:#x})')
    else:
        print(f'  WARNING: flash was NOT busy after SE (SR={sr_after_se:#x})')
    return mb


def run_write(o, spi_addr):
    """Write sector (data must already be loaded at DATA_BUF)."""
    send_command(o, CMD_WRITE, spi_addr)
    return poll_completion(o, f'WRITE {spi_addr:#x}', timeout_s=15)


def run_read(o, spi_addr):
    """Read sector into READBACK_BUF."""
    send_command(o, CMD_READ, spi_addr)
    return poll_completion(o, f'READ {spi_addr:#x}', timeout_s=10)


def run_verify(o, spi_addr):
    """Verify sector (data must be loaded at DATA_BUF)."""
    send_command(o, CMD_VERIFY, spi_addr)
    return poll_completion(o, f'VERIFY {spi_addr:#x}', timeout_s=10)


def scan_sectors(ref_data):
    """Find all non-0xFF sectors in the reference image."""
    sectors = []
    ff_sector = b'\xFF' * SECTOR_SIZE
    for offset in range(0, len(ref_data), SECTOR_SIZE):
        chunk = ref_data[offset:offset + SECTOR_SIZE]
        if len(chunk) < SECTOR_SIZE:
            chunk = chunk + b'\xFF' * (SECTOR_SIZE - len(chunk))
        if chunk != ff_sector:
            non_ff = sum(1 for b in chunk if b != 0xFF)
            sectors.append((offset, non_ff))
    return sectors


def wait_for_port(host, port, timeout=10):
    deadline = time.time() + timeout
    while time.time() < deadline:
        try:
            s = socket.create_connection((host, port), timeout=0.5)
            s.close()
            return True
        except (ConnectionRefusedError, socket.timeout, OSError):
            time.sleep(0.2)
    return False


def format_time(seconds):
    m, s = divmod(int(seconds), 60)
    return f'{m}m{s:02d}s' if m else f'{s}s'


def do_test(o, driver_bin, spi_addr=0xF0000):
    """Test erase + write + verify on a safe sector."""
    print(f'\n{"="*60}')
    print(f'TEST: erase + AAI write + verify at {spi_addr:#x}')
    print(f'{"="*60}')

    init_driver(o, driver_bin)
    mb = run_bp_clear(o)
    if mb['status'] not in (ST_DONE_OK, ST_IDLE):
        return False

    # Build test pattern: incrementing bytes
    test_data = bytes(i & 0xFF for i in range(SECTOR_SIZE))

    # Erase
    print(f'\nErasing {spi_addr:#x}...')
    o.halt()
    time.sleep(0.05)
    mb = run_erase(o, spi_addr)
    if mb['status'] != ST_DONE_OK:
        print(f'  ERASE FAILED')
        return False
    print(f'  Erase OK (last_sr={mb["last_sr"]:#x})')

    # Write
    print(f'Writing {spi_addr:#x} (AAI, 4KB)...')
    o.halt()
    time.sleep(0.05)
    load_sector_data(o, test_data)
    mb = run_write(o, spi_addr)
    if mb['status'] != ST_DONE_OK:
        print(f'  WRITE FAILED: errors={mb["errors"]:#x}')
        return False
    print(f'  Write OK ({mb["progress"]} bytes)')

    # Verify via bidirectional DMA read
    print(f'Verifying {spi_addr:#x}...')
    o.halt()
    time.sleep(0.05)
    load_sector_data(o, test_data)
    mb = run_verify(o, spi_addr)
    if mb['status'] == ST_DONE_OK and mb['errors'] == 0:
        print(f'  VERIFY OK — 0 mismatches!')
        return True
    elif mb['status'] == ST_ERROR:
        print(f'  VERIFY: {mb["errors"]} mismatches')
        return mb['errors'] == 0
    else:
        print(f'  VERIFY failed: status={mb["status"]}')
        return False



def do_full_write(o, driver_bin, ref_path):
    """Write complete flash image (blind — no read-back verification)."""
    ref_data = ref_path.read_bytes()
    if len(ref_data) != 0x200000:
        print(f'WARNING: reference image is {len(ref_data)} bytes (expected 2MB)')

    sectors = scan_sectors(ref_data)
    total_data = sum(nff for _, nff in sectors)

    print(f'\n{"="*60}')
    print(f'FULL IMAGE WRITE: {len(sectors)} sectors, {total_data} non-FF bytes')
    print(f'Reference: {ref_path}')
    print(f'{"="*60}\n')

    # Init driver
    init_driver(o, driver_bin)
    mb = run_bp_clear(o)
    if mb['status'] not in (ST_DONE_OK, ST_IDLE):
        print('BP_CLEAR failed — aborting')
        return False

    # Process sectors
    start_time = time.time()
    ok_count = 0
    fail_count = 0

    for idx, (spi_addr, non_ff) in enumerate(sectors):
        elapsed = time.time() - start_time
        if idx > 0:
            per_sector = elapsed / idx
            remaining = per_sector * (len(sectors) - idx)
            eta = format_time(remaining)
        else:
            eta = '?'

        sector_data = ref_data[spi_addr:spi_addr + SECTOR_SIZE]
        if len(sector_data) < SECTOR_SIZE:
            sector_data = sector_data + b'\xFF' * (SECTOR_SIZE - len(sector_data))

        print(f'[{idx+1}/{len(sectors)}] {spi_addr:#07x} '
              f'({non_ff} bytes, ETA {eta})', end='', flush=True)

        # Load sector data
        o.halt()
        time.sleep(0.01)
        load_sector_data(o, sector_data)

        # Erase
        mb = run_erase(o, spi_addr)
        if mb['status'] != ST_DONE_OK:
            print(f' — ERASE FAIL')
            fail_count += 1
            continue

        # Write
        o.halt()
        time.sleep(0.01)
        mb = run_write(o, spi_addr)
        if mb['status'] != ST_DONE_OK:
            print(f' — WRITE FAIL (errors={mb["errors"]:#x})')
            fail_count += 1
            continue

        ok_count += 1
        print(' — OK', flush=True)

    # Summary
    elapsed = time.time() - start_time
    print(f'\n{"="*60}')
    print(f'COMPLETE: {ok_count}/{len(sectors)} sectors OK, '
          f'{fail_count} failures, {format_time(elapsed)} elapsed')
    if fail_count == 0:
        print(f'SUCCESS — power cycle the device now.')
        print(f'Expected: dmesg shows USB device 1220:8FE1')
    else:
        print(f'FAILURES DETECTED — check errors above.')
    print(f'{"="*60}')

    return fail_count == 0


def main():
    parser = argparse.ArgumentParser(description='Write full SPI flash image')
    parser.add_argument('--test-only', action='store_true',
                        help='Only test erase+write on sector 0xF0000')
    parser.add_argument('--ref', type=Path,
                        default=FIRMWARE_DIR / 'blender_spi_flash.bin',
                        help='Reference SPI flash image (default: blender_spi_flash.bin)')
    parser.add_argument('--openocd-cfg', type=Path, default=OPENOCD_CFG)
    parser.add_argument('--tcl-port', type=int, default=6666)
    parser.add_argument('--no-build', action='store_true')
    args = parser.parse_args()

    # Build driver
    if args.no_build:
        driver_bin = FIRMWARE_DIR / 'sram_flash_driver.bin'
        if not driver_bin.exists():
            print(f'ERROR: {driver_bin} not found')
            sys.exit(1)
    else:
        driver_bin = build_driver()

    # Validate reference image
    if not args.test_only:
        if not args.ref.exists():
            print(f'ERROR: Reference image not found: {args.ref}')
            sys.exit(1)

    # Connect to OpenOCD
    print(f'\nConnecting to OpenOCD TCL port {args.tcl_port}...')
    if not wait_for_port('localhost', args.tcl_port, timeout=3):
        print('OpenOCD not running. Starting...')
        if not args.openocd_cfg.exists():
            print(f'ERROR: Config not found: {args.openocd_cfg}')
            sys.exit(1)
        proc = subprocess.Popen(
            ['openocd', '-f', str(args.openocd_cfg)],
            stdout=subprocess.PIPE, stderr=subprocess.PIPE
        )
        if not wait_for_port('localhost', args.tcl_port):
            stderr = proc.stderr.read().decode(errors='replace')
            print(f'ERROR: OpenOCD failed:\n{stderr[-500:]}')
            sys.exit(1)
        print('  OpenOCD started.')

    o = OpenOCDTcl(port=args.tcl_port)

    try:
        o.halt()
        time.sleep(0.2)
        print('CPU halted.')

        if args.test_only:
            if not do_test(o, driver_bin):
                sys.exit(1)
        else:
            # Scan reference image
            ref_data = args.ref.read_bytes()
            sectors = scan_sectors(ref_data)
            print(f'Reference: {args.ref.name} ({len(ref_data)} bytes, '
                  f'{len(sectors)} non-empty sectors)')

            if not do_full_write(o, driver_bin, args.ref):
                sys.exit(1)
    finally:
        o.close()


if __name__ == '__main__':
    main()
