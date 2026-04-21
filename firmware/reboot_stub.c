/* JTAG-uploaded entry @ 0x2B000 — logic lives in reboot_common.c */

#include <stdint.h>
#include "reboot_common.h"

static void uart_putc(char c) {
    volatile uint32_t *uart = (volatile uint32_t *)0xC5000000;
    for (volatile int i = 0; i < 5000; i++) {
        if (uart[1] & (1 << 5)) {
            uart[0] = c;
            return;
        }
    }
}

static void uart_puts(const char *s) {
    while (*s)
        uart_putc(*s++);
}

void reboot_uart_line(const char *s) { uart_puts(s); }

void __attribute__((noreturn, section(".text.entry"))) reboot_entry(void) {
    reboot_to_tcat_bootloader(REBOOT_SRAM_CLEAR_END, REBOOT_STUB_LOAD_ADDR,
                              REBOOT_STUB_LOAD_ADDR + REBOOT_STUB_RESERVE);
}
