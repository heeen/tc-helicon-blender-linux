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
 *   0x2C000-0x2DFFF  driver code
 *   0x2E000-0x2E0FF  V2_MBOX (mailbox)
 *   0x2E100-0x2E17F  TX_SCRATCH / RX_SCRATCH / misc
 *   0x2E200-0x2E7FF  V2_LOG_RING (128 × 12 B)
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
    TIM->magic                     = V2_TIMINGS_MAGIC;
}
#define LOG_RING  ((struct v2_log_entry *)V2_LOG_RING_ADDR)
#define DATA_BUF  ((uint8_t *)V2_DATA_BUF_ADDR)

#define TX_SCRATCH  ((volatile uint8_t *)0x0002E100u)
#define RX_SCRATCH  ((volatile uint8_t *)0x0002E120u)
#define TX_SCRATCH_MAX  16

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
    SPI_CLK = 2;
    SPI_CLRINT = 0;
    SPI_DMACFG0 = 0;
    SPI_DMACFG1 = 0;
    DMA_EN = 0;
    DMA_ICLR = 3;
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
    DMA_ICLR = 3;
    DMA_CHCLR(0) = 1;
    DMA_CHCLR(1) = 1;
    SPI_CS = 0;
    SPI_EN = 0;
    dwb();
}

/* DMA TX — CS assert + N-byte burst + CS deassert. Timer-bounded. */
static int dma_tx(const volatile uint8_t *buf, uint32_t len, uint32_t budget_us) {
    dwb();
    while (SPI_STAT & SPI_STAT_BUSY) {}
    SPI_EN = 0;
    SPI_CS = 0;
    SPI_DMAGO = 0;
    SPI_CTRL = 0x107;
    SPI_LEN = len - 1;
    SPI_DMAMD = 2;
    SPI_CLK = 2;
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
    DMA_CHREG(0, 0x10) = DMA_TRG_TX;
    dwb();

    SPI_CS = 1;
    SPI_CLRINT = 0;
    dwb();

    uint32_t t0 = now_us();
    while (!(DMA_ISTAT & 1)) {
        if (now_us() - t0 > budget_us) { dma_abort(); return -1; }
    }
    /* Give BUSY time to assert before waiting for it to clear. */
    for (volatile int j = 0; j < 200; j++)
        if (SPI_STAT & SPI_STAT_BUSY) break;
    while (SPI_STAT & SPI_STAT_BUSY) {
        if (now_us() - t0 > budget_us) { dma_abort(); return -1; }
    }
    SPI_CS = 0;
    SPI_EN = 0;
    DMA_EN = 0;
    DMA_ICLR = 3;
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
    SPI_CLK = 2;
    SPI_DMACFG0 = 4;
    SPI_DMACFG1 = 3;
    dwb();
    SPI_EN = 1;
    dwb();

    DMA_EN = 3;
    DMA_ICLR = 3;
    DMA_CHCLR(0) = 1;
    DMA_CHCLR(1) = 1;
    dwb();

    DMA_CHREG(0, 0x10) = 0;
    DMA_CHREG(0, 0x00) = SPI_RX_PORT;
    DMA_CHREG(0, 0x04) = (uint32_t)(uintptr_t)RX_SCRATCH;
    DMA_CHREG(0, 0x08) = 0;
    DMA_CHREG(0, 0x0C) = (total & 0xFFF) | DMA_CFG_RX;
    dwb();
    DMA_CHREG(0, 0x10) = DMA_TRG_RX;

    DMA_CHREG(1, 0x10) = 0;
    DMA_CHREG(1, 0x00) = (uint32_t)(uintptr_t)TX_SCRATCH;
    DMA_CHREG(1, 0x04) = SPI_TX_PORT;
    DMA_CHREG(1, 0x08) = 0;
    DMA_CHREG(1, 0x0C) = 0xFFF | DMA_CFG_TX;
    dwb();
    DMA_CHREG(1, 0x10) = DMA_TRG_TX;
    dwb();

    SPI_CS = 1;
    SPI_CLRINT = 0;
    dwb();

    uint32_t t0 = now_us();
    while (!(DMA_ISTAT & 1)) {
        if (now_us() - t0 > budget_us) { dma_abort(); return -1; }
    }
    while (SPI_STAT & SPI_STAT_BUSY) {
        if (now_us() - t0 > budget_us) { dma_abort(); return -1; }
    }
    SPI_CS = 0;
    SPI_EN = 0;
    DMA_EN = 0;
    DMA_ICLR = 3;
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

#define REBOOT_USB_BASE            0x90000000u
#define REBOOT_USB_USBCMD          (*(volatile uint32_t *)(REBOOT_USB_BASE + 0x08))
#define REBOOT_USB_USBSTS          (*(volatile uint32_t *)(REBOOT_USB_BASE + 0x14))
#define REBOOT_USB_USBINTR         (*(volatile uint32_t *)(REBOOT_USB_BASE + 0x18))
#define REBOOT_USB_DEVICEADDR      (*(volatile uint32_t *)(REBOOT_USB_BASE + 0x24))
#define REBOOT_USB_ENDPTLISTADDR   (*(volatile uint32_t *)(REBOOT_USB_BASE + 0x28))
#define REBOOT_TCAT_USBMODE        (*(volatile uint32_t *)(REBOOT_USB_BASE + 0x800))
#define REBOOT_TCAT_EP_COMP_STATUS (*(volatile uint32_t *)(REBOOT_USB_BASE + 0x818))
#define REBOOT_TCAT_EP_COMP_ENABLE (*(volatile uint32_t *)(REBOOT_USB_BASE + 0x81C))
#define REBOOT_TCAT_EP_ASYNC_PRIME (*(volatile uint32_t *)(REBOOT_USB_BASE + 0x834))
#define REBOOT_TCAT_EP_RX_EN       (*(volatile uint32_t *)(REBOOT_USB_BASE + 0x810))
#define REBOOT_TCAT_EP_TX_EN       (*(volatile uint32_t *)(REBOOT_USB_BASE + 0x814))

#define REBOOT_VIC_INT_EN_CLR (*(volatile uint32_t *)0xFFFFF014)
#define REBOOT_VIC_SOFT_CLR   (*(volatile uint32_t *)0xFFFFF01C)

#define REBOOT_FLASH_SPI      ((volatile uint32_t *)0xCC000000u)
#define REBOOT_LED_SPI        ((volatile uint32_t *)0xCF000000u)
#define REBOOT_LED_GPIO       (*(volatile uint32_t *)0xCB000020)

static void spi_ip_quiesce(volatile uint32_t *s) {
    for (volatile int i = 0; i < 100000; i++)
        if (!(s[0x28 / 4] & 1u)) break;
    s[0x10 / 4] = 0;
    s[0x08 / 4] = 0;
    s[0x2C / 4] = 0;
    s[0x4C / 4] = 0;
    s[0x50 / 4] = 0;
    s[0x54 / 4] = 0;
    s[0x00 / 4] = 0;
    s[0x04 / 4] = 0;
    s[0x18 / 4] = 0;
    s[0x34 / 4] = 0;
    dwb();
}

static void dma_engine_reset(void) {
    volatile uint32_t *dma = (volatile uint32_t *)0x80000000u;
    dma[0x08 / 4] = 0;
    dma[0x10 / 4] = 0x0Fu;
    for (unsigned ch = 0; ch < 4; ch++)
        dma[0x30 / 4 + ch] = 1;
    for (unsigned ch = 0; ch < 4; ch++) {
        volatile uint32_t *b = (volatile uint32_t *)(0x80000100u + ch * 0x20u);
        b[0x10 / 4] = 0;
        b[0x0C / 4] = 0;
        b[0x08 / 4] = 0;
        b[0x00 / 4] = 0;
        b[0x04 / 4] = 0;
    }
    dwb();
}

static void usb_hw_reset(void) {
    REBOOT_USB_USBSTS = 0xFFFFFFFFu;
    REBOOT_USB_USBCMD &= ~(uint32_t)1;
    REBOOT_USB_USBCMD |= 2;
    for (volatile int i = 0; i < 100000; i++)
        if (!(REBOOT_USB_USBCMD & 2)) break;
    REBOOT_USB_USBSTS = 0xFFFFFFFFu;
    REBOOT_USB_USBINTR = 0;
    REBOOT_USB_USBCMD &= ~(uint32_t)((1u << 0) | (1u << 13) | (1u << 14));
    REBOOT_USB_DEVICEADDR = 0;
    REBOOT_USB_ENDPTLISTADDR = 0;
    REBOOT_TCAT_USBMODE = 2u;
    REBOOT_TCAT_EP_COMP_STATUS = 0xFFFFFFFFu;
    REBOOT_TCAT_EP_COMP_ENABLE = 0;
    REBOOT_TCAT_EP_ASYNC_PRIME = 0;
    REBOOT_TCAT_EP_RX_EN = 0;
    REBOOT_TCAT_EP_TX_EN = 0;
    dwb();
}

static void driver_setup(void) {
    /* Timer0 must be running before log_put() is called (it timestamps). */
    timer_enable();
    time_reset();
    set_phase(V2_PHASE_INIT);
    log_put(V2_EVT_SETUP_BEGIN, 0, 0);

    __asm__ volatile("mov r0, #0xD3 \n msr cpsr_c, r0 \n" ::: "r0", "memory");

    set_phase(V2_PHASE_TEARDOWN);
    usb_hw_reset();

    spi_ip_quiesce(REBOOT_LED_SPI);
    REBOOT_LED_GPIO = 0;
    REBOOT_LED_SPI[0x14 / 4] = 0xFF;
    dwb();
    spi_ip_quiesce(REBOOT_FLASH_SPI);

    dma_engine_reset();

    REBOOT_VIC_INT_EN_CLR = 0xFFFFFFFFu;
    REBOOT_VIC_SOFT_CLR   = 0xFFFFFFFFu;
    dwb();

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

static int wait_not_busy(uint32_t budget_us, uint8_t *out_sr) {
    uint32_t t0 = now_us();
    for (;;) {
        uint8_t sr = rdsr();
        if (sr != 0xFF) {
            MBOX->last_sr = sr;
            if (!(sr & SST_SR_BUSY)) {
                if (out_sr) *out_sr = sr;
                return 0;
            }
        }
        if (now_us() - t0 > budget_us) {
            if (out_sr) *out_sr = sr;
            return -1;
        }
    }
}

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
#define AAI_MAX_RUN 0x8000u
#define AAI_TAIL_SPLIT 4u

static int do_aai_program_run(uint32_t spi_addr, const uint8_t *data, uint32_t len);

/* Byte-program a single byte at spi_addr. Used to patch the 4-byte tail
 * of each AAI run, which AAI silently drops on this silicon. */
static int do_byte_program(uint32_t spi_addr, uint8_t byte) {
    log_put(V2_EVT_AAI_WREN, byte, spi_addr);
    spi_reset_clean();
    if (spi_cmd1(SST_CMD_WREN) != 0) {
        log_put(V2_EVT_BAD_STATE, 0x01, spi_addr);
        return -1;
    }
    busy_wait_us(5);
    TX_SCRATCH[0] = SST_CMD_BYTE_PRG;
    TX_SCRATCH[1] = (spi_addr >> 16) & 0xFF;
    TX_SCRATCH[2] = (spi_addr >> 8) & 0xFF;
    TX_SCRATCH[3] = spi_addr & 0xFF;
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
     * + 2 dummy FF) and the 2nd dummy pair (3 bytes: 0xAD FF FF).
     * Leaves the controller configured and ready for aai_pair(). */
    int  (*aai_begin)(uint32_t spi_addr);
    /* Send one AAI subsequent pair (3 bytes: 0xAD + d0 + d1) as a
     * single CS cycle. `pair_idx` is the 0-based pair index for logs. */
    int  (*aai_pair)(uint8_t d0, uint8_t d1, uint32_t pair_idx,
                     uint32_t spi_addr_for_log);
    /* Tear down after the last pair: CS=0, EN=0, DMA idle. */
    void (*aai_end)(void);
} spi_xport_t;

static const spi_xport_t xport_dma;
static const spi_xport_t xport_pio;
static const spi_xport_t *XPORT = &xport_dma;

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

static int xport_dma_aai_begin(uint32_t spi_addr) {
    /* Send AAI_FIRST (6 bytes: cmd + 3 addr + 2 dummy FF) via a short
     * DMA TX. This matches the historical v2 behavior. */
    set_phase(V2_PHASE_AAI_FIRST);
    TX_SCRATCH[0] = SST_CMD_AAI;
    TX_SCRATCH[1] = (spi_addr >> 16) & 0xFF;
    TX_SCRATCH[2] = (spi_addr >> 8) & 0xFF;
    TX_SCRATCH[3] = spi_addr & 0xFF;
    TX_SCRATCH[4] = 0xFF;
    TX_SCRATCH[5] = 0xFF;
    if (dma_tx(TX_SCRATCH, 6, 5000) != 0) return -1;
    log_put(V2_EVT_AAI_FIRST, 0, spi_addr);
    if (aai_pair_wait(0) != 0) return -1;

    /* Second dummy pair — subsequent-pair form (3 bytes). */
    TX_SCRATCH[0] = SST_CMD_AAI;
    TX_SCRATCH[1] = 0xFF;
    TX_SCRATCH[2] = 0xFF;
    if (dma_tx(TX_SCRATCH, 3, 5000) != 0) return -1;
    if (aai_pair_wait(1) != 0) return -1;

    /* Configure the controller for the bidir DMA hot loop. */
    SPI_EN = 0;
    SPI_CS = 0;
    SPI_DMAGO = 0;
    SPI_CTRL = 0x007;
    SPI_LEN = 2;
    SPI_DMAMD = 3;
    SPI_CLK = 2;
    SPI_DMACFG0 = 4;
    SPI_DMACFG1 = 3;
    dwb();
    SPI_EN = 1;
    dwb();

    DMA_EN = 3;
    dwb();
    DMA_CHREG(0, 0x04) = (uint32_t)(uintptr_t)RX_SCRATCH;
    DMA_CHREG(0, 0x08) = 0;
    DMA_CHREG(1, 0x04) = SPI_TX_PORT;
    DMA_CHREG(1, 0x08) = 0;
    dwb();

    TX_SCRATCH[0] = SST_CMD_AAI;
    dwb();
    return 0;
}

/* Core pair transmission: one CS cycle shifting [0xAD, d0, d1] via bidir
 * DMA. Caller sets TX_SCRATCH[1..2] before calling. Returns the SPI_ERR
 * value latched right after the DMA/SPI busy drain; 0 == clean.
 * Returns -1 on spin timeout. */
static int xport_dma_aai_shift_once(uint32_t *out_err) {
    DMA_ICLR = 3;
    DMA_CHCLR(0) = 1;
    DMA_CHCLR(1) = 1;

    DMA_CHREG(0, 0x10) = 0;
    DMA_CHREG(0, 0x00) = SPI_RX_PORT;
    DMA_CHREG(0, 0x04) = (uint32_t)(uintptr_t)RX_SCRATCH;
    DMA_CHREG(0, 0x08) = 0;
    DMA_CHREG(0, 0x0C) = 3u | DMA_CFG_RX;
    DMA_CHREG(1, 0x10) = 0;
    DMA_CHREG(1, 0x00) = (uint32_t)(uintptr_t)TX_SCRATCH;
    DMA_CHREG(1, 0x04) = SPI_TX_PORT;
    DMA_CHREG(1, 0x08) = 0;
    DMA_CHREG(1, 0x0C) = 3u | DMA_CFG_TX;
    dwb();

    DMA_CHREG(0, 0x10) = DMA_TRG_RX;
    DMA_CHREG(1, 0x10) = DMA_TRG_TX;
    dwb();

    SPI_CS = 1;
    SPI_CLRINT = 0;
    dwb();

    int spun = 0;
    while (!(DMA_ISTAT & 1)) {
        if (++spun > 20000) { dma_abort(); return -1; }
    }
    while (SPI_STAT & SPI_STAT_BUSY) {
        if (++spun > 30000) { dma_abort(); return -1; }
    }
    /* Sample SPI_ERR BEFORE dropping CS — the bit may be cleared by the
     * next CS assert edge, so sampling after is racy. Bit 2 == transfer
     * error per bootloader's dma_poll_complete. */
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

    /* First attempt. If SPI_ERR flags the transfer-error bit (bit 2 per
     * bootloader dma_poll_complete @ 0x4F16C-0x4F184), retry up to 3x.
     * Bit 0 is a benign "transfer happened" flag — fires on every pair,
     * not a drop indicator. Retry count is mirrored into
     * MBOX->pair_retries for host visibility. */
    uint32_t err = 0;
    for (int attempt = 0; attempt < 4; attempt++) {
        if (xport_dma_aai_shift_once(&err) != 0) return -1;
        if ((err & SPI_ERR_XFER) == 0) break;
        MBOX->pair_retries++;
        log_put(V2_EVT_BAD_STATE, (uint16_t)err, spi_addr_for_log);
    }

    busy_wait_us(TIM->aai_pair_fixed_us);
    (void)pair_idx;
    return 0;
}

static void xport_dma_aai_end(void) {
    SPI_CS = 0;
    SPI_EN = 0;
    DMA_EN = 0;
    DMA_ICLR = 3;
    dwb();
}

static const spi_xport_t xport_dma = {
    .aai_begin = xport_dma_aai_begin,
    .aai_pair  = xport_dma_aai_pair,
    .aai_end   = xport_dma_aai_end,
};

/* ─── xport_pio: byte-by-byte DATA-register backend ───────────
 *
 * WARNING (2026-04-18): PIO TX on the flash SPI controller (0xCC) does
 * not clock bytes reliably. v1's `pio_cmd1` uses `dma_tx` internally
 * despite the misleading name, and a commit-time note in v1
 * (sram_flash_driver.c:267-270) documents that "genuine PIO was tried
 * and the WREN silently fails on the stock firmware path." Matches what
 * we see here: this AAI-via-PIO path compiles, completes its spin
 * loops, leaves last_sr=0xFF and writes nothing to flash. Kept as a
 * stub for documentation and future debugging; not production-viable.
 * Use xport_dma for real flashing. */

static int pio_send_bytes(const uint8_t *buf, uint32_t len) {
    /* Caller must have CS asserted and LEN / EN configured. Pushes each
     * byte into the DATA register after TX_READY. Returns 0 on success,
     * -1 on spin-timeout. */
    for (uint32_t i = 0; i < len; i++) {
        int spun = 0;
        while (!(SPI_STAT & SPI_STAT_TX_RDY)) {
            if (++spun > 20000) return -1;
        }
        SPI_DATA = buf[i];
    }
    int spun = 0;
    while (SPI_STAT & SPI_STAT_BUSY) {
        if (++spun > 30000) return -1;
    }
    return 0;
}

static int pio_cs_cycle(const uint8_t *buf, uint32_t len) {
    /* Full CS cycle + PIO burst for a short transfer (up to TX_SCRATCH_MAX
     * bytes). Reconfigures the controller each call so we can interleave
     * with other modes cleanly. */
    while (SPI_STAT & SPI_STAT_BUSY) {}
    SPI_EN = 0;
    SPI_CS = 0;
    SPI_DMAGO = 0;
    SPI_DMAMD = 0;
    SPI_CTRL = 0x207;            /* PIO TX (v1 validated value) */
    SPI_LEN = len - 1;
    SPI_CLK = 2;
    dwb();
    SPI_EN = 1;
    SPI_CS = 1;
    SPI_CLRINT = 0;
    dwb();

    int rc = pio_send_bytes(buf, len);

    SPI_CS = 0;
    SPI_EN = 0;
    dwb();
    return rc;
}

static int xport_pio_aai_begin(uint32_t spi_addr) {
    set_phase(V2_PHASE_AAI_FIRST);
    TX_SCRATCH[0] = SST_CMD_AAI;
    TX_SCRATCH[1] = (uint8_t)(spi_addr >> 16);
    TX_SCRATCH[2] = (uint8_t)(spi_addr >> 8);
    TX_SCRATCH[3] = (uint8_t)spi_addr;
    TX_SCRATCH[4] = 0xFF;
    TX_SCRATCH[5] = 0xFF;
    if (pio_cs_cycle((const uint8_t *)TX_SCRATCH, 6) != 0) return -1;
    log_put(V2_EVT_AAI_FIRST, 0, spi_addr);
    if (aai_pair_wait(0) != 0) return -1;

    TX_SCRATCH[0] = SST_CMD_AAI;
    TX_SCRATCH[1] = 0xFF;
    TX_SCRATCH[2] = 0xFF;
    if (pio_cs_cycle((const uint8_t *)TX_SCRATCH, 3) != 0) return -1;
    if (aai_pair_wait(1) != 0) return -1;

    /* Leave SPI off/idle — each aai_pair call configures fresh. */
    return 0;
}

static int xport_pio_aai_pair(uint8_t d0, uint8_t d1, uint32_t pair_idx,
                              uint32_t spi_addr_for_log) {
    TX_SCRATCH[0] = SST_CMD_AAI;
    TX_SCRATCH[1] = d0;
    TX_SCRATCH[2] = d1;
    if (pio_cs_cycle((const uint8_t *)TX_SCRATCH, 3) != 0) return -1;
    busy_wait_us(TIM->aai_pair_fixed_us);
    (void)pair_idx; (void)spi_addr_for_log;
    return 0;
}

static void xport_pio_aai_end(void) {
    SPI_CS = 0;
    SPI_EN = 0;
    dwb();
}

static const spi_xport_t xport_pio = {
    .aai_begin = xport_pio_aai_begin,
    .aai_pair  = xport_pio_aai_pair,
    .aai_end   = xport_pio_aai_end,
};

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

    /* Silicon quirk: first 2 AAI pairs drop on this part. Both our DMA
     * and PIO backends handle the 2 dummy-FF prefix internally via
     * aai_begin(). Data starts at logical addr+0. */
    if (XPORT->aai_begin(spi_addr) != 0) {
        (void)spi_cmd1(SST_CMD_WRDI);
        set_error(V2_ERR_AAI_FIRST_TX, spi_addr, 0);
        return -1;
    }

    set_phase(V2_PHASE_AAI_PAIR);

    /* Write `len` real data bytes + 8 trailing dummy FF bytes. The tail
     * pad absorbs the symmetric "last 2 pairs drop" silicon quirk where
     * possible. Remaining tail drops are fixed by the host-side verify +
     * BYTE_PATCH repair step (flash_all --repair). */
    uint32_t total_bytes = len + 8;
    for (uint32_t i = 0; i < total_bytes; i += 2) {
        uint8_t d0 = (i     < len) ? data[i]     : 0xFF;
        uint8_t d1 = (i + 1 < len) ? data[i + 1] : 0xFF;
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
#define READ_RX_SKIP 8u
#define READ_CHUNK   0x800u

static int dma_bidir_read(uint32_t spi_addr, uint8_t *out, uint32_t len,
                          uint32_t budget_us)
{
    uint32_t xfer_len = READ_RX_SKIP + len;
    if (xfer_len > 0xFFF) return -1;

    BIDIR_TX_BUF[0] = SST_CMD_READ;
    BIDIR_TX_BUF[1] = (spi_addr >> 16) & 0xFF;
    BIDIR_TX_BUF[2] = (spi_addr >> 8) & 0xFF;
    BIDIR_TX_BUF[3] = spi_addr & 0xFF;
    for (uint32_t i = 4; i < xfer_len; i++) BIDIR_TX_BUF[i] = 0;
    dwb();

    while (SPI_STAT & SPI_STAT_BUSY) {}
    SPI_EN = 0;
    SPI_CS = 0;
    SPI_DMAGO = 0;
    SPI_CTRL = 0x007;
    SPI_LEN = xfer_len - 1;
    SPI_DMAMD = 3;
    SPI_CLK = 2;
    SPI_DMACFG0 = 4;
    SPI_DMACFG1 = 3;
    dwb();
    SPI_EN = 1;
    dwb();

    DMA_EN = 3;
    DMA_ICLR = 3;
    DMA_CHCLR(0) = 1;
    DMA_CHCLR(1) = 1;
    dwb();

    DMA_CHREG(0, 0x10) = 0;
    DMA_CHREG(0, 0x00) = SPI_RX_PORT;
    DMA_CHREG(0, 0x04) = (uint32_t)(uintptr_t)RX_TEMP;
    DMA_CHREG(0, 0x08) = 0;
    DMA_CHREG(0, 0x0C) = (xfer_len & 0xFFF) | DMA_CFG_RX;
    dwb();
    DMA_CHREG(0, 0x10) = DMA_TRG_RX;

    DMA_CHREG(1, 0x10) = 0;
    DMA_CHREG(1, 0x00) = (uint32_t)(uintptr_t)BIDIR_TX_BUF;
    DMA_CHREG(1, 0x04) = SPI_TX_PORT;
    DMA_CHREG(1, 0x08) = 0;
    DMA_CHREG(1, 0x0C) = 0xFFF | DMA_CFG_TX;
    dwb();
    DMA_CHREG(1, 0x10) = DMA_TRG_TX;
    dwb();

    SPI_CS = 1;
    SPI_CLRINT = 0;
    dwb();

    uint32_t t0 = now_us();
    while (!(DMA_ISTAT & 1)) {
        if (now_us() - t0 > budget_us) { dma_abort(); return -1; }
    }
    while (SPI_STAT & SPI_STAT_BUSY) {
        if (now_us() - t0 > budget_us) { dma_abort(); return -1; }
    }
    SPI_CS = 0;
    SPI_EN = 0;
    DMA_EN = 0;
    DMA_ICLR = 3;
    for (uint32_t i = 0; i < len; i++) out[i] = RX_TEMP[READ_RX_SKIP + i];
    return 0;
}

static int do_read(uint32_t spi_addr, uint8_t *dest, uint32_t len) {
    set_phase(V2_PHASE_READ);
    for (uint32_t off = 0; off < len; off += READ_CHUNK) {
        uint32_t chunk = len - off;
        if (chunk > READ_CHUNK) chunk = READ_CHUNK;
        spi_reset_clean();   /* clean state per chunk */
        if (dma_bidir_read(spi_addr + off, dest + off, chunk, 100000) != 0) {
            set_error(V2_ERR_READ_DMA, spi_addr + off, (uint8_t)MBOX->last_sr);
            return -1;
        }
        MBOX->phase_detail = off + chunk;
    }
    return 0;
}

static int do_verify(uint32_t spi_addr, const uint8_t *expected, uint32_t len) {
    set_phase(V2_PHASE_VERIFY);
    /* 4 KB scratch buffer at the start of SRAM — reused across the driver
     * for single-sector readback. Host must not place data there while a
     * verify is in progress. */
    uint8_t *readback = (uint8_t *)0x00000000u;
    uint32_t remaining = len;
    uint32_t off = 0;
    while (remaining) {
        uint32_t chunk = remaining > V2_SECTOR_SIZE ? V2_SECTOR_SIZE : remaining;
        if (do_read(spi_addr + off, readback, chunk) != 0)
            return -1;
        for (uint32_t i = 0; i < chunk; i++) {
            if (readback[i] != expected[off + i]) {
                uint8_t exp_b = expected[off + i];
                uint8_t got_b = readback[i];
                log_put(V2_EVT_VERIFY_MISS, (uint16_t)(off + i),
                        spi_addr + off + i);
                MBOX->last_sr = ((uint32_t)exp_b << 8) | got_b;
                set_error(V2_ERR_VERIFY_MISMATCH, spi_addr + off + i, got_b);
                return -1;
            }
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
    if (do_verify(spi_addr, data, len) != 0) return -1;
    return 0;
}

/* ── Command dispatch ─────────────────────────────────────── */

static void handle_command(uint32_t cmd) {
    log_put(V2_EVT_CMD_RX, (uint16_t)cmd, MBOX->flash_addr);
    MBOX->status = V2_STATUS_BUSY;
    MBOX->err_code = V2_ERR_NONE;
    MBOX->err_spi_addr = 0;
    MBOX->phase_detail = 0;
    MBOX->pair_retries = 0;
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
    MBOX->build_tag    = 0x56324652u;   /* 'V2FR' marker */
    init_timings_if_needed();
    /* Latch the AAI transport backend from the timings struct. The
     * default is DMA; host may set xport_mode=V2_XPORT_PIO before
     * issuing the first command. */
    XPORT = (TIM->xport_mode == V2_XPORT_PIO) ? &xport_pio : &xport_dma;
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
        XPORT = (TIM->xport_mode == V2_XPORT_PIO) ? &xport_pio : &xport_dma;
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
