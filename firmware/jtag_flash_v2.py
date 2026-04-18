#!/usr/bin/env python3 -u
"""Host-side client for the v2 SRAM flash driver.

Speaks the mailbox protocol defined in sram_flash_mailbox_v2.h:
  * Single-writer-per-field (closes the v1 init race).
  * Phase-based progress polling (no halt required — mdw while running works).
  * Structured log ring dump on ERROR for postmortem without guessing.

CLI:
    python3 firmware/jtag_flash_v2.py --ref firmware/blender_spi_patched.bin \\
        --speed 1000 --test-sector 0x3f000
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
from pathlib import Path

FIRMWARE_DIR = Path(__file__).resolve().parent
ROOT = FIRMWARE_DIR.parent
OPENOCD_CFG = ROOT / "jtag" / "miolink-dice3-openocd.cfg"
DRIVER_BIN = FIRMWARE_DIR / "sram_flash_driver_v2.bin"

# Keep in sync with sram_flash_mailbox_v2.h.
MBOX_ADDR        = 0x0002E000
TIMINGS_ADDR     = 0x0002E180
LOG_RING_ADDR    = 0x0002E200
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
)
XPORT_DMA = 0
XPORT_PIO = 1
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
    0x40: "VERIFY_OK", 0x41: "VERIFY_MISS",
    0x50: "REPAIR_HIT",
    0x60: "DMA_TX_DONE", 0x61: "DMA_RX_DONE",
    0xF0: "TIMEOUT", 0xF1: "ABORT", 0xF2: "BAD_STATE",
}

SECTOR_SIZE = 0x1000
LOG_RING_ENTRIES = 128
LOG_ENTRY_SIZE = 12  # bytes


class OpenOCD:
    """Thin wrapper over openocd's TCL socket — reused conventions from v1."""
    def __init__(self, speed=1000):
        subprocess.run(["pkill", "-x", "openocd"], capture_output=True)
        time.sleep(1)
        self.proc = subprocess.Popen(
            ["openocd", "-f", str(OPENOCD_CFG)],
            stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL,
        )
        time.sleep(3)
        self.sock = socket.create_connection(("localhost", 6666), timeout=30)
        self.sock.settimeout(30)
        self.cmd(f"adapter speed {speed}")

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


class FlashClientV2:
    def __init__(self, ocd, log_path=None):
        self.o = ocd
        self.log_path = log_path or "/tmp/flash_v2.log"

    # ── Driver lifecycle ──────────────────────────────────────
    def load_driver(self):
        print("Loading v2 driver...")
        self.o.halt()
        host_teardown(self.o)
        self.o.load_image(str(DRIVER_BIN), DRIVER_CODE_ADDR)
        # Zero the mailbox (HOST fields); driver only touches DEV fields.
        for off in (O_MAGIC, O_COMMAND, O_FLASH_ADDR, O_BUF_ADDR, O_LENGTH):
            self.o.mww(MBOX_ADDR + off, 0)
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
               poll_interval=0.05, overall_timeout=None):
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

            if seq != last_seq:
                print(f"  [{time.monotonic() - t_start:6.2f}s] "
                      f"status={status} phase={PHASE_NAMES.get(phase, phase)}"
                      f" detail={detail}")
                last_seq = seq

            if status == STATUS_OK:
                self.o.halt()
                elapsed = self.o.mdw(MBOX_ADDR + O_ELAPSED_US)
                last_sr = self.o.mdw(MBOX_ADDR + O_LAST_SR)
                self.o.resume()
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
                f.write(f"{t_us:>10} {ename:<16} {pname:<12} "
                        f"{detail:>6} 0x{spi:08x}\n")
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


def plan_flash_ops(sectors, block_threshold_64k=12, block_threshold_32k=6):
    """Pick flash ops covering `sectors`. Each op is one JTAG round-trip.

    Per 64 KB block containing any non-empty sector:
      - If ≥ block_threshold_64k sectors non-empty → FLASH_BLOCK(64K)
      - Else split into two 32 KB halves:
          - Half with ≥ block_threshold_32k sectors → FLASH_BLOCK(32K)
          - Else per-sector FLASH_SECTOR
    Returns a list of ("block_64k"|"block_32k"|"sector", addr, span) tuples.
    The span is 0x10000, 0x8000, or 0x1000 respectively — tells flash_all
    how many bytes to upload.
    """
    sectors = sorted(set(sectors))
    by_64k = {}
    for s in sectors:
        by_64k.setdefault(s & ~(BLOCK_64K - 1), []).append(s)

    ops = []
    for blk_addr, sec_list in sorted(by_64k.items()):
        if len(sec_list) >= block_threshold_64k:
            ops.append(("block_64k", blk_addr, BLOCK_64K))
            continue
        low  = [s for s in sec_list if s < blk_addr + BLOCK_32K]
        high = [s for s in sec_list if s >= blk_addr + BLOCK_32K]
        for half_secs, half_addr in ((low, blk_addr),
                                     (high, blk_addr + BLOCK_32K)):
            if not half_secs: continue
            if len(half_secs) >= block_threshold_32k:
                ops.append(("block_32k", half_addr, BLOCK_32K))
            else:
                for s in half_secs:
                    ops.append(("sector", s, SECTOR_SIZE))
    return ops


def flash_all(client, args):
    """Flash every non-empty sector of --ref.

    Prefers one autonomous block op per 64 KB region when the block is
    mostly full (≥12 sectors non-empty), else falls back to per-sector
    ops. Saves round-trips vs the old erase+program+verify loop."""
    ref = Path(args.ref).read_bytes()
    print(f"Reference: {args.ref} ({len(ref)} bytes)")

    if args.sectors:
        sectors = [int(s.strip(), 0) for s in args.sectors.split(",") if s.strip()]
    else:
        sectors = []
        for addr in range(0, len(ref), SECTOR_SIZE):
            if any(b != 0xFF for b in ref[addr:addr + SECTOR_SIZE]):
                sectors.append(addr)
    if args.limit > 0:
        sectors = sectors[:args.limit]

    ops = plan_flash_ops(sectors)
    kinds = {}
    for k, *_ in ops: kinds[k] = kinds.get(k, 0) + 1
    kind_summary = ", ".join(f"{v}×{k}" for k, v in sorted(kinds.items()))
    total_kb = len(sectors) * SECTOR_SIZE // 1024
    print(f"  {len(sectors)} non-empty sectors ({total_kb} KB); "
          f"plan: {kind_summary} ({len(ops)} commands)")

    if not client.bp_clear():
        return 1

    total_t0 = time.monotonic()
    failed = []
    total_retries = 0
    for op_idx, (kind, addr, span) in enumerate(ops):
        # Pull the block data from ref.
        data = ref[addr:addr + span]
        op_t0 = time.monotonic()
        if kind == "sector":
            ok = client.flash_sector(addr, data)
        else:
            ok = client.flash_block(addr, data)
        dt = time.monotonic() - op_t0
        # Driver resets pair_retries per command, so snapshot after each op.
        try: op_retries = client.o.mdw(MBOX_ADDR + O_PAIR_RETRIES)
        except Exception: op_retries = 0
        total_retries += op_retries
        label = kind if kind != "sector" else "sector"
        status = "OK" if ok else "FAIL"
        retry_note = f" retries={op_retries}" if op_retries else ""
        print(f"  [{op_idx+1}/{len(ops)}] {label} 0x{addr:06x} "
              f"len={span} {status} {dt:.2f}s{retry_note}")
        if not ok:
            # Mark all sectors covered by this op as failed.
            for s in sectors:
                if addr <= s < addr + span:
                    failed.append((kind, s))

    total = time.monotonic() - total_t0
    print(f"\n=== flash-all done in {total:.1f}s, "
          f"{total_retries} AAI pair retries ===")
    if failed:
        print(f"FAILED ops: {failed}")
        # Keep going — repair step below will catch the dropped bytes.

    # Host-side verify + byte-patch repair. Both on-device verify AND
    # on-device CMD_READ read through a post-write buffer that hides tail
    # drops. Use JTAG-driven bidir DMA (skip=12) to see true cell state.
    if args.repair:
        import sys as _sys
        _sys.path.insert(0, str(FIRMWARE_DIR))
        from debug_flash_harness import jtag_dma_read
        import debug_flash_harness as h
        h.READ_RX_SKIP = 12
        print(f"\n=== host-side verify + byte-patch repair ===")
        repair_t0 = time.monotonic()
        repairs = 0
        total_bad = 0
        for op_idx, (kind, addr, span) in enumerate(ops):
            want = ref[addr:addr + span]
            got = jtag_dma_read(client.o, addr, span)
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


def main():
    p = argparse.ArgumentParser(description=__doc__)
    p.add_argument("--ref", default=str(FIRMWARE_DIR / "blender_spi_patched.bin"),
                   help="Reference SPI image for smoke tests")
    p.add_argument("--speed", type=int, default=1000,
                   help="JTAG adapter clock in kHz")
    p.add_argument("--test-sector", type=lambda x: int(x, 0), default=0x3F000,
                   help="Scratch sector for test mode (default 0x3F000)")
    p.add_argument("--mode", choices=("test", "erase-only", "readback",
                                       "flash-all", "autonomous",
                                       "block-sector", "block-64k"),
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
    p.add_argument("--repair", action=argparse.BooleanOptionalAction, default=True,
                   help="flash-all: after flashing, host-side-verify every op via "
                        "CMD_READ and byte-program any mismatched bytes (default on). "
                        "Counters the on-device verify's tendency to miss tail drops.")
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
    p.add_argument("--xport", choices=("dma", "pio"), default=None,
                   help="Select AAI hot-loop transport backend on the "
                        "driver (dma = current; pio = byte-by-byte DATA "
                        "register writes). Diagnostic lever for isolating "
                        "DMA-engine drops from silicon-level drops.")
    args = p.parse_args()

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
        timing_overrides["xport_mode"] = XPORT_PIO if args.xport == "pio" else XPORT_DMA

    ocd = OpenOCD(speed=args.speed)
    try:
        client = FlashClientV2(ocd)
        client.load_driver()
        if timing_overrides:
            client.set_timings(**timing_overrides)
        if args.show_timings:
            t = client.read_timings()
            for k, v in t.items():
                print(f"  {k} = {v}")
            return 0
        if args.mode == "flash-all":
            return flash_all(client, args)
        if args.mode == "autonomous":
            if not client.bp_clear():
                raise SystemExit(1)
            pattern = bytes(((i * 37) & 0xFF) if (i % 257) else 0
                            for i in range(SECTOR_SIZE))
            t0 = time.monotonic()
            ok = client.flash_sector(args.test_sector, pattern)
            dt = time.monotonic() - t0
            print(f"autonomous flash_sector: {dt:.2f}s wall, "
                  f"{'OK' if ok else 'FAIL'}")
            return 0 if ok else 1
        if args.mode == "block-sector":
            # V2_CMD_FLASH_BLOCK with a single-sector (4 KB) payload —
            # exercises the auto-pick logic on its SE branch.
            if not client.bp_clear():
                raise SystemExit(1)
            pattern = bytes(((i * 37) & 0xFF) if (i % 257) else 0
                            for i in range(SECTOR_SIZE))
            t0 = time.monotonic()
            ok = client.flash_block(args.test_sector, pattern)
            dt = time.monotonic() - t0
            print(f"flash_block(4 KB): {dt:.2f}s wall, "
                  f"{'OK' if ok else 'FAIL'}")
            return 0 if ok else 1
        if args.mode == "block-64k":
            # FLASH_BLOCK with a 64 KB payload — auto-pick → BE64.
            # --test-sector must be 64 KB-aligned and pointing at an
            # expendable region (0x90000+ is all-0xFF in the patched
            # image, safe).
            if args.test_sector & 0xFFFF:
                raise SystemExit("--test-sector must be 64 KB-aligned for block-64k")
            if not client.bp_clear():
                raise SystemExit(1)
            pattern = bytes(((i * 37) & 0xFF) if (i % 257) else 0
                            for i in range(0x10000))
            t0 = time.monotonic()
            ok = client.flash_block(args.test_sector, pattern)
            dt = time.monotonic() - t0
            print(f"flash_block(64 KB): {dt:.2f}s wall, "
                  f"{'OK' if ok else 'FAIL'}")
            return 0 if ok else 1
        if args.mode == "erase-only":
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
        return 0 if ok else 1
    finally:
        ocd.close()


if __name__ == "__main__":
    raise SystemExit(main())
