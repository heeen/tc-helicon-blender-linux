#!/usr/bin/env python3
"""End-to-end MIDI flash smoke test:
  1. Pick a benign sector in the trailing flash region (well past the
     primary firmware end) — sector 60 @ 0x07C000.
  2. Read its current 4 KB content via OP_READ.
  3. Build a 'reference' that's a copy of device flash but with sector 60
     mutated (XOR a fixed 4-byte tag near the start).
  4. Run sector-diff against this reference — expect exactly 1 differing
     sector (sector 60).
  5. Erase + write sector 60 with the mutated content via WRITE_AAI in
     192-byte chunks.
  6. Re-run sector-diff — expect 0 differing sectors.
  7. Restore the original sector content (erase + rewrite original).
  8. Final sector-diff with the original-bytes reference — expect 0 diffs.

Verifies the full erase→write→hash_range→re-verify loop end to end without
risking any code/data sectors. Sector 60 (SPI 0x07C000) is in the post-
firmware tail region used only for cached state, safe to scribble.

NOTE: this writes flash. Aborts if device JEDEC/info disagrees with what
we expect, to avoid clobbering the wrong device.
"""

from __future__ import annotations

import sys
from pathlib import Path

HERE = Path(__file__).resolve().parent
sys.path.insert(0, str(HERE))

from midi_flash import MidiFlashClient, MidiUsb  # noqa: E402
from sector_diff import (  # noqa: E402
    SECTOR_SIZE, batch_diff, zlib_rom_crc32,
)


REGION_ADDR = 0x40000
REGION_SECTORS = 75
TARGET_SECTOR_INDEX = 60     # SPI 0x40000 + 60*0x1000 = 0x7C000
TAG_OFFSET = 0x080
TAG = b"\xDE\xAD\xBE\xEF"    # XORed into the sector at TAG_OFFSET


def dump_sector(fc: MidiFlashClient, addr: int) -> bytes:
    out = bytearray()
    n = SECTOR_SIZE
    a = addr
    while n > 0:
        c = min(192, n)
        out.extend(fc.read(a, c))
        a += c
        n -= c
    return bytes(out)


def write_sector(fc: MidiFlashClient, addr: int, data: bytes) -> None:
    assert len(data) == SECTOR_SIZE
    fc.erase_sector(addr, 1)
    chunk_size = fc.WRITE_AAI_CHUNK
    off = 0
    while off < SECTOR_SIZE:
        c = min(chunk_size, SECTOR_SIZE - off)
        fc.write_aai(addr + off, data[off:off + c])
        off += c


def build_reference(fc: MidiFlashClient) -> tuple[bytearray, bytes, int]:
    """Build a 75-sector reference that mirrors device EXCEPT one sector
    is mutated. Returns (reference, original_sector_bytes, target_addr)."""
    print("Dumping current device region (75 sectors)...")
    dump = bytearray()
    a = REGION_ADDR
    n = REGION_SECTORS * SECTOR_SIZE
    while n > 0:
        c = min(192, n)
        dump.extend(fc.read(a, c))
        a += c
        n -= c
    print(f"  dumped {len(dump)} bytes")

    target_addr = REGION_ADDR + TARGET_SECTOR_INDEX * SECTOR_SIZE
    sec_off = TARGET_SECTOR_INDEX * SECTOR_SIZE
    original = bytes(dump[sec_off:sec_off + SECTOR_SIZE])

    mutated = bytearray(original)
    for i, b in enumerate(TAG):
        mutated[TAG_OFFSET + i] ^= b
    dump[sec_off:sec_off + SECTOR_SIZE] = mutated
    print(f"  mutated sector {TARGET_SECTOR_INDEX} @ 0x{target_addr:06x}: "
          f"XOR'd 4 bytes at offset 0x{TAG_OFFSET:03x}")
    print(f"  original CRC: 0x{zlib_rom_crc32(original):08x}")
    print(f"  mutated CRC:  0x{zlib_rom_crc32(bytes(mutated)):08x}")
    return dump, original, target_addr


def main() -> int:
    usb_ = MidiUsb()
    fc = MidiFlashClient(usb_)
    try:
        info = fc.info()
        if info["jedec"] != 0x00BF2541:
            print(f"unexpected JEDEC 0x{info['jedec']:08x}; aborting",
                  file=sys.stderr)
            return 1

        # Sector-diff abuses 0-based addressing internally — but our region
        # starts at 0x40000. Pass `dump` as the ref bytes for batch_diff;
        # the 'address' it returns is sector_index * 0x1000 within the
        # region, so we'll add REGION_ADDR when calling erase/write.

        class RegionClient:
            """Adapter: batch_diff calls device_crc32_batch(0, len, …) —
            we redirect that to the real device at REGION_ADDR offset."""
            def device_crc32_batch(self, addr: int, length: int,
                                   sector_buf_addr=None):
                return fc.device_crc32_batch(REGION_ADDR + addr, length)

        rc = RegionClient()

        ref, original, target_addr = build_reference(fc)

        print("\n[1] sector-diff before flash")
        match, diff = batch_diff(rc, bytes(ref), quiet=False)
        if match or diff != {TARGET_SECTOR_INDEX * SECTOR_SIZE}:
            print(f"  expected exactly sector {TARGET_SECTOR_INDEX} "
                  f"(rel addr 0x{TARGET_SECTOR_INDEX * SECTOR_SIZE:x}); "
                  f"got match={match}, diff={sorted(diff)}")
            return 2
        print(f"  ok: 1 differing sector")

        print("\n[2] erase + write mutated sector via MIDI SysEx")
        target_sector_data = bytes(ref[
            TARGET_SECTOR_INDEX * SECTOR_SIZE:
            (TARGET_SECTOR_INDEX + 1) * SECTOR_SIZE
        ])
        write_sector(fc, target_addr, target_sector_data)

        print("\n[3] sector-diff after write — expect zero diffs")
        match, diff = batch_diff(rc, bytes(ref), quiet=False)
        if not match or diff:
            print(f"  verification FAILED: match={match}, "
                  f"diff={sorted(diff)}")
            return 3
        print("  ok: device matches mutated reference")

        print("\n[4] restoring original sector content")
        write_sector(fc, target_addr, original)
        # Update ref back to original.
        ref[TARGET_SECTOR_INDEX * SECTOR_SIZE:
            (TARGET_SECTOR_INDEX + 1) * SECTOR_SIZE] = original
        match, diff = batch_diff(rc, bytes(ref), quiet=False)
        if not match or diff:
            print(f"  restore verification FAILED: match={match}, "
                  f"diff={sorted(diff)}")
            return 4
        print("  ok: original content restored, region clean")

        print("\nMIDI flash smoke test PASSED")
        return 0
    finally:
        usb_.close()


if __name__ == "__main__":
    sys.exit(main())
