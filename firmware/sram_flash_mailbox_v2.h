/*
 * sram_flash_mailbox_v2.h — shared host/device mailbox layout for the v2
 * SRAM flash driver.
 *
 * Single-writer per field: HOST fields are never touched by the driver,
 * DEV fields are never touched by the host. This closes the init-race
 * v1 had where driver's do_main clobbered mb->command.
 *
 * The mailbox lives at SRAM 0x2E000 and the log ring at 0x2E200 (both
 * placed by sram_flash_driver_v2.ld). Do not change these addresses
 * without updating every host tool.
 */
#ifndef SRAM_FLASH_MAILBOX_V2_H
#define SRAM_FLASH_MAILBOX_V2_H

#include <stdint.h>

/* ── Fixed SRAM addresses ─────────────────────────────────── */
#define V2_MBOX_ADDR        0x0002E000u
#define V2_TIMINGS_ADDR     0x0002E180u   /* 64 B, all DEV/HOST-R/W */
#define V2_LOG_RING_ADDR    0x0002E200u
#define V2_DATA_BUF_ADDR    0x0002B000u   /* single-sector source/dest */
#define V2_IMAGE_BASE_ADDR  0x0002F900u   /* bulk sector payload */
#define V2_SECTOR_LIST_ADDR 0x0002F100u
#define V2_DRIVER_CODE_ADDR 0x0002C000u

#define V2_SECTOR_SIZE      0x1000u

/* ── Magic values ─────────────────────────────────────────── */
#define V2_MAGIC_CMD        0x4D324657u  /* 'M2FW' — host sets on new cmd */
#define V2_BUILD_TAG_MASK   0xFFFFFFFFu

/* ── Commands (HOST → DEV) ────────────────────────────────── */
#define V2_CMD_IDLE         0u
#define V2_CMD_ERASE        1u    /* erase 1 sector at flash_addr */
#define V2_CMD_PROGRAM      2u    /* AAI word-program length B from buf_addr */
#define V2_CMD_READ         3u    /* read length B from flash into buf_addr */
#define V2_CMD_VERIFY       4u    /* read+cmp length B against buf_addr */
#define V2_CMD_BP_CLEAR     5u    /* clear block protection */
#define V2_CMD_FLASH_ALL    6u    /* iterate sector list, erase+program+verify */
#define V2_CMD_QUIT         7u    /* exit driver (halt for JTAG pickup) */
#define V2_CMD_ERASE_32K    8u    /* 32 KB block erase (flash_addr 32K-aligned) */
#define V2_CMD_ERASE_64K    9u    /* 64 KB block erase (flash_addr 64K-aligned) */
#define V2_CMD_FLASH_SECTOR 10u   /* erase+program+verify one 4 KB sector autonomously */
#define V2_CMD_FLASH_BLOCK  11u   /* erase+AAI-program+verify up to 64 KB; auto-picks
                                   * SE/BE32/BE64 from flash_addr alignment + length */
#define V2_CMD_BYTE_PATCH   12u   /* BYTE_PROGRAM each byte in buf_addr[0..length-1]
                                   * onto flash[flash_addr..flash_addr+length-1].
                                   * No erase. Used to fix AAI-dropped tail bytes. */
#define V2_CMD_READ_ID      13u   /* Exercise dma_rx with a multi-byte response.
                                   * flash_addr: 0=JEDEC(0x9F, 3B), 1=LEGACY(0xAB, 1B)
                                   * length: ignored; fixed by command variant.
                                   * Result bytes land at buf_addr (DATA_BUF if host
                                   * doesn't set it). Diagnostic — confirms the full
                                   * DMA-RX + IRQ/polling fallback machinery works. */

/* ── Status (DEV) ─────────────────────────────────────────── */
#define V2_STATUS_READY     0u
#define V2_STATUS_BUSY      1u
#define V2_STATUS_OK        2u
#define V2_STATUS_ERR       3u

/* ── Phase (DEV) — what the driver is doing right now ─────── */
#define V2_PHASE_INIT        0u
#define V2_PHASE_TEARDOWN    1u
#define V2_PHASE_T0_ENABLE   2u
#define V2_PHASE_IDLE        3u
#define V2_PHASE_BP_CLEAR    4u
#define V2_PHASE_ERASE_WREN  5u
#define V2_PHASE_ERASE_CMD   6u
#define V2_PHASE_ERASE_POLL  7u
#define V2_PHASE_AAI_WREN    8u
#define V2_PHASE_AAI_FIRST   9u
#define V2_PHASE_AAI_PAIR    10u
#define V2_PHASE_AAI_WRDI    11u
#define V2_PHASE_VERIFY      12u
#define V2_PHASE_READ        13u
#define V2_PHASE_DONE        14u
#define V2_PHASE_ERROR       15u

/* ── Events (log ring) ────────────────────────────────────── */
#define V2_EVT_SETUP_BEGIN   0x01
#define V2_EVT_SETUP_DONE    0x02
#define V2_EVT_T0_ENABLED    0x03
#define V2_EVT_CMD_RX        0x04
#define V2_EVT_BP_CLEAR_PRE  0x10
#define V2_EVT_BP_CLEAR_POST 0x11
#define V2_EVT_ERASE_WREN    0x20
#define V2_EVT_ERASE_CMD     0x21
#define V2_EVT_ERASE_DONE    0x22
#define V2_EVT_AAI_WREN      0x30
#define V2_EVT_AAI_FIRST     0x31
#define V2_EVT_AAI_PAIR      0x32   /* every 64th pair */
#define V2_EVT_AAI_BUSY_FAST 0x33   /* fixed-delay was enough */
#define V2_EVT_AAI_BUSY_POLL 0x34   /* had to fall through to RDSR */
#define V2_EVT_AAI_WRDI      0x35
#define V2_EVT_VERIFY_OK     0x40
#define V2_EVT_VERIFY_MISS   0x41
#define V2_EVT_REPAIR_HIT    0x50   /* head-of-sector repair triggered */
#define V2_EVT_DMA_TX_DONE   0x60
#define V2_EVT_DMA_RX_DONE   0x61
#define V2_EVT_TIMEOUT       0xF0
#define V2_EVT_ABORT         0xF1
#define V2_EVT_BAD_STATE     0xF2

/* ── Error codes (mb->err_code) ───────────────────────────── */
#define V2_ERR_NONE             0x0000
#define V2_ERR_BP_CLEAR_WREN    0x0101
#define V2_ERR_BP_CLEAR_WRSR    0x0102
#define V2_ERR_BP_CLEAR_STUCK   0x0103
#define V2_ERR_ERASE_WREN       0x0201
#define V2_ERR_ERASE_CMD        0x0202
#define V2_ERR_ERASE_TIMEOUT    0x0203
#define V2_ERR_AAI_WREN         0x0301
#define V2_ERR_AAI_FIRST_TX     0x0302
#define V2_ERR_AAI_PAIR_TX      0x0303
#define V2_ERR_AAI_PAIR_TIMEOUT 0x0304
#define V2_ERR_AAI_WEL_LOST     0x0305
#define V2_ERR_VERIFY_MISMATCH  0x0401
#define V2_ERR_READ_DMA         0x0402
#define V2_ERR_BAD_COMMAND      0x0501
#define V2_ERR_BAD_LENGTH       0x0502
#define V2_ERR_BAD_ADDR         0x0503

/* ── Mailbox struct (single-writer-per-field) ─────────────── */
struct v2_mailbox {
    /* HOST owns these — driver only reads/clears magic on accept. */
    volatile uint32_t magic;          /* +0x00 */
    volatile uint32_t command;        /* +0x04 */
    volatile uint32_t flash_addr;     /* +0x08 */
    volatile uint32_t buf_addr;       /* +0x0C */
    volatile uint32_t length;         /* +0x10 */

    /* DEV owns these — host only reads. */
    volatile uint32_t status;         /* +0x14 */
    volatile uint32_t phase;          /* +0x18 */
    volatile uint32_t phase_detail;   /* +0x1C */
    volatile uint32_t last_sr;        /* +0x20 */
    volatile uint32_t err_code;       /* +0x24 */
    volatile uint32_t err_spi_addr;   /* +0x28 */
    volatile uint32_t elapsed_us;     /* +0x2C */
    volatile uint32_t seq;            /* +0x30 — monotonic; host polls */
    volatile uint32_t log_head;       /* +0x34 */
    volatile uint32_t log_tail;       /* +0x38 — host's advisory cursor */
    volatile uint32_t pair_retries;   /* +0x3C — AAI pair SPI_ERR retries,
                                       * reset per command */
    volatile uint32_t build_tag;      /* +0x40 */

    /* Verify-miss capture — DEV writes these when VERIFY_MISS fires,
     * before setting err_code. Host reads them alongside the error. */
    volatile uint32_t miss_offset;    /* +0x44 — byte index inside verify window */
    volatile uint32_t miss_got;       /* +0x48 — 4 bytes @ miss_offset (LE, zero-pad at tail) */
    volatile uint32_t miss_expected;  /* +0x4C — 4 bytes @ miss_offset from expected buf */
    volatile uint32_t miss_spi_stat;  /* +0x50 — SPI_STAT at moment of miss */
    volatile uint32_t miss_dma_istat; /* +0x54 — DMA_ISTAT at moment of miss */
    volatile uint32_t miss_reads_done;/* +0x58 — #dma_bidir_read chunks issued before miss */
};

/* ── Log ring entry (12 bytes) ────────────────────────────── */
struct v2_log_entry {
    uint32_t t_us;        /* Timer0-derived microsecond timestamp */
    uint8_t  evt;         /* V2_EVT_* */
    uint8_t  phase;       /* snapshot of MBOX->phase */
    uint16_t detail;      /* pair idx, miss offset, etc. */
    uint32_t spi_addr;    /* the SPI address being operated on */
};

#define V2_LOG_RING_ENTRIES 128u
#define V2_LOG_RING_MASK    (V2_LOG_RING_ENTRIES - 1u)

/* ── Tunable timings (HOST R/W, DEV R) ────────────────────── */
struct v2_timings {
    volatile uint32_t magic;                     /* 0 = driver uses defaults */

    /* Per-AAI-pair wait (datasheet §4.4 "check BUSY before next pair"). */
    volatile uint32_t aai_pair_fixed_us;         /* fixed prologue */
    volatile uint32_t aai_pair_poll_interval_us; /* RDSR poll interval */
    volatile uint32_t aai_pair_poll_budget_us;   /* max extra poll time */

    /* Pre-WRDI wait at end of an AAI run. */
    volatile uint32_t pre_wrdi_fixed_us;
    volatile uint32_t pre_wrdi_poll_interval_us;
    volatile uint32_t pre_wrdi_poll_budget_us;

    /* Post-WRDI wait before the next op (verify / next run). */
    volatile uint32_t post_wrdi_fixed_us;
    volatile uint32_t post_wrdi_poll_interval_us;
    volatile uint32_t post_wrdi_poll_budget_us;

    /* Erase wait (SE / BE32 / BE64 share these values). */
    volatile uint32_t erase_fixed_us;
    volatile uint32_t erase_poll_interval_us;
    volatile uint32_t erase_poll_budget_us;

    /* SPI transport backend: 0 = DMA (default), 1 = PIO. Latched once
     * at driver init; host must set before the first command. Diagnostic
     * lever for isolating AAI tail drops between SPI DMA engine and the
     * flash silicon itself. */
    volatile uint32_t xport_mode;

    /* Byte-program wire-address offset. Added to the passed spi_addr
     * before framing the BYTE_PRG command. Default 4 (the empirical
     * off-by-4 workaround). Set to 0 to observe raw landing behavior. */
    volatile uint32_t byte_prog_offset;
};

#define V2_XPORT_DMA 0u
#define V2_XPORT_PIO 1u

#define V2_TIMINGS_MAGIC      0x54494D32u  /* 'TIM2' — HOST sets when valid */

/* Conservative datasheet-sourced defaults. Driver uses these if the host
 * hasn't written a valid v2_timings struct (magic != V2_TIMINGS_MAGIC). */
#define V2_TIM_DEFAULT_AAI_PAIR_FIXED_US         10u
#define V2_TIM_DEFAULT_AAI_PAIR_POLL_INTERVAL_US 5u
#define V2_TIM_DEFAULT_AAI_PAIR_POLL_BUDGET_US   100u
#define V2_TIM_DEFAULT_PRE_WRDI_FIXED_US         40u
#define V2_TIM_DEFAULT_PRE_WRDI_POLL_INTERVAL_US 500u
#define V2_TIM_DEFAULT_PRE_WRDI_POLL_BUDGET_US   5000u
#define V2_TIM_DEFAULT_POST_WRDI_FIXED_US        200u
#define V2_TIM_DEFAULT_POST_WRDI_POLL_INTERVAL_US 500u
#define V2_TIM_DEFAULT_POST_WRDI_POLL_BUDGET_US  20000u
#define V2_TIM_DEFAULT_ERASE_FIXED_US            60000u
#define V2_TIM_DEFAULT_ERASE_POLL_INTERVAL_US    2000u
#define V2_TIM_DEFAULT_ERASE_POLL_BUDGET_US      500000u
#define V2_TIM_DEFAULT_XPORT_MODE                V2_XPORT_DMA
#define V2_TIM_DEFAULT_BYTE_PROG_OFFSET          0u

#endif /* SRAM_FLASH_MAILBOX_V2_H */
