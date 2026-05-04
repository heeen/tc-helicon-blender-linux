# DICE3 Boot & Initialization

Single canonical source for the boot path from ROM through eCos scheduler
hand-off, plus the UART recovery loader. Supersedes:

- `boot_sequence_verified.md` (chronological boot flow)
- `firmware_init_flow.md` (per-stage bisection guide)
- `tcat_uart_loader.md` (ROM UART CLI reference)

Inline status tag convention: see `hardware-reference.md` header.

---

## 1. Boot stages overview

```
Power-on
  └─ TCAT mask-ROM @ 0x20000000
     └─ Stage-2 bootloader (dice3_bootloader.bin) @ XIP 0x4F000
        └─ boot_check_and_load @ 0x4F404 — 3-stage fallback ladder:
           ├─ slot A (primary):  SPI 0x40000 or 0x110000 (boot_order_flag picks)
           ├─ slot B (alt):      the other of 0x40000 / 0x110000
           └─ slot C (golden):   SPI 0x10000
        └─ each slot: header check → DMA-read body → CRC32 → jump to firmware_entry
        └─ all-fail path: ROM UART loader CLI (TCAT-BOOT shell)
   
firmware_entry @ 0x200 (primary firmware)
  ├─ A. pll_clock_init       @ 0x8FAC  — PLL up, AHB at 200 MHz
  ├─ B/C. install ARM exception vectors at 0x04..0x3C
  ├─ D. zero BSS              [DAT_0x698 .. DAT_0x69C)
  ├─ E. hal_platform_init    @ 0x9414  — UART, DMA, I-cache enable
  ├─ F. ctors_run            @ 0x8D70  — .ctors array
  └─ G. rtos_app_init        @ 0x9CD4  — eCos scheduler start (NEVER RETURNS)
```

[verified 2026-04-23 Ghidra `firmware_entry @ 0x200` decompile]

---

## 2. ROM mask boot

The ARM926EJ-S ROM is mapped at `0x20000000`. Reads SPI-flash byte 0 looking
for a 16-byte TCAT header (magic `'TCAT'` + version 3). On valid header,
DMA-loads the bootloader payload to XIP and jumps to `0x4F000`. On
invalid header or CRC fail, drops into the ROM UART CLI ("TCAT-BOOT
shell" — see §6).

ROM functions of interest (in the ROM bootloader binary, base 0x20000000):
- `*ptr_rom_uart_loader` — the CLI re-entry the stage-2 bootloader can call
  on its own all-fail path. [verified Ghidra]
- `_ROM_CRC_COMPUTE @ 0x20000DA4` — CRC32 step routine; conv: poly
  0xEDB88320 reflected, init=0, no final XOR. [verified 2026-05-01 Ghidra
  + dynamic test against `zlib.crc32(buf, 0xFFFFFFFF) ^ 0xFFFFFFFF`]
- ROM table at `0x20000100` (magic `'TC30'`, version, function pointer
  table; CRC step at table offset +0x0C). [verified 2026-05-01 Ghidra]

---

## 3. Stage-2 bootloader (dice3_bootloader.bin @ 0x4F000)

Loaded into XIP (`0x4F000..0x4F8FA`, fixed ~2.3 KB read-only window) by
the ROM. Single key function:

### `boot_check_and_load @ 0x4F404` [verified 2026-05-03 Ghidra full decompile]

```c
// 1. Programs UART, SPI flash CLK_DIV (=2), LED SPI CLK_DIV (=0xFF).
// 2. led_spi_write_cmd(); led_spi_read_jedec() x2 — board-ID probe via LED bus.
// 3. Picks slot order based on boot_order_flag bit:
//    - flag set: try 0x110000 then 0x40000 then 0x10000
//    - flag clear: try 0x40000 then 0x110000 then 0x10000
// 4. For each slot:
//    - spi_read_header() to read sig+magic+body_size_words
//    - check sig == 'Z','Z' AND header_magic == fw_header_magic
//    - check body_size_words < 0x34000  (= 832 KB max for primary slots)
//    - body_size_words < 0xC000        (= 192 KB max for golden 0x10000)
//    - spi_dma_read_and_crc(slot + 0x30, body_size_words << 2)
//    - on success: fw_spi_base_addr = slot; firmware_entry_stub() (= jump 0x200)
// 5. All slots fail: uart_puts(...); (*ptr_rom_uart_loader)(); infinite loop.
```

Slot layout summary [verified Ghidra]:

| Slot | SPI offset | Max body size | Role |
|---|---|---|---|
| Primary A | 0x40000 | 832 KB (0x34000 words × 4) | Application firmware (Blender uses this) |
| Primary B / alt | 0x110000 | 832 KB | Secondary copy (boot-order-flag selectable) |
| Golden / recovery | 0x10000 | 192 KB | Minimal firmware for in-field repair; no `'Z','Z'` enforcement on boot path |

Header at SPI `slot + 0x00` (32 B): `'Z','Z'` magic at +4/+5,
`fw_header_magic` at +0x10, `body_size_words` at +0x14. Body starts
at SPI `slot + 0x30`. [verified Ghidra `boot_check_and_load` body]

### Bootloader's read pattern — `dma_spi_read_setup @ 0x4F290`

```c
SPI_FLASH_CMD      = 0x307;        // RX-DMA mode (CTRL: dir=RX | base=0x007)
SPI_FLASH_SIZE     = length - 1;
SPI_FLASH_DMAMD    = 1;            // RX-only DMA (NOT bidir like our v2 driver)
SPI_FLASH_ENABLE   = 1;            // controller EN=1
SPI_FLASH_DMACFG1  = 3;
DMA_CH0_SRC = 0xCC000060;          // DATA register, NOT 0xCC000070 (RX_PORT)
DMA_CH0_DST = dest;
DMA_CH0_XFER = (length & 0xFFF) | 0x88009000;
DMA_CH0_CFG = 0xD006 | 1;          // arm
SPI_DATA = 0x03;                   // PIO push READ opcode + 3-byte address
SPI_DATA = (addr >> 16) & 0xFF;
SPI_DATA = (addr >>  8) & 0xFF;
SPI_DATA = addr & 0xFF;
SPI_CS = 1;                        // start clocking (= "SPI_FLASH_START")
```

Caps each chunk at `0x800` (2048) to keep within the 12-bit DMA count
field. Cross-referenced with the v2 driver's bidir read in `spi-flash.md`.
[verified 2026-05-02 Ghidra]

---

## 4. XIP window (bootloader-only)

| Property | Value | Verification |
|---|---|---|
| CPU address range | `0x4F000..0x4F8FA` (fixed ~2.3 KB) | [verified 2026-03-27 JTAG] |
| SPI source range | `0x10..0x90A` | [verified] |
| Mapping | `XIP_addr = (SPI_addr - 0x10) + 0x4F000` | [verified 5 byte-for-byte comparisons] |
| Encryption | **NONE** — bytes match SPI 1:1 | [verified — `xip_encryption.md` body refutes its own title's "encryption" conjecture] |
| Window size | Hardware-fixed at power-on from TCAT header | [verified: writing 0xDEADBEEF to SPI 0x1000 doesn't appear at XIP 0x4FFF0] |
| Execution | MMU off, single-stepping works at XIP addresses | [verified JTAG] |

Custom code at higher SPI addresses is unreachable via XIP and must
be loaded into SRAM. [verified by attempted experiment]

---

## 5. Primary firmware init (firmware_entry @ 0x200)

[All verified 2026-04-23 Ghidra]

### Stage A: `pll_clock_init @ 0x8FAC`

Programs 0xC9 clock block. Brings AHB to 200 MHz (Fvco / 2).
Polls `0xC2000030 & 1` for PLL lock. See `hardware-reference.md` §
"Clock & PLL block" for full register sequence with the verified
values. [verified 2026-05-03 Ghidra direct decompile]

**Bisect signal**: hang here = PLL never locks. JTAG-read 0xC9000000 +
0xC2000030.

> Note: a separate function `pll_program @ 0x162C0` exists; its name is
> misleading — it writes to `0xC4000700-0xC4000708` which is the
> **audio mixer block (0xC4)**, NOT the system PLL. It's the audio
> PLL/sample-rate config, not the CPU PLL. [verified 2026-05-03 Ghidra]

### Stages B–C: exception vector install

Two phases: undef/SVC/data-abort first, then IRQ/FIQ/PrefetchAbort.
Vector source `DAT_0x6E8` (early) / `DAT_0x6BC` (late) → copies into
`0x04..0x3C`. [verified Ghidra]

### Stage D: `hal_platform_init @ 0x9414`

```
*0xC200002C = 0;                  // timer pre-arm clear
patch interrupt vector slot[+0x14] for IRQ obj
hal_interrupt_unmask(5);          // VIC channel 5 (timer)
*0xC2000028 = 0xA6;               // timer enable bits
uart_baud_init();                 // 0x968C
if (*0xC9000028 != 1) gpio_set_directions(...);  // BOARD_REV check
dma_controller_init();            // 0x9948 — builds 0x50 driver-state block
dma_irq_init();                   // 0x95B0 — IRQ 10 unmask
coproc_Control |= 0x1000;         // I-cache enable
```

`dma_controller_init` builds the 0x50 driver-state block at the
audio-clock handler table (`0x2F610`), wires `audio_clock_select`
defaults, calls `audio_clock_enable()` and re-selects clock 1.
`dma_irq_init` writes `DMA_ENGINE.master_en=1, en=0xFF, iclr=0xFF`,
then registers `dma_isr/dma_dsr` on IRQ 10 and unmasks it. [verified Ghidra]

**Bisect signals**: no UART output → `uart_baud_init` failed (PLL freq
wrong). DMA never completes → `dma_irq_init` skipped. Random crashes
only with cache: bit `0x1000` of CP15 Control.

### Stage E: `ctors_run @ 0x8D70`

Iterates the eCos `.ctors` array, calls each. Specific ctors decoded:
- `ctor_16_init @ 0x0B84` — flash partition table (9 entries) [verified Ghidra]
- Other ctors (1–15, 17+) untraced [conjecture: standard eCos device init]

### Stage F: `rtos_app_init @ 0x9CD4` (entry to scheduler)

Spawns `dice3_device_thread_start @ 0x13B6C` which creates `sysMainThread`
and starts the eCos scheduler. **Never returns.**

### Thread map (post-scheduler-start)

| Thread | Priority | Entry | Address | Stack | Created by |
|---|---|---|---|---|---|
| sysMainThread | 16 | `dice3_device_init` | 0x13760 | 3000 | `dice3_device_thread_start` |
| dcp_usb_thread | 16 | `dcp_command_dispatch_thread` | 0x1C3BC | 1200 | `dcp_usb_init` (via `dice_usb_driver_init`) |
| usb_io_thread | 16 | `usb_control_endpoint_thread` | 0x2016C | 500 | `usb_endpoint_open` (via `dice_usb_driver_init`) |
| (audio thread) | 16 | `audio_io_thread_main` | 0x142C4 | 800 | `dice3_audio_driver_init` |
| my_main_thread | ? | `midi_engine_thread` | 0x5010 | ? | (not visible in boot trace) |
| led_update_thread | 17 | `ble_spi_bridge_thread` | 0x4F38 | 4000 | `midi_engine_thread` |

### `sysMainThread` init (`dice3_device_init @ 0x13760`)

```
dcp_usb_subsystem_init_once @ 0x1C4E0   — one-time DCP/USB clock init
flash_firmware_validate_and_init @ 0x1874C
  └── flash_subsystem_init @ 0x100A8     — SPI flash driver
dice3_audio_routes_init @ 0x15DA0        — I2S + mixer routing
dice3_audio_driver_init @ 0x147C8        — creates audio thread (SUSPENDED)
usb_config_validate @ 0x1EF68
dcp_install_handler_with_data            — standard DCP handlers (categories 0-4)
dice3_usb_config_register @ 0x1439C      — audio stream subscriber
dice_audio_subsystem_init @ 0x17FD0      — mixer HW + DCP categories
```

[verified Ghidra]

---

## 6. ROM UART loader ("TCAT-BOOT shell")

Reverse-engineered from `dice3_rom_bootloader.bin` (Ghidra), base
`0x20000000`. ROM version `1.1.0.2477`, ARM926EJ-S.

### When it activates

The ROM UART loader activates when **all firmware images fail validation**:

1. ROM reads 16-byte SPI header at offset 0 → validates `'TCAT'` magic + version 3
2. If header valid: DMA-reads firmware body, optionally CRC32-checks it
3. If header/CRC fails: enters UART CLI instead of jumping to firmware

The TCAT bootloader at `0x4F000` has its own 3-stage fallback (see §3).
If all three fail, it calls `(*ptr_rom_uart_loader)()` which re-enters
the ROM CLI.

### UART configuration

| Parameter | Value |
|---|---|
| Baud | 115200 |
| Data bits | 8 |
| Parity | None |
| Stop bits | 1 |
| Hardware | `0xC5000000` (DICE3 UART) |
| Connector | Unpopulated 5×2 1.27mm header (Header 2) |
| Linux device | `/dev/ttyUSB1` (via FTDI adapter) [verified 2026-03-26 UART probe] |

### Boot banner

```
TCAT-BOOT
Reason  : Header          ← or "CRC"
Version : 1.1.0.2477
Device  : TCD3021
$
```

- **Reason: Header** — SPI header magic/version invalid (sector 0 corrupt or erased)
- **Reason: CRC** — header valid but firmware body CRC32 failed

### CLI commands

Single-character, case-sensitive. `$` prompt. Max 17 chars input, CR/LF to submit.

| Cmd | Form | Purpose |
|---|---|---|
| `S` | `S` | Re-display boot banner |
| `R` | `R 0x<address>` | Run (jump via BLX to address) |
| `L` | `L 0x<addr>` | XMODEM-CRC load to SRAM, then can `R` |
| (more) | — | (full reference: see `tcat_uart_loader.md` historical content; Ghidra `dice3_rom_bootloader.bin`) |

Most useful for our work: **`L`** then **`R`** to load the v2 flash
driver from a clean ROM state when eCos is broken. [verified 2026-04-13]
