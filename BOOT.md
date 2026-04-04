# DICE3 Boot Process — TC Helicon Blender

Complete boot flow from power-on through normal operation. For LED index tables
and animation internals, see [`led-map.md`](led-map.md). For USB/MIDI init details,
see [`USB.md`](USB.md).

## Boot Stages Overview

```
Power on
  → Internal ROM → TCAT bootloader (XIP 0x4F000)
    → 3-stage image validation with CRC32
    → DMA copy firmware body to SRAM
  → Firmware entry (SRAM 0x344)
    → [patched] boot_init: register DCP handler
    → rtos_app_init: start eCos scheduler
      → dice3_device_init: audio, USB, BLE, UART
      → Main event loop
        → [patched] init_audio_clock, patch_midi_callback, arm_midi_rx_endpoint
  → Host enumerates USB device
    → snd-usb-audio probes → ALSA card + MIDI port created
```

## Stage 1: Bootloader (XIP 0x4F000)

**CRITICAL**: The bootloader only runs on **power cycle**. `nRST` / `reset run`
resets the CPU to SRAM 0x0, which re-enters `firmware_entry` directly from
stale SRAM — the bootloader is skipped entirely.

### Hardware Init (`boot_check_and_load` at 0x4F404)

1. Set stack pointer to 0x4FD00
2. Clear bootloader BSS (0x4F8F4–0x4F900)
3. Read clock dividers from 0xC9000000
4. Configure UART: dividers at 0xC5000024/28, control 0xC500002C = 0x70 (115200 baud)
5. Configure GPIO (4 pin groups via 0xC9000030–44)
6. Disable LED coprocessor (0xCB000020 = 0)
7. Reset SPI flash controller (0xCC000014 = 2)
8. Reset SPI LED controller (0xCF000014 = 0xFF)
9. Initialize DMA engine (0x80000030, 0x80000110)
10. Read SPI flash JEDEC ID (3 bytes)
11. Read LED controller JEDEC ID via `led_spi_read_jedec` (0x4F1A4)

### UART Boot Banner

```
TCAT-BOOT
Reason  : Header
Version : 1.1.0.2477
Device  : TCD3021
```

Output at 115200 baud. Visible on FTDI FT232R adapter (`uart_monitor.py` auto-detects).
Bootloader prompt is `$` (minimal CLI, no useful commands).

### 3-Stage Image Validation

The bootloader tries three SPI locations in order. Each requires DMA read of the
48-byte header, validation of magic bytes and size, then CRC32 of the full body.

| Priority | SPI Address | Max Size | Magic Check | LED Progress | Notes |
|----------|-------------|----------|-------------|-------------|-------|
| 1 | 0x110000 | 208KB | 'ZZ' required | XOR bit 1 | **Unused on Blender** — no valid header here |
| 2 | 0x040000 | 208KB | 'ZZ' required | XOR bit 2 | **Primary firmware** — normal boot source |
| 3 | 0x010000 | 48KB | NO 'ZZ' check | XOR bit 3 | **Golden/recovery** — last resort |

The bootloader first reads the header at 0x110000 to check the 'Z' marker.
On the Blender, this slot is empty (0xFF), so it immediately falls through to 0x40000.

#### Image Header Format (48 bytes)

```
Offset  Size  Field
0x00    2     signature     'ZZ' magic (required for slots 1-2, skipped for slot 3)
0x02    2     crc_sig       additional validation bytes
0x04    4     size_words    body size in 32-bit words (body_bytes = size_words × 4)
0x08    40    reserved
```

Body starts at header + 0x30 (48 bytes). CRC32 is embedded as the last 4 bytes
of the body (`CRC32(body, init=0xFFFFFFFF) == 0xFFFFFFFF`).

#### CRC Verification (`spi_dma_read_and_crc` at 0x4F32C)

Reads body in 2KB chunks via DMA from SPI 0xCC000000 to SRAM buffer. CRC32 is
computed incrementally over each chunk. Returns 0 if `CRC32(full_body) == 0xFFFFFFFF`.

### Boot Success → Jump to Firmware

On successful validation:
1. Store boot source address at 0x4FFC0 (firmware reads this to detect golden copy)
2. Store clock config bytes at 0x4FFC4–0x4FFC5
3. Wait for UART TX to drain
4. Jump to SRAM 0x200: `mov r3, #0x200; bx r3`

### Boot Failure

| Failure | UART Output | LED State |
|---------|-------------|-----------|
| Primary header invalid | `* Corrupt boot header` | XOR bit 2 |
| Primary CRC failed | (silent, falls through) | XOR bit 2 |
| All slots invalid | `* None found - enter UART Loader` | All off |
| UART loader | `$` prompt (no useful commands) | All off |

When all three slots fail, the bootloader enters an infinite loop. The LED
engine is not running (requires RTOS). The device appears dead — all LEDs off,
`$` on UART, no USB enumeration.

## Stage 2: Firmware Entry (SRAM 0x344)

The bootloader DMA-copies the firmware body to SRAM starting at 0x200. The CPU
jumps to 0x200, which branches to `firmware_entry` at 0x344.

### Stock Firmware Entry

```
firmware_entry (0x344):
  1. pll_clock_init          — PLL/clock configuration
  2. Exception vectors       — Load IRQ/FIQ/abort handlers from ROM table
  3. BSS clear               — Zero-initialize BSS (0x2AA84–0x325F8)
  4. .ctors                  — Run global constructors (0x2AA40–0x2AA84)
  5. hal_platform_init       — Watchdog disable, IRQ unmask, UART baud, GPIO, DMA
  6. bl rtos_app_init        — Start eCos scheduler (never returns)
```

### Patched Firmware Entry

Our persistent patch replaces step 6:

```
firmware_entry (0x344):
  ...same steps 1-5...
  6. bl boot_init            — [PATCHED] replaces bl rtos_app_init
       → Register DCP 0x81F handler via direct memory writes to list at 0x313C4
       → Init flags: done_flag=DONE_MAGIC, midi_patched=0, clock_initialized=0
       → UART: "[boot_init] DCP registered"
       → Chain to rtos_app_init (starts scheduler, never returns)
```

## Stage 3: RTOS Startup (`rtos_app_init` at 0x9CD4)

Starts the eCos scheduler and creates system threads:

| Thread | Entry Point | Purpose |
|--------|-------------|---------|
| DICE3 device | `dice3_device_init` (0x13760) | Main device initialization |
| DCP dispatch | `dcp_command_dispatch_thread` (0x1C3BC) | DCP command processing |
| MIDI engine | `midi_engine_thread` (0x5010) | MIDI RX poll + dispatch |
| USB device | — | USB controller management |
| UART console | `uart_console_thread` (0x1ECDC) | Debug CLI (`>` prompt) |

## Stage 4: Device Init (`dice3_device_init` at 0x13760)

The main initialization thread. Runs as an eCos thread after the scheduler starts.

```
dice3_device_init:
  1. dice_set_device_context()
  2. dcp_usb_subsystem_init_once()
  3. flash_firmware_validate_and_init()     ← detects primary vs golden copy
  4. dice3_audio_routes_init()
  5. dice3_audio_driver_init()
  6. dice_audio_subsystem_init()            ← populates clock source table
  7. dice_set_sample_rate(literal)          ← stores rate value only (does NOT program HPLL!)
  8. dice_usb_init()                        ← registers USB endpoints + DCP handlers
  9. dice_usb_driver_init()                 ← USB controller goes live → device visible to host
  10. audio_thread_resume()
  11. uart_console_thread(0)                ← starts UART CLI
```

### Golden Copy Detection (step 3)

`flash_firmware_validate_and_init` reads the boot source from 0x4FFC0 (set by bootloader):

| Value at 0x4FFC0 | Boot Source | `is_golden_copy` | Behavior |
|-------------------|-------------|-------------------|----------|
| 0x040000 | Primary | 0 | Full operation: BLE, USB, mixer control |
| 0x010000 | Golden/recovery | 1 | **Restricted**: cycling LED animation, audio passthrough only. No BLE, no USB control, no mixer state init |

### The HPLL Gap (step 7)

`dice_set_sample_rate` only stores the rate value to a global — it does **not**
program the hardware PLL. The HPLL remains uninitialized until either:
- The host sends SET_CUR 48kHz (the "boot quirk"), or
- Our `init_audio_clock()` hook fires (step 6 below)

Without HPLL initialization, the kernel's `snd-usb-audio` driver fails to probe
because clock descriptors are invalid. No ALSA card is created — no audio, no MIDI.

## Stage 5: Main Event Loop (0x4FAC hook)

After all init threads complete, the main event loop runs. Our hook at 0x4FAC
fires on every iteration (~50Hz via `cyg_flag_wait` wakeup).

### First Iteration (patched firmware)

```
flash_handler_init():
  1. init_audio_clock()       — usb_audio_rx_set_sample_rate(48000)
                                 → clock_source_binary_search → usb_audio_mode_config
                                 → HPLL programmed, clock descriptors now valid
                                 UART: "[clock] 48kHz"

  2. patch_midi_callback()    — patches midi_parser_ctx+0x28 to our midi_cc_handler
                                 → copies mixer_state to mixer_snapshot for change detection

  3. arm_midi_rx_endpoint()   — forces EP 0x03 state to 0x02 (configured)
                                 → calls usb_midi_rx_start(ep, 1)
                                 → EP 0x03 armed for native USB MIDI receive
                                 UART: "[midi] EP armed"

  4. midi_emit_state_diff()   — compares mixer_state vs snapshot
                                 → sends MIDI CC for any changes (initial state dump)
```

### Subsequent Iterations

- `process_mailbox()` — JTAG mailbox commands (erase/write/reboot)
- `init_audio_clock()` — no-op (already initialized)
- `patch_midi_callback()` — no-op (already patched)
- `arm_midi_rx_endpoint()` — checks ep+8, re-arms if callback cleared (after USB disconnect)
- `midi_emit_state_diff()` — detects knob/BLE changes, sends MIDI CC to host

## Stage 6: USB Enumeration (host side)

After `dice_usb_driver_init` (Stage 4 step 9), the device appears on the USB bus.

```
~0ms    USB controller enabled, device visible to host
~100ms  Host: GET_DESCRIPTOR (device, config, string)
~200ms  Host: SET_ADDRESS, SET_CONFIGURATION
~300ms  Host: snd-usb-audio probe begins
          → CS_RANGE query for sample rates (fails: DICE3 hardware bug)
          → Fallback to QUIRK_FLAG_FIXED_RATE (48kHz)
          → "Quirk or no altset; falling back to MIDI 1.0"
~1s     ALSA card registered: audio PCM + MIDI port
```

**With our clock init**: HPLL is programmed before USB goes live (Stage 5 fires
before host completes enumeration), so clock descriptors are valid. The boot quirk
in the kernel becomes redundant.

**Without our clock init**: Kernel needs the `snd_usb_tc_helicon_boot_quirk` to
send SET_CUR 48kHz before probing. Without it, no ALSA card is created.

## LED Pattern Diagnostic

### What to look at first

| What you see | What it means | What to do |
|-------------|---------------|------------|
| **Mixer state LEDs** (inputs W/R/B, outputs W/B, level meter) | Normal boot from primary | Everything working |
| **White+blue sweep** cycling L→R→L with 4-beat pause | Recovery boot (golden copy) | Primary image CRC failed. Reflash via JTAG. |
| **All LEDs off**, `$` on UART | Bootloader: all images failed CRC | Both primary and golden are corrupted. JTAG reflash required. |
| **All LEDs off**, no UART output | No power or bootloader itself corrupted | Check power. If powered, SPI sector 0x00000 (bootloader) may be damaged. |
| **6 red input LEDs** (IN1-IN6 red), all else off | Fatal firmware error | Flash bounds violation or critical error. Power cycle to recover. |
| **Blue sweep L→R** | BLE coprocessor recovery | Normal after BLE module reflash. Wait for completion. |
| **Blue sweep R→L** (reverse) | BLE FW update in progress | **Do NOT power off.** Wait for completion. |
| **All LEDs on** (all 46 bright) | Firmware crash during init | Init code corrupted or hook crashed before LED engine started. JTAG reflash. |

### Timeline: Normal Boot (Primary)

```
Time     LEDs                          UART                    Stage
─────    ────                          ────                    ─────
0.0s     Off                           (nothing)               Power applied
0.1s     Brief status flash            TCAT-BOOT...            Bootloader validating
0.5s     Off                           $                       Bootloader → SRAM jump
0.7s     Status LED (39) on            (nothing)               hal_platform_init
1.0s     USB white (37) on             [boot_init] DCP...      boot_init → rtos_app_init
1.5s     Input/output LEDs appear      [clock] 48kHz           dice3_device_init complete
                                       [midi] EP armed         Main loop first iteration
2.0s     Level meter active            >                       UART CLI ready
~3s      Full mixer state              (host probing)          USB enumeration complete
```

### Timeline: Recovery Boot (Golden Copy)

```
Time     LEDs                          UART                    Stage
─────    ────                          ────                    ─────
0.0s     Off                           (nothing)               Power applied
0.1s     Brief status flash            TCAT-BOOT...            Bootloader: primary CRC FAIL
0.3s     Brief flash                   * Corrupt boot...       Bootloader: trying golden
0.5s     Off                           (nothing)               Golden CRC OK → SRAM jump
1.0s     White+blue cycling sweep      (nothing)               is_golden_copy=1, animation started
         ↓ continues indefinitely                              Audio passthrough only, no BLE/USB ctrl
```

### Timeline: Total Failure

```
Time     LEDs                          UART                    Stage
─────    ────                          ────                    ─────
0.0s     Off                           (nothing)               Power applied
0.1s     Brief flash                   TCAT-BOOT...            Bootloader: primary CRC FAIL
0.3s     Brief flash                   * No valid app          Bootloader: golden CRC FAIL
0.5s     All off                       * None found...         Infinite loop, $$ prompt
         ↓ stays off                   $                       No firmware loaded
```

## Key Addresses

| Address | Name | Stage | Purpose |
|---------|------|-------|---------|
| 0x4F000 | `_reset_vector` | Bootloader | ARM vector table |
| 0x4F404 | `boot_check_and_load` | Bootloader | 3-stage validation + DMA copy |
| 0x4F32C | `spi_dma_read_and_crc` | Bootloader | CRC32 verification |
| 0x4F22C | `led_spi_write` | Bootloader | 10-byte LED status update |
| 0x00344 | `firmware_entry` | Firmware | PLL, BSS, .ctors, platform init |
| 0x09414 | `hal_platform_init` | Firmware | Watchdog, IRQ, UART, GPIO, DMA |
| 0x09CD4 | `rtos_app_init` | Firmware | Start eCos scheduler |
| 0x13760 | `dice3_device_init` | Firmware | Audio, USB, BLE subsystem init |
| 0x1874C | `flash_firmware_validate_and_init` | Firmware | Detect primary vs golden |
| 0x1E424 | `dice_set_sample_rate` | Firmware | Store rate (NO HPLL programming) |
| 0x194C8 | `usb_audio_rx_set_sample_rate` | Firmware | Clock source lookup + HPLL program |
| 0x04FAC | Hook site | Patch | Main loop — our init_audio_clock + EP arming |
| 0x32600 | Patch zone | Patch | Handler code (2297 bytes) |
| 0x4FFC0 | Boot source | Data | Set by bootloader, read by firmware for golden detection |

## SPI Flash Layout

```
0x00000 ┌─────────────────────┐
        │ Bootloader (sector 0)│  XIP at 0x4F000
0x10000 ├─────────────────────┤
        │ Golden/Recovery Copy │  48KB max, no 'ZZ' check
        │ (protected)          │
0x40000 ├─────────────────────┤
        │ Primary Firmware     │  208KB max, 'ZZ' + CRC
        │  Header (0x30 bytes) │
        │  Body (size_words×4) │
        │  [patched handler]   │  0x32600 in SRAM = SPI ~0x6A030
        │  CRC checkword       │
0x8B000 ├─────────────────────┤
        │ (unused/data)        │
0x110000├─────────────────────┤
        │ Slot 1 (unused)     │  Checked first by bootloader, empty on Blender
0x200000└─────────────────────┘  2MB SST25VF016B
```

## Recovery Procedures

### Corrupted Primary (golden still works)

Symptom: White+blue cycling sweep animation, `is_golden_copy=1`.

Fix: JTAG reflash primary sectors (0x40000–0x72000) from `blender_spi_patched.bin`
or `blender_spi_flash_restored.bin` using `flash_full_image.py`.

### Corrupted Both Primary and Golden

Symptom: All LEDs off, `$` on UART, no USB.

Fix: JTAG reflash entire SPI image (122 sectors) using `flash_full_image.py --ref blender_spi_flash_restored.bin`.

### After JTAG Reflash

1. Kill OpenOCD: `kill $(pgrep openocd)`
2. Power cycle (unplug all cables, reconnect)
3. Monitor UART: `python3 firmware/uart_monitor.py --expect TCAT-BOOT`
4. Verify USB: `lsusb | grep 1220`
5. Verify MIDI: `amidi -l`
