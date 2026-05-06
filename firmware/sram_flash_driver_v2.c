/*
 * sram_flash_driver_v2.c — v2 bare-metal SRAM flash driver for the
 * TC Helicon Blender (DICE3 SoC, ARM926EJ-S, SST25VF016B flash).
 *
 * Design goals (see ~/.claude/plans/lets-redesign-the-flash-linked-blossom.md):
 *   1. Deterministic peripheral init (USB/DMA/SPI/LED/Timer0/VIC) — no
 *      more "depends on previous firmware state" surprises.
 *   2. Single-writer-per-field mailbox (closes v1 init-race).
 *   3. Clean AAI word-program loop with fixed-delay + escape-to-RDSR.
 *   4. Timer-bounded timeouts everywhere; no PLL-dependent spin counts.
 *   5. Structured log ring (128 × 12 B) with timestamps + phase.
 *
 * Layout (all fixed; host finds these via sram_flash_mailbox_v2.h):
 *   0x28000-0x29FFF  BIDIR_TX_BUF / RX_TEMP (flash read scratch)
 *   0x2B000-0x2BFFF  V2_DATA_BUF (single-sector source/dest, 4 KB)
 *   0x2C000-0x2E3FF  driver code (9 KB after V2_CMD_HASH_RANGE bump)
 *   0x2E400-0x2E57F  V2_MBOX (mailbox)
 *   0x2E580-0x2E5FF  V2_TIMINGS
 *   0x2E600-0x2EBFF  V2_LOG_RING (128 × 12 B)
 *   0x2F100-0x2F8FF  V2_SECTOR_LIST
 *   0x2F900-0x7FFFF  V2_IMAGE_BASE
 *
 * Build:
 *   arm-none-eabi-gcc -march=armv5te -marm -Os -nostdlib -ffreestanding \
 *     -T firmware/sram_flash_driver_v2.ld \
 *     -o firmware/sram_flash_driver_v2.elf firmware/sram_flash_driver_v2.c
 *   arm-none-eabi-objcopy -O binary \
 *     firmware/sram_flash_driver_v2.elf firmware/sram_flash_driver_v2.bin
 */

#include <stdint.h>

#include "dice3_hw.h"
#include "blender_periph_lib.h"
#include "sram_flash_mailbox_v2.h"

/* ── Lvalue accessors for the fixed SRAM addresses ──────────── */
#define MBOX      ((struct v2_mailbox *)V2_MBOX_ADDR)
#define TIM       ((struct v2_timings *)V2_TIMINGS_ADDR)

static void init_timings_if_needed(void) {
    if (TIM->magic == V2_TIMINGS_MAGIC) return;
    TIM->aai_pair_fixed_us         = V2_TIM_DEFAULT_AAI_PAIR_FIXED_US;
    TIM->aai_pair_poll_interval_us = V2_TIM_DEFAULT_AAI_PAIR_POLL_INTERVAL_US;
    TIM->aai_pair_poll_budget_us   = V2_TIM_DEFAULT_AAI_PAIR_POLL_BUDGET_US;
    TIM->pre_wrdi_fixed_us         = V2_TIM_DEFAULT_PRE_WRDI_FIXED_US;
    TIM->pre_wrdi_poll_interval_us = V2_TIM_DEFAULT_PRE_WRDI_POLL_INTERVAL_US;
    TIM->pre_wrdi_poll_budget_us   = V2_TIM_DEFAULT_PRE_WRDI_POLL_BUDGET_US;
    TIM->post_wrdi_fixed_us        = V2_TIM_DEFAULT_POST_WRDI_FIXED_US;
    TIM->post_wrdi_poll_interval_us= V2_TIM_DEFAULT_POST_WRDI_POLL_INTERVAL_US;
    TIM->post_wrdi_poll_budget_us  = V2_TIM_DEFAULT_POST_WRDI_POLL_BUDGET_US;
    TIM->erase_fixed_us            = V2_TIM_DEFAULT_ERASE_FIXED_US;
    TIM->erase_poll_interval_us    = V2_TIM_DEFAULT_ERASE_POLL_INTERVAL_US;
    TIM->erase_poll_budget_us      = V2_TIM_DEFAULT_ERASE_POLL_BUDGET_US;
    TIM->xport_mode                = V2_TIM_DEFAULT_XPORT_MODE;
    TIM->byte_prog_offset          = V2_TIM_DEFAULT_BYTE_PROG_OFFSET;
    TIM->verify_quiesce_mode       = V2_TIM_DEFAULT_VERIFY_QUIESCE_MODE;
    TIM->log_silent                = V2_TIM_DEFAULT_LOG_SILENT;
    TIM->read_mode                 = V2_TIM_DEFAULT_READ_MODE;
    TIM->rx_sample_dly             = V2_TIM_DEFAULT_RX_SAMPLE_DLY;
    TIM->spi_mode_bits             = V2_TIM_DEFAULT_SPI_MODE_BITS;
    TIM->flash_ser                 = V2_TIM_DEFAULT_FLASH_SER;
    TIM->aai_sync_mode             = V2_TIM_DEFAULT_AAI_SYNC_MODE;
    TIM->magic                     = V2_TIMINGS_MAGIC;
}
#define LOG_RING  ((struct v2_log_entry *)V2_LOG_RING_ADDR)
#define DATA_BUF  ((uint8_t *)V2_DATA_BUF_ADDR)

/* TX_SCRATCH / RX_SCRATCH live ABOVE the driver-managed region (which
 * runs 0x2C000..0x2E3FF). They were originally at 0x2E100/0x2E120, but
 * the driver's .bss section grew over them when log_silent was added —
 * silently zeroing TX_SCRATCH[0..3] right before each dma_rx populated
 * its cmd byte. Symptom: JEDEC ID returned 0xffffff (cmd 0x00 sent
 * instead of 0x9F), legacy ID still worked because BIDIR_TX_BUF lives
 * at 0x28000 and is unaffected. Park the scratches well below
 * BIDIR_TX_BUF in unused SRAM — the bus scan in dice3_address_map.md
 * confirms 0x00000-0x2AFFF is free of fixtures. */
#define TX_SCRATCH  ((volatile uint8_t *)0x00029F00u)
#define RX_SCRATCH  ((volatile uint8_t *)0x00029F40u)
#define TX_SCRATCH_MAX  16

typedef struct {
    volatile uint32_t src;
    volatile uint32_t dst;
    volatile uint32_t next;
    volatile uint32_t cfg;
} dma_desc_t;

/* Chained descriptor scratch (inside 0x2E100-0x2E17F misc area). */
#define RD_TX_DESC1 ((volatile dma_desc_t *)0x0002E150u)

/* Bidir-read scratch (below driver code, doesn't collide with anything). */
#define BIDIR_TX_BUF  ((volatile uint8_t *)0x00028000u)
#define RX_TEMP       ((uint8_t *)0x0002A000u)

/* ── ARM barriers ───────────────────────────────────────────── */
static inline void dwb(void) {
    __asm__ volatile("mcr p15, 0, %0, c7, c10, 4" :: "r"(0) : "memory");
}

/* ── Timer0 helpers ─────────────────────────────────────────── *
 * Free-running down-counter; reload 500000, ~18 MHz tick.
 * now_us() is a soft wrap-aware microsecond counter. */
#define TIMER_RELOAD_V2  500000u
#define TIMER_TICKS_PER_US 18u   /* empirical, documented in memory */

/* SPI clock config.
 *   CLK_DIV_SPI = 0x0000 → crystal fallback (~12 MHz to SPI IP, halved
 *                          internally to ~6 MHz)
 *   SPI_CLK     = 2      → /2 → ~3 MHz on the wire
 *
 * Attempted 2026-04-21: adopt eCos's (0x8000, 0x32) = 400 MHz PLL / 50
 * = 8 MHz. AAI pairs immediately failed (AAI_PAIR_TX timeouts on the
 * very first pair after AAI_FIRST) — the SPI IP likely can't latch at
 * 400 MHz input even though eCos's idle snapshot shows this config. To
 * go faster safely we'd need to keep the SPI IP clocked by something
 * closer to its rated input — probe further. See hardware-reference.md and
 * firmware/flash_stats.csv. */
#define DRV_CLK_DIV_SPI_RAW  0x0000u
#define DRV_SPI_CLK_DIV      2u

static uint32_t s_us_accum;
static uint32_t s_last_raw;

static void timer_enable(void) {
    TIMER_CTRL = 0;
    TIMER_RELOAD = TIMER_RELOAD_V2;
    TIMER_CTRL = TIMER_CTRL_RUN;
    dwb();
}

static void time_reset(void) {
    s_us_accum = 0;
    s_last_raw = TIMER_COUNT;
}

static uint32_t now_us(void) {
    uint32_t r = TIMER_COUNT;
    uint32_t delta;
    if (r <= s_last_raw) {
        delta = s_last_raw - r;
    } else {
        delta = s_last_raw + (TIMER_RELOAD_V2 - r);
    }
    s_last_raw = r;
    s_us_accum += delta / TIMER_TICKS_PER_US;
    return s_us_accum;
}

static void busy_wait_us(uint32_t us) {
    uint32_t deadline = now_us() + us;
    while (now_us() < deadline) {}
}

/* ── Log ring ──────────────────────────────────────────────── */
static void log_put(uint8_t evt, uint16_t detail, uint32_t spi_addr) {
    if (TIM->log_silent) return;
    uint32_t idx = MBOX->log_head & V2_LOG_RING_MASK;
    struct v2_log_entry *e = &LOG_RING[idx];
    e->t_us     = now_us();
    e->evt      = evt;
    e->phase    = (uint8_t)MBOX->phase;
    e->detail   = detail;
    e->spi_addr = spi_addr;
    dwb();
    MBOX->log_head = (MBOX->log_head + 1u) & 0xFFFFu;
}

/* ── Mailbox helpers ───────────────────────────────────────── */
static inline void set_phase(uint32_t phase) {
    MBOX->phase = phase;
    MBOX->seq++;
    dwb();
}

static void set_error(uint16_t err_code, uint32_t spi_addr, uint8_t last_sr) {
    MBOX->err_code     = err_code;
    MBOX->err_spi_addr = spi_addr;
    MBOX->last_sr      = last_sr;
    MBOX->status       = V2_STATUS_ERR;
    set_phase(V2_PHASE_ERROR);
    log_put(V2_EVT_ABORT, err_code, spi_addr);
}

/* ── Bare-metal IRQ infrastructure for DMA completion ────────
 *
 * Stock firmware drives the DMA engine via VIC vector 10 + an eCos
 * flag; our polling driver was skipping that handshake entirely.
 * This block lets a real IRQ released by the completion interrupt
 * pace each DMA transfer — see plan file
 * ~/.claude/plans/lets-redesign-the-flash-linked-blossom.md for the
 * hypothesis behind the tail-drop / off-by-4 symptoms.
 *
 * The driver owns the ARM while it's JTAG-loaded: we install our own
 * trampoline at the IRQ vector (0x18 → 0x38), seed SP_irq, configure
 * the VIC for source 10 only, and let dma_irq_handler set a flag that
 * wait_dma_irq() blocks on with wfi. */

static volatile uint32_t dma_done_flag;

/* IRQ-mode stack. The ISR saves r0-r12+lr (56 B) and calls no
 * subroutines — 256 B has ample headroom. */
static uint32_t irq_stack[64] __attribute__((aligned(8)));

/* Original 32-bit words we clobber at 0x18 and 0x38; saved on first
 * install so we can restore on driver teardown if ever needed. */
static uint32_t saved_vec_18;
static uint32_t saved_vec_38;

static inline void enable_irq_cpsr(void) {
    __asm__ volatile(
        "mrs r0, cpsr\n"
        "bic r0, r0, #0x80\n"
        "msr cpsr_c, r0\n"
        : : : "r0", "memory");
}

static inline void disable_irq_cpsr(void) {
    __asm__ volatile(
        "mrs r0, cpsr\n"
        "orr r0, r0, #0x80\n"
        "msr cpsr_c, r0\n"
        : : : "r0", "memory");
}

/* ARMv5TE wait-for-interrupt — CP15 c7,c0,4 drains the pipeline and
 * stops the core until the next IRQ/FIQ. */
static inline void arm_wfi(void) {
    __asm__ volatile("mcr p15, 0, r0, c7, c0, 4" : : : "r0", "memory");
}

__attribute__((interrupt("IRQ")))
static void dma_irq_handler(void) {
    /* PL190 handshake: the VIC only latches "in-service" priority on
     * a *read* of VECT_ADDRESS. Without this read, the matching write
     * at end-of-ISR is a no-op and the VIC keeps re-asserting IRQ. */
    (void)VIC_VECT_ADDRESS;
    /* Stop the DMA engine, then clear every interrupt-source register.
     * Needed because DMA_ICLR=3 alone doesn't sink DMA_ISTAT (channels
     * keep re-asserting on their internal completion latches) — the
     * dma_abort / post-transfer cleanup path uses the exact same trio
     * of writes: EN=0, ICLR=3, CHCLR(0)=1, CHCLR(1)=1. */
    DMA_EN = 0;
    DMA_ICLR = 0xF;
    DMA_CHCLR(0) = 1;
    DMA_CHCLR(1) = 1;
    /* SPI has its own IRQ edge at +0x18; clear it so a latched edge
     * from the completed transfer doesn't re-trigger us. */
    SPI_CLRINT = 0;
    dma_done_flag = 1;
    dwb();
    /* Any write to VECT_ADDRESS releases the priority latch. */
    VIC_VECT_ADDRESS = 0;
}

/* Rewrite the IRQ trampoline at 0x18 + handler pointer at 0x38. This
 * is idempotent: do_verify's readback scratch sits at 0x0-0xFFF and
 * clobbers these bytes, so handle_command calls this at the top of
 * every dispatch to restore them. */
#pragma GCC diagnostic push
#pragma GCC diagnostic ignored "-Warray-bounds"
static void install_vectors(void) {
    volatile uint32_t *v18 = (volatile uint32_t *)0x00000018u;
    volatile uint32_t *v38 = (volatile uint32_t *)0x00000038u;
    static int first = 1;
    if (first) {
        saved_vec_18 = *v18;
        saved_vec_38 = *v38;
        first = 0;
    }
    /* 0xE59FF018 = `ldr pc, [pc, #24]`. At execution time PC=vec+8, so
     * [pc, #24] points at 0x18 + 8 + 24 = 0x38. */
    *v18 = 0xE59FF018u;
    *v38 = (uint32_t)(uintptr_t)dma_irq_handler;
    dwb();
    /* Invalidate the I-cache in case stale bytes sat in the decoded
     * instruction stream. */
    __asm__ volatile("mcr p15, 0, %0, c7, c5, 0" :: "r"(0) : "memory");
}
#pragma GCC diagnostic pop

__attribute__((unused))
static void install_dma_irq(void) {
    /* Seed SP_irq: briefly hop into IRQ mode with interrupts masked. */
    uint32_t irq_sp_top = (uint32_t)(uintptr_t)&irq_stack[64];
    __asm__ volatile(
        "mrs r1, cpsr\n"
        "msr cpsr_c, #0xD2\n"    /* IRQ mode, I+F masked */
        "mov sp, %0\n"
        "msr cpsr_c, r1\n"
        : : "r"(irq_sp_top) : "r1", "memory");

    install_vectors();

    /* VIC: clear all pending, enable only DMA (source 10) on slot 0. */
    VIC_INT_EN_CLR = 0xFFFFFFFFu;
    VIC_VECT_ADDR(0) = (uint32_t)(uintptr_t)dma_irq_handler;
    VIC_VECT_CNTL(0) = VIC_CNTL_ENABLE | VIC_DMA_IRQ;
    VIC_INT_EN = (1u << VIC_DMA_IRQ);
    dwb();
}

/* Wait for DMA completion on the channels encoded in `ch_mask` (one
 * bit per channel, bit N = CH N done). Prefers the IRQ-released flag
 * but polls DMA_ISTAT as a fallback — BIDIR (dual-channel) transfers
 * don't fire our vector reliably (hypothesis: source-10 SPI completion
 * needs additional handshake we don't provide). The polling fallback
 * keeps the rest of the refactor testable while we sort out IRQ
 * plumbing. Pass 0x1 for CH0-only transfers (dma_tx/dma_rx),
 * 0x4 for CH2 (bidir RX), etc. */
static int wait_dma_irq_mask(uint32_t budget_us, uint32_t ch_mask) {
    uint32_t t0 = now_us();
    enable_irq_cpsr();
    while (!dma_done_flag) {
        if (DMA_ISTAT & ch_mask) break;  /* polled completion */
        if (now_us() - t0 > budget_us) {
            disable_irq_cpsr();
            return -1;
        }
    }
    disable_irq_cpsr();
    return 0;
}

static int wait_dma_irq(uint32_t budget_us) {
    return wait_dma_irq_mask(budget_us, 1u);
}

/* ── SPI + DMA low-level ────────────────────────────────────
 * Values match v1's proven config (CTRL=0x107, DMAMD=2 for TX,
 * DMAMD=3 for bidir, DMACFG0=4, DMACFG1=3). Deliberately avoid the
 * ctrl_base bit 12 flag — tests 2026-04-17 showed it breaks WREN
 * in polling mode. */

static void spi_reset_clean(void) {
    SPI_EN = 0;
    SPI_CS = 0;
    SPI_DMAGO = 0;
    SPI_DMAMD = 0;
    SPI_CTRL = 0x107;
    SPI_LEN = 0;
    SPI_DATA = 0;
    SPI_CLK = DRV_SPI_CLK_DIV;
    SPI_CLRINT = 0;
    SPI_DMACFG0 = 0;
    SPI_DMACFG1 = 0;
    /* DW SSI RX_SAMPLE_DLY (+0xF0). Tunable knob — defaults to 0 (no
     * delay), set non-zero to relax RX setup timing for AAI tail-drop. */
    *(volatile uint32_t *)(SPI_BASE + 0xF0) = TIM->rx_sample_dly;
    DMA_EN = 0;
    DMA_ICLR = 0xF;
    DMA_CHCLR(0) = 1;
    DMA_CHCLR(1) = 1;
    DMA_CHREG(0, 0x0C) = 0;
    DMA_CHREG(0, 0x10) = 0;
    DMA_CHREG(1, 0x0C) = 0;
    DMA_CHREG(1, 0x10) = 0;
    dwb();
    for (volatile int i = 0; i < 1000; i++)
        if (!(SPI_STAT & SPI_STAT_BUSY)) break;
}

static void dma_abort(void) {
    SPI_DMAGO = 0;
    DMA_EN = 0;
    DMA_ICLR = 0xF;
    DMA_CHCLR(0) = 1;
    DMA_CHCLR(1) = 1;
    SPI_CS = 0;
    SPI_EN = 0;
    dwb();
}

/* PL080 TRM §3.2.5 "Disabling a DMA channel without losing data in
 * the FIFO": set Halt (bit 18), poll Active (bit 17) until 0, then
 * clear Enable. This forces the channel to finish draining its
 * internal 4-word FIFO before shutting down, instead of losing
 * whatever bytes were stuck there. Used between bidir transactions
 * to prevent stale TX bytes from clocking out at the start of the
 * next transfer (root cause of the +2B verify shift — see
 * 2026-04-24 diagnostic work in task #32).  */
static void dma_channel_drain(uint32_t ch, uint32_t budget_us) {
    volatile uint32_t *cfg = &DMA_CHREG(ch, 0x10);
    *cfg |= (1u << 18);                /* Halt: ignore further DMA requests */
    dwb();
    uint32_t t0 = now_us();
    while (*cfg & (1u << 17)) {        /* poll Active until FIFO empty */
        if (now_us() - t0 > budget_us) break;
    }
    *cfg = 0;                          /* clear Enable (Active=0 → no data loss) */
    dwb();
}

/* DMA TX — CS assert + N-byte burst + CS deassert. Timer-bounded. */
static int dma_tx(const volatile uint8_t *buf, uint32_t len, uint32_t budget_us) {
    dwb();
    while (SPI_STAT & SPI_STAT_BUSY) {}
    SPI_EN = 0;
    SPI_CS = 0;
    SPI_DMAGO = 0;
    /* CTRL bit 12 (0x1000) enables the silicon's IRQ/descriptor-ring
     * handshake. Safe to set now that dma_irq_handler is installed. */
    SPI_CTRL = 0x107;
    SPI_LEN = len - 1;
    SPI_DMAMD = 2;
    SPI_CLK = DRV_SPI_CLK_DIV;
    SPI_DMACFG0 = 4;
    SPI_DMACFG1 = 3;
    dwb();
    SPI_EN = 1;
    dwb();

    DMA_EN = 1;
    DMA_ICLR = 1;
    DMA_CHCLR(0) = 1;
    dwb();
    DMA_CHREG(0, 0x10) = 0;
    DMA_CHREG(0, 0x00) = (uint32_t)(uintptr_t)buf;
    DMA_CHREG(0, 0x04) = SPI_TX_PORT;
    DMA_CHREG(0, 0x08) = 0;
    DMA_CHREG(0, 0x0C) = len | DMA_CFG_TX;
    dwb();
    dma_done_flag = 0;
    dwb();
    DMA_CHREG(0, 0x10) = DMA_TRG_TX;
    dwb();

    SPI_CS = 1;
    SPI_CLRINT = 0;
    dwb();

    if (wait_dma_irq(budget_us) != 0) { dma_abort(); return -1; }
    /* BUSY drain is short; leave as-is (polled). */
    uint32_t t0 = now_us();
    for (volatile int j = 0; j < 200; j++)
        if (SPI_STAT & SPI_STAT_BUSY) break;
    while (SPI_STAT & SPI_STAT_BUSY) {
        if (now_us() - t0 > budget_us) { dma_abort(); return -1; }
    }
    SPI_CS = 0;
    SPI_EN = 0;
    DMA_EN = 0;
    DMA_ICLR = 0xF;
    return 0;
}

/* Bidirectional DMA RX for small commands (RDSR etc). Cmd byte is
 * clocked on TX channel 1; response captured on RX channel 0. */
static int dma_rx(uint32_t spi_cmd, uint8_t *out, uint32_t len,
                  uint32_t budget_us)
{
    uint32_t total = 1 + len;
    if (total > TX_SCRATCH_MAX || len == 0)
        return -1;

    TX_SCRATCH[0] = (uint8_t)spi_cmd;
    for (uint32_t i = 1; i < total; i++) TX_SCRATCH[i] = 0;
    dwb();

    while (SPI_STAT & SPI_STAT_BUSY) {}
    SPI_EN = 0;
    SPI_CS = 0;
    SPI_DMAGO = 0;
    SPI_CTRL = 0x007;
    SPI_LEN = total - 1;
    SPI_DMAMD = 3;
    SPI_CLK = DRV_SPI_CLK_DIV;
    SPI_DMACFG0 = 4;
    SPI_DMACFG1 = 3;
    dwb();
    SPI_EN = 1;
    dwb();

    DMA_EN = 3;
    DMA_ICLR = 0xF;
    DMA_CHCLR(0) = 1;
    DMA_CHCLR(1) = 1;
    dwb();

    DMA_CHREG(0, 0x10) = 0;
    DMA_CHREG(0, 0x00) = SPI_RX_PORT;
    DMA_CHREG(0, 0x04) = (uint32_t)(uintptr_t)RX_SCRATCH;
    DMA_CHREG(0, 0x08) = 0;
    DMA_CHREG(0, 0x0C) = (total & 0xFFF) | DMA_CFG_RX;
    dwb();
    dma_done_flag = 0;
    dwb();
    DMA_CHREG(0, 0x10) = DMA_TRG_RX;

    DMA_CHREG(1, 0x10) = 0;
    DMA_CHREG(1, 0x00) = (uint32_t)(uintptr_t)TX_SCRATCH;
    DMA_CHREG(1, 0x04) = SPI_TX_PORT;
    DMA_CHREG(1, 0x08) = 0;
    /* Exact byte count — historically 0xFFF which clocked ~500 µs of
     * stale TX_SCRATCH garbage per RDSR. Logic-analyzer 2026-04-20. */
    DMA_CHREG(1, 0x0C) = total | DMA_CFG_TX;
    dwb();
    DMA_CHREG(1, 0x10) = DMA_TRG_TX;
    dwb();

    SPI_CS = 1;
    SPI_CLRINT = 0;
    dwb();

    if (wait_dma_irq(budget_us) != 0) { dma_abort(); return -1; }
    uint32_t t0 = now_us();
    while (SPI_STAT & SPI_STAT_BUSY) {
        if (now_us() - t0 > budget_us) { dma_abort(); return -1; }
    }
    SPI_CS = 0;
    SPI_EN = 0;
    DMA_EN = 0;
    DMA_ICLR = 0xF;
    for (uint32_t j = 0; j < len; j++)
        out[j] = ((volatile uint8_t *)RX_SCRATCH)[1 + j];
    return 0;
}

static uint8_t rdsr(void) {
    uint8_t sr;
    if (dma_rx(SST_CMD_RDSR, &sr, 1, 5000) != 0)
        return 0xFF;
    MBOX->last_sr = sr;
    return sr;
}

static int spi_cmd1(uint8_t cmd) {
    TX_SCRATCH[0] = cmd;
    return dma_tx(TX_SCRATCH, 1, 5000);
}

static int spi_cmd2(uint8_t c1, uint8_t c2) {
    TX_SCRATCH[0] = c1; TX_SCRATCH[1] = c2;
    return dma_tx(TX_SCRATCH, 2, 5000);
}

/* ── Peripheral teardown ──────────────────────────────────── */

#define REBOOT_FLASH_SPI      ((volatile uint32_t *)0xCC000000u)
#define REBOOT_LED_SPI        ((volatile uint32_t *)0xCF000000u)
#define REBOOT_LED_GPIO       (*(volatile uint32_t *)0xCB000020)

/* Bundled teardown — safe in both TCAT-BOOT and eCos states. Order
 * matters: USB first (has DMA clients), then SPIs, then DMA engine,
 * then VIC (so no IRQs fire while we write the rest), then timer +
 * mixer. Mirror of reboot_common.c:peripheral_full_teardown(); kept
 * in-tree so v2 driver still builds as a single .c for bench iter.
 *
 * Touches ONE field in the 0xC9 clock block: CLK_DIV_SPI (+0x14). We
 * force it to DRV_CLK_DIV_SPI_RAW (currently 0 — crystal fallback).
 * The eCos value 0x8000 (400 MHz PLL passthrough) was attempted
 * 2026-04-21 with SPI_CLK=0x32 for 8 MHz wire but immediately broke
 * AAI pair TX — see the DRV_* comment near the top. All 0xC9 writes
 * need the 0xABCD upper-16 key. */
static void peripheral_full_teardown(void) {
    /* Reboot-like teardown baseline shared with soft-reboot path. */
    *(volatile uint32_t *)0xC9000014u = 0xABCD0000u | DRV_CLK_DIV_SPI_RAW;
    blender_periph_dwb();

    blender_usb_compact_handoff_reset();

    blender_spi_ip_block_quiesce(REBOOT_LED_SPI);
    blender_spi_ip_drain_rx(REBOOT_LED_SPI);
    REBOOT_LED_GPIO = 0;
    REBOOT_LED_SPI[0x14 / 4] = 0xFF;

    blender_spi_ip_block_quiesce(REBOOT_FLASH_SPI);
    blender_spi_ip_drain_rx(REBOOT_FLASH_SPI);

    /* Keep CH4..7 intact in v2 path: historical bench data shows wiping
     * those channels can desync runtime LED-SPI state and destabilize
     * subsequent ops. Reboot-style in ordering, but CH0..3 only here. */
    blender_dma_engine_reset((volatile uint32_t *)0x80000000u, 4u);
    blender_vic_clear(0u);
    blender_timer_blocks_disable();
    blender_mixer_block_quiesce();
    blender_periph_dwb();
}

static void driver_setup(void) {
    /* Timer0 must be running before log_put() is called (it timestamps). */
    timer_enable();
    time_reset();
    set_phase(V2_PHASE_INIT);
    log_put(V2_EVT_SETUP_BEGIN, 0, 0);

    __asm__ volatile("mov r0, #0xD3 \n msr cpsr_c, r0 \n" ::: "r0", "memory");

    set_phase(V2_PHASE_TEARDOWN);
    /* Full hardware quiesce — USB, both SPIs, DMA (ch0-3), VIC, timers,
     * mixer. Lets us flash reliably from both TCAT-BOOT and eCos
     * contexts (eCos needed timer+mixer+extended-VIC to stop; observed
     * 60% reliability without them, 2026-04-20). */
    peripheral_full_teardown();

    /* Re-enable Timer0 — teardown turned it off (eCos was driving it).
     * busy_wait_us / now_us depend on it; observed 2026-04-21: skipping
     * this hangs the first busy_wait in BP_CLEAR because TIMER_COUNT
     * stays frozen and now_us() deadline never reaches. */
    timer_enable();
    time_reset();

    /* IRQ install disabled after observing ISR-loop stuck-state right
     * after a fresh TCAT-BOOT power-up (2026-04-20). Polling fallback
     * in wait_dma_irq handles all paths correctly. */
    /* install_dma_irq(); */

    /* PIN_MUX left untouched. ROM gpio_configure (0x4F090) disassembly
     * shows pinmux registers at 0xC9000030-0x3C are 8 nibbles × 4 bits
     * each — one nibble per pin — written via direct RMW (no write key).
     * A blanket `PIN_MUX_SPI = 0xABCD4666` would clobber 7 unrelated
     * pins with invalid mux selectors. Whatever code loaded us already
     * set the muxes correctly; don't touch. */

    spi_reset_clean();
    /* If a previous run left flash in AAI mode, WRDI exits it. Safe
     * to send unconditionally. */
    (void)spi_cmd1(SST_CMD_WRDI);

    set_phase(V2_PHASE_T0_ENABLE);
    log_put(V2_EVT_T0_ENABLED, 0, 0);

    log_put(V2_EVT_SETUP_DONE, 0, 0);
}

/* ── Flash operations ─────────────────────────────────────── */

/* Wait a fixed interval (long enough to cover the typical operation
 * time) then poll RDSR at poll_interval_us until BUSY clears or
 * max_extra_us is reached. Avoids the expensive dma_rx overhead during
 * the normal-case erase/program wait. */
static int wait_fixed_then_poll(uint32_t fixed_us,
                                uint32_t poll_interval_us,
                                uint32_t max_extra_us,
                                uint8_t *out_sr)
{
    busy_wait_us(fixed_us);
    uint32_t t_extra = 0;
    for (;;) {
        uint8_t sr = rdsr();
        if (sr != 0xFF) {
            MBOX->last_sr = sr;
            if (!(sr & SST_SR_BUSY)) {
                if (out_sr) *out_sr = sr;
                return 0;
            }
        }
        if (t_extra >= max_extra_us) {
            if (out_sr) *out_sr = sr;
            return -1;
        }
        busy_wait_us(poll_interval_us);
        t_extra += poll_interval_us;
    }
}

static int aai_pair_wait(uint32_t pair_idx) {
    /* Fixed prologue (always) + optional RDSR poll (only if budget>0).
     * RDSR between AAI pairs drops the chip out of AAI on this part —
     * confirmed 2026-04-18 via aai_pair_poll_budget_us>0 timing test.
     * Host can still opt in when debugging tunings. */
    uint32_t fixed    = TIM->aai_pair_fixed_us;
    uint32_t interval = TIM->aai_pair_poll_interval_us;
    uint32_t budget   = TIM->aai_pair_poll_budget_us;
    if (fixed) busy_wait_us(fixed);
    if (budget == 0) {
        if ((pair_idx & 0x3F) == 0)
            log_put(V2_EVT_AAI_BUSY_FAST, (uint16_t)pair_idx, 0);
        return 0;
    }
    uint8_t sr = rdsr();
    if (sr != 0xFF && !(sr & SST_SR_BUSY)) {
        if ((pair_idx & 0x3F) == 0)
            log_put(V2_EVT_AAI_BUSY_FAST, (uint16_t)pair_idx, sr);
        return 0;
    }
    log_put(V2_EVT_AAI_BUSY_POLL, (uint16_t)pair_idx, sr);
    for (uint32_t t = 0; t < budget; t += interval) {
        busy_wait_us(interval);
        sr = rdsr();
        if (sr != 0xFF && !(sr & SST_SR_BUSY))
            return 0;
    }
    log_put(V2_EVT_TIMEOUT, (uint16_t)pair_idx, sr);
    return -1;
}

static int do_bp_clear(void) {
    set_phase(V2_PHASE_BP_CLEAR);
    log_put(V2_EVT_BP_CLEAR_PRE, 0, 0);
    spi_reset_clean();
    if (spi_cmd1(SST_CMD_EWSR) != 0) { set_error(V2_ERR_BP_CLEAR_WREN, 0, 0); return -1; }
    if (spi_cmd2(SST_CMD_WRSR, 0x00) != 0) { set_error(V2_ERR_BP_CLEAR_WRSR, 0, 0); return -1; }
    uint8_t sr = 0xFF;
    /* Datasheet tWRSR max 10 ms. Wait 12 ms fixed, poll 5 ms every
     * 20 ms extra. */
    if (wait_fixed_then_poll(12000, 5000, 20000, &sr) != 0) {
        set_error(V2_ERR_BP_CLEAR_STUCK, 0, sr);
        return -1;
    }
    if (sr & SST_SR_BP_MASK) {
        /* Retry with WREN (some SST parts want WREN, not EWSR). */
        (void)spi_cmd1(SST_CMD_WREN);
        (void)spi_cmd2(SST_CMD_WRSR, 0x00);
        (void)wait_fixed_then_poll(12000, 5000, 20000, &sr);
    }
    MBOX->last_sr = sr;
    log_put(V2_EVT_BP_CLEAR_POST, 0, 0);
    return (sr & SST_SR_BP_MASK) ? -1 : 0;
}

static int do_erase_any(uint32_t spi_addr, uint8_t opcode) {
    set_phase(V2_PHASE_ERASE_WREN);
    spi_reset_clean();
    /* Give the chip a moment to settle after whatever came before
     * (often a VERIFY bidir-DMA read). Without this, back-to-back
     * BE_64K ops wedged at pair_idx >=16128 on 2026-04-18 — chip
     * started returning 0xFF to RDSR for 400+ms (observed as
     * V2_ERR_ERASE_TIMEOUT at err_spi_addr=0x70000 after 0x60000
     * programmed cleanly). */
    busy_wait_us(500);
    if (spi_cmd1(SST_CMD_WREN) != 0) {
        set_error(V2_ERR_ERASE_WREN, spi_addr, 0);
        return -1;
    }
    log_put(V2_EVT_ERASE_WREN, opcode, spi_addr);

    set_phase(V2_PHASE_ERASE_CMD);
    TX_SCRATCH[0] = opcode;
    TX_SCRATCH[1] = (spi_addr >> 16) & 0xFF;
    TX_SCRATCH[2] = (spi_addr >> 8) & 0xFF;
    TX_SCRATCH[3] = spi_addr & 0xFF;
    if (dma_tx(TX_SCRATCH, 4, 5000) != 0) {
        set_error(V2_ERR_ERASE_CMD, spi_addr, 0);
        return -1;
    }
    log_put(V2_EVT_ERASE_CMD, opcode, spi_addr);

    set_phase(V2_PHASE_ERASE_POLL);
    uint8_t sr = 0xFF;
    /* Erase wait (tunable via TIM->erase_*). */
    busy_wait_us(TIM->erase_fixed_us);
    if (wait_fixed_then_poll(0, TIM->erase_poll_interval_us,
                             TIM->erase_poll_budget_us, &sr) != 0) {
        set_error(V2_ERR_ERASE_TIMEOUT, spi_addr, sr);
        return -1;
    }
    MBOX->last_sr = sr;
    log_put(V2_EVT_ERASE_DONE, opcode, spi_addr);
    return 0;
}

static int do_erase(uint32_t spi_addr) {
    /* SE (4 KB) requires flash_addr aligned to 4 KB; low 12 bits are
     * ignored by hardware but we want loud failures, not silent
     * truncation. */
    if (spi_addr & 0xFFFu) {
        set_error(V2_ERR_BAD_ADDR, spi_addr, 0);
        return -1;
    }
    return do_erase_any(spi_addr, SST_CMD_SE);
}

static int do_erase_block_32k(uint32_t spi_addr) {
    if (spi_addr & 0x7FFFu) {
        set_error(V2_ERR_BAD_ADDR, spi_addr, 0);
        return -1;
    }
    return do_erase_any(spi_addr, SST_CMD_BE_32K);
}

static int do_erase_block_64k(uint32_t spi_addr) {
    if (spi_addr & 0xFFFFu) {
        set_error(V2_ERR_BAD_ADDR, spi_addr, 0);
        return -1;
    }
    return do_erase_any(spi_addr, SST_CMD_BE_64K);
}

/* SST25VF016B AAI silicon appears to corrupt the last ~3 bytes when a
 * single AAI run exceeds 32 KB (observed 2026-04-18: 4 KB, 32 KB clean;
 * 64 KB reliably wrong at offsets 0xFFFD/0xFFFE/0xFFFF with values that
 * look like the target AND'd with some stale mask). Cap each AAI burst
 * at 32 KB; larger spans are split into multiple back-to-back runs
 * (each with its own WREN + first-pair + WRDI). */
/* Long AAI runs (≥32 KB) silently drop the last 2 pairs on this part
 * (observed 2026-04-18). Short AAI runs commit cleanly. Compromise:
 * do_aai_program issues the bulk as one big run minus the final 4 bytes,
 * then a separate tiny 4-byte AAI run for those last 4 bytes — short
 * runs don't trigger the quirk. Total overhead per big run: one extra
 * WREN + AAI_FIRST + 2 pairs + WRDI ≈ ~100 µs. */
/* 2026-05-02: capping AAI_MAX_RUN to 0x400 (512 pairs) was tested
 * but does NOT help the post-AAI +4B verify-read shift — the bug
 * accumulates across separate AAI runs (each with their own
 * WREN/AAI_FIRST/WRDI), not just within a single continuous run.
 * Host-level chunking (test G in spi-flash.md §10) and
 * driver-internal chunking both fail the same way. Restoring 32 KB
 * cap; the bug is mitigated by do_verify's 4× retry, not here. */
#define AAI_MAX_RUN 0x8000u
#define AAI_TAIL_SPLIT 4u

static int do_aai_program_run(uint32_t spi_addr, const uint8_t *data, uint32_t len);

/* Byte-program a single byte at spi_addr. Used to patch the 4-byte tail
 * of each AAI run, which AAI silently drops on this silicon. */
static int do_byte_program(uint32_t spi_addr, uint8_t byte) {
    log_put(V2_EVT_AAI_WREN, byte, spi_addr);
    /* Empirical: BYTE_PRG via our dma_tx path consistently lands 4 bytes
     * earlier than the passed address. Workaround: add TIM->byte_prog_offset
     * (default 4) on the wire. Tunable by host so we can probe the raw
     * landing offset by setting it to 0. */
    uint32_t wire_addr = spi_addr + TIM->byte_prog_offset;
    spi_reset_clean();
    if (spi_cmd1(SST_CMD_WREN) != 0) {
        log_put(V2_EVT_BAD_STATE, 0x01, spi_addr);
        return -1;
    }
    busy_wait_us(5);
    TX_SCRATCH[0] = SST_CMD_BYTE_PRG;
    TX_SCRATCH[1] = (wire_addr >> 16) & 0xFF;
    TX_SCRATCH[2] = (wire_addr >> 8) & 0xFF;
    TX_SCRATCH[3] = wire_addr & 0xFF;
    TX_SCRATCH[4] = byte;
    if (dma_tx(TX_SCRATCH, 5, 5000) != 0) {
        log_put(V2_EVT_BAD_STATE, 0x02, spi_addr);
        return -1;
    }
    log_put(V2_EVT_AAI_PAIR, byte, spi_addr);
    busy_wait_us(50);
    uint8_t sr;
    if (wait_fixed_then_poll(0, 20, 5000, &sr) != 0) {
        log_put(V2_EVT_TIMEOUT, sr, spi_addr);
        return -1;
    }
    log_put(V2_EVT_AAI_WRDI, sr, spi_addr);
    return 0;
}

/* ── AAI transport vtable (diagnostic: DMA vs PIO) ───────────
 *
 * The AAI hot pair loop is abstracted behind this vtable so we can swap
 * between DMA (current path) and PIO (byte-by-byte DATA register writes)
 * at runtime without recompiling. Helps us isolate whether the observed
 * tail drops originate in the SPI DMA engine or in the flash silicon.
 * Short commands (WREN, WRDI, erase, byte-program) still go through the
 * existing DMA helpers — they aren't in the drop signal path.
 * ─────────────────────────────────────────────────────────────── */
typedef struct {
    /* Begin an AAI run. Sends AAI_FIRST (6 bytes: 0xAD + 3-byte addr
     * + d0 + d1) writing the first pair of real data bytes at spi_addr.
     * After return, AAI internal counter is at spi_addr+2 and subsequent
     * aai_pair() calls continue from there. Leaves the controller
     * configured and ready for aai_pair(). */
    int  (*aai_begin)(uint32_t spi_addr, uint8_t d0, uint8_t d1);
    /* Send one AAI subsequent pair (3 bytes: 0xAD + d0 + d1) as a
     * single CS cycle. `pair_idx` is the 0-based pair index for logs. */
    int  (*aai_pair)(uint8_t d0, uint8_t d1, uint32_t pair_idx,
                     uint32_t spi_addr_for_log);
    /* Tear down after the last pair: CS=0, EN=0, DMA idle. */
    void (*aai_end)(void);
} spi_xport_t;

static const spi_xport_t xport_dma;
static const spi_xport_t *XPORT = &xport_dma;

static void aai_hot_loop_setup(void);

/* When non-zero, the xport_dma path runs the AAI hot loop in TMOD=TO
 * (TX-only) instead of TMOD=TR (bidir). Set by do_main from
 * TIM->xport_mode == V2_XPORT_DMA_TX. Single-byte to keep the size
 * delta minimal — every byte counts against the 9 KB SRAM budget. */
static uint8_t s_xport_tx_only;

static int do_aai_program(uint32_t spi_addr, const uint8_t *data, uint32_t len) {
    if (len < 2 || (len & 1)) {
        set_error(V2_ERR_BAD_LENGTH, spi_addr, 0);
        return -1;
    }
    if (spi_addr & 1u) {
        set_error(V2_ERR_BAD_ADDR, spi_addr, 0);
        return -1;
    }
    uint32_t off = 0;
    while (off < len) {
        uint32_t run = len - off;
        if (run > AAI_MAX_RUN) run = AAI_MAX_RUN;
        if (do_aai_program_run(spi_addr + off, data + off, run) != 0)
            return -1;
        off += run;
    }
    return 0;
}

/* ─── xport_dma: current DMA-based AAI backend ──────────────── */

static int xport_dma_aai_begin(uint32_t spi_addr, uint8_t d0, uint8_t d1) {
    /* Send AAI_FIRST (6 bytes: cmd + 3 addr + d0 + d1) writing the first
     * pair of REAL data. The historical 2-dummy-FF prefix was misplaced
     * — it wasn't working around a silicon "first-2-pairs-drop" quirk,
     * it was compensating for a READ_RX_SKIP miscount that shifted the
     * readback by -4 bytes. With the readback fixed (2026-04-20), those
     * dummy pairs simply wasted the first 4 bytes of the target sector
     * and added ~60 µs per AAI run. */
    set_phase(V2_PHASE_AAI_FIRST);
    TX_SCRATCH[0] = SST_CMD_AAI;
    TX_SCRATCH[1] = (spi_addr >> 16) & 0xFF;
    TX_SCRATCH[2] = (spi_addr >> 8) & 0xFF;
    TX_SCRATCH[3] = spi_addr & 0xFF;
    TX_SCRATCH[4] = d0;
    TX_SCRATCH[5] = d1;
    if (dma_tx(TX_SCRATCH, 6, 5000) != 0) return -1;
    log_put(V2_EVT_AAI_FIRST, 0, spi_addr);
    if (aai_pair_wait(0) != 0) return -1;

    aai_hot_loop_setup();
    return 0;
}

/* Configure SPI controller + DMA channels for the AAI hot-loop. Called
 * once per AAI run by xport_dma_aai_begin, AND again after each per-pair
 * RDSR poll if TIM->aai_sync_mode == STOCK (the rdsr() call tears down
 * the SPI EN state, requiring a re-setup before the next shift_once).
 *
 * tx_only=1 → DW SSI TMOD=TO + DMACR=TDMAE; tx_only=0 → bidir (TMOD=TR
 * + DMACR=TDMAE|RDMAE). spi_mode_bits OR'd in for SCPH+SCPOL Mode 3. */

static void aai_hot_loop_setup(void) {
    SPI_EN = 0;
    SPI_CS = 0;
    SPI_DMAGO = 0;
    SPI_CTRL = (s_xport_tx_only ? 0x107 : 0x007) | TIM->spi_mode_bits;
    SPI_LEN = 2;
    SPI_DMAMD = s_xport_tx_only ? 2 : 3;
    SPI_CLK = DRV_SPI_CLK_DIV;
    SPI_DMACFG0 = 4;
    SPI_DMACFG1 = 3;
    dwb();
    SPI_EN = 1;
    dwb();

    DMA_EN = s_xport_tx_only ? 2 : 3;
    dwb();
    if (!s_xport_tx_only) {
        DMA_CHREG(0, 0x04) = (uint32_t)(uintptr_t)RX_SCRATCH;
        DMA_CHREG(0, 0x08) = 0;
    }
    DMA_CHREG(1, 0x04) = SPI_TX_PORT;
    DMA_CHREG(1, 0x08) = 0;
    dwb();

    TX_SCRATCH[0] = SST_CMD_AAI;
    dwb();
}

/* Core pair transmission: one CS cycle shifting [0xAD, d0, d1] via bidir
 * DMA. Caller sets TX_SCRATCH[1..2] before calling. Returns the SPI_ERR
 * value latched right after the DMA/SPI busy drain; 0 == clean.
 * Returns -1 on spin timeout. */
static int xport_dma_aai_shift_once(uint32_t *out_err) {
    DMA_ICLR = 0xF;
    if (!s_xport_tx_only) DMA_CHCLR(0) = 1;
    DMA_CHCLR(1) = 1;

    if (!s_xport_tx_only) {
        DMA_CHREG(0, 0x10) = 0;
        DMA_CHREG(0, 0x00) = SPI_RX_PORT;
        DMA_CHREG(0, 0x04) = (uint32_t)(uintptr_t)RX_SCRATCH;
        DMA_CHREG(0, 0x08) = 0;
        DMA_CHREG(0, 0x0C) = 3u | DMA_CFG_RX;
    }
    DMA_CHREG(1, 0x10) = 0;
    DMA_CHREG(1, 0x00) = (uint32_t)(uintptr_t)TX_SCRATCH;
    DMA_CHREG(1, 0x04) = SPI_TX_PORT;
    DMA_CHREG(1, 0x08) = 0;
    DMA_CHREG(1, 0x0C) = 3u | DMA_CFG_TX;
    dwb();

    dma_done_flag = 0;
    dwb();
    if (!s_xport_tx_only) DMA_CHREG(0, 0x10) = DMA_TRG_RX;
    DMA_CHREG(1, 0x10) = DMA_TRG_TX;
    dwb();

    /* DW SSI SER (slave-enable bitmask). TIM->flash_ser = 1 (current
     * v2 default) or 4 (stock-aligned bit 2). Only this hot-path CS
     * assertion is parameterized — other paths still use SPI_CS=1. */
    SPI_CS = TIM->flash_ser;
    SPI_CLRINT = 0;
    dwb();

    /* Bidir waits on CH0 (RX completes after TX in the SPI core's
     * model); TX-only waits on CH1. */
    uint32_t mask = s_xport_tx_only ? 0x2u : 0x1u;
    if (wait_dma_irq_mask(5000, mask) != 0) { dma_abort(); return -1; }
    uint32_t t0 = now_us();
    while (SPI_STAT & SPI_STAT_BUSY) {
        if (now_us() - t0 > 5000) { dma_abort(); return -1; }
    }
    /* Sample SPI_ERR (= DW SSI RISR) BEFORE dropping CS — the bit may
     * be cleared by the next CS assert edge. Bit 2 = RXUI on stock DW
     * SSI; on TX-only it shouldn't fire (RX disabled). Bit 1 = TXOI
     * (TX overrun) is the canonical underrun-after-CS-deassert symptom. */
    *out_err = SPI_ERR;
    SPI_CS = 0;
    dwb();
    return 0;
}

static int xport_dma_aai_pair(uint8_t d0, uint8_t d1, uint32_t pair_idx,
                              uint32_t spi_addr_for_log) {
    TX_SCRATCH[1] = d0;
    TX_SCRATCH[2] = d1;
    dwb();

    /* Single shot — no retry. Bootloader's dma_poll_complete @ 0x4F120
     * (Ghidra 2026-04-20) treats SPI_ERR bit 2 as a DIAGNOSTIC counter,
     * not a retry trigger: it bumps an error count and returns SUCCESS
     * regardless. The retry loop this replaces (commit d68debd) was
     * firing on *every* pair because bit 2 always sets on this IP,
     * causing each pair to commit 4 times to flash → the 4× pair
     * duplication observed in readback. Keep the counter bump for
     * visibility; don't retry. */
    uint32_t err = 0;
    if (xport_dma_aai_shift_once(&err) != 0) return -1;
    if (err & SPI_ERR_XFER) {
        MBOX->pair_retries++;
    }

    if (TIM->aai_sync_mode == V2_AAI_SYNC_STOCK) {
        /* Stock-aligned readiness sync (added 2026-05-04). aai_pair_wait
         * does the fixed delay + RDSR poll until WIP clears. RDSR (via
         * dma_rx) tears down the AAI hot-loop SPI EN state, so we must
         * re-arm before the next shift_once. ~30-50 µs/pair total vs
         * ~10 µs in FAST mode — the cost of catching Tbp_max drift. */
        if (aai_pair_wait(pair_idx) != 0) return -1;
        aai_hot_loop_setup();
    } else {
        busy_wait_us(TIM->aai_pair_fixed_us);
    }
    (void)spi_addr_for_log;
    return 0;
}

static void xport_dma_aai_end(void) {
    SPI_CS = 0;
    SPI_EN = 0;
    DMA_EN = 0;
    DMA_ICLR = 0xF;
    dwb();
    /* NOTE: we do NOT drain the RX FIFO here — tried it 2026-04-22 and
     * it regresses flash-all to 17 deterministic failures. Reading
     * SPI_DATA while SPI_EN=0 apparently has a side effect (clocks
     * another byte?) that corrupts state. Whatever half-word alignment
     * the FIFO packer is in after AAI is handled elsewhere or reset
     * by the next CS-deassert edge. */
}

static const spi_xport_t xport_dma = {
    .aai_begin = xport_dma_aai_begin,
    .aai_pair  = xport_dma_aai_pair,
    .aai_end   = xport_dma_aai_end,
};

/* xport_dma_tx is not a separate vtable — it shares xport_dma's
 * functions and toggles a static flag (s_xport_tx_only) read inside
 * xport_dma_aai_begin / xport_dma_aai_shift_once. The flag is set in
 * do_main when latching XPORT from TIM->xport_mode. */

/* PIO xport (V2_XPORT_PIO=1) was tried 2026-04-18 and removed 2026-05-04
 * to free SRAM budget for the stock-aligned RDSR sync. PIO TX on the
 * flash SPI controller (0xCC) does not clock bytes reliably on this
 * silicon — leaves last_sr=0xFF and writes nothing. v1's pio_cmd1 uses
 * dma_tx internally despite the misleading name. Use V2_XPORT_DMA or
 * V2_XPORT_DMA_TX. */

/* ─── do_aai_program_run: xport-dispatched ──────────────────── */

static int do_aai_program_run(uint32_t spi_addr, const uint8_t *data, uint32_t len) {

    set_phase(V2_PHASE_AAI_WREN);
    spi_reset_clean();
    if (spi_cmd1(SST_CMD_WREN) != 0) {
        set_error(V2_ERR_AAI_WREN, spi_addr, 0);
        return -1;
    }
    busy_wait_us(10);
    log_put(V2_EVT_AAI_WREN, 0, spi_addr);

    /* AAI_FIRST carries the first pair of real data (data[0], data[1]).
     * Previously this was dummy FFs followed by another dummy-FF pair,
     * a workaround for what turned out to be a readback bug (2026-04-20
     * logic-analyzer capture — see READ_RX_SKIP comment). Removing them
     * saves 4 wasted flash bytes per sector and one extra tAAI cycle. */
    if (XPORT->aai_begin(spi_addr, data[0], data[1]) != 0) {
        (void)spi_cmd1(SST_CMD_WRDI);
        set_error(V2_ERR_AAI_FIRST_TX, spi_addr, 0);
        return -1;
    }

    set_phase(V2_PHASE_AAI_PAIR);

    /* Continue from pair 1 (data[2..3]). Tail-pad with 0xFF was there
     * for a "last 2 pairs drop" theory that was also a readback bug;
     * drop it too. */
    for (uint32_t i = 2; i < len; i += 2) {
        uint8_t d0 = data[i];
        uint8_t d1 = data[i + 1];
        if (XPORT->aai_pair(d0, d1, i >> 1, spi_addr + i) != 0) {
            XPORT->aai_end();
            (void)spi_cmd1(SST_CMD_WRDI);
            set_error(V2_ERR_AAI_PAIR_TX, spi_addr + i, 0);
            return -1;
        }
        if ((i & 0x1FF) == 0) {
            MBOX->phase_detail = i >> 1;
            MBOX->seq++;
            log_put(V2_EVT_AAI_PAIR, (uint16_t)(i >> 1), spi_addr + i);
        }
    }

    XPORT->aai_end();

    /* Pre-WRDI settle (tunable via TIM->pre_wrdi_*). */
    {
        uint8_t sr;
        (void)wait_fixed_then_poll(TIM->pre_wrdi_fixed_us,
                                   TIM->pre_wrdi_poll_interval_us,
                                   TIM->pre_wrdi_poll_budget_us, &sr);
    }

    set_phase(V2_PHASE_AAI_WRDI);
    (void)spi_cmd1(SST_CMD_WRDI);
    /* Post-WRDI settle (tunable via TIM->post_wrdi_*). */
    {
        uint8_t sr;
        (void)wait_fixed_then_poll(TIM->post_wrdi_fixed_us,
                                   TIM->post_wrdi_poll_interval_us,
                                   TIM->post_wrdi_poll_budget_us, &sr);
    }
    log_put(V2_EVT_AAI_WRDI, 0, spi_addr);
    return 0;
}

/* Bidirectional DMA flash read. */
/* READ_RX_SKIP = 4: the first 4 MISO bytes during a READ transaction align
 * with the 4-byte cmd+addr phase (flash MISO is undefined during these, so
 * we discard them). Was 8 for historical reasons — that miscount shifted
 * readback data by -4 bytes and masqueraded as a "BYTE_PROGRAM off-by-4"
 * silicon quirk. Proof from logic-analyzer capture 2026-04-20:
 * BYTE_PRG wire payload is exactly `02 addrH addrM addrL data` (5 bytes),
 * so the write itself is on-target. */
#define READ_RX_SKIP 4u
#define READ_CHUNK   0x800u

static int dma_bidir_read(uint32_t spi_addr, uint8_t *out, uint32_t len,
                          uint32_t budget_us)
{
    /* 2026-05-02: post-AAI FIFO word-drop workaround.
     * One dummy 8-byte JEDEC bidir read (single CS cycle) before the
     * real read absorbs whatever stuck FIFO state AAI leaves behind.
     * Threshold (empirical): ≥ 5 bytes total in the dummy CS cycle
     * eliminates the +4B verify shift; below that it's only partial
     * mitigation. 8 bytes gives safety margin. 4 + 8 = 12 µs overhead
     * per chunk at 6 MHz wire — negligible vs the chunk's ~25 ms.
     *
     * Why JEDEC: it's a known-clean SPI cmd that returns valid data
     * regardless of chip state, and it goes through the same dma_rx
     * bidir path. The dummy_buf return value is discarded. */
    {
        uint8_t dummy_buf[7];
        (void)dma_rx(0x9F, dummy_buf, 7, 5000);
    }
    uint32_t xfer_len = READ_RX_SKIP + len;
    if (len == 0 || xfer_len > 0xFFF) return -1;

    BIDIR_TX_BUF[0] = SST_CMD_READ;
    BIDIR_TX_BUF[1] = (spi_addr >> 16) & 0xFF;
    BIDIR_TX_BUF[2] = (spi_addr >> 8) & 0xFF;
    BIDIR_TX_BUF[3] = spi_addr & 0xFF;
    for (uint32_t i = 4; i < xfer_len; i++) BIDIR_TX_BUF[i] = 0;
    dwb();

    while (SPI_STAT & SPI_STAT_BUSY) {}
    /* Pre-arm state — sampled AFTER spi_reset_clean but BEFORE any
     * SPI register writes. Smoking-gun test: RX_RDY (bit 3) or BUSY
     * (bit 0) set here = stale FIFO content that will corrupt the
     * next transfer. */
    MBOX->last_pre_stat = SPI_STAT;
    MBOX->last_pre_err  = SPI_ERR;
    SPI_EN = 0;
    SPI_CS = 0;
    SPI_DMAGO = 0;
    SPI_CTRL = 0x007;
    SPI_LEN = xfer_len - 1;
    SPI_DMAMD = 3;
    SPI_CLK = DRV_SPI_CLK_DIV;
    SPI_DMACFG0 = 4;
    SPI_DMACFG1 = 3;
    dwb();
    SPI_EN = 1;
    dwb();

    DMA_EN = 3;
    DMA_ICLR = 0xF;
    DMA_CHCLR(0) = 1;
    DMA_CHCLR(1) = 1;
    dwb();

    /* Linked TX descriptor tail: payload dummy clocks after preamble. */
    RD_TX_DESC1->src = (uint32_t)(uintptr_t)(BIDIR_TX_BUF + READ_RX_SKIP);
    RD_TX_DESC1->dst = SPI_TX_PORT;
    RD_TX_DESC1->next = 0;
    RD_TX_DESC1->cfg = ((xfer_len - READ_RX_SKIP) & DMA_CFG_CNT_MASK) | DMA_CFG_TX;
    dwb();

    DMA_CHREG(0, 0x10) = 0;
    DMA_CHREG(0, 0x00) = SPI_RX_PORT;
    DMA_CHREG(0, 0x04) = (uint32_t)(uintptr_t)RX_TEMP;
    DMA_CHREG(0, 0x08) = 0;
    DMA_CHREG(0, 0x0C) = (xfer_len & DMA_CFG_CNT_MASK) | DMA_CFG_RX;
    dwb();
    dma_done_flag = 0;
    dwb();
    DMA_CHREG(0, 0x10) = DMA_GO_ARM(DMA_TRG_RX);

    DMA_CHREG(1, 0x10) = 0;
    DMA_CHREG(1, 0x00) = (uint32_t)(uintptr_t)BIDIR_TX_BUF;
    DMA_CHREG(1, 0x04) = SPI_TX_PORT;
    DMA_CHREG(1, 0x08) = (uint32_t)(uintptr_t)RD_TX_DESC1;
    DMA_CHREG(1, 0x0C) = READ_RX_SKIP | (DMA_CFG_TX & ~DMA_CFG_END);
    dwb();
    DMA_CHREG(1, 0x10) = DMA_GO_ARM(DMA_TRG_TX);
    dwb();

    SPI_CS = 1;
    SPI_CLRINT = 0;
    dwb();

    if (wait_dma_irq(budget_us) != 0) { dma_abort(); return -1; }
    uint32_t t0 = now_us();
    while (SPI_STAT & SPI_STAT_BUSY) {
        if (now_us() - t0 > budget_us) { dma_abort(); return -1; }
    }
    /* Post-TC snapshot — sample BEFORE any teardown. Overwritten every
     * chunk; VERIFY_MISS copies these into miss_* for post-mortem. */
    MBOX->last_spi_err     = SPI_ERR;
    MBOX->last_spi_pending = *(volatile uint32_t *)(SPI_BASE + 0x10);
    MBOX->last_dma_stat    = *(volatile uint32_t *)(DMA_BASE + 0x04);
    MBOX->last_ch0_ctrl    = DMA_CHREG(0, 0x0C);
    MBOX->last_ch0_cfg     = DMA_CHREG(0, 0x10);
    MBOX->last_ch1_ctrl    = DMA_CHREG(1, 0x0C);
    MBOX->last_ch1_cfg     = DMA_CHREG(1, 0x10);
    MBOX->last_dma_en      = DMA_EN;
    /* Capture the first 8 RX_TEMP bytes (including the READ_RX_SKIP=4
     * region we'd normally discard). Lets us see whether the shift is
     * on the TX-alignment side (real flash data leaks into skip region)
     * or on the RX-alignment side (dummies leak into our data).  */
    MBOX->last_rx_head0 = *(uint32_t *)(uintptr_t)RX_TEMP;
    MBOX->last_rx_head1 = *(uint32_t *)((uintptr_t)RX_TEMP + 4);
    /* Drain both channels via PL080-proper Halt+Active-poll+Enable-clear
     * BEFORE dropping CS / disabling the engine. Prevents stale bytes
     * in either the PL080 channel FIFO or the SPI IP TX FIFO from
     * persisting into the next transaction.  2026-04-24  */
    dma_channel_drain(0, 1000);
    dma_channel_drain(1, 1000);
    SPI_CS = 0;
    SPI_EN = 0;
    DMA_EN = 0;
    DMA_ICLR = 0xF;
    for (uint32_t i = 0; i < len; i++) out[i] = RX_TEMP[READ_RX_SKIP + i];
    return 0;
}

/* Split-mode read (TX-only DMA cmd + mid-CS DMAMD switch to RX-only DMA)
 * was tested 2026-05-01 and does not work on this Arasan IP — flash
 * returns idle 0xFF whether SPI_EN is toggled or not between phases.
 * Best explanation: the IP needs a CS edge to start clocking a new
 * transfer, so a mid-CS DMAMD/CTRL/LEN change does not re-trigger SCK.
 * Stock eCos's spi_flash_cmd_pp only splits across separate CS cycles —
 * which works for PROGRAM but not for READ (READ requires cmd→data
 * contiguous within a single CS hold). See spi-flash.md §8 for the
 * full investigation log. */

/* RX-only DMA read mirroring the ROM bootloader's dma_spi_read_setup
 * @ 0x4F290. Verified pattern that the bootloader uses to CRC-check
 * every firmware boot at PLL clock — proves it works at PLL.
 *
 * Differences from dma_bidir_read:
 *   - SPI_CTRL = 0x307 (RX-DMA mode, not 0x007 base)
 *   - SPI_DMAMD = 1 (RX-only, not 3 bidir)
 *   - Single DMA channel (CH0 RX), no TX channel
 *   - DMA SRC = SPI_BASE + 0x60 (DATA register), NOT 0x70 (RX_PORT)
 *   - cmd + 3-byte address pushed via PIO writes to SPI_DATA register
 *     before kicking the engine via SPI_CS = 1
 *
 * The DATA-register-as-DMA-source is the key bit: bypasses the bidir
 * RX_PORT FIFO packer state that causes the +4B post-AAI shift.
 * Output goes directly to caller's `out` buffer — no echo bytes to
 * skip, no RX_TEMP intermediate copy. */
static int dma_rxonly_read(uint32_t spi_addr, uint8_t *out, uint32_t len,
                           uint32_t budget_us)
{
    if (len == 0 || len > 0xFFF) return -1;

    /* No dummy JEDEC preamble here — verified 2026-05-05 that adding
     * one (mirroring dma_bidir_read 2026-05-02) DOUBLES the +2B-shift
     * miss rate (23% → 47%). The dummy JEDEC is a bidir transaction;
     * running it before an rxonly read re-injects the very FIFO-packer
     * state that rxonly was bypassing. The two paths have different
     * post-AAI sensitivities: bidir's own first chunk hits the packer
     * bug (dummy absorbs it); rxonly's CTRL=0x307 / TMOD=EEPROM_READ
     * sidesteps the packer entirely (no dummy needed). */

    while (SPI_STAT & SPI_STAT_BUSY) {}
    SPI_EN = 0;
    SPI_CS = 0;
    SPI_DMAGO = 0;
    SPI_CTRL = 0x307;            /* RX-DMA mode (bootloader value) */
    SPI_LEN = len - 1;           /* response length only */
    SPI_DMAMD = 1;               /* RX-only DMA */
    SPI_CLK = DRV_SPI_CLK_DIV;
    SPI_DMACFG0 = 4;
    SPI_DMACFG1 = 3;
    dwb();
    SPI_EN = 1;
    dwb();

    /* Arm CH0 RX-only, sourcing from DATA register (0xCC000060). */
    DMA_EN = 1;
    DMA_ICLR = 0xF;
    DMA_CHCLR(0) = 1;
    dwb();
    DMA_CHREG(0, 0x10) = 0;
    DMA_CHREG(0, 0x00) = SPI_BASE + 0x60u;   /* DATA register */
    DMA_CHREG(0, 0x04) = (uint32_t)(uintptr_t)out;
    DMA_CHREG(0, 0x08) = 0;
    DMA_CHREG(0, 0x0C) = (len & DMA_CFG_CNT_MASK) | DMA_CFG_RX;
    dwb();
    dma_done_flag = 0;
    dwb();
    DMA_CHREG(0, 0x10) = DMA_GO_ARM(DMA_TRG_RX);
    dwb();

    /* Push READ opcode + 3-byte address via PIO DATA-register writes —
     * the bootloader pattern. The IP queues these on the TX side; CS=1
     * (= bootloader's "SPI_FLASH_START=1") starts clocking. */
    SPI_DATA = 0x03;
    SPI_DATA = (spi_addr >> 16) & 0xFF;
    SPI_DATA = (spi_addr >> 8) & 0xFF;
    SPI_DATA = spi_addr & 0xFF;
    dwb();

    SPI_CS = 1;
    SPI_CLRINT = 0;
    dwb();

    if (wait_dma_irq(budget_us) != 0) { dma_abort(); return -1; }
    {
        uint32_t t0 = now_us();
        while (SPI_STAT & SPI_STAT_BUSY) {
            if (now_us() - t0 > budget_us) { dma_abort(); return -1; }
        }
    }

    /* VERIFY_MISS introspection — first 8 bytes of the destination so the
     * existing miss classifier can compare against expected. */
    MBOX->last_rx_head0 = *(uint32_t *)(uintptr_t)out;
    MBOX->last_rx_head1 = *(uint32_t *)((uintptr_t)out + 4);

    SPI_CS = 0;
    SPI_EN = 0;
    DMA_EN = 0;
    DMA_ICLR = 0xF;
    return 0;
}

static int do_read(uint32_t spi_addr, uint8_t *dest, uint32_t len) {
    set_phase(V2_PHASE_READ);
    int (*read_fn)(uint32_t, uint8_t *, uint32_t, uint32_t) =
        (TIM->read_mode == V2_READ_MODE_RXONLY) ? dma_rxonly_read
                                                : dma_bidir_read;
    for (uint32_t off = 0; off < len; off += READ_CHUNK) {
        uint32_t chunk = len - off;
        if (chunk > READ_CHUNK) chunk = READ_CHUNK;
        spi_reset_clean();   /* clean state per chunk */
        if (read_fn(spi_addr + off, dest + off, chunk, 100000) != 0) {
            set_error(V2_ERR_READ_DMA, spi_addr + off, (uint8_t)MBOX->last_sr);
            return -1;
        }
        MBOX->phase_detail = off + chunk;
    }
    return 0;
}

/* ROM CRC32 step routine — same one the TCAT bootloader uses
 * (spi_dma_read_and_crc @ 0x4F32C calls *(ROM[0x10C]) = 0x20000DA4).
 * Convention: poly 0xEDB88320 reflected, init=0, no final XOR. Host
 * matches with `zlib.crc32(buf, ~0) ^ ~0`. */
typedef uint32_t (*rom_crc_step_t)(uint32_t crc, const void *buf, uint32_t len);
#define ROM_CRC_STEP ((rom_crc_step_t)0x20000DA4u)

static int do_hash_range(uint32_t spi_addr, uint32_t len, uint32_t *out_crc,
                         uint32_t *sec_buf) {
    /* Reuse do_read into the 4 KB scratch at SRAM 0x0 — saves a duplicate
     * chunk loop. Read errors come through as V2_ERR_READ_DMA.
     *
     * If sec_buf != NULL, also accumulate per-sector CRCs and write each
     * one to sec_buf[i] when we cross a V2_SECTOR_SIZE boundary. With
     * READ_CHUNK = 0x800 and V2_SECTOR_SIZE = 0x1000, exactly two chunks
     * fit per sector — no need to split chunks. */
    uint8_t *scratch = (uint8_t *)0x00000000u;
    uint32_t crc = 0;
    uint32_t sec_crc = 0;
    uint32_t sec_off = 0;        /* bytes into the current sector */
    uint32_t sec_idx = 0;        /* index into sec_buf */
    for (uint32_t off = 0; off < len; off += READ_CHUNK) {
        uint32_t chunk = len - off;
        if (chunk > READ_CHUNK) chunk = READ_CHUNK;
        if (do_read(spi_addr + off, scratch, chunk) != 0) return -1;
        crc = ROM_CRC_STEP(crc, scratch, chunk);
        if (sec_buf) {
            sec_crc = ROM_CRC_STEP(sec_crc, scratch, chunk);
            sec_off += chunk;
            if (sec_off >= V2_SECTOR_SIZE) {
                sec_buf[sec_idx++] = sec_crc;
                sec_crc = 0;
                sec_off = 0;
            }
        }
    }
    *out_crc = crc;
    return 0;
}

static void verify_preflight_quiesce(void) {
    uint32_t mode = TIM->verify_quiesce_mode;

    if (mode == V2_VERIFY_QUIESCE_OFF) return;

    if (mode == V2_VERIFY_QUIESCE_LIGHT) {
        /* Lightweight isolate-before-verify: clear SPI state and DMA
         * channels that participate in dma_bidir_read. */
        dma_abort();
        spi_reset_clean();
        blender_dma_engine_reset((volatile uint32_t *)0x80000000u, 4u);
        busy_wait_us(200);
        return;
    }

    /* Strict mode (default): reboot-like teardown so verify runs
     * from a bootloader-like baseline even under live eCos state.
     *
     * Important: this tears down timers too, so we must re-enable Timer0
     * immediately afterward or timeout paths (now_us/busy_wait_us) stall. */
    peripheral_full_teardown();
    timer_enable();
    time_reset();
    spi_reset_clean();
    (void)spi_cmd1(SST_CMD_WRDI);
    busy_wait_us(200);
}

/* In-driver verify-retry counter. Host reads for diagnostics. */
#define V2_VERIFY_RETRY_MAX 4u

static int do_verify(uint32_t spi_addr, const uint8_t *expected, uint32_t len) {
    set_phase(V2_PHASE_VERIFY);
    verify_preflight_quiesce();
    /* 4 KB scratch buffer at the start of SRAM — reused across the driver
     * for single-sector readback. Host must not place data there while a
     * verify is in progress. */
    uint8_t *readback = (uint8_t *)0x00000000u;
    uint32_t remaining = len;
    uint32_t off = 0;
    uint32_t chunks_done = 0;
    while (remaining) {
        uint32_t chunk = remaining > V2_SECTOR_SIZE ? V2_SECTOR_SIZE : remaining;

        /* In-driver retry for the SoC-internal half-word DMA-capture
         * bug: if the first read miscompares, re-issue the read up to
         * MAX times. The flash content is almost certainly correct
         * (silicon-bug causes ~+2 SRAM offset in the captured bytes,
         * not miswritten flash). Retrying gives the buggy capture a
         * fresh roll of the dice. See firmware/spi-flash.md and
         * task #32 for the full story. */
        uint32_t first_bad = chunk;  /* sentinel for "all matched" */
        uint8_t  first_got = 0, first_exp = 0;

        for (uint32_t attempt = 0; attempt < V2_VERIFY_RETRY_MAX; attempt++) {
            if (do_read(spi_addr + off, readback, chunk) != 0)
                return -1;
            /* Scan for first mismatch. */
            first_bad = chunk;
            for (uint32_t i = 0; i < chunk; i++) {
                if (readback[i] != expected[off + i]) {
                    first_bad = i;
                    first_got = readback[i];
                    first_exp = expected[off + i];
                    break;
                }
            }
            if (first_bad == chunk) break;   /* clean */
            MBOX->verify_retries++;
        }
        chunks_done++;

        if (first_bad != chunk) {
            /* Still bad after retries — report miss. Capture state. */
            uint32_t abs_off = off + first_bad;
            uint32_t got_w = 0, exp_w = 0;
            uint32_t n = chunk - first_bad; if (n > 4) n = 4;
            for (uint32_t k = 0; k < n; k++) {
                got_w |= (uint32_t)readback[first_bad + k] << (k * 8);
                exp_w |= (uint32_t)expected[off + first_bad + k] << (k * 8);
            }
            MBOX->miss_offset     = abs_off;
            MBOX->miss_got        = got_w;
            MBOX->miss_expected   = exp_w;
            MBOX->miss_spi_stat   = SPI_STAT;
            MBOX->miss_dma_istat  = DMA_ISTAT;
            MBOX->miss_reads_done = chunks_done;
            /* Snapshot the last-chunk diagnostic set (sampled in
             * dma_bidir_read post-TC) so it's preserved even if
             * subsequent chunks run and overwrite last_*. */
            MBOX->miss_spi_err     = MBOX->last_spi_err;
            MBOX->miss_spi_pending = MBOX->last_spi_pending;
            MBOX->miss_dma_stat    = MBOX->last_dma_stat;
            MBOX->miss_ch0_ctrl    = MBOX->last_ch0_ctrl;
            MBOX->miss_ch0_cfg     = MBOX->last_ch0_cfg;
            MBOX->miss_ch1_ctrl    = MBOX->last_ch1_ctrl;
            MBOX->miss_ch1_cfg     = MBOX->last_ch1_cfg;
            MBOX->miss_dma_en      = MBOX->last_dma_en;
            MBOX->miss_pre_stat    = MBOX->last_pre_stat;
            MBOX->miss_pre_err     = MBOX->last_pre_err;
            MBOX->miss_rx_head0    = MBOX->last_rx_head0;
            MBOX->miss_rx_head1    = MBOX->last_rx_head1;
            MBOX->miss_rdsr_after  = MBOX->last_rdsr_after;
            dwb();
            log_put(V2_EVT_VERIFY_MISS, (uint16_t)abs_off,
                    spi_addr + abs_off);
            MBOX->last_sr = ((uint32_t)first_exp << 8) | first_got;
            set_error(V2_ERR_VERIFY_MISMATCH, spi_addr + abs_off, first_got);
            return -1;
        }
        off       += chunk;
        remaining -= chunk;
    }
    log_put(V2_EVT_VERIFY_OK, 0, spi_addr);
    return 0;
}

/* Autonomous "flash block" op — erase (largest fitting SE/BE32/BE64)
 * → AAI program the whole span → verify — all in one command.
 *
 * Erase-size auto-pick:
 *   64 KB: len ≤ 0x10000 AND addr 64 KB-aligned
 *   32 KB: len ≤ 0x08000 AND addr 32 KB-aligned
 *    4 KB: len ≤ 0x01000 AND addr  4 KB-aligned
 *   else: V2_ERR_BAD_ADDR / V2_ERR_BAD_LENGTH
 *
 * AAI auto-increments the internal address pointer, so a single
 * do_aai_program call can cover up to the erased span contiguously.
 * 0xFF stretches in the data still cost AAI cycles (no skip logic);
 * if that becomes a bottleneck we'll add a sector-skip variant. */
static int do_flash_block(uint32_t spi_addr, const uint8_t *data, uint32_t len) {
    if (len == 0) {
        set_error(V2_ERR_BAD_LENGTH, spi_addr, 0);
        return -1;
    }
    uint8_t erase_op;
    if ((spi_addr & 0xFFFFu) == 0 && len <= 0x10000u) {
        erase_op = SST_CMD_BE_64K;
    } else if ((spi_addr & 0x7FFFu) == 0 && len <= 0x8000u) {
        erase_op = SST_CMD_BE_32K;
    } else if ((spi_addr & 0xFFFu) == 0 && len <= V2_SECTOR_SIZE) {
        erase_op = SST_CMD_SE;
    } else {
        set_error(V2_ERR_BAD_ADDR, spi_addr, 0);
        return -1;
    }
    if (do_erase_any(spi_addr, erase_op) != 0) return -1;
    if (do_aai_program(spi_addr, data, len) != 0) return -1;
    /* Verify preflight is centralized in do_verify via
     * verify_preflight_quiesce(). */
    if (do_verify(spi_addr, data, len) != 0) return -1;
    return 0;
}

/* Autonomous "flash one sector" op — erase + AAI program + verify in
 * one command. Saves the host ~2/3 of the JTAG round-trips it would
 * otherwise spend issuing ERASE → PROGRAM → VERIFY separately. */
static int do_flash_sector(uint32_t spi_addr, const uint8_t *data, uint32_t len) {
    if (len == 0 || len > V2_SECTOR_SIZE) {
        set_error(V2_ERR_BAD_LENGTH, spi_addr, 0);
        return -1;
    }
    if (spi_addr & 0xFFFu) {
        set_error(V2_ERR_BAD_ADDR, spi_addr, 0);
        return -1;
    }
    if (do_erase(spi_addr) != 0) return -1;
    if (do_aai_program(spi_addr, data, len) != 0) return -1;
    /* Verify preflight is centralized in do_verify via
     * verify_preflight_quiesce(). */
    if (do_verify(spi_addr, data, len) != 0) return -1;
    return 0;
}

/* ── Command dispatch ─────────────────────────────────────── */

static void handle_command(uint32_t cmd) {
    /* do_verify's readback scratch lives at 0x0-0xFFF, which overlaps
     * the IRQ vector trampoline at 0x18/0x38. Refresh the vectors at
     * the top of every command so any prior verify can't brick the
     * next DMA wait. Cheap: two word writes + icache invalidate. */
    install_vectors();
    log_put(V2_EVT_CMD_RX, (uint16_t)cmd, MBOX->flash_addr);
    MBOX->status = V2_STATUS_BUSY;
    MBOX->err_code = V2_ERR_NONE;
    MBOX->err_spi_addr = 0;
    MBOX->phase_detail = 0;
    MBOX->pair_retries = 0;
    MBOX->miss_ch2_cfg = MBOX->miss_ch3_cfg = MBOX->miss_ch4_cfg = 0;
    MBOX->miss_ch5_cfg = MBOX->miss_ch6_cfg = MBOX->miss_ch7_cfg = 0;
    uint32_t op_start_us = now_us();

    int rc = -1;
    switch (cmd) {
    case V2_CMD_BP_CLEAR:
        rc = do_bp_clear();
        break;
    case V2_CMD_ERASE:
        rc = do_erase(MBOX->flash_addr);
        break;
    case V2_CMD_ERASE_32K:
        rc = do_erase_block_32k(MBOX->flash_addr);
        break;
    case V2_CMD_ERASE_64K:
        rc = do_erase_block_64k(MBOX->flash_addr);
        break;
    case V2_CMD_FLASH_SECTOR: {
        const uint8_t *src = (const uint8_t *)(uintptr_t)MBOX->buf_addr;
        rc = do_flash_sector(MBOX->flash_addr, src, MBOX->length);
        break;
    }
    case V2_CMD_FLASH_BLOCK: {
        const uint8_t *src = (const uint8_t *)(uintptr_t)MBOX->buf_addr;
        rc = do_flash_block(MBOX->flash_addr, src, MBOX->length);
        break;
    }
    case V2_CMD_PROGRAM: {
        const uint8_t *src = (const uint8_t *)(uintptr_t)MBOX->buf_addr;
        rc = do_aai_program(MBOX->flash_addr, src, MBOX->length);
        break;
    }
    case V2_CMD_READ: {
        uint8_t *dst = (uint8_t *)(uintptr_t)MBOX->buf_addr;
        rc = do_read(MBOX->flash_addr, dst, MBOX->length);
        break;
    }
    case V2_CMD_VERIFY: {
        const uint8_t *exp = (const uint8_t *)(uintptr_t)MBOX->buf_addr;
        rc = do_verify(MBOX->flash_addr, exp, MBOX->length);
        break;
    }
    case V2_CMD_BYTE_PATCH: {
        const uint8_t *src = (const uint8_t *)(uintptr_t)MBOX->buf_addr;
        rc = 0;
        for (uint32_t k = 0; k < MBOX->length; k++) {
            if (do_byte_program(MBOX->flash_addr + k, src[k]) != 0) {
                set_error(V2_ERR_AAI_PAIR_TX, MBOX->flash_addr + k, src[k]);
                rc = -1;
                break;
            }
        }
        break;
    }
    case V2_CMD_READ_ID: {
        uint8_t *dst = (uint8_t *)(uintptr_t)MBOX->buf_addr;
        spi_reset_clean();
        if (MBOX->flash_addr == 0) {
            /* JEDEC 0x9F — returns [manuf, type, capacity] = 3 bytes. */
            rc = dma_rx(0x9F, dst, 3, 5000);
            MBOX->length = 3;
        } else {
            /* Legacy Read-ID 0xAB — 3 dummy addr bytes before 1B resp.
             * dma_rx only supports cmd+resp framing, so use a custom
             * 5-byte bidir via the dma_bidir_read machinery. Simpler:
             * stage [0xAB, 0, 0, 0, 0] in BIDIR_TX_BUF and capture at
             * offset 4. */
            BIDIR_TX_BUF[0] = 0xAB;
            BIDIR_TX_BUF[1] = 0;
            BIDIR_TX_BUF[2] = 0;
            BIDIR_TX_BUF[3] = 0;
            BIDIR_TX_BUF[4] = 0;
            dwb();
            /* Reuse dma_bidir_read — but that uses READ_RX_SKIP=8.
             * Fall through to a minimal hand-rolled TX+RX bidir. */
            while (SPI_STAT & SPI_STAT_BUSY) {}
            SPI_EN = 0; SPI_CS = 0; SPI_DMAGO = 0;
            SPI_CTRL = 0x007; SPI_LEN = 4; SPI_DMAMD = 3;
            SPI_CLK = DRV_SPI_CLK_DIV; SPI_DMACFG0 = 4; SPI_DMACFG1 = 3;
            dwb(); SPI_EN = 1; dwb();
            DMA_EN = 3; DMA_ICLR = 3;
            DMA_CHCLR(0) = 1; DMA_CHCLR(1) = 1; dwb();
            DMA_CHREG(0, 0x10) = 0;
            DMA_CHREG(0, 0x00) = SPI_RX_PORT;
            DMA_CHREG(0, 0x04) = (uint32_t)(uintptr_t)RX_TEMP;
            DMA_CHREG(0, 0x08) = 0;
            DMA_CHREG(0, 0x0C) = 5u | DMA_CFG_RX;
            dwb();
            dma_done_flag = 0; dwb();
            DMA_CHREG(0, 0x10) = DMA_TRG_RX;
            DMA_CHREG(1, 0x10) = 0;
            DMA_CHREG(1, 0x00) = (uint32_t)(uintptr_t)BIDIR_TX_BUF;
            DMA_CHREG(1, 0x04) = SPI_TX_PORT;
            DMA_CHREG(1, 0x08) = 0;
            DMA_CHREG(1, 0x0C) = 5u | DMA_CFG_TX;
            dwb();
            DMA_CHREG(1, 0x10) = DMA_TRG_TX; dwb();
            SPI_CS = 1; SPI_CLRINT = 0; dwb();
            if (wait_dma_irq(5000) != 0) { dma_abort(); rc = -1; break; }
            uint32_t t0 = now_us();
            while (SPI_STAT & SPI_STAT_BUSY) {
                if (now_us() - t0 > 5000) { dma_abort(); rc = -1; goto rid_done;}
            }
            SPI_CS = 0; SPI_EN = 0; DMA_EN = 0; DMA_ICLR = 3;
            dst[0] = RX_TEMP[4];  /* last byte is manufacturer */
            MBOX->length = 1;
            rc = 0;
        }
rid_done:
        if (rc != 0) set_error(V2_ERR_READ_DMA, MBOX->flash_addr, 0);
        break;
    }
    case V2_CMD_HASH_RANGE: {
        uint32_t crc = 0;
        uint32_t *sec_buf = MBOX->buf_addr ?
                            (uint32_t *)(uintptr_t)MBOX->buf_addr : (uint32_t *)0;
        rc = do_hash_range(MBOX->flash_addr, MBOX->length, &crc, sec_buf);
        MBOX->hash_result = crc;
        break;
    }
    default:
        set_error(V2_ERR_BAD_COMMAND, 0, (uint8_t)cmd);
        MBOX->elapsed_us = now_us() - op_start_us;
        return;
    }

    MBOX->elapsed_us = now_us() - op_start_us;
    if (rc == 0) {
        MBOX->status = V2_STATUS_OK;
        set_phase(V2_PHASE_DONE);
    }
    /* On failure set_error already flipped status to ERR. */
}

/* ── Main loop ────────────────────────────────────────────── */

void do_main(void) {
    /* Only touch DEV-owned fields in init. HOST may have pre-staged a
     * command+magic before resume; do NOT clobber that. */
    MBOX->status       = V2_STATUS_BUSY;
    MBOX->phase        = V2_PHASE_INIT;
    MBOX->phase_detail = 0;
    MBOX->last_sr      = 0;
    MBOX->err_code     = 0;
    MBOX->err_spi_addr = 0;
    MBOX->elapsed_us   = 0;
    MBOX->seq          = 0;
    MBOX->log_head     = 0;
    MBOX->log_tail     = 0;
    MBOX->build_tag    = 0x56324648u;   /* 'V2FH' — adds V2_CMD_HASH_RANGE */
    init_timings_if_needed();
    /* Latch the AAI transport backend from the timings struct. The
     * default is bidir DMA. PIO is broken on this silicon (kept for
     * documentation). DMA_TX flips a flag inside xport_dma's inner
     * channel-program code to TMOD=TO + single-channel — diagnostic
     * for the AAI tail-drop hypothesis. */
    XPORT = &xport_dma;
    s_xport_tx_only = (TIM->xport_mode == V2_XPORT_DMA_TX) ? 1 : 0;
    dwb();

    driver_setup();

    MBOX->status = V2_STATUS_READY;
    set_phase(V2_PHASE_IDLE);

    for (;;) {
        if (MBOX->magic != V2_MAGIC_CMD) continue;
        uint32_t cmd = MBOX->command;
        /* Accept: clear magic (host re-arms by writing magic again). */
        MBOX->magic = 0;
        /* Re-latch the AAI xport — host may have toggled between cmds. */
        XPORT = &xport_dma;
        s_xport_tx_only = (TIM->xport_mode == V2_XPORT_DMA_TX) ? 1 : 0;
        dwb();

        handle_command(cmd);
        set_phase(V2_PHASE_IDLE);
    }
}

/* ── Entry ────────────────────────────────────────────────── */

void _start(void) __attribute__((naked, section(".text.entry")));
void _start(void) {
    __asm__ volatile(
        "msr cpsr_c, #0xd3\n"
        "mov r0, #0\n"
        "mcr p15, 0, r0, c7, c6, 0\n"
        "mcr p15, 0, r0, c7, c5, 0\n"
        "mrc p15, 0, r0, c1, c0, 0\n"
        "bic r0, r0, #5\n"
        "mcr p15, 0, r0, c1, c0, 0\n"
        "ldr sp, =0x2AFFC\n"
        "bl  do_main\n"
        "1: b 1b\n"
    );
}
