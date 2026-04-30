#!/usr/bin/env python3
"""Capture Blender UART, then run JTAG soft-reboot (reboot_stub), then capture more.

Close any other serial user (tio/minicom) first — only one client can open the port.

Usage:
  python3 firmware/uart_capture_then_reboot.py
  python3 firmware/uart_capture_then_reboot.py --port /dev/ttyUSB0 --pre 5 --post 20
  python3 firmware/uart_capture_then_reboot.py -o /tmp/uart_reboot.log --no-reboot
"""

from __future__ import annotations

import argparse
import glob
import os
import subprocess
import sys
import threading
import time
from pathlib import Path

try:
    import serial
except ImportError:
    print("Install pyserial: pip install pyserial", file=sys.stderr)
    sys.exit(1)

FIRMWARE_DIR = Path(__file__).resolve().parent


def find_ftdi_port() -> str | None:
    for tty in sorted(glob.glob("/dev/ttyUSB*")):
        dev = os.path.basename(tty)
        syspath = os.path.realpath(f"/sys/class/tty/{dev}/device")
        while syspath and syspath != "/":
            vid_path = os.path.join(syspath, "idVendor")
            if os.path.exists(vid_path):
                vid = open(vid_path).read().strip()
                pid = open(os.path.join(syspath, "idProduct")).read().strip()
                if vid == "0403" and pid == "6001":
                    return tty
                break
            syspath = os.path.dirname(syspath)
    return None


def reader_thread(
    ser: serial.Serial,
    buf: bytearray,
    stop: threading.Event,
) -> None:
    while not stop.is_set():
        try:
            chunk = ser.read(512)
            if chunk:
                buf.extend(chunk)
                try:
                    sys.stdout.buffer.write(chunk)
                except Exception:
                    sys.stdout.write(chunk.decode("utf-8", errors="replace"))
                sys.stdout.flush()
        except Exception:
            break


def main() -> int:
    p = argparse.ArgumentParser(description="UART capture → JTAG reboot → more capture")
    p.add_argument("--port", default=None, help="Serial device (default: FTDI ttyUSB*)")
    p.add_argument("--baud", type=int, default=115200)
    p.add_argument(
        "--pre",
        type=float,
        default=3.0,
        help="Seconds to capture before reboot (default 3)",
    )
    p.add_argument(
        "--post",
        type=float,
        default=15.0,
        help="Seconds to capture after reboot stub returns (default 15)",
    )
    p.add_argument(
        "-o",
        "--output",
        type=Path,
        default=None,
        help="Also write full capture to this file",
    )
    p.add_argument(
        "--no-reboot",
        action="store_true",
        help="Only capture --pre seconds, skip JTAG (debug wiring)",
    )
    args = p.parse_args()

    port = args.port or find_ftdi_port()
    if not port:
        print("No serial port — use --port /dev/ttyUSB0", file=sys.stderr)
        return 1

    print(f"[+] {port} @ {args.baud} — capture {args.pre}s, then reboot", file=sys.stderr)

    try:
        ser = serial.Serial(port, args.baud, timeout=0.1)
    except serial.SerialException as e:
        print(f"Cannot open {port}: {e}\n(Quit tio/minicom first.)", file=sys.stderr)
        return 1

    ser.reset_input_buffer()
    buf = bytearray()
    stop = threading.Event()
    th = threading.Thread(target=reader_thread, args=(ser, buf, stop), daemon=True)
    th.start()

    try:
        time.sleep(args.pre)

        if not args.no_reboot:
            print("\n[+] running blender_tool reboot …", file=sys.stderr, flush=True)
            r = subprocess.run(
                [sys.executable, str(FIRMWARE_DIR / "blender_tool.py"), "reboot"],
                cwd=str(FIRMWARE_DIR),
                capture_output=True,
                text=True,
            )
            if r.returncode != 0:
                print(r.stdout, file=sys.stderr)
                print(r.stderr, file=sys.stderr)
                print(f"[!!] jtag_reboot exited {r.returncode}", file=sys.stderr)
            else:
                print(r.stdout, file=sys.stderr)

            time.sleep(args.post)
        else:
            print("[+] --no-reboot: done after pre period", file=sys.stderr)

    finally:
        stop.set()
        time.sleep(0.2)
        ser.close()

    if args.output:
        args.output.write_bytes(bytes(buf))
        print(f"[+] wrote {len(buf)} bytes → {args.output}", file=sys.stderr)

    return 0


if __name__ == "__main__":
    sys.exit(main())
