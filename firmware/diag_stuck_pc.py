#!/usr/bin/env python3
"""Attach to a running-stuck driver, halt, and dump PC + key state.

Assumes driver was loaded and started by a prior invocation (e.g. jtag_flash_fast.py
is still hanging). Run alongside to inspect where the CPU is stuck.
"""
from __future__ import annotations

import socket
import subprocess
import sys
import time
from pathlib import Path

FIRMWARE_DIR = Path(__file__).parent
OPENOCD_CFG = FIRMWARE_DIR.parent / "jtag" / "miolink-dice3-openocd.cfg"
MAILBOX = 0x2E000


def run_oocd(cmd_list):
    subprocess.run(["pkill", "-x", "openocd"], capture_output=True)
    time.sleep(1)
    proc = subprocess.Popen(
        ["openocd", "-f", str(OPENOCD_CFG)],
        stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL,
    )
    time.sleep(3)
    try:
        sock = socket.create_connection(("localhost", 6666), timeout=30)
        sock.settimeout(30)
        for c in cmd_list:
            sock.sendall((c + "\x1a").encode())
            buf = b""
            while b"\x1a" not in buf:
                buf += sock.recv(4096)
            print(f"> {c}")
            print(buf.decode().strip("\x1a").strip())
            print("---")
    finally:
        try:
            sock.close()
        except Exception:
            pass
        proc.terminate()
        proc.wait(timeout=5)


if __name__ == "__main__":
    speed = 400 if len(sys.argv) < 2 else int(sys.argv[1])
    run_oocd([
        f"adapter speed {speed}",
        "halt",
        "reg",
        f"mdw {MAILBOX:#x} 8",
        "mdw 0x2E07C",
        "mdw 0x2E080 32",
        # SPI status
        "mdw 0xCC000028",
        "mdw 0xCC000008",
        "mdw 0xCC000010",
        # DMA state
        "mdw 0x80000014",
        "mdw 0x80000108",
        "mdw 0x8000010C",
        "mdw 0x80000110",
        # Timer state
        "mdw 0xC2000000",
        "mdw 0xC2000004",
        "resume",
    ])
