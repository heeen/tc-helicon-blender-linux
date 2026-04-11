#!/usr/bin/env python3
"""JTAG flash writer with on-device verify — single OpenOCD session.

Loads the SRAM flash driver once, then iterates sectors: erase → write → verify.
Verify is done ON-DEVICE (CPU reads flash back and compares against SRAM buffer),
not via JTAG reads, so it's immune to JTAG signal quality issues.

Usage:
    python3 firmware/jtag_flash_verified.py
    python3 firmware/jtag_flash_verified.py --ref firmware/blender_spi_patched.bin
    python3 firmware/jtag_flash_verified.py --start 17    # resume from sector index
"""

import argparse
import os
import socket
import struct
import subprocess
import sys
import time
from pathlib import Path

FIRMWARE_DIR = Path(__file__).parent
OPENOCD_CFG = FIRMWARE_DIR.parent / "jtag" / "miolink-dice3-openocd.cfg"
DRIVER_BIN = FIRMWARE_DIR / "sram_flash_driver.bin"

DATA_BUF = 0x2B000
DRIVER_CODE = 0x2C000
MAILBOX = 0x2E000

CMD_ERASE = 1
CMD_WRITE = 2
CMD_READ = 3
CMD_VERIFY = 4
CMD_BP_CLEAR = 5

ST_IDLE = 0
ST_RUNNING = 1
ST_DONE_OK = 2
ST_ERROR = 0xFF


class OpenOCD:
    def __init__(self, speed=1000):
        self.speed = speed
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

    def mww(self, addr, val):
        self.cmd(f"mww {addr:#x} {val:#x}")

    def mdw(self, addr):
        r = self.cmd(f"mdw {addr:#x}")
        return int(r.split()[-1], 16)

    def load_image(self, path, addr):
        self.cmd(f"load_image {path} {addr:#x} bin")


def poll_status(o, label, timeout_s=30):
    """Poll mailbox status until done, running on-device."""
    deadline = time.monotonic() + timeout_s
    while time.monotonic() < deadline:
        time.sleep(0.2)
        o.halt()
        status = o.mdw(MAILBOX)
        if status == ST_DONE_OK:
            o.resume()
            return True
        if status == ST_ERROR:
            errors = o.mdw(MAILBOX + 16)
            print(f"{label}: ERROR (errors={errors})")
            o.resume()
            return False
        # ST_IDLE = driver hasn't picked up command yet
        # ST_RUNNING = driver is executing command
        # Either way, keep polling
        o.resume()
    print(f"{label}: TIMEOUT")
    return False


def send_cmd(o, cmd, spi_addr=0, timeout_s=30, label="cmd"):
    """Send mailbox command and poll until done."""
    o.halt()
    time.sleep(0.01)  # let halt settle
    o.mww(MAILBOX + 0, 0)       # status = idle
    o.mww(MAILBOX + 8, spi_addr)  # spi_addr
    o.mww(MAILBOX + 12, 0)      # progress
    o.mww(MAILBOX + 16, 0)      # errors
    o.mww(MAILBOX + 4, cmd)     # command — write LAST (driver polls this)
    o.resume()
    time.sleep(0.1)  # let driver pick up command before first poll
    return poll_status(o, label, timeout_s)


def init_driver(o):
    """Load driver, set PC, resume into command loop."""
    o.halt()

    # Disable USB to prevent its DMA activity corrupting SRAM.
    # Do NOT disable the DMA engine or SPI — the flash driver needs them.
    o.mww(0x90000008, 0)           # USB stop
    o.mww(0x90000014, 0xFFFFFFFF)  # USB flush all endpoints

    o.load_image(str(DRIVER_BIN), DRIVER_CODE)

    # Clear mailbox
    for i in range(8):
        o.mww(MAILBOX + i * 4, 0)

    # Invalidate caches
    o.cmd("arm mcr 15 0 7 5 0 0")
    o.cmd("arm mcr 15 0 7 6 0 0")

    # Set PC to driver entry, SVC mode
    o.cmd(f"reg pc {DRIVER_CODE:#x}")
    o.cmd("reg cpsr 0xd3")
    o.resume()
    time.sleep(0.3)

    # Verify driver is in idle loop
    o.halt()
    status = o.mdw(MAILBOX)
    pc = o.cmd("reg pc")
    if status != ST_IDLE:
        print(f"WARNING: driver status={status} after init (expected 0), {pc}")
    else:
        print(f"Driver initialized (idle). {pc}")

    # Verify mailbox address is correct in binary
    mb_cmd = o.mdw(MAILBOX + 4)
    print(f"  Mailbox at {MAILBOX:#x}, command={mb_cmd}")
    o.resume()


def main():
    p = argparse.ArgumentParser()
    p.add_argument("--ref", default=str(FIRMWARE_DIR / "blender_spi_flash_restored.bin"))
    p.add_argument("--start", type=int, default=0)
    p.add_argument("--speed", type=int, default=1000)
    p.add_argument("--retries", type=int, default=3)
    args = p.parse_args()

    spi = Path(args.ref).read_bytes()

    # Find non-empty sectors
    sectors = []
    for addr in range(0, len(spi), 0x1000):
        sec = spi[addr:addr + 0x1000]
        non_ff = sum(1 for b in sec if b != 0xFF)
        if non_ff > 0:
            sectors.append((addr, non_ff))

    print(f"Reference: {args.ref} ({len(spi)} bytes, {len(sectors)} non-empty sectors)")
    print(f"Speed: {args.speed} kHz")

    subprocess.run(["pkill", "-x", "openocd"], capture_output=True)
    time.sleep(1)

    o = OpenOCD(speed=args.speed)

    try:
        init_driver(o)

        # Clear block protection once
        print("Clearing block protection...")
        if not send_cmd(o, CMD_BP_CLEAR, timeout_s=30, label="BP_CLEAR"):
            print("BP_CLEAR failed — continuing anyway")

        ok = 0
        fail = 0

        for idx, (addr, non_ff) in enumerate(sectors):
            if idx < args.start:
                continue

            sector_data = spi[addr:addr + 0x1000]

            for attempt in range(args.retries):
                label = f"[{idx+1}/{len(sectors)}] {addr:#07x}"
                if attempt:
                    label += f" retry {attempt+1}"
                print(label, end=" ", flush=True)

                try:
                    # Load sector data
                    tmp = f"/tmp/jtag_sector_{addr:05x}.bin"
                    with open(tmp, "wb") as f:
                        f.write(sector_data)
                    o.halt()
                    o.load_image(tmp, DATA_BUF)
                    o.resume()
                    os.unlink(tmp)

                    # Erase
                    if not send_cmd(o, CMD_ERASE, addr, timeout_s=5, label="erase"):
                        print("ERASE FAIL")
                        continue

                    # Write
                    if not send_cmd(o, CMD_WRITE, addr, timeout_s=10, label="write"):
                        print("WRITE FAIL")
                        continue

                    # Verify (on-device: reads flash, compares against DATA_BUF)
                    if not send_cmd(o, CMD_VERIFY, addr, timeout_s=10, label="verify"):
                        o.halt()
                        errs = o.mdw(MAILBOX + 16)
                        o.resume()
                        print(f"VERIFY FAIL ({errs} mismatches)")
                        continue

                    print("OK")
                    ok += 1
                    break

                except Exception as e:
                    print(f"ERROR: {e}")
                    # Try reinitializing driver
                    try:
                        init_driver(o)
                    except Exception:
                        pass
            else:
                fail += 1
                print(f"  FAILED after {args.retries} attempts")

        print(f"\nDone: {ok} OK, {fail} failed out of {len(sectors)} sectors")
        if fail == 0:
            print("SUCCESS — power cycle the device now.")
        else:
            print(f"WARNING: {fail} sectors failed.")

    finally:
        o.close()


if __name__ == "__main__":
    main()
