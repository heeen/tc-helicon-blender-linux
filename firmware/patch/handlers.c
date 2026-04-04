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


/* MIDI TX — sends MIDI data out USB MIDI EP IN (device → host). */
extern int midi_tx_write(const void *data, int len);

/* USB endpoint transfer submission — bypasses streaming state checks.
 * Programs the USB controller queue head and primes the endpoint. */
extern void usb_endpoint_submit_transfer(void *ep, void *buf, int size,
                                          void *callback, int ctx);
extern void usb_midi_rx_complete(void *param1, int param2);

/* USB MIDI RX endpoint arming */
extern void usb_midi_rx_start(void *ep_handle, int num_cables);
extern void *usb_iface_get_rx_endpoint(void *conn, int type);
extern uint8_t usb_midi_conn[];  /* connection object at 0x2A230 */

extern uint8_t sst25xx_driver_ctx[];  /* SPI flash driver context at 0x2A93C */
#define DRIVER_CTX  ((void *)sst25xx_driver_ctx)

/* Mixer state in firmware BSS — inputs[4][7] + compressor[4] + master[4] */
extern uint8_t mixer_state[36];
#define MIXER_STATE  ((volatile uint8_t *)mixer_state)

/* MIDI parser context — channel_msg_cb pointer at offset +0x28 */
extern uint8_t midi_parser_ctx[0x34];
#define MIDI_PARSER_CTX     ((volatile uint32_t *)midi_parser_ctx)
#define MIDI_CB_OFFSET      0x28  /* offset to channel_msg_cb function pointer */

/* ── UART debug output (firmware functions, resolved via firmware_symbols.ld) ── */

extern void uart_putc(char c);
extern void uart_print_string(const char *s);
extern int  uart_printf(const char *fmt, ...);

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
#define MIX_STATE_SIZE    0x24  /* total: 4*7 + 4 + 4 = 36 bytes */

/* ── MIDI CC output ─────────────────────────────────────────────────── */

static void midi_send_cc(uint8_t channel, uint8_t cc_num, uint8_t cc_val) {
    uint8_t msg[3] = { (uint8_t)(0xB0 | (channel & 0x0F)), cc_num & 0x7F, cc_val & 0x7F };
    int ret = midi_tx_write(msg, 3);
    {
        static const char hex[] = "0123456789ABCDEF";
        uart_putc('<');
        uart_putc(hex[(msg[0] >> 4) & 0xF]);
        uart_putc(hex[msg[0] & 0xF]);
        uart_putc(' ');
        uart_putc(hex[(msg[1] >> 4) & 0xF]);
        uart_putc(hex[msg[1] & 0xF]);
        uart_putc(' ');
        uart_putc(hex[(msg[2] >> 4) & 0xF]);
        uart_putc(hex[msg[2] & 0xF]);
        uart_putc(' ');
        uart_putc(ret > 0 ? 'k' : 'E');
        uart_putc('>');
        uart_putc('\r');
        uart_putc('\n');
    }
}

/* Snapshot of mixer_state for change detection.
 * Compared against live mixer_state to emit MIDI CCs for BLE/knob changes. */
static uint8_t mixer_snapshot[MIX_STATE_SIZE];

static void midi_emit_state_diff(void) {
    volatile uint8_t *live = MIXER_STATE;

    for (int bus = 0; bus < 4; bus++) {
        /* Inputs 0-6 → CC 16-22 */
        for (int input = 0; input < 7; input++) {
            int off = bus * MIX_INPUT_STRIDE + input;
            uint8_t cur = live[off];
            if (cur != mixer_snapshot[off]) {
                mixer_snapshot[off] = cur;
                midi_send_cc((uint8_t)bus, (uint8_t)(16 + input), (uint8_t)(cur << 2));
            }
        }
        /* Compressor → CC 23 */
        {
            int off = MIX_COMP_OFFSET + bus;
            uint8_t cur = live[off];
            if (cur != mixer_snapshot[off]) {
                mixer_snapshot[off] = cur;
                midi_send_cc((uint8_t)bus, 23, (uint8_t)(cur << 2));
            }
        }
        /* Master → CC 7 */
        {
            int off = MIX_MASTER_OFFSET + bus;
            uint8_t cur = live[off];
            if (cur != mixer_snapshot[off]) {
                mixer_snapshot[off] = cur;
                midi_send_cc((uint8_t)bus, 7, (uint8_t)(cur << 2));
            }
        }
    }
}

/* ── MIDI CC → Mixer Control ────────────────────────────────────────── */

static void midi_cc_handler(void *parser_ctx, void *parser_ctx2) {
    uint8_t *p = (uint8_t *)parser_ctx;
    uint8_t status = p[0];

    uart_putc('[');
    /* Print status and data bytes as hex via putc */
    {
        static const char hex[] = "0123456789ABCDEF";
        uart_putc(hex[(status >> 4) & 0xF]);
        uart_putc(hex[status & 0xF]);
        uart_putc(' ');
        uart_putc(hex[(p[1] >> 4) & 0xF]);
        uart_putc(hex[p[1] & 0xF]);
        uart_putc(' ');
        uart_putc(hex[(p[2] >> 4) & 0xF]);
        uart_putc(hex[p[2] & 0xF]);
    }
    uart_putc(']');
    uart_putc('\r');
    uart_putc('\n');

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
        mixer_snapshot[MIX_MASTER_OFFSET + bus] = (uint8_t)level;
        master_level_apply(bus, level);
        midi_send_cc(channel, cc_num, cc_val);
    } else if (cc_num >= 16 && cc_num <= 22) {
        /* CC 16-22: Input 1-7 levels */
        int input = cc_num - 16;
        int off = bus * MIX_INPUT_STRIDE + input;
        MIXER_STATE[off] = (uint8_t)level;
        mixer_snapshot[off] = (uint8_t)level;
        channel_level_apply(bus, input, level);
        midi_send_cc(channel, cc_num, cc_val);
    } else if (cc_num == 23) {
        /* CC 23: Compressor level */
        MIXER_STATE[MIX_COMP_OFFSET + bus] = (uint8_t)level;
        mixer_snapshot[MIX_COMP_OFFSET + bus] = (uint8_t)level;
        compressor_level_apply(bus, level);
        midi_send_cc(channel, cc_num, cc_val);
    }
    /* Unrecognized CCs fall through to original handler */

chain:
    midi_channel_msg_cb_orig(parser_ctx, parser_ctx2);
}

/* ── Software reboot ─────────────────────────────────────────────────── */

static void __attribute__((noreturn)) software_reboot(void) {
    /* Full reboot: reset SPI controller to XIP mode, then jump to
     * TCAT bootloader at XIP 0x4F000. The bootloader DMA-copies
     * firmware from SPI to SRAM — equivalent to power cycle.
     *
     * Must reset SPI controller first because flash operations leave
     * it in DMA mode, and XIP instruction fetch requires default mode. */
    volatile uint32_t *spi = (volatile uint32_t *)0xCC000000;

    /* Disable interrupts */
    __asm__ volatile (
        "mrs r0, cpsr        \n"
        "orr r0, r0, #0xC0   \n"
        "msr cpsr_c, r0      \n"
        ::: "r0"
    );

    /* Restore SPI controller to power-on state (XIP mode) */
    spi[0x08 / 4] = 1;    /* ctrl = 1 (power-on default) */
    spi[0x10 / 4] = 2;    /* TX count = 2 (power-on default) */
    spi[0x14 / 4] = 0x38; /* mode = 0x38 (XIP mode, NOT 0x02 DMA) */

    /* Invalidate caches */
    __asm__ volatile (
        "mov r0, #0          \n"
        "mcr p15, 0, r0, c7, c5, 0 \n"  /* invalidate I-cache */
        "mcr p15, 0, r0, c7, c6, 0 \n"  /* invalidate D-cache */
        "ldr pc, =0x4F000    \n"         /* jump to XIP bootloader */
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

/* ── Boot-time initialization ────────────────────────────────────────── */

/* Called from patched firmware_entry at 0x344 (replaces `bl rtos_app_init`).
 * Runs AFTER .ctors (DCP state initialized) but BEFORE thread creation.
 * Registers DCP handler via direct memory writes — no firmware function
 * calls (dcp_register_handler crashes at this stage).
 * Then chains to the original rtos_app_init.
 */
extern void rtos_app_init(void);

#define DCP_HANDLER_LIST  (*(volatile uint32_t *)0x313C4)

static volatile uint32_t midi_patched;

static void patch_midi_callback(void) {
    if (midi_patched)
        return;
    volatile uint32_t *cb_ptr =
        (volatile uint32_t *)((uint8_t *)MIDI_PARSER_CTX + MIDI_CB_OFFSET);
    *cb_ptr = (uint32_t)(void *)midi_cc_handler;
    memcpy(mixer_snapshot, (const void *)MIXER_STATE, MIX_STATE_SIZE);
    midi_patched = 1;
}

/* ── EP 0x03 arming (native USB MIDI RX) ─────────────────────────────── */

static void arm_midi_rx_endpoint(void) {
    /* Get EP handle via vtable dispatch — same call usb_midi_streaming_start uses.
     * Returns NULL if USB subsystem not yet initialized. */
    void *ep = usb_iface_get_rx_endpoint(usb_midi_conn, 3);
    if (ep == (void *)0)
        return;  /* USB not ready, retry next iteration */

    /* Already armed: callback at ep+8 is non-zero when transfer is in flight.
     * The completion callback (usb_midi_rx_complete) clears this and re-arms. */
    if (*(volatile uint32_t *)((uint8_t *)ep + 8) != 0)
        return;

    /* Force EP state to "configured" (0x02).
     * usb_hw_ep_start_transfer checks this byte — it's firmware-level tracking,
     * not a hardware register. The USB controller queue heads and dTDs are
     * initialized for ALL endpoints at boot by usb_hw_controller_init.
     * EP 0x03 is otherwise unused (streaming_start never fires for MIDI-only). */
    *(volatile uint8_t *)((uint8_t *)ep + 0x20) = 0x02;

    /* Arm: sets up buffer at midi_pkt_buf, stores EP handle, primes DMA.
     * Re-arming is automatic via usb_midi_rx_complete → usb_midi_rx_dispatch. */
    usb_midi_rx_start(ep, 1);

    uart_print_string("[midi] EP armed\r\n");
}

void boot_init(void) {
    /* Direct DCP handler registration via memory writes. */
    static struct handler_node boot_node;
    uint32_t old_head = DCP_HANDLER_LIST;
    boot_node.next     = (void *)old_head;
    boot_node.category = CATEGORY_FLASH;
    boot_node.padding  = 0;
    boot_node.handler  = (void *)flash_handler;
    boot_node.context  = (void *)0;
    DCP_HANDLER_LIST = (uint32_t)(void *)&boot_node;

    done_flag = DONE_MAGIC;
    midi_patched = 0;  /* BSS not zeroed in patch zone — must init explicitly */

    uart_print_string("[boot_init] DCP registered\r\n");

    /* Chain to original rtos_app_init — starts scheduler, never returns */
    rtos_app_init();
}

/* ── Main-loop hook (called from trampoline at 0x4FAC) ───────────────── */

/* If the main loop ever reaches 0x4FAC, this handles mailbox polling.
 * Currently the main loop is blocked in cyg_flag_wait, so this rarely fires.
 */
void flash_handler_init(void) {
    if (done_flag == DONE_MAGIC) {
        process_mailbox();
        patch_midi_callback();
        arm_midi_rx_endpoint();
        midi_emit_state_diff();
        return;
    }

    uart_print_string("A");

    /* Register DCP flash handler */
    static struct handler_node node;
    node.next     = (void *)0;
    node.category = CATEGORY_FLASH;
    node.padding  = 0;
    node.handler  = (void *)flash_handler;
    node.context  = (void *)0;

    dcp_register_handler(&node);
    uart_print_string("B");

    /* Patch MIDI parser's channel_msg_cb to intercept CC messages */
    volatile uint32_t *cb_ptr =
        (volatile uint32_t *)((uint8_t *)MIDI_PARSER_CTX + MIDI_CB_OFFSET);
    *cb_ptr = (uint32_t)(void *)midi_cc_handler;
    uart_print_string("C");

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

    case 5: { /* MIDI_INIT — patch MIDI callback + arm RX endpoint */
        patch_midi_callback();

        /* EP arming TODO: USB bulk OUT EP 0x03 is not armed.
         * usb_endpoint_submit_transfer crashes the USB stack.
         * For now, use DCP opcode 6 for host→device CC. */

        *(uint32_t *)body = midi_patched;
        dcp_send_response(0, body, 4);
        return;
    }

    case 6: { /* MIDI_SEND — host sends MIDI bytes via DCP (bypasses broken bulk EP) */
        /* Body: raw MIDI bytes (3 bytes per CC message, concatenated) */
        for (uint16_t i = 0; i + 2 < body_len; i += 3) {
            uint8_t status = body[i];
            if ((status & 0xF0) != 0xB0)
                continue;
            uint8_t channel = status & 0x0F;
            uint8_t cc_num = body[i + 1];
            uint8_t cc_val = body[i + 2];
            if (channel >= 4)
                continue;
            int bus = (int)channel;
            int level = (int)(cc_val >> 2);
            if (cc_num == 7) {
                MIXER_STATE[MIX_MASTER_OFFSET + bus] = (uint8_t)level;
                mixer_snapshot[MIX_MASTER_OFFSET + bus] = (uint8_t)level;
                master_level_apply(bus, level);
                midi_send_cc(channel, cc_num, cc_val);
            } else if (cc_num >= 16 && cc_num <= 22) {
                int input = cc_num - 16;
                int off = bus * MIX_INPUT_STRIDE + input;
                MIXER_STATE[off] = (uint8_t)level;
                mixer_snapshot[off] = (uint8_t)level;
                channel_level_apply(bus, input, level);
                midi_send_cc(channel, cc_num, cc_val);
            } else if (cc_num == 23) {
                MIXER_STATE[MIX_COMP_OFFSET + bus] = (uint8_t)level;
                mixer_snapshot[MIX_COMP_OFFSET + bus] = (uint8_t)level;
                compressor_level_apply(bus, level);
                midi_send_cc(channel, cc_num, cc_val);
            }
        }
        *(uint32_t *)body = 0;
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
