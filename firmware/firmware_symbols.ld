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
sst25xx_fast_read      = 0x8734;   /* FAST_READ (0x0B) — see sram_flash_handler.c plate comment */
sst25xx_driver_ctx     = 0x2A93C;
dma_transfer_execute   = 0xF5CC;
spi_flash_cmd_rdsr     = 0x8550;

/* Utility */
memcpy = 0xA77C;

/* DCP (Device Control Protocol) */
dcp_register_handler = 0x1BEE0;
dcp_send_response    = 0x1C1F8;
dcp_state            = 0x30F78;

/* Displaced function — original target of `bl` at 0x5180 (midi_engine_thread main loop) */
midi_rx_poll = 0x2AB8;
FUN_2ab8 = 0x2AB8;

/* Legacy: original target of `bl` at 0x4FAC (ble_spi_bridge_thread, no longer hooked) */
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
usb_iface_get_tx_endpoint    = 0x20768;  /* (conn, type) → EP handle via vtable dispatch */
usb_midi_conn                = 0x2A230;  /* USB MIDI connection object (vtable at word 0, state at +0x2BC) */
midi_rx_ep_handle            = 0x2A7E8;  /* EP handle struct for EP 0x03 OUT (expected addr) */
midi_pkt_buf                 = 0x30D64;  /* USB-MIDI packet buffer (0x210 bytes + metadata) */

/* USB MIDI streaming — proper init sequence (from usb_midi_streaming_start) */
usb_midi_streaming_start         = 0x19C40;  /* full MIDI streaming init — needs R4 = streaming_state */
usb_midi_streaming_reconfigure   = 0x19BF8;  /* reconfigure on alt-setting change */
usb_midi_tx_endpoint_start       = 0x1BE10;  /* (tx_ep, cable_flag) → start TX side */
usb_endpoint_descriptor_build    = 0x19A94;  /* (desc_area, ep_type) → build 7-byte EP descriptor */
usb_endpoint_start_if_configured = 0x1FF34;  /* (conn) → program hardware via vtable if state >= 5 */
usb_hw_ep_read_start             = 0x2115C;  /* (conn, ep_descriptor) → full QH init for endpoint */
usb_hw_ep_start_transfer_rx      = 0x21A6C;  /* Ghidra "_rx" — actually IN submit (calls dma_start_rx which pokes 0x834 IN-prime) */
usb_hw_ep_start_transfer         = 0x2140C;  /* Ghidra generic — actually OUT submit (calls dma_start; no 0x834 poke) */
dcp_streaming_state_set          = 0x1C454;  /* (state) → enable/disable DCP streaming gate */

/* MIDI engine startup functions */
midi_usb_endpoints_init          = 0x4434;   /* mixer_hw_dsr callback + routing setup */
midi_usb_endpoints_setup         = 0x1080;   /* find + link RX/TX endpoint handlers */
midi_engine_start                = 0x2998;   /* clear FSS flag, set engine running */
midi_rx_poll                     = 0x2AB8;   /* read ring buffer, feed parser */
midi_parser_process_bytes        = 0x3ED0;   /* (ctx, buf, len) MIDI byte SM — used by midi_rx_dispatch passthrough */

/* TCAT MIDI streaming state object (BSS) — referenced by streaming_start/reconfigure */
midi_streaming_state             = 0x29CA4;  /* +0x20=EP desc, +0x28=cable cfg, +0x460=DCP TX ep */

/* MIDI subsystem (primary firmware) */
midi_parser_ctx         = 0x29764;  /* MIDI parser state: status+0, cb_channel+0x28, cb_sysex+0x2C */
midi_channel_msg_cb_orig = 0x2A44; /* original channel message callback (chains to this) */
midi_tx_write           = 0x1114;  /* (ringbuf+0x2C, data, len) → USB MIDI EP IN */
midi_rx_ringbuf         = 0x29404; /* ring buffer: base+0, len+4, woff+8, roff+C */

/* Canonical "set level + apply + LED + BLE notify" API — same path the BLE/
 * knob dispatchers use. Updates mixer_state, calls *_apply (DSP), refreshes
 * LED level meter, and schedules a BLE outbound notification. */
master_level_set_notify     = 0x1F50;  /* (bus, level)        */
compressor_level_set_notify = 0x1EE4;  /* (bus, level)        */
channel_level_set_notify    = 0x1E64;  /* (input, bus, level) — note arg order! */

/* Lower-level helpers (still useful for state dumps / diagnostics). */
ble_send_full_state_dump = 0x1D14;  /* () — full mixer_state + hasblender to BLE outbound */
ble_write_tuple         = 0x618C;  /* (paramId, subParam, value) — generic 3-byte tuple */
led_full_refresh        = 0x1BD4;  /* () — full LED refresh (bargraph + button LEDs) */

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
