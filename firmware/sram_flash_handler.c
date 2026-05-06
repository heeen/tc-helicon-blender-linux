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

#include "midi_flash_dispatch.h"

/* ── Firmware functions (resolved by linker via firmware_symbols.ld) ── */

extern void FUN_2f38(void);
extern void dcp_register_handler(void *node);
extern void dcp_send_response(int retcode, void *body, int len);

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

/* ── DCP framing constants ──────────────────────────────────────────── */

#define CATEGORY_FLASH     0x81F

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
 * Opcodes (handled by midi_flash_dispatch.c shared core):
 *   0 = INFO        -> info(16)        (legacy callers see only first 12 B)
 *   1 = READ        body[addr:4, len:2] -> raw flash data (max 4096)
 *   2 = ERASE       body[addr:4, len:4] -> status(4)
 *   3 = WRITE/AAI   body[addr:4, data:N] -> status(4)
 *
 * HASH_RANGE is intentionally NOT exposed via DCP — it's MIDI-only (the
 * SysEx filter in patch/midi_sysex_filter.c). DCP recovery path is rare;
 * host-side READ-based sector diff is acceptable. Saves ~400 B in this
 * handler region (capped at ~1 KB by the SRAM DEADBEEF zone).
 *
 * The shared core in midi_flash_dispatch.c writes the response payload
 * into `body` and sets `resp_len`. We forward to dcp_send_response.
 */
static void flash_handler(void *ctx, uint16_t category, uint16_t opcode,
                          uint8_t *body, uint16_t body_len)
{
    (void)ctx;
    (void)category;

    uint32_t resp_len = 4;  /* default for status-only paths */

    switch (opcode) {
    case 0: /* INFO */
        flash_op_info(body, &resp_len);
        break;
    case 1: { /* READ */
        uint32_t addr = *(uint32_t *)body;
        uint32_t len  = (uint32_t)(*(uint16_t *)(body + 4));
        flash_op_read(addr, len, body, &resp_len);
        break;
    }
    case 2: { /* ERASE */
        uint32_t addr = *(uint32_t *)body;
        uint32_t len  = *(uint32_t *)(body + 4);
        uint32_t count = (len + FLASH_SECTOR_SIZE_B - 1) / FLASH_SECTOR_SIZE_B;
        flash_op_erase_sector(addr, count, body, &resp_len);
        break;
    }
    case 3: { /* WRITE / AAI */
        uint32_t addr = *(uint32_t *)body;
        int32_t  data_len = (int32_t)body_len - 4;
        if (data_len <= 0) {
            *(uint32_t *)body = ERR_BAD_BODY;
            break;
        }
        flash_op_aai_write(addr, body + 4, (uint32_t)data_len, body, &resp_len);
        break;
    }
    default:
        *(uint32_t *)body = ERR_UNKNOWN_OPCODE;
        break;
    }

    dcp_send_response(0, body, (int)resp_len);
}
