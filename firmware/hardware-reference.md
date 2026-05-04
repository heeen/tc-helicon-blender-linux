# DICE3 Hardware Reference

Single canonical source for peripheral memory map, register layouts, pin
assignments, and IP fingerprints. Supersedes:

- `hw_mapped_regions_usage.md` (Ghidra MMIO scan)
- `Registers.md` (TCAT-BOOT vs eCos register comparison)
- `ip_fingerprints.md` (JTAG-probe IP identification)
- `dice3_usb_registers.md` (USB controller register windows)
- Hardware sections of `spi_architecture.md` and `dma_engine.md`

Inline status tag convention used throughout:

- `[verified DATE METHOD]` — confirmed by hardware probe / decompile / runtime test
- `[conjecture]` — best current hypothesis, not yet tested
- `[outdated DATE: REASON]` — kept as paper trail; do not act on
- `[ambiguous: see X]` — two reasonable interpretations exist

---

## Peripheral memory map

| Base | Function | Notes |
|---|---|---|
| 0x00000000 | SRAM (512 KB) | Bit-19 alias: 0x80000 mirrors 0x00000 [verified 2026-04 JTAG] |
| 0x80000000 | AHB DMA controller | 8 channels CH0-CH7, per-ch +0x100..+0x1E0 [verified 2026-04-22 JTAG]. NOT shared with USB. |
| 0x90000000 | USB controller (Synopsys DWC2 v3.20a) | Internal DMA, separate from 0x80 [verified 2026-04-29 GSNPSID readback] |
| 0xC2000000 | Timer / tick / perf counter | [verified Ghidra] |
| 0xC4000000 | Audio mixer | [verified Ghidra] |
| 0xC5000000 | UART | 115200 8N1 console [verified 2026-03-26 UART probe] |
| 0xC9000000 | Clock/PLL | All writes need 0xABCD upper-16 key [verified 2026-04 JTAG] |
| 0xCA000000 | GPIO bank 0 (pins 0–7) | Per-pin: addr = base + (1<<pin)*4 [verified 2026-05-03 Ghidra `gpio_write_sdk`]. BLE reset pin 6 = `0xCA000100`, mode-strap pin 7 = `0xCA000200`. |
| 0xCB000000 | GPIO bank 1 (pins 8–15) | Per-pin: addr = base + (1<<(pin-8))*4 [verified 2026-05-03 Ghidra]. BLE enable pin 8 = `0xCB000004`, CS strobe pin 11 = `0xCB000020`, pin 12 = `0xCB000040`. |
| 0xCC000000 | Flash SPI controller | Drives SST25VF016B (JEDEC 0xBF2541, 2 MB, 4 KB sectors) [verified] |
| 0xCF000000 | LED SPI controller | TC-BLE module 3-wire bridge + LED ring [verified Ghidra] |
| 0xFFFFF000 | VIC (PL190-family) | [verified 2026-04-23 JTAG] |

### Unused address space

JTAG bus scan finds `0x40000000` reads `0x800000` consistently, but no
firmware or bootloader code accesses it as a peripheral (zero literals in
`dice3_bootloader.bin`, no MMIO function-references in primary firmware
per Ghidra scan). Likely a phantom address decoded by the AHB but with no
software consumer. [verified 2026-03-27 JTAG probe; verified 2026-04-23
Ghidra MMIO scan]

---
## SPI controller register layout (0xCC=flash, 0xCF=LED — same IP block)

| Offset | Name | Description |
|---|---|---|
| 0x00 | CTRL | Mode word: `direction \| base`. Base = 0x007 (polling) or 0x1006 (IRQ-mode, stock fw). Direction adds: `0x100` = TX-only, `0x200` = RX-only, `0x300` = bidir-channel-mode, `0x000` = bidir. So `0x107` = TX, `0x207` = RX-only, `0x307` = RX-DMA-mode (stock bootloader's read), `0x007` = bidir (our v2 driver). [verified Ghidra + runtime] |
| 0x04 | LEN | Total transfer length minus 1 (1 ⇒ 2 bytes on wire) [verified] |
| 0x08 | EN | Controller enable: 1 = on, 0 = off [verified] |
| 0x0C | (unknown) | Sometimes written 0; purpose unclear [conjecture] |
| 0x10 | CS | Chip select assert (bit 0 = CS low). The cs_index field in the firmware's resource_t is a controller slot selector, NOT a MOSI CS mask — the chip only responds when bit 0 is set. [verified 2026-04-17] |
| 0x14 | CLK | IP-level prescaler. Flash uses 2 (driver) or 0x32 (stock eCos = 8 MHz wire); LED uses 0xFF [verified] |
| 0x18 | CLRINT | Write 0 to clear pending IRQ edge (done right before kickoff in DMA mode) [verified] |
| 0x28 | STAT | bit 0 = BUSY, bit 1 = TX_READY, bit 3 = RX_READY [verified] |
| 0x2C | DMAGO | Stock v1 firmware never writes 1 here (reset only); our v2 follows the same convention [verified Ghidra] |
| 0x34 | ERR | Error flags. bit 2 = transfer error (bootloader's `dma_poll_complete` increments a counter on this bit) [verified Ghidra] |
| 0x4C | DMAMD | DMA mode select. 0 = PIO, 1 = RX-only DMA, 2 = TX-only DMA (CH0 → TX_PORT), 3 = bidir DMA (CH0=RX, CH1=TX) [verified] |
| 0x50 | DMACFG0 | Per-direction DMA framing constant. Stock always writes 4 [verified] |
| 0x54 | DMACFG1 | Per-direction DMA framing constant. Stock always writes 3 [verified] |
| 0x60 | DATA | PIO data register (8-bit r/w). Also DMA source for the bootloader's RX-DMA pattern (CTRL=0x307) — the IP multiplexes TX-PIO writes and RX-DMA reads on the same register [verified 2026-05-02] |
| 0x70 | RX_PORT | RX DMA port (used by our v2 driver bidir reads) [verified] |
| 0x80 | TX_PORT | TX DMA port [verified] |

`Registers.md` mislabeled several rows (CS at 0x18, DMAMD at 0x2C, etc.).
The offsets above match driver source `dice3_hw.h`, Ghidra decompile of stock
firmware, and bootloader code. [Registers.md labels: outdated 2026-05-02]

---
## DMA — two independent controllers

DICE3 has TWO physically distinct DMA controllers. The USB controller does
NOT consume channels on the generic engine. [verified 2026-04-22 JTAG live state]

| Controller | MMIO base | Scope | Channels | Used by |
|---|---|---|---|---|
| Generic AHB DMA | `0x80000000` | mem ↔ any peripheral | **8 (CH0..CH7)** | flash SPI, LED SPI |
| USB-internal DMA | inside `0x90000000` USB IP | USB FIFO ↔ SRAM | 6 EP pipes | USB device controller |

USB registers that look DMA-shaped (`0x90000104..0x90000118`) are pointers
to dTD-like descriptor heads in SRAM consumed by the USB IP's embedded DMA,
not pointers into the generic engine. SPI driver DMA hygiene (`DMA_ICLR`,
channel arm semantics) has no effect on USB and vice versa. [verified Ghidra]

`spi_architecture.md` originally claimed "Ch0-3" — written before the full
DMA map was probed. [outdated 2026-04-22: probe confirms 8 channels]

## Generic AHB DMA engine — 0x80000000

### Engine-level registers

| Offset | Name | Role | Stock writes |
|---|---|---|---|
| +0x04 | STATUS | per-channel done bits (read; DSR reads this) | — |
| +0x08 | EN | per-channel enable bitmap | `1<<ch` per arm; `0xFF` at init |
| +0x10 | ICLR | per-channel IRQ clear (write-1) | `1<<ch` per arm; `0xFF` at init |
| +0x14 | (latched history) | reads 0x0F post-run | — |
| +0x30 | MASTER_EN | engine master go flag | `1` at `dma_irq_init` |
| +0x100 + ch*0x20 | per-channel descriptors (5 words) | see below | per-arm |

Note: `dice3_hw.h` originally called `+0x14` "DMA_ISTAT"; stock's `dma_dsr`
always reads `+0x04` for the "transfer done" mask. Both reflect related
state. [verified Ghidra `dma_dsr @ 0x951C`]

### Per-channel descriptor block (+0x100 + ch*0x20, 5 words)

| Field | Offset | Role |
|---|---|---|
| src | +0x00 | source physical address |
| dst | +0x04 | destination physical address |
| next | +0x08 | next descriptor in chain (0 = end) |
| cfg | +0x0C | byte count (low 12b) \| direction/mode (upper bits); bit 31 = end-of-chain |
| go | +0x10 | trigger word: `cfg \| 0x8001` at minimum |

Trigger word values observed:
- Driver v2: `0xD005` (TX) / `0xD007` (RX)
- Stock eCos live: `0xD006` (CH0) / `0xD004` (CH1)
- Bootloader (RX-DMA mode): `0xD006 \| 1 = 0xD007` to start

The driver's values differ from stock but are functional. Exact bit
encoding is undocumented. [verified runtime; encoding ambiguous]

### Live channel allocation (eCos running, idle, JTAG-probed 2026-04-22)

| Channel | SRC | DST | Owner |
|---|---|---|---|
| CH0 | `0xCC000060` | `0x34037` | flash, stage-2 residue (was driver scratch) |
| CH2 | `0xCC000070` | `0x2F211` | stock flash RX_PORT |
| CH3 | `0x2F1F1` | `0xCC000080` | stock flash TX_PORT |
| CH4 | `0xCF000070` | `0x297EA` | LED SPI RX_PORT |
| CH5 | `0x297ED` | `0xCF000080` | LED SPI TX_PORT |
| CH1, CH6, CH7 | — | — | unused/zero |

v2 driver uses CH0/CH1 for flash to avoid contention with stock CH2/CH3
when running from warm eCos state. [verified 2026-04-22]

---
## Clock & PLL block — 0xC9000000

All writes to offsets 0x00..0x24 require `0xABCD` in the upper 16 bits;
writes without the key are silently ignored. Reads return only the lower
16 bits. Offsets 0x28+ (BOARD_REV, CHIP_REV) are read-only. Offsets
0x30..0x38 (PIN_MUX) are writable WITHOUT the key. [verified 2026-04 JTAG]

| Offset | Name | TCAT-BOOT | Firmware | Notes |
|---|---|---|---|---|
| +0x00 | PLL_CONFIG | `0x8364` | `0x8364` | bit 15 = enable, bits 14:8 = N (post-divider) = 3, bits 7:0 = M (multiplier) = 100. `Fvco = 12 MHz × M / N = 400 MHz`. [verified 2026-05-03 Ghidra `pll_clock_init @ 0x8FAC`] |
| +0x04 | (unknown) | 0 | 0 | always zero [conjecture: unused] |
| +0x08 | CLK_DIV_A | 0 | `0x8022` | bit 15 = enable, bits 6:0 = divisor (=34). [verified Ghidra: `pll_clock_init` writes `0xABCD8022`] |
| +0x0C | **PLL_OUTPUT** | 0 | `0x8001` | bit 15 = output enable, bits 3:0 = source select. **0 = PLL OFF, CPU/AHB falls back to crystal.** ROM writes `0xABCD0000` here on boot-fail (source at 0x20000B24). v2 driver toggles this for `ensure_crystal_clock` / `ensure_pll_clock`. [verified 2026-05-03 Ghidra: `pll_clock_init` writes `0xABCD8001` to enable] |
| +0x10 | (unused) | 0 | 0 | always zero |
| +0x14 | CLK_DIV_SPI | 0 | `0x8000` | bit 15 = enable, bits 6:0 = divisor. eCos value 0x8000 = passthrough (no division applied to PLL output). When 0, falls back to crystal source. [verified Ghidra: `pll_clock_init` writes `0xABCD8000`] |
| +0x18 | CLK_DIV_B | 0 | `0x8024` | divisor 36. [verified Ghidra: `pll_clock_init` writes `0xABCD8024`. Conjecture name; purpose not yet traced.] |
| +0x1C | CLK_DIV_C | 0 | `0x8080` | divisor 128. [verified Ghidra: `pll_clock_init` writes `0xABCD8080`. Conjecture name.] |
| +0x20 | CLK_MUX_A | `0x01FE` | `0x000C` | Source mux. [verified Ghidra: `pll_clock_init` writes `0xABCD01FE` initially] |
| +0x24 | CLK_MUX_B | `0x01FE` | `0x000C` | Mirror of MUX_A. [verified JTAG] |
| +0x28 | BOARD_REV | `0x0001` | `0x0001` | Read-only. Blender = 1. [verified JTAG] |
| +0x2C | CHIP_REV | `0x0002` | `0x0002` | Read-only. **TCD3021 = 2**. [verified JTAG] |
| +0x30 | PIN_MUX_0 | 0 | `0x44444666` | No key needed for writes. [verified] |
| +0x34 | PIN_MUX_SPI | `0x4666` | `0x04444664` | No key needed. [verified] |
| +0x38 | PIN_MUX_2 | 0 | `0x00000004` | No key needed. [verified] |

### Crystal & PLL frequencies

- Crystal: **12.0 MHz** [verified — only value yielding firmware-stored 400 MHz constant]
- PLL VCO: 12 × 100 / 3 = **400 MHz** [verified Ghidra `pll_clock_init` stores `0x17D78400` to global at 0x29AAC]
- **AHB/CPU clock: Fvco / 2 = 200 MHz** [verified 2026-05-03 Ghidra: `pll_clock_init` writes `PLL_FREQ >> 1` to *0x29AA8 — the AHB-freq global the rest of firmware reads] [verified 2026-05-04 empirical via `measure_clocks.py`: PLL/crystal CPU stub-rate ratio = 17.49, derived PLL CPU = 209.9 MHz vs expected 200 MHz, within crystal-tolerance + measurement uncertainty (~5%)]
- Quarter-PLL = 100 MHz, eighth-PLL = 50 MHz (peripheral clock options) [verified Ghidra: writes to *0x29AA4 and *0x29AA0]
- SPI bus at PLL passthrough + SPI_CLK=0x32: 200 / 50 = **4 MHz wire** (stock eCos)
- SPI bus at crystal fallback + SPI_CLK=2: 12 / 2 = **6 MHz wire** (v2 driver — actually FASTER than eCos at PLL)

`Registers.md`'s "400 MHz / 50 = 8 MHz wire" inference was based on
treating Fvco as the SPI parent. Ghidra confirms there is a /2 between
VCO and the AHB/peripheral clock domain. [Registers.md outdated 2026-05-03]

### `Registers.md` mislabels

- `0xC9000008` labeled "CLK_DIV_CPU" — actually CLK_DIV_A
- `0xC900000C` labeled "CLK_DIV_AHB" — actually **PLL_OUTPUT** (bit-15 enable; `pll_clock_init` writes `0xABCD8001` here to turn the PLL on)
- `0xC9000018` labeled "CLK_DIV_LEDSPI" — actually CLK_DIV_B (purpose untraced)
- `0xC900001C` labeled "CLK_DIV_LEDOUT?" — actually CLK_DIV_C (purpose untraced)

[Registers.md clock labels: outdated 2026-05-02 — superseded by Ghidra
decompile of `pll_clock_init @ 0x8FAC` which directly names every register
it writes via the 0xABCD-keyed sequence]

---
## GPIO — banks at 0xCA000000 / 0xCB000000

Two banks of 8 pins each, addressed individually by bit-mask:

```
write/read pin N: *(base + (1 << (N & 7)) * 4)
direction bit N: *(base + 0x400) — bitmap, set = output
```

| Pin | Address | Function | Notes |
|---|---|---|---|
| 0 | `0xCA000004` | **footswitch input** (read individually + as part of bank-0 group) | INPUT [verified 2026-05-04 Ghidra `hal_hardware_init @ 0x89C` + `footswitch_scan_poll @ 0x2CFC`] |
| 1 | `0xCA000008` | (unused) | not configured in `hal_hardware_init` [verified Ghidra] |
| 2 | `0xCA000010` | (unused) | not configured in `hal_hardware_init` [verified Ghidra] |
| 3 | `0xCA000020` | **footswitch input** (bank-0 group bit) | INPUT [verified Ghidra `hal_hardware_init`] |
| 4 | `0xCA000040` | **footswitch input** (bank-0 group bit) | INPUT [verified Ghidra] |
| 5 | `0xCA000080` | **footswitch input** (bank-0 group bit) | INPUT [verified Ghidra] |
| 6 | `0xCA000100` | **BLE module RESET** (active LOW) | OUTPUT, default LOW. Pulses 0→1 around two `0x9B` LED-SPI sync writes [verified Ghidra `ble_spi_reset @ 0x68A4`] |
| 7 | `0xCA000200` | **BLE module mode strap** | OUTPUT, default LOW. Set HIGH at start of `ble_spi_reset`. Function name "power/mode strap" is conjecture — could be BLE-module power-enable, mode select, or both [verified Ghidra direction; conjecture meaning] |
| 8 | `0xCB000004` | **BLE module enable** | OUTPUT, default HIGH. Driven HIGH after `cyg_thread_resume(led_update_thread)` [verified Ghidra] |
| 9 | `0xCB000008` | (unused) | not configured in `hal_hardware_init` [verified Ghidra] |
| 10 | `0xCB000010` | (unused) | not configured in `hal_hardware_init` [verified Ghidra] |
| 11 | `0xCB000020` | **BLE per-tuple CS strobe** | OUTPUT, default LOW [verified Ghidra `ble_spi_write_tuple`] |
| 12 | `0xCB000040` | (purpose untraced) | OUTPUT, default LOW. Cleared at `ble_spi_reset` exit; Ghidra-decoded `hal_hardware_init` configures as output. [conjecture: function not yet decoded] |
| 13 | `0xCB000080` | **rotary encoder A** | INPUT [verified 2026-05-04 Ghidra `encoder_scan_poll @ 0x2F60`] |
| 14 | `0xCB000100` | **rotary encoder B** | INPUT [verified Ghidra `encoder_scan_poll`] |
| 15 | `0xCB000200` | **footswitch input** (bank-1 bit 0x80) | [verified Ghidra `footswitch_scan_poll`] |
| dir bank 0 | `0xCA000400` | u32 bitmap: bit N = output (1) / input (0) for pin N | [verified Ghidra `gpio_set_direction`] |
| dir bank 1 | `0xCB000400` | same for pins 8–15 | [verified Ghidra] |

`PIN_MUX_0/SPI/2` registers at `0xC9000030/34/38` route alternate
functions onto these pins (e.g., SPI MISO/MOSI/SCK). [verified Ghidra
`pll_clock_init` writes; alternate-function map not yet decoded.]

---
## IP fingerprints

| Block | Identification | Method |
|---|---|---|
| DMA @ 0x80000000 | **ARM PrimeCell PL080 rev 1** | CIDs `0xB105F00D` at +0xFF0..+0xFFF, part `0x080`, designer `0x41` (ARM JEP106) [verified 2026-04-23 JTAG] |
| VIC @ 0xFFFFF000 | **ARM PrimeCell PL190-family** (custom part `0x808`) | CIDs `0xB105F00D`, designer ARM. Non-standard part suggests TCAT customization. [verified 2026-04-23 JTAG] |
| USB @ 0x90000000 | **Synopsys DWC2 v3.20a** | GSNPSID register readback [verified 2026-04-29 JTAG] |
| SPI @ 0xCC / 0xCF | **Unidentified** — **likely TCAT-internal** based on source-path strings (`/home/tcat/tch-git/.../dice3-sdk/...` in primary firmware). Speculatively named "Arasan" in earlier docs/comments, but no fingerprint probe confirms this and the firmware contains zero "arasan" strings. Available IDs at `+0x58 = 0x921B5` and `+0x5C = 0x3332322A` (ASCII `*223`) are exposed by the IP but **never read by firmware** [verified 2026-05-03 Ghidra: `get_xrefs_to(0xCC000058) = none`]. Behavior is well-characterized empirically (RX FIFO packer mid-word state, post-AAI word-drop bug) regardless of IP identity. [conjecture: IP vendor; verified IDs from JTAG read-only] |
| BLE module (external MCU) | TC-BLE proprietary | Identified by string `TCH-BLE` at firmware offset `0x23042` and bridged via 3-wire SPI on 0xCF + GPIO on 0xCA/0xCB [verified Ghidra] |

Throughout this documentation, references to "Arasan SPI IP" should be
read as `[conjecture]` until a fingerprint probe confirms otherwise.

---
## SRAM map

512 KB physical at `0x00000–0x7FFFF`. Bit 19 ignored on the address bus
→ `0x80000` aliases `0x00000`. No second bank exists. Firmware uses
`0x00000–0x50000`. [verified 2026-04 JTAG bus scan; firmware-side use
range from Ghidra segments + memory-file `dice3_address_map.md`]

| Range | Size | Use |
|---|---|---|
| 0x00000–0x001FF | 512 B | ARM exception vector table + early stub [verified Ghidra: `firmware_entry @ 0x200` is the first non-vector code] |
| 0x00200–0x2AA83 | ~169 KB | eCos firmware `.text` + `.rodata` body [verified Ghidra segment `ram: 00000200 - 0002aa83`] |
| 0x2AA84–0x325F8 | ~31 KB | `.bss` (zero-init data) [verified memory-file] |
| 0x2A9C–0x2A9CC | — | Clock-frequency globals: `*0x29AAC = 400000000` (PLL VCO), `*0x29AA8 = 200000000` (AHB), `*0x29AA4 = 100000000`, `*0x29AA0 = 50000000` [verified 2026-05-03 Ghidra `pll_clock_init`] |
| 0x325F8–0x4FFFF | ~121 KB | Heap (dlmalloc arena, pool object at `0x2FC2C`) [verified memory-file] |
| 0x50000–0x7FFFF | 192 KB | Unused by firmware (but aliases `0x00000` half via bit-19 ignore) [verified bus scan] |

### XIP window

`firmware/boot-and-init.md` covers this in detail. Brief: the TCAT boot
ROM maps SPI flash `0x10..0x90A` onto CPU addresses `0x4F000..0x4F8FA`
(a fixed ~2.3 KB read-only window). NOT encrypted — bytes match SPI 1:1.
Earlier "XIP encryption" claim in `xip_encryption.md` frontmatter was
wrong; the file's body refutes its own title. [verified 2026-03-27 JTAG
+ memory-file `xip_encryption.md` body]

---

## SST25VF016B flash specs

| Property | Value | Source |
|---|---|---|
| JEDEC ID | `0xBF2541` (manuf=0xBF, type=0x25, capacity=0x41) | [verified 2026-05-03 Ghidra: device-descriptor table at `0x23404`] |
| Capacity | 2 MB (16 Mbit) | [verified Ghidra device table] |
| Sector size | 4 KB (`0x1000`) | [verified Ghidra: device table at `0x233F4` = 0x1000] |
| Block 32K size | 32 KB (`0x8000`) | [verified Ghidra `0x233F8`] |
| Block 64K size | 64 KB (`0x10000`) | [verified Ghidra `0x233FC`] |
| Bank concept | Driver wraps physical chip into one bank with `start_addr..end_addr` range; `sst25xx_addr_in_bank` translates absolute → bank-local | [verified Ghidra `sst25xx_addr_in_bank @ 0x862C`] |

### Command opcodes used by stock driver

| Opcode | Mnemonic | Where used |
|---|---|---|
| `0x03` | READ | bootloader `dma_spi_read_setup @ 0x4F290` |
| `0x0B` | FAST_READ (1 dummy byte) | `sst25xx_read_id @ 0x8734` (named "read_id" in stock; actually fast-read) [verified Ghidra] |
| `0x9F` | RDID (JEDEC) | `sst25xx_init_legacy @ 0x8818` |
| `0x05` | RDSR | (status reads in AAI loop) |
| `0x06` | WREN | (before writes) |
| `0x04` | WRDI | (after AAI sequence) |
| `0xAD` | AAI (auto-address-increment word program) | `sst25xx_aai_write @ 0x8968` |
| `0x02` | BYTE_PROGRAM | (single-byte program) |
| `0x20` | SE (sector erase, 4 KB) | `sst25xx_sector_erase_banked @ 0x8B38` |
| `0x52` | BE32 (block erase, 32 KB) | (per device table entry) |
| `0xD8` | BE64 (block erase, 64 KB) | (per device table entry) |
| `0x60`/`0xC7` | CHIP_ERASE | (not used by stock) |
| `0x50` | EWSR (enable write status reg) | `sst25xx_clear_block_protection @ 0x8660` (REQUIRED before WRSR; standard WREN does NOT enable WRSR on this chip) |
| `0x01` | WRSR (write status register) | (after EWSR for BP-bit clear) |

The driver supports multiple SST25 variants — additional JEDEC IDs in
the device table starting at `0x23404` (`0xBF2541` = VF016B) and
`0x2340C` (`0xBF258E` = likely VF032B / 4 MB). Blender uses VF016B.
[verified 2026-05-03 Ghidra device-descriptor table inspection]


