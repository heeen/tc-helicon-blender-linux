#!/bin/bash
zstd -d /lib/modules/6.17.0-8-generic/updates/dkms/snd-usb-audio.ko.zst -o /tmp/snd-usb-audio.ko -f
echo "=== clock validity string ==="
strings /tmp/snd-usb-audio.ko | grep -i "clock source.*is not valid"
echo "=== TC Helicon boot quirk string ==="
strings /tmp/snd-usb-audio.ko | grep -i "TC Helicon"
echo "=== snd_usb_tc_helicon ==="
strings /tmp/snd-usb-audio.ko | grep "tc_helicon"
echo "=== using predefined rate ==="
strings /tmp/snd-usb-audio.ko | grep "predefined rate"
