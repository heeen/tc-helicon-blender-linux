
    .arm
    .cpu arm926ej-s
    .section .text, "ax", %progbits
    .global ctor_init
    .type ctor_init, %function
    .align 2

ctor_init:
    stmfd   sp!, {r4, lr}
    /* Call displaced .ctors [15] and [16] */
    ldr     r4, =0x7a4
    blx     r4
    ldr     r4, =0xb84
    blx     r4
    /* memcpy(0x80000, handler_blob, handler_size) */
    ldr     r0, =0x80000
    ldr     r1, =0x2ab20
    ldr     r2, =0x395
    ldr     r4, =0xa77c
    blx     r4
    ldmfd   sp!, {r4, pc}
    .ltorg
    .size ctor_init, . - ctor_init
