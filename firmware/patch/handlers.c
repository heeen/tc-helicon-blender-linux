/*
 * handlers.c — Hook handler implementations for Blender DICE3
 *
 * Built by the patch framework (hooks_gen.S calls into these).
 * All functions here are called from generated stubs, not directly
 * from firmware code.
 *
 * Current handlers:
 *   flash_handler_init  — registers DCP 0x81F handler (called via "before" hook)
 *   flash_handler       — DCP dispatch for INFO/READ/ERASE/WRITE
 *   midi_cc_handler     — intercepts MIDI CC → mixer control
 */

#include <stdint.h>

/* ── Firmware functions (resolved by linker via firmware_symbols.ld) ── */

extern int  sst25xx_sector_erase(void *ctx, uint32_t addr);
extern int  sst25xx_aai_write(void *ctx, uint32_t addr,
                               const void *buf, uint32_t len);
extern void *memcpy(void *dst, const void *src, unsigned int len);
extern void dcp_register_handler(void *node);
extern void dcp_send_response(int retcode, void *body, int len);

/* Mixer hardware functions */
extern void channel_level_apply(int bus, int input, int level);
extern void master_level_apply(int bus, int level);
extern void compressor_level_apply(int bus, int level);
extern void led_mute_update(int input, int on_off);

/* Original MIDI channel message callback (we chain to it) */
extern void midi_channel_msg_cb_orig(void *parser_ctx, void *parser_ctx2);

extern char sst25xx_driver_ctx;  /* linker symbol — use &sst25xx_driver_ctx */
#define DRIVER_CTX  ((void *)&sst25xx_driver_ctx)

/* Mixer state in firmware BSS — inputs[4][7] + compressor[4] + master[4] */
extern char mixer_state;
#define MIXER_STATE  ((volatile uint8_t *)&mixer_state)

/* MIDI parser context — channel_msg_cb pointer at offset +0x28 */
extern char midi_parser_ctx;
#define MIDI_PARSER_CTX     ((volatile uint32_t *)&midi_parser_ctx)
#define MIDI_CB_OFFSET      0x28  /* offset to channel_msg_cb function pointer */

/* ── Done flag ───────────────────────────────────────────────────────── */
/* Placed in BSS — zeroed by C runtime (or by JTAG clear).
 * For body-append mode, the bootloader zeroes BSS on boot.
 * For JTAG injection, we clear it explicitly.
 */

static volatile uint32_t done_flag;
#define DONE_MAGIC  0x444F4E45  /* "DONE" */

/* ══════════════════════════════════════════════════════════════════════
 * JTAG Mailbox — alternative flash path that bypasses USB entirely.
 *
 * JTAG writes commands to this struct; the main-loop hook processes them.
 * This solves the "JTAG halt disrupts USB" problem: after injection,
 * JTAG can load sector data to a SRAM buffer, write a command here,
 * and the handler executes it on the next loop iteration (~10ms).
 *
 * Also used for DCP REBOOT (opcode 4) for future self-updates.
 * ══════════════════════════════════════════════════════════════════════ */

#define MBOX_CMD_IDLE    0
#define MBOX_CMD_ERASE   1  /* erase sector at spi_addr */
#define MBOX_CMD_WRITE   2  /* write data_len bytes from data_addr to spi_addr */
#define MBOX_CMD_REBOOT  3  /* software reset */

#define MBOX_RESULT_PENDING  0
#define MBOX_RESULT_OK       1
#define MBOX_RESULT_ERROR    2

static volatile struct {
    uint32_t command;     /* MBOX_CMD_* */
    uint32_t spi_addr;
    uint32_t data_addr;   /* SRAM address of source data */
    uint32_t data_len;
    uint32_t result;      /* MBOX_RESULT_* */
    uint32_t error_code;  /* 0 on success, sst25xx return code on error */
} jtag_mailbox;

/* ── DCP handler node ────────────────────────────────────────────────── */

struct handler_node {
    void     *next;
    uint16_t  category;
    uint16_t  padding;
    void     *handler;
    void     *context;
};

#define CATEGORY_FLASH     0x81F

/* ── Constants ───────────────────────────────────────────────────────── */

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

/* ── Forward declarations ────────────────────────────────────────────── */

static void flash_handler(void *ctx, uint16_t category, uint16_t opcode,
                          uint8_t *body, uint16_t body_len);

/* ══════════════════════════════════════════════════════════════════════
 * MIDI CC → Mixer Control
 *
 * MIDI CC messages on channels 1-4 map to mixer buses A-D.
 * Value range: MIDI 0-127 → firmware 0-31 (>> 2).
 *
 * CC mapping:
 *   CC  7       → Master level
 *   CC 16-21    → Input 1-6 level
 *   CC 22       → Input 7 (mic gain/talk) level
 *   CC 23       → Compressor level
 *
 * The MIDI parser calls our handler via the channel_msg_cb pointer
 * (patched at init). Parser context layout:
 *   +0x00: status byte (0xB0|ch for CC)
 *   +0x01: data byte 1 (CC number)
 *   +0x02: data byte 2 (CC value)
 * ══════════════════════════════════════════════════════════════════════ */

/* Mixer state layout offsets */
#define MIX_INPUT_STRIDE  7     /* 7 inputs per bus */
#define MIX_COMP_OFFSET   0x1C  /* compressor[4] starts here */
#define MIX_MASTER_OFFSET 0x20  /* master[4] starts here */

static void midi_cc_handler(void *parser_ctx, void *parser_ctx2) {
    uint8_t *p = (uint8_t *)parser_ctx;
    uint8_t status = p[0];

    /* Only handle Control Change (0xB0-0xBF) */
    if ((status & 0xF0) != 0xB0)
        goto chain;

    uint8_t channel = status & 0x0F;
    uint8_t cc_num  = p[1];
    uint8_t cc_val  = p[2];

    /* Only channels 1-4 (0-3) map to buses */
    if (channel >= 4)
        goto chain;

    int bus   = (int)channel;
    int level = (int)(cc_val >> 2);  /* 0-127 → 0-31 */

    if (cc_num == 7) {
        /* CC 7: Master volume */
        MIXER_STATE[MIX_MASTER_OFFSET + bus] = (uint8_t)level;
        master_level_apply(bus, level);
    } else if (cc_num >= 16 && cc_num <= 22) {
        /* CC 16-22: Input 1-7 levels */
        int input = cc_num - 16;
        MIXER_STATE[bus * MIX_INPUT_STRIDE + input] = (uint8_t)level;
        channel_level_apply(bus, input, level);
    } else if (cc_num == 23) {
        /* CC 23: Compressor level */
        MIXER_STATE[MIX_COMP_OFFSET + bus] = (uint8_t)level;
        compressor_level_apply(bus, level);
    }
    /* Unrecognized CCs fall through to original handler */

chain:
    midi_channel_msg_cb_orig(parser_ctx, parser_ctx2);
}

/* ── Software reboot ─────────────────────────────────────────────────── */

static void __attribute__((noreturn)) software_reboot(void) {
    /* Disable IRQ + FIQ, jump to reset vector (bootloader entry) */
    __asm__ volatile (
        "mrs r0, cpsr        \n"
        "orr r0, r0, #0xC0   \n"  /* set I+F bits — disable interrupts */
        "msr cpsr_c, r0      \n"
        "mov r0, #0          \n"
        "mcr p15, 0, r0, c7, c5, 0 \n"  /* invalidate I-cache */
        "mcr p15, 0, r0, c7, c6, 0 \n"  /* invalidate D-cache */
        "mov pc, #0          \n"  /* jump to reset vector */
        ::: "r0", "memory"
    );
    __builtin_unreachable();
}

/* ── Mailbox processing (called every main-loop iteration) ───────────── */

static void process_mailbox(void) {
    uint32_t cmd = jtag_mailbox.command;
    if (cmd == MBOX_CMD_IDLE)
        return;

    jtag_mailbox.result = MBOX_RESULT_PENDING;

    switch (cmd) {

    case MBOX_CMD_ERASE: {
        int ret = sst25xx_sector_erase(DRIVER_CTX, jtag_mailbox.spi_addr);
        jtag_mailbox.error_code = (uint32_t)ret;
        jtag_mailbox.result = (ret == 0) ? MBOX_RESULT_OK : MBOX_RESULT_ERROR;
        break;
    }

    case MBOX_CMD_WRITE: {
        int ret = sst25xx_aai_write(
            DRIVER_CTX, jtag_mailbox.spi_addr,
            (const void *)jtag_mailbox.data_addr, jtag_mailbox.data_len);
        jtag_mailbox.error_code = (uint32_t)ret;
        jtag_mailbox.result = (ret == 0) ? MBOX_RESULT_OK : MBOX_RESULT_ERROR;
        break;
    }

    case MBOX_CMD_REBOOT:
        jtag_mailbox.result = MBOX_RESULT_OK;
        software_reboot();
        /* never returns */

    default:
        jtag_mailbox.error_code = 0xBADC0DE;
        jtag_mailbox.result = MBOX_RESULT_ERROR;
        break;
    }

    jtag_mailbox.command = MBOX_CMD_IDLE;
}

/* ── Initialization (called from hook stub, "before" mode) ───────────── */

/* The generated stub calls this on every main-loop iteration.
 * First call: register DCP handler + patch MIDI callback.
 * Every call: check JTAG mailbox for flash commands.
 *
 * The handler lives at SRAM 0x29D08 — a 1028-byte zero gap in the
 * firmware's code/data section. This area is loaded by the bootloader
 * and NEVER touched by eCos init (BSS clear, heap, thread stacks).
 */
void flash_handler_init(void) {
    if (done_flag == DONE_MAGIC) {
        process_mailbox();
        return;
    }

    /* Register DCP flash handler */
    static struct handler_node node;
    node.next     = (void *)0;
    node.category = CATEGORY_FLASH;
    node.padding  = 0;
    node.handler  = (void *)flash_handler;
    node.context  = (void *)0;

    dcp_register_handler(&node);

    /* Patch MIDI parser's channel_msg_cb to intercept CC messages */
    volatile uint32_t *cb_ptr =
        (volatile uint32_t *)((uint8_t *)MIDI_PARSER_CTX + MIDI_CB_OFFSET);
    *cb_ptr = (uint32_t)(void *)midi_cc_handler;

    done_flag = DONE_MAGIC;
}

/* ── DCP Flash Handler ───────────────────────────────────────────────── */
/*
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
        memcpy(body, (void *)(uintptr_t)addr, len);
        dcp_send_response(0, body, len);
        return;
    }

    case 2: { /* ERASE */
        uint32_t addr = *(uint32_t *)body;
        uint32_t len  = *(uint32_t *)(body + 4);

        if (addr < GOLDEN_COPY_END && (addr + len) > GOLDEN_COPY_START) {
            *(uint32_t *)body = ERR_GOLDEN_PROTECT;
            dcp_send_response(0, body, 4);
            return;
        }

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

    case 4: { /* REBOOT */
        *(uint32_t *)body = 0;
        dcp_send_response(0, body, 4);
        /* Brief delay so USB can send the response */
        for (volatile int i = 0; i < 100000; i++) {}
        software_reboot();
        /* never returns */
    }

    default: {
        *(uint32_t *)body = ERR_UNKNOWN_OPCODE;
        dcp_send_response(0, body, 4);
        return;
    }

    }
}
