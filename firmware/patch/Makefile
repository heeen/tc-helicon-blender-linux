# Makefile for Blender DICE3 firmware patch
#
# Build pipeline:
#   hooks.py generate  →  hooks_gen.S + patch.ld
#   make               →  hooks.elf → hooks.bin + bootstrap.bin
#   hooks.py patch     →  blender_spi_patched.bin (persistent SPI image)
#   hooks.py inject    →  JTAG SRAM load (temporary, no bootstrap needed)

CC      = arm-none-eabi-gcc
AS      = arm-none-eabi-as
OBJCOPY = arm-none-eabi-objcopy
SIZE    = arm-none-eabi-size
NM      = arm-none-eabi-nm

CFLAGS  = -march=armv5te -marm -mno-thumb-interwork \
          -Os -nostdlib -ffreestanding -Wall -Wextra -Wno-unused-parameter \
          -I../patch \
          -ffunction-sections -fdata-sections
# --gc-sections drops unused functions (keeps hooks.elf under the 6528 B
# PATCH region). -u, forces symbols that are only referenced externally
# (hooks.py patches firmware_entry's bl to call boot_init; nothing in
# this link unit references it directly).
LDFLAGS = -Wl,--gc-sections -Wl,-u,boot_init
ASFLAGS = -march=armv5te

ELF     = hooks.elf
BIN     = hooks.bin
SRCS    = hooks_gen.S handlers.c ../reboot_common.c
LD      = patch.ld

# Bootstrap: small DMA loader in safe code section gap
BOOT_S   = bootstrap.S
BOOT_ELF = bootstrap.elf
BOOT_BIN = bootstrap.bin
PATCH_BODY = patched_body.bin
SRAM_DRV_C   = ../../firmware/sram_flash_driver.c
SRAM_DRV_LD  = ../../firmware/sram_flash_driver.ld
SRAM_DRV_ELF = ../../firmware/sram_flash_driver.elf
SRAM_DRV_BIN = ../../firmware/sram_flash_driver.bin
UART_LOAD = ../../firmware/uart_xmodem_load.py
HYBRID_REPAIR = ../../firmware/repair_flash_hybrid.py

# Bootstrap addresses
BOOTSTRAP_SRAM = 0x299EC
HANDLER_SRAM   = 0x31480
HANDLER_SPI    = 0x6B000
ROM_LOAD_ADDR  ?= 0x200
REPAIR_REF     ?= ../../firmware/blender_spi_patched.bin
REPAIR_SECTOR  ?= 0x0

.PHONY: all clean size generate patch inject load patch-load repair-hybrid

all: $(BIN) $(BOOT_BIN)

HEADERS = dice_platform.h

$(ELF): $(SRCS) $(LD) $(HEADERS)
	$(CC) $(CFLAGS) $(LDFLAGS) -T $(LD) -o $@ $(SRCS)

$(BIN): $(ELF)
	$(OBJCOPY) -O binary $< $@
	@$(SIZE) $<

# Bootstrap build: extract constants from hooks.elf, assemble bootstrap
$(BOOT_BIN): $(BOOT_S) $(BIN)
	@MAGIC=$$(python3 -c "import struct; d=open('$(BIN)','rb').read(); print(f'0x{struct.unpack_from(\"<I\",d,0)[0]:08X}')"); \
	INIT=$$($(NM) $(ELF) | awk '/T midi_loop_hook/ {print "0x"$$1}'); \
	SIZE=$$(stat -c%s $(BIN)); \
	SIZE_ALIGNED=$$(( ($$SIZE + 3) & ~3 )); \
	WCOUNT=$$(( $$SIZE_ALIGNED / 4 - 1 )); \
	echo "Bootstrap: magic=$$MAGIC init=$$INIT size=$$SIZE_ALIGNED wcount=$$WCOUNT"; \
	$(AS) $(ASFLAGS) \
		--defsym HANDLER_SRAM=$(HANDLER_SRAM) \
		--defsym HANDLER_MAGIC=$$MAGIC \
		--defsym HANDLER_SPI=$(HANDLER_SPI) \
		--defsym HANDLER_DMA_WCOUNT=$$WCOUNT \
		--defsym HANDLER_SIZE=$$SIZE_ALIGNED \
		--defsym HANDLER_INIT=$$INIT \
		--defsym FUN_2f38=0x2F38 \
		-o bootstrap.o $(BOOT_S) && \
	arm-none-eabi-ld -Ttext=$(BOOTSTRAP_SRAM) -o $(BOOT_ELF) bootstrap.o && \
	$(OBJCOPY) -O binary $(BOOT_ELF) $(BOOT_BIN) && \
	echo "Bootstrap: $$(stat -c%s $(BOOT_BIN)) bytes (max 152)" && \
	[ $$(stat -c%s $(BOOT_BIN)) -le 152 ] || (echo "ERROR: bootstrap too large!"; rm $(BOOT_BIN); false)

# Generate hooks_gen.S and patch.ld from hook definitions
generate:
	python3 hooks.py generate

# Build and create patched SPI image (persistent)
patch: $(BIN) $(BOOT_BIN)
	python3 hooks.py patch

# Build and inject via JTAG (temporary, SRAM-only)
inject: $(BIN)
	python3 hooks.py inject

# Build patched SRAM body and load it via ROM UART XMODEM
patch-load: patch
	python3 $(UART_LOAD) $(PATCH_BODY) --load-addr $(ROM_LOAD_ADDR) --run-addr $(ROM_LOAD_ADDR)

load: patch-load

repair-hybrid:
	python3 $(HYBRID_REPAIR) --ref $(REPAIR_REF) --sector $(REPAIR_SECTOR)

# SRAM flash driver (v2) used by jtag_flash_v2.py
SRAM_DRV_V2_C   = ../../firmware/sram_flash_driver_v2.c
SRAM_DRV_V2_LD  = ../../firmware/sram_flash_driver_v2.ld
SRAM_DRV_V2_ELF = ../../firmware/sram_flash_driver_v2.elf
SRAM_DRV_V2_BIN = ../../firmware/sram_flash_driver_v2.bin

$(SRAM_DRV_V2_BIN): $(SRAM_DRV_V2_C) $(SRAM_DRV_V2_LD)
	arm-none-eabi-gcc -march=armv5te -marm -Os -nostdlib -ffreestanding \
		-Wall -Wextra -T $(SRAM_DRV_V2_LD) -o $(SRAM_DRV_V2_ELF) $(SRAM_DRV_V2_C)
	$(OBJCOPY) -O binary $(SRAM_DRV_V2_ELF) $(SRAM_DRV_V2_BIN)

# Flash patched image via JTAG using the v2 driver + flash-all planner.
# v2 covers every 64 KB block (erase-only op for all-FF regions), catches
# stale garbage that a bytes-differ-from-reference shortcut would skip.
# python3 -u keeps stdout unbuffered so progress streams under make.
# Override with `make flash FLASH_SPEED=400` if the 1 MHz default flakes.
FLASH_SPEED ?= 1000
flash: $(BIN) $(BOOT_BIN) $(SRAM_DRV_V2_BIN)
	python3 -u hooks.py patch
	python3 -u ../../firmware/jtag_flash_v2.py \
		--mode flash-all --nreset \
		--ref ../../firmware/blender_spi_patched.bin \
		--speed $(FLASH_SPEED)

size: $(ELF)
	$(SIZE) -A $<

# Ghidra symbol export → header generation pipeline
GHIDRA       ?= analyzeHeadless
GHIDRA_PROJ  ?= $(HOME)/ghidra_projects
PROJECT      ?= blender
PROGRAM      ?= blender_primary_body.bin
SCRIPTS_DIR  = ../../ghidra_scripts
GENERATE     = ../../scripts/generate_patch_files.py
SYMBOLS_JSON = symbols.json

symbols: $(SYMBOLS_JSON)
$(SYMBOLS_JSON): $(GHIDRA_PROJ)/$(PROJECT)/$(PROJECT).rep
	$(GHIDRA) $(GHIDRA_PROJ)/$(PROJECT) $(PROJECT) \
		-process $(PROGRAM) -noanalysis \
		-scriptPath $(abspath $(SCRIPTS_DIR)) \
		-postScript ExportSymbols.java $(abspath $@)

# Generate fw_blender.h + fw_symbols.ld from Ghidra export
gen-headers: $(SYMBOLS_JSON)
	python3 $(GENERATE) $(SYMBOLS_JSON) -o . --header-name fw_blender.h

clean:
	rm -f $(ELF) $(BIN) hooks_gen.S patch.ld patched_body.bin
	rm -f bootstrap.o $(BOOT_ELF) $(BOOT_BIN)
