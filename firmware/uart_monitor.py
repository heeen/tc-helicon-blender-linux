#!/usr/bin/env python3
"""Monitor Blender UART and return after data + silence.

Waits for serial data, collects it, then returns after a configurable
silence period (no new data for N seconds). Useful for detecting boot
messages, reboot completion, or firmware debug output.

Usage:
    python3 firmware/uart_monitor.py                    # wait for data + 2s silence
    python3 firmware/uart_monitor.py --timeout 30       # give up after 30s of no data
    python3 firmware/uart_monitor.py --silence 0.5      # return after 0.5s silence
    python3 firmware/uart_monitor.py --notify            # also send desktop notification
    python3 firmware/uart_monitor.py --expect TCAT-BOOT  # wait for specific string
"""

import argparse
import glob
import os
import subprocess
import sys
import time

import serial


def notify(title: str, body: str) -> None:
    try:
        subprocess.run(
            ["notify-send", title, body],
            timeout=5,
            capture_output=True,
        )
    except Exception:
        pass


def find_ftdi_port() -> str | None:
    """Auto-detect FTDI FT232R serial port (Blender UART adapter)."""
    for tty in sorted(glob.glob("/dev/ttyUSB*")):
        dev = os.path.basename(tty)
        syspath = os.path.realpath(f"/sys/class/tty/{dev}/device")
        while syspath and syspath != "/":
            vid_path = os.path.join(syspath, "idVendor")
            if os.path.exists(vid_path):
                vid = open(vid_path).read().strip()
                pid = open(os.path.join(syspath, "idProduct")).read().strip()
                if vid == "0403" and pid == "6001":  # FTDI FT232R
                    return tty
                break
            syspath = os.path.dirname(syspath)
    return None


def monitor(
    port: str | None = None,
    baud: int = 115200,
    timeout: float = 60.0,
    silence: float = 2.0,
    expect: str | None = None,
    do_notify: bool = False,
) -> str:
    """Monitor UART, return collected output after silence.

    Args:
        port: serial port path
        baud: baud rate
        timeout: max seconds to wait for ANY data (0 = forever)
        silence: seconds of quiet after last byte before returning
        expect: if set, don't start silence timer until this string appears
        do_notify: send desktop notification when done

    Returns:
        Collected UART output as string.
    """
    if port is None:
        port = find_ftdi_port()
        if port is None:
            raise RuntimeError("No FTDI FT232R found — specify --port")
        print(f"Auto-detected: {port}", file=sys.stderr)

    ser = serial.Serial(port, baud, timeout=0.1)
    ser.reset_input_buffer()

    buf = bytearray()
    last_data_t = None
    start_t = time.monotonic()
    expect_seen = expect is None  # True if no expect string

    try:
        while True:
            chunk = ser.read(256)
            now = time.monotonic()

            if chunk:
                buf.extend(chunk)
                last_data_t = now

                # Decode and print live
                try:
                    text = chunk.decode("ascii", errors="replace")
                    sys.stdout.write(text)
                    sys.stdout.flush()
                except Exception:
                    pass

                # Check expect string
                if not expect_seen and expect:
                    try:
                        if expect in buf.decode("ascii", errors="replace"):
                            expect_seen = True
                    except Exception:
                        pass

            # Check silence condition
            if last_data_t is not None and expect_seen:
                if now - last_data_t >= silence:
                    break

            # Check initial timeout (no data at all)
            if last_data_t is None and timeout > 0:
                if now - start_t >= timeout:
                    result = buf.decode("ascii", errors="replace")
                    if do_notify:
                        notify("UART timeout", f"No data after {timeout}s")
                    return result

    except KeyboardInterrupt:
        pass
    finally:
        ser.close()

    result = buf.decode("ascii", errors="replace")

    if do_notify:
        # First line or first 80 chars as summary
        summary = result.strip().split("\n")[0][:80] if result.strip() else "(empty)"
        notify("UART", summary)

    return result


def main():
    p = argparse.ArgumentParser(description="Monitor Blender UART")
    p.add_argument("--port", default=None,
                   help="Serial port (auto-detects FTDI FT232R if omitted)")
    p.add_argument("--baud", type=int, default=115200)
    p.add_argument("--timeout", type=float, default=60.0,
                   help="Max seconds to wait for first data (0=forever)")
    p.add_argument("--silence", type=float, default=2.0,
                   help="Return after this many seconds of silence")
    p.add_argument("--expect", type=str, default=None,
                   help="Wait for this string before starting silence timer")
    p.add_argument("--notify", action="store_true",
                   help="Send desktop notification when done")
    args = p.parse_args()

    result = monitor(
        port=args.port,
        baud=args.baud,
        timeout=args.timeout,
        silence=args.silence,
        expect=args.expect,
        do_notify=args.notify,
    )

    if not result.strip():
        print("\n(no data received)", file=sys.stderr)
        sys.exit(1)


if __name__ == "__main__":
    main()
