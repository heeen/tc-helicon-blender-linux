# Stock eCos DMA + SPI — annotated decompile

Source: `blender_primary_body.bin` (primary firmware body, 174 KB) via
Ghidra. Function addresses are in the primary-body image (base 0x200).

This document decompiles the stock eCos routines that drive the DMA
engine and flash/LED SPI IP, renders them into human-readable C, and
calls out the details that matter for our v2 bare-metal flash driver.

See also `Registers.md` for the live peripheral-state comparison that
motivated this analysis.

---

## 1. Peripheral register map (inferred from stock writes)

### DMA engine — `0x80000000`

Probed live on 2026-04-22. **The engine has 8 channels** (CH0–CH7), not
4 as `dice3_hw.h` implies.

| offset | role (stock semantics) | stock writes | notes |
|---:|---|---|---|
| +0x04 | **STATUS / ISTAT (read)** | — | Per-channel "transfer done" bits, iterable over `value & 0xFF`. DSR reads this. Our dice3_hw.h called `+0x14` "DMA_ISTAT"; both reflect similar state but stock's DSR always reads `+0x04`. |
| +0x08 | **DMA_EN (write)** | `1<<ch` per arm; `0xFF` at init | Per-channel enable. Writes are bit-set (other bits unchanged). |
| +0x10 | **DMA_ICLR (write)** | `1<<ch` per arm; `0xFF` at init | Per-channel IRQ/latch clear. Stock uses a **narrow per-channel mask** during normal ops, `0xFF` only at `dma_irq_init`. |
| +0x30 | — | `1` at init | Some enable-latch byte. |
| +0x100 + ch*0x20 | CH`ch` SRC, DST, ?, CNT, GO | — | Per-channel descriptors, 5 words each. |
| +0x180..+0x1FF | **CH4..CH7** | — | Previously unmapped in our code. **CH4+CH5 are eCos's LED-SPI DMA channels, actively running when we take over.** |

Active live state (eCos running patched firmware, idle):
- `CH0`: SRC=`0xCC000060` → DST=`0x34037` (flash, stage-2 residue)
- `CH2`: SRC=`0xCC000070` → DST=`0x2F211` (flash RX_PORT — stock flash driver's RX channel)
- `CH3`: SRC=`0x2F1F1` → DST=`0xCC000080` (flash TX_PORT — stock flash driver's TX channel)
- `CH4`: SRC=`0xCF000070` → DST=`0x297EA` (**LED SPI RX_PORT**)
- `CH5`: SRC=`0x297ED` → DST=`0xCF000080` (**LED SPI TX_PORT**)
- `CH1, CH6, CH7`: unused/zero

### Flash SPI IP — `0xCC000000`

Offsets addressed as `puVar6[n] = *(uint32_t *)(base + n*4)`:

| puVar6[n] | offset | role (inferred) | stock values |
|---:|---:|---|---|
| [0] | 0x00 | **CTRL** | mode-dependent (see §3) |
| [1] | 0x04 | LEN | `xfer_len - 1` |
| [2] | 0x08 | EN | `0` then `1` to start |
| [3] | 0x0C | — | sometimes `0` |
| [4] | 0x10 | CS | `1 << slave_id` to assert |
| [5] | 0x14 | CLK | divider, default `2` |
| [6] | 0x18 | — | cleared at arm time |
| [0xB] | 0x2C | — | cleared at arm time (maybe CS-pre-setup) |
| [0x10] | 0x40 | — | |
| [0x13] | 0x4C | **DMAMD** | 1=RX, 2=TX, 3=BIDIR |
| [0x14] | 0x50 | DMACFG0 | **always 4** |
| [0x15] | 0x54 | DMACFG1 | **always 3** |
| [0x18] | 0x60 | DATA | zeroed at start for RX-only |

Our `dice3_hw.h` swapped DMACFG0↔DMACFG1 labels vs. the stock values,
but the net writes match (we write `DMACFG0=4, DMACFG1=3` → `+0x50=4,
+0x54=3`, same as stock).

---

## 2. `dma_irq_init` @ 0x95b0 — full-engine reset + ISR hookup

```c
void dma_irq_init(void) {
    DMA[+0x30] = 1;           // engine master latch
    DMA_EN     = 0xFF;        // enable all 8 channels
    DMA_ICLR   = 0xFF;        // clear ALL 8 channels' IRQ flags
    cyg_interrupt_create(/*vec=*/10, /*pri=*/0, &dma_ctx,
                         &dma_isr, &dma_dsr, ...);
    hal_interrupt_attach_isr(dma_ctx.isr_ptr);
    hal_interrupt_unmask(10);   // VIC vec 10 = DMA completion
}
```

**Key takeaway:** at one-time init stock blasts `0xFF` into both
DMA_EN and DMA_ICLR. Our `peripheral_full_teardown → dma_engine_reset`
now also writes `0xFF` (was `0x0F`, which left latched status in the
upper 4 channels — bit 3 of `DMA_ISTAT` is what our VERIFY_MISS
instrumentation catches).

---

## 3. `dma_channel_arm` @ 0x9620 — per-channel arming

```c
// param_1 = channel index
// param_2 = pointer to channel descriptor struct
void dma_channel_arm(uint ch, uint *desc) {
    DMA_EN   |= 1 << ch;        // set only this channel's enable bit
    DMA_ICLR  = 1 << ch;        // clear only this channel's IRQ flag
    dma_channel_ptr_table[ch] = desc;

    volatile uint32_t *c = CH(ch);  // 0x80000100 + ch*0x20
    c[4]  = 0;                   // "GO" cleared first
    c[0]  = desc.src;
    c[1]  = desc.dst;
    c[2]  = desc.flags;          // 0x70009000 for DMA byte-count tx
    c[3]  = desc.next;
    c[4]  = desc.cfg | 0x8001;   // GO with bit 15 = start, bit 0 = active
}
```

**Key takeaway:** stock clears `DMA_ICLR` with a *narrow* `1 << ch`
mask per arming, not a wide `0xFF`. Our hot-path widening to `0xF`
(and now `0xFF` in init) was a reaction to intermittent latching; the
correct match to stock would be `1 << 0 | 1 << 1` (`0x3`) per bidir
arm. Tried, regressed — something else relies on the broader clear.

---

## 4. `dma_dsr` @ 0x951c — completion dispatch

```c
// Deferred Service Routine — runs with IRQs re-enabled after dma_isr.
void dma_dsr(int vec) {
    uint stat = DMA[+0x04];          // read 8-bit status
    DMA_EN    = stat;                // re-enable any channel that just
                                     // completed (keeps it listening
                                     // for the next burst)
    for (uint bits = stat & 0xFF, ch = 0;
         bits != 0;
         bits >>= 1, ch += 1) {
        if ((bits & 1) == 0) continue;
        cb_entry = dma_channel_cb_table[ch];
        if (cb_entry && cb_entry->done_fn) {
            cb_entry->done_fn(cb_entry, shared_arg);
        }
    }
    hal_interrupt_unmask(vec);
}
```

**Key takeaway:** stock **never writes `DMA_ICLR` in the DSR**. The
channel-done bits self-clear on the next arm (because the `1<<ch` write
to `DMA_ICLR` in `dma_channel_arm` clears just that channel). Our
driver writes `DMA_ICLR = 0xFF` after every transfer — redundant with
stock but not harmful.

The done-callback for flash RX is `spi_dma_rx_done_cb`
(sets a `done_flag` then signals a cyg_cond). TX's is
`spi_dma_tx_done_cb` — same shape, different offset.

---

## 5. `spi_engine_queue_and_arm` @ 0xfa10 — the BIDIR hot path

This is the main "send this data, fill this buffer" routine, also
embedded in `dma_engine_commit_and_wait`. Condensed:

```c
void spi_engine_queue_and_arm(engine_t *eng, /*...*/ uint dir,
                              uint8_t *tx_buf, uint8_t *rx_buf,
                              uint len, bool arm_now) {
    ctx = eng->ctx;
    // Build DMA descriptors for TX side (chained bursts of 4 KB)
    for (remaining = len, i = 0; remaining > 0; remaining -= chunk, i++) {
        chunk = min(remaining, 4096);
        ctx->tx_desc[i] = {
            .src   = tx_buf || ctx->tx_scratch,
            .dst   = spi_base + 0x80,       // TX_PORT
            .next  = &ctx->tx_desc[i+1].data,
            .cfg   = chunk | 0x70009000,    // DMA mode flags
        };
        if (tx_buf) {
            ctx->tx_desc[i].cfg |= 0x4000000;  // source-increment
            tx_buf += chunk;
        }
    }
    // Similar loop for RX descriptor chain (SRC=+0x70 RX_PORT).

    if (!arm_now) return;

    // Arm time — program the SPI IP.
    SPI = ctx->spi;
    while (SPI[0x0A] & 1) {}          // wait for BUSY clear
    SPI[ 2] = 0;                       // EN = 0
    SPI[ 4] = 0;                       // CS = 0
    SPI[0xB] = 0;                      // ??
    SPI[ 1] = ctx->xfer_len - 1;       // LEN
    SPI[ 5] = ctx->clk_div;            // CLK
    SPI[0x14] = 4;                     // DMACFG0
    SPI[0x15] = 3;                     // DMACFG1

    uint ctrl_bits = (dir == 1 ? 0x200 :    // RX-only
                      dir == 3 ? 0x000 :    // BIDIR  (!)
                      dir == 4 ? 0x300 :
                                 0x100);    // TX-only (0,2)
    SPI[0] = ctrl_bits | ctx->ctrl_base;    // CTRL
    SPI[2] = 1;                              // EN

    // Mark last descriptors as "end-of-chain" (bit 31 set).
    ctx->tx_desc[last].cfg |= 0x80000000;
    ctx->rx_desc[last].cfg |= 0x80000000;
    ctx->tx_desc[last].next = 0;
    ctx->rx_desc[last].next = 0;

    SPI[0x13] = dir;                   // DMAMD = 1|2|3|4

    switch (dir) {
      case 0: case 2:     /* TX-only */
        dma_channel_arm(ctx->tx_chan, &ctx->tx_desc_head);
        break;
      case 1:             /* RX-only */
        dma_channel_arm(ctx->rx_chan, &ctx->rx_desc_head);
        break;
      case 3: case 4:     /* BIDIR */
        dma_channel_arm(ctx->tx_chan, &ctx->tx_desc_head);
        dma_channel_arm(ctx->rx_chan, &ctx->rx_desc_head);
        break;
    }

    cyg_mutex_lock(&eng->busy_mutex);
    cyg_scheduler_lock();
    SPI[4] = 1 << ctx->slave_id;       // CS = assert slave
    SPI[6] = 0;                         // "go" pulse?

    while (!(tx_done && rx_done && epilogue_done)) {
        cyg_flag_timed_wait(&eng->done_flag);
    }
    cyg_scheduler_unlock();
    cyg_mutex_unlock(&eng->busy_mutex);
}
```

**Key takeaways vs. our driver:**

1. **Direction ordering for BIDIR**: stock arms TX channel first,
   then RX channel. Our `dma_bidir_read` arms RX (CH0) first, TX
   (CH1) second. Both sequences arm both channels before CS rises,
   so either should be correct in principle.

2. **Descriptor chaining with end-bit (bit 31)** — stock builds a
   multi-descriptor chain for transfers > 4 KB, marking the final
   descriptor with `cfg |= 0x80000000`. Our driver uses a single
   descriptor per call, capped at 2 KB (`READ_CHUNK = 0x800`). We
   could probably transfer 4 KB at once by setting that flag; not
   the current concern.

3. **TX / RX have separate DMA channels** — stock uses `ctx->tx_chan`
   and `ctx->rx_chan` from the engine context. Live probe shows
   flash uses CH2 (RX) + CH3 (TX), LED uses CH4 (RX) + CH5 (TX). Our
   driver uses CH0 (RX) + CH1 (TX) for flash — channels that were
   unused by stock. Good: no contention. Bad: no reuse of stock's
   descriptor-chain ring buffers either.

4. **No TX overrun** — stock's TX descriptor count exactly matches
   the intended transfer. Our driver programs `TX_count = 0xFFF`
   (~2 KB overrun) because tightening to `xfer_len` stalls the RX
   pipeline (experiment 2026-04-20). Stock's single-descriptor
   approach doesn't have that problem because it completes in
   one chained transfer, whereas our per-2KB-chunk approach needs
   overrun to keep SPI clocking while RX drains.

5. **`SPI[6]` write after CS-assert** — stock writes `0` to
   `puVar6[6]` (SPI +0x18) right after asserting CS. Our driver
   writes `SPI_CLRINT = 0` (`+0x54`) at the same point instead.
   Different register — stock's `+0x18` might be a "start" pulse
   we don't use. Worth investigating.

---

## 6. `spi_flash_cmd_pp` @ 0x85b4 — byte-programming path

Not AAI — this is single-byte `0x02` PROGRAM. Stock sends the 4-byte
command header, then a 1-byte data body, all via the generic
`spi_resource_transfer()` → `spi_engine_queue_and_arm()`.

```c
void spi_flash_cmd_pp(flash_ctx_t *ctx, uint32_t addr, uint8_t byte) {
    engine = ctx->spi_engine;
    cmd_buf[0] = 0x02;
    cmd_buf[1] = (addr >> 16) & 0xFF;
    cmd_buf[2] = (addr >>  8) & 0xFF;
    cmd_buf[3] =  addr        & 0xFF;
    spi_resource_acquire(engine);
    spi_resource_transfer(engine, dir=0, len=4, tx=cmd_buf, rx=NULL);
    spi_resource_transfer(engine, dir=0, len=1, tx=&byte,    rx=NULL);
    spi_resource_release(engine);
}
```

Note: **the cmd header and payload are two separate transfers**, not a
single 5-byte one. CS stays asserted between them via the engine's
mutex-held state. That's a different pattern from our driver's
single-transfer-with-appended-cmd byte, and might be the reason stock
doesn't see our READ_RX_SKIP shift issues.

---

## 7. Implications for the v2 flash driver

Concrete changes we've shipped that match stock behaviour:
- **`DMA_ICLR = 0xFF` at teardown** (was `0x0F` → `0xFF` on 2026-04-22).
- **Narrow `1 << ch` semantics of DMA_ICLR** confirmed — widening to
  `0xF` or `0xFF` in every hot-path write was speculative but does
  no harm (stock's pattern is cleaner but not necessary).
- **8-channel engine** is now documented in `dma_engine_reset`.

Not yet tried:
- **Single chained descriptor for 4 KB read** — instead of 2 × 2 KB
  chunks with the TX overrun workaround. Removes the RX-FIFO-overflow
  hypothesis entirely.
- **Write to SPI `+0x18` post-CS-assert** to see if that's the
  missing "start pulse".
- **Split cmd/data transfers** like `spi_flash_cmd_pp` does, keeping
  CS asserted across them via the engine state.

Not actionable here:
- stock's cyg_mutex/scheduler wrapping — our driver is bare-metal
  with IRQs masked, no threading to protect against.

Open question for the residual +2-byte shift: the miss pattern
(`got = 00 00 ff ..` where `exp = ff ff 00 00`) plus `DMA_ISTAT
bit 3 set` suggests our single-chunk bidir read is losing 2 RX
bytes in the FIFO. Single-chained-4KB-descriptor would obviate
this. Tracked as task #32.

---

## 8. Follow-up experiments (2026-04-22)

After extensive Ghidra labelling, more experiments tried and (mostly)
ruled out:

### DMA_CFG bit pattern — partially fixed
Stock's `spi_engine_queue_and_arm` WRITES `0x70009000 | 0x04000000`
(TX inc) or `0x70009000 | 0x08000000` (RX inc) + EOC bit to the
descriptor cfg at build time. Live probe of eCos at idle shows the
channel's cfg READBACK as `0xF4009xxx` (TX) or `0x88009xxx` (RX). TX
matches; **RX readback is missing bits 28/29/30** that the stock code
wrote.

Theory: bits 28/29/30 encode "transfer active" or burst-state flags
that hardware clears once the transfer completes. Stock WRITES them
as part of the base pattern; hardware CLEARS them after running.

Tested: set our `DMA_CFG_RX` from `0x88009000` to `0xF8009000` (match
stock's write-time value). Result: **regressed** from ~1 miss/20 to
7/20 shift-repro iters. Confirms these bits are "don't write" for a
fresh DMA setup; the observed 0x88009000 IS the correct write value.
Revert kept.

### Chained-descriptor attempt — mechanism unclear
Built a chain of N descriptors (2–8) for a single bidir read with one
CS cycle. Result: `V2_ERR_READ_DMA` timeouts on every attempt — DMA
engine never signaled completion. Root cause likely the trigger word:
stock writes `wrapper[0] | 0x8001` to the GO reg, where `wrapper[0]`
is some pre-initialized value we haven't decoded (observed live eCos
CH0 GO = 0xD006, CH1 GO = 0xD004; our `DMA_TRG_TX = 0xD005`,
`DMA_TRG_RX = 0xD007` don't match stock's exact bits but do work for
single-descriptor transfers). Without the Arasan datasheet explaining
how GO register bits encode chain-mode vs single-shot, this can't be
implemented blindly.

### RX FIFO drain between transactions — regresses reliably
Tried adding `spi_ip_drain_rx` (read SPI_DATA until STAT.RX_RDY
clears) at various points:
* inside `spi_reset_clean` (start of each read chunk)
* at `xport_dma_aai_end` (after AAI, before first verify read)
* inside `dma_bidir_read` after DMA completes (before CS deassert)

All three variants regress to 10–17 deterministic failures. Reading
`SPI_DATA` with `SPI_EN=0` evidently has a side effect that
corrupts state. Without Arasan-documented drain semantics, "drain
the FIFO" isn't safely implementable.

### What the residual bug IS (as of 2026-04-22)

A SoC-internal half-word DMA capture glitch. Confirmed:
* **Wire level clean** — LA capture shows MISO byte-perfect.
* **SRAM has +2-byte shift at miss_offset** — half-word aligned
  (`miss_offset % 2 == 0 && miss_offset % 4 ∈ {0, 2}`).
* **Data-dependent, not burst-size-dependent** — same failure rate at
  `READ_CHUNK = 0x800`, `0x100`, `0x40`; same blocks fail deterministically.
* **DMA_ISTAT = 0x03** (bits 0+1 only) at miss — clean, bit 3 no
  longer latched thanks to `DMA_ICLR = 0xFF` widening.

Mitigated, not fixed:
* TX count word-aligned (`0xFFF` → `0xFFC`)
* `DMA_ICLR = 0xFF` in teardown (all 8 channels)
* In-driver `do_verify` retry × 4 per chunk
* Host `flash_block` retry × 3 at the per-op level

End-state failure rate: 0–2 per 64-op flash-all from eCos state,
~60–90 s runtime. Stubborn blocks (specific data patterns, specific
addresses) fail all retries occasionally and surface as `FAILED ops`
in the host-side summary. Re-running usually clears them.

The Arasan datasheet would be the unlock. Without it, further
micro-investigations have diminishing returns — the silicon likely
has a known workaround published somewhere or requires a specific
reset sequence we haven't found.

---

## 9. Follow-up experiments (2026-05-01)

Another investigation pass exploring the user's hypothesis that the bug
lives in the SPI↔AHB cross-clock-domain handshake (SPI parent ~12 MHz
crystal vs AHB/DMA ~200 MHz PLL).

### Mid-CS DMAMD switch (split-mode read) — does not work

Hypothesis: avoid bidir DMAMD=3 entirely by using TX-only DMA for the
4-byte cmd phase, then mid-CS switching to RX-only DMA for the data
phase. CS held throughout. Tested both with and without `SPI_EN=0/1`
toggle between phases.

Result: flash returns idle 0xFF for the data phase in both variants.
The IP appears to require a CS edge to start clocking a new transfer —
mid-CS DMAMD/CTRL/LEN updates do not re-trigger SCK. Confirmed by the
elapsed time per chunk (60 µs total for both phases, much faster than
the 100 µs theoretical minimum for 36 bytes at 3 MHz wire).

This is consistent with stock eCos's `spi_flash_cmd_pp` only splitting
across separate CS cycles — works for PROGRAM, doesn't apply to READ.
**Code reverted.** The split-read variant is not pursueable without
deeper IP knowledge.

### Clock binding (CLK_DIV_SPI sweep)

Hypothesis: tying SPI parent to PLL (so both SPI IP and AHB derive from
the same PLL with rational ratio) eliminates async cross-domain glitches.

Tested 5 configurations across 30 sample runs (paired and unpaired):

| CLK_DIV_SPI | SPI parent  | wire freq  | mean miss rate |
|-------------|-------------|------------|----------------|
| `0x0000`    | crystal     | 6 MHz      | ~50% (high variance: 27–100%) |
| `0x800C`    | PLL/12      | 8.3 MHz    | ~40% |
| `0x8008`    | PLL/8       | 12.5 MHz   | ~50% |
| `0x8010`    | PLL/16      | 6.25 MHz   | ~50% |
| `0x8088`    | PLL/136     | 0.75 MHz   | ~60% |

**Variance dominates.** Within one paired test pass clock binding showed
~16 percentage point spread, but across passes the same configs landed
anywhere from 27% to 100%. No clean signal that PLL parent helps.

The 100% rates are NOT explainable by config alone — they rotate which
config they hit between runs. Likely some unobserved chip-state variable
(IP arbitration state, residual FIFO content from prior ops, thermal).

### Verify-quiesce mode (`peripheral_full_teardown`) — small but real effect

`verify_quiesce_mode=STRICT` (the previous default) called
`peripheral_full_teardown` on every verify. That includes
`blender_spi_ip_drain_rx` — *the exact "read SPI_DATA with EN=0" pattern
documented in §8 above as **regressing** the bug*. The strict mitigation
was actively triggering the bug it was designed to mask.

Empirical comparison across many fresh-sector samples:
- STRICT: 70-100% miss rate
- LIGHT: 50-67%
- OFF:   30-50%

**Default changed to OFF** in `sram_flash_mailbox_v2.h`. Driver is also
~30 bytes smaller without the strict-mode quiesce path being the default.

### Log-put silencer

Hypothesis: per-AAI-pair `log_put` (2048× per sector) and its `dwb()`
write barriers might serialize SPI register writes more than expected,
affecting FIFO timing.

Result: marginal improvement — ~13% relative miss-rate reduction on 3 of
5 paired sectors at PLL/12, no change on the other 2. Within variance.

**Kept as a runtime tunable** (`v2_timings.log_silent`, host
`--timing log_silent=1`) in case future investigators want a clean
"no-side-effects" hot loop. Default 0 (logging on).

### TX_SCRATCH overlap fix + shift re-characterized as +4B (2026-05-01 late)

While probing whether the bug reproduces with reads only or with JEDEC,
we discovered a separate latent bug: `TX_SCRATCH` was hardcoded at
SRAM `0x2E100`, **inside the linker-managed driver region**
(`0x2C000-0x2E3FF`). When `.bss` was small enough, this happened to be
in unused space; once `.bss` grew (via the new `log_silent` field) it
extended to `0x2E103`, overlapping `TX_SCRATCH[0..3]`. Driver init's
`.bss` zero-fill silently clobbered `TX_SCRATCH[0]` to `0x00` —
meaning every `dma_rx` call was sending opcode `0x00` to flash instead
of the intended cmd. JEDEC ID returned `0xffffff` (no real cmd → flash
idle). Legacy ID was unaffected because its 5-byte path uses
`BIDIR_TX_BUF` at `0x28000`, which is properly outside the driver region.

The bug almost certainly existed in HEAD too (TX_SCRATCH was always in
the linker region) — the `0xef` bogus RDSR values surfacing in
`first_rdsr` on every verify-miss were likely caused by the same overlap
when `.bss` zeroing happened to cover that address range.

**Fix**: moved `TX_SCRATCH` / `RX_SCRATCH` to `0x29F00` / `0x29F40`
(unused SRAM well below `BIDIR_TX_BUF`).

**Side effect**: with `dma_rx` (and therefore RDSR) returning real data,
the post-AAI verify shift re-characterized as **+4 bytes (one full
32-bit FIFO word)**, not +2. The host shift classifier was extended to
detect shifts up to +8B; new sector-loop sample shows
`{'+4B shift': 26/30, '+6B shift': 3/30, '+2B shift': 1/30}`.

This is a much cleaner alignment story for the Arasan FIFO packer
hypothesis: the IP packs 4 MISO bytes per 32-bit word in the RX FIFO,
and the post-AAI glitch drops exactly one whole word from the stream.
The earlier "half-word shift" diagnosis was distorted by the
TX_SCRATCH/RDSR contamination — RDSR was returning bogus values, AAI's
busy-poll path was running on bogus state, and the verify-miss
classifier only checked shifts 1-3 (off-by-one bug `if s < 4 else
False`) so +4 always fell through to the "+2B shift" classifier
spuriously matching `got[0] == exp[2]` whenever pattern bytes happened
to repeat at a 2-byte offset.

### Bootloader read pattern (rxonly DMA via DATA register) — no help

Tested 2026-05-02. The bootloader's `dma_spi_read_setup` @ 0x4F290 uses
`CTRL=0x307`, `DMAMD=1` (RX-only DMA), DMA source = `0xCC000060`
(DATA register, not RX_PORT 0x70), and pushes the cmd+addr via PIO
DATA-register writes before kicking the engine via CS=1. We mirrored
this exactly (`dma_rxonly_read`, available under `read_mode=1`), and
verified register state (CTRL=0x307, DMAMD=1) sticks. Pure reads work
(0/5 misses), but **post-AAI verify reads still produce the +4B shift
at the same chunk-boundary positions** as the bidir path. So the bug
isn't in the bidir TX/RX_PORT FIFO packer specifically — both bidir
and rxonly read paths suffer it equally after AAI.

### What triggers the bug — AAI's pair stream is the culprit

Isolation experiments 2026-05-02:

| Pre-verify operation | Verify result |
|---|---|
| erase only | clean (legit FF/00 pattern mismatch only) |
| erase + BYTE_PRG (4 separate CS cycles) | **clean** |
| erase + AAI(2 B = 1 pair) | **clean** |
| erase + AAI(1024 B = 512 pairs) | **clean** |
| erase + AAI(2048 B = 1024 pairs) | shift bug appears |
| erase + AAI(4096 B = 2048 pairs) | shift bug, stronger |
| erase + 4 × AAI(1024 B) host-orchestrated | **same as 1 × 4096** — bug |
| erase + 4 × AAI(1024 B) driver-internal (`AAI_MAX_RUN=0x400`) | **same as 1 × 4096** — bug |
| erase + AAI(4092) + BYTE_PRG(4) | bug (AAI bulk dominates) |

Conclusions:
- BYTE_PRG (separate WREN/PROG/WRDI per byte) does **not** corrupt
  state — even if all 4096 bytes were programmed via BYTE_PRG (would
  take ~106s/sector — too slow for production).
- A single AAI pair is fine. The bug **accumulates** across pairs.
- Threshold is around 512–1024 pairs total.
- The accumulation **persists across separate WREN/AAI_FIRST/WRDI
  runs** — neither host-level nor driver-internal chunking helps.
  Each fresh AAI run still adds to whatever IP-side counter or FIFO
  pointer is being skewed.

The bug appears to be in some IP register/state that AAI's pair stream
mutates and that survives the WREN/AAI_FIRST/WRDI cycle. Without IP
documentation we can't identify which register to reset.

### FIX FOUND: dummy DMA bidir read absorbs the stuck FIFO state

2026-05-02 late: a **dummy 8-byte JEDEC `dma_rx` call (single CS cycle)
inserted at the start of each `dma_bidir_read`** completely eliminates
the post-AAI verify shift. **0/30 misses across 3 fresh sectors with
10 iters each**, vs the previous 5/5 → 30/30 baseline.

Threshold sweep:

| dummy size (TX cmd byte + RX response) | misses |
|---|---|
| no dummy (baseline) | **5/5, +4B shift** |
| 4 bytes (1 cmd + 3-byte JEDEC) | 3/5, +2B shift |
| 8 bytes (2 × 4-byte cycles) | 2/5, +2B shift |
| **5-16 bytes single CS cycle** | **0/5 — clean** |

Key insight: a single longer CS cycle works far better than multiple
short ones. The minimum effective dummy is 5 bytes total in one CS
cycle. Below that, the dummy partially absorbs the stuck state but
doesn't fully clear it. Production uses 8-byte dummy for safety
margin (~12 µs overhead per chunk at 6 MHz wire — negligible vs the
chunk's ~25 ms).

The dummy uses cmd 0x9F (JEDEC ID) because it's known-clean, returns
valid data regardless of chip state, and goes through the same
`dma_rx` bidir path. The captured data is discarded.

What this tells us about the bug: AAI's pair stream parks the IP's
RX FIFO packer in a state that drops one 32-bit word from the next
long DMA read. A short bidir CS cycle "consumes" that stuck state —
the dummy's RX side reads the corrupted word, then the IP is clean
for the next transaction. Multiple short cycles only partially
mitigate because each one starts fresh (the stuck word doesn't get
consumed across CS-cycle boundaries the same way).

This is a silicon-level workaround; we don't have IP docs, but the
empirical pattern is unambiguous and reliable.

### Net result

- Post-AAI verify shift is **fixed in production** by the dummy DMA
  bidir read at the start of `dma_bidir_read` (`sram_flash_driver_v2.c`
  ~line 1196). The do_verify 4× retry and host flash_block 3× retry
  remain as belt-and-suspenders.
- Pure flash reads (no preceding AAI) were always clean.
- The bug requires AAI ≥~1000 pairs to accumulate and only manifests
  in long bidir DMA reads — not in JEDEC, RDSR, or short reads.
- Bootloader's `dma_rxonly_read` pattern still available under
  `read_mode=1` for future investigation, but not needed now that
  the dummy-DMA fix works on the bidir path.
