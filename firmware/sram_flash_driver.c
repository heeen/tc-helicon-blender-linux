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
 * Proven register sequences from sram_sector_write.c (2026-03-24):
 *   PIO TX:  ≤4 byte commands (WREN, EWSR, WRSR, SE, WRDI, AAI subsequent)
 *   DMA TX:  >4 byte commands (AAI first pair = 6 bytes)
 *   DMA RX:  RDSR (1 byte) + bulk READ (2KB chunks)
 *
 * SRAM layout:
 *   0x2B000  Sector data buffer (4096 bytes, loaded by host)
 *   0x2C000  This code (up to 8KB)
 *   0x2E000  Mailbox (32 bytes)
 *   0x2E020  TX scratch (16 bytes)
 *   0x2E030  RX scratch (16 bytes)
 *   0x2E100  Read-back buffer (4096 bytes, for read/verify)
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

#define DATA_BUF     ((const uint8_t *)0x2B000)
#define READBACK_BUF ((uint8_t *)0x2E100)
#define SECTOR_SIZE  0x1000
#define READ_CHUNK   0x800   /* 2KB — fits in DMA CFG 12-bit count */

struct mailbox {
    volatile uint32_t status;    /* 0=idle, 1=running, 2=done_ok, 0xFF=error */
    volatile uint32_t command;   /* 0=nop, 1=erase, 2=write, 3=read, 4=verify, 5=bp_clear */
    volatile uint32_t spi_addr;
    volatile uint32_t progress;  /* bytes processed so far */
    volatile uint32_t errors;    /* error count */
    volatile uint32_t last_sr;   /* last RDSR value (debug) */
    volatile uint32_t reserved1;
    volatile uint32_t reserved2;
};
#define MBOX ((struct mailbox *)0x2E000)

#define TX_SCRATCH ((volatile uint8_t *)0x2E020)
#define RX_SCRATCH ((volatile uint8_t *)0x2E030)

/* ── Write barrier (ARM926EJ-S has no DMB) ──────────────────── */

static inline void dwb(void) {
    __asm__ volatile("mcr p15, 0, %0, c7, c10, 4" :: "r"(0) : "memory");
}

/* ── Delay ──────────────────────────────────────────────────── */

static void delay_us(uint32_t us) {
    /* ~3µs per iteration with D-cache off (each iter = 6 insns × SRAM bus) */
    /* With I-cache ON: ~0.5µs per iter. Without: ~3µs. Use larger multiplier. */
    volatile uint32_t n = us * 20;
    if (n == 0) n = 1;
    while (n--) { __asm__ volatile(""); }
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

static void pio_cmd1(uint8_t cmd) {
    pio_tx_begin(1);
    pio_tx_byte(cmd);
    pio_tx_end();
}

static void pio_cmd2(uint8_t c1, uint8_t c2) {
    pio_tx_begin(2);
    pio_tx_byte(c1);
    pio_tx_byte(c2);
    pio_tx_end();
}

static void pio_cmd4(uint8_t c1, uint8_t c2, uint8_t c3, uint8_t c4) {
    pio_tx_begin(4);
    pio_tx_byte(c1);
    pio_tx_byte(c2);
    pio_tx_byte(c3);
    pio_tx_byte(c4);
    pio_tx_end();
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

/* ── DMA RX (1-byte commands only: RDSR, JEDEC ID) ─────────── */
/*
 * CTRL=0x307 sends only 1 byte from SPI_DATA before receiving.
 * Works for 1-byte commands (RDSR=0x05, JEDEC=0x9F).
 * For multi-byte commands (READ=0x03+addr), use dma_bidir_read below.
 */
static int dma_rx(uint32_t spi_cmd, uint8_t *buf, uint32_t len) {
    while (SPI_STAT & 1) {}
    SPI_EN = 0;
    SPI_CS = 0;
    SPI_DMAGO = 0;

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
            SPI_CS = 0;
            SPI_EN = 0;
            return 0;
        }
    }

    SPI_CS = 0;
    SPI_EN = 0;
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
#define READ_TX_BUF  ((volatile uint8_t *)0x2B000) /* reuse DATA_BUF area */
#define RX_TEMP      ((uint8_t *)0x32000)  /* temp RX buffer in unused BSS zone */
#define RX_OVERHEAD  4  /* 4 cmd-phase garbage before flash data */

static int dma_bidir_read(uint32_t spi_addr, uint8_t *buf, uint32_t data_len) {
    uint32_t total = RX_OVERHEAD + data_len;

    /* Save DATA_BUF[0..3] (clobbered by READ command, needed for verify) */
    uint8_t saved[4] = { READ_TX_BUF[0], READ_TX_BUF[1], READ_TX_BUF[2], READ_TX_BUF[3] };

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
            /* Restore DATA_BUF[0..3] */
            READ_TX_BUF[0] = saved[0]; READ_TX_BUF[1] = saved[1];
            READ_TX_BUF[2] = saved[2]; READ_TX_BUF[3] = saved[3];
            return 0;
        }
    }

    SPI_CS = 0;
    SPI_EN = 0;
    DMA_CHCLR(1) = 1;
    return -1;
}

/* ── RDSR + busy-wait ───────────────────────────────────────── */

static int use_fixed_delays = 1;  /* RDSR polling unreliable across sessions */

static uint8_t rdsr(void) {
    if (dma_rx(0x05, (uint8_t *)RX_SCRATCH, 1) == 0)
        return RX_SCRATCH[0];
    return 0xFF;
}

/*
 * Poll RDSR until BUSY (bit 0) clears, or fall back to fixed delay.
 * On first RDSR failure, switches to fixed-delay mode permanently.
 */
static int wait_ready(uint32_t timeout_us) {
    if (use_fixed_delays) {
        delay_us(timeout_us);
        return 0;
    }

    uint32_t attempts = timeout_us / 10;
    if (attempts < 100) attempts = 100;

    uint32_t ff_count = 0;
    for (uint32_t i = 0; i < attempts; i++) {
        uint8_t sr = rdsr();
        if (sr == 0xFF) {
            ff_count++;
            if (ff_count > 5) {
                /* DMA RX broken — fall back to fixed delays */
                use_fixed_delays = 1;
                MBOX->last_sr = 0xDEAD;
                delay_us(timeout_us);
                return 0;
            }
            continue;
        }
        ff_count = 0;
        MBOX->last_sr = sr;
        if (!(sr & 1))
            return 0;
    }
    return -1;
}

/* ── Command handlers ───────────────────────────────────────── */

static void do_bp_clear(void) {
    spi_reset();

    /* EWSR (1B PIO) + WRSR (2B DMA TX) */
    pio_cmd1(0x50);
    TX_SCRATCH[0] = 0x01;
    TX_SCRATCH[1] = 0x00;
    dma_tx(TX_SCRATCH, 2);
    delay_us(15000);

    /* WREN (1B PIO) + WRSR (2B DMA TX) — in case EWSR didn't work */
    pio_cmd1(0x06);
    TX_SCRATCH[0] = 0x01;
    TX_SCRATCH[1] = 0x00;
    dma_tx(TX_SCRATCH, 2);
    delay_us(15000);
}

static int do_erase(uint32_t spi_addr) {
    spi_reset();
    pio_cmd1(0x06);  /* WREN */

    /* SE via DMA TX (4 bytes) */
    TX_SCRATCH[0] = 0x20;
    TX_SCRATCH[1] = (spi_addr >> 16) & 0xFF;
    TX_SCRATCH[2] = (spi_addr >> 8) & 0xFF;
    TX_SCRATCH[3] = spi_addr & 0xFF;
    dma_tx(TX_SCRATCH, 4);

    /* Wait for erase completion (tSE max 25ms) */
    delay_us(30000);
    return 0;
}

/*
 * AAI Word-Program: writes full 4KB sector from DATA_BUF.
 *   First pair:  DMA TX 6 bytes [0xAD, A23, A15, A7, D0, D1]
 *   Subsequent:  PIO TX 3 bytes [0xAD, D_n, D_n+1] (≤4 byte limit)
 *   Each pair programs 2 bytes at ~10µs; 2048 pairs ≈ 40ms total.
 */
static int do_write(uint32_t spi_addr) {
    spi_reset();
    pio_cmd1(0x06);

    /* First AAI pair — 6 bytes, needs DMA TX */
    TX_SCRATCH[0] = 0xAD;
    TX_SCRATCH[1] = (spi_addr >> 16) & 0xFF;
    TX_SCRATCH[2] = (spi_addr >> 8) & 0xFF;
    TX_SCRATCH[3] = spi_addr & 0xFF;
    TX_SCRATCH[4] = DATA_BUF[0];
    TX_SCRATCH[5] = DATA_BUF[1];

    if (dma_tx(TX_SCRATCH, 6) != 0) {
        MBOX->errors = 0xDA01;  /* DMA TX fail on first AAI */
        pio_cmd1(0x04);  /* WRDI — exit AAI on error */
        return -1;
    }

    /* Wait for first pair — fixed delay only!
     * RDSR between AAI pairs causes CTRL mode switch (0x107→0x307→0x107)
     * which sends spurious bytes on MOSI, aborting AAI mode. */
    delay_us(20);  /* tBP max 10µs, 2x margin */
    MBOX->progress = 2;

    /* Subsequent AAI pairs — 3 bytes each via DMA TX.
     * MUST use fixed delays (no RDSR) to stay in AAI mode. */
    for (uint32_t i = 2; i < SECTOR_SIZE; i += 2) {
        TX_SCRATCH[0] = 0xAD;
        TX_SCRATCH[1] = DATA_BUF[i];
        TX_SCRATCH[2] = DATA_BUF[i + 1];

        if (dma_tx(TX_SCRATCH, 3) != 0) {
            MBOX->errors = i;
            pio_cmd1(0x04);
            return -1;
        }

        delay_us(20);  /* tBP max 10µs */

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

/* ── Main command loop ──────────────────────────────────────── */

void do_main(void) {
    struct mailbox *mb = MBOX;

    /* Initialize SPI controller state */
    while (SPI_STAT & 1) {}
    SPI_EN = 0;
    SPI_CS = 0;
    SPI_CLK = 2;

    /* Exit AAI mode if flash is stuck in it (from a previous aborted write).
     * In AAI mode, only RDSR and WRDI are accepted. WRDI exits AAI. */
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
