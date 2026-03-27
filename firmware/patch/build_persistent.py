#!/usr/bin/env python3
"""Build persistent firmware patch with .ctors relocator.

The handler runs from SRAM 0x80000 (second bank, survives eCos init).
A tiny .ctors init stub copies the handler there at boot time.

Usage:
    python3 firmware/patch/build_persistent.py         # build + patch SPI
    python3 firmware/patch/build_persistent.py flash    # build + patch + JTAG flash
"""

import struct
import subprocess
import sys
import zlib
from pathlib import Path

FIRMWARE_DIR = Path(__file__).resolve().parent.parent
PATCH_DIR = Path(__file__).resolve().parent

sys.path.insert(0, str(PATCH_DIR))
from hook_framework import (
    encode_arm_bl, compute_crc_checkword,
    sram_to_file, SRAM_LOAD_ADDR, SPI_BODY_START
)

# ── Addresses ──

HANDLER_RUNTIME = 0x80000          # where handler runs from (second SRAM bank)
CTOR_INIT_SRAM  = 0x2AA88          # .ctors init function (body-append)
HANDLER_BLOB_SRAM = 0x2AB20        # handler blob in body (copied to 0x80000)
HOOK_TARGET     = 0x4FAC           # main loop hook site
MEMCPY          = 0xA77C           # firmware memcpy
CTOR_ORIG_15    = 0x7A4            # original .ctors[15]
CTOR_ORIG_16    = 0xB84            # original .ctors[16]

# .ctors runner pointers
CTORS_END_ADDR  = 0x8D98           # DAT_8D98: .ctors end pointer
CTORS_END_ORIG  = 0x2AA84

# BSS
BSS_START_ADDR  = 0x698
BSS_START_ORIG  = 0x2AA84

# SPI image
SPI_IMAGE = FIRMWARE_DIR / 'blender_spi_flash_restored.bin'
PATCHED_SPI = FIRMWARE_DIR / 'blender_spi_patched.bin'

CC = 'arm-none-eabi-gcc'
OBJCOPY = 'arm-none-eabi-objcopy'
CFLAGS = '-march=armv5te -marm -mno-thumb-interwork -Os -nostdlib -ffreestanding -Wall -Wextra -Wno-unused-parameter'.split()


def build_handler():
    """Build the handler binary for runtime address 0x80000."""
    print("=== Building handler (target 0x80000) ===")
    # Generate hooks stub + linker script via hooks.py
    subprocess.run([sys.executable, str(PATCH_DIR / 'hooks.py'), 'generate'],
                   cwd=str(PATCH_DIR), check=True, capture_output=True)
    # Build
    subprocess.run([CC] + CFLAGS + [
        '-T', str(PATCH_DIR / 'patch.ld'),
        '-o', str(PATCH_DIR / 'hooks.elf'),
        str(PATCH_DIR / 'hooks_gen.S'),
        str(PATCH_DIR / 'handlers.c'),
    ], check=True)
    subprocess.run([OBJCOPY, '-O', 'binary',
                    str(PATCH_DIR / 'hooks.elf'),
                    str(PATCH_DIR / 'hooks.bin')], check=True)
    handler = (PATCH_DIR / 'hooks.bin').read_bytes()
    print(f"  Handler: {len(handler)} bytes for SRAM {HANDLER_RUNTIME:#x}")
    return handler


def build_ctor_stub(handler_size):
    """Build the .ctors init stub (ARM assembly)."""
    print("\n=== Building .ctors relocator stub ===")

    # Generate assembly with correct sizes
    asm = f"""
    .arm
    .cpu arm926ej-s
    .section .text, "ax", %progbits
    .global ctor_init
    .type ctor_init, %function
    .align 2

ctor_init:
    stmfd   sp!, {{r4, lr}}
    /* Call displaced .ctors [15] and [16] */
    ldr     r4, =0x{CTOR_ORIG_15:x}
    blx     r4
    ldr     r4, =0x{CTOR_ORIG_16:x}
    blx     r4
    /* memcpy(0x80000, handler_blob, handler_size) */
    ldr     r0, =0x{HANDLER_RUNTIME:x}
    ldr     r1, =0x{HANDLER_BLOB_SRAM:x}
    ldr     r2, =0x{handler_size:x}
    ldr     r4, =0x{MEMCPY:x}
    blx     r4
    ldmfd   sp!, {{r4, pc}}
    .ltorg
    .size ctor_init, . - ctor_init
"""
    asm_path = PATCH_DIR / 'ctor_stub.S'
    asm_path.write_text(asm)

    # Simple linker script for the stub at CTOR_INIT_SRAM
    ld = f"""
MEMORY {{ STUB (rx) : ORIGIN = 0x{CTOR_INIT_SRAM:x}, LENGTH = 152 }}
SECTIONS {{ .text : {{ *(.text*) }} > STUB /DISCARD/ : {{ *(.ARM.*) *(.comment) }} }}
"""
    ld_path = PATCH_DIR / 'ctor_stub.ld'
    ld_path.write_text(ld)

    subprocess.run([CC, '-march=armv5te', '-marm', '-nostdlib', '-ffreestanding',
                    '-T', str(ld_path), '-o', str(PATCH_DIR / 'ctor_stub.elf'),
                    str(asm_path)], check=True)
    subprocess.run([OBJCOPY, '-O', 'binary',
                    str(PATCH_DIR / 'ctor_stub.elf'),
                    str(PATCH_DIR / 'ctor_stub.bin')], check=True)

    stub = (PATCH_DIR / 'ctor_stub.bin').read_bytes()
    print(f"  Stub: {len(stub)} bytes at SRAM {CTOR_INIT_SRAM:#x}")
    if len(stub) > 152:
        raise ValueError(f"Stub too large ({len(stub)} > 152 bytes)")
    return stub


def patch_spi(handler, stub):
    """Create the patched SPI image."""
    print("\n=== Patching SPI image ===")
    spi = bytearray(SPI_IMAGE.read_bytes())
    orig_sz = struct.unpack_from('<I', spi, 0x40004)[0]

    # Work on body content
    body_start = SPI_BODY_START
    content = bytearray(spi[body_start:body_start + orig_sz * 4 - 4])

    # 1. Trampoline: BL from 0x4FAC to 0x80000
    tramp = encode_arm_bl(HOOK_TARGET, HANDLER_RUNTIME)
    struct.pack_into('<I', content, HOOK_TARGET - SRAM_LOAD_ADDR, tramp)
    print(f"  Trampoline: 0x{HOOK_TARGET:05X} → BL 0x{HANDLER_RUNTIME:05X} ({tramp:#010x})")

    # 2. Hijack .ctors[15]: point to our init at 0x2AA88
    struct.pack_into('<I', content, 0x2AA7C - SRAM_LOAD_ADDR, CTOR_INIT_SRAM)
    print(f"  .ctors[15]: 0x7A4 → 0x{CTOR_INIT_SRAM:05X}")

    # 3. Shrink .ctors by 1: end 0x2AA84 → 0x2AA80
    #    (entry [16] at 0x2AA80 is removed; its function called by our init instead)
    struct.pack_into('<I', content, CTORS_END_ADDR - SRAM_LOAD_ADDR, 0x2AA80)
    print(f"  .ctors end: 0x2AA84 → 0x2AA80 (drop entry [16], called by init)")

    # 4. Place .ctors entry at 0x2AA80 pointing to ctor_init
    #    Wait — we shrunk .ctors to end at 0x2AA80, so 0x2AA80 is NOT called.
    #    The entry at 0x2AA7C IS the last called entry, and we changed it to
    #    point to ctor_init. No need for anything at 0x2AA80.

    # 5. Extend content to include stub + handler blob
    # Stub at 0x2AA88 (file offset 0x2A888)
    stub_off = CTOR_INIT_SRAM - SRAM_LOAD_ADDR
    blob_off = HANDLER_BLOB_SRAM - SRAM_LOAD_ADDR

    # Extend content to fit blob
    needed = blob_off + len(handler)
    if needed > len(content):
        content.extend(b'\x00' * (needed - len(content)))

    # Write stub
    content[stub_off:stub_off + len(stub)] = stub
    print(f"  Stub: {len(stub)}B at file offset {stub_off:#x} (SRAM {CTOR_INIT_SRAM:#x})")

    # Write handler blob
    content[blob_off:blob_off + len(handler)] = handler
    print(f"  Handler blob: {len(handler)}B at file offset {blob_off:#x} (SRAM {HANDLER_BLOB_SRAM:#x})")

    # 6. Patch BSS_start to protect stub + handler blob
    new_bss = (HANDLER_BLOB_SRAM + len(handler) + 3) & ~3
    struct.pack_into('<I', content, BSS_START_ADDR - SRAM_LOAD_ADDR, new_bss)
    print(f"  BSS_start: {BSS_START_ORIG:#x} → {new_bss:#x}")

    # 7. Pad to 4-byte alignment
    pad = (4 - len(content) % 4) % 4
    if pad:
        content.extend(b'\x00' * pad)

    # 8. Compute CRC
    new_crc = compute_crc_checkword(bytes(content))
    new_body = bytes(content) + new_crc
    check = zlib.crc32(new_body, 0xFFFFFFFF) & 0xFFFFFFFF
    if check != 0xFFFFFFFF:
        raise RuntimeError(f"CRC verification failed: {check:#010x}")

    new_size_words = len(new_body) // 4
    print(f"  Body: {len(new_body)} bytes (size_words={new_size_words:#x})")
    print(f"  CRC: OK")

    # 9. Write to SPI image
    struct.pack_into('<I', spi, 0x40004, new_size_words)
    end = body_start + len(new_body)
    if end > len(spi):
        spi.extend(b'\xff' * (end - len(spi)))
    spi[body_start:body_start + len(new_body)] = new_body

    PATCHED_SPI.write_bytes(spi)
    print(f"\n  Wrote: {PATCHED_SPI}")

    # Show changed sectors
    orig = SPI_IMAGE.read_bytes()
    changed = []
    for off in range(0, max(len(orig), len(spi)), 0x1000):
        a = orig[off:off + 0x1000].ljust(0x1000, b'\xff')
        b = bytes(spi[off:off + 0x1000]).ljust(0x1000, b'\xff')
        if a != b:
            changed.append(off)
    print(f"  Changed sectors: {len(changed)}")
    return changed


def main():
    handler = build_handler()
    stub = build_ctor_stub(len(handler))
    changed = patch_spi(handler, stub)

    if len(sys.argv) > 1 and sys.argv[1] == 'flash':
        print(f"\n=== Flashing {len(changed)} sectors via JTAG ===")
        # TODO: call JTAG flash
        print("  (not yet implemented — use test_sector_write.py approach)")

    print(f"\n{'='*50}")
    print(f"Persistent patch built!")
    print(f"  .ctors init copies handler to 0x80000 at boot")
    print(f"  Trampoline at 0x4FAC → BL 0x80000")
    print(f"{'='*50}")


if __name__ == '__main__':
    main()
