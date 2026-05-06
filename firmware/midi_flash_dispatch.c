/*
 * midi_flash_dispatch.c — shared eCos-context flash-op core.
 *
 * Build for two targets:
 *   1. JTAG-injected DCP handler:
 *        compiled alongside firmware/sram_flash_handler.c via the JTAG
 *        build (see top of sram_flash_handler.c). Both .o files link
 *        with firmware/firmware_symbols.ld.
 *   2. Patched-SPI MIDI SysEx hook:
 *        compiled alongside firmware/patch/handlers.c +
 *        firmware/patch/midi_sysex_filter.c via firmware/patch/Makefile.
 *
 * All sst25xx_*, memcpy, and bootloader CRC calls resolve to known
 * primary-firmware addresses via firmware_symbols.ld. No standalone
 * dependencies; runs entirely in stock-fw eCos context.
 */

#include <stdint.h>

#include "midi_flash_dispatch.h"

/* ── Stock-firmware symbols (resolved via firmware_symbols.ld) ───────── */

extern int  sst25xx_sector_erase(void *ctx, uint32_t addr);
extern int  sst25xx_aai_write(void *ctx, uint32_t addr,
                              const void *buf, uint32_t len);
extern int  sst25xx_fast_read(void *ctx, uint32_t addr,
                              void *buf, int len);
extern void *memcpy(void *dst, const void *src, unsigned int len);
extern char  sst25xx_driver_ctx;       /* address-of via &sst25xx_driver_ctx */

#define DRIVER_CTX  ((void *)&sst25xx_driver_ctx)

/* HASH_RANGE / ROM CRC lives in patch/midi_sysex_filter.c — kept out of
 * the shared core to fit the JTAG-injected handler region (1 KB) without
 * spillover. The DCP recovery path uses host-side READ-based diff. */

/* ── Helpers ─────────────────────────────────────────────────────────── */

/* All u32/u16 stores are 4-byte aligned in our use (resp_buf is the DCP
 * body buffer at dcp_state+0x14, word-aligned). Plain *(uint32_t*)= is
 * safe and emits a single STR — much smaller than per-byte stores. */
#define PUT_U32(p, v) (*(uint32_t *)(p) = (uint32_t)(v))
#define PUT_U16(p, v) (*(uint16_t *)(p) = (uint16_t)(v))

static int golden_overlap(uint32_t addr, uint32_t len) {
    return (addr < FLASH_GOLDEN_END) && ((addr + len) > FLASH_GOLDEN_START);
}

static int reply_status(uint32_t status, uint8_t *resp_buf, uint32_t *resp_len) {
    PUT_U32(resp_buf, status);
    *resp_len = 4;
    return (int)status;
}

/* ── flash_op_info ───────────────────────────────────────────────────── */

int flash_op_info(uint8_t *resp_buf, uint32_t *resp_len) {
    PUT_U32(resp_buf +  0, FLASH_JEDEC_ID);
    PUT_U32(resp_buf +  4, FLASH_SECTOR_SIZE_B);
    PUT_U32(resp_buf +  8, FLASH_TOTAL_SIZE_B);
    PUT_U32(resp_buf + 12, FLASH_DISPATCH_VERSION);
    *resp_len = 16;
    return 0;
}

/* ── flash_op_read ───────────────────────────────────────────────────── */

int flash_op_read(uint32_t addr, uint32_t len,
                  uint8_t *resp_buf, uint32_t *resp_len)
{
    if (len > 4096) {
        return reply_status(ERR_BAD_BODY, resp_buf, resp_len);
    }
    /* Use stock's sst25xx_fast_read (opcode 0x0B + dummy byte) — addr
     * is unambiguously a SPI flash address. The earlier `memcpy from
     * (void*)addr` pattern was wrong: CPU 0x40000 = SRAM, not flash;
     * the XIP-mapped window is only the small bootloader region near
     * 0x4F000. */
    int rc = sst25xx_fast_read(DRIVER_CTX, addr, resp_buf, (int)len);
    if (rc != 0) {
        return reply_status(ERR_BAD_BODY, resp_buf, resp_len);
    }
    *resp_len = len;
    return 0;
}

int flash_op_mem_read(uint32_t addr, uint32_t len,
                      uint8_t *resp_buf, uint32_t *resp_len)
{
    if (len > 1024) {
        return reply_status(ERR_BAD_BODY, resp_buf, resp_len);
    }
    /* Raw CPU-memory read — used to drain HASH_RANGE per-sector CRCs
     * from the SRAM sector_buf (typically 0x2B000+). NO bounds check
     * on the address: trust the host to point at safe memory. */
    memcpy(resp_buf, (void *)(uintptr_t)addr, len);
    *resp_len = len;
    return 0;
}

/* ── flash_op_erase_sector ───────────────────────────────────────────── */

int flash_op_erase_sector(uint32_t addr, uint32_t count,
                          uint8_t *resp_buf, uint32_t *resp_len)
{
    uint32_t total_len = count * FLASH_SECTOR_SIZE_B;
    if (golden_overlap(addr, total_len)) {
        return reply_status(ERR_GOLDEN_PROTECT, resp_buf, resp_len);
    }
    for (uint32_t i = 0; i < count; i++) {
        int rc = sst25xx_sector_erase(DRIVER_CTX, addr + i * FLASH_SECTOR_SIZE_B);
        if (rc != 0) {
            return reply_status(ERR_ERASE_FAIL, resp_buf, resp_len);
        }
    }
    return reply_status(0, resp_buf, resp_len);
}

/* ── flash_op_aai_write ──────────────────────────────────────────────── */

int flash_op_aai_write(uint32_t addr, const uint8_t *data, uint32_t len,
                       uint8_t *resp_buf, uint32_t *resp_len)
{
    if (len == 0) {
        return reply_status(ERR_BAD_BODY, resp_buf, resp_len);
    }
    if (golden_overlap(addr, len)) {
        return reply_status(ERR_GOLDEN_PROTECT, resp_buf, resp_len);
    }
    int rc = sst25xx_aai_write(DRIVER_CTX, addr, data, len);
    if (rc != 0) {
        return reply_status(ERR_AAI_FAIL, resp_buf, resp_len);
    }
    return reply_status(0, resp_buf, resp_len);
}
