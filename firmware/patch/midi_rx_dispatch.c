/*
 * midi_rx_dispatch.c — replaces stock's BL midi_parser_process_bytes
 * inside midi_rx_poll @ 0x2AB8.
 *
 * Why not hook midi_sysex_cb (0x2A34) instead?  The stock MIDI parser
 * accumulates SysEx bodies into a 23-byte buffer at parser_ctx+0x01..+0x17.
 * Frames longer than that are silently chunked (or dropped, depending on
 * the F7 timing) before the cb sees them — capping our request payloads
 * at 11 raw bytes (24-byte wire frame). Catastrophic for WRITE_AAI.
 *
 * Hook site (per Ghidra decompile of midi_rx_poll @ 0x2AB8):
 *   0x2AD0  ldr  r0,[0x2AE4]   ; ctx pointer literal
 *   0x2AD4  mov  r1, sp        ; 16-byte stack buffer holding USB MIDI bytes
 *   0x2AD8  bl   0x3ED0        ; midi_parser_process_bytes(ctx, buf, len)
 *                                ─── replace with bl midi_rx_dispatch ───
 *   0x2ADC  add  sp, sp, #0x14
 *
 * After replacement we receive (ctx, buf, len) directly. We demultiplex
 * the byte stream:
 *   - Recognize frames starting `F0 7D ...` and accumulate them in our
 *     own private buffer (≥ ringbuf size of stock parser).
 *   - On F7, dispatch to midi_sysex_handle() (defined in midi_sysex_filter.c).
 *   - Forward all other bytes to the displaced midi_parser_process_bytes
 *     so stock CC/note/Roland-SysEx handling is preserved.
 *
 * State persists across batches because USB MIDI bytes arrive in 16-byte
 * chunks but a single SysEx frame spans many chunks.
 *
 * Runs in eCos midi_engine_thread context — same constraints as
 * midi_sysex_filter.c (may block on AAI/erase, may call midi_tx_write).
 */

#include <stdint.h>

extern void midi_parser_process_bytes(void *ctx, const uint8_t *buf, uint32_t len);
extern void midi_sysex_handle(const uint8_t *frame, uint32_t len);

/* Private SysEx accumulator. Lives in the upper half of the
 * 0x2B000-0x2CFFF DEADBEEF SRAM zone so we don't compete with the
 * patch-zone BSS budget (a 1 KB+ static array overflows the 9216 B
 * PATCH region). The lower half (0x2B000-0x2BFFF) is shared scratch
 * already used by midi_sysex_filter.c for UNPACK/PACK/SECTOR_CRC/DATA
 * slots — the upper half is free. */
#define OUR_SYSEX_BUFSIZE 1024
#define OUR_SYSEX_BUF ((uint8_t *)0x2C000u)

/* Worst-case WRITE_AAI = 192 raw + 4 addr + 7-of-8 pack overhead =
 * 224 packed + 6 envelope = 230 wire bytes — well under 1 KB.
 *
 * The patch zone's .bss is NOT zeroed at boot (stock's BSS_clear stops
 * at 0x325F8); statics here would carry random/stale SRAM values into
 * the first invocation. boot_init in handlers.c calls
 * midi_rx_dispatch_init() to zero our state explicitly. */
static uint16_t our_sysex_pos;

enum { ST_NORMAL = 0, ST_F0_PENDING, ST_OUR_SYSEX, ST_OVERFLOW };
static uint8_t stream_state;

void midi_rx_dispatch_init(void)
{
    our_sysex_pos = 0;
    stream_state = ST_NORMAL;
}

void midi_rx_dispatch(void *ctx, const uint8_t *buf, uint32_t len)
{
    /* Worst case: every byte goes to stock (state stays NORMAL).
     * pass_buf is sized to match stock's 16-byte stack buffer +1 (the
     * single F0 we may have buffered from the previous batch). */
    uint8_t  pass_buf[20];
    uint32_t pass_len = 0;

    for (uint32_t i = 0; i < len; i++) {
        uint8_t b = buf[i];

        switch (stream_state) {
        case ST_NORMAL:
            if (b == 0xF0u) {
                /* Defer F0: we don't know yet whether this is ours. */
                stream_state = ST_F0_PENDING;
            } else {
                if (pass_len < sizeof(pass_buf)) pass_buf[pass_len++] = b;
            }
            break;

        case ST_F0_PENDING:
            if (b == 0x7Du) {
                /* Ours. Begin accumulation. */
                our_sysex_pos = 0;
                OUR_SYSEX_BUF[our_sysex_pos++] = 0xF0u;
                OUR_SYSEX_BUF[our_sysex_pos++] = 0x7Du;
                stream_state = ST_OUR_SYSEX;
            } else {
                /* Not ours — flush buffered F0 + this byte to stock. */
                if (pass_len + 2 <= sizeof(pass_buf)) {
                    pass_buf[pass_len++] = 0xF0u;
                    pass_buf[pass_len++] = b;
                }
                stream_state = ST_NORMAL;
            }
            break;

        case ST_OUR_SYSEX:
            if (our_sysex_pos < OUR_SYSEX_BUFSIZE) {
                OUR_SYSEX_BUF[our_sysex_pos++] = b;
            } else {
                /* Frame overruns our buffer — give up on it. Stay in
                 * OVERFLOW until F7, then resume. */
                stream_state = ST_OVERFLOW;
            }
            if (b == 0xF7u) {
                midi_sysex_handle(OUR_SYSEX_BUF, our_sysex_pos);
                stream_state = ST_NORMAL;
            }
            break;

        case ST_OVERFLOW:
            if (b == 0xF7u) {
                /* Frame ended — drop and resume. Host will time out. */
                stream_state = ST_NORMAL;
            }
            break;
        }
    }

    if (pass_len > 0) {
        midi_parser_process_bytes(ctx, pass_buf, pass_len);
    }
}
