/*
 * dice3_hw.h — DICE3 peripheral register map, flash driver constants
 *
 * Every value here is annotated with:
 *   - Where it lives in the HW memory map
 *   - How/where we verified it (Ghidra function, probe script, datasheet)
 *
 * Memory-file cross-refs under ~/.claude/projects/.../memory/:
 *   dice3_address_map.md, spi_controller_architecture.md,
 *   spi_pio_dma_constraint.md, working_jtag_tools.md, dice3_timer0.md
 */

#ifndef DICE3_HW_H
#define DICE3_HW_H

#include <stdint.h>

/*
 * Scope: SPI flash + system DMA engine (0xCC…, 0x8000…). USB MIDI bulk OUT/RX
 * uses the USB device controller at 0x90000000 (ChipIdea-like + TCAT), QH banks,
 * and TCAT async prime at +0x834 — not SPI_DMAMD / DMA_TRG_* here. See
 * firmware/patch/dice_usb_regs.h and firmware/usb_dma_ep_ghidra.txt.
 */

/* ──────────────────────────────────────────────────────────────
 * SPI controller IP block  —  two instances on DICE3
 *
 *   0xCC000000  flash controller (connects to SST25VF016B, cs_index=2)
 *   0xCF000000  LED/coprocessor bus (cs_index=4)
 *
 * Register map and bit-field semantics were recovered by decompiling
 * the stock firmware's transaction engine in Ghidra (2026-04-17):
 *   spi_engine_queue_and_arm @ 0xfa10   (was FUN_0000f384 / "spi_dma_transfer_start")
 *   dma_engine_commit_and_wait @ 0xf7b4
 *   dma_engine_acquire_hw @ 0xf72c  (computes ctrl_base + clk_div on first use)
 *   dma_transfer_execute_submit @ 0xf5cc (acquire + transfer + release one-shot)
 *
 * Per-resource constants the firmware holds in its resource_t:
 *   flash @ 0x000237ac   ctrl_base=0x1006, clk_div=0x0880, cs_index=2
 *   led   @ 0x000237f8   ctrl_base=0x100A, clk_div=0x0900, cs_index=4
 * ────────────────────────────────────────────────────────────── */
#define SPI_BASE        0xCC000000u

/* +0x00 CTRL — (direction_bits << 8) | ctrl_base.
 *
 *   direction_bits select the transfer mode for the current descriptor:
 *     0x000  BIDIR       (full-duplex, DMA TX + RX, e.g. flash READ/RDSR)
 *     0x100  TX          (DMA TX only, e.g. byte-program, WREN, erase)
 *     0x200  RX-only     (DMA RX only — uncommon; primes with DATA=0)
 *     0x300  chained BIDIR (used by multi-descriptor reads)
 *
 *   ctrl_base is built by dma_engine_acquire_hw from the resource struct:
 *     | 0x80  if res.byte6 != 0  (16-bit word mode)
 *     | 0x40  if res.byte7 != 0  (MSB-first / polarity)
 *     | 0x07  if res.byte5 == 0  (std 8-bit) else | 0x0F
 *     | 0x1000                   (silicon-level IRQ/descriptor-ring
 *                                 participation bit — see note below)
 *
 *   Stock-fw CTRL values for flash (from spi_resource_flash_cfg_singleton
 *   @ 0x000237d0):
 *     TX    = 0x100 | 0x1006 = 0x1106
 *     BIDIR = 0x000 | 0x1006 = 0x1006
 *     RX    = 0x200 | 0x1006 = 0x1206
 *
 *   Polling-mode driver (what we ship) uses direction_bits | 0x06 instead
 *   — bit 12 is the silicon's "participate in the DMA IRQ / descriptor
 *   ring handshake" flag. Stock fw sets it because it drives the whole
 *   transaction from the IRQ vector 10 completion callbacks
 *   (spi_dma_tx_done_cb @ 0xf618, spi_dma_rx_done_cb @ 0xf63c), which
 *   cyg_cond_signal a flag at engine_ctx+0x88. dma_engine_commit_and_wait
 *   @ 0xf7b4 blocks on cyg_flag_timed_wait until the ISR fires. Our
 *   bare-metal driver polls DMA_ISTAT directly, does not install vector
 *   10, and empirically breaks WREN when bit 12 is set — the silicon
 *   seems to stall the controller waiting for the descriptor-ring
 *   handshake that never comes.
 *
 *   WARNING: Earlier memory notes labelled 0x107 = "TX" and 0x207 = "RX".
 *   That was a partial decompile — the real format is direction | base,
 *   not `0xN07` standalone. */
#define SPI_CTRL_OFF    0x00

/* Direction bits to OR into CTRL (shift position already baked in). */
#define SPI_CTRL_DIR_BIDIR      0x000u
#define SPI_CTRL_DIR_TX         0x100u
#define SPI_CTRL_DIR_RX         0x200u
#define SPI_CTRL_DIR_BIDIR_CH   0x300u

/* Flash ctrl_base variants. Stock fw uses _IRQ (0x1006); our polling
 * driver uses _POLL (0x006) because bit 12 wants the IRQ+descriptor-ring
 * context we don't set up. */
#define SPI_CTRL_BASE_FLASH_IRQ     0x1006u
#define SPI_CTRL_BASE_FLASH_POLL    0x0006u

/* +0x04 LEN — frame length in bytes minus 1 (e.g. 4 ⇒ 5 bytes on the wire). */
#define SPI_LEN_OFF     0x04
/* +0x08 EN — SPI controller enable (1=on). Must be 0 while reconfiguring. */
#define SPI_EN_OFF      0x08

/* +0x10 CS — CS assert (write 1 to pull flash CS# low).
 * Ghidra decompile suggested this is a bitmask of shape `1 << cs_index`
 * (cs_index=2 for flash, 4 for LED), but probe 2026-04-17 confirmed the
 * real flash chip only responds when bit 0 is set. The cs_index field in
 * the firmware's resource_t is probably a register/slot selector inside
 * the controller, not a MOSI CS mask. Keep writing 1. */
#define SPI_CS_OFF      0x10

/* +0x14 CLK — clock divider (flash uses 0x0880 in stock fw; 2 is
 * bootloader default and works for bare-metal TCAT-BOOT context). */
#define SPI_CLK_OFF     0x14
/* +0x18 CLRINT — write 0 to clear pending IRQ edge (done right before
 * kickoff so the completion interrupt is armed fresh). */
#define SPI_CLRINT_OFF  0x18
/* +0x28 STAT — transfer-status flags.
 *   bit 0 BUSY     — transfer in flight. DMA completion drains this.
 *   bit 1 TX_READY — set when TX FIFO can accept another byte.
 *                    v1 pio_tx_byte polls this before each DATA write.
 *   bit 3 RX_READY — set when a received byte is available in DATA.
 *                    Bootloader's led_spi_read_jedec polls this. */
#define SPI_STAT_OFF    0x28
#define SPI_STAT_BUSY     0x01u
#define SPI_STAT_TX_RDY   0x02u
#define SPI_STAT_RX_RDY   0x08u
/* +0x2C DMAGO — stock v1 firmware never writes 1 here (reset only).
 * Our v2 follows the same convention. */
#define SPI_DMAGO_OFF   0x2C
/* +0x34 ERR — error flags. Bootloader `dma_poll_complete` reads this
 * after every DMA RX completion; bit 2 = transfer error. If set, it
 * increments an error counter and surfaces nothing to the caller.
 * We should check this in our AAI pair loop too — a silent DMA error
 * here could explain the tail-drop we see (silicon writes that never
 * actually reach the cell). */
#define SPI_ERR_OFF     0x34
#define SPI_ERR_XFER    0x04u
/* +0x4C DMAMD — DMA mode select.
 *   0 = PIO (writes to SPI_DATA)
 *   1 = DMA RX-only
 *   2 = DMA TX-only (ch0 → SPI_TX_PORT)
 *   3 = DMA bidirectional (ch0=RX, ch1=TX). */
#define SPI_DMAMD_OFF   0x4C
/* +0x50/+0x54 DMACFG0/1 — per-direction DMA framing constants.
 * Stock firmware always writes 4 and 3 respectively (confirmed via
 * every decompiled start method — they're fixed for this IP block). */
#define SPI_DMACFG0_OFF 0x50
#define SPI_DMACFG1_OFF 0x54
/* +0x60 DATA — PIO data register (8-bit r/w). Also written to 0 at
 * kickoff time for RX-only transfers to prime the shift clocks. */
#define SPI_DATA_OFF    0x60
/* +0x70 RX_PORT / +0x80 TX_PORT — AHB slave ports for DMA transfers.
 * Actually *not used* by the stock flash driver — it routes via DMA
 * descriptor rings at engine ctx+0x3c/+0x4c instead. We use these
 * because our bare-metal path goes straight to the raw DMA channels. */
#define SPI_RX_PORT_OFF 0x70
#define SPI_TX_PORT_OFF 0x80

/* Per-register volatile lvalues (flash controller) */
#define SPI_CTRL    (*(volatile uint32_t *)(SPI_BASE + SPI_CTRL_OFF))
#define SPI_LEN     (*(volatile uint32_t *)(SPI_BASE + SPI_LEN_OFF))
#define SPI_EN      (*(volatile uint32_t *)(SPI_BASE + SPI_EN_OFF))
#define SPI_CS      (*(volatile uint32_t *)(SPI_BASE + SPI_CS_OFF))
#define SPI_CLK     (*(volatile uint32_t *)(SPI_BASE + SPI_CLK_OFF))
#define SPI_CLRINT  (*(volatile uint32_t *)(SPI_BASE + SPI_CLRINT_OFF))
#define SPI_STAT    (*(volatile uint32_t *)(SPI_BASE + SPI_STAT_OFF))
#define SPI_DMAGO   (*(volatile uint32_t *)(SPI_BASE + SPI_DMAGO_OFF))
#define SPI_ERR     (*(volatile uint32_t *)(SPI_BASE + SPI_ERR_OFF))
#define SPI_DMAMD   (*(volatile uint32_t *)(SPI_BASE + SPI_DMAMD_OFF))
#define SPI_DMACFG0 (*(volatile uint32_t *)(SPI_BASE + SPI_DMACFG0_OFF))
#define SPI_DMACFG1 (*(volatile uint32_t *)(SPI_BASE + SPI_DMACFG1_OFF))
#define SPI_DATA    (*(volatile uint32_t *)(SPI_BASE + SPI_DATA_OFF))
#define SPI_TX_PORT (SPI_BASE + SPI_TX_PORT_OFF)
#define SPI_RX_PORT (SPI_BASE + SPI_RX_PORT_OFF)

/* ──────────────────────────────────────────────────────────────
 * DMA engine @ 0x80000000
 *
 * Channel 0 is used for flash TX + RX. Channel 1 co-operates with ch0
 * for bidirectional DMA (DMAMD=3). Register layout derived from Ghidra
 * FUN_000091f0 (per-channel arm/trigger) + FUN_0000f384 (transfer start).
 * ────────────────────────────────────────────────────────────── */
#define DMA_BASE        0x80000000u
/* +0x08 EN  — engine enable mask (bit 0 = ch0, bit 1 = ch1). */
#define DMA_EN          (*(volatile uint32_t *)(DMA_BASE + 0x08))
/* +0x10 ICLR — write-1-to-clear per-channel interrupt. */
#define DMA_ICLR        (*(volatile uint32_t *)(DMA_BASE + 0x10))
/* +0x14 ISTAT — interrupt status (bit 0 = ch0 complete). */
#define DMA_ISTAT       (*(volatile uint32_t *)(DMA_BASE + 0x14))

/* Per-channel registers (base = 0x80000100 + ch*0x20):
 *   +0x00 SRC  — source address (RAM for TX, SPI_RX_PORT for RX)
 *   +0x04 DST  — dest address   (SPI_TX_PORT for TX, RAM for RX)
 *   +0x08 NXT  — next descriptor (unused; keep 0)
 *   +0x0C CFG  — low 12 bits = byte count, upper bits = mode flags
 *   +0x10 TRG  — trigger word: 0xD005 = TX arm, 0xD007 = RX arm
 * Channel clears live at +0x30 + ch*4 (write 1 to clear). */
#define DMA_CHREG(ch, off) (*(volatile uint32_t *)(DMA_BASE + 0x100 + (ch)*0x20 + (off)))
#define DMA_CHCLR(ch)      (*(volatile uint32_t *)(DMA_BASE + 0x30 + (ch)*4))

/* CFG high-bits magic: copied verbatim from firmware flash driver (Ghidra).
 *   TX  = (byte_count & 0xFFF) | DMA_CFG_TX  → 0xF4009xxx
 *   RX  = (byte_count & 0xFFF) | DMA_CFG_RX  → 0x88009xxx */
#define DMA_CFG_TX      0xF4009000u
#define DMA_CFG_RX      0x88009000u
#define DMA_TRG_TX      0xD005u
#define DMA_TRG_RX      0xD007u

/* ──────────────────────────────────────────────────────────────
 * Timer0 @ 0xC2000000
 *
 * Stock firmware never enables Timer0 (eCos alarms run off a different
 * clock source). Driver must initialize it — verified 2026-04-17 via
 * diag_timer_state.py probe: writing 0x80 to +0x08 starts the down-counter.
 * See memory/dice3_timer0.md for the full behavior table.
 * ────────────────────────────────────────────────────────────── */
#define TIMER0_BASE     0xC2000000u
#define TIMER_RELOAD    (*(volatile uint32_t *)(TIMER0_BASE + 0x00))
#define TIMER_COUNT     (*(volatile uint32_t *)(TIMER0_BASE + 0x04))
/* +0x08 CTRL:
 *   0x00 = disabled (counter frozen at current value)
 *   0x01 = stopped
 *   0x80 = free-run auto-reload (COUNT decrements; wraps RELOAD → 0)
 *   0x81 = one-shot/latched at 0x10000 */
#define TIMER_CTRL      (*(volatile uint32_t *)(TIMER0_BASE + 0x08))
#define TIMER_CTRL_RUN  0x80u
#define TIMER_RELOAD_DEFAULT 500000u   /* stock fw value; ~28ms @ 18MHz tick */

/* ──────────────────────────────────────────────────────────────
 * Clock / PLL / pin-mux block @ 0xC9000000
 *
 * Heavy block with PLL config, peripheral clock enables, and GPIO
 * alt-function muxes. Full register-level exploration is in
 * memory/dice3_clock_pll.md; here we only expose the handful our
 * flash driver touches.
 *
 * Writes to this block require a 0xABCD prefix in the UPPER 16 bits
 * of the written word (e.g. to set +0x34 to 0x4666, write 0xABCD4666).
 * Without the prefix the write is silently ignored — learned the
 * hard way during the JTAG bus exploration on 2026-04-13.
 * ────────────────────────────────────────────────────────────── */
#define CLOCK_BASE          0xC9000000u
#define CLOCK_WRITE_KEY     0xABCD0000u
#define PIN_MUX_SPI_OFF     0x34
/* Value 0x4666 selects SPI alt-functions on the flash pins. Bootloader
 * writes this at init time; if the block was partially torn down (e.g.
 * by our flash driver teardown hitting 0xC9 registers) we may need
 * to rewrite it before flash ops. */
#define PIN_MUX_SPI_VALUE   0x4666u
#define PIN_MUX_SPI         (*(volatile uint32_t *)(CLOCK_BASE + PIN_MUX_SPI_OFF))

/* ──────────────────────────────────────────────────────────────
 * 0xCB000000 — unlabelled peripheral block ("CB").
 *
 * Bootloader `boot_check_and_load` writes 0xCB000400 = 8 early on,
 * and LED SPI RX path sets something here to 8 during the 4-byte
 * RX-trigger sequence. Not clear what it gates yet — candidates are:
 * peripheral clock enable, SPI-to-GPIO routing, or DMA-to-SPI handshake
 * enable. Likely harmless to leave at 0 for the flash path (hasn't
 * blocked anything observed), but worth probing if flash ops fail
 * from a cold boot state.
 * ────────────────────────────────────────────────────────────── */
#define PERIPH_CB_BASE      0xCB000000u
#define PERIPH_CB_CTRL      (*(volatile uint32_t *)(PERIPH_CB_BASE + 0x000))
#define PERIPH_CB_CTRL_400  (*(volatile uint32_t *)(PERIPH_CB_BASE + 0x400))

/* ──────────────────────────────────────────────────────────────
 * SST25VF016B SPI flash opcodes (datasheet DS20005044C)
 * ────────────────────────────────────────────────────────────── */
#define SST_CMD_WRSR      0x01  /* Write Status Register */
#define SST_CMD_BYTE_PRG  0x02  /* Byte-Program (5B: cmd + 3B addr + 1B data) */
#define SST_CMD_READ      0x03  /* Low-speed read (cmd + 3B addr) */
#define SST_CMD_WRDI      0x04  /* Write Disable */
#define SST_CMD_RDSR      0x05  /* Read Status Register */
#define SST_CMD_WREN      0x06  /* Write Enable */
#define SST_CMD_SE        0x20  /* 4 KB Sector Erase (cmd + 3B addr) */
#define SST_CMD_BE_32K    0x52  /* 32 KB Block Erase (addr 32K-aligned) */
#define SST_CMD_BE_64K    0xD8  /* 64 KB Block Erase (addr 64K-aligned) */
#define SST_CMD_CHIP_ERASE 0xC7 /* Whole-chip erase (no addr) */
#define SST_CMD_EWSR      0x50  /* Enable-Write-Status-Register (NOT WREN) */
#define SST_CMD_EBSY      0x70  /* Enable RY/BY# on SO (AAI) */
#define SST_CMD_AAI       0xAD  /* Auto-Address Increment word-program */

/* Status register bits (SR at RDSR result). */
#define SST_SR_BUSY       0x01
#define SST_SR_WEL        0x02
#define SST_SR_BP_MASK    0x3C  /* BP3:BP0 — must be 0 before program/erase */
#define SST_SR_AAI        0x40  /* AAI in progress */
#define SST_SR_BPL        0x80  /* Block Protection Lock */

#endif /* DICE3_HW_H */
