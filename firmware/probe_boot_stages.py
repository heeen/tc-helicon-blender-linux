#!/usr/bin/env python3
"""Walk through boot stages after nRESET, sampling PC + a couple regs
at each dwell time. Shows which stage we're actually in when the v2
driver's --nreset + 1500ms wait hands off."""

import sys
import time
from pathlib import Path

sys.path.insert(0, str(Path(__file__).parent))
from jtag_flash_v2 import OpenOCD, OPENOCD_CFG_SRST  # noqa: E402

DWELLS_MS = [0, 50, 150, 300, 500, 1000, 1500, 2500, 4000]


def classify_pc(pc):
    if pc < 0x200:
        return "ROM (reset vectors / handlers)"
    if pc < 0x2000:
        return "ROM code region"
    if pc < 0x10000:
        return "SRAM low — stage-2 bootloader?"
    if pc < 0x40000:
        return "SRAM mid — probably eCos"
    if pc < 0x80000:
        return "SRAM high"
    if 0x4F000 <= pc <= 0x50000:
        return "XIP window (stage-2 XIP)"
    if pc >= 0xCC000000:
        return "??? peripheral range"
    return "unknown"


def main():
    ocd = OpenOCD(speed=1000, openocd_cfg=OPENOCD_CFG_SRST)

    print(f"{'dwell':>8} {'PC':>10} {'CLK_SPI':>9} {'TIM_CNT':>10} {'SPI_CTRL':>10}  where")
    for ms in DWELLS_MS:
        ocd.cmd("reset run", timeout=30)
        time.sleep(ms / 1000.0)
        ocd.cmd("halt", timeout=30)
        try:
            pc_line = ocd.cmd("reg pc")
            pc = int(pc_line.split()[-1].strip(":"), 0)
        except Exception:
            pc = 0
        try:
            clk = ocd.mdw(0xC9000014)
            tim = ocd.mdw(0xC2000004)
            ctrl = ocd.mdw(0xCC000000)
        except Exception:
            clk = tim = ctrl = 0
        print(f"{ms:>6}ms  0x{pc:08X}  0x{clk:06X}  0x{tim:08X}  0x{ctrl:08X}  {classify_pc(pc)}")

    ocd.resume()


if __name__ == "__main__":
    main()
