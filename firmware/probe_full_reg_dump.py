#!/usr/bin/env python3
"""Full peripheral register dump via JTAG. Writes to a markdown file
tagged with the current state (tcat-boot | ecos)."""

import argparse
import socket
import subprocess
import sys
import time
from datetime import datetime
from pathlib import Path

sys.path.insert(0, str(Path(__file__).parent))
from jtag_flash_v2 import OpenOCD  # noqa: E402


# Peripheral blocks to dump. (name, base, count_words, stride_bytes)
BLOCKS = [
    ("Clock block",                 0xC9000000,  32, 4),
    ("Flash SPI controller",        0xCC000000,  32, 4),
    ("LED SPI controller",          0xCF000000,  32, 4),
    ("DMA engine base",             0x80000000,  16, 4),
    ("DMA channel 0",               0x80000100,   8, 4),
    ("DMA channel 1",               0x80000120,   8, 4),
    ("DMA channel 2",               0x80000140,   8, 4),
    ("DMA channel 3",               0x80000160,   8, 4),
    ("Timer 0",                     0xC2000000,   4, 4),
    ("Timer 1",                     0xC2000020,   4, 4),
    ("Mixer / crossbar",            0xC4000000,  16, 4),
    ("UART",                        0xC5000000,  16, 4),
    ("GPIO (LED_GPIO)",             0xCB000000,  16, 4),
    ("USB ChipIdea",                0x90000000,  16, 4),
    ("USB ChipIdea (op regs)",      0x90000140,  16, 4),
    ("TCAT USB extension",          0x90000800,  16, 4),
    ("VIC base",                    0xFFFFF000,  16, 4),
    ("VIC vector addresses",        0xFFFFF100,  16, 4),
    ("VIC vector controls",         0xFFFFF200,  16, 4),
]


def dump(ocd, label):
    lines = [f"# Peripheral register dump — {label}",
             f"",
             f"Date: {datetime.now().isoformat()}",
             f""]
    try:
        pc = ocd.cmd("reg pc")
        lines.append(f"- **CPU PC**: `{pc.strip()}`")
    except Exception as e:
        lines.append(f"- CPU PC: err {e}")

    for r in ("r0", "r1", "r2", "r3", "lr", "sp", "cpsr"):
        try:
            v = ocd.cmd(f"reg {r}")
            lines.append(f"- {r}: `{v.strip()}`")
        except Exception:
            pass

    # CP15 — cache/MMU state
    cp15_lines = ["", "## CP15 (cache / MMU)", ""]
    cp15 = [
        ("ID code",        "mrc 15 0 0 0 0"),
        ("Cache type",     "mrc 15 0 0 0 1"),
        ("TCM status",     "mrc 15 0 0 0 2"),
        ("Control (SCTLR)","mrc 15 0 1 0 0"),
        ("TTBR",           "mrc 15 0 2 0 0"),
        ("DACR",           "mrc 15 0 3 0 0"),
    ]
    for label, cmd in cp15:
        try:
            r = ocd.cmd(f"arm {cmd}")
            cp15_lines.append(f"- **{label}**: `{r.strip()}`")
        except Exception as e:
            cp15_lines.append(f"- {label}: ERR {e}")
    lines.extend(cp15_lines)
    lines.append("")

    for name, base, n, stride in BLOCKS:
        lines.append(f"## {name} — 0x{base:08X}")
        lines.append("")
        lines.append("| offset | addr | value |")
        lines.append("|---:|---|---|")
        for i in range(n):
            addr = base + i * stride
            try:
                v = ocd.mdw(addr)
                lines.append(f"| +0x{i*stride:02X} | 0x{addr:08X} | 0x{v:08X} |")
            except Exception as e:
                lines.append(f"| +0x{i*stride:02X} | 0x{addr:08X} | ERR {e} |")
        lines.append("")
    return "\n".join(lines)


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("label", help="tcat-boot or ecos — tags the output file")
    ap.add_argument("--out", help="output path (default: firmware/register_state_LABEL.md)")
    args = ap.parse_args()

    ocd = OpenOCD(speed=1000)
    print(f"Halting target (label={args.label})…")
    ocd.halt()
    text = dump(ocd, args.label)
    ocd.resume()

    path = Path(args.out) if args.out else Path(
        f"firmware/register_state_{args.label}.md")
    path.write_text(text)
    print(f"Wrote {path} ({len(text)} bytes)")


if __name__ == "__main__":
    main()
