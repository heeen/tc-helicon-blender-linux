/* Linker script for SRAM flash handler
 *
 * Code at 0x31480 (DEADBEEF fill zone, proven working).
 * Node/done-flag at 0x2B200+ (safe from stack growth — 26KB below SP).
 */

INCLUDE "firmware/firmware_symbols.ld"

ENTRY(_start)

MEMORY {
    SHIM    (rwx) : ORIGIN = 0x31480, LENGTH = 0x80
    HANDLER (rwx) : ORIGIN = 0x31500, LENGTH = 0x340
}

SECTIONS {
    .text.entry : {
        *(.text.entry)
    } > SHIM

    .text : {
        *(.text*)
        *(.rodata*)
    } > HANDLER

    .data : {
        *(.data*)
    } > HANDLER

    .bss : {
        *(.bss*)
        *(COMMON)
    } > HANDLER

    /DISCARD/ : {
        *(.ARM.attributes)
        *(.ARM.exidx*)
        *(.comment)
    }
}
