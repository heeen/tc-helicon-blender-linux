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

#include "dice_platform.h"
#include "dice_usb_regs.h"
#include "../reboot_common.h"

void reboot_uart_line(const char *s) { uart_print_string(s); }

#define DRIVER_CTX  ((void *)sst25xx_driver_ctx)
#define MIXER_STATE ((volatile uint8_t *)mixer_state)
#define MIDI_PARSER_CTX ((volatile uint32_t *)midi_parser_ctx)

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

/* ── USB control ────────────────────────────────────────────────────── */

#define USB_USBMODE     TCAT_USBMODE

/* Force USB re-enumeration: full reset, wait for host to process
 * disconnect, then re-init as device and reconnect. */
static void usb_reenumerate(void) {
    usb_hw_reset();
    /* Wait ~500ms for host to fully process disconnect */
    for (volatile int i = 0; i < 2000000; i++) {}
    /* Re-set device mode (required after RST clears USBMODE) */
    USB_USBMODE = 2;
    /* Reconnect: set RS → D+ pulled high → host sees new device */
    USB_USBCMD |= 1;
}

static void __attribute__((noreturn)) software_reboot(void) {
    /* Patch zone @ 0x32600+: clear firmware RAM below it; no JTAG stub skip. */
    reboot_to_tcat_bootloader(REBOOT_SRAM_CLEAR_END, 0, 0);
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
        (volatile uint32_t *)((uint8_t *)MIDI_PARSER_CTX + MIDI_PARSER_CH_CB_OFFSET);
    *cb_ptr = (uint32_t)(void *)midi_cc_handler;
    memcpy(mixer_snapshot, (const void *)MIXER_STATE, MIX_STATE_SIZE);
    midi_patched = 1;
    uart_print_string("[midi] cb patched\r\n");
}

/* ── Audio clock self-init ────────────────────────────────────────────── */
/* Without this, the DICE3 HPLL is uninitialized and the kernel's
 * snd-usb-audio driver fails to probe (clock descriptors invalid).
 * The stock firmware waits for the host to send SET_CUR 48kHz (the
 * "boot quirk"), but standalone sequencers and stock kernels don't.
 * At boot with TX/RX disabled, this is just a clock source table
 * lookup + mode flag write — no I2S, no scheduler, no side effects. */

static volatile uint32_t clock_initialized;
static volatile uint32_t loop_count;
static volatile uint32_t last_total_pkts;
static volatile uint32_t ep3_rx_enabled;
static volatile uint32_t ep3_rx_primed;

static void uart_hex32(uint32_t v) {
    static const char hex[] = "0123456789ABCDEF";
    for (int i = 28; i >= 0; i -= 4) uart_putc(hex[(v >> i) & 0xF]);
}

static void uart_hex8(uint8_t v) {
    static const char hex[] = "0123456789ABCDEF";
    uart_putc(hex[(v >> 4) & 0xF]);
    uart_putc(hex[v & 0xF]);
}

/* usb_hw_ep_read_start OUT path: slot = (*(usb_midi_conn+4)) + (ep_num-1)*0x40;
 * qh/dTD ptr @ slot+0x1F4, gate byte @ +0x1F8 (must be 0 to enter), +0x1FC scratch.
 * See firmware/midi_usb_handoff.txt — do not confuse with ep_handle+0x20. */
static void uart_log_ep3_slot_and_dma(void) {
    uint32_t tbl = *(volatile uint32_t *)((uint8_t *)usb_midi_conn + 4);
    if (tbl < 0x10000u || tbl >= 0x80000u)
        return;
    uint8_t *slot = (uint8_t *)(uintptr_t)(tbl + 2u * 0x40u); /* EP3 → index 2 */
    uint8_t g1f8 = *(volatile uint8_t *)(slot + 0x1f8);
    uint8_t g1fc = *(volatile uint8_t *)(slot + 0x1fc);
    uint32_t qh_slot = *(volatile uint32_t *)(slot + 0x1f4);
    uart_putc(' ');
    uart_putc('s');
    uart_putc('l');
    uart_putc('=');
    uart_hex8(g1f8);
    uart_putc('>');
    uart_hex8(g1fc);
    uart_putc(' ');
    uart_putc('s');
    uart_putc('q');
    uart_putc('=');
    uart_hex32(qh_slot);
}

/* Gates + call order: firmware/usb_dma_ep_ghidra.txt (Ghidra: usb_endpoint_submit_transfer
 * sets ep+0x08 then calls start_fn; usb_hw_ep_start_transfer_rx needs (char)ep+0x24==0x02 and
 * ep+0x08!=0, buffer aligned — else completion_cb runs with -5/-16 and no 0x834 prime). */
static void usb_hw_ep_start_transfer_rx_trace(void *epv) {
    ep_handle_t *ep = (ep_handle_t *)epv;
    uint32_t cb = *(volatile uint32_t *)((uint8_t *)epv + 0x08);
    uint32_t buf = *(volatile uint32_t *)((uint8_t *)epv + 0x10);
    uint32_t sz = *(volatile uint32_t *)((uint8_t *)epv + 0x14);
    uint16_t mp_pre = *(volatile uint16_t *)((uint8_t *)epv + 0x3C);
    uint8_t pre_st = *(volatile uint8_t *)((uint8_t *)epv + 0x20);
    uint32_t pre_fl = ep->flags;
    volatile uint32_t *qh = ep->qh_ptr;
    uint32_t qh2_pre = qh[2];
    uint8_t post_st;
    uint32_t post_fl;
    uint32_t qh2_post;
    uint16_t mp_post;
    /* Ghidra: usb_hw_ep_start_transfer_rx gates on *(char *)(ep+0x24)==0x02 — that is
     * flags low byte, NOT ep->state (+0x20). JTAG dump: state=3, flags=0x205 → fail. */
    ep->flags = (ep->flags & ~0xFFu) | 0x02u;
    /* usb_hw_ep_dma_start_rx(ep+0x1c) reads MPS as *(ushort*)(uint32_t* param+8 words)
     * => ep+0x3C. HS bulk OUT needs 0x200 here or the prime path may not run. */
    *(volatile uint16_t *)((uint8_t *)epv + 0x3C) = 0x200u;
    mp_post = *(volatile uint16_t *)((uint8_t *)epv + 0x3C);
    /* Do not call usb_hw_ep_dma_start_rx from here: this runs as the EP
     * start_fn inside usb_midi_rx_start → usb_hw_ep_start_transfer_rx, which
     * already takes cyg_scheduler_lock; a second lock + direct dma caused
     * data/prefetch aborts (PC ~0x279xx). Use the stock wrapper only. */
    extern void usb_hw_ep_start_transfer_rx(void *);
    usb_hw_ep_start_transfer_rx(epv);
    post_st = *(volatile uint8_t *)((uint8_t *)epv + 0x20);
    post_fl = ep->flags;
    qh2_post = qh[2];
    uart_putc('R'); uart_putc(' ');
    uart_putc("0123456789ABCDEF"[pre_st & 0xF]);
    uart_putc('>');
    uart_putc("0123456789ABCDEF"[post_st & 0xF]);
    uart_putc(' ');
    uart_hex32(pre_fl);
    uart_putc('>');
    uart_hex32(post_fl);
    uart_putc(' ');
    uart_hex32(cb);
    uart_putc(' ');
    uart_hex32(buf);
    uart_putc(' ');
    uart_hex32(sz);
    uart_putc(' ');
    uart_hex32((uint32_t)mp_pre);
    uart_putc('>');
    uart_hex32((uint32_t)mp_post);
    /* +0x834 async prime often reads 0 (WO/pulse). QH[2]=dTD overlay; 0 = none. */
    uart_print_string(" q2=");
    uart_hex32(qh2_pre);
    uart_putc('>');
    uart_hex32(qh2_post);
    uart_putc('\r'); uart_putc('\n');
}

static void init_audio_clock(void) {
    if (clock_initialized)
        return;
    usb_audio_rx_set_sample_rate(48000);
    clock_initialized = 1;
    uart_print_string("[clock] 48kHz\r\n");
}

/* hooks.py patches firmware_entry's `bl rtos_app_init` at 0x344 to call
 * boot_init instead; no C code references this symbol, so the linker's
 * --gc-sections would otherwise strip it. `used` prevents that. */
__attribute__((used))
void boot_init(void) {
    /* Full LED SPI quiesce — EN/CS-only was insufficient; animation could
     * resume from stale CTRL/DMA path. Match spi_ip_block_quiesce order. */
    spi_ip_block_quiesce(REBOOT_LED_SPI);
    REBOOT_LED_GPIO = 0;
    REBOOT_LED_SPI[0x14 / 4] = 0xFF;
    __asm__ volatile ("mcr p15, 0, %0, c7, c10, 4" :: "r"(0) : "memory");

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
    midi_patched = 0;       /* BSS not zeroed in patch zone — must init explicitly */
    clock_initialized = 0;
    loop_count = 0;
    last_total_pkts = 0;
    ep3_rx_enabled = 0;
    ep3_rx_primed = 0;

    /* Initialize HPLL clock before USB starts. Without this, the kernel's
     * snd-usb-audio driver fails to probe (clock descriptors invalid).
     * Safe here: .ctors have populated the clock source table, but the
     * scheduler hasn't started yet so no USB/DMA/thread interference. */
    init_audio_clock();

    /* PATCH: Make firmware always take the FS MIDI arming path.
     *
     * At 0x19BE0: `cmp r3, #2` checks if speed == FS.
     * At 0x19BE4: `bne skip` branches if NOT FS (skips MIDI at HS).
     *
     * Replace `cmp r3, #2` (0xE3530002) with `cmp r3, r3` (0xE1530003)
     * so the comparison always equals → bne never branches → FS path runs.
     * The FS path uses max_pkt=64 which properly configures the DMA. */
    *(volatile uint32_t *)0x19BE0 = 0xE1530003;  /* cmp r3, r3 (always EQ) */
    icache_invalidate();

    uart_print_string("[boot_init] DCP registered\r\n");

    /* Chain to original rtos_app_init — starts scheduler, never returns */
    rtos_app_init();
}

/* ── MIDI engine loop hook (replaces bl midi_rx_poll at 0x5180) ───────── */
/* Called every 2-tick alarm cycle in midi_engine_thread.
 * Runs midi_rx_poll (the displaced function), then our processing. */

extern void midi_rx_poll(void);

void midi_loop_hook(void) {
    /* One-shot: confirm the HS speed-check bypass worked.
     * boot_init patches 0x19BE0 (cmp r3,#2 -> cmp r3,r3) so SET_CONFIGURATION
     * now arms MIDI endpoints at High Speed. Just log the result. */
    if (!ep3_rx_enabled) {
        uint32_t ep_h = *(volatile uint32_t *)(midi_pkt_buf + MIDI_PKT_RX_EP);
        if (ep_h) {
            uart_print_string("[midi] ep=");
            {
                static const char hex[] = "0123456789ABCDEF";
                for (int i = 28; i >= 0; i -= 4) uart_putc(hex[(ep_h >> i) & 0xF]);
            }
        } else {
            /* After soft reboot, first hook often runs before SET_CONFIGURATION
             * arms midi_pkt_buf+RX_EP. Do not latch ep3_rx_enabled until we see
             * a non-zero handle — otherwise we never run the EP3 patch (cold
             * boot usually has the handle already). */
            static uint8_t ep_wait_logged;
            if (!ep_wait_logged) {
                uart_print_string("[midi] ep=0 (waiting for USB arm)\r\n");
                ep_wait_logged = 1;
            }
        }
        if (ep_h) {
            uint8_t st = *(volatile uint8_t *)(ep_h + 0x20);
            uart_print_string(" st=");
            uart_putc('0' + st);
            uart_print_string(" f=");
            uart_hex32(*(volatile uint32_t *)(ep_h + 0x00));
            uart_print_string(" cb=");
            uart_hex32(*(volatile uint32_t *)(ep_h + 0x08));
            uart_print_string(" b=");
            uart_hex32(*(volatile uint32_t *)(ep_h + 0x10));
            uart_print_string(" n=");
            uart_hex32(*(volatile uint32_t *)(ep_h + 0x14));
            uart_print_string(" fl=");
            uart_hex32(*(volatile uint32_t *)(ep_h + 0x24));
            uart_print_string(" m=");
            uart_hex32((uint32_t)*(volatile uint16_t *)(ep_h + 0x3C));
            /* 2026-04-20: After ISR decomp, EP3 OUT stock init is correct —
             * no runtime ep_handle mutation needed here. See
             * firmware/usb_dma_ep_ghidra.txt. The HS-skip bypass in boot_init
             * is sufficient to enter the arming path; remaining bug is that
             * QH[0] ACTIVE bit (26) / QH[6] state (0x80 vs 0x10) don't
             * transition correctly — needs deeper investigation. */
            uart_putc('\r'); uart_putc('\n');
            if (!ep3_rx_primed) {
                *(volatile uint32_t *)(ep_h + 0x00) = (uint32_t)(void *)usb_hw_ep_start_transfer_rx_trace;
                uart_print_string("[midi] ep3 start_fn->trace\r\n");
                ep3_rx_primed = 1;
            }
            ep3_rx_enabled = 1;
        }
    }

    /* Call the displaced midi_rx_poll first — processes USB MIDI RX data */
    midi_rx_poll();

    /* Patch MIDI parser callback (idempotent — first call patches, rest no-op) */
    patch_midi_callback();

    /* JTAG mailbox polling */
    process_mailbox();

    /* Monitor USB MIDI RX state — report changes */
    uint32_t total = *(volatile uint32_t *)(midi_pkt_buf + MIDI_PKT_TOTAL);
    if (total != last_total_pkts) {
        static const char hex[] = "0123456789ABCDEF";
        uart_print_string("[rx] ");
        uart_putc(hex[(total >> 4) & 0xF]);
        uart_putc(hex[total & 0xF]);
        uart_putc('\r'); uart_putc('\n');
        last_total_pkts = total;
    }
    /* 2026-04-20: removed periodic `TCAT_EP_ASYNC_PRIME |= EP_BIT_RX(3)`.
     * Post-ISR-decomp, 0x90000834 is the IN async-prime register, not OUT.
     * OUT direction endpoints (like EP3 MIDI RX) don't use 0x834. See
     * dice_usb_regs.h and firmware/usb_dma_ep_ghidra.txt for the corrected
     * direction labels. The old reinforcement poke was pulsing bit 3 of
     * 0x834 which is actually EP3 IN prime — no effect for MIDI RX. */

    /* Periodic EP status + USB registers (~every 4 seconds). */
    loop_count++;
    if ((loop_count & 0x3FF) == 0) {
        uint32_t ep_handle = *(volatile uint32_t *)(midi_pkt_buf + MIDI_PKT_RX_EP);
        uint8_t cable_cnt = *(volatile uint8_t *)(midi_pkt_buf + MIDI_PKT_CABLE_CNT);
        uint32_t complete_fn = ep_handle ? *(volatile uint32_t *)((uint8_t *)ep_handle + 8) : 0;
        uint32_t start_fn = ep_handle ? *(volatile uint32_t *)((uint8_t *)ep_handle + 0) : 0;
        uint32_t qh_ptr = ep_handle ? *(volatile uint32_t *)((uint8_t *)ep_handle + 0x1C) : 0;
        uint8_t ep_state = ep_handle ? *(volatile uint8_t *)((uint8_t *)ep_handle + 0x20) : 0;
        uint32_t comp_status = TCAT_EP_COMP_STATUS;
        uint32_t comp_enable = TCAT_EP_COMP_ENABLE;
        uint32_t async_prime = TCAT_EP_ASYNC_PRIME;
        uint32_t tcat_portsc = TCAT_PORTSC;
        uint32_t tcat_rx_en = TCAT_EP_RX_EN;
        uint32_t tcat_tx_en = TCAT_EP_TX_EN;
        uint32_t epprime = *(volatile uint32_t *)(USB_BASE + 0x80);
        uint32_t epstat = *(volatile uint32_t *)(USB_BASE + 0x88);
        uint32_t epcomp = *(volatile uint32_t *)(USB_BASE + 0x8C);
        uint32_t epctrl3 = *(volatile uint32_t *)(USB_BASE + 0x9C);
        uint32_t qh0 = 0, qh2 = 0, qh4 = 0, qh6 = 0;
        if (qh_ptr >= 0x90000000u && qh_ptr < 0x90001000u) {
            volatile uint32_t *qh = (volatile uint32_t *)(uintptr_t)qh_ptr;
            qh0 = qh[0];
            qh2 = qh[2];
            qh4 = qh[4];
            qh6 = qh[6];
        }
        /* Fixed banks for quick sanity check against qh_ptr-dereferenced data. */
        volatile uint32_t *qh_out_fixed = TCAT_QH_EP3_OUT;
        volatile uint32_t *qh_in_fixed  = TCAT_QH_EP3_IN;
        uint32_t q0o = qh_out_fixed[0];
        uint32_t q0i = qh_in_fixed[0];

        uart_putc(ep_handle ? 'E' : 'e');    /* E=ep set, e=no ep */
        uart_putc(complete_fn ? 'C' : 'c');  /* C=callback set, c=no cb */
        uart_putc('0' + cable_cnt);          /* cable count digit */
        uart_putc(' ');
        uart_putc('s'); uart_putc('='); uart_putc('0' + ep_state);
        uart_putc(' ');
        uart_putc('f'); uart_putc('='); uart_hex32(start_fn);
        uart_putc(' ');
        uart_putc('q'); uart_putc('p'); uart_putc('=');
        uart_hex32(qh_ptr);
        uart_putc(' ');
        uart_putc('c'); uart_putc('m'); uart_putc('p'); uart_putc('=');
        uart_hex32(comp_status);
        uart_putc(' ');
        uart_putc('e'); uart_putc('n'); uart_putc('=');
        uart_hex32(comp_enable);
        uart_putc(' ');
        uart_putc('p'); uart_putc('r'); uart_putc('=');
        uart_hex32(async_prime);
        (void)tcat_portsc; /* mostly churn/noise; omit from periodic log */
        uart_putc(' ');
        uart_putc('r'); uart_putc('x'); uart_putc('=');
        uart_hex32(tcat_rx_en);
        uart_putc(' ');
        uart_putc('t'); uart_putc('x'); uart_putc('=');
        uart_hex32(tcat_tx_en);
        uart_putc(' ');
        uart_putc('p'); uart_putc('m'); uart_putc('=');
        uart_hex32(epprime);
        uart_putc(' ');
        uart_putc('s'); uart_putc('t'); uart_putc('=');
        uart_hex32(epstat);
        uart_putc(' ');
        uart_putc('c'); uart_putc('p'); uart_putc('=');
        uart_hex32(epcomp);
        uart_putc(' ');
        uart_putc('e'); uart_putc('3'); uart_putc('=');
        uart_hex32(epctrl3);
        uart_putc(' ');
        uart_putc('q'); uart_putc('0'); uart_putc('=');
        uart_hex32(qh0);
        uart_putc(' ');
        uart_putc('q'); uart_putc('2'); uart_putc('=');
        uart_hex32(qh2);
        uart_putc(' ');
        uart_putc('q'); uart_putc('4'); uart_putc('=');
        uart_hex32(qh4);
        uart_putc(' ');
        uart_putc('q'); uart_putc('6'); uart_putc('=');
        uart_hex32(qh6);
        uart_putc(' ');
        uart_putc('o'); uart_putc('0'); uart_putc('=');
        uart_hex32(q0o);
        uart_putc(' ');
        uart_putc('i'); uart_putc('0'); uart_putc('=');
        uart_hex32(q0i);
        uart_putc(' ');
        uart_putc('d'); uart_putc('m'); uart_putc('=');
        uart_hex32(TCAT_DMA_CFG);
        uart_putc(' ');
        uart_putc('z'); uart_putc('6'); uart_putc('=');
        uart_hex32(TCAT_QH_OUT(0)[6]);
        uart_putc('\r'); uart_putc('\n');

        /* EC2 — one flash cycle: stack "next step" fields that EC1 omits:
         *   USB conn state, TCAT USBMODE, streaming speed byte,
         *   EP3 DMA buffer + size + flags + max_pkt + halted,
         *   ENDPTCTRL0 vs EP3, USBCMD/STS/INTR, addr/list,
         *   QH[1,3,5] (often dTD / overlay; EC1 has even words). */
        uart_print_string("EC2 ");
        {
            uint8_t usb_st = usb_conn_get_state(usb_midi_conn);
            uart_putc('u'); uart_putc('=');
            uart_putc("0123456789ABCDEF"[(usb_st >> 4) & 0xF]);
            uart_putc("0123456789ABCDEF"[usb_st & 0xF]);
        }
        uart_putc(' ');
        {
            uint8_t spd = *(volatile uint8_t *)(midi_streaming_state + 0x1C);
            uart_putc('g'); uart_putc('=');
            uart_putc("0123456789ABCDEF"[(spd >> 4) & 0xF]);
            uart_putc("0123456789ABCDEF"[spd & 0xF]);
        }
        uart_putc(' ');
        uart_putc('m'); uart_putc('=');
        uart_hex32(TCAT_USBMODE);
        if (ep_handle) {
            uint32_t bufp = *(volatile uint32_t *)((uint8_t *)ep_handle + ECOS_EP_BUFFER);
            int32_t bufsz = *(volatile int32_t *)((uint8_t *)ep_handle + ECOS_EP_BUFFER_SIZE);
            uint32_t halted_w = *(volatile uint32_t *)((uint8_t *)ep_handle + ECOS_EP_HALTED);
            uint32_t epflags = *(volatile uint32_t *)((uint8_t *)ep_handle + 0x24);
            uint16_t mps = *(volatile uint16_t *)((uint8_t *)ep_handle + 0x3C);
            uart_putc(' ');
            uart_putc('b'); uart_putc('p'); uart_putc('=');
            uart_hex32(bufp);
            uart_putc(' ');
            uart_putc('b'); uart_putc('s'); uart_putc('=');
            uart_hex32((uint32_t)bufsz);
            uart_putc(' ');
            uart_putc('h'); uart_putc('l'); uart_putc('=');
            uart_hex32(halted_w);
            uart_putc(' ');
            uart_putc('F'); uart_putc('=');
            uart_hex32(epflags);
            uart_putc(' ');
            uart_putc('m'); uart_putc('p'); uart_putc('s'); uart_putc('=');
            uart_hex32((uint32_t)mps);
            uart_log_ep3_slot_and_dma();
        }
        uart_putc(' ');
        uart_putc('e'); uart_putc('0'); uart_putc('=');
        uart_hex32(*(volatile uint32_t *)(USB_BASE + 0x90));
        uart_putc(' ');
        uart_putc('C'); uart_putc('m'); uart_putc('d'); uart_putc('=');
        uart_hex32(USB_USBCMD);
        uart_putc(' ');
        uart_putc('S'); uart_putc('t'); uart_putc('s'); uart_putc('=');
        uart_hex32(USB_USBSTS);
        uart_putc(' ');
        uart_putc('I'); uart_putc('n'); uart_putc('t'); uart_putc('=');
        uart_hex32(USB_USBINTR);
        uart_putc(' ');
        uart_putc('D'); uart_putc('a'); uart_putc('=');
        uart_hex32(USB_DEVICEADDR);
        uart_putc(' ');
        uart_putc('E'); uart_putc('l'); uart_putc('a'); uart_putc('=');
        uart_hex32(USB_ENDPTLISTADDR);
        uart_putc(' ');
        uart_putc('F'); uart_putc('r'); uart_putc('=');
        uart_hex32(USB_FRINDEX);
        uart_putc('\r'); uart_putc('\n');
    }

    /* Emit MIDI CC feedback for mixer state changes (knobs, BLE) */
    midi_emit_state_diff();
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

    case 5: { /* MIDI_INIT — patch MIDI callback (endpoints armed by firmware) */
        patch_midi_callback();
        /* Diagnostic: [0..3]=midi_patched, [4]=usb_state, [5]=rx_armed */
        *(uint32_t *)body = midi_patched;
        body[4] = usb_conn_get_state(usb_midi_conn);
        body[5] = (*(volatile uint32_t *)(midi_pkt_buf + MIDI_PKT_RX_EP)) ? 1 : 0;
        dcp_send_response(0, body, 8);
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

    case 7: { /* USB_REENUM — force USB re-enumeration (simulates cable replug) */
        *(uint32_t *)body = 0;
        dcp_send_response(0, body, 4);
        /* Brief delay so USB can send the response */
        for (volatile int i = 0; i < 100000; i++) {}
        uart_print_string("[usb] re-enumerate\r\n");
        usb_reenumerate();
        return;
    }

    case 8: { /* USB_DIAG — read USB controller + EP state */
        uint32_t *out = (uint32_t *)body;
        uint32_t ep_h = *(volatile uint32_t *)(midi_pkt_buf + MIDI_PKT_RX_EP);
        /* EP struct fields */
        out[0] = ep_h;  /* EP handle address */
        out[1] = ep_h ? *(volatile uint32_t *)((uint8_t *)ep_h + 0x1C) : 0;  /* qh_ptr */
        out[2] = ep_h ? *(volatile uint32_t *)((uint8_t *)ep_h + 0x20) : 0;  /* state+num */
        out[3] = ep_h ? *(volatile uint32_t *)((uint8_t *)ep_h + 0x24) : 0;  /* flags */
        out[4] = ep_h ? *(volatile uint32_t *)((uint8_t *)ep_h + 0x28) : 0;  /* dma_buf_copy */
        out[5] = ep_h ? *(volatile uint32_t *)((uint8_t *)ep_h + 0x2C) : 0;  /* dma_size_copy */
        /* QH from ep->qh_ptr (read actual HW QH) */
        uint32_t qh_addr = ep_h ? *(volatile uint32_t *)((uint8_t *)ep_h + 0x1C) : 0;
        if (qh_addr) {
            volatile uint32_t *qh = (volatile uint32_t *)qh_addr;
            out[6] = qh[0];  out[7] = qh[1];  out[8] = qh[2];
            out[9] = qh[3];  out[10] = qh[4]; out[11] = qh[5];
            out[12] = qh[6]; out[13] = qh[7];
        }
        /* USB controller registers */
        out[14] = *(volatile uint32_t *)0x90000818;  /* EP comp status */
        out[15] = *(volatile uint32_t *)0x9000081C;  /* EP comp enable */
        out[16] = *(volatile uint32_t *)0x90000810;  /* EP RX enable */
        out[17] = *(volatile uint32_t *)0x90000028;  /* ENDPTLISTADDR */
        dcp_send_response(0, body, 72);
        return;
    }

    default: {
        *(uint32_t *)body = ERR_UNKNOWN_OPCODE;
        dcp_send_response(0, body, 4);
        return;
    }

    }
}
