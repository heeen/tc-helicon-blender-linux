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

/* ── USB controller (Freescale/ChipIdea OTG at 0x90000000) ──── */

#define USB_USBCMD      (*(volatile uint32_t *)0x90000008)  /* bit 0=RS, bit 1=RST */
#define USB_ENDPTFLUSH   (*(volatile uint32_t *)0x90000014)
#define USB_USBMODE      (*(volatile uint32_t *)0x90000808)

/* Full USB controller reset: flush endpoints, clear RS (D+ released →
 * host sees disconnect), set RST (self-clearing, resets all internal state).
 * USBCMD.RST is bit 1 — self-clears when reset completes (~1ms).
 * After reset, USBMODE must be re-set to device mode (0x02). */
static void usb_hw_reset(void) {
    USB_ENDPTFLUSH = 0xFFFFFFFF;     /* flush all endpoints */
    USB_USBCMD &= ~(uint32_t)1;     /* clear RS → D+ released → disconnect */
    USB_USBCMD |= 2;                /* set RST → full controller reset */
    /* Wait for RST self-clear (typically <1ms) */
    for (volatile int i = 0; i < 100000; i++) {
        if (!(USB_USBCMD & 2)) break;
    }
}

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

/* ── Software reboot ─────────────────────────────────────────────────── */

static void __attribute__((noreturn)) software_reboot(void) {
    /* Full reboot: disconnect USB, tear down all peripherals, then jump to
     * TCAT bootloader at XIP 0x4F000. The bootloader re-validates CRC,
     * DMA-copies firmware from SPI to SRAM — equivalent to power cycle. */
    volatile uint32_t *spi = (volatile uint32_t *)0xCC000000;
    volatile uint32_t *dma = (volatile uint32_t *)0x80000000;

    uart_print_string("[reboot] start\r\n");

    /* 1. USB disconnect (while system still live, host sees clean removal) */
    usb_hw_reset();
    uart_print_string("[reboot] usb off\r\n");

    /* 2. SVC mode + disable IRQ+FIQ (bootloader expects SVC) */
    __asm__ volatile ("mov r0, #0xD3 \n msr cpsr_c, r0 \n" ::: "r0");
    uart_print_string("[reboot] irq off\r\n");

    /* 3. Stop eCos scheduler tick timer */
    *(volatile uint32_t *)0xC2000008 = 0;

    /* 4. Wait for any in-flight SPI/DMA transfer to complete.
     * sram_flash_driver_v2.c warns: touching DMA_EN/CHCLR during active
     * transfer permanently breaks the DMA engine. */
    for (volatile int i = 0; i < 100000; i++) {
        if (!(spi[0x28 / 4] & 1)) break;  /* SPI_STAT bit 0 = busy */
    }
    uart_print_string("[reboot] spi idle\r\n");

    /* 5. Fully disable SPI */
    spi[0x10 / 4] = 0;    /* SPI_CS = 0 */
    spi[0x08 / 4] = 0;    /* SPI_EN = 0 */
    spi[0x2C / 4] = 0;    /* SPI_DMAGO = 0 */
    spi[0x4C / 4] = 0;    /* SPI_DMAMD = 0 */
    spi[0x50 / 4] = 0;    /* SPI_DMACFG0 = 0 */
    spi[0x54 / 4] = 0;    /* SPI_DMACFG1 = 0 */
    __asm__ volatile ("mcr p15, 0, %0, c7, c10, 4" :: "r"(0) : "memory");
    uart_print_string("[reboot] spi off\r\n");

    /* 6. DMA engine teardown (proven pattern from spi_restore_clean) */
    dma[0x08 / 4] = 0;    /* DMA_EN = 0 — disable all channels */
    dma[0x10 / 4] = 3;    /* DMA_ICLR = clear ch0+ch1 */
    dma[0x30 / 4] = 1;    /* DMA_CHCLR(0) */
    dma[0x34 / 4] = 1;    /* DMA_CHCLR(1) */
    *(volatile uint32_t *)(0x80000110) = 0;  /* ch0 TRG */
    *(volatile uint32_t *)(0x8000010C) = 0;  /* ch0 CFG */
    *(volatile uint32_t *)(0x80000130) = 0;  /* ch1 TRG */
    *(volatile uint32_t *)(0x8000012C) = 0;  /* ch1 CFG */
    __asm__ volatile ("mcr p15, 0, %0, c7, c10, 4" :: "r"(0) : "memory");
    uart_print_string("[reboot] dma off\r\n");

    /* 7. Restore SPI to XIP-compatible power-on defaults */
    spi[0x00 / 4] = 1;    /* SPI_CTRL = 1 (power-on default) */
    spi[0x04 / 4] = 2;    /* SPI_LEN = 2 (power-on default) */
    spi[0x14 / 4] = 0x38; /* SPI_CLK = XIP mode */
    __asm__ volatile ("mcr p15, 0, %0, c7, c10, 4" :: "r"(0) : "memory");
    uart_print_string("[reboot] xip ok\r\n");

    /* 8. Invalidate caches and jump to bootloader */
    uart_print_string("[reboot] jump 0x4F000\r\n");
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

static void init_audio_clock(void) {
    if (clock_initialized)
        return;
    usb_audio_rx_set_sample_rate(48000);
    clock_initialized = 1;
    uart_print_string("[clock] 48kHz\r\n");
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
    midi_patched = 0;       /* BSS not zeroed in patch zone — must init explicitly */
    clock_initialized = 0;
    loop_count = 0;
    last_total_pkts = 0;
    ep3_rx_enabled = 0;

    /* Initialize HPLL clock before USB starts. Without this, the kernel's
     * snd-usb-audio driver fails to probe (clock descriptors invalid).
     * Safe here: .ctors have populated the clock source table, but the
     * scheduler hasn't started yet so no USB/DMA/thread interference. */
    init_audio_clock();

    /* EP3 MIDI RX reprime handled in midi_loop_hook after USB is up. */

    uart_print_string("[boot_init] DCP registered\r\n");

    /* Chain to original rtos_app_init — starts scheduler, never returns */
    rtos_app_init();
}

/* ── MIDI engine loop hook (replaces bl midi_rx_poll at 0x5180) ───────── */
/* Called every 2-tick alarm cycle in midi_engine_thread.
 * Runs midi_rx_poll (the displaced function), then our processing. */

extern void midi_rx_poll(void);

void midi_loop_hook(void) {
    /* Enable EP3 RX completion interrupt (one-shot, after USB controller init).
     * EP3 OUT QH reinit + reprime (one-shot, after USB controller is up). */
    if (!ep3_rx_enabled) {
        /* EP3 OUT (MIDI RX) — full QH reinit + reprime via direct MMIO.
         *
         * Root cause: usb_hw_ep_read_start fails during SET_CONFIGURATION:
         *   1. State byte (ep+0x20) must be 0 — dma_start set it to 3
         *   2. QH[0] bit 31 must be clear — dma_start set it (0x84000000)
         * So QH bits 28,27 never get set → DMA engine won't accept transfers.
         *
         * Fix: write correct QH configuration directly, then reprime.
         * QH at 0x90000B60 (TCAT RX bank, EP3 OUT, 0x20-byte block).
         * Completion interrupt bit 19 of 0x81C already set by firmware.
         */
        uint32_t ep_h = *(volatile uint32_t *)(midi_pkt_buf + MIDI_PKT_RX_EP);
        if (ep_h) {
            volatile uint8_t *ep_state = (volatile uint8_t *)(ep_h + 0x20);
            uint32_t qh_addr = *(volatile uint32_t *)((uint8_t *)ep_h + 0x1C);

            if (qh_addr && *ep_state != 2) {
                volatile uint32_t *qh = (volatile uint32_t *)qh_addr;

                /* Configure QH for bulk OUT, 512-byte max packet.
                 * Replicates what usb_hw_ep_read_start writes:
                 *   bit 28 = capability (0x10000000)
                 *   bit 27 = IOC (0x08000000)
                 *   bit 19 = bulk type (0x00080000)
                 *   bit 15 = IOS (0x00008000)
                 *   bits 9:0 = max_pkt (0x200)
                 * Clears bit 31 ("in use") and bit 21. */
                qh[0] = 0x18088200;

                /* Reset EP state to configured */
                *ep_state = 2;
                *(volatile uint32_t *)((uint8_t *)ep_h + ECOS_EP_COMPLETE_FN) = 0;

                /* Reprime via firmware rx_start → dispatch → submit → dma_start.
                 * dma_start will set QH[0] |= 0x84000000 (active+IOC)
                 * and QH[4] with transfer size. */
                uint8_t cables = *(volatile uint8_t *)(midi_pkt_buf + MIDI_PKT_CABLE_CNT);
                usb_midi_rx_start((void *)ep_h, cables ? cables : 1);

                /* Kick the standard ChipIdea ENDPTPRIME register.
                 * TCAT proprietary registers handle DMA config, but the
                 * controller may still need ENDPTPRIME to start accepting
                 * bulk OUT tokens. Bit 3 = EP3 RX (USB OUT direction). */
                *(volatile uint32_t *)0x90000080 = (1u << 3);  /* ENDPTPRIME EP3 RX */

                /* Log QH after reprime */
                uint32_t qh0 = qh[0];
                static const char hex[] = "0123456789ABCDEF";
                uart_print_string("[midi] QH=");
                for (int i = 28; i >= 0; i -= 4) uart_putc(hex[(qh0 >> i) & 0xF]);
                uart_putc('\r'); uart_putc('\n');
            }
        }
        ep3_rx_enabled = 1;
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

    /* Periodic EP status (every ~60 seconds = 4096 loops at ~15ms) */
    loop_count++;
    if ((loop_count & 0xFFF) == 0) {
        uint32_t ep_handle = *(volatile uint32_t *)(midi_pkt_buf + MIDI_PKT_RX_EP);
        uint8_t cable_cnt = *(volatile uint8_t *)(midi_pkt_buf + MIDI_PKT_CABLE_CNT);
        uint32_t complete_fn = ep_handle ? *(volatile uint32_t *)((uint8_t *)ep_handle + 8) : 0;
        uart_putc(ep_handle ? 'E' : 'e');    /* E=ep set, e=no ep */
        uart_putc(complete_fn ? 'C' : 'c');  /* C=callback set, c=no cb */
        uart_putc('0' + cable_cnt);          /* cable count digit */
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
