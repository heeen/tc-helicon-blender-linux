# DICE3 SPI Flash — driver, AAI, and the post-AAI verify shift bug

Single canonical source for the SPI flash subsystem and its history.
Supersedes:

- `spi_architecture.md` (Mar 24 — first SPI register table; outdated labels)
- `StockDmaAndSpi.md` (May 2 — Ghidra decompile + investigation log + fix)
- `flash-suggestions.md` (Apr 18 — exploratory hypotheses, mostly superseded)
- `repair_flash_hybrid.md` (Apr 16 — hybrid recovery path)

Hardware register layout for the SPI controller is in `hardware-reference.md`
(SPI controller register layout section). This file covers driver behaviour,
stock vs v2 differences, and the post-AAI verify shift bug.

Inline status tag convention: see `hardware-reference.md` header.

---

## 1. SPI IP overview

DICE3 has two instances of the same SPI IP block:

| Base | Use | Notes |
|---|---|---|
| 0xCC000000 | Flash SPI | Drives SST25VF016B (2 MB, 4 KB sectors). Used for firmware storage. |
| 0xCF000000 | LED SPI | Drives LED ring + bridges to TC-BLE module. |

The IP vendor is **unidentified** [conjecture: was called "Arasan SPI" in
older docs but no firmware string supports this; source paths point to
TCAT-internal SDK code]. See `hardware-reference.md` § "IP fingerprints".

Per-register layout (CTRL/LEN/EN/CS/CLK/CLRINT/STAT/DMAGO/ERR/DMAMD/
DMACFG0/DMACFG1/DATA/RX_PORT/TX_PORT) is in `hardware-reference.md`.

---

## 2. Stock eCos driver (Ghidra-decoded)

### `sst25xx_*` driver (Ghidra symbol prefix in primary firmware)

Located in eCos generic-flash framework (`flash.c` string at firmware
offset `0x22D2A`). Source path: `/home/tcat/tch-git/.../dice3-sdk/`.
[verified Ghidra string scan]

| Function | Address | Role |
|---|---|---|
| `sst25xx_init_legacy` | `0x8818` | RDID probe, device-table lookup |
| `sst25xx_read_id` | `0x8734` | Misnamed — actually FAST_READ (0x0B) data path |
| `sst25xx_addr_in_bank` | `0x862C` | Absolute → bank-local addr translation |
| `sst25xx_clear_block_protection` | `0x8660` | EWSR(0x50) + WRSR(0x01, 0x00) |
| `sst25xx_set_block_protection` | `0x86C4` | Same shape, sets BP bits |
| `sst25xx_aai_write` | `0x8968` | AAI word program (opcode 0xAD) |
| `sst25xx_sector_erase_banked` | `0x8B38` | SE (0x20) per 4 KB sector |

[verified 2026-05-03 Ghidra `search_functions`]

### Key stock idioms

**Split cmd/data transfers** — `sst25xx_read_id` (the FAST_READ path)
issues two `spi_resource_transfer()` calls per read: 5-byte cmd phase
(opcode + 24-bit addr + 1 dummy), then `tx_len` data phase. CS held by
the engine's mutex/scheduler-lock across both. [verified 2026-05-03
Ghidra decompile of `sst25xx_read_id @ 0x8734`]

**Bidir DMA mode (DMAMD=3) with dual channels** — stock flash uses
CH2 (RX) + CH3 (TX) on the generic 0x80 DMA engine. v2 driver uses
CH0/CH1 to avoid contention. [verified 2026-04-22 JTAG live state]

**No-IP-reset between transactions** — stock relies on its mutex
discipline, not register-level reset. The v2 driver adds aggressive
DMA channel clears + spi_reset_clean per chunk. [verified Ghidra
`spi_engine_queue_and_arm @ 0xFA10`]

### Bootloader's RX-only DMA pattern (separate idiom)

`dma_spi_read_setup @ 0x4F290` in stage-2 bootloader — see
`boot-and-init.md` § 3 for full sequence. Key: uses `CTRL=0x307`,
`DMAMD=1` (RX-only), DMA source = `0xCC000060` (DATA register, NOT
`0xCC000070` RX_PORT), and pushes cmd+addr via PIO writes to DATA
before kicking via CS. **This is a different read pattern from the
stock eCos driver's bidir mode** — both work, but they exercise
different IP datapaths. [verified 2026-05-02 Ghidra]

---

## 3. v2 SRAM flash driver

`firmware/sram_flash_driver_v2.c` — bare-metal driver loaded into SRAM
via JTAG, runs with IRQs masked. Used for full-image flashing from
TCAT-BOOT or eCos contexts.

### Layout (SRAM)

```
0x2B000  V2_DATA_BUF (single-sector source/dest, 4 KB)
0x2C000  Driver .text + .data + .bss (~9 KB region 0x2C000..0x2E3FF)
0x2E400  V2_MBOX (mailbox, 384 B)
0x2E580  V2_TIMINGS (128 B reserved)
0x2E600  V2_LOG_RING (1.5 KB)
0x29F00  TX_SCRATCH (16 B)  ← moved here from 0x2E100 [committed f2c4e73]
0x29F40  RX_SCRATCH (16 B)
0x28000  BIDIR_TX_BUF (flash read scratch, 8 KB)
0x2A000  RX_TEMP (8 KB)
0x2F100  V2_SECTOR_LIST (sector descriptors for bulk flash)
0x2F900  V2_IMAGE_BASE (bulk image payload)
```

[verified — see `sram_flash_driver_v2.ld` linker script]

### Channel allocation

- CH0 = RX (flash data into SRAM)
- CH1 = TX (cmd/addr/dummy bytes to SPI)

Differs from stock CH2/CH3. [verified — driver source]

### Mailbox interface

Host writes a command (`V2_CMD_*`) + parameters + magic; driver picks
up, executes, writes status. Commands include READ, ERASE, PROGRAM,
VERIFY, FLASH_ALL, FLASH_SECTOR, FLASH_BLOCK, BYTE_PATCH, READ_ID,
HASH_RANGE. See `sram_flash_mailbox_v2.h` for the full struct.

### Read paths

| Mode | Function | Pattern |
|---|---|---|
| `read_mode = 0` (bidir, default) | `dma_bidir_read` | DMAMD=3, single CS cycle, TX channel sends cmd+addr+dummies, RX captures all 4+len bytes through RX_PORT (0x70). **Has the +4B post-AAI shift bug** — fixed via dummy DMA (see § 5). |
| `read_mode = 1` (rxonly, opt-in) | `dma_rxonly_read` | Mirrors bootloader: DMAMD=1, CTRL=0x307, cmd via PIO DATA, RX-DMA sources from DATA (0x60). Bypasses bidir but suffers the same +4B shift after AAI [verified 2026-05-02]. Kept as opt-in for future investigation. |

---

## 4. The "+4B post-AAI verify shift" bug — full history

### Symptom

After AAI program, verify-read returns data shifted forward by exactly
4 bytes (one 32-bit FIFO word) at chunk-boundary offsets (`0x7fb` /
`0x3fc` for `READ_CHUNK = 0x800`). Wire-level SPI is byte-perfect
(logic analyzer 35/35 captures match). The corruption is in the SoC
SPI IP, not on the bus.

### Trigger conditions [verified 2026-05-02 isolation experiments]

| Pre-verify operation | Verify result |
|---|---|
| erase only | clean (no shift) |
| erase + BYTE_PRG (separate WREN/PROG/WRDI per byte) | **clean** |
| erase + AAI(2 B = 1 pair) | **clean** |
| erase + AAI(1024 B = 512 pairs) | **clean** |
| erase + AAI(2048 B = 1024 pairs) | shift bug appears |
| erase + AAI(4096 B = 2048 pairs) | shift bug, stronger |
| erase + 4 × AAI(1024 B) host-orchestrated | same as 1×4096 — bug |
| erase + 4 × AAI(1024 B) driver-internal (`AAI_MAX_RUN=0x400`) | same — bug |
| erase + AAI(4092) + BYTE_PRG(4) | bug (AAI bulk dominates) |

**Threshold: ~512–1024 AAI pairs cumulative** trigger the bug. Below
that, the IP stays clean. The accumulation **persists across separate
WREN/AAI_FIRST/WRDI runs** — neither host nor driver chunking helps.
Pure flash reads (no preceding AAI), JEDEC ID, RDSR, and BYTE_PRG-only
sequences are all clean.

### Fix (committed `2eba8e5` 2026-05-02)

A single short bidir CS cycle (8-byte JEDEC `dma_rx`) inserted at the
start of every `dma_bidir_read` completely eliminates the shift.

```c
{
    uint8_t dummy_buf[7];
    (void)dma_rx(0x9F, dummy_buf, 7, 5000);   // 8-byte single CS cycle
}
```

**Verification: 0/30 misses across 3 fresh sectors** vs the previous
5/5 → 30/30 baseline. ~12 µs overhead per chunk at 6 MHz wire,
negligible vs the chunk's ~25 ms.

### Threshold sweep on dummy size

| Dummy size | Result |
|---|---|
| no dummy (baseline) | 5/5, +4B shift |
| 4 bytes (1 cmd + 3-byte JEDEC) | 3/5, +2B shift |
| 8 bytes (2 × 4-byte cycles) | 2/5, +2B shift |
| **5–16 bytes single CS cycle** | **0/5 — clean** |

A single longer CS cycle works far better than multiple short ones — the
stuck word only clears once a single bidir cycle clocks ≥5 bytes.
Multiple short cycles each start fresh and don't fully consume the
state. [verified 2026-05-02]

### Mechanism [conjecture]

Best-current-hypothesis: AAI's pair stream parks the IP's RX FIFO
packer with one 32-bit word "stuck". The dummy bidir CS cycle reads
that stuck word out via dma_rx (output discarded), leaving the IP
clean for the next long DMA read. Multiple short cycles only partially
mitigate because the stuck state regenerates per CS cycle below a
threshold; one longer cycle consumes it. **Without the SPI IP datasheet
we cannot confirm.** [conjecture: stuck FIFO word; verified empirical:
dummy DMA fix]

### Distorted earlier diagnoses [outdated]

- Pre-2026-05-01 docs called this a "+2B half-word shift". That diagnosis
  was distorted by an unrelated TX_SCRATCH/.bss overlap bug (fixed in
  commit `f2c4e73`) which was silently corrupting RDSR responses to
  `0xef`, making AAI's busy-poll path run on bogus state and the
  verify-miss classifier misclassify +4B shifts as "+2B". [outdated
  2026-05-01: superseded by +4B word-drop finding once TX_SCRATCH was
  moved to 0x29F00]
- Pre-2026-05-02 docs blamed the bug on bidir DMA (DMAMD=3). The
  rxonly path mirroring the bootloader's pattern (DMAMD=1, DATA-register
  source) was tested and **shows the same shift after AAI**, confirming
  the bug isn't in the bidir TX/RX_PORT FIFO path specifically.
  [outdated 2026-05-02]
- `flash-suggestions.md` (Apr 18) attributed first-byte-FF and last-byte
  losses to AAI head/tail dropping pairs. **The wire is byte-perfect**
  per LA capture; the dummy 0xFF prefix has been removed from the v2
  driver. [outdated 2026-04-20: wire confirmed clean]
- `verify_quiesce_mode = STRICT` was originally added as a mitigation
  but its `peripheral_full_teardown` calls `blender_spi_ip_drain_rx` —
  the documented "read SPI_DATA with EN=0" pattern that *causes* FIFO
  state corruption. Now `OFF` by default. [outdated 2026-05-02 — see
  fix commit `2eba8e5`]

### Related dead-ends [outdated, kept as paper trail]

- **TX overrun fix** (TX count `0xFFF → 0xFFC` word-align, commit
  `6069fed`): partial mitigation, ~90% reduction in misses pre-fix.
  Still in code but largely superseded by the dummy-DMA fix.
- **In-driver verify-retry × 4** (commit `a91cb1b`): catches transient
  glitches, kept as belt-and-suspenders.
- **Mid-CS DMAMD switching (split-mode read)**: tested, doesn't work —
  IP requires CS edge to start a new transfer. [verified negative
  2026-05-01]
- **Driver-internal AAI chunking** (`AAI_MAX_RUN=0x400`): tested, doesn't
  help — bug accumulates across separate AAI runs. [verified negative
  2026-05-02]
- **RX FIFO drain** via SPI_DATA reads with EN=0: regresses to 17
  deterministic failures. [verified negative 2026-04-22]
- **DMA_CFG bits 28/29/30 forced as stock writes them**: regressed to
  7/20 failures; bits are HW runtime flags. [verified negative 2026-04-22]
- **CLK_DIV_SPI sweep + cpu-clock variations**: variance dominates;
  no clean clock-binding signal. [verified 2026-05-01]

### Production reliability

The dummy-DMA fix in `dma_bidir_read` plus the in-driver `do_verify`
4× retry plus the host `flash_block` 3× retry give effectively
zero-fail flash-all in normal use. The retries remain as
belt-and-suspenders.

---

## 5. Other notable bugs and fixes

### TX_SCRATCH/.bss overlap (fixed `f2c4e73`)

`TX_SCRATCH` was hardcoded at SRAM `0x2E100`, inside the linker-managed
driver region (`0x2C000–0x2E3FF`). Whenever `.bss` grew past `0x2E0FF`,
the compiler-emitted `.bss` zero-fill at startup silently clobbered
`TX_SCRATCH[0..3]` to `0x00` — sending opcode `0x00` to flash on every
`dma_rx` call. Symptom: JEDEC ID returned `0xffffff`, RDSR returned
bogus `0xef`. Fix: moved scratches to `0x29F00`/`0x29F40`. [verified
2026-05-01 commit `f2c4e73`]

### AAI tail-drop quirk

Long AAI runs (≥32 KB historically) silently drop the last 2 pairs on
this part. Compromise in v2 driver: split each run as bulk minus final
4 bytes, then a separate tiny 4-byte AAI run for those last 4 bytes.
Total overhead: ~100 µs per big run. [verified 2026-04-18; mitigation
in `do_aai_program @ 0x853` of `sram_flash_driver_v2.c`]. The exact
drop count varies in tests (saw 12 bytes from end in one test, 2 in
another) — pattern is more nuanced than "last N pairs". [conjecture: drop
count varies with AAI length; no clean predictor]

### Off-by-4 readback (fixed `a38a50b`)

`READ_RX_SKIP` was historically 8 (carryover from a different IP
assumption). With cmd+3-byte addr = 4 bytes, the correct skip is
4 — not 8. Wrong value shifted readback by -4 bytes and masqueraded
as a "BYTE_PROGRAM off-by-4" silicon quirk. LA capture confirmed
wire is byte-perfect. [verified 2026-04-20 LA capture; fixed in
`a38a50b`]

### BP-clear requires EWSR not WREN

The SST25VF016B requires `EWSR (0x50)` before `WRSR (0x01, 0x00)`.
Standard `WREN (0x06)` does NOT enable WRSR on this chip — the eCos
`sst25xx_unlock()` uses WREN and won't clear BP bits without the
EWSR fix. [verified 2026-03-21 + Ghidra]

---

## 6. Hybrid JTAG repair playbook

Verified recovery path 2026-04-16 — combines two transports:

- `sram_flash_driver.bin` (v1 mailbox driver) for: BP clear, sector
  erase, sector readback / verify
- Host-driven JTAG register writes for: byte program (WREN +
  5-byte DMA TX byte-program)

Validated on real hardware by repairing SPI sector `0x0000` and then
doing a full power cycle. The device booted cleanly afterward.

```sh
python3 firmware/repair_flash_hybrid.py --ref firmware/blender_spi_patched.bin --sector 0x0
# or
make -C firmware/patch repair-hybrid REPAIR_SECTOR=0x40000
```

**Important**: Slow (host-driven byte programming). A successful verify
is not enough by itself. Do a **full power cycle** after repair before
deciding whether boot is fixed. [verified 2026-04-16]

The pure mailbox-driver write path is unreliable in TCAT-BOOT context.
The older standalone on-device writer also proved unreliable. The
hybrid split is the first path that verified correct flash contents
and actually recovered a booting unit.

---

## 7. Open levers (not pursued)

- **DMA channels CH2/CH3** — match stock eCos. If bug is channel-specific
  FIFO behaviour, swap might expose it. Not tested.
- **DMAMD = 4** — unknown stock mode 4 (CTRL=0x300). Not tested.
- **Increased TX preamble padding** (READ_RX_SKIP from 4 to 8 to keep
  packer fully word-aligned) — would need address+len math adjustment.
  Not tested.

The Arasan datasheet would be the unlock — except the IP is **not
verified to be Arasan**. The actual IP vendor is unknown
[conjecture: TCAT-internal based on source-path strings].
