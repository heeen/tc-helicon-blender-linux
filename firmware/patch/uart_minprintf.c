/*
 * Tiny variadic formatter: %x %b %c %%. No %d/%u (would need soft-div on -nostdlib).
 */

#include "uart_minprintf.h"
#include "dice_platform.h"

#include <stdint.h>

static const char HEX[] = "0123456789abcdef";

static void put_u32_hex(uint32_t v)
{
    unsigned started = 0u;

    for (int sh = 28; sh >= 0; sh -= 4) {
        unsigned nib = (unsigned)((v >> sh) & 0xFu);
        if (nib != 0u || sh == 0 || started) {
            uart_putc(HEX[nib]);
            started = 1u;
        }
    }
}

int uart_mini_printf(const char *fmt, ...)
{
    va_list ap;

    va_start(ap, fmt);
    for (; *fmt != '\0'; fmt++) {
        if (*fmt != '%') {
            uart_putc(*fmt);
            continue;
        }
        fmt++;
        if (*fmt == '\0')
            break;
        switch (*fmt) {
        case '%':
            uart_putc('%');
            break;
        case 'x':
            put_u32_hex(va_arg(ap, unsigned int));
            break;
        case 'b': {
            unsigned x = va_arg(ap, unsigned int) & 0xFFu;
            uart_putc(HEX[(x >> 4) & 0xFu]);
            uart_putc(HEX[x & 0xFu]);
            break;
        }
        case 'c':
            uart_putc((char)va_arg(ap, int));
            break;
        default:
            uart_putc('%');
            uart_putc(*fmt);
            break;
        }
    }
    va_end(ap);
    return 0;
}
