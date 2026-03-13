#!/usr/bin/env openocd
# One-shot BT button inject via JTAG — halts briefly, injects, resumes, exits.
# Usage: openocd -f dice3-openocd.cfg -f jtag/inject_bt_oneshot.tcl

set RINGBUF_ADDR 0x29834

proc mem_write8 {addr val} {
    write_memory $addr 8 [list $val]
}

proc mem_write32 {addr val} {
    write_memory $addr 32 [list $val]
}

# Halt, inject press, resume
halt
set vals [read_memory $RINGBUF_ADDR 32 4]
set base  [lindex $vals 0]
set size  [lindex $vals 1]
set write [lindex $vals 2]

# Write button press: (0xDB, 3, 0) = button 3 pressed
set addr [expr {$base + $write}]
mem_write8 $addr 0xDB
mem_write8 [expr {$addr + 1}] 3
mem_write8 [expr {$addr + 2}] 0
set new_write [expr {$write + 3}]
if {$new_write >= $size} { set new_write [expr {$new_write - $size}] }
mem_write32 [expr {$RINGBUF_ADDR + 8}] $new_write
echo "Injected BT press (0xDB, 3, 0) write=$write->$new_write"
resume

# Brief pause, then inject release
after 200
halt
set vals [read_memory $RINGBUF_ADDR 32 4]
set write [lindex $vals 2]
set addr [expr {$base + $write}]
mem_write8 $addr 0xDB
mem_write8 [expr {$addr + 1}] 3
mem_write8 [expr {$addr + 2}] 1
set new_write [expr {$write + 3}]
if {$new_write >= $size} { set new_write [expr {$new_write - $size}] }
mem_write32 [expr {$RINGBUF_ADDR + 8}] $new_write
echo "Injected BT release (0xDB, 3, 1) write=$write->$new_write"
resume

echo "Done — exiting OpenOCD"
shutdown
