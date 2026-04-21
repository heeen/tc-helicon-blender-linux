#!/usr/bin/env python3
"""Snapshot SPI/DMA/timer state multiple times while eCos runs,
to detect autonomous activity that could interfere with the v2 driver.

For each register we sample N times (with the CPU resumed between
halts), then report unique values observed. Anything that changes
between samples indicates an autonomous source of SPI/DMA ops.
"""

import sys
import time
from collections import Counter
from pathlib import Path

sys.path.insert(0, str(Path(__file__).parent))
from jtag_flash_v2 import OpenOCD  # noqa: E402

PROBES = [
    # Flash SPI controller
    ("SPI_CTRL     @0xCC000000", 0xCC000000),
    ("SPI_EN       @0xCC000008", 0xCC000008),
    ("SPI_CS       @0xCC000018", 0xCC000018),
    ("SPI_STAT     @0xCC000028", 0xCC000028),
    ("SPI_DMAMD    @0xCC00002C", 0xCC00002C),
    ("SPI_DMAGO    @0xCC000034", 0xCC000034),
    ("SPI_ERR      @0xCC000038", 0xCC000038),   # SPI_ERR_OFF per dice3_hw.h
    # LED SPI controller
    ("LED_CTRL     @0xCF000000", 0xCF000000),
    ("LED_EN       @0xCF000008", 0xCF000008),
    ("LED_CS       @0xCF000018", 0xCF000018),
    ("LED_STAT     @0xCF000028", 0xCF000028),
    ("LED_DMAMD    @0xCF00002C", 0xCF00002C),
    ("LED_DMAGO    @0xCF000034", 0xCF000034),
    # DMA engine
    ("DMA_CFG      @0x80000008", 0x80000008),
    ("DMA_ISTAT    @0x80000010", 0x80000010),
    ("DMA_CH0_SRC  @0x80000100", 0x80000100),
    ("DMA_CH0_DST  @0x80000104", 0x80000104),
    ("DMA_CH0_CNT  @0x8000010C", 0x8000010C),
    ("DMA_CH0_GO   @0x80000110", 0x80000110),
    ("DMA_CH1_SRC  @0x80000120", 0x80000120),
    ("DMA_CH1_DST  @0x80000124", 0x80000124),
    ("DMA_CH1_CNT  @0x8000012C", 0x8000012C),
    ("DMA_CH1_GO   @0x80000130", 0x80000130),
    ("DMA_CH2_CFG  @0x80000140", 0x80000140),
    ("DMA_CH3_CFG  @0x80000160", 0x80000160),
    # Timer
    ("TIM0_COUNT   @0xC2000004", 0xC2000004),
    # VIC state
    ("VIC_RAW_INT  @0xFFFFF008", 0xFFFFF008),
    ("VIC_INT_EN   @0xFFFFF010", 0xFFFFF010),
    ("VIC_VECT_A0  @0xFFFFF100", 0xFFFFF100),
    ("VIC_VECT_C0  @0xFFFFF200", 0xFFFFF200),
]

N_SAMPLES = 10
DWELL_MS = 100


def main():
    ocd = OpenOCD(speed=1000)
    # Collect samples.
    samples = [[] for _ in PROBES]
    for i in range(N_SAMPLES):
        ocd.halt()
        for idx, (_, addr) in enumerate(PROBES):
            try:
                samples[idx].append(ocd.mdw(addr))
            except Exception:
                samples[idx].append(None)
        ocd.resume()
        time.sleep(DWELL_MS / 1000.0)

    print(f"\n=== {N_SAMPLES} snapshots, {DWELL_MS} ms apart ===\n")
    for (name, addr), vals in zip(PROBES, samples):
        uniq = list(dict.fromkeys([v for v in vals if v is not None]))
        ctr = Counter(vals)
        changing = len(uniq) > 1
        flag = "  ← CHANGING" if changing else ""
        if changing:
            spread = sorted(uniq)
            print(f"{name}: {len(uniq)} unique values{flag}")
            for v in spread:
                c = ctr[v]
                print(f"    0x{v:08X}  ×{c}")
        else:
            v = uniq[0] if uniq else 0
            print(f"{name}: 0x{v:08X}  (stable)")

    ocd.halt()
    print("\nResuming...")
    ocd.resume()


if __name__ == "__main__":
    main()
