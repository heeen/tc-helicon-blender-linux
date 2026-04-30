#!/usr/bin/env python3
"""Send USB-MIDI packets on bulk OUT EP3 (0x03) and report write result.

Shows WROTE vs TIMEOUT vs USBERR — TIMEOUT usually means device NAK (no buffer
primed / STALL / not configured). Requires PyUSB; may need kernel driver
detached from interface 4 (snd-usb-audio MIDI).

Default: TC Helicon Blender (VID 0x1220, PID 0x8FE1).
"""

import argparse
import sys
import time

import usb.core
import usb.util

DEFAULT_VID = 0x1220
DEFAULT_PID = 0x8FE1
DEFAULT_IFACE = 4
DEFAULT_EP_OUT = 0x03


def main() -> int:
    ap = argparse.ArgumentParser(description=__doc__)
    ap.add_argument("--vid", type=lambda x: int(x, 0), default=DEFAULT_VID)
    ap.add_argument("--pid", type=lambda x: int(x, 0), default=DEFAULT_PID)
    ap.add_argument("--iface", type=int, default=DEFAULT_IFACE, help="MIDI interface")
    ap.add_argument("--ep", type=lambda x: int(x, 0), default=DEFAULT_EP_OUT, help="bulk OUT addr")
    ap.add_argument("--timeout-ms", type=int, default=300)
    ap.add_argument("--sleep", type=float, default=0.005, help="seconds between packets")
    ap.add_argument("--count", type=int, default=64, help="total packets to send (cycles through pkts)")
    ap.add_argument("--size", type=int, default=4,
                    help="bytes per bulk transfer (pads with repeated MIDI CC events, 4B each). "
                         "Try 4, 64, 128, 256, 512 to test hw size-match sensitivity.")
    args = ap.parse_args()

    dev = usb.core.find(idVendor=args.vid, idProduct=args.pid)
    if dev is None:
        print("NO_DEVICE", file=sys.stderr)
        return 1

    had_kernel = False
    try:
        had_kernel = dev.is_kernel_driver_active(args.iface)
    except (NotImplementedError, ValueError, usb.core.USBError):
        had_kernel = False

    if had_kernel:
        dev.detach_kernel_driver(args.iface)
    usb.util.claim_interface(dev, args.iface)

    # USB-MIDI 1.0 cable packets: CIN 0xB = CC, + 3 MIDI bytes (4 bytes each).
    base_pkts = [
        bytes([0x0B, 0xB0, 0x07, 0x7F]),
        bytes([0x0B, 0xB0, 0x07, 0x00]),
        bytes([0x0B, 0xB0, 0x10, 0x40]),
        bytes([0x0B, 0xB0, 0x11, 0x20]),
        bytes([0x0B, 0xB0, 0x12, 0x7F]),
        bytes([0x0B, 0xB0, 0x13, 0x00]),
    ]
    # Pad each bulk transfer to args.size bytes by concatenating MIDI events.
    size = max(4, (args.size // 4) * 4)  # round down to multiple of 4
    pkts_per_xfer = size // 4
    pkts = []
    for i in range(len(base_pkts)):
        payload = b"".join(base_pkts[(i + k) % len(base_pkts)] for k in range(pkts_per_xfer))
        pkts.append(payload)
    print(f"[send] size={size} pkts_per_xfer={pkts_per_xfer} xfers={args.count}")

    wrote_n = 0
    timeout_n = 0
    err_n = 0
    try:
        for i in range(1, args.count + 1):
            p = pkts[(i - 1) % len(pkts)]
            try:
                w = dev.write(args.ep, p, timeout=args.timeout_ms)
                wrote_n += 1
                print(f"pkt{i}: WROTE {w}")
            except usb.core.USBTimeoutError:
                timeout_n += 1
                print(f"pkt{i}: TIMEOUT")
            except usb.core.USBError as e:
                err_n += 1
                print(f"pkt{i}: USBERR {e}")
            if args.sleep > 0:
                time.sleep(args.sleep)
        print(f"summary: wrote={wrote_n} timeout={timeout_n} usberr={err_n} total={args.count}")
    finally:
        usb.util.release_interface(dev, args.iface)
        if had_kernel:
            try:
                dev.attach_kernel_driver(args.iface)
            except usb.core.USBError:
                pass

    return 0


if __name__ == "__main__":
    raise SystemExit(main())
