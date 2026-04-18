#!/usr/bin/env python3 -u
"""Flash → verify → independent read-back → host-side diff harness.

Exercises the full v2 pipeline end-to-end and THEN does a third,
independent readback on the host to expose bugs that the driver's
own verify might hide (e.g. when write path and read path share a
corruption that silently self-cancels during on-device verify).

Usage:
    python3 firmware/debug_flash_harness.py --sector 0x3F000
"""
from __future__ import annotations

import argparse
import os
import subprocess
import sys
import time
from pathlib import Path

FIRMWARE_DIR = Path(__file__).resolve().parent
sys.path.insert(0, str(FIRMWARE_DIR.parent))

from firmware.jtag_flash_v2 import (  # noqa: E402
    OpenOCD, FlashClientV2, DATA_BUF_ADDR, SECTOR_SIZE,
    CMD_READ, MBOX_ADDR, O_STATUS, O_COMMAND, O_FLASH_ADDR, O_BUF_ADDR,
    O_LENGTH, O_MAGIC, MAGIC_CMD,
)


def make_pattern(seed: int, n: int) -> bytes:
    """Deterministic high-entropy pattern so any shift / drop is visible."""
    import hashlib
    out = bytearray()
    ctr = 0
    while len(out) < n:
        out.extend(hashlib.sha256(f"{seed}:{ctr}".encode()).digest())
        ctr += 1
    return bytes(out[:n])


def compare_buffers(label_a, a, label_b, b, *, max_shown=12):
    """Return (n_diffs, trailing_ff_run) and print the first few deltas."""
    if len(a) != len(b):
        print(f"!! length mismatch {label_a}={len(a)} vs {label_b}={len(b)}")
        return -1, 0
    diffs = [i for i in range(len(a)) if a[i] != b[i]]
    print(f"{label_a} vs {label_b}: {len(diffs)} diffs")
    for i in diffs[:max_shown]:
        print(f"  +0x{i:04x}: {label_a}={a[i]:02x} {label_b}={b[i]:02x}")
    # Trailing 0xFF run from the end of b?
    trail = 0
    for i in reversed(range(len(b))):
        if b[i] == 0xFF and a[i] != 0xFF:
            trail += 1
        else:
            break
    return len(diffs), trail


def main():
    p = argparse.ArgumentParser(description=__doc__)
    p.add_argument("--sector", type=lambda x: int(x, 0), default=0x3F000,
                   help="Test sector address (default 0x3F000 — scratch)")
    p.add_argument("--speed", type=int, default=1000)
    p.add_argument("--length", type=lambda x: int(x, 0), default=SECTOR_SIZE,
                   help="Payload bytes to flash (default 4096)")
    p.add_argument("--mode", choices=("sector", "block"), default="sector",
                   help="sector = V2_CMD_FLASH_SECTOR, block = V2_CMD_FLASH_BLOCK")
    args = p.parse_args()

    pattern = make_pattern(args.sector, args.length)
    print(f"Test: {args.mode} at 0x{args.sector:06x} len={args.length}")
    print(f"  pattern[0:8]   = {pattern[:8].hex(' ')}")
    print(f"  pattern[-8:]   = {pattern[-8:].hex(' ')}")

    o = OpenOCD(speed=args.speed)
    try:
        client = FlashClientV2(o)
        client.load_driver()

        if not client.bp_clear():
            raise SystemExit("bp_clear failed")

        # 1. Do the autonomous flash via v2 driver.
        t0 = time.monotonic()
        if args.mode == "block":
            ok = client.flash_block(args.sector, pattern)
        else:
            ok = client.flash_sector(args.sector, pattern)
        dt = time.monotonic() - t0
        if not ok:
            print(f"  driver flash_{args.mode} returned FAIL after {dt:.2f}s — "
                  f"continuing to read-back anyway for diff investigation")
        else:
            print(f"  driver said OK in {dt:.2f}s (erase+program+verify)")

        # 2. Read back via the driver's own READ path in small pieces so
        #    we can see per-chunk behaviour even if the full read hits a
        #    read-path bug at chunk boundaries.
        rb_pieces = []
        for off in range(0, args.length, SECTOR_SIZE):
            chunk = min(SECTOR_SIZE, args.length - off)
            piece = client.read(args.sector + off, chunk)
            if piece is None:
                print(f"  !! driver read piece failed at +0x{off:x}")
                piece = b"\xFF" * chunk
            rb_pieces.append(piece)
        rb_driver = b"".join(rb_pieces)

        # 3. Independent halt-read skipped (driver may be in ERR state
        #    after a failed flash_block; CMD_READ wouldn't re-arm cleanly).
        rb_halt = b""  # placeholder so compare logic still runs

        # 4. True independent read via JTAG-driven DMA (no v2 driver, no
        #    stub executed on-target). This exposes driver read-path bugs.
        stub_data = jtag_dma_read(o, args.sector, args.length)

        # 5. Compare everything.
        print()
        print(f"  flash[first 16] driver-read = {rb_driver[:16].hex(' ')}")
        print(f"  flash[first 16] halt-read   = {rb_halt[:16].hex(' ')}")
        if stub_data is not None:
            print(f"  flash[first 16] stub-read   = {stub_data[:16].hex(' ')}")
        print(f"  flash[last 16 ] driver-read = {rb_driver[-16:].hex(' ')}")
        print(f"  flash[last 16 ] halt-read   = {rb_halt[-16:].hex(' ')}")
        if stub_data is not None:
            print(f"  flash[last 16 ] stub-read   = {stub_data[-16:].hex(' ')}")

        d1, t1 = compare_buffers("pattern", pattern, "driver-read", rb_driver)
        d2, t2 = compare_buffers("pattern", pattern, "halt-read",   rb_halt)
        d3, t3 = (-1, 0)
        if stub_data is not None:
            d3, t3 = compare_buffers("pattern", pattern, "stub-read", stub_data)
        d4, _  = compare_buffers("driver-read", rb_driver, "halt-read", rb_halt)
        if stub_data is not None:
            d5, _ = compare_buffers("driver-read", rb_driver, "stub-read", stub_data)

        print()
        print(f"  summary: driver-diffs={d1} halt-diffs={d2} stub-diffs={d3}"
              f"   driver==halt? {d4==0}  trailing_ff(driver)={t1}"
              f" trailing_ff(halt)={t2} trailing_ff(stub)={t3}")
        return 0 if d1 == 0 and d2 == 0 and (d3 == 0 or stub_data is None) else 1
    finally:
        o.close()


SPI_BASE = 0xCC000000
DMA_BASE = 0x80000000
TX_BUF   = 0x28000      # BIDIR_TX_BUF in v2 driver
RX_BUF   = 0x2A000      # RX_TEMP     in v2 driver
READ_RX_SKIP = 12       # empirical: 4 cmd + 8 RX-pipeline bytes before first data
READ_CHUNK   = 0x800    # v2 driver's per-chunk size (xfer_len ≤ 0xFFF)
SST_CMD_READ = 0x03


def _dma_wait_done(o, timeout=2.0):
    deadline = time.monotonic() + timeout
    while time.monotonic() < deadline:
        if o.mdw(DMA_BASE + 0x14) & 1:
            return True
        time.sleep(0.005)
    return False


def jtag_dma_read(o, spi_addr, length):
    """Independent read path: host-driven bidirectional DMA via JTAG.

    Mirrors sram_flash_driver_v2.c dma_bidir_read() exactly — driven
    entirely from the host, so the target stays halted and no code
    executes on device. DMA ch1 shifts out [0x03, A23, A15, A7, 0…]
    via SPI_TX_PORT; ch0 simultaneously captures bytes from SPI_RX_PORT
    into RX_TEMP. First READ_RX_SKIP bytes are command-phase noise, the
    next `len` bytes are the actual flash data.
    """
    out = bytearray()
    for off in range(0, length, READ_CHUNK):
        chunk = min(READ_CHUNK, length - off)
        xfer_len = READ_RX_SKIP + chunk
        if xfer_len > 0xFFF:
            raise ValueError("chunk too large for 12-bit DMA length field")
        addr = spi_addr + off

        # Build TX buffer (cmd + 3-byte addr, rest zero) on disk, load to SRAM.
        tx = bytearray(xfer_len)
        tx[0] = SST_CMD_READ
        tx[1] = (addr >> 16) & 0xFF
        tx[2] = (addr >> 8)  & 0xFF
        tx[3] =  addr        & 0xFF
        tx_path = "/tmp/v2_dbg_tx.bin"
        Path(tx_path).write_bytes(bytes(tx))

        o.halt()
        # Wait for any in-flight SPI transaction to finish before reconfig.
        while o.mdw(SPI_BASE + 0x28) & 1:
            pass

        # Clean SPI + DMA state.
        o.mww(SPI_BASE + 0x08, 0)            # EN=0
        o.mww(SPI_BASE + 0x10, 0)            # CS=0
        o.mww(SPI_BASE + 0x2C, 0)            # DMAGO=0
        o.mww(DMA_BASE + 0x08, 0)            # DMA EN=0
        o.mww(DMA_BASE + 0x10, 0x3)          # ICLR ch0+ch1
        o.mww(DMA_BASE + 0x30, 1)            # CHCLR ch0
        o.mww(DMA_BASE + 0x34, 1)            # CHCLR ch1

        o.load_image(tx_path, TX_BUF)

        # SPI: bidirectional DMA framing.
        o.mww(SPI_BASE + 0x00, 0x007)        # CTRL: bidir
        o.mww(SPI_BASE + 0x04, xfer_len - 1) # LEN
        o.mww(SPI_BASE + 0x4C, 3)            # DMAMD: bidir
        o.mww(SPI_BASE + 0x14, 2)            # CLK divider
        o.mww(SPI_BASE + 0x50, 4)            # DMACFG0
        o.mww(SPI_BASE + 0x54, 3)            # DMACFG1
        o.mww(SPI_BASE + 0x08, 1)            # EN

        # DMA: ch0=RX (SPI_RX_PORT -> RX_BUF), ch1=TX (TX_BUF -> SPI_TX_PORT).
        o.mww(DMA_BASE + 0x08, 3)            # enable ch0+ch1
        o.mww(DMA_BASE + 0x10, 0x3)          # clear IRQs

        o.mww(DMA_BASE + 0x110, 0)           # ch0 TRG=0
        o.mww(DMA_BASE + 0x100, SPI_BASE + 0x70)     # SRC = SPI_RX_PORT
        o.mww(DMA_BASE + 0x104, RX_BUF)              # DST
        o.mww(DMA_BASE + 0x108, 0)
        o.mww(DMA_BASE + 0x10C, (xfer_len & 0xFFF) | 0x88009000)
        o.mww(DMA_BASE + 0x110, 0xD007)              # RX trigger

        o.mww(DMA_BASE + 0x130, 0)           # ch1 TRG=0
        o.mww(DMA_BASE + 0x120, TX_BUF)              # SRC
        o.mww(DMA_BASE + 0x124, SPI_BASE + 0x80)     # DST = SPI_TX_PORT
        o.mww(DMA_BASE + 0x128, 0)
        o.mww(DMA_BASE + 0x12C, 0xFFF | 0xF4009000)  # CFG: 0xFFF + DMA_CFG_TX
        o.mww(DMA_BASE + 0x130, 0xD005)              # TX trigger

        # Kick off.
        o.mww(SPI_BASE + 0x10, 1)            # CS assert
        o.mww(SPI_BASE + 0x18, 0)            # CLRINT

        if not _dma_wait_done(o):
            print(f"  !! jtag_dma_read: DMA timeout at +0x{off:x} "
                  f"(ISTAT={o.mdw(DMA_BASE + 0x14):#x})")
            out += b"\xFF" * chunk
        else:
            # Cleanup.
            o.mww(SPI_BASE + 0x10, 0)
            o.mww(SPI_BASE + 0x08, 0)
            o.mww(DMA_BASE + 0x08, 0)
            o.mww(DMA_BASE + 0x10, 0x3)
            raw = o.dump_image(RX_BUF, xfer_len)
            out += raw[READ_RX_SKIP:READ_RX_SKIP + chunk]

        try: os.unlink(tx_path)
        except FileNotFoundError: pass
    return bytes(out[:length])


if __name__ == "__main__":
    raise SystemExit(main())
