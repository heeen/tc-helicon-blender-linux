#!/usr/bin/env python3
"""Resolve the contradiction: per-sector CRCs from hash_range disagree
with the host's reference for sectors 51 & 52, but the WHOLE-image CRC
matches exactly. Either:
  (a) OP_READ has a chunking bug and is returning slightly-wrong data
      while hash_range and the host reference both agree → device flash
      bytes match the reference.
  (b) The device flash actually differs from the reference, AND the
      whole-image CRC match is the impossible coincidence (1 in 2^32).
  (c) hash_range and OP_READ both faithfully report device flash, but
      hash_range's whole-CRC has an independent bug that hides bytes
      51/52 from contributing differently.

Test plan:
  1. Dump sectors 51-52 fully via repeated 192-B OP_READs.
  2. Compute CRC over those bytes.
  3. Compare to per-sector CRC from hash_range (single device-side run).
  4. If they match: OP_READ and hash_range agree → reference file is
     stale (device flash actually differs from blender_spi_patched.bin).
  5. If they differ: OP_READ is unreliable at small chunk granularity.

Then dump those same sectors via a SINGLE 4096-B OP_READ (no chunking)
and check if THAT matches the hash_range CRC too. That distinguishes
'small-chunk OP_READ bug' from 'OP_READ generally wrong'.
"""

from __future__ import annotations

import sys
from pathlib import Path

HERE = Path(__file__).resolve().parent
sys.path.insert(0, str(HERE))

from midi_flash import MidiFlashClient, MidiUsb  # noqa: E402
from sector_diff import SECTOR_SIZE, zlib_rom_crc32  # noqa: E402


REGION_ADDR = 0x40000
SECTORS = [50, 51, 52, 53]   # include neighbors for context


def main() -> int:
    usb_ = MidiUsb()
    fc = MidiFlashClient(usb_)
    try:
        # Single hash_range run to get device-authoritative per-sector CRCs.
        whole, dev_sec_crcs = fc.device_crc32_batch(REGION_ADDR, 75 * SECTOR_SIZE)
        print(f"device whole CRC: 0x{whole:08x}")

        for s in SECTORS:
            addr = REGION_ADDR + s * SECTOR_SIZE
            dev_hash_crc = dev_sec_crcs[s]

            # Path A: many small (192 B) OP_READ chunks.
            small = bytearray()
            n, a = SECTOR_SIZE, addr
            while n > 0:
                c = min(192, n)
                small.extend(fc.read(a, c))
                a += c
                n -= c
            small_crc = zlib_rom_crc32(bytes(small))

            # Path B: a SINGLE OP_READ that pulls back the limit (192 B max
            # response cap is enforced device-side; can't exceed it). So we
            # also check 96 B chunks to see if smaller is more reliable.
            tiny = bytearray()
            n, a = SECTOR_SIZE, addr
            while n > 0:
                c = min(96, n)
                tiny.extend(fc.read(a, c))
                a += c
                n -= c
            tiny_crc = zlib_rom_crc32(bytes(tiny))

            print(f"\nsector {s} @ 0x{addr:06x}:")
            print(f"  hash_range:           0x{dev_hash_crc:08x}")
            print(f"  OP_READ 192-B chunks: 0x{small_crc:08x}  "
                  f"{'MATCH hash' if small_crc == dev_hash_crc else 'DIFF hash'}")
            print(f"  OP_READ  96-B chunks: 0x{tiny_crc:08x}  "
                  f"{'MATCH hash' if tiny_crc == dev_hash_crc else 'DIFF hash'}")

            if small != tiny:
                # Where do the two OP_READ paths disagree?
                divs = [i for i in range(len(small)) if small[i] != tiny[i]]
                print(f"  192-vs-96 byte differences: {len(divs)}; "
                      f"first offsets: {divs[:8]}")
        return 0
    finally:
        usb_.close()


if __name__ == "__main__":
    sys.exit(main())
