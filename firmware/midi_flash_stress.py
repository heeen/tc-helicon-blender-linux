#!/usr/bin/env python3
"""Stress-test the MIDI SysEx flash path.

Runs `midi_flash update` against a reference image N times in a row,
counts failures and tracks per-iteration timing. Mirrors the
shift-repro harness we used against the bare-metal v2 driver to compare
miss rates apples-to-apples.

Usage:
    python3 firmware/midi_flash_stress.py --ref blender_spi_patched.bin --iters 100

Exit status:
    0 — all iterations matched on second batch_diff (whole-image CRC OK)
    1 — at least one iteration failed verification
    2 — harness error (USB / device unreachable)
"""

from __future__ import annotations

import argparse
import statistics
import sys
import time
from pathlib import Path

HERE = Path(__file__).resolve().parent
sys.path.insert(0, str(HERE))

from sector_diff import batch_diff
from midi_flash import MidiUsb, MidiFlashClient


def stress(ref_path: Path, iters: int) -> int:
    ref = ref_path.read_bytes()
    print(f"Reference: {ref_path} ({len(ref)} bytes)")
    print(f"Iterations: {iters}")
    print()

    usb = MidiUsb()
    fc = MidiFlashClient(usb)
    try:
        # Verify the device speaks our SysEx protocol.
        info = fc.info()
        print(f"Device JEDEC: {info['jedec']:#08x}, ver {info['version']:#x}")
        if info["jedec"] != 0x00BF2541:
            print(f"  WARNING: unexpected JEDEC, expected 0x00BF2541")

        per_iter_seconds: list[float] = []
        failures = 0
        miss_sectors_total = 0

        for i in range(iters):
            t0 = time.monotonic()
            match, diff = batch_diff(fc, ref, quiet=True)
            if match:
                # Already up to date — no work needed; this is a success
                # and a good baseline.
                dt = time.monotonic() - t0
                per_iter_seconds.append(dt)
                print(f"[{i+1:3d}/{iters}] match  ({dt:5.1f}s)")
                continue

            # Some sectors differ — flash them, then re-verify.
            for sector_addr in sorted(diff):
                sector = ref[sector_addr:sector_addr + 0x1000]
                fc.erase_sector(sector_addr, 1)
                for off in range(0, len(sector), 256):
                    fc.write_aai(sector_addr + off, sector[off:off + 256])

            re_match, re_diff = batch_diff(fc, ref, quiet=True)
            dt = time.monotonic() - t0
            per_iter_seconds.append(dt)
            if re_match:
                print(f"[{i+1:3d}/{iters}] flashed {len(diff):3d}  ({dt:5.1f}s) ✓")
            else:
                failures += 1
                miss_sectors_total += len(re_diff)
                print(f"[{i+1:3d}/{iters}] flashed {len(diff):3d}  ({dt:5.1f}s) "
                      f"VERIFY FAILED — {len(re_diff)} sectors still differ")

        print()
        print(f"=== summary: {failures}/{iters} iterations failed ===")
        if per_iter_seconds:
            print(f"  median: {statistics.median(per_iter_seconds):5.2f}s")
            print(f"  mean:   {statistics.mean(per_iter_seconds):5.2f}s")
            print(f"  min:    {min(per_iter_seconds):5.2f}s")
            print(f"  max:    {max(per_iter_seconds):5.2f}s")
        if failures:
            print(f"  total residual mismatched sectors: {miss_sectors_total}")
        return 0 if failures == 0 else 1
    finally:
        usb.close()


def main(argv: list[str] | None = None) -> int:
    p = argparse.ArgumentParser(description=__doc__,
                                formatter_class=argparse.RawDescriptionHelpFormatter)
    p.add_argument("--ref", required=True, type=Path)
    p.add_argument("--iters", type=int, default=100)
    args = p.parse_args(argv)
    if not args.ref.exists():
        print(f"ERROR: --ref {args.ref} not found", file=sys.stderr)
        return 2
    try:
        return stress(args.ref, args.iters)
    except Exception as e:
        print(f"harness error: {e}", file=sys.stderr)
        return 2


if __name__ == "__main__":
    sys.exit(main())
