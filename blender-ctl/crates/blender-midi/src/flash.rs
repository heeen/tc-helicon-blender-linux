//! USB-MIDI SysEx flash client for the patched DICE3 Blender firmware.
//!
//! Wire-compatible with `firmware/midi_flash.py` (manuf 0x7D, opcodes
//! 0x01–0x07, Roland 7-of-8 packing, CRC-8/ATM).  Bypasses ALSA/seq by
//! talking directly to the MIDIStreaming bulk endpoints (EP 0x03 OUT /
//! EP 0x82 IN) over `rusb` — same approach as the Python tool, since
//! ALSA seq commonly chokes on the larger SysEx frames the protocol
//! uses.
//!
//! The on-device dispatcher version ships in INFO (`MFD1`/`MFD2`/…):
//! MFD1 caps WRITE_AAI raw payload at 11 (24-byte wire frame, stock
//! parser limit); MFD2 raises it to ~880 via a custom byte-stream hook
//! at `midi_rx_poll`.  This client picks the chunk size at session
//! start based on the version.
//!
//! See `firmware/midi_flash_protocol.md` for the wire spec.

use std::time::{Duration, Instant};

use anyhow::{Context, Result, anyhow, bail};
use rusb::GlobalContext;

// ─── Device + protocol constants ──────────────────────────────────────────

pub const VENDOR_ID: u16 = 0x1220;
pub const PRODUCT_ID: u16 = 0x8FE1;

const IFACE_MIDI: u8 = 4;
const EP_OUT: u8 = 0x03;
const EP_IN: u8 = 0x82;

const MFR_ID: u8 = 0x7D;

pub const OP_INFO: u8 = 0x01;
pub const OP_READ: u8 = 0x02;
pub const OP_HASH_RANGE: u8 = 0x03;
pub const OP_ERASE_SECTOR: u8 = 0x04;
pub const OP_WRITE_AAI: u8 = 0x05;
pub const OP_REBOOT: u8 = 0x06;
pub const OP_MEM_READ: u8 = 0x07;

pub const REPLY_OK_BIT: u8 = 0x40;
pub const REPLY_ERR_BIT: u8 = 0x80;

const HASH_FLAG_PER_SECTOR: u8 = 0x01;

pub const SECTOR_SIZE: u32 = 0x1000;
pub const FLASH_SIZE: u32 = 0x200000;

/// Default SRAM scratch for HASH_RANGE per-sector CRC array.
/// Must NOT alias UNPACK_BUF (0x2B000) or PACK_BUF (0x2B200) used by
/// the device-side handler.  See firmware/sector_diff.py.
pub const DEFAULT_SECTOR_BUF_ADDR: u32 = 0x0002_B400;

pub const VERSION_MFD1: u32 = 0x4D464431;
pub const VERSION_MFD2: u32 = 0x4D464432;

const ERR_NAMES: &[(u32, &str)] = &[
    (0xDEAD_0001, "ERR_GOLDEN_PROTECT"),
    (0xDEAD_0002, "ERR_BAD_BODY"),
    (0xDEAD_0003, "ERR_ERASE_FAIL"),
    (0xDEAD_0004, "ERR_AAI_FAIL"),
    (0xDEAD_0005, "ERR_BAD_CRC"),
    (0xDEAD_0006, "ERR_TRUNCATED"),
    (0xDEAD_00FF, "ERR_UNKNOWN_OPCODE"),
];

fn err_name(code: u32) -> String {
    for (c, n) in ERR_NAMES {
        if *c == code {
            return format!("{n} ({code:#010x})");
        }
    }
    format!("{code:#010x}")
}

// ─── Roland 7-of-8 pack/unpack ───────────────────────────────────────────

fn pack78(raw: &[u8]) -> Vec<u8> {
    let mut out = Vec::with_capacity(raw.len() * 8 / 7 + 8);
    let mut i = 0;
    while i < raw.len() {
        let group = &raw[i..raw.len().min(i + 7)];
        let mut mask: u8 = 0;
        for (b, &byte) in group.iter().enumerate() {
            if byte & 0x80 != 0 {
                mask |= 1 << b;
            }
        }
        out.push(mask);
        for &byte in group {
            out.push(byte & 0x7F);
        }
        i += 7;
    }
    out
}

fn unpack78(packed: &[u8]) -> Vec<u8> {
    let mut out = Vec::with_capacity(packed.len());
    let mut i = 0;
    while i < packed.len() {
        let mask = packed[i];
        i += 1;
        for b in 0..7 {
            if i >= packed.len() {
                break;
            }
            let mut byte = packed[i] & 0x7F;
            if mask & (1 << b) != 0 {
                byte |= 0x80;
            }
            out.push(byte);
            i += 1;
        }
    }
    out
}

// ─── CRC-8/ATM (poly 0x07, init 0x00, no XOR-out) ────────────────────────

fn crc8(buf: &[u8]) -> u8 {
    let mut c: u8 = 0;
    for &b in buf {
        c ^= b;
        for _ in 0..8 {
            c = if c & 0x80 != 0 {
                (c << 1) ^ 0x07
            } else {
                c << 1
            };
        }
    }
    c
}

// ─── ROM CRC32 helper (matches device's bootloader routine at 0x20000DA4) ─
// poly 0xEDB88320 reflected, init=0, no final XOR.

pub fn rom_crc32(buf: &[u8]) -> u32 {
    let mut crc: u32 = 0;
    for &b in buf {
        crc ^= b as u32;
        for _ in 0..8 {
            crc = if crc & 1 != 0 {
                (crc >> 1) ^ 0xEDB8_8320
            } else {
                crc >> 1
            };
        }
    }
    crc
}

// ─── Frame build/parse ───────────────────────────────────────────────────

fn build_frame(op: u8, seq: u8, raw_payload: &[u8]) -> Vec<u8> {
    let packed = pack78(raw_payload);
    let mut inner = Vec::with_capacity(2 + packed.len());
    inner.push(op & 0x7F);
    inner.push(seq & 0x7F);
    inner.extend_from_slice(&packed);
    let crc = crc8(&inner) & 0x7F;

    let mut frame = Vec::with_capacity(inner.len() + 4);
    frame.push(0xF0);
    frame.push(MFR_ID);
    frame.extend_from_slice(&inner);
    frame.push(crc);
    frame.push(0xF7);
    frame
}

/// Returns (op_with_flag, seq, raw_payload).
fn parse_frame(sysex: &[u8]) -> Result<(u8, u8, Vec<u8>)> {
    if sysex.len() < 6 || sysex[0] != 0xF0 || *sysex.last().unwrap() != 0xF7 {
        bail!("not a SysEx frame");
    }
    if sysex[1] != MFR_ID {
        bail!("wrong manuf ID: 0x{:02x}", sysex[1]);
    }
    let op = sysex[2];
    let seq = sysex[3];
    let inner = &sysex[4..sysex.len() - 2];
    let sent_crc = sysex[sysex.len() - 2] & 0x7F;
    let mut crc_input = Vec::with_capacity(2 + inner.len());
    crc_input.push(op);
    crc_input.push(seq);
    crc_input.extend_from_slice(inner);
    let got_crc = crc8(&crc_input) & 0x7F;
    if sent_crc != got_crc {
        bail!("CRC mismatch: sent 0x{sent_crc:02x}, got 0x{got_crc:02x}");
    }
    Ok((op, seq, unpack78(inner)))
}

// ─── USB-MIDI 1.0 packet wrapping ────────────────────────────────────────

fn sysex_to_usbmidi(sysex: &[u8]) -> Vec<u8> {
    let mut out = Vec::with_capacity(sysex.len() * 4 / 3 + 4);
    let n = sysex.len();
    let mut i = 0;
    while n - i > 3 {
        out.extend_from_slice(&[0x04, sysex[i], sysex[i + 1], sysex[i + 2]]);
        i += 3;
    }
    let rem = n - i;
    match rem {
        1 => out.extend_from_slice(&[0x05, sysex[i], 0, 0]),
        2 => out.extend_from_slice(&[0x06, sysex[i], sysex[i + 1], 0]),
        3 => out.extend_from_slice(&[0x07, sysex[i], sysex[i + 1], sysex[i + 2]]),
        _ => unreachable!(),
    }
    out
}

fn usbmidi_to_sysex(packets: &[u8]) -> Vec<Vec<u8>> {
    let mut msgs: Vec<Vec<u8>> = Vec::new();
    let mut cur: Vec<u8> = Vec::new();
    let mut in_sysex = false;
    let mut off = 0;
    while off + 4 <= packets.len() {
        let cin = packets[off] & 0x0F;
        let b1 = packets[off + 1];
        let b2 = packets[off + 2];
        let b3 = packets[off + 3];
        match cin {
            0x4 => {
                if !in_sysex && b1 != 0xF0 {
                    off += 4;
                    continue;
                }
                if b1 == 0xF0 {
                    cur.clear();
                    in_sysex = true;
                }
                cur.push(b1);
                cur.push(b2);
                cur.push(b3);
            }
            0x5 | 0x6 | 0x7 => {
                if !in_sysex && b1 != 0xF0 {
                    off += 4;
                    continue;
                }
                if b1 == 0xF0 {
                    cur.clear();
                    in_sysex = true;
                }
                cur.push(b1);
                if cin >= 0x6 {
                    cur.push(b2);
                }
                if cin >= 0x7 {
                    cur.push(b3);
                }
                if cur.last() == Some(&0xF7) {
                    msgs.push(std::mem::take(&mut cur));
                    in_sysex = false;
                }
            }
            _ => {}
        }
        off += 4;
    }
    msgs
}

// ─── USB transport ───────────────────────────────────────────────────────

pub struct MidiFlash {
    handle: rusb::DeviceHandle<GlobalContext>,
    timeout: Duration,
    seq: u8,
}

impl MidiFlash {
    pub fn open() -> Result<Self> {
        let device = rusb::devices()
            .context("rusb::devices")?
            .iter()
            .find(|d| {
                d.device_descriptor()
                    .map(|desc| desc.vendor_id() == VENDOR_ID && desc.product_id() == PRODUCT_ID)
                    .unwrap_or(false)
            })
            .ok_or_else(|| anyhow!("Blender USB device {VENDOR_ID:04x}:{PRODUCT_ID:04x} not found"))?;

        let handle = device.open().context("open USB device")?;

        // Detach kernel driver from MIDI iface (ALSA's snd-usb-audio
        // claims interface 4 by default).
        match handle.kernel_driver_active(IFACE_MIDI) {
            Ok(true) => {
                handle.detach_kernel_driver(IFACE_MIDI).ok();
            }
            _ => {}
        }
        handle
            .claim_interface(IFACE_MIDI)
            .context("claim MIDI interface 4")?;

        // Sanity-check endpoints exist on the interface (lsusb confirms,
        // but kernel-side state can drift). We don't iterate descriptors
        // here — the bulk read/write below will surface a clear error if
        // the EP isn't present.

        Ok(Self {
            handle,
            timeout: Duration::from_secs(5),
            seq: 0,
        })
    }

    fn next_seq(&mut self) -> u8 {
        self.seq = (self.seq + 1) & 0x7F;
        if self.seq == 0 {
            self.seq = 1;
        }
        self.seq
    }

    fn send_sysex(&self, sysex: &[u8]) -> Result<usize> {
        let usb_packets = sysex_to_usbmidi(sysex);
        self.handle
            .write_bulk(EP_OUT, &usb_packets, self.timeout)
            .context("USB MIDI bulk OUT")
    }

    fn recv_sysex(&self, expect_seq: u8, deadline: Duration) -> Result<(u8, Vec<u8>)> {
        let end = Instant::now() + deadline;
        let mut buf = vec![0u8; 512];
        let mut leftover: Vec<u8> = Vec::new();
        while Instant::now() < end {
            match self
                .handle
                .read_bulk(EP_IN, &mut buf, Duration::from_millis(200))
            {
                Ok(n) => leftover.extend_from_slice(&buf[..n]),
                Err(rusb::Error::Timeout) => continue,
                Err(e) => return Err(anyhow!("USB MIDI bulk IN: {e}")),
            }
            // Drain any complete SysEx messages.
            let msgs = usbmidi_to_sysex(&leftover);
            if !msgs.is_empty() {
                for m in &msgs {
                    if let Ok((op, seq, payload)) = parse_frame(m) {
                        if seq == expect_seq {
                            return Ok((op, payload));
                        }
                    }
                }
                leftover.clear();
            }
        }
        Err(anyhow!("no reply for seq={expect_seq}"))
    }

    fn cmd(&mut self, op: u8, raw_payload: &[u8], timeout: Duration) -> Result<(u8, Vec<u8>)> {
        let seq = self.next_seq();
        let frame = build_frame(op, seq, raw_payload);
        self.send_sysex(&frame)?;
        self.recv_sysex(seq, timeout)
    }

    pub fn info(&mut self) -> Result<FlashInfo> {
        let (rop, body) = self.cmd(OP_INFO, &[], self.timeout)?;
        if rop & REPLY_ERR_BIT != 0 {
            bail!("INFO failed: {}", err_body(&body));
        }
        if body.len() < 16 {
            bail!("INFO short reply: {} bytes", body.len());
        }
        let jedec = u32::from_le_bytes(body[0..4].try_into().unwrap());
        let sector_size = u32::from_le_bytes(body[4..8].try_into().unwrap());
        let flash_size = u32::from_le_bytes(body[8..12].try_into().unwrap());
        let version = u32::from_le_bytes(body[12..16].try_into().unwrap());
        Ok(FlashInfo {
            jedec,
            sector_size,
            flash_size,
            version,
        })
    }

    /// Read SPI flash via stock sst25xx_fast_read.
    pub fn read(&mut self, addr: u32, len: u16) -> Result<Vec<u8>> {
        let mut req = Vec::with_capacity(6);
        req.extend_from_slice(&addr.to_le_bytes());
        req.extend_from_slice(&len.to_le_bytes());
        let (rop, body) = self.cmd(OP_READ, &req, self.timeout)?;
        if rop & REPLY_ERR_BIT != 0 {
            bail!("READ {addr:#x}+{len}: {}", err_body(&body));
        }
        let n = (len as usize).min(body.len());
        Ok(body[..n].to_vec())
    }

    /// Read CPU memory via memcpy (used to drain SRAM scratch buffers).
    pub fn mem_read(&mut self, addr: u32, len: u16) -> Result<Vec<u8>> {
        let mut req = Vec::with_capacity(6);
        req.extend_from_slice(&addr.to_le_bytes());
        req.extend_from_slice(&len.to_le_bytes());
        let (rop, body) = self.cmd(OP_MEM_READ, &req, self.timeout)?;
        if rop & REPLY_ERR_BIT != 0 {
            bail!("MEM_READ {addr:#x}+{len}: {}", err_body(&body));
        }
        let n = (len as usize).min(body.len());
        Ok(body[..n].to_vec())
    }

    pub fn hash_range(
        &mut self,
        addr: u32,
        length: u32,
        sectorbuf: u32,
        per_sector: bool,
    ) -> Result<(u32, u16)> {
        let mut req = Vec::with_capacity(13);
        req.extend_from_slice(&addr.to_le_bytes());
        req.extend_from_slice(&length.to_le_bytes());
        req.extend_from_slice(&sectorbuf.to_le_bytes());
        req.push(if per_sector { HASH_FLAG_PER_SECTOR } else { 0 });
        let (rop, body) = self.cmd(OP_HASH_RANGE, &req, Duration::from_secs(30))?;
        if rop & REPLY_ERR_BIT != 0 {
            bail!("HASH_RANGE: {}", err_body(&body));
        }
        if body.len() < 6 {
            bail!("HASH_RANGE short reply");
        }
        let whole = u32::from_le_bytes(body[0..4].try_into().unwrap());
        let count = u16::from_le_bytes(body[4..6].try_into().unwrap());
        Ok((whole, count))
    }

    pub fn erase_sector(&mut self, addr: u32, count: u16) -> Result<()> {
        let mut req = Vec::with_capacity(6);
        req.extend_from_slice(&addr.to_le_bytes());
        req.extend_from_slice(&count.to_le_bytes());
        let (rop, body) = self.cmd(OP_ERASE_SECTOR, &req, Duration::from_secs(30))?;
        if rop & REPLY_ERR_BIT != 0 {
            bail!("ERASE {addr:#x}×{count}: {}", err_body(&body));
        }
        Ok(())
    }

    /// Write a single AAI chunk. Caller must respect dispatcher's chunk
    /// budget (use `WriteAaiChunk::for_version`).
    pub fn write_aai(&mut self, addr: u32, data: &[u8]) -> Result<()> {
        let mut req = Vec::with_capacity(4 + data.len());
        req.extend_from_slice(&addr.to_le_bytes());
        req.extend_from_slice(data);
        let (rop, body) = self.cmd(OP_WRITE_AAI, &req, Duration::from_secs(30))?;
        if rop & REPLY_ERR_BIT != 0 {
            bail!("WRITE {addr:#x}+{}: {}", data.len(), err_body(&body));
        }
        Ok(())
    }

    /// Best-effort REBOOT — ignore the timeout since the device may
    /// reset before sending a reply.
    pub fn reboot(&mut self, mode: u8) -> Result<()> {
        let _ = self.cmd(OP_REBOOT, &[mode], Duration::from_secs(2));
        Ok(())
    }

    /// Drains per-sector CRCs from SRAM via repeated MEM_READ chunks.
    /// Used by sector-diff to compare host vs device sector-by-sector.
    pub fn device_crc32_batch(
        &mut self,
        addr: u32,
        length: u32,
        sector_buf_addr: u32,
    ) -> Result<(u32, Vec<u32>)> {
        let (whole, count) = self.hash_range(addr, length, sector_buf_addr, true)?;
        let total_bytes = (count as u32) * 4;
        let mut sec_crcs: Vec<u32> = Vec::with_capacity(count as usize);
        let mut off: u32 = 0;
        while off < total_bytes {
            let chunk = (total_bytes - off).min(192) as u16;
            let data = self.mem_read(sector_buf_addr + off, chunk)?;
            for i in (0..data.len()).step_by(4) {
                if i + 4 <= data.len() {
                    sec_crcs.push(u32::from_le_bytes(data[i..i + 4].try_into().unwrap()));
                }
            }
            off += chunk as u32;
        }
        Ok((whole, sec_crcs))
    }
}

#[derive(Clone, Copy, Debug)]
pub struct FlashInfo {
    pub jedec: u32,
    pub sector_size: u32,
    pub flash_size: u32,
    pub version: u32,
}

impl FlashInfo {
    /// Max raw bytes per WRITE_AAI call for this dispatcher version.
    /// MFD2+ has the byte-stream hook that bypasses the 23-byte parser
    /// buffer, so we can use the larger 192 B chunks. MFD1 must stay at
    /// 10 (even-aligned for AAI word pairs, ≤ 11 raw payload limit).
    pub fn write_aai_chunk(&self) -> usize {
        if self.version >= VERSION_MFD2 {
            192
        } else {
            10
        }
    }
}

impl std::fmt::Display for FlashInfo {
    fn fmt(&self, f: &mut std::fmt::Formatter<'_>) -> std::fmt::Result {
        // Tag is stored as ASCII chars 'M','F','D','1' packed into a LE
        // u32 (so bytes 0x31,0x44,0x46,0x4D = 0x4D464431).  Display in
        // big-endian char order so the user sees 'MFD1' not '1DFM'.
        let tag = self.version.to_be_bytes();
        let tag_str: String = tag
            .iter()
            .map(|&b| {
                if b.is_ascii_graphic() {
                    b as char
                } else {
                    '.'
                }
            })
            .collect();
        writeln!(f, "JEDEC ID:    {:#08x}", self.jedec)?;
        writeln!(
            f,
            "Sector size: {} bytes ({} KB)",
            self.sector_size,
            self.sector_size / 1024
        )?;
        writeln!(
            f,
            "Flash size:  {} bytes ({} KB)",
            self.flash_size,
            self.flash_size / 1024
        )?;
        write!(
            f,
            "Version:     {:#010x} '{}' (WRITE_AAI chunk = {} B)",
            self.version,
            tag_str,
            self.write_aai_chunk()
        )
    }
}

fn err_body(body: &[u8]) -> String {
    if body.len() >= 4 {
        let code = u32::from_le_bytes(body[..4].try_into().unwrap());
        err_name(code)
    } else {
        format!("<short err body: {} bytes>", body.len())
    }
}

// ─── Sector-diff strategy ────────────────────────────────────────────────
//
// Mirrors firmware/sector_diff.py: device sweep returns whole + per-sector
// CRCs, host computes the same locally, intersection identifies which
// 4 KB sectors actually need flashing.

pub struct SectorDiff {
    /// Sectors that differ between device and reference (start address
    /// relative to the diff range — caller adds the region base).
    pub diff: Vec<u32>,
    pub whole_local: u32,
    pub whole_device: u32,
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn pack78_roundtrip_at_critical_lengths() {
        // Verify pack/unpack roundtrip across boundaries that previously
        // exposed bugs in the Python reference (final-group handling,
        // empty input, exact multiple of 7).
        for &n in &[0usize, 1, 6, 7, 8, 13, 14, 15, 192, 256, 512] {
            let raw: Vec<u8> = (0..n).map(|i| (i as u8).wrapping_mul(17).wrapping_add(5)).collect();
            let packed = pack78(&raw);
            assert!(packed.iter().all(|b| *b <= 0x7F), "len={n}: non-MIDI byte in packed");
            let back = unpack78(&packed);
            assert_eq!(back, raw, "roundtrip at len={n}");
        }
    }

    #[test]
    fn pack78_high_bits_all_set() {
        let raw = [0xFFu8; 7];
        let packed = pack78(&raw);
        let mut expected = vec![0x7Fu8]; // mask: bits 0..6 set
        expected.extend(std::iter::repeat(0x7F).take(7));
        assert_eq!(packed, expected);
    }

    #[test]
    fn crc8_atm_known_vectors() {
        // CRC-8/ATM (poly 0x07) reference checks.
        assert_eq!(crc8(b""), 0x00);
        assert_eq!(crc8(b"\x00"), 0x00);
        assert_eq!(crc8(b"\x01"), 0x07);
        assert_eq!(crc8(b"123456789"), 0xF4);
    }

    #[test]
    fn build_then_parse_frame_roundtrip() {
        let payloads: &[&[u8]] = &[
            &[],
            &[0x00, 0x00, 0x04, 0x00, 0x00, 0x10],          // READ-ish
            &[0x00, 0x00, 0x04, 0x00, 0xB0, 0xFF, 0x00, 0x00, 0x00, 0xB0, 0x02, 0x00, 0x01],
            &[0xFF; 192],                                     // worst-case WRITE_AAI MFD2
        ];
        for &payload in payloads {
            for &seq in &[1u8, 0x42, 0x7F] {
                let frame = build_frame(OP_HASH_RANGE, seq, payload);
                assert_eq!(frame[0], 0xF0);
                assert_eq!(*frame.last().unwrap(), 0xF7);
                let (op, got_seq, got_payload) = parse_frame(&frame).expect("parse");
                assert_eq!(op, OP_HASH_RANGE);
                assert_eq!(got_seq, seq);
                assert_eq!(got_payload, payload);
            }
        }
    }

    #[test]
    fn parse_frame_rejects_corrupted_crc() {
        let mut frame = build_frame(OP_INFO, 1, &[]);
        let crc_idx = frame.len() - 2;
        frame[crc_idx] ^= 0x01;
        let err = parse_frame(&frame).expect_err("must reject corrupted CRC");
        assert!(format!("{err}").contains("CRC"));
    }

    #[test]
    fn rom_crc32_matches_python_zlib_sequence() {
        // The Python reference uses zlib.crc32(buf, ~0) ^ ~0 which is
        // identical to "init=0, no XOR-out". Vectors from running the
        // Python reference firmware/sector_diff.py::zlib_rom_crc32:
        assert_eq!(rom_crc32(b""), 0x00000000);
        assert_eq!(rom_crc32(b"123456789"), 0x2DFD2D88);
        assert_eq!(rom_crc32(&[0xFFu8; 16]), 0xD3088D4F);
    }

    #[test]
    fn sysex_to_usbmidi_pads_correctly() {
        // F0 7D 01 01 (CRC=?) F7 — minimum frame, 6 bytes. Should produce
        // 2 USB-MIDI 4-byte packets.
        let frame = build_frame(OP_INFO, 1, &[]);
        assert_eq!(frame.len(), 6);
        let pkts = sysex_to_usbmidi(&frame);
        assert_eq!(pkts.len(), 8); // 2 packets × 4 bytes
        assert_eq!(pkts[0] & 0x0F, 0x4); // SysEx start (3 bytes)
        assert_eq!(pkts[4] & 0x0F, 0x7); // SysEx end (3 bytes — F7 is last)
    }

    #[test]
    fn usbmidi_to_sysex_reassembles_from_packets() {
        let frame = build_frame(OP_HASH_RANGE, 0x10, &[0xDEu8, 0xAD, 0xBE, 0xEF]);
        let pkts = sysex_to_usbmidi(&frame);
        let msgs = usbmidi_to_sysex(&pkts);
        assert_eq!(msgs.len(), 1);
        assert_eq!(msgs[0], frame);
    }

    #[test]
    fn write_aai_chunk_picks_per_dispatcher_version() {
        let mfd1 = FlashInfo {
            jedec: 0xBF2541,
            sector_size: 4096,
            flash_size: 0x200000,
            version: VERSION_MFD1,
        };
        let mfd2 = FlashInfo { version: VERSION_MFD2, ..mfd1 };
        assert_eq!(mfd1.write_aai_chunk(), 10);
        assert_eq!(mfd2.write_aai_chunk(), 192);
    }
}

pub fn batch_diff(
    fc: &mut MidiFlash,
    base_addr: u32,
    ref_bytes: &[u8],
    sector_buf_addr: u32,
) -> Result<SectorDiff> {
    let len = ref_bytes.len() as u32;
    let whole_local = rom_crc32(ref_bytes);
    let (whole_device, sec_crcs) = fc.device_crc32_batch(base_addr, len, sector_buf_addr)?;

    let mut diff = Vec::new();
    for (i, dev_crc) in sec_crcs.iter().enumerate() {
        let off = i as u32 * SECTOR_SIZE;
        let end = ((off + SECTOR_SIZE) as usize).min(ref_bytes.len());
        let local = rom_crc32(&ref_bytes[off as usize..end]);
        if local != *dev_crc {
            diff.push(off);
        }
    }

    Ok(SectorDiff {
        diff,
        whole_local,
        whole_device,
    })
}
