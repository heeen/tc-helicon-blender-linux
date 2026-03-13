#!/bin/bash
# Monitor all USB traffic on the Blender's bus, filtering out isochronous (audio) transfers.
# Usage: sudo ./usbmon_blender.sh
# Then in another terminal: cargo run -- dcp

set -e

modprobe usbmon 2>/dev/null || true

BUS=$(lsusb -d 1220:8fe1 | grep -oP 'Bus \K\d+' | sed 's/^0*//')
DEV=$(lsusb -d 1220:8fe1 | grep -oP 'Device \K\d+' | sed 's/^0*//')

if [ -z "$BUS" ] || [ -z "$DEV" ]; then
    echo "Blender not found"
    exit 1
fi

echo "Blender on bus $BUS device $DEV"
echo "Monitoring /sys/kernel/debug/usb/usbmon/${BUS}u"
echo "Filtering out isochronous (Zi/Zo) transfers"
echo "Press Ctrl+C to stop"
echo "---"

# First show a few raw lines to verify format
echo "(showing first 3 raw lines for format check...)"
timeout 2 head -3 /sys/kernel/debug/usb/usbmon/${BUS}u 2>/dev/null || echo "(no traffic yet)"
echo "---"

# Filter: exclude Z (isochronous), show all control/interrupt/bulk for our device
# usbmon format: TAG TIMESTAMP TYPE ADDRESS STATUS LENGTH DATA
# ADDRESS format varies - just grep for the device number
cat /sys/kernel/debug/usb/usbmon/${BUS}u | grep -v " Z[io] " | grep ":${DEV}:"
