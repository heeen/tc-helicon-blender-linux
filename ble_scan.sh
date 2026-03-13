#!/bin/bash
# Scan for BLE devices for 15 seconds
bluetoothctl <<EOF
menu scan
transport le
back
scan on
EOF
sleep 15
bluetoothctl scan off
bluetoothctl devices
