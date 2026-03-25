/* firmware_symbols.ld — Ghidra-derived symbol addresses for DICE3 firmware
 *
 * IMPORTANT: Golden (recovery) and primary images have DIFFERENT function
 * addresses despite sharing the same SDK code. The primary body is 136 bytes
 * shorter, causing a systematic +0x430 shift for SDK functions and variable
 * shifts for app-specific code.
 *
 * Source: Ghidra analysis of blender_xip_1mb.bin (golden) and
 *         blender_primary_fixed.bin (primary)
 */

/*
 * ════════════════════════════════════════════════════════════
 * XIP flash mapping
 * CPU address = SPI address + XIP_BASE
 * Bootloader at CPU 0x4F404 = SPI 0xF404 confirms XIP_BASE = 0x40000
 * ════════════════════════════════════════════════════════════
 */
XIP_BASE = 0x40000;

/*
 * ════════════════════════════════════════════════════════════
 * PRIMARY FIRMWARE addresses (used by sram_flash_handler.c)
 * These are SRAM addresses as seen by the running firmware.
 * ════════════════════════════════════════════════════════════
 */

/* SST25xx SPI flash driver */
sst25xx_sector_erase   = 0x8B38;
sst25xx_aai_write      = 0x8968;
sst25xx_driver_ctx     = 0x2A93C;
dma_transfer_execute   = 0xF5CC;
spi_flash_cmd_rdsr     = 0x8550;

/* Utility */
memcpy = 0xA77C;

/* DCP (Device Control Protocol) */
dcp_register_handler = 0x1BEE0;
dcp_send_response    = 0x1C1F8;
dcp_state            = 0x30F78;

/* Displaced function — original target of `bl` at 0x4FAC (primary event loop) */
FUN_2f38 = 0x2F38;

/*
 * ════════════════════════════════════════════════════════════
 * GOLDEN FIRMWARE addresses (used by sram_flash_restore.c)
 * Kept for reference and for the sector restore tool.
 * ════════════════════════════════════════════════════════════
 */

/* Golden: SST25xx SPI flash driver */
/* sst25xx_sector_erase = 0x8708; */
/* sst25xx_aai_write    = 0x8538; */
/* sst25xx_driver_ctx   = 0x2A9C4; */

/* Golden: DCP */
/* dcp_register_handler = 0x1C008; */
/* dcp_send_response    = 0x1C324; */
/* dcp_state            = 0x316F0; */

/* Golden: displaced function for restore hook at 0x2C88 */
led_animation_engine = 0x46c4;

/* Golden: eCos kernel — cyg_* C API */
cyg_scheduler_lock      = 0xA410;
cyg_scheduler_unlock    = 0xA41C;
cyg_scheduler_read_lock = 0xA43C;
cyg_thread_create       = 0xA44C;
cyg_thread_resume       = 0xA49C;
cyg_thread_self         = 0xA4D4;
cyg_thread_delay        = 0xA4E4;
cyg_thread_get_next     = 0xA4FC;
cyg_thread_get_info     = 0xA58C;
cyg_current_time        = 0xA6C0;
cyg_clock_to_counter    = 0xA6D0;
cyg_real_time_clock     = 0xA6D8;
cyg_alarm_create        = 0xA6FC;
cyg_alarm_initialize    = 0xA734;
cyg_flag_init           = 0xA74C;
cyg_flag_setbits        = 0xA758;
cyg_flag_wait           = 0xA75C;
cyg_flag_timed_wait     = 0xA78C;
