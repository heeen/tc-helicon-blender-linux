#!/usr/bin/env python3
"""Hook definitions for Blender DICE3 firmware.

First hook: DCP flash handler (category 0x81F) — provides USB-accessible
SPI flash operations for firmware update without JTAG.

Usage:
    python3 firmware/patch/hooks.py validate   # check hook points
    python3 firmware/patch/hooks.py generate   # emit hooks_gen.S + patch.ld
    make -C firmware/patch                     # compile
    python3 firmware/patch/hooks.py patch      # create patched SPI image
    python3 firmware/patch/hooks.py inject     # JTAG live injection
"""

from pathlib import Path
from hook_framework import Hook, PatchProject

FIRMWARE_DIR = Path(__file__).resolve().parent.parent
PATCH_DIR = Path(__file__).resolve().parent
SPI_IMAGE = FIRMWARE_DIR / 'blender_spi_flash_restored.bin'
PATCHED_SPI = FIRMWARE_DIR / 'blender_spi_patched.bin'

# ── Layout ──────────────────────────────────────────────────────────────────
#
# Current firmware body: 0xAA22 words = 174216 bytes (content + CRC)
# Body content (no CRC): 174212 bytes = 0x2A884 bytes
# SRAM load address: 0x200
#
# Body append approach: extend body with handler code.
# Bootloader copies size_words*4 bytes from SPI to SRAM (max 0x34000 words).
# eCos BSS clear starts at __bss_start (stored at SRAM 0x698).
# We MUST patch bss_start to skip past our handler, or eCos zeros it.
#
# Layout:
#   [original content 174212B] [handler ~448B] [new CRC 4B]
#   bss_start patched: 0x2AA84 → 0x2AA84 + handler_padded

OLD_SIZE_WORDS = 0xAA22
BODY_CONTENT_SIZE = OLD_SIZE_WORDS * 4 - 4  # 174212 bytes
PATCH_ZONE_SRAM = 0x200 + BODY_CONTENT_SIZE  # 0x2AA84 (append to body)
PATCH_ZONE_SIZE = 0x800                       # 2KB for future growth

# eCos BSS start pointer location (in body, SRAM address 0x698)
BSS_START_PTR_SRAM = 0x698
BSS_START_ORIG = 0x2AA84

# Firmware identity: first word of sst25xx_aai_write
IDENTITY_ADDR = 0x8968
IDENTITY_WORD = 0xe92d4ff0

# ── Hook: DCP flash handler ────────────────────────────────────────────────
#
# Hook site: 0x4FAC — `bl 0x2F38` (FUN_2f38) in primary event loop.
# This is the last call in the main loop, fires ~100Hz.
#
# Our handler registers a DCP category 0x81F handler on first call,
# then becomes a no-op passthrough. Mode="before" means we always
# continue to the original function after our handler runs.

hooks = [
    Hook(
        name="dcp_flash",
        target=0x4FAC,
        handler="flash_handler_init",
        mode="before",
    ),
]

# ── Project ─────────────────────────────────────────────────────────────────

project = PatchProject(
    hooks=hooks,
    spi_image=str(SPI_IMAGE),
    patched_spi=str(PATCHED_SPI),
    hook_bin=str(PATCH_DIR / "hooks.bin"),
    elf_path=str(PATCH_DIR / "hooks.elf"),
    build_dir=str(PATCH_DIR),
    engine_kwargs=dict(
        patch_zone_start=PATCH_ZONE_SRAM,
        patch_zone_size=PATCH_ZONE_SIZE,
    ),
    firmware_symbols=str(FIRMWARE_DIR / "firmware_symbols.ld"),
    identity_check=(IDENTITY_ADDR, IDENTITY_WORD),
    bss_start_ptr=BSS_START_PTR_SRAM,
)

if __name__ == '__main__':
    project.main()
