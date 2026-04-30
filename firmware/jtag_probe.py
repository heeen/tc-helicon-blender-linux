#!/usr/bin/env python3
"""Probe DICE3 SRAM via OpenOCD TCL.

Reads counters declared as static volatile in firmware/patch/handlers.c by
looking up their addresses in firmware/patch/hooks.elf via `nm`. Survives
rebuilds without manual address edits.
"""

import socket
import subprocess
import sys
import time
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
CFG = ROOT / "jtag" / "miolink-dice3-openocd.cfg"
ELF = ROOT / "firmware" / "patch" / "hooks.elf"
SYMS_LD = ROOT / "firmware" / "firmware_symbols.ld"

COUNTER_NAMES = [
    "loop_count",
    "ep3_xfer_compl",
    "ep3_active_pokes",
    "ep3_out_disabled",
    "ep3_rx_primed",
    "ep3_rx_enabled",
    "ep3_dcfg_fixups",
    "ep3_rxflvl_seen",
    "ep3_rxflvl_last",
    "ep3_last_q0",
    "ep3_out_trace_calls",
    "ep3_out_trace_installed",
    "ep3_cmp_calls",
    "ep3_cmp_installed",
    "ep3_q2_changes",
    "ep3_q2_last",
    "ep3_cmps_seen",
    "ep3_cf_clears",
    "ep3_cf_last",
    "ep3_force_rearms",
    "ep3_stuck_loops",
    "ep3_hs_arms",
    "ep3_hs_rearms",
]

# DWC2 USB
USB_BASE = 0x90000000
DOEPCTL3   = USB_BASE + 0xB00 + 3*0x20
DOEPTSIZ3  = USB_BASE + 0xB10 + 3*0x20
DOEPINT3   = USB_BASE + 0xB08 + 3*0x20
DAINT      = USB_BASE + 0x818
DAINTMSK   = USB_BASE + 0x81C
GINTSTS    = USB_BASE + 0x014
GINTMSK    = USB_BASE + 0x018
DSTS       = USB_BASE + 0x808
DCFG       = USB_BASE + 0x800


def load_symbols(elf):
    """Read nm output, return {name: addr_int}."""
    out = subprocess.run(["nm", str(elf)], capture_output=True, text=True, check=True).stdout
    syms = {}
    for line in out.splitlines():
        parts = line.split()
        if len(parts) >= 3 and parts[1].lower() in ("t", "b", "d", "r"):
            try:
                syms[parts[2]] = int(parts[0], 16)
            except ValueError:
                pass
    return syms


def parse_ld_symbols(ld_path):
    """Read firmware_symbols.ld 'name = 0xADDR;' lines."""
    syms = {}
    if not ld_path.exists():
        return syms
    for line in ld_path.read_text().splitlines():
        line = line.strip()
        if line.startswith("/*") or line.startswith("//") or "=" not in line:
            continue
        if not line.endswith(";"):
            continue
        try:
            name_part, addr_part = line[:-1].split("=", 1)
            name = name_part.strip()
            addr = addr_part.split(";")[0].split("/*")[0].strip()
            syms[name] = int(addr, 0)
        except (ValueError, IndexError):
            pass
    return syms


def cmd(sock, c, timeout=10):
    sock.settimeout(timeout)
    sock.sendall((c + "\x1a").encode())
    buf = b""
    while b"\x1a" not in buf:
        buf += sock.recv(4096)
    return buf.decode().strip("\x1a").strip()


def mdw(sock, addr):
    r = cmd(sock, f"mdw 0x{addr:x}")
    parts = r.split()
    if not parts:
        return None
    try:
        return int(parts[-1], 16)
    except ValueError:
        return None


def mdb_block(sock, addr, n):
    r = cmd(sock, f"mdb 0x{addr:x} {n}")
    out = []
    for line in r.split("\n"):
        parts = line.split(":", 1)
        if len(parts) != 2:
            continue
        for v in parts[1].split():
            try:
                out.append(int(v, 16))
            except ValueError:
                pass
    return out[:n]


def main():
    elf_syms = load_symbols(ELF)
    ld_syms = parse_ld_symbols(SYMS_LD)
    mixer_state = ld_syms.get("mixer_state", 0x2D81C)

    subprocess.run(["pkill", "-x", "openocd"], capture_output=True)
    time.sleep(1)
    proc = subprocess.Popen(
        ["openocd", "-f", str(CFG)],
        stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL,
    )
    time.sleep(3)
    try:
        s = socket.create_connection(("localhost", 6666), timeout=10)
        cmd(s, "adapter speed 1000")
        cmd(s, "halt")

        print("=== COUNTERS ===")
        for name in COUNTER_NAMES:
            addr = elf_syms.get(name)
            if addr is None:
                print(f"  {name:24s} (no symbol)")
                continue
            v = mdw(s, addr)
            print(f"  {name:24s} @ 0x{addr:05x} = 0x{v:08x} ({v})")

        print("\n=== USB DWC2 EP3 OUT ===")
        for name, addr in [
            ("DOEPCTL3",  DOEPCTL3),
            ("DOEPTSIZ3", DOEPTSIZ3),
            ("DOEPINT3",  DOEPINT3),
            ("DAINT",     DAINT),
            ("DAINTMSK",  DAINTMSK),
            ("GINTSTS",   GINTSTS),
            ("GINTMSK",   GINTMSK),
            ("DSTS",      DSTS),
            ("DCFG",      DCFG),
        ]:
            v = mdw(s, addr)
            print(f"  {name:10s} = 0x{v:08x}")

        print(f"\n=== MIXER_STATE 0x{mixer_state:x} [0..35] ===")
        bytes_ = mdb_block(s, mixer_state, 36)
        for i, b in enumerate(bytes_):
            print(f"  [{i:2d}] = 0x{b:02x}", end="\n" if i % 4 == 3 else "")

        cmd(s, "resume")
        s.close()
    finally:
        proc.terminate()
        try: proc.wait(timeout=5)
        except: pass


if __name__ == "__main__":
    main()
