/*
 * explore_spi_test.c — Bare-metal SPI behavior experiments for DICE3
 *
 * Tests:
 *   1. DMA re-trigger without full setup (can we skip SPI reconfiguration?)
 *   2. SPI_STAT timing after DMA completion
 *   3. RDSR polling reliability (10 consecutive reads after clean reset)
 *   4. PIO TX for 3-byte AAI subsequent pairs
 *   5. Timer characterization (calibrate 0xC2000004 counter)
 *
 * Results written to a struct at RESULT_BASE (0x2E000).
 * Host reads results via JTAG after completion.
 *
 * Build:
 *   arm-none-eabi-gcc -march=armv5te -marm -Os -nostdlib -ffreestanding \
 *     -Wl,--section-start=.text=0x2C000 -Wl,-e,_start \
 *     -o firmware/explore_spi_test.elf firmware/explore_spi_test.c
 *   arm-none-eabi-objcopy -O binary \
 *     firmware/explore_spi_test.elf firmware/explore_spi_test.bin
 */

#include <stdint.h>

/* ── Hardware registers ─────────────────────────────────────── */

#define SPI_BASE    0xCC000000
#define SPI_CTRL    (*(volatile uint32_t *)(SPI_BASE + 0x00))
#define SPI_LEN     (*(volatile uint32_t *)(SPI_BASE + 0x04))
#define SPI_EN      (*(volatile uint32_t *)(SPI_BASE + 0x08))
#define SPI_CS      (*(volatile uint32_t *)(SPI_BASE + 0x10))
#define SPI_CLK     (*(volatile uint32_t *)(SPI_BASE + 0x14))
#define SPI_CLRINT  (*(volatile uint32_t *)(SPI_BASE + 0x18))
#define SPI_STAT    (*(volatile uint32_t *)(SPI_BASE + 0x28))
#define SPI_DMAGO   (*(volatile uint32_t *)(SPI_BASE + 0x2C))
#define SPI_ERR     (*(volatile uint32_t *)(SPI_BASE + 0x34))
#define SPI_DMAMD   (*(volatile uint32_t *)(SPI_BASE + 0x4C))
#define SPI_DMACFG0 (*(volatile uint32_t *)(SPI_BASE + 0x50))
#define SPI_DMACFG1 (*(volatile uint32_t *)(SPI_BASE + 0x54))
#define SPI_DATA    (*(volatile uint32_t *)(SPI_BASE + 0x60))

#define DMA_BASE    0x80000000
#define DMA_EN      (*(volatile uint32_t *)(DMA_BASE + 0x08))
#define DMA_ICLR    (*(volatile uint32_t *)(DMA_BASE + 0x10))
#define DMA_ISTAT   (*(volatile uint32_t *)(DMA_BASE + 0x14))
#define DMA_CHREG(ch, off) (*(volatile uint32_t *)(DMA_BASE + 0x100 + (ch)*0x20 + (off)))
#define DMA_CHCLR(ch)      (*(volatile uint32_t *)(DMA_BASE + 0x30 + (ch)*4))

#define SPI_TX_PORT (SPI_BASE + 0x80)
#define SPI_RX_PORT (SPI_BASE + 0x70)

/* Timer — free-running down-counter */
#define TIMER_RELOAD (*(volatile uint32_t *)0xC2000000)
#define TIMER_COUNT  (*(volatile uint32_t *)0xC2000004)
#define TIMER_CTRL   (*(volatile uint32_t *)0xC2000008)
#define TIMER_COUNT2 (*(volatile uint32_t *)0xC2000024)  /* 32-bit counter */

/* ── Result structure ──────────────────────────────────────── */

struct results {
    volatile uint32_t magic;           /* 0x00: 0x54455354 = "TEST" when complete */
    volatile uint32_t test_mask;       /* 0x04: bitmask of tests run */

    /* Test 1: DMA re-trigger */
    volatile uint32_t t1_full_setup_ok;    /* 0x08: 1 if full dma_tx works */
    volatile uint32_t t1_retrigger_ok;     /* 0x0C: 1 if re-trigger (minimal setup) works */
    volatile uint32_t t1_full_cycles;      /* 0x10: timer cycles for full setup */
    volatile uint32_t t1_retrig_cycles;    /* 0x14: timer cycles for re-trigger */

    /* Test 2: SPI_STAT timing */
    volatile uint32_t t2_stat_after_dma;   /* 0x18: SPI_STAT immediately after DMA_ISTAT fires */
    volatile uint32_t t2_stat_poll_count;  /* 0x1C: iterations until SPI_STAT bit 0 clears */
    volatile uint32_t t2_dma_poll_count;   /* 0x20: iterations until DMA_ISTAT fires */

    /* Test 3: RDSR reliability */
    volatile uint32_t t3_reads_ok;     /* 0x24: count of successful RDSR reads (out of 10) */
    volatile uint32_t t3_values[10];   /* 0x28-0x4C: RDSR values from each read */
    volatile uint32_t t3_ff_count;     /* 0x50: count of 0xFF (failure) reads */

    /* Test 4: PIO AAI */
    volatile uint32_t t4_pio_aai_ok;   /* 0x54: 1 if PIO AAI subsequent pair worked */
    volatile uint32_t t4_verify_byte0; /* 0x58: readback of first written byte */
    volatile uint32_t t4_verify_byte1; /* 0x5C: readback of second written byte */

    /* Test 5: Timer calibration */
    volatile uint32_t t5_reload_val;   /* 0x60: TIMER_RELOAD value */
    volatile uint32_t t5_ctrl_val;     /* 0x64: TIMER_CTRL value */
    volatile uint32_t t5_100iter_delta;/* 0x68: timer delta for 100 empty loop iterations */
    volatile uint32_t t5_1000iter_delta;/* 0x6C: timer delta for 1000 iterations */
    volatile uint32_t t5_counter2_delta;/* 0x70: TIMER_COUNT2 delta for 1000 iterations */
    volatile uint32_t t5_dma_tx_3byte_cycles; /* 0x74: timer cycles for one 3-byte DMA TX */
    volatile uint32_t t5_pio_tx_1byte_cycles; /* 0x78: timer cycles for one 1-byte PIO TX */
};

#define RESULT ((struct results *)0x2E000)

/* Scratch buffers */
#define TX_BUF  ((volatile uint8_t *)0x2E100)
#define RX_BUF  ((volatile uint8_t *)0x2E110)
#define DATA_BUF ((uint8_t *)0x2B000)

/* ── Helpers ───────────────────────────────────────────────── */

static inline void dwb(void) {
    __asm__ volatile("mcr p15, 0, %0, c7, c10, 4" :: "r"(0) : "memory");
}

static void spi_reset(void) {
    SPI_EN = 0;
    SPI_CS = 0;
    SPI_DMAGO = 0;
    SPI_DMAMD = 0;
    SPI_CLK = 2;
    DMA_EN = 0;
    DMA_ICLR = 3;
    DMA_CHCLR(0) = 1;
    DMA_CHCLR(1) = 1;
    dwb();
    for (volatile int i = 0; i < 1000; i++) {
        if (!(SPI_STAT & 1)) break;
    }
}

static uint32_t timer_read(void) {
    return TIMER_COUNT;
}

/* Returns elapsed cycles (handles wrap for down-counter) */
static uint32_t timer_elapsed(uint32_t start, uint32_t end) {
    if (start >= end)
        return start - end;  /* normal down-count */
    else
        return start + (TIMER_RELOAD - end);  /* wrapped */
}

/* ── PIO TX ────────────────────────────────────────────────── */

static void pio_tx_begin(uint32_t nbytes) {
    while (SPI_STAT & 1) {}
    SPI_EN = 0; SPI_CS = 0;
    SPI_DMAMD = 0;
    SPI_CTRL = 0x107;
    SPI_LEN = nbytes - 1;
    SPI_CLK = 2;
    dwb();
    SPI_EN = 1;
    SPI_CS = 1;
    dwb();
}

static void pio_tx_byte(uint8_t b) {
    while (!(SPI_STAT & 2)) {}
    SPI_DATA = b;
}

static void pio_tx_end(void) {
    while (SPI_STAT & 1) {}
    SPI_CS = 0;
    SPI_EN = 0;
}

static void pio_cmd1(uint8_t cmd) {
    pio_tx_begin(1);
    pio_tx_byte(cmd);
    pio_tx_end();
}

/* ── DMA TX (full setup) ──────────────────────────────────── */

static int dma_tx_full(const volatile uint8_t *buf, uint32_t len) {
    dwb();
    while (SPI_STAT & 1) {}
    SPI_EN = 0; SPI_CS = 0; SPI_DMAGO = 0;
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
    DMA_CHREG(0, 0x0C) = len | 0xF4009000;
    dwb();
    DMA_CHREG(0, 0x10) = 0xD005;
    dwb();

    SPI_CS = 1;
    SPI_CLRINT = 0;
    dwb();

    for (volatile int i = 0; i < 500000; i++) {
        if (DMA_ISTAT & 1) {
            while (SPI_STAT & 1) {}
            SPI_DMAGO = 1;
            SPI_CS = 0;
            SPI_EN = 0;
            return 0;
        }
    }
    SPI_CS = 0; SPI_EN = 0;
    return -1;
}

/* ── DMA TX (minimal re-trigger: SPI already configured) ── */

static int dma_tx_retrigger(const volatile uint8_t *buf, uint32_t len) {
    dwb();

    /* Only update what changes: DMA source, count, and trigger.
     * SPI CTRL/CLK/DMAMD/DMACFG0/DMACFG1 stay from previous call.
     * We still need to: update LEN, deassert/reassert CS, clear DMA. */

    while (SPI_STAT & 1) {}
    SPI_CS = 0;
    SPI_DMAGO = 0;
    SPI_LEN = len - 1;
    dwb();

    /* Re-enable SPI (it was disabled at end of last tx) */
    SPI_EN = 1;
    dwb();

    /* DMA: clear interrupt, reset channel, program, arm */
    DMA_ICLR = 1;
    DMA_CHCLR(0) = 1;
    dwb();
    DMA_CHREG(0, 0x10) = 0;
    DMA_CHREG(0, 0x00) = (uint32_t)(uintptr_t)buf;
    DMA_CHREG(0, 0x04) = SPI_TX_PORT;
    DMA_CHREG(0, 0x08) = 0;
    DMA_CHREG(0, 0x0C) = len | 0xF4009000;
    dwb();
    DMA_CHREG(0, 0x10) = 0xD005;
    dwb();

    SPI_CS = 1;
    SPI_CLRINT = 0;
    dwb();

    for (volatile int i = 0; i < 500000; i++) {
        if (DMA_ISTAT & 1) {
            while (SPI_STAT & 1) {}
            SPI_DMAGO = 1;
            SPI_CS = 0;
            SPI_EN = 0;
            return 0;
        }
    }
    SPI_CS = 0; SPI_EN = 0;
    return -1;
}

/* Even more minimal: don't disable SPI_EN between transfers */
static int dma_tx_retrigger_noen(const volatile uint8_t *buf, uint32_t len) {
    dwb();
    while (SPI_STAT & 1) {}
    SPI_CS = 0;
    SPI_DMAGO = 0;
    SPI_LEN = len - 1;

    /* SPI_EN stays 1 — don't toggle it */

    DMA_ICLR = 1;
    DMA_CHCLR(0) = 1;
    dwb();
    DMA_CHREG(0, 0x10) = 0;
    DMA_CHREG(0, 0x00) = (uint32_t)(uintptr_t)buf;
    /* DST stays SPI_TX_PORT from last call */
    DMA_CHREG(0, 0x04) = SPI_TX_PORT;
    DMA_CHREG(0, 0x08) = 0;
    DMA_CHREG(0, 0x0C) = len | 0xF4009000;
    dwb();
    DMA_CHREG(0, 0x10) = 0xD005;
    dwb();

    SPI_CS = 1;
    SPI_CLRINT = 0;
    dwb();

    for (volatile int i = 0; i < 500000; i++) {
        if (DMA_ISTAT & 1) {
            while (SPI_STAT & 1) {}
            SPI_DMAGO = 1;
            SPI_CS = 0;
            /* Don't disable SPI_EN */
            return 0;
        }
    }
    SPI_CS = 0;
    return -1;
}

/* ── DMA RX ────────────────────────────────────────────────── */

static int dma_rx(uint32_t spi_cmd, uint8_t *buf, uint32_t len) {
    while (SPI_STAT & 1) {}
    SPI_EN = 0; SPI_CS = 0; SPI_DMAGO = 0;
    SPI_CTRL = 0x307;
    SPI_LEN = len - 1;
    SPI_DMAMD = 1;
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
    DMA_CHREG(0, 0x00) = SPI_RX_PORT;
    DMA_CHREG(0, 0x04) = (uint32_t)(uintptr_t)buf;
    DMA_CHREG(0, 0x0C) = (len & 0xFFF) | 0x88009000;
    dwb();
    DMA_CHREG(0, 0x10) = 0xD007;
    dwb();

    SPI_DATA = spi_cmd;
    SPI_CS = 1;
    dwb();

    for (volatile int i = 0; i < 500000; i++) {
        if (DMA_ISTAT & 1) {
            while (SPI_STAT & 1) {}
            SPI_CS = 0; SPI_EN = 0;
            return 0;
        }
    }
    SPI_CS = 0; SPI_EN = 0;
    return -1;
}

/* ── Test implementations ──────────────────────────────────── */

static void test1_dma_retrigger(void) {
    spi_reset();

    /* Send RDSR (0x05) as a harmless 1-byte command via DMA */
    TX_BUF[0] = 0x05;

    /* Full setup first */
    uint32_t t0 = timer_read();
    int ok = dma_tx_full(TX_BUF, 1);
    uint32_t t1 = timer_read();
    RESULT->t1_full_setup_ok = (ok == 0) ? 1 : 0;
    RESULT->t1_full_cycles = timer_elapsed(t0, t1);

    if (ok != 0) return;

    /* Now re-configure SPI for TX mode once */
    spi_reset();
    /* Do a first full setup to configure SPI */
    TX_BUF[0] = 0x05;
    dma_tx_full(TX_BUF, 1);

    /* Try re-trigger (minimal setup) */
    TX_BUF[0] = 0x05;
    t0 = timer_read();
    ok = dma_tx_retrigger(TX_BUF, 1);
    t1 = timer_read();
    RESULT->t1_retrigger_ok = (ok == 0) ? 1 : 0;
    RESULT->t1_retrig_cycles = timer_elapsed(t0, t1);

    /* Also try the no-EN variant */
    spi_reset();
    dma_tx_full(TX_BUF, 1);
    TX_BUF[0] = 0x05;
    ok = dma_tx_retrigger_noen(TX_BUF, 1);
    /* Store in retrigger_ok bit 1 */
    if (ok == 0)
        RESULT->t1_retrigger_ok |= 2;
}

static void test2_spi_stat_timing(void) {
    spi_reset();

    TX_BUF[0] = 0x05;  /* RDSR */

    /* Do a DMA TX and count how many iterations DMA_ISTAT takes */
    dwb();
    while (SPI_STAT & 1) {}
    SPI_EN = 0; SPI_CS = 0; SPI_DMAGO = 0;
    SPI_CTRL = 0x107;
    SPI_LEN = 0;  /* 1 byte */
    SPI_DMAMD = 2;
    SPI_CLK = 2;
    SPI_DMACFG0 = 4; SPI_DMACFG1 = 3;
    dwb();
    SPI_EN = 1; dwb();

    DMA_EN = 1; DMA_ICLR = 1; DMA_CHCLR(0) = 1; dwb();
    DMA_CHREG(0, 0x10) = 0;
    DMA_CHREG(0, 0x00) = (uint32_t)(uintptr_t)TX_BUF;
    DMA_CHREG(0, 0x04) = SPI_TX_PORT;
    DMA_CHREG(0, 0x08) = 0;
    DMA_CHREG(0, 0x0C) = 1 | 0xF4009000;
    dwb();
    DMA_CHREG(0, 0x10) = 0xD005; dwb();

    SPI_CS = 1; SPI_CLRINT = 0; dwb();

    /* Count DMA completion iterations */
    uint32_t dma_count = 0;
    for (volatile int i = 0; i < 500000; i++) {
        if (DMA_ISTAT & 1) { dma_count = (uint32_t)i; break; }
    }

    /* SPI_STAT right after DMA fires */
    RESULT->t2_stat_after_dma = SPI_STAT;

    /* Count SPI busy-wait iterations */
    uint32_t stat_count = 0;
    for (volatile int i = 0; i < 100000; i++) {
        if (!(SPI_STAT & 1)) { stat_count = (uint32_t)i; break; }
    }

    SPI_DMAGO = 1; SPI_CS = 0; SPI_EN = 0;

    RESULT->t2_dma_poll_count = dma_count;
    RESULT->t2_stat_poll_count = stat_count;
}

static void test3_rdsr_reliability(void) {
    spi_reset();

    uint32_t ok = 0;
    uint32_t ff = 0;

    for (int i = 0; i < 10; i++) {
        uint8_t val = 0xEE;  /* sentinel */
        int ret = dma_rx(0x05, (uint8_t *)RX_BUF, 1);
        if (ret == 0) {
            val = RX_BUF[0];
            if (val != 0xFF) ok++;
            else ff++;
        } else {
            val = 0xFE;  /* DMA timeout sentinel */
            ff++;
        }
        RESULT->t3_values[i] = val;
    }

    RESULT->t3_reads_ok = ok;
    RESULT->t3_ff_count = ff;
}

static void test4_pio_aai(void) {
    /* This test writes 2 bytes to an erased sector using PIO for the
     * subsequent AAI pair. We use sector 0x80000 (above firmware).
     *
     * Steps:
     *  1. Erase sector 0x80000
     *  2. WREN
     *  3. First AAI pair via DMA (6 bytes: 0xAD, addr, data0, data1)
     *  4. Wait tBP
     *  5. Second AAI pair via PIO (3 bytes: 0xAD, data2, data3)
     *  6. WRDI to exit AAI
     *  7. Read back and verify
     */
    spi_reset();

    /* BP clear (EWSR + WRSR) */
    pio_cmd1(0x50);  /* EWSR */
    TX_BUF[0] = 0x01; TX_BUF[1] = 0x00;
    dma_tx_full(TX_BUF, 2);
    for (volatile int i = 0; i < 1500000; i++) {}  /* 15ms-ish */

    /* Erase sector 0x80000 */
    pio_cmd1(0x06);  /* WREN */
    TX_BUF[0] = 0x20; TX_BUF[1] = 0x08; TX_BUF[2] = 0x00; TX_BUF[3] = 0x00;
    dma_tx_full(TX_BUF, 4);
    for (volatile int i = 0; i < 3000000; i++) {}  /* 30ms-ish */

    /* WREN for AAI */
    spi_reset();
    pio_cmd1(0x06);

    /* First AAI pair: DMA TX 6 bytes */
    TX_BUF[0] = 0xAD;
    TX_BUF[1] = 0x08;  /* addr = 0x80000 */
    TX_BUF[2] = 0x00;
    TX_BUF[3] = 0x00;
    TX_BUF[4] = 0xA5;  /* data byte 0 */
    TX_BUF[5] = 0x5A;  /* data byte 1 */
    dma_tx_full(TX_BUF, 6);
    for (volatile int i = 0; i < 100000; i++) {}  /* ~10us tBP */

    /* Second AAI pair: PIO TX 3 bytes */
    pio_tx_begin(3);
    pio_tx_byte(0xAD);
    pio_tx_byte(0xC3);  /* data byte 2 */
    pio_tx_byte(0x3C);  /* data byte 3 */
    pio_tx_end();
    for (volatile int i = 0; i < 100000; i++) {}  /* tBP */

    /* WRDI to exit AAI */
    pio_cmd1(0x04);
    for (volatile int i = 0; i < 100000; i++) {}

    /* Readback: use DMA bidir read to check 4 bytes at 0x80000 */
    /* Simpler: use DMA RX for RDSR first to check flash is idle */
    spi_reset();

    /* Read 4 bytes from XIP address 0x80000 via memcpy won't work
     * (XIP window is limited). Use DMA bidir read instead.
     * For simplicity, read via PIO: send READ cmd (0x03) + 3 addr bytes + read */

    /* Actually: use the XIP mapping. XIP maps SPI to CPU addresses.
     * BUT the XIP window is only ~2.3KB starting at 0x4F000.
     * 0x80000 is way outside that. Need DMA read.
     *
     * For now, just verify the first pair worked by checking byte 0,1.
     * Use a minimal DMA bidir read (same as dma_bidir_read in the driver).
     */

    /* Build TX buffer for READ command */
    TX_BUF[0] = 0x03;
    TX_BUF[1] = 0x08;  /* 0x80000 >> 16 */
    TX_BUF[2] = 0x00;
    TX_BUF[3] = 0x00;
    /* 4 dummy TX bytes for data phase */
    TX_BUF[4] = 0; TX_BUF[5] = 0; TX_BUF[6] = 0; TX_BUF[7] = 0;

    uint32_t total = 8;  /* 4 cmd + 4 data */

    while (SPI_STAT & 1) {}
    SPI_EN = 0; SPI_CS = 0; SPI_DMAGO = 0;
    SPI_CTRL = 0x007;  /* bidirectional */
    SPI_LEN = total - 1;
    SPI_DMAMD = 3;
    SPI_CLK = 2;
    SPI_DMACFG0 = 4; SPI_DMACFG1 = 3;
    dwb(); SPI_EN = 1; dwb();

    DMA_EN = 3; DMA_ICLR = 3;
    DMA_CHCLR(0) = 1; DMA_CHCLR(1) = 1; dwb();

    /* Ch0: RX */
    DMA_CHREG(0, 0x10) = 0;
    DMA_CHREG(0, 0x00) = SPI_RX_PORT;
    DMA_CHREG(0, 0x04) = (uint32_t)(uintptr_t)RX_BUF;
    DMA_CHREG(0, 0x08) = 0;
    DMA_CHREG(0, 0x0C) = (total & 0xFFF) | 0x88009000;
    dwb(); DMA_CHREG(0, 0x10) = 0xD007; dwb();

    /* Ch1: TX (oversized) */
    DMA_CHREG(1, 0x10) = 0;
    DMA_CHREG(1, 0x00) = (uint32_t)(uintptr_t)TX_BUF;
    DMA_CHREG(1, 0x04) = SPI_TX_PORT;
    DMA_CHREG(1, 0x08) = 0;
    DMA_CHREG(1, 0x0C) = 0xFFF | 0xF4009000;
    dwb(); DMA_CHREG(1, 0x10) = 0xD005; dwb();

    SPI_CS = 1; SPI_CLRINT = 0; dwb();

    for (volatile int i = 0; i < 1000000; i++) {
        if (DMA_ISTAT & 1) break;
    }
    while (SPI_STAT & 1) {}
    SPI_DMAGO = 1; SPI_CS = 0; SPI_EN = 0;
    DMA_CHCLR(1) = 1;

    /* RX_BUF[0..3] = cmd-phase garbage, [4..7] = flash data */
    RESULT->t4_verify_byte0 = RX_BUF[4];
    RESULT->t4_verify_byte1 = RX_BUF[5];
    /* Check if PIO AAI pair also worked */
    uint32_t byte2 = RX_BUF[6];
    uint32_t byte3 = RX_BUF[7];

    /* PIO AAI is OK if bytes 0-3 match what we wrote */
    RESULT->t4_pio_aai_ok = 0;
    if (RX_BUF[4] == 0xA5 && RX_BUF[5] == 0x5A) {
        /* First pair OK (DMA AAI) */
        RESULT->t4_pio_aai_ok |= 1;
    }
    if (byte2 == 0xC3 && byte3 == 0x3C) {
        /* Second pair OK (PIO AAI!) */
        RESULT->t4_pio_aai_ok |= 2;
    }
}

static void test5_timer(void) {
    RESULT->t5_reload_val = TIMER_RELOAD;
    RESULT->t5_ctrl_val = TIMER_CTRL;

    /* 100 empty loop iterations */
    uint32_t t0 = timer_read();
    for (volatile int i = 0; i < 100; i++) { __asm__ volatile(""); }
    uint32_t t1 = timer_read();
    RESULT->t5_100iter_delta = timer_elapsed(t0, t1);

    /* 1000 empty loop iterations */
    t0 = timer_read();
    for (volatile int i = 0; i < 1000; i++) { __asm__ volatile(""); }
    t1 = timer_read();
    RESULT->t5_1000iter_delta = timer_elapsed(t0, t1);

    /* 1000 iterations on the 32-bit counter */
    uint32_t c0 = TIMER_COUNT2;
    for (volatile int i = 0; i < 1000; i++) { __asm__ volatile(""); }
    uint32_t c1 = TIMER_COUNT2;
    /* Assume down-counting */
    if (c0 >= c1)
        RESULT->t5_counter2_delta = c0 - c1;
    else
        RESULT->t5_counter2_delta = c0 + (0xFFFFFFFF - c1);

    /* Time one 3-byte DMA TX (RDSR-like) */
    spi_reset();
    TX_BUF[0] = 0x05;
    t0 = timer_read();
    dma_tx_full(TX_BUF, 1);
    t1 = timer_read();
    RESULT->t5_dma_tx_3byte_cycles = timer_elapsed(t0, t1);

    /* Time one 1-byte PIO TX */
    spi_reset();
    t0 = timer_read();
    pio_cmd1(0x04);  /* WRDI — safe no-op */
    t1 = timer_read();
    RESULT->t5_pio_tx_1byte_cycles = timer_elapsed(t0, t1);
}

/* ── Main ──────────────────────────────────────────────────── */

void do_main(void) {
    struct results *r = RESULT;

    /* Clear results */
    uint32_t *p = (uint32_t *)r;
    for (int i = 0; i < (int)(sizeof(struct results) / 4); i++)
        p[i] = 0;

    r->magic = 0;
    r->test_mask = 0;

    /* Run all tests */
    test5_timer();       r->test_mask |= 0x10;
    test1_dma_retrigger(); r->test_mask |= 0x01;
    test2_spi_stat_timing(); r->test_mask |= 0x02;
    test3_rdsr_reliability(); r->test_mask |= 0x04;
    test4_pio_aai();     r->test_mask |= 0x08;

    /* Clean up */
    spi_reset();

    r->magic = 0x54455354;  /* "TEST" — signals completion */
}

void _start(void) __attribute__((naked, section(".text.entry")));
void _start(void) {
    __asm__ volatile(
        "msr cpsr_c, #0xd3\n"
        "mov r0, #0\n"
        "mcr p15, 0, r0, c7, c6, 0\n"    /* invalidate D-cache */
        "mcr p15, 0, r0, c7, c5, 0\n"    /* invalidate I-cache */
        "mrc p15, 0, r0, c1, c0, 0\n"
        "bic r0, r0, #5\n"               /* MMU off + D-cache off */
        "mcr p15, 0, r0, c1, c0, 0\n"
        "ldr sp, =0x2AFFC\n"
        "bl  do_main\n"
        "1: b 1b\n"
    );
}
