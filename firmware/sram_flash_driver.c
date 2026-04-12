/*
 * sram_flash_driver.c — Bare-metal SPI flash driver for full image restore
 *
 * Command-driven stub: host loads data + sets mailbox command via JTAG,
 * resumes CPU.  Stub executes command, sets status, returns to idle loop.
 *
 * Operations (mailbox.command):
 *   0 = NOP (idle)
 *   1 = ERASE  — sector erase at spi_addr, polls RDSR for completion
 *   2 = WRITE  — AAI word-program from DATA_BUF to spi_addr (4KB)
 *   3 = READ   — DMA RX from spi_addr into READBACK_BUF (4KB)
 *   4 = VERIFY — read + compare with DATA_BUF, report mismatches
 *   5 = BP_CLEAR — clear block protection (run once at start)
 *
 * Proven register sequences (2026-04-11 hardware exploration):
 *   PIO TX:  ≤4 byte commands AND 3-byte AAI subsequent pairs
 *   DMA TX:  >4 byte commands (AAI first pair = 6 bytes only)
 *   DMA RX:  RDSR (1 byte) + bulk READ (2KB chunks)
 *   RDSR:    polling reliable after clean spi_reset() (10/10 in test)
 *
 * SRAM layout (eCos stopped, full 512KB available):
 *   0x00000  Readback/verify buffer (4KB)
 *   0x01000  Sector list for CMD_FLASH_ALL (2KB, 256 entries)
 *   0x02000  Mailbox (32 bytes) + TX/RX scratch
 *   0x03000  This code (up to 8KB) + stack
 *   0x05000  Bulk image data (492KB, loaded by host for CMD_FLASH_ALL)
 *
 * Single-sector commands (CMD_ERASE/WRITE/VERIFY) still work:
 *   host loads 4KB to DATA_BUF (0x05000), sends command.
 *
 * CMD_FLASH_ALL:
 *   host packs all sector data contiguously at IMAGE_BASE (0x05000),
 *   builds sector_entry list at SECTOR_LIST (0x01000),
 *   sends CMD_FLASH_ALL. Driver iterates autonomously.
 *
 * Build:
 *   arm-none-eabi-gcc -march=armv5te -marm -Os -nostdlib -ffreestanding \
 *     -T firmware/sram_flash_driver.ld \
 *     -o firmware/sram_flash_driver.elf firmware/sram_flash_driver.c
 *   arm-none-eabi-objcopy -O binary \
 *     firmware/sram_flash_driver.elf firmware/sram_flash_driver.bin
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
#define SPI_DMAMD   (*(volatile uint32_t *)(SPI_BASE + 0x4C))
#define SPI_DMACFG0 (*(volatile uint32_t *)(SPI_BASE + 0x50))
#define SPI_DMACFG1 (*(volatile uint32_t *)(SPI_BASE + 0x54))
#define SPI_DATA    (*(volatile uint32_t *)(SPI_BASE + 0x60))

#define DMA_BASE    0x80000000
#define DMA_EN      (*(volatile uint32_t *)(DMA_BASE + 0x08))
#define DMA_ICLR    (*(volatile uint32_t *)(DMA_BASE + 0x10))
#define DMA_ISTAT   (*(volatile uint32_t *)(DMA_BASE + 0x14))

/* Per-channel registers at 0x80000100 + ch*0x20 */
#define DMA_CHREG(ch, off) (*(volatile uint32_t *)(DMA_BASE + 0x100 + (ch)*0x20 + (off)))
#define DMA_CHCLR(ch)      (*(volatile uint32_t *)(DMA_BASE + 0x30 + (ch)*4))

#define SPI_TX_PORT (SPI_BASE + 0x80)
#define SPI_RX_PORT (SPI_BASE + 0x70)  /* RXDMA port (0x60=DATA is for PIO/bootloader) */

/* ── SRAM layout ────────────────────────────────────────────── */

/* Original proven SRAM layout (single-sector mode):
 *   0x2B000  DATA_BUF (4KB sector data)
 *   0x2C000  Driver code (8KB)
 *   0x2E000  Mailbox + scratch
 *   0x2E100  READBACK_BUF (4KB)
 *
 * Bulk mode (CMD_FLASH_ALL) adds:
 *   0x2F100  SECTOR_LIST (2KB, up to 256 entries)
 *   0x2F900  IMAGE_BASE (bulk sector data, ~326KB to 0x7FFFF)
 */
#define DATA_BUF     ((uint8_t *)0x2B000)
#define READBACK_BUF ((uint8_t *)0x2E100)
#define SECTOR_LIST  ((struct sector_entry *)0x2F100)
#define IMAGE_BASE   ((const uint8_t *)0x2F900)
#define SECTOR_SIZE  0x1000
#define READ_CHUNK   0x800   /* 2KB — fits in DMA CFG 12-bit count */

struct mailbox {
    volatile uint32_t status;    /* 0=idle, 1=running, 2=done_ok, 0xFF=error */
    volatile uint32_t command;   /* 0=nop, 1=erase, 2=write, 3=read, 4=verify, 5=bp_clear, 6=flash_all */
    volatile uint32_t spi_addr;
    volatile uint32_t progress;  /* sector index (flash_all) or bytes (single) */
    volatile uint32_t errors;    /* error/mismatch count */
    volatile uint32_t last_sr;   /* last RDSR value (debug) */
    volatile uint32_t total;     /* total sectors (flash_all) */
    volatile uint32_t reserved;
};
#define MBOX ((struct mailbox *)0x2E000)

#define TX_SCRATCH ((volatile uint8_t *)0x2E020)
#define RX_SCRATCH ((volatile uint8_t *)0x2E030)

struct sector_entry {
    uint32_t spi_addr;     /* SPI flash address (sector-aligned) */
    uint32_t sram_offset;  /* byte offset into IMAGE_BASE */
};

/* ── Write barrier (ARM926EJ-S has no DMB) ──────────────────── */

static inline void dwb(void) {
    __asm__ volatile("mcr p15, 0, %0, c7, c10, 4" :: "r"(0) : "memory");
}

/* ── Hardware timer (PLL-independent) ──────────────────────── */
/*
 * Timer0 at 0xC2000000: down-counting, reload=500000.
 * ~11 timer cycles per loop iteration at 196MHz → ~18MHz timer clock.
 * Used for timeout guards only — actual waits use RDSR polling.
 */
#define TIMER_COUNT  (*(volatile uint32_t *)0xC2000004)
#define TIMER_RELOAD (*(volatile uint32_t *)0xC2000000)

static uint32_t timer_read(void) {
    return TIMER_COUNT;
}

static uint32_t timer_elapsed(uint32_t start, uint32_t end) {
    uint32_t reload = TIMER_RELOAD;
    if (start >= end)
        return start - end;
    return start + (reload - end);
}

/* ── PIO TX helpers (≤4 bytes only!) ────────────────────────── */

static void spi_off(void) {
    SPI_EN = 0;
    SPI_CS = 0;
}

/* Hard reset SPI + DMA to a known clean state */
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
    /* Brief spin in case SPI needs time to deassert busy */
    for (volatile int i = 0; i < 1000; i++) {
        if (!(SPI_STAT & 1)) break;
    }
}

static void pio_tx_begin(uint32_t nbytes) {
    while (SPI_STAT & 1) {}
    spi_off();
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

/* Forward declaration — dma_tx defined below */
static int dma_tx(const volatile uint8_t *buf, uint32_t len);

/* Short SPI commands via DMA TX (replaces PIO which doesn't work in
 * TCAT-BOOT loader context — SPI_STAT stuck at 0x6 for PIO). */
static void pio_cmd1(uint8_t cmd) {
    TX_SCRATCH[0] = cmd;
    dma_tx(TX_SCRATCH, 1);
}

static void pio_cmd2(uint8_t c1, uint8_t c2) {
    TX_SCRATCH[0] = c1;
    TX_SCRATCH[1] = c2;
    dma_tx(TX_SCRATCH, 2);
}

static void pio_cmd3(uint8_t c1, uint8_t c2, uint8_t c3) {
    TX_SCRATCH[0] = c1;
    TX_SCRATCH[1] = c2;
    TX_SCRATCH[2] = c3;
    dma_tx(TX_SCRATCH, 3);
}

static void pio_cmd4(uint8_t c1, uint8_t c2, uint8_t c3, uint8_t c4) {
    TX_SCRATCH[0] = c1;
    TX_SCRATCH[1] = c2;
    TX_SCRATCH[2] = c3;
    TX_SCRATCH[3] = c4;
    dma_tx(TX_SCRATCH, 4);
}

/* ── DMA TX (>4 bytes) ──────────────────────────────────────── */
/*
 * Correct ordering (Ghidra FUN_0000f384 + FUN_000091f0):
 *   1. SPI config + enable (CS deasserted)
 *   2. DMA: clear → program → arm trigger (before CS!)
 *   3. Assert CS → SPI requests DMA data → DMA responds
 *   4. CLRINT, poll completion
 */
static int dma_tx(const volatile uint8_t *buf, uint32_t len) {
    dwb();

    while (SPI_STAT & 1) {}
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
    DMA_CHREG(0, 0x10) = 0;           /* TRG = 0 */
    DMA_CHREG(0, 0x00) = (uint32_t)(uintptr_t)buf;  /* SRC */
    DMA_CHREG(0, 0x04) = SPI_TX_PORT; /* DST */
    DMA_CHREG(0, 0x08) = 0;           /* NXT */
    DMA_CHREG(0, 0x0C) = len | 0xF4009000;  /* CFG */
    dwb();
    DMA_CHREG(0, 0x10) = 0xD005;      /* TRG */
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

    SPI_CS = 0;
    SPI_EN = 0;
    return -1;
}

/* ── DMA RX (1-byte commands: RDSR, JEDEC ID) ──────────────── */
/*
 * Bidirectional DMA (DMAMD=3): ch0=RX, ch1=TX.
 * PIO via SPI_DATA doesn't work in TCAT-BOOT mode, so the command
 * byte must also come from DMA. TX_SCRATCH holds [cmd, 0x00...]. */
static int dma_rx(uint32_t spi_cmd, uint8_t *buf, uint32_t len) {
    uint32_t total = 1 + len;  /* 1 cmd byte + len response bytes */

    /* Build TX buffer: command + dummy bytes */
    TX_SCRATCH[0] = (uint8_t)spi_cmd;
    for (uint32_t i = 1; i < total && i < 16; i++)
        TX_SCRATCH[i] = 0;

    while (SPI_STAT & 1) {}
    SPI_EN = 0;
    SPI_CS = 0;
    SPI_DMAGO = 0;

    SPI_CTRL = 0x007;          /* bidirectional (direction 3) */
    SPI_LEN = total - 1;
    SPI_DMAMD = 3;             /* bidirectional DMA */
    SPI_CLK = 2;
    SPI_DMACFG0 = 4;
    SPI_DMACFG1 = 3;
    dwb();
    SPI_EN = 1;
    dwb();

    /* ch0 = RX: SPI_RX_PORT → RX_SCRATCH (need total bytes, skip first) */
    DMA_EN = 3;
    DMA_ICLR = 3;
    DMA_CHCLR(0) = 1;
    DMA_CHCLR(1) = 1;
    dwb();

    DMA_CHREG(0, 0x10) = 0;
    DMA_CHREG(0, 0x00) = SPI_RX_PORT;
    DMA_CHREG(0, 0x04) = (uint32_t)(uintptr_t)RX_SCRATCH;
    DMA_CHREG(0, 0x08) = 0;
    DMA_CHREG(0, 0x0C) = (total & 0xFFF) | 0x88009000;
    dwb();
    DMA_CHREG(0, 0x10) = 0xD007;

    /* ch1 = TX: TX_SCRATCH → SPI_TX_PORT */
    DMA_CHREG(1, 0x10) = 0;
    DMA_CHREG(1, 0x00) = (uint32_t)(uintptr_t)TX_SCRATCH;
    DMA_CHREG(1, 0x04) = SPI_TX_PORT;
    DMA_CHREG(1, 0x08) = 0;
    DMA_CHREG(1, 0x0C) = 0xFFF | 0xF4009000;  /* max count */
    dwb();
    DMA_CHREG(1, 0x10) = 0xD005;
    dwb();

    SPI_CS = 1;
    SPI_CLRINT = 0;
    dwb();

    for (volatile int i = 0; i < 500000; i++) {
        if (DMA_ISTAT & 1) {
            SPI_DMAGO = 1;            /* signal SPI that DMA is done */
            while (SPI_STAT & 1) {}   /* now wait for SPI idle */
            SPI_CS = 0;
            SPI_EN = 0;
            DMA_EN = 0;
            DMA_ICLR = 3;
            /* Copy response (skip first byte which was during cmd TX) */
            for (uint32_t j = 0; j < len; j++)
                buf[j] = ((volatile uint8_t *)RX_SCRATCH)[1 + j];
            return 0;
        }
    }

    SPI_CS = 0;
    SPI_EN = 0;
    DMA_EN = 0;
    return -1;
}

/* ── Bidirectional DMA flash read (Ghidra FUN_0000f384, direction 3) ── */
/*
 * Full-duplex SPI transfer using TWO DMA channels:
 *   ch0 = TX: sends cmd+addr+dummy from tx_buf to SPI_TX_PORT
 *   ch1 = RX: captures response from SPI_DATA to rx_buf
 *
 * Firmware uses DMAMD=3 (bidirectional) and arms both channels
 * before CS assert (FUN_0000f384 case 3/4, FUN_000091f0 per channel).
 *
 * TX buf: [0x03, A23, A15, A7, 0x00...] (4 cmd + data_len dummy)
 * RX buf: [garbage×4, data×data_len]     (skip first 4 bytes)
 * Total transfer length = 4 + data_len
 */
#define READ_TX_BUF  ((volatile uint8_t *)0x2E040) /* TX cmd buffer for bidir read */
#define RX_TEMP      ((uint8_t *)0x2A000)          /* temp RX buffer (2KB+4, below DATA_BUF) */
#define RX_OVERHEAD  4  /* 4 cmd-phase garbage before flash data */

static int dma_bidir_read(uint32_t spi_addr, uint8_t *buf, uint32_t data_len) {
    uint32_t total = RX_OVERHEAD + data_len;

    /* Build TX buffer: READ command + dummy bytes */
    READ_TX_BUF[0] = 0x03;
    READ_TX_BUF[1] = (spi_addr >> 16) & 0xFF;
    READ_TX_BUF[2] = (spi_addr >> 8) & 0xFF;
    READ_TX_BUF[3] = spi_addr & 0xFF;
    dwb();

    /*
     * Bidirectional DMA (DMAMD=3) with oversized TX count.
     *
     * Problem: TX DMA finishes before all bytes are clocked (FIFO buffering),
     * which causes the SPI controller to stop and RX stalls.
     *
     * Fix: set TX DMA count to max (0xFFF=4095) so TX never "completes"
     * during the transfer. SPI_LEN controls the actual clock count.
     * After LEN+1 clocks, the SPI stops, RX DMA has all data.
     * TX DMA will have a partial transfer (not completed) — that's fine.
     */
    while (SPI_STAT & 1) {}
    SPI_EN = 0;
    SPI_CS = 0;
    SPI_DMAGO = 0;
    SPI_CTRL = 0x007;          /* bidirectional (firmware direction 3) */
    SPI_LEN = total - 1;
    SPI_DMAMD = 3;             /* bidirectional DMA */
    SPI_CLK = 2;
    SPI_DMACFG0 = 4;
    SPI_DMACFG1 = 3;
    dwb();
    SPI_EN = 1;
    dwb();

    /* DMA ch0: RX */
    DMA_EN = 3;
    DMA_ICLR = 3;
    DMA_CHCLR(0) = 1;
    DMA_CHCLR(1) = 1;
    dwb();

    DMA_CHREG(0, 0x10) = 0;
    DMA_CHREG(0, 0x00) = SPI_RX_PORT;
    DMA_CHREG(0, 0x04) = (uint32_t)(uintptr_t)RX_TEMP;  /* write to temp buffer */
    DMA_CHREG(0, 0x08) = 0;
    DMA_CHREG(0, 0x0C) = (total & 0xFFF) | 0x88009000;
    dwb();
    DMA_CHREG(0, 0x10) = 0xD007;
    dwb();

    /* DMA ch1: TX — oversized count (0xFFF) so it NEVER completes */
    DMA_CHREG(1, 0x10) = 0;
    DMA_CHREG(1, 0x00) = (uint32_t)(uintptr_t)READ_TX_BUF;
    DMA_CHREG(1, 0x04) = SPI_TX_PORT;
    DMA_CHREG(1, 0x08) = 0;
    DMA_CHREG(1, 0x0C) = 0xFFF | 0xF4009000;  /* max count = 4095 */
    dwb();
    DMA_CHREG(1, 0x10) = 0xD005;
    dwb();

    SPI_CS = 1;
    SPI_CLRINT = 0;
    dwb();

    /* Poll RX DMA only (ch0 bit 0). TX won't complete — that's intentional. */
    for (volatile int i = 0; i < 1000000; i++) {
        if (DMA_ISTAT & 1) {
            while (SPI_STAT & 1) {}
            SPI_DMAGO = 1;
            SPI_CS = 0;
            SPI_EN = 0;
            DMA_CHCLR(1) = 1;
            return 0;
        }
    }

    SPI_CS = 0;
    SPI_EN = 0;
    DMA_CHCLR(1) = 1;
    return -1;
}

/* ── RDSR polling (hardware-exploration confirmed reliable) ─── */

static uint8_t rdsr(void) {
    if (dma_rx(0x05, (uint8_t *)RX_SCRATCH, 1) == 0)
        return RX_SCRATCH[0];
    return 0xFF;
}

/*
 * Poll RDSR until BUSY (bit 0) clears.
 * Uses hardware timer for timeout (PLL-independent).
 * timeout_cycles: timer cycles to wait (TIMER_RELOAD=500000 ≈ 28ms).
 */
static int wait_ready(uint32_t timeout_cycles) {
    uint32_t t0 = timer_read();

    for (;;) {
        uint8_t sr = rdsr();
        if (sr != 0xFF) {
            MBOX->last_sr = sr;
            if (!(sr & 1))
                return 0;  /* flash ready */
        }

        uint32_t elapsed = timer_elapsed(t0, timer_read());
        if (elapsed > timeout_cycles) {
            MBOX->last_sr = 0xDEAD;
            return -1;  /* timeout */
        }
    }
}

/* Convenience: timeout in timer reload units (1 unit ≈ 28ms) */
#define TIMER_MS(ms) ((uint32_t)((ms) * (500000 / 28)))

/* ── Command handlers ───────────────────────────────────────── */

static void do_bp_clear(void) {
    spi_reset();

    /* EWSR (0x50) + WRSR (0x01, 0x00) — clear all block protection bits.
     * SST25VF016B requires EWSR (not WREN!) to enable WRSR writes. */
    pio_cmd1(0x50);              /* EWSR */
    pio_cmd2(0x01, 0x00);       /* WRSR: clear all BP bits */
    wait_ready(TIMER_RELOAD);    /* tWRSR max 10ms → poll RDSR */

    /* Verify: read SR and check BP bits (mask 0x3C = BP3:BP0) */
    uint8_t sr = rdsr();
    MBOX->last_sr = sr;
    if (sr & 0x3C) {
        /* First attempt failed — retry with WREN as fallback */
        pio_cmd1(0x06);          /* WREN */
        pio_cmd2(0x01, 0x00);   /* WRSR */
        wait_ready(TIMER_RELOAD);
        sr = rdsr();
        MBOX->last_sr = sr;
    }
}

static int do_erase(uint32_t spi_addr) {
    spi_reset();
    pio_cmd1(0x06);  /* WREN */

    /* SE (0x20) — 4 bytes, fits in PIO */
    pio_cmd4(0x20,
             (spi_addr >> 16) & 0xFF,
             (spi_addr >> 8) & 0xFF,
             spi_addr & 0xFF);

    /* Poll RDSR for erase completion (tSE max 25ms) */
    return wait_ready(TIMER_RELOAD);  /* ~28ms timeout */
}

/*
 * AAI Word-Program: writes full 4KB sector from DATA_BUF.
 *
 * SST25VF016B AAI protocol (DS20005044C, Figure 4-9):
 *   - WREN (0x06)
 *   - First pair:  CE# low → [0xAD, A23, A15, A7, D0, D1] → CE# high → wait tBP
 *   - Subsequent:  CE# low → [0xAD, Dn, Dn+1] → CE# high → wait tBP
 *   - Terminate:   CE# low → WRDI (0x04) → CE# high
 *
 * CE# rising edge commits each pair to the internal programming array.
 * tBP = 7µs typical (page 1). Between pairs, CE# MUST go high for ≥ tBP.
 *
 * Optimized (2026-04-11):
 *   - First pair: DMA TX 6 bytes (>4 byte PIO limit)
 *   - Subsequent 2047 pairs: PIO TX 3 bytes each — NO DMA overhead
 *   - tBP wait: RDSR polling (flash signals ready via SR bit 0)
 *   - Hardware exploration confirmed PIO AAI works on 0xCC controller
 */
static int do_write(uint32_t spi_addr) {
    spi_reset();
    pio_cmd1(0x06);  /* WREN */

    /* First AAI pair — 6 bytes, needs DMA TX (>4 byte PIO limit) */
    TX_SCRATCH[0] = 0xAD;
    TX_SCRATCH[1] = (spi_addr >> 16) & 0xFF;
    TX_SCRATCH[2] = (spi_addr >> 8) & 0xFF;
    TX_SCRATCH[3] = spi_addr & 0xFF;
    TX_SCRATCH[4] = DATA_BUF[0];
    TX_SCRATCH[5] = DATA_BUF[1];

    if (dma_tx(TX_SCRATCH, 6) != 0) {
        MBOX->errors = 0xDA01;
        pio_cmd1(0x04);  /* WRDI */
        return -1;
    }

    /* Wait tBP via RDSR polling. In AAI mode, only RDSR (0x05) and
     * WRDI (0x04) are accepted. RDSR returns SR with BUSY=1 during
     * programming and BUSY=0 when ready for next pair. */
    if (wait_ready(TIMER_RELOAD / 10) != 0) {
        pio_cmd1(0x04);
        MBOX->errors = 0xDA02;
        return -1;
    }
    MBOX->progress = 2;

    /* Subsequent AAI pairs — 3 bytes each via PIO TX.
     * PIO confirmed working for AAI on this controller (2026-04-11).
     * After each pair, poll RDSR for tBP completion (7µs typical). */
    for (uint32_t i = 2; i < SECTOR_SIZE; i += 2) {
        pio_cmd3(0xAD, DATA_BUF[i], DATA_BUF[i + 1]);

        /* Poll RDSR for tBP (7µs typ, 10µs max).
         * Timeout is generous — if this fails something is very wrong. */
        if (wait_ready(TIMER_RELOAD / 10) != 0) {
            MBOX->errors = i;
            pio_cmd1(0x04);
            return -1;
        }

        if ((i & 0xFF) == 0)
            MBOX->progress = i + 2;
    }

    /* Exit AAI mode */
    pio_cmd1(0x04);  /* WRDI */

    return 0;
}

/*
 * Read sector via bidirectional DMA in chunks.
 * Each chunk reads into RX_TEMP (which includes 4-byte garbage prefix),
 * then copies data portion to READBACK_BUF.
 */
static int do_read(uint32_t spi_addr) {
    /* Flush SPI RX pipeline: do a dummy 1-byte RDSR to drain stale data.
     * Without this, subsequent chunks get 4 extra stale bytes in the RX path. */
    dma_rx(0x05, (uint8_t *)RX_SCRATCH, 1);

    for (uint32_t off = 0; off < SECTOR_SIZE; off += READ_CHUNK) {
        /* Flush between chunks too */
        if (off > 0)
            dma_rx(0x05, (uint8_t *)RX_SCRATCH, 1);

        if (dma_bidir_read(spi_addr + off, RX_TEMP, READ_CHUNK) != 0) {
            MBOX->errors = off;
            return -1;
        }
        /* Copy data from RX_TEMP (skip 4-byte cmd-phase garbage) */
        for (uint32_t i = 0; i < READ_CHUNK; i++)
            READBACK_BUF[off + i] = RX_TEMP[RX_OVERHEAD + i];
        MBOX->progress = off + READ_CHUNK;
    }
    return 0;
}

static int do_verify(uint32_t spi_addr) {
    if (do_read(spi_addr) != 0)
        return -1;

    uint32_t mismatches = 0;
    for (uint32_t i = 0; i < SECTOR_SIZE; i++) {
        if (READBACK_BUF[i] != DATA_BUF[i])
            mismatches++;
    }
    MBOX->errors = mismatches;
    return (mismatches == 0) ? 0 : -1;
}

/* ── Autonomous flash-all ──────────────────────────────────── */
/*
 * CMD_FLASH_ALL: iterate sector list, erase+write+verify each.
 * Host pre-loads image data at IMAGE_BASE and sector list at SECTOR_LIST.
 * Driver runs autonomously — host just polls mb->progress.
 */
static void do_flash_all(void) {
    struct sector_entry *list = SECTOR_LIST;
    const uint8_t *image = IMAGE_BASE;

    /* Count sectors for progress reporting */
    uint32_t total = 0;
    while (list[total].spi_addr != 0xFFFFFFFF && total < 256)
        total++;
    MBOX->total = total;

    /* Clear block protection once */
    do_bp_clear();

    for (uint32_t i = 0; i < total; i++) {
        uint32_t addr = list[i].spi_addr;
        const uint8_t *src = image + list[i].sram_offset;

        /* Copy sector to DATA_BUF (write/verify functions read from there) */
        for (uint32_t j = 0; j < SECTOR_SIZE; j++)
            DATA_BUF[j] = src[j];

        MBOX->progress = i;

        /* Erase */
        if (do_erase(addr) != 0) {
            MBOX->errors++;
            continue;
        }

        /* Write */
        if (do_write(addr) != 0) {
            MBOX->errors++;
            continue;
        }

        /* Verify (on-device: reads flash, compares against DATA_BUF) */
        if (do_verify(addr) != 0) {
            MBOX->errors++;
            continue;
        }
    }

    MBOX->progress = total;
}

/* ── SPI/DMA cleanup for soft reboot ───────────────────────── */
/*
 * Restore SPI + DMA to power-on defaults so the ROM bootloader
 * (or TCAT XIP bootloader) can reinitialize from scratch.
 *
 * The ROM at 0x20000578 expects:
 *   - SPI controller idle (STAT bit 0 = 0)
 *   - DMA channels not active
 *   - SPI_CLK = 0x38 (XIP default) or will be overwritten by ROM
 *
 * The TCAT bootloader at 0x4F000 expects:
 *   - SPI_CLK = 2 (it sets this explicitly)
 *   - Everything else reinitialized by bootloader_init
 */
static void spi_restore_clean(void) {
    /* Wait for any in-flight SPI transfer to complete */
    for (volatile int i = 0; i < 100000; i++) {
        if (!(SPI_STAT & 1)) break;
    }

    /* Fully disable SPI */
    SPI_CS = 0;
    SPI_EN = 0;
    SPI_DMAGO = 0;
    SPI_DMAMD = 0;
    SPI_DMACFG0 = 0;
    SPI_DMACFG1 = 0;
    dwb();

    /* Reset DMA engine — disable all channels, clear interrupts */
    DMA_EN = 0;
    DMA_ICLR = 3;
    DMA_CHCLR(0) = 1;
    DMA_CHCLR(1) = 1;
    /* Clear channel registers to avoid stale config */
    DMA_CHREG(0, 0x10) = 0;  /* TRG = 0 */
    DMA_CHREG(0, 0x0C) = 0;  /* CFG = 0 */
    DMA_CHREG(1, 0x10) = 0;
    DMA_CHREG(1, 0x0C) = 0;
    dwb();

    /* Restore SPI to power-on / XIP-compatible state */
    SPI_CTRL = 1;             /* power-on default */
    SPI_LEN = 2;              /* power-on default */
    SPI_CLK = 0x38;           /* XIP mode (what TCAT bootloader sets) */
    dwb();
}

/* ── Main command loop ──────────────────────────────────────── */

void do_main(void) {
    struct mailbox *mb = MBOX;

    /* Full SPI/DMA reset — required for TCAT-BOOT mode where the
     * bootloader leaves the SPI controller in DMA-read state. */
    spi_reset();

    /* Exit AAI mode if flash is stuck in it (from a previous aborted write).
     * In AAI mode, only RDSR and WRDI are accepted. WRDI exits AAI.
     * Uses DMA TX (PIO doesn't work in TCAT-BOOT mode). */
    pio_cmd1(0x04);  /* WRDI — safe to send even if not in AAI mode */

    mb->status = 0;
    mb->command = 0;
    mb->progress = 0;
    mb->errors = 0;
    mb->last_sr = 0;

    for (;;) {
        uint32_t cmd = mb->command;
        if (cmd == 0)
            continue;

        mb->status = 1;  /* running */
        mb->progress = 0;
        mb->errors = 0;

        uint32_t addr = mb->spi_addr;
        int ret = 0;

        switch (cmd) {
        case 1: ret = do_erase(addr); break;
        case 2: ret = do_write(addr); break;
        case 3: ret = do_read(addr); break;
        case 4: ret = do_verify(addr); break;
        case 5: do_bp_clear(); break;
        case 6: do_flash_all(); break;
        case 7: spi_restore_clean(); break;
        default: ret = -1; break;
        }

        mb->command = 0;
        mb->status = (ret == 0) ? 2 : 0xFF;
    }
}

/* ── Entry point ────────────────────────────────────────────── */

void _start(void) __attribute__((naked, section(".text.entry")));
void _start(void) {
    __asm__ volatile(
        "msr cpsr_c, #0xd3\n"            /* SVC mode, IRQs off */
        "mov r0, #0\n"
        "mcr p15, 0, r0, c7, c6, 0\n"    /* invalidate D-cache */
        "mcr p15, 0, r0, c7, c5, 0\n"    /* invalidate I-cache */
        "mrc p15, 0, r0, c1, c0, 0\n"
        "bic r0, r0, #5\n"               /* MMU off + D-cache off (keep I-cache!) */
        "mcr p15, 0, r0, c1, c0, 0\n"
        "ldr sp, =0x2AFFC\n"             /* stack below data buffer */
        "bl  do_main\n"
        "1: b 1b\n"
    );
}
