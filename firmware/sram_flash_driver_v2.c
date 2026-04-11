/*
 * sram_flash_driver_v2.c — Clean rewrite of SPI flash driver
 *
 * Built and tested incrementally. Each primitive verified in isolation
 * before composition. All addresses and timings from DS20005044C.
 *
 * SRAM layout (proven working):
 *   0x2A000  RX_TEMP      (2KB+4, bidir DMA read scratch)
 *   0x2B000  DATA_BUF     (4KB, sector data for write/verify)
 *   0x2C000  Driver code  (8KB max, this file)
 *   0x2E000  Mailbox      (32 bytes)
 *   0x2E020  TX_SCRATCH   (16 bytes)
 *   0x2E030  RX_SCRATCH   (16 bytes)
 *   0x2E040  READ_TX_BUF  (16 bytes, bidir read command buffer)
 *   0x2E100  READBACK_BUF (4KB, flash readback)
 *   0x2F100  SECTOR_LIST  (2KB, 256 entries max)
 *   0x2F900  IMAGE_BASE   (326KB bulk data, fits 80 sectors)
 *   Stack at 0x2AFFC (grows down)
 *
 * Build:
 *   arm-none-eabi-gcc -march=armv5te -marm -Os -nostdlib -ffreestanding \
 *     -T firmware/sram_flash_driver.ld \
 *     -o firmware/sram_flash_driver_v2.elf firmware/sram_flash_driver_v2.c
 *   arm-none-eabi-objcopy -O binary \
 *     firmware/sram_flash_driver_v2.elf firmware/sram_flash_driver_v2.bin
 */

#include <stdint.h>

/* ═══════════════════════════════════════════════════════════════
 * Hardware Registers
 * ═══════════════════════════════════════════════════════════════ */

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
#define DMA_CHREG(ch, off) (*(volatile uint32_t *)(DMA_BASE + 0x100 + (ch)*0x20 + (off)))
#define DMA_CHCLR(ch)      (*(volatile uint32_t *)(DMA_BASE + 0x30 + (ch)*4))

#define SPI_TX_PORT (SPI_BASE + 0x80)
#define SPI_RX_PORT (SPI_BASE + 0x70)

/* ═══════════════════════════════════════════════════════════════
 * SRAM Layout
 * ═══════════════════════════════════════════════════════════════ */

#define RX_TEMP      ((uint8_t *)0x2A000)
#define DATA_BUF     ((uint8_t *)0x2B000)
#define READBACK_BUF ((uint8_t *)0x2E100)
#define SECTOR_LIST  ((struct sector_entry *)0x2F100)
#define IMAGE_BASE   ((const uint8_t *)0x2F900)
#define TX_SCRATCH   ((volatile uint8_t *)0x2E020)
#define RX_SCRATCH   ((volatile uint8_t *)0x2E030)
#define READ_TX_BUF  ((volatile uint8_t *)0x2E040)

#define SECTOR_SIZE  0x1000
#define READ_CHUNK   0x800
#define RX_OVERHEAD  4
#define MAX_BATCH    32  /* limited by safe SRAM for load_image (0x2F900-0x4FFFF) */

struct sector_entry {
    uint32_t spi_addr;
    uint32_t sram_offset;
};

/* ═══════════════════════════════════════════════════════════════
 * Mailbox
 * ═══════════════════════════════════════════════════════════════ */

#define ST_IDLE    0
#define ST_RUNNING 1
#define ST_DONE    2
#define ST_ERROR   0xFF

struct mailbox {
    volatile uint32_t status;
    volatile uint32_t command;
    volatile uint32_t spi_addr;
    volatile uint32_t progress;
    volatile uint32_t errors;
    volatile uint32_t last_sr;
    volatile uint32_t total;
    volatile uint32_t fail_addr;
};
#define MB ((struct mailbox *)0x2E000)

/* ═══════════════════════════════════════════════════════════════
 * Primitives
 * ═══════════════════════════════════════════════════════════════ */

static inline void dwb(void) {
    __asm__ volatile("mcr p15, 0, %0, c7, c10, 4" :: "r"(0) : "memory");
}

/* PLL-independent delay. Multiplier 80 gives ~12µs for delay_us(10)
 * on PLL-active clock (~196MHz). On ROM clock (~49MHz) it gives ~48µs.
 * Both are safe for tBP max=10µs. */
static void delay_us(uint32_t us) {
    volatile uint32_t n = us * 80;
    while (n--) { __asm__ volatile(""); }
}

/* ── SPI reset ─────────────────────────────────────────────── */

static void spi_wait_idle(void) {
    for (volatile int i = 0; i < 100000; i++)
        if (!(SPI_STAT & 1)) return;
}

static void spi_reset(void) {
    spi_wait_idle();
    SPI_CS = 0;
    SPI_EN = 0;
    SPI_DMAGO = 0;
    SPI_DMAMD = 0;
    SPI_CLK = 2;
    /* Do NOT touch DMA_EN or DMA_CHCLR here — they break DMA permanently */
    dwb();
}

/* ── PIO TX (≤4 bytes) ─────────────────────────────────────── */

static void pio_begin(uint32_t nbytes) {
    spi_wait_idle();
    SPI_EN = 0; SPI_CS = 0;
    SPI_DMAMD = 0;
    SPI_CTRL = 0x107;
    SPI_LEN = nbytes - 1;
    SPI_CLK = 2;
    dwb();
    SPI_EN = 1; SPI_CS = 1;
    dwb();
}

static void pio_byte(uint8_t b) {
    while (!(SPI_STAT & 2)) {}
    SPI_DATA = b;
}

static void pio_end(void) {
    spi_wait_idle();
    SPI_CS = 0; SPI_EN = 0;
}

static void pio_cmd1(uint8_t c) {
    pio_begin(1); pio_byte(c); pio_end();
}

/* ── DMA TX (any length) ───────────────────────────────────── */

static int dma_tx(const volatile uint8_t *buf, uint32_t len) {
    dwb();
    spi_wait_idle();
    SPI_EN = 0; SPI_CS = 0; SPI_DMAGO = 0;
    SPI_CTRL = 0x107;
    SPI_LEN = len - 1;
    SPI_DMAMD = 2;
    SPI_CLK = 2;
    SPI_DMACFG0 = 4; SPI_DMACFG1 = 3;
    dwb();
    SPI_EN = 1;
    dwb();

    DMA_EN = 1;
    DMA_ICLR = 1;
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
            spi_wait_idle();
            SPI_DMAGO = 1;
            SPI_CS = 0; SPI_EN = 0;
            return 0;
        }
    }
    SPI_CS = 0; SPI_EN = 0;
    return -1;
}

/* ── DMA RX (1-byte cmd + response) ────────────────────────── */

static int dma_rx(uint8_t spi_cmd, uint8_t *buf, uint32_t len) {
    spi_wait_idle();
    SPI_EN = 0; SPI_CS = 0; SPI_DMAGO = 0;
    SPI_CTRL = 0x307;
    SPI_LEN = len - 1;
    SPI_DMAMD = 1;
    SPI_CLK = 2;
    SPI_DMACFG0 = 4; SPI_DMACFG1 = 3;
    dwb();
    SPI_EN = 1;
    dwb();

    DMA_EN = 1;
    DMA_ICLR = 1;
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
            spi_wait_idle();
            SPI_CS = 0; SPI_EN = 0;
            return 0;
        }
    }
    SPI_CS = 0; SPI_EN = 0;
    return -1;
}

/* ── DMA Bidirectional Read (READ 0x03 + addr) ─────────────── */

static int dma_bidir_read(uint32_t spi_addr, uint32_t data_len) {
    uint32_t total = RX_OVERHEAD + data_len;

    READ_TX_BUF[0] = 0x03;
    READ_TX_BUF[1] = (spi_addr >> 16) & 0xFF;
    READ_TX_BUF[2] = (spi_addr >> 8) & 0xFF;
    READ_TX_BUF[3] = spi_addr & 0xFF;
    dwb();

    spi_wait_idle();
    SPI_EN = 0; SPI_CS = 0; SPI_DMAGO = 0;
    SPI_CTRL = 0x007;
    SPI_LEN = total - 1;
    SPI_DMAMD = 3;
    SPI_CLK = 2;
    SPI_DMACFG0 = 4; SPI_DMACFG1 = 3;
    dwb();
    SPI_EN = 1;
    dwb();

    DMA_EN = 3;
    DMA_ICLR = 3;
    dwb();

    /* CH0: RX → RX_TEMP */
    DMA_CHREG(0, 0x10) = 0;
    DMA_CHREG(0, 0x00) = SPI_RX_PORT;
    DMA_CHREG(0, 0x04) = (uint32_t)(uintptr_t)RX_TEMP;
    DMA_CHREG(0, 0x08) = 0;
    DMA_CHREG(0, 0x0C) = (total & 0xFFF) | 0x88009000;
    dwb();
    DMA_CHREG(0, 0x10) = 0xD007;
    dwb();

    /* CH1: TX — oversized (0xFFF) so RX completes first */
    DMA_CHREG(1, 0x10) = 0;
    DMA_CHREG(1, 0x00) = (uint32_t)(uintptr_t)READ_TX_BUF;
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
            spi_wait_idle();
            SPI_DMAGO = 1;
            SPI_CS = 0; SPI_EN = 0;
            DMA_CHCLR(1) = 1;  /* kill incomplete TX channel */
            return 0;
        }
    }
    SPI_CS = 0; SPI_EN = 0;
    DMA_CHCLR(1) = 1;
    return -1;
}

/* ═══════════════════════════════════════════════════════════════
 * Flash Operations (built on primitives)
 * ═══════════════════════════════════════════════════════════════ */

static uint8_t rdsr(void) {
    if (dma_rx(0x05, (uint8_t *)RX_SCRATCH, 1) == 0)
        return RX_SCRATCH[0];
    return 0xFF;
}

/* Poll RDSR until BUSY (bit 0) clears. Returns 0 on success.
 * Used for erase and WRSR — NOT during AAI (breaks AAI mode). */
static int poll_busy(uint32_t max_ms) {
    for (uint32_t i = 0; i < max_ms * 10; i++) {
        uint8_t sr = rdsr();
        MB->last_sr = sr;
        if (sr == 0xFF) {
            delay_us(100);
            continue;  /* DMA glitch, retry */
        }
        if (!(sr & 1)) return 0;  /* not busy */
        delay_us(100);
    }
    return -1;  /* timeout */
}

/* ── CMD 7: JEDEC ID ───────────────────────────────────────── */

static void do_jedec_id(void) {
    /* JEDEC Read-ID: send 0x9F, receive 3 bytes (mfr, type, cap) */
    if (dma_rx(0x9F, (uint8_t *)RX_SCRATCH, 3) != 0) {
        MB->errors = 0xBAD0;
        return;
    }
    /* Pack as 0x00MMTTCC (manufacturer, memory type, capacity) */
    MB->last_sr = ((uint32_t)RX_SCRATCH[0] << 16) |
                  ((uint32_t)RX_SCRATCH[1] << 8) |
                  (uint32_t)RX_SCRATCH[2];
}

/* ── CMD 5: BP Clear ───────────────────────────────────────── */

static void do_bp_clear(void) {
    spi_reset();

    /* EWSR (0x50) — required for SST25, NOT WREN */
    pio_cmd1(0x50);

    /* WRSR (0x01, 0x00) — clear all BP bits */
    TX_SCRATCH[0] = 0x01;
    TX_SCRATCH[1] = 0x00;
    dma_tx(TX_SCRATCH, 2);

    /* Poll RDSR until WRSR completes (tWRSR ≤ 10ms per datasheet) */
    if (poll_busy(50) != 0) {
        MB->errors = 0xBFC1;  /* BP clear timeout */
        return;
    }

    /* Verify BP bits actually cleared */
    uint8_t sr = rdsr();
    MB->last_sr = sr;
    if (sr & 0x3C) {
        MB->errors = 0xBFC2;  /* BP bits still set */
    }
}

/* ── CMD 1: Sector Erase ───────────────────────────────────── */

static int do_erase(uint32_t spi_addr) {
    spi_reset();
    pio_cmd1(0x06);  /* WREN */

    /* SE (0x20) + 3-byte address */
    TX_SCRATCH[0] = 0x20;
    TX_SCRATCH[1] = (spi_addr >> 16) & 0xFF;
    TX_SCRATCH[2] = (spi_addr >> 8) & 0xFF;
    TX_SCRATCH[3] = spi_addr & 0xFF;
    if (dma_tx(TX_SCRATCH, 4) != 0)
        return -1;

    /* Poll RDSR until erase completes (tSE max 25ms) */
    return poll_busy(100);
}

/* ── CMD 2: AAI Write ──────────────────────────────────────── */

static int do_write(uint32_t spi_addr) {
    spi_reset();
    pio_cmd1(0x06);  /* WREN */

    /* First AAI pair: 6 bytes [0xAD, A23, A15, A7, D0, D1] */
    TX_SCRATCH[0] = 0xAD;
    TX_SCRATCH[1] = (spi_addr >> 16) & 0xFF;
    TX_SCRATCH[2] = (spi_addr >> 8) & 0xFF;
    TX_SCRATCH[3] = spi_addr & 0xFF;
    TX_SCRATCH[4] = DATA_BUF[0];
    TX_SCRATCH[5] = DATA_BUF[1];

    if (dma_tx(TX_SCRATCH, 6) != 0) {
        pio_cmd1(0x04);  /* WRDI — abort AAI */
        return -1;
    }

    delay_us(10);  /* tBP max 10µs */

    /* Subsequent pairs: 3 bytes [0xAD, Dn, Dn+1] via DMA TX.
     * Full dma_tx() per pair — the SPI controller requires complete
     * reconfiguration between DMA transfers. Optimized re-arm attempts
     * (keeping SPI/DMA partially configured) produce corrupt data. */
    for (uint32_t i = 2; i < SECTOR_SIZE; i += 2) {
        TX_SCRATCH[0] = 0xAD;
        TX_SCRATCH[1] = DATA_BUF[i];
        TX_SCRATCH[2] = DATA_BUF[i + 1];

        if (dma_tx(TX_SCRATCH, 3) != 0) {
            pio_cmd1(0x04);
            return -1;
        }

        delay_us(10);  /* tBP max 10µs */

        if ((i & 0x1FF) == 0)
            MB->progress = i;
    }

    pio_cmd1(0x04);  /* WRDI — exit AAI mode */
    MB->progress = SECTOR_SIZE;
    return 0;
}

/* ── CMD 3: Read Sector ────────────────────────────────────── */

static int do_read(uint32_t spi_addr) {
    /* Flush SPI RX pipeline */
    dma_rx(0x05, (uint8_t *)RX_SCRATCH, 1);

    for (uint32_t off = 0; off < SECTOR_SIZE; off += READ_CHUNK) {
        if (off > 0)
            dma_rx(0x05, (uint8_t *)RX_SCRATCH, 1);  /* flush between chunks */

        if (dma_bidir_read(spi_addr + off, READ_CHUNK) != 0)
            return -1;

        /* Copy from RX_TEMP (skip 4-byte cmd garbage) to READBACK_BUF */
        for (uint32_t i = 0; i < READ_CHUNK; i++)
            READBACK_BUF[off + i] = RX_TEMP[RX_OVERHEAD + i];

        MB->progress = off + READ_CHUNK;
    }
    return 0;
}

/* ── CMD 4: Verify ─────────────────────────────────────────── */

static int do_verify(uint32_t spi_addr) {
    if (do_read(spi_addr) != 0)
        return -1;

    uint32_t mismatches = 0;
    for (uint32_t i = 0; i < SECTOR_SIZE; i++) {
        if (READBACK_BUF[i] != DATA_BUF[i])
            mismatches++;
    }
    MB->errors = mismatches;
    return (mismatches == 0) ? 0 : -1;
}

/* ── CMD 6: Flash All ──────────────────────────────────────── */

static void do_flash_all(void) {
    struct sector_entry *list = SECTOR_LIST;
    const uint8_t *image = IMAGE_BASE;

    uint32_t total = 0;
    while (list[total].spi_addr != 0xFFFFFFFF && total < MAX_BATCH)
        total++;
    MB->total = total;

    /* BP clear once */
    do_bp_clear();
    if (MB->errors != 0) return;

    for (uint32_t i = 0; i < total; i++) {
        uint32_t addr = list[i].spi_addr;
        const uint8_t *src = image + list[i].sram_offset;

        /* Stage sector data to DATA_BUF */
        for (uint32_t j = 0; j < SECTOR_SIZE; j++)
            DATA_BUF[j] = src[j];

        MB->progress = i;
        MB->spi_addr = addr;

        /* Erase */
        if (do_erase(addr) != 0) {
            MB->fail_addr = addr;
            MB->errors = 0xE001;
            return;
        }

        /* Write */
        if (do_write(addr) != 0) {
            MB->fail_addr = addr;
            MB->errors = 0xE002;
            return;
        }

        /* Verify */
        if (do_verify(addr) != 0) {
            MB->fail_addr = addr;
            if (MB->errors == 0) MB->errors = 0xE003;
            return;
        }
    }

    MB->progress = total;
}

/* ── CMD 8: Fast AAI Write (PIO inner loop test) ───────────── */

static int do_write_fast(uint32_t spi_addr) {
    spi_reset();
    pio_cmd1(0x06);  /* WREN */

    /* First pair: 6 bytes via DMA (proven working) */
    TX_SCRATCH[0] = 0xAD;
    TX_SCRATCH[1] = (spi_addr >> 16) & 0xFF;
    TX_SCRATCH[2] = (spi_addr >> 8) & 0xFF;
    TX_SCRATCH[3] = spi_addr & 0xFF;
    TX_SCRATCH[4] = DATA_BUF[0];
    TX_SCRATCH[5] = DATA_BUF[1];

    if (dma_tx(TX_SCRATCH, 6) != 0) {
        pio_cmd1(0x04);
        return -1;
    }
    delay_us(10);

    /* Subsequent pairs: PIO with minimal SPI_EN toggle.
     * CTRL/CLK/DMAMD stay constant. Only toggle EN (re-arms LEN)
     * and CS (triggers flash CE#). */
    SPI_DMAMD = 0;
    SPI_CTRL = 0x107;
    SPI_CLK = 2;

    for (uint32_t i = 2; i < SECTOR_SIZE; i += 2) {
        spi_wait_idle();
        SPI_EN = 0;
        SPI_LEN = 2;    /* 3 bytes - 1 */
        dwb();
        SPI_EN = 1;
        SPI_CS = 1;

        while (!(SPI_STAT & 2)) {}
        SPI_DATA = 0xAD;
        while (!(SPI_STAT & 2)) {}
        SPI_DATA = DATA_BUF[i];
        while (!(SPI_STAT & 2)) {}
        SPI_DATA = DATA_BUF[i + 1];

        spi_wait_idle();
        SPI_CS = 0;

        delay_us(10);

        if ((i & 0x1FF) == 0)
            MB->progress = i;
    }

    SPI_EN = 0;
    pio_cmd1(0x04);  /* WRDI */
    MB->progress = SECTOR_SIZE;
    return 0;
}

/* ═══════════════════════════════════════════════════════════════
 * Main Command Loop
 * ═══════════════════════════════════════════════════════════════ */

void do_main(void) {
    struct mailbox *mb = MB;

    /* Initialize SPI */
    spi_wait_idle();
    SPI_EN = 0; SPI_CS = 0; SPI_CLK = 2;
    pio_cmd1(0x04);  /* WRDI — exit AAI if stuck */

    mb->status = ST_IDLE;
    mb->command = 0;
    mb->progress = 0;
    mb->errors = 0;
    mb->last_sr = 0;
    mb->fail_addr = 0;

    for (;;) {
        uint32_t cmd = mb->command;
        if (cmd == 0) continue;

        mb->status = ST_RUNNING;
        mb->progress = 0;
        mb->errors = 0;
        mb->fail_addr = 0;

        uint32_t addr = mb->spi_addr;
        int ret = 0;

        switch (cmd) {
        case 1: ret = do_erase(addr); break;
        case 2: ret = do_write(addr); break;
        case 3: ret = do_read(addr); break;
        case 4: ret = do_verify(addr); break;
        case 5: do_bp_clear(); ret = (mb->errors == 0) ? 0 : -1; break;
        case 6: do_flash_all(); ret = (mb->errors == 0) ? 0 : -1; break;
        case 7: do_jedec_id(); break;
        case 8: ret = do_write_fast(addr); break;
        default: ret = -1; break;
        }

        /* Status written LAST — host polls this */
        mb->status = (ret == 0) ? ST_DONE : ST_ERROR;
        /* Clear command AFTER status — prevents race */
        dwb();
        mb->command = 0;
    }
}

/* ═══════════════════════════════════════════════════════════════
 * Entry Point
 * ═══════════════════════════════════════════════════════════════ */

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
