#!/usr/bin/env python3
"""Program the test sector with the driver, then read the first bytes back
two different ways to see if the "+0..+3 = 0xFF" artifact is a READ
pipeline issue rather than a WRITE issue.

Method A: driver's own do_read (dma_bidir_read via mailbox cmd 3)
Method B: independent dma_read_stub.bin (bootloader's read function)
"""
from __future__ import annotations

import subprocess
import sys
import time
from pathlib import Path

FIRMWARE_DIR = Path(__file__).parent
sys.path.insert(0, str(FIRMWARE_DIR.parent))

from firmware.jtag_flash_fast import (
    DRIVER_BIN, DRIVER_CODE, MAILBOX, MB_COMMAND, MB_SPI_ADDR, MB_STATUS,
    READBACK_BUF, ST_DONE_OK, OpenOCD, host_reboot_style_teardown,
)

TEST_SECTOR = 0x03F000
READ_STUB = FIRMWARE_DIR / "dma_read_stub.bin"
READ_CODE = 0x32E00
READ_BUF = 0x2B800


def boot_driver(o):
    o.halt()
    host_reboot_style_teardown(o)
    o.load_image(str(DRIVER_BIN), DRIVER_CODE)
    for i in range(8):
        o.mww(MAILBOX + i * 4, 0)
    o.cmd("arm mcr 15 0 7 5 0 0")
    o.cmd("arm mcr 15 0 7 6 0 0")
    o.cmd(f"reg pc {DRIVER_CODE:#x}")
    o.cmd("reg cpsr 0xd3")
    o.resume()
    time.sleep(0.3)


def mailbox_read(o, spi_addr):
    o.halt()
    for i in range(7):
        o.mww(MAILBOX + i * 4, 0)
    o.mww(MAILBOX + MB_SPI_ADDR, spi_addr)
    o.mww(MAILBOX + MB_COMMAND, 3)  # CMD_READ
    o.resume()
    t0 = time.monotonic()
    while time.monotonic() - t0 < 60:
        time.sleep(0.1)
        o.halt()
        st = o.mdw(MAILBOX + MB_STATUS)
        if st == ST_DONE_OK or st == 0xFF:
            break
        o.resume()
    tmp = "/tmp/diag_mailbox_read.bin"
    o.cmd(f"dump_image {tmp} {READBACK_BUF:#x} 0x1000", timeout=30)
    return Path(tmp).read_bytes()


def stub_read(o, spi_addr, length=0x800):
    if not READ_STUB.exists():
        return None
    o.halt()
    o.mww(0xCC000008, 0)
    o.mww(0xCC000010, 0)
    o.mww(0xCC000014, 2)
    o.mww(0x80000008, 0)
    o.load_image(str(READ_STUB), READ_CODE)
    o.cmd("arm mcr 15 0 7 5 0 0")
    o.cmd(f"reg r0 {spi_addr:#x}")
    o.cmd(f"reg r1 {READ_BUF:#x}")
    o.cmd(f"reg r2 {length:#x}")
    o.cmd(f"reg pc {READ_CODE:#x}")
    o.cmd("reg cpsr 0xd3")
    o.resume()
    time.sleep(0.5)
    o.halt()
    tmp = "/tmp/diag_stub_read.bin"
    o.cmd(f"dump_image {tmp} {READ_BUF:#x} {length:#x}", timeout=30)
    return Path(tmp).read_bytes()


def main():
    subprocess.run(["pkill", "-x", "openocd"], capture_output=True)
    time.sleep(1)

    o = OpenOCD(speed=400)
    try:
        boot_driver(o)
        print("Driver loaded.")
        data_a = mailbox_read(o, TEST_SECTOR)
        print(f"Mailbox read (driver)  first 32 bytes: {data_a[:32].hex(' ')}")

        data_b = stub_read(o, TEST_SECTOR, 0x100)
        if data_b is not None:
            print(f"Stub read (bootloader) first 32 bytes: {data_b[:32].hex(' ')}")

        # After comparing, the next reads should exercise a "warm" driver
        data_a2 = mailbox_read(o, TEST_SECTOR)
        print(f"Mailbox read (repeat)  first 32 bytes: {data_a2[:32].hex(' ')}")
    finally:
        try:
            o.cmd("resume")
        except Exception:
            pass
        o.close()


if __name__ == "__main__":
    main()
