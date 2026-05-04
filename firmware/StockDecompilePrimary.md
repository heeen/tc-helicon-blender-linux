# Stock eCos primary firmware — DMA/SPI/flash decompile collection

Program: `blender_primary_body.bin` (image base `0x200`).
Captured: 2026-04-21.
Post-labeling decompile output from Ghidra with our struct types applied
(`dma_channel_regs`, `dma_descriptor`, `dma_channel_request`, `spi_engine_ctx`,
`spi_engine_config`, `spi_resource_t`, `sst25xx_driver_ctx_t`, `spi_regs`).

---

## DMA core

### `dma_irq_init` @ 0x95b0

Plate comment: *Initialize DMA controller IRQ path: set master_en=1, enable+clear all 8 channel IRQs, then register dma_isr/dma_dsr with eCos on IRQ 10 using storage embedded in g_dma_channel_tbl (interrupt object at +0x3c, isr-vector slot at +0x20). Unmasks IRQ 10 so DMA completions fire.*

```c
void dma_irq_init(void)
{
  undefined4 in_r3;
  int chan_tbl;

  chan_tbl = PTR_dma_channel_tbl_2;
  DMA_ENGINE.master_en = 1;
  DMA_ENGINE.en = 0xff;
  DMA_ENGINE.iclr = 0xff;
  cyg_interrupt_create
            (10,0,PTR_dma_channel_tbl_2,PTR_dma_isr,PTR_dma_dsr,PTR_dma_channel_tbl_2 + 0x3c,
             PTR_dma_channel_tbl_2 + 0x20,in_r3);
  hal_interrupt_attach_isr(*(undefined4 *)(chan_tbl + 0x3c));
  hal_interrupt_unmask(10);
  return;
}
```

Boots the DMA engine: enables the master, un-masks all 8 channel IRQs, registers eCos ISR/DSR on IRQ 10. Our v2 driver skips this entirely — we poll status bits instead of using interrupts.

---

### `dma_controller_init` @ 0x9948

Plate comment: *Initialize DMA driver bookkeeping structures (called before dma_irq_init / dma_channel_arm). First, fill a 256-byte software driver-state block at PTR_dma_driver_state (0x50 in SRAM) with a default hook pointer; then install key ops at fixed slots: state[0]=DMA_INIT_MAGIC_80017, state[0x30]=audio_clock_select, state[0x34]=audio_clock_select2, state[0x40]=dma_irq_init, state[0x48]=timer_wait_microseconds, and zero a few reserved slots. Also fills 24 entries of an audio_clock handler table at PTR_audio_clock_handler_tbl (0x2f610) with audio_clock_select as the default. Finally runs the audio-clock enable dance (two selects of -2, clock_enable, two selects of 1) — this is the stock sequence, not DMA-specific. TODO: the exact meaning of the 0x50 state layout isn't decoded here; slots [0x44] and [0x4c] stay zero.*

```c
void dma_controller_init(void)
{
  int i;
  undefined4 default_hook;
  undefined4 default_hook2;
  undefined4 *drv_state;
  undefined4 *tbl;

  tbl = PTR_dma_driver_state;
  default_hook = PTR_audio_clock_select;
  i = 0;
  do {
    tbl[i] = default_hook;
    drv_state = PTR_dma_driver_state;
    default_hook2 = PTR_audio_clock_select;
    i = i + 1;
  } while (i != 0x40);
  *PTR_dma_driver_state = DMA_INIT_MAGIC_80017;
  drv_state[0x10] = PTR_dma_irq_init;
  drv_state[0x12] = PTR_timer_wait_microseconds;
  tbl = PTR_audio_clock_handler_tbl;
  drv_state[0x13] = 0;
  drv_state[0xe] = 0;
  *tbl = default_hook2;
  tbl[1] = default_hook2;
  tbl[2] = default_hook2;
  tbl[3] = default_hook2;
  tbl[4] = default_hook2;
  tbl[5] = default_hook2;
  tbl[6] = default_hook2;
  tbl[7] = default_hook2;
  tbl[8] = default_hook2;
  tbl[9] = default_hook2;
  tbl[10] = default_hook2;
  tbl[0xb] = default_hook2;
  tbl[0xc] = default_hook2;
  tbl[0xd] = default_hook2;
  tbl[0xe] = default_hook2;
  tbl[0xf] = default_hook2;
  tbl[0x10] = default_hook2;
  tbl[0x11] = default_hook2;
  tbl[0x12] = default_hook2;
  tbl[0x13] = default_hook2;
  tbl[0x14] = default_hook2;
  tbl[0x15] = default_hook2;
  tbl[0x16] = default_hook2;
  tbl[0x17] = default_hook2;
  drv_state[0xc] = PTR_audio_clock_select_2;
  drv_state[0xd] = PTR_audio_clock_select2;
  audio_clock_select(0xfffffffe);
  audio_clock_select2(0xfffffffe);
  audio_clock_enable();
  audio_clock_select(1);
  audio_clock_select2(1);
  drv_state[0x11] = 0;
  return;
}
```

Wires up the SDK's DMA "driver state" block with function pointers and triggers the audio-clock enable sequence. Much of this is SDK boilerplate that our v2 driver does not reproduce; we rely only on `dma_channel_arm`-equivalent register pokes.

---

### `dma_channel_arm` @ 0x9620

Plate comment: *Arm DMA channel `ch` to execute a descriptor chain. Enables + clears the channel's IRQ bit, registers req into the per-channel software table (PTR_dma_channel_tbl_dup == g_dma_channel_tbl), then copies the first descriptor (src/dst/next/cfg) into DMA_CHAN[ch] and kicks the engine by writing cfg_base|0x8001 into ch->go. Bit 0x8000 is GO, bit 0x0001 enables chaining.*

```c
void dma_channel_arm(uint ch,dma_channel_request *req)
{
  dma_descriptor *desc;
  dma_channel_regs *ch_regs;
  uint ch_mask;

  ch_mask = 1 << (ch & 0xff);
  DMA_ENGINE.en = ch_mask;
  DMA_ENGINE.iclr = ch_mask;
  *(dma_channel_request **)(PTR_dma_channel_tbl_dup + ch * 4) = req;
  ch_regs = (dma_channel_regs *)((ch + 0x4000008) * 0x20);
  ch_regs->go = 0;
  desc = req->pDesc;
  ch_regs->src = desc->src;
  ch_regs->dst = desc->dst;
  ch_regs->next = (uint)desc->pNext;
  ch_regs->cfg = desc->cfg;
  ch_regs->go = req->trigger | 0x8001;
  return;
}
```

Core "kick the engine" helper — this is the exact register poke sequence our v2 driver now emits inline for SPI TX chains. The `0x8001` GO|CHAIN bit encoding matches what works from our bare-metal loader.

---

### `dma_isr` @ 0x9594

Plate comment: *DMA top-half ISR (IRQ 10). Masks the IRQ and returns 2 (CYG_ISR_HANDLED | CYG_ISR_CALL_DSR) so the DSR runs. Actual bit-clearing + per-channel dispatch happens in dma_dsr.*

```c
uint dma_isr(uint irq)
{
  hal_interrupt_mask_irq();
  hal_interrupt_unmask(irq);
  return 2;
}
```

Minimal top-half: just schedules the DSR. Not used in our v2 driver (we poll).

---

### `dma_dsr` @ 0x951c

Plate comment: *DMA bottom-half DSR (IRQ 10). Snapshots DMA_ENGINE.status, writes it back via DMA_ENGINE.en to clear done bits (W1C), then walks the 8-bit pending mask. For each asserted channel, loads the per-channel dma_channel_request * from g_dma_channel_tbl[ch] (stored by dma_channel_arm) and invokes req->pCallback with (req, DMA_CHAN[ch].dst).*

```c
void dma_dsr(uint irq)
{
  int chan;
  uint pending;
  int tbl_off;
  undefined4 *ch_regs;
  int chan_tbl;

  chan_tbl = PTR_dma_channel_tbl;
  pending = DMA_ENGINE.status;
  DMA_ENGINE.en = pending;
  tbl_off = 0;
  ch_regs = PTR_DMA_CHAN0_dst;
  for (pending = pending & 0xff; pending != 0; pending = pending >> 1) {
    if ((((pending & 1) != 0) && (chan = *(int *)(tbl_off + chan_tbl), chan != 0)) &&
       (*(code **)(chan + 4) != (code *)0x0)) {
      (**(code **)(chan + 4))(chan,*ch_regs);
    }
    tbl_off = tbl_off + 4;
    ch_regs = ch_regs + 8;
  }
  hal_interrupt_unmask(irq);
  return;
}
```

DSR walks the pending-channel mask and calls each channel's `pCallback` (this is where `spi_dma_tx_done_cb` / `spi_dma_rx_done_cb` get invoked). Status bits are W1C: the write of `pending` back into `DMA_ENGINE.en` clears them.

---

## SPI engine

### `spi_resource_acquire` @ 0xf560

Plate comment: *spi_resource_acquire — lock the engine mutex (retry on failure) and invoke the engine's `configure` hook so the resource-specific SPI controller state (clock divider, mode, etc.) is applied before a transfer.*

```c
void spi_resource_acquire(spi_resource_t *res)
{
  int iVar1;
  spi_engine_ctx *psVar2;

  psVar2 = res->pEngine;
  do {
    iVar1 = cyg_mutex_lock(psVar2);
  } while (iVar1 == 0);
  (*psVar2->pConfigure)(res);
  return;
}
```

Takes the per-engine mutex and calls the resource's `configure` hook. Our v2 driver is single-threaded so we skip the mutex and call the equivalent `spi_reset_clean` directly.

---

### `spi_resource_transfer` @ 0xf58c

Plate comment: *spi_resource_transfer — thin trampoline that invokes the engine's `transfer` hook (engine->transfer) with the current TX/RX buffer args. Does not release the resource (caller uses spi_exec_single_transfer to bundle acquire+transfer+release).*

```c
void spi_resource_transfer
               (spi_resource_t *res,void *tx_buf,uint tx_len,void *rx_buf,uint rx_len,uint flags)
{
  (*res->pEngine->pTransfer)();
  return;
}
```

Vtable trampoline — jumps through `engine->pTransfer` (which on this platform resolves to the `spi_engine_queue_and_arm` path).

---

### `spi_resource_release` @ 0xf5b0

Plate comment: *spi_resource_release — invokes the engine's `finish` hook, wakes any waiter parked on the resource-shared condition variable, and drops priority boosts (eCos mutex). Called from spi_exec_single_transfer after the bundled transfer completes.*

```c
void spi_resource_release(spi_resource_t *res)
{
  char cVar1;
  int iVar2;
  uint uVar3;
  undefined4 unaff_r4;
  spi_engine_ctx *psVar4;

  psVar4 = res->pEngine;
  (*psVar4->pFinish)();
  sched_lock_inc();
  if (psVar4->owner_thread_ptr != 0) {
    iVar2 = Cyg_Scheduler__get_current_thread(&psVar4->owner_thread_ptr);
    if ((char)psVar4->active_resource == '\x01') {
      cyg_mutex_acquire_adjust_priority
                (iVar2 + 0x1c,psVar4->flags,&psVar4->owner_thread_ptr,1,unaff_r4);
    }
    *(undefined1 *)(iVar2 + 0x80) = 0;
    *(undefined1 *)(iVar2 + 0x81) = 7;
    Cyg_Thread__wake(iVar2);
  }
  cVar1 = (char)psVar4->active_resource;
  if (cVar1 != '\0') {
    uVar3 = psVar4->flags;
    *(int *)(uVar3 + 0x2c) = *(int *)(uVar3 + 0x2c) + -1;
    if (cVar1 == '\x01') {
      cyg_mutex_release_restore_priority(uVar3 + 0x1c);
    }
  }
  if ((char)psVar4->active_resource == '\x02') {
    cyg_mutex_release_restore_priority(psVar4->flags + 0x1c);
  }
  *(undefined1 *)&psVar4->master_clock_hz = 0;
  psVar4->flags = 0;
  sched_lock_dec();
  return;
}
```

Tears down the engine lease, wakes a blocked owner thread (if any), and un-boosts priorities. Irrelevant for our bare-metal driver.

---

### `spi_exec_single_transfer` @ 0xf5cc

Plate comment: *spi_exec_single_transfer — all-in-one acquire → transfer → release helper. Acquires the engine mutex (spi_resource_acquire), runs the transfer via the engine's configured vtable, then releases priorities/wakes waiters exactly like spi_resource_release.*

```c
void spi_exec_single_transfer(spi_resource_t *res,void *tx_buf,uint tx_len,void *rx_buf,uint rx_len)
{
  char cVar1;
  int iVar2;
  uint uVar3;
  spi_engine_ctx *psVar4;
  undefined4 unaff_r4;
  undefined4 unaff_r5;
  undefined4 unaff_r6;
  undefined4 in_lr;

  spi_resource_acquire(res);
  spi_resource_transfer(res,tx_buf,tx_len,rx_buf,rx_len,1);
  psVar4 = res->pEngine;
  (*psVar4->pFinish)();
  sched_lock_inc();
  if (psVar4->owner_thread_ptr != 0) {
    iVar2 = Cyg_Scheduler__get_current_thread(&psVar4->owner_thread_ptr);
    if ((char)psVar4->active_resource == '\x01') {
      cyg_mutex_acquire_adjust_priority
                (iVar2 + 0x1c,psVar4->flags,&psVar4->owner_thread_ptr,1,unaff_r4,unaff_r5,unaff_r6,
                 in_lr);
    }
    *(undefined1 *)(iVar2 + 0x80) = 0;
    *(undefined1 *)(iVar2 + 0x81) = 7;
    Cyg_Thread__wake(iVar2);
  }
  cVar1 = (char)psVar4->active_resource;
  if (cVar1 != '\0') {
    uVar3 = psVar4->flags;
    *(int *)(uVar3 + 0x2c) = *(int *)(uVar3 + 0x2c) + -1;
    if (cVar1 == '\x01') {
      cyg_mutex_release_restore_priority(uVar3 + 0x1c);
    }
  }
  if ((char)psVar4->active_resource == '\x02') {
    cyg_mutex_release_restore_priority(psVar4->flags + 0x1c);
  }
  *(undefined1 *)&psVar4->master_clock_hz = 0;
  psVar4->flags = 0;
  sched_lock_dec();
  return;
}
```

The all-in-one caller used by every single-shot flash command (WREN, WRDI, RDSR, WRSR, PP, SE, READ_ID). Our v2 driver open-codes this sequence for each command.

---

### `dma_engine_acquire_hw` @ 0xf72c

Plate comment: *(none set)*

```c
void dma_engine_acquire_hw(int *param_1)
{
  undefined4 uVar1;
  int iVar2;
  uint uVar3;
  int iVar4;

  iVar4 = *param_1;
  if (param_1[3] == 0) {
    if (*(char *)((int)param_1 + 6) == '\0') {
      uVar3 = 0;
    }
    else {
      uVar3 = 0x80;
    }
    if (*(char *)((int)param_1 + 7) != '\0') {
      uVar3 = uVar3 | 0x40;
    }
    if (*(char *)((int)param_1 + 5) == '\0') {
      uVar3 = uVar3 | 7;
    }
    else {
      uVar3 = uVar3 | 0xf;
    }
    param_1[3] = uVar3;
    uVar1 = gpio_init();
    iVar2 = udiv32(uVar1,param_1[2]);
    param_1[4] = iVar2 + 7U >> 2 & DAT_0000f7b0;
  }
  *(undefined4 *)(iVar4 + 0x30) = 1;
  *(undefined4 *)(iVar4 + 0x34) = 0;
  *(undefined4 *)(iVar4 + 0x38) = 0;
  *(undefined4 *)(iVar4 + 0x3c) = 0;
  return;
}
```

First-acquire lazy init: computes a per-resource SPI control-word (word-size, CS polarity bits) and clock divider from the platform GPIO/clock rate, then resets engine state `_30=1; _34=_38=_3c=0`. Subsequent calls skip recompute and just reset the counters.

---

### `dma_engine_commit_and_wait` @ 0xf7b4

Plate comment: *dma_engine_commit_and_wait — close out the descriptor chain(s) currently queued in the engine and block until the SPI/DMA transfer finishes.*

Steps (per plate comment):
1. Wait for controller idle (`spi[10] & 1 == 0`)
2. Program SPI controller regs from the queued state (`_38_total_len`, `reserved_0c`, `config_flags`)
3. Compute dir_bits from `eng->_3c_dir_code` (0/2→0x100 TX-only, 1→0x200 RX-only, 3→0 BIDIR, 4→0x300 alt-BIDIR)
4. Mark the last TX and RX descriptors as end-of-chain (`cfg |= 0x80000000`, `next=NULL`)
5. Arm DMA channel(s) via `dma_channel_arm`, using `engine->config->{tx,rx}_{chan,req}`
6. Clear `eng->{tx,rx,epilogue}_done`, start the SPI controller (`puVar6[4] = 1 << config_flags`)
7. Block on `eng->done_flag` (`cyg_flag_timed_wait`) until all three flags are set
8. Reset queued-descriptor bookkeeping (`_34_desc_count / _38_total_len / _3c_dir_code`)

This is the identical tail of `spi_engine_queue_and_arm` extracted as a separate helper.

```c
void dma_engine_commit_and_wait(spi_resource_t *res)
{
  dma_descriptor *desc;
  spi_engine_config *cfg;
  int tail_idx;
  uint dir_bits;
  spi_engine_ctx *eng;
  spi_regs *spi;

  eng = res->pEngine;
  cfg = eng->pConfig;
  spi = cfg->pRegs;
  if (eng->_34_desc_count == 0) {
    return;
  }
  do {
  } while ((spi->dwStat & 1) != 0);
  spi->dwEn = 0;
  spi->dwCs = 0;
  spi->dwDmago = 0;
  spi->dwLen = eng->_38_total_len - 1;
  spi->dwClk = res->reserved_10;
  spi->dwDmacfg0 = 4;
  spi->dwDmacfg1 = 3;
  dir_bits = eng->_3c_dir_code;
  if (dir_bits == 1) {
    dir_bits = 0x200;
  }
  else if ((dir_bits == 0) || (1 < dir_bits - 3)) {
    dir_bits = 0x100;
  }
  else if (dir_bits == 3) {
    dir_bits = 0;
  }
  else {
    dir_bits = 0x300;
  }
  spi->dwCtrl = dir_bits | res->reserved_0c;
  spi->dwEn = 1;
  desc = (cfg->tx_req).pDesc;
  tail_idx = eng->_34_desc_count - 1;
  dir_bits = desc[tail_idx].cfg;
  eng->rx_done = 0;
  eng->tx_done = 0;
  eng->epilogue_done = 0;
  desc[tail_idx].pNext = (dma_descriptor *)0x0;
  desc[tail_idx].cfg = dir_bits | 0x80000000;
  desc = (cfg->rx_req).pDesc;
  desc[tail_idx].pNext = (dma_descriptor *)0x0;
  desc[tail_idx].cfg = desc[tail_idx].cfg | 0x80000000;
  switch(eng->_3c_dir_code) {
  case 0:
  case 2:
    spi->dwDmamd = 2;
    dma_channel_arm(cfg->tx_chan,&cfg->tx_req);
    eng->rx_done = 1;
    break;
  case 1:
    spi->dwDmamd = 1;
    dma_channel_arm(cfg->rx_chan,&cfg->rx_req);
    eng->tx_done = 1;
    eng->epilogue_done = 1;
    break;
  case 3:
  case 4:
    spi->dwDmamd = 3;
    dma_channel_arm(cfg->rx_chan,&cfg->rx_req);
    dma_channel_arm(eng->pConfig->tx_chan,&eng->pConfig->tx_req);
    eng->epilogue_done = 1;
  }
  cyg_mutex_lock(eng->pMutex_74);
  cyg_scheduler_lock();
  spi->dwCs = 1 << (sbyte)res->config_flags;
  spi->dwClrint = 0;
  if (eng->_3c_dir_code == 1) {
    spi->dwData = 0;
  }
  do {
    if ((eng->tx_done != 0) && (eng->epilogue_done == 0)) {
      spi->dwDmago = 1;
    }
    if (((eng->tx_done == 0) || (eng->rx_done == 0)) || (eng->epilogue_done == 0)) {
      cyg_flag_timed_wait(eng->pDone_flag);
    }
  } while (((eng->tx_done == 0) || (eng->rx_done == 0)) || (eng->epilogue_done == 0));
  cyg_scheduler_unlock();
  cyg_mutex_unlock(eng->pMutex_74);
  eng->_34_desc_count = 0;
  eng->_38_total_len = 0;
  eng->_3c_dir_code = 0;
  return;
}
```

This is the canonical "commit" tail. Ground truth for our v2 driver's SPI start sequence: wait idle → program LEN/CLK/DMACFG → set CTRL (direction bits OR'd with `reserved_0c`) → `EN=1` → terminate chain → `DMAMD` per direction → arm channel(s) → `CS` → `DMAGO=1` (TX-only/epilogue) or the RX prefill write. The direction-to-ctrl mapping `{0/2: 0x100, 1: 0x200, 3: 0, 4: 0x300}` corresponds to our `SPI_CTRL=0x207` (TX) / `0x107` (RX) constants once the `reserved_0c` base (0x107) is OR'd in.

---

### `dma_engine_release` @ 0xf9f8

Plate comment: *(none set)*

```c
void dma_engine_release(spi_resource_t *param_1)
{
  spi_engine_ctx *psVar1;

  psVar1 = param_1->pEngine;
  dma_engine_commit_and_wait(param_1);
  psVar1->_30 = 0;
  return;
}
```

"Release" hook — flushes any outstanding queued descriptors (so partial chained transfers finish) and clears the lazy-init flag.

---

### `spi_engine_queue_and_arm` @ 0xfa10

Plate comment: *spi_engine_queue_and_arm — HOT PATH: queue one or more DMA descriptors describing a SPI transfer, optionally commit & wait.*

Full plate doc from the function header:

> Arguments:
> - `res` — `spi_resource_t`. `res->engine` is the engine ctx, `res->config_flags` byte 1 toggles "wide" (halfword) mode.
> - `cmd_word` — SPI command/header word OR'd with dir_bits into SPI ctrl reg.
> - `total_len` — byte length (halved when `config_flags.byte1` set → halfword mode).
> - `tx_buf` — user TX buffer pointer. If 0, `&eng->scratch_tx` is used and the TX descriptor DOES NOT set bit `0x04000000` (stays at the fixed scratch, tx_buf stride=0 each iteration).
> - `rx_buf` — user RX buffer pointer. Same scratch/stride treatment.
> - `commit` — non-zero: after queuing, invoke the `dma_engine_commit_and_wait` tail. zero: just append descriptors and return (chained transfer).
>
> Per-iteration body (for each `DAT_0000fbac`-sized chunk, typically 0xFFE bytes):
> ```
> desc = cfg->tx_req.desc (TX ring)
>   desc[idx].src  = tx_src (or &eng->scratch_tx)
>   desc[idx].dst  = spi_regs + 0x80 (SPI TX DATA reg)
>   desc[idx].next = &desc[idx+1]
>   desc[idx].cfg  = chunk_bits | 0x70009000 (8-bit) or 0x70249000 (16-bit)
>                   |= 0x04000000 if user tx_buf (src post-increment)
> desc = cfg->rx_req.desc (RX ring)
>   desc[idx].src  = spi_regs + 0x70 (SPI RX DATA reg)
>   desc[idx].dst  = rx_buf (or &eng->scratch_rx)
>   desc[idx].next = &desc[idx+1]
>   desc[idx].cfg  = chunk_bits | 0x70009000/0x70249000
>                   |= 0x08000000 if user rx_buf (dst post-increment)
> ```
>
> Direction encoding:
>
>  local `iVar6` = 3 if both tx and rx given, else 2 (tx>1), else 1 (rx only), else 0.
>  `eng->_3c_dir_code` is then LUT-updated via `DAT_0000fbb0[old_code*4 + iVar6]` (4 bytes per prior state → preserves direction across chained calls).
>
> BIDIR (case 3/4) commit path:
> ```
> puVar8[0x13] = 3                        // SPI ctrl "bidir" mode
> dma_channel_arm(cfg->rx_chan, &cfg->rx_req);   // start RX first so it hooks the FIFO
> dma_channel_arm(cfg->tx_chan, &cfg->tx_req);   // then TX generates clocks
> eng->epilogue_done = 1;                 // BIDIR has no epilogue step
> // wait loop now needs tx_done && rx_done
> ```
>
> The final SPI start write (`puVar8[4] = 1 << res->config_flags`) is what generates clocks.

```c
void spi_engine_queue_and_arm
               (spi_resource_t *res,uint cmd_word,uint total_len,uint tx_buf,uint rx_buf,uint commit
               )
{
  dma_descriptor *desc;
  spi_engine_config *cfg;
  spi_engine_ctx *eng;
  uint chunk;
  int dir_code;
  int stride;
  spi_regs *spi;
  uint idx;
  uint *tx_src;
  uint chunk_bits;

  eng = res->pEngine;
  if (total_len != 0) {
    dir_code = 1 - tx_buf;
    if (1 < tx_buf) {
      dir_code = 0;
    }
    if (tx_buf == 0 || rx_buf == 0) {
      if (dir_code == 0) {
        dir_code = 2;
      }
      else {
        dir_code = 0;
        if (rx_buf != 0) {
          dir_code = 1;
        }
      }
    }
    else {
      dir_code = 3;
    }
    if (*(char *)((int)&res->config_flags + 1) != '\0') {
      total_len = total_len >> 1;
    }
    while (total_len != 0) {
      cfg = eng->pConfig;
      idx = eng->_34_desc_count;
      desc = (cfg->tx_req).pDesc;
      spi = cfg->pRegs;
      chunk = DAT_0000fbac;
      if (total_len < DAT_0000fbac) {
        chunk = total_len;
      }
      tx_src = (uint *)tx_buf;
      if ((uint *)tx_buf == (uint *)0x0) {
        tx_src = &eng->scratch_tx;
      }
      desc[idx].src = (uint)tx_src;
      desc[idx].dst = (uint)&spi->dwTx_port;
      desc[idx].pNext = desc + idx + 1;
      chunk_bits = chunk & 0xfff;
      desc[idx].cfg = chunk_bits | 0x70009000;
      total_len = total_len - chunk;
      if (*(char *)((int)&res->config_flags + 1) != '\0') {
        desc[idx].cfg = chunk_bits | 0x70249000;
      }
      if ((uint *)tx_buf != (uint *)0x0) {
        desc[idx].cfg = desc[idx].cfg | 0x4000000;
        if (*(char *)((int)&res->config_flags + 1) == '\0') {
          stride = 1;
        }
        else {
          stride = 2;
        }
        tx_buf = chunk * stride + tx_buf;
      }
      desc = (cfg->rx_req).pDesc;
      desc[idx].src = (uint)&spi->dwRx_port;
      tx_src = (uint *)rx_buf;
      if ((uint *)rx_buf == (uint *)0x0) {
        tx_src = &eng->scratch_rx;
      }
      desc[idx].dst = (uint)tx_src;
      desc[idx].cfg = chunk_bits | 0x70009000;
      desc[idx].pNext = desc + idx + 1;
      if (*(char *)((int)&res->config_flags + 1) != '\0') {
        desc[idx].cfg = chunk_bits | 0x70249000;
      }
      if ((uint *)rx_buf != (uint *)0x0) {
        desc[idx].cfg = desc[idx].cfg | 0x8000000;
        if (*(char *)((int)&res->config_flags + 1) == '\0') {
          stride = 1;
        }
        else {
          stride = 2;
        }
        rx_buf = chunk * stride + rx_buf;
      }
      eng->_34_desc_count = idx + 1;
    }
    eng->_3c_dir_code = (uint)*(byte *)(DAT_0000fbb0 + dir_code + eng->_3c_dir_code * 4);
    if (commit != 0) {
      eng = res->pEngine;
      cfg = eng->pConfig;
      spi = cfg->pRegs;
      if (eng->_34_desc_count == 0) {
        return;
      }
      do {
      } while ((spi->dwStat & 1) != 0);
      spi->dwEn = 0;
      spi->dwCs = 0;
      spi->dwDmago = 0;
      spi->dwLen = eng->_38_total_len - 1;
      spi->dwClk = res->reserved_10;
      spi->dwDmacfg0 = 4;
      spi->dwDmacfg1 = 3;
      chunk = eng->_3c_dir_code;
      if (chunk == 1) {
        chunk = 0x200;
      }
      else if ((chunk == 0) || (1 < chunk - 3)) {
        chunk = 0x100;
      }
      else if (chunk == 3) {
        chunk = 0;
      }
      else {
        chunk = 0x300;
      }
      spi->dwCtrl = chunk | res->reserved_0c;
      spi->dwEn = 1;
      desc = (cfg->tx_req).pDesc;
      dir_code = eng->_34_desc_count - 1;
      chunk = desc[dir_code].cfg;
      eng->rx_done = 0;
      eng->tx_done = 0;
      eng->epilogue_done = 0;
      desc[dir_code].pNext = (dma_descriptor *)0x0;
      desc[dir_code].cfg = chunk | 0x80000000;
      desc = (cfg->rx_req).pDesc;
      desc[dir_code].pNext = (dma_descriptor *)0x0;
      desc[dir_code].cfg = desc[dir_code].cfg | 0x80000000;
      switch(eng->_3c_dir_code) {
      case 0:
      case 2:
        spi->dwDmamd = 2;
        dma_channel_arm(cfg->tx_chan,&cfg->tx_req);
        eng->rx_done = 1;
        break;
      case 1:
        spi->dwDmamd = 1;
        dma_channel_arm(cfg->rx_chan,&cfg->rx_req);
        eng->tx_done = 1;
        eng->epilogue_done = 1;
        break;
      case 3:
      case 4:
        spi->dwDmamd = 3;
        dma_channel_arm(cfg->rx_chan,&cfg->rx_req);
        dma_channel_arm(eng->pConfig->tx_chan,&eng->pConfig->tx_req);
        eng->epilogue_done = 1;
      }
      cyg_mutex_lock(eng->pMutex_74);
      cyg_scheduler_lock();
      spi->dwCs = 1 << (sbyte)res->config_flags;
      spi->dwClrint = 0;
      if (eng->_3c_dir_code == 1) {
        spi->dwData = 0;
      }
      do {
        if ((eng->tx_done != 0) && (eng->epilogue_done == 0)) {
          spi->dwDmago = 1;
        }
        if (((eng->tx_done == 0) || (eng->rx_done == 0)) || (eng->epilogue_done == 0)) {
          cyg_flag_timed_wait(eng->pDone_flag);
        }
      } while (((eng->tx_done == 0) || (eng->rx_done == 0)) || (eng->epilogue_done == 0));
      cyg_scheduler_unlock();
      cyg_mutex_unlock(eng->pMutex_74);
      eng->_34_desc_count = 0;
      eng->_38_total_len = 0;
      eng->_3c_dir_code = 0;
      return;
    }
  }
  return;
}
```

Hot path for every flash command. Queues descriptors (chunked to ~4KB), auto-selects TX-only / RX-only / BIDIR based on which buffers were provided, then (if `commit`) runs the exact same commit tail as `dma_engine_commit_and_wait`. Descriptor cfg constants `0x70009000` (8-bit) / `0x70249000` (16-bit) with `0x04000000` / `0x08000000` post-increment bits match what our v2 driver assembles.

---

### `spi_dma_tx_done_cb` @ 0xf618

Plate comment: *spi_dma_tx_done_cb — DMA TX-chain completion ISR callback. Called by the DMA core when the last TX descriptor of the armed chain finishes. Marks eng->tx_done=1, signals eng->done_flag, then decrements the scheduler lock. The dma-channel state holds an spi_engine_ctx* at channel+8.*

```c
void spi_dma_tx_done_cb(void *dma_chan)
{
  int iVar1;

  iVar1 = *(int *)((int)dma_chan + 8);
  cyg_scheduler_lock();
  *(undefined4 *)(iVar1 + 0x40) = 1;
  cyg_cond_signal(iVar1 + 0x88);
  if (*DAT_0000a868 == 0) {
    return;
  }
  sched_lock_dec();
  return;
}
```

Thin wake-the-waiter callback for TX-chain completion.

---

### `spi_dma_rx_done_cb` @ 0xf63c

Plate comment: *spi_dma_rx_done_cb — DMA RX-chain completion ISR callback. Called by the DMA core when the last RX descriptor of the armed chain finishes. Marks eng->rx_done=1, signals eng->done_flag, then decrements the scheduler lock. The dma-channel state holds an spi_engine_ctx* at channel+8.*

```c
void spi_dma_rx_done_cb(void *dma_chan)
{
  int iVar1;

  iVar1 = *(int *)((int)dma_chan + 8);
  cyg_scheduler_lock();
  *(undefined4 *)(iVar1 + 0x44) = 1;
  cyg_cond_signal(iVar1 + 0x88);
  if (*DAT_0000a868 == 0) {
    return;
  }
  sched_lock_dec();
  return;
}
```

Same as TX side but writes `rx_done` at `+0x44`.

---

## SST25xx flash driver

### `spi_flash_cmd_wren` @ 0x8524

Plate comment: *Write Enable (opcode 0x06): 1-byte command, sets the flash WEL bit.*

```c
void spi_flash_cmd_wren(sst25xx_driver_ctx_t *ctx)
{
  undefined4 in_r3;
  undefined1 wren_cmd;
  undefined3 uStack_b;

  _wren_cmd = CONCAT31((int3)((uint)in_r3 >> 8),6);
  spi_exec_single_transfer(ctx->spi_res,(void *)0x0,1,&wren_cmd,0);
  return;
}
```

Single-byte 0x06 transfer.

---

### `spi_flash_cmd_rdsr` @ 0x8550

Plate comment: *Read Status Register (opcode 0x05): 2-byte exec-single-transfer, return byte[1]. Used throughout to poll BUSY (bit 0) after program/erase.*

```c
byte spi_flash_cmd_rdsr(sst25xx_driver_ctx_t *ctx)
{
  undefined4 in_r2;
  undefined4 in_r3;
  undefined2 rdsr_cmd;
  undefined2 uStack_e;
  undefined1 uStack_c;
  byte local_b;

  local_b = (byte)((uint)in_r3 >> 8);
  _rdsr_cmd = CONCAT22((short)((uint)in_r2 >> 0x10),5);
  spi_exec_single_transfer(ctx->spi_res,(void *)0x0,2,&rdsr_cmd,(uint)&uStack_c);
  return local_b;
}
```

Polling primitive for program/erase completion.

---

### `spi_flash_cmd_wrdi` @ 0x8588

Plate comment: *Write Disable (opcode 0x04): 1-byte command, clears WEL. Called at the end of a write sequence so a stray WREN can't leave the chip armed.*

```c
void spi_flash_cmd_wrdi(sst25xx_driver_ctx_t *ctx)
{
  undefined4 in_r3;
  undefined1 wrdi_cmd;
  undefined3 uStack_b;

  _wrdi_cmd = CONCAT31((int3)((uint)in_r3 >> 8),4);
  spi_exec_single_transfer(ctx->spi_res,(void *)0x0,1,&wrdi_cmd,0);
  return;
}
```

Single-byte 0x04 transfer.

---

### `spi_flash_cmd_byte_pp` @ 0x85b4

Plate comment: *SPI flash Page Program (opcode 0x02). Sends 4-byte header [0x02, A23..A16, A15..A8, A7..A0] then a single data byte in a second transfer, both bracketed by one acquire/release (CS asserted for the entire command).*

```c
void spi_flash_cmd_byte_pp(sst25xx_driver_ctx_t *ctx,uint addr,void *byte_ptr)
{
  spi_resource_t *res;
  undefined1 pp_cmd;
  undefined1 pp_a23_16;
  undefined1 pp_a15_8;
  undefined1 pp_a7_0;

  res = ctx->spi_res;
  _pp_cmd = CONCAT13((char)addr,CONCAT12((char)(addr >> 8),CONCAT11((char)(addr >> 0x10),2)));
  spi_resource_acquire(res);
  spi_resource_transfer(res,(void *)0x0,4,&pp_cmd,0,0);
  spi_resource_transfer(res,(void *)0x0,1,byte_ptr,0,0);
  spi_resource_release(res);
  return;
}
```

Single-byte PP used by AAI odd-byte prologue/epilogue.

---

### `sst25xx_addr_in_bank` @ 0x862c

Plate comment: *Bank-range check: if start_addr <= *addr <= end_addr, subtract start_addr and return true. Used by every erase/program/read entry point to convert absolute SPI addresses into bank-local offsets before commanding the chip.*

```c
int sst25xx_addr_in_bank(sst25xx_driver_ctx_t *ctx,uint *addr)
{
  bool bVar1;
  uint uVar2;

  uVar2 = *addr;
  if (ctx->start_addr <= uVar2) {
    bVar1 = uVar2 <= ctx->end_addr;
    if (bVar1) {
      *addr = uVar2 - ctx->start_addr;
    }
    return (uint)bVar1;
  }
  return 0;
}
```

Absolute→bank-local translation gate. Blender has a single bank so this is effectively a bounds check.

---

### `sst25xx_clear_block_protection` @ 0x8660

Plate comment: *Clear SST25xx block-protection bits via WRSR (opcode 0x01, data=0x00). Preceded by WREN. (Note: SST25 family requires EWSR 0x50 instead of WREN for the subsequent WRSR on some parts — see spi_controller_architecture memory.)*

```c
int sst25xx_clear_block_protection(sst25xx_driver_ctx_t *ctx,uint addr)
{
  int iVar1;
  spi_resource_t *res;
  uint abs_addr;
  undefined1 wrsr_cmd;
  undefined1 wrsr_val;

  res = ctx->spi_res;
  wrsr_cmd = 1;
  wrsr_val = 0;
  abs_addr = addr;
  iVar1 = sst25xx_addr_in_bank(ctx,&abs_addr);
  if (iVar1 != 0) {
    spi_flash_cmd_wren(ctx);
    spi_exec_single_transfer(res,(void *)0x0,2,&wrsr_cmd,0);
  }
  return (uint)(iVar1 == 0);
}
```

Emits `WREN` + `WRSR 0x00`. Per our lab note (`spi_controller_architecture.md`) this is insufficient on SST25VF016B — the real BP-clear sequence needs `EWSR 0x50` before `WRSR`, which the stock firmware does not do. This is why we saw silent write failures when attempting to poke above 0x80000 via the CPU: the SDK assumes WREN will unlock WRSR, but on this chip only EWSR does.

---

### `sst25xx_set_block_protection` @ 0x86c4

Plate comment: *Lock flash: WREN + WRSR 0x3C (sets BP0..BP3 — protects entire chip). Data at DAT_00008730 is the 2-byte payload `01 3C` (WRSR opcode + value). Symmetric counterpart of sst25xx_clear_block_protection (which uses EWSR+WRSR 0x00). `addr` routes to the right chip instance in multi-bank setups; returns 0 on success, 1 if addr is outside this bank. Previously misnamed sst25xx_write_enable (SDK name) and briefly misnamed sst25xx_wrsr_unlock by a cleanup agent (0x3C locks, not unlocks).*

```c
int sst25xx_set_block_protection(sst25xx_driver_ctx_t *ctx,uint addr)
{
  int iVar1;
  spi_resource_t *res;
  uint abs_addr;
  undefined1 auStack_14 [8];

  res = ctx->spi_res;
  abs_addr = addr;
  memcpy(auStack_14,DAT_00008730,2);
  iVar1 = sst25xx_addr_in_bank(ctx,&abs_addr);
  if (iVar1 != 0) {
    spi_flash_cmd_wren(ctx);
    spi_exec_single_transfer(res,(void *)0x0,2,auStack_14,0);
  }
  return (uint)(iVar1 == 0);
}
```

Writes `WRSR 0x3C` to set all four BP bits. Stock path that leaves BPL=1 on the live device we probe.

---

### `sst25xx_read_id` @ 0x8734  (*actually "Fast Read 0x0B"*)

Plate comment: *Fast Read (0x0B): [0x0B, A23..A8, A7..A0, dummy] then burst read len bytes. Named "read_id" in the stock source but it's actually the Fast Read path used by higher-level readers. Uses acquire/transfer/release for one atomic transaction.*

```c
int sst25xx_read_id(sst25xx_driver_ctx_t *ctx,uint addr,void *buf,int len)
{
  int iVar1;
  uint tx_len;
  spi_resource_t *res;
  undefined1 fast_read_cmd;
  undefined1 a23_16;
  undefined1 a15_8;
  undefined1 a7_0;
  undefined1 dummy;
  uint bank_addr [2];

  bank_addr[0] = addr;
  iVar1 = sst25xx_addr_in_bank(ctx,bank_addr);
  if (iVar1 == 0) {
    iVar1 = 1;
  }
  else {
    for (; len != 0; len = len - tx_len) {
      res = ctx->spi_res;
      a23_16 = (undefined1)(bank_addr[0] >> 0x10);
      tx_len = len;
      if (len == 0xffffffff) {
        tx_len = 0xffffffff;
      }
      a15_8 = (undefined1)(bank_addr[0] >> 8);
      a7_0 = (undefined1)bank_addr[0];
      dummy = 0;
      fast_read_cmd = 0xb;
      spi_resource_acquire(res);
      spi_resource_transfer(res,(void *)0x0,5,&fast_read_cmd,0,0);
      spi_resource_transfer(res,(void *)0x0,tx_len,(void *)0x0,(uint)buf,0);
      spi_resource_release(res);
      buf = (void *)((int)buf + tx_len);
      bank_addr[0] = bank_addr[0] + tx_len;
    }
    iVar1 = 0;
  }
  return iVar1;
}
```

This is the only "flash read" path in the stock firmware that performs CPU-initiated SPI reads. Bulk reads for most of the firmware go through the XIP window, not this function; this is the fallback used by the flash filesystem.

---

### `sst25xx_init_legacy` @ 0x8818

Plate comment: *Legacy SST25xx init: issues READ_ID (0x90) to get manufacturer/device ID, walks the per-device descriptor table at DAT_00008958 looking for a matching entry (SST 0xBF | device_id<<16). For a matching entry with sector_size==0x1000 (4KB), fills ctx->end_addr and ctx->info_out with sector_size/sector_count and returns 0 on success. Non-matching parts emit log callbacks.*

```c
int sst25xx_init_legacy(sst25xx_driver_ctx_t *ctx)
{
  uint uVar1;
  int iVar2;
  int *piVar3;
  int iVar4;
  uint uVar5;
  undefined1 read_id_cmd;
  undefined1 local_1f;
  undefined1 local_1e;
  undefined1 local_1d;
  undefined1 local_1c;
  undefined1 local_1b;
  undefined1 auStack_18 [4];
  byte mfg_id;
  byte dev_id;

  iVar4 = DAT_00008958;
  read_id_cmd = 0x90;
  local_1f = 0;
  local_1e = 0;
  local_1d = 0;
  local_1c = 0;
  local_1b = 0;
  spi_exec_single_transfer(ctx->spi_res,(void *)0x1,6,&read_id_cmd,(uint)auStack_18);
  while( true ) {
    uVar1 = *(uint *)(iVar4 + 0x18);
    if (uVar1 == 0) {
      return 1;
    }
    if (uVar1 == (dev_id | 0x2500 | (uint)mfg_id << 0x10)) break;
    iVar4 = iVar4 + 0x1c;
  }
  uVar5 = 0;
  while (iVar2 = *(int *)(iVar4 + (uVar5 + 2) * 4), iVar2 != 0 && iVar2 != 0x1000) {
    uVar5 = uVar5 + 1 & 0xff;
  }
  if (iVar2 == 0x1000) {
    if (ctx->device_type == 1) {
      if (ctx->info_out != (void *)0x0) {
        if (ctx->log_cb != (code *)0x0) {
          (*ctx->log_cb)(DAT_00008964);
        }
        iVar2 = *(int *)(iVar4 + (uVar5 + 2) * 4);
        uVar1 = (uint)*(ushort *)(iVar4 + uVar5 * 2);
        ctx->end_addr = iVar2 * uVar1 + (ctx->start_addr - 1);
        piVar3 = ctx->info_out;
        *piVar3 = iVar2;
        piVar3[1] = uVar1;
        return 0;
      }
      return 1;
    }
  }
  else {
    if (ctx->log_cb != (code *)0x0) {
      (*ctx->log_cb)(DAT_0000895c,uVar1);
    }
    if (ctx->log_cb != (code *)0x0) {
      (*ctx->log_cb)(DAT_00008960,0x1000);
    }
  }
  return 1;
}
```

Probes the chip with READ_ID (0x90) and matches against a static descriptor table; only accepts 4KB-sector devices. Our v2 driver skips this entirely — we hardcode the SST25VF016B geometry.

---

### `sst25xx_aai_write` @ 0x8968

Plate comment: *SST25VF016B/SST25xx AAI (Auto-Address-Increment) word-program (0xAD). Validates addr via sst25xx_addr_in_bank, programs a leading odd byte via PP (0x02) if start is odd, then loops 2 bytes per AAI transaction (header 0xAD+3-byte addr, then 2 data bytes) polling RDSR.BUSY between iterations. Trailing odd byte is written with a final PP. WREN/WRDI bracket each write phase.*

```c
int sst25xx_aai_write(sst25xx_driver_ctx_t *ctx,uint addr,void *buf,uint len)
{
  uint uVar1;
  byte bVar2;
  byte bVar3;
  int iVar4;
  uint tx_len;
  spi_resource_t *res;
  uint uVar5;
  uint uVar6;
  void *rx_buf;
  uint bank_addr;
  undefined1 aai_cmd;
  undefined1 aai_a23_16;
  undefined1 aai_a15_8;
  undefined1 aai_a7_0;

  bank_addr = addr;
  iVar4 = sst25xx_addr_in_bank(ctx,&bank_addr);
  uVar1 = bank_addr;
  if (iVar4 == 0) {
    uVar5 = 1;
  }
  else {
    uVar6 = len;
    if ((bank_addr & 1) != 0) {
      spi_flash_cmd_wren(ctx);
      spi_flash_cmd_byte_pp(ctx,bank_addr,buf);
      do {
        timer_delay_ms(10);
        bVar2 = spi_flash_cmd_rdsr(ctx);
      } while ((bVar2 & 1) != 0);
      spi_flash_cmd_wrdi(ctx);
      buf = (void *)((int)buf + 1);
      bank_addr = bank_addr + 1;
      uVar6 = len - 1;
    }
    if (1 < uVar6) {
      spi_flash_cmd_wren(ctx);
      bVar2 = 1;
      rx_buf = buf;
      uVar5 = uVar6;
      do {
        res = ctx->spi_res;
        aai_cmd = 0xad;
        aai_a23_16 = (undefined1)(bank_addr >> 0x10);
        aai_a7_0 = (undefined1)bank_addr;
        aai_a15_8 = (undefined1)(bank_addr >> 8);
        spi_resource_acquire(res);
        if (bVar2 == 1) {
          tx_len = 4;
        }
        else {
          tx_len = 1;
        }
        spi_resource_transfer(res,(void *)0x0,tx_len,&aai_cmd,0,0);
        spi_resource_transfer(res,(void *)0x0,2,rx_buf,0,0);
        spi_resource_release(res);
        rx_buf = (void *)((int)rx_buf + 2);
        uVar5 = uVar5 - 2;
        do {
          timer_delay_ms(10);
          bVar3 = spi_flash_cmd_rdsr(ctx);
          bVar2 = bVar3 & 1;
        } while ((bVar3 & 1) != 0);
      } while (1 < uVar5);
      buf = (void *)((int)buf + (uVar6 - 2 & 0xfffffffe) + 2);
      uVar6 = uVar6 & 1;
      spi_flash_cmd_wrdi(ctx);
    }
    uVar5 = 0;
    if (uVar6 != 0) {
      spi_flash_cmd_wren(ctx);
      spi_flash_cmd_byte_pp(ctx,(uVar1 - 1) + len,buf);
      do {
        timer_delay_ms(10);
        bVar2 = spi_flash_cmd_rdsr(ctx);
        uVar5 = bVar2 & 1;
      } while ((bVar2 & 1) != 0);
      spi_flash_cmd_wrdi(ctx);
    }
  }
  return uVar5;
}
```

Canonical AAI programming loop: odd-byte prologue → AAI word loop (full 4-byte header the first iteration, 1-byte thereafter via `spi_resource_acquire/transfer/release` keeping CS asserted) → odd-byte epilogue. Matches the v2 driver's AAI sequence except our driver uses PIO + fixed delays rather than RDSR polling and avoids the `cyg_thread_delay(10ms)` per word (which is why the SDK's 0.55s/sector rate is so much slower than ours).

---

### `sst25xx_sector_erase_banked` @ 0x8b38

Plate comment: *SST25xx 4KB sector erase (opcode 0x20) with bank-relative address translation. Calls sst25xx_addr_in_bank, WREN, then issues a single 4-byte transfer [0x20, A23..A16, A15..A8, A7..A0] and polls RDSR.BUSY with cyg_thread_delay(2).*

```c
int sst25xx_sector_erase_banked(sst25xx_driver_ctx_t *ctx,uint addr)
{
  byte bVar1;
  int iVar2;
  uint uVar3;
  uint bank_addr;
  undefined1 se_cmd;
  undefined1 se_a23_16;
  undefined1 se_a15_8;
  undefined1 se_a7_0;

  bank_addr = addr;
  iVar2 = sst25xx_addr_in_bank(ctx,&bank_addr);
  if (iVar2 == 0) {
    uVar3 = 1;
  }
  else {
    spi_flash_cmd_wren(ctx);
    _se_cmd = CONCAT13((char)bank_addr,
                       CONCAT12((char)(bank_addr >> 8),CONCAT11((char)(bank_addr >> 0x10),0x20)));
    spi_exec_single_transfer(ctx->spi_res,(void *)0x0,4,&se_cmd,0);
    do {
      cyg_thread_delay(2,0);
      bVar1 = spi_flash_cmd_rdsr(ctx);
      uVar3 = bVar1 & 1;
    } while ((bVar1 & 1) != 0);
  }
  return uVar3;
}
```

Standard 4KB sector erase (0x20). The RDSR polling uses `cyg_thread_delay(2, ...)` (2 ticks ≈ 20 ms). Our driver uses a microsecond-scale busy-wait instead, which is safe because we disable interrupts around the op.

---

### `spi_callback_dispatch` @ 0x8bc0

Plate comment: *(none set)*

```c
void spi_callback_dispatch
               (undefined4 param_1,undefined4 param_2,undefined4 param_3,undefined4 param_4)
{
  code *pcVar1;

  pcVar1 = (code *)*DAT_00008be8;
  if (pcVar1 == (code *)0x0) {
    return;
  }
  (*pcVar1)(param_2,*DAT_00008bec,DAT_00008bec,pcVar1,param_4);
  return;
}
```

Tiny generic "invoke the registered callback if non-null" dispatcher; structure-bound so hard to name without its caller context.

---

### `spi_flash_post_dispatch` @ 0x8d60

Plate comment: *(none set)*

```c
void spi_flash_post_dispatch(int param_1)
{
  if (*DAT_0000b234 != *DAT_0000b358) {
    return;
  }
  (*(code *)*DAT_0000b238)
            (DAT_0000b238[1],*(undefined4 *)(param_1 + 0x44),param_1,(code *)*DAT_0000b238,
             *DAT_0000b234);
  return;
}
```

Post-op callback gate: invokes a registered async callback only when two globals match (likely a thread-ID / completion-token check). Not on the hot write path — just used by fss plumbing.

---

### `spi_flash_post_dispatch_2` @ 0x8d6c

Plate comment: *(none set)*

```c
void spi_flash_post_dispatch_2(void)
{
  return;
}
```

Stub — returns immediately. Likely a weakly-linked hook whose strong variant lives elsewhere (or is simply disabled in this build).

---

## DMA buffer params (audio path, not flash)

### `dma_buffer_params_set` @ 0x15c68

Plate comment: *(none set)*

```c
void dma_buffer_params_set(undefined4 param_1,uint *param_2)
{
  int iVar1;
  uint uVar2;

  uVar2 = *param_2;
  if ((byte)param_2[3] < 7) {
    iVar1 = clock_source_lookup((byte)param_2[3]);
  }
  else {
    iVar1 = clock_source_find((char)param_2[1]);
  }
  if (iVar1 != 0) {
    if (*(char *)(iVar1 + 0x17) == '\x01') {
      if (uVar2 < 0x3e81) {
        uVar2 = 10;
      }
      else {
        uVar2 = 0x50;
      }
      param_2[5] = 0x1b9;
      param_2[4] = uVar2;
    }
    else {
      if (uVar2 < 0x3e81) {
        uVar2 = 0x30;
      }
      else {
        uVar2 = 6;
      }
      param_2[5] = uVar2;
      param_2[4] = 1;
    }
    param_2[2] = *(uint *)(iVar1 + 8);
    return;
  }
  return;
}
```

Populates DMA descriptor rate/period parameters for audio streaming based on the current clock source (sample-rate threshold 16001). **Not related to the flash path** — this is for the audio DMA ring (`usb_dma_ring_init`). Listed for completeness per the task brief.

---

## Notes on omissions

- No function named `*xip*` exists in the primary — XIP reads go through the SPI-controller XIP window (`0xCC000000` + offset) directly via CPU loads; there is no software "xip read" helper.
- `fss_read` / `fss_hexdump_read` / `flash_firmware_validate_and_init` are the higher-level filesystem consumers of `sst25xx_read_id` (Fast Read) and are outside the DMA/SPI/flash-core scope requested here.
- No separate `bp_clear` / `block_protect` helper beyond `sst25xx_{clear,set}_block_protection` exists.
