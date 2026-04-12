/* SRAM stub to clear SPI flash block protection via DMA TX.
 *
 * The DICE3 SPI flash controller is DMA-only for actual data transfer.
 * PIO via SPI_DATA only works for the initial command bytes when DMA
 * is armed. The bootloader confirms this — all flash ops use DMA.
 *
 * This stub replicates the bootloader's DMA pattern but for TX writes
 * (EWSR + WRSR) to clear block protection on the SST25VF016B. */

#include <stdint.h>

#define SPI_BASE    0xCC000000
#define SPI_CTRL    (*(volatile uint32_t *)(SPI_BASE + 0x00))
#define SPI_LEN     (*(volatile uint32_t *)(SPI_BASE + 0x04))
#define SPI_EN      (*(volatile uint32_t *)(SPI_BASE + 0x08))
#define SPI_CS      (*(volatile uint32_t *)(SPI_BASE + 0x10))
#define SPI_CLK     (*(volatile uint32_t *)(SPI_BASE + 0x14))
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
#define DMA_CHCLR(n) (*(volatile uint32_t *)(DMA_BASE + 0x30 + (n)*4))
#define DMA_CH0_SRC (*(volatile uint32_t *)(DMA_BASE + 0x100))
#define DMA_CH0_DST (*(volatile uint32_t *)(DMA_BASE + 0x104))
#define DMA_CH0_NXT (*(volatile uint32_t *)(DMA_BASE + 0x108))
#define DMA_CH0_CFG (*(volatile uint32_t *)(DMA_BASE + 0x10C))
#define DMA_CH0_TRG (*(volatile uint32_t *)(DMA_BASE + 0x110))

#define SPI_TX_PORT (SPI_BASE + 0x80)  /* DMA TX port */
#define SPI_RX_PORT (SPI_BASE + 0x70)  /* DMA RX port */

#define RESULT_ADDR 0x2AF00
static volatile uint32_t *result = (volatile uint32_t *)RESULT_ADDR;

/* Scratch buffer for DMA source data — must be in SRAM */
static uint8_t __attribute__((aligned(4))) scratch[16] __attribute__((section(".data")));

static inline void dwb(void) {
    __asm__ volatile("mcr p15, 0, %0, c7, c10, 4" :: "r"(0) : "memory");
}

static void spi_dma_reset(void) {
    SPI_CS = 0;
    SPI_EN = 0;
    SPI_DMAGO = 0;
    SPI_DMAMD = 0;
    SPI_DMACFG0 = 0;
    SPI_DMACFG1 = 0;
    DMA_EN = 0;
    DMA_ICLR = 3;
    DMA_CHCLR(0) = 1;
    DMA_CHCLR(1) = 1;
    dwb();
    for (volatile int i = 0; i < 1000; i++) {
        if (!(SPI_STAT & 1)) break;
    }
}

/* DMA TX transfer: send nbytes from src buffer to SPI flash.
 * Replicates the bootloader's DMA pattern but for TX direction. */
static void dma_spi_tx(const uint8_t *src, uint32_t nbytes) {
    spi_dma_reset();

    /* SPI setup for TX */
    SPI_CTRL = 0x107;         /* TX mode (same as firmware PIO) */
    SPI_LEN = nbytes - 1;
    SPI_DMAMD = 2;            /* DMA TX mode */
    SPI_CLK = 2;
    dwb();

    /* DMA channel 0: src buffer → SPI TX port
     * CFG = 0xF4009000: proven working TX pattern from sram_flash_driver.c
     * TRG = 0xD005: proven working TX trigger */
    DMA_EN = 1;
    DMA_ICLR = 1;
    DMA_CHCLR(0) = 1;
    DMA_CH0_TRG = 0;
    DMA_CH0_SRC = (uint32_t)src;
    DMA_CH0_DST = SPI_TX_PORT;
    DMA_CH0_NXT = 0;
    DMA_CH0_CFG = (nbytes & 0xFFF) | 0xF4009000;
    dwb();

    /* Start: enable SPI, assert CS, trigger DMA */
    SPI_EN = 1;
    DMA_CH0_TRG = 0xD005;
    SPI_CS = 1;
    dwb();

    /* Wait for SPI completion */
    for (volatile int i = 0; i < 100000; i++) {
        if (!(SPI_STAT & 1)) break;
    }

    /* Cleanup */
    SPI_CS = 0;
    SPI_EN = 0;
    SPI_DMAMD = 0;
    DMA_EN = 0;
    DMA_ICLR = 1;
    dwb();
}

/* DMA RX transfer: send tx_len bytes, receive rx_len bytes back.
 * Used for RDSR: send opcode, read status byte. */
static uint8_t dma_spi_rdsr(void) {
    uint8_t txbuf[4] = {0x05, 0x00};  /* RDSR opcode + dummy */
    uint8_t rxbuf[4] = {0xFF, 0xFF};

    spi_dma_reset();

    /* Bidirectional: TX sends command, RX captures response */
    SPI_CTRL = 0x307;         /* Bidirectional mode (bootloader uses this for reads) */
    SPI_LEN = 1;              /* 2 bytes total */
    SPI_DMAMD = 3;            /* Bidirectional DMA */
    SPI_CLK = 2;
    dwb();

    /* DMA ch0 (RX): SPI RX port → rxbuf */
    DMA_EN = 3;               /* Enable ch0 + ch1 */
    DMA_ICLR = 3;
    DMA_CHCLR(0) = 1;
    DMA_CHCLR(1) = 1;

    DMA_CH0_TRG = 0;
    DMA_CH0_SRC = SPI_RX_PORT;
    DMA_CH0_DST = (uint32_t)rxbuf;
    DMA_CH0_NXT = 0;
    DMA_CH0_CFG = 2 | 0x88009000;
    dwb();
    DMA_CH0_TRG = 0xD007;

    /* DMA ch1 (TX): txbuf → SPI TX port */
    *(volatile uint32_t *)(DMA_BASE + 0x130) = 0;  /* ch1 TRG */
    *(volatile uint32_t *)(DMA_BASE + 0x120) = (uint32_t)txbuf;  /* ch1 SRC */
    *(volatile uint32_t *)(DMA_BASE + 0x124) = SPI_TX_PORT;      /* ch1 DST */
    *(volatile uint32_t *)(DMA_BASE + 0x128) = 0;                /* ch1 NXT */
    *(volatile uint32_t *)(DMA_BASE + 0x12C) = 0xFFF | 0xF4009000; /* ch1 CFG: max count */
    dwb();

    SPI_EN = 1;
    *(volatile uint32_t *)(DMA_BASE + 0x130) = 0xD005; /* ch1 TRG (TX) */
    SPI_CS = 1;
    dwb();

    for (volatile int i = 0; i < 100000; i++) {
        if (!(SPI_STAT & 1)) break;
    }

    SPI_CS = 0;
    SPI_EN = 0;
    SPI_DMAMD = 0;
    DMA_EN = 0;
    DMA_ICLR = 3;
    dwb();

    return rxbuf[1];  /* Skip first byte (received during opcode TX) */
}

void __attribute__((noreturn, section(".text.entry"))) bp_clear_entry(void) {
    __asm__ volatile ("mov r0, #0xD3 \n msr cpsr_c, r0 \n" ::: "r0");

    result[0] = 0;  /* running */
    result[1] = 0;
    result[2] = 0;
    result[3] = 0;

    spi_dma_reset();

    /* Read initial status register */
    result[1] = dma_spi_rdsr();

    /* EWSR (0x50) — 1 byte DMA TX */
    scratch[0] = 0x50;
    dma_spi_tx(scratch, 1);

    /* WRSR (0x01, 0x00) — 2 byte DMA TX */
    scratch[0] = 0x01;
    scratch[1] = 0x00;
    dma_spi_tx(scratch, 2);

    /* Wait for WRSR to complete (WIP bit) */
    for (volatile int i = 0; i < 100000; i++) {
        uint8_t sr = dma_spi_rdsr();
        if (!(sr & 1)) break;
    }

    /* Read final status register */
    result[2] = dma_spi_rdsr();
    result[0] = (result[2] == 0) ? 0x4F4B : 0xDEAD;

    for (;;) {}
}
