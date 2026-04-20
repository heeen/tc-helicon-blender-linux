#!/usr/bin/env python3
"""Probe two AAI/byte-program quirks that survived the retry-loop fix.

Usage:
    python3 firmware/test_quirks.py [--test head|bytes|both]

Test 1 (head): is the "first-byte drop" content-dependent or location-dependent?
  1a  autonomous 0x00-pattern → fresh non-sector-0 location
  1b  TCAT magic bytes → fresh non-sector-0 location
  1c  autonomous 0x00-pattern → sector 0

Test 2 (bytes): single-byte writes with TIM->byte_prog_offset=0 (no +4 hack).
  - 8 addresses at varied alignment
  - Back-to-back pair: does op B inherit op A's address?
  - Multi-byte patch: do all N bytes share the same -4 offset?

All writes go to the 0x40000-0x9FFFF region which is all-FF in the patched
image, so we won't overwrite anything we care about.
"""
import argparse
import os
import sys
import time
from pathlib import Path

sys.path.insert(0, str(Path(__file__).parent))
from jtag_flash_v2 import OpenOCD, FlashClientV2, SECTOR_SIZE, DATA_BUF_ADDR


def stage_data_buf(ocd, data):
    """Push up to SECTOR_SIZE bytes into DATA_BUF via JTAG."""
    tmp = f"/tmp/test_quirks_{id(data):x}.bin"
    Path(tmp).write_bytes(data)
    ocd.halt()
    ocd.load_image(tmp, DATA_BUF_ADDR)
    ocd.resume()
    os.unlink(tmp)


def run_head_drop_tests(client):
    """Test 1: is the head-drop content-dependent or location-dependent?"""
    print("\n" + "=" * 64)
    print("TEST 1: AAI head-drop content vs location")
    print("=" * 64)

    # Reference patterns
    pat_zero = bytes(((i * 37) & 0xFF) if (i % 257) else 0
                     for i in range(SECTOR_SIZE))
    # 17-byte TCAT header: 54 43 41 54 20 03 31 64 03 00 f0 04 00 3e 02 00 02
    tcat_hdr = bytes([0x54, 0x43, 0x41, 0x54, 0x20, 0x03, 0x31, 0x64,
                      0x03, 0x00, 0xF0, 0x04, 0x00, 0x3E, 0x02, 0x00, 0x02])
    pat_tcat = tcat_hdr + pat_zero[len(tcat_hdr):]

    cases = [
        ("1a  zero-pat @ 0x070000 (fresh, non-0)", 0x70000, pat_zero),
        ("1b  TCAT-pat @ 0x080000 (fresh, non-0)", 0x80000, pat_tcat),
        ("1c  zero-pat @ 0x000000 (sector 0)    ", 0x000000, pat_zero),
        ("1d  TCAT-pat @ 0x090000 (fresh, non-0)", 0x90000, pat_tcat),
    ]

    for name, addr, data in cases:
        assert len(data) == SECTOR_SIZE
        # flash_sector does erase + AAI + verify in one driver op.
        ver_ok = client.flash_sector(addr, data)
        rb = client.read(addr, SECTOR_SIZE)
        if rb is None:
            print(f"{name}: readback FAILED")
            continue
        diffs = [(i, data[i], rb[i]) for i in range(SECTOR_SIZE)
                 if data[i] != rb[i]]
        print(f"{name}  verify={'OK' if ver_ok else 'FAIL'}  diffs={len(diffs)}")
        if diffs:
            head_diffs = [d for d in diffs if d[0] < 32]
            tail_diffs = [d for d in diffs if d[0] >= SECTOR_SIZE - 32]
            mid_diffs = [d for d in diffs
                         if 32 <= d[0] < SECTOR_SIZE - 32]
            if head_diffs:
                print(f"    HEAD ({len(head_diffs)}): "
                      + ", ".join(f"+0x{o:03x}:{e:02x}→{g:02x}"
                                  for o, e, g in head_diffs[:6])
                      + ("..." if len(head_diffs) > 6 else ""))
            if mid_diffs:
                print(f"    MID  ({len(mid_diffs)}): "
                      + ", ".join(f"+0x{o:03x}:{e:02x}→{g:02x}"
                                  for o, e, g in mid_diffs[:6])
                      + ("..." if len(mid_diffs) > 6 else ""))
            if tail_diffs:
                print(f"    TAIL ({len(tail_diffs)}): "
                      + ", ".join(f"+0x{o:03x}:{e:02x}→{g:02x}"
                                  for o, e, g in tail_diffs[:6])
                      + ("..." if len(tail_diffs) > 6 else ""))


def run_byte_offset_tests(client):
    """Test 2: probe BYTE_PROGRAM wire-address offset under varied conditions.

    Runs with TIM->byte_prog_offset=0 (disables the +4 hack) so we can observe
    the raw landing offset.
    """
    print("\n" + "=" * 64)
    print("TEST 2: BYTE_PROGRAM landing offset (byte_prog_offset=0)")
    print("=" * 64)

    client.set_timings(byte_prog_offset=0)

    def probe_one(sector_base, offset, val, label=""):
        """Erase, write 1 byte, readback, report actual landing offset(s)."""
        if not client.erase(sector_base):
            print(f"  [{label}] erase FAILED"); return None
        spi_addr = sector_base + offset
        if not client.byte_patch(spi_addr, bytes([val])):
            print(f"  [{label}] byte_patch FAILED"); return None
        rb = client.read(sector_base, SECTOR_SIZE)
        if rb is None:
            print(f"  [{label}] readback FAILED"); return None
        hits = [(i, b) for i, b in enumerate(rb) if b != 0xFF]
        deltas = [(i - offset, b) for i, b in hits]
        return deltas, hits

    def probe_multi(sector_base, offset, vals, label=""):
        """Erase, write N consecutive bytes starting at offset."""
        if not client.erase(sector_base):
            print(f"  [{label}] erase FAILED"); return None
        spi_addr = sector_base + offset
        if not client.byte_patch(spi_addr, bytes(vals)):
            print(f"  [{label}] byte_patch FAILED"); return None
        rb = client.read(sector_base, SECTOR_SIZE)
        if rb is None:
            print(f"  [{label}] readback FAILED"); return None
        hits = [(i, b) for i, b in enumerate(rb) if b != 0xFF]
        deltas = [(i - offset, b) for i, b in hits]
        return deltas, hits

    print("\n-- 2a single-byte write, various addresses --")
    print("    delta = landed_offset - asked_offset")
    for offset in (0x000, 0x001, 0x002, 0x003, 0x004, 0x010, 0x020,
                   0x100, 0x1FF, 0x3FF, 0x800):
        result = probe_one(0x70000, offset, 0x5A, label=f"0x{offset:03x}")
        if result:
            deltas, hits = result
            hit_str = ", ".join(f"+0x{off:03x}={b:02x}" for off, b in hits)
            print(f"  ask offset 0x{offset:03x} val=0x5A → {hit_str}  "
                  f"deltas={[d for d, _ in deltas]}")

    print("\n-- 2b back-to-back: op B after op A, separate sectors --")
    # Op A: write at 0x80020 in a fresh sector
    probe_one(0x80000, 0x020, 0xAA, label="A")
    print("  op A: sector 0x80000 + 0x020 = 0x80020 val=0xAA  "
          "(see above entries)")
    # Op B: now write at 0x81100 in ANOTHER fresh sector — does it
    # inherit 0x80020 somehow?
    result = probe_one(0x81000, 0x100, 0x5A, label="B")
    if result:
        deltas, hits = result
        hit_str = ", ".join(f"+0x{off:03x}={b:02x}" for off, b in hits)
        print(f"  op B: sector 0x81000 + 0x100 = 0x81100 val=0x5A → "
              f"{hit_str}  deltas={[d for d, _ in deltas]}")

    print("\n-- 2d does data value affect landing address? --")
    # Same asked address (0x200 in a fresh sector each time), vary data byte.
    # If data bits leak into address, landing address varies.
    for i, val in enumerate([0x00, 0x01, 0x02, 0x5A, 0xAA, 0x55, 0xF0, 0x0F, 0xFF]):
        sector_base = 0x90000 + (i << 12)
        result = probe_one(sector_base, 0x200, val, label=f"val=0x{val:02x}")
        if result:
            deltas, hits = result
            hit_str = ", ".join(f"+0x{off:03x}={b:02x}"
                                for off, b in hits if b != 0x00 or off == 0x1FC)
            data_hit = next((o for o, b in hits if b == val), None)
            delta = (data_hit - 0x200) if data_hit is not None else None
            print(f"  ask 0x{sector_base:06x}+0x200 val=0x{val:02x}: "
                  f"data at {'+0x%03x' % data_hit if data_hit else 'NONE'} "
                  f"delta={delta}")

    print("\n-- 2e does cmd byte affect offset? (try BYTE_PRG variants) --")
    # SST25VF016B opcodes: 0x02 BYTE_PRG. Try others as sanity check.
    # Skipping — would need custom driver ops.

    print("\n-- 2c multi-byte patch: 2/3/4/5 consecutive bytes --")
    for nbytes, vals in (
        (2, [0xA1, 0xA2]),
        (3, [0xB1, 0xB2, 0xB3]),
        (4, [0xC1, 0xC2, 0xC3, 0xC4]),
        (5, [0xD1, 0xD2, 0xD3, 0xD4, 0xD5]),
    ):
        # Use separate sectors so no cross-contamination
        sector_base = 0x60000 + (nbytes << 12)
        result = probe_multi(sector_base, 0x100, vals,
                             label=f"multi-{nbytes}")
        if result:
            deltas, hits = result
            hit_str = ", ".join(f"+0x{off:03x}={b:02x}" for off, b in hits)
            print(f"  {nbytes} bytes asked at 0x{sector_base:06x}+0x100 vals={vals}")
            print(f"    hits: {hit_str}")
            print(f"    deltas from 0x100: {[d for d, _ in deltas]}")

    # Restore the hack so rest of the driver works
    client.set_timings(byte_prog_offset=4)


def main():
    p = argparse.ArgumentParser()
    p.add_argument("--test", choices=("head", "bytes", "both"), default="both")
    p.add_argument("--speed", type=int, default=1000)
    args = p.parse_args()

    ocd = OpenOCD(speed=args.speed)
    try:
        client = FlashClientV2(ocd)
        client.load_driver()
        if not client.bp_clear():
            raise SystemExit("bp_clear failed")

        if args.test in ("head", "both"):
            run_head_drop_tests(client)
        if args.test in ("bytes", "both"):
            run_byte_offset_tests(client)
    finally:
        ocd.close()
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
