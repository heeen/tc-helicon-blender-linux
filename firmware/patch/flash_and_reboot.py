#!/usr/bin/env python3
"""Flash patched firmware and reboot via JTAG.

Reliable flash-reboot-test cycle:
  1. JTAG inject handler (halt → load SRAM → resume)
  2. USB replug (automatic via JTAG reset + re-enumerate)
  3. Flash sectors via USB DCP (one chunk per USB session, retry on failure)
  4. Reboot via JTAG nRST

Usage:
    python3 firmware/patch/flash_and_reboot.py          # inject + flash + reboot
    python3 firmware/patch/flash_and_reboot.py --reboot  # just reboot
    python3 firmware/patch/flash_and_reboot.py --inject  # just inject (no flash)
"""

import argparse
import struct
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
VID, PID = 0x1220, 0x8FE1

# Patch zone and identity from hooks.py
PATCH_ZONE_SRAM = 0x2AA84
IDENTITY_ADDR = 0x8968
IDENTITY_WORD = 0xE92D4FF0
HOOK_TARGET = 0x4FAC


# ── OpenOCD helpers ──────────────────────────────────────────────────────

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
    """Start OpenOCD if not running, return True if available."""
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
    print("\n── JTAG Inject ──")

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
    result = ocd.load_image(str(HOOKS_BIN.resolve()), PATCH_ZONE_SRAM)
    print(f"  Load: {result}")

    # Write trampoline
    tramp = encode_arm_bl(HOOK_TARGET, PATCH_ZONE_SRAM)
    ocd.mww(HOOK_TARGET, tramp)
    print(f"  Trampoline: {HOOK_TARGET:#x} → BL {PATCH_ZONE_SRAM:#x} ({tramp:#010x})")

    # I-cache invalidate + resume
    ocd.invalidate_icache()
    ocd.resume()
    print("  Resumed — handler live")
    return True


# ── USB flash ────────────────────────────────────────────────────────────

def usb_find():
    """Find Blender USB device."""
    import usb.core
    return usb.core.find(idVendor=VID, idProduct=PID)


def usb_reset_and_wait(max_wait=10):
    """Reset USB device and wait for re-enumeration."""
    dev = usb_find()
    if dev:
        try:
            dev.reset()
        except Exception:
            pass
    for _ in range(max_wait * 2):
        time.sleep(0.5)
        if usb_find():
            time.sleep(0.5)
            return True
    return False


def dcp_command_oneshot(cmd_id, body=b'', timeout_ms=5000):
    """Open USB, send one DCP command, read response, close. Ultra-reliable."""
    import usb.core
    dev = usb.core.find(idVendor=VID, idProduct=PID)
    if not dev:
        raise RuntimeError("Device not found")

    # Detach kernel drivers
    for cfg in dev:
        for intf in cfg:
            try:
                dev.detach_kernel_driver(intf.bInterfaceNumber)
            except Exception:
                pass
    dev.set_configuration()

    # Build DCP packet (cmd_idx=1, doesn't matter for single-shot)
    hdr = struct.pack('<IHH', cmd_id, len(body), 1) + b'\x00' * 8
    pkt = hdr + body

    # Send command
    dev.ctrl_transfer(0x41, 2, 0, 0, pkt, timeout=timeout_ms)

    # Read response with retries
    for _ in range(40):
        time.sleep(0.025)
        try:
            resp = bytes(dev.ctrl_transfer(0xC1, 3, 0, 0, 1040, timeout=timeout_ms))
            if len(resp) > 16:
                return resp[16:]  # strip DCP header
        except Exception:
            pass

    # Fallback: return empty
    return b''


def flash_info():
    """Query flash info via DCP."""
    resp = dcp_command_oneshot(0x81F000)
    if len(resp) >= 12:
        jedec = struct.unpack_from('<I', resp, 0)[0]
        return jedec
    return 0


def flash_erase_sector(spi_addr):
    """Erase one sector."""
    body = struct.pack('<II', spi_addr, SECTOR_SIZE)
    resp = dcp_command_oneshot(0x81F002, body)
    if len(resp) >= 4:
        return struct.unpack_from('<I', resp, 0)[0]
    return -1


def flash_write_chunk(spi_addr, data):
    """Write a small chunk (≤256 bytes)."""
    body = struct.pack('<I', spi_addr) + data
    resp = dcp_command_oneshot(0x81F003, body, timeout_ms=10000)
    if len(resp) >= 4:
        return struct.unpack_from('<I', resp, 0)[0]
    return -1


def flash_sectors(sectors_to_write):
    """Write sectors to SPI flash. Each DCP command is a fresh USB session."""
    CHUNK = 128  # bytes per write — conservative for reliability

    print("\n── Flash Sectors ──")
    jedec = flash_info()
    if jedec != 0xBF2541:
        print(f"  Flash handler not responding (JEDEC={jedec:#x})")
        return False

    print(f"  Flash handler OK (JEDEC {jedec:#06x})")

    patched = SPI_PATCHED.read_bytes()

    for spi_addr in sectors_to_write:
        sector = patched[spi_addr:spi_addr + SECTOR_SIZE]
        if len(sector) < SECTOR_SIZE:
            sector += b'\xff' * (SECTOR_SIZE - len(sector))

        # Erase
        print(f"  [{spi_addr:#07x}] erase...", end=' ', flush=True)
        for attempt in range(3):
            status = flash_erase_sector(spi_addr)
            if status == 0:
                print("OK")
                break
            print(f"retry({status:#x})...", end=' ', flush=True)
            usb_reset_and_wait(5)
        else:
            print("FAILED")
            return False

        time.sleep(0.3)

        # Write in chunks
        print(f"  [{spi_addr:#07x}] write ", end='', flush=True)
        for off in range(0, SECTOR_SIZE, CHUNK):
            chunk = sector[off:off + CHUNK]
            for attempt in range(3):
                try:
                    status = flash_write_chunk(spi_addr + off, chunk)
                    if status == 0:
                        break
                except Exception:
                    status = -1
                if attempt < 2:
                    usb_reset_and_wait(3)
            else:
                print(f" FAIL@+{off:#x}")
                return False
            print('.', end='', flush=True)
        print(" OK")
        time.sleep(0.3)

    return True


# ── Reboot ───────────────────────────────────────────────────────────────

def jtag_reboot(ocd):
    """Reboot device via JTAG nRST."""
    print("\n── Reboot ──")
    ocd.reset_run()
    print("  nRST asserted — device rebooting")
    time.sleep(3)
    if usb_find():
        print("  USB re-enumerated — device up")
        return True
    # Wait longer
    for _ in range(10):
        time.sleep(1)
        if usb_find():
            print("  USB re-enumerated — device up")
            return True
    print("  WARNING: USB not re-enumerated after 13s")
    return False


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
    parser.add_argument('--inject', action='store_true', help='Just JTAG inject (no flash)')
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
            print("Replug USB to test, or run without --inject to flash.")
            return

        # After inject, device USB is disrupted. Reset to get clean USB.
        print("\n── USB Recovery ──")
        ocd.reset_run()
        print("  Device reset via nRST")
        time.sleep(2)

        # Re-inject after reset (SRAM was cleared)
        print("  Re-injecting after reset...")
        time.sleep(1)
        if not jtag_inject(ocd):
            sys.exit(1)

        # Now we need USB. The inject halted briefly — try USB reset.
        print("  Waiting for USB...", end=' ', flush=True)
        time.sleep(2)
        if not usb_find():
            # Try nRST to get USB back, but that clears SRAM...
            # Instead, just wait longer
            for _ in range(10):
                time.sleep(1)
                if usb_find():
                    break
        if usb_find():
            print("OK")
        else:
            print("FAIL — please replug USB cable")
            input("Press Enter when USB is replugged...")

        # Flash
        sectors = find_changed_sectors()
        print(f"\nSectors to flash: {[f'{s:#07x}' for s in sectors]}")
        if not flash_sectors(sectors):
            print("\nFlash FAILED. Device may need recovery.")
            sys.exit(1)

        # Reboot to activate persistent patch
        jtag_reboot(ocd)

        # Verify
        print("\n── Verify ──")
        time.sleep(2)
        jedec = flash_info()
        if jedec == 0xBF2541:
            print("  Flash handler alive after reboot — persistent patch WORKS")
        else:
            print(f"  Flash handler not responding (JEDEC={jedec:#x})")
            print("  Persistent patch may not have installed correctly")

        print("\n" + "=" * 50)
        print("Done. Test MIDI CCs:")
        print("  amidi -p hw:1,0,0 -S 'B0 07 40'  # master bus A ~50%")
        print("=" * 50)

    finally:
        ocd.close()


if __name__ == '__main__':
    main()
