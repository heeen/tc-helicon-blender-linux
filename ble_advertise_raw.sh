#!/bin/bash
# Advertise ParameterService UUID using bluetoothctl
# and register a GATT server via bluetoothctl

bluetoothctl <<'EOF'
menu advertise
uuids E71EE188-279F-4ED6-8055-12D77BFD900C
discoverable on
connectable on
name BlenderCtl
back
advertise on
EOF

echo "Advertising started. Press pair button on Blender."
echo "Press Ctrl+C to stop."
sleep 300
