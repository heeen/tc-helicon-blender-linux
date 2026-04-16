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
#define TX_SCRATCH_MAX 16 /* bytes; contiguous through 0x2E02F */
#define RX_SCRATCH ((volatile uint8_t *)0x2E030)
#define SR_TRACE_IDX (*(volatile uint32_t *)0x2E07C)
#define SR_TRACE_BUF ((volatile uint32_t *)0x2E080)  /* 32 entries */

struct sector_entry {
    uint32_t spi_addr;     /* SPI flash address (sector-aligned) */
    uint32_t sram_offset;  /* byte offset into IMAGE_BASE */
};

/* SR trace event tags (high byte in trace word). */
#define EVT_BP_CLEAR_PRE   0x01
#define EVT_BP_CLEAR_POST  0x02
#define EVT_ERASE_WREN     0x10
#define EVT_ERASE_DONE     0x11
#define EVT_WRITE_WREN     0x20
#define EVT_WRITE_FIRST    0x21
#define EVT_WRITE_MID      0x22
#define EVT_WRITE_WRDI     0x23
#define EVT_WRITE_DONE     0x24
#define EVT_WRITE_PAIR_SR  0x25
#define EVT_WRITE_WEL_LOST 0x26
#define EVT_WRITE_PREPAIR  0x27
#define EVT_WRITE_POSTPAIR 0x28
#define EVT_WRITE_WRDI_RETRY 0x29
#define EVT_WRITE_FAIL_SR  0x2A

static void trace_sr(uint8_t evt, uint8_t sr) {
    uint32_t idx = SR_TRACE_IDX & 0x1F;
    SR_TRACE_BUF[idx] = ((uint32_t)evt << 24) | sr;
    SR_TRACE_IDX = (idx + 1) & 0x1F;
}

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

/* Timer-based AAI inter-pair delay.
 * Tune near TBP (10us max) with a small safety margin. */
static inline void aai_pair_delay(uint32_t ticks) {
    uint32_t t0 = timer_read();
    while (timer_elapsed(t0, timer_read()) < ticks) {}
}

/* ── PIO TX helpers (≤4 bytes only!) ────────────────────────── */

static void spi_off(void) {
    SPI_EN = 0;
    SPI_CS = 0;
}

/*
 * Stop DMA and SPI after a failed or abandoned transfer.
 * Must match teardown on success paths so the next dma_tx/dma_rx cannot inherit
 * half-armed channels.
 */
static void dma_abort_inflight(void) {
    SPI_DMAGO = 0;
    DMA_EN = 0;
    DMA_ICLR = 3;
    DMA_CHCLR(0) = 1;
    DMA_CHCLR(1) = 1;
    SPI_CS = 0;
    SPI_EN = 0;
    dwb();
}

/* Hard reset SPI + DMA to a known clean state */
static void spi_reset(void) {
    SPI_EN = 0;
    SPI_CS = 0;
    SPI_DMAGO = 0;
    SPI_DMAMD = 0;
    SPI_CTRL = 0x107;   /* TX mode — power-on-like default */
    SPI_LEN = 0;
    SPI_DATA = 0;       /* clear stale cmd/addr from bootloader XIP reads */
    SPI_CLK = 2;
    SPI_CLRINT = 0;
    SPI_DMACFG0 = 0;
    SPI_DMACFG1 = 0;
    DMA_EN = 0;
    DMA_ICLR = 3;
    DMA_CHCLR(0) = 1;
    DMA_CHCLR(1) = 1;
    /* Zero out DMA channel registers to prevent stale config */
    DMA_CHREG(0, 0x0C) = 0;  /* ch0 CFG */
    DMA_CHREG(0, 0x10) = 0;  /* ch0 TRG */
    DMA_CHREG(1, 0x0C) = 0;  /* ch1 CFG */
    DMA_CHREG(1, 0x10) = 0;  /* ch1 TRG */
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
static int pio_cmd1(uint8_t cmd) {
    TX_SCRATCH[0] = cmd;
    return dma_tx(TX_SCRATCH, 1);
}

static int pio_cmd2(uint8_t c1, uint8_t c2) {
    TX_SCRATCH[0] = c1;
    TX_SCRATCH[1] = c2;
    return dma_tx(TX_SCRATCH, 2);
}

static int pio_cmd3(uint8_t c1, uint8_t c2, uint8_t c3) {
    TX_SCRATCH[0] = c1;
    TX_SCRATCH[1] = c2;
    TX_SCRATCH[2] = c3;
    return dma_tx(TX_SCRATCH, 3);
}

static int pio_cmd4(uint8_t c1, uint8_t c2, uint8_t c3, uint8_t c4) {
    TX_SCRATCH[0] = c1;
    TX_SCRATCH[1] = c2;
    TX_SCRATCH[2] = c3;
    TX_SCRATCH[3] = c4;
    return dma_tx(TX_SCRATCH, 4);
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
            SPI_DMAGO = 1;
            while (SPI_STAT & 1) {}
            SPI_CS = 0;
            SPI_EN = 0;
            DMA_EN = 0;
            DMA_ICLR = 3;
            return 0;
        }
    }

    dma_abort_inflight();
    return -1;
}

/* ── DMA RX (1-byte commands: RDSR, JEDEC ID) ──────────────── */
/*
 * Bidirectional DMA (DMAMD=3): ch0=RX, ch1=TX.
 * PIO via SPI_DATA doesn't work in TCAT-BOOT mode, so the command
 * byte must also come from DMA. TX_SCRATCH holds [cmd, 0x00...]. */
static int dma_rx(uint32_t spi_cmd, uint8_t *buf, uint32_t len) {
    uint32_t total = 1 + len;  /* 1 cmd byte + len response bytes */

    if (total > TX_SCRATCH_MAX || len == 0)
        return -1;

    /* Build TX buffer: command + dummy bytes */
    TX_SCRATCH[0] = (uint8_t)spi_cmd;
    for (uint32_t i = 1; i < total; i++)
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

    dma_abort_inflight();
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
 * xfer_len = READ_RX_SKIP + data_len (see READ_RX_SKIP: cmd/addr + pipeline + dummies).
 * RX: discard first READ_RX_SKIP bytes, then flash payload.
 */
/*
 * Bidirectional READ TX must supply one byte per SPI clock for the whole
 * transfer (cmd + addr + dummies).  Only initializing [0..3] leaves DMA
 * sourcing garbage for the data phase → MISO often reads as 0xFF.
 * Use a dedicated buffer below RX_TEMP (0x2A000); max xfer = READ_RX_SKIP + READ_CHUNK.
 */
#define BIDIR_TX_BUF ((volatile uint8_t *)0x28000)
#define RX_TEMP      ((uint8_t *)0x2A000)          /* temp RX buffer (2KB+overhead) */
/*
 * TCAT SPI DMA: first MISO data byte aligns after 8 RX samples (4 cmd/addr +
 * 4 pipeline/dummy clocks). Match xfer_len to READ_RX_SKIP + data_len.
 */
#define READ_RX_SKIP 8

static int dma_bidir_read(uint32_t spi_addr, uint8_t *buf, uint32_t data_len) {
    uint32_t xfer_len = READ_RX_SKIP + data_len;

    BIDIR_TX_BUF[0] = 0x03;
    BIDIR_TX_BUF[1] = (spi_addr >> 16) & 0xFF;
    BIDIR_TX_BUF[2] = (spi_addr >> 8) & 0xFF;
    BIDIR_TX_BUF[3] = spi_addr & 0xFF;
    for (uint32_t i = 4; i < xfer_len; i++)
        BIDIR_TX_BUF[i] = 0;
    dwb();

    while (SPI_STAT & 1) {}
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
    DMA_CHREG(0, 0x0C) = (xfer_len & 0xFFF) | 0x88009000;
    dwb();
    DMA_CHREG(0, 0x10) = 0xD007;
    dwb();

    DMA_CHREG(1, 0x10) = 0;
    DMA_CHREG(1, 0x00) = (uint32_t)(uintptr_t)BIDIR_TX_BUF;
    DMA_CHREG(1, 0x04) = SPI_TX_PORT;
    DMA_CHREG(1, 0x08) = 0;
    DMA_CHREG(1, 0x0C) = 0xFFF | 0xF4009000;
    dwb();
    DMA_CHREG(1, 0x10) = 0xD005;
    dwb();

    SPI_CS = 1;
    SPI_CLRINT = 0;
    dwb();

    for (volatile int i = 0; i < 1000000; i++) {
        if (DMA_ISTAT & 1) {
            SPI_DMAGO = 1;
            while (SPI_STAT & 1) {}
            SPI_CS = 0;
            SPI_EN = 0;
            DMA_EN = 0;
            DMA_ICLR = 3;
            DMA_CHCLR(0) = 1;
            DMA_CHCLR(1) = 1;
            for (uint32_t j = 0; j < data_len; j++)
                buf[j] = RX_TEMP[READ_RX_SKIP + j];
            return 0;
        }
    }

    dma_abort_inflight();
    return -1;
}

/* ── RDSR polling (hardware-exploration confirmed reliable) ─── */

static uint8_t rdsr(void) {
    if (dma_rx(0x05, (uint8_t *)RX_SCRATCH, 1) == 0)
        return RX_SCRATCH[0];
    return 0xFF;
}

/* Read SR until two consecutive non-0xFF samples match. */
static uint8_t rdsr_stable(uint32_t timeout_cycles) {
    uint32_t t0 = timer_read();
    uint8_t prev = 0xFF;
    for (;;) {
        uint8_t sr = rdsr();
        if (sr != 0xFF) {
            if (sr == prev)
                return sr;
            prev = sr;
        }
        if (timer_elapsed(t0, timer_read()) > timeout_cycles)
            return 0xFF;
    }
}

/* During AAI, RDSR can intermittently return 0xFF on this transport.
 * Treat single bad samples as noise; only fail on repeated stable WEL=0. */
static int aai_wel_ok(void) {
    int clear_hits = 0;
    for (int n = 0; n < 6; n++) {
        uint8_t sr = rdsr_stable(TIMER_RELOAD / 32 + 1);
        if (sr == 0xFF)
            continue;
        trace_sr(EVT_WRITE_PAIR_SR, sr);
        if (sr & 0x02)
            return 1;  /* WEL set */
        clear_hits++;
        if (clear_hits >= 2) {
            trace_sr(EVT_WRITE_WEL_LOST, sr);
            return 0;  /* repeated stable clear -> real failure */
        }
    }
    return 1;  /* inconclusive noisy path; don't false-fail */
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
static int do_read(uint32_t spi_addr);

static int do_bp_clear(void) {
    spi_reset();
    trace_sr(EVT_BP_CLEAR_PRE, rdsr());

    /* EWSR (0x50) + WRSR (0x01, 0x00) — clear all block protection bits.
     * SST25VF016B requires EWSR (not WREN!) to enable WRSR writes. */
    if (pio_cmd1(0x50) != 0 || pio_cmd2(0x01, 0x00) != 0) {
        MBOX->errors = 0xDA50;
        spi_reset();
        return -1;
    }
    wait_ready(TIMER_RELOAD);    /* tWRSR max 10ms → poll RDSR */

    /* Verify: read SR and check BP bits (mask 0x3C = BP3:BP0) */
    uint8_t sr = rdsr();
    MBOX->last_sr = sr;
    if (sr & 0x3C) {
        /* First attempt failed — retry with WREN as fallback */
        if (pio_cmd1(0x06) != 0 || pio_cmd2(0x01, 0x00) != 0) {
            MBOX->errors = 0xDA50;
            spi_reset();
            return -1;
        }
        wait_ready(TIMER_RELOAD);
        sr = rdsr();
        MBOX->last_sr = sr;
    }
    trace_sr(EVT_BP_CLEAR_POST, sr);
    return 0;
}

static int do_erase(uint32_t spi_addr) {
    spi_reset();
    if (pio_cmd1(0x06) != 0) {  /* WREN */
        MBOX->errors = 0xDA51;
        spi_reset();
        return -1;
    }
    trace_sr(EVT_ERASE_WREN, rdsr());

    /* SE (0x20) — 4 bytes, fits in PIO */
    if (pio_cmd4(0x20,
                  (spi_addr >> 16) & 0xFF,
                  (spi_addr >> 8) & 0xFF,
                  spi_addr & 0xFF) != 0) {
        MBOX->errors = 0xDA52;
        spi_reset();
        return -1;
    }

    /* Poll RDSR for erase completion (tSE max 25ms) */
    int ret = wait_ready(TIMER_RELOAD);  /* ~28ms timeout */
    trace_sr(EVT_ERASE_DONE, rdsr());
    return ret;
}

/*
 * AAI Word-Program: writes full 4KB sector from DATA_BUF.
 *
 * SST25VF016B AAI protocol (DS20005044C, Figure 4-9):
 *   - WREN (0x06)
 *   - First pair:  CE# low → [0xAD, A23, A15, A7, D0, D1] → CE# high
 *   - Subsequent:  CE# low → [0xAD, Dn, Dn+1] → CE# high
 *   - Between pairs: wait until RDSR.BUSY clears
 *   - Terminate:   CE# low → WRDI (0x04) → CE# high
 *
 * CE# rising edge commits each pair to internal programming; issuing the
 * next AAI pair before BUSY clears can drop/garble data.  Polling BUSY is
 * both valid per datasheet and faster than fixed worst-case delays.
 */
static int do_write(uint32_t spi_addr) {
    /* Byte-program fallback: slower than AAI, but this is the best baseline we
     * have in TCAT-BOOT. */
    const uint32_t warmup_addr = spi_addr + SECTOR_SIZE - 1;
    const uint32_t total_launches = SECTOR_SIZE + 3;
    for (uint32_t i = 0; i < total_launches; i++) {
        spi_reset();
        if (pio_cmd1(0x06) != 0) {  /* WREN */
            MBOX->errors = 0xDA53;
            spi_reset();
            return -1;
        }
        {
            uint8_t sr = rdsr_stable(TIMER_RELOAD / 4);
            MBOX->last_sr = sr;
            if (i == 0 || (i & 0x7F) == 0)
                trace_sr((i == 0) ? EVT_WRITE_WREN : EVT_WRITE_MID, sr);
            if ((sr & 0x02) == 0) {
                MBOX->errors = 0xDA20;
                spi_reset();
                return -1;
            }
        }

        TX_SCRATCH[0] = 0x02;
        if (i < 3) {
            /* Diagnostic: if the first few byte-program launches are being lost
             * by the controller, spend them on an erased tail byte where a 0xFF
             * write is harmless, then begin real programming at byte 0. */
            TX_SCRATCH[1] = (warmup_addr >> 16) & 0xFF;
            TX_SCRATCH[2] = (warmup_addr >> 8) & 0xFF;
            TX_SCRATCH[3] = warmup_addr & 0xFF;
            TX_SCRATCH[4] = 0xFF;
        } else {
            TX_SCRATCH[1] = ((spi_addr + (i - 3)) >> 16) & 0xFF;
            TX_SCRATCH[2] = ((spi_addr + (i - 3)) >> 8) & 0xFF;
            TX_SCRATCH[3] = (spi_addr + (i - 3)) & 0xFF;
            TX_SCRATCH[4] = DATA_BUF[i - 3];
        }
        dwb();

        if (dma_tx(TX_SCRATCH, 5) != 0) {
            MBOX->errors = 0xDA01;
            spi_reset();
            return -1;
        }
        if (i == 0)
            trace_sr(EVT_WRITE_FIRST, rdsr_stable(TIMER_RELOAD / 8));

        if (wait_ready(TIMER_RELOAD) != 0) {
            MBOX->errors = 0xDA12;
            spi_reset();
            return -1;
        }

        if (i >= 3 && (((i - 2) & 0xFF) == 0) && MBOX->command != 6)
            MBOX->progress = i - 2;
    }

    {
        uint8_t sr = rdsr_stable(TIMER_RELOAD / 4);
        MBOX->last_sr = sr;
        trace_sr(EVT_WRITE_DONE, sr);
    }

    /*
     * In CMD_FLASH_ALL we transition directly to VERIFY with no host-side
     * gap. Ensure the final internal program cycle is complete before any
     * subsequent READ/VERIFY, otherwise verify can race BUSY and report
     * transient mismatches.
     */
    spi_reset();
    if (wait_ready(TIMER_RELOAD * 2) != 0) {
        MBOX->errors = 0xDA02;
        spi_reset();
        return -1;
    }

    /* Debug guard: verify first 16 bytes immediately after write.
     * Catches silent "write accepted but not programmed" failures. */
    if (do_read(spi_addr) != 0) {
        MBOX->errors = 0xDA30;
        return -1;
    }
    for (uint32_t i = 0; i < 16; i++) {
        if (READBACK_BUF[i] != DATA_BUF[i]) {
            MBOX->errors = 0xDA31;
            MBOX->last_sr = 0xE0000000 | (i << 16) |
                ((uint32_t)DATA_BUF[i] << 8) | (uint32_t)READBACK_BUF[i];
            return -1;
        }
    }

    return 0;
}

/*
 * Read sector via bidirectional DMA in chunks.
 * Each chunk reads into RX_TEMP, then copies (skipping 4-byte cmd overhead).
 */
static int do_read(uint32_t spi_addr) {
    /* When READ follows AAI WRITE inside CMD_FLASH_ALL, normalize SPI/DMA
     * state first to avoid stale mode/FIFO interactions. */
    spi_reset();

    /* Flush SPI RX pipeline: do a dummy 1-byte RDSR to drain stale data. */
    if (dma_rx(0x05, (uint8_t *)RX_SCRATCH, 1) != 0) {
        MBOX->errors = 0xDA60;
        spi_reset();
        return -1;
    }

    for (uint32_t off = 0; off < SECTOR_SIZE; off += READ_CHUNK) {
        if (off > 0) {
            if (dma_rx(0x05, (uint8_t *)RX_SCRATCH, 1) != 0) {
                MBOX->errors = 0xDA60;
                spi_reset();
                return -1;
            }
        }

        if (dma_bidir_read(spi_addr + off, READBACK_BUF + off, READ_CHUNK) != 0) {
            MBOX->errors = off;
            return -1;
        }
        if (MBOX->command != 6)
            MBOX->progress = off + READ_CHUNK;
    }
    return 0;
}

static int do_verify(uint32_t spi_addr) {
    if (do_read(spi_addr) != 0) {
        MBOX->last_sr = 0xD0000000 | (spi_addr & 0x0FFFFFFF);  /* verify read path fail */
        return -1;
    }

    uint32_t mismatches = 0;
    uint32_t first_idx = 0xFFFFFFFF;
    uint8_t first_exp = 0;
    uint8_t first_act = 0;
    for (uint32_t i = 0; i < SECTOR_SIZE; i++) {
        if (READBACK_BUF[i] != DATA_BUF[i]) {
            if (first_idx == 0xFFFFFFFF) {
                first_idx = i;
                first_exp = DATA_BUF[i];
                first_act = READBACK_BUF[i];
            }
            mismatches++;
        }
    }
    MBOX->errors = mismatches;
    if (mismatches != 0) {
        /* Encode mismatch detail for host:
         * last_sr: [31:28]=0xC tag, [27:16]=first mismatch index, [15:8]=expected, [7:0]=actual
         */
        MBOX->last_sr = 0xC0000000 |
            ((first_idx & 0x0FFF) << 16) |
            ((uint32_t)first_exp << 8) |
            (uint32_t)first_act;
        /* Store a tiny readback signature to detect all-0x00 / all-0xFF patterns quickly. */
        uint32_t sig = ((uint32_t)READBACK_BUF[0]) |
                       ((uint32_t)READBACK_BUF[1] << 8) |
                       ((uint32_t)READBACK_BUF[2] << 16) |
                       ((uint32_t)READBACK_BUF[3] << 24);
        MBOX->reserved = sig;
    }
    return (mismatches == 0) ? 0 : -1;
}

#define VERIFY_CHUNK_BYTES 0x80
#define SECTOR_RETRY_LIMIT 1

/* FSS-like write-and-verify pass: write first, then chunked read/compare. */
static int verify_chunked_after_write(uint32_t spi_addr) {
    spi_reset();
    /* First READ after a long byte-program run can come back 4 bytes
     * misaligned unless the controller sees one short RX transaction first.
     * Reopened mailbox sessions naturally do this during setup; mirror that
     * here before the first verify chunk. */
    if (dma_rx(0x05, (uint8_t *)RX_SCRATCH, 1) != 0) {
        MBOX->errors = 0xDA60;
        return -1;
    }
    for (uint32_t off = 0; off < SECTOR_SIZE; off += VERIFY_CHUNK_BYTES) {
        if (off > 0 && dma_rx(0x05, (uint8_t *)RX_SCRATCH, 1) != 0) {
            MBOX->errors = 0xDA60;
            return -1;
        }
        if (dma_bidir_read(spi_addr + off, READBACK_BUF, VERIFY_CHUNK_BYTES) != 0) {
            MBOX->errors = 0xDA61;
            MBOX->last_sr = 0xD0000000 | ((spi_addr + off) & 0x0FFFFFFF);
            return -1;
        }
        for (uint32_t i = 0; i < VERIFY_CHUNK_BYTES; i++) {
            uint8_t act = READBACK_BUF[i];
            uint8_t exp = DATA_BUF[off + i];
            if (act != exp) {
                MBOX->errors = 0xDA62;
                MBOX->last_sr = 0xE0000000 |
                    (((off + i) & 0x0FFF) << 16) |
                    ((uint32_t)exp << 8) |
                    (uint32_t)act;
                return -1;
            }
        }
    }
    return 0;
}

/* FSS-style sector program: erase -> write -> verify with one retry. */
static int program_sector_with_retry(uint32_t spi_addr) {
    for (uint32_t attempt = 0; attempt <= SECTOR_RETRY_LIMIT; attempt++) {
        if (attempt > 0) {
            /* Retry gate based on current SR state (FSS-like conservative recovery). */
            spi_reset();
            uint8_t sr = rdsr_stable(TIMER_RELOAD / 4);
            trace_sr(EVT_WRITE_FAIL_SR, sr);
            if (sr != 0xFF && (sr & 0x3C)) {
                (void)do_bp_clear();  /* clear BP bits if they reappeared */
            }
            if (sr != 0xFF && (sr & 0x40)) {
                (void)pio_cmd1(0x04); /* force exit AAI if still latched */
                (void)wait_ready(TIMER_RELOAD);
            }
        }
        if (do_erase(spi_addr) != 0) {
            MBOX->errors = 0xDA70 | attempt;
            continue;
        }
        aai_pair_delay(36000);  /* settle gap after erase */
        if (do_write(spi_addr) != 0) {
            MBOX->errors = 0xDA72 | attempt;
            continue;
        }
        if (verify_chunked_after_write(spi_addr) == 0)
            return 0;
        MBOX->errors = 0xDA74 | attempt;
    }
    return -1;
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
    if (do_bp_clear() != 0) {
        MBOX->errors = total;
        MBOX->last_sr = 0xD0000000u;
        MBOX->progress = total;
        return;
    }

    uint32_t fail_count = 0;

    for (uint32_t i = 0; i < total; i++) {
        uint32_t addr = list[i].spi_addr;
        const uint8_t *src = image + list[i].sram_offset;

        /* Copy sector to DATA_BUF (write/verify functions read from there) */
        for (uint32_t j = 0; j < SECTOR_SIZE; j++)
            DATA_BUF[j] = src[j];

        /* CMD_FLASH_ALL progress is sector-based (0..total). */
        MBOX->progress = i;

        if (program_sector_with_retry(addr) != 0) {
            fail_count++;
            MBOX->reserved = MBOX->errors;  /* preserve stage-specific detail */
            if ((MBOX->last_sr & 0xF0000000) < 0xA0000000)
                MBOX->last_sr = 0xB0000000 | (addr & 0x0FFFFFFF);
        }
        MBOX->errors = fail_count;
        MBOX->progress = i + 1;
    }

    MBOX->progress = total;
    MBOX->errors = fail_count;
}

/* CMD_FLASH_ALL_NOVRFY (cmd 9): one full sector pipeline per dispatch.
 * State machine (driven by MBOX->reserved):
 *   0 = start: bp_clear, then set state=1
 *   1 = program current sector (erase+write+verify-chunked), advance
 */
static int do_flash_step(void) {
    struct sector_entry *list = SECTOR_LIST;
    const uint8_t *image = IMAGE_BASE;
    uint32_t state = MBOX->reserved;
    uint32_t idx = MBOX->progress;

    if (state == 0) {
        /* Init: count sectors, bp_clear */
        uint32_t total = 0;
        while (list[total].spi_addr != 0xFFFFFFFF && total < 256)
            total++;
        MBOX->total = total;
        MBOX->progress = 0;
        MBOX->errors = 0;
        if (do_bp_clear() != 0) {
            MBOX->errors++;
            MBOX->last_sr = 0xD0000000u;
            MBOX->reserved = 0;
            return 0;  /* abort — BP clear failed */
        }
        MBOX->reserved = 1;
        return 1;  /* not done */
    }

    if (idx >= MBOX->total) {
        MBOX->reserved = 0;
        return 0;  /* done */
    }

    uint32_t addr = list[idx].spi_addr;

    if (state == 1) {
        /* Copy sector data to DATA_BUF */
        const uint8_t *src = image + list[idx].sram_offset;
        for (uint32_t j = 0; j < SECTOR_SIZE; j++)
            DATA_BUF[j] = src[j];
        if (program_sector_with_retry(addr) != 0) {
            MBOX->errors++;
            if ((MBOX->last_sr & 0xF0000000) < 0xA0000000)
                MBOX->last_sr = 0xB0000000 | (addr & 0x0FFFFFFF);
            MBOX->progress = idx + 1;
            return 1;
        }
        MBOX->reserved = 1;
        MBOX->progress = idx + 1;
        return 1;  /* next sector */
    }

    return 0;  /* unknown state */
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
    if (pio_cmd1(0x04) != 0)  /* WRDI — safe to send even if not in AAI mode */
        spi_reset();

    mb->status = 0;
    mb->command = 0;
    mb->progress = 0;
    mb->errors = 0;
    mb->last_sr = 0;
    SR_TRACE_IDX = 0;

    for (;;) {
        uint32_t cmd = mb->command;
        if (cmd == 0)
            continue;

        mb->status = 1;  /* running */
        /* Don't reset progress/errors for cmd 9 re-entries */
        if (cmd != 9) {
            mb->progress = 0;
            mb->errors = 0;
        }

        uint32_t addr = mb->spi_addr;
        int ret = 0;

        switch (cmd) {
        case 1: ret = do_erase(addr); break;
        case 2: ret = do_write(addr); break;
        case 3: ret = do_read(addr); break;
        case 4: ret = do_verify(addr); break;
        case 5: ret = do_bp_clear(); break;
        case 6: do_flash_all(); break;
        case 7: spi_restore_clean(); break;
        case 9:
            /* State machine: one sub-op per host dispatch.
             * Host polls status, re-sends cmd=9 for each step.
             * ret=0 from do_flash_step means done; ret=1 means more. */
            ret = do_flash_step() ? 0 : 0;  /* always "success" for status */
            break;
        case 8: {
            /* Diagnostic: read 4 flash bytes via dma_rx code path.
             * Sends [0x03, A23, A15, A7] + 4 dummy = 8 bytes bidir.
             * Result in mailbox: last_sr = first 4 data bytes (LE). */
            uint8_t rx[12];
            TX_SCRATCH[0] = 0x03;
            TX_SCRATCH[1] = (addr >> 16) & 0xFF;
            TX_SCRATCH[2] = (addr >> 8) & 0xFF;
            TX_SCRATCH[3] = addr & 0xFF;
            for (int k = 4; k < 16; k++) TX_SCRATCH[k] = 0;

            /* Exact dma_rx path but total=8, dest=rx */
            uint32_t total = 8;
            while (SPI_STAT & 1) {}
            SPI_EN = 0; SPI_CS = 0; SPI_DMAGO = 0;
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
            DMA_CHREG(0, 0x04) = (uint32_t)(uintptr_t)rx;
            DMA_CHREG(0, 0x08) = 0;
            DMA_CHREG(0, 0x0C) = (total & 0xFFF) | 0x88009000;
            dwb();
            DMA_CHREG(0, 0x10) = 0xD007;

            DMA_CHREG(1, 0x10) = 0;
            DMA_CHREG(1, 0x00) = (uint32_t)(uintptr_t)TX_SCRATCH;
            DMA_CHREG(1, 0x04) = SPI_TX_PORT;
            DMA_CHREG(1, 0x08) = 0;
            DMA_CHREG(1, 0x0C) = 0xFFF | 0xF4009000;
            dwb();
            DMA_CHREG(1, 0x10) = 0xD005;
            dwb();

            SPI_CS = 1;
            SPI_CLRINT = 0;
            dwb();

            int ok = 0;
            for (volatile int w = 0; w < 500000; w++) {
                if (DMA_ISTAT & 1) {
                    SPI_DMAGO = 1;
                    while (SPI_STAT & 1) {}
                    SPI_CS = 0; SPI_EN = 0;
                    DMA_EN = 0; DMA_ICLR = 3;
                    DMA_CHCLR(0) = 1;
                    DMA_CHCLR(1) = 1;
                    ok = 1;
                    break;
                }
            }
            if (!ok) {
                dma_abort_inflight();
                ret = -1;
                break;
            }

            /* Pack flash bytes (skip 4 cmd-phase garbage) into mailbox */
            mb->last_sr = (uint32_t)rx[4] | ((uint32_t)rx[5] << 8) |
                          ((uint32_t)rx[6] << 16) | ((uint32_t)rx[7] << 24);
            /* Also store cmd-phase garbage for debug */
            mb->errors = (uint32_t)rx[0] | ((uint32_t)rx[1] << 8) |
                         ((uint32_t)rx[2] << 16) | ((uint32_t)rx[3] << 24);
            break;
        }
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
