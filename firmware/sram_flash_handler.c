/*
 * sram_flash_handler.c — DCP flash handler injected via eCos hook
 *
 * Registers a DCP category 0x81F handler into the running firmware's
 * command dispatch linked list. The handler provides USB-accessible
 * SPI flash operations: INFO, READ, ERASE, WRITE.
 *
 * Runs as a displaced-instruction hook inside the eCos main loop thread.
 * The host patches `bl 0x14dc` at 0x2e48 to `bl 0x31C00` (this code).
 * On first call: register handler, set done flag, call original function.
 * On subsequent calls: just call original function.
 *
 * Build:
 *   arm-none-eabi-gcc -march=armv5te -marm -mno-thumb-interwork \
 *     -Os -nostdlib -ffreestanding -T firmware/sram_handler.ld \
 *     -o firmware/sram_flash_handler.elf firmware/sram_flash_handler.c
 *   arm-none-eabi-objcopy -O binary \
 *     firmware/sram_flash_handler.elf firmware/sram_flash_handler.bin
 *
 * SRAM layout (must match inject_handler.py):
 *   0x2B000  Registration shim + handler code (contiguous, ~440 bytes)
 *   0x2B200  Done flag (4 bytes)
 *   0x2B210  DCP handler node (16 bytes)
 *   0x2B220  Mailbox (8 bytes)
 *
 * All in DEADBEEF fill at 0x2B000-0x2C000, 26KB below nearest stack.
 */

#include <stdint.h>

/* ── Firmware functions (resolved by linker via firmware_symbols.ld) ── */

extern int  sst25xx_sector_erase(void *ctx, uint32_t addr);
extern int  sst25xx_aai_write(void *ctx, uint32_t addr,
                               const void *buf, uint32_t len);
extern void *memcpy(void *dst, const void *src, unsigned int len);
extern void FUN_2f38(void);
extern void dcp_register_handler(void *node);
extern void dcp_send_response(int retcode, void *body, int len);

extern char sst25xx_driver_ctx;  /* linker symbol — use &sst25xx_driver_ctx */

#define DRIVER_CTX       ((void *)&sst25xx_driver_ctx)

/* ── Fixed addresses (must match inject_handler.py) ─────────────────── */

#define DONE_FLAG     ((volatile uint32_t *)0x2B200)
#define DONE_MAGIC    0x444F4E45  /* "DONE" */

/* Handler node at 0x2B210 — safe from stack growth (26KB below SP ~0x317C0) */
#define NODE_ADDR     ((void *)0x2B210)

/* ── DCP handler node layout (16 bytes) ─────────────────────────────── */

struct handler_node {
    void     *next;       /* populated by dcp_register_handler */
    uint16_t  category;
    uint16_t  padding;
    void     *handler;
    void     *context;
};

/* ── Constants ──────────────────────────────────────────────────────── */

#define CATEGORY_FLASH     0x81F
#define GOLDEN_COPY_START  0x10000
#define GOLDEN_COPY_END    0x40000
#define SECTOR_SIZE        0x1000

/* JEDEC ID for SST25VF016B */
#define JEDEC_ID           0x00BF2541
#define FLASH_SECTOR_SIZE  0x00001000
#define FLASH_TOTAL_SIZE   0x00200000

/* Error codes */
#define ERR_GOLDEN_PROTECT 0xDEAD0001
#define ERR_WRITE_BAD_BODY 0xDEAD0002
#define ERR_ERASE_FAIL     0xDEAD0003
#define ERR_UNKNOWN_OPCODE 0xDEAD00FF

/* ── Mailbox for host polling ───────────────────────────────────────── */

struct mailbox {
    volatile uint32_t magic;      /* 0x444F4E45 = "DONE" when registered */
    volatile uint32_t status;     /* 0 = ok */
};

#define MAILBOX ((struct mailbox *)0x2B220)

/* Note: handler CODE lives at 0x31480 (linker script), safe from stacks.
 * Only the NODE and DONE_FLAG are at 0x2B200+ (extra safe, far from stacks).
 */

/* ── Forward declaration ────────────────────────────────────────────── */

static void flash_handler(void *ctx, uint16_t category, uint16_t opcode,
                          uint8_t *body, uint16_t body_len);

/* ── Registration ───────────────────────────────────────────────────── */

void register_handler(void) {
    struct handler_node *node = (struct handler_node *)NODE_ADDR;
    node->next     = (void *)0;
    node->category = CATEGORY_FLASH;
    node->padding  = 0;
    node->handler  = (void *)flash_handler;
    node->context  = (void *)0;

    dcp_register_handler(node);

    /* Signal completion */
    MAILBOX->status = 0;
    MAILBOX->magic  = DONE_MAGIC;
    *DONE_FLAG = DONE_MAGIC;
}

/* ── Entry point (naked trampoline) ─────────────────────────────────── */
/*
 * Called via bl from 0x2e48 (replaces `bl 0x14dc` in primary firmware).
 *
 * On first call: register DCP handler, then tail-call FUN_2f38.
 * On subsequent calls: just tail-call FUN_2f38.
 */
void _start(void) __attribute__((naked, section(".text.entry")));
void _start(void) {
    __asm__ volatile(
        "push  {r0-r3, r4-r11, lr}\n"
        /* Check done flag */
        "ldr   r0, =0x2B200\n"        /* DONE_FLAG address */
        "ldr   r1, [r0]\n"
        "ldr   r2, =0x444F4E45\n"     /* DONE_MAGIC */
        "cmp   r1, r2\n"
        "beq   1f\n"
        "bl    register_handler\n"
        "1:\n"
        "pop   {r0-r3, r4-r11, lr}\n"
        "b     FUN_2f38\n"            /* tail-call displaced function */
    );
}

/* ── DCP Flash Handler ──────────────────────────────────────────────── */
/*
 * Called by the DCP dispatch thread when category 0x81F command arrives.
 *
 * Calling convention (from dcp_usb_thread disassembly):
 *   r0 = context      (node->context, NULL for us)
 *   r1 = category     (0x81F)
 *   r2 = opcode       (lower 12 bits of cmd_id)
 *   r3 = body_ptr     (0x31704 = state+0x14, shared cmd/response buffer)
 *   [sp+0] = body_len (u16)
 *
 * Opcodes:
 *   0 = INFO:   -> 12 bytes (jedec_id, sector_size, flash_size)
 *   1 = READ:   body[addr:4, len:2] -> raw flash data (max 1024)
 *   2 = ERASE:  body[addr:4, len:4] -> status(4)
 *   3 = WRITE:  body[addr:4, data:N] -> status(4)
 */
static void flash_handler(void *ctx, uint16_t category, uint16_t opcode,
                          uint8_t *body, uint16_t body_len)
{
    (void)ctx;
    (void)category;

    switch (opcode) {

    case 0: { /* INFO */
        uint32_t *out = (uint32_t *)body;
        out[0] = JEDEC_ID;
        out[1] = FLASH_SECTOR_SIZE;
        out[2] = FLASH_TOTAL_SIZE;
        dcp_send_response(0, body, 12);
        return;
    }

    case 1: { /* READ */
        uint32_t addr = *(uint32_t *)body;
        uint16_t len  = *(uint16_t *)(body + 4);
        if (len > 1024) len = 1024;

        /* XIP read: memory address == SPI address */
        memcpy(body, (void *)(uintptr_t)addr, len);
        dcp_send_response(0, body, len);
        return;
    }

    case 2: { /* ERASE */
        uint32_t addr = *(uint32_t *)body;
        uint32_t len  = *(uint32_t *)(body + 4);

        /* Golden copy protection */
        if (addr < GOLDEN_COPY_END && (addr + len) > GOLDEN_COPY_START) {
            *(uint32_t *)body = ERR_GOLDEN_PROTECT;
            dcp_send_response(0, body, 4);
            return;
        }

        /* Erase sectors */
        uint32_t end = addr + len;
        while (addr < end) {
            int ret = sst25xx_sector_erase(DRIVER_CTX, addr);
            if (ret != 0) {
                *(uint32_t *)body = ERR_ERASE_FAIL;
                dcp_send_response(0, body, 4);
                return;
            }
            addr += SECTOR_SIZE;
        }

        *(uint32_t *)body = 0;
        dcp_send_response(0, body, 4);
        return;
    }

    case 3: { /* WRITE */
        uint32_t addr = *(uint32_t *)body;
        int32_t  data_len = (int32_t)body_len - 4;

        if (data_len <= 0) {
            *(uint32_t *)body = ERR_WRITE_BAD_BODY;
            dcp_send_response(0, body, 4);
            return;
        }

        /* Golden copy protection */
        if (addr < GOLDEN_COPY_END && (addr + (uint32_t)data_len) > GOLDEN_COPY_START) {
            *(uint32_t *)body = ERR_GOLDEN_PROTECT;
            dcp_send_response(0, body, 4);
            return;
        }

        int ret = sst25xx_aai_write(DRIVER_CTX, addr, body + 4, (uint32_t)data_len);
        *(uint32_t *)body = (uint32_t)ret;
        dcp_send_response(0, body, 4);
        return;
    }

    default: {
        *(uint32_t *)body = ERR_UNKNOWN_OPCODE;
        dcp_send_response(0, body, 4);
        return;
    }

    }
}
