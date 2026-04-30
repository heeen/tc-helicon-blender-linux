/* JTAG-uploaded entry @ 0x2B000 — logic lives in reboot_common.c */

#include <stdint.h>
#include "reboot_common.h"

/* Keep the JTAG stub independent from UART state.
 * On crashy runtime states, touching UART can stall the reboot path. */
void reboot_uart_line(const char *s) { (void)s; }

void __attribute__((noreturn, section(".text.entry"))) reboot_entry(void) {
    /* JTAG path: skip SRAM scrub while stabilizing reboot mechanics. */
    reboot_to_tcat_bootloader(0x200u, REBOOT_STUB_LOAD_ADDR,
                              REBOOT_STUB_LOAD_ADDR + REBOOT_STUB_RESERVE);
}
