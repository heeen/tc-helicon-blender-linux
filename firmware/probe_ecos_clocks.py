#!/usr/bin/env python3
"""Probe live eCos clock/SPI/timer register state via JTAG.

Halt the ARM briefly, dump the registers we rely on being in a known
state after `peripheral_full_teardown()`, then resume. The purpose is to
verify (not assume) what eCos actually leaves behind before the v2
driver takes over.
"""

import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).parent))
from jtag_flash_v2 import OpenOCD  # noqa: E402


# (name, addr, [interesting bit breakdown])
PROBES = [
    # Clock block 0xC9000000
    ("CLK_DIV_SPI     @0xC9000014", 0xC9000014, "0x0000=crystal fallback, 0x8000=400MHz PLL passthrough"),
    ("CLK_PLL         @0xC9000004", 0xC9000004, "PLL config"),
    ("CLK_DIV_CPU     @0xC9000008", 0xC9000008, "CPU clock divider"),
    ("CLK_DIV_AHB     @0xC900000C", 0xC900000C, "AHB clock divider"),
    ("CLK_DIV_APB     @0xC9000010", 0xC9000010, "APB clock divider"),
    ("CLK_DIV_LEDSPI  @0xC9000018", 0xC9000018, "LED SPI divider"),
    ("PIN_MUX?        @0xC9000034", 0xC9000034, "pin mux / SPI pin routing"),

    # Flash SPI controller 0xCC000000
    ("SPI_CTRL        @0xCC000000", 0xCC000000, "bit0=EN, bit1=CPOL?, bit12=desc-ring IRQ"),
    ("SPI_LEN         @0xCC000004", 0xCC000004, "transfer length - 1"),
    ("SPI_EN          @0xCC000008", 0xCC000008, "enable"),
    ("SPI_CLK         @0xCC000014", 0xCC000014, "wire clock divider (we hardcode 2)"),
    ("SPI_CS          @0xCC000018", 0xCC000018, "CS (1=assert)"),
    ("SPI_STAT        @0xCC000028", 0xCC000028, "bit0=BUSY, bit3=RX_RDY"),
    ("SPI_DMAMD       @0xCC00002C", 0xCC00002C, "DMA mode (0=off, 2=TX, 3=BIDIR)"),
    ("SPI_DMAGO       @0xCC000034", 0xCC000034, "DMA go"),
    ("SPI_DMACFG0     @0xCC00004C", 0xCC00004C, "DMA cfg 0"),
    ("SPI_DMACFG1     @0xCC000050", 0xCC000050, "DMA cfg 1"),
    ("SPI_CLRINT      @0xCC000054", 0xCC000054, "clear interrupt"),

    # DMA engine @0x80000000
    ("DMA_CFG         @0x80000008", 0x80000008, "bit0=enable"),
    ("DMA_ISTAT       @0x80000010", 0x80000010, "channel interrupt status"),
    ("DMA_CH0_CFG     @0x8000010C", 0x8000010C, "channel 0 config"),
    ("DMA_CH0_GO      @0x80000110", 0x80000110, "channel 0 go"),
    ("DMA_CH1_CFG     @0x8000012C", 0x8000012C, "channel 1 config"),

    # Timer @0xC2000000
    ("TIM0_COUNT      @0xC2000004", 0xC2000004, "current count (18 MHz tick)"),
    ("TIM0_CTRL       @0xC2000008", 0xC2000008, "bit0=enable"),
    ("TIM0_RELOAD     @0xC2000000", 0xC2000000, "reload value"),

    # VIC @0xFFFFF000
    ("VIC_INT_EN      @0xFFFFF010", 0xFFFFF010, "unmasked IRQs"),
    ("VIC_RAW_INT     @0xFFFFF008", 0xFFFFF008, "raw interrupt lines"),
    ("VIC_IRQ_STAT    @0xFFFFF000", 0xFFFFF000, "active masked IRQs"),

    # LED SPI 0xCF000000 — for reference
    ("LED_SPI_CTRL    @0xCF000000", 0xCF000000, "LED SPI controller CTRL"),
    ("LED_SPI_STAT    @0xCF000028", 0xCF000028, "LED SPI BUSY"),
]


def main():
    import argparse
    ap = argparse.ArgumentParser()
    ap.add_argument("--nreset", action="store_true",
                    help="Pulse nRESET first, land in TCAT-BOOT.")
    args = ap.parse_args()

    from jtag_flash_v2 import OPENOCD_CFG_SRST
    cfg = OPENOCD_CFG_SRST if args.nreset else None
    ocd = OpenOCD(speed=1000, openocd_cfg=cfg)
    if args.nreset:
        print("Resetting to TCAT-BOOT and halting…")
        ocd.reset_halt()
    else:
        print("Halting eCos for probe…")
        ocd.halt()
    print("Reading registers:\n")

    # CPU PC to show where we interrupted eCos.
    try:
        pc = ocd.cmd("reg pc")
        print(f"CPU PC: {pc.strip()}\n")
    except Exception as e:
        print(f"(could not read PC: {e})\n")

    for name, addr, note in PROBES:
        try:
            v = ocd.mdw(addr)
            marker = ""
            if addr == 0xC9000014:
                if v == 0x0000:
                    marker = "  ← crystal fallback"
                elif v == 0x8000:
                    marker = "  ← 400 MHz PLL passthrough (TEARDOWN DID NOT RESET)"
                else:
                    marker = "  ← OTHER value"
            if addr == 0xCC000014 and v != 0:
                marker = f"  ← wire clk divider = {v}"
            if addr == 0xCC000028:
                bits = []
                if v & 1: bits.append("BUSY")
                if v & 8: bits.append("RX_RDY")
                if bits:
                    marker = f"  ← {','.join(bits)}"
            print(f"  {name:30s} = 0x{v:08X}  {marker}")
            if note:
                print(f"       {note}")
        except Exception as e:
            print(f"  {name:30s} = ERR {e}")

    print("\nResuming target…")
    ocd.resume()


if __name__ == "__main__":
    main()
