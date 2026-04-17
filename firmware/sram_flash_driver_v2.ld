/* Linker script for the v2 SRAM flash driver.
 *
 * SRAM layout (DICE3 has 512 KB at 0x00000-0x7FFFF, bit 19 ignored):
 *   0x00000-0x2AFFF  free (host can use for future bulk pages)
 *   0x2B000-0x2BFFF  V2_DATA_BUF (single-sector source/dest, 4 KB)
 *   0x2C000-0x2DFFF  Driver .text + .data + .bss (8 KB)
 *   0x2E000-0x2E0FF  V2_MBOX (mailbox, 256 B reserved)
 *   0x2E100-0x2E1FF  scratch / readback buffers (ad-hoc in driver)
 *   0x2E200-0x2E7FF  V2_LOG_RING (128 × 12 B = 1536 B, rounded to 1.5 KB)
 *   0x2E800-0x2EFFF  reserved / future
 *   0x2F100-0x2F8FF  V2_SECTOR_LIST (sector descriptors for bulk mode)
 *   0x2F900-0x7FFFF  V2_IMAGE_BASE (bulk image payload, up to ~326 KB)
 *
 * Only 0x2C000-0x2DFFF is linker-managed; everything else is fixed
 * addresses in sram_flash_mailbox_v2.h so host tooling can find them
 * without linker symbols.
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
