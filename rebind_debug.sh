#!/bin/bash
# Enable dynamic debug for snd-usb-audio
echo 'module snd_usb_audio +p' > /sys/kernel/debug/dynamic_debug/control 2>/dev/null

# Find the Blender interface paths
for i in /sys/bus/usb/drivers/snd-usb-audio/7-2:*; do
    if [ -e "$i" ]; then
        iface=$(basename "$i")
        echo "Unbinding $iface"
        echo "$iface" > /sys/bus/usb/drivers/snd-usb-audio/unbind 2>/dev/null
    fi
done

sleep 1

# Rebind
for i in 7-2:1.0 7-2:1.1 7-2:1.2 7-2:1.3 7-2:1.4; do
    echo "Binding $i"
    echo "$i" > /sys/bus/usb/drivers/snd-usb-audio/bind 2>/dev/null
done

sleep 3
echo "=== dmesg tail ==="
dmesg | tail -60
