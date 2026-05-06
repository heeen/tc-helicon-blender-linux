#!/usr/bin/env python3
"""Diagnose which 2/75 sector CRCs mismatch between device and host while
the whole-image CRC matches.

Background: a previous device_crc32_batch over the patched primary region
returned whole=0x262e87e0 (matches host) but only 73/75 per-sector CRCs
matched. We need to know exactly which sectors and at what indices —
this isolates whether it's a sector-boundary off-by-one in the device's
hash_range loop, a buffer-aliasing artifact in MEM_READ chunking, or
something else."""

from __future__ import annotations

import struct
import sys
from pathlib import Path

HERE = Path(__file__).resolve().parent
sys.path.insert(0, str(HERE))

from midi_flash import MidiFlashClient, MidiUsb  # noqa: E402
from sector_diff import SECTOR_SIZE, DEFAULT_SECTOR_BUF_ADDR, zlib_rom_crc32  # noqa: E402


# Patched primary region. Adjust if the prior test scanned a different range.
REGION_ADDR = 0x40000
REGION_SIZE = 75 * SECTOR_SIZE     # 75 sectors = 300 KB


def main() -> int:
    # Use the full-SPI patched image — 2 MB, covers the patched primary
    # at 0x40000 with the full 75-sector trailing region intact.
    ref_path = HERE / "blender_spi_patched.bin"
    if not ref_path.exists():
        print(f"missing {ref_path}", file=sys.stderr)
        return 1
    ref = ref_path.read_bytes()
    if len(ref) < REGION_ADDR + REGION_SIZE:
        print(f"reference too short: {len(ref)} < {REGION_ADDR + REGION_SIZE}",
              file=sys.stderr)
        return 1

    region = ref[REGION_ADDR:REGION_ADDR + REGION_SIZE]
    print(f"region: {REGION_ADDR:#x}+{REGION_SIZE:#x} "
          f"({REGION_SIZE // SECTOR_SIZE} sectors)")

    usb_ = MidiUsb()
    fc = MidiFlashClient(usb_)
    try:
        whole_dev, dev_crcs = fc.device_crc32_batch(REGION_ADDR, REGION_SIZE)
        whole_host = zlib_rom_crc32(region)
        print(f"whole device: 0x{whole_dev:08x}")
        print(f"whole host:   0x{whole_host:08x}  "
              f"{'MATCH' if whole_dev == whole_host else 'MISMATCH'}")
        print(f"sectors drained: {len(dev_crcs)}")

        host_crcs = [
            zlib_rom_crc32(region[i * SECTOR_SIZE:(i + 1) * SECTOR_SIZE])
            for i in range(REGION_SIZE // SECTOR_SIZE)
        ]
        if len(dev_crcs) != len(host_crcs):
            print(f"sector count mismatch: device={len(dev_crcs)} "
                  f"host={len(host_crcs)}")

        mism = []
        for i, (d, h) in enumerate(zip(dev_crcs, host_crcs)):
            if d != h:
                mism.append(i)

        print(f"\n{len(mism)}/{len(host_crcs)} sectors mismatch")
        if mism:
            print("idx  addr      host        device")
            for i in mism:
                addr = REGION_ADDR + i * SECTOR_SIZE
                print(f"{i:3d}  0x{addr:06x}  0x{host_crcs[i]:08x}  0x{dev_crcs[i]:08x}")

        # Slot the mismatching CRCs into the MEM_READ chunking model
        # (192-byte chunks → 48 CRCs/chunk). Check whether mismatches
        # cluster around a chunk boundary (would indicate MEM_READ corruption).
        if mism:
            print("\nMEM_READ chunk slot per mismatch (chunk = 192 B = 48 CRCs):")
            for i in mism:
                chunk_idx = (i * 4) // 192
                offset_in_chunk = (i * 4) % 192
                print(f"  sector {i}: chunk {chunk_idx}, offset {offset_in_chunk}")

        # Also check a direct READ of sector_buf for those indices to verify
        # the CRCs we got match what's actually in SRAM right now (i.e. nothing
        # is corrupting the values during MEM_READ chunking).
        if mism:
            print("\nRe-reading sector_buf directly via MEM_READ for first 4 mismatches:")
            for i in mism[:4]:
                got = fc.mem_read(DEFAULT_SECTOR_BUF_ADDR + i * 4, 4)
                v = struct.unpack("<I", got)[0]
                print(f"  sector {i}: drained earlier=0x{dev_crcs[i]:08x}  "
                      f"resampled=0x{v:08x}  "
                      f"{'STABLE' if v == dev_crcs[i] else 'DIFFERS'}")

        return 0 if not mism else 2
    finally:
        usb_.close()


if __name__ == "__main__":
    sys.exit(main())
