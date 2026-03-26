#!/usr/bin/env python3
"""Phase 2: Install persistent DCP flash handler by appending to firmware body.

There is NO XIP on DICE3. All code runs from SRAM. The bootloader copies the
firmware body from SPI to SRAM at boot. We append our handler to the body so
it gets copied automatically.

Layout change:
  Before: [body content (174212 bytes)] [CRC checkword (4 bytes)]
  After:  [body content (174212 bytes)] [handler (440 bytes)] [new CRC (4 bytes)]

The bootloader reads size_words from the header and copies that many bytes.
We update size_words to include our handler. CRC is recomputed.

Prerequisites:
  - Phase 1 handler must be active (run inject_flash_handler.py first)
  - `blender-ctl usb flash info` must work

Usage:
    python3 firmware/install_handler.py                     # install
    python3 firmware/install_handler.py --uninstall         # remove
    python3 firmware/install_handler.py --dry-run           # show plan
"""

import argparse
import struct
import subprocess
import sys
import tempfile
import zlib
from pathlib import Path

FIRMWARE_DIR = Path(__file__).parent

# ── Layout ────────────────────────────────────────────────────────────

PRIMARY_HDR_SPI   = 0x40000    # SPI address of primary header (0x30 bytes)
PRIMARY_BODY_SPI  = 0x40030    # SPI address of primary body
SRAM_LOAD_ADDR    = 0x200      # Bootloader copies body here

SECTOR_SIZE       = 0x1000

# Hook site: `bl 0x2F38` in primary firmware event loop
HOOK_SRAM_ADDR    = 0x4FAC
HOOK_BODY_OFFSET  = HOOK_SRAM_ADDR - SRAM_LOAD_ADDR  # = 0x4DAC
HOOK_SPI_ADDR     = PRIMARY_BODY_SPI + HOOK_BODY_OFFSET

# Original instruction
ORIG_TARGET       = 0x2F38
_BL_ORIG_OFF      = ((ORIG_TARGET - HOOK_SRAM_ADDR - 8) >> 2) & 0x00FFFFFF
BL_ORIG_INSN      = 0xEB000000 | _BL_ORIG_OFF  # 0xEBFFF7E1


# ── USB flash operations via blender-ctl (Rust) ──────────────────────
#
# Using cargo/blender-ctl instead of pyusb because:
# 1. Proven reliable (correct DCP protocol handling, retries, init)
# 2. Handles kernel driver detach/reattach correctly
# 3. pyusb has stale-response issues with DCP pipe

import time

BLENDER_CTL = FIRMWARE_DIR.parent / 'blender-ctl' / 'target' / 'release' / 'blender-ctl'
if not BLENDER_CTL.exists():
    # Fall back to cargo run
    BLENDER_CTL = None

def _flash_cmd():
    if BLENDER_CTL:
        return [str(BLENDER_CTL)]
    return ['cargo', 'run', '-q', '--manifest-path',
            str(FIRMWARE_DIR.parent / 'blender-ctl' / 'Cargo.toml'), '--']

def _run_flash(args, timeout=60):
    cmd = _flash_cmd() + ['usb', 'flash'] + args
    r = subprocess.run(cmd, capture_output=True, text=True, timeout=timeout)
    if r.returncode != 0:
        raise RuntimeError(f"flash {' '.join(args)}: {r.stderr.strip()}")
    return r

def flash_read_sram(sram_addr, length):
    """Read entire range in ONE blender-ctl invocation (fast, single USB session)."""
    with tempfile.NamedTemporaryFile(suffix='.bin', delete=False) as f:
        tmp = f.name
    try:
        # Single invocation reads all chunks internally via DCP
        _run_flash(['read', f'{sram_addr:#x}', str(length), tmp], timeout=120)
        return Path(tmp).read_bytes()
    finally:
        Path(tmp).unlink(missing_ok=True)

def flash_erase(spi_addr, length):
    _run_flash(['erase', f'{spi_addr:#x}', f'{length:#x}'])

def flash_write(spi_addr, data):
    with tempfile.NamedTemporaryFile(suffix='.bin', delete=False) as f:
        f.write(data)
        tmp = f.name
    try:
        _run_flash(['write', f'{spi_addr:#x}', tmp], timeout=120)
    finally:
        Path(tmp).unlink(missing_ok=True)


# ── CRC ───────────────────────────────────────────────────────────────

def compute_crc_checkword(body_without_crc: bytes) -> bytes:
    """Compute 4-byte CRC checkword that makes CRC32(body + checkword) == 0xFFFFFFFF.

    Uses meet-in-the-middle: forward 2 bytes from CRC state, reverse 2 bytes
    from target, find matching intermediate state.
    """
    # zlib.crc32(data, v) uses internal register = v ^ 0xFFFFFFFF.
    # Work with internal registers for table-based computation.
    crc_zlib = zlib.crc32(body_without_crc, 0xFFFFFFFF) & 0xFFFFFFFF
    crc = crc_zlib ^ 0xFFFFFFFF       # internal register after body
    target = 0xFFFFFFFF ^ 0xFFFFFFFF  # = 0 (want zlib to return 0xFFFFFFFF)

    # Build CRC32 table (poly 0xEDB88320)
    table = []
    for i in range(256):
        c = i
        for _ in range(8):
            c = (0xEDB88320 ^ (c >> 1)) if (c & 1) else (c >> 1)
        table.append(c)

    # Reverse lookup: top byte of table entry → table index
    top_to_idx = [0] * 256
    for i in range(256):
        top_to_idx[(table[i] >> 24) & 0xFF] = i

    # Forward: all states after processing b0, b1
    forward = {}
    for b0 in range(256):
        s1 = table[(crc ^ b0) & 0xFF] ^ (crc >> 8)
        for b1 in range(256):
            s2 = table[(s1 ^ b1) & 0xFF] ^ (s1 >> 8)
            if s2 not in forward:
                forward[s2] = (b0, b1)

    # Backward: reverse b3, b2 from target
    idx3 = top_to_idx[(target >> 24) & 0xFF]
    for lo3 in range(256):
        s3 = ((target ^ table[idx3]) << 8) | lo3
        b3 = idx3 ^ lo3
        idx2 = top_to_idx[(s3 >> 24) & 0xFF]
        for lo2 in range(256):
            s2 = ((s3 ^ table[idx2]) << 8) | lo2
            b2 = idx2 ^ lo2
            if s2 in forward:
                b0, b1 = forward[s2]
                result = bytes([b0, b1, b2, b3])
                check = zlib.crc32(body_without_crc + result, 0xFFFFFFFF) & 0xFFFFFFFF
                if check != 0xFFFFFFFF:
                    raise RuntimeError(f"CRC checkword verification failed: {check:#010x}")
                return result

    raise RuntimeError("No CRC checkword found")


# ── Handler build ─────────────────────────────────────────────────────

def build_handler(sram_addr: int) -> bytes:
    """Build handler binary for given SRAM address (contiguous layout)."""
    ld_path = FIRMWARE_DIR / 'body_handler.ld'
    c_src = FIRMWARE_DIR / 'sram_flash_handler.c'
    elf = FIRMWARE_DIR / 'body_flash_handler.elf'
    binf = FIRMWARE_DIR / 'body_flash_handler.bin'

    ld_path.write_text(f'''\
/* Handler appended to firmware body, loaded to SRAM by bootloader.
 * Contiguous layout at SRAM {sram_addr:#x}.
 */
INCLUDE "firmware/firmware_symbols.ld"
ENTRY(_start)

MEMORY {{
    CODE (rwx) : ORIGIN = {sram_addr:#x}, LENGTH = 0x400
}}

SECTIONS {{
    .text : {{
        *(.text.entry)
        *(.text*)
        *(.rodata*)
    }} > CODE

    /DISCARD/ : {{
        *(.ARM.attributes)
        *(.ARM.exidx*)
        *(.comment)
        *(.data*)
        *(.bss*)
    }}
}}
''')

    print(f'Building handler for SRAM {sram_addr:#x}...')
    subprocess.run([
        'arm-none-eabi-gcc', '-march=armv5te', '-marm', '-mno-thumb-interwork',
        '-Os', '-nostdlib', '-ffreestanding', '-Wall',
        '-T', str(ld_path), '-o', str(elf), str(c_src)
    ], check=True)
    subprocess.run([
        'arm-none-eabi-objcopy', '-O', 'binary', str(elf), str(binf)
    ], check=True)

    r = subprocess.run(['arm-none-eabi-size', str(elf)], capture_output=True, text=True)
    if r.returncode == 0:
        print(r.stdout.strip())

    data = binf.read_bytes()
    print(f'  Binary: {len(data)} bytes')
    return data


# ── Install ───────────────────────────────────────────────────────────

def install(dry_run=False):
    # Step 1: Read current header from SRAM (bootloader copies header area too?
    # Actually header is at SPI 0x40000, not in SRAM. We need to read it via
    # our handler from wherever it is accessible. The bootloader area at
    # SRAM ~0x40000+ has it, but that may not be accessible.
    # Instead, read the body from SRAM and use known header values.)

    # Read body size from SRAM: the header at SPI 0x40000 has size_words at offset 4.
    # But we can infer it: body is at SRAM 0x200, we know size = 0xAA22 words = 174216 bytes.
    # Let's verify by checking for known patterns.
    print("Reading current firmware body from SRAM...")
    OLD_SIZE_WORDS = 0xAA22
    old_body_bytes = OLD_SIZE_WORDS * 4  # 174216

    body = flash_read_sram(SRAM_LOAD_ADDR, old_body_bytes)
    print(f"  Read {len(body)} bytes from SRAM {SRAM_LOAD_ADDR:#x}")

    # Verify CRC
    crc = zlib.crc32(body, 0xFFFFFFFF) & 0xFFFFFFFF
    print(f"  CRC32: {crc:#010x} {'PASS' if crc == 0xFFFFFFFF else 'FAIL'}")
    if crc != 0xFFFFFFFF:
        print("  [!] CRC failed — body may be corrupt or size is wrong")
        sys.exit(1)

    # Verify hook site
    hook_insn = struct.unpack_from('<I', body, HOOK_BODY_OFFSET)[0]
    print(f"  Hook at offset {HOOK_BODY_OFFSET:#x}: {hook_insn:#010x}", end='')
    if hook_insn == BL_ORIG_INSN:
        print(" (original, unpatched)")
    else:
        print(f" (NOT original {BL_ORIG_INSN:#010x}!)")
        if hook_insn == struct.unpack('<I', b'\xb4\xf6\xff\xeb')[0]:
            print("  Already patched for body-append handler")
        sys.exit(1)

    # Step 2: Build handler for append address
    body_content = body[:-4]  # everything except CRC checkword
    handler_sram = SRAM_LOAD_ADDR + len(body_content)
    handler_data = build_handler(handler_sram)

    # handler_data must be word-aligned
    if len(handler_data) % 4:
        handler_data += b'\x00' * (4 - len(handler_data) % 4)

    # Step 3: Compute new bl instruction
    bl_off = ((handler_sram - HOOK_SRAM_ADDR - 8) >> 2) & 0x00FFFFFF
    bl_new_insn = 0xEB000000 | bl_off
    print(f"\nHook patch: bl {ORIG_TARGET:#x} → bl {handler_sram:#x}")
    print(f"  {BL_ORIG_INSN:#010x} → {bl_new_insn:#010x}")

    # Step 4: Build new body
    new_body_content = bytearray(body_content)
    struct.pack_into('<I', new_body_content, HOOK_BODY_OFFSET, bl_new_insn)
    new_body_content += handler_data
    new_crc = compute_crc_checkword(bytes(new_body_content))
    new_body = bytes(new_body_content) + new_crc

    new_size_words = len(new_body) // 4
    print(f"\nBody: {old_body_bytes} → {len(new_body)} bytes "
          f"(size_words: {OLD_SIZE_WORDS:#x} → {new_size_words:#x})")

    # Verify new CRC
    check = zlib.crc32(new_body, 0xFFFFFFFF) & 0xFFFFFFFF
    print(f"  New CRC: {check:#010x} {'PASS' if check == 0xFFFFFFFF else 'FAIL'}")
    if check != 0xFFFFFFFF:
        sys.exit(1)

    # Step 5: Determine changed sectors
    # Header sector: SPI 0x40000 (size_words at offset 4)
    # Hook sector: SPI 0x44000 (bl instruction)
    # Tail sector: where handler + new CRC land
    tail_spi_start = PRIMARY_BODY_SPI + len(body_content)  # where handler starts in SPI
    tail_spi_end = PRIMARY_BODY_SPI + len(new_body)
    tail_sector_start = tail_spi_start & ~0xFFF
    tail_sector_end = (tail_spi_end - 1) & ~0xFFF

    hook_sector = HOOK_SPI_ADDR & ~0xFFF
    hdr_sector = PRIMARY_HDR_SPI & ~0xFFF

    sectors = sorted(set([hdr_sector, hook_sector, tail_sector_start]))
    if tail_sector_end != tail_sector_start:
        sectors.append(tail_sector_end)
        sectors.sort()

    print(f"\n{'=' * 60}")
    print("Installation plan:")
    print(f"  Header sector {hdr_sector:#x}: size_words {OLD_SIZE_WORDS:#x} → {new_size_words:#x}")
    print(f"  Hook sector {hook_sector:#x}: bl patch")
    for s in sectors:
        if s != hdr_sector and s != hook_sector:
            print(f"  Tail sector {s:#x}: handler code + CRC")
    print(f"  Total: {len(sectors)} sector(s)")
    print(f"{'=' * 60}")

    if dry_run:
        print("\n(dry-run — no changes made)")
        return

    resp = input("\nProceed? [y/N] ")
    if resp.lower() != 'y':
        print("Aborted.")
        sys.exit(0)

    # Step 6: Read current sector data from SPI, patch, and write back
    # For each sector, we need the CURRENT SPI content. We read from SRAM
    # (which has the body), but for the header sector we need to read
    # from wherever the header is accessible.

    # Header: modify size_words at SPI 0x40004
    print(f"\n[1] Reading header sector from SRAM...")
    # The header (0x30 bytes) is at SPI 0x40000, NOT in the body.
    # Bootloader area may have it. Let's read from the SPI region
    # that's just before the body. Actually the header isn't in SRAM body.
    # We need to read the header sector from SPI. But our READ reads CPU mem...
    # The header is 0x30 bytes. The body starts at SPI 0x40030.
    # SRAM 0x200 = body start. Header is NOT in SRAM at a known location.
    #
    # Solution: read the SRAM region at 0x200 - 0x30 = 0x1D0 to get header + body start.
    # But 0x1D0 may contain exception vectors, not the header.
    #
    # Better: read the header sector content from wherever the bootloader
    # copied it. The bootloader copies to a fixed area.
    # Actually, looking at boot_check_and_load: it calls spi_read_flash_header()
    # which reads 0x30 bytes via SPI DMA into a local variable. It doesn't keep
    # the header in a global.
    #
    # So the header is ONLY in SPI flash, not in SRAM. We can't read it via USB.
    # But we know exactly what it contains (from the SPI dump + our knowledge of
    # the format). We only need to change the size_words field at offset 4.
    #
    # Approach: read the header sector from the SPI dump file, patch size_words,
    # and write it back. This is safe as long as the header sector hasn't been
    # modified since the dump.
    #
    # Actually, even simpler: we know the header layout. Read the sector
    # containing the header from SPI dump, or just write the 4-byte size field.
    # But ERASE is sector-granularity. We need the whole sector content.
    #
    # The safest approach: DON'T modify the header. Instead, account for the
    # size difference in the body itself. But the bootloader reads size_words
    # to know how much to copy... we MUST update it.
    #
    # Let's read the header sector from the SPI dump file.
    spi_dump = FIRMWARE_DIR / 'blender_spi_flash.bin'
    if not spi_dump.exists():
        print(f"  [!] SPI dump not found: {spi_dump}")
        print(f"      Need it for header sector content.")
        print(f"      Create a dump first: blender-ctl usb flash dump firmware/blender_spi_flash.bin")
        sys.exit(1)

    spi_data = spi_dump.read_bytes()

    # But wait — the SPI dump may be stale. The sectors we care about:
    # 0x40000 (header) — should be unchanged since dump
    # Let's verify: the current body CRC passes with OLD_SIZE_WORDS, so
    # the SPI header must still have size_words = OLD_SIZE_WORDS.

    dump_size_words = struct.unpack_from('<I', spi_data, PRIMARY_HDR_SPI + 4)[0]
    if dump_size_words != OLD_SIZE_WORDS:
        print(f"  [!] SPI dump has size_words={dump_size_words:#x}, expected {OLD_SIZE_WORDS:#x}")
        sys.exit(1)

    # Patch header sector from dump
    hdr_sec_data = bytearray(spi_data[hdr_sector:hdr_sector + SECTOR_SIZE])
    hdr_offset_in_sector = (PRIMARY_HDR_SPI + 4) - hdr_sector
    struct.pack_into('<I', hdr_sec_data, hdr_offset_in_sector, new_size_words)

    print(f"  Header sector: size_words patched to {new_size_words:#x}")

    # Hook sector: read from SRAM body, patch, this IS body content
    hook_sec_body_start = (hook_sector - PRIMARY_BODY_SPI)
    hook_sec_data = bytearray(new_body[hook_sec_body_start:hook_sec_body_start + SECTOR_SIZE])

    # Tail sector(s): from new body
    tail_sectors = {}
    for s in sectors:
        if s == hdr_sector or s == hook_sector:
            continue
        sec_body_start = s - PRIMARY_BODY_SPI
        sec_body_end = sec_body_start + SECTOR_SIZE
        chunk = new_body[sec_body_start:sec_body_end]
        # Pad with 0xFF if chunk is shorter than sector (end of body)
        if len(chunk) < SECTOR_SIZE:
            chunk = chunk + b'\xff' * (SECTOR_SIZE - len(chunk))
        tail_sectors[s] = bytes(chunk)

    # Step 7: Write sectors
    step = 1
    total = len(sectors)

    for s in sectors:
        if s == hdr_sector:
            data = bytes(hdr_sec_data)
        elif s == hook_sector:
            data = bytes(hook_sec_data)
        else:
            data = tail_sectors[s]

        print(f"[{step}/{total}] Erase+write sector SPI {s:#x}...")
        flash_erase(s, SECTOR_SIZE)
        flash_write(s, data)
        print(f"  OK")
        step += 1

    print(f"\n{'=' * 60}")
    print("SUCCESS — persistent handler installed")
    print("Power cycle to activate. After reboot:")
    print("  blender-ctl usb flash info")
    print(f"{'=' * 60}")


# ── Uninstall ─────────────────────────────────────────────────────────

def uninstall(dry_run=False):
    """Restore original body size + hook from SPI dump."""
    spi_dump = FIRMWARE_DIR / 'blender_spi_flash.bin'
    if not spi_dump.exists():
        print(f"Need SPI dump: {spi_dump}")
        sys.exit(1)

    print("Reading current body from SRAM to check state...")
    # We don't know the current size_words (it may have been extended).
    # Try the original size first, then extended.
    OLD_SIZE_WORDS = 0xAA22
    body = flash_read_sram(SRAM_LOAD_ADDR, OLD_SIZE_WORDS * 4)
    crc = zlib.crc32(body, 0xFFFFFFFF) & 0xFFFFFFFF

    if crc == 0xFFFFFFFF:
        hook_insn = struct.unpack_from('<I', body, HOOK_BODY_OFFSET)[0]
        if hook_insn == BL_ORIG_INSN:
            print("  Already original — nothing to undo.")
            return
        print(f"  CRC passes but hook is patched: {hook_insn:#010x}")
        print("  This shouldn't happen with body-append method.")
        # The body might have been extended. Need to restore from dump.

    spi_data = bytearray(Path(spi_dump).read_bytes())

    # Restore header, hook, and tail sectors from dump
    hdr_sector = PRIMARY_HDR_SPI & ~0xFFF
    hook_sector = HOOK_SPI_ADDR & ~0xFFF
    # Also restore the sector that had the old CRC (end of original body)
    old_body_end_spi = PRIMARY_BODY_SPI + OLD_SIZE_WORDS * 4
    tail_sector = (old_body_end_spi - 1) & ~0xFFF

    sectors = sorted(set([hdr_sector, hook_sector, tail_sector]))

    print(f"\nRestore {len(sectors)} sector(s) from SPI dump:")
    for s in sectors:
        print(f"  SPI {s:#x}")

    if dry_run:
        print("\n(dry-run)")
        return

    resp = input("\nRestore? [y/N] ")
    if resp.lower() != 'y':
        print("Aborted.")
        sys.exit(0)

    for i, s in enumerate(sectors):
        data = bytes(spi_data[s:s + SECTOR_SIZE])
        print(f"[{i+1}/{len(sectors)}] Erase+write SPI {s:#x}...")
        flash_erase(s, SECTOR_SIZE)
        flash_write(s, data)
        print("  OK")

    print(f"\n{'=' * 60}")
    print("Restored from SPI dump. Power cycle to take effect.")
    print(f"{'=' * 60}")


def main():
    parser = argparse.ArgumentParser(
        description='Install/remove persistent DCP flash handler')
    parser.add_argument('--uninstall', action='store_true')
    parser.add_argument('--dry-run', action='store_true')
    args = parser.parse_args()

    if args.uninstall:
        uninstall(dry_run=args.dry_run)
    else:
        install(dry_run=args.dry_run)

if __name__ == '__main__':
    main()
