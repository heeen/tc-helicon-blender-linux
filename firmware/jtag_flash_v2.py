#!/usr/bin/env python3 -u
"""Host-side client for the v2 SRAM flash driver.

Speaks the mailbox protocol defined in sram_flash_mailbox_v2.h:
  * Single-writer-per-field (closes the v1 init race).
  * Phase-based progress polling (no halt required — mdw while running works).
  * Structured log ring dump on ERROR for postmortem without guessing.

CLI:
    python3 firmware/jtag_flash_v2.py --ref firmware/blender_spi_patched.bin \\
        --speed 1000 --test-sector 0x3f000
    python3 firmware/jtag_flash_v2.py --mode flash-all --nreset
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
# Hardware nRST enabled — required for `reset halt` to pulse the target.
OPENOCD_CFG_SRST = ROOT / "jtag" / "miolink-dice3-openocd-srst.cfg"
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
    "byte_prog_offset",
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
    "byte_prog_offset":           0,
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
        # Verify-miss capture (zeroed unless err_code == V2_ERR_VERIFY_MISMATCH).
        miss_off     = self.o.mdw(MBOX_ADDR + O_MISS_OFFSET)
        miss_got     = self.o.mdw(MBOX_ADDR + O_MISS_GOT)
        miss_exp     = self.o.mdw(MBOX_ADDR + O_MISS_EXP)
        miss_spi_st  = self.o.mdw(MBOX_ADDR + O_MISS_SPI_ST)
        miss_dma_ist = self.o.mdw(MBOX_ADDR + O_MISS_DMA_IST)
        miss_reads   = self.o.mdw(MBOX_ADDR + O_MISS_READS)
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
                print(f"  miss@+{miss_off}  got={got4.hex()} exp={exp4.hex()}"
                      f"  spi_stat=0x{miss_spi_st:x} dma_istat=0x{miss_dma_ist:x}"
                      f"  reads={miss_reads}{shift_hint}")
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
                   block_threshold_32k=6):
    """Pick flash ops covering every 4 KB sector in [0, image_size).

    For each 64 KB block:
      - If every byte in reference is 0xFF → single ERASE_64K (fastest)
      - Else if ≥ block_threshold_64k non-FF sectors → FLASH_BLOCK(64K)
      - Else split into two 32 KB halves; each half similarly classified
        (all-FF → ERASE_32K; dense → FLASH_BLOCK(32K); sparse → per-sector)

    Per-sector ops likewise split into ("erase", ...) for all-FF sectors
    and ("sector", ...) for content. Erase ops leave flash at 0xFF, which
    is the correct state for any reference byte that's 0xFF.

    Returns list of (kind, addr, span) tuples. kind ∈ {block_64k,
    block_32k, sector, erase_64k, erase_32k, erase}.
    """
    def is_ff(lo, hi):
        return all(b == 0xFF for b in ref[lo:hi])

    ops = []
    for blk_addr in range(0, image_size, BLOCK_64K):
        blk_hi = blk_addr + BLOCK_64K
        if is_ff(blk_addr, blk_hi):
            ops.append(("erase_64k", blk_addr, BLOCK_64K))
            continue
        # Count non-empty sectors in this block
        sec_list = [blk_addr + i*SECTOR_SIZE
                    for i in range(BLOCK_64K // SECTOR_SIZE)
                    if not is_ff(blk_addr + i*SECTOR_SIZE,
                                 blk_addr + (i+1)*SECTOR_SIZE)]
        if len(sec_list) >= block_threshold_64k:
            ops.append(("block_64k", blk_addr, BLOCK_64K))
            continue
        # Split into two 32 KB halves
        for half_addr in (blk_addr, blk_addr + BLOCK_32K):
            half_hi = half_addr + BLOCK_32K
            if is_ff(half_addr, half_hi):
                ops.append(("erase_32k", half_addr, BLOCK_32K))
                continue
            half_secs = [s for s in sec_list
                         if half_addr <= s < half_hi]
            if len(half_secs) >= block_threshold_32k:
                ops.append(("block_32k", half_addr, BLOCK_32K))
            else:
                for sec_addr in range(half_addr, half_hi, SECTOR_SIZE):
                    if is_ff(sec_addr, sec_addr + SECTOR_SIZE):
                        ops.append(("erase", sec_addr, SECTOR_SIZE))
                    else:
                        ops.append(("sector", sec_addr, SECTOR_SIZE))
    return ops


def flash_all(client, args):
    """Flash every non-empty sector of --ref.

    Prefers one autonomous block op per 64 KB region when the block is
    mostly full (≥12 sectors non-empty), else falls back to per-sector
    ops. Saves round-trips vs the old erase+program+verify loop."""
    ref = Path(args.ref).read_bytes()
    print(f"Reference: {args.ref} ({len(ref)} bytes)")

    if args.sectors:
        # Explicit sector list — treat each as a separate op (content or
        # erase decided per-sector).
        sectors = [int(s.strip(), 0) for s in args.sectors.split(",") if s.strip()]
        ops = []
        for addr in sectors:
            if all(b == 0xFF for b in ref[addr:addr + SECTOR_SIZE]):
                ops.append(("erase", addr, SECTOR_SIZE))
            else:
                ops.append(("sector", addr, SECTOR_SIZE))
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
                # Host-side re-read to confirm flash actually has the pattern.
                data = client.read(target, length)
                if data is None:
                    print(f"  iter {i}: readback failed")
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
                for s in (1, 2, 3, 4):
                    if got4[0] == exp4[s] if s < 4 else False:
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
        for s in (1, 2, 3, 4):
            if first + s < length and got4[0] == expected[first + s]:
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
                                       "block-sector", "block-64k",
                                       "byte-test", "byte-only", "read-id",
                                       "shift-repro"),
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
    p.add_argument("--byte-addr", type=lambda x: int(x, 0), default=0x3F020,
                   help="byte-test: target SPI address for the single-byte write")
    p.add_argument("--byte-val", type=lambda x: int(x, 0), default=0x5A,
                   help="byte-test: byte value to program")
    p.add_argument("--xport", choices=("dma", "pio"), default=None,
                   help="Select AAI hot-loop transport backend on the "
                        "driver (dma = current; pio = byte-by-byte DATA "
                        "register writes). Diagnostic lever for isolating "
                        "DMA-engine drops from silicon-level drops.")
    p.add_argument("--nreset", action="store_true",
                   help="Pulse hardware nRESET (reset halt) after OpenOCD "
                        "connects, before loading the SRAM driver. Uses "
                        f"{OPENOCD_CFG_SRST.name} (SRST enabled); use when "
                        "the TAP is wedged or the driver never reaches READY.")
    p.add_argument("--openocd-cfg", type=str, default="",
                   help="OpenOCD config file (default: miolink-dice3-openocd.cfg, "
                        "or -srst variant when --nreset is set)")
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
            return 0 if ok else 1
        if args.mode == "byte-only":
            # Minimal: issue ONE BYTE_PRG at --byte-addr with --byte-val.
            # No bp_clear, no erase, no readback. For logic-analyzer probing.
            # On the wire you'll see: WREN (0x06) + BYTE_PRG (0x02 + 3B addr
            # + 1B data) + RDSR poll loop (0x05 + status until !BUSY).
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
            return 0
        if args.mode == "byte-test":
            # Isolate BYTE_PROGRAM landing offset: erase the containing
            # sector, write one byte at --byte-addr, read the sector
            # back, report which offsets ended up non-0xFF. Confirms
            # whether the off-by-4 symptom survived the IRQ refactor.
            sector = args.byte_addr & ~0xFFF
            offset_in_sector = args.byte_addr & 0xFFF
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
            return 0 if ok else 1
        if args.mode == "shift-repro":
            return _shift_repro(client, args)
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
