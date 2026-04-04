# DICE3 USB Architecture — TC Helicon Blender

Reverse-engineered from Ghidra analysis of `blender_primary_body.bin` (ARM926EJ-S, eCos RTOS).
All addresses are primary firmware SRAM addresses (image base 0x200).

## USB Controller

- **Type**: Freescale/ChipIdea USB OTG
- **MMIO Base**: `0x90000000`
- **IRQ**: 14

### Key Registers

| Register | Address | Purpose |
|----------|---------|---------|
| OTGSC | 0x90000004 | OTG status/control |
| MODE | 0x90000008 | Controller mode |
| ENDPTSETUPSTAT | 0x9000000C | Setup status |
| ENDPTPRIME | 0x90000010 | Endpoint prime (start) |
| ENDPTFLUSH | 0x90000014 | Endpoint flush |
| ENDPTSTAT | 0x90000018 | Endpoint status (active mask) |
| ENDPTCOMPLETE | 0x9000001C | Endpoint complete flags |
| EP_LIST_ADDR | 0x90000100 | Queue head base address |
| EP0_TX_QH | 0x90000104 | EP0 TX queue head |
| EP0_RX_QH | 0x90000108 | EP0 RX queue head |
| EP1_TX_QH | 0x9000010C | EP1 TX queue head |
| ... | +0x04/EP | Alternating TX/RX per endpoint |
| ENDPTPRIME2 | 0x90000834 | Explicit endpoint prime register |

Queue heads are spaced 0x80 bytes apart, 6 TX + 6 RX endpoints initialized at boot.

### Endpoint Priming

To arm an endpoint for receive:
1. Build a dTD (Device Transfer Descriptor) with buffer pointer + length
2. Link dTD to queue head
3. Set queue head word[0] |= `0x84000000` (active + prime bits)
4. USB controller reads dTD, arms endpoint for next host OUT token
5. Host sends OUT → DMA fills buffer → completion interrupt fires

## USB Endpoints (Blender)

| EP | Direction | Type | Interface | Purpose |
|----|-----------|------|-----------|---------|
| 0x00 | CTRL | Control | 0 | USB control pipe (DCP vendor commands) |
| 0x81 | IN | Interrupt | 0 | Audio control status |
| 0x08 | OUT | Isochronous | 1 (alt 1) | Audio playback (2ch S32_LE 48kHz) |
| 0x88 | IN | Isochronous | 2 (alt 1) | Audio capture (12ch S32_LE 48kHz) |
| 0x82 | IN | Bulk | 4 | MIDI Out (device → host) - **WORKS** |
| 0x03 | OUT | Bulk | 4 | MIDI In (host → device) - **NEVER ARMED** |

## USB Initialization

### Boot Sequence

```
firmware_entry (0x344)
  → rtos_app_init (0x9CD4)
    → dice3_device_thread_start (0x13B6C)
      → dice3_device_init (0x13760)
        → usb_hw_controller_init (0x214A8)  — programs 0x90000000 registers
        → dice_usb_init (0x1B740)
          → usb_midi_rx_endpoint_register (0x1BEA8) — adds EP node to linked list
          → dice_usb_midi_subsystem_init (0x195C0) — alarm, ringbuf, parser
          → dcp_register_handler × 4 — standard DCP categories
        → dcp_usb_init (0x1BF30) — creates DCP dispatch thread
```

### USB Controller Init (`usb_hw_controller_init` @ 0x214A8)

1. Write `0x82` to `0xC9000024` (clock config)
2. Initialize 6 TX + 6 RX endpoint descriptor arrays
3. Set queue head base: `0x90000104` + offsets from descriptor table
4. Configure USB mode, speed, interrupt mask
5. Clear all endpoint status/complete registers
6. Initialize EP0 queue head with control capability bit (`0x8000000`)
7. Create ISR: `cyg_interrupt_create(14, ...)` → `usb_hw_dsr_interrupt_dispatch`
8. Unmask IRQ 14, enable controller (`0x90000008 |= 1`)

## MIDI Subsystem

### Data Structures

```
dice3midi subsystem base: 0x29404
  +0x00: RX ring buffer (base=0x2D2D8, size=0x404)
  +0x2C: TX ring buffer (base=0x2D6DC, size=0x104)
  +0x34: name string "dice3midi" (at 0x29464)
  +0x48: TX callback pointer (triggers USB IN transfer)

MIDI parser context: 0x29764
  +0x00: status byte (running status)
  +0x01-0x17: data buffer
  +0x18: write position
  +0x1C: expected message length
  +0x20: state (0=idle, 1=channel msg, 2=sysex)
  +0x28: channel_msg_cb function pointer  ← WE PATCH THIS
  +0x2C: sysex_cb → 0x2A34
  +0x30: realtime_cb → 0x2A30

MIDI packet buffer: 0x30D64
  +0x000: USB-MIDI 4-byte packet array (0x200 words = 128 packets)
  +0x200: write_idx (packet count from last USB transfer)
  +0x204: read_idx (packets consumed so far)
  +0x208: EP handle pointer (set by usb_midi_rx_start)
  +0x20C: num_cables (byte)
  +0x20D: token/sequence byte (set to 0xFF during processing)

Mixer state: 0x2D81C (36 bytes)
  +0x00: bus[0].inputs[7]
  +0x07: bus[1].inputs[7]
  +0x0E: bus[2].inputs[7]
  +0x15: bus[3].inputs[7]
  +0x1C: compressor[4]
  +0x20: master[4]
```

### MIDI TX Path (device → host) — WORKING

```
midi_send_cc(channel, cc_num, cc_val)           [our handler code]
  → midi_tx_write(msg, 3)                       [0x1114]
    → ringbuf_write(dice3midi+0x2C, data, len)  [0x6B10]
    → if (*(base+0x48) != 0) call TX callback   — triggers USB IN transfer
  → USB controller → EP 0x82 IN (bulk)
  → host kernel snd-usbmidi-lib → ALSA sequencer
```

### MIDI RX Path (host → device) — BROKEN

```
Host amidi/ALSA → EP 0x03 OUT (bulk)
  → USB controller queue head → DMA → buffer at 0x30D64
  → usb_midi_rx_complete (0x1B9E4) — DMA completion callback
    → parses USB-MIDI 4-byte packets (CIN + 3 MIDI bytes)
    → strips headers → writes raw MIDI to dice3midi RX ringbuf
  → midi_engine_thread (0x5010) polls via cyg_flag_wait (~50Hz)
    → midi_rx_poll (0x2AB8)
      → ringbuf_read(16 bytes)
      → midi_parser_process_bytes (0x3ED0)
        → channel_msg_cb at midi_parser_ctx+0x28
          → stock: midi_channel_msg_cb (0x2A44) — DEAD STUB
          → patched: midi_cc_handler (our code) — applies CC to mixer
```

**Why it's broken (stock firmware)**: EP 0x03 OUT is never primed for MIDI-only use. The arming path only triggers via `usb_audio_set_alt_setting` → `usb_midi_streaming_start` when the host sends `SET_INTERFACE(iface=1, alt=1)`, which the kernel only sends when audio streaming starts (`arecord`/`aplay`).

**Our fix**: The patched firmware calls `usb_midi_rx_start()` directly from the main-loop hook, bypassing all streaming gates. EP state byte forced to 0x02 ("configured"). Re-arming is automatic via the self-sustaining `usb_midi_rx_complete` → `usb_midi_rx_dispatch` cycle. Standard MIDI tools (`amidi`, `aconnect`, sequencers) work natively.

### Stock MIDI Callback (`midi_channel_msg_cb` @ 0x2A44)

Dead stub. Disassembly:
- 0xF2 (Song Position): ignored
- 0xD0 (Channel Pressure): ignored
- 0xA0 (Poly Aftertouch): ignored
- 0x80 (Note Off): copies to stack local, discards (no-op)
- **Everything else** (CC, Note On, Program Change, Pitch Bend): falls through to return

TC Helicon built the entire MIDI RX infrastructure but the callback does nothing. MIDI control was planned but never shipped.

## EP 0x03 Arming — Full Call Graph

### Layer 1: Hardware

```
Freescale/ChipIdea USB OTG at 0x90000000
  Queue heads at 0x90000104+
  To receive: prime queue head (word[0] |= 0x84000000)
```

### Layer 2: USB HAL (`usb_hw_ep_start_transfer` @ 0x2140C)

```c
/* Ghidra struct: usb_ep_handle (48 bytes, applied at 0x2A7E8) */
struct usb_ep_handle {
    void  *start_transfer;   // +0x00: vtable[0] = usb_hw_ep_start_transfer (0x2140C)
    void  *read_byte;        // +0x04: vtable[1] = usb_midi_rx_read_byte (0x1B960)
    void  *completion_cb;    // +0x08: DMA completion callback (0 = idle, checked before arm)
    void  *cb_context;       // +0x0C: passed as arg1 to completion_cb
    void  *buffer_ptr;       // +0x10: DMA buffer (must be non-NULL, 4-byte aligned)
    uint   buffer_size;      // +0x14: transfer size (must be 4-byte aligned)
    uint   reserved_18;      // +0x18
    // --- dTD (device transfer descriptor) starts here, passed to usb_hw_ep_dma_start ---
    void  *qh_ptr;           // +0x1C: queue head in USB controller SRAM (set at boot)
    byte   state;            // +0x20: EP state (also dTD byte 0: set to 3 on DMA start)
    byte   dtd_flags[3];     // +0x21: dTD metadata (error_code at dtd_flags[1..2])
    uint   dtd_field2;       // +0x24
    void  *dma_buf_copy;     // +0x28: shadow copy of buffer_ptr (set by start_transfer)
    uint   dma_size_copy;    // +0x2C: shadow copy of buffer_size
};
// States: 1=init, 2=configured, 3=active, 4=stalled, 0x84=suspended
// usb_hw_ep_dma_start writes state=3 (active) after priming the queue head
// usb_hw_ep_state_set (0x20FBC) sets state from USB ISR on bus events
```

PRECONDITION: `ep->state` must be 0x02 ("configured"). **This is never set for EP 0x03
in MIDI-only mode** — `usb_endpoint_start_if_configured` (which propagates state) only
runs inside `usb_midi_streaming_start`, gated on audio streaming. Our patch forces it.

Verified against implant disassembly:
- `ldr r2, [r3, #8]` → checks `ep->completion_cb` (offset 0x08)
- `strb r2, [r3, #32]` → forces `ep->state = 2` (offset 0x20 = decimal 32)
- `bl usb_midi_rx_start` with r0=ep, r1=1 → correct args

### Layer 3: Transfer Submit (`usb_endpoint_submit_transfer` @ 0x2077C)

```c
void usb_endpoint_submit_transfer(ep, buf, size, callback, ctx) {
    ep[2] = callback;    // +0x08
    ep[4] = buf;         // +0x10
    ep[3] = ctx;         // +0x0C
    ep[5] = size;        // +0x14
    (*ep[0])(ep);        // → usb_hw_ep_start_transfer
}
```

PRECONDITION: EP state == 0x02

### Layer 4: MIDI RX Start (`usb_midi_rx_start` @ 0x1BE50)

```c
void usb_midi_rx_start(ep_handle, num_cables) {
    midi_buf.write_idx = 0;
    midi_buf.read_idx = 0;
    midi_buf.ep_handle = ep_handle;
    midi_buf.num_cables = num_cables;
    midi_buf.token = 0xFF;
    if (num_cables != 0 && *(ep_handle + 8) == 0) {  // GATE
        usb_midi_rx_dispatch();  // → usb_endpoint_submit_transfer
    }
}
```

PRECONDITION: ep+8 (completion_callback) must be 0

### Layer 5: Streaming Start (`usb_midi_streaming_start` @ 0x19C40)

```c
void usb_midi_streaming_start() {
    ep_tx = usb_iface_get_tx_endpoint(usb_midi_conn, 2);
    usb_midi_tx_endpoint_start(ep_tx, cable_config[0]);

    if (usb_endpoint_descriptor_build(midi_state + 0x20, 3) == 0)
        return;                                           // GATE 1: alt-setting descriptor

    usb_endpoint_start_if_configured(usb_midi_conn);      // GATE 2: conn+0x2BC >= 5

    ep_rx = usb_iface_get_rx_endpoint(usb_midi_conn, 3);
    usb_midi_rx_start(ep_rx, cable_config[2]);
    dcp_streaming_state_set(1);
    // ... also configures clock source and notifies audio driver
}
```

PRECONDITIONS: alt-setting descriptor valid, config state >= 5, ep->completion_cb == 0

**No static callers.** Registered as a callback in the MIDI state struct during init.
Called indirectly through the vtable chain triggered by `usb_audio_set_alt_setting`.

### Layer 6: Trigger (`usb_audio_set_alt_setting` @ 0x1B30C)

```c
int usb_audio_set_alt_setting(int alt_setting) {
    if (alt_setting == 0) return 0;
    midi_state->alt_setting_idx = alt_setting - 1;       // +0x54
    host_conn = *(midi_state + 0x474);                    // USB host connection obj
    vtable = *(void **)host_conn;
    vtable[7](host_conn, midi_state->sample_rate, flags); // → endpoint configure chain
    return 1;                                              // → eventually fires streaming_start
}
```

PRECONDITION: Host sends USB SET_INTERFACE(iface=1, alt=1+)

### Layer 7: USB Control Request (`usb_audio_ctrl_iface_handler` @ 0x1B3BC)

```c
void usb_audio_ctrl_iface_handler(void *ctx, byte *setup_pkt, void *param3) {
    uint bmRequestType = setup_pkt[0];
    // Route DCP vendor requests (class=1, type=interface|vendor)
    if ((bmRequestType & 0x1F) == 1 && (bmRequestType & 0x60) == 0x40) {
        dcp_usb_rx_handler(setup_pkt);  // DCP vendor path
        return;
    }
    // Route class requests (type=interface|class)
    if ((bmRequestType & 0x60) == 0x20 && (bmRequestType & 0x1F) == 1) {
        if (setup_pkt[1] == 1) {  // SET_CUR
            if (setup_pkt[5] == 0x28) {                        // CS_INTERFACE (alt-setting)
                usb_audio_set_alt_setting(alt & 0xFF);
            } else if (setup_pkt[5] >= 0x29) {                 // CS_ENDPOINT (sample rate)
                usb_audio_set_sample_rate_cmd(data);
            }
        }
    }
}
```

Registered as function pointer at 0x2A178 (data xref). The USB device stack dispatches
EP0 setup packets here for interface-targeted class/vendor requests.

### Full Stock Trigger Chain

```
Host starts audio streaming (aplay/arecord)
  → kernel sends SET_INTERFACE(iface=1, alt=1)
  → USB ISR delivers setup packet to control pipe
  → usb_audio_ctrl_iface_handler (0x1B3BC)
      route: SET_CUR, class request, descriptor=0x28
  → usb_audio_set_alt_setting(1) (0x1B30C)
      stores alt_setting-1 at midi_state+0x54
      dispatches via vtable[7] on host connection at *(midi_state+0x474)
  → [vtable chain — usb_midi_streaming_start registered as indirect callback]
  → usb_midi_streaming_start (0x19C40)
      gate: usb_endpoint_descriptor_build(midi_state+0x20, 3) — alt-setting
      gate: usb_endpoint_start_if_configured(conn) — conn+0x2BC >= 5
  → usb_iface_get_rx_endpoint(conn, 3) → ep_handle at 0x2A7E8
  → usb_midi_rx_start(ep, cables) (0x1BE50)
      gate: ep->completion_cb == 0
  → usb_midi_rx_dispatch (0x1B7F0)
  → usb_endpoint_submit_transfer(ep, midi_pkt_buf, 0x200, usb_midi_rx_complete, 0)
  → usb_hw_ep_start_transfer(ep) (0x2140C)
      gate: ep->state == 0x02
  → usb_hw_ep_dma_start(ep->dTD) (0x20EAC)
      → queue_head->word[0] |= 0x84000000  — EP ARMED
```

### Summary of Preconditions

| # | Gate | Stock | Patched | Set by |
|---|------|-------|---------|--------|
| 1 | USB controller initialized | OK | OK | Boot (`usb_hw_controller_init`) |
| 2 | `ep->state` == 0x02 | FAIL | **FORCED** | Stock: `usb_endpoint_start_if_configured` / Patched: `arm_midi_rx_endpoint` writes 0x02 |
| 3 | Config state >= 5 | FAIL | **BYPASSED** | Stock: SET_INTERFACE / Patched: calls `usb_midi_rx_start` directly |
| 4 | Alt-setting descriptor valid | FAIL | **BYPASSED** | Stock: SET_INTERFACE / Patched: skips `usb_midi_streaming_start` |
| 5 | `ep->completion_cb` == 0 | OK | OK | Cleared by `usb_midi_rx_complete` on each transfer |
| 6 | MIDI parser callback patched | — | OK | `boot_init` at 0x344 / main-loop hook |

Gates 2–4 require SET_INTERFACE from host, which only fires during audio streaming.
Our patch bypasses all three.

## Working Solution: Native USB MIDI + DCP Flash

EP 0x03 is armed by the patched firmware's main-loop hook (`arm_midi_rx_endpoint` in handlers.c).
Standard MIDI tools work natively. DCP remains for flash operations and diagnostics:

### DCP Category 0x81F Opcodes

| Opcode | Name | Body (host→device) | Response |
|--------|------|--------------------|----------|
| 0 | INFO | (none) | 12 bytes: JEDEC ID, sector size, flash size |
| 1 | READ | addr:u32, len:u16 | raw data (max 1024 bytes) |
| 2 | ERASE | addr:u32, len:u32 | status:u32 |
| 3 | WRITE | addr:u32, data:N | status:u32 |
| 4 | REBOOT | (none) | status:u32 (then reboots) |
| 5 | MIDI_INIT | (none) | midi_patched:u32 (1=ok) |
| 6 | MIDI_SEND | raw MIDI bytes (3 per CC) | status:u32 (0=ok) |

### Bidirectional CC Flow

```
Host → Device (native MIDI bulk EP 0x03 OUT):
  amidi / aconnect / midir → ALSA MIDI output → kernel snd-usbmidi-lib → EP 0x03 OUT
  → USB controller DMA → midi_pkt_buf → usb_midi_rx_complete (0x1B9E4)
  → USB-MIDI packet parser → dice3midi RX ringbuf → midi_engine_thread (0x5010)
  → midi_parser_process_bytes (0x3ED0) → midi_cc_handler (patched callback)
  → mixer_state update → *_apply() → midi_send_cc() echoes back via TX

Device → Host (MIDI bulk EP 0x82 IN):
  firmware midi_send_cc() → midi_tx_write → TX ringbuf → USB IN
  → kernel snd-usbmidi-lib → ALSA sequencer → midir callback
  → MixerState update → watch channel → TUI redraw

Legacy: Host → Device via DCP opcode 6 still works as fallback (vendor control on EP 0).
```

### Key Constraint: No Interface Claiming

The DCP handle must NOT claim or detach any USB interface. The kernel `snd-usb-audio` driver owns all interfaces. Vendor control transfers on EP 0 work without claiming — they go through the default control pipe which is always available.

If any interface is detached/claimed/released, the kernel re-probes the device and the MIDI port disappears permanently (kernel logs "USB device is in the shutdown state").

## DCP Protocol Details

### Packet Format (16-byte header + body)

```
Offset  Size  Field
0x00    u32   cmd_id = (category << 12) | opcode
0x04    u16   body_length
0x06    u16   cmd_idx (sequence counter, increments per command)
0x08    u64   reserved (zeros)
0x10+         body (body_length bytes)
```

### USB Transport

- **Write**: `bRequest=2, bmRequestType=OUT|VENDOR|INTERFACE, wValue=0, wIndex=0`
- **Read**: `bRequest=3, bmRequestType=IN|VENDOR|INTERFACE, wValue=0, wIndex=0`
- Response matches request cmd_id and cmd_idx
- Stale responses (wrong cmd_idx) are discarded on retry

### Handler Registration

DCP handler nodes form a linked list at `dcp_state + 0x44C` (= 0x313C4):

```
struct handler_node {
    void     *next;        // +0x00
    uint16_t  category;    // +0x04 (low 16 bits)
    uint16_t  padding;     // +0x06
    void     *handler;     // +0x08: fn(ctx, category, opcode, body, body_len)
    void     *context;     // +0x0C: passed as first arg
};
```

Dispatcher: `dcp_command_dispatch_thread` (0x1C3BC) — waits on mutex, extracts category from cmd_id bits [23:12], walks list, calls matching handler.

## Firmware Functions Reference

### USB Controller HAL

| Address | Name | Signature |
|---------|------|-----------|
| 0x214A8 | usb_hw_controller_init | (usb_dev_state, ...) |
| 0x2140C | usb_hw_ep_start_transfer | (ep_handle) — checks state, programs DMA |
| 0x20EAC | usb_hw_ep_dma_start | (dTD_ptr) — writes queue head, primes EP |
| 0x20FBC | usb_hw_ep_state_set | (usb_dev, new_state) — state machine |
| 0x21D70 | usb_hw_dsr_interrupt_dispatch | ISR handler for IRQ 14 |

### USB MIDI

| Address | Name | Signature |
|---------|------|-----------|
| 0x1B740 | dice_usb_init | (usb_dev) — registers endpoints + DCP handlers |
| 0x1BEA8 | usb_midi_rx_endpoint_register | () — adds EP to linked list |
| 0x195C0 | dice_usb_midi_subsystem_init | () — alarm, ringbuf, parser |
| 0x19C40 | usb_midi_streaming_start | () — arms EP 0x03 (never called) |
| 0x1BE50 | usb_midi_rx_start | (ep_handle, num_cables) |
| 0x1B9E4 | usb_midi_rx_complete | (ctx, byte_count) — DMA completion |
| 0x1B960 | usb_midi_rx_read_byte | (ep, out_byte) — per-byte consumer |
| 0x1B30C | usb_audio_set_alt_setting | (alt) — SET_INTERFACE handler |
| 0x1B3BC | usb_audio_ctrl_iface_handler | — dispatches control requests |
| 0x1FF34 | usb_endpoint_start_if_configured | (conn) — checks state >= 5 |
| 0x2077C | usb_endpoint_submit_transfer | (ep, buf, size, cb, ctx) |
| 0x20754 | usb_midi_get_rx_ep | (conn, type) — returns EP handle |
| 0x108E4 | usb_rx_endpoint_find | (name_str) — searches EP linked list |
| 0x10928 | usb_tx_endpoint_find | (name_str) |
| 0x109AC | usb_endpoint_link_handler | (ep, handler_node) |
| 0x1096C | usb_endpoint_unlink_handler | (handler_node) |
| 0x1080  | midi_usb_endpoints_setup | () — called from midi_engine_thread |

### MIDI Engine

| Address | Name | Signature |
|---------|------|-----------|
| 0x5010  | midi_engine_thread | () — main loop: flag_wait → poll → dispatch |
| 0x2998  | midi_engine_start | () |
| 0x29D8  | midi_engine_stop | () |
| 0x2AB8  | midi_rx_poll | () — reads ringbuf → parser |
| 0x3ED0  | midi_parser_process_bytes | (ctx, data) |
| 0x2A44  | midi_channel_msg_cb | (ctx, data, len, ...) — stock stub (no-op) |
| 0x1114  | midi_tx_write | (data, len) → int bytes_written |

### DCP

| Address | Name | Signature |
|---------|------|-----------|
| 0x1BEE0 | dcp_register_handler | (node) — prepends to list |
| 0x1C1F8 | dcp_send_response | (retcode, body, len) |
| 0x1C3BC | dcp_command_dispatch_thread | () — worker thread |
| 0x1BF30 | dcp_usb_init | () — creates dispatch thread |
| 0x1BF6C | dcp_usb_rx_handler | () — USB control transfer → DCP state |
| 0x1C454 | dcp_streaming_state_set | (state) |
| 0x1C4A0 | dcp_install_handler_with_data | (category, handler, ...) |

### Mixer

| Address | Name | Signature |
|---------|------|-----------|
| 0x1468  | channel_level_apply | (bus, input, level) — level 0-31 |
| 0x45C0  | master_level_apply | (bus, level) |
| 0x13F0  | compressor_level_apply | (bus, level) |
| 0x4570  | mute_set | (bus, on_off) |
| 0x68F4  | led_mute_update | (input, on_off) |
| 0x1188  | mixer_reset_defaults | () |
| 0x2018  | mixer_init_state | () |

### BLE

| Address | Name | Signature |
|---------|------|-----------|
| 0x21E8  | ble_param_dispatch | () — processes BLE tuples |
| 0x618C  | ble_write_tuple | (param, sub, val) — outbound to BLE SPI |
| 0x6528  | ble_send_mixer_state | () — full state dump |
| 0x6044  | ble_spi_rx_poll | () — drains SPI FIFO |
| 0x2F38  | ble_spi_read_tuple | () — displaced fn at hook site 0x4FAC |

### Boot

| Address | Name | Signature |
|---------|------|-----------|
| 0x344   | firmware_entry | () — reset vector, BSS clear, .ctors, rtos_app_init |
| 0x9CD4  | rtos_app_init | () — starts eCos scheduler (never returns) |
| 0x13760 | dice3_device_init | (dev_obj) — vtable-driven init |
| 0x9414  | hal_platform_init | () — timer, DMA, UART, IRQ |

### eCos Kernel

| Address | Name | Signature |
|---------|------|-----------|
| 0xA44C  | cyg_thread_create | (priority, entry, arg, name, stack, stack_size, handle, thread_obj) |
| 0xA49C  | cyg_thread_resume | (handle) |
| 0xA75C  | cyg_flag_wait | (flag, pattern, mode) |
| 0xA758  | cyg_flag_setbits | (flag, bits) |
| 0xA6FC  | cyg_alarm_create | (counter, fn, data, handle, alarm_obj) |
| 0xA734  | cyg_alarm_initialize | (alarm, trigger, interval) |
| 0xAAF0  | cyg_current_time | () → tick_count |

### Threads

| Thread | Entry | Stack | Created by |
|--------|-------|-------|------------|
| DICE3 device | 0x13760 | — | dice3_device_thread_start |
| DCP dispatch | 0x1C3BC | — | dcp_usb_init |
| MIDI engine | 0x5010 | — | self (at boot) |
| MIDI RX | — | — | midi_engine_thread |
| Audio driver | — | — | dice3_audio_driver_init |
| USB device | — | — | usb_device_thread_start |
| UART console | 0x1ECDC | — | dice3_device_init |

## Key Data Addresses

| Address | Name | Ghidra Type | Size | Content |
|---------|------|-------------|------|---------|
| 0x29404 | dice3midi_base | `dice3midi_t` | 76 | MIDI subsystem struct (RX+TX ringbufs, TX callback) |
| 0x29764 | midi_parser_ctx | `midi_parser_ctx_t` | 52 | MIDI parser state machine (channel_msg_cb at +0x28) |
| 0x2A1C0 | midi_rx_ep_node | `dcp_handler_node` | 16 | RX endpoint linked list node |
| 0x2A230 | usb_midi_conn | — | ~0x300 | USB MIDI connection state (vtable at word 0) |
| 0x2A7E8 | midi_rx_ep_handle | `usb_ep_handle` | 40 | EP 0x03 OUT handle (state at +0x24, dTD at +0x1C) |
| 0x2A93C | sst25xx_driver_ctx | — | — | SPI flash driver context |
| 0x2D81C | mixer_state | `mixer_state_t` | 36 | bus[4]×inputs[7] + comp[4] + master[4] |
| 0x30D64 | midi_pkt_buf | `midi_pkt_buf_t` | 526 | USB-MIDI 4-byte packets + write/read idx + EP handle ptr |
| 0x30F78 | dcp_state | — | ~0x500 | DCP state machine + handler list |
| 0x313C4 | dcp_handler_list | `dcp_handler_node *` | 4 | Linked list head pointer |

## Ghidra Struct Definitions

All structs defined in `blender_primary_body.bin` Ghidra project. Applied to data addresses
where within the loaded binary range (0x200–0x2AA83). BSS addresses (0x2D81C+) are
outside the binary and typed via `firmware_symbols.ld` in the patch framework instead.

```c
struct usb_ep_handle {       // 48 bytes — applied at 0x2A7E8
    void  *start_transfer;   // +0x00: vtable[0] → usb_hw_ep_start_transfer
    void  *read_byte;        // +0x04: vtable[1] → usb_midi_rx_read_byte
    void  *completion_cb;    // +0x08: DMA completion callback (0 = idle)
    void  *cb_context;       // +0x0C: arg1 for completion_cb
    void  *buffer_ptr;       // +0x10: DMA buffer (4-byte aligned)
    uint   buffer_size;      // +0x14: transfer size (4-byte aligned)
    uint   reserved_18;      // +0x18
    void  *qh_ptr;           // +0x1C: queue head pointer (dTD starts here)
    byte   state;            // +0x20: 1=init, 2=configured, 3=active, 4=stalled, 0x84=suspended
    byte   dtd_flags[3];     // +0x21: dTD metadata (error_code at +1..2)
    uint   dtd_field2;       // +0x24
    void  *dma_buf_copy;     // +0x28: shadow copy of buffer_ptr
    uint   dma_size_copy;    // +0x2C: shadow copy of buffer_size
};

struct dcp_handler_node {    // 16 bytes — applied at 0x2A1C0
    void  *next;             // +0x00: linked list next
    ushort category;         // +0x04: DCP category (e.g. 0x81F)
    ushort padding;          // +0x06
    void  *handler;          // +0x08: fn(ctx, category, opcode, body, body_len)
    void  *context;          // +0x0C: passed as first arg
};

struct midi_parser_ctx_t {   // 52 bytes — applied at 0x29764
    byte   status;           // +0x00: running status byte
    byte   data_buf[23];     // +0x01: parser data buffer
    uint   write_pos;        // +0x18
    uint   expected_len;     // +0x1C
    uint   state;            // +0x20: 0=idle, 1=channel_msg, 2=sysex
    uint   reserved_24;      // +0x24
    void  *channel_msg_cb;   // +0x28: ← PATCHED to midi_cc_handler
    void  *sysex_cb;         // +0x2C: → 0x2A34
    void  *realtime_cb;      // +0x30: → 0x2A30
};

struct midi_pkt_buf_t {      // 526 bytes — at 0x30D64 (BSS, not in binary)
    uint   packets[128];     // +0x000: USB-MIDI 4-byte packet array
    uint   write_idx;        // +0x200: packet count from last USB transfer
    uint   read_idx;         // +0x204: packets consumed
    void  *ep_handle;        // +0x208: set by usb_midi_rx_start
    byte   num_cables;       // +0x20C
    byte   token;            // +0x20D: set to 0xFF during processing
};

struct mixer_state_t {       // 36 bytes — at 0x2D81C (BSS)
    byte bus0_inputs[7];     // +0x00: bus A inputs 0-6 (level 0-31)
    byte bus1_inputs[7];     // +0x07: bus B
    byte bus2_inputs[7];     // +0x0E: bus C
    byte bus3_inputs[7];     // +0x15: bus D
    byte compressor[4];      // +0x1C: per-bus compressor level
    byte master[4];          // +0x20: per-bus master level
};

struct dcp_packet_header {   // 16 bytes
    uint   cmd_id;           // +0x00: (category << 12) | opcode
    ushort body_length;      // +0x04
    ushort cmd_idx;          // +0x06: sequence counter
    byte   reserved[8];      // +0x08
};

struct dice3midi_t {         // 76 bytes — applied at 0x29404
    byte   rx_ringbuf[44];   // +0x00: RX ring buffer (base=0x2D2D8, size=0x404)
    byte   tx_ringbuf[8];    // +0x2C: TX ring buffer (base=0x2D6DC, size=0x104)
    void  *name;             // +0x34: → "dice3midi"
    byte   reserved_38[20];  // +0x38
    void  *tx_callback;      // +0x48: triggers USB IN transfer
};
```
