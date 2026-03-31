#!/usr/bin/env python3
"""Hook definitions for Blender DICE3 firmware.

Usage:
    python3 firmware/patch/hooks.py validate   # check hook points
    python3 firmware/patch/hooks.py generate   # emit hooks_gen.S + patch.ld
    make -C firmware/patch                     # compile
    python3 firmware/patch/hooks.py patch      # create patched SPI image
    python3 firmware/patch/hooks.py inject     # JTAG live injection
"""

import struct
import subprocess
import sys
import zlib
from pathlib import Path
from hook_framework import (Hook, PatchProject, compute_crc_checkword,
                             encode_arm_b, encode_arm_bl,
                             sram_to_file, SRAM_LOAD_ADDR,
                             SPI_BODY_START, SPI_HEADER_ADDR)

FIRMWARE_DIR = Path(__file__).resolve().parent.parent
PATCH_DIR = Path(__file__).resolve().parent
SPI_IMAGE = FIRMWARE_DIR / 'blender_spi_flash_restored.bin'
PATCHED_SPI = FIRMWARE_DIR / 'blender_spi_patched.bin'

# ── Layout ──────────────────────────────────────────────────────────────────
#
# Handler at SRAM 0x32600 — between BSS_end (0x325F8) and heap_start.
# Heap base literal patched from 0x325F8 to 0x32A00 so allocator skips handler.
# BSS clear stops at 0x325F8 — doesn't touch handler. Heap starts at 0x32A00.
# DEADBEEF fill is below 0x325F8 — doesn't touch handler.
#
# This address works for BOTH JTAG injection AND persistent SPI install.
# DCP registration still needs JTAG inject (main loop blocked in cyg_flag_wait).

PATCH_ZONE_SRAM = 0x32600
PATCH_ZONE_SIZE = 0x400  # 1024 bytes (gap: 0x32600-0x329FF)

# Heap base literal in code section (patched for persistent mode)
HEAP_LITERAL_SRAM = 0xC570     # mempool_var_init loads heap_start from here
HEAP_BASE_ORIG    = 0x325F8    # original heap_start = BSS_end
HEAP_BASE_NEW     = 0x32A00    # new heap_start (after handler zone)

# Firmware identity
IDENTITY_ADDR = 0x8968
IDENTITY_WORD = 0xe92d4ff0

# Hook site
HOOK_TARGET = 0x4FAC


# ── Hook definition ────────────────────────────────────────────────────────

hooks = [
    Hook(
        name="dcp_flash",
        target=HOOK_TARGET,
        handler="flash_handler_init",
        mode="before",
    ),
]

# ── Project ─────────────────────────────────────────────────────────────────

project = PatchProject(
    hooks=hooks,
    spi_image=str(SPI_IMAGE),
    patched_spi=str(PATCHED_SPI),
    hook_bin=str(PATCH_DIR / "hooks.bin"),
    elf_path=str(PATCH_DIR / "hooks.elf"),
    build_dir=str(PATCH_DIR),
    engine_kwargs=dict(
        patch_zone_start=PATCH_ZONE_SRAM,
        patch_zone_size=PATCH_ZONE_SIZE,
    ),
    firmware_symbols=str(FIRMWARE_DIR / "firmware_symbols.ld"),
    identity_check=(IDENTITY_ADDR, IDENTITY_WORD),
    dcp_handler_list=0x313C4,
)


def cmd_patch():
    """Create persistent patched SPI image.

    Handler at SRAM 0x32600 (between BSS_end and heap_start).
    Heap base literal patched so allocator skips handler zone.
    DCP registration still needs JTAG inject after each power cycle.
    """
    hooks_bin = PATCH_DIR / "hooks.bin"

    if not hooks_bin.exists():
        print(f"ERROR: {hooks_bin} not found. Run: make -C firmware/patch")
        sys.exit(1)

    handler = hooks_bin.read_bytes()
    handler_padded = handler + b'\x00' * ((4 - len(handler) % 4) % 4)
    print(f"Handler: {len(handler)} bytes → SRAM 0x{PATCH_ZONE_SRAM:05X}")

    # Read original SPI image
    spi = bytearray(SPI_IMAGE.read_bytes())
    size_words = struct.unpack_from('<I', spi, SPI_HEADER_ADDR + 4)[0]
    body = spi[SPI_BODY_START:SPI_BODY_START + size_words * 4]

    crc = zlib.crc32(body, 0xFFFFFFFF) & 0xFFFFFFFF
    if crc != 0xFFFFFFFF:
        print(f"ERROR: original CRC failed ({crc:#010x})")
        sys.exit(1)
    print(f"Original body: {len(body)} bytes, CRC OK")

    content = bytearray(body[:-4])  # strip CRC

    # ── Patch 1: Extend body to include handler ──
    handler_file_off = sram_to_file(PATCH_ZONE_SRAM)
    if handler_file_off + len(handler_padded) > len(content):
        content.extend(b'\x00' * (handler_file_off + len(handler_padded) - len(content)))
    content[handler_file_off:handler_file_off + len(handler_padded)] = handler_padded
    content.extend(b'\x00' * ((4 - len(content) % 4) % 4))
    new_size_words = (len(content) + 4) // 4
    print(f"  Body extended: 0x{size_words:X} → 0x{new_size_words:X} words")

    # ── Patch 2: Boot init call at 0x344 ──
    # Replace `bl rtos_app_init` with `bl boot_init` in firmware_entry.
    # boot_init registers DCP handler directly, then chains to rtos_app_init.
    r = subprocess.run(['arm-none-eabi-nm', str(PATCH_DIR / 'hooks.elf')],
                       capture_output=True, text=True)
    symbols = {}
    for line in r.stdout.strip().split('\n'):
        parts = line.strip().split()
        if len(parts) == 3:
            symbols[parts[2]] = int(parts[0], 16)
    boot_init_addr = symbols.get('boot_init')
    if not boot_init_addr:
        print("  ERROR: boot_init not found in ELF"); sys.exit(1)
    boot_call_off = sram_to_file(0x344)
    old_call = struct.unpack_from('<I', content, boot_call_off)[0]
    new_call = encode_arm_bl(0x344, boot_init_addr)
    struct.pack_into('<I', content, boot_call_off, new_call)
    print(f"  Boot init: 0x344 bl 0x{boot_init_addr:05X} ({new_call:#010x}, was {old_call:#010x})")

    # ── Patch 3: Move heap_start past handler zone ──
    # mempool_var_init loads heap_start from literal at 0xC570
    # heap_size is computed as 0x50000 - heap_start (RSB instruction, auto-adjusts)
    heap_off = sram_to_file(HEAP_LITERAL_SRAM)
    old_heap = struct.unpack_from('<I', content, heap_off)[0]
    if old_heap != HEAP_BASE_ORIG:
        print(f"  WARNING: heap literal is 0x{old_heap:05X}, expected 0x{HEAP_BASE_ORIG:05X}")
    struct.pack_into('<I', content, heap_off, HEAP_BASE_NEW)
    print(f"  Heap base: 0x{HEAP_BASE_ORIG:05X} → 0x{HEAP_BASE_NEW:05X} "
          f"(handler zone 0x{PATCH_ZONE_SRAM:05X}-0x{HEAP_BASE_NEW:05X})")

    # ── Recompute CRC ──
    new_crc = compute_crc_checkword(bytes(content))
    new_body = bytes(content) + new_crc
    check = zlib.crc32(new_body, 0xFFFFFFFF) & 0xFFFFFFFF
    if check != 0xFFFFFFFF:
        raise RuntimeError(f"CRC verification failed: {check:#010x}")
    print(f"  CRC: OK (body = {len(new_body)} bytes)")

    # ── Write patched SPI image ──
    struct.pack_into('<I', spi, SPI_HEADER_ADDR + 4, new_size_words)
    end = SPI_BODY_START + len(new_body)
    if end > len(spi):
        spi.extend(b'\xff' * (end - len(spi)))
    spi[SPI_BODY_START:SPI_BODY_START + len(new_body)] = new_body

    PATCHED_SPI.write_bytes(spi)
    print(f"\nWrote: {PATCHED_SPI}")

    # Show changed sectors
    original = SPI_IMAGE.read_bytes()
    patched = bytes(spi)
    changed = []
    for off in range(0, max(len(original), len(patched)), 0x1000):
        a = original[off:off + 0x1000].ljust(0x1000, b'\xff')
        b = patched[off:off + 0x1000].ljust(0x1000, b'\xff')
        if a != b:
            changed.append(off)
    print(f"\nChanged sectors ({len(changed)}):")
    for s in changed:
        print(f"  SPI 0x{s:06X}")


if __name__ == '__main__':
    if len(sys.argv) < 2:
        print(f"Usage: {sys.argv[0]} <validate|generate|patch|inject>")
        sys.exit(1)

    cmd = sys.argv[1]
    if cmd == 'patch':
        cmd_patch()
    else:
        project.main()
