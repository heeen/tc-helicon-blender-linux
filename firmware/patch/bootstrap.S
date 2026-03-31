/*
 * bootstrap.S — Persistent patch bootstrap for DICE3 Blender
 *
 * Lives in 152-byte safe gap at SRAM 0x299EC. On first main-loop call,
 * DMA-reads handler binary from SPI to SRAM 0x31480 (DEADBEEF zone).
 * Subsequent calls go straight to the handler.
 *
 * Budget: 32 instructions (128B) + 6 literals (24B) = 152 bytes exactly.
 */

    .arm
    .cpu arm926ej-s

    .section .text.bootstrap, "ax", %progbits
    .global _bootstrap
    .type _bootstrap, %function
    .align 2

_bootstrap:
    /* ── Check if handler loaded (magic word at handler base) ── */
    ldr     r0, .Lhbase
    ldr     r1, [r0]
    ldr     r2, .Lmagic
    cmp     r1, r2
    beq     .Lready

    /* ── SPI controller (0xCC000000): reset + DMA read mode ── */
    mov     r1, #0xCC000000
    mov     r2, #0
    str     r2, [r1, #0x08]            /* SRST */
    str     r2, [r1, #0x10]            /* TX count = 0 */
    mov     r2, #2
    str     r2, [r1, #0x14]            /* mode = DMA read */

    /* ── DMA controller (0x80000000): dest + word count ── */
    mov     r2, #0x80000000
    mov     r3, #0
    str     r3, [r2, #0x08]            /* DMA clear */
    str     r0, [r2, #0x00]            /* dest = handler_base */
    ldr     r3, .Lwc
    str     r3, [r2, #0x04]            /* word count */

    /* ── Trigger SPI read ── */
    ldr     r3, .Lsrc
    str     r3, [r1, #0x1C]            /* SPI read address */
    ldr     r3, .Lsz
    str     r3, [r1, #0x18]            /* byte count (triggers DMA) */

    /* ── Poll DMA complete ── */
1:  ldr     r3, [r2, #0x08]
    tst     r3, #1
    beq     1b

    /* ── Invalidate I-cache after writing code to SRAM ── */
    mov     r0, #0
    mcr     p15, 0, r0, c7, c5, 0

    /* ── Call handler init, then tail-call displaced BL target ── */
.Lready:
    stmfd   sp!, {lr}
    ldr     r12, .Linit
    mov     lr, pc
    bx      r12
    ldmfd   sp!, {lr}
    b       FUN_2f38

    /* ── Literal pool (6 words = 24 bytes) ── */
.Lhbase: .word  HANDLER_SRAM
.Lmagic: .word  HANDLER_MAGIC
.Lsrc:   .word  HANDLER_SPI
.Lwc:    .word  HANDLER_DMA_WCOUNT
.Lsz:    .word  HANDLER_SIZE
.Linit:  .word  HANDLER_INIT

    .size _bootstrap, . - _bootstrap
