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
          -Os -nostdlib -ffreestanding -Wall -Wextra -Wno-unused-parameter
ASFLAGS = -march=armv5te

ELF     = hooks.elf
BIN     = hooks.bin
SRCS    = hooks_gen.S handlers.c
LD      = patch.ld

# Bootstrap: small DMA loader in safe code section gap
BOOT_S   = bootstrap.S
BOOT_ELF = bootstrap.elf
BOOT_BIN = bootstrap.bin

# Bootstrap addresses
BOOTSTRAP_SRAM = 0x299EC
HANDLER_SRAM   = 0x31480
HANDLER_SPI    = 0x6B000

.PHONY: all clean size generate patch inject

all: $(BIN) $(BOOT_BIN)

$(ELF): $(SRCS) $(LD)
	$(CC) $(CFLAGS) -T $(LD) -o $@ $(SRCS)

$(BIN): $(ELF)
	$(OBJCOPY) -O binary $< $@
	@$(SIZE) $<

# Bootstrap build: extract constants from hooks.elf, assemble bootstrap
$(BOOT_BIN): $(BOOT_S) $(BIN)
	@MAGIC=$$(python3 -c "import struct; d=open('$(BIN)','rb').read(); print(f'0x{struct.unpack_from(\"<I\",d,0)[0]:08X}')"); \
	INIT=$$($(NM) $(ELF) | awk '/T flash_handler_init/ {print "0x"$$1}'); \
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

size: $(ELF)
	$(SIZE) -A $<

clean:
	rm -f $(ELF) $(BIN) hooks_gen.S patch.ld patched_body.bin
	rm -f bootstrap.o $(BOOT_ELF) $(BOOT_BIN)
