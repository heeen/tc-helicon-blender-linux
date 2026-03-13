# TC Helicon Blender — Complete Protocol Reference

Reverse engineered from:
- JTAG firmware dump (`blender_flash_dump.bin`, 1MB, v1.2.8.2, Oct 23 2018)
- Ghidra static analysis of ARM926EJ-S (DICE3) firmware
- GoXLR Windows app (`GoXLR App.exe` v1.6.4.014, Ghidra)
- TC Helicon Blender Android app v1.2 (`tchelicon.com.blenderappandroid`, jadx)
- USB pcap captures (Windows driver)
- goxlr-utility reference implementation

Device: TC Helicon Blender — VID `0x1220` (TC Electronic), PID `0x8FE1`
Platform: TCAT DICE3, ARM926EJ-S, eCos RTOS
Sister devices: GoXLR (`0x8FE0`), GoXLR Mini (`0x8FE4`)

---

## 1. Hardware Architecture

### SoC: TCAT DICE3

- CPU: ARM926EJ-S, little-endian
- JTAG IDCODE: `0x17900F0F`, 4-bit IR
- RTOS: eCos (TC Applied Technologies fork)
- Build: `/home/tcat/tch-git/project/blender-dice3/dice3-sdk/`

### Memory Map (from firmware analysis)

| Region | Address | Description |
|--------|---------|-------------|
| Flash (active) | `0x00000000`–`0x0007FFFF` | Primary firmware image (512KB) |
| Flash (golden) | `0x00080000`–`0x000F7FFF` | Backup/recovery firmware (exact duplicate) |
| Flash (pad) | `0x000F8000`–`0x000FFFFF` | Padding (`0xd6dcfffe` pattern) |
| SRAM work area | `0x00200000` | CPU work area (16KB) |
| Mixer registers | `0xC4000200` | DICE hardware mixer coefficient matrix |
| Boot config | `0xC9000000` | Boot configuration registers |
| LED control | `0xCB000400` | LED GPIO/peripheral control |
| Clock PLL | `0xC5000024` | Clock/PLL configuration |
| GPIO control | `0xCC000014` | General-purpose I/O |
| Interrupt ctrl | `0xCF000014` | Interrupt controller |

### Peripherals

| Peripheral | Interface | Details |
|------------|-----------|---------|
| DCP (codec) | GPIO bit-bang (pins 3=CLK, 4=DATA) | Serial protocol to onboard codec/DSP |
| BLE module | UART (COM port) | External "TCH-BLE" module, handles all BLE |
| SPI flash | SPI (SST25XX) | External flash for persistent settings |
| USB audio | DICE USB core | 12×2 USB audio interface (UAC2) |
| MIDI | USB | "Blender MIDI In" / "Blender MIDI Out" via `dice3midi` |
| LEDs | GPIO pin 5 | Status indicator (firmware update, boot) |

### Firmware Threads

| Thread | Entry Address | Description |
|--------|---------------|-------------|
| `my_main_thread` | `0x00000750` | Main application thread |
| `sys_main_thread` | `0x00013B6C` | System/DICE SDK main thread |
| `dcp_usb_thread` | `0x0001BEF8` | DCP-over-USB command handler |
| `usb_io_thread` | `0x0001FE9C` | USB I/O transfer handler |
| `led_update_thread` | `0x00005010` | LED status updates |
| `idle_thread` | (via string xref) | eCos idle thread |

---

## 2. USB Boot Initialization & Quirks

The device does not stream audio until the host sends a specific init sequence. Without it, ALSA sees the card but `arecord` gets I/O errors. There are **three** separate quirk touchpoints in the Linux kernel driver.

### Quirk 1: Boot Init Sequence (`snd_usb_tc_helicon_boot_quirk`)

Runs **once** at first probe, from `snd_usb_apply_boot_quirk_once` (called when `chip` is NULL — before any card instance is created). NOT in the per-interface `snd_usb_apply_boot_quirk`.

| # | Direction | bmRequestType | bRequest | wValue | wIndex | Data | Fatal? |
|---|-----------|---------------|----------|--------|--------|------|--------|
| 1 | IN | Vendor/Interface | `0x00` | 0 | 0 | read 24 bytes | No |
| 2 | OUT | Class/Interface | `0x01` | `0x0100` | `0x2900` | `80 bb 00 00` | **Yes** |
| 3 | OUT | Vendor/Interface | `0x01` | 0 | 0 | zero-length | No |
| 4 | — | — | — | — | — | `msleep(1000)` | — |
| 5 | IN | Vendor/Interface | `0x03` | 0 | 0 | read 16 bytes | No |

#### What the firmware does at each step

**Step 1 — Vendor read (bReq=0x00): Wake vendor interface**

The firmware's USB setup handler (`FUN_0001b3bc`) dispatches vendor+interface requests to the DCP handler (`FUN_0001bf6c`). For bReq=0x00 IN, it serves 24 bytes of static DICE global data from address `0x00027928`, containing the magic values `0x04061973` and `0x18101966` (TCAT DICE identification). The kernel discards this data — the purpose is purely to exercise the vendor interface and ensure it's responsive.

**Step 2 — SET_CUR 48kHz (bReq=0x01 class): Start the clock domain** *(only fatal step)*

The USB dispatcher routes class+interface requests with `wIndex_hi >= 0x29` (entity ID ≥ 41) and bReq=0x01 to the clock source handler (`FUN_0001b364`). This function:
1. Receives the 4-byte rate value (`0x0000BB80` = 48000) via USB OUT transfer
2. Calls into the DAL vtable (`FUN_000194c8`) to configure the clock domain
3. The DAL sets the internal clock source to 48kHz, configures the DICE HPLL (High-Precision PLL), and starts the clock domain
4. Once the PLL locks, the DICE USB core enables isochronous endpoints

**This is why audio doesn't work without the quirk:** The DICE3 USB core doesn't set up isochronous transfers until a clock domain is running. Without SET_CUR, there's no PLL lock, no isochronous endpoints, and `arecord` gets `EIO`. The Windows Thesycon driver (`TUSBAUDIO`) does the identical `SetSampleRate(48000)` call during `tcat_dice_tusb_device_open()`.

The wIndex encodes both interface and entity: `0x2900` = interface 0, entity 41 (`0x29`). Entity 41 is the UAC2 clock source. wValue `0x0100` = `UAC2_CS_CONTROL_SAM_FREQ << 8` (sampling frequency control, channel 0).

**Step 3 — Vendor write (bReq=0x01): Reset DCP state machine**

The DCP handler maintains a state machine at `0x00030F78` with states:
- 1 = idle/ready
- 2 = command received, processing
- 3 = awaiting response read
- 4 = response ready
- 5/6 = reset/error

bReq=0x01 OUT resets the command sequence counter to 1, clears error counters (offsets 0x109–0x10B), and flushes any stale DCP state (if state was 3→5 or 4→6, pending responses are discarded). Also triggers `FUN_0001bec8(1)` which sends a 6-byte notification on the interrupt endpoint to signal state change.

This ensures the DCP command interface starts clean — the Windows driver does the same probe+flush before issuing real DCP commands.

**Step 4 — `msleep(1000)`: Wait for PLL lock**

One second for the DICE HPLL to lock and the firmware to finish configuring isochronous endpoint descriptors. The DAL rate state machine goes through `STATE_WAIT_RATE_STABLE` → `STATE_CHECK_RATE` during this time.

**Step 5 — Vendor read (bReq=0x03): Drain stale DCP response**

Reads 16 bytes from the DCP response buffer. If a stale command response was pending (state 4), this drains it and signals the completion semaphore. If an error response was pending (state 6), this drains the error. Either way, leaves the DCP state machine clean for future commands. Response is discarded by the kernel.

### Quirk 2: Clock Validity Override (`clock.c`)

After boot, the kernel's UAC2 driver queries clock validity via GET_CUR on the clock source entity with `wValue_hi=2`. The firmware's class request handler (`FUN_0001b3bc`) returns a validity byte — but **the Blender always reports 0 (invalid)** even when the clock is running fine at 48kHz.

`uac_clock_source_is_valid_quirk()` unconditionally returns `true` for all three TC Electronic PIDs (`0x8FE0`, `0x8FE1`, `0x8FE4`). Without this, the driver would reject the clock source and refuse to start audio streaming.

### Quirk 3: Fixed Sample Rate (`format.c`)

When the kernel enumerates supported sample rates via `UAC2_CS_RANGE` (bReq=0x02):
1. **2-byte count query succeeds** — device reports 1 rate triplet
2. **14-byte full query fails with EPROTO (-71)** — the firmware's rate table builder (`FUN_0001b3bc`, bReq=0x02 path) constructs the response from DAL clock domain data, but the full transfer triggers a USB protocol error, likely a packet size/timing bug in the DICE USB core

`line6_parse_audio_format_rates_quirk()` catches this failure and hardcodes `48000 Hz` / `SNDRV_PCM_RATE_48000` as the only supported rate. Without this, the driver fails to parse audio format descriptors and the card doesn't work.

### Summary of all three quirks

| Quirk | File | When | Purpose |
|-------|------|------|---------|
| Boot init | `quirks.c` | Once at first probe | Vendor wake + SET_CUR 48kHz + DCP flush; starts clock domain |
| Clock validity | `clock.c` | Every clock check | Override false "invalid" report from device |
| Fixed rate | `format.c` | Format enumeration | Hardcode 48kHz because CS_RANGE full query returns EPROTO |

### Why the Windows driver doesn't need quirks

The Thesycon TUSBAUDIO driver doesn't use standard UAC2 clock queries — it has its own API (`SetSampleRate`, `GetCurrentSampleRate`, `GetSampleRateList`) that goes through vendor-specific DCP commands instead of UAC2 CS_RANGE. So the firmware bugs in the UAC2 class request handlers are never triggered on Windows.

---

## 3. USB Audio Format

| Direction | Channels | Format | Rate |
|-----------|----------|--------|------|
| Capture (device→host) | 12 (6 stereo pairs) | S32_LE, 24-bit | 48000 Hz |
| Playback (host→device) | 2 (1 stereo pair) | S32_LE, 24-bit | 48000 Hz |

The 12 capture channels correspond to the 6 mixer input pairs. The 2 playback channels are the main stereo output.

### USB MIDI

The device presents two MIDI endpoints:
- **Blender MIDI In** — host→device, string at `0x000231FB`
- **Blender MIDI Out** — device→host, string at `0x0002320B`

MIDI is handled by the `dice3midi` subsystem (string at `0x00029464`). This is standard DICE MIDI-over-USB — no vendor-specific extensions found. MIDI likely used for DAW integration (transport control, CC mapping).

---

## 4. DCP (DICE Control Protocol) over USB

### Transport

DCP packets are sent via USB vendor control transfers:

| Field | Write (host→device) | Read (device→host) |
|-------|---------------------|-------------------|
| bmRequestType | `OUT|VENDOR|INTERFACE` | `IN|VENDOR|INTERFACE` |
| bRequest | `0x01` | `0x01` |
| wValue | `0` | `0` |
| wIndex | `0` (Linux) or `0x2900` (Windows TUSBAUDIO) | same |

Protocol: send OUT transfer with command, then IN transfer to read response.

### Firmware USB Request Dispatch (`FUN_0001b3bc`)

The firmware's USB setup handler dispatches based on `bmRequestType`:

- **Vendor + Interface** (`(bmRequestType & 0x60) == 0x40`): Routes to DCP handler `FUN_0001bf6c` after validating wIndex matches the vendor interface number
- **Class + Interface** (`(bmRequestType & 0x60) == 0x20`): Routes to UAC2 audio class handlers for clock control (SET_CUR/GET_CUR/CS_RANGE)

### Vendor Request bRequest Values (DCP Handler `FUN_0001bf6c`)

| bReq | Dir | Function | Details |
|------|-----|----------|---------|
| `0x00` | IN | Read DICE global data | Serves 24 bytes from `0x00027928` (magic `0x04061973`/`0x18101966`) |
| `0x01` | OUT | Reset/ping | Resets sequence counter to 1, clears error counters, flushes stale DCP state |
| `0x02` | OUT | Send DCP command | Validates header (body ≤ 1024 bytes, sequence number match), sets state=2, wakes DCP worker thread |
| `0x03` | IN | Read DCP response | If state=4: sends header+body, signals completion semaphore. If state=6: sends error header |

### DCP State Machine

The DCP handler maintains a state machine at `0x00030F78`:

```
State 1 (idle) ──bReq=0x02──→ State 2 (processing) ──worker done──→ State 4 (response ready)
                                                                          │
State 4 ──bReq=0x03──→ State 1 (idle)                                    │
State 4 ──bReq=0x01──→ State 6 (error/flush) ──bReq=0x03──→ drain        │
State 3 (awaiting read) ──bReq=0x01──→ State 5 (reset)                   │
```

After bReq=0x02 validation passes, `FUN_0001bec8` sends a 6-byte interrupt endpoint notification to tell the host a response is pending, prompting the host to issue bReq=0x03.

Command validation on bReq=0x02:
- `wLength - 0x10` must equal the body_length field in the header
- Sequence number in header must match expected counter (auto-incremented)
- Command must not be null (category=0 and opcode=0 rejected unless it's a flush)

### Packet Format

16-byte big-endian header + optional payload:

```
Offset  Size  Field
0x00    4     word0: (category & 0xFFF) << 12 | (opcode & 0xFFF)
0x04    4     word1: (seqnum & 0xFFFF) << 16 | (data_size & 0xFFFF)
0x08    4     reserved (0)
0x0C    4     reserved (0)
0x10    N     payload (data_size bytes, big-endian u32 words)
```

### Standard DICE Categories (0x000–0x004) — GoXLR Reference

These categories exist in the DICE SDK but **the Blender firmware does NOT register handlers for them**. All return header-only responses with body_len=0 (verified 2026-03-12).

| Category | Name | GoXLR Opcodes (for reference) |
|----------|------|-------------------------------|
| 0 | System | 0x000=flush/ping, 0x001=query category, 0x002=system info, 0x018=global data |
| 1 | Peaks | 0x000=peak metering capabilities |
| 2 | Mixer | 0x000=mixer capabilities (8 bytes) |
| 3 | Router | 0x000=router capabilities (0x0C bytes) |
| 4 | NVM | 0x000=capabilities, 0x001=segment info, 0x002-0x004=erase/write, 0x006=CRC32 |

### Vendor Categories (0x800+) — GoXLR Reference

These categories are defined in the GoXLR firmware/utility. **The Blender firmware does NOT register handlers for any of them** — the DCP worker thread's category dispatch loop walks an empty handler list and returns header-only for all (verified 2026-03-12).

| Category | Opcode | Dir | Size | Description | Blender Status |
|----------|--------|-----|------|-------------|----------------|
| 0x800 | 0 | R | varies | Full device state dump | header-only |
| 0x801 | 0 | W | N×8 | Bulk set DSP effect parameters | header-only |
| 0x804 | row_idx | W | 0x1A | Set routing matrix row (13 × u16) | header-only |
| 0x805 | varies | W | varies | Fader channel assignment | header-only |
| 0x806 | (fader<<4)\|chan | W | 1 | Fader position (0–255) | header-only |
| 0x808 | 0 | W | 0x18 | Button state bitmap | header-only |
| 0x80A | enc_idx | W | 1 | Encoder position | header-only |
| 0x80B | 0 | W | N×8 | Set DSP parameters (key-value) | header-only |
| 0x80C | 0 | R | 2 | Serial number (u16) | header-only |
| 0x80F | 0 | R | 0x18 | Firmware version info | header-only |
| 0x810 | 5 | R | 0x18 | Poll firmware update status | header-only |
| 0x814 | chan_idx | W | 2 | Per-channel volume (u16) | header-only |

**The Blender's DCP over USB is essentially non-functional beyond cmd=2 (SystemInfo).** The mixer is controlled entirely via BLE, not USB DCP. The DCP USB thread exists because it's part of the shared DICE SDK code, but the Blender application code (`my_main_thread`) never registers any category handlers. This is the fundamental architectural difference from the GoXLR, which uses DCP for everything.

### Blender DCP — Verified Working Commands (2026-03-12)

Tested systematically with `probe_dcp.py` — bReq=1 init, bReq=3 flush, then all categories/opcodes.

| Command | Result | Notes |
|---------|--------|-------|
| bReq=1 (ping/reset) | OK | Resets DCP state machine, required before any commands |
| bReq=3 (read response) | OK | Drains pending responses |
| cmd=0 (reset command index) | OK (header-only) | Resets sequence counter |
| cmd=2 (SystemInfo) | **88 bytes** | Only command that returns data — see below |
| All standard categories (0x000–0x004) | header-only | No registered handlers |
| All vendor categories (0x800–0x81F) | header-only | No registered handlers |
| 0x80F op=2+ | **timeout/crash** | Causes DCP state machine desync, needs bReq=1 recovery |

### SystemInfo Response (cmd=2) — 88 bytes

The only DCP command that returns payload data on the Blender:

```
Offset  Size  Value                  Field
0x00    4     00 00 00 00            (reserved)
0x04    4     05 00 10 20            DICE capabilities flags
0x08    4     16 00 00 00            (unknown, 0x16 = 22)
0x0C    4     08 20 10 00            (unknown)
0x10    12    "Oct 23 2018\0"        Build date (null-terminated)
0x1C    12    "18:42:08\0..."        Build time (with padding)
0x28    12    "1.2.8.2\0..."         Firmware version string
0x34    4     66 01 00 00            Firmware packed version (0x166)
0x38    4     e1 8f 00 00            USB PID (0x8FE1 = Blender)
0x3C    ...   00 00 ...              (padding/reserved)
```

### Why DCP Is Empty on Blender

The DCP worker thread (`FUN_0001c3bc` @ `0x0001C3BC`) dispatches commands by walking a linked list of registered category handlers at runtime. The GoXLR firmware registers handlers for categories 0x800–0x818 during initialization. The Blender firmware does NOT — its `my_main_thread_entry` only initializes mixer coefficients and spawns the LED/BLE polling thread. The linked list stays empty, so all commands fall through to the "no handler" path which returns header-only.

The `cmd=2` (SystemInfo) response comes from the DICE SDK's built-in System category handler, which is registered by the SDK init code (`sys_main_thread_entry`) rather than application code. This is why it's the only working command.

### Firmware Update over USB — NOT POSSIBLE on Blender

The NVM category (0x004) returns header-only — no erase, write, or CRC32 commands are functional. DCP 0x810 (firmware update status poll) also returns header-only. The `usb_setup_dispatch` handler only routes vendor requests to `dcp_vendor_handler` (bReq 0–3) and class requests to UAC2 clock/sample-rate handlers — no direct SPI flash read/write USB endpoints exist. **There is no USB firmware update path for the DICE3 itself on the Blender.**

The DICE3 firmware CAN be updated via:
- **JTAG/SWD** (unpopulated 5×2 1.27mm header on PCB)
- **UART loader** (bootloader fallback when all images fail — `"enter UART Loader"`)
- **Factory programming**

The bootloader's primary/golden/recovery image architecture (§10) is inherited from the DICE3 SDK and used for factory reliability, not for field updates.

No Blender firmware has ever been published online for download.

---

## 5. DCP Bit-Bang Serial Protocol (Firmware-Internal)

The DICE3 communicates with the **BLE module** (TCH-BLE) via a custom serial protocol bit-banged on GPIO pins 3 (clock) and 4 (data). This is used exclusively for BLE module firmware updates (§9), **not** accessible over USB.

### GPIO Pin Assignment

| Pin | Function | Direction |
|-----|----------|-----------|
| 3 | CLK (serial clock) | Output |
| 4 | DATA (serial data) | Bidirectional |
| 5 | Status LED / DCP reset | Output |

### Protocol Details

**Write transaction** (`dcp_bitbang_write` @ `0x00005360`):
1. Clock out 8-bit address MSB-first on DATA, toggling CLK
2. Switch DATA to input, read 3-bit ACK (expect `001`)
3. Switch DATA to output, clock out 32-bit data LSB-first
4. Send 1-bit parity (XOR of all data bits)

**Read transaction** (`dcp_bitbang_read` @ `0x000051CC`):
1. Clock out 8-bit address with read flag (`addr | 0x20, XOR 0x04`) MSB-first
2. Read 3-bit ACK (expect `001`)
3. Read 32-bit data LSB-first
4. Read 1-bit parity, verify

**Higher-level operations:**
- `dcp_write_reg(addr, data)` @ `0x00005590`: write via address `0xD1` then data `0xDD`
- `dcp_read_reg(addr, &data)` @ `0x000054C0`: write address `0xD1`, read from `0xDD` (reads twice, uses second)
- `dcp_wait_complete()` @ `0x00005518`: poll status register until bits `0x90000000` clear, verify `0xF0000000 == 0xA0000000`

### DCP Reset Sequence (`dcp_reset` @ `0x000055C8`)

1. Assert LED pin 5 low, delay ~63 cycles, deassert
2. Send 54 clock pulses with DATA=1 (sync), then 4 with DATA=0
3. Read chip ID from address `0x81`, verify expected value
4. Write `0x54000000` to address `0x95` (configuration)
5. Write `0` to address `0x8D`, write `2` to address `0xC5`
6. Write `0x80000000` to command register, verify completion

### DCP Register Addresses (from firmware constants)

| Address | Usage |
|---------|-------|
| `0x81` | Chip ID (read during reset) |
| `0x8D` | Configuration register |
| `0x95` | Configuration register |
| `0xC5` | Configuration register |
| `0xD1` | Register address select |
| `0xDD` | Data read/write |

---

## 6. Mixer Hardware

### DICE Mixer Matrix

The hardware mixer is at memory-mapped register `0xC4000200`. The `dcp_dump_mixer_coefficients` function (`0x0001768C`) reads and prints the full matrix.

Mixer configuration (from `0xC4000200` register):
- Bit 0: mixer enabled
- Bits 24–28: number of inputs - 1
- Bits 16–20: number of outputs - 1
- Bit 1: mixer mode flag

Coefficients are stored as 16-bit signed values in a matrix: `coeff[input * num_outputs + output]`, with 16 input channels.

### CLI Mixer Commands

| Command | Usage | Description |
|---------|-------|-------------|
| `mix.dump` | `mix.dump` | Dump full mixer state and coefficients |
| `mix.gain` | `mix.gain <out> <in0..15>` | Set gain coefficients for one output |
| `mix.cfg` | `mix.cfg <ins> <outs> <on>` | Configure mixer dimensions (1–32 ins/outs) |
| `mix.clrin` | `mix.clrin <in>` | Clear all coefficients for an input |
| `mix.clrout` | `mix.clrout <out>` | Clear all coefficients for an output |

---

## 7. Audio Routing (DAL — DICE Abstraction Layer)

The DAL manages clock domains and audio routing between physical/virtual endpoints.

### Clock Sources

From CLI help string at `0x00024220`:
```
<source>: int, aes, wc<n>, adat<n>, usb, avs<n> (use 8-15 for avb)
```

### Sample Rates

| Rate Constant | Value |
|---------------|-------|
| `NOMINAL_RATE_32` | 32 kHz |
| `NOMINAL_RATE_44_1` | 44.1 kHz |
| `NOMINAL_RATE_48` | 48 kHz |
| `NOMINAL_RATE_88_2` | 88.2 kHz |
| `NOMINAL_RATE_96` | 96 kHz |
| `NOMINAL_RATE_176_4` | 176.4 kHz |
| `NOMINAL_RATE_192` | 192 kHz |
| `NOMINAL_RATE_ANY` | Any rate |
| `NOMINAL_RATE_NONE` | Disabled |
| `NOMINAL_RATE_USER_1..8` | User-defined rates |

### Rate Modes

| Mode | Description |
|------|-------------|
| `RATE_MODE_LOW` | 32–48 kHz |
| `RATE_MODE_LOW_MID` | 32–96 kHz |
| `RATE_MODE_MID` | 88.2–96 kHz |
| `RATE_MODE_HIGH` | 176.4–192 kHz |
| `RATE_MODE_ALL` | All rates |

### Rate State Machine (from `dalClkCtrl.c`)

```
STATE_DISABLED → STATE_WAIT_RATE_STABLE → STATE_CHECK_RATE
                                        → STATE_ILLEGAL_RATE
                                        → STATE_RATE_IS_FLAKY
```

**Note:** The Blender only supports 48 kHz over USB. The DAL rate infrastructure is from the DICE SDK and supports the full range, but the Blender's boot quirk hardcodes 48kHz and the device does not appear to support rate switching.

### Audio Routing Devices

From CLI help string at `0x00024368`:
```
<dev>: mute, ins, aes, adat, flt, mix, aio, avs<n>, usb, avb<n>
```

Full routing command:
```
dal:route <0|1> <dstDev> <dstCh> <srcDev> <srcCh> <chnls>
```

### DAL CLI Commands

| Command | Description |
|---------|-------------|
| `dal:clock <0\|1> <source> <rate>` | Set clock source and rate for domain |
| `dal:route <0\|1> <dst> <dstCh> <src> <srcCh> <n>` | Set audio route |
| `dal:create <0\|1> <rateMode> <inputs> <outputs>` | Create clock domain |
| `dal:destroy <0\|1>` | Destroy clock domain |
| `dal:start <0\|1>` | Start clock domain |
| `dal:dump <0\|1>` | Dump domain config and status |

---

## 8. BLE Protocol (Blender-Specific)

The Blender uses an external BLE module ("TCH-BLE", string at `0x00023042`) connected via SPI-like serial. The BLE module handles all Bluetooth — the DICE3 firmware sends/receives 3-byte parameter tuples over the serial link.

### Architecture (Dual-Mode BLE)

The Blender supports **both** BLE roles:

1. **Blender as Central** (primary mode in Android app): The host app acts as GATT peripheral (server), advertises ParameterService UUID. The Blender discovers and connects. The Android app's `Peripheral.java` implements this.

2. **Blender as Peripheral** (confirmed working 2026-03-13): Press BT/PAIR → Blender advertises as "Blender" with ParameterService UUID. A BLE central (e.g. `bleak` on Linux) scans, connects, and subscribes to notifications. The Android app's `Central.java` implements this. The advertising window is brief (~15 seconds).

**No pairing, bonding, or PIN.** The protocol is entirely unauthenticated and unencrypted. Any device advertising or scanning for the ParameterService UUID can connect.

### GATT Services

Three services are defined in `BluetoothConstants.java`. The Blender's BLE module exposes ParameterService and DummyService as GATT services; AppDetailService is app-only:

#### ParameterService (PRIMARY — mixer control)

| Field | Value |
|-------|-------|
| Service UUID | `E71EE188-279F-4ED6-8055-12D77BFD900C` |
| Characteristic UUID | `50E2D021-F23B-46FB-B7E6-FBE12301276A` |
| CCCD Descriptor | `00002902-0000-1000-8000-00805F9B34FB` |
| User Description Descriptor | `00002901-0000-1000-8000-00805F9B34FB` |
| Properties (app peripheral) | `0x1A` = WRITE \| WRITE_NO_RESPONSE \| NOTIFY \| INDICATE |
| Properties (Blender peripheral) | READ \| WRITE \| NOTIFY (confirmed via bleak 2026-03-13) |
| Permissions | `0x11` = READ \| WRITE |

When app is peripheral (Blender=Central):
- Peripheral→Central: **indications** (confirm=true, not plain notifications)
- Central→Peripheral: `WRITE_TYPE_NO_RESPONSE` (fire-and-forget)
- Requested MTU: **180 bytes** (allows up to 59 tuples per packet)

When Blender is peripheral (Linux/app=Central):
- Blender→Host: **notifications** (subscribe via CCCD)
- Host→Blender: GATT writes with response (`WRITE_TYPE_DEFAULT`)

#### AppDetailService (app-only, NOT on Blender hardware)

| Field | Value |
|-------|-------|
| Service UUID | `F7E58580-9BB5-48A3-B8A4-6BE6A391B8DF` |
| Characteristic UUID | `7BB81501-46CF-4722-AB91-276AF25528EE` |

Used in the Android app for **phone-to-phone** metadata sync. Carries the same 3-byte tuple format as ParameterService but for non-mixer data:

- **`iconChange(14, channel, iconID)`** — custom icon assignments per input channel (9 slots, IDs 0-27 mapping to named icons like `defaultIcon`, `A`-`Z`, etc.)
- **`hasBlender(18, 0, 0/1)`** — in central mode, the app sends this via the AppDetail channel (not Parameter channel) to confirm Blender presence to other connected apps
- **`scanForMore(15, 0, 0)`** — triggers the app to scan for additional Blender devices
- **`requestBlenderState(19, 0, 0)`** — can also be processed on this channel

NOT registered on the Blender's BLE module GATT server — `initService()` in `Peripheral.java` skips it because `UseAppDetails = false`. Only exists in the app's own GATT server when the app acts as peripheral.

#### DummyService (on Blender hardware, no-op)

| Field | Value |
|-------|-------|
| Service UUID | `4264DDEF-59BD-49CC-854A-CC09CAA2232A` |
| Characteristic UUID | `435A6273-2810-4136-ACFE-4A0C0BD65186` |
| Properties | None (confirmed via bleak scan 2026-03-13) |

Despite the name in `BluetoothConstants.java`, this service **is** exposed by the Blender's BLE module and is visible during GATT discovery. However, its characteristic has **no properties** (no read, write, notify, or indicate) — the Blender firmware has no handlers for it. It appears to be a vestigial service in the BLE module firmware.

The Android app does reference `DummyService_UUID` and `DummyCharacteristic_UUID` in `Central.java` and `Peripheral.java` but never reads from or writes to them.

### Discovery and Advertising

#### Mode A: Host as Peripheral (Android app default)

**Peripheral (app/Linux host) advertises:**
- Mode: `ADVERTISE_MODE_LOW_LATENCY` (scan interval optimized for speed)
- TX Power: `ADVERTISE_TX_POWER_HIGH`
- Connectable: `true`
- Timeout: `0` (indefinite)
- Advertise data: ParameterService UUID only (no device name in advertisement data)
- No scan response data

**Central (Blender hardware) scans:**
- Scan filter: matches ParameterService UUID `E71EE188-279F-4ED6-8055-12D77BFD900C`
- Scan mode: `LOW_LATENCY`
- Scan timeout: 15 seconds (`Constants.scanningTimeOutTime = 15000`)
- User must press the physical Bluetooth button on the Blender to initiate scanning

#### Mode B: Blender as Peripheral (confirmed working 2026-03-13)

**Peripheral (Blender hardware) advertises:**
- Device name: `Blender`
- BLE MAC: `00:A0:50:28:B7:21` (this unit)
- Advertises ParameterService UUID
- Triggered by BT/PAIR button press (button 3 short press → firmware sends `(0xF1, 1, 0)` to BLE module)
- Advertising window is brief (~15 seconds)

**Central (Linux host) scans and connects:**
- Scan with service UUID filter for ParameterService
- Connect immediately on discovery (advertising window is short)
- Subscribe to notifications on ParameterCharacteristic
- Working implementation: `blender_ble.py` using `bleak` library

### Connection Flow — Mode A (Blender=Central)

```
   Phone/Linux (Peripheral)                    Blender (Central)
   ─────────────────────                       ─────────────────
1. Start GATT server
   Register ParameterService
   Begin advertising UUID
                                          2. User presses BT button
                                             Scan for ParameterService UUID
                                             Find advertiser, stop scan

                                          3. connectGatt(TRANSPORT_LE)
   onConnectionStateChange(CONNECTED) <──────>  STATE_CONNECTED
                                          4. requestConnectionPriority(HIGH)
                                             discoverServices()
   onServicesDiscovered() <──────────────────  onServicesDiscovered()
                                          5. Enable notifications on
                                             ParameterCharacteristic
                                             (write CCCD = ENABLE_NOTIFICATION)
                                             requestMtu(180)

                                          6. Write isBlender tuple:
   onCharacteristicWriteRequest() <──────────  write(17, 0, 0)
   processParameter: isBlender received
   setHasBlender(true)

7. Send via indication:
   hasBlender(18, 0, 1) ──────────────────>  onCharacteristicChanged()
   requestBlenderState(19, 0, 0) ─────────>  onCharacteristicChanged()

                                          8. Write full state dump:
   onCharacteristicWriteRequest() <──────────  write([all current params])
   Update all UI/state

9. Bidirectional steady-state:
   indication(param changes) ─────────────>  knob changes written
   onCharacteristicWriteRequest() <──────────  write(knob deltas)
```

### Connection Flow — Mode B (Blender=Peripheral, confirmed 2026-03-13)

```
   Linux Host (Central/bleak)                  Blender (Peripheral)
   ──────────────────────────                  ────────────────────
                                          1. User presses BT button
                                             BLE module starts advertising
                                             "Blender" with ParameterService UUID

2. BleakScanner.discover()
   Filter by ParameterService UUID
   Find "Blender" (00:A0:50:28:B7:21)

3. BleakClient.connect()
   Discover services ─────────────────────>  CONNECTED
   Subscribe to notifications on
   ParameterCharacteristic (CCCD)

                                          4. Blender sends full state dump
   on_notification() <─────────────────────  notify([all params + isBlender(17,0,0)])
   Detect isBlender in dump

5. GATT write:
   hasBlender(18, 0, 1) ──────────────────>  onCharacteristicWrite()
   requestBlenderState(19, 0, 0) ─────────>  onCharacteristicWrite()

                                          6. Blender sends second state dump
   on_notification() <─────────────────────  notify([all params])

7. Bidirectional steady-state:
   write_gatt_char(param changes) ────────>  knob changes via notify
   on_notification() <─────────────────────  notify(knob deltas)
```

### Wire Format

Data on ParameterCharacteristic is packed as sequential **3-byte tuples**:

```
byte 0: paramID    (0–255)
byte 1: subParam   (0–3 for mixer buses, 0 for toggles)
byte 2: value      (0–255, interpreted as signed or unsigned per param)
```

Multiple tuples concatenated in a single write/indication. Total must be a multiple of 3 — partial tuples are logged as errors.

Broadcast timer: every **120ms** (`Constants.broadcastTime`), pending parameter changes are batched and sent. One device at a time (`broadcastToOneAtATime = true`).

### BLE Parameter Table (App-Side)

| ID | Name | SubParams | Default | Description |
|----|------|-----------|---------|-------------|
| 0 | input1 | 0–3 (output buses A–D) | 205 (0xCD) | Input 1 level per bus |
| 1 | input2 | 0–3 | 205 | Input 2 level per bus |
| 2 | input3 | 0–3 | 205 | Input 3 level per bus |
| 3 | input4 | 0–3 | 205 | Input 4 level per bus |
| 4 | input5 | 0–3 | 205 | Input 5 level per bus |
| 5 | input6 | 0–3 | 205 | Input 6 level per bus |
| 6 | level | 0–3 | 82 (0x52) | Master output level per bus |
| 7 | compressor | 0–3 | 131 (0x83) | Compressor threshold per bus |
| 8 | micGain | 0–3 | 82 (0x52) | Room mic gain per bus |
| 9 | talk | 0 | 0 | Talkback toggle (0/1) |
| 11 | blenderState | 0 | 0 | Jack sense (sub=input bits, val=output+SD bits) |
| 12 | version | 0 | 0 | Firmware version (read-only from device) |
| 14 | iconChange | 0–8 | 0 | Channel icon ID (app-to-app only) |
| 15 | scanForMore | 0 | 0 | Request central to scan for more peripherals |
| 16 | disconnectPeripheral | 0 | 0 | Request central to disconnect a peripheral |
| 17 | isBlender | 0 | 0 | Device identifies itself as Blender |
| 18 | hasBlender | 0 | 0 | Host confirms Blender is connected (0/1) |
| 19 | requestBlenderState | 0 | 0 | Host requests full state dump |
| 20 | muteOutput | 0 | 0 | Mute bitmap (bit3=A, bit2=B, bit1=C, bit0=D) |
| 21 | compressorOnOff | 0 | 0 | Compressor enable bitmap (bit3=A, bit2=B, bit1=C, bit0=D) |

**Sub-parameters (0–3)** = output buses A–D. Slider params (IDs 0–8) have 4 sub-params each; toggle params use sub=0 only.

### Firmware-Internal BLE Parameters (Additional)

The firmware uses additional parameter IDs internally between the DICE3 and the TCH-BLE module that are NOT in the Android app:

| ID | Name | Direction | Description |
|----|------|-----------|-------------|
| 0xAE | rotary_encoder | DICE→BLE | Volume knob delta (±0x40), sub=channel |
| 0xDB | button_press | DICE→BLE | Physical button events (sub=0: channel select/mute/reset; sub=1: solo; sub=2: system commands) |
| 0xDC | switch_state | DICE→BLE | 16-bit switch bitmask, mute states for 6 channels |
| 0xF1 | link_status | BLE→DICE | BLE connection status updates |

#### Button press dispatch (paramID=0xDB):

| subParam | value | Action |
|----------|-------|--------|
| 0 | 0, 1 | Select input channel 0, 1 |
| 0 | 0xE, 0xF | Select input channel 2, 3 |
| 0 | 2 | Toggle mute |
| 0 | 3 | Factory reset |
| 0 | 8–0xD | Select sub-channel |
| 1 | 5, 6 | Toggle solo/headphone per channel |
| 2 | 3 | Reset all levels to default |
| 2 | 4 | Factory reset |
| 2 | 5 | Enable headphone mode |
| 2 | 6 | Disable headphone mode |

### Firmware-Internal BLE Response Parameters (BLE→DICE)

| ID | Description |
|----|-------------|
| 0–5 | Individual channel levels (value >> 3) |
| 6 | Pan L values |
| 7 | Pan R values |
| 8 | EQ values |
| 9 | Feature enable/disable |
| 10 | Global parameter |
| 0x14 | Mute/solo indicator LEDs |
| 0x15 | Channel active indicators (4 channels + master) |

### BLE Module Communication (Firmware Side)

The DICE3 communicates with the TCH-BLE module via a **SPI-like serial link** (not classic UART), using the same 3-byte tuple format end-to-end.

#### Data Flow Architecture

```
  Android App / Linux Host
        │
        │ BLE GATT (3-byte tuples)
        │
  TCH-BLE Module (external chip)
        │
        │ SPI serial (3-byte tuples, 0x9E command prefix)
        │ Hardware: GPIO 0xB, SPI control at 0xCB000020
        │
  DICE3 ARM CPU
        │
        ├── Ring buffers (ringbuf_* @ 0x6A68–0x6BE0)
        │     ├── TX ring buf → FUN_000040bc (ble_send_param)
        │     ├── TX ring buf → FUN_0000618c (ble_send_mixer_param)
        │     └── RX ring buf ← FUN_00006044 (ble_serial_bridge)
        │
        ├── FUN_000021e8 (ble_param_dispatch) → mixer/DCP operations
        ├── FUN_00001d14 (ble_response_dispatch) → LED/status updates
        │
        └── DICE3 Mixer Hardware (0xC4000204)
```

#### Key Functions

| Address | Name | Description |
|---------|------|-------------|
| `0x00006044` | `ble_serial_bridge` | Reads 3-byte tuples from SPI RX, state machine reassembly (0xFF=idle), forwards to response ring buffer |
| `0x000040BC` | `ble_send_param` | Packs (paramID, subParam, value) into TX ring buffer |
| `0x0000618C` | `ble_send_mixer_param` | Sends mixer state updates to BLE module |
| `0x000021E8` | `ble_param_dispatch` | Dispatches incoming BLE params to mixer operations |
| `0x00001D14` | `ble_response_dispatch` | Dispatches BLE responses to LED/status updates |
| `0x00002F60` | `rotary_encoder_send` | Reads rotary encoder GPIO (pins 0xD, 0xE), sends 0xAE deltas |
| `0x00002B9C` | `button_scan` | Scans 16 physical buttons, sends 0xDB params |
| `0x00002CFC` | `switch_state_read` | Reads GPIO inputs, sends 0xDC switch state |

All of these run in the `led_update_thread` main loop (`0x00005010`) which polls continuously.

#### Ring Buffer Implementation

| Address | Function |
|---------|----------|
| `0x00006A68` | `ringbuf_available` — bytes available to read |
| `0x00006A84` | `ringbuf_free` — space available to write |
| `0x00006AA0` | `ringbuf_put_byte` — write single byte |
| `0x00006AD4` | `ringbuf_get_byte` — read single byte |
| `0x00006B10` | `ringbuf_write` — write multi-byte |
| `0x00006BE0` | `ringbuf_read` — read multi-byte |

#### FSS (Flash Storage, Not Serial)

FSS is a **flash file system**, not a serial subsystem. The "TCH-BLE" FSS segment (`FUN_000187C8` = `fss_find_segment`) stores the BLE module's firmware/calibration data in SPI flash, used during firmware update (`dcp_firmware_update`). It is NOT the runtime communication path.

#### Note on Mislabeled Functions

Functions previously labeled `ble_read_data` (`0x00003194`) and `ble_packet_parse` (`0x00003364`) are actually **flash memory read** functions — they read firmware image data from SPI flash during the DCP firmware update process, not from the BLE module.

### App-Side Error Handling

| Mechanism | Detail |
|-----------|--------|
| Command queue | Serial `BLECommandQueue` with `ReentrantLock`, 200ms timeout per command |
| Write retry | 1 retry attempt per peripheral on `writeCharacteristic` failure |
| Service discovery | Re-queued on `GATT_FAILURE` (257) |
| Partial message | Logged as error if received bytes not multiple of 3 |
| Disconnection | GATT closed, peripheral removed; `hasBlender=false` if no Blender remains |
| Scan timeout | 15 seconds; stops scan, disconnects if no Blender found |
| Slider lock | `slidersBeingEdited` prevents incoming BLE from overriding slider user is touching |

---

## 9. BLE Module Firmware Update (Internal, Boot-Time)

The DICE3 firmware contains a boot-time BLE module firmware update path. On every boot, `app_main_thread` (`0x000006F0`) runs the following sequence:

1. `fss_find_segment("Golden")` — locate BLE firmware image in SPI flash FSS
2. `ble_module_fw_check_and_update` (`0x00006240`) — query BLE module version via SPI serial:
   - Sends param `0xF2` (enter update mode) and `0xF0` (query version) to BLE module
   - If version == 0 (needs update): proceed to flash
   - If version == 6 (up to date): skip, enter main loop
3. `ble_module_enter_programming_mode` (`0x00006214`) — switch BLE module to DCP programming mode
4. `dcp_firmware_update` (`0x00005790`) — flash BLE module via DCP bit-bang (GPIO 3/4)

The firmware image header magic is compared against the "TCH-BLE" string (`0x00023042`), confirming the target is the BLE module. Flash data is read via `flash_read_data` (`0x00003194`) and parsed via `flash_parse_segment` (`0x00003364`).

### Update Packet Structure

The firmware image in flash contains 5 tagged sections:

| Tag | Content |
|-----|---------|
| 0 | Chip ID (4 bytes) — verified against connected DCP device |
| 1 | Target bank (1 byte) — flash bank selector |
| 2 | Code image — main firmware code (length field, pointer to data) |
| 3 | Checksum (2 bytes) — image verification |
| 4 | Data image — ancillary data (length field, pointer to data) |

### Flash Process

1. Parse packet, verify all 5 sections present (bitmask `0x1F`)
2. Reset DCP: `gpio_set(5, 1)` → `dcp_reset()`
3. Verify chip ID matches (read from DCP address register, compare)
4. Erase: write erase command `0x8000000D` or `0x8000000A` depending on chip type
5. Write code in 128-byte blocks (command `0x80000004`), using address register `0xD7B6` base
6. Verify code by reading back and comparing byte-by-byte
7. Write data in 64-byte blocks (command `0x80000004`), different address stride
8. Verify data
9. Verify checksum from DCP register
10. On success: toggle LED pin 5 with 1-second delay

### Return Codes

| Code | Meaning |
|------|---------|
| 0 | Success |
| 1 | Flash segment parse error |
| 2 | Magic/header mismatch |
| 3 | DCP reset failed |
| 4 | Chip ID mismatch |
| 5 | Pre-flash setup failed |
| 6 | Flash info read failed |
| 7 | Code write/verify failed |
| 8 | Code verify mismatch |
| 9 | Data write failed |
| 10 | Data verify failed / checksum mismatch |
| 11 | Post-flash verify failed |

---

## 10. Bootloader

The bootloader resides at `0x0004F050`–`0x0004F73C` and handles device startup, image selection, and jump-to-application.

### Boot Flow (`boot_check_and_load` @ `0x0004F404`)

1. Configure clock PLL from boot config registers (`0xC9000000`)
2. Initialize peripherals (LEDs, GPIO, interrupts)
3. Set clock dividers at `0xC5000024`–`0xC500002C`
4. Check boot flags byte — if bits `0x09` not set, skip to UART loader
5. Try primary image at `0x110000`, check header magic (`'ZZ'`)
6. If primary corrupt, try golden image at `0x40000`
7. Verify: header magic, size < `0x34000` words, CRC/checksum
8. On valid image: `boot_prepare_jump()`, set jump address, branch
9. If both fail: try recovery image at `0x10000` (size < `0xC000`)
10. If all fail: enter UART loader (infinite loop)

### Boot Header

Images start with a header checked for:
- Bytes 0–1: `'ZZ'` magic (both must be `'Z'`)
- Word at offset 0: compared against expected constant
- Size field: image size in 32-bit words, must not exceed maximum
- Image verification via `boot_verify_image(data_addr, size_in_words)`

### Boot Error Strings

| Address | String |
|---------|--------|
| `0x0004F854` | `"Corrupt boot header"` |
| `0x0004F86C` | `"No valid app"` |
| `0x0004F87D` | `"Try Golden"` |
| `0x0004F88A` | `"enter UART Loader"` |

---

## 11. CLI System

The firmware has a full interactive CLI accessible via UART console. Commands are installed by subsystem-specific `*_cli_install` functions.

### CLI Infrastructure

- Source: `cli.c`, `cliBuiltIn.c` from DICE SDK
- Error handling: `"CLI ERROR: Command/Variable not found. Try ? for help."`
- Built-in commands: `get`, `set`, `gms` (get-modify-set) — direct memory/register access
- zModem: `"Sorry, zModem not available yet"` (placeholder exists)

### CLI Command Groups

#### DAL (DICE Abstraction Layer) — `dal:*`
```
dal:clock  <0|1> <source> <rate>     — Set clock source/rate
dal:route  <0|1> <dst> <ch> <src> <ch> <n> — Set audio route
dal:create <0|1> <rateMode> <ins> <outs>   — Create clock domain
dal:destroy <0|1>                          — Destroy clock domain
dal:start  <0|1>                           — Start clock domain
dal:dump   <0|1>                           — Dump domain status
```

#### DICE Hardware — `dice:*`
```
dice:dump  <module>     — Dump registers (clock, router, hpll)
dice:wclk  <value>      — Set word clock
```

#### Mixer — `mix.*`
```
mix.dump                        — Dump mixer coefficients
mix.gain <out> <in0..15>        — Set gain
mix.cfg  <ins> <outs> <on>      — Configure mixer (1–32 ins/outs)
mix.clrin  <in>                 — Clear input coefficients
mix.clrout <out>                — Clear output coefficients
```

#### InS (Input/Serializer) — `ins.*`
```
ins.line  <port> <line> <rxtx> <mode> <dly> <align> <enable>
ins.clock <port> <mck> <bck> <fsl> <mck_inv> <bck_inv> <fck_inv> <enable>
ins.mute  <...>
```

Clock options:
- MCK: `f256br`, `f512br`, `f128fs`, `f256fs`
- BCK: `ch2`, `ch4`, `ch8`, `ch16`
- FSL: `0` (one clock), `1` (32 clocks)
- Inversion: `on`, `off`

#### USB — `usb.*`
```
usb.dump          — Dump USB audio configuration
usb.safety <rxtx> <sfty> <sfty_max> — Set USB safety margins
event <op>        — USB Audio events (dump, dumpifset, clear, df)
```

USB dump output includes:
- USB speed (HS/FS)
- Enabled status
- Audio configuration details

#### Built-in — `get`, `set`, `gms`
```
get <var|addr[:size]> [hex|binary|decimal|boolean]
get <var|addr[:size]> bit <n>
set <var|addr[:size]> <value>
set <var|addr[:size]> or <val>
set <var|addr[:size]> and <val>
set <var|addr[:size]> and <v1> or <v2>
set <var|addr[:size]> bit <n> on|off
gms <var|addr[:size]> <clearmask> <setmask>
```

Direct memory/register read/write with bitwise operations. Extremely powerful for runtime debugging.

---

## 12. SPI Flash (SST25XX)

External SPI flash for persistent storage (preset/configuration data).

- Init function: `sst25xx_init` @ `0x00008818`
- Init message: `"SST25XX : Init device with JEDEC ID"`
- Error handler: `"SPI Flash Error"`
- Subsystem check: `"FLASH sub-system not initialized"`

The SST25XX is a standard SPI NOR flash (SST25VF series). JEDEC ID is read and logged during init.

---

## 13. DICE SDK Source Modules

The firmware is built from the TCAT DICE3 SDK. Source file paths embedded in the binary:

| Module | Path | Description |
|--------|------|-------------|
| DAL | `dal/current/src/dal.c` | DICE Abstraction Layer core |
| DAL ClkCtrl | `dal/current/src/dalClkCtrl.c` | Clock domain controller |
| DAL Events | `dal/current/src/dalEvents.c` | Event handling |
| DAL Rates | `dal/current/src/dalRates.c` | Sample rate management |
| DICE AES | `dice_audio/current/src/diceAES.c` | AES3/EBU audio interface |
| DICE Clock | `dice_audio/current/src/diceClock.c` | Clock generation/PLL |
| DICE HPLL | `dice_audio/current/src/diceHPLL.c` | High-precision PLL |
| DICE InS | `dice_audio/current/src/diceInS.c` | Input serializer |
| DICE Mixer | `dice_audio/current/src/diceMixer.c` | Hardware mixer |
| DICE DCP | `dice_audio/current/src/diceDCP.c` | DCP interface |
| FSS | `fss/current/src/fss.c` | Flash file system (stores BLE module firmware/config) |
| CLI | `cli/current/src/cli.c` | Command-line interface |
| CLI BuiltIn | `cli/current/src/cliBuiltIn.c` | Built-in CLI commands |
| Key-Value | `misc/current/src/keyValue.c` | Key-value store |
| MIDI | — | `midi_engine.c`, `dice3midi` |
| Timers | — | `dice3_timers.c` |
| Flash | — | `flash.c` |

---

## 14. Blender vs GoXLR Differences

| Feature | GoXLR | Blender |
|---------|-------|---------|
| USB PID | `0x8FE0` / `0x8FE4` (Mini) | `0x8FE1` |
| Audio channels | 12×8 (in×out) | 12×2 (stereo mixer) |
| Physical controls | 4 faders, buttons, 4 encoders | 6 knobs only |
| **DCP vendor categories** | **Full (0x800–0x818)** | **None registered — all return header-only** |
| **DCP cmd=2 (SystemInfo)** | Yes (88 bytes) | **Yes (88 bytes) — only working DCP command** |
| **Mixer control** | **USB DCP (0x80B, 0x806, etc.)** | **BLE only (3-byte tuples)** |
| **DICE3 firmware update over USB** | **Yes (NVM cat 4 + DCP 0x810)** | **No (NVM not registered, no SPI flash USB endpoint)** |
| **BLE module firmware update** | N/A (no BLE module) | **Yes — boot-time via DCP bit-bang from "TCH-BLE" FSS segment** |
| Scribble strips (0x802) | Yes (LCD) | No |
| LED colors (0x803) | Yes (RGB per button) | No (single status LED) |
| LED state (0x811) | Yes (per-fader) | No |
| Animations (0x809, 0x816) | Yes | No |
| BLE control | No | Yes (reversed central/peripheral) |
| DSP params (0x80B) | GoXLR registers handler | Blender does NOT register handler |
| Routing (0x804) | GoXLR registers handler | Blender does NOT register handler |
| Boot quirk | Same | Same |
| DICE platform | Same (DICE3) | Same (DICE3) |
| Firmware build path | `troll-dsp/goxlr-dice3/` | `blender-dice3/` |
| BLE module | None | External "TCH-BLE" via SPI serial |
| SPI flash | Yes (SST25XX) | Yes (SST25XX) |
| CLI | Yes | Yes (identical command set) |

---

## 15. Key Firmware Addresses

### Functions

| Address | Name | Description |
|---------|------|-------------|
| `0x00000750` | `my_main_thread_entry` | Main application entry (creates `app_main_thread`) |
| `0x000006F0` | `app_main_thread` | Boot sequence + main loop (BLE fw check, image select, polling) |
| `0x00001468` | `set_mixer_gain` | Write gain to DICE mixer hardware (`0xC4000204`) |
| `0x000013F0` | `set_master_volume` | Set master output level |
| `0x0000153C` | `set_headphone_mode` | Switch headphone/normal monitoring |
| `0x00001D14` | `ble_response_dispatch` | Dispatch BLE responses → LED/status updates |
| `0x000021E8` | `ble_param_dispatch` | Dispatch incoming BLE params → mixer ops |
| `0x00002B9C` | `button_scan` | Scan 16 physical buttons, send 0xDB to BLE |
| `0x00002CFC` | `switch_state_read` | Read GPIO inputs, send 0xDC to BLE |
| `0x00002F60` | `rotary_encoder_send` | Read rotary encoder, send 0xAE delta to BLE |
| `0x00003194` | `flash_read_data` | Read data from SPI flash |
| `0x000031CC` | `flash_write_enable` | Enable SPI flash write operations |
| `0x000032D4` | `flash_verify_data` | Verify flash contents match buffer |
| `0x00003304` | `flash_erase_segment` | Erase a boot image flash segment |
| `0x00003364` | `flash_parse_segment` | Parse flash firmware segment header |
| `0x000033B8` | `boot_header_modify` | Modify boot image header flags in flash |
| `0x00003484` | `boot_image_validate_and_swap` | Validate boot images, swap if needed |
| `0x00003548` | `boot_image_select` | Select primary/golden boot image (checks 'ZZ' magic) |
| `0x000040BC` | `ble_send_param` | Pack and queue 3-byte tuple to BLE TX ring buffer |
| `0x000045C0` | `set_channel_volume` | Set channel gain (3 coefficients from lookup table) |
| `0x00004570` | `set_channel_solo` | Enable/disable channel solo mode |
| `0x00004FD4` | `halt_infinite_loop` | Dead-end: corrupt firmware trap (delay loop forever) |
| `0x00005010` | `led_update_thread_entry` | Main loop: polls buttons, encoder, BLE, GPIO |
| `0x000051CC` | `dcp_bitbang_read` | DCP serial read (GPIO 3/4) — BLE module programming |
| `0x00005360` | `dcp_bitbang_write` | DCP serial write (GPIO 3/4) — BLE module programming |
| `0x000054C0` | `dcp_read_reg` | Read DCP register (addr 0xD1 + data 0xDD) |
| `0x00005518` | `dcp_wait_complete` | Poll DCP status until complete |
| `0x00005590` | `dcp_write_reg` | Write DCP register (addr 0xD1 + data 0xDD) |
| `0x000055C8` | `dcp_reset` | Full DCP reset sequence |
| `0x00005790` | `dcp_firmware_update` | Flash BLE module firmware via DCP bit-bang |
| `0x00006214` | `ble_module_enter_programming_mode` | Switch BLE module from SPI serial to DCP programming |
| `0x00006240` | `ble_module_fw_check_and_update` | Boot-time BLE module version check + flash if needed |
| `0x000061CC` | `ble_read_response` | Read response from BLE module via SPI |
| `0x00008818` | `sst25xx_init` | SPI flash initialization |
| `0x00010150` | `flash_erase_range` | Erase flash address range (sector-aligned) |
| `0x00010454` | `spi_flash_read` | Low-level SPI flash read |
| `0x00009100` | `gpio_set_direction` | Set GPIO pin direction |
| `0x00009134` | `gpio_set` | Set GPIO pin value |
| `0x00009160` | `gpio_read` | Read GPIO pin value |
| `0x0000A804` | `thread_lock` | eCos thread mutex lock |
| `0x0000A81C` | `thread_unlock` | eCos thread mutex unlock |
| `0x0000A87C` | `thread_create_wrapper` | Create eCos thread |
| `0x0000AC6C` | `thread_create` | Thread creation |
| `0x0000AE08` | `thread_resume` | Resume suspended thread |
| `0x0000B8A4` | `thread_signal` | Signal/wake thread |
| `0x0000BBC0` | `semaphore_init` | Initialize semaphore |
| `0x0000F4C0` | `memcmp_8bytes` | 8-byte memory compare |
| `0x0001316C` | `dcp_printf` | Formatted output function |
| `0x00013B6C` | `sys_main_thread_entry` | DICE SDK system thread |
| `0x0001768C` | `dcp_dump_mixer_coefficients` | Print mixer matrix |
| `0x000187C8` | `fss_find_segment` | Find named segment in flash file system |
| `0x0001B3BC` | `usb_setup_dispatch` | USB setup packet dispatcher (vendor/class routing) |
| `0x0001BEF8` | `dcp_usb_thread_entry` | DCP-over-USB command handler |
| `0x0001BF6C` | `dcp_vendor_handler` | DCP vendor request handler (bReq 0x00–0x03 state machine) |
| `0x0001FE9C` | `usb_io_thread_entry` | USB I/O thread |
| `0x00002AE8` | `delay_ms` | Millisecond delay |
| `0x00006044` | `ble_serial_bridge` | SPI RX → ring buffer (3-byte reassembly, 0xFF=idle) |
| `0x0000618C` | `ble_send_mixer_param` | Send mixer state to BLE module |
| `0x00006A68` | `ringbuf_available` | Ring buffer: bytes available |
| `0x00006A84` | `ringbuf_free` | Ring buffer: space available |
| `0x00006AA0` | `ringbuf_put_byte` | Ring buffer: write byte |
| `0x00006AD4` | `ringbuf_get_byte` | Ring buffer: read byte |
| `0x00006B10` | `ringbuf_write` | Ring buffer: write multi-byte |
| `0x00006BE0` | `ringbuf_read` | Ring buffer: read multi-byte |
| `0x0004F050` | `boot_prepare_jump` | Prepare to jump to application |
| `0x0004F090` | `boot_init_peripheral` | Initialize boot peripherals |
| `0x0004F1A4` | `boot_delay` | Boot delay |
| `0x0004F22C` | `boot_update_leds` | Update boot status LEDs |
| `0x0004F32C` | `boot_verify_image` | Verify firmware image CRC |
| `0x0004F3E4` | `boot_read_header` | Read image header |
| `0x0004F404` | `boot_check_and_load` | Main bootloader entry |
| `0x0004F73C` | `boot_calc_timing` | Calculate clock timing |
| `0x00096DA4` | `dice_cli_install` | Install DICE CLI commands |
| `0x00097984` | `mixer_cli_install` | Install mixer CLI commands |
| `0x000995C0` | `usb_cli_install` | Install USB CLI commands |

### Data/Strings

| Address | Label | Content |
|---------|-------|---------|
| `0x00022ACC` | `str_Blender` | `"Blender"` (device name) |
| `0x00023042` | `str_TCH_BLE` | `"TCH-BLE"` (BLE module name) |
| `0x000231FB` | `str_Blender_MIDI_In` | `"Blender MIDI In"` |
| `0x0002320B` | `str_Blender_MIDI_Out` | `"Blender MIDI Out"` |
| `0x00029464` | `str_dice3midi` | `"dice3midi"` |
| `0x00080000` | `GOLDEN_IMAGE_START` | Start of backup firmware copy |
