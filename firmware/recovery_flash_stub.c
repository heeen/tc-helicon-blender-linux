/* Recovery flash stub — works from TCAT-BOOT loader mode.
 *
 * Uses ONLY DMA TX for all SPI commands. For RDSR (which needs to read
 * a response byte), we send the command via DMA TX, then read the SPI
 * RX FIFO directly — no bidirectional DMA, no PIO.
 *
 * Proven patterns:
 *   DMA TX: SPI_CTRL=0x107, DMAMD=2, CFG=0xF4009000, TRG=0xD005
 *   BP clear: EWSR(0x50) + WRSR(0x01,0x00) via DMA TX — confirmed working
 *
 * Mailbox at 0x2E000:
 *   [0] status: 0=idle, 1=running, 2=done_ok, 0xFF=error
 *   [4] command: 0=idle, 6=flash_all
 *   [8] progress (sector index)
 *   [12] errors
 *   [16] total sectors
 *
 * Sector list at 0x2F100: array of {spi_addr:u32, sram_offset:u32}
 *   terminated by spi_addr == 0xFFFFFFFF
 * Image data at 0x2F900.
 */

#include <stdint.h>

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

#define SPI_TX_PORT (SPI_BASE + 0x80)

#define DMA_BASE    0x80000000
#define DMA_EN      (*(volatile uint32_t *)(DMA_BASE + 0x08))
#define DMA_ICLR    (*(volatile uint32_t *)(DMA_BASE + 0x10))
#define DMA_ISTAT   (*(volatile uint32_t *)(DMA_BASE + 0x14))
#define DMA_CHCLR(n) (*(volatile uint32_t *)(DMA_BASE + 0x30 + (n)*4))
#define DMA_CHREG(ch, off) (*(volatile uint32_t *)(DMA_BASE + 0x100 + (ch)*0x20 + (off)))

#define SECTOR_SIZE 0x1000

struct mailbox {
    volatile uint32_t status;
    volatile uint32_t command;
    volatile uint32_t progress;
    volatile uint32_t errors;
    volatile uint32_t total;
};
#define MBOX ((struct mailbox *)0x2E000)

/* Scratch buffers at fixed SRAM addresses */
#define TX_SCRATCH ((volatile uint8_t *)0x2E020)
#define DATA_BUF   ((volatile uint8_t *)0x2B000)

struct sector_entry { uint32_t spi_addr, sram_offset; };
#define SECTOR_LIST ((struct sector_entry *)0x2F100)
#define IMAGE_BASE  ((const uint8_t *)0x2F900)

static inline void dwb(void) {
    __asm__ volatile("mcr p15, 0, %0, c7, c10, 4" :: "r"(0) : "memory");
}

static void spi_reset(void) {
    SPI_CS = 0; SPI_EN = 0; SPI_DMAGO = 0; SPI_DMAMD = 0;
    SPI_DMACFG0 = 0; SPI_DMACFG1 = 0;
    DMA_EN = 0; DMA_ICLR = 3; DMA_CHCLR(0) = 1; DMA_CHCLR(1) = 1;
    dwb();
    for (volatile int i = 0; i < 1000; i++)
        if (!(SPI_STAT & 1)) break;
}

/* DMA TX: send nbytes from buf. Proven working in TCAT-BOOT. */
static int dma_tx(const volatile uint8_t *buf, uint32_t len) {
    dwb();
    SPI_EN = 0; SPI_CS = 0; SPI_DMAGO = 0;
    SPI_CTRL = 0x107; SPI_LEN = len - 1; SPI_DMAMD = 2; SPI_CLK = 2;
    SPI_DMACFG0 = 4; SPI_DMACFG1 = 3;
    dwb(); SPI_EN = 1; dwb();

    DMA_EN = 1; DMA_ICLR = 1; DMA_CHCLR(0) = 1; dwb();
    DMA_CHREG(0, 0x10) = 0;
    DMA_CHREG(0, 0x00) = (uint32_t)(uintptr_t)buf;
    DMA_CHREG(0, 0x04) = SPI_TX_PORT;
    DMA_CHREG(0, 0x08) = 0;
    DMA_CHREG(0, 0x0C) = len | 0xF4009000;
    dwb();
    DMA_CHREG(0, 0x10) = 0xD005;
    dwb();
    SPI_CS = 1; SPI_CLRINT = 0; dwb();

    for (volatile int i = 0; i < 500000; i++) {
        if (DMA_ISTAT & 1) {
            while (SPI_STAT & 1) {}
            SPI_DMAGO = 1;
            SPI_CS = 0; SPI_EN = 0;
            return 0;
        }
    }
    SPI_CS = 0; SPI_EN = 0;
    return -1;
}

static void spi_cmd1(uint8_t c) { TX_SCRATCH[0]=c; dma_tx(TX_SCRATCH,1); }
static void spi_cmd2(uint8_t a, uint8_t b) { TX_SCRATCH[0]=a; TX_SCRATCH[1]=b; dma_tx(TX_SCRATCH,2); }
static void spi_cmd3(uint8_t a, uint8_t b, uint8_t c) {
    TX_SCRATCH[0]=a; TX_SCRATCH[1]=b; TX_SCRATCH[2]=c; dma_tx(TX_SCRATCH,3);
}
static void spi_cmd4(uint8_t a, uint8_t b, uint8_t c, uint8_t d) {
    TX_SCRATCH[0]=a; TX_SCRATCH[1]=b; TX_SCRATCH[2]=c; TX_SCRATCH[3]=d; dma_tx(TX_SCRATCH,4);
}

/* Blind delay — RDSR doesn't work with DMA TX only (no RX path).
 * SST25VF016B max times: tBP=10µs, tSE=25ms, tWRSR=10ms.
 * At ~196MHz, 1000 loop iterations ≈ 30µs. */
static void delay_us(uint32_t us) {
    volatile uint32_t n = us * 33;  /* ~33 iterations per µs at 196MHz */
    while (n--) {}
}

static void wait_bp(void)    { delay_us(20); }     /* tBP 10µs + 2x margin */
static void wait_erase(void) { delay_us(30000); }  /* tSE 25ms + margin */
static void wait_wrsr(void)  { delay_us(15000); }  /* tWRSR 10ms + margin */

static void bp_clear(void) {
    spi_cmd1(0x50);        /* EWSR */
    spi_cmd2(0x01, 0x00);  /* WRSR = 0 */
    wait_wrsr();
}

static void sector_erase(uint32_t addr) {
    spi_cmd1(0x06);  /* WREN */
    spi_cmd4(0x20, (addr >> 16) & 0xFF, (addr >> 8) & 0xFF, addr & 0xFF);
    wait_erase();
}

/* AAI word-program: write 4KB sector from DATA_BUF to SPI addr.
 * SST25 AAI writes 2 bytes at a time after initial 4-byte setup. */
static void sector_write(uint32_t addr) {
    uint8_t a2 = (addr >> 16) & 0xFF;
    uint8_t a1 = (addr >> 8) & 0xFF;
    uint8_t a0 = addr & 0xFF;

    spi_cmd1(0x06);  /* WREN */

    /* First AAI pair: [0xAD, addr2, addr1, addr0, data0, data1] */
    TX_SCRATCH[0] = 0xAD;
    TX_SCRATCH[1] = a2;
    TX_SCRATCH[2] = a1;
    TX_SCRATCH[3] = a0;
    TX_SCRATCH[4] = DATA_BUF[0];
    TX_SCRATCH[5] = DATA_BUF[1];
    dma_tx(TX_SCRATCH, 6);
    wait_bp();

    /* Subsequent pairs: [0xAD, data_n, data_n+1] */
    for (uint32_t i = 2; i < SECTOR_SIZE; i += 2) {
        spi_cmd3(0xAD, DATA_BUF[i], DATA_BUF[i + 1]);
        wait_bp();
    }

    spi_cmd1(0x04);  /* WRDI — exit AAI mode */
}

static void do_flash_all(void) {
    struct sector_entry *list = SECTOR_LIST;
    uint32_t total = 0;
    while (list[total].spi_addr != 0xFFFFFFFF && total < 256) total++;
    MBOX->total = total;

    bp_clear();

    for (uint32_t i = 0; i < total; i++) {
        uint32_t addr = list[i].spi_addr;
        const uint8_t *src = IMAGE_BASE + list[i].sram_offset;

        /* Copy to DATA_BUF */
        for (uint32_t j = 0; j < SECTOR_SIZE; j++)
            DATA_BUF[j] = src[j];

        MBOX->progress = i;

        sector_erase(addr);
        sector_write(addr);
        /* No verify — we'll verify after reboot */
    }

    MBOX->progress = total;
}

void do_main(void) {
    struct mailbox *mb = MBOX;
    spi_reset();
    spi_cmd1(0x04);  /* WRDI in case stuck in AAI */
    mb->status = 0; mb->command = 0; mb->progress = 0; mb->errors = 0;

    for (;;) {
        uint32_t cmd = mb->command;
        if (cmd == 0) continue;
        mb->status = 1;
        switch (cmd) {
        case 5: bp_clear(); break;
        case 6: do_flash_all(); break;
        case 7: spi_reset(); break;
        default: break;
        }
        mb->command = 0;
        mb->status = 2;
    }
}

void _start(void) __attribute__((naked, section(".text.entry")));
void _start(void) {
    __asm__ volatile(
        "msr cpsr_c, #0xd3\n"
        "mov r0, #0\n"
        "mcr p15, 0, r0, c7, c6, 0\n"
        "mcr p15, 0, r0, c7, c5, 0\n"
        "ldr sp, =0x2AFFC\n"
        "bl  do_main\n"
        "1: b 1b\n"
    );
}
