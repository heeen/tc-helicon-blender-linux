#!/usr/bin/env python3 -u
"""Host-side client for the v2 SRAM flash driver.

Speaks the mailbox protocol defined in sram_flash_mailbox_v2.h:
  * Single-writer-per-field (closes the v1 init race).
  * Phase-based progress polling (no halt required — mdw while running works).
  * Structured log ring dump on ERROR for postmortem without guessing.

CLI:
    python3 firmware/blender_tool.py flash --ref firmware/blender_spi_patched.bin \\
        --speed 1000 --test-sector 0x3f000
    python3 firmware/blender_tool.py flash --mode flash-all --nreset
    python3 firmware/blender_tool.py flash --mode flash-all --reboot
"""
from __future__ import annotations

import argparse
import datetime as dt
import os
import socket
import struct
import subprocess
import sys
import time
import zlib
from pathlib import Path

from jtag_common import DRIVER_V2_BIN
from jtag_reboot import run_reboot

FIRMWARE_DIR = Path(__file__).resolve().parent
ROOT = FIRMWARE_DIR.parent
OPENOCD_CFG = ROOT / "jtag" / "miolink-dice3-openocd.cfg"
# Hardware nRST enabled — required for `reset halt` to pulse the target.
OPENOCD_CFG_SRST = ROOT / "jtag" / "miolink-dice3-openocd-srst.cfg"
DRIVER_BIN = DRIVER_V2_BIN

# Keep in sync with sram_flash_mailbox_v2.h.
MBOX_ADDR        = 0x0002E400
TIMINGS_ADDR     = 0x0002E580
LOG_RING_ADDR    = 0x0002E600
DATA_BUF_ADDR    = 0x0002B000
IMAGE_BASE_ADDR  = 0x0002F900
SECTOR_LIST_ADDR = 0x0002F100
DRIVER_CODE_ADDR = 0x0002C000

# v2_timings struct (matches sram_flash_mailbox_v2.h). All u32 LE.
TIMINGS_MAGIC = 0x54494D32   # 'TIM2'
TIMING_FIELDS = (
    "aai_pair_fixed_us",
    "aai_pair_poll_interval_us",
    "aai_pair_poll_budget_us",
    "pre_wrdi_fixed_us",
    "pre_wrdi_poll_interval_us",
    "pre_wrdi_poll_budget_us",
    "post_wrdi_fixed_us",
    "post_wrdi_poll_interval_us",
    "post_wrdi_poll_budget_us",
    "erase_fixed_us",
    "erase_poll_interval_us",
    "erase_poll_budget_us",
    "xport_mode",
    "byte_prog_offset",
    "verify_quiesce_mode",
    "log_silent",
    "read_mode",
    "rx_sample_dly",      # DW SSI +0xF0 (0..7)
    "spi_mode_bits",      # 0 = Mode 0, 0xC0 = Mode 3 (SCPH+SCPOL)
    "flash_ser",          # 1 = bit 0 (current v2), 4 = bit 2 (stock-aligned)
    "aai_sync_mode",      # 0 = FAST (busy_wait only), 1 = STOCK (busy_wait + RDSR poll)
)
XPORT_DMA    = 0   # bidir DMA: CH0 RX + CH1 TX, TMOD=0  (CTRL=0x007)
XPORT_PIO    = 1   # PIO byte-by-byte (broken on this silicon)
XPORT_DMA_TX = 2   # TX-only DMA: CH1 only, TMOD=1 (CTRL=0x107). AAI tail-drop probe.
READ_MODE_BIDIR  = 0
READ_MODE_RXONLY = 1
TIMING_DEFAULTS = {
    "aai_pair_fixed_us":          10,
    "aai_pair_poll_interval_us":  5,
    "aai_pair_poll_budget_us":    100,
    "pre_wrdi_fixed_us":          40,
    "pre_wrdi_poll_interval_us":  500,
    "pre_wrdi_poll_budget_us":    5000,
    "post_wrdi_fixed_us":         200,
    "post_wrdi_poll_interval_us": 500,
    "post_wrdi_poll_budget_us":   20000,
    "erase_fixed_us":             60000,
    "erase_poll_interval_us":     2000,
    "erase_poll_budget_us":       500000,
    "xport_mode":                 XPORT_DMA,
    "byte_prog_offset":           0,
    "verify_quiesce_mode":        0,
    "log_silent":                 0,
    "read_mode":                  READ_MODE_RXONLY,
    "rx_sample_dly":              0,
    "spi_mode_bits":              0,
    "flash_ser":                  1,
    "aai_sync_mode":              0,
}

MAGIC_CMD = 0x4D324657  # 'M2FW'

# Mailbox field offsets
O_MAGIC        = 0x00
O_COMMAND      = 0x04
O_FLASH_ADDR   = 0x08
O_BUF_ADDR     = 0x0C
O_LENGTH       = 0x10
O_STATUS       = 0x14
O_PHASE        = 0x18
O_PHASE_DETAIL = 0x1C
O_LAST_SR      = 0x20
O_ERR_CODE     = 0x24
O_ERR_ADDR     = 0x28
O_ELAPSED_US   = 0x2C
O_SEQ          = 0x30
O_LOG_HEAD     = 0x34
O_LOG_TAIL     = 0x38
O_PAIR_RETRIES = 0x3C
O_BUILD_TAG    = 0x40
O_MISS_OFFSET  = 0x44
O_MISS_GOT     = 0x48
O_MISS_EXP     = 0x4C
O_MISS_SPI_ST  = 0x50
O_MISS_DMA_IST = 0x54
O_MISS_READS   = 0x58
O_VERIFY_RTRY  = 0x5C

# Last-chunk DMA/SPI snapshot (updated at end of every dma_bidir_read).
O_LAST_SPI_ERR = 0x60
O_LAST_SPI_PND = 0x64
O_LAST_DMA_ST  = 0x68
O_LAST_CH0_CTL = 0x6C
O_LAST_CH0_CFG = 0x70
O_LAST_CH1_CTL = 0x74
O_LAST_CH1_CFG = 0x78
O_LAST_DMA_EN  = 0x7C

# VERIFY_MISS snapshot of the same (preserved for post-mortem).
O_MISS_SPI_ERR = 0x80
O_MISS_SPI_PND = 0x84
O_MISS_DMA_ST  = 0x88
O_MISS_CH0_CTL = 0x8C
O_MISS_CH0_CFG = 0x90
O_MISS_CH1_CTL = 0x94
O_MISS_CH1_CFG = 0x98
O_MISS_DMA_EN  = 0x9C
O_LAST_PRE_ST  = 0xA0
O_LAST_PRE_ERR = 0xA4
O_MISS_PRE_ST  = 0xA8
O_MISS_PRE_ERR = 0xAC
O_LAST_RX_H0   = 0xB0
O_LAST_RX_H1   = 0xB4
O_MISS_RX_H0   = 0xB8
O_MISS_RX_H1   = 0xBC
O_LAST_CH2_CFG = 0xC0
O_LAST_CH3_CFG = 0xC4
O_LAST_CH4_CFG = 0xC8
O_LAST_CH5_CFG = 0xCC
O_LAST_CH6_CFG = 0xD0
O_LAST_CH7_CFG = 0xD4
O_MISS_CH2_CFG = 0xD8
O_MISS_CH3_CFG = 0xDC
O_MISS_CH4_CFG = 0xE0
O_MISS_CH5_CFG = 0xE4
O_MISS_CH6_CFG = 0xE8
O_MISS_CH7_CFG = 0xEC
O_LAST_RDSR_AF = 0xF0
O_MISS_RDSR_AF = 0xF4
O_MISS_RDSR_BF = 0xF8
O_FIRST_RDSR   = 0xFC
O_LAST_DMA_CMB = 0x100
O_LAST_DMA_ERR = 0x104
O_LAST_DMA_RAW = 0x108
O_LAST_DMA_ENB = 0x10C
O_LAST_SPI_40  = 0x110
O_MISS_DMA_CMB = 0x114
O_MISS_DMA_ERR = 0x118
O_MISS_DMA_RAW = 0x11C
O_MISS_DMA_ENB = 0x120
O_MISS_SPI_40  = 0x124
O_HASH_RESULT  = 0x128

STATUS_READY = 0
STATUS_BUSY  = 1
STATUS_OK    = 2
STATUS_ERR   = 3

# Commands
CMD_IDLE      = 0
CMD_ERASE     = 1
CMD_PROGRAM   = 2
CMD_READ      = 3
CMD_VERIFY    = 4
CMD_BP_CLEAR  = 5
CMD_FLASH_ALL = 6
CMD_QUIT      = 7
CMD_ERASE_32K = 8
CMD_ERASE_64K = 9
CMD_FLASH_SECTOR = 10
CMD_FLASH_BLOCK  = 11
CMD_BYTE_PATCH   = 12
CMD_READ_ID      = 13
CMD_HASH_RANGE   = 14

PHASE_NAMES = {
    0: "INIT", 1: "TEARDOWN", 2: "T0_ENABLE", 3: "IDLE", 4: "BP_CLEAR",
    5: "ERASE_WREN", 6: "ERASE_CMD", 7: "ERASE_POLL", 8: "AAI_WREN",
    9: "AAI_FIRST", 10: "AAI_PAIR", 11: "AAI_WRDI", 12: "VERIFY",
    13: "READ", 14: "DONE", 15: "ERROR",
}

EVT_NAMES = {
    0x01: "SETUP_BEGIN", 0x02: "SETUP_DONE", 0x03: "T0_ENABLED",
    0x04: "CMD_RX",
    0x10: "BP_CLEAR_PRE", 0x11: "BP_CLEAR_POST",
    0x20: "ERASE_WREN", 0x21: "ERASE_CMD", 0x22: "ERASE_DONE",
    0x30: "AAI_WREN", 0x31: "AAI_FIRST", 0x32: "AAI_PAIR",
    0x33: "AAI_BUSY_FAST", 0x34: "AAI_BUSY_POLL", 0x35: "AAI_WRDI",
    0x40: "VERIFY_OK", 0x41: "VERIFY_MISS", 0x42: "READ_CHUNK_SR",
    0x43: "READ_CHUNK_WEL", 0x44: "READ_LONG_RDSR",
    0x50: "REPAIR_HIT",
    0x60: "DMA_TX_DONE", 0x61: "DMA_RX_DONE",
    0xF0: "TIMEOUT", 0xF1: "ABORT", 0xF2: "BAD_STATE",
}

SECTOR_SIZE = 0x1000
LOG_RING_ENTRIES = 128
LOG_ENTRY_SIZE = 12  # bytes


class OpenOCD:
    """Thin wrapper over openocd's TCL socket — reused conventions from v1."""
    def __init__(self, speed=1000, openocd_cfg=None):
        cfg = Path(openocd_cfg) if openocd_cfg else OPENOCD_CFG
        subprocess.run(["pkill", "-x", "openocd"], capture_output=True)
        time.sleep(1)
        self.proc = subprocess.Popen(
            ["openocd", "-f", str(cfg)],
            stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL,
        )
        time.sleep(3)
        self.sock = socket.create_connection(("localhost", 6666), timeout=30)
        self.sock.settimeout(30)
        self.cmd(f"adapter speed {speed}")

    def reset_halt(self, run_ms=1500):
        """Hardware reset (needs SRST in OpenOCD cfg). We do `reset run`
        and let the ROM bootloader execute for `run_ms` milliseconds
        before halting — long enough to initialize PLL + peripheral
        clocks per the TCAT header, short enough to catch the device
        before it hands off to primary firmware. Halting at the reset
        vector (`reset halt`) leaves PLL off and breaks SPI ops that
        need a working clock; observed 2026-04-21: BP_CLEAR TIMEOUT at
        first WRSR poll because SPI wasn't clocking.

        Our v2 driver's peripheral_full_teardown then handles whatever
        state the ROM/stage-2/primary left behind. ~300 ms is plenty —
        ROM PLL config is done in the first few ms, and at 300 ms the
        device is typically mid-stage-2 or just starting primary —
        before USB enumerates so host won't see a brief enumeration."""
        self.cmd("reset run", timeout=30)
        time.sleep(run_ms / 1000.0)
        return self.cmd("halt", timeout=30)

    def cmd(self, c, timeout=30):
        self.sock.settimeout(timeout)
        self.sock.sendall((c + "\x1a").encode())
        buf = b""
        while b"\x1a" not in buf:
            buf += self.sock.recv(4096)
        return buf.decode().strip("\x1a").strip()

    def halt(self):  self.cmd("halt")
    def resume(self):
        try: self.cmd("resume")
        except Exception: pass

    def mww(self, addr, val):
        self.cmd(f"mww {addr:#x} {val:#x}")

    def mdw(self, addr):
        # Retry on transient empty output (target mid-transition).
        for _ in range(3):
            r = self.cmd(f"mdw {addr:#x}")
            parts = r.split()
            if parts:
                try:
                    return int(parts[-1], 16)
                except ValueError:
                    pass
            self.cmd("halt")
            time.sleep(0.05)
        raise RuntimeError(f"mdw {addr:#x} kept returning empty — target stuck")

    def load_image(self, path, addr):
        return self.cmd(f"load_image {path} {addr:#x} bin", timeout=120)

    def dump_image(self, addr, size):
        tmp = f"/tmp/v2_dump_{addr:x}.bin"
        self.cmd(f"dump_image {tmp} {addr:#x} {size}", timeout=60)
        data = Path(tmp).read_bytes()
        try: os.unlink(tmp)
        except FileNotFoundError: pass
        return data

    def close(self):
        try: self.resume()
        except Exception: pass
        try: self.sock.close()
        except Exception: pass
        self.proc.terminate()
        try: self.proc.wait(timeout=5)
        except subprocess.TimeoutExpired: self.proc.kill()


def host_teardown(o):
    """Minimal host-side peripheral quiesce — matches v1 reboot_style but
    no target-side code needed since the v2 driver's own driver_setup()
    already teardown-quiesces. This just disables USB so we don't race."""
    o.mww(0x90000008, 0)          # USBCMD: stop
    o.mww(0x90000014, 0xFFFFFFFF) # USBSTS: clear all


def ensure_crystal_clock(o):
    """Drop the chip to base-crystal clock — matches the TCAT-BOOT shell
    state where flash is known-good 100%.

    Mirrors the ROM boot-fail sequence (see dice3_clock_pll.md and
    register_state_tcat-boot_*.md): one keyed write to PLL_OUTPUT
    (0xC900000C = 0xABCD0000) disables the PLL. The hardware MUX
    auto-falls-back to the crystal source — that's how the ROM survives
    disabling its own clock at 0x20000B24 while running on PLL.

    The driver's own peripheral_full_teardown() then forces CLK_DIV_SPI
    (0xC9000014) to crystal-fallback for the SPI bus parent. Together
    these two writes give us CPU + SPI on crystal, like TCAT-BOOT.

    No-op if PLL output is already disabled. CPU must be halted before
    the write; the post-write delay covers the clock-domain switch.
    """
    o.halt()
    pll_config_before = o.mdw(0xC9000000)
    pll_output_before = o.mdw(0xC900000C)
    if pll_output_before == 0:
        print(f"  PLL already disabled "
              f"(CLK_PLL_CONFIG=0x{pll_config_before:04x}) — on crystal")
        return
    print(f"  Dropping to crystal: "
          f"CLK_PLL_CONFIG=0x{pll_config_before:04x}, "
          f"CLK_PLL_OUTPUT=0x{pll_output_before:04x} → 0")
    o.mww(0xC900000C, 0xABCD0000)
    time.sleep(0.05)  # let the clock-domain switch settle
    pll_output_after = o.mdw(0xC900000C)
    if pll_output_after != 0:
        raise RuntimeError(
            f"Failed to disable PLL output: read 0x{pll_output_after:04x}, "
            f"expected 0. CPU may still be on PLL — flash from this state "
            f"is not the validated TCAT-BOOT baseline."
        )
    print(f"  PLL disabled — CPU on crystal "
          f"(CLK_PLL_OUTPUT=0x{pll_output_after:04x})")


def ensure_pll_clock(o):
    """Re-enable PLL output after a prior ensure_crystal_clock disable.

    Used by --cpu-clock=pll/toggle in shift-repro to flip CPU/AHB clock
    state and observe the +2B verify-shift failure rate as a function of
    DMA-controller clock domain. Crystal: ~12 MHz CPU/AHB; PLL: 196 MHz
    derived from the bootloader's PLL_CONFIG=0x8364 (M=100, N=3 → 400 MHz
    VCO with /2 post-divide).

    Writes 0xABCD8001 to PLL_OUTPUT — the inverse of ensure_crystal_clock.
    Bit 15 = enable, bits 3:0 = source select 1 (PLL output, vs 0 = bypass).
    """
    o.halt()
    pll_output_before = o.mdw(0xC900000C)
    if pll_output_before != 0:
        print(f"  PLL already enabled "
              f"(CLK_PLL_OUTPUT=0x{pll_output_before:04x}) — on PLL")
        return
    print(f"  Re-enabling PLL: CLK_PLL_OUTPUT=0x{pll_output_before:04x} → 0x8001")
    o.mww(0xC900000C, 0xABCD8001)
    time.sleep(0.05)
    pll_output_after = o.mdw(0xC900000C)
    if pll_output_after == 0:
        raise RuntimeError(
            f"Failed to enable PLL output: read 0x{pll_output_after:04x}, "
            f"expected nonzero."
        )
    print(f"  PLL enabled — CPU on PLL "
          f"(CLK_PLL_OUTPUT=0x{pll_output_after:04x})")


def maybe_drop_crystal(client, args):
    """Run ensure_crystal_clock unless --no-crystal was passed.

    Called right before any write op (bp_clear / erase / program) so reads
    and CRC sweeps stay at PLL (matching the bootloader's CRC routine),
    while writes drop to crystal (matching the user's XMODEM-validated
    flash driver config).
    """
    if getattr(args, "drop_to_crystal", True):
        ensure_crystal_clock(client.o)


class FlashClientV2:
    def __init__(self, ocd, log_path=None):
        self.o = ocd
        self.log_path = log_path or "/tmp/flash_v2.log"

    # ── Driver lifecycle ──────────────────────────────────────
    def load_driver(self, drop_to_crystal=False):
        """Load the SRAM driver and bring it to READY.

        drop_to_crystal: by default we no longer drop CPU to crystal here.
        The bootloader's CRC routine runs at PLL on every successful boot,
        so reads/hashes are validated at PLL and stay fast. The crystal
        drop is invoked separately by maybe_drop_crystal() right before
        any write op (bp_clear / erase / program), matching the user's
        XMODEM-validated flash configuration.
        """
        print("Loading v2 driver...")
        self.o.halt()
        host_teardown(self.o)
        if drop_to_crystal:
            ensure_crystal_clock(self.o)
        self.o.load_image(str(DRIVER_BIN), DRIVER_CODE_ADDR)
        # Zero the mailbox (HOST fields); driver only touches DEV fields.
        for off in (O_MAGIC, O_COMMAND, O_FLASH_ADDR, O_BUF_ADDR, O_LENGTH):
            self.o.mww(MBOX_ADDR + off, 0)
        # Zero v2_timings.magic so the driver re-runs init_timings_if_needed
        # and picks up the current build's defaults — otherwise stale values
        # from a prior driver-load session persist (the magic survives in
        # SRAM across driver reloads). 2026-05-01.
        self.o.mww(TIMINGS_ADDR, 0)
        # Invalidate caches so driver sees clean SRAM.
        self.o.cmd("arm mcr 15 0 7 5 0 0")
        self.o.cmd("arm mcr 15 0 7 6 0 0")
        self.o.cmd(f"reg pc {DRIVER_CODE_ADDR:#x}")
        self.o.cmd("reg cpsr 0xd3")
        self.o.resume()
        self._wait_ready(timeout=3.0)
        build_tag = self.o.mdw(MBOX_ADDR + O_BUILD_TAG)
        print(f"Driver READY (build_tag=0x{build_tag:08x})")

    def _wait_ready(self, timeout):
        t0 = time.monotonic()
        while time.monotonic() - t0 < timeout:
            time.sleep(0.1)
            try:
                self.o.halt()
                status = self.o.mdw(MBOX_ADDR + O_STATUS)
                self.o.resume()
            except RuntimeError:
                continue
            if status == STATUS_READY:
                return
        raise RuntimeError(f"driver did not reach READY within {timeout:.1f}s")

    # ── Command dispatch ──────────────────────────────────────
    def _issue(self, cmd, flash_addr=0, buf_addr=0, length=0, est_us=50_000,
               poll_interval=0.05, overall_timeout=None, quiet=False):
        """Single command → poll phase/status → on ERR dump log ring."""
        if overall_timeout is None:
            overall_timeout = max(est_us / 1e6 * 4.0, 5.0)

        self.o.halt()
        self.o.mww(MBOX_ADDR + O_FLASH_ADDR, flash_addr)
        self.o.mww(MBOX_ADDR + O_BUF_ADDR, buf_addr)
        self.o.mww(MBOX_ADDR + O_LENGTH, length)
        self.o.mww(MBOX_ADDR + O_COMMAND, cmd)
        self.o.mww(MBOX_ADDR + O_MAGIC, MAGIC_CMD)
        self.o.resume()

        last_seq = -1
        t_start = time.monotonic()
        while True:
            time.sleep(poll_interval)
            self.o.halt()
            status = self.o.mdw(MBOX_ADDR + O_STATUS)
            seq    = self.o.mdw(MBOX_ADDR + O_SEQ)
            phase  = self.o.mdw(MBOX_ADDR + O_PHASE)
            detail = self.o.mdw(MBOX_ADDR + O_PHASE_DETAIL)
            self.o.resume()

            if seq != last_seq and not quiet:
                print(f"  [{time.monotonic() - t_start:6.2f}s] "
                      f"status={status} phase={PHASE_NAMES.get(phase, phase)}"
                      f" detail={detail}")
                last_seq = seq

            if status == STATUS_OK:
                self.o.halt()
                elapsed = self.o.mdw(MBOX_ADDR + O_ELAPSED_US)
                last_sr = self.o.mdw(MBOX_ADDR + O_LAST_SR)
                self.o.resume()
                if not quiet:
                    print(f"  OK ({elapsed} µs, last_sr=0x{last_sr:02x})")
                return True
            if status == STATUS_ERR:
                self._dump_error()
                return False
            if time.monotonic() - t_start > overall_timeout:
                print(f"  TIMEOUT after {overall_timeout:.1f}s "
                      f"(phase={PHASE_NAMES.get(phase, phase)})")
                self._dump_error(extra_note="host-side timeout")
                return False

    def _dump_error(self, extra_note=""):
        self.o.halt()
        err_code  = self.o.mdw(MBOX_ADDR + O_ERR_CODE)
        err_addr  = self.o.mdw(MBOX_ADDR + O_ERR_ADDR)
        last_sr   = self.o.mdw(MBOX_ADDR + O_LAST_SR)
        phase     = self.o.mdw(MBOX_ADDR + O_PHASE)
        log_head  = self.o.mdw(MBOX_ADDR + O_LOG_HEAD)
        elapsed   = self.o.mdw(MBOX_ADDR + O_ELAPSED_US)
        # Verify-miss capture (zeroed unless err_code == V2_ERR_VERIFY_MISMATCH).
        miss_off     = self.o.mdw(MBOX_ADDR + O_MISS_OFFSET)
        miss_got     = self.o.mdw(MBOX_ADDR + O_MISS_GOT)
        miss_exp     = self.o.mdw(MBOX_ADDR + O_MISS_EXP)
        miss_spi_st  = self.o.mdw(MBOX_ADDR + O_MISS_SPI_ST)
        miss_dma_ist = self.o.mdw(MBOX_ADDR + O_MISS_DMA_IST)
        miss_reads   = self.o.mdw(MBOX_ADDR + O_MISS_READS)
        # Extended diagnostic snapshot (also captured at VERIFY_MISS).
        miss_spi_err = self.o.mdw(MBOX_ADDR + O_MISS_SPI_ERR)
        miss_spi_pnd = self.o.mdw(MBOX_ADDR + O_MISS_SPI_PND)
        miss_dma_st  = self.o.mdw(MBOX_ADDR + O_MISS_DMA_ST)
        miss_ch0_ctl = self.o.mdw(MBOX_ADDR + O_MISS_CH0_CTL)
        miss_ch0_cfg = self.o.mdw(MBOX_ADDR + O_MISS_CH0_CFG)
        miss_ch1_ctl = self.o.mdw(MBOX_ADDR + O_MISS_CH1_CTL)
        miss_ch1_cfg = self.o.mdw(MBOX_ADDR + O_MISS_CH1_CFG)
        miss_dma_en  = self.o.mdw(MBOX_ADDR + O_MISS_DMA_EN)
        miss_pre_st  = self.o.mdw(MBOX_ADDR + O_MISS_PRE_ST)
        miss_pre_err = self.o.mdw(MBOX_ADDR + O_MISS_PRE_ERR)
        miss_rx_h0   = self.o.mdw(MBOX_ADDR + O_MISS_RX_H0)
        miss_rx_h1   = self.o.mdw(MBOX_ADDR + O_MISS_RX_H1)
        miss_chX_cfg = [self.o.mdw(MBOX_ADDR + off) for off in
                        (O_MISS_CH2_CFG, O_MISS_CH3_CFG, O_MISS_CH4_CFG,
                         O_MISS_CH5_CFG, O_MISS_CH6_CFG, O_MISS_CH7_CFG)]
        miss_rdsr_af = self.o.mdw(MBOX_ADDR + O_MISS_RDSR_AF)
        miss_rdsr_bf = self.o.mdw(MBOX_ADDR + O_MISS_RDSR_BF)
        first_rdsr   = self.o.mdw(MBOX_ADDR + O_FIRST_RDSR)
        last_rdsr_af = self.o.mdw(MBOX_ADDR + O_LAST_RDSR_AF)
        miss_dma_cmb = self.o.mdw(MBOX_ADDR + O_MISS_DMA_CMB)
        miss_dma_err = self.o.mdw(MBOX_ADDR + O_MISS_DMA_ERR)
        miss_dma_raw = self.o.mdw(MBOX_ADDR + O_MISS_DMA_RAW)
        miss_dma_enb = self.o.mdw(MBOX_ADDR + O_MISS_DMA_ENB)
        miss_spi_40  = self.o.mdw(MBOX_ADDR + O_MISS_SPI_40)
        last_chX_cfg = [self.o.mdw(MBOX_ADDR + off) for off in
                        (O_LAST_CH2_CFG, O_LAST_CH3_CFG, O_LAST_CH4_CFG,
                         O_LAST_CH5_CFG, O_LAST_CH6_CFG, O_LAST_CH7_CFG)]
        # Last-chunk snapshot — on VERIFY_OK this shows the state of the
        # last successful chunk, which we can diff against the miss case.
        last_spi_err = self.o.mdw(MBOX_ADDR + O_LAST_SPI_ERR)
        last_spi_pnd = self.o.mdw(MBOX_ADDR + O_LAST_SPI_PND)
        last_dma_st  = self.o.mdw(MBOX_ADDR + O_LAST_DMA_ST)
        last_ch0_ctl = self.o.mdw(MBOX_ADDR + O_LAST_CH0_CTL)
        last_ch0_cfg = self.o.mdw(MBOX_ADDR + O_LAST_CH0_CFG)
        last_ch1_ctl = self.o.mdw(MBOX_ADDR + O_LAST_CH1_CTL)
        last_ch1_cfg = self.o.mdw(MBOX_ADDR + O_LAST_CH1_CFG)
        last_dma_en  = self.o.mdw(MBOX_ADDR + O_LAST_DMA_EN)
        last_dma_cmb = self.o.mdw(MBOX_ADDR + O_LAST_DMA_CMB)
        last_dma_err = self.o.mdw(MBOX_ADDR + O_LAST_DMA_ERR)
        last_dma_raw = self.o.mdw(MBOX_ADDR + O_LAST_DMA_RAW)
        last_dma_enb = self.o.mdw(MBOX_ADDR + O_LAST_DMA_ENB)
        last_spi_40  = self.o.mdw(MBOX_ADDR + O_LAST_SPI_40)
        last_pre_st  = self.o.mdw(MBOX_ADDR + O_LAST_PRE_ST)
        last_pre_err = self.o.mdw(MBOX_ADDR + O_LAST_PRE_ERR)
        last_rx_h0   = self.o.mdw(MBOX_ADDR + O_LAST_RX_H0)
        last_rx_h1   = self.o.mdw(MBOX_ADDR + O_LAST_RX_H1)
        ring      = self.o.dump_image(LOG_RING_ADDR,
                                      LOG_ENTRY_SIZE * LOG_RING_ENTRIES)
        self.o.resume()

        ts = dt.datetime.now().strftime("%Y%m%dT%H%M%S")
        path = self.log_path.replace(".log", f"_{ts}.log")
        with open(path, "w") as f:
            f.write(f"# v2 flash ERROR dump {ts}\n")
            if extra_note:
                f.write(f"# note: {extra_note}\n")
            f.write(f"err_code = 0x{err_code:04x}\n")
            f.write(f"err_spi_addr = 0x{err_addr:08x}\n")
            f.write(f"last_sr = 0x{last_sr:02x}\n")
            f.write(f"phase = {phase} ({PHASE_NAMES.get(phase, '?')})\n")
            f.write(f"elapsed_us = {elapsed}\n")
            f.write(f"log_head = {log_head}\n")
            if err_code == 0x0401:  # V2_ERR_VERIFY_MISMATCH
                got4 = miss_got.to_bytes(4, "little")
                exp4 = miss_exp.to_bytes(4, "little")
                shift_hint = ""
                # Attempt to classify the shift: does got[0] match exp[1],
                # exp[2], exp[3]? If so it's a +1/+2/+3 shift.
                for s in (1, 2, 3):
                    if got4[0] == exp4[s]:
                        shift_hint = f"  (got[0]==exp[{s}]  → +{s}-byte shift)"
                        break
                f.write("# verify miss capture:\n")
                f.write(f"miss_offset      = {miss_off} (0x{miss_off:x})\n")
                f.write(f"miss_got         = {' '.join(f'{b:02x}' for b in got4)}\n")
                f.write(f"miss_expected    = {' '.join(f'{b:02x}' for b in exp4)}"
                        f"{shift_hint}\n")
                f.write(f"miss_spi_stat    = 0x{miss_spi_st:08x}\n")
                f.write(f"miss_dma_istat   = 0x{miss_dma_ist:08x}\n")
                f.write(f"miss_reads_done  = {miss_reads}\n")
                # Extended diagnostic snapshot.
                def _decode_cfg(v):
                    return (f"E={v&1} FlowCntrl={(v>>11)&7} "
                            f"DestPeri={(v>>6)&0xF} SrcPeri={(v>>1)&0xF} "
                            f"L={(v>>16)&1} A={(v>>17)&1} H={(v>>18)&1}")
                def _decode_ctl(v):
                    return (f"count={v&0xFFF} SBSize={(v>>12)&7} "
                            f"DBSize={(v>>15)&7} SWidth={(v>>18)&7} "
                            f"DWidth={(v>>21)&7} SI={(v>>26)&1} "
                            f"DI={(v>>27)&1} Prot={(v>>28)&7} I={(v>>31)&1}")
                f.write("# ── DMA/SPI snapshot (miss vs last-OK) ──\n")
                def _decode_sstat(v):
                    bits = []
                    if v & 0x01: bits.append("BUSY")
                    if v & 0x02: bits.append("TX_RDY")
                    if v & 0x08: bits.append("RX_RDY")
                    return ",".join(bits) if bits else "-"
                def _hexbytes(w):
                    return " ".join(f"{(w>>(8*i))&0xFF:02x}" for i in range(4))
                miss_head = f"{_hexbytes(miss_rx_h0)} | {_hexbytes(miss_rx_h1)}"
                last_head = f"{_hexbytes(last_rx_h0)} | {_hexbytes(last_rx_h1)}"
                f.write(f"miss_rx_head     = {miss_head}\n")
                f.write(f"last_rx_head     = {last_head}\n")
                def _decode_sr(v):
                    b = v & 0xFF
                    bits = []
                    if b & 0x01: bits.append("BUSY")
                    if b & 0x02: bits.append("WEL")
                    bp = (b >> 2) & 0xF
                    if bp:       bits.append(f"BP={bp:x}")
                    if b & 0x40: bits.append("AAI")
                    if b & 0x80: bits.append("BPL")
                    return ",".join(bits) if bits else "clean"
                f.write(f"first_rdsr       = 0x{first_rdsr&0xFF:02x}  "
                        f"({_decode_sr(first_rdsr)})\n")
                f.write(f"miss_rdsr_before = 0x{miss_rdsr_bf&0xFF:02x} "
                        f"(ch{(miss_rdsr_bf>>8)&0xFFFFFF}  "
                        f"{_decode_sr(miss_rdsr_bf)})\n")
                f.write(f"miss_rdsr_after  = 0x{miss_rdsr_af&0xFF:02x} "
                        f"(ch{(miss_rdsr_af>>8)&0xFFFFFF}  "
                        f"{_decode_sr(miss_rdsr_af)})\n")
                f.write(f"last_rdsr_after  = 0x{last_rdsr_af&0xFF:02x} "
                        f"(ch{(last_rdsr_af>>8)&0xFFFFFF}  "
                        f"{_decode_sr(last_rdsr_af)})\n")
                print(f"    first_rdsr=0x{first_rdsr&0xFF:02x}  "
                      f"before_miss(ch{(miss_rdsr_bf>>8)&0xFFFFFF})="
                      f"0x{miss_rdsr_bf&0xFF:02x}  "
                      f"after_miss=0x{miss_rdsr_af&0xFF:02x}")
                # CH2..CH7 Configuration sampled at pre-arm time (AHB
                # contention detector). Active means another master is
                # moving data while we try to arm.
                f.write("# ── CH2..CH7 Configuration @ pre-arm (AHB contention) ──\n")
                for i, (m, l) in enumerate(zip(miss_chX_cfg, last_chX_cfg), 2):
                    m_active = "ACTIVE!" if (m & 0x20001) else "idle"
                    l_active = "ACTIVE!" if (l & 0x20001) else "idle"
                    f.write(f"  CH{i} miss=0x{m:08x} ({m_active})   "
                            f"last=0x{l:08x} ({l_active})\n")
                # Dump any active channel prominently on stdout.
                active = [(i+2, v) for i, v in enumerate(miss_chX_cfg)
                          if v & 0x20001]
                if active:
                    print("    ⚠ contending channels @ miss arm-time: " +
                          ", ".join(f"CH{ch}=0x{v:x}" for ch, v in active))
                # Expected: first 4 bytes dummy (cmd+addr echo), next 4 = flash data.
                # RX_TEMP[4] should == exp[0]. Any deviation localizes the shift.
                f.write(f"miss_pre_stat    = 0x{miss_pre_st:08x}   "
                        f"({_decode_sstat(miss_pre_st)})   "
                        f"last=0x{last_pre_st:08x}\n")
                f.write(f"miss_pre_err     = 0x{miss_pre_err:08x}   "
                        f"last=0x{last_pre_err:08x}\n")
                f.write(f"miss_spi_err     = 0x{miss_spi_err:08x}   "
                        f"last=0x{last_spi_err:08x}\n")
                f.write(f"miss_spi_pending = 0x{miss_spi_pnd:08x}   "
                        f"last=0x{last_spi_pnd:08x}\n")
                f.write(f"miss_dma_stat    = 0x{miss_dma_st:08x}   "
                        f"last=0x{last_dma_st:08x}\n")
                f.write(f"miss_dma_en      = 0x{miss_dma_en:08x}   "
                        f"last=0x{last_dma_en:08x}\n")
                f.write(f"miss_dma_comb    = 0x{miss_dma_cmb:08x}   "
                        f"last=0x{last_dma_cmb:08x}\n")
                f.write(f"miss_dma_errst   = 0x{miss_dma_err:08x}   "
                        f"last=0x{last_dma_err:08x}\n")
                f.write(f"miss_dma_rawerr  = 0x{miss_dma_raw:08x}   "
                        f"last=0x{last_dma_raw:08x}\n")
                f.write(f"miss_dma_enbldch = 0x{miss_dma_enb:08x}   "
                        f"last=0x{last_dma_enb:08x}\n")
                f.write(f"miss_spi_40      = 0x{miss_spi_40:08x}   "
                        f"last=0x{last_spi_40:08x}\n")
                f.write(f"miss_ch0_ctrl    = 0x{miss_ch0_ctl:08x}   "
                        f"({_decode_ctl(miss_ch0_ctl)})\n")
                f.write(f"     last_ch0    = 0x{last_ch0_ctl:08x}   "
                        f"({_decode_ctl(last_ch0_ctl)})\n")
                f.write(f"miss_ch0_cfg     = 0x{miss_ch0_cfg:08x}   "
                        f"({_decode_cfg(miss_ch0_cfg)})\n")
                f.write(f"     last_ch0    = 0x{last_ch0_cfg:08x}   "
                        f"({_decode_cfg(last_ch0_cfg)})\n")
                f.write(f"miss_ch1_ctrl    = 0x{miss_ch1_ctl:08x}   "
                        f"({_decode_ctl(miss_ch1_ctl)})\n")
                f.write(f"     last_ch1    = 0x{last_ch1_ctl:08x}   "
                        f"({_decode_ctl(last_ch1_ctl)})\n")
                f.write(f"miss_ch1_cfg     = 0x{miss_ch1_cfg:08x}   "
                        f"({_decode_cfg(miss_ch1_cfg)})\n")
                f.write(f"     last_ch1    = 0x{last_ch1_cfg:08x}   "
                        f"({_decode_cfg(last_ch1_cfg)})\n")
                print(f"  miss@+{miss_off}  got={got4.hex()} exp={exp4.hex()}"
                      f"  spi_stat=0x{miss_spi_st:x} dma_istat=0x{miss_dma_ist:x}"
                      f"  reads={miss_reads}{shift_hint}")
                # Compact single-line diff on stdout.
                diffs = []
                for name, m, l in [
                    ("pre_stat", miss_pre_st,  last_pre_st),
                    ("pre_err",  miss_pre_err, last_pre_err),
                    ("spi_err",  miss_spi_err, last_spi_err),
                    ("spi_pnd",  miss_spi_pnd, last_spi_pnd),
                    ("dma_st",   miss_dma_st,  last_dma_st),
                    ("ch0_ctl",  miss_ch0_ctl, last_ch0_ctl),
                    ("ch0_cfg",  miss_ch0_cfg, last_ch0_cfg),
                    ("ch1_ctl",  miss_ch1_ctl, last_ch1_ctl),
                    ("ch1_cfg",  miss_ch1_cfg, last_ch1_cfg),
                ]:
                    if m != l:
                        diffs.append(f"{name}: 0x{m:x}≠0x{l:x}")
                if diffs:
                    print("    miss-vs-lastOK diffs:  " + "  ".join(diffs))
                print(f"    ch0 cfg A={(miss_ch0_cfg>>17)&1} "
                      f"H={(miss_ch0_cfg>>18)&1} E={miss_ch0_cfg&1}  "
                      f"ch1 cfg A={(miss_ch1_cfg>>17)&1} "
                      f"H={(miss_ch1_cfg>>18)&1} E={miss_ch1_cfg&1}  "
                      f"spi_err=0x{miss_spi_err:x}  "
                      f"dma_err=0x{miss_dma_err:x} raw=0x{miss_dma_raw:x} "
                      f"enb=0x{miss_dma_enb:x}")
            f.write("#\n")
            f.write(f"{'t_us':>10} {'evt':<16} {'phase':<12} "
                    f"{'detail':>6} {'spi_addr':>10}\n")
            # Walk ring from (log_head - N) mod N to log_head - 1.
            count = min(log_head, LOG_RING_ENTRIES)
            start = (log_head - count) & (LOG_RING_ENTRIES - 1)
            for i in range(count):
                idx = (start + i) & (LOG_RING_ENTRIES - 1)
                off = idx * LOG_ENTRY_SIZE
                t_us, evt, ph, detail, spi = struct.unpack_from(
                    "<IBBHI", ring, off)
                ename = EVT_NAMES.get(evt, f"0x{evt:02x}")
                pname = PHASE_NAMES.get(ph, f"0x{ph:02x}")
                detail_str = f"{detail:>6}"
                if evt == 0x42:  # READ_CHUNK_SR — decode the detail field.
                    detail_str = f"ch{(detail>>8)&0xFF:>3} sr=0x{detail&0xFF:02x}"
                elif evt == 0x43:  # READ_CHUNK_WEL — sanity after WREN.
                    sr = detail & 0xFF
                    detail_str = (f"ch{(detail>>8)&0xFF:>3} sr=0x{sr:02x} "
                                  f"WEL={(sr>>1)&1}")
                elif evt == 0x44:  # READ_LONG_RDSR — first two response bytes.
                    detail_str = (f"b0=0x{((detail>>8)&0xFF):02x} "
                                  f"b1=0x{detail&0xFF:02x}")
                f.write(f"{t_us:>10} {ename:<16} {pname:<12} "
                        f"{detail_str:<15} 0x{spi:08x}\n")
        print(f"  error log → {path}")

    # ── High-level ops ────────────────────────────────────────
    def bp_clear(self):
        print("bp_clear...")
        return self._issue(CMD_BP_CLEAR, est_us=30_000, overall_timeout=5.0)

    def read_timings(self):
        """Read the active v2_timings struct. Returns a dict (empty if
        driver hasn't stamped the magic yet)."""
        self.o.halt()
        magic = self.o.mdw(TIMINGS_ADDR)
        out = {"magic": magic}
        if magic != TIMINGS_MAGIC:
            return out
        for i, name in enumerate(TIMING_FIELDS):
            out[name] = self.o.mdw(TIMINGS_ADDR + 4 + i * 4)
        return out

    def set_timings(self, **overrides):
        """Write defaults + overrides into the v2_timings struct, then
        stamp the magic last so the driver picks up a consistent set.
        Unrecognized keys raise; values clamped to u32. Defaults fill
        any fields not overridden."""
        for k in overrides:
            if k not in TIMING_DEFAULTS:
                raise ValueError(f"unknown timing {k!r}; known: {list(TIMING_DEFAULTS)}")
        values = {**TIMING_DEFAULTS, **overrides}
        self.o.halt()
        # Clear magic first so driver won't pick up a torn struct.
        self.o.mww(TIMINGS_ADDR, 0)
        for i, name in enumerate(TIMING_FIELDS):
            v = int(values[name]) & 0xFFFFFFFF
            self.o.mww(TIMINGS_ADDR + 4 + i * 4, v)
        # Stamp magic last.
        self.o.mww(TIMINGS_ADDR, TIMINGS_MAGIC)
        print(f"timings set: {values}")

    def erase(self, spi_addr):
        print(f"erase 0x{spi_addr:06x}...")
        return self._issue(CMD_ERASE, flash_addr=spi_addr,
                           est_us=30_000, overall_timeout=5.0)

    def erase_64k(self, spi_addr):
        print(f"erase_64k 0x{spi_addr:06x}...")
        return self._issue(CMD_ERASE_64K, flash_addr=spi_addr,
                           est_us=30_000, overall_timeout=5.0)

    def erase_32k(self, spi_addr):
        print(f"erase_32k 0x{spi_addr:06x}...")
        return self._issue(CMD_ERASE_32K, flash_addr=spi_addr,
                           est_us=30_000, overall_timeout=5.0)

    def program(self, spi_addr, data_bytes):
        """Upload data_bytes to DATA_BUF_ADDR, then issue PROGRAM."""
        tmp = f"/tmp/v2_prog_{spi_addr:x}.bin"
        Path(tmp).write_bytes(data_bytes)
        self.o.halt()
        self.o.load_image(tmp, DATA_BUF_ADDR)
        self.o.resume()
        try: os.unlink(tmp)
        except FileNotFoundError: pass
        print(f"program 0x{spi_addr:06x} (len={len(data_bytes)})...")
        # With RDSR polling per AAI pair, per-pair ~30 ms, so a 4 KB
        # sector takes ~60 s end-to-end. Scale per length with headroom.
        pairs = max(1, len(data_bytes) // 2)
        est_us = pairs * 35_000
        return self._issue(CMD_PROGRAM, flash_addr=spi_addr,
                           buf_addr=DATA_BUF_ADDR, length=len(data_bytes),
                           est_us=est_us,
                           overall_timeout=max(est_us / 1e6 * 1.5, 90.0))

    def read(self, spi_addr, length):
        """Read from flash into DATA_BUF_ADDR, then return bytes."""
        print(f"read 0x{spi_addr:06x} len={length}...")
        ok = self._issue(CMD_READ, flash_addr=spi_addr,
                         buf_addr=DATA_BUF_ADDR, length=length,
                         est_us=30_000, overall_timeout=5.0)
        if not ok: return None
        self.o.halt()
        data = self.o.dump_image(DATA_BUF_ADDR, length)
        self.o.resume()
        return data

    def byte_patch(self, spi_addr, data_bytes):
        """BYTE_PROGRAM each byte. Requires target cells to be erased
        (0xFF) or to contain a superset of the target bits. Used to fix
        AAI-dropped tail bytes."""
        if not data_bytes:
            return True
        tmp = f"/tmp/v2_bp_{spi_addr:x}.bin"
        Path(tmp).write_bytes(data_bytes)
        self.o.halt()
        self.o.load_image(tmp, DATA_BUF_ADDR)
        self.o.resume()
        try: os.unlink(tmp)
        except FileNotFoundError: pass
        print(f"byte_patch 0x{spi_addr:06x} len={len(data_bytes)}...")
        est_us = len(data_bytes) * 5000   # ~5 ms per byte (WREN+TX+poll)
        return self._issue(CMD_BYTE_PATCH, flash_addr=spi_addr,
                           buf_addr=DATA_BUF_ADDR, length=len(data_bytes),
                           est_us=est_us,
                           overall_timeout=max(est_us / 1e6 * 2, 5.0))

    def device_crc32(self, spi_addr, length, *, quiet=True,
                     sector_buf_addr=0):
        """CRC32 of [spi_addr, spi_addr+length) computed on-device by the
        same ROM step routine the bootloader uses (poly 0xEDB88320, init=0,
        no final XOR). Host equivalent: zlib.crc32(buf, ~0) ^ ~0.

        If sector_buf_addr != 0, the driver also writes per-sector CRCs
        (one u32 per V2_SECTOR_SIZE) to that SRAM address. length must
        be a multiple of SECTOR_SIZE for batch mode.

        Returns just the whole-range CRC. Caller must dump_image
        sector_buf_addr separately if it wants the per-sector array.
        """
        if not quiet:
            mode = "batch" if sector_buf_addr else "single"
            print(f"hash 0x{spi_addr:06x} len={length} ({mode})...")
        # Empirical: ~14 µs/byte at PLL (CLK_DIV_SPI=0, ~6 MHz SPI bus + per-
        # chunk DMA setup), ~20 µs/byte at crystal. Batch mode adds a
        # second CRC step per byte (~25% bump at PLL, ~1% at crystal since
        # SPI dominates). Pad to 30 µs/byte plus a 1 s floor.
        est_us = max(length * 30 + 1_000_000, 5_000)
        ok = self._issue(CMD_HASH_RANGE, flash_addr=spi_addr,
                         buf_addr=sector_buf_addr,
                         length=length, est_us=est_us,
                         overall_timeout=max(est_us / 1e6 * 2, 5.0),
                         quiet=quiet)
        if not ok:
            raise RuntimeError(f"device_crc32(0x{spi_addr:06x}, {length}) failed")
        self.o.halt()
        v = self.o.mdw(MBOX_ADDR + O_HASH_RESULT)
        self.o.resume()
        return v & 0xFFFFFFFF

    def device_crc32_batch(self, spi_addr, length, sector_buf_addr):
        """One sweep that returns the whole-range CRC AND a list of
        per-sector CRCs (one per V2_SECTOR_SIZE). Avoids the second pass
        on the mismatch path."""
        n_sectors = length // SECTOR_SIZE
        if length % SECTOR_SIZE:
            raise ValueError(
                f"batch hash needs sector-aligned length, got {length}")
        whole = self.device_crc32(spi_addr, length, quiet=True,
                                  sector_buf_addr=sector_buf_addr)
        self.o.halt()
        raw = self.o.dump_image(sector_buf_addr, n_sectors * 4)
        self.o.resume()
        sec_crcs = list(struct.unpack(f"<{n_sectors}I", raw))
        return whole, sec_crcs

    def read_id(self, mode="jedec"):
        """Exercise dma_rx + wait_dma_irq path on short bidir reads.
        mode='jedec' → cmd 0x9F, 3B response. mode='legacy' → cmd 0xAB, 1B.
        Returns bytes or None on failure."""
        flag = 0 if mode == "jedec" else 1
        print(f"read_id ({mode})...")
        ok = self._issue(CMD_READ_ID, flash_addr=flag,
                         buf_addr=DATA_BUF_ADDR, length=0,
                         est_us=5000, overall_timeout=5.0)
        if not ok: return None
        resp_len = 3 if mode == "jedec" else 1
        self.o.halt()
        data = self.o.dump_image(DATA_BUF_ADDR, resp_len)
        self.o.resume()
        return data

    def flash_block(self, spi_addr, data_bytes, load_addr=IMAGE_BASE_ADDR):
        """Autonomous erase+AAI-program+verify for up to 64 KB in one cmd.
        Driver auto-picks SE/BE32/BE64 from addr alignment + length.
        Data is uploaded to `load_addr` in SRAM (default IMAGE_BASE so we
        can fit up to 64 KB safely)."""
        tmp = f"/tmp/v2_fb_{spi_addr:x}.bin"
        Path(tmp).write_bytes(data_bytes)
        self.o.halt()
        self.o.load_image(tmp, load_addr)
        self.o.resume()
        try: os.unlink(tmp)
        except FileNotFoundError: pass
        print(f"flash_block 0x{spi_addr:06x} (len={len(data_bytes)})...")
        pairs = max(1, len(data_bytes) // 2)
        tim = self.read_timings()
        per_pair = max(260, int(tim.get("aai_pair_fixed_us", 20)) + 20_000)
        est_us = pairs * per_pair + 60_000 + (len(data_bytes) // 1024) * 20_000
        return self._issue(CMD_FLASH_BLOCK, flash_addr=spi_addr,
                           buf_addr=load_addr, length=len(data_bytes),
                           est_us=est_us,
                           overall_timeout=max(est_us / 1e6 * 2, 10.0))

    def flash_sector(self, spi_addr, data_bytes):
        """Autonomous erase+program+verify for one sector in a single cmd.
        Saves 2 of the 3 JTAG handshakes per sector vs erase/program/verify."""
        tmp = f"/tmp/v2_fs_{spi_addr:x}.bin"
        Path(tmp).write_bytes(data_bytes)
        self.o.halt()
        self.o.load_image(tmp, DATA_BUF_ADDR)
        self.o.resume()
        try: os.unlink(tmp)
        except FileNotFoundError: pass
        print(f"flash_sector 0x{spi_addr:06x} (len={len(data_bytes)})...")
        pairs = max(1, len(data_bytes) // 2)
        tim = self.read_timings()
        # Observed: actual per-pair time ≈ aai_pair_fixed_us + ~20 µs DMA
        # overhead at 1 MHz JTAG.
        per_pair = max(260, int(tim.get("aai_pair_fixed_us", 20)) + 20_000)
        est_us = pairs * per_pair + 55_000 + 80_000   # program + erase + verify
        return self._issue(CMD_FLASH_SECTOR, flash_addr=spi_addr,
                           buf_addr=DATA_BUF_ADDR, length=len(data_bytes),
                           est_us=est_us,
                           overall_timeout=max(est_us / 1e6 * 2, 5.0))

    def verify(self, spi_addr, expected_bytes):
        tmp = f"/tmp/v2_expect_{spi_addr:x}.bin"
        Path(tmp).write_bytes(expected_bytes)
        self.o.halt()
        self.o.load_image(tmp, DATA_BUF_ADDR)
        self.o.resume()
        try: os.unlink(tmp)
        except FileNotFoundError: pass
        print(f"verify 0x{spi_addr:06x} (len={len(expected_bytes)})...")
        return self._issue(CMD_VERIFY, flash_addr=spi_addr,
                           buf_addr=DATA_BUF_ADDR, length=len(expected_bytes),
                           est_us=30_000, overall_timeout=5.0)


BLOCK_32K = 0x8000
BLOCK_64K = 0x10000


def plan_erase_ops(sectors, policy):
    """Pick a sequence of (erase_kind, addr) ops covering all `sectors`.

    Strategies:
      'se'     — one 4 KB SE per sector (no over-erase)
      'be64'   — one 64 KB BE per block containing any sector (may
                 erase adjacent untouched regions)
      'smart'  — use BE64 only when ≥8/16 sectors in that 64 KB block
                 need programming, else BE32 when ≥4/8 in a 32 KB
                 block, else per-sector SE.
    Returns a list like [('be64', 0x40000), ('se', 0x3F000), ...].
    """
    sectors = sorted(set(sectors))
    if policy == "se":
        return [("se", s) for s in sectors]

    # Group into 64 KB blocks.
    blocks64 = {}
    for s in sectors:
        blocks64.setdefault(s & ~(BLOCK_64K - 1), []).append(s)

    ops = []
    if policy == "be64":
        for blk_addr in sorted(blocks64):
            ops.append(("be64", blk_addr))
        return ops

    # 'smart'
    for blk_addr, sec_list in sorted(blocks64.items()):
        n = len(sec_list)
        if n >= 8:                     # majority of block — erase 64 KB
            ops.append(("be64", blk_addr))
            continue
        # Try splitting into two 32 KB halves.
        low  = [s for s in sec_list if s < blk_addr + BLOCK_32K]
        high = [s for s in sec_list if s >= blk_addr + BLOCK_32K]
        for halves in ((low, blk_addr), (high, blk_addr + BLOCK_32K)):
            hsecs, haddr = halves
            if not hsecs: continue
            if len(hsecs) >= 4:
                ops.append(("be32", haddr))
            else:
                for s in hsecs: ops.append(("se", s))
    return ops


def plan_flash_ops(ref, image_size, block_threshold_64k=12,
                   block_threshold_32k=6, target_sectors=None):
    """Pick flash ops covering every 4 KB sector in [0, image_size).

    For each 64 KB block:
      - If every byte in reference is 0xFF → single ERASE_64K (fastest)
      - Else if ≥ block_threshold_64k non-FF sectors → FLASH_BLOCK(64K)
      - Else split into two 32 KB halves; each half similarly classified
        (all-FF → ERASE_32K; dense → FLASH_BLOCK(32K); sparse → per-sector)

    Per-sector ops likewise split into ("erase", ...) for all-FF sectors
    and ("sector", ...) for content. Erase ops leave flash at 0xFF, which
    is the correct state for any reference byte that's 0xFF.

    When `target_sectors` is provided, only those 4 KB sectors are
    considered for planning. Block/half-block ops are only emitted when
    ALL sectors in the corresponding region are targeted (avoids touching
    sectors outside the diff set).

    Returns list of (kind, addr, span) tuples. kind ∈ {block_64k,
    block_32k, sector, erase_64k, erase_32k, erase}.
    """
    def is_ff(lo, hi):
        return all(b == 0xFF for b in ref[lo:hi])

    if target_sectors is not None:
        target_set = {
            int(s) & ~(SECTOR_SIZE - 1)
            for s in target_sectors
            if 0 <= int(s) < image_size
        }
    else:
        target_set = None

    ops = []
    for blk_addr in range(0, image_size, BLOCK_64K):
        blk_hi = blk_addr + BLOCK_64K
        if target_set is not None:
            blk_targets = sorted(
                s for s in target_set if blk_addr <= s < blk_hi
            )
            if not blk_targets:
                continue
        else:
            blk_targets = [blk_addr + i * SECTOR_SIZE
                           for i in range(BLOCK_64K // SECTOR_SIZE)]

        full_block_targeted = len(blk_targets) == (BLOCK_64K // SECTOR_SIZE)
        if is_ff(blk_addr, blk_hi):
            if full_block_targeted:
                ops.append(("erase_64k", blk_addr, BLOCK_64K))
            else:
                for sec_addr in blk_targets:
                    ops.append(("erase", sec_addr, SECTOR_SIZE))
            continue
        # Count non-empty sectors in this block among targeted sectors.
        sec_list = [sec for sec in blk_targets
                    if not is_ff(sec, sec + SECTOR_SIZE)]
        if full_block_targeted and len(sec_list) >= block_threshold_64k:
            ops.append(("block_64k", blk_addr, BLOCK_64K))
            continue
        # Split into two 32 KB halves
        for half_addr in (blk_addr, blk_addr + BLOCK_32K):
            half_hi = half_addr + BLOCK_32K
            half_targets = [s for s in blk_targets if half_addr <= s < half_hi]
            if not half_targets:
                continue
            full_half_targeted = len(half_targets) == (BLOCK_32K // SECTOR_SIZE)
            if is_ff(half_addr, half_hi):
                if full_half_targeted:
                    ops.append(("erase_32k", half_addr, BLOCK_32K))
                else:
                    for sec_addr in half_targets:
                        ops.append(("erase", sec_addr, SECTOR_SIZE))
                continue
            half_secs = [s for s in sec_list if half_addr <= s < half_hi]
            if full_half_targeted and len(half_secs) >= block_threshold_32k:
                ops.append(("block_32k", half_addr, BLOCK_32K))
            else:
                for sec_addr in half_targets:
                    if is_ff(sec_addr, sec_addr + SECTOR_SIZE):
                        ops.append(("erase", sec_addr, SECTOR_SIZE))
                    else:
                        ops.append(("sector", sec_addr, SECTOR_SIZE))
    return ops


from sector_diff import (
    zlib_rom_crc32 as _zlib_rom_crc32,
    batch_diff,
)


def flash_all(client, args):
    """Flash every non-empty sector of --ref.

    Prefers one autonomous block op per 64 KB region when the block is
    mostly full (≥12 sectors non-empty), else falls back to per-sector
    ops. Saves round-trips vs the old erase+program+verify loop."""
    ref = Path(args.ref).read_bytes()
    print(f"Reference: {args.ref} ({len(ref)} bytes)")

    diff_sectors = None
    if args.diff and not args.sectors:
        # One sweep returns whole-image + per-sector CRCs. Plan from the
        # diff set directly so we don't build then trim a full-image plan.
        match, diff_sectors = batch_diff(client, ref)
        if match:
            print("\n=== flash-all done "
                  "(image already matches device, whole-image CRC) ===")
            return 0
        if not diff_sectors:
            # Should be rare (whole-image mismatch but no sector-level hit).
            print("  ⚠ whole-image mismatch but per-sector found no diff "
                  "— defaulting to full-image planning")

    if args.sectors:
        # Explicit sector list — treat each as a separate op (content or
        # erase decided per-sector).
        sectors = [int(s.strip(), 0) for s in args.sectors.split(",") if s.strip()]
        ops = plan_flash_ops(ref, len(ref), target_sectors=sectors)
    elif diff_sectors:
        ops = plan_flash_ops(ref, len(ref), target_sectors=diff_sectors)
    else:
        # Cover the ENTIRE reference image. All-FF regions get erase-only
        # ops; content regions get flash-sector/block as before. No more
        # "skip empties" shortcut that leaves stale bytes behind.
        ops = plan_flash_ops(ref, len(ref))
    if args.limit > 0:
        ops = ops[:args.limit]

    kinds = {}
    for k, *_ in ops: kinds[k] = kinds.get(k, 0) + 1
    kind_summary = ", ".join(f"{v}×{k}" for k, v in sorted(kinds.items()))
    non_empty = sum(span for k, _, span in ops if not k.startswith("erase"))
    print(f"  {non_empty // 1024} KB content, "
          f"plan: {kind_summary} ({len(ops)} commands)")

    if not ops:
        print("  plan: no flash operations needed")
        return 0

    # About to write — drop CPU to crystal to match the user's XMODEM-
    # validated flash configuration, then bp_clear (itself a WRSR write).
    maybe_drop_crystal(client, args)
    if not client.bp_clear():
        return 1

    if args.pre_erase_mb > 0:
        # Erase the first N MB of the chip as 64 KB blocks. Catches stale
        # garbage in sectors that are "empty" in the reference and would
        # otherwise be skipped — those leftover bytes break the boot-
        # loader's whole-region CRC check.
        erase_end = args.pre_erase_mb * 0x100000
        pre_t0 = time.monotonic()
        n_blocks = erase_end // 0x10000
        print(f"Pre-erasing 0..0x{erase_end:06x} "
              f"({n_blocks} × 64 KB blocks)...")
        for addr in range(0, erase_end, 0x10000):
            if not client.erase_64k(addr):
                print(f"  pre-erase 0x{addr:06x} FAILED"); return 1
        print(f"Pre-erase done in {time.monotonic()-pre_t0:.1f}s")

    def run_op(kind, addr, span):
        if kind == "erase":       return client.erase(addr)
        if kind == "erase_32k":   return client.erase_32k(addr)
        if kind == "erase_64k":   return client.erase_64k(addr)
        if kind == "sector":      return client.flash_sector(addr, ref[addr:addr+span])
        # block_32k / block_64k
        return client.flash_block(addr, ref[addr:addr+span])

    total_t0 = time.monotonic()
    failed = []
    total_retries = 0
    # Intermittent block-op failures from the READ-exhaustion driver bug
    # in do_verify (mid-block verify reads return stale bytes after ~8+
    # chunks). A simple retry almost always succeeds because the DMA
    # pipeline resets between our CMD_FLASH_BLOCK invocations.
    MAX_RETRIES = 2
    for op_idx, (kind, addr, span) in enumerate(ops):
        op_t0 = time.monotonic()
        ok = run_op(kind, addr, span)
        attempt = 1
        while not ok and attempt <= MAX_RETRIES:
            print(f"  [{op_idx+1}/{len(ops)}] {kind} 0x{addr:06x} "
                  f"attempt {attempt}/{MAX_RETRIES+1} failed — retrying")
            ok = run_op(kind, addr, span)
            attempt += 1
        dt = time.monotonic() - op_t0
        try: op_retries = client.o.mdw(MBOX_ADDR + O_PAIR_RETRIES)
        except Exception: op_retries = 0
        total_retries += op_retries
        status = "OK" if ok else "FAIL"
        retry_note = f" retries={op_retries}" if op_retries else ""
        attempt_note = f" (attempt {attempt})" if attempt > 1 else ""
        print(f"  [{op_idx+1}/{len(ops)}] {kind} 0x{addr:06x} "
              f"len={span} {status} {dt:.2f}s{retry_note}{attempt_note}")
        if not ok:
            failed.append((kind, addr))

    total = time.monotonic() - total_t0
    print(f"\n=== flash-all done in {total:.1f}s, "
          f"{total_retries} AAI pair retries ===")
    if failed:
        print(f"FAILED ops: {failed}")
        # Keep going — repair step below will catch the dropped bytes.

    # Host-side verify + byte-patch repair. Previously we used
    # jtag_dma_read (direct host-driven DMA) with READ_RX_SKIP=12
    # because the on-device CMD_READ had a readback bug that shifted
    # data by -4. Driver fixed 2026-04-20 (READ_RX_SKIP = 4 now);
    # on-device CMD_READ is trustworthy, so repair can just use
    # client.read() — much faster, uses the driver's DMA path.
    if args.repair:
        print(f"\n=== host-side verify + byte-patch repair ===")
        repair_t0 = time.monotonic()
        repairs = 0
        total_bad = 0
        for op_idx, (kind, addr, span) in enumerate(ops):
            want = ref[addr:addr + span]
            got = client.read(addr, span)
            if got is None:
                print(f"  [{op_idx+1}/{len(ops)}] 0x{addr:06x} READ FAILED")
                continue
            diffs = [i for i in range(span) if got[i] != want[i]]
            if not diffs: continue
            total_bad += len(diffs)
            print(f"  [{op_idx+1}/{len(ops)}] 0x{addr:06x} {len(diffs)} diffs, "
                  f"first offs={diffs[:4]} last={diffs[-4:]}")
            # Group contiguous runs of mismatched bytes.
            runs = []
            s = diffs[0]; prev = s
            for b in diffs[1:]:
                if b == prev + 1: prev = b
                else: runs.append((s, prev + 1)); s = prev = b
            runs.append((s, prev + 1))
            for rs, re in runs:
                chunk = want[rs:re]
                if client.byte_patch(addr + rs, chunk):
                    repairs += 1
                else:
                    print(f"  repair 0x{addr+rs:06x}+{re-rs} FAILED")
        rdt = time.monotonic() - repair_t0
        print(f"=== repair done: {total_bad} mismatched bytes "
              f"in {repairs} runs, {rdt:.1f}s ===")
        _append_run_stats(args, client, total, rdt, len(ops), len(failed),
                          total_bad, repairs, total_retries)
    return 0


def _append_run_stats(args, client, flash_s, repair_s, ops, failed_ops,
                      patched_bytes, repair_runs, pair_retries):
    """Append a row to firmware/flash_stats.csv so we can correlate drop
    counts + AAI pair retries with timing configs over many runs."""
    import csv, os, datetime
    tim = client.read_timings()
    stats_path = FIRMWARE_DIR / "flash_stats.csv"
    new_file = not stats_path.exists()
    with open(stats_path, "a", newline="") as f:
        w = csv.writer(f)
        if new_file:
            w.writerow([
                "timestamp", "ref",
                "aai_pair_fixed_us", "aai_pair_poll_budget_us",
                "erase_fixed_us", "xport_mode",
                "ops", "failed_ops", "pair_retries",
                "patched_bytes", "repair_runs",
                "flash_s", "repair_s",
            ])
        w.writerow([
            datetime.datetime.now().isoformat(timespec="seconds"),
            os.path.basename(args.ref),
            tim.get("aai_pair_fixed_us", ""),
            tim.get("aai_pair_poll_budget_us", ""),
            tim.get("erase_fixed_us", ""),
            tim.get("xport_mode", ""),
            ops, failed_ops, pair_retries,
            patched_bytes, repair_runs,
            f"{flash_s:.1f}", f"{repair_s:.1f}",
        ])
    print(f"  stats appended → {stats_path}")


def _shift_repro(client, args):
    """Minimal +2-byte shift reproduction harness.

    Two modes, chosen by --repro-mode:

      read-only: tight-loop V2_CMD_READ of a fixed flash region and
                 compare host-side. Exercises ONLY do_read →
                 dma_bidir_read. Does not write to flash. The post-AAI
                 shift typically does NOT reproduce here (reads by
                 themselves are clean) — use this mode to confirm pure
                 reads are good, or to collect long LA captures.

      sector-loop (default): loops V2_CMD_FLASH_SECTOR on a scratch
                 sector (--test-sector), which runs
                 erase → AAI-program → on-device verify exactly like
                 flash-all. This reliably triggers the shift. The
                 on-device VERIFY_MISS capture is read back per-iter.

    Trigger suggestion for an LA: scope the 4 flash-SPI signals
    (CS, CLK, MOSI, MISO) at 0xCC000000. In sector-loop mode each
    iter is ~1.5–2 s of wire activity (erase, then AAI pairs ≈ 8 ms
    at 3 MHz + spi_reset_clean, then verify reads). Trigger on
    MISO-data-transition during the verify phase to catch the shift.

    Per-miss output includes the offset, the 4-byte got/expected
    window, and the shift magnitude inferred from got[0]==exp[N].
    """
    import time

    ref = Path(args.ref).read_bytes()
    spi_addr = args.repro_addr
    length   = args.repro_length
    n_iters  = args.repro_iters
    mode     = args.repro_mode

    if mode == "read-only":
        if spi_addr + length > len(ref):
            raise SystemExit(
                f"--repro-addr 0x{spi_addr:x} + --repro-length {length} "
                f"exceeds reference image ({len(ref)} bytes)")
        expected = ref[spi_addr : spi_addr + length]
        print(f"=== shift-repro read-only: addr=0x{spi_addr:06x} "
              f"len={length} iters={n_iters} ===")
    elif mode == "sector-loop":
        sector = args.test_sector & ~0xFFF
        length = 4096
        pattern = bytes([0xFF] * 1022 + [0x00] * 1024
                        + [(i * 37) & 0xFF for i in range(2050)])[:4096]
        expected = pattern
        print(f"=== shift-repro sector-loop: sector=0x{sector:06x} "
              f"iters={n_iters} (will erase+program+verify each iter) ===")
    else:
        # block-loop: 64 KB FLASH_BLOCK per iter, 64 KB-aligned address.
        # Take the test sector's 64 KB-aligned base; if it falls inside
        # the golden-copy-protected 0x10000-0x3FFFF range, use 0x1F0000
        # (last 64 KB — always safe).
        block = args.test_sector & ~0xFFFF
        if 0x10000 <= block <= 0x3F0000 and block < 0x40000:
            block = 0x1F0000
        length = 0x10000
        # Synthetic pattern rich in 0xFF → 0x00 transitions — that's the
        # data-shape where the +2-byte shift is observable (got prepends
        # 0x00 0x00 before an expected 0xFF 0xFF). 256 B repeating
        # structure: 2 × FF, 2 × 00, 252 bytes of varying. Every
        # 256-byte boundary has a transition that the verify reads
        # against. Also pull a chunk of real firmware data from 0x40000
        # at offset 0x2000 (mid-pattern) so if the shift IS data-
        # dependent we've got both varieties in one buffer.
        fw_slab = ref[0x40000:0x40000 + 0x1000] if len(ref) >= 0x41000 \
                  else bytes(range(256)) * 16
        parts = []
        for chunk_i in range(length // 256):
            if chunk_i % 16 == 0:
                parts.append(fw_slab[(chunk_i // 16) * 256 :
                                      (chunk_i // 16) * 256 + 256])
            else:
                parts.append(bytes([0xFF, 0xFF, 0x00, 0x00] +
                                   [(chunk_i * 37 + j) & 0xFF
                                    for j in range(252)]))
        pattern = b"".join(parts)[:length]
        expected = pattern
        print(f"=== shift-repro block-loop: block=0x{block:06x} "
              f"len=0x{length:x} iters={n_iters} ===")
        print(f"    (each iter: erase+AAI-program+on-device-verify; "
              f"~1.5-2 s/iter)")
        print(f"    pattern: synthetic FF-FF-00-00 transitions every "
              f"256 B + firmware slab every 16th bucket")
        if args.repro_dump_pattern:
            p = Path(args.repro_dump_pattern)
            # Lay pattern out at the same block offset so parse_la_log.py
            # can pass it as --ref and address-resolve naturally.
            buf = bytearray(b"\xFF" * (block + length))
            buf[block:block + length] = pattern
            p.write_bytes(bytes(buf))
            print(f"    wrote pattern ref to {p} "
                  f"({block + length} bytes, pattern @0x{block:06x})")

    misses = []
    t0 = time.monotonic()
    for i in range(n_iters):
        if mode == "read-only":
            data = client.read(spi_addr, length)
            if data is None:
                print(f"  iter {i}: READ FAILED")
                continue
        else:
            if mode == "block-loop":
                ok = client.flash_block(block, pattern)
                target = block
            else:
                ok = client.flash_sector(sector, pattern)
                target = sector
            if ok:
                # On-device verify already ran (CMD_FLASH_BLOCK does
                # erase+program+verify atomically). Trust it — a host
                # readback of 64 KB into DATA_BUF_ADDR would overflow
                # the 4 KB DATA_BUF and clobber driver code at 0x2C000+.
                # If belt-and-suspenders host readback is desired,
                # split into 4 KB chunks against a scratch buf.
                continue
            else:
                # On-device verify already caught the miss — mailbox has
                # miss_* fields populated. Read them and synthesize a
                # "data" string that produces the same miss.
                client.o.halt()
                miss_off = client.o.mdw(MBOX_ADDR + O_MISS_OFFSET)
                miss_got = client.o.mdw(MBOX_ADDR + O_MISS_GOT)
                miss_exp = client.o.mdw(MBOX_ADDR + O_MISS_EXP)
                client.o.resume()
                got4 = miss_got.to_bytes(4, "little")
                exp4 = miss_exp.to_bytes(4, "little")
                shift_hint = ""
                for s in (1, 2, 3):
                    if got4[0] == exp4[s]:
                        shift_hint = f" +{s}B shift"
                        break
                else:
                    # Off-buffer shift (got[0] beyond exp's 4-byte
                    # window). Probe the synthesized pattern for a
                    # match up to +8 bytes — TX_SCRATCH fix exposed
                    # post-AAI shifts of +4 (one full word).
                    for s in range(4, 9):
                        target = miss_off + s
                        if target + 4 <= len(expected) \
                                and expected[target:target+4] == got4:
                            shift_hint = f" +{s}B shift"
                            break
                print(f"  iter {i}: DEVICE VERIFY_MISS @0x{miss_off:x}"
                      f"  got={got4.hex(' ')} exp={exp4.hex(' ')}"
                      f"{shift_hint}")
                misses.append((i, miss_off, got4, exp4, shift_hint))
                continue

        if data == expected:
            continue
        first = next(idx for idx in range(length) if data[idx] != expected[idx])
        got4 = data[first:first+4].ljust(4, b'\x00')
        exp4 = expected[first:first+4].ljust(4, b'\x00')
        shift_hint = ""
        for s in range(1, 9):
            if first + s + 4 <= length and \
                    expected[first + s:first + s + 4] == got4:
                shift_hint = f" +{s}B shift"
                break
        print(f"  iter {i}: MISS @0x{first:04x} (dec {first})"
              f"  got={got4.hex(' ')} exp={exp4.hex(' ')}{shift_hint}")
        misses.append((i, first, bytes(got4), bytes(exp4), shift_hint))

    dt = time.monotonic() - t0
    print()
    print(f"=== summary: {len(misses)}/{n_iters} misses in {dt:.1f}s"
          f" ({n_iters / dt:.2f} iter/s) ===")
    if misses:
        import collections
        offsets = sorted({m[1] for m in misses})
        print(f"Miss offsets ({len(offsets)} unique): "
              f"{', '.join(f'0x{o:x}' for o in offsets[:16])}"
              f"{'...' if len(offsets) > 16 else ''}")
        cnt = collections.Counter(o & ~0xFF for o in (m[1] for m in misses))
        top = cnt.most_common(5)
        print(f"Top miss regions (256 B buckets): "
              f"{', '.join(f'0x{o:x}×{c}' for o, c in top)}")
        shifts = collections.Counter(m[4].strip() for m in misses)
        print(f"Shift classifications: {dict(shifts)}")
    return 0 if not misses else 2


def main(argv=None):
    p = argparse.ArgumentParser(description=__doc__)
    p.add_argument("--ref", default=str(FIRMWARE_DIR / "blender_spi_patched.bin"),
                   help="Reference SPI image for smoke tests")
    p.add_argument("--speed", type=int, default=1000,
                   help="JTAG adapter clock in kHz")
    p.add_argument("--test-sector", type=lambda x: int(x, 0), default=0x3F000,
                   help="Scratch sector for test mode (default 0x3F000)")
    p.add_argument("--mode", choices=("test", "erase-only", "readback",
                                       "flash-all", "autonomous",
                                       "block-sector", "block-64k",
                                       "byte-test", "byte-only", "read-id",
                                       "shift-repro", "hash-range"),
                   default="test",
                   help="'test' = host-driven erase+program+verify on test sector, "
                        "'autonomous' = single V2_CMD_FLASH_SECTOR (4 KB only), "
                        "'block-sector' = V2_CMD_FLASH_BLOCK with a 4 KB payload "
                        "(exercises auto-pick → SE), "
                        "'erase-only' = just erase, "
                        "'readback' = read 4 KB from --test-sector to stdout hex, "
                        "'flash-all' = write every non-empty sector from --ref")
    p.add_argument("--limit", type=int, default=0,
                   help="flash-all: stop after N sectors (0 = all)")
    p.add_argument("--pre-erase-mb", type=int, default=0,
                   help="flash-all: 64 KB-block-erase the first N MB before "
                        "programming. Use 1 for primary region (catches "
                        "stale bytes in sectors that are empty in --ref but "
                        "non-erased on chip). 0 = no pre-erase (default).")
    p.add_argument("--repair", action=argparse.BooleanOptionalAction, default=False,
                   help="flash-all: after flashing, host-side-verify every op via "
                        "CMD_READ and byte-program any mismatched bytes. Default "
                        "OFF — flash ops already include on-device verify via "
                        "do_verify (same dma_bidir_read path), so repair is "
                        "redundant when all flash ops report OK. Re-enable with "
                        "--repair if you distrust on-device verify.")
    p.add_argument("--skip-verify", action="store_true",
                   help="flash-all: skip per-sector verify (faster, riskier)")
    p.add_argument("--sectors", type=str, default="",
                   help="flash-all: comma-separated hex sector addrs "
                        "(overrides the non-empty scan of --ref)")
    p.add_argument("--erase-policy", choices=("se", "be32", "be64", "smart"),
                   default="smart",
                   help="flash-all erase strategy: "
                        "'se' = one 4 KB SE per sector (no over-erase), "
                        "'be64' = always 64 KB BE per block, "
                        "'smart' = BE64 for ≥8/16 sectors, BE32 for ≥4/8, "
                        "SE otherwise (default)")
    p.add_argument("--timing", action="append", default=[],
                   metavar="NAME=VALUE",
                   help=f"Override a v2_timings field (in µs). Repeatable. "
                        f"Known names: {', '.join(TIMING_FIELDS)}. "
                        f"Unset fields use the on-device defaults.")
    p.add_argument("--show-timings", action="store_true",
                   help="Print the active on-device timings and exit.")
    p.add_argument("--repro-dump-pattern", type=str, default="",
                   metavar="FILE",
                   help="shift-repro: also write the synthetic block-loop "
                        "pattern to FILE (at byte offset = --test-sector). "
                        "Useful as the --ref for parse_la_log.py when "
                        "decoding logic-analyzer captures of the repro.")
    p.add_argument("--repro-mode",
                   choices=("read-only", "sector-loop", "block-loop"),
                   default="block-loop",
                   help="shift-repro backend. 'block-loop' (default) drives "
                        "erase+AAI+verify on a 64 KB block (64 KB → many more "
                        "AAI pairs + verify chunks per iter than sector-loop, "
                        "matches the actual flash-all pattern that reproduces "
                        "the shift). 'sector-loop' runs a 4 KB FLASH_SECTOR per "
                        "iter on --test-sector. 'read-only' tight-loops "
                        "CMD_READ of --repro-addr for LA captures of pure "
                        "reads (pure reads do NOT reproduce the shift).")
    p.add_argument("--repro-addr", type=lambda x: int(x, 0), default=0x070000,
                   help="shift-repro: SPI address to read in a tight loop. "
                        "Default 0x70000 is an empirically failure-prone block "
                        "on the patched image (2026-04-22).")
    p.add_argument("--repro-length", type=int, default=4096,
                   help="shift-repro: bytes per read (default 4096 = one sector). "
                        "The v2 driver reads in READ_CHUNK (2 KB) sub-chunks, so "
                        "4096 exercises 2 back-to-back dma_bidir_reads per iter.")
    p.add_argument("--repro-iters", type=int, default=200,
                   help="shift-repro: number of read iterations (default 200)")
    p.add_argument("--repro-variant",
                   choices=("bidir", "rxonly"),
                   default="bidir",
                   help="shift-repro: which read backend to exercise. "
                        "'bidir' (default) = current dma_bidir_read "
                        "(DMAMD=3, has +4B post-AAI shift bug). "
                        "'rxonly' = dma_rxonly_read mirroring the ROM "
                        "bootloader's read path (DMAMD=1, CTRL=0x307, "
                        "cmd via PIO DATA-register writes, RX-DMA "
                        "from DATA register). Bypasses the bidir FIFO "
                        "packer state.")
    p.add_argument("--cpu-clock",
                   choices=("as-is", "crystal", "pll"),
                   default="as-is",
                   help="shift-repro: force CPU/AHB clock state before the "
                        "loop. 'as-is' (default) leaves whatever the chip "
                        "arrived in. 'crystal' drops PLL_OUTPUT (CPU ~12 MHz). "
                        "'pll' re-enables PLL (CPU ~196 MHz). Diagnostic for "
                        "the +2B shift's correlation with CPU/AHB-clock-tied "
                        "DMA controller speed.")
    p.add_argument("--clk-div-spi", type=lambda x: int(x, 0), default=None,
                   metavar="VAL",
                   help="shift-repro: write VAL to CLK_DIV_SPI (0xC9000014) "
                        "after timings are set. Use 0x8000+div to tie SPI parent "
                        "to PLL with the given post-PLL divider (e.g. 0x8022 = "
                        "200MHz/34 ≈ 6 MHz IP parent → 3 MHz wire after SPI_CLK=2). "
                        "0x0000 = crystal fallback (the driver's default). Pair "
                        "with --timing verify_quiesce_mode=0 so the driver does "
                        "NOT reset CLK_DIV_SPI back to crystal between reads. "
                        "Tests the user hypothesis: does the shift go away when "
                        "SPI parent and AHB/DMA both run from PLL?")
    p.add_argument("--byte-addr", type=lambda x: int(x, 0), default=0x3F020,
                   help="byte-test: target SPI address for the single-byte write")
    p.add_argument("--byte-val", type=lambda x: int(x, 0), default=0x5A,
                   help="byte-test: byte value to program")
    p.add_argument("--xport", choices=("dma", "pio", "dma-tx"), default=None,
                   help="Select AAI hot-loop transport backend on the "
                        "driver (dma = bidir DMA, current default; pio = "
                        "byte-by-byte DATA writes, broken; dma-tx = TX-only "
                        "DMA with TMOD=1, isolates the DW SSI bidir bus-"
                        "arbitration tail-drop from silicon-level drops).")
    p.add_argument("--nreset", action="store_true",
                   help="Pulse hardware nRESET (reset halt) after OpenOCD "
                        "connects, before loading the SRAM driver. Uses "
                        f"{OPENOCD_CFG_SRST.name} (SRST enabled); use when "
                        "the TAP is wedged or the driver never reaches READY.")
    p.add_argument("--openocd-cfg", type=str, default="",
                   help="OpenOCD config file (default: miolink-dice3-openocd.cfg, "
                        "or -srst variant when --nreset is set)")
    p.add_argument("--reboot", action="store_true",
                   help="Run reboot strategy after a successful flash run.")
    p.add_argument("--no-crystal", dest="drop_to_crystal",
                   action="store_false", default=True,
                   help="Skip the pre-flash PLL-disable step. Default-on: "
                        "the host writes 0xABCD0000 to 0xC900000C (PLL_OUTPUT) "
                        "with CPU halted, dropping CPU to crystal — matches "
                        "the TCAT-BOOT shell state where flash is known-good.")
    p.add_argument("--no-diff", dest="diff",
                   action="store_false", default=True,
                   help="Skip the pre-flash per-sector CRC32 diff in flash-all. "
                        "Default-on: sweep every sector with V2_CMD_HASH_RANGE "
                        "(uses the bootloader's ROM CRC32 routine at 0x20000DA4) "
                        "and only flash sectors whose CRC differs from --ref. "
                        "Ignored when --sectors is given.")
    args = p.parse_args(argv)

    if not DRIVER_BIN.exists():
        raise SystemExit(f"ERROR: driver binary not found: {DRIVER_BIN}. "
                         "Run: arm-none-eabi-gcc ... && objcopy ...")

    timing_overrides = {}
    for spec in args.timing:
        if "=" not in spec:
            raise SystemExit(f"bad --timing {spec!r}: need NAME=VALUE")
        name, val = spec.split("=", 1)
        name = name.strip()
        if name not in TIMING_DEFAULTS:
            raise SystemExit(f"unknown --timing field {name!r}; "
                             f"known: {list(TIMING_DEFAULTS)}")
        timing_overrides[name] = int(val.strip(), 0)
    if args.xport is not None:
        timing_overrides["xport_mode"] = {
            "dma":    XPORT_DMA,
            "pio":    XPORT_PIO,
            "dma-tx": XPORT_DMA_TX,
        }[args.xport]
    if getattr(args, "repro_variant", "bidir") == "rxonly":
        timing_overrides["read_mode"] = READ_MODE_RXONLY

    if args.openocd_cfg:
        ocd_cfg = Path(args.openocd_cfg)
    elif args.nreset:
        if not OPENOCD_CFG_SRST.exists():
            raise SystemExit(
                f"ERROR: --nreset needs {OPENOCD_CFG_SRST} (missing). "
                "Copy from miolink-dice3-openocd.cfg or pass --openocd-cfg."
            )
        ocd_cfg = OPENOCD_CFG_SRST
    else:
        ocd_cfg = OPENOCD_CFG

    ocd = OpenOCD(speed=args.speed, openocd_cfg=ocd_cfg)
    rc = 1
    try:
        if args.nreset:
            print("nRESET: reset halt (target should stop at reset vector)...")
            try:
                ocd.reset_halt()
            except Exception as ex:
                raise SystemExit(
                    f"nRESET/reset halt failed ({ex}). "
                    "Check MioLink nRST wiring to DICE3; try without --nreset."
                ) from ex
            time.sleep(0.15)
        client = FlashClientV2(ocd)
        # Crystal-drop deferred until just before any write op (see
        # maybe_drop_crystal). Reads and CRC sweeps run at whatever clock
        # the chip arrived in — PLL after a successful boot, crystal in
        # TCAT-BOOT shell. The bootloader's CRC routine validates the
        # SPI bus + DMA at PLL on every successful boot worldwide, so
        # PLL reads are safe.
        client.load_driver()
        if timing_overrides:
            client.set_timings(**timing_overrides)
        if args.show_timings:
            t = client.read_timings()
            for k, v in t.items():
                print(f"  {k} = {v}")
            rc = 0
            return rc
        if args.mode == "flash-all":
            rc = flash_all(client, args)
            return rc
        if args.mode == "autonomous":
            maybe_drop_crystal(client, args)
            if not client.bp_clear():
                raise SystemExit(1)
            pattern = bytes(((i * 37) & 0xFF) if (i % 257) else 0
                            for i in range(SECTOR_SIZE))
            t0 = time.monotonic()
            ok = client.flash_sector(args.test_sector, pattern)
            dt = time.monotonic() - t0
            print(f"autonomous flash_sector: {dt:.2f}s wall, "
                  f"{'OK' if ok else 'FAIL'}")
            rc = 0 if ok else 1
            return rc
        if args.mode == "block-sector":
            # V2_CMD_FLASH_BLOCK with a single-sector (4 KB) payload —
            # exercises the auto-pick logic on its SE branch.
            maybe_drop_crystal(client, args)
            if not client.bp_clear():
                raise SystemExit(1)
            pattern = bytes(((i * 37) & 0xFF) if (i % 257) else 0
                            for i in range(SECTOR_SIZE))
            t0 = time.monotonic()
            ok = client.flash_block(args.test_sector, pattern)
            dt = time.monotonic() - t0
            print(f"flash_block(4 KB): {dt:.2f}s wall, "
                  f"{'OK' if ok else 'FAIL'}")
            rc = 0 if ok else 1
            return rc
        if args.mode == "block-64k":
            # FLASH_BLOCK with a 64 KB payload — auto-pick → BE64.
            # --test-sector must be 64 KB-aligned and pointing at an
            # expendable region (0x90000+ is all-0xFF in the patched
            # image, safe).
            if args.test_sector & 0xFFFF:
                raise SystemExit("--test-sector must be 64 KB-aligned for block-64k")
            maybe_drop_crystal(client, args)
            if not client.bp_clear():
                raise SystemExit(1)
            pattern = bytes(((i * 37) & 0xFF) if (i % 257) else 0
                            for i in range(0x10000))
            t0 = time.monotonic()
            ok = client.flash_block(args.test_sector, pattern)
            dt = time.monotonic() - t0
            print(f"flash_block(64 KB): {dt:.2f}s wall, "
                  f"{'OK' if ok else 'FAIL'}")
            rc = 0 if ok else 1
            return rc
        if args.mode == "read-id":
            jedec = client.read_id(mode="jedec")
            legacy = client.read_id(mode="legacy")
            print(f"JEDEC  (0x9F, 3B): {jedec.hex() if jedec else 'FAIL'} "
                  f"(expect bf 25 41 for SST25VF016B)")
            print(f"Legacy (0xAB, 1B): {legacy.hex() if legacy else 'FAIL'} "
                  f"(expect bf)")
            ok = (jedec == bytes.fromhex("bf2541")
                  and legacy == bytes.fromhex("bf"))
            print(f"read-id: {'OK' if ok else 'FAIL'}")
            rc = 0 if ok else 1
            return rc
        if args.mode == "hash-range":
            # T4 verification: device CRC vs zlib-equivalent CRC of the
            # same range read back via CMD_READ. They must match exactly.
            addr = args.test_sector & ~0xFFF
            length = SECTOR_SIZE
            print(f"hash-range probe at 0x{addr:06x} len={length}...")
            dev = client.device_crc32(addr, length, quiet=False)
            host_buf = client.read(addr, length)
            host = _zlib_rom_crc32(host_buf) if host_buf else None
            print(f"  device CRC32 : 0x{dev:08x}")
            print(f"  host   CRC32 : "
                  f"{'0x' + format(host, '08x') if host is not None else 'READ FAIL'}")
            ok = (host is not None and dev == host)
            print(f"hash-range: {'OK' if ok else 'FAIL'}")
            rc = 0 if ok else 1
            return rc
        if args.mode == "byte-only":
            # Minimal: issue ONE BYTE_PRG at --byte-addr with --byte-val.
            # No bp_clear, no erase, no readback. For logic-analyzer probing.
            # On the wire you'll see: WREN (0x06) + BYTE_PRG (0x02 + 3B addr
            # + 1B data) + RDSR poll loop (0x05 + status until !BUSY).
            maybe_drop_crystal(client, args)
            if not client.byte_patch(args.byte_addr, bytes([args.byte_val])):
                raise SystemExit("byte_patch failed")
            print(f"byte-only: wrote 0x{args.byte_val:02x} @ 0x{args.byte_addr:06x}")
            print("Wire sequence (each = separate CS cycle):")
            print(f"  1. WREN   : 06")
            ah = (args.byte_addr >> 16) & 0xFF
            am = (args.byte_addr >>  8) & 0xFF
            al =  args.byte_addr        & 0xFF
            print(f"  2. BYTE_PRG: 02 {ah:02x} {am:02x} {al:02x} {args.byte_val:02x}")
            print(f"  3. RDSR    : 05 XX (poll until !BUSY)  [repeats]")
            rc = 0
            return rc
        if args.mode == "byte-test":
            # Isolate BYTE_PROGRAM landing offset: erase the containing
            # sector, write one byte at --byte-addr, read the sector
            # back, report which offsets ended up non-0xFF. Confirms
            # whether the off-by-4 symptom survived the IRQ refactor.
            sector = args.byte_addr & ~0xFFF
            offset_in_sector = args.byte_addr & 0xFFF
            maybe_drop_crystal(client, args)
            if not client.bp_clear():
                raise SystemExit(1)
            if not client.erase(sector):
                raise SystemExit(f"erase 0x{sector:06x} failed")
            if not client.byte_patch(args.byte_addr, bytes([args.byte_val])):
                raise SystemExit("byte_patch failed")
            data = client.read(sector, SECTOR_SIZE)
            if data is None:
                raise SystemExit("readback failed")
            hits = [(i, b) for i, b in enumerate(data) if b != 0xFF]
            print(f"byte-test: asked addr=0x{args.byte_addr:06x} "
                  f"(sector 0x{sector:06x} + 0x{offset_in_sector:03x}) "
                  f"val=0x{args.byte_val:02x}")
            print(f"byte-test: {len(hits)} non-0xFF byte(s) in sector:")
            for off, b in hits:
                delta = off - offset_in_sector
                print(f"  +0x{off:03x}  = 0x{b:02x}  "
                      f"(delta {delta:+d} from asked)")
            expected = [(offset_in_sector, args.byte_val)]
            ok = hits == expected
            print(f"byte-test: {'OK' if ok else 'FAIL'}")
            rc = 0 if ok else 1
            return rc
        if args.mode == "shift-repro":
            cpu_clock = getattr(args, "cpu_clock", "as-is")
            if cpu_clock == "crystal":
                ensure_crystal_clock(client.o)
            elif cpu_clock == "pll":
                ensure_pll_clock(client.o)
            if args.clk_div_spi is not None:
                client.o.halt()
                before = client.o.mdw(0xC9000014)
                client.o.mww(0xC9000014, 0xABCD0000 | (args.clk_div_spi & 0xFFFF))
                after = client.o.mdw(0xC9000014)
                client.o.resume()
                print(f"  CLK_DIV_SPI: 0x{before:04x} → 0x{after:04x}")
            rc = _shift_repro(client, args)
            return rc
        if args.mode == "erase-only":
            maybe_drop_crystal(client, args)
            ok = client.erase(args.test_sector)
        elif args.mode == "readback":
            data = client.read(args.test_sector, SECTOR_SIZE)
            if data is None:
                ok = False
            else:
                print(f"Sector 0x{args.test_sector:06x} first 32 bytes:")
                print("  " + " ".join(f"{b:02x}" for b in data[:32]))
                print(f"Sector 0x{args.test_sector:06x} last 32 bytes:")
                print("  " + " ".join(f"{b:02x}" for b in data[-32:]))
                ok = True
        else:
            # Full round-trip: erase → program known pattern → verify.
            maybe_drop_crystal(client, args)
            if not client.bp_clear():
                raise SystemExit(1)
            if not client.erase(args.test_sector):
                raise SystemExit(1)
            pattern = bytes(((i * 37) & 0xFF) if (i % 257) else 0
                            for i in range(SECTOR_SIZE))
            if not client.program(args.test_sector, pattern):
                raise SystemExit(1)
            if not client.verify(args.test_sector, pattern):
                raise SystemExit(1)
            print("ALL GOOD.")
            ok = True
        rc = 0 if ok else 1
        return rc
    finally:
        ocd.close()
        if args.reboot and rc == 0:
            print("Post-flash reboot via shared reboot path...")
            reboot_rc = run_reboot(speed=args.speed, classify=True)
            if reboot_rc != 0:
                print(f"WARNING: post-flash reboot failed ({reboot_rc})", file=sys.stderr)
                raise SystemExit(reboot_rc)


if __name__ == "__main__":
    raise SystemExit(main())
