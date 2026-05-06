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
 * uses the Synopsys DesignWare USB 2.0 OTG (DWC2) v3.20a controller at
 * 0x90000000 — not SPI_DMAMD / DMA_TRG_* here. See firmware/patch/dice_usb_regs.h
 * and firmware/usb-stack.md.
 */

/* ──────────────────────────────────────────────────────────────
 * SPI controller IP block  —  Synopsys DW_apb_ssi v3.22, two instances
 *
 *   0xCC000000  flash controller (IDR=1, drives SST25VF016B + multiplexed BLE)
 *   0xCF000000  LED bus           (IDR=2, drives LED ring)
 *
 * Verified 2026-05-04 via JTAG: VERSION_ID @ +0x5C = 0x3332322A (ASCII
 * "*223" → DW SSI v3.22), IDR @ +0x58 = 1 / 2, no PrimeCell magic at
 * +0xFE0 (correctly indicating non-ARM IP). Register layout matches DW
 * spec at every standard offset; DFS clamping at bits [3:0] verified
 * empirically (write DFS=3, observe 4 bits clocked). See
 * firmware/hardware-reference.md "IP fingerprints" + register layout
 * tables for the DW-canonical name of each register.
 *
 * TCAT extensions on top of stock DW_apb_ssi:
 *   - Dedicated DMA AHB ports at +0x70 (RX) and +0x80 (TX). Stock DW
 *     would route DMA through DR0 @ 0x60.
 *   - CTRLR0[12] (DW: CFS[0], unused under FRF=Motorola) is repurposed
 *     as "DMA-IRQ / descriptor-ring participation" — see SPI_CTRL note
 *     below.
 *   - +0x18 CLRINT, +0x2C DMAGO, +0x34 ERR are TCAT-simplified IRQ
 *     aggregation registers replacing DW's TXFTLR/IMR/RISR set.
 *
 * Stock-fw register accesses recovered via Ghidra decompile (2026-04-17):
 *   spi_engine_queue_and_arm @ 0xfa10
 *   dma_engine_commit_and_wait @ 0xf7b4
 *   dma_engine_acquire_hw @ 0xf72c
 *   dma_transfer_execute_submit @ 0xf5cc
 *
 * RUNTIME observation (2026-05-04 live JTAG, post-power-cycle stock fw):
 *   Flash TX ops: CTRL = 0x1c7 = DFS=7 + SCPH=1 + SCPOL=1 + TMOD=TO →
 *     SPI Mode 3, 8-bit, TX-only. SER = 4 (bit 2). BAUDR = 8.
 *   BLE bus ops: CTRL = 0x007 = DFS=7 + TMOD=TR (Mode 0 bidir).
 *     SER = 8 (bit 3). BAUDR = 0x32.
 *   Earlier static-analysis claim that stock uses 0x1006 with DFS=6
 *   (= 7-bit frames) was a Ghidra misattribution — the static value
 *   0x1006 in firmware is in some other field, not the runtime
 *   CTRL_BASE.
 * ────────────────────────────────────────────────────────────── */
#define SPI_BASE        0xCC000000u

/* +0x00 CTRL = DW CTRLR0. Bit fields per DW spec:
 *
 *   [3:0] DFS   data frame size, N_BITS - 1. Always 7 (8-bit).
 *   [5:4] FRF   frame format. Always 0 (Motorola SPI).
 *   [6]   SCPH  clock phase  (0 = sample on first edge,  1 = second).
 *   [7]   SCPOL clock polarity (0 = clock idles low, 1 = idles high).
 *   [9:8] TMOD  transfer mode: 0=TR (bidir), 1=TO (TX only),
 *               2=RO (RX only), 3=EEPROM_READ.
 *   [12]  CFS[0] (DW spec: Microwire control-frame-size, unused under
 *               FRF=Motorola). TCAT repurposes as IRQ/descriptor-ring
 *               participation bit. Stock fw sets it (`0x1006` static
 *               firmware constant) and drives the transaction from
 *               vector-10 callbacks (spi_dma_tx_done_cb @ 0xf618,
 *               spi_dma_rx_done_cb @ 0xf63c). Polling driver MUST clear
 *               it — empirically breaks WREN when set without an IRQ
 *               consumer attached.
 *
 *   v2 driver values (polling, Mode 0):
 *     0x107 = DFS=7 | TMOD=TO   (TX-only)
 *     0x207 = DFS=7 | TMOD=RO   (RX-only, primes with DATA=0)
 *     0x007 = DFS=7 | TMOD=TR   (bidir)
 *     0x307 = DFS=7 | TMOD=EEPROM_READ (used by stock bootloader's read)
 *
 *   Stock eCos at runtime uses Mode 3 (SCPH+SCPOL set) for flash TX —
 *   v2 driver is in Mode 0 and works, but Mode 3 may give better timing
 *   margin on the AAI hot loop. Switching is a one-line change here
 *   (OR 0xC0 into the value).
 */
#define SPI_CTRL_OFF    0x00

/* Direction bits to OR into CTRL (= DW CTRLR0[9:8] = TMOD). */
#define SPI_CTRL_DIR_BIDIR      0x000u   /* DW TMOD=0 (TR)         */
#define SPI_CTRL_DIR_TX         0x100u   /* DW TMOD=1 (TO)         */
#define SPI_CTRL_DIR_RX         0x200u   /* DW TMOD=2 (RO)         */
#define SPI_CTRL_DIR_BIDIR_CH   0x300u   /* DW TMOD=3 (EEPROM)     */

/* SPI Mode bits — stock uses Mode 3 for flash TX, v2 uses Mode 0. */
#define SPI_CTRL_SCPH           0x040u   /* CTRLR0[6] = sample on 2nd edge */
#define SPI_CTRL_SCPOL          0x080u   /* CTRLR0[7] = idle high */
#define SPI_CTRL_MODE3          (SPI_CTRL_SCPH | SPI_CTRL_SCPOL)

/* Flash CTRL_BASE: DFS=7 (8-bit) + FRF=0 (Motorola). v2 driver uses
 * polling base (no bit-12 vendor IRQ flag). Stock fw stores 0x1006
 * statically but live-probe shows runtime CTRL = direction | 0x07
 * (Mode 0) or direction | 0xC7 (Mode 3) — both with bit 12 = 0. The
 * historical _IRQ variant is kept for reference; do NOT use without
 * also installing vector 10 + the descriptor-ring handshake. */
#define SPI_CTRL_BASE_FLASH_POLL    0x0007u
#define SPI_CTRL_BASE_FLASH_IRQ     0x1007u   /* + bit 12 = TCAT IRQ-ring */

/* +0x04 LEN — frame length in bytes minus 1 (e.g. 4 ⇒ 5 bytes on the wire). */
#define SPI_LEN_OFF     0x04
/* +0x08 EN — SPI controller enable (1=on). Must be 0 while reconfiguring. */
#define SPI_EN_OFF      0x08

/* +0x10 CS = DW SER (slave-enable bitmask). `1 << slave_idx` selects
 * which slave's CS line is asserted while a transaction runs.
 *
 * Live JTAG observation 2026-05-04 of stock eCos:
 *   SER = 4 (bit 2) when stock fw talks to flash (matches Ghidra
 *           cs_index=2 for the flash resource_t).
 *   SER = 8 (bit 3) when stock fw talks to BLE module via the same
 *           0xCC SPI controller (multiplexed bus).
 *
 * v2 driver currently writes SER=1 (bit 0). The April 2026 probe
 * found bit 0 also drives flash — possibly the silicon ANDs all SER
 * bits onto the flash CS line, or there's a hardware mux. Either way,
 * v2's bit-0 choice is empirically functional but is NOT
 * stock-aligned; aligning to bit 2 reduces the unknowns. [verified
 * 2026-04-17 + 2026-05-04 live JTAG] */
#define SPI_CS_OFF      0x10
#define SPI_SER_FLASH   (1u << 2)    /* stock-aligned flash select */
#define SPI_SER_BLE     (1u << 3)    /* stock-aligned BLE select   */

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
 * Full annotated register map in firmware/dice3_dma.h — use the
 * `DMA->...` struct accessors there for new code. Macros below are
 * kept for the v2 flash driver's hot paths and eventually should be
 * migrated. See also firmware/spi-flash.md for the Ghidra
 * decompile that motivated the struct.
 * ────────────────────────────────────────────────────────────── */
#include "dice3_dma.h"

/* +0x08 EN  — per-channel enable bitmap (bit N = CH N). */
#define DMA_EN          (DMA->en)
/* +0x10 ICLR — write-1-to-clear per-channel interrupt. */
#define DMA_ICLR        (DMA->iclr)
/* +0x14 — labelled ISTAT historically; stock never reads it. Reads as
 * a latched "ever-done" history (0x0F after a flash-all). Our driver's
 * wait_dma_irq polls this, and empirically bit N fires when CH N done. */
#define DMA_ISTAT       (DMA->_14)

/* Per-channel registers; prefer DMA->chans[ch].src etc. in new code. */
#define DMA_CHREG(ch, off) (*(volatile uint32_t *)(DMA_BASE + 0x100 + (ch)*0x20 + (off)))
/* +0x30..+0x4C historical "CHCLR" — actually only +0x30 is meaningful
 * to stock (written to 1 once at dma_irq_init). Our writes here are
 * mostly no-ops; see dice3_dma.h for the gory details. */
#define DMA_CHCLR(ch)      (*(volatile uint32_t *)(DMA_BASE + 0x30 + (ch)*4))
/* DMA_CFG_TX/RX and DMA_TRG_TX/RX are now in dice3_dma.h. */

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

/* ──────────────────────────────────────────────────────────────
 * PL190-style VIC @ 0xFFFFF000
 *
 * Bootloader `install_vic_handlers` and the stock firmware's
 * cyg_interrupt_create(n, ...) both treat this as a 32-source PL190:
 *   +0x00 IRQ_STAT, +0x04 FIQ_STAT, +0x08 RAW_STAT
 *   +0x10 INT_EN, +0x14 INT_EN_CLR, +0x30 VECT_ADDRESS
 *   +0x100..+0x17C 16× VECT_ADDR[n], +0x200..+0x27C 16× VECT_CNTL[n]
 *
 * DMA-completion is source 10 (stock flash driver calls
 * cyg_interrupt_create(10, ...) at init time — 0x9594 ISR returns 2
 * and the DSR at 0x951C clears DMA_ISTAT and signals a cyg flag).
 * ────────────────────────────────────────────────────────────── */
#define VIC_BASE            0xFFFFF000u
#define VIC_INT_EN          (*(volatile uint32_t *)(VIC_BASE + 0x10))
#define VIC_INT_EN_CLR      (*(volatile uint32_t *)(VIC_BASE + 0x14))
#define VIC_VECT_ADDRESS    (*(volatile uint32_t *)(VIC_BASE + 0x30))
#define VIC_VECT_ADDR(n)    (*(volatile uint32_t *)(VIC_BASE + 0x100 + (n)*4))
#define VIC_VECT_CNTL(n)    (*(volatile uint32_t *)(VIC_BASE + 0x200 + (n)*4))
#define VIC_CNTL_ENABLE     0x20u
#define VIC_DMA_IRQ         10u

#endif /* DICE3_HW_H */
