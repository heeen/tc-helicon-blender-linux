#!/usr/bin/env python3
"""Upload reboot stub to SRAM and execute it via JTAG.

Usage:
    python3 firmware/jtag_reboot.py
"""

import socket
import subprocess
import sys
import time
from pathlib import Path

FIRMWARE_DIR = Path(__file__).parent
OPENOCD_CFG = FIRMWARE_DIR.parent / "jtag" / "miolink-dice3-openocd.cfg"
REBOOT_BIN = FIRMWARE_DIR / "reboot_stub.bin"
REBOOT_ADDR = 0x2B000


class OpenOCD:
    def __init__(self, speed=1000):
        subprocess.run(["pkill", "-x", "openocd"], capture_output=True)
        time.sleep(0.5)
        self.proc = subprocess.Popen(
            ["openocd", "-f", str(OPENOCD_CFG)],
            stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL,
        )
        time.sleep(3)
        self.sock = socket.create_connection(("localhost", 6666), timeout=30)
        self.cmd(f"adapter speed {speed}")

    def close(self):
        try:
            self.cmd("resume")
        except Exception:
            pass
        self.sock.close()
        self.proc.terminate()
        self.proc.wait(timeout=5)

    def cmd(self, c, timeout=30):
        self.sock.settimeout(timeout)
        self.sock.sendall((c + "\x1a").encode())
        buf = b""
        while b"\x1a" not in buf:
            buf += self.sock.recv(4096)
        return buf.decode().strip("\x1a").strip()

    def halt(self):
        self.cmd("halt")

    def resume(self):
        self.cmd("resume")

    def load_image(self, path, addr):
        self.cmd(f"load_image {path} {addr:#010x} bin")


def main():
    if not REBOOT_BIN.exists():
        print(f"ERROR: {REBOOT_BIN} not found. Build with:")
        print("  cd firmware && arm-none-eabi-gcc -march=armv5te -marm -Os -nostdlib "
              "-ffreestanding -Wl,-Ttext=0x2B000 -Wl,--entry=reboot_entry "
              "-o reboot_stub.elf reboot_stub.c && "
              "arm-none-eabi-objcopy -O binary reboot_stub.elf reboot_stub.bin")
        sys.exit(1)

    print(f"Reboot stub: {REBOOT_BIN} ({REBOOT_BIN.stat().st_size} bytes)")

    o = OpenOCD()
    try:
        print("Halting CPU...")
        o.halt()

        # Get entry point from ELF
        import struct as st
        with open(FIRMWARE_DIR / "reboot_stub.elf", "rb") as f:
            f.seek(0x18)  # e_entry offset in ELF32
            entry = st.unpack("<I", f.read(4))[0]
        print(f"Loading stub to {REBOOT_ADDR:#x}, entry at {entry:#x}")

        o.load_image(str(REBOOT_BIN), REBOOT_ADDR)

        # Invalidate I-cache so CPU fetches fresh code
        o.cmd("arm mcr 15 0 7 5 0 0")

        # Set entry point and SVC mode
        o.cmd(f"reg pc {entry:#x}")
        o.cmd("reg cpsr 0xd3")

        print("Resuming — watch UART for [reboot] messages...")
        o.resume()

    finally:
        o.close()


if __name__ == "__main__":
    main()
