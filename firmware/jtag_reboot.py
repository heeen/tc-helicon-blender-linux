#!/usr/bin/env python3
"""Upload reboot stub to SRAM and execute it via JTAG."""

import argparse
import time

from jtag_common import (
    REBOOT_ADDR,
    REBOOT_BIN,
    REBOOT_ELF,
    REBOOT_STACK,
    BOOTLOADER_START,
    BOOTLOADER_END,
    OpenOCDSession,
    classify_pc,
    read_elf_entry,
)


def run_reboot(speed=1000, classify=True, settle=2.0):
    if not REBOOT_BIN.exists() or not REBOOT_ELF.exists():
        print(f"ERROR: missing reboot artifacts: {REBOOT_BIN} / {REBOOT_ELF}")
        return 1

    print(f"Reboot stub: {REBOOT_BIN} ({REBOOT_BIN.stat().st_size} bytes)")
    entry = read_elf_entry(REBOOT_ELF)

    o = OpenOCDSession(speed=speed)
    try:
        o.resume()
        time.sleep(0.05)
        print("Halting CPU...")
        o.halt()
        print(f"Loading stub to {REBOOT_ADDR:#x}, entry at {entry:#x}")
        o.load_image(str(REBOOT_BIN), REBOOT_ADDR)

        o.cmd("arm mcr 15 0 7 5 0 0")
        o.reg_set("pc", entry)
        o.reg_set("cpsr", 0xD3)
        for reg_name in ("sp", "sp_svc", "sp_usr", "sp_irq", "sp_abt", "sp_und"):
            try:
                o.reg_set(reg_name, REBOOT_STACK)
            except Exception:
                pass

        print("Resuming reboot stub...")
        o.resume()
        time.sleep(settle)

        if not classify:
            return 0

        samples = []
        for wait_s in (1.0, 3.0, 6.0):
            time.sleep(wait_s)
            o.halt()
            pc = o.reg_get("pc")
            cpsr = o.reg_get("cpsr")
            samples.append((wait_s, pc, cpsr, classify_pc(pc)))
            o.resume()

        print("Reboot state samples:")
        for wait_s, pc, cpsr, tag in samples:
            print(f"  +{wait_s:>4.1f}s pc=0x{pc:08x} cpsr=0x{cpsr:08x} ({tag})")

        last_pc = samples[-1][1]
        if BOOTLOADER_START <= last_pc < BOOTLOADER_END:
            print("ERROR: bootloader_stuck (post-reboot PC stayed in 0x4F000 range)")
            return 2
        return 0
    finally:
        o.close()


def main(argv=None):
    p = argparse.ArgumentParser(description=__doc__)
    p.add_argument("--speed", type=int, default=1000, help="JTAG speed in kHz")
    p.add_argument("--no-classify", action="store_true",
                   help="Skip post-reboot PC state classification")
    p.add_argument("--settle", type=float, default=2.0,
                   help="Seconds to wait immediately after resume")
    args = p.parse_args(argv)
    return run_reboot(speed=args.speed, classify=not args.no_classify, settle=args.settle)


if __name__ == "__main__":
    raise SystemExit(main())
