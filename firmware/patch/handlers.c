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
#include "uart_minprintf.h"
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

/* MIDI 7-bit (0-127) ⇄ mixer 5-bit (0-31) full-range maps.
 * Precomputed because the patch isn't linked against libgcc (no __aeabi_uidiv).
 * TX table values = round((level * 127) / 31), so level 31 → cc 127. */
static const uint8_t TX_LEVEL_TO_CC[32] = {
      0,   4,   8,  12,  16,  20,  25,  29,
     33,  37,  41,  45,  49,  53,  57,  62,
     66,  70,  74,  78,  82,  86,  90,  94,
     98, 103, 107, 111, 115, 119, 123, 127,
};
static inline uint8_t mixer_from_cc(uint8_t cc_val) {
    /* RX uses simple right-shift: 124..127 all map to level 31, lossless
     * round-trip with TX_LEVEL_TO_CC for the 32 canonical points. */
    return (uint8_t)((cc_val & 0x7F) >> 2);
}
static inline uint8_t cc_from_mixer(uint8_t level) {
    return TX_LEVEL_TO_CC[level & 0x1F];
}

/* ── MIDI CC output ─────────────────────────────────────────────────── */

static void midi_send_cc(uint8_t channel, uint8_t cc_num, uint8_t cc_val) {
    uint8_t msg[3] = { (uint8_t)(0xB0 | (channel & 0x0F)), cc_num & 0x7F, cc_val & 0x7F };
    int ret = midi_tx_write(msg, 3);
    uart_mini_printf("<%b %b %b %c>\r\n", (unsigned)msg[0], (unsigned)msg[1],
                     (unsigned)msg[2], (char)(ret > 0 ? 'k' : 'E'));
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
                midi_send_cc((uint8_t)bus, (uint8_t)(16 + input), cc_from_mixer(cur));
            }
        }
        /* Compressor → CC 23 */
        {
            int off = MIX_COMP_OFFSET + bus;
            uint8_t cur = live[off];
            if (cur != mixer_snapshot[off]) {
                mixer_snapshot[off] = cur;
                midi_send_cc((uint8_t)bus, 23, cc_from_mixer(cur));
            }
        }
        /* Master → CC 7 */
        {
            int off = MIX_MASTER_OFFSET + bus;
            uint8_t cur = live[off];
            if (cur != mixer_snapshot[off]) {
                mixer_snapshot[off] = cur;
                midi_send_cc((uint8_t)bus, 7, cc_from_mixer(cur));
            }
        }
    }
}

/* ── MIDI CC → Mixer Control ────────────────────────────────────────── */

static void midi_cc_handler(void *parser_ctx, void *parser_ctx2, int len) {
    uint8_t *p = (uint8_t *)parser_ctx;
    uint8_t status = p[0];

    uart_mini_printf("[%b %b %b]\r\n", (unsigned)p[0], (unsigned)p[1], (unsigned)p[2]);

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
    int level = (int)mixer_from_cc(cc_val);  /* 0-127 → 0-31, full-range linear */

    /* Use the canonical *_set_notify helpers — they update mixer_state,
     * apply DSP gain, refresh the LED level meter, and schedule the BLE
     * outbound notification in one call. Same path the knob/BLE dispatchers
     * use, so on-device LEDs and the BT phone app stay in sync.
     *
     * No echo on host CCs (idiomatic MIDI): we update mixer_snapshot in
     * lockstep so the diff path doesn't re-emit the same value back. */
    if (cc_num == 7) {
        master_level_set_notify(bus, level);
        mixer_snapshot[MIX_MASTER_OFFSET + bus] = (uint8_t)level;
    } else if (cc_num >= 16 && cc_num <= 22) {
        int input = cc_num - 16;
        channel_level_set_notify(input, bus, level);   /* input first! */
        mixer_snapshot[bus * MIX_INPUT_STRIDE + input] = (uint8_t)level;
    } else if (cc_num == 23) {
        compressor_level_set_notify(bus, level);
        mixer_snapshot[MIX_COMP_OFFSET + bus] = (uint8_t)level;
    }
    /* Unrecognized CCs fall through to original handler */

chain:
    midi_channel_msg_cb_orig(parser_ctx, parser_ctx2, len);
}

/* ── USB control ────────────────────────────────────────────────────── */

/* Force USB re-enumeration via DWC2 soft disconnect/reconnect.
 * DCTL.SFTDISCON (bit 1) = 1 holds the D+ pull-up low (host sees disconnect);
 * clearing it re-asserts the pull-up (host sees a new connect). */
static void usb_reenumerate(void) {
    USB_DCTL |= DCTL_SFTDISCON;
    /* Wait ~500ms for host to fully process disconnect */
    for (volatile int i = 0; i < 2000000; i++) {}
    USB_DCTL &= ~DCTL_SFTDISCON;
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
    uart_mini_printf("[midi] cb patched\r\n");
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
/* 2026-04-23 cleanup: removed ep3_slot_gate_clears (confirmed gate is normal
 * stock behavior, not blocker), ep3_trace_calls (legacy IN-variant, unused),
 * ep3_flags_fixes (confirmed stable), ep3_manual_kicks (removed with IN kick). */
/* DWC2-aware EP3 OUT instrumentation */
static volatile uint32_t ep3_active_pokes;       /* DOEPCTL3 EPENA|CNAK|USBACTEP repokes */
static volatile uint32_t ep3_xfer_compl;         /* DOEPINT3.XFERCOMPL sightings */
static volatile uint32_t ep3_out_disabled;       /* DOEPINT3.OUTTKNEPDIS sightings */
/* Slave-mode HS arm state (DWC2 v3.20a, GAHBCFG.DMAEn=0). */
static volatile uint32_t ep3_hs_arms;            /* hsa: full slave-mode arm sequences */
static volatile uint32_t ep3_hs_rearms;          /* hsr: re-arms after EPENA observed clear */
static volatile uint32_t ep3_dcfg_fixups;        /* DCFG.DevSpd→HS fix-ups applied */
static volatile uint32_t ep3_rxflvl_seen;        /* rfc: GINTSTS.RXFLVL transitions seen */
static volatile uint32_t ep3_rxflvl_last;        /* last observed GINTSTS.RXFLVL bit */
static volatile uint32_t ep3_last_q0;            /* last observed DOEPCTL3 (for EPENA-clear edge detect) */
static volatile uint32_t ep3_out_trace_calls;     /* 2026-04-22: OUT-variant wrapper (0x2140C) calls */
static volatile uint32_t ep3_out_trace_installed; /* 1 once wrapper is installed as start_fn */
/* 2026-04-23 instrumentation batch */
static volatile uint32_t ep3_cmp_calls;           /* completion callback invocations (ours wrapping 0x1B9E4) */
static volatile uint32_t ep3_cmp_installed;       /* 1 once complete_fn wrapper installed */
static volatile uint32_t ep3_q2_changes;          /* DOEPINT3 value-change observations */
static volatile uint32_t ep3_q2_last;             /* last observed DOEPINT3 */
static volatile uint32_t ep3_cmps_seen;           /* DAINT bit 19 (EP3 OUT pending) sightings */
static volatile uint32_t ep3_cf_clears;           /* complete_fn observed cleared (= ISR processed completion) */
static volatile uint32_t ep3_cf_last;             /* last observed ep+0x08 (complete_fn) */
static volatile uint32_t ep3_force_rearms;        /* manual re-arm attempts */
static volatile int32_t  ep3_stuck_loops;         /* consecutive loops with state==3 + no q2 progress */

/* 2026-04-23 cleanup: removed uart_log_ep3_slot_and_dma + ep3_force_slot_gate_open
 * + associated logging. Slot gate clears stopped being informative once we
 * established the gate transition pattern is stock-correct. */

/* 2026-04-22: removed legacy IN-variant trace wrapper and manual IN-kick path.
 * Under corrected direction labels, 0x21A6C is the IN submit path, wrong for
 * EP3 OUT. Replaced by usb_hw_ep_start_transfer_out_trace below. */

/* 2026-04-22: OUT-variant trace. 0x2140C is the OUT submit path (dma_start;
 * no 0x834 poke). Installed as ep->start_fn to count stock-flow invocations. */
static void usb_hw_ep_start_transfer_out_trace(void *epv) {
    ep3_out_trace_calls++;
    extern void usb_hw_ep_start_transfer(void *);
    usb_hw_ep_start_transfer(epv);
}

/* 2026-04-23: completion-callback trace. Stock submit_transfer fills ep+0x08
 * with the callback ptr passed by usb_midi_rx_dispatch (= 0x1B9E4). After
 * first arm we override ep+0x08 with our wrapper so every completion-IRQ
 * upcall is counted. If ep3_cmp_calls stays 0 across host bursts, the
 * completion IRQ never reaches the MIDI layer. */
static void usb_midi_rx_complete_trace(void *ctx, int byte_count) {
    ep3_cmp_calls++;
    if (ep3_cmp_calls <= 4u)
        uart_mini_printf("[midi] cmp %x %x\r\n", ep3_cmp_calls, (unsigned)byte_count);
    extern void usb_midi_rx_complete(void *, int);
    usb_midi_rx_complete(ctx, byte_count);
}

static void init_audio_clock(void) {
    if (clock_initialized)
        return;
    usb_audio_rx_set_sample_rate(48000);
    clock_initialized = 1;
    uart_mini_printf("[clock] 48kHz\r\n");
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
    ep3_active_pokes = 0;
    ep3_xfer_compl = 0;
    ep3_out_disabled = 0;
    ep3_out_trace_calls = 0;
    ep3_out_trace_installed = 0;
    ep3_cmp_calls = 0;
    ep3_cmp_installed = 0;
    ep3_q2_changes = 0;
    ep3_q2_last = 0;
    ep3_cmps_seen = 0;
    ep3_cf_clears = 0;
    ep3_cf_last = 0;
    ep3_force_rearms = 0;
    ep3_stuck_loops = 0;
    ep3_hs_arms = 0;
    ep3_hs_rearms = 0;
    ep3_dcfg_fixups = 0;
    ep3_rxflvl_seen = 0;
    ep3_rxflvl_last = 0;
    ep3_last_q0 = 0;

    /* Initialize HPLL clock before USB starts. Without this, the kernel's
     * snd-usb-audio driver fails to probe (clock descriptors invalid).
     * Safe here: .ctors have populated the clock source table, but the
     * scheduler hasn't started yet so no USB/DMA/thread interference. */
    init_audio_clock();
    uart_mini_printf("[midi] build tag=fx-flags-v1\r\n");

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

    /* IP is Synopsys DWC2 v3.20a (GSNPSID=0x4F54320A). Defensively set DWC2
     * device-mode interrupt masks — stock fw sets these during SET_CONFIG but
     * we've seen them read as 0 under some conditions. Idempotent OR. */
    USB_DOEPMSK  |= DOEP_XFERCOMPL | DOEP_EPDISBLD | DOEP_AHBERR
                  | DOEP_SETUP | DOEP_STSPHSERCVD;
    USB_DAINTMSK |= DAINT_IN(0) | DAINT_IN(1) | DAINT_IN(2)
                  | DAINT_OUT(0) | DAINT_OUT(3);

    uart_mini_printf("[boot_init] DCP registered\r\n");

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
        if (!ep_h) {
            /* After soft reboot, first hook often runs before SET_CONFIGURATION
             * arms midi_pkt_buf+RX_EP. Do not latch ep3_rx_enabled until we see
             * a non-zero handle — otherwise we never run the EP3 patch (cold
             * boot usually has the handle already). */
            static uint8_t ep_wait_logged;
            if (!ep_wait_logged) {
                uart_mini_printf("[midi] ep=0 (waiting for USB arm)\r\n");
                ep_wait_logged = 1;
            }
        } else {
            uint8_t st = *(volatile uint8_t *)(ep_h + ECOS_EP_STATE);
            /* 2026-04-20: After ISR decomp, EP3 OUT stock init is correct —
             * no runtime ep_handle mutation needed here. See
             * firmware/usb_dma_ep_ghidra.txt. */
            uart_mini_printf("[midi] ep=%x st=%c\r\n", ep_h, (char)('0' + st));
            if (!ep3_rx_primed) {
                uint32_t fn0 = *(volatile uint32_t *)(ep_h + ECOS_EP_START_FN);
                uint32_t fn4 = *(volatile uint32_t *)(ep_h + ECOS_EP_SET_HALTED_FN);
                uint32_t fn8 = *(volatile uint32_t *)(ep_h + ECOS_EP_COMPLETE_FN);
                uart_mini_printf("[midi] ep3 fn0=%x fn4=%x fn8=%x\r\n", fn0, fn4, fn8);
                /* 2026-04-22: install OUT-variant trace wrapper as ep->start_fn
                 * to count every stock-flow invocation in ep3_out_trace_calls. */
                if (!ep3_out_trace_installed) {
                    *(volatile uint32_t *)(ep_h + ECOS_EP_START_FN) =
                        (uint32_t)(uintptr_t)usb_hw_ep_start_transfer_out_trace;
                    ep3_out_trace_installed = 1;
                    uart_mini_printf("[midi] installed OUT trace as start_fn\r\n");
                }

                /* No manual kick needed: OUT trace wrapper is now installed as
                 * ep->start_fn, so next stock submit-transfer call routes
                 * through our trace + stock OUT path and increments otc. */
                uart_mini_printf("[midi] otc=%x\r\n", ep3_out_trace_calls);
                ep3_rx_primed = 1;
            }
            ep3_rx_enabled = 1;
        }
    }

    /* Call the displaced midi_rx_poll first — processes USB MIDI RX data */
    midi_rx_poll();

    /* 2026-04-23 instrumentation: per-loop EP3 OUT state poll. */
    {
        uint32_t ep_h = *(volatile uint32_t *)(midi_pkt_buf + MIDI_PKT_RX_EP);
        if (ep_h) {
            /* (a) Install completion-callback wrapper once complete_fn is non-zero. */
            uint32_t cf = *(volatile uint32_t *)(ep_h + ECOS_EP_COMPLETE_FN);
            if (!ep3_cmp_installed && cf != 0u
                && cf != (uint32_t)(uintptr_t)usb_midi_rx_complete_trace) {
                *(volatile uint32_t *)(ep_h + ECOS_EP_COMPLETE_FN) =
                    (uint32_t)(uintptr_t)usb_midi_rx_complete_trace;
                ep3_cmp_installed = 1;
                uart_mini_printf("[midi] cmp wrapper installed (was %x)\r\n", cf);
            }
            /* (b) Track complete_fn transitions to zero — means stock ISR ran cmp. */
            if (cf != ep3_cf_last) {
                if (ep3_cf_last != 0u && cf == 0u) ep3_cf_clears++;
                ep3_cf_last = cf;
            }

            /* (c) DOEPINT3 — DWC2 per-EP interrupt reg.
             *   OUTTKNEPDIS = host OUT token arrived while EP disabled —
             *   the hw's way of telling us arming never stuck. */
            uint32_t q2_now = USB_DOEPINT(3);
            if (q2_now != ep3_q2_last) {
                ep3_q2_changes++;
                if (q2_now & DOEP_XFERCOMPL)    ep3_xfer_compl++;
                if (q2_now & DOEP_OUTTKNEPDIS)  ep3_out_disabled++;
                if (ep3_q2_changes <= 16u)
                    uart_mini_printf("[dwc2] DOEPINT3=%x\r\n", q2_now);
                ep3_q2_last = q2_now;
            }

            /* (d) DAINT bit 19 = EP3 OUT interrupt pending. */
            if (USB_DAINT & DAINT_OUT(3)) ep3_cmps_seen++;

            /* (e) Slave-mode HS arm. Once ep_handle is allocated (state==2/3
             *   means stock fw has reached configured / DMA-active), force
             *   DOEPCTL3/DOEPTSIZ3 into the canonical HS-bulk-OUT shape and
             *   leave it there. Re-run the sequence each time hw clears EPENA
             *   (one-shot completion or OUTTKNEPDIS auto-disable). DOEPDMA3
             *   is intentionally untouched — slave mode (GAHBCFG.DMAEn=0)
             *   ignores it. */
            uint32_t q0_now = USB_DOEPCTL(3);
            uint8_t st = *(volatile uint8_t *)(ep_h + ECOS_EP_STATE);
            uint8_t want_arm = 0;
            /* Only arm once stock fw has progressed ep_state to 2 or 3
             * (DMA_ACTIVE / READY). Arming earlier (st==0/1) clashes with
             * stock fw's setup and can leave the EP halted (hl=1). */
            if (st == 3u || st == 2u) {
                if ((q0_now & DEPCTL_EPENA) == 0u) {
                    /* hw cleared EPENA after one-shot completion — re-arm. */
                    if ((ep3_last_q0 & DEPCTL_EPENA) != 0u) {
                        ep3_hs_rearms++;
                    }
                    want_arm = 1;
                } else if (ep3_hs_arms == 0u) {
                    /* Boot path: first observation, stamp register fields
                     * to canonical values. */
                    want_arm = 1;
                }
                /* DON'T re-arm on NAKSTS=1 alone — see comment in earlier
                 * commit; the FIFO-drain is the ISR's job. */
            }
            if (want_arm) {
                /* Pre-arm DCFG fix-up: if the host enumerated us at HS speed
                 * (DSTS.EnumSpd=0) but DCFG.DevSpd is non-HS (probably forced
                 * by the FS arming path running via the HS-skip patch), clear
                 * DevSpd back to 0 so the controller advertises HS. Idempotent. */
                if ((USB_DSTS & DSTS_ENUMSPD_MASK) == DSTS_ENUMSPD_HS &&
                    (USB_DCFG & DCFG_DEVSPD_MASK) != DCFG_DEVSPD_HS) {
                    USB_DCFG = (USB_DCFG & ~DCFG_DEVSPD_MASK) | DCFG_DEVSPD_HS;
                    ep3_dcfg_fixups++;
                }

                USB_DOEPINT(3)  = 0xFFFFFFFFu;                              /* W1C stale */
                USB_DOEPTSIZ(3) = DEPTSIZ_PKTCNT(1) | DEPTSIZ_XFERSIZE(512);
                USB_DOEPCTL(3) =
                    (q0_now & ~(DEPCTL_MPS_MASK | DEPCTL_EPTYPE_MASK | DEPCTL_STALL))
                    | DEPCTL_MPS(512) | DEPCTL_EPTYPE_BULK | DEPCTL_USBACTEP
                    | DEPCTL_EPENA | DEPCTL_CNAK;
                USB_DAINTMSK |= DAINT_OUT(3);

                ep3_hs_arms++;
                ep3_active_pokes++;
                if (ep3_hs_arms <= 4u || (ep3_hs_arms & 0x3F) == 0u) {
                    uart_mini_printf("[dwc2] hsarm #%x q0pre=%x q0post=%x dcfg=%x dsts=%x\r\n",
                                     ep3_hs_arms, q0_now, USB_DOEPCTL(3),
                                     USB_DCFG, USB_DSTS);
                }
            }
            ep3_last_q0 = q0_now;

            /* (f) RXFLVL telemetry. If GINTSTS.RXFLVL toggles, the global
             *   RX FIFO has data — useful for distinguishing "tokens never
             *   reach controller" from "tokens reach controller but ISR
             *   doesn't classify EP3 OUT". */
            uint32_t gints = USB_GINTSTS;
            uint32_t rxflvl_now = (gints & GINT_RXFLVL) ? 1u : 0u;
            if (rxflvl_now != ep3_rxflvl_last) {
                ep3_rxflvl_seen++;
                ep3_rxflvl_last = rxflvl_now;
            }
        }
    }

    /* Patch MIDI parser callback (idempotent — first call patches, rest no-op) */
    patch_midi_callback();

    /* JTAG mailbox polling */
    process_mailbox();

    /* Monitor USB MIDI RX state — report changes */
    uint32_t total = *(volatile uint32_t *)(midi_pkt_buf + MIDI_PKT_TOTAL);
    if (total != last_total_pkts) {
        uart_mini_printf("[rx] %b\r\n", (unsigned)(uint8_t)total);
        last_total_pkts = total;
    }
    /* Periodic EP status + USB registers (~every 4 seconds). */
    loop_count++;
    if ((loop_count & 0x3FF) == 0) {
        uint32_t ep_handle = *(volatile uint32_t *)(midi_pkt_buf + MIDI_PKT_RX_EP);
        uint8_t cable_cnt = *(volatile uint8_t *)(midi_pkt_buf + MIDI_PKT_CABLE_CNT);
        uint32_t complete_fn = ep_handle ? *(volatile uint32_t *)((uint8_t *)ep_handle + 8) : 0;
        uint32_t start_fn = ep_handle ? *(volatile uint32_t *)((uint8_t *)ep_handle + 0) : 0;
        uint32_t ep_regs = ep_handle ? *(volatile uint32_t *)((uint8_t *)ep_handle + ECOS_EP_REGS) : 0;
        uint8_t ep_state = ep_handle ? *(volatile uint8_t *)((uint8_t *)ep_handle + ECOS_EP_STATE) : 0;

        /* EC1 — EP3 OUT register window (DOEPCTL3 / DOEPINT3 / DOEPTSIZ3 /
         * DOEPDMA3) + DAINT / DAINTMSK + counters.
         *   qp= ep->ep_regs (was qh_ptr; now points at DOEPCTL3 MMIO)
         *   q0= DOEPCTL3   q2= DOEPINT3   q4= DOEPTSIZ3   q5= DOEPDMA3
         *   da= DAINT      dam= DAINTMSK
         *   dim= DIEPMSK   dom= DOEPMSK   dem= DIEPEMPMSK   th= DTHRCTL */
        uint32_t q0 = USB_DOEPCTL(3);
        uint32_t q2 = USB_DOEPINT(3);
        uint32_t q4 = USB_DOEPTSIZ(3);
        uint32_t q5 = USB_DOEPDMA(3);

        uart_mini_printf(
            "%c%c%c s=%c f=%x qp=%x q0=%x q2=%x q4=%x q5=%x da=%x dam=%x "
            "dim=%x dom=%x dem=%x th=%x "
            "otc=%x cmc=%x q2c=%x css=%x cfc=%x fr=%x act=%x xc=%x od=%x "
            "hsa=%x hsr=%x dfx=%x rfc=%x\r\n",
            ep_handle ? 'E' : 'e', complete_fn ? 'C' : 'c', (char)('0' + cable_cnt),
            (char)('0' + ep_state), start_fn, ep_regs,
            q0, q2, q4, q5,
            USB_DAINT, USB_DAINTMSK,
            USB_DIEPMSK, USB_DOEPMSK, USB_DIEPEMPMSK, USB_DTHRCTL,
            ep3_out_trace_calls, ep3_cmp_calls, ep3_q2_changes,
            ep3_cmps_seen, ep3_cf_clears, ep3_force_rearms, ep3_active_pokes,
            ep3_xfer_compl, ep3_out_disabled,
            ep3_hs_arms, ep3_hs_rearms, ep3_dcfg_fixups, ep3_rxflvl_seen);

        /* EC2 — eCos conn state + speed byte + DSTS.EnumSpd decoded
         *       (es=H/F/L/X) + DCFG/DCTL/DSTS + ep_handle fields. */
        uint32_t dsts = USB_DSTS;
        uint32_t enumspd = (dsts & DSTS_ENUMSPD_MASK) >> DSTS_ENUMSPD_SHIFT;
        char es = (enumspd == 0u) ? 'H'
                : (enumspd == 1u) ? 'F'
                : (enumspd == 2u) ? 'L'
                                  : 'X';   /* 3 = FS-on-HS-PHY */
        uart_mini_printf(
            "EC2 u=%b g=%b es=%c dcfg=%x dctl=%x dsts=%x",
            (unsigned)(uint8_t)usb_conn_get_state(usb_midi_conn),
            (unsigned)*(volatile uint8_t *)(midi_streaming_state + 0x1C),
            es, USB_DCFG, USB_DCTL, dsts);
        if (ep_handle) {
            uint32_t bufp = *(volatile uint32_t *)((uint8_t *)ep_handle + ECOS_EP_BUFFER);
            int32_t bufsz = *(volatile int32_t *)((uint8_t *)ep_handle + ECOS_EP_BUFFER_SIZE);
            uint32_t halted_w = *(volatile uint32_t *)((uint8_t *)ep_handle + ECOS_EP_HALTED);
            uint32_t epflags = *(volatile uint32_t *)((uint8_t *)ep_handle + ECOS_EP_FLAGS);
            uint16_t mps = *(volatile uint16_t *)((uint8_t *)ep_handle + ECOS_EP_MAX_PKT);
            uart_mini_printf(" bp=%x bs=%x hl=%x F=%x mps=%x", bufp, (uint32_t)bufsz,
                             halted_w, epflags, (uint32_t)mps);
        }
        uart_mini_printf("\r\n");

        /* EC3 — DWC2 global regs:
         *   ha= GAHBCFG   us= GUSBCFG   gis= GINTSTS   gim= GINTMSK
         *   grf= GRXFSIZ  gnt= GNPTXFSIZ gns= GNPTXSTS
         *   hc2= GHWCFG2  hc3= GHWCFG3  sid= GSNPSID   gdc= GDFIFOCFG */
        uart_mini_printf(
            "EC3 ha=%x us=%x gis=%x gim=%x grf=%x gnt=%x gns=%x hc2=%x hc3=%x "
            "sid=%x gdc=%x\r\n",
            USB_GAHBCFG, USB_GUSBCFG, USB_GINTSTS, USB_GINTMSK,
            USB_GRXFSIZ, USB_GNPTXFSIZ, USB_GNPTXSTS,
            USB_GHWCFG2, USB_GHWCFG3, USB_GSNPSID, USB_GDFIFOCFG);
    }

    /* Emit MIDI CCs for any mixer_state changes since last poll.
     * midi_cc_handler updates mixer_snapshot in lockstep with host-driven
     * changes, so this only emits for BLE / knob / DCP-originated edits. */
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
            int level = (int)mixer_from_cc(cc_val);
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
        uart_mini_printf("[usb] re-enumerate\r\n");
        usb_reenumerate();
        return;
    }

    case 8: { /* USB_DIAG — read DWC2 controller + EP state */
        uint32_t *out = (uint32_t *)body;
        uint32_t ep_h = *(volatile uint32_t *)(midi_pkt_buf + MIDI_PKT_RX_EP);
        /* EP struct fields */
        out[0] = ep_h;                                                                    /* EP handle address */
        out[1] = ep_h ? *(volatile uint32_t *)((uint8_t *)ep_h + ECOS_EP_REGS) : 0;          /* ep_regs (DOEPCTL3) */
        out[2] = ep_h ? *(volatile uint32_t *)((uint8_t *)ep_h + ECOS_EP_STATE) : 0;         /* state+num */
        out[3] = ep_h ? *(volatile uint32_t *)((uint8_t *)ep_h + ECOS_EP_FLAGS) : 0;         /* flags */
        out[4] = ep_h ? *(volatile uint32_t *)((uint8_t *)ep_h + ECOS_EP_DMA_BUF_COPY) : 0;  /* dma_buf_copy */
        out[5] = ep_h ? *(volatile uint32_t *)((uint8_t *)ep_h + ECOS_EP_DMA_SIZE_COPY) : 0; /* dma_size_copy */
        /* Per-EP DWC2 register window (DOEPCTL3 / DOEPINT3 / DOEPTSIZ3 / DOEPDMA3 +
         * shadow words at +0x4 / +0xC / +0x18 / +0x1C). */
        uint32_t ep_regs = ep_h ? *(volatile uint32_t *)((uint8_t *)ep_h + ECOS_EP_REGS) : 0;
        if (ep_regs) {
            volatile uint32_t *r = (volatile uint32_t *)ep_regs;
            out[6] = r[0];  out[7] = r[1];  out[8] = r[2];
            out[9] = r[3];  out[10] = r[4]; out[11] = r[5];
            out[12] = r[6]; out[13] = r[7];
        }
        /* DWC2 device-mode + global registers */
        out[14] = USB_DAINT;
        out[15] = USB_DAINTMSK;
        out[16] = USB_DOEPMSK;
        out[17] = USB_GNPTXFSIZ;
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
