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
    /* Pure fixed delay. RDSR polling between pairs turned out to
     * sometimes drop the flash out of AAI mode after ~73 pairs
     * (observed 2026-04-17). Datasheet tBP max is 10 µs; use 30 µs
     * (3× margin). Short AAI runs (< 128 pairs) with this scheme verified
     * clean; long runs still need the dummy-pair head workaround above. */
    busy_wait_us(30);
    if ((pair_idx & 0x3F) == 0)
        log_put(V2_EVT_AAI_BUSY_FAST, (uint16_t)pair_idx, 0);
    return 0;
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
    /* Datasheet tSE == tBE32 == tBE64 == 25 ms max; observed ~55 ms on
     * this part for 4 KB SE. Block erases typically same-order. Wait
     * 55 ms fixed then RDSR slack. Polling mid-busy adds dma_rx
     * overhead (~5 ms per failed sample), so the fixed-wait is both
     * faster and more predictable. */
    busy_wait_us(55000);
    if (wait_fixed_then_poll(0, 2000, 30000, &sr) != 0) {
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

static int do_aai_program(uint32_t spi_addr, const uint8_t *data, uint32_t len) {
    if (len < 2 || (len & 1)) {
        set_error(V2_ERR_BAD_LENGTH, spi_addr, 0);
        return -1;
    }
    /* AAI ignores A0 in hardware (writes land at addr & ~1 and addr | 1).
     * Require explicit even alignment so odd addresses don't silently
     * truncate. */
    if (spi_addr & 1u) {
        set_error(V2_ERR_BAD_ADDR, spi_addr, 0);
        return -1;
    }

    set_phase(V2_PHASE_AAI_WREN);
    spi_reset_clean();
    if (spi_cmd1(SST_CMD_WREN) != 0) {
        set_error(V2_ERR_AAI_WREN, spi_addr, 0);
        return -1;
    }
    /* Do NOT verify WEL via rdsr here — the dma_rx mode switch right
     * after a dma_tx is flaky on this controller (observed 2026-04-17:
     * 5 ms timeouts). If WREN didn't take, the first AAI pair will fail
     * and the post-program verify will catch any silent failure. */
    busy_wait_us(10);   /* tiny settle before the next TX */
    log_put(V2_EVT_AAI_WREN, 0, spi_addr);

    /* Silicon quirk (observed 2026-04-17): the first TWO AAI pairs after
     * a sector erase are silently dropped AND the internal address
     * pointer does NOT advance. Subsequent pairs then start writing at
     * the target address. Compensate by sending two dummy 0xFF pairs
     * first — 0xFF is a no-op on erased flash even if one happens to
     * commit, and if they drop (expected) the real data still lands at
     * the correct offset. Costs ~60 ms per sector. */
    set_phase(V2_PHASE_AAI_FIRST);
    TX_SCRATCH[0] = SST_CMD_AAI;
    TX_SCRATCH[1] = (spi_addr >> 16) & 0xFF;
    TX_SCRATCH[2] = (spi_addr >> 8) & 0xFF;
    TX_SCRATCH[3] = spi_addr & 0xFF;
    TX_SCRATCH[4] = 0xFF;   /* dummy */
    TX_SCRATCH[5] = 0xFF;   /* dummy */
    if (dma_tx(TX_SCRATCH, 6, 5000) != 0) {
        (void)spi_cmd1(SST_CMD_WRDI);
        set_error(V2_ERR_AAI_FIRST_TX, spi_addr, 0);
        return -1;
    }
    log_put(V2_EVT_AAI_FIRST, 0, spi_addr);
    if (aai_pair_wait(0) != 0) {
        (void)spi_cmd1(SST_CMD_WRDI);
        set_error(V2_ERR_AAI_PAIR_TIMEOUT, spi_addr, (uint8_t)MBOX->last_sr);
        return -1;
    }

    /* Second dummy pair — subsequent-pair form (3 bytes). */
    TX_SCRATCH[0] = SST_CMD_AAI;
    TX_SCRATCH[1] = 0xFF;
    TX_SCRATCH[2] = 0xFF;
    if (dma_tx(TX_SCRATCH, 3, 5000) != 0) {
        (void)spi_cmd1(SST_CMD_WRDI);
        set_error(V2_ERR_AAI_PAIR_TX, spi_addr, 0);
        return -1;
    }
    if (aai_pair_wait(1) != 0) {
        (void)spi_cmd1(SST_CMD_WRDI);
        set_error(V2_ERR_AAI_PAIR_TIMEOUT, spi_addr, (uint8_t)MBOX->last_sr);
        return -1;
    }

    set_phase(V2_PHASE_AAI_PAIR);
    /* Hot pair loop — SPI is already configured from the last dma_tx
     * (CTRL=0x107, LEN=2, DMAMD=2, CLK=2, DMACFG0=4, DMACFG1=3, EN=0
     * after dma_tx). Keep SPI_EN across pairs and only reprogram the
     * DMA channel + toggle CS. Drops the ~3 ms/pair overhead of full
     * SPI reconfig to ~microseconds.
     *
     * Three slots per pair live in TX_SCRATCH (volatile) so the DMA
     * channel's SRC pointer doesn't move; only the data bytes change. */
    SPI_EN = 0;
    SPI_CS = 0;
    SPI_DMAGO = 0;
    SPI_CTRL = 0x107;
    SPI_LEN = 2;                 /* 3 bytes on the wire per sub-pair */
    SPI_DMAMD = 2;
    SPI_CLK = 2;
    SPI_DMACFG0 = 4;
    SPI_DMACFG1 = 3;
    dwb();
    SPI_EN = 1;
    dwb();

    DMA_EN = 1;
    dwb();
    /* Preload the channel registers that don't change. SRC, CFG, TRG
     * get touched per pair; DST, NXT are set once. */
    DMA_CHREG(0, 0x04) = SPI_TX_PORT;
    DMA_CHREG(0, 0x08) = 0;
    dwb();

    TX_SCRATCH[0] = SST_CMD_AAI;       /* stays 0xAD for every pair */
    dwb();

    for (uint32_t i = 0; i < len; i += 2) {
        TX_SCRATCH[1] = data[i];
        TX_SCRATCH[2] = data[i + 1];
        dwb();

        DMA_ICLR = 1;
        DMA_CHCLR(0) = 1;
        DMA_CHREG(0, 0x10) = 0;
        DMA_CHREG(0, 0x00) = (uint32_t)(uintptr_t)TX_SCRATCH;
        DMA_CHREG(0, 0x0C) = 3u | DMA_CFG_TX;
        dwb();
        DMA_CHREG(0, 0x10) = DMA_TRG_TX;
        dwb();

        SPI_CS = 1;
        SPI_CLRINT = 0;
        dwb();

        /* Tight poll — at ~25 MHz CPU each iter is ~10 cycles, so
         * 20000 iters ≈ 8 ms which is well over the datasheet
         * combined transfer+tBP window. */
        int spun = 0;
        while (!(DMA_ISTAT & 1)) {
            if (++spun > 20000) {
                dma_abort();
                (void)spi_cmd1(SST_CMD_WRDI);
                set_error(V2_ERR_AAI_PAIR_TX, spi_addr + i, 0);
                return -1;
            }
        }
        while (SPI_STAT & SPI_STAT_BUSY) {
            if (++spun > 30000) {
                dma_abort();
                (void)spi_cmd1(SST_CMD_WRDI);
                set_error(V2_ERR_AAI_PAIR_TX, spi_addr + i, 0);
                return -1;
            }
        }
        SPI_CS = 0;
        dwb();

        /* Fixed tBP delay before the next pair. Datasheet max 10 µs;
         * 12 µs gives 20 % margin and shaves 37 ms off a 4 KB sector
         * vs 30 µs. */
        busy_wait_us(12);

        if ((i & 0x1FF) == 0) {
            MBOX->phase_detail = i >> 1;
            MBOX->seq++;
            log_put(V2_EVT_AAI_PAIR, (uint16_t)(i >> 1), spi_addr + i);
        }
    }

    /* Clean teardown of DMA + SPI now that the pair loop is done. */
    SPI_CS = 0;
    SPI_EN = 0;
    DMA_EN = 0;
    DMA_ICLR = 3;
    dwb();

    set_phase(V2_PHASE_AAI_WRDI);
    (void)spi_cmd1(SST_CMD_WRDI);
    (void)wait_not_busy(1000, (uint8_t *)0);
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

/* ── Command dispatch ─────────────────────────────────────── */

static void handle_command(uint32_t cmd) {
    log_put(V2_EVT_CMD_RX, (uint16_t)cmd, MBOX->flash_addr);
    MBOX->status = V2_STATUS_BUSY;
    MBOX->err_code = V2_ERR_NONE;
    MBOX->err_spi_addr = 0;
    MBOX->phase_detail = 0;
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
    dwb();

    driver_setup();

    MBOX->status = V2_STATUS_READY;
    set_phase(V2_PHASE_IDLE);

    for (;;) {
        if (MBOX->magic != V2_MAGIC_CMD) continue;
        uint32_t cmd = MBOX->command;
        /* Accept: clear magic (host re-arms by writing magic again). */
        MBOX->magic = 0;
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
