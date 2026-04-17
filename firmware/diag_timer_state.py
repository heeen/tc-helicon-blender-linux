#!/usr/bin/env python3
"""Fresh-halt the device, sample Timer0 / periphs, resume — repeat.

Purpose: detect whether Timer0 is actively counting in various states
(stock firmware running, after JTAG halt, after reboot-style teardown).
"""
from __future__ import annotations

import socket
import subprocess
import sys
import time
from pathlib import Path

FIRMWARE_DIR = Path(__file__).parent
OPENOCD_CFG = FIRMWARE_DIR.parent / "jtag" / "miolink-dice3-openocd.cfg"


class OpenOCD:
    def __init__(self, speed=400):
        subprocess.run(["pkill", "-x", "openocd"], capture_output=True)
        time.sleep(1)
        self.proc = subprocess.Popen(
            ["openocd", "-f", str(OPENOCD_CFG)],
            stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL,
        )
        time.sleep(3)
        self.sock = socket.create_connection(("localhost", 6666), timeout=30)
        self.sock.settimeout(30)
        self.cmd(f"adapter speed {speed}")

    def cmd(self, c, timeout=30):
        self.sock.settimeout(timeout)
        self.sock.sendall((c + "\x1a").encode())
        buf = b""
        while b"\x1a" not in buf:
            buf += self.sock.recv(4096)
        return buf.decode().strip("\x1a").strip()

    def close(self):
        try:
            self.cmd("resume")
        except Exception:
            pass
        self.sock.close()
        self.proc.terminate()
        self.proc.wait(timeout=5)

    def mdw(self, a):
        r = self.cmd(f"mdw {a:#x}")
        return int(r.split()[-1], 16)


def sample_timer(o, label):
    a = o.mdw(0xC2000004)
    b = o.mdw(0xC2000004)
    c = o.mdw(0xC2000004)
    reload_v = o.mdw(0xC2000000)
    ctrl_8 = o.mdw(0xC2000008)
    ctrl_c = o.mdw(0xC200000C)
    running = (a != b) or (b != c)
    print(f"{label}: COUNT samples {a:#06x}, {b:#06x}, {c:#06x}  "
          f"RELOAD={reload_v:#x}  +08={ctrl_8:#x}  +0C={ctrl_c:#x}  "
          f"RUNNING={running}")


def main():
    o = OpenOCD(speed=400)
    try:
        # State 1: as we attach — device was running stock firmware
        o.cmd("halt")
        sample_timer(o, "fresh-halt (stock fw)")

        # State 2: resume so stock fw runs, halt again after a moment
        o.cmd("resume")
        time.sleep(0.5)
        o.cmd("halt")
        sample_timer(o, "after brief resume")

        # State 3: disable interrupts (CPSR I/F set) — timer should still tick
        o.cmd("reg cpsr 0xd3")
        sample_timer(o, "cpsr=0xd3 (irq off), halted")
        o.cmd("resume")
        time.sleep(0.3)
        o.cmd("halt")
        sample_timer(o, "after resume w/ cpsr=0xd3")

        # State 4: try enabling timer ctrl bit
        for val in (1, 0x80, 0x81):
            o.cmd(f"mww 0xC2000008 {val:#x}")
            time.sleep(0.05)
            sample_timer(o, f"after +08={val:#x}")

        # State 5: after resume with ctrl=1
        o.cmd("mww 0xC2000008 0x1")
        o.cmd("resume")
        time.sleep(0.3)
        o.cmd("halt")
        sample_timer(o, "resume with +08=1")

    finally:
        o.close()


if __name__ == "__main__":
    main()
