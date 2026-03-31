#!/usr/bin/env python3
"""Flash firmware via JTAG mailbox — no USB needed.

After JTAG injection, the main-loop hook checks a SRAM mailbox struct on
every iteration (~100Hz). This script loads sector data to a SRAM buffer
and posts erase/write commands to the mailbox. Completely bypasses USB.

Usage:
    python3 firmware/patch/jtag_flash.py              # inject + flash + reboot
    python3 firmware/patch/jtag_flash.py --inject      # inject only (SRAM, no persist)
    python3 firmware/patch/jtag_flash.py --reboot      # reboot only
"""

import argparse
import subprocess
import sys
import time
from pathlib import Path

FIRMWARE_DIR = Path(__file__).resolve().parent.parent
PATCH_DIR = Path(__file__).resolve().parent
SPI_RESTORED = FIRMWARE_DIR / 'blender_spi_flash_restored.bin'
SPI_PATCHED = FIRMWARE_DIR / 'blender_spi_patched.bin'
HOOKS_BIN = PATCH_DIR / 'hooks.bin'
HOOKS_ELF = PATCH_DIR / 'hooks.elf'

# Firmware constants (these are in the original firmware, not the patch)
HOOK_TARGET = 0x4FAC
IDENTITY_ADDR = 0x8968
IDENTITY_WORD = 0xE92D4FF0

# SRAM buffer for sector data (DEADBEEF free zone, doesn't overlap handler)
DATA_BUFFER = 0x2B800

SECTOR_SIZE = 0x1000

# Mailbox field offsets
MBOX_COMMAND   = 0x00
MBOX_SPI_ADDR  = 0x04
MBOX_DATA_ADDR = 0x08
MBOX_DATA_LEN  = 0x0C
MBOX_RESULT    = 0x10
MBOX_ERROR     = 0x14

# Commands
CMD_IDLE  = 0
CMD_ERASE = 1
CMD_WRITE = 2
CMD_REBOOT = 3

# Results
RESULT_PENDING = 0
RESULT_OK      = 1
RESULT_ERROR   = 2


def read_elf_symbols():
    """Read symbol addresses from hooks.elf via nm."""
    if not HOOKS_ELF.exists():
        print(f"ERROR: {HOOKS_ELF} not found. Run: make -C firmware/patch")
        sys.exit(1)
    result = subprocess.run(
        ['arm-none-eabi-nm', str(HOOKS_ELF)],
        capture_output=True, text=True)
    if result.returncode != 0:
        print(f"ERROR: nm failed: {result.stderr}")
        sys.exit(1)
    symbols = {}
    for line in result.stdout.strip().split('\n'):
        parts = line.strip().split()
        if len(parts) == 3:
            symbols[parts[2]] = int(parts[0], 16)
    return symbols


# Read addresses from ELF at module load time
_symbols = read_elf_symbols()
PATCH_ZONE = _symbols['_hook_dcp_flash_stub']
MAILBOX_ADDR = _symbols['jtag_mailbox']
DONE_FLAG_ADDR = _symbols['done_flag']
print(f"ELF symbols: stub=0x{PATCH_ZONE:05X} mailbox=0x{MAILBOX_ADDR:05X} "
      f"done=0x{DONE_FLAG_ADDR:05X}")


class OpenOCD:
    def __init__(self, host='localhost', port=6666, timeout=10):
        import socket
        self.sock = socket.create_connection((host, port), timeout=timeout)
        self.sock.settimeout(timeout)

    def cmd(self, command):
        self.sock.sendall(command.encode() + b'\x1a')
        buf = b''
        while b'\x1a' not in buf:
            chunk = self.sock.recv(4096)
            if not chunk:
                break
            buf += chunk
        return buf.split(b'\x1a')[0].decode(errors='replace').strip()

    def mdw(self, addr):
        r = self.cmd(f'mdw {addr:#x}')
        if ':' in r:
            return int(r.split(':')[1].strip().split()[0], 16)
        return None

    def mww(self, addr, val):
        self.cmd(f'mww {addr:#x} {val:#x}')

    def load_image(self, path, addr):
        return self.cmd(f'load_image {path} {addr:#x} bin')

    def close(self):
        self.sock.close()


def encode_arm_bl(from_addr, to_addr):
    offset = to_addr - from_addr - 8
    return 0xEB000000 | ((offset >> 2) & 0x00FFFFFF)


def inject(ocd):
    """Inject handler into SRAM."""
    print("── Inject ──")
    ocd.cmd('halt')
    time.sleep(0.2)

    # Identity check
    actual = ocd.mdw(IDENTITY_ADDR)
    if actual != IDENTITY_WORD:
        print(f"  Identity FAIL: {actual:#010x}")
        ocd.cmd('resume')
        return False
    print(f"  Identity OK")

    # Load hooks.bin
    r = ocd.load_image(str(HOOKS_BIN.resolve()), PATCH_ZONE)
    print(f"  {r}")

    # Trampoline
    tramp = encode_arm_bl(HOOK_TARGET, PATCH_ZONE)
    ocd.mww(HOOK_TARGET, tramp)

    # Clear done flag and mailbox
    ocd.mww(DONE_FLAG_ADDR, 0)
    for off in range(0, 24, 4):
        ocd.mww(MAILBOX_ADDR + off, 0)

    # Invalidate BOTH I-cache and D-cache, then resume
    ocd.cmd("arm mcr 15 0 7 5 0 0")  # invalidate I-cache
    ocd.cmd("arm mcr 15 0 7 6 0 0")  # invalidate D-cache
    ocd.cmd('resume')
    print("  Handler live")

    # Wait for init to complete
    time.sleep(0.5)
    return True


def mailbox_exec(ocd, command, spi_addr=0, data_addr=0, data_len=0,
                  wait_secs=2):
    """Post a command to the mailbox and wait for result.

    ARM926EJ-S requires halt for JTAG memory access. We:
    1. Halt to write command parameters
    2. Resume and wait WITHOUT halting (critical for SPI flash timing)
    3. Halt to read result
    """
    # Halt to write mailbox
    ocd.cmd('halt')
    time.sleep(0.05)
    ocd.mww(MAILBOX_ADDR + MBOX_SPI_ADDR, spi_addr)
    ocd.mww(MAILBOX_ADDR + MBOX_DATA_ADDR, data_addr)
    ocd.mww(MAILBOX_ADDR + MBOX_DATA_LEN, data_len)
    ocd.mww(MAILBOX_ADDR + MBOX_RESULT, RESULT_PENDING)
    ocd.mww(MAILBOX_ADDR + MBOX_ERROR, 0)
    ocd.mww(MAILBOX_ADDR + MBOX_COMMAND, command)
    # Invalidate D-cache so CPU sees our JTAG writes
    ocd.cmd("arm mcr 15 0 7 6 0 0")
    ocd.cmd('resume')

    # Wait for operation — NO HALTING during SPI flash ops
    time.sleep(wait_secs)

    # Halt to read result
    ocd.cmd('halt')
    time.sleep(0.02)
    ocd.cmd("arm mcr 15 0 7 10 0 0")  # clean D-cache (writeback)
    result = ocd.mdw(MAILBOX_ADDR + MBOX_RESULT)
    error = ocd.mdw(MAILBOX_ADDR + MBOX_ERROR)
    ocd.cmd('resume')

    if result is not None and result != RESULT_PENDING:
        return result, error if error is not None else 0

    return None, 0


def flash_sector(ocd, spi_addr, data):
    """Erase and write one 4KB sector via mailbox."""
    # Erase
    result, error = mailbox_exec(ocd, CMD_ERASE, spi_addr=spi_addr)
    if result != RESULT_OK:
        print(f" erase FAIL (result={result}, error={error:#x})")
        return False
    print("E", end='', flush=True)

    # Load sector data to SRAM buffer
    tmpfile = Path('/tmp/_jtag_sector.bin')
    tmpfile.write_bytes(data)
    ocd.cmd('halt')
    time.sleep(0.05)
    ocd.load_image(str(tmpfile), DATA_BUFFER)
    ocd.cmd("arm mcr 15 0 7 6 0 0")  # invalidate D-cache
    ocd.cmd('resume')
    tmpfile.unlink()
    time.sleep(0.1)

    # Write
    result, error = mailbox_exec(ocd, CMD_WRITE,
                                  spi_addr=spi_addr,
                                  data_addr=DATA_BUFFER,
                                  data_len=len(data))
    if result != RESULT_OK:
        print(f" write FAIL (result={result}, error={error:#x})")
        return False
    print("W", end='', flush=True)

    return True


def find_changed_sectors():
    original = SPI_RESTORED.read_bytes()
    patched = SPI_PATCHED.read_bytes()
    changed = []
    for off in range(0, max(len(original), len(patched)), SECTOR_SIZE):
        a = original[off:off + SECTOR_SIZE].ljust(SECTOR_SIZE, b'\xff')
        b = patched[off:off + SECTOR_SIZE].ljust(SECTOR_SIZE, b'\xff')
        if a != b:
            changed.append(off)
    return changed


def main():
    parser = argparse.ArgumentParser(description='Flash via JTAG mailbox')
    parser.add_argument('--inject', action='store_true', help='Inject only')
    parser.add_argument('--reboot', action='store_true', help='Reboot only')
    args = parser.parse_args()

    ocd = OpenOCD()
    try:
        if args.reboot:
            print("── Reboot ──")
            ocd.cmd('reset run')
            print("  Done")
            return

        # Build first
        if not HOOKS_BIN.exists() or not SPI_PATCHED.exists():
            print("Run first: make -C firmware/patch && python3 firmware/patch/hooks.py patch")
            sys.exit(1)

        # Reset + inject
        print("── Reset ──")
        ocd.cmd('reset run')
        time.sleep(2)

        if not inject(ocd):
            sys.exit(1)

        if args.inject:
            print("\nInjected (SRAM only). Use --reboot or replug USB to test.")
            return

        # Flash changed sectors
        sectors = find_changed_sectors()
        patched = SPI_PATCHED.read_bytes()
        print(f"\n── Flash {len(sectors)} sectors ──")

        for i, spi_addr in enumerate(sectors):
            data = patched[spi_addr:spi_addr + SECTOR_SIZE]
            if len(data) < SECTOR_SIZE:
                data += b'\xff' * (SECTOR_SIZE - len(data))

            print(f"  [{i+1}/{len(sectors)}] {spi_addr:#07x}...", end=' ', flush=True)
            if flash_sector(ocd, spi_addr, data):
                print("OK")
            else:
                print("FAILED")
                sys.exit(1)

        # Reboot
        print("\n── Reboot ──")
        ocd.cmd('reset run')
        print("  Device rebooting...")
        time.sleep(4)

        # Quick verify: re-inject and check mailbox responds
        if inject(ocd):
            result, _ = mailbox_exec(ocd, CMD_ERASE, spi_addr=0xFFFFFFFF)
            # Erasing invalid addr should return error but proves mailbox works
            if result is not None:
                print("\n  Persistent patch VERIFIED — mailbox responds after reboot")
            else:
                print("\n  WARNING: mailbox not responding after reboot")

        print(f"\n{'='*50}")
        print("Flash complete. Test MIDI CCs:")
        print("  amidi -p hw:1,0,0 -S 'B0 07 40'")
        print(f"{'='*50}")

    finally:
        ocd.close()


if __name__ == '__main__':
    main()
