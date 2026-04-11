/* Linker script for SRAM flash driver stub
 * Code at 0x2C000 (8KB), data buffer at 0x2B000, mailbox at 0x2E000
 *
 * For CMD_FLASH_ALL bulk mode, the host loads image data at 0x50000+
 * (above firmware heap, 192KB available = enough for 122 sectors).
 * Wait -- 122 * 4KB = 488KB > 192KB. Bulk data must start lower.
 *
 * Layout for bulk flash (eCos stopped, peripherals disabled):
 *   0x00000-0x00FFF  Readback buffer (4KB)
 *   0x01000-0x01FFF  Sector list (2KB)
 *   0x02000-0x0FFFF  Free
 *   0x10000-0x2AFFF  Bulk image data (part 1, 108KB)
 *   0x2B000-0x2BFFF  Single-sector data buffer (4KB)
 *   0x2C000-0x2DFFF  Driver code (8KB)
 *   0x2E000-0x2EFFF  Mailbox + scratch (4KB)
 *   0x2F000-0x7FFFF  Bulk image data (part 2, 324KB)
 *   Total bulk: 432KB (can hold ~105 sectors; remaining fetched per-sector)
 *
 * Simpler: just use everything from 0x10000-0x2AFFF + 0x2F000-0x7FFFF
 * But that's non-contiguous. Instead, bulk data goes at 0x2F000-0x7FFFF (324KB)
 * and we process in two passes or accept the limitation.
 *
 * Simplest proven approach: keep original layout, bulk data at 0x2F100+.
 */

ENTRY(_start)

MEMORY {
    SRAM (rwx) : ORIGIN = 0x2C000, LENGTH = 0x2000
}

SECTIONS {
    .text : {
        *(.text.entry)
        *(.text*)
        *(.rodata*)
    } > SRAM

    .data : { *(.data*) } > SRAM
    .bss  : { *(.bss*) *(COMMON) } > SRAM

    /DISCARD/ : { *(.ARM.attributes) *(.ARM.exidx*) *(.comment) }
}
