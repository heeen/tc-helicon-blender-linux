#!/usr/bin/env python3
"""Read UART output from Blender DICE3. Non-blocking, prints to stdout."""

import serial
import sys
import time

PORT = '/dev/serial/by-id/usb-FTDI_FT232R_USB_UART_A50285BI-if00-port0'
BAUD = 115200

def main():
    timeout = float(sys.argv[1]) if len(sys.argv) > 1 else 5.0
    ser = serial.Serial(PORT, BAUD, timeout=0.1)
    # Flush any stale data
    ser.reset_input_buffer()

    print(f"[uart_read] listening on {PORT} for {timeout}s...", file=sys.stderr)
    deadline = time.time() + timeout
    buf = b''
    while time.time() < deadline:
        data = ser.read(256)
        if data:
            buf += data
            sys.stdout.buffer.write(data)
            sys.stdout.buffer.flush()

    ser.close()
    if not buf:
        print("[uart_read] no data received", file=sys.stderr)
    else:
        print(f"\n[uart_read] {len(buf)} bytes received", file=sys.stderr)

if __name__ == '__main__':
    main()
