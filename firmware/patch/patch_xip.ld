/* XIP linker script — code in SPI flash, BSS in SRAM.
 *
 * Handler code executes from XIP (read-only, persistent in flash).
 * Writable data (done_flag, mailbox, node) lives in SRAM BSS.
 * The XIP address = SPI address + 0x40000 (DICE3 XIP mapping).
 */

INCLUDE "/home/florian/src-misc/helicon-blender/firmware/firmware_symbols.ld"

ENTRY(_hook_dcp_flash_stub)

MEMORY {
    /* Handler code in SPI flash via XIP */
    XIP (rx)  : ORIGIN = 0x41000, LENGTH = 4096

    /* Handler BSS in SRAM (end of eCos BSS, confirmed zero at runtime) */
    SRAM (rw) : ORIGIN = 0x325B8, LENGTH = 64
}

SECTIONS {
    /* Hook stubs go first in XIP */
    .text.hook_dcp_flash 0x41000 : {
        *(.text.hook_dcp_flash)
    } > XIP

    /* All handler code + rodata in XIP */
    .text : {
        *(.text*)
        *(.rodata*)
    } > XIP

    /* Initialized data (if any) in SRAM, loaded from... nowhere (XIP is RO).
     * All initialized data must be compile-time constants placed in .rodata. */
    .data : {
        *(.data*)
    } > SRAM

    /* BSS in SRAM */
    .bss : {
        *(.bss*)
        *(COMMON)
    } > SRAM

    /DISCARD/ : {
        *(.ARM.attributes)
        *(.ARM.exidx*)
        *(.comment)
    }
}
