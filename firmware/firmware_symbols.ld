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

/* eCos RTOS app init — called from firmware_entry at 0x344 */
rtos_app_init = 0x9CD4;

/* Mixer hardware functions (primary firmware) */
channel_level_apply    = 0x1468;  /* (bus 0-3, input 0-6, level 0-31) → DICE mixer coeff */
master_level_apply     = 0x45C0;  /* (bus 0-3, level 0-31) → master output */
compressor_level_apply = 0x13F0;  /* (bus 0-3, level 0-31) → compressor amount */
mute_set               = 0x4570;  /* (bus 0-3, on/off) */
led_mute_update        = 0x68F4;  /* (input 0-5, on/off) → LED via SPI to coprocessor */

/* Mixer state (SRAM BSS) — inputs[4][7] + compressor[4] + master[4] = 36 bytes */
mixer_state = 0x2D81C;

/* DCP handler list (primary firmware) */
dcp_handler_list        = 0x313C4;  /* dcp_state + 0x44C — linked list of handler nodes */

/* UART output (primary firmware) */
uart_putc               = 0x971C;   /* void uart_putc(char c) — polls status bit 5, writes to 0xC5000000 */
uart_print_string       = 0x9E48;   /* void uart_print_string(const char *s) — null-terminated, handles \n→\r\n */
uart_printf             = 0x9E68;   /* int uart_printf(const char *fmt, ...) — variadic, full printf */

/* eCos kernel API (primary firmware = golden + 0x430) */
cyg_real_time_clock    = 0xAB08;
cyg_clock_to_counter   = 0xAB00;
cyg_alarm_create       = 0xAB2C;
cyg_alarm_initialize   = 0xAB64;
cyg_current_time       = 0xAAF0;

/* Audio clock / sample rate (primary firmware) */
usb_audio_rx_set_sample_rate = 0x194C8;  /* (uint sample_rate) → clock source lookup + mode config */

/* USB MIDI streaming (primary firmware) */
usb_endpoint_submit_transfer = 0x2077C;  /* (ep, buf, size, callback, ctx) → primes EP */
usb_midi_rx_complete         = 0x1B9E4;  /* bulk OUT DMA completion callback */
usb_midi_rx_start            = 0x1BE50;  /* (ep_handle, num_cables) → arms EP for receive */
usb_iface_get_rx_endpoint    = 0x20754;  /* (conn, type) → EP handle via vtable dispatch */
usb_midi_conn                = 0x2A230;  /* USB MIDI connection object (vtable at word 0) */
midi_rx_ep_handle            = 0x2A7E8;  /* EP handle struct for EP 0x03 OUT (expected addr) */
midi_pkt_buf                 = 0x30D64;  /* USB-MIDI packet buffer (0x210 bytes + metadata) */

/* MIDI subsystem (primary firmware) */
midi_parser_ctx         = 0x29764;  /* MIDI parser state: status+0, cb_channel+0x28, cb_sysex+0x2C */
midi_channel_msg_cb_orig = 0x2A44; /* original channel message callback (chains to this) */
midi_tx_write           = 0x1114;  /* (ringbuf+0x2C, data, len) → USB MIDI EP IN */
midi_rx_ringbuf         = 0x29404; /* ring buffer: base+0, len+4, woff+8, roff+C */

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
