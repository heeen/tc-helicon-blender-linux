/* DICE3 DMA engine register map — derived from Ghidra xrefs on
 * blender_primary_body.bin and live JTAG probes (2026-04-22).
 *
 * Register accesses observed in stock eCos:
 *   dma_irq_init  @ 0x95b0 writes: +0x08 (EN=0xFF), +0x10 (ICLR=0xFF),
 *                                  +0x30 (value 1)
 *   dma_channel_arm @ 0x9620 writes: +0x08 (EN |= 1<<ch),
 *                                    +0x10 (ICLR = 1<<ch)
 *                                    + per-channel block at
 *                                      ((ch + 0x4000008) * 0x20)
 *   dma_dsr @ 0x951c reads: +0x04 (STATUS) twice; writes: +0x08 (EN=stat)
 *
 * Offsets never touched by stock (but seen live / used by our driver):
 *   +0x00, +0x14, +0x18, +0x1C, +0x20, +0x24, +0x28, +0x2C, +0x34..+0xFC
 *
 * Full register file in firmware/Registers.md, decompile of the above
 * functions in firmware/StockDmaAndSpi.md. */

#ifndef DICE3_DMA_H
#define DICE3_DMA_H

#include <stdint.h>

#define DMA_BASE 0x80000000u

/* Per-channel descriptor block — 8 bytes * 8 channels at +0x100..+0x1FF.
 * The channel's GO register is written LAST (cfg | 0x8001 or similar
 * trigger word) to start the transfer. Stock only uses fields 0..4. */
struct dma_channel {
    volatile uint32_t src;     /* +0x00 — source address (SPI RX_PORT for RX) */
    volatile uint32_t dst;     /* +0x04 — dest address   (SPI TX_PORT for TX) */
    volatile uint32_t next;    /* +0x08 — next descriptor in chain (0=end) */
    volatile uint32_t cfg;     /* +0x0C — low 12b = byte count;
                                *         bit 31 = end-of-chain (SW latches)
                                *         upper bits = mode flags (TX/RX) */
    volatile uint32_t go;      /* +0x10 — trigger (write cfg | 0x8001 at min;
                                *         our driver OR's DMA_TRG_TX/RX) */
    volatile uint32_t _14[3];  /* +0x14..+0x1C — not used by stock */
};

/* Engine-level registers at the DMA base. Offsets past +0x30 are mostly
 * unknown — stock doesn't touch them but hardware readbacks show
 * nonzero values for +0x00, +0x14, +0x20, +0x24. Leave them as raw
 * `_NN` slots so we can probe/annotate individually. */
struct dma_controller {
    volatile uint32_t _00;         /* reads same as status; mirror of +0x04? */
    volatile uint32_t status;      /* +0x04 — STATUS: per-channel "done" bits
                                    *  (stock dma_dsr reads this, iterates
                                    *  bits 0..7 → 8 channels). Not written
                                    *  by stock; self-clears on next arm. */
    volatile uint32_t en;          /* +0x08 — per-channel enable (bitmap).
                                    *  Stock writes 1<<ch per arm, 0xFF at
                                    *  dma_irq_init. */
    volatile uint32_t _0c;
    volatile uint32_t iclr;        /* +0x10 — per-channel IRQ clear (write-1).
                                    *  Stock writes 1<<ch per arm, 0xFF at
                                    *  dma_irq_init. Our teardown writes
                                    *  0xFF (widened from 0x0F, 2026-04-22). */
    volatile uint32_t _14;         /* latched completion history, reads as
                                    *  0x0F after a flash-all run. Stock
                                    *  never touches this. Our dice3_hw.h
                                    *  previously labelled it DMA_ISTAT. */
    volatile uint32_t _18[2];
    volatile uint32_t _20;         /* reads 0x04 when CH2 was used recently. */
    volatile uint32_t _24;         /* reads 0x04 when CH2 was used recently. */
    volatile uint32_t _28[2];
    volatile uint32_t master_en;   /* +0x30 — stock writes 1 at dma_irq_init.
                                    *  Plausibly "engine master go" flag.
                                    *  Our old DMA_CHCLR(0) macro wrote 1
                                    *  here per teardown (harmless). */
    volatile uint32_t _34[51];     /* +0x34..+0xFC — unused by stock. Our
                                    *  old DMA_CHCLR(1..3) wrote 1 to
                                    *  +0x34,+0x38,+0x3C — probably harmless
                                    *  but not meaningful. */
    struct dma_channel chans[8];   /* +0x100..+0x1FF — CH0..CH7. */
};

#define DMA ((struct dma_controller *)DMA_BASE)

/* ── Live channel assignments (probed 2026-04-22) ───────────────────
 *  CH0/CH1  — our v2 driver (flash bidir read)
 *  CH2      — stock eCos flash RX (SRC=SPI_RX_PORT, DST=SRAM)
 *  CH3      — stock eCos flash TX (SRC=SRAM, DST=SPI_TX_PORT)
 *  CH4      — stock eCos LED SPI RX (active during eCos runtime!)
 *  CH5      — stock eCos LED SPI TX (active during eCos runtime!)
 *  CH6,CH7  — idle
 *
 * LED SPI DMA on CH4+5 remains active while our driver runs (teardown
 * kills CH0..3 data regs but leaves CH4..7 alone — zeroing them caused
 * 17 deterministic failures, so eCos expects them to stay configured).
 * LED DMA stealing AHB cycles is the leading hypothesis for the
 * residual +2-byte verify shift we see intermittently. */

/* ── Legacy / bit-level helpers ───────────────────────────────── */

/* Build a channel trigger word: match stock's `cfg | 0x8001` pattern. */
#define DMA_GO_ARM(cfg)   ((cfg) | 0x8001u)

/* Descriptor-cfg flags. Derived from live CH0/CH1 observation after a
 * flash-all run plus stock's spi_engine_queue_and_arm analysis. */
#define DMA_CFG_CNT_MASK  0x00000FFFu    /* low 12 bits = byte count */
#define DMA_CFG_END       0x80000000u    /* set on the final descriptor */
#define DMA_CFG_TX_DIR    0x74000000u    /* upper nibble differs TX vs RX */
#define DMA_CFG_RX_DIR    0x08000000u
#define DMA_CFG_BASE      0x00009000u    /* stock's "base" in 0x70009000 */

/* CFG word for a transfer of `n` bytes.
 *
 * Stock eCos's spi_engine_queue_and_arm writes `0x70009000` into the
 * descriptor's cfg initially, then ORs in 0x04000000 (TX inc) or
 * 0x08000000 (RX inc), then 0x80000000 (end-of-chain) on the last
 * descriptor. That gives 0xF4009xxx for TX, 0xF8009xxx for RX at
 * write-time. But LIVE PROBE of a running eCos showed the post-run
 * channel cfg as 0xF4009xxx for TX (matches) and 0x88009xxx for RX
 * (bits 28/29/30 CLEARED). We tried 0xF8009xxx to match the stock
 * pre-write value — regressed from ~1 miss/20 to 7/20 shift_repro
 * iters. Bits 28/29/30 evidently mark "transfer in progress" or
 * similar runtime flags the hardware manages; they must NOT be set
 * in our RX descriptor cfg. Keep the post-run observed values. */
/* PL080 Control register burst-size fields (§3.4.19):
 *   [17:15] DBSize,  [14:12] SBSize,  Table 3-20:
 *     0b001 = 4 transfers/burst.
 * Stock primary + bootloader both use 001 (4-burst). Tested 0, 8, 16 on
 * 2026-04-23 — all broke deterministically (20/20 fails, not shifts but
 * total data corruption). Arasan SPI IP signals BREQ/SREQ on a fixed
 * 4-byte cadence; burst size is effectively hard-coded at 4 by the
 * peripheral. Keep 4-burst. */
#define DMA_CFG_TX        0xF4009000u    /* bits 31/30/29/28/26 | SBSize=001 | DBSize=001 (4-burst) */
#define DMA_CFG_RX        0x88009000u    /* bits 31/27 | SBSize=001 | DBSize=001 (4-burst) */

/* Per-channel trigger words. Decoded via PL080 TRM (ARM DDI 0196C)
 * Configuration register (§3.4.20):
 *   [15] ITC mask  [14] IE mask  [13:11] FlowCntrl
 *   [9:6] DestPeripheral  [4:1] SrcPeripheral  [0] E
 *
 * RX (0xD007): ITC=1, IE=1, FlowCntrl=010 (P2M), SrcPeripheral=3, E=1.
 *   Matches live eCos CH2 probe (0xD006 idle → 0xD007 armed). Good.
 *
 * TX: was 0xD005 = FlowCntrl=010 (P2M, wrong!) + SrcPeripheral=2 —
 *   told the DMA "peripheral-to-memory from peripheral 2", opposite
 *   of what we want. Control register SI=1/DI=0 pointed the data
 *   flow the right way, but BREQ/SREQ handshaking keyed off the
 *   wrong peripheral line. Leading hypothesis for the residual +2B
 *   read-verify shift.
 * TX (0x8881): ITC=1, FlowCntrl=001 (M2P), DestPeripheral=2, E=1.
 *   Matches live eCos CH3 probe (0x8880 idle → 0x8881 armed). */
#define DMA_TRG_TX        0x8881u
#define DMA_TRG_RX        0xD007u

#endif /* DICE3_DMA_H */
