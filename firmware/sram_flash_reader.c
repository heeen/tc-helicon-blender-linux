/*
 * sram_flash_reader.c — Reliable SPI flash reader via BKPT self-halt
 *
 * Strategy: reset CPU, catch in bootloader (before firmware runs),
 * disable MMU/caches/IRQs, re-init SPI from scratch (matching bootloader's
 * init sequence from Ghidra), then read flash using the bootloader's own
 * dma_spi_read_setup ROM function.
 *
 * The ROM function at 0x4F290 is in boot ROM (survives reset, no MMU needed).
 * We replicate the bootloader's hardware init (clocks, GPIO, SPI) so the
 * ROM function works in our bare-metal context.
 *
 * SRAM layout:
 *   0x2B000  Read buffer (4096 bytes)
 *   0x2C000  This code
 *   0x2E000  Mailbox: [addr, done, count]
 *
 * Build:
 *   arm-none-eabi-gcc -march=armv5te -marm -Os -nostdlib -ffreestanding \
 *     -T firmware/sram_flash_driver.ld \
 *     -o firmware/sram_flash_reader.elf firmware/sram_flash_reader.c
 *   arm-none-eabi-objcopy -O binary \
 *     firmware/sram_flash_reader.elf firmware/sram_flash_reader.bin
 */

#include <stdint.h>

#define BUF       ((uint8_t *)0x2B000)

struct mailbox {
    volatile uint32_t addr;
    volatile uint32_t done;
    volatile uint32_t count;
};
#define MBOX ((struct mailbox *)0x2E000)

/* Bootloader ROM function */
typedef void (*dma_spi_read_t)(uint32_t spi_addr, uint32_t dest, uint32_t size);
#define dma_spi_read ((dma_spi_read_t)0x4F290)

static inline void dwb(void) {
    __asm__ volatile("mcr p15, 0, %0, c7, c10, 4" :: "r"(0) : "memory");
}

/* Replicate bootloader's hardware init (from Ghidra boot_check_and_load 0x4F404) */
static void hw_init(void) {
    volatile uint32_t *cb = (volatile uint32_t *)0xCB000000;
    volatile uint32_t *c5 = (volatile uint32_t *)0xC5000000;
    volatile uint32_t *cc = (volatile uint32_t *)0xCC000000;
    volatile uint32_t *cf = (volatile uint32_t *)0xCF000000;

    /* Peripheral control (from boot_check_and_load) */
    cb[0x400/4] = 8;
    cb[0x020/4] = 0;

    /* GPIO pin configuration — use known working value */
    *(volatile uint32_t *)0xC9000034 = 0x4666;

    /* PLL / clock setup (from boot_check_and_load) */
    c5[0x2C/4] = 0x60;
    c5[0x04/4] = 0;
    /* Clock dividers — use values from working session */
    c5[0x24/4] = 0x1B;
    c5[0x28/4] = 0x08;
    c5[0x2C/4] = 0x70;
    c5[0x30/4] = 0x301;

    /* SPI flash clock */
    cc[0x14/4] = 2;       /* SPI_CLK = 2 */
    /* LED SPI clock */
    cf[0x14/4] = 0xFF;

    dwb();
}

static void spi_prep(void) {
    *(volatile uint32_t *)0xCC000008 = 0;  /* SPI_EN = 0 */
    *(volatile uint32_t *)0xCC000010 = 0;  /* SPI_CS = 0 */
    *(volatile uint32_t *)0xCC000014 = 2;  /* SPI_CLK = 2 */
    *(volatile uint32_t *)0x80000008 = 0;  /* DMA_EN = 0 */
    dwb();
}

static void dma_poll(void) {
    volatile uint32_t *istat = (volatile uint32_t *)0x80000014;
    while (!(*istat & 1)) {}
}

/* PIO TX for 1-byte commands (WREN, EWSR, WRDI) */
static void pio_cmd1(uint8_t cmd) {
    volatile uint32_t *spi = (volatile uint32_t *)0xCC000000;
    while (spi[0x28/4] & 1) {}
    spi[0x08/4] = 0; spi[0x10/4] = 0;
    spi[0x4C/4] = 0; spi[0x00/4] = 0x107;
    spi[0x04/4] = 0; spi[0x14/4] = 2;
    dwb(); spi[0x08/4] = 1; spi[0x10/4] = 1; dwb();
    while (!(spi[0x28/4] & 2)) {}
    spi[0x60/4] = cmd;
    while (spi[0x28/4] & 1) {}
    spi[0x10/4] = 0; spi[0x08/4] = 0;
}

/* DMA TX for multi-byte commands (SE, WRSR) */
static void dma_tx(const volatile uint8_t *buf, uint32_t len) {
    volatile uint32_t *spi = (volatile uint32_t *)0xCC000000;
    volatile uint32_t *dma = (volatile uint32_t *)0x80000000;
    dwb();
    while (spi[0x28/4] & 1) {}
    spi[0x08/4] = 0; spi[0x10/4] = 0; spi[0x2C/4] = 0;
    spi[0x00/4] = 0x107; spi[0x04/4] = len - 1;
    spi[0x4C/4] = 2; spi[0x14/4] = 2;
    spi[0x50/4] = 4; spi[0x54/4] = 3;
    dwb(); spi[0x08/4] = 1; dwb();
    dma[0x08/4] = 1; dma[0x10/4] = 1; dma[0x30/4] = 1; dwb();
    dma[0x110/4] = 0;
    dma[0x100/4] = (uint32_t)(uintptr_t)buf;
    dma[0x104/4] = 0xCC000080;
    dma[0x108/4] = 0;
    dma[0x10C/4] = len | 0xF4009000;
    dwb(); dma[0x110/4] = 0xD005; dwb();
    spi[0x10/4] = 1; spi[0x18/4] = 0; dwb();
    for (volatile int i = 0; i < 500000; i++)
        if (dma[0x14/4] & 1) break;
    while (spi[0x28/4] & 1) {}
    spi[0x2C/4] = 1; spi[0x10/4] = 0; spi[0x08/4] = 0;
}

static void delay(volatile uint32_t n) {
    while (n--) { __asm__ volatile(""); }
}

static void spi_full_reset(void) {
    volatile uint32_t *spi = (volatile uint32_t *)0xCC000000;
    volatile uint32_t *dma = (volatile uint32_t *)0x80000000;
    spi[0x08/4] = 0;  /* EN off */
    spi[0x10/4] = 0;  /* CS off */
    spi[0x2C/4] = 0;  /* DMAGO off */
    spi[0x4C/4] = 0;  /* DMAMD = PIO */
    spi[0x14/4] = 2;  /* CLK */
    dma[0x08/4] = 0;  /* DMA off */
    dma[0x10/4] = 3;  /* clear both ch interrupts */
    dma[0x30/4] = 1;  /* clear ch0 */
    dma[0x34/4] = 1;  /* clear ch1 */
    dwb();
    for (volatile int i = 0; i < 1000; i++)
        if (!(spi[0x28/4] & 1)) break;
}

static void bp_clear(void) {
    spi_full_reset();
    pio_cmd1(0x04);  /* WRDI — exit AAI if stuck */
    pio_cmd1(0x50);  /* EWSR */
    volatile uint8_t wrsr[2] = {0x01, 0x00};
    dma_tx(wrsr, 2);
    delay(500000);
    spi_full_reset();
    pio_cmd1(0x06);  /* WREN */
    wrsr[0] = 0x01; wrsr[1] = 0x00;
    dma_tx(wrsr, 2);
    delay(500000);
}

static void sector_erase(uint32_t addr) {
    spi_full_reset();
    pio_cmd1(0x06);  /* WREN */
    volatile uint8_t se[4] = {0x20, addr>>16, (addr>>8)&0xFF, addr&0xFF};
    dma_tx(se, 4);
    delay(1000000);
}

/*
 * Mailbox protocol:
 *   addr bit 31 clear = READ sector at addr
 *   addr bit 31 set   = ERASE sector at (addr & 0x7FFFFFFF)
 *   addr == 0xFFFFFFFE = BP_CLEAR
 */
void do_main(void) {
    struct mailbox *mb = MBOX;

    hw_init();
    mb->done = 0;
    mb->count = 0;

    for (;;) {
        uint32_t raw = mb->addr;

        if (raw == 0xFFFFFFFE) {
            /* BP clear */
            bp_clear();
        } else if (raw & 0x80000000) {
            /* Erase */
            uint32_t spi_addr = raw & 0x7FFFFFFF;
            sector_erase(spi_addr);
        } else {
            /* Read 4KB */
            uint32_t spi_addr = raw;
            spi_prep();
            dma_spi_read(spi_addr, (uint32_t)(uintptr_t)BUF, 0x800);
            dma_poll();
            spi_prep();
            dma_spi_read(spi_addr + 0x800, (uint32_t)(uintptr_t)(BUF + 0x800), 0x800);
            dma_poll();
        }

        mb->count++;
        mb->done = 1;
        while (mb->done) {}
    }
}

void _start(void) __attribute__((naked, section(".text.entry")));
void _start(void) {
    __asm__ volatile(
        "msr cpsr_c, #0xd3\n"            /* SVC mode, IRQs+FIQs off */
        "mov r0, #0\n"
        "mcr p15, 0, r0, c7, c6, 0\n"    /* invalidate D-cache */
        "mcr p15, 0, r0, c7, c5, 0\n"    /* invalidate I-cache */
        /* Disable MMU + D-cache + I-cache */
        "mrc p15, 0, r0, c1, c0, 0\n"
        "bic r0, r0, #5\n"               /* clear M(0) + C(2) */
        "bic r0, r0, #0x1000\n"          /* clear I(12) */
        "mcr p15, 0, r0, c1, c0, 0\n"
        /* Zero the vector table to prevent stale exception handlers */
        "mov r0, #0\n"
        "mov r1, #0\n"
        "str r1, [r0, #0x00]\n"          /* reset vector = 0 */
        "str r1, [r0, #0x04]\n"          /* undef vector = 0 */
        "str r1, [r0, #0x08]\n"          /* swi vector = 0 */
        "str r1, [r0, #0x0C]\n"          /* prefetch abort = 0 */
        "str r1, [r0, #0x10]\n"          /* data abort = 0 */
        "str r1, [r0, #0x18]\n"          /* irq = 0 */
        "str r1, [r0, #0x1C]\n"          /* fiq = 0 */
        "ldr sp, =0x2AFFC\n"
        "bl  do_main\n"
        "1: b 1b\n"
    );
}
