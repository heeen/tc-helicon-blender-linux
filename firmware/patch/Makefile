# Makefile for Blender DICE3 firmware patch
#
# Build pipeline:
#   hooks.py generate  →  hooks_gen.S + patch.ld
#   make               →  hooks.elf → hooks.bin
#   hooks.py patch     →  blender_spi_patched.bin (SPI image with CRC)
#   hooks.py inject    →  JTAG SRAM load (temporary)

CC      = arm-none-eabi-gcc
OBJCOPY = arm-none-eabi-objcopy
SIZE    = arm-none-eabi-size

CFLAGS  = -march=armv5te -marm -mno-thumb-interwork \
          -Os -nostdlib -ffreestanding -Wall -Wextra -Wno-unused-parameter

ELF     = hooks.elf
BIN     = hooks.bin
SRCS    = hooks_gen.S handlers.c
LD      = patch.ld

.PHONY: all clean size generate patch inject

all: $(BIN)

$(ELF): $(SRCS) $(LD)
	$(CC) $(CFLAGS) -T $(LD) -o $@ $(SRCS)

$(BIN): $(ELF)
	$(OBJCOPY) -O binary $< $@
	@$(SIZE) $<

# Generate hooks_gen.S and patch.ld from hook definitions
generate:
	python3 hooks.py generate

# Build and create patched SPI image
patch: $(BIN)
	python3 hooks.py patch

# Build and inject via JTAG (temporary, SRAM-only)
inject: $(BIN)
	python3 hooks.py inject

size: $(ELF)
	$(SIZE) -A $<

clean:
	rm -f $(ELF) $(BIN) hooks_gen.S patch.ld patched_body.bin
