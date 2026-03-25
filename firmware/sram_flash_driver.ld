/* Linker script for SRAM flash driver stub
 * Code at 0x2C000 (8KB), data buffer at 0x2B000, mailbox at 0x2E000
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
