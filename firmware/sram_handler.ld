/* Linker script for SRAM flash handler
 *
 * Code at 0x31480 (DEADBEEF fill zone, proven working).
 * Node/done-flag at 0x2B200+ (safe from stack growth — 26KB below SP).
 *
 * DEADBEEF zone runs 0x31480..0x31880 (1 KB). SHIM (0x80) + HANDLER
 * (0x380) fully consume it. Bumped from 0x340 → 0x380 on 2026-05-05
 * after factoring the shared midi_flash_dispatch.c into the handler
 * — adds ~100 B of dispatch core. CLAUDE.md notes 0x31880 begins
 * "active DCP/firmware data" so we MUST stop at 0x31880 exactly.
 */

INCLUDE "firmware/firmware_symbols.ld"

ENTRY(_start)

MEMORY {
    SHIM    (rwx) : ORIGIN = 0x31480, LENGTH = 0x80
    HANDLER (rwx) : ORIGIN = 0x31500, LENGTH = 0x380
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
