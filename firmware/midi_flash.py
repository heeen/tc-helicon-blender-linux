#!/usr/bin/env python3
"""MIDI SysEx flash host tool for the patched DICE3 Blender firmware.

Speaks the protocol documented in firmware/midi_flash_protocol.md against
the SysEx filter hook installed by firmware/patch/hooks.py at
midi_sysex_cb @ 0x2A34 (manufacturer ID 0x7D).

Transport: raw USB bulk EP3 via pyusb (interface 4 = MIDIStreaming).
This bypasses ALSA/seq layers that often choke on the larger SysEx
frames our protocol uses (~300+ bytes).

Subcommands:
    info                              — INFO opcode, prints JEDEC etc.
    read   <addr> <len>               — READ, dumps to stdout/file
    hash   <addr> <len>               — HASH_RANGE, prints whole + per-sector CRCs
    erase  <addr> <count>             — ERASE_SECTOR
    write  <addr> <file>              — WRITE_AAI a small region (one frame)
    update --ref <image>              — full sector-diff update (recommended)
    reboot [--mode N]                 — REBOOT op

Sector-diff update reuses firmware/sector_diff.py — same code path the v2
mailbox driver has been using since 2026-04-30.
"""

from __future__ import annotations

import argparse
import struct
import sys
import time
from pathlib import Path

import usb.core
import usb.util

# Import shared sector-diff after path setup so this script runs from
# anywhere via `python3 firmware/midi_flash.py`.
HERE = Path(__file__).resolve().parent
sys.path.insert(0, str(HERE))
from sector_diff import (
    SECTOR_SIZE, DEFAULT_SECTOR_BUF_ADDR,
    zlib_rom_crc32, batch_diff, filter_ops_by_diff,
)


# ── Device defaults (TC Helicon Blender) ─────────────────────────────────

DEFAULT_VID = 0x1220
DEFAULT_PID = 0x8FE1
DEFAULT_IFACE = 4         # MIDIStreaming interface
DEFAULT_EP_OUT = 0x03     # bulk OUT (host→device, MIDI IN jack on device)
DEFAULT_EP_IN  = 0x82     # bulk IN  (device→host, MIDI OUT jack on device)

# Protocol constants — keep in sync with firmware/midi_flash_protocol.md
MFR_ID = 0x7D

OP_INFO          = 0x01
OP_READ          = 0x02   # SPI flash read (sst25xx_fast_read)
OP_HASH_RANGE    = 0x03
OP_ERASE_SECTOR  = 0x04
OP_WRITE_AAI     = 0x05
OP_REBOOT        = 0x06
OP_MEM_READ      = 0x07   # CPU-memory memcpy (used to drain SRAM sector_buf)

REPLY_OK_BIT  = 0x40
REPLY_ERR_BIT = 0x80

HASH_FLAG_PER_SECTOR = 0x01

ERR_NAMES = {
    0xDEAD0001: "ERR_GOLDEN_PROTECT",
    0xDEAD0002: "ERR_BAD_BODY",
    0xDEAD0003: "ERR_ERASE_FAIL",
    0xDEAD0004: "ERR_AAI_FAIL",
    0xDEAD0005: "ERR_BAD_CRC",
    0xDEAD0006: "ERR_TRUNCATED",
    0xDEAD00FF: "ERR_UNKNOWN_OPCODE",
}


# ── 7-of-8 pack/unpack — Roland MMA standard ─────────────────────────────

def pack78(raw: bytes) -> bytes:
    """Pack `raw` into 7-bit MIDI payload. Each 7-byte group becomes
    1 mask byte + 7 (low-7-bit) bytes. Final group may be short."""
    out = bytearray()
    i = 0
    n = len(raw)
    while i < n:
        group = raw[i:i + 7]
        mask = 0
        for b, byte in enumerate(group):
            if byte & 0x80:
                mask |= (1 << b)
        out.append(mask)
        for byte in group:
            out.append(byte & 0x7F)
        i += 7
    return bytes(out)


def unpack78(packed: bytes) -> bytes:
    """Inverse of pack78. Tolerates a short trailing group."""
    out = bytearray()
    i = 0
    n = len(packed)
    while i < n:
        mask = packed[i]; i += 1
        for b in range(7):
            if i >= n:
                break
            byte = packed[i] & 0x7F
            if mask & (1 << b):
                byte |= 0x80
            out.append(byte)
            i += 1
    return bytes(out)


def crc8(buf: bytes) -> int:
    """CRC-8/ATM (poly 0x07, init 0x00, no XOR-out)."""
    c = 0
    for byte in buf:
        c ^= byte
        for _ in range(8):
            c = ((c << 1) ^ 0x07) & 0xFF if c & 0x80 else (c << 1) & 0xFF
    return c


# ── USB MIDI 1.0 packet framing ──────────────────────────────────────────
#
# Each USB packet is 4 bytes: [cable_num<<4 | CIN, b1, b2, b3].
# CIN values relevant to SysEx:
#   0x4 — SysEx starts or continues, 3 bytes
#   0x5 — SysEx ends with 1 byte
#   0x6 — SysEx ends with 2 bytes
#   0x7 — SysEx ends with 3 bytes
#
# We pack a complete SysEx (F0 ... F7) into 4-byte USB MIDI packets
# matching this convention. Cable number 0.

def sysex_to_usbmidi(sysex: bytes, cable: int = 0) -> bytes:
    """Wrap a complete F0..F7 SysEx into USB-MIDI 1.0 packets (4 B each)."""
    if not sysex or sysex[0] != 0xF0 or sysex[-1] != 0xF7:
        raise ValueError("sysex must start with F0 and end with F7")
    out = bytearray()
    n = len(sysex)
    i = 0
    cable_hi = (cable & 0x0F) << 4
    while n - i > 3:
        out.append(cable_hi | 0x4)        # SysEx start/continue, 3 B
        out.append(sysex[i])
        out.append(sysex[i + 1])
        out.append(sysex[i + 2])
        i += 3
    rem = n - i
    if rem == 1:
        out.append(cable_hi | 0x5)
        out.append(sysex[i])
        out.append(0)
        out.append(0)
    elif rem == 2:
        out.append(cable_hi | 0x6)
        out.append(sysex[i])
        out.append(sysex[i + 1])
        out.append(0)
    elif rem == 3:
        out.append(cable_hi | 0x7)
        out.append(sysex[i])
        out.append(sysex[i + 1])
        out.append(sysex[i + 2])
    else:
        raise AssertionError("unreachable: rem=" + str(rem))
    return bytes(out)


def usbmidi_to_sysex(packets: bytes) -> list[bytes]:
    """Reassemble complete SysEx messages from a stream of USB-MIDI
    packets. Returns a list of complete F0..F7 messages found.
    Tolerates leading/trailing junk."""
    msgs: list[bytes] = []
    cur = bytearray()
    in_sysex = False
    for off in range(0, len(packets), 4):
        if off + 4 > len(packets):
            break
        cin = packets[off] & 0x0F
        b1, b2, b3 = packets[off + 1], packets[off + 2], packets[off + 3]
        if cin == 0x4:
            if not in_sysex and b1 != 0xF0:
                continue                  # garbage
            if b1 == 0xF0:
                cur = bytearray()
                in_sysex = True
            cur.append(b1); cur.append(b2); cur.append(b3)
        elif cin in (0x5, 0x6, 0x7):
            if not in_sysex and b1 != 0xF0:
                continue
            if b1 == 0xF0:
                cur = bytearray()
                in_sysex = True
            cur.append(b1)
            if cin >= 0x6:
                cur.append(b2)
            if cin >= 0x7:
                cur.append(b3)
            # If the user sent the whole SysEx in one short packet
            # (rare) the F7 is somewhere in cur. End normally.
            if cur and cur[-1] == 0xF7:
                msgs.append(bytes(cur))
                cur = bytearray()
                in_sysex = False
    return msgs


# ── Frame build / parse ──────────────────────────────────────────────────

def build_frame(op: int, seq: int, raw_payload: bytes) -> bytes:
    packed = pack78(raw_payload)
    inner = bytes([op & 0x7F, seq & 0x7F]) + packed
    crc = crc8(inner) & 0x7F
    return b"\xF0" + bytes([MFR_ID]) + inner + bytes([crc]) + b"\xF7"


def parse_frame(sysex: bytes) -> tuple[int, int, bytes]:
    """Returns (op_byte_with_flag, seq, raw_payload). Raises on bad CRC
    or short frame. Skips frames whose manufacturer ID isn't 0x7D."""
    if len(sysex) < 6 or sysex[0] != 0xF0 or sysex[-1] != 0xF7:
        raise ValueError("not a SysEx frame")
    if sysex[1] != MFR_ID:
        raise ValueError(f"wrong manuf ID: {sysex[1]:#x}")
    op = sysex[2]
    seq = sysex[3]
    inner = sysex[4:-2]
    sent_crc = sysex[-2] & 0x7F
    got_crc = crc8(bytes([op, seq]) + inner) & 0x7F
    if sent_crc != got_crc:
        raise ValueError(f"CRC mismatch: sent {sent_crc:#x}, got {got_crc:#x}")
    return op, seq, unpack78(inner)


# ── USB transport ────────────────────────────────────────────────────────

class MidiUsb:
    def __init__(self, vid=DEFAULT_VID, pid=DEFAULT_PID,
                 iface=DEFAULT_IFACE, ep_out=DEFAULT_EP_OUT, ep_in=DEFAULT_EP_IN,
                 timeout_ms=2000):
        dev = usb.core.find(idVendor=vid, idProduct=pid)
        if dev is None:
            raise SystemExit(f"USB device {vid:04x}:{pid:04x} not found")
        try:
            if dev.is_kernel_driver_active(iface):
                dev.detach_kernel_driver(iface)
        except (NotImplementedError, ValueError, usb.core.USBError):
            pass
        usb.util.claim_interface(dev, iface)
        self.dev = dev
        self.iface = iface
        self.ep_out = ep_out
        self.ep_in = ep_in
        self.timeout_ms = timeout_ms
        self._seq = 0

    def close(self):
        try:
            usb.util.release_interface(self.dev, self.iface)
        except Exception:
            pass

    def next_seq(self) -> int:
        self._seq = (self._seq + 1) & 0x7F
        if self._seq == 0:
            self._seq = 1
        return self._seq

    def send_sysex(self, sysex: bytes) -> int:
        """Send a complete F0..F7 message via bulk OUT EP3, returns
        bytes written."""
        usb_packets = sysex_to_usbmidi(sysex)
        return self.dev.write(self.ep_out, usb_packets, timeout=self.timeout_ms)

    def recv_sysex(self, expect_seq: int, deadline_s: float = 5.0) -> tuple[int, int, bytes]:
        """Read USB-MIDI packets until we reassemble a SysEx whose seq
        matches expect_seq. Returns (op, seq, raw_payload)."""
        end = time.monotonic() + deadline_s
        leftover = bytearray()
        while time.monotonic() < end:
            try:
                data = self.dev.read(self.ep_in, 512, timeout=200)
            except usb.core.USBError as e:
                if "timeout" in str(e).lower() or "timed out" in str(e).lower():
                    continue
                raise
            leftover.extend(bytes(data))
            # Drain whatever complete SysEx messages have accumulated.
            msgs = usbmidi_to_sysex(bytes(leftover))
            if msgs:
                # Find the first complete frame for our seq.
                for m in msgs:
                    try:
                        op, seq, payload = parse_frame(m)
                    except ValueError:
                        continue
                    if seq == expect_seq:
                        return op, seq, payload
                # No matching frame yet — clear accumulator and keep reading.
                leftover = bytearray()
        raise TimeoutError(f"no reply for seq={expect_seq}")

    def request(self, op: int, raw_payload: bytes,
                timeout_s: float = 5.0) -> tuple[int, bytes]:
        """Send op + payload, wait for matching reply. Returns (reply_op,
        raw_payload). reply_op has REPLY_OK_BIT or REPLY_ERR_BIT set."""
        seq = self.next_seq()
        frame = build_frame(op, seq, raw_payload)
        self.send_sysex(frame)
        return self.recv_sysex(seq, timeout_s)[0], self.recv_last_payload  # type: ignore  # noqa: F821

    # Simpler request that returns (reply_op, payload):
    def cmd(self, op: int, raw_payload: bytes,
            timeout_s: float = 5.0) -> tuple[int, bytes]:
        seq = self.next_seq()
        frame = build_frame(op, seq, raw_payload)
        self.send_sysex(frame)
        rop, _, rpl = self.recv_sysex(seq, timeout_s)
        return rop, rpl


# ── Flash client (implements sector_diff.Client) ─────────────────────────

class MidiFlashClient:
    def __init__(self, usb: MidiUsb):
        self.usb = usb

    def info(self) -> dict:
        rop, body = self.usb.cmd(OP_INFO, b"")
        if rop & REPLY_ERR_BIT:
            raise RuntimeError(f"INFO failed: {self._err(body)}")
        if len(body) < 16:
            raise RuntimeError(f"INFO short: {body!r}")
        jedec, sector, total, version = struct.unpack_from("<IIII", body, 0)
        return {
            "jedec": jedec, "sector_size": sector,
            "flash_size": total, "version": version,
        }

    def read(self, addr: int, length: int) -> bytes:
        """Read SPI flash via sst25xx_fast_read (addr is a SPI address)."""
        rop, body = self.usb.cmd(OP_READ, struct.pack("<IH", addr, length & 0xFFFF))
        if rop & REPLY_ERR_BIT:
            raise RuntimeError(f"READ {addr:#x}+{length}: {self._err(body)}")
        return body[:length]

    def mem_read(self, addr: int, length: int) -> bytes:
        """Read CPU memory via memcpy (addr is SRAM/MMIO address)."""
        rop, body = self.usb.cmd(OP_MEM_READ, struct.pack("<IH", addr, length & 0xFFFF))
        if rop & REPLY_ERR_BIT:
            raise RuntimeError(f"MEM_READ {addr:#x}+{length}: {self._err(body)}")
        return body[:length]

    def hash_range(self, addr: int, length: int,
                   sectorbuf: int = DEFAULT_SECTOR_BUF_ADDR,
                   per_sector: bool = True) -> tuple[int, int]:
        flags = HASH_FLAG_PER_SECTOR if per_sector else 0
        body = struct.pack("<IIIB", addr, length, sectorbuf, flags)
        rop, rpl = self.usb.cmd(OP_HASH_RANGE, body, timeout_s=30.0)
        if rop & REPLY_ERR_BIT:
            raise RuntimeError(f"HASH_RANGE: {self._err(rpl)}")
        whole, count = struct.unpack_from("<IH", rpl, 0)
        return whole, count

    def erase_sector(self, addr: int, count: int = 1) -> None:
        rop, body = self.usb.cmd(OP_ERASE_SECTOR,
                                 struct.pack("<IH", addr, count),
                                 timeout_s=30.0)
        if rop & REPLY_ERR_BIT:
            raise RuntimeError(f"ERASE {addr:#x}×{count}: {self._err(body)}")

    # Max raw data per WRITE_AAI frame. Request is addr(4)+data(N).
    # MFD2+ has the midi_rx_dispatch hook that bypasses the stock
    # parser's 23-byte SysEx buffer, so the binding constraint is now
    # the device-side accumulator (1 KB private buffer) and the reply
    # ringbuf (192 raw worst case for READ/MEM_READ replies). For
    # WRITE_AAI the reply is just 4 bytes so request size dominates.
    # 192 raw → 224 packed → 230 wire frame; comfortable in 1 KB.
    WRITE_AAI_CHUNK = 192

    def write_aai(self, addr: int, data: bytes) -> None:
        if len(data) > self.WRITE_AAI_CHUNK:
            raise ValueError(f"WRITE_AAI payload {len(data)} > {self.WRITE_AAI_CHUNK}")
        rop, body = self.usb.cmd(OP_WRITE_AAI,
                                 struct.pack("<I", addr) + data,
                                 timeout_s=30.0)
        if rop & REPLY_ERR_BIT:
            raise RuntimeError(f"WRITE {addr:#x}: {self._err(body)}")

    def reboot(self, mode: int = 0) -> None:
        try:
            self.usb.cmd(OP_REBOOT, bytes([mode]), timeout_s=2.0)
        except TimeoutError:
            pass  # device resets before reply, expected

    # ── sector_diff.Client interface ──────────────────────────────

    def device_crc32_batch(self, addr: int, length: int,
                           sector_buf_addr: int = DEFAULT_SECTOR_BUF_ADDR
                           ) -> tuple[int, list[int]]:
        whole, count = self.hash_range(addr, length, sector_buf_addr,
                                       per_sector=True)
        # Drain per-sector CRCs from SRAM sector_buf via MEM_READ
        # (NOT flash read — sector_buf is SRAM).
        total_bytes = count * 4
        sec_crcs: list[int] = []
        off = 0
        while off < total_bytes:
            chunk = min(192, total_bytes - off)
            data = self.mem_read(sector_buf_addr + off, chunk)
            for i in range(0, len(data), 4):
                if i + 4 <= len(data):
                    sec_crcs.append(struct.unpack_from("<I", data, i)[0])
            off += chunk
        return whole, sec_crcs

    @staticmethod
    def _err(body: bytes) -> str:
        if len(body) >= 4:
            code = struct.unpack_from("<I", body, 0)[0]
            return f"{ERR_NAMES.get(code, hex(code))} ({code:#010x})"
        return f"<short err body: {body!r}>"


# ── Subcommands ──────────────────────────────────────────────────────────

def cmd_info(args, fc: MidiFlashClient) -> int:
    info = fc.info()
    print(f"JEDEC ID:    {info['jedec']:#08x}")
    print(f"Sector size: {info['sector_size']} bytes "
          f"({info['sector_size'] // 1024} KB)")
    print(f"Flash size:  {info['flash_size']} bytes "
          f"({info['flash_size'] // 1024} KB)")
    print(f"Version:     {info['version']:#010x}")
    return 0


def cmd_read(args, fc: MidiFlashClient) -> int:
    n = args.length
    out = bytearray()
    chunk = 192      # match device-side MEM_READ/READ cap (TX ringbuf budget)
    addr = args.addr
    while n > 0:
        c = min(chunk, n)
        out.extend(fc.read(addr, c))
        addr += c
        n -= c
    if args.out:
        Path(args.out).write_bytes(bytes(out))
        print(f"wrote {len(out)} bytes → {args.out}")
    else:
        for off in range(0, len(out), 16):
            row = out[off:off + 16]
            hexs = " ".join(f"{b:02x}" for b in row)
            print(f"  {args.addr + off:#08x}  {hexs}")
    return 0


def cmd_hash(args, fc: MidiFlashClient) -> int:
    whole, count = fc.hash_range(args.addr, args.length, per_sector=True)
    print(f"whole CRC: {whole:#010x}")
    print(f"sectors:   {count}")
    if args.show_sectors:
        # Drain per-sector CRCs via the same path as Client.
        whole2, crcs = fc.device_crc32_batch(args.addr, args.length)
        for i, c in enumerate(crcs):
            print(f"  sector {i:3d} (0x{args.addr + i * SECTOR_SIZE:06x}): "
                  f"{c:#010x}")
    return 0


def cmd_erase(args, fc: MidiFlashClient) -> int:
    fc.erase_sector(args.addr, args.count)
    print(f"erased {args.count} sectors @ {args.addr:#x}")
    return 0


def cmd_write(args, fc: MidiFlashClient) -> int:
    data = Path(args.file).read_bytes()
    if len(data) > 256:
        raise SystemExit("WRITE: max 256 bytes per frame; use 'update' for "
                         "multi-frame write")
    fc.write_aai(args.addr, data)
    print(f"wrote {len(data)} bytes @ {args.addr:#x}")
    return 0


def cmd_update(args, fc: MidiFlashClient) -> int:
    ref = Path(args.ref).read_bytes()
    print(f"Reference: {args.ref} ({len(ref)} bytes, "
          f"{len(ref) // SECTOR_SIZE} sectors)")
    match, diff = batch_diff(fc, ref)
    if match:
        print("Already up to date.")
        return 0
    if not diff:
        print("Whole-image mismatch but no per-sector diff — bug?")
        return 1
    print(f"Need to flash {len(diff)} sectors")

    flashed = 0
    for sector_addr in sorted(diff):
        sector = ref[sector_addr:sector_addr + SECTOR_SIZE]
        fc.erase_sector(sector_addr, 1)
        # AAI-write in WRITE_AAI_CHUNK B chunks (one frame each).
        cs = fc.WRITE_AAI_CHUNK
        for off in range(0, len(sector), cs):
            chunk = sector[off:off + cs]
            fc.write_aai(sector_addr + off, chunk)
        flashed += 1
        if flashed % 16 == 0:
            print(f"  {flashed}/{len(diff)} sectors...")

    # Final verify via HASH_RANGE.
    final_match, final_diff = batch_diff(fc, ref)
    if final_match:
        print(f"Done. {flashed} sectors flashed; whole-image CRC matches.")
        return 0
    print(f"Verify FAILED: {len(final_diff)} sectors still differ")
    return 2


def cmd_reboot(args, fc: MidiFlashClient) -> int:
    fc.reboot(args.mode)
    print("reboot requested")
    return 0


# ── Main ─────────────────────────────────────────────────────────────────

def main(argv: list[str] | None = None) -> int:
    p = argparse.ArgumentParser(description=__doc__,
                                formatter_class=argparse.RawDescriptionHelpFormatter)
    p.add_argument("--vid", type=lambda x: int(x, 0), default=DEFAULT_VID)
    p.add_argument("--pid", type=lambda x: int(x, 0), default=DEFAULT_PID)
    sub = p.add_subparsers(dest="cmd", required=True)

    p_info = sub.add_parser("info"); p_info.set_defaults(fn=cmd_info)

    p_read = sub.add_parser("read")
    p_read.add_argument("addr", type=lambda x: int(x, 0))
    p_read.add_argument("length", type=lambda x: int(x, 0))
    p_read.add_argument("--out", help="write to FILE instead of hex-dump")
    p_read.set_defaults(fn=cmd_read)

    p_hash = sub.add_parser("hash")
    p_hash.add_argument("addr", type=lambda x: int(x, 0))
    p_hash.add_argument("length", type=lambda x: int(x, 0))
    p_hash.add_argument("--show-sectors", action="store_true")
    p_hash.set_defaults(fn=cmd_hash)

    p_erase = sub.add_parser("erase")
    p_erase.add_argument("addr", type=lambda x: int(x, 0))
    p_erase.add_argument("count", type=int, nargs="?", default=1)
    p_erase.set_defaults(fn=cmd_erase)

    p_write = sub.add_parser("write")
    p_write.add_argument("addr", type=lambda x: int(x, 0))
    p_write.add_argument("file")
    p_write.set_defaults(fn=cmd_write)

    p_update = sub.add_parser("update")
    p_update.add_argument("--ref", required=True)
    p_update.set_defaults(fn=cmd_update)

    p_reboot = sub.add_parser("reboot")
    p_reboot.add_argument("--mode", type=int, default=0)
    p_reboot.set_defaults(fn=cmd_reboot)

    args = p.parse_args(argv)

    usb_ = MidiUsb(vid=args.vid, pid=args.pid)
    fc = MidiFlashClient(usb_)
    try:
        return args.fn(args, fc)
    finally:
        usb_.close()


if __name__ == "__main__":
    sys.exit(main())
