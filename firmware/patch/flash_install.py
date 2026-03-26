#!/usr/bin/env python3
"""Flash the patched firmware to the device via USB DCP.

Prerequisites:
  - DCP flash handler must be active (JTAG inject or already persistent)
  - `blender-ctl usb flash info` must work

Usage:
    python3 firmware/patch/flash_install.py              # install
    python3 firmware/patch/flash_install.py --dry-run    # show plan only
    python3 firmware/patch/flash_install.py --uninstall  # restore original
"""

import argparse
import subprocess
import sys
import tempfile
from pathlib import Path

FIRMWARE_DIR = Path(__file__).resolve().parent.parent
PATCH_DIR = Path(__file__).resolve().parent

RESTORED = FIRMWARE_DIR / 'blender_spi_flash_restored.bin'
PATCHED = FIRMWARE_DIR / 'blender_spi_patched.bin'
SECTOR_SIZE = 0x1000

BLENDER_CTL = FIRMWARE_DIR.parent / 'blender-ctl' / 'target' / 'release' / 'blender-ctl'


def _flash_cmd():
    if BLENDER_CTL.exists():
        return [str(BLENDER_CTL)]
    return ['cargo', 'run', '-q', '--manifest-path',
            str(FIRMWARE_DIR.parent / 'blender-ctl' / 'Cargo.toml'), '--']


def flash_info():
    cmd = _flash_cmd() + ['usb', 'flash', 'info']
    r = subprocess.run(cmd, capture_output=True, text=True, timeout=30)
    if r.returncode != 0:
        print(f"ERROR: flash info failed: {r.stderr.strip()}")
        return False
    print(f"  {r.stdout.strip()}")
    return True


def flash_erase(spi_addr, length):
    cmd = _flash_cmd() + ['usb', 'flash', 'erase', f'{spi_addr:#x}', f'{length:#x}']
    r = subprocess.run(cmd, capture_output=True, text=True, timeout=60)
    if r.returncode != 0:
        raise RuntimeError(f"erase {spi_addr:#x}: {r.stderr.strip()}")


def flash_write(spi_addr, data):
    with tempfile.NamedTemporaryFile(suffix='.bin', delete=False) as f:
        f.write(data)
        tmp = f.name
    try:
        cmd = _flash_cmd() + ['usb', 'flash', 'write', f'{spi_addr:#x}', tmp]
        r = subprocess.run(cmd, capture_output=True, text=True, timeout=120)
        if r.returncode != 0:
            raise RuntimeError(f"write {spi_addr:#x}: {r.stderr.strip()}")
    finally:
        Path(tmp).unlink(missing_ok=True)


def find_changed_sectors(original: bytes, patched: bytes) -> list[int]:
    """Return list of SPI sector addresses that differ."""
    changed = []
    for off in range(0, max(len(original), len(patched)), SECTOR_SIZE):
        a = original[off:off + SECTOR_SIZE]
        b = patched[off:off + SECTOR_SIZE]
        if len(b) < SECTOR_SIZE:
            b = b + b'\xff' * (SECTOR_SIZE - len(b))
        if len(a) < SECTOR_SIZE:
            a = a + b'\xff' * (SECTOR_SIZE - len(a))
        if a != b:
            changed.append(off)
    return changed


def install(dry_run=False):
    if not PATCHED.exists():
        print(f"ERROR: {PATCHED} not found. Run: make -C firmware/patch && python3 firmware/patch/hooks.py patch")
        sys.exit(1)

    original = RESTORED.read_bytes()
    patched = PATCHED.read_bytes()
    sectors = find_changed_sectors(original, patched)

    print(f"Install patched firmware ({len(sectors)} sectors to write):")
    for s in sectors:
        print(f"  SPI 0x{s:06X}")

    if dry_run:
        print("\n(dry-run — no changes made)")
        return

    print("\nChecking flash handler...")
    if not flash_info():
        print("\nFlash handler not active. Inject first:")
        print("  python3 firmware/inject_flash_handler.py")
        sys.exit(1)

    resp = input("\nProceed with flash? [y/N] ")
    if resp.lower() != 'y':
        print("Aborted.")
        sys.exit(0)

    # DICE3 USB bug: erase+write in one USB session stalls EP0.
    # Work around by doing each operation in its own blender-ctl process.
    import time

    print("\nPhase 1: Erase sectors")
    for i, s in enumerate(sectors):
        print(f"  [{i+1}/{len(sectors)}] Erase SPI 0x{s:06X}...", end=' ', flush=True)
        flash_erase(s, SECTOR_SIZE)
        print("OK")
        time.sleep(0.3)

    print("\nPhase 2: Write sectors")
    for i, s in enumerate(sectors):
        data = patched[s:s + SECTOR_SIZE]
        if len(data) < SECTOR_SIZE:
            data = data + b'\xff' * (SECTOR_SIZE - len(data))
        print(f"  [{i+1}/{len(sectors)}] Write SPI 0x{s:06X} ({len(data)} bytes)...", end=' ', flush=True)
        flash_write(s, data)
        print("OK")
        time.sleep(0.3)

    print(f"\n{'=' * 50}")
    print("SUCCESS — patched firmware written")
    print("Power cycle to activate, then test:")
    print("  blender-ctl usb flash info")
    print(f"{'=' * 50}")


def uninstall(dry_run=False):
    original = RESTORED.read_bytes()
    patched = PATCHED.read_bytes()
    sectors = find_changed_sectors(original, patched)

    print(f"Restore original firmware ({len(sectors)} sectors):")
    for s in sectors:
        print(f"  SPI 0x{s:06X}")

    if dry_run:
        print("\n(dry-run)")
        return

    print("\nChecking flash handler...")
    if not flash_info():
        print("\nFlash handler not active.")
        sys.exit(1)

    resp = input("\nRestore original? [y/N] ")
    if resp.lower() != 'y':
        print("Aborted.")
        sys.exit(0)

    for i, s in enumerate(sectors):
        data = original[s:s + SECTOR_SIZE]
        print(f"[{i+1}/{len(sectors)}] Erase+write SPI 0x{s:06X}...", end=' ', flush=True)
        flash_erase(s, SECTOR_SIZE)
        flash_write(s, data)
        print("OK")

    print(f"\n{'=' * 50}")
    print("Restored. Power cycle to revert.")
    print(f"{'=' * 50}")


def main():
    parser = argparse.ArgumentParser(description='Flash patched firmware via USB')
    parser.add_argument('--dry-run', action='store_true')
    parser.add_argument('--uninstall', action='store_true')
    args = parser.parse_args()

    if args.uninstall:
        uninstall(dry_run=args.dry_run)
    else:
        install(dry_run=args.dry_run)


if __name__ == '__main__':
    main()
