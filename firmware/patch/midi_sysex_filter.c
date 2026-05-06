/*
 * midi_sysex_filter.c — SysEx-MIDI flash protocol handler.
 *
 * Called from midi_rx_dispatch.c (in turn invoked via the BL-replace
 * hook at 0x2AD8 in midi_rx_poll). The dispatcher demultiplexes the
 * USB MIDI byte stream and only invokes us with a complete F0 7D ... F7
 * frame already captured into a private buffer (no 23-byte stock-parser
 * limit). Calling convention:
 *
 *     void midi_sysex_handle(const uint8_t *frame, uint32_t len);
 *
 *   frame[0]      = 0xF0 (SysEx start)
 *   frame[1]      = 0x7D (manufacturer ID — guaranteed by dispatcher)
 *   frame[len-1]  = 0xF7 (SysEx end)
 *
 * Parses per firmware/midi_flash_protocol.md, dispatches via the shared
 * core in midi_flash_dispatch.c, packs the reply, sends via midi_tx_write
 * @ 0x1114. No return value — replies are sent inline.
 *
 * Runs in eCos thread context (midi_engine_thread @ 0x5010 polling
 * loop). Free to mutex-lock, call sst25xx_*, etc.
 */

#include <stdint.h>

#include "../midi_flash_dispatch.h"

/* ── Stock-firmware symbols (resolved via firmware_symbols.ld) ───────── */

extern int  midi_tx_write(const void *data, int len);
extern int  sst25xx_fast_read(void *ctx, uint32_t addr, void *buf, int len);
extern char sst25xx_driver_ctx;
extern void *memcpy(void *dst, const void *src, unsigned int len);

#define SST_DRIVER_CTX  ((void *)&sst25xx_driver_ctx)

/* ROM CRC32 step routine (TCAT bootloader's helper at 0x20000DA4).
 * Convention: poly 0xEDB88320 reflected, init=0, no final XOR.
 * Host equivalence: zlib.crc32(buf, ~0) ^ ~0. */
typedef uint32_t (*rom_crc_step_t)(uint32_t crc, const void *buf, uint32_t len);
#define ROM_CRC_STEP ((rom_crc_step_t)0x20000DA4u)

/* ── Protocol constants (mirror firmware/midi_flash_protocol.md) ────── */

#define MFR_ID            0x7Du
#define OP_INFO           0x01u
#define OP_READ           0x02u
#define OP_HASH_RANGE     0x03u
#define OP_ERASE_SECTOR   0x04u
#define OP_WRITE_AAI      0x05u
#define OP_REBOOT         0x06u
#define OP_MEM_READ       0x07u

#define REPLY_OK_BIT      0x40u
#define REPLY_ERR_BIT     0x80u

/* Shared SRAM scratch buffers — same 0x2B000 slot used by v2 driver's
 * V2_DATA_BUF and the JTAG-implant handler's data path. Safe because
 * both run in eCos thread context (serialized) and never overlap. */
#define WRITE_BUF        ((uint8_t *)0x2B000u)
#define WRITE_BUF_SIZE   0x1000u   /* 4 KB — one sector */

/* TX/RX scratch for unpack/pack (small enough to live in patch BSS, but
 * we put it in WRITE_BUF too — first 512 B for unpacked request, next
 * 512 B for packed reply, rest free for HASH_RANGE per-sector CRC array). */
#define UNPACK_BUF       (WRITE_BUF + 0x000)   /* 0x2B000 — request payload (raw) */
#define PACK_BUF         (WRITE_BUF + 0x200)   /* 0x2B200 — reply frame (packed) */
#define SECTOR_CRC_BUF   (WRITE_BUF + 0x400)   /* 0x2B400 — HASH per-sector CRCs */
#define DATA_BUF         (WRITE_BUF + 0x600)   /* 0x2B600 — WRITE_AAI raw data slot */

/* ── CRC8 ATM/CCITT (poly 0x07, init 0x00, no XOR-out) ───────────────── */

static uint8_t crc8(const uint8_t *buf, uint32_t len) {
    uint8_t crc = 0;
    for (uint32_t i = 0; i < len; i++) {
        crc ^= buf[i];
        for (int b = 0; b < 8; b++) {
            crc = (crc & 0x80u) ? (uint8_t)((crc << 1) ^ 0x07u)
                                 : (uint8_t)(crc << 1);
        }
    }
    return crc;
}

/* ── Roland 7-of-8 pack/unpack ───────────────────────────────────────── */
/*
 * Each group of 7 raw bytes → 8 transmitted bytes:
 *   sent[0]      = M (bit i = bit 7 of raw[i])
 *   sent[1..7]   = raw[0..6] & 0x7F
 * Final group may be short.
 */

/* Unpacks `n` packed bytes into `out`, returns raw byte count or -1. */
static int32_t unpack78(const uint8_t *in, uint32_t n, uint8_t *out, uint32_t out_max) {
    uint32_t i = 0, o = 0;
    while (i < n) {
        uint8_t m = in[i++];
        for (uint32_t b = 0; b < 7 && i < n; b++) {
            if (o >= out_max) return -1;
            uint8_t v = in[i++] & 0x7Fu;
            if (m & (uint8_t)(1u << b)) v |= 0x80u;
            out[o++] = v;
        }
    }
    return (int32_t)o;
}

/* Packs `n` raw bytes into `out`, returns packed byte count. */
static uint32_t pack78(const uint8_t *in, uint32_t n, uint8_t *out) {
    uint32_t i = 0, o = 0;
    while (i < n) {
        uint8_t m = 0;
        uint32_t group = (n - i < 7) ? (n - i) : 7;
        uint32_t mask_idx = o++;
        for (uint32_t b = 0; b < group; b++) {
            uint8_t v = in[i + b];
            if (v & 0x80u) m |= (uint8_t)(1u << b);
            out[o++] = v & 0x7Fu;
        }
        out[mask_idx] = m;
        i += group;
    }
    return o;
}

/* ── HASH_RANGE (MIDI-only — not in shared dispatch) ─────────────────── */

static int hash_range(uint32_t addr, uint32_t len,
                      uint32_t sectorbuf, uint32_t flags,
                      uint8_t *resp_buf, uint32_t *resp_len)
{
    uint32_t whole = 0;
    uint32_t sec = 0;
    uint32_t sec_off = 0;
    uint32_t sec_idx = 0;
    uint32_t *sec_dst = (flags & HASH_FLAG_PER_SECTOR)
                            ? (uint32_t *)(uintptr_t)sectorbuf
                            : (uint32_t *)0;

    /* Read through stock sst25xx_fast_read into a small scratch slab
     * inside DATA_BUF, then CRC. The XIP window is only the small
     * bootloader region near 0x4F000 — we can't blindly hash via XIP
     * for arbitrary SPI addresses. */
    uint8_t *slab = DATA_BUF;       /* 0x2B600, ~2.5 KB headroom */
    const uint32_t SLAB = 1024u;    /* 1 KB chunk */
    uint32_t off = 0;
    while (off < len) {
        uint32_t rem  = FLASH_SECTOR_SIZE_B - sec_off;
        uint32_t left = len - off;
        uint32_t chunk = (left < rem) ? left : rem;
        if (chunk > SLAB) chunk = SLAB;
        if (sst25xx_fast_read(SST_DRIVER_CTX, addr + off, slab, (int)chunk) != 0) {
            return -1;
        }
        whole = ROM_CRC_STEP(whole, slab, chunk);
        if (sec_dst) sec = ROM_CRC_STEP(sec, slab, chunk);
        sec_off += chunk;
        off     += chunk;
        if (sec_off >= FLASH_SECTOR_SIZE_B) {
            if (sec_dst) sec_dst[sec_idx] = sec;
            sec_idx++;
            sec = 0; sec_off = 0;
        }
    }
    if (sec_off > 0) {
        if (sec_dst) sec_dst[sec_idx] = sec;
        sec_idx++;
    }

    *(uint32_t *)(resp_buf + 0) = whole;
    *(uint16_t *)(resp_buf + 4) = (uint16_t)sec_idx;
    *resp_len = 6;
    return 0;
}

/* ── Reply framing ───────────────────────────────────────────────────── */
/*
 * Builds:  F0 7D <op> <seq> <packed_resp> <crc8> F7
 * `op_with_flag` = original_op | 0x40 (success) or | 0x80 (error)
 * `resp` = raw response payload, `resp_len` raw bytes.
 */
static void send_reply(uint8_t op_with_flag, uint8_t seq,
                       const uint8_t *resp, uint32_t resp_len)
{
    uint8_t *out = PACK_BUF;
    uint32_t i = 0;

    out[i++] = 0xF0u;
    out[i++] = MFR_ID;
    out[i++] = op_with_flag;
    out[i++] = seq;

    /* Pack the response payload. */
    uint32_t packed = pack78(resp, resp_len, &out[i]);
    /* Append CRC8 over op||seq||packed_payload — strip MIDI bit. */
    uint8_t crc = crc8(&out[2], 2 + packed);
    out[i + packed] = (uint8_t)(crc & 0x7Fu);
    i += packed + 1;

    out[i++] = 0xF7u;
    (void)midi_tx_write(out, (int)i);
}

static void send_err(uint8_t op, uint8_t seq, uint32_t err) {
    uint8_t buf[4];
    *(uint32_t *)buf = err;
    send_reply((uint8_t)(op | REPLY_ERR_BIT), seq, buf, 4);
}

/* ── Handler entry point (called by midi_rx_dispatch) ────────────────── */

void midi_sysex_handle(const uint8_t *bytes, uint32_t len)
{
    /* Dispatcher already validated F0/0x7D/F7 envelope, but minimum
     * frame size still depends on op+seq+crc presence. */
    if (len < 6) return;

    uint8_t op  = bytes[2];
    uint8_t seq = bytes[3];

    /* CRC8 over op||seq||packed_payload (excludes F0/MFR/CRC/F7). */
    uint32_t inner_len = len - 6;            /* packed payload size */
    uint8_t  exp_crc   = bytes[len - 2] & 0x7Fu;
    uint8_t  got_crc   = crc8(&bytes[2], 2 + inner_len) & 0x7Fu;
    if (got_crc != exp_crc) {
        send_err(op, seq, ERR_BAD_CRC);
        return;
    }

    /* Unpack request payload into UNPACK_BUF. */
    int32_t raw_len = unpack78(&bytes[4], inner_len, UNPACK_BUF, 0x200u);
    if (raw_len < 0) {
        send_err(op, seq, ERR_TRUNCATED);
        return;
    }
    const uint8_t *req  = UNPACK_BUF;
    uint32_t      rl    = (uint32_t)raw_len;
    uint8_t       resp[16];
    uint32_t      resp_len = 0;
    int           rc       = 0;

    switch (op) {
    case OP_INFO:
        rc = flash_op_info(resp, &resp_len);
        break;

    case OP_READ:
    case OP_MEM_READ: {
        if (rl < 6) { send_err(op, seq, ERR_BAD_BODY); return; }
        uint32_t addr = *(uint32_t *)req;
        uint32_t rlen = (uint32_t)(*(uint16_t *)(req + 4));
        if (rlen > 192) rlen = 192;          /* one frame per chunk; midi_tx_write
                                              * ringbuf empirically tops out near
                                              * 256 packed bytes (= 192 raw + 8/7
                                              * pack overhead = 220 packed + 6
                                              * envelope = ~226 wire) */
        /* Resp goes into UNPACK_BUF (request already consumed). The
         * local resp[16] is too small for READ; reuse the 512 B
         * UNPACK_BUF slot. */
        if (op == OP_READ) {
            rc = flash_op_read(addr, rlen, UNPACK_BUF, &resp_len);
        } else {
            rc = flash_op_mem_read(addr, rlen, UNPACK_BUF, &resp_len);
        }
        if (rc == 0) {
            send_reply((uint8_t)(op | REPLY_OK_BIT), seq, UNPACK_BUF, resp_len);
        } else {
            send_err(op, seq, *(uint32_t *)UNPACK_BUF);
        }
        return;
    }

    case OP_HASH_RANGE: {
        if (rl < 13) { send_err(op, seq, ERR_BAD_BODY); return; }
        uint32_t addr      = *(uint32_t *)req;
        uint32_t len4      = *(uint32_t *)(req + 4);
        uint32_t sectorbuf = *(uint32_t *)(req + 8);
        uint32_t flags     = req[12];
        if (sectorbuf == 0) sectorbuf = (uint32_t)(uintptr_t)SECTOR_CRC_BUF;
        rc = hash_range(addr, len4, sectorbuf, flags, resp, &resp_len);
        break;
    }

    case OP_ERASE_SECTOR: {
        if (rl < 6) { send_err(op, seq, ERR_BAD_BODY); return; }
        uint32_t addr  = *(uint32_t *)req;
        uint32_t count = (uint32_t)(*(uint16_t *)(req + 4));
        rc = flash_op_erase_sector(addr, count, resp, &resp_len);
        break;
    }

    case OP_WRITE_AAI: {
        if (rl < 5) { send_err(op, seq, ERR_BAD_BODY); return; }
        uint32_t addr  = *(uint32_t *)req;
        uint32_t dlen  = rl - 4;
        /* Stage data out of UNPACK_BUF (which is also the resp buf for
         * single-frame ops) into DATA_BUF so AAI can use it cleanly. */
        memcpy(DATA_BUF, req + 4, dlen);
        rc = flash_op_aai_write(addr, DATA_BUF, dlen, resp, &resp_len);
        break;
    }

    case OP_REBOOT:
        /* Reply BEFORE the actual reset takes effect so host sees ack. */
        *(uint32_t *)resp = 0;
        resp_len = 4;
        send_reply((uint8_t)(op | REPLY_OK_BIT), seq, resp, resp_len);
        return;

    default:
        send_err(op, seq, ERR_UNKNOWN_OPCODE);
        return;
    }

    if (rc == 0) {
        send_reply((uint8_t)(op | REPLY_OK_BIT), seq, resp, resp_len);
    } else {
        /* The shared core already wrote an ERR_* code into resp[0..3]. */
        send_reply((uint8_t)(op | REPLY_ERR_BIT), seq, resp, 4);
    }
}
