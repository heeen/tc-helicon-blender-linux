#!/usr/bin/env python3
"""Dump Blender firmware via vendor read bReq=0x00.

Tries different wValue/wIndex combinations to read the full address space.
"""

import usb.core
import usb.util
import struct
import sys
import time

VID = 0x1220
PID = 0x8FE1

dev = usb.core.find(idVendor=VID, idProduct=PID)
if dev is None:
    print("Blender not found")
    sys.exit(1)

print(f"Found Blender at bus {dev.bus} dev {dev.address}")

# Detach kernel driver if needed
for iface in range(5):
    try:
        if dev.is_kernel_driver_active(iface):
            dev.detach_kernel_driver(iface)
    except Exception:
        pass

dev.set_configuration()

bmReqIn = usb.util.build_request_type(
    usb.util.CTRL_IN, usb.util.CTRL_TYPE_VENDOR, usb.util.CTRL_RECIPIENT_INTERFACE
)

CHUNK = 1024
outfile = "blender_memdump.bin"

# Phase 1: wValue as offset (16-bit), wIndex=0
print(f"\n=== Phase 1: wValue as offset, wIndex=0, {CHUNK} bytes per read ===")
data_wval = bytearray()
last_chunk = None
for offset in range(0, 0x10000, CHUNK):
    wval = offset & 0xFFFF
    try:
        chunk = dev.ctrl_transfer(bmReqIn, 0x00, wval, 0, CHUNK, timeout=2000)
        chunk = bytes(chunk)
        if offset == 0:
            print(f"  wVal=0x{wval:04x}: {len(chunk)} bytes, first 16: {chunk[:16].hex()}")
        elif chunk != last_chunk:
            print(f"  wVal=0x{wval:04x}: {len(chunk)} bytes, first 16: {chunk[:16].hex()}")
        else:
            # Same as previous — might be wrapping
            if offset % 0x4000 == 0:
                print(f"  wVal=0x{wval:04x}: (same as previous)")
        data_wval.extend(chunk)
        last_chunk = chunk
    except usb.core.USBError as e:
        print(f"  wVal=0x{wval:04x}: {e}")
        break

# Check if wValue actually changes the data
unique_chunks = set()
for i in range(0, len(data_wval), CHUNK):
    unique_chunks.add(data_wval[i:i+CHUNK])
print(f"  Total: {len(data_wval)} bytes, {len(unique_chunks)} unique chunks")

if len(unique_chunks) > 1:
    with open("blender_dump_wval.bin", "wb") as f:
        f.write(data_wval)
    print(f"  Saved to blender_dump_wval.bin")
print()

# Phase 2: wIndex as offset (16-bit), wValue=0
print(f"=== Phase 2: wIndex as offset, wValue=0, {CHUNK} bytes per read ===")
data_widx = bytearray()
last_chunk = None
for offset in range(0, 0x10000, CHUNK):
    widx = offset & 0xFFFF
    try:
        chunk = dev.ctrl_transfer(bmReqIn, 0x00, 0, widx, CHUNK, timeout=2000)
        chunk = bytes(chunk)
        if offset == 0:
            print(f"  wIdx=0x{widx:04x}: {len(chunk)} bytes, first 16: {chunk[:16].hex()}")
        elif chunk != last_chunk:
            print(f"  wIdx=0x{widx:04x}: {len(chunk)} bytes, first 16: {chunk[:16].hex()}")
        else:
            if offset % 0x4000 == 0:
                print(f"  wIdx=0x{widx:04x}: (same as previous)")
        data_widx.extend(chunk)
        last_chunk = chunk
    except usb.core.USBError as e:
        print(f"  wIdx=0x{widx:04x}: {e}")
        break

unique_chunks2 = set()
for i in range(0, len(data_widx), CHUNK):
    unique_chunks2.add(data_widx[i:i+CHUNK])
print(f"  Total: {len(data_widx)} bytes, {len(unique_chunks2)} unique chunks")

if len(unique_chunks2) > 1:
    with open("blender_dump_widx.bin", "wb") as f:
        f.write(data_widx)
    print(f"  Saved to blender_dump_widx.bin")
print()

# Phase 3: wValue as high word, wIndex as low word (32-bit address)
print(f"=== Phase 3: wValue:wIndex as 32-bit address ===")
# Try a few strategic addresses
addrs = [
    0x00000000, 0x00000400, 0x00001000, 0x00002000,
    0x00004000, 0x00008000, 0x00010000, 0x00020000,
    0x00040000, 0x00080000, 0x00100000,
    0x10000000, 0x20000000, 0x90000000, 0xC4000000,
]
for addr in addrs:
    wval = (addr >> 16) & 0xFFFF
    widx = addr & 0xFFFF
    try:
        chunk = dev.ctrl_transfer(bmReqIn, 0x00, wval, widx, CHUNK, timeout=1000)
        chunk = bytes(chunk)
        ascii_str = ''.join(chr(b) if 32 <= b < 127 else '.' for b in chunk[:32])
        print(f"  0x{addr:08x} (wV=0x{wval:04x} wI=0x{widx:04x}): {len(chunk)} bytes | {chunk[:16].hex()} | {ascii_str[:16]}")
    except usb.core.USBError as e:
        print(f"  0x{addr:08x}: {e}")

print()

# Phase 4: Try different bRequest values with wValue offset
print(f"=== Phase 4: bReq=0x00, larger wLength ===")
for length in [64, 512, 1024, 2048, 4096, 8192, 16384, 32768, 65535]:
    try:
        chunk = dev.ctrl_transfer(bmReqIn, 0x00, 0, 0, length, timeout=2000)
        print(f"  wLength={length}: got {len(chunk)} bytes")
    except usb.core.USBError as e:
        print(f"  wLength={length}: {e}")

print("\nDone.")
