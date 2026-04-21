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
