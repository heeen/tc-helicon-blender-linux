# JTAG scripts for TC Helicon Blender (DICE3)

## Hardware setup

FT232R connected to the 5x2 1.27mm JTAG header on the Blender PCB.
Pin mapping: TXD=TCK, RXD=TDI, RTS=TDO, CTS=TMS

- DICE3 core: ARM926EJ-S
- JTAG IDCODE: `0x17900F0F` (verified)
- Config: `../dice3-openocd.cfg`

## Triggering the BT/PAIR button

```bash
openocd -f dice3-openocd.cfg -f jtag/trigger_bt.tcl
```

Then in telnet (`telnet localhost 4444`):

```
# Option 1: Identify which button is BT by pressing it physically
> identify_button

# Option 2: Try all 16 buttons (run bluetoothctl scan on in another terminal)
> trigger_all_buttons

# Option 3: Trigger a specific button
> trigger_one 5
```

## Reading the flash

```bash
openocd -f dice3-openocd.cfg \
  -c "halt" \
  -c "dump_image blender_flash_dump.bin 0x00000000 0x100000" \
  -c shutdown
```
