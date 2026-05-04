# BLE Module — Firmware Side

Single canonical source for the BLE bridge protocol, GPIO map, and
firmware-side handlers. All findings from Ghidra decompile of
`blender_primary_body.bin` (image base `0x200`) unless noted.

GPIO pin assignments are also in `hardware-reference.md` § "GPIO" — this
file goes deeper on BLE-specific protocol and bring-up.

Inline status tag convention: see `hardware-reference.md` header.
[verified 2026-04-26 Ghidra; updated 2026-05-04 with full pin map
from `hal_hardware_init` xref pass]

The external "BLE module" is a separate MCU on the board (TCH-BLE) that owns
the radio + GATT. The DICE3 talks to it over a 3-wire SPI link plus GPIOs and
exchanges 3-byte tuples (`param`, `sub`, `value`). The DICE3 has no IRQ from
the module; the bridge is **strictly polled** at the rate of the MIDI engine
flag-wait tick.

---

## 1. Pins (decoded from `gpio_write_sdk` @ `0x9134`)

```c
gpio_write_sdk(pin, val):
  base   = (pin < 8) ? 0xCA000000 : 0xCB000000;
  offset = (1 << (pin & 7)) * 4;
  *(u32*)(base + offset) = (val ? (1 << (pin & 7)) : 0);
```

So each `gpio_write_sdk(N, …)` writes a single bit-mask register, one register
per pin (write-only, 1 = drive high, 0 = drive low; mask in low byte).

| Pin | Register      | Used as                                                  |
| --: | :------------ | :------------------------------------------------------- |
|   6 | `0xCA000100`  | BLE module RESET line  (`ble_spi_reset`)                 |
|   7 | `0xCA000200`  | BLE module power / mode strap (`ble_spi_reset`)          |
|   8 | `0xCB000004`  | Driven high after `cyg_thread_resume(led_update_thread)` (BLE module enable) |
|  11 | `0xCB000020`  | BLE chip-select / strobe per outgoing tuple (`ble_spi_write_tuple`) |

`ble_spi_reset` also writes `0` to `0xCB000040` (pin 12 register) at exit.

---

## 2. Bring-up — `midi_engine_thread` @ `0x5010`

This thread is the BLE owner. It is **not** spawned by `rtos_app_init` /
`dice3_device_init`; it is created externally and runs the MIDI/BLE/UI engine.

Sequence with verified call-site addresses (from the disassembly of `0x5010`):

| @     | Call                              | Effect                                       |
| ----: | :-------------------------------- | :------------------------------------------- |
| `0x5090` | `bl 0x68A4`  `ble_spi_reset`         | toggles RESET (pin 6 0→1) around two `0x9b` LED-SPI sync writes; prep state |
| `0x50E0` | `bl 0xA87C`  `cyg_thread_create`     | priority `0x11`, stack 4000 B, name "led_update_thread", entry `ble_spi_bridge_thread` (`0x4F38`) |
| `0x50E8` | `bl 0xA8CC`  `cyg_thread_resume`     | starts the bridge thread                     |
| `0x50EC` | `bl 0x2AE8`  `midi_sleep_ms(10)`     | settle                                       |
| `0x50FC` | `bl 0x0E04`  `gpio_write_sdk(8, …)`  | drive pin 8 — **BLE module enable / power-on** |
| `0x5104` | `bl 0x6240`  `ble_module_firmware_update` | version probe + optional flash             |
| `0x510C…0x515C` | `cyg_flag_init` + `cyg_alarm_create`/`_initialize` | sets up the 2-tick flag alarm that drives the main loop |
| `0x5168` | `bl 0x2018`  `mixer_init_state`      | only if `unaff_r6 == 0` (cold start)         |
| `0x516C` | `bl 0x6208`  `led_animation_init`    |                                              |

After bring-up the thread enters the **main bridge loop** (described next).

### `ble_spi_reset` @ `0x68A4` — decompiled

```c
gpio_write_sdk(7, 1);        // 0xCA000200 := 0x80
gpio_write_sdk(6, 0);        // 0xCA000100 := 0  (RESET asserted)
led_spi_write_cmd(1, 0x9b);  // SPI sync byte (0x9e,1,0x9b) on the LED bus
gpio_write_sdk(6, 1);        // 0xCA000100 := 0x40 (RESET released)
led_spi_write_cmd(1, 0x9b);
*(u32*)0xCB000040 = 0;       // clear pin-12 register
```

### `ble_spi_bridge_thread` @ `0x4F38`

```c
cyg_flag_init(&flag);
alarm = cyg_alarm_create(ble_spi_get_timer_handle(), <1-tick>, &flag, …);
cyg_alarm_initialize(alarm, now+1, period=1, enable=0);
for (;;) {
    cyg_flag_wait(&flag, 1, CYG_FLAG_WAITMODE_OR | CLR);
    led_animation_tick();        // 0x6F68
    ble_spi_read_tuple();        // 0x2F38 — small 6-byte SPI transfer on LED/BLE bus
}
```

Hook site `0x4FAC` (= `bl 0x2F38`) is the documented inject point used by the
patch tooling.

---

## 3. Main bridge loop (the polling / dispatch core)

Tail of `midi_engine_thread` — exact call sites:

```text
0x5170: cyg_flag_wait(&flag, 1, CYG_FLAG_WAITMODE_OR|CLR)   // 0xAB8C
0x5180: midi_rx_poll                                        // 0x2AB8
0x5184: ble_spi_write_tuple        ── full-duplex SPI       // 0x2EF0
0x5188: button_scan_poll                                    // 0x2B9C
0x518C: footswitch_scan_poll                                // 0x2CFC
0x5190: encoder_scan_poll                                   // 0x2F60
0x5194: ble_spi_rx_poll            ── full-duplex SPI       // 0x6044
0x5198: cmp r6, #0
0x519C: bne 0x5170                 ── if "module-update mode" active, skip dispatch
0x51A0: ble_param_dispatch         ── act on local-event ringbuf // 0x21E8
0x51A4: ble_rx_dispatch            ── act on inbound BLE ringbuf // 0x1D14
0x51A8: ble_rx_handshake_poll                                // 0x3EA8
0x51AC: b 0x5170
```

`r6 == 0` is set at startup based on a config compare; when nonzero, the
module is being held in update / programming mode and tuple dispatch is
suppressed (the loop still pumps SPI so the module sees clocks).

There is **no IRQ path** from the BLE module. New BLE data appears in firmware
only because `ble_spi_rx_poll` ran a SPI transfer.

---

## 4. The three tuple ringbufs (resolved from data refs)

| eCos ringbuf | Address     | Direction                      | Producer(s)                                          | Consumer                                |
| :----------- | :---------- | :----------------------------- | :--------------------------------------------------- | :-------------------------------------- |
| **Internal** | `0x29834`   | local hw events → mixer/BLE   | `button_scan_poll`, `encoder_scan_poll`, `footswitch_scan_poll` (via `ble_ringbuf_write_tuple` @ `0x40BC`) | `ble_param_dispatch` @ `0x21E8` (via `ble_ringbuf_read_tuple` @ `0x414C`) |
| **Inbound**  | `0x29848`   | BLE module → DICE              | `ble_spi_rx_poll` (after 3-byte reassembly)          | `ble_rx_dispatch` @ `0x1D14` (via `ble_ringbuf_read_tuple` @ `0x61CC`) |
| **Outbound** | `0x29858`   | DICE → BLE module              | `ble_write_tuple` @ `0x618C`, `ble_send_button_event` @ `0x6300`, `ble_send_hasblender` @ `0x6310`, `ble_send_mixer_state` @ `0x6528`, etc. | `ble_spi_rx_poll` (drains in 30-byte SPI bursts) |

The two `ble_ringbuf_read_tuple` entry points (`0x414C` and `0x61CC`) are
**different functions on different ringbufs** — they only share a name. Same
for the two `ble_ringbuf_write_tuple` shapes.

---

## 5. How "new BT data" actually reaches the MCU

`ble_spi_rx_poll` @ `0x6044` is the entire bridge. One pass:

1. Build a 30-byte TX scratch (`0x2F1D4`) by pulling up to 10 outgoing tuples
   from the **outbound** ringbuf `0x29858`; pad missing slots with `0xFF`.
2. `spi_exec_single_transfer(handle=0x2AA18, cb=NULL, len=30, tx=0x2F1D4, rx=0x2F1F4)`.
   This is a **full-duplex** burst: it ships outgoing tuples and clocks back
   30 bytes from the BLE module simultaneously.
3. Walk the 30 received bytes through a tiny state machine (state pointer
   `0x2F214`, 3-byte assembly buffer at `0x2F215`):

   | state | rx byte | next state | side effect                     |
   | ----: | :------ | :--------- | :------------------------------ |
   |   0   | `0xFF`  | 0          | idle gap                        |
   |   0   | other   | 2          | byte0 = rx                      |
   |   1   | `0xFF`  | 0          | resync                          |
   |   1   | other   | 2          | byte0 = rx                      |
   |   2   | any     | 3          | byte1 = rx                      |
   |   3   | any     | 1          | byte2 = rx; **push 3-byte tuple to inbound `0x29848`** |

4. Loop. Whatever arrives in the inbound ringbuf is consumed later in the same
   main-loop iteration by `ble_rx_dispatch`.

This is the only place any byte ever moves between the BLE module and the
DICE3. The "notification" mechanism is just polling at the main-loop rate.

---

## 6. Inbound dispatch — `ble_rx_dispatch` @ `0x1D14`

Reads from `0x29848` and acts on tuples originated by the **BLE app** (phone
side), reaching us via the module:

| `param` | Meaning (from handler shape)                     | Action |
| ------: | :----------------------------------------------- | :----- |
| `0x06`  | Compressor channel level (per-channel 0..3)      | writes mixer slot, `compressor_level_set_notify` |
| `0x07`  | Master channel level (per-channel 0..3)          | writes mixer slot, `master_level_set_notify` |
| `0x08`  | Channel level (per-channel)                      | `channel_level_set_notify` |
| `0x09`  | "isBlender" probe                                | stores flag, `ble_rx_handle_isblender` |
| `0x0A`  | "hasBlender" / requestState stub                 | `ble_rx_handle_requeststate_stub` |
| `0x0C`  | Echo / ack                                       | replies with `ble_write_tuple(0x0C, 1, 0)` |
| `0x13`  | requestBlenderState                              | `ble_send_full_state_dump` |
| `0x15`  | mute / FX bitmap                                 | writes per-channel mute flags + FX bits |
| `0xF1`  | Connection state (link up/down)                  | `ble_rx_handle_connected(byte1)` |

`ble_send_full_state_dump` = `ble_send_mixer_state` + `ble_send_hasblender`,
both of which write into the outbound ringbuf `0x29858` (so the response goes
out the next pass through `ble_spi_rx_poll`).

`ble_rx_handle_connected` updates link state, kicks LED status, and primes the
channel-select LED block. This is the firmware's notion of "BLE connected".

## 7. Internal-event dispatch — `ble_param_dispatch` @ `0x21E8`

Reads from `0x29834` (the *local* event ringbuf — written by the polling
`button_scan_poll` / `encoder_scan_poll` / `footswitch_scan_poll`) and:

| `param` | Local source       | Action                                            |
| ------: | :----------------- | :------------------------------------------------ |
| `0xAE`  | rotary encoder Δ   | adjusts master / channel / compressor level depending on UI focus, then `ble_write_tuple` shadow-update + LED refresh |
| `0xDB`  | button event       | mode/select/mute/compressor toggles; some paths emit `ble_send_button_event(1)` (= outbound `0xF1`) and `ble_send_disconnect` |
| `0xDC`  | switch-bitmap      | per-channel mute toggles, FX/COMP/limiter bits   |

So the same dispatcher that handles encoder/button input is responsible for
**echoing the resulting state to the BLE app** by appending tuples to the
outbound ringbuf. That echo is how the phone UI stays in sync with knob/button
input on the device.

---

## 8. Outbound write helpers

All eventually `ringbuf_write(0x29858, 3 bytes)`:

- `ble_write_tuple(p, s, v)` @ `0x618C` — generic
- `ble_send_button_event(b1)` @ `0x6300` — emits `0xF1, b1, 0`
- `ble_send_hasblender()` @ `0x6310` — emits `0x11, 0, 0`
- `ble_send_disconnect()` @ `0x6214` — emits `0xF2, 0, 0`, then a synchronous
  `ble_spi_rx_poll` to flush, then sets `*0x… = 1` (update-mode flag)
- `ble_send_mixer_state` @ `0x6528` — diff-walks the mixer cache and emits
  one tuple per changed slot (params `0x06` `0x07` `0x08` `0x09` `0x0A` `0x14`
  `0x15`); used by `ble_send_full_state_dump` and after handshake.

---

## 9. Module firmware-update path — `ble_module_firmware_update` @ `0x6240`

```c
for (i = 0x32; i; --i) {
    midi_sleep_ms(100);
    ble_spi_rx_poll();             // pump bridge
    ble_write_tuple(0xF2, 1, 0);   // "enter update mode"
    ble_write_tuple(0xF0, 0, 0);   // "report version"
    if (ble_ringbuf_read_tuple(&t) && t.param == 0xF0) {
        if (t.byte1_byte2 == 0)    return;   // no version, retry
        if (t.byte1_byte2 == 6)    return;   // already at v6 — done
        break;                                // mismatch — fall through
    }
}
led_startup_animation_start(0, version_const, -1);
ble_send_disconnect();
ble_module_flash_program(image);   // 0x5790 — bit-bang module firmware
led_startup_animation_start(version_const, 0, 1);
```

`ble_module_flash_program` (`0x5790`) is the only path that uses the bit-bang
module-flash protocol; runtime mixer traffic never touches it.

---

## 10. Disable strategies (with verified call sites)

### Soft — drop incoming control only (least invasive)

NOP the call at `0x4FAC` (`bl 0x2F38`) inside the bridge thread, **and / or**
NOP `ble_spi_rx_poll` at `0x5194` in `midi_engine_thread`. The latter is the
authoritative cut: with no SPI poll, no inbound tuple ever lands in `0x29848`
and `ble_rx_dispatch` becomes a no-op. The module remains powered and may
still advertise.

### Medium — kill the bridge subsystem

In `midi_engine_thread`, NOP:

- `0x5090` `bl 0x68A4`  (`ble_spi_reset`)
- `0x50E0` `bl 0xA87C`  (`cyg_thread_create` of `ble_spi_bridge_thread`)
- `0x50FC` `bl 0x0E04`  (`gpio_write_sdk(8, …)` — module enable)
- `0x5184` `bl 0x2EF0`  (outbound `ble_spi_write_tuple`)
- `0x5194` `bl 0x6044`  (inbound `ble_spi_rx_poll`)
- `0x51A0` `bl 0x21E8`  (`ble_param_dispatch`)
- `0x51A4` `bl 0x1D14`  (`ble_rx_dispatch`)

Optionally also `0x5104` `bl 0x6240` (`ble_module_firmware_update`) if you
want to skip version probing.

### Hard — physically reset / power-gate the module

`gpio_write_sdk(6, 0)` → `*0xCA000100 = 0` holds the module in RESET
permanently. Pair with `gpio_write_sdk(8, 0)` (`*0xCB000004 = 0`) to drop the
"module enable" line. A patch that runs both writes once at boot and removes
the calls in `ble_spi_reset` / `midi_engine_thread` that re-drive them gives a
deterministic OFF.

### Keep update path, kill runtime sessions

Leave `ble_module_firmware_update` (`0x5104`) intact, NOP only the inner-loop
dispatchers (`0x51A0`, `0x51A4`) and the outbound write at `0x5184`. The
module still gets enabled and version-probed at boot but no GATT traffic is
acted on or originated.

---

## 11. Function map (single sheet)

| Address  | Symbol                          | Role                                            |
| :------- | :------------------------------ | :---------------------------------------------- |
| `0x5010` | `midi_engine_thread`            | owner thread; bring-up + main loop              |
| `0x4F38` | `ble_spi_bridge_thread`         | "led_update_thread" — 1-tick alarm bridge       |
| `0x68A4` | `ble_spi_reset`                 | toggles RESET via GPIO 6/7 + LED-SPI sync       |
| `0x6240` | `ble_module_firmware_update`    | version probe + optional `ble_module_flash_program` |
| `0x5790` | `ble_module_flash_program`      | bit-bang module firmware (used only on mismatch)|
| `0x6044` | `ble_spi_rx_poll`               | full-duplex SPI bridge + tuple reassembly       |
| `0x2EF0` | `ble_spi_write_tuple`           | helper that toggles GPIO 11 then SPI-writes 3 B |
| `0x2F38` | `ble_spi_read_tuple`            | small 6-byte SPI transfer (called from bridge thread alarm) |
| `0x21E8` | `ble_param_dispatch`            | consumes `0x29834` (local events)               |
| `0x1D14` | `ble_rx_dispatch`               | consumes `0x29848` (BLE-app commands)           |
| `0x3EA8` | `ble_rx_handshake_poll`         | reads MIDI-USB jack state into `*DAT_3ECC`      |
| `0x618C` | `ble_write_tuple`               | append tuple to `0x29858`                       |
| `0x40BC` | `ble_ringbuf_write_tuple`       | append tuple to `0x29834` (panics if param!=0)  |
| `0x414C` | `ble_ringbuf_read_tuple` (a)    | pop from `0x29834`                              |
| `0x61CC` | `ble_ringbuf_read_tuple` (b)    | pop from `0x29848`                              |
| `0x6300` | `ble_send_button_event`         | outbound `(0xF1, b, 0)`                         |
| `0x6310` | `ble_send_hasblender`           | outbound `(0x11, 0, 0)`                         |
| `0x6214` | `ble_send_disconnect`           | outbound `(0xF2, 0, 0)` + sync flush + flag     |
| `0x6528` | `ble_send_mixer_state`          | diff-walk mixer state → outbound tuples         |
| `0x1E28` | `ble_send_full_state_dump`      | `ble_send_mixer_state` + `ble_send_hasblender`  |
| `0x1D40` | `ble_rx_handle_isblender`       | `0x09` handler                                  |
| `0x1DD8` | `ble_rx_handle_connected`       | `0xF1` handler — link state update              |
| `0x9134` | `gpio_write_sdk`                | one bit-mask write, base `0xCA000000` (pin<8) or `0xCB000000` (pin≥8) |
| `0x696C` | `ble_spi_get_timer_handle`      | returns `*0x6978` = `0x2F378`                   |

---

## 12. Evidence sources

- Decompiled with the in-repo Ghidra MCP server against
  `blender_primary_body.bin`.
- Disassembly of `midi_engine_thread` (`0x5010`) for call-site offsets.
- Memory reads of data-pointer slots (`DAT_*`) to resolve ringbuf and SPI
  handle addresses (`0x29834`, `0x29848`, `0x29858`, `0x2AA18`, `0x2F378`,
  `0x2F1D4`, `0x2F1F4`, `0x2F214`, `0x2F215`).
- `ble_spi_rx_poll` (`0x6044`) decompile for the full-duplex burst + state
  machine; `ble_param_dispatch` (`0x21E8`) and `ble_rx_dispatch` (`0x1D14`)
  for the two consumer halves.
- `gpio_write_sdk` (`0x9134`) decompile for the GPIO register-bit map.
