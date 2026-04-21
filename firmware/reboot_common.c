/*
 * Single implementation for soft-reboot (handlers + JTAG stub).
 * Combines: full ChipIdea USB RST + TCAT quiesce (handlers), full DMA ch0–3
 * teardown, optional SRAM scrub with stub skip, UART drain before XIP jump (stub).
 */

#include "reboot_common.h"
#include "dice_usb_regs.h"

#define FLASH_SPI_BASE   ((volatile uint32_t *)0xCC000000u)
#define VIC_INT_EN_CLR   (*(volatile uint32_t *)0xFFFFF014)
#define VIC_SOFT_CLR     (*(volatile uint32_t *)0xFFFFF01C)

__attribute__((weak)) void reboot_uart_line(const char *s) { (void)s; }

void usb_hw_reset(void) {
    USB_USBSTS = 0xFFFFFFFF;
    USB_USBCMD &= ~(uint32_t)1;
    USB_USBCMD |= 2;
    for (volatile int i = 0; i < 100000; i++) {
        if (!(USB_USBCMD & 2))
            break;
    }
    USB_USBSTS = 0xFFFFFFFFu;
    USB_USBINTR = 0;
    USB_USBCMD &= ~(uint32_t)(USBCMD_RS | USBCMD_SUTW | USBCMD_ATDTW);
    USB_DEVICEADDR = 0;
    USB_ENDPTLISTADDR = 0;
    TCAT_USBMODE = 2u;
    TCAT_EP_COMP_STATUS = 0xFFFFFFFFu;
    TCAT_EP_COMP_ENABLE = 0;
    TCAT_EP_ASYNC_PRIME = 0;
    TCAT_EP_RX_EN = 0;
    TCAT_EP_TX_EN = 0;
    __asm__ volatile("mcr p15, 0, %0, c7, c10, 4" ::"r"(0) : "memory");
    for (volatile int i = 0; i < 400000; i++) {}
}

void spi_ip_block_quiesce(volatile uint32_t *s) {
    for (volatile int i = 0; i < 100000; i++) {
        if (!(s[0x28 / 4] & 1u))
            break;
    }
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
    __asm__ volatile("mcr p15, 0, %0, c7, c10, 4" ::"r"(0) : "memory");
}

void dma_engine_full_reset(volatile uint32_t *dma) {
    unsigned ch;
    dma[0x08 / 4] = 0;
    dma[0x10 / 4] = 0x0Fu;
    for (ch = 0; ch < 4; ch++)
        dma[0x30 / 4 + ch] = 1;
    for (ch = 0; ch < 4; ch++) {
        volatile uint32_t *b = (volatile uint32_t *)(0x80000100u + ch * 0x20u);
        b[0x10 / 4] = 0;
        b[0x0C / 4] = 0;
        b[0x08 / 4] = 0;
        b[0x00 / 4] = 0;
        b[0x04 / 4] = 0;
    }
    __asm__ volatile("mcr p15, 0, %0, c7, c10, 4" ::"r"(0) : "memory");
}

void timer_blocks_disable(void) {
    /* Timer0 (+0x00..+0x0B) and Timer1 (+0x20..+0x2B) at 0xC2000000.
     * Writing 0 to CTRL (+0x08/+0x28) stops the counter and masks its
     * IRQ line. Verified 2026-04-17 via diag_timer_state.py: eCos runs
     * Timer0 at reload=0x493E0 / ~18 MHz tick. */
    *(volatile uint32_t *)0xC2000008 = 0;  /* Timer0 CTRL */
    *(volatile uint32_t *)0xC200002C = 0;  /* Timer1 CTRL */
}

void mixer_block_quiesce(void) {
    /* TCAT audio mixer / crossbar @ 0xC4000000. Writing 0 to its main
     * enable/control register (+0x00) stops channel routing. Full
     * register map unknown, but zeroing the first 64 bytes matches
     * what boot_check_and_load does for initialization and has been
     * observed non-destructive in both states (audio silences, chip
     * stays alive). Leaves clock source alone. */
    volatile uint32_t *mx = (volatile uint32_t *)0xC4000000u;
    for (unsigned i = 0; i < 16; i++) mx[i] = 0;
    __asm__ volatile("mcr p15, 0, %0, c7, c10, 4" ::"r"(0) : "memory");
}

void spi_ip_drain_rx(volatile uint32_t *s) {
    /* STAT (+0x28) bit 3 = RX_RDY. DATA (+0x60) reads a byte from RX
     * FIFO and clears it. Drain up to a small bound so a stuck bit
     * doesn't hang us. */
    for (int i = 0; i < 32; i++) {
        if (!(s[0x28 / 4] & 0x08u)) break;
        (void)s[0x60 / 4];
    }
}

void peripheral_full_teardown(void) {
    /* Order matters: USB first (it has DMA clients), then SPIs, then
     * the DMA engine itself, then VIC (so no IRQ fires while we write
     * the rest), then timers and mixer. Caller must already have
     * masked CPSR.I — we don't touch CPU mode here. */
    volatile uint32_t *flash_spi = FLASH_SPI_BASE;
    volatile uint32_t *led_spi   = REBOOT_LED_SPI;
    volatile uint32_t *dma       = (volatile uint32_t *)0x80000000u;

    /* Drop SPI block clock mux back to fallback crystal BEFORE any SPI
     * ops. eCos / stage-2 leave CLK_DIV_SPI (0xC9000014) = 0x8000
     * (passthrough from 400 MHz PLL); combined with our SPI_CLK=2
     * divider that's 200 MHz on the wire, past the SST25VF016B spec.
     * Writing 0xABCD0000 (with the 0xC9 block's required write key)
     * disables the PLL mux → SPI runs at the familiar ~3 MHz rate
     * v2 driver was tuned for. Observed 2026-04-21: omitting this
     * causes BP_CLEAR TIMEOUT after nRESET-then-boot-brief wait. */
    *(volatile uint32_t *)0xC9000014u = 0xABCD0000u;
    __asm__ volatile("mcr p15, 0, %0, c7, c10, 4" ::"r"(0) : "memory");

    usb_hw_reset();

    spi_ip_block_quiesce(led_spi);
    spi_ip_drain_rx(led_spi);
    REBOOT_LED_GPIO = 0;
    led_spi[0x14 / 4] = 0xFF;

    spi_ip_block_quiesce(flash_spi);
    spi_ip_drain_rx(flash_spi);

    dma_engine_full_reset(dma);

    VIC_INT_EN_CLR = 0xFFFFFFFFu;
    VIC_SOFT_CLR   = 0xFFFFFFFFu;
    /* Clear vectored-IRQ slot controls so stale handler bindings from
     * firmware don't route a later pending IRQ anywhere useful. */
    for (unsigned i = 0; i < 16; i++)
        *(volatile uint32_t *)(0xFFFFF200u + i * 4) = 0;

    timer_blocks_disable();
    mixer_block_quiesce();

    __asm__ volatile("mcr p15, 0, %0, c7, c10, 4" ::"r"(0) : "memory");
}

static void reboot_clear_sram(uint32_t end_exclusive, uint32_t skip_lo,
                              uint32_t skip_hi) {
    volatile uint32_t *p = (volatile uint32_t *)0x200;
    volatile uint32_t *const end = (volatile uint32_t *)end_exclusive;
    while (p < end) {
        uintptr_t a = (uintptr_t)p;
        if (skip_hi > skip_lo && a >= skip_lo && a < skip_hi) {
            p = (volatile uint32_t *)skip_hi;
            continue;
        }
        *p++ = 0;
    }
}

void reboot_to_tcat_bootloader(uint32_t ram_clear_end, uint32_t skip_lo,
                               uint32_t skip_hi) {
    volatile uint32_t *spi = FLASH_SPI_BASE;
    volatile uint32_t *led_spi = REBOOT_LED_SPI;
    volatile uint32_t *dma = (volatile uint32_t *)0x80000000;

    reboot_uart_line("[reboot] start\r\n");

    usb_hw_reset();
    reboot_uart_line("[reboot] usb off\r\n");

    __asm__ volatile("mov r0, #0xD3 \n msr cpsr_c, r0 \n" ::: "r0");
    reboot_uart_line("[reboot] irq off\r\n");

    /* Inlined instead of peripheral_full_teardown() because the patch-
     * build's PATCH region is size-constrained (6528 B) and doesn't
     * need the timer/mixer/extended-VIC teardown the v2 driver and
     * reboot_stub get via peripheral_full_teardown(). If the patch
     * ever needs eCos-level quiesce, call peripheral_full_teardown()
     * here and grow PATCH in patch.ld. */
    *(volatile uint32_t *)0xC2000008 = 0;
    *(volatile uint32_t *)0xC200002C = 0;

    spi_ip_block_quiesce(led_spi);
    REBOOT_LED_GPIO = 0;
    led_spi[0x14 / 4] = 0xFF;
    __asm__ volatile("mcr p15, 0, %0, c7, c10, 4" ::"r"(0) : "memory");
    reboot_uart_line("[reboot] led_spi off\r\n");

    spi_ip_block_quiesce(spi);
    reboot_uart_line("[reboot] flash_spi off\r\n");

    dma_engine_full_reset(dma);
    reboot_uart_line("[reboot] dma off\r\n");

    VIC_INT_EN_CLR = 0xFFFFFFFFu;
    VIC_SOFT_CLR = 0xFFFFFFFFu;

    spi[0x00 / 4] = 1;
    spi[0x04 / 4] = 2;
    spi[0x14 / 4] = 0x38;
    __asm__ volatile("mcr p15, 0, %0, c7, c10, 4" ::"r"(0) : "memory");
    reboot_uart_line("[reboot] xip ok\r\n");

    if (ram_clear_end > 0x200)
        reboot_clear_sram(ram_clear_end, skip_lo, skip_hi);
    reboot_uart_line("[reboot] ram clear\r\n");

    reboot_uart_line("[reboot] jump 0x4F000\r\n");
    for (volatile int i = 0; i < 50000; i++) {}

    __asm__ volatile("mov r0, #0          \n"
                     "mcr p15, 0, r0, c7, c5, 0 \n"
                     "mcr p15, 0, r0, c7, c6, 0 \n"
                     "ldr pc, =0x4F000    \n"
                     ::: "r0", "memory");
    __builtin_unreachable();
}
