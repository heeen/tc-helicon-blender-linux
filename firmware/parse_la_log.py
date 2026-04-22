#!/usr/bin/env python3
"""Parse a sigrok/PulseView SPI decoder log, align MOSI/MISO by
timestamp, and dump aligned transactions.

Input format per line:
    <start_ns>-<end_ns> SPI: (MOSI|MISO) transfers: <hex bytes>
(other lines — MOSI/MISO "bits: N", address-group boundaries, etc.
— are filtered out.)

Example usage:
    # Full dump (aligned MOSI/MISO byte-by-byte):
    python3 firmware/parse_la_log.py read-error-all.log.txt

    # Only show READ transactions (MOSI[0] == 0x03):
    python3 firmware/parse_la_log.py read-error-all.log.txt --reads

    # Only transactions where MISO length > 16 (skip RDSRs etc.):
    python3 firmware/parse_la_log.py read-error-all.log.txt --min-bytes 16

    # Dump one specific transaction by start timestamp:
    python3 firmware/parse_la_log.py read-error-all.log.txt --at 609360892
"""

import argparse
import re
import sys
from collections import defaultdict
from pathlib import Path

LINE_RE = re.compile(
    r"^(\d+)-(\d+)\s+SPI:\s+(MOSI|MISO)\s+transfers:\s+(.*)$"
)


def parse_hex_bytes(s):
    """Parse 'XX XX XX ...' into a list of ints."""
    return [int(b, 16) for b in s.strip().split()]


def load_transactions(path):
    """Return a list of (start, end, mosi_bytes, miso_bytes) tuples,
    sorted by start timestamp. MOSI and MISO are paired by start
    timestamp; either side may be missing (empty list)."""
    by_start = defaultdict(lambda: {"end": 0, "mosi": None, "miso": None})
    with open(path) as f:
        for line in f:
            m = LINE_RE.match(line)
            if not m:
                continue
            start, end, direction, hex_bytes = m.groups()
            start = int(start)
            end = int(end)
            bytes_list = parse_hex_bytes(hex_bytes)
            entry = by_start[start]
            entry["end"] = max(entry["end"], end)
            if direction == "MOSI":
                entry["mosi"] = bytes_list
            else:
                entry["miso"] = bytes_list

    out = []
    for start in sorted(by_start):
        e = by_start[start]
        out.append((start, e["end"],
                    e["mosi"] or [],
                    e["miso"] or []))
    return out


def dump_transaction(i, start, end, mosi, miso, *, indent="  ",
                     max_bytes=None, hide_padding=False):
    dt = end - start
    # Classify by MOSI cmd byte.
    cmd_name = "?"
    addr = None
    if mosi:
        op = mosi[0]
        cmd_name = {
            0x03: "READ",
            0x0B: "FAST_READ",
            0x02: "PROG",
            0x05: "RDSR",
            0x06: "WREN",
            0x04: "WRDI",
            0x01: "WRSR",
            0x20: "SE_4KB",
            0x52: "BE_32KB",
            0xD8: "BE_64KB",
            0xAD: "AAI",
            0x9F: "RDID",
            0xAB: "RDID_LEGACY",
            0x60: "CE",
        }.get(op, f"CMD_0x{op:02X}")
        if op in (0x03, 0x0B, 0x02, 0xAD, 0x20, 0x52, 0xD8) and len(mosi) >= 4:
            addr = (mosi[1] << 16) | (mosi[2] << 8) | mosi[3]

    length = max(len(mosi), len(miso))
    head = f"[#{i}] t={start:>14d} dt={dt:>6d}ns  {cmd_name}"
    if addr is not None:
        head += f" @0x{addr:06X}"
    head += f"  ({length} bytes)"
    print(head)

    # For the aligned hex table, optionally trim the "boring" tail of
    # 0x00-MOSI / 0xFF-MISO (or all-zero on both) — the shift is
    # usually visible in the first 32 or at a specific offset.
    limit = length if max_bytes is None else min(length, max_bytes)
    boring_start = length  # no trim by default
    if hide_padding:
        # Walk from the end; stop at first non-padding byte.
        i2 = length
        while i2 > 4:
            m = mosi[i2 - 1] if i2 - 1 < len(mosi) else None
            s = miso[i2 - 1] if i2 - 1 < len(miso) else None
            if (m in (None, 0x00)) and (s in (None, 0xFF, 0x00)):
                i2 -= 1
            else:
                break
        boring_start = i2
    shown = min(limit, boring_start)
    print(f"{indent}offset  MOSI  MISO")
    for k in range(shown):
        m = f"{mosi[k]:02X}" if k < len(mosi) else "--"
        s = f"{miso[k]:02X}" if k < len(miso) else "--"
        flag = ""
        if cmd_name == "READ" and k >= 4:
            # MOSI should be 0x00 during the data phase of a READ.
            if k < len(mosi) and mosi[k] != 0x00:
                flag += " ←MOSI≠0"
        print(f"{indent}  {k:5d}  {m}    {s}{flag}")
    if shown < length:
        hidden = length - shown
        print(f"{indent}  ... {hidden} more byte(s){'' if hide_padding else ''}")
    print()


def summarise(txns):
    # Quick stats
    reads = sum(1 for _, _, m, _ in txns if m and m[0] == 0x03)
    aai   = sum(1 for _, _, m, _ in txns if m and m[0] == 0xAD)
    rdsr  = sum(1 for _, _, m, _ in txns if m and m[0] == 0x05)
    other = len(txns) - reads - aai - rdsr
    total_bytes = sum(max(len(m), len(s)) for _, _, m, s in txns)
    print(f"# {len(txns)} transactions parsed")
    print(f"#   READ  (0x03): {reads}")
    print(f"#   AAI   (0xAD): {aai}")
    print(f"#   RDSR  (0x05): {rdsr}")
    print(f"#   other       : {other}")
    print(f"#   total bytes : {total_bytes}")
    print()


def main():
    ap = argparse.ArgumentParser(description=__doc__,
        formatter_class=argparse.RawDescriptionHelpFormatter)
    ap.add_argument("log", help="Logic-analyzer log (txt)")
    ap.add_argument("--reads", action="store_true",
                    help="Show only READ (0x03) transactions")
    ap.add_argument("--aai", action="store_true",
                    help="Show only AAI (0xAD) transactions")
    ap.add_argument("--min-bytes", type=int, default=0,
                    help="Only transactions where max(|MOSI|,|MISO|) ≥ N")
    ap.add_argument("--max-bytes", type=int, default=None,
                    help="Cap bytes printed per transaction (default: all)")
    ap.add_argument("--at", type=int, default=None,
                    help="Show only the transaction with this start timestamp")
    ap.add_argument("--from-idx", type=int, default=0,
                    help="Skip the first N transactions")
    ap.add_argument("--count", type=int, default=None,
                    help="Show at most N transactions")
    ap.add_argument("--hide-padding", action="store_true",
                    help="Trim trailing 00/FF padding from the hex table")
    ap.add_argument("--no-summary", action="store_true",
                    help="Skip the transaction-count summary header")
    ap.add_argument("--ref", default=None,
                    help="Reference SPI image for shift-detection. When set, "
                         "each READ transaction's MISO[4:] is compared against "
                         "ref[addr:addr+len] and flagged if it doesn't match "
                         "(or if got[0]==ref[N] hinting a +N byte shift).")
    ap.add_argument("--flag-only", action="store_true",
                    help="With --ref, only print transactions that mismatch.")
    args = ap.parse_args()

    path = Path(args.log)
    if not path.exists():
        sys.exit(f"File not found: {path}")

    ref_data = None
    if args.ref:
        ref_data = Path(args.ref).read_bytes()
        print(f"# Ref: {args.ref} ({len(ref_data)} bytes)", file=sys.stderr)

    print(f"# Parsing {path} ...", file=sys.stderr)
    txns = load_transactions(path)
    print(f"# Found {len(txns)} transactions", file=sys.stderr)

    # Filter
    filtered = []
    for t in txns:
        start, end, mosi, miso = t
        if args.at is not None and start != args.at:
            continue
        if args.reads and not (mosi and mosi[0] == 0x03):
            continue
        if args.aai and not (mosi and mosi[0] == 0xAD):
            continue
        if max(len(mosi), len(miso)) < args.min_bytes:
            continue
        filtered.append(t)

    # Tag each transaction with shift info (only meaningful if --ref).
    diagnostics = []
    for t in filtered:
        start, end, mosi, miso = t
        diag = {"mismatch": False, "shift": None, "bad_at": None,
                "exp": None, "got": None}
        if ref_data and mosi and mosi[0] == 0x03 and len(mosi) >= 4 and miso:
            addr = (mosi[1] << 16) | (mosi[2] << 8) | mosi[3]
            data = bytes(miso[4:])        # MISO bytes after cmd+addr
            want = ref_data[addr:addr + len(data)]
            # If reference is shorter, skip.
            if len(want) == len(data):
                if data != want:
                    diag["mismatch"] = True
                    # First differing byte
                    first = next((i for i in range(len(data))
                                  if data[i] != want[i]), None)
                    diag["bad_at"] = first
                    diag["got"] = data[first:first + 8].hex(" ")
                    diag["exp"] = want[first:first + 8].hex(" ")
                    # Classify shift: got[first] == want[first + N]?
                    for s in (1, 2, 3, 4, -1, -2, -3, -4):
                        idx = first + s
                        if 0 <= idx < len(want) and data[first] == want[idx]:
                            diag["shift"] = s
                            break
        diagnostics.append(diag)

    if not args.no_summary:
        summarise(filtered)
        if ref_data:
            n_mm = sum(1 for d in diagnostics if d["mismatch"])
            print(f"#   MISMATCHED   : {n_mm}")
            if n_mm:
                from collections import Counter
                shifts = Counter(d["shift"] for d in diagnostics
                                 if d["mismatch"])
                print(f"#   shift histogram: {dict(shifts)}")
            print()

    # Slice
    end_idx = len(filtered) if args.count is None \
        else args.from_idx + args.count
    view = list(zip(filtered, diagnostics))[args.from_idx:end_idx]

    for i, ((start, end, mosi, miso), diag) in enumerate(
            view, start=args.from_idx):
        if args.flag_only and not diag["mismatch"]:
            continue
        dump_transaction(i, start, end, mosi, miso,
                         max_bytes=args.max_bytes,
                         hide_padding=args.hide_padding)
        if diag["mismatch"]:
            shift_s = (f"shift={diag['shift']:+d}" if diag["shift"]
                       else "shift=unknown")
            print(f"  !!! MISMATCH at MISO[{diag['bad_at'] + 4}] ({shift_s})")
            print(f"      got: {diag['got']}")
            print(f"      exp: {diag['exp']}")
            print()


if __name__ == "__main__":
    main()
