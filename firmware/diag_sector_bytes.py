#!/usr/bin/env python3
"""Byte-level comparison of device flash vs host reference for sectors 51 & 52
(0x073000 and 0x074000) — the two sectors whose per-sector CRCs differ
between device and host even though the whole-image CRC matches.

If the bytes match: the device's hash_range is computing per-sector CRC
incorrectly for sectors 51/52 specifically. If the bytes differ: device
is somehow reading different data for those sectors when CRCing them
than when reading them via OP_READ — but the whole-CRC=match constraint
makes the latter unlikely (whole CRC == CRC over all bytes including
those sectors).
"""

from __future__ import annotations

import sys
from pathlib import Path

HERE = Path(__file__).resolve().parent
sys.path.insert(0, str(HERE))

from midi_flash import MidiFlashClient, MidiUsb  # noqa: E402
from sector_diff import SECTOR_SIZE, zlib_rom_crc32  # noqa: E402


SECTORS = [51, 52]
REGION_ADDR = 0x40000


def read_sector(fc: MidiFlashClient, sector_addr: int) -> bytes:
    """Read a 4 KB sector via repeated OP_READ chunks (≤192 B each)."""
    out = bytearray()
    n = SECTOR_SIZE
    addr = sector_addr
    while n > 0:
        c = min(192, n)
        out.extend(fc.read(addr, c))
        addr += c
        n -= c
    return bytes(out)


def main() -> int:
    ref_path = HERE / "blender_spi_patched.bin"
    ref = ref_path.read_bytes()

    usb_ = MidiUsb()
    fc = MidiFlashClient(usb_)
    try:
        for s in SECTORS:
            sector_addr = REGION_ADDR + s * SECTOR_SIZE
            host = ref[sector_addr:sector_addr + SECTOR_SIZE]
            host_crc = zlib_rom_crc32(host)

            # Pull entire 4 KB via OP_READ.
            dev = read_sector(fc, sector_addr)
            dev_crc = zlib_rom_crc32(dev)

            print(f"sector {s} @ 0x{sector_addr:06x}:")
            print(f"  host CRC over reference bytes: 0x{host_crc:08x}")
            print(f"  CRC over device-read bytes:    0x{dev_crc:08x}")
            print(f"  bytes match: {dev == host}")
            if dev != host:
                # Show first diverging offsets.
                divs = [i for i in range(len(host)) if host[i] != dev[i]]
                print(f"  {len(divs)} byte differences; first 32 offsets: {divs[:32]}")
                if divs:
                    o = divs[0]
                    print(f"  at offset 0x{o:03x}:")
                    print(f"    host: {host[o:o+16].hex()}")
                    print(f"    dev:  {dev[o:o+16].hex()}")
        return 0
    finally:
        usb_.close()


if __name__ == "__main__":
    sys.exit(main())
