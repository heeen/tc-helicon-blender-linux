/*
 * Minimal UART formatter for patch builds (no libc printf).
 *
 * Conversions: %x (uint32_t hex), %b (byte as two hex digits), %c (char).
 * No decimal (%d/%u) — avoids soft integer divide under -nostdlib. Literal %%
 *
 * For %x with a byte, mask if needed: (unsigned)(uint8_t)b.
 */

#ifndef UART_MINPRINTF_H
#define UART_MINPRINTF_H

#include <stdarg.h>

int uart_mini_printf(const char *fmt, ...);

#endif
