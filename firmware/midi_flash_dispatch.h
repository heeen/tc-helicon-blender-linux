/*
 * midi_flash_dispatch.h — shared eCos-context flash-op core
 *
 * Used by BOTH the DCP-0x81F handler (sram_flash_handler.c, JTAG-injected)
 * and the MIDI SysEx filter hook (patch/midi_sysex_filter.c, baked into
 * the patched SPI image). Single source of truth for sst25xx_* calls,
 * golden-copy guard, and HASH_RANGE.
 *
 * All entry points run in eCos thread context — they MAY call
 * cyg_mutex_lock, may block on cyg_flag_timed_wait via the stock
 * spi_engine. They MUST NOT be called from interrupt context.
 *
 * Convention: each `flash_op_*` writes its response into `resp_buf`,
 * sets `*resp_len` to the number of bytes written, and returns 0 on
 * success or one of the ERR_* codes on failure. On error, resp_len
 * is also set to 4 and resp_buf[0..3] contains the error code (LE).
 */

#ifndef MIDI_FLASH_DISPATCH_H
#define MIDI_FLASH_DISPATCH_H

#include <stdint.h>

/* ── Shared constants (mirror sram_flash_handler.c / patch/handlers.c) ── */

#define FLASH_GOLDEN_START   0x10000u
#define FLASH_GOLDEN_END     0x40000u
#define FLASH_SECTOR_SIZE_B  0x1000u
#define FLASH_JEDEC_ID       0x00BF2541u
#define FLASH_TOTAL_SIZE_B   0x00200000u

/* Error codes (kept in lock-step with the existing handlers so
 * cross-path error parity is preserved). */
#define ERR_GOLDEN_PROTECT   0xDEAD0001u
#define ERR_BAD_BODY         0xDEAD0002u
#define ERR_ERASE_FAIL       0xDEAD0003u
#define ERR_AAI_FAIL         0xDEAD0004u
#define ERR_BAD_CRC          0xDEAD0005u
#define ERR_TRUNCATED        0xDEAD0006u
#define ERR_UNKNOWN_OPCODE   0xDEAD00FFu

/* Build-tag echoed in INFO; bumped per shared-core revision.
 * v1 (MFD1): midi_sysex_cb filter hook; 24-byte parser limit; WRITE_AAI ≤ 11 raw.
 * v2 (MFD2): midi_rx_dispatch BL-replace hook; 1 KB private accumulator;
 *            WRITE_AAI ≤ 192 raw (reply ringbuf is now the binding limit). */
#define FLASH_DISPATCH_VERSION 0x4D464432u  /* 'MFD2' = MIDI Flash Dispatch v2 */

/* HASH_RANGE is implemented in patch/midi_sysex_filter.c (MIDI path only).
 * The DCP recovery path doesn't need it — recovery is rare and host-side
 * READ-based sector diff is acceptable. Keeping HASH_RANGE out of the
 * shared core saves ~400 bytes in the JTAG-injected handler region
 * (0x31480..0x31880, ~1 KB DEADBEEF zone). */
#define HASH_FLAG_PER_SECTOR 0x01u  /* used by SysEx HASH_RANGE only */

/* ── Op entry points ─────────────────────────────────────────────────── */

/* INFO: response = jedec(4) sector_size(4) total_size(4) version(4) */
int flash_op_info(uint8_t *resp_buf, uint32_t *resp_len);

/* READ: SPI flash read via sst25xx_fast_read (opcode 0x0B + dummy).
 *       `addr` is unambiguously a SPI flash address (0..0x1FFFFF).
 *       Max 4096 bytes per call. */
int flash_op_read(uint32_t addr, uint32_t len,
                  uint8_t *resp_buf, uint32_t *resp_len);

/* MEM_READ: raw CPU-memory memcpy. `addr` is a CPU memory address
 *           (SRAM 0..0x7FFFF, MMIO, XIP window). Used by the sector-
 *           diff host flow to drain HASH_RANGE's per-sector CRC array
 *           out of SRAM. Max 1024 bytes per call. */
int flash_op_mem_read(uint32_t addr, uint32_t len,
                      uint8_t *resp_buf, uint32_t *resp_len);

/* ERASE_SECTOR: erase `count` consecutive 4KB sectors starting at
 *   `addr`. Refuses any range overlapping golden copy. */
int flash_op_erase_sector(uint32_t addr, uint32_t count,
                          uint8_t *resp_buf, uint32_t *resp_len);

/* WRITE_AAI: program `len` bytes from `data` to `addr` via stock's
 *   sst25xx_aai_write (full RDSR-poll sync). Refuses golden copy. */
int flash_op_aai_write(uint32_t addr, const uint8_t *data, uint32_t len,
                       uint8_t *resp_buf, uint32_t *resp_len);

#endif /* MIDI_FLASH_DISPATCH_H */
