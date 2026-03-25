# DICE3 SPI Controller Architecture

Confirmed via Ghidra decompilation of `blender_xip_1mb.bin` (2026-03-23).

## Peripheral Map

| Base Address | Function | PIO? | DMA? | Connected To |
|-------------|----------|------|------|--------------|
| 0x80000000 | DMA engine | - | - | Ch0-3 at +0x100/+0x120/+0x140/+0x160; per-ch: SRC/DST/NXT/CFG/TRG |
| 0xC5000000 | Clock/PLL | - | - | System clocks (NOT SPI) |
| 0xC9000000 | Config regs | - | - | Boot config, PLL multipliers |
| 0xCB000000 | GPIO/periph | - | - | Pin config, LED CS, peripheral control |
| 0xCC000000 | SPI flash controller | Unproven | Yes | SST25VF016B (2MB) |
| 0xCF000000 | Coprocessor SPI | Yes | No | LED controller / coprocessor (NOT flash) |

## SPI Register Layout (shared IP block at 0xCC and 0xCF)

| Offset | Name | Description |
|--------|------|-------------|
| 0x00 | CTRL | Mode: 0x107=TX, 0x287=RX PIO, 0x307=RX DMA |
| 0x04 | LEN | Transfer length (n_bytes - 1) |
| 0x08 | EN | Enable (1=on, 0=off) |
| 0x10 | CS | Chip select (1=assert); also RX start trigger (4=RX go) |
| 0x14 | CLK | Clock divider (flash=2, LED=0xFF) |
| 0x18 | CLRINT | Interrupt/status clear (written 0 after CS assert in DMA mode) |
| 0x28 | STAT | bit0=busy, bit1=TX ready, bit3=RX ready |
| 0x2C | DMAGO | DMA go/complete signal (0 before xfer, 1 after TX DMA complete) |
| 0x34 | ERR | Error flags (bit2 checked after DMA) |
| 0x4C | DMAMD | 0=PIO, 1=RX DMA, 2=TX DMA, 3=bidir DMA |
| 0x50 | DMACFG0 | DMA config 0 (set to 4 before DMA transfer) |
| 0x54 | DMACFG1 | DMA config 1 (set to 3 before DMA transfer) |
| 0x60 | DATA | TX/RX data register (also DMA command register for 0xCC) |
| 0x70 | RXDMA | RX DMA port |
| 0x80 | TXDMA | TX DMA port |

## Flash Access Paths

### Bootloader DMA Read (dma_spi_read_setup @ 0x4F290)
```
0xCC000000 = 0x307      (RX DMA mode)
0xCC000004 = size - 1   (transfer length)
0xCC00004C = 1           (DMA mode)
0xCC000008 = 1           (enable)
0xCC000054 = 3
DMA descriptors at 0x80000100 (ch0)
0xCC000060 = spi_addr    (SPI READ command — controller auto-prepends 0x03?)
0xCC000010 = 1           (chip select / start)
→ dma_poll_complete() checks 0x80000014 & 1 for completion
```

### Firmware DMA Write (dma_transfer_execute @ 0xF19C)
```
hw_resource_acquire()     → semaphore lock
hw_resource_start()       → vtable: (*(device+0x18))() — platform SPI setup
  → sets up 0xCC registers + DMA descriptors via vtable function
  → triggers DMA transfer
ISR fires on completion   → thread wake
hw_resource release       → scheduler unlock
```

Calling convention (from spi_flash_cmd_wren disassembly):
- r0 = DMA channel handle (driver_ctx + 0x18)
- r1 = 0
- r2 = byte count
- r3 = TX buffer pointer
- [sp+0] = RX buffer pointer (NULL for TX-only)

### Bootloader LED PIO (spi_send_10byte_cmd @ 0x4F22C)
```
0xCF000000 = 0x107       (TX mode)
0xCF000010 = 1           (chip select)
Loop 10x: poll 0xCF000028 & 2, write byte to 0xCF000060
Poll 0xCF000028 & 1 for idle
→ NOT connected to SPI flash — controls LED patterns
```

### Bootloader LED PIO RX (spi_read_3byte_response @ 0x4F1A4)
```
0xCF000000 = 0x287       (RX PIO mode)
0xCF000004 = 2
0xCF000010 = 4           (RX start trigger)
Write 0 to 0xCF000060    (dummy byte to start clocking)
Loop 3x: poll 0xCF000028 & 8, read byte from 0xCF000060
```

## Boot Sequence (boot_check_and_load @ 0x4F404)

1. Init clocks: 0xC9, 0xC5 registers
2. Configure GPIO pins
3. Set SPI clock dividers: 0xCC000014=2, 0xCF000014=0xFF
4. Send LED pattern via 0xCF (spi_send_10byte_cmd)
5. Read LED response (spi_read_3byte_response) × 2
6. Try golden at SPI 0x110000: read 0x30-byte header, check 'ZZ' magic
7. Try primary at SPI 0x40000: same check
8. DMA read firmware body + CRC verify (max 0xD0000 bytes)
9. On success: firmware_entry_rtos_init()
10. On fail: try fallback, then recovery at SPI 0x10000
11. Recovery path does NOT check 'ZZ' magic — only img_type and size

## DMA Engine Register Layout (0x80000000)

| Offset | Name | Description |
|--------|------|-------------|
| 0x08 | EN | Channel enable bits (write 1<<ch to enable) |
| 0x10 | ICLR | Interrupt clear bits (write 1<<ch) |
| 0x14 | ISTAT | Interrupt status (bit N = ch N complete) |
| 0x30+ch*4 | CHnCLR | Clear/reset channel N |

Per-channel registers at 0x100 + ch*0x20:

| Offset | Name | Description |
|--------|------|-------------|
| +0x00 | SRC | Source address |
| +0x04 | DST | Destination address |
| +0x08 | NXT | Next descriptor pointer (0 = no chain) |
| +0x0C | CFG | Control: [11:0]=byte count, [31]=interrupt-on-complete |
| +0x10 | TRG | Trigger (0xD005 for SPI TX ch0; must clear to 0 before programming) |

Firmware CFG flags: `0x70009000` base + `0x04000000` (src increment) + `0x80000000` (last/IRQ).
Trigger is written as `base_trigger | 0x8001` by FUN_000091f0.

## Correct DMA TX Register Ordering (from Ghidra FUN_0000f384 + FUN_000091f0)

The SPI controller in DMA mode issues DMA requests on CS assert.  If no DMA
channel is armed, the requests are lost and the transfer fails silently.

**Working order (firmware + fixed sram_sector_write.c):**
```
1. SPI: disable (EN=0, CS=0), DMAGO=0, configure (CTRL, LEN, DMAMD, CLK,
        DMACFG0=4, DMACFG1=3), enable (EN=1)
2. DMA: enable + clear ch, TRG=0 (clear stale), SRC/DST/NXT/CFG, TRG=arm
3. SPI: CS=1 (SPI starts clocking, issues DMA request → DMA responds)
4. SPI: CLRINT=0 (clear pending interrupt/status)
5. Poll DMA ISTAT for completion
6. Poll SPI STAT for idle
7. SPI: DMAGO=1 (signal complete), CS=0, EN=0
```

**Broken order (old sram_sector_write.c — why it failed):**
```
SPI config → EN=1 → CS=1 → DMA config → TRG  ← CS before DMA: requests lost!
```

**JTAG mww order (works due to ~1ms latency per write):**
```
SPI config → EN=1 → DMA config → CS=1 → TRG  ← CS after DMA config, before TRG
```
Works because DMA request stays pending during the 1ms between CS and TRG writes.

## Key Implications

1. **No PIO path to SPI flash exists in the firmware.** All flash I/O goes through
   DMA. The eCos `cyg_spi_transfer` was replaced by TCAT with `dma_transfer_execute`.

2. **0xCC PIO is unproven.** Same register IP block as 0xCF (which does PIO), but
   the flash controller may not have a PIO datapath. The spi_write_stub.s attempts
   PIO on 0xCC as a best-effort fallback.

3. **BP bits must be cleared before erase.** The SST25VF016B requires EWSR(0x50)
   before WRSR(0x01, 0x00). Standard WREN does NOT enable WRSR on this chip.
   The eCos sst25xx_unlock() uses WREN — it won't work.

4. **Primary recovery path: eCos DMA via sram_flash_restore.c.** Boot recovery
   firmware via SRAM patch, hook into eCos thread, call dma_transfer_execute
   directly for BP clear, then use sst25xx_sector_erase/aai_write.

5. **External SPI programmer is the most reliable fallback.** CH341A + SOIC-8 clip,
   hold SoC in reset via nRST, use flashrom.
