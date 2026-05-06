#!/usr/bin/env python3
"""Last-resort sanity check: dump the WHOLE 75-sector region via OP_READ
and recompute both whole-image and per-sector CRCs locally. Compare to:
  (1) device hash_range whole CRC
  (2) device hash_range per-sector CRCs
  (3) host reference whole CRC
  (4) host reference per-sector CRCs

If (1) == (3) but the dumped bytes differ from reference, then either:
  - the CRC32 collision is a real (unprecedented) coincidence, or
  - one of the CRC paths is computing on different data than we think.

This dumps slowly (~22 OP_READs/sector × 75 sectors ≈ 1650 reads), so
runs once. Stores the dump to /tmp/device_region.bin for later inspection.
"""

from __future__ import annotations

import sys
import time
from pathlib import Path

HERE = Path(__file__).resolve().parent
sys.path.insert(0, str(HERE))

from midi_flash import MidiFlashClient, MidiUsb  # noqa: E402
from sector_diff import SECTOR_SIZE, zlib_rom_crc32  # noqa: E402


REGION_ADDR = 0x40000
REGION_SECTORS = 75


def main() -> int:
    ref = (HERE / "blender_spi_patched.bin").read_bytes()
    region_ref = ref[REGION_ADDR:REGION_ADDR + REGION_SECTORS * SECTOR_SIZE]

    usb_ = MidiUsb()
    fc = MidiFlashClient(usb_)
    try:
        whole_dev, dev_sec_crcs = fc.device_crc32_batch(
            REGION_ADDR, REGION_SECTORS * SECTOR_SIZE)
        print(f"device hash_range whole CRC: 0x{whole_dev:08x}")
        print(f"host reference whole CRC:    0x{zlib_rom_crc32(region_ref):08x}")

        print("\nDumping full region via OP_READ (this takes a bit)...")
        t0 = time.monotonic()
        dump = bytearray()
        a = REGION_ADDR
        n = REGION_SECTORS * SECTOR_SIZE
        while n > 0:
            c = min(192, n)
            dump.extend(fc.read(a, c))
            a += c
            n -= c
        dt = time.monotonic() - t0
        print(f"dumped {len(dump)} bytes in {dt:.1f}s")

        out_path = Path("/tmp/device_region.bin")
        out_path.write_bytes(bytes(dump))
        print(f"saved to {out_path}")

        whole_dump = zlib_rom_crc32(bytes(dump))
        print(f"\nCRC of OP_READ dump:         0x{whole_dump:08x}  "
              f"{'MATCH device hash' if whole_dump == whole_dev else 'DIFF device hash'}")
        print(f"OP_READ dump == reference:   {bytes(dump) == region_ref}")

        if bytes(dump) != region_ref:
            divs = [i for i in range(len(dump)) if dump[i] != region_ref[i]]
            print(f"  {len(divs)} byte differences across whole region")
            print(f"  first 16 diff offsets: {divs[:16]}")
            print(f"  affected sectors: "
                  f"{sorted({d // SECTOR_SIZE for d in divs})}")

        # Now compute per-sector CRCs from the dumped bytes.
        n_mism_dump_vs_dev = 0
        n_mism_dump_vs_ref = 0
        for i in range(REGION_SECTORS):
            sec_bytes = bytes(dump[i * SECTOR_SIZE:(i + 1) * SECTOR_SIZE])
            sec_ref   = region_ref[i * SECTOR_SIZE:(i + 1) * SECTOR_SIZE]
            crc_dump  = zlib_rom_crc32(sec_bytes)
            crc_ref   = zlib_rom_crc32(sec_ref)
            crc_dev   = dev_sec_crcs[i]
            if crc_dump != crc_dev:
                n_mism_dump_vs_dev += 1
                if n_mism_dump_vs_dev <= 4:
                    print(f"  sector {i}: dump_crc=0x{crc_dump:08x} "
                          f"dev_crc=0x{crc_dev:08x}")
            if crc_dump != crc_ref:
                n_mism_dump_vs_ref += 1

        print(f"\nOP_READ-dump per-sector CRCs vs device hash_range: "
              f"{REGION_SECTORS - n_mism_dump_vs_dev}/{REGION_SECTORS} match")
        print(f"OP_READ-dump per-sector CRCs vs host reference:    "
              f"{REGION_SECTORS - n_mism_dump_vs_ref}/{REGION_SECTORS} match")

        return 0
    finally:
        usb_.close()


if __name__ == "__main__":
    sys.exit(main())
