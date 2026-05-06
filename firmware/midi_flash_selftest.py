#!/usr/bin/env python3
"""Self-tests for the MIDI flash protocol layer.

Exercises pack78/unpack78, crc8, frame build/parse, USB-MIDI packet
framing, and the sector_diff module against a mock device — no
hardware required.

Run before flashing the patched image to catch protocol bugs early.
"""

from __future__ import annotations

import sys
import zlib
from pathlib import Path

HERE = Path(__file__).resolve().parent
sys.path.insert(0, str(HERE))

from midi_flash import (
    pack78, unpack78, crc8,
    build_frame, parse_frame,
    sysex_to_usbmidi, usbmidi_to_sysex,
    OP_INFO, OP_READ, OP_HASH_RANGE, OP_ERASE_SECTOR, OP_WRITE_AAI,
    REPLY_OK_BIT, MFR_ID,
)
from sector_diff import (
    SECTOR_SIZE, zlib_rom_crc32, batch_diff, filter_ops_by_diff,
)


def t(name: str, cond: bool, *, detail: str = "") -> None:
    status = "✓" if cond else "✗"
    print(f"  [{status}] {name}" + (f"  ({detail})" if detail else ""))
    if not cond:
        raise SystemExit(1)


# ── pack78 / unpack78 ─────────────────────────────────────────────────────

def test_pack78() -> None:
    print("pack78 / unpack78:")
    for n in (0, 1, 6, 7, 8, 13, 14, 15, 256, 1024):
        raw = bytes((i * 17 + 5) & 0xFF for i in range(n))
        packed = pack78(raw)
        # Every packed byte must be ≤ 0x7F (MIDI safe).
        assert all(b <= 0x7F for b in packed), f"non-MIDI byte at len={n}"
        roundtrip = unpack78(packed)
        t(f"len {n:4d} → {len(packed):4d} packed → {len(roundtrip):4d} raw",
          roundtrip == raw)
    # Specific edge: high bits all set.
    raw = bytes([0xFF] * 7)
    packed = pack78(raw)
    t("0xFF×7 → mask=0x7F + 7×0x7F", packed == bytes([0x7F] + [0x7F] * 7))


# ── CRC8 ATM (poly 0x07, init 0x00) ──────────────────────────────────────

def test_crc8() -> None:
    print("crc8 (CRC-8/ATM, poly 0x07, init 0x00):")
    t("crc8(b'') == 0x00",                      crc8(b"") == 0x00)
    t("crc8(b'123456789') == 0xF4 (check)",     crc8(b"123456789") == 0xF4)
    t("crc8(b'\\x00') == 0x00",                  crc8(b"\x00") == 0x00)
    t("crc8(b'\\x01') == 0x07 (poly)",           crc8(b"\x01") == 0x07)


# ── Frame round-trip ─────────────────────────────────────────────────────

def test_frames() -> None:
    print("build_frame / parse_frame:")
    for op, payload in [
        (OP_INFO, b""),
        (OP_READ, bytes([0x00, 0x00, 0x04, 0x00, 0x00, 0x10])),
        (OP_HASH_RANGE,
         bytes([0x00, 0x00, 0x04, 0x00,    # addr 0x40000
                0xB0, 0xFF, 0x00, 0x00,    # len 0xFFB0
                0x00, 0xB0, 0x02, 0x00,    # sectorbuf 0x2B000
                0x01])),                    # flags PER_SECTOR
        (OP_WRITE_AAI, bytes([0x00, 0x00, 0x04, 0x00]) + bytes(range(256))),
    ]:
        for seq in (1, 0x42, 0x7F):
            f = build_frame(op, seq, payload)
            t(f"op={op:#04x} seq={seq:#04x} len={len(payload):3d} "
              f"→ frame len {len(f):3d}: F0…F7",
              f[0] == 0xF0 and f[-1] == 0xF7 and f[1] == MFR_ID)
            ro, rs, rp = parse_frame(f)
            t(f"  round-trip op/seq/payload",
              ro == op and rs == seq and rp == payload)


def test_frame_crc_corruption() -> None:
    print("frame CRC integrity:")
    f = bytearray(build_frame(OP_INFO, 1, b""))
    f[2] ^= 0x01    # flip a bit in op
    try:
        parse_frame(bytes(f))
        t("corrupted frame should have raised", False)
    except ValueError as e:
        t("corrupted frame rejected", "CRC" in str(e),
          detail=f"raised: {e}")


# ── USB-MIDI 1.0 packet framing ──────────────────────────────────────────

def test_usbmidi() -> None:
    print("USB-MIDI 1.0 packet framing:")
    for n in (6, 7, 8, 9, 14, 15, 16, 100, 300, 600):
        sysex = bytes([0xF0, MFR_ID]) + bytes((i & 0x7F) for i in range(n - 3)) + b"\x40\xF7"
        # Note: bytes 4..N-2 should be MIDI-safe (≤ 0x7F) for a real
        # transmission; the framing layer doesn't care, so we just feed
        # representative data and confirm round-trip.
        # Mask high bit on the data bytes to keep MIDI-clean.
        sysex = bytes(b & 0x7F if 1 < i < len(sysex) - 1 else b
                      for i, b in enumerate(sysex))
        packets = sysex_to_usbmidi(sysex)
        t(f"sysex {len(sysex):3d} B → {len(packets):3d} B "
          f"({len(packets) // 4} packets, all 4-aligned)",
          len(packets) % 4 == 0)
        recovered = usbmidi_to_sysex(packets)
        t(f"  round-trip preserves message", recovered == [sysex])


# ── sector_diff against a mock device ────────────────────────────────────

class MockDevice:
    """Minimum viable Client for batch_diff: returns the CRC of an
    in-memory blob, with optional injected mismatches."""
    def __init__(self, blob: bytes):
        self.blob = blob

    def device_crc32_batch(self, addr: int, length: int,
                           sector_buf_addr: int = 0):
        whole = zlib_rom_crc32(self.blob[addr:addr + length])
        sec_crcs = []
        for s in range(0, length, SECTOR_SIZE):
            sec_crcs.append(
                zlib_rom_crc32(self.blob[addr + s:addr + s + SECTOR_SIZE]))
        return whole, sec_crcs


def test_sector_diff() -> None:
    print("sector_diff against MockDevice:")
    ref = bytes((i & 0xFF) for i in range(8 * SECTOR_SIZE))   # 32 KB
    dev = MockDevice(ref)
    match, diff = batch_diff(dev, ref, quiet=True)
    t("identical blob → match=True, diff=∅", match and diff == set())

    # Mutate one byte in sector 3, two bytes in sector 5.
    mutated = bytearray(ref)
    mutated[3 * SECTOR_SIZE + 100] ^= 0xFF
    mutated[5 * SECTOR_SIZE + 0]   ^= 0xFF
    mutated[5 * SECTOR_SIZE + 1]   ^= 0xFF
    dev_mut = MockDevice(bytes(mutated))
    match, diff = batch_diff(dev_mut, ref, quiet=True)
    t("two-sector diff → match=False, diff={3,5}",
      not match and diff == {3 * SECTOR_SIZE, 5 * SECTOR_SIZE})

    # filter_ops_by_diff
    ops = [
        ("sector", 0,                SECTOR_SIZE),
        ("sector", 3 * SECTOR_SIZE,  SECTOR_SIZE),
        ("block",  4 * SECTOR_SIZE,  4 * SECTOR_SIZE),  # spans 4..7
        ("sector", 7 * SECTOR_SIZE,  SECTOR_SIZE),
    ]
    filtered = filter_ops_by_diff(ops, diff)
    t("filter_ops keeps only ops touching changed sectors",
      ("sector", 0, SECTOR_SIZE)              not in filtered and
      ("sector", 3 * SECTOR_SIZE, SECTOR_SIZE)    in filtered and
      ("block",  4 * SECTOR_SIZE, 4 * SECTOR_SIZE) in filtered and
      ("sector", 7 * SECTOR_SIZE, SECTOR_SIZE) not in filtered,
      detail=f"got {filtered}")


# ── Reply parsing edge cases ─────────────────────────────────────────────

def test_replies() -> None:
    print("reply parsing:")
    # Success reply: op | 0x40, body = 16-byte INFO.
    info_body = bytes([
        0x41, 0x25, 0xBF, 0x00,  # JEDEC LE
        0x00, 0x10, 0x00, 0x00,  # sector 0x1000
        0x00, 0x00, 0x20, 0x00,  # total 0x200000
        0x31, 0x44, 0x46, 0x4D,  # version 'MFD1'
    ])
    f = build_frame(OP_INFO | REPLY_OK_BIT, 0x42, info_body)
    op, seq, payload = parse_frame(f)
    t("INFO reply parses",
      op == (OP_INFO | REPLY_OK_BIT) and seq == 0x42 and payload == info_body)


# ── Run all ─────────────────────────────────────────────────────────────

def main() -> int:
    tests = [
        test_pack78,
        test_crc8,
        test_frames,
        test_frame_crc_corruption,
        test_usbmidi,
        test_sector_diff,
        test_replies,
    ]
    for fn in tests:
        try:
            fn()
        except SystemExit:
            print(f"  FAILED in {fn.__name__}")
            return 1
        except Exception as e:
            print(f"  EXCEPTION in {fn.__name__}: {e}")
            return 1
        print()
    print("All self-tests passed.")
    return 0


if __name__ == "__main__":
    sys.exit(main())
