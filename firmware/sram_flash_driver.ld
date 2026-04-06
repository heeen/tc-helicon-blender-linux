/* Linker script for SRAM flash driver stub
 *
 * Layout (eCos stopped, we own all 512KB SRAM):
 *   0x00000 – 0x00FFF   Readback/verify buffer (4KB)
 *   0x01000 – 0x01FFF   Sector list for CMD_FLASH_ALL (2KB)
 *   0x02000 – 0x02FFF   TX/RX scratch + mailbox (4KB)
 *   0x03000 – 0x04FFF   Driver code + stack (8KB)
 *   0x05000 – 0x7FFFF   Bulk image data (492KB)
 */

ENTRY(_start)

MEMORY {
    SRAM (rwx) : ORIGIN = 0x3000, LENGTH = 0x2000
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
