/*
 * dice_platform.h — eCos / TCAT DICE3 platform declarations for Blender firmware patches
 *
 * Provides typed access to firmware functions and data structures.
 * All addresses resolved at link time via firmware_symbols.ld.
 *
 * Structure layouts derived from:
 *   - Ghidra analysis of blender_primary_body.bin
 *   - eCos open-source kernel headers (packages/io/usb/, packages/kernel/)
 *   - JTAG runtime inspection of SRAM
 *
 * TCAT DICE3 extends standard eCos USB with proprietary streaming,
 * DCP (DICE Control Protocol), and audio routing layers.
 */

#ifndef DICE_PLATFORM_H
#define DICE_PLATFORM_H

#include <stdint.h>

/* ════════════════════════════════════════════════════════════════════
 * eCos Kernel — minimal types and API
 *
 * Reference: ecos/packages/kernel/current/include/kapi.h
 *            ecos/packages/kernel/current/include/ktypes.h
 * ════════════════════════════════════════════════════════════════════ */

typedef uint32_t  cyg_handle_t;
typedef uint32_t  cyg_addrword_t;
typedef int32_t   cyg_bool_t;
typedef uint64_t  cyg_tick_count_t;

extern void cyg_scheduler_lock(void);
extern void cyg_scheduler_unlock(void);
extern void cyg_thread_delay(uint32_t ticks);
extern cyg_tick_count_t cyg_current_time(void);

/* ════════════════════════════════════════════════════════════════════
 * USB Device Stack — eCos base + TCAT DICE3 extensions
 *
 * eCos base: ecos/packages/io/usb/slave/current/include/usbs.h
 * TCAT extends endpoints and adds connection/streaming objects.
 *
 * USB device state machine (eCos standard, usbs.h):
 *   DETACHED=1, ATTACHED=2, POWERED=3, DEFAULT=4,
 *   ADDRESSED=5, CONFIGURED=6
 * ════════════════════════════════════════════════════════════════════ */

/* ── eCos USB Endpoint Base Layout ──────────────────────────────────
 * From eCos usbs_rx_endpoint / usbs_tx_endpoint:
 *   +0x00  void (*start_fn)(endpoint*)     // start transfer
 *   +0x04  void (*set_halted_fn)(ep*, bool) // halt/unhalt
 *   +0x08  void (*complete_fn)(void*, int)  // completion callback
 *   +0x0C  void *complete_data              // callback context
 *   +0x10  uint8_t *buffer                  // DMA buffer
 *   +0x14  int buffer_size                  // buffer length
 *   +0x18  cyg_bool halted                  // halted flag
 *
 * Beyond 0x18 the eCos subslot extends with DWC2-specific fields:
 *   +0x1C  uint32_t *ep_regs                // pointer into per-EP DWC2 reg
 *                                              window (DIEPCTLn / DOEPCTLn);
 *                                              named "qh_ptr" in the original
 *                                              ChipIdea-derived driver, but
 *                                              there is no SRAM dQH on DWC2.
 *   +0x20  uint8_t  state                   // 0=idle, 2=configured, 3=DMA active
 *   +0x21  uint8_t  _pad21[3]
 *   +0x24  uint32_t flags                   // ep_num | (attr << 8)
 *   +0x28  void    *dma_buf_copy            // copy of buffer during DMA
 *   +0x2C  uint32_t dma_size_copy
 *   +0x30  uint32_t _reserved[2]
 *   +0x38  uint16_t max_pkt                 // used by dma_start
 * ─────────────────────────────────────────────────────────────────── */

#define ECOS_EP_START_FN       0x00
#define ECOS_EP_SET_HALTED_FN  0x04
#define ECOS_EP_COMPLETE_FN    0x08  /* non-zero = transfer in flight */
#define ECOS_EP_COMPLETE_DATA  0x0C
#define ECOS_EP_BUFFER         0x10
#define ECOS_EP_BUFFER_SIZE    0x14
#define ECOS_EP_HALTED         0x18
#define ECOS_EP_REGS           0x1C  /* DIEPCTLn / DOEPCTLn pointer */
#define ECOS_EP_STATE          0x20  /* uint8: 0/2/3 (see EP_STATE_* in dice_usb_regs.h) */
#define ECOS_EP_FLAGS          0x24  /* ep_num | (attr << 8) */
#define ECOS_EP_DMA_BUF_COPY   0x28
#define ECOS_EP_DMA_SIZE_COPY  0x2C
/* +0x38 also holds a uint16 max_pkt per the eCos struct comment, but
 * usb_hw_ep_dma_start_rx and the EP3 burst path empirically read the
 * value at +0x3C. Use that. */
#define ECOS_EP_MAX_PKT        0x3C  /* uint16 */

/* ── TCAT USB Connection Object ─────────────────────────────────────
 * At usb_midi_conn (0x2A230).
 *   +0x00   void **vtable
 *              [2] = usb_hw_ep_read_start (programs controller QH/dTD)
 *              [7] = get_rx_endpoint (returns EP handle by type)
 *              [8] = get_tx_endpoint
 *   +0x2BC  uint8_t usb_state  (>= 5 means addressed/configured)
 * ─────────────────────────────────────────────────────────────────── */

#define USB_CONN_STATE_OFFSET      0x2BC
#define USB_STATE_ADDRESSED        5
#define USB_STATE_CONFIGURED       6

static inline uint8_t usb_conn_get_state(void *conn) {
    return *(volatile uint8_t *)((uint8_t *)conn + USB_CONN_STATE_OFFSET);
}

/* ── TCAT MIDI Streaming State ──────────────────────────────────────
 * At midi_streaming_state (0x29CA4).
 *   +0x04   uint32_t field_04        (used in notify check)
 *   +0x08   uint32_t field_08        (used in notify check)
 *   +0x1C   uint8_t  speed_mode      (0x02 = high-speed)
 *   +0x20   uint8_t[7] ep_descriptor (built by usb_endpoint_descriptor_build)
 *   +0x28   void    *cable_config_ptr
 *              cable_config[0..1] = first_cable_count (short)
 *              cable_config[2]   = rx_cable_count (byte)
 *   +0x460  void    *dcp_tx_endpoint
 *   +0x474  void   **notify_obj_ptr  (vtable for streaming notifications)
 * ─────────────────────────────────────────────────────────────────── */

#define MIDI_STREAM_EP_DESC       0x20
#define MIDI_STREAM_CABLE_CFG     0x28
#define MIDI_STREAM_DCP_TX_EP     0x460
#define MIDI_STREAM_NOTIFY_OBJ    0x474

/* ── USB-MIDI Packet Buffer State ───────────────────────────────────
 * At midi_pkt_buf (0x30D64).
 *   +0x000  uint8_t[0x200] pkt_buf     (512-byte USB-MIDI packet buffer)
 *   +0x200  uint32_t total_packets      (set by completion callback)
 *   +0x204  uint32_t current_index      (processing index)
 *   +0x208  void    *rx_ep_handle       (0 = not armed, non-zero = armed)
 *   +0x20C  uint8_t  cable_count
 *   +0x20D  uint8_t  overflow_cable     (0xFF = none)
 * ─────────────────────────────────────────────────────────────────── */

#define MIDI_PKT_BUF_SIZE         0x200
#define MIDI_PKT_TOTAL            0x200
#define MIDI_PKT_INDEX            0x204
#define MIDI_PKT_RX_EP            0x208
#define MIDI_PKT_CABLE_CNT        0x20C
#define MIDI_PKT_OVERFLOW         0x20D

/* ── USB EP type codes for usb_endpoint_descriptor_build() ────────── */

#define USB_EP_TYPE_INT_IN     0  /* EP 0x81, interrupt, 6 bytes, interval 8 */
#define USB_EP_TYPE_DCP_TX     1  /* EP 0x84, special, 4 bytes, interval 4 */
#define USB_EP_TYPE_MIDI_TX    2  /* EP 0x82, bulk IN */
#define USB_EP_TYPE_MIDI_RX    3  /* EP 0x03, bulk OUT */
#define USB_EP_TYPE_INT_IN2    4  /* EP 0x85, interrupt, 8 bytes, interval 8 */

/* ── USB Function Declarations ─────────────────────────────────────── */

/* Endpoint handle lookup via connection vtable dispatch */
extern void *usb_iface_get_tx_endpoint(void *conn, int type);
extern void *usb_iface_get_rx_endpoint(void *conn, int type);

/* Build 7-byte USB endpoint descriptor.
 * desc_area: streaming_state + 0x20 (descriptor metadata)
 * ep_type: USB_EP_TYPE_* constant
 * out: 7-byte output buffer for the descriptor
 * Returns non-zero if EP is available. */
extern int usb_endpoint_descriptor_build(void *desc_area, int ep_type, uint8_t *out);

/* Start endpoint if USB state >= ADDRESSED.
 * Dispatches to usb_hw_ep_read_start via vtable[2].
 * WARNING: R1 (descriptor ptr) must be set by caller — thin pass-through!
 * Returns 0 on success, -1 if not ready, -0x16 if conn is NULL. */
extern int usb_endpoint_start_if_configured(void *conn);

/* Direct QH initialization — properly takes both conn and descriptor.
 * conn: USB connection object (0x2A230)
 * ep_desc: 7-byte endpoint descriptor (built by usb_endpoint_descriptor_build) */
extern int usb_hw_ep_read_start(void *conn, void *ep_desc);

/* Submit USB transfer: programs ChipIdea controller QH and primes dTD.
 * Callback signature: void (*cb)(void *ctx, int byte_count) */
extern void usb_endpoint_submit_transfer(void *ep, void *buf, int size,
                                          void *callback, int ctx);

/* Start MIDI TX endpoint for device-to-host bulk IN.
 * cable_flag: non-zero if cables present. */
extern int usb_midi_tx_endpoint_start(void *tx_ep, int cable_flag);

/* Arm MIDI RX endpoint for host-to-device bulk OUT reception.
 * Stores ep_handle in pkt_state, starts DMA.
 * Re-arming is automatic via usb_midi_rx_complete. */
extern void usb_midi_rx_start(void *ep_handle, int num_cables);

/* DMA completion callback for bulk OUT — parses USB-MIDI events, re-arms EP. */
extern void usb_midi_rx_complete(void *ctx, int byte_count);

/* Full MIDI streaming init — proper sequence that doesn't break audio.
 * WARNING: reads R4 as streaming_state pointer (Ghidra: unaff_r4).
 * Must set R4 = midi_streaming_state before calling. */
extern void usb_midi_streaming_start(void);

/* MIDI streaming reconfigure — called on USB alt-setting change. */
extern void usb_midi_streaming_reconfigure(void *desc_ptr);

/* DCP streaming gate: state=1 enables categories 1-3/5-8, state=0 disables. */
extern void dcp_streaming_state_set(int state);

/* ════════════════════════════════════════════════════════════════════
 * MIDI Subsystem
 *
 * MIDI parser context at midi_parser_ctx (0x29764), ~0x34 bytes.
 * Parser calls channel_msg_cb at offset +0x28 for each channel message.
 * ════════════════════════════════════════════════════════════════════ */

#define MIDI_PARSER_CH_CB_OFFSET    0x28
#define MIDI_PARSER_SYSEX_CB_OFFSET 0x2C

/* MIDI TX — write raw bytes to USB MIDI EP IN (device → host) */
extern int midi_tx_write(const void *data, int len);

/* Canonical "set level + apply + LED + BLE notify" API. Use these instead of
 * writing mixer_state by hand — they're what the BLE/knob dispatchers use,
 * so on-device LEDs and the BT phone app stay in sync automatically. */
extern void master_level_set_notify(int bus, int level);
extern void compressor_level_set_notify(int bus, int level);
extern void channel_level_set_notify(int input, int bus, int level);   /* input first! */

/* Lower-level helpers — kept for state dumps and direct LED control. */
extern void ble_send_full_state_dump(void);
extern void ble_write_tuple(uint8_t param_id, uint8_t sub_param, uint8_t value);
extern void led_full_refresh(void);

/* Original channel message callback — chain to this for unhandled messages.
 * Parser passes (ctx, ctx, len) per ARM EABI: r0=ctx, r1=ctx, r2=len. The
 * orig handler asserts `len <= 3`, so we must preserve r2 through the chain. */
extern void midi_channel_msg_cb_orig(void *parser_ctx, void *parser_ctx2, int len);

/* MIDI engine init sequence (called from midi_engine_thread) */
extern void midi_usb_endpoints_init(void);   /* mixer_hw_dsr callback + routing */
extern int  midi_usb_endpoints_setup(void);  /* find + link EP handlers */
extern void midi_engine_start(void);         /* clear FSS flag, set running */

/* MIDI RX polling — reads ring buffer, feeds parser */
extern void midi_rx_poll(void);

/* ════════════════════════════════════════════════════════════════════
 * DCP (DICE Control Protocol)
 * ════════════════════════════════════════════════════════════════════ */

extern void dcp_register_handler(void *node);
extern void dcp_send_response(int retcode, void *body, int len);

/* ════════════════════════════════════════════════════════════════════
 * SPI Flash (SST25VF016B)
 * ════════════════════════════════════════════════════════════════════ */

extern int  sst25xx_sector_erase(void *ctx, uint32_t addr);
extern int  sst25xx_aai_write(void *ctx, uint32_t addr,
                               const void *buf, uint32_t len);
extern uint8_t sst25xx_driver_ctx[];

/* ════════════════════════════════════════════════════════════════════
 * Mixer Hardware
 *
 * mixer_state layout: inputs[4 buses × 7 inputs] + compressor[4] + master[4]
 * Level range: 0-31 (firmware internal), MIDI maps 0-127 >> 2.
 * ════════════════════════════════════════════════════════════════════ */

extern void channel_level_apply(int bus, int input, int level);
extern void master_level_apply(int bus, int level);
extern void compressor_level_apply(int bus, int level);
extern void led_mute_update(int input, int on_off);
extern uint8_t mixer_state[];

/* ════════════════════════════════════════════════════════════════════
 * UART Debug Output
 * ════════════════════════════════════════════════════════════════════ */

extern void uart_putc(char c);
extern void uart_print_string(const char *s);
extern int  uart_printf(const char *fmt, ...);

/* ════════════════════════════════════════════════════════════════════
 * Utility / Platform
 * ════════════════════════════════════════════════════════════════════ */

extern void *memcpy(void *dst, const void *src, unsigned int len);
extern void rtos_app_init(void);
extern void usb_audio_rx_set_sample_rate(uint32_t sample_rate);

/* ── Global Data (resolved via firmware_symbols.ld) ────────────────── */

extern uint8_t usb_midi_conn[];          /* 0x2A230 — USB connection object */
extern uint8_t midi_streaming_state[];   /* 0x29CA4 — MIDI streaming state */
extern uint8_t midi_pkt_buf[];           /* 0x30D64 — USB-MIDI packet buffer */
extern uint8_t midi_parser_ctx[];        /* 0x29764 — MIDI parser context */

/* ── ARM926EJ-S Cache Control ──────────────────────────────────────
 * Reference: ecos/packages/hal/arm/arm9/var/current/include/hal_cache.h
 * I-cache: 16KB, 32-byte lines, 4-way associative
 * D-cache: 16KB, 32-byte lines, 4-way associative
 * ────────────────────────────────────────────────────────────────── */

static inline void icache_invalidate(void) {
    __asm__ volatile("mcr p15, 0, %0, c7, c5, 0" :: "r"(0) : "memory");
}

static inline void dcache_invalidate(void) {
    __asm__ volatile("mcr p15, 0, %0, c7, c6, 0" :: "r"(0) : "memory");
}

#endif /* DICE_PLATFORM_H */
