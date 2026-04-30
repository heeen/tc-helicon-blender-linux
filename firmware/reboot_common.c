/*
 * Single implementation for soft-reboot (handlers + JTAG stub).
 * Combines: DWC2 warm-handoff quiesce (detach + EP/FIFO drain), full DMA
 * ch0–7 teardown, optional SRAM scrub with stub skip, UART drain before XIP
 * jump (stub).
 */

#include "reboot_common.h"
#include "blender_periph_lib.h"

#define FLASH_SPI_BASE   ((volatile uint32_t *)0xCC000000u)
#define DMA_CH_COUNT     8u

__attribute__((weak)) void reboot_uart_line(const char *s) { (void)s; }

void usb_hw_reset(void) {
    blender_usb_warm_handoff_reset();
}

void spi_ip_block_quiesce(volatile uint32_t *s) {
    blender_spi_ip_block_quiesce(s);
}

void dma_engine_full_reset(volatile uint32_t *dma) {
    blender_dma_engine_reset(dma, DMA_CH_COUNT);
}

void timer_blocks_disable(void) {
    blender_timer_blocks_disable();
}

void mixer_block_quiesce(void) {
    blender_mixer_block_quiesce();
}

void spi_ip_drain_rx(volatile uint32_t *s) {
    blender_spi_ip_drain_rx(s);
}

void peripheral_full_teardown(void) {
    /* Order matters: USB first (it has DMA clients), then SPIs, then
     * the DMA engine itself, then VIC (so no IRQ fires while we write
     * the rest), then timers and mixer. Caller must already have
     * masked CPSR.I — we don't touch CPU mode here. */
    blender_periph_cfg_t cfg = {
        .spi_clk_div_raw = 0u,
        .dma_channel_count = DMA_CH_COUNT,
        .clear_vic_vector_slots = 1u,
    };
    volatile uint32_t *led_gpio = (volatile uint32_t *)&REBOOT_LED_GPIO;
    blender_peripheral_full_teardown(
        &cfg, FLASH_SPI_BASE, REBOOT_LED_SPI, led_gpio);
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

    reboot_uart_line("[reboot] start\r\n");

    __asm__ volatile("mov r0, #0xD3 \n msr cpsr_c, r0 \n" ::: "r0");
    reboot_uart_line("[reboot] irq off\r\n");

    /* Use the same full quiesce sequence proven in the flash driver:
     * USB + SPI + DMA + VIC + timer + mixer and SPI parent-clock fallback. */
    peripheral_full_teardown();
    reboot_uart_line("[reboot] periph off\r\n");

    spi[0x00 / 4] = 1;
    spi[0x04 / 4] = 2;
    spi[0x14 / 4] = 0x38;
    __asm__ volatile("mcr p15, 0, %0, c7, c10, 4" ::"r"(0) : "memory");
    reboot_uart_line("[reboot] xip ok\r\n");

    if (ram_clear_end > 0x200) {
        /* Critical: reboot_clear_sram wipes low SRAM progressively from
         * 0x200 upward. The caller's live stack is often in that range
         * (~0x2AFxx in eCos / reboot stub path), so include a guard window
         * around current SP in the skip range to avoid self-clobber while
         * this function is still executing. */
        uint32_t dyn_lo = skip_lo, dyn_hi = skip_hi;
        uintptr_t sp_now;
        __asm__ volatile("mov %0, sp" : "=r"(sp_now));
        if (sp_now >= 0x200u && sp_now < ram_clear_end) {
            uint32_t guard_lo = (sp_now > 0x1000u) ? (uint32_t)(sp_now - 0x1000u) : 0x200u;
            uint32_t guard_hi = (uint32_t)(sp_now + 0x1000u);
            if (guard_hi > ram_clear_end) guard_hi = ram_clear_end;
            if (dyn_hi > dyn_lo) {
                if (guard_lo < dyn_lo) dyn_lo = guard_lo;
                if (guard_hi > dyn_hi) dyn_hi = guard_hi;
            } else {
                dyn_lo = guard_lo;
                dyn_hi = guard_hi;
            }
        }
        reboot_clear_sram(ram_clear_end, dyn_lo, dyn_hi);
    }
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
