/*
 * Shared soft-reboot: TCAT bootloader @ XIP 0x4F000.
 * Used by firmware/patch (handlers.c) and JTAG-loaded reboot_stub.c.
 */
#ifndef REBOOT_COMMON_H
#define REBOOT_COMMON_H

#include <stdint.h>

/* Clear [0x200, ram_clear_end); if skip_hi > skip_lo, skip [skip_lo, skip_hi)
 * (stub lives at REBOOT_STUB_LOAD_ADDR — do not wipe while executing). */
void reboot_to_tcat_bootloader(uint32_t ram_clear_end,
                               uint32_t skip_lo, uint32_t skip_hi)
    __attribute__((noreturn));

void usb_hw_reset(void);
void spi_ip_block_quiesce(volatile uint32_t *s);
void dma_engine_full_reset(volatile uint32_t *dma);

/* Timer0/Timer1 disable @ 0xC2000000. eCos uses Timer0 for the alarm
 * thread; leaving it running sends tick IRQs we don't service. */
void timer_blocks_disable(void);

/* TCAT audio mixer @ 0xC4000000. Stops any active audio stream so its
 * DMA requests don't fire while we're flashing. */
void mixer_block_quiesce(void);

/* Drain SPI RX FIFO by reading DATA (+0x60) until !RX_RDY. Call after
 * spi_ip_block_quiesce to clear any stale bytes. */
void spi_ip_drain_rx(volatile uint32_t *s);

/* Superset teardown: USB + SPI (flash + LED) + DMA (ch0-3) + VIC +
 * Timer + Mixer. Idempotent and safe in both TCAT-BOOT and eCos
 * states — TCAT-BOOT sees most of these as no-ops because peripherals
 * are already quiet; eCos gets properly quiesced. Does NOT touch 0xC9
 * clock/PLL block (write-key-protected, easy to brick). Callers
 * (v2 driver, reboot stub, handlers) reapply clocks they need after. */
void peripheral_full_teardown(void);

/* Strong symbol in each link (uart_print_string vs minimal uart_puts). */
void reboot_uart_line(const char *s);

#define REBOOT_SRAM_CLEAR_END  0x32600u
#define REBOOT_STUB_LOAD_ADDR  0x2B000u
/* Must cover reboot_stub.bin; linker places .text at LOAD_ADDR */
#define REBOOT_STUB_RESERVE    0x1000u

#define REBOOT_LED_SPI   ((volatile uint32_t *)0xCF000000u)
#define REBOOT_LED_GPIO  (*(volatile uint32_t *)0xCB000020)

#endif
