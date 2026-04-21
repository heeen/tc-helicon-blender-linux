# DICE3 Peripheral Register Reference — TCAT-BOOT vs eCos

Snapshots captured via JTAG (`firmware/probe_full_reg_dump.py`).

- **TCAT-BOOT** column: device in stage-1 ROM polling loop at
  PC≈0x2000_0C00. `cpsr=0x200000D3` → SVC mode, IRQ+FIQ masked.
  `SCTLR=0x50078` → MMU off, I-cache off, D-cache off.
- **eCos** column: fully-booted patched primary firmware at
  PC≈0x0000_063C (early vector / task-switch area on the sample).
  `cpsr=0x200000D3` → SVC, IRQ+FIQ currently masked (JTAG-halt
  artifact — eCos normally unmasks). `SCTLR=0x51078` → MMU off,
  **I-cache ON**, D-cache off.

Raw snapshots: `firmware/register_state_tcat-boot.md`,
`firmware/register_state_ecos.md`.

---

## Top differences that matter for the v2 flash driver

| Register | TCAT-BOOT | eCos (stock+patched) | Why this matters |
|---|---|---|---|
| `CLK_DIV_CPU`    (0xC9000008) | `0x00000000` | `0x00008022` | **CPU clock source**. TCAT crystal (~12 MHz); eCos PLL-derived. Fixed-iter spin loops in v2 driver scale with this. |
| `CLK_DIV_AHB`    (0xC900000C) | `0x00000000` | `0x00008001` | AHB bus — DMA throughput scales with this. |
| `CLK_DIV_SPI`    (0xC9000014) | `0x00000000` | **`0x00008000`** | **eCos drives flash SPI from 400 MHz PLL**. Our teardown writes `0xABCD0000` → 0, forcing crystal fallback — *this is actually correct and necessary*. Without it, `SPI_CLK=2` would give 200 MHz on the wire. |
| `CLK_DIV_LEDSPI` (0xC9000018) | `0x00000000` | `0x00008024` | LED SPI on PLL (for audio-sync'd refresh). Our teardown does not reset this; irrelevant since we don't use LED SPI for flash ops. |
| `CLK_DIV_LEDOUT?`(0xC900001C) | `0x00000000` | `0x00008080` | Another PLL-derived peripheral clock. |
| `SPI_CLK`        (0xCC000014) | `2`          | **`0x32 (50)`** | **eCos: 400 MHz / 50 = 8 MHz on wire.** Ours: 24 MHz crystal / 2 = 3 MHz on wire. **eCos already runs 2.6× faster than us.** Our 25 MHz target is only 3× eCos, not 8× an imaginary baseline. |
| `SPI_CTRL`       (0xCC000000) | `0x307`      | `0x007`      | TCAT-BOOT has bits 8+9 (DMA auto-chain?); eCos just basic. Our driver uses `0x107`. |
| `SPI_LEN`        (0xCC000004) | `0x10F`      | `0xFFFF`     | eCos leaves max-length armed for streaming read. |
| `SPI_EN`         (0xCC000008) | `0`          | `1`          | eCos keeps SPI enabled at idle. |
| `SPI_DMAMD`      (0xCC00002C) | `0x3F`       | `0`          | TCAT-BOOT leaves all-ones lower bits; eCos clean. Our teardown zeroes it. |
| `DMA_CH2 SRC/DST`(0x80000140/44) | `0/0`     | `0xCC000070 → 0x0002F211` | **eCos uses CH2 for flash RX** (not CH0 like our driver). |
| `DMA_CH3 SRC/DST`(0x80000160/64) | `0/0`     | `0x0002F1F1 → 0xCC000080` | **eCos uses CH3 for flash TX.** Our driver uses CH0/CH1; doesn't conflict, but teardown must clear CH2/3 too (it does). |
| `TIMER0_CTRL`    (0xC2000008) | `0`          | `0xE2`       | eCos: bits 1,5,6,7. Ours: `0x80` (bit 7 only). **Bits 1/5/6 unknown** — possibly prescaler + IRQ enable + mode. Our driver runs Timer0 at different config than eCos. |
| `SCTLR` (CP15 c1)              | `0x50078`    | `0x51078`    | eCos has **bit 12 (I-cache) ON**. Our driver's `_start` invalidates I-cache but doesn't explicitly disable the I bit — if CPU executes one stale line before the invalidate takes effect, we have a subtle race. |
| `VIC_INT_EN`     (0xFFFFF010) | `0`          | `0x10064071` | **eCos actively uses VIC** with 7 IRQs enabled. Teardown clears all — correct. |
| `VIC_IRQ_STAT`   (0xFFFFF000) | `0`          | `0x10004050` | 4 IRQs were active+masked when we halted. |
| `Mixer` (0xC4000000..)        | all zero     | `0x2B01` × 16 | Audio crossbar fully configured by eCos. Irrelevant for flash ops (we don't touch audio output). |
| `USB +0x18` (ChipIdea)        | `0`          | `0x708E3C16` | Frame-index counter — USB is up and running. |

Implications:
1. **Clock: eCos 8 MHz wire** is the baseline our 25 MHz plan needs to
   exceed. Our driver's current 3 MHz is the *slower* path.
2. **Our teardown's CLK_DIV_SPI drop IS required** — without it, running
   `SPI_CLK=2` while inheriting eCos's `CLK_DIV_SPI=0x8000` would be
   200 MHz, way past SST spec.
3. **Timer0 bits 1/5/6** in eCos are mystery — need to decode what
   they do, since our driver may rely on Timer0 for µs timing but set
   up less-optimal flags.
4. **I-cache race in driver entry** — SCTLR bit 12 is on when we
   take over. `_start` invalidates I-cache but doesn't clear bit 12
   first. Fix: clear I/C bits in SCTLR before any branch.

---

## Clock block — 0xC9000000

Clock / PLL / pinmux. Writes need `0xABCD` upper-16 prefix.

| offset | name (guess) | TCAT-BOOT | eCos | interpretation |
|---:|---|---|---|---|
| +0x00 |              | `0x8364` | `0x8364` | **PLL config master** — same in both states. Bit 15 set; low bits carry M/N parameters. |
| +0x04 | `CLK_PLL`?   | `0x0000` | `0x0000` | PLL control/reset (write-1-to-clear?). |
| +0x08 | `CLK_DIV_CPU`| `0x0000` | `0x8022` | bit 15 = PLL select; low bits = divider. |
| +0x0C | `CLK_DIV_AHB`| `0x0000` | `0x8001` | Minimal divider from PLL. |
| +0x10 | `CLK_DIV_APB`| `0x0000` | `0x0000` | APB stays on crystal in both states. |
| +0x14 | `CLK_DIV_SPI`| `0x0000` | **`0x8000`** | **eCos: PLL passthrough → 400 MHz to SPI IP.** |
| +0x18 | `CLK_DIV_LEDSPI` | `0x0000` | `0x8024` | |
| +0x1C | `CLK_DIV_?`  | `0x0000` | `0x8080` | Probably another peripheral clock. |
| +0x20 |              | `0x01FE` | `0x000C` | TCAT: wide enable mask (0x1FE = bits 1..8). eCos: only bits 2+3. |
| +0x24 |              | `0x01FE` | `0x000C` | Mirror of +0x20. |
| +0x28 |              | `0x0001` | `0x0001` | PLL lock or status flag — constant. |
| +0x2C |              | `0x0002` | `0x0002` | Same. |
| +0x30 | `PIN_MUX 1`  | `0x0000` | `0x44444666` | **Different** — eCos configures a second pin-mux bank that TCAT-BOOT leaves zero. |
| +0x34 | `PIN_MUX 0`  | `0x4666` | `0x04444664` | Low nibble: TCAT=`0x6`, eCos=`0x4` — flash SPI pin routing differs. |
| +0x38 |              | `0x0000` | `0x0004` | Small bit field. |
| +0x58 |              | `0x921B5` | `0x921B5` | **Chip-ID / lot code** — invariant. |

---

## Flash SPI controller — 0xCC000000

| offset | name | TCAT-BOOT | eCos | |
|---:|---|---|---|---|
| +0x00 | CTRL     | `0x307`  | `0x007`  | bits 0/1/2 = base enable. TCAT also 8+9 (auto-chain?). |
| +0x04 | LEN      | `0x10F`  | `0xFFFF` | eCos max-length streaming preset. |
| +0x08 | EN       | `0`      | `1`      | eCos keeps SPI enabled. |
| +0x10 | —        | `0x01`   | `0x08`   | Some status / pending flag. |
| +0x14 | CLK      | `2`      | **`0x32`** | **400 MHz / 50 = 8 MHz wire in eCos.** |
| +0x18 | CS       | `0`      | `0`      | |
| +0x28 | STAT     | `0x06`   | `0x06`   | bit1=TX_RDY, bit2=? always-set-at-idle. |
| +0x2C | DMAMD    | `0x3F`   | `0`      | TCAT residue. |
| +0x34 | DMAGO    | `0`      | `1`      | eCos keeps DMAGO armed? (unclear — our driver clears it). |
| +0x4C | DMACFG0  | `0x01`   | `0x03`   | Channel-route config differs. |
| +0x50 | DMACFG1  | `0x00`   | `0x04`   | Ditto. |
| +0x54 | CLRINT   | `0x03`   | `0x03`   | Same latched IRQ flags. |
| +0x58 |          | `0x01`   | `0x01`   | |
| +0x5C |          | `0x3332322A` | `0x3332322A` | **IP version ID — invariant.** (ASCII "*223" big-endian.) |
| +0x60..7C | DATA | `0x84 × 8` | **`0xFF × 8`** | RX FIFO residue: TCAT has stale `0x84`, eCos has `0xFF` (from recent idle reads or post-XIP state). |

---

## LED SPI controller — 0xCF000000

Same register map as flash SPI at a different base.

| offset | name | TCAT-BOOT | eCos | |
|---:|---|---|---|---|
| +0x00 | CTRL  | `0x107` | `0x007` | TCAT sets bit 8 (matches our flash driver); eCos doesn't. |
| +0x04 | LEN   | `0x02`  | `0xFFFF` | eCos streaming like flash. |
| +0x08 | EN    | `0`     | `1`     | |
| +0x14 | CLK   | `0xFE`  | `0x08`  | eCos: 400 MHz / 8 = **50 MHz LED clock**. Very fast. |
| +0x5C |       | `0x3332322A` | same | Same IP version as flash SPI. |
| +0x60..7C | — | all `0` | all `0` | RX FIFO empty in both. |

---

## DMA engine — 0x80000000 (PL080-family)

### Base registers

| offset | name | TCAT-BOOT | eCos | |
|---:|---|---|---|---|
| +0x00 |      | `0x00`   | `0x0C` | eCos: bits 2+3 set (CH2/CH3 active). |
| +0x04 |      | `0x00`   | `0x0C` | Mirror. |
| +0x08 | CFG  | `0x00`   | `0x00` | Master enable — off (no in-flight transfers). |
| +0x14 |      | `0x00`   | `0x0C` | Another CH2/3 status bit. |
| +0x20 |      | `0x00`   | `0x14` | |
| +0x24 |      | `0x00`   | `0x14` | |
| +0x30 |      | `0x01`   | `0x01` | |

### Channel 0 — 0x80000100

eCos leaves this as the stage-2 loader's residue (CH0 = `CC000060` →
SRAM, i.e. DMA-triggered flash reads via the ADDRESS register).

| offset | TCAT-BOOT | eCos | |
|---:|---|---|---|
| +0x00 SRC | `0xCC000060` | `0xCC000060` | same leftover |
| +0x04 DST | `0x0002AB0F` | `0x00034037` | different SRAM ptr |
| +0x0C CNT | `0x88009000` | `0x88009000` | same |
| +0x10 GO  | `0x0000D006` | `0x0000D006` | same — leftover descriptor |

### Channel 1 — 0x80000120: all zero in both states.

### Channel 2 — 0x80000140  **(eCos: flash RX DMA)**

| offset | TCAT-BOOT | eCos | |
|---:|---|---|---|
| +0x00 SRC | `0` | `0xCC000070` | flash RX_PORT |
| +0x04 DST | `0` | `0x0002F211` | SRAM RX buffer |
| +0x0C CNT | `0` | `0xF8009000` | |
| +0x10 GO  | `0` | `0x00009006` | |

### Channel 3 — 0x80000160  **(eCos: flash TX DMA)**

| offset | TCAT-BOOT | eCos | |
|---:|---|---|---|
| +0x00 SRC | `0` | `0x0002F1F1` | SRAM TX buffer |
| +0x04 DST | `0` | `0xCC000080` | flash TX_PORT |
| +0x0C CNT | `0` | `0xF4009000` | |
| +0x10 GO  | `0` | `0x00008880` | |

**Our v2 driver uses CH0 (TX) + CH1 (RX). eCos uses CH2 (RX) + CH3 (TX).**
No channel-collision today (we clear them all), but switching our driver
to CH2/3 might benefit from eCos's existing DMACFG routing.

---

## Timer 0 — 0xC2000000

| offset | TCAT-BOOT | eCos | |
|---:|---|---|---|
| +0x00 RELOAD | `0x000005FA` | `0x0007A120` | TCAT: ~85 µs tick. eCos: 500 000 → matches our `TIMER_RELOAD_V2`. |
| +0x04 COUNT  | `0x00000000` | `0x0003B003` | eCos running. |
| +0x08 CTRL   | `0x00000000` | **`0xE2`** | **eCos: bits 1, 5, 6, 7.** Our driver writes 0x80 (bit 7 only). Bits 1/5/6 = prescaler? IRQ-enable? mode? Needs decode. |
| +0x0C        | `0x0` | `0x0` | |

## Timer 1 — 0xC2000020
| offset | TCAT-BOOT | eCos | |
|---:|---|---|---|
| +0x00 | `0x0000_0000` | `0x0000_0000` | |
| +0x04 | `0xFFFFFFFF`  | `0xD8BBD54C`  | Free-running counter. eCos has it enabled. |
| +0x08 | `0x20`        | `0xA6`        | Control — different. |
| +0x0C | `0x0`         | `0x0`         | |

---

## VIC — 0xFFFFF000

| offset | name | TCAT-BOOT | eCos | interpretation |
|---:|---|---|---|---|
| +0x00 | IRQ_STAT | `0`        | `0x10004050` | 4 IRQs active-and-masked |
| +0x04 | FIQ_STAT | `0`        | `0`          | No FIQs used |
| +0x08 | RAW_INT  | `0x04`     | `0x10404454` | 6 raw interrupt lines |
| +0x10 | INT_EN   | `0`        | **`0x10064071`** | **eCos enables 7 IRQs.** Bits 0, 4, 5, 6, 14, 17, 28. |
| +0x14 | INT_EN_CLR | `0`      | `0`          | |
| +0x100..+0x13C | VECT_ADDR 0..15 | all zero | all zero | **No vectored handlers** — eCos uses polling IRQ dispatch. |
| +0x200..+0x23C | VECT_CNTL 0..15 | all zero | all zero | |

Enabled IRQs (bits in INT_EN = 0x10064071):
- bit 0 (1): ?
- bit 4 (16): ?
- bit 5 (32): ?
- bit 6 (64): ?
- bit 14 (16384): ?
- bit 17 (131072): ?
- bit 28 (268435456): ?

Need to cross-reference with `dice_usb_regs.h` or Ghidra IRQ table to
decode sources. Common PL190-on-SoC sources are timer0/timer1,
UART, SPI0/1, USB, DMA-done, DSP.

---

## Mixer / crossbar — 0xC4000000

TCAT-BOOT: all zero. eCos: `0x2B01` repeated across 16 words (64 bytes).

`0x2B01` = 11009 — likely a routing word. Audio mixer is fully
configured in eCos. Our `mixer_block_quiesce()` (in `reboot_common.c`)
zeroes the first 16 words, so we cleanly disable audio during flash ops.

---

## UART — 0xC5000000

Both states have the UART alive at 115200 baud (divisor 0x301 with
12 MHz crystal). Slight difference in status registers between states
but both ready to TX.

---

## USB — 0x90000000 (ChipIdea) + 0x90000800 (TCAT ext)

Both states have some registers populated. eCos has `+0x18 = 0x708E3C16`
(frame-index counter) and `+0x8_00..+0x8_34` populated with device
config (`+0x28 = 0x17D7`, `+0x2C = 0x5B8` = endpoint configs).

---

## CP15 — cache / MMU

| | TCAT-BOOT | eCos | |
|---|---|---|---|
| ID           | `0x41069265` | `0x41069265` | ARM926EJ-S r0p5 |
| Cache type   | `0x1D0D2112` | `0x1D0D2112` | same silicon |
| **SCTLR**    | `0x50078`    | **`0x51078`** | eCos: **bit 12 (I) set** → I-cache enabled. |
| TTBR, DACR   | `0, 0`       | `0, 0`       | MMU off in both |

**Action for v2 driver**: clear SCTLR.I before any branch in `_start` —
currently we only invalidate the I-cache contents, but if the CPU
prefetched stale lines at the new driver addresses before we loaded
them via JTAG, those prefetch hits could survive the invalidate.

---

## How to regenerate

```bash
python3 firmware/probe_full_reg_dump.py tcat-boot   # device in TCAT-BOOT
python3 firmware/probe_full_reg_dump.py ecos        # device in booted eCos

python3 firmware/probe_ecos_clocks.py               # focused clock probe
python3 firmware/probe_ecos_activity.py             # activity sampler
```

The raw `register_state_{tcat-boot,ecos}.md` files retain the full
word-for-word dumps; this file is the curated comparison + interpretation.
