/* Minimal soft-reboot stub — uploaded to SRAM work area and executed via JTAG.
 * Mirrors software_reboot() from handlers.c but standalone (no firmware deps). */

#include <stdint.h>

#define USB_USBCMD      (*(volatile uint32_t *)0x90000008)
#define USB_ENDPTFLUSH  (*(volatile uint32_t *)0x90000014)

static void uart_putc(char c) {
    volatile uint32_t *uart = (volatile uint32_t *)0xC5000000;
    for (volatile int i = 0; i < 5000; i++) {
        if (uart[1] & (1 << 5)) { uart[0] = c; return; }
    }
    /* UART not ready (e.g. TCAT-BOOT mode) — skip */
}

static void uart_puts(const char *s) {
    while (*s) uart_putc(*s++);
}

void __attribute__((noreturn, section(".text.entry"))) reboot_entry(void) {
    volatile uint32_t *spi = (volatile uint32_t *)0xCC000000;
    volatile uint32_t *dma = (volatile uint32_t *)0x80000000;

    uart_puts("[reboot] start\r\n");

    /* USB disconnect */
    USB_ENDPTFLUSH = 0xFFFFFFFF;
    USB_USBCMD &= ~(uint32_t)1;
    USB_USBCMD |= 2;
    for (volatile int i = 0; i < 100000; i++) {
        if (!(USB_USBCMD & 2)) break;
    }
    uart_puts("[reboot] usb off\r\n");

    /* SVC mode + disable IRQ+FIQ */
    __asm__ volatile ("mov r0, #0xD3 \n msr cpsr_c, r0 \n" ::: "r0");

    /* Stop timer */
    *(volatile uint32_t *)0xC2000008 = 0;

    /* Wait for SPI idle */
    for (volatile int i = 0; i < 100000; i++) {
        if (!(spi[0x28 / 4] & 1)) break;
    }
    uart_puts("[reboot] spi idle\r\n");

    /* Disable SPI */
    spi[0x10 / 4] = 0;    /* SPI_CS */
    spi[0x08 / 4] = 0;    /* SPI_EN */
    spi[0x2C / 4] = 0;    /* SPI_DMAGO */
    spi[0x4C / 4] = 0;    /* SPI_DMAMD */
    spi[0x50 / 4] = 0;    /* SPI_DMACFG0 */
    spi[0x54 / 4] = 0;    /* SPI_DMACFG1 */
    __asm__ volatile ("mcr p15, 0, %0, c7, c10, 4" :: "r"(0) : "memory");

    /* DMA teardown */
    dma[0x08 / 4] = 0;    /* DMA_EN */
    dma[0x10 / 4] = 3;    /* DMA_ICLR */
    dma[0x30 / 4] = 1;    /* CHCLR(0) */
    dma[0x34 / 4] = 1;    /* CHCLR(1) */
    *(volatile uint32_t *)(0x80000110) = 0;
    *(volatile uint32_t *)(0x8000010C) = 0;
    *(volatile uint32_t *)(0x80000130) = 0;
    *(volatile uint32_t *)(0x8000012C) = 0;
    __asm__ volatile ("mcr p15, 0, %0, c7, c10, 4" :: "r"(0) : "memory");
    uart_puts("[reboot] dma off\r\n");

    /* Restore SPI to XIP */
    spi[0x00 / 4] = 1;    /* SPI_CTRL */
    spi[0x04 / 4] = 2;    /* SPI_LEN */
    spi[0x14 / 4] = 0x38; /* SPI_CLK = XIP */
    __asm__ volatile ("mcr p15, 0, %0, c7, c10, 4" :: "r"(0) : "memory");
    uart_puts("[reboot] xip ok\r\n");

    uart_puts("[reboot] jump 0x4F000\r\n");
    /* Drain UART TX before jumping */
    for (volatile int i = 0; i < 50000; i++) {}

    /* Invalidate caches and jump to bootloader */
    __asm__ volatile (
        "mov r0, #0          \n"
        "mcr p15, 0, r0, c7, c5, 0 \n"
        "mcr p15, 0, r0, c7, c6, 0 \n"
        "ldr pc, =0x4F000    \n"
        ::: "r0", "memory"
    );
    __builtin_unreachable();
}
