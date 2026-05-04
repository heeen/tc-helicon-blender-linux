# Stock stage-2 bootloader — DMA/SPI/flash read collection

Program: `dice3_bootloader.bin` (image base `0x4F000`).
Captured: 2026-04-21.

Context: this bootloader runs between TCAT-BOOT ROM and the eCos primary.
It initialises UART/GPIO, reads the firmware header(s) from SPI flash,
DMA-reads the body into SRAM while CRC-checking as it goes, then jumps to
the firmware entry stub. Purely polled — no eCos, no interrupts, no mutex.
21 functions total; 13 touch SPI/DMA/flash directly.

The bootloader uses a single DMA-engine channel (CH0) to burst flash payload
into SRAM and polls `DMA_ISTAT` in a tight loop. It never touches the SPI FIFO
directly except for the 4-byte `READ`+address preamble — the DMA engine drains
the rest.

## Constants referenced

| Symbol | Value | Meaning |
| --- | --- | --- |
| `spi_flash_read_cmd` (`0x4f320`) | `0x00000307` | SPI_FLASH_CMD: 1-byte cmd + 3-byte addr, TX direction, READ |
| `led_spi_jedec_cmd`  (`0x4f224`) | `0x00000287` | LED/aux SPI: 1-byte cmd + 2-byte read (JEDEC RDID 0x9F path) |
| `led_spi_write_cmd`  (`0x4f288`) | `0x00000107` | LED/aux SPI: 1-byte cmd + multi-byte write |
| `dma_ch0_xfer_idle` (`0x4f19c`) | `0x88009000` | DMA_CH0_XFER after reset (no length, size=word, periph src) |
| `dma_ch0_enable_config` (`0x4f328`) | `0x0000D006` | DMA_CH0_CFG pre-enable value (flow, burst, dst-inc, size=word) |
| `ptr_spi_dma_rx_fifo` (`0x4f324`) | `0xCC000060` | SPI_FLASH_DATA FIFO address (DMA src) |
| `uart_baud_rate` (`0x4f710`) | `0x0001C200` (115200) | UART baud |
| `uart_line_config` (`0x4f71c`) | `0x00000301` | UART_CTRL |
| `golden_body_spi_addr` (`0x4f72c`) | `0x00010030` | Golden firmware body start (header is at 0x10000) |
| `fw_header_magic` | matches `header_magic` field | Magic word at offset 0 of a firmware header |

SPI flash clock divider set to **`SPI_FLASH_CLK_DIV = 2`** (fast path).
LED/aux SPI divider set to **`SPI_LED_CLK_DIV = 0xFF`** (slow).

---

## Functions that touch DMA/SPI/flash

### `bootloader_init` @ `0x4f014`

```c
void bootloader_init(void)
{
  undefined4 *puVar1;
  int iVar2;
  undefined4 *puVar3;
  uint unaff_r4;
  bool bVar4;

  puVar1 = ptr_bss_end;
  for (puVar3 = ptr_bss_start; bVar4 = puVar3 == puVar1, (int)puVar3 < (int)puVar1;
      puVar3 = puVar3 + 1) {
    *puVar3 = 0;
  }
  iVar2 = (*ptr_boot_check_and_load)();
  if (!bVar4) {
    halt_baddata();
  }
  (*(code *)(unaff_r4 & iVar2 << 0x1a))();
  return;
}
```

Entry: zeros BSS then jumps to `boot_check_and_load`. Trailing code is a
mis-recovered jumptable; real flow never returns.

---

### `uart_wait_tx_ready` @ `0x4f050`

```c
void uart_wait_tx_ready(void)
{
  uint uVar1;
  do { uVar1 = UART_STATUS; } while ((uVar1 & 8) != 0);
  return;
}
```

Polls UART_STATUS bit 3 for TX-idle.

---

### `uart_puts` @ `0x4f064`

```c
void uart_puts(char *str)
{
  uint uVar1;
  while (true) {
    if ((byte)*str == 0) break;
    do { uVar1 = UART_STATUS; } while ((uVar1 & 0x20) != 0);
    UART_TX_DATA = (uint)(byte)*str;
    str = str + 1;
  }
  return;
}
```

Polling TX on `UART_STATUS` bit 5 (FIFO-full).

---

### `gpio_configure` @ `0x4f090`

```c
void gpio_configure(uint config)
{
  int iVar1;
  uint uVar2, uVar3, uVar4;

  uVar3 = config >> 1 & 0xc;
  iVar1 = (config & 7) << 2;
  uVar4 = config >> 5 & 0xf;
  *(uint *)(&CLK_PIN_MUX_0 + uVar3) =
       *(uint *)(&CLK_PIN_MUX_0 + uVar3) & ~(0xf << iVar1) | uVar4 << iVar1;

  uVar3 = config >> 0x11 & 0xf;
  if (uVar3 != 0) {
    uVar2 = GPIO_DIR;
    if (uVar4 == 8) { uVar3 = uVar3 | uVar2; }
    else            { uVar3 = uVar2 & ~uVar3; }
    GPIO_DIR = uVar3;
  }
  if ((config >> 9 & 0xf) != 8) {
    uVar3 = GPIO_ALT_FUNC;
    iVar1 = (config >> 9 & 7) << 2;
    GPIO_ALT_FUNC = uVar3 & ~(0xf << iVar1) | (config >> 0xd & 0xf) << iVar1;
    return;
  }
  return;
}
```

Packed-bitfield GPIO/pinmux programmer used 4× at early init for UART/SPI
pins. Not DMA-related, but establishes the SPI pinmux before the first
flash read.

---

### `dma_poll_complete` @ `0x4f120`

```c
int dma_poll_complete(void)
{
  uint uVar1;
  int iVar2;

  uVar1 = DMA_ISTAT;
  if ((uVar1 & 1) == 0) {
    (*_ROM_CRC_FUNC_PTR)(ptr_rom_uart_puts,8);   // TODO: actually a nop/sleep stub via ROM
    iVar2 = 0;
  } else {
    DMA_ICLR = 1;                                // clear CH0 done
    iVar2 = 1;
    DMA_CH0_XFER = dma_ch0_xfer_idle;            // = 0x88009000
    uVar1 = SPI_FLASH_ERROR;
    if ((uVar1 & 4) != 0) {
      *ptr_spi_error_count = *ptr_spi_error_count + 1;
    }
    SPI_FLASH_ENABLE = 0;                        // deassert CS
  }
  return iVar2;
}
```

One iteration of the DMA-done poll. Returns 0 while busy, 1 when channel 0
done bit is latched. On done it clears the IRQ, parks CH0 with the idle
XFER word `0x88009000`, reads and (conditionally) counts SPI_FLASH_ERROR
bit 2, and releases CS by clearing `SPI_FLASH_ENABLE`.

**Key:** the bootloader does **not** drain the SPI FIFO at all; it relies on
DMA CH0 having already consumed every word before `DMA_ISTAT & 1` latches.

---

### `led_spi_read_jedec` @ `0x4f1a4`

```c
void led_spi_read_jedec(void)
{
  uint uVar1;
  undefined4 uVar2;
  int iVar3, iVar4;

  iVar3 = ptr_led_jedec_buf;
  SPI_LED_CMD    = led_spi_jedec_cmd;   // 0x287
  SPI_LED_SIZE   = 2;
  SPI_LED_ENABLE = 1;
  SPI_LED_START  = 4;
  PERIPH_CB_CTRL = 8;
  iVar4 = 0;
  SPI_LED_DATA   = 0;
  do {
    do { uVar1 = SPI_LED_STATUS; } while ((uVar1 & 8) == 0);   // RX ready
    uVar2 = SPI_LED_DATA;
    *(char *)(iVar4 + iVar3) = (char)uVar2;
    iVar4 = iVar4 + 1;
  } while (iVar4 != 3);
  do { uVar1 = SPI_LED_STATUS; } while ((uVar1 & 1) != 0);     // busy
  SPI_LED_ENABLE = 0;
  PERIPH_CB_CTRL = 0;
  return;
}
```

Polled RDID-like probe on the **LED SPI bus** (`0xCF000000`), not flash.
Used early for board-ID / LED-ring presence. Pure PIO, no DMA.

---

### `led_spi_wait_idle` @ `0x4f200`

```c
void led_spi_wait_idle(void)
{
  uint uVar1;
  do { uVar1 = SPI_LED_STATUS; } while ((uVar1 & 1) != 0);
  SPI_LED_ENABLE = 0;
  PERIPH_CB_CTRL = 0;
  return;
}
```

Trailer for LED SPI ops.

---

### `led_spi_write` @ `0x4f22c`

```c
void led_spi_write(void)
{
  byte *pbVar1;
  uint uVar2;
  int iVar3;

  SPI_LED_CMD    = led_spi_write_cmd;  // 0x107
  SPI_LED_ENABLE = 1;
  SPI_LED_START  = 1;
  iVar3 = 0;
  do {
    do { uVar2 = SPI_LED_STATUS; } while ((uVar2 & 2) == 0);   // TX fifo space
    pbVar1 = (byte *)(iVar3 + ptr_led_write_buf);
    iVar3 = iVar3 + 1;
    SPI_LED_DATA = (uint)*pbVar1;
  } while (iVar3 != 10);
  do { uVar2 = SPI_LED_STATUS; } while ((uVar2 & 1) != 0);
  SPI_LED_ENABLE = 0;
  return;
}
```

Pushes a fixed 10-byte LED-ring frame (status bits → LEDs) with polled TX.
Not flash-related; kept for completeness because it's on the same SPI IP.

---

### `dma_spi_read_setup` @ `0x4f290`  (the critical function)

```c
void dma_spi_read_setup(uint spi_addr, void *dest, uint length)
{
  uint uVar1;

  SPI_FLASH_CMD      = spi_flash_read_cmd;          // 0x307
  SPI_FLASH_SIZE     = length - 1;
  SPI_FLASH_DMAMD    = 1;                            // DMA mode
  SPI_FLASH_ENABLE   = 1;                            // CS asserted
  SPI_FLASH_DMA_CTRL = 3;                            // arm DMA request line

  DMA_ICLR       = 1;                                // clear CH0 done
  DMA_SYNC       = 1;                                // sync
  DMA_CH0_SRC    = ptr_spi_dma_rx_fifo;              // 0xCC000060
  DMA_CH0_DST    = dest;
  DMA_CH0_XFER   = length & 0xfff | 0x88009000;      // count | flags
  DMA_CH0_CFG    = dma_ch0_enable_config;            // 0x0000D006
  uVar1          = DMA_CH0_CFG;                      // read-back
  DMA_CH0_CFG    = uVar1 | 1;                        // start (bit 0)

  SPI_FLASH_DATA = 3;                                 // READ opcode 0x03
  SPI_FLASH_DATA = spi_addr >> 0x10;
  SPI_FLASH_DATA = spi_addr >> 8;
  SPI_FLASH_DATA = spi_addr;
  SPI_FLASH_START = 1;
  return;
}
```

Arms a single-shot DMA receive of up to `length` bytes from the SPI flash
FIFO into `dest`.

Wire sequence programmed:

1. Configure SPI engine: `CMD=0x307` (READ, 1-byte cmd + 3-byte addr, TX),
   `SIZE=length-1`, `DMAMD=1`, `ENABLE=1`, `DMA_CTRL=3`.
2. Clear/resync DMA, program CH0 src/dst/xfer/cfg.
3. Set `CH0_XFER = (length & 0xFFF) | 0x88009000` — bits 27 = enable, 15 = periph-src, 12 = word size.
4. Write `CH0_CFG = 0xD006` then read-back then OR `|1` to start.
5. Push READ opcode (`0x03`), then 3 address bytes MSB-first, into
   `SPI_FLASH_DATA`. Asserting `SPI_FLASH_START=1` kicks the engine.
6. No explicit FIFO reset or IP reset beforehand. CS is left asserted;
   `dma_poll_complete` drops it after completion.

**Compare primary firmware**: uses DMA CFG **0x1006** for RX and **0x0880**
for TX, with a FIFO-drain/IP-reset preamble. Bootloader uses CFG
**0xD006** for RX (flow-control bit different?) and no reset.

Note the 12-bit length field (`length & 0xFFF`). The caller chunks at
0x800 (2048) precisely so this mask can never truncate.

---

### `spi_dma_read_and_crc` @ `0x4f32c`

```c
uint spi_dma_read_and_crc(uint spi_addr, uint length)
{
  int iVar1;
  code *pcVar2, *dest;
  uint uVar3, length_00;

  pcVar2 = firmware_entry_stub;
  uVar3  = 0;
  dest   = firmware_entry_stub;
  for (; length != 0; length = length - length_00) {
    length_00 = length;
    if (0x7ff < length) length_00 = 0x800;        // cap chunk at 2048 bytes
    dma_spi_read_setup(spi_addr, dest, length_00);
    while (iVar1 = dma_poll_complete(), iVar1 == 0) {
      if (pcVar2 < dest) {
        uVar3 = (*_ROM_CRC_COMPUTE)(uVar3, pcVar2, 0x100);   // 256-byte CRC step
        pcVar2 = pcVar2 + 0x100;
      }
    }
    spi_addr = spi_addr + length_00;
    dest     = dest     + length_00;
  }
  if (dest != pcVar2) {
    uVar3 = (*_ROM_CRC_COMPUTE)(uVar3, pcVar2);           // tail
  }
  return uVar3;
}
```

Load-and-verify loop. DMAs the firmware body from `spi_addr` into SRAM at
`firmware_entry_stub` in **2 KiB** chunks, CRC'ing 256-byte slices of
already-landed data **while** DMA is in flight (so CRC cost is hidden).
Returns the final CRC.

Destination is fixed at `firmware_entry_stub` (SRAM load address).

---

### `spi_read_header` @ `0x4f3e4`

```c
void spi_read_header(uint spi_addr, void *dest)
{
  int iVar1;
  dma_spi_read_setup(spi_addr, dest, 0x30);
  do { iVar1 = dma_poll_complete(); } while (iVar1 == 0);
  return;
}
```

48-byte DMA pull of the firmware header (magic, size, sig bytes, boot
order flag, etc.) into a caller stack buffer. Same DMA path as the body.

---

### `boot_check_and_load` @ `0x4f404`

```c
void boot_check_and_load(void)
{
  int iVar1, iVar2;
  byte *pbVar3;
  char *str;
  uint uVar4, uVar5, uVar6;
  int header_magic;
  uint body_size_words;
  char sig_byte_0, sig_byte_1, boot_order_flag;

  iVar2 = ptr_bootloader_config;
  iVar1 = cpu_clock_hz;
  *(int *)(ptr_bootloader_config + 8) = cpu_clock_hz;

  uVar4 = GPIO_STATUS;
  if ((uVar4 & 0x8000) != 0) {                        // PLL-measured clock path
    uVar6 = GPIO_DATA;
    uVar4 = uVar4 & 0xf;
    if (uVar4 == 0) uVar4 = 1;
    uVar5 = uVar6 >> 8 & 0xf;
    if (uVar5 == 0) uVar5 = 1;
    uVar4 = udiv32(iVar1 * (uVar6 & 0xff), uVar5 * uVar4);
    *(uint *)(iVar2 + 8) = uVar4 >> 1;
  }

  _DAT_cb000400 = 8;
  PERIPH_CB_CTRL = 0;
  gpio_configure(gpio_config_0);
  gpio_configure(gpio_config_1);
  gpio_configure(gpio_config_2);
  gpio_configure(gpio_config_3);

  iVar1 = ptr_bootloader_config;
  UART_LINE_CTRL = 0x60;
  uVar4 = udiv32(*(uint *)(ptr_bootloader_config + 8), uart_baud_rate);  // /115200
  UART_BAUD_INT  = uVar4 >> 6;
  UART_BAUD_FRAC = uVar4 & 0x3f;
  UART_LINE_CTRL = 0x70;
  UART_CTRL      = uart_line_config;
  UART_RX_DATA   = 0;

  SPI_FLASH_CLK_DIV = 2;          // *** flash bus: /2 divider ***
  SPI_LED_CLK_DIV   = 0xff;       // LED bus: /255 divider

  led_spi_write();
  led_spi_read_jedec();
  led_spi_read_jedec();

  if ((*(byte *)(iVar1 + 5) & 9) == 0) goto LAB_golden_only;

  spi_read_header(0x110000, &header_magic);
  if (boot_order_flag == 'Z') {
    uVar4 = 0x40000;  uVar6 = 0x110000;     // image A at 0x110000, fallback 0x40000
  } else {
    spi_read_header(0x40000, &header_magic);
    uVar4 = 0x110000; uVar6 = 0x40000;       // image B at 0x40000, fallback 0x110000
  }

  pbVar3 = ptr_led_state_byte;
  *ptr_led_state_byte = *ptr_led_state_byte ^ 2;
  led_spi_write();

  if ((sig_byte_0 == 'Z') && (sig_byte_1 == 'Z')) {
    str = ptr_str_corrupt_header;
    if (((header_magic == fw_header_magic) && (body_size_words < 0x34000)) &&
       (uVar4 = spi_dma_read_and_crc(uVar6 + 0x30, body_size_words << 2),
        str = ptr_str_corrupt_header, uVar4 == 0)) {
      uart_wait_tx_ready();
      fw_spi_base_addr = uVar6;
LAB_jump_firmware:
      fw_boot_flags_0 = *(undefined1 *)(ptr_bootloader_config + 5);
      fw_boot_flags_1 = *(undefined1 *)(ptr_bootloader_config + 6);
      firmware_entry_stub();        // *** hand off to primary ***
      str = ptr_str_corrupt_header;
    }
  } else {
    *pbVar3 = *pbVar3 ^ 4;
    led_spi_write();
    spi_read_header(uVar4, &header_magic);
    str = ptr_str_no_valid_app;
    if ((sig_byte_0 == 'Z') &&
       ((((sig_byte_1 == 'Z' && (str = ptr_str_corrupt_header,
                                 header_magic == fw_header_magic)) &&
         (body_size_words < 0x34000)) &&
        (uVar6 = spi_dma_read_and_crc(uVar4 + 0x30, body_size_words << 2),
         str = ptr_str_corrupt_header, uVar6 == 0)))) {
      uart_wait_tx_ready();
      fw_spi_base_addr = uVar4;
      goto LAB_jump_firmware;
    }
  }
  uart_puts(str);

LAB_golden_only:
  uart_puts(ptr_str_try_golden);
  *ptr_led_state_byte = *ptr_led_state_byte ^ 8;
  led_spi_write();
  spi_read_header(0x10000, &header_magic);
  if (((header_magic == fw_header_magic) && (body_size_words < 0xc000)) &&
     (uVar4 = spi_dma_read_and_crc(golden_body_spi_addr, body_size_words << 2),
      uVar4 == 0)) {
    uart_wait_tx_ready();
    fw_spi_base_addr = 0x10000;
    fw_boot_flags_0 = *(undefined1 *)(ptr_bootloader_config + 5);
    fw_boot_flags_1 = *(undefined1 *)(ptr_bootloader_config + 6);
    firmware_entry_stub();
  }
  *ptr_led_state_byte = 0;
  led_spi_write();
  uart_puts(ptr_str_none_found);
  uart_puts(ptr_str_separator);
  uart_wait_tx_ready();
  (*ptr_rom_uart_loader)();           // fall back to ROM XMODEM
  do { } while (true);
}
```

The whole boot orchestration. Order of flash attempts:

1. Primary image A @ SPI `0x110000`, fallback image B @ `0x40000`
   (or vice-versa depending on `boot_order_flag`).
2. Golden image @ SPI `0x10000`.
3. ROM UART loader (XMODEM prompt).

Each image: 48-byte header via `spi_read_header`, then
`body_size_words * 4` bytes via `spi_dma_read_and_crc`. On CRC success
it jumps to `firmware_entry_stub` in SRAM.

Size ceilings: primary 0x34000 words (832 KiB), golden 0xC000 words
(192 KiB).

---

### `udiv32` @ `0x4f73c`

Software 32-bit unsigned division (ARM9 has no UDIV). Used by the UART
baud calculation and the optional PLL-from-GPIO clock measurement.
Not flash-related but referenced from the SPI init path so kept.

```c
uint udiv32(uint dividend, uint divisor);   // full body elided (see Ghidra)
```

---

## Comparison with primary firmware

- **Same SPI IP, different DMA CFG word.** Bootloader writes
  `DMA_CH0_CFG = 0xD006` for RX. Primary firmware uses `0x1006` (RX) and
  `0x0880` (TX). Bit 15 (0x8000) in the bootloader config is the likely
  source of the robustness gap — possibly enables hardware flow-control
  / back-pressure that the primary firmware's driver disables. Worth
  isolating: copy `0xD006` into the primary RX path and see if byte drops
  vanish.
- **Same SPI_FLASH_CMD encoding.** Both use `0x307` (1-byte cmd + 3-byte
  addr, TX) and opcode `0x03` (slow READ). Neither uses `0x0B`
  FAST_READ. Clock divider on the bootloader is **2** vs the primary's
  runtime 8 — bootloader actually runs the bus hotter.
- **No FIFO-drain / IP-reset preamble in the bootloader.** It just sets
  `SPI_FLASH_ENABLE=1; SPI_FLASH_DMA_CTRL=3` and rearms DMA every chunk.
  After completion, `dma_poll_complete` drops `SPI_FLASH_ENABLE` (CS
  release) which presumably flushes any residual FIFO state before the
  next `dma_spi_read_setup` re-enables.
- **Single-shot DMA, no chained descriptors.** Chunks at 2 KiB inside a
  software loop. The 12-bit XFER length field (`length & 0xFFF`) forces
  the 2 KiB cap. Matches the primary firmware's chunking strategy.
- **Polling, no interrupts.** `DMA_ISTAT & 1` in a busy loop; CRC work is
  interleaved into the poll loop so the CPU is not idle. The bare-metal
  v2 driver does the same, but without the CS-drop/FIFO-release step
  between chunks — that gap is where drops may creep in.
