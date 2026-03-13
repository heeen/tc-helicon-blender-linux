#!/bin/bash
# TC Helicon Blender USB audio init script
# Called by udev when the Blender device is detected.
# Unbinds snd-usb-audio, runs vendor init, then rebinds.
#
# The device needs a vendor-specific USB init sequence before its UAC2 clock
# source responds to queries. Without this, snd-usb-audio probe fails with:
#   parse_audio_format_rates_v2v3(): unable to retrieve sample rate range

DEVPATH="$1"
BUSNUM="$2"
DEVNUM="$3"

logger -t blender-init "Starting init for $DEVPATH (bus $BUSNUM dev $DEVNUM)"

# Find the USB device path (e.g. "7-2")
USBDEV=$(basename "$DEVPATH")

# Unbind all interfaces from snd-usb-audio
for i in 0 1 2 3 4; do
    echo "${USBDEV}:1.${i}" > /sys/bus/usb/drivers/snd-usb-audio/unbind 2>/dev/null
done

# Run the vendor init sequence using blender-ctl
/usr/local/bin/blender-ctl init 2>&1 | logger -t blender-init

# Small delay for device to settle
sleep 1

# Rebind snd-usb-audio to the interfaces
for i in 0 1 2 3 4; do
    echo "${USBDEV}:1.${i}" > /sys/bus/usb/drivers/snd-usb-audio/bind 2>/dev/null
done

logger -t blender-init "Init complete for $USBDEV"
