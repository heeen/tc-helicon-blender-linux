#!/usr/bin/env openocd
# trigger_bt.tcl — Trigger the BT/PAIR button on the TC Helicon Blender via JTAG
#
# Usage:
#   openocd -f dice3-openocd.cfg -f jtag/trigger_bt.tcl
#
# Then in telnet (localhost:4444):
#   trigger_all_buttons
#   identify_button
#   trigger_one N
#
# Ring buffer struct for button events at 0x29834 in RAM:
#   +0: base   (pointer to data buffer)
#   +4: size   (buffer capacity in bytes)
#   +8: write  (write index)
#   +12: read  (read index)
#
# Button events are 3-byte tuples: (0xDB, button_idx, state)
#   state: 0=pressed, 1=released, 2=long press

set RINGBUF_ADDR 0x29834
set GPIO_BASE 0x297e8

# --- Helper: read a 32-bit word from memory ---
proc mem_read32 {addr} {
    set vals [read_memory $addr 32 1]
    return [lindex $vals 0]
}

# --- Helper: read a byte from memory ---
proc mem_read8 {addr} {
    set vals [read_memory $addr 8 1]
    return [lindex $vals 0]
}

# --- Helper: write a byte to memory ---
proc mem_write8 {addr val} {
    write_memory $addr 8 [list $val]
}

# --- Helper: write a 32-bit word to memory ---
proc mem_write32 {addr val} {
    write_memory $addr 32 [list $val]
}

proc read_ringbuf {} {
    global RINGBUF_ADDR
    set vals [read_memory $RINGBUF_ADDR 32 4]
    set base  [lindex $vals 0]
    set size  [lindex $vals 1]
    set write [lindex $vals 2]
    set read  [lindex $vals 3]
    echo "Ring buffer: base=0x[format %08x $base] size=$size write=$write read=$read"
    return [list $base $size $write $read]
}

proc ringbuf_inject {param_id sub_param value} {
    global RINGBUF_ADDR
    set vals [read_memory $RINGBUF_ADDR 32 4]
    set base  [lindex $vals 0]
    set size  [lindex $vals 1]
    set write [lindex $vals 2]
    set read  [lindex $vals 3]

    # Check free space
    if {$write >= $read} {
        set free [expr {$size - $write + $read - 1}]
    } else {
        set free [expr {$read - $write - 1}]
    }

    if {$free < 3} {
        echo "ERROR: ring buffer full (free=$free, need 3)"
        return 0
    }

    # Write 3 bytes at base + write
    set addr [expr {$base + $write}]
    mem_write8 $addr $param_id
    mem_write8 [expr {$addr + 1}] $sub_param
    mem_write8 [expr {$addr + 2}] $value

    # Advance write index (wrap at size)
    set new_write [expr {$write + 3}]
    if {$new_write >= $size} {
        set new_write [expr {$new_write - $size}]
    }
    mem_write32 [expr {$RINGBUF_ADDR + 8}] $new_write

    echo "  Injected: (0x[format %02x $param_id], $sub_param, $value) write=$write->$new_write"
    return 1
}

proc trigger_button {idx} {
    echo "=== Button $idx: press ==="
    halt
    ringbuf_inject 0xDB $idx 0
    resume
    sleep 200
    echo "=== Button $idx: release ==="
    halt
    ringbuf_inject 0xDB $idx 1
    resume
    sleep 500
}

proc trigger_all_buttons {} {
    echo ""
    echo "=== Triggering all 16 buttons ==="
    echo "=== Monitor BLE advertising with: bluetoothctl scan on ==="
    echo ""
    for {set i 0} {$i < 16} {incr i} {
        trigger_button $i
        echo ""
    }
    echo "Done. Check if Blender started advertising."
}

proc trigger_one {idx} {
    echo "Triggering button $idx..."
    trigger_button $idx
    echo "Done."
}

# --- GPIO reading ---

proc read_buttons {} {
    global GPIO_BASE
    set vals [read_memory [expr {$GPIO_BASE + 1}] 8 2]
    set row1 [lindex $vals 0]
    set row2 [lindex $vals 1]
    echo "GPIO row1=0x[format %02x $row1] (buttons 0-7)  row2=0x[format %02x $row2] (buttons 8-15)"
    # Button pressed when GPIO bit = 0 (active low)
    set pressed {}
    for {set bit 0} {$bit < 8} {incr bit} {
        if {!($row1 & (1 << $bit))} {
            lappend pressed $bit
        }
    }
    for {set bit 0} {$bit < 8} {incr bit} {
        if {!($row2 & (1 << $bit))} {
            lappend pressed [expr {$bit + 8}]
        }
    }
    if {[llength $pressed] > 0} {
        echo "Pressed buttons: $pressed"
    } else {
        echo "No buttons pressed"
    }
    return [list $row1 $row2]
}

proc identify_button {} {
    echo ""
    echo "=== Button identification mode ==="
    echo "Press and HOLD the BT button on the Blender..."
    echo "Reading GPIO 30 times at 500ms intervals:"
    echo ""
    for {set i 0} {$i < 30} {incr i} {
        halt
        read_buttons
        resume
        sleep 500
    }
}

# Main: show usage
echo ""
echo "TC Helicon Blender — JTAG Button Trigger"
echo ""
echo "Commands:"
echo "  trigger_all_buttons  — try all 16 button indices (brute force)"
echo "  trigger_one N        — trigger button N (0-15)"
echo "  identify_button      — read GPIO while you press BT (30 readings)"
echo "  read_buttons         — read GPIO button state once"
echo "  read_ringbuf         — show ring buffer status"
echo ""
