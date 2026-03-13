#!/usr/bin/env python3
"""
Probe the TC Helicon Blender's USB MIDI interface.

Interface 4 is a standard USB MIDI Streaming interface with:
  - EP 0x82 IN  (Bulk) — "Blender MIDI Out" (device → host)
  - EP 0x03 OUT (Bulk) — "Blender MIDI In"  (host → device)

USB-MIDI uses 4-byte packets (USB-MIDI Event Packets):
  byte 0: cable number (high nibble) | code index number (low nibble)
  bytes 1-3: MIDI data

Run with the Blender plugged in. Turn knobs / press buttons to see
what MIDI messages come out. Type commands to send MIDI in.
"""

import sys
import struct
import threading
import time

import usb.core
import usb.util

VID = 0x1220
PID = 0x8FE1
MIDI_IFACE = 4
EP_IN = 0x82
EP_OUT = 0x03

# USB-MIDI Code Index Numbers (CIN)
CIN_NAMES = {
    0x0: "misc",
    0x1: "cable_event",
    0x2: "2byte_system",
    0x3: "3byte_system",
    0x4: "sysex_start",
    0x5: "sysex_end_1",
    0x6: "sysex_end_2",
    0x7: "sysex_end_3",
    0x8: "note_off",
    0x9: "note_on",
    0xA: "poly_pressure",
    0xB: "control_change",
    0xC: "program_change",
    0xD: "channel_pressure",
    0xE: "pitch_bend",
    0xF: "single_byte",
}

# Standard MIDI CC names (common ones)
CC_NAMES = {
    0: "Bank Select",
    1: "Mod Wheel",
    2: "Breath",
    7: "Volume",
    10: "Pan",
    11: "Expression",
    64: "Sustain",
    120: "All Sound Off",
    121: "Reset All",
    123: "All Notes Off",
}


def format_midi_packet(data: bytes) -> str:
    """Format a 4-byte USB-MIDI event packet."""
    if len(data) < 4:
        return f"short: {data.hex()}"
    cable = (data[0] >> 4) & 0x0F
    cin = data[0] & 0x0F
    cin_name = CIN_NAMES.get(cin, f"0x{cin:X}")
    b1, b2, b3 = data[1], data[2], data[3]

    detail = ""
    if cin == 0xB:  # CC
        ch = b1 & 0x0F
        cc_name = CC_NAMES.get(b2, f"CC{b2}")
        detail = f"  ch={ch} {cc_name} val={b3}"
    elif cin == 0x9:  # Note On
        ch = b1 & 0x0F
        detail = f"  ch={ch} note={b2} vel={b3}"
    elif cin == 0x8:  # Note Off
        ch = b1 & 0x0F
        detail = f"  ch={ch} note={b2} vel={b3}"
    elif cin == 0xC:  # Program Change
        ch = b1 & 0x0F
        detail = f"  ch={ch} prog={b2}"
    elif cin == 0xE:  # Pitch Bend
        ch = b1 & 0x0F
        val = b2 | (b3 << 7)
        detail = f"  ch={ch} bend={val - 8192}"

    return f"cable={cable} {cin_name:16s} [{b1:02X} {b2:02X} {b3:02X}]{detail}"


def read_loop(dev, stop_event):
    """Continuously read MIDI from the Blender."""
    while not stop_event.is_set():
        try:
            data = dev.read(EP_IN, 64, timeout=500)
            # Process 4-byte packets
            for i in range(0, len(data), 4):
                pkt = bytes(data[i:i + 4])
                if pkt == b"\x00\x00\x00\x00":
                    continue
                print(f"  IN   {pkt.hex()}  {format_midi_packet(pkt)}")
        except usb.core.USBTimeoutError:
            pass
        except usb.core.USBError as e:
            if e.errno == 19:  # No such device
                print("Device disconnected")
                stop_event.set()
                break
            if not stop_event.is_set():
                print(f"  USB error: {e}")
                time.sleep(0.1)


def send_midi(dev, cin: int, b1: int, b2: int, b3: int, cable: int = 0):
    """Send a single USB-MIDI event packet."""
    pkt = bytes([(cable << 4) | cin, b1, b2, b3])
    dev.write(EP_OUT, pkt, timeout=1000)
    print(f"  OUT  {pkt.hex()}  {format_midi_packet(pkt)}")


def main():
    dev = usb.core.find(idVendor=VID, idProduct=PID)
    if not dev:
        print("Blender not found")
        return 1

    print(f"Found: {dev.manufacturer} {dev.product} (serial {dev.serial_number})")

    # Detach kernel driver if needed
    try:
        if dev.is_kernel_driver_active(MIDI_IFACE):
            dev.detach_kernel_driver(MIDI_IFACE)
            print(f"Detached kernel driver from interface {MIDI_IFACE}")
    except usb.core.USBError:
        pass

    # Claim interface
    usb.util.claim_interface(dev, MIDI_IFACE)
    print(f"Claimed MIDI interface {MIDI_IFACE}")
    print(f"  EP IN:  0x{EP_IN:02X}  (device → host)")
    print(f"  EP OUT: 0x{EP_OUT:02X}  (host → device)")
    print()
    print("Listening for MIDI... Turn knobs or press buttons on the Blender.")
    print("Commands:")
    print("  cc <ch> <cc#> <val>    Send Control Change")
    print("  note <ch> <note> <vel> Send Note On (vel=0 for off)")
    print("  pc <ch> <prog>         Send Program Change")
    print("  sysex <hex bytes>      Send SysEx (F0 ... F7)")
    print("  raw <4 hex bytes>      Send raw USB-MIDI packet")
    print("  id                     Send Identity Request (SysEx)")
    print("  quit")
    print()

    stop = threading.Event()
    reader = threading.Thread(target=read_loop, args=(dev, stop), daemon=True)
    reader.start()

    try:
        while not stop.is_set():
            try:
                line = input("midi> ")
            except (EOFError, KeyboardInterrupt):
                break

            parts = line.strip().split()
            if not parts:
                continue
            cmd = parts[0].lower()

            try:
                if cmd in ("quit", "q", "exit"):
                    break
                elif cmd == "cc":
                    ch, cc, val = int(parts[1]), int(parts[2]), int(parts[3])
                    send_midi(dev, 0xB, 0xB0 | (ch & 0xF), cc & 0x7F, val & 0x7F)
                elif cmd == "note":
                    ch, note, vel = int(parts[1]), int(parts[2]), int(parts[3])
                    cin = 0x9 if vel > 0 else 0x8
                    status = (0x90 if vel > 0 else 0x80) | (ch & 0xF)
                    send_midi(dev, cin, status, note & 0x7F, vel & 0x7F)
                elif cmd == "pc":
                    ch, prog = int(parts[1]), int(parts[2])
                    send_midi(dev, 0xC, 0xC0 | (ch & 0xF), prog & 0x7F, 0)
                elif cmd == "id":
                    # Universal SysEx Identity Request: F0 7E 7F 06 01 F7
                    # USB-MIDI: SysEx start (CIN=4) + SysEx end 2 (CIN=6)
                    send_midi(dev, 0x4, 0xF0, 0x7E, 0x7F)  # SysEx start
                    send_midi(dev, 0x6, 0x06, 0x01, 0xF7)  # SysEx end (2 data + F7)
                elif cmd == "sysex":
                    # Parse hex bytes, pack into USB-MIDI SysEx packets
                    data = bytes(int(x, 16) for x in parts[1:])
                    i = 0
                    while i < len(data):
                        remaining = len(data) - i
                        if remaining >= 3 and data[i + remaining - 1] != 0xF7:
                            # SysEx start/continue: CIN=4, 3 bytes
                            send_midi(dev, 0x4, data[i], data[i + 1], data[i + 2])
                            i += 3
                        elif remaining == 1 and data[i] == 0xF7:
                            send_midi(dev, 0x5, 0xF7, 0x00, 0x00)
                            i += 1
                        elif remaining == 2 and data[i + 1] == 0xF7:
                            send_midi(dev, 0x6, data[i], 0xF7, 0x00)
                            i += 2
                        elif remaining >= 3 and data[i + 2] == 0xF7:
                            send_midi(dev, 0x7, data[i], data[i + 1], 0xF7)
                            i += 3
                        elif remaining >= 3:
                            send_midi(dev, 0x4, data[i], data[i + 1], data[i + 2])
                            i += 3
                        else:
                            print(f"  Leftover bytes: {data[i:].hex()}")
                            break
                elif cmd == "raw":
                    data = bytes(int(x, 16) for x in parts[1:])
                    if len(data) != 4:
                        print("  raw needs exactly 4 hex bytes")
                    else:
                        dev.write(EP_OUT, data, timeout=1000)
                        print(f"  OUT  {data.hex()}  {format_midi_packet(data)}")
                else:
                    print(f"  Unknown command: {cmd}")
            except (IndexError, ValueError) as e:
                print(f"  Error: {e}")
            except usb.core.USBError as e:
                print(f"  USB error: {e}")

    finally:
        stop.set()
        reader.join(timeout=2)
        usb.util.release_interface(dev, MIDI_IFACE)
        print("Released MIDI interface.")


if __name__ == "__main__":
    sys.exit(main() or 0)
