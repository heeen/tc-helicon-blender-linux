"""Transport-agnostic sector-diff strategy for DICE3 flash updates.

Originally part of jtag_flash_v2.py — extracted 2026-05-05 so the new
MIDI SysEx host tool (firmware/midi_flash.py) can reuse the same logic
unchanged.

Both the v2 mailbox driver and the eCos-context SysEx handler implement
HASH_RANGE in batch mode: one device sweep produces (whole_crc,
[per-4KB-sector_crcs]) for the requested range. The host then:

  1. Compares whole_crc against locally-computed CRC of the reference
     image — if equal, the device is already up-to-date (no writes).
  2. Otherwise iterates the per-sector CRCs to identify exactly which
     4 KB sectors differ.
  3. Filters its op-plan down to ops that touch at least one differing
     sector.

Wire format expectations — the `Client` interface below abstracts the
two transports so both consumers share identical batch_diff /
filter_ops_by_diff code paths.
"""

from __future__ import annotations

import zlib
from typing import Protocol


# 4 KB SST25VF016B sector — same on both transports.
SECTOR_SIZE = 0x1000

# Default SRAM scratch for per-sector CRCs.
# v2 mailbox: V2_DATA_BUF / V2_SECTOR_BUF at 0x2B000 (one shared 4 KB slot).
# SysEx eCos handler: WRITE_BUF + 0x400 (= 0x2B400) — see
# firmware/patch/midi_sysex_filter.c. **MUST NOT alias UNPACK_BUF
# (0x2B000) or PACK_BUF (0x2B200)** because each subsequent SysEx
# request would overwrite the per-sector CRC array with its own
# request/reply payload before the host can drain it via MEM_READ.
DEFAULT_SECTOR_BUF_ADDR = 0x0002B400


def zlib_rom_crc32(buf: bytes) -> int:
    """Match the on-device ROM CRC32 step routine: poly 0xEDB88320,
    init=0, no final XOR. Equivalent to zlib.crc32 with init/finalize
    cancelled (zlib does ~init at start and ~result at end; passing
    0xFFFFFFFF for both inverts twice = identity)."""
    return zlib.crc32(buf, 0xFFFFFFFF) ^ 0xFFFFFFFF


class Client(Protocol):
    """Minimum interface a device transport must expose for sector-diff.

    Implementations exist in:
      - firmware/jtag_flash_v2.py     (FlashClientV2.device_crc32_batch)
      - firmware/midi_flash.py        (MidiFlashClient.device_crc32_batch)
    """

    def device_crc32_batch(
        self,
        addr: int,
        length: int,
        sector_buf_addr: int = DEFAULT_SECTOR_BUF_ADDR,
    ) -> tuple[int, list[int]]:
        """Compute device-side CRC32 over [addr, addr+length).

        Returns (whole_crc, per_sector_crcs). Per-sector list has one
        u32 entry per 4 KB sector covered by the range.
        """
        ...


def batch_diff(
    client: Client,
    ref: bytes,
    sector_buf_addr: int = DEFAULT_SECTOR_BUF_ADDR,
    *,
    quiet: bool = False,
) -> tuple[bool, set[int]]:
    """Single device sweep that returns:
      - whole-image match boolean
      - set of differing sector start addresses (empty if matched)

    Mirrors the bootloader's spi_dma_read_and_crc — one continuous
    DMA+CRC run over the whole image — but additionally writes per-sector
    CRCs to a host-readable SRAM buffer. Avoids the double-SPI-read of a
    separate whole-image-then-per-sector flow on the mismatch path.

    Per-sector CRCs are always compared (not just on whole-CRC mismatch).
    CRC32 is linear over GF(2) — two byte sequences differing only in a
    nullspace pattern produce identical whole-image CRCs while clearly
    differing per-sector. We hit this once with an 8-byte relocation diff
    that landed in the nullspace; rather than rely on the 1/2^32 odds of
    that recurring, do the per-sector compare unconditionally (same total
    CRC work since the device already returned every per-sector CRC).
    """
    import time
    t0 = time.monotonic()
    if not quiet:
        print(f"  batch CRC ({len(ref)} bytes, "
              f"{len(ref) // SECTOR_SIZE} sectors)...")
    whole_local = zlib_rom_crc32(ref)
    whole_dev, sec_crcs = client.device_crc32_batch(0, len(ref), sector_buf_addr)
    dt = time.monotonic() - t0
    diff: set[int] = set()
    for i, dev_crc in enumerate(sec_crcs):
        addr = i * SECTOR_SIZE
        local = zlib_rom_crc32(ref[addr:addr + SECTOR_SIZE])
        if local != dev_crc:
            diff.add(addr)
    if not diff:
        if not quiet:
            print(f"  whole-image CRC match (0x{whole_local:08x}) in {dt:.1f}s")
        return True, set()
    if not quiet:
        whole_relation = ("MATCH but per-sector diff (CRC32 nullspace)"
                          if whole_dev == whole_local
                          else "mismatch")
        print(f"  whole-image {whole_relation} "
              f"(local=0x{whole_local:08x}, device=0x{whole_dev:08x}); "
              f"{len(diff)} sectors differ — sweep took {dt:.1f}s")
    return False, diff


def filter_ops_by_diff(
    ops: list[tuple],
    diff: set[int],
    sector_size: int = SECTOR_SIZE,
) -> list[tuple]:
    """Drop ops whose constituent sectors are all already-correct on
    device. Preserves the density heuristic on top of the diff set:
    a dense block op stays a single block op when any sector inside it
    differs; an unchanged block op vanishes entirely.

    `ops` items are (kind:str, addr:int, span:int). Result is the same
    shape, filtered.
    """
    out = []
    for op in ops:
        kind, addr, span = op[0], op[1], op[2]
        sectors_in_op = range(addr, addr + span, sector_size)
        if any(s in diff for s in sectors_in_op):
            out.append(op)
    return out
