/* .ctors init stub: copies handler from body to 0x80000 at boot.
 *
 * Called during .ctors (step 6 of boot). At this point:
 *   - BSS is cleared
 *   - HW init done, IRQ enabled
 *   - Heap initialized
 *   - NO threads created yet → 0x2AB20+ still has body data
 *
 * This function:
 *   1. Calls the two original .ctors we displaced ([15]=0x7A4, [16]=0xB84)
 *   2. Copies the handler blob from body (0x2AB20) to 0x80000 via memcpy
 *   3. Returns — .ctors runner continues, threads get created later
 *
 * Size: must be < 152 bytes (0x2AA88 to 0x2AB20 = 0x98 bytes)
 */

    .arm
    .cpu arm926ej-s
    .section .text.ctor_init, "ax", %progbits
    .global ctor_init
    .type ctor_init, %function
    .align 2

ctor_init:
    stmfd   sp!, {r4, lr}

    /* Call displaced .ctors [15] and [16] */
    ldr     r4, =0x7A4
    blx     r4
    ldr     r4, =0xB84
    blx     r4

    /* memcpy(0x80000, handler_blob_addr, handler_blob_size) */
    ldr     r0, =0x80000           /* destination: second SRAM bank */
    ldr     r1, =handler_blob      /* source: handler in body (linker resolves) */
    ldr     r2, =handler_blob_size /* size */
    ldr     r4, =0xA77C            /* memcpy address */
    blx     r4

    ldmfd   sp!, {r4, pc}

    .ltorg

    .size ctor_init, . - ctor_init
