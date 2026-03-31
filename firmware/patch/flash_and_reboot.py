#!/usr/bin/env python3
"""Flash patched firmware and reboot via JTAG + USB.

Reliable flash-reboot-test cycle:
  1. JTAG inject handler (halt → load SRAM → resume)
  2. Replug USB (JTAG halt disrupts USB; device is self-powered)
  3. Flash sectors via blender-ctl USB DCP
  4. Reboot via JTAG nRST

Usage:
    python3 firmware/patch/flash_and_reboot.py          # inject + flash + reboot
    python3 firmware/patch/flash_and_reboot.py --reboot  # just reboot
    python3 firmware/patch/flash_and_reboot.py --inject  # just inject (no flash)
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
OPENOCD_CFG = FIRMWARE_DIR.parent / 'jtag' / 'miolink-dice3-openocd.cfg'

SECTOR_SIZE = 0x1000

# Firmware constants (not patch addresses)
IDENTITY_ADDR = 0x8968
IDENTITY_WORD = 0xE92D4FF0
HOOK_TARGET = 0x4FAC

BLENDER_CTL = FIRMWARE_DIR.parent / 'blender-ctl' / 'target' / 'release' / 'blender-ctl'


def _flash_cmd():
    if BLENDER_CTL.exists():
        return [str(BLENDER_CTL)]
    return ['cargo', 'run', '-q', '--manifest-path',
            str(FIRMWARE_DIR.parent / 'blender-ctl' / 'Cargo.toml'), '--']


def read_elf_symbols():
    """Read symbol addresses from hooks.elf."""
    result = subprocess.run(
        ['arm-none-eabi-nm', str(HOOKS_ELF)],
        capture_output=True, text=True)
    if result.returncode != 0:
        return {}
    symbols = {}
    for line in result.stdout.strip().split('\n'):
        parts = line.strip().split()
        if len(parts) == 3:
            symbols[parts[2]] = int(parts[0], 16)
    return symbols


# ── OpenOCD ──────────────────────────────────────────────────────────────

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
        return buf.split(b'\x1a')[0].decode('utf-8', errors='replace').strip()

    def halt(self):
        return self.cmd('halt')

    def resume(self):
        return self.cmd('resume')

    def reset_run(self):
        return self.cmd('reset run')

    def mdw(self, addr):
        r = self.cmd(f'mdw {addr:#x}')
        if ':' in r:
            return int(r.split(':')[1].strip().split()[0], 16)
        return None

    def mww(self, addr, val):
        return self.cmd(f'mww {addr:#x} {val:#x}')

    def load_image(self, path, addr):
        return self.cmd(f'load_image {path} {addr:#x} bin')

    def invalidate_icache(self):
        self.cmd("arm mcr 15 0 7 5 0 0")

    def close(self):
        self.sock.close()


def ensure_openocd():
    """Start OpenOCD if not running."""
    import socket
    try:
        s = socket.create_connection(('localhost', 6666), timeout=2)
        s.close()
        return True
    except (ConnectionRefusedError, OSError):
        pass

    if not OPENOCD_CFG.exists():
        print(f"ERROR: {OPENOCD_CFG} not found")
        return False

    print("Starting OpenOCD...", end=' ', flush=True)
    subprocess.Popen(
        ['openocd', '-f', str(OPENOCD_CFG)],
        stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL
    )
    for _ in range(20):
        time.sleep(0.5)
        try:
            s = socket.create_connection(('localhost', 6666), timeout=1)
            s.close()
            print("OK")
            return True
        except (ConnectionRefusedError, OSError):
            pass
    print("FAILED")
    return False


# ── JTAG inject ──────────────────────────────────────────────────────────

def encode_arm_bl(from_addr, to_addr):
    offset = to_addr - from_addr - 8
    imm24 = (offset >> 2) & 0x00FFFFFF
    return 0xEB000000 | imm24


def jtag_inject(ocd):
    """Inject handler into SRAM via JTAG. Returns True on success."""
    symbols = read_elf_symbols()
    patch_zone = symbols.get('_hook_dcp_flash_stub')
    done_flag = symbols.get('done_flag')
    mailbox = symbols.get('jtag_mailbox')
    if not patch_zone:
        print("  ERROR: Can't find _hook_dcp_flash_stub in ELF")
        return False

    print(f"\n── JTAG Inject (stub@0x{patch_zone:05X}) ──")

    ocd.halt()
    time.sleep(0.2)

    # Identity check
    actual = ocd.mdw(IDENTITY_ADDR)
    if actual != IDENTITY_WORD:
        print(f"  Identity FAIL: {actual:#010x} != {IDENTITY_WORD:#010x}")
        ocd.resume()
        return False
    print(f"  Identity OK")

    # Load hook binary
    result = ocd.load_image(str(HOOKS_BIN.resolve()), patch_zone)
    print(f"  Load: {result}")

    # Write trampoline
    tramp = encode_arm_bl(HOOK_TARGET, patch_zone)
    ocd.mww(HOOK_TARGET, tramp)
    print(f"  Trampoline: {HOOK_TARGET:#x} → BL {patch_zone:#x} ({tramp:#010x})")

    # Clear done flag and mailbox
    if done_flag:
        ocd.mww(done_flag, 0)
    if mailbox:
        for off in range(0, 24, 4):
            ocd.mww(mailbox + off, 0)

    # I-cache + D-cache invalidate, resume
    ocd.invalidate_icache()
    ocd.cmd("arm mcr 15 0 7 6 0 0")  # D-cache
    ocd.resume()
    print("  Resumed — handler live")
    return True


# ── USB flash via blender-ctl ────────────────────────────────────────────

def flash_info():
    """Check flash handler via blender-ctl."""
    cmd = _flash_cmd() + ['usb', 'flash', 'info']
    r = subprocess.run(cmd, capture_output=True, text=True, timeout=30)
    if r.returncode != 0:
        return False
    print(f"  {r.stdout.strip()}")
    return True


def flash_update():
    """Flash the patched SPI image via blender-ctl."""
    cmd = _flash_cmd() + ['usb', 'flash', 'update', str(SPI_PATCHED)]
    r = subprocess.run(cmd, capture_output=True, text=True, timeout=300)
    print(r.stdout)
    if r.returncode != 0:
        print(f"ERROR: {r.stderr}")
        return False
    return True


# ── Reboot ───────────────────────────────────────────────────────────────

def jtag_reboot(ocd):
    """Reboot device via JTAG nRST."""
    print("\n── Reboot ──")
    ocd.reset_run()
    print("  nRST asserted — device rebooting")


# ── Main ─────────────────────────────────────────────────────────────────

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
    parser = argparse.ArgumentParser(description='Flash + reboot via JTAG')
    parser.add_argument('--reboot', action='store_true', help='Just reboot')
    parser.add_argument('--inject', action='store_true', help='Just JTAG inject')
    args = parser.parse_args()

    if not ensure_openocd():
        sys.exit(1)

    ocd = OpenOCD()
    try:
        if args.reboot:
            jtag_reboot(ocd)
            return

        # Inject handler
        if not jtag_inject(ocd):
            sys.exit(1)

        if args.inject:
            print("\nHandler injected (SRAM only, not persistent).")
            print("Replug USB, then test: blender-ctl usb flash info")
            return

        # After inject, USB is disrupted. Prompt for replug.
        print("\n── USB Recovery ──")
        print("  JTAG halt disrupted USB. Please replug the USB cable.")
        input("  Press Enter when USB is replugged...")

        # Verify flash handler
        print("\nChecking flash handler...")
        if not flash_info():
            print("Flash handler not responding after replug.")
            sys.exit(1)

        # Show what will change
        sectors = find_changed_sectors()
        print(f"\nSectors to flash: {[f'{s:#07x}' for s in sectors]}")

        resp = input("Proceed? [y/N] ")
        if resp.lower() != 'y':
            print("Aborted.")
            return

        # Flash via blender-ctl
        if not flash_update():
            print("Flash FAILED.")
            sys.exit(1)

        # Reboot to activate persistent patch
        jtag_reboot(ocd)

        print("\n" + "=" * 50)
        print("Done. Power cycle to test persistent patch.")
        print("  blender-ctl usb flash info")
        print("  amidi -p hw:1,0,0 -S 'B0 07 40'")
        print("=" * 50)

    finally:
        ocd.close()


if __name__ == '__main__':
    main()
