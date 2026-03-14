#!/usr/bin/env python3
"""
Probe all DCP categories and opcodes on the Blender via USB.
Properly initializes the DCP state machine before probing.
"""

import usb.core
import usb.util
import struct
import time
import sys

VID = 0x1220
PID = 0x8FE1

def find_device():
    dev = usb.core.find(idVendor=VID, idProduct=PID)
    if dev is None:
        print("Blender not found")
        sys.exit(1)
    for iface in range(5):
        try:
            if dev.is_kernel_driver_active(iface):
                dev.detach_kernel_driver(iface)
        except Exception:
            pass
    dev.set_configuration()
    for iface in range(5):
        try:
            usb.util.claim_interface(dev, iface)
        except Exception:
            pass
    return dev

BMREQ_OUT_VENDOR = usb.util.build_request_type(
    usb.util.CTRL_OUT, usb.util.CTRL_TYPE_VENDOR, usb.util.CTRL_RECIPIENT_INTERFACE)
BMREQ_IN_VENDOR = usb.util.build_request_type(
    usb.util.CTRL_IN, usb.util.CTRL_TYPE_VENDOR, usb.util.CTRL_RECIPIENT_INTERFACE)

def dcp_init(dev):
    """Initialize DCP: bReq=1 ping, bReq=3 flush, cmd_id=0 reset."""
    # bReq=1: reset DCP state machine
    try:
        dev.ctrl_transfer(BMREQ_OUT_VENDOR, 1, 0, 0, b'', 2000)
        print("  bReq=1 ping: OK")
    except usb.core.USBError as e:
        print(f"  bReq=1 ping: {e}")
        # Boot sequence might be needed
        bmreq_in = usb.util.build_request_type(
            usb.util.CTRL_IN, usb.util.CTRL_TYPE_VENDOR, usb.util.CTRL_RECIPIENT_INTERFACE)
        bmreq_out_class = usb.util.build_request_type(
            usb.util.CTRL_OUT, usb.util.CTRL_TYPE_CLASS, usb.util.CTRL_RECIPIENT_INTERFACE)
        # Vendor read bReq=0
        try:
            dev.ctrl_transfer(bmreq_in, 0, 0, 0, 24, 1000)
        except: pass
        # SET_CUR 48kHz
        dev.ctrl_transfer(bmreq_out_class, 0x01, 0x0100, 0x2900,
                          struct.pack('<I', 48000), 1000)
        print("  Boot sequence: SET_CUR 48kHz sent")
        time.sleep(1)
        # Retry ping
        try:
            dev.ctrl_transfer(BMREQ_OUT_VENDOR, 1, 0, 0, b'', 2000)
            print("  bReq=1 retry: OK")
        except usb.core.USBError as e2:
            print(f"  bReq=1 retry: {e2}")

    # bReq=3: flush any pending response
    try:
        resp = dev.ctrl_transfer(BMREQ_IN_VENDOR, 3, 0, 0, 1040, 500)
        print(f"  bReq=3 flush: drained {len(resp)} bytes")
    except usb.core.USBError:
        print("  bReq=3 flush: pipe empty (OK)")

    # cmd_id=0: reset command index
    pkt = struct.pack('<IHH', 0, 0, 0) + b'\x00' * 8
    try:
        dev.ctrl_transfer(BMREQ_OUT_VENDOR, 2, 0, 0, pkt, 2000)
        time.sleep(0.01)
        resp = dev.ctrl_transfer(BMREQ_IN_VENDOR, 3, 0, 0, 1040, 2000)
        print(f"  Reset cmd_idx: OK ({len(resp)} bytes)")
    except usb.core.USBError as e:
        print(f"  Reset cmd_idx: {e}")

def dcp_recover(dev):
    """Recover from broken DCP state: bReq=1 reset, bReq=3 drain."""
    try:
        dev.ctrl_transfer(BMREQ_OUT_VENDOR, 1, 0, 0, b'', 2000)
    except: pass
    time.sleep(0.05)
    try:
        dev.ctrl_transfer(BMREQ_IN_VENDOR, 3, 0, 0, 1040, 500)
    except: pass
    time.sleep(0.05)

def dcp_command(dev, cmd_id, body=b'', cmd_idx=0, timeout=2000):
    """Send DCP command, return response bytes or None on error."""
    pkt = struct.pack('<IHH', cmd_id, len(body), cmd_idx) + b'\x00' * 8 + body
    try:
        dev.ctrl_transfer(BMREQ_OUT_VENDOR, 2, 0, 0, pkt, timeout)
    except usb.core.USBError as e:
        return None, f"write error: {e}"

    for attempt in range(20):
        time.sleep(0.01)
        try:
            resp = dev.ctrl_transfer(BMREQ_IN_VENDOR, 3, 0, 0, 1040, timeout)
            return bytes(resp), None
        except usb.core.USBError:
            continue
    return None, "read timeout after 20 attempts"

def parse_response(data):
    if len(data) < 16:
        return None
    cmd_id, body_len, cmd_idx = struct.unpack_from('<IHH', data, 0)
    body = data[16:]
    return {
        'cmd_id': cmd_id, 'body_len': body_len, 'cmd_idx': cmd_idx,
        'body': body, 'category': (cmd_id >> 12) & 0xFFF, 'opcode': cmd_id & 0xFFF,
    }

def hexdump_short(data, max_bytes=64):
    if not data: return "(empty)"
    shown = data[:max_bytes]
    hex_str = ' '.join(f'{b:02x}' for b in shown)
    if len(data) > max_bytes: hex_str += ' ...'
    ascii_str = ''.join(chr(b) if 32 <= b < 127 else '.' for b in shown)
    return f"{hex_str}\n      |{ascii_str}|"


def main():
    dev = find_device()
    print(f"Found Blender at bus {dev.bus} dev {dev.address}\n")

    print("=== DCP Init ===")
    dcp_init(dev)

    cmd_idx = [0]
    errors_in_a_row = [0]

    def send(cmd_id, body=b'', label=''):
        cmd_idx[0] += 1
        resp, err = dcp_command(dev, cmd_id, body, cmd_idx[0])
        if err:
            errors_in_a_row[0] += 1
            print(f"  {label or f'cmd=0x{cmd_id:08x}'}: ERROR {err}")
            if errors_in_a_row[0] >= 3:
                print("  >> Recovering DCP state machine...")
                dcp_recover(dev)
                # Re-reset command index
                cmd_idx[0] = 0
                pkt = struct.pack('<IHH', 0, 0, 0) + b'\x00' * 8
                try:
                    dev.ctrl_transfer(BMREQ_OUT_VENDOR, 2, 0, 0, pkt, 2000)
                    time.sleep(0.01)
                    dev.ctrl_transfer(BMREQ_IN_VENDOR, 3, 0, 0, 1040, 2000)
                except: pass
                errors_in_a_row[0] = 0
            return None
        errors_in_a_row[0] = 0
        parsed = parse_response(resp)
        if parsed is None:
            print(f"  {label or f'cmd=0x{cmd_id:08x}'}: short response ({len(resp)} bytes)")
            return None
        body = parsed['body']
        body_len = parsed['body_len']
        if len(body) == 0:
            print(f"  {label or f'cmd=0x{cmd_id:08x}'}: header-only (body_len={body_len})")
        else:
            print(f"  {label or f'cmd=0x{cmd_id:08x}'}: {len(body)} bytes (hdr says {body_len})")
            print(f"    {hexdump_short(body)}")
        return parsed

    # === Known-working commands first (verify init worked) ===
    print("\n=== Verify Init: Known Working Commands ===")
    send(2, label="SystemInfo (cmd=2)")
    send((0x80F << 12), label="FW Version (0x80F000)")
    send((0x80F << 12) | 1, label="Serial (0x80F001)")
    send((0x80C << 12), label="MicLevel (0x80C000)")
    send((0x800 << 12), label="ButtonStates (0x800000)")

    # === Standard DICE categories ===
    print("\n=== Standard DICE Categories (0x000-0x004) ===")
    dice_cats = [
        (0x000, "System"), (0x001, "Peaks"), (0x002, "Mixer"),
        (0x003, "Router"), (0x004, "NVM"),
    ]
    for cat, name in dice_cats:
        for op in range(4):  # fewer ops, enough to see pattern
            send((cat << 12) | op, label=f"{name} cat=0x{cat:03x} op={op}")

    # === Vendor categories — op=0 (read) ===
    print("\n=== Vendor Categories READ op=0 (0x800-0x81F) ===")
    for cat in range(0x800, 0x820):
        send(cat << 12, label=f"cat=0x{cat:03x} op=0")

    # === Interesting vendor cats with multiple ops ===
    print("\n=== Interesting Vendor Categories (multi-op) ===")
    for cat, name in [(0x80B, "DSP"), (0x80C, "MicLvl"), (0x80F, "HWInfo"),
                       (0x810, "FWStatus"), (0x814, "Volume")]:
        for op in range(4):
            send((cat << 12) | op, label=f"{name} 0x{cat:03x} op={op}")

    # === Write tests (safe: 1-byte body, read-like) ===
    # Volume set attempt: send 1-byte body to 0x806
    print("\n=== Volume Write Tests (0x806, 0x814) ===")
    for ch in range(6):
        # Read current value via 0x806
        send((0x806 << 12) | ch, body=bytes([128]), label=f"Fader 0x806 ch={ch} write=128")

    # === DSP key-value write test ===
    print("\n=== DSP Key-Value Write Test (0x80B) ===")
    # Try sending a read (no body) to see what happens
    send(0x80B << 12, label="DSP 0x80B read (no body)")
    # Try sending 8-byte key-value (paramID=0, value=0 — should be harmless)
    send(0x80B << 12, body=struct.pack('<II', 0, 0), label="DSP 0x80B write (0,0)")

    # === Routing read test ===
    print("\n=== Routing Tests (0x804) ===")
    for row in range(4):
        send((0x804 << 12) | row, label=f"Route 0x804 row={row}")

    # === NVM capabilities ===
    print("\n=== NVM Tests (0x004) ===")
    for op in range(4):
        send((0x004 << 12) | op, label=f"NVM op={op}")

    print("\nDone.")


if __name__ == '__main__':
    main()
