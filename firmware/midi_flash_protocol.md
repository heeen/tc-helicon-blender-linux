# MIDI SysEx Flash Protocol — DICE3 Blender

Wire protocol for the in-eCos-context flash handler hooked at
`midi_sysex_cb @ 0x2a34` in primary firmware. Mirrors the DCP-0x81F handler's
opcodes plus `HASH_RANGE` for the sector-diff strategy.

## Frame layout

```
F0 7D <op:1> <seq:1> <packed_payload>... <crc8:1> F7
```

| Field | Width | Notes |
|---|---|---|
| `F0` | 1 | SysEx start byte |
| `7D` | 1 | Manufacturer ID — Educational/reserved (silently ignored by stock fw) |
| `op` | 1 | Request opcode 0x01..0x06; replies set bit 6 (0x40) on success or bit 7 (0x80) on error |
| `seq` | 1 | Request/response correlation; host increments per request, device echoes |
| `packed_payload` | N | Raw-byte payload Roland-packed to 7-bit (see below); empty for INFO |
| `crc8` | 1 | CRC-8/ATM (poly 0x07, init 0x00, no XOR-out) over `op || seq || packed_payload`. The CRC8 byte itself is the low 7 bits; bit 7 always 0. |
| `F7` | 1 | SysEx end byte |

All bytes between `F0` and `F7` (exclusive) MUST have bit 7 = 0 per MIDI spec.
The `op` and `seq` bytes are constrained to `[0x00..0x7F]`.

## Roland 7-of-8 packing

Standard "MMA Universal SysEx Packed" encoding. Each group of 7 raw bytes
becomes 8 transmitted bytes:

```
raw:   B0 B1 B2 B3 B4 B5 B6
sent:  M  L0 L1 L2 L3 L4 L5 L6
       │  └──────── low 7 bits of B0..B6 ─────────┘
       └─ M[bit i] = bit 7 of raw byte Bi
```

Final group may be short: `M` byte is followed by N (≤ 7) low-7-bit data
bytes, where N = number of remaining raw bytes. Decoder uses received-byte
count to know N.

**Overhead**: 8/7 = 14.3% expansion. A 256-byte raw payload becomes
`ceil(256/7)*8 = 296` packed bytes plus one CRC8.

## Frame-size limits

The hook architecture differs by firmware version (echoed in INFO):

- **MFD1** (filter-mode hook at `midi_sysex_cb @ 0x2A34`): inherits
  stock `midi_parser_process_bytes`'s 23-byte parser ctx data buffer
  (`0x29764+0x01..+0x17`). Frames > 22 SysEx-data bytes are silently
  dropped before reaching the hook. **Max raw payload = 15 bytes**;
  WRITE_AAI capped at 11 (10 host-side for AAI word alignment).
- **MFD2** (BL-replace hook at the call to `midi_parser_process_bytes`
  inside `midi_rx_poll` @ 0x2AD8): demultiplexes the raw byte stream
  before the parser sees it. F0 7D... frames go to a 1 KB private
  accumulator in patch-zone SRAM (0x2C000). Other bytes are forwarded
  to the displaced parser unchanged. **Max raw payload ≈ 880 bytes**;
  in practice WRITE_AAI uses 192 raw / 230 wire frame.

Reply direction (device → host) is the same in both: `midi_tx_write @
0x1114` ringbuf empirically tops out near **256 packed bytes** (≈ 192
raw + envelope), so READ and MEM_READ cap their reply at 192 raw bytes
regardless of dispatcher version.

Throughput (MFD2): ~730 ms / 4 KB sector via WRITE_AAI 192 B chunks
(21 round-trips). MFD1 (10 B chunks, 410 round-trips) was ~5× slower.

## CRC8

```
ATM/CCITT poly 0x07, init 0x00, no reflect, no XOR-out.
CRC = crc8(op || seq || packed_payload)
sent_byte = CRC & 0x7F     # high bit cleared (MIDI requires it)
```

The host MUST set bit 7 to 0; the device verifies the low-7-bit CRC matches
its own computation `crc & 0x7F`. Collisions on bit 7 are a protocol-design
trade-off (we lose 1 bit of CRC strength to MIDI's 7-bit constraint —
acceptable since we also have the `seq` for coarse anti-replay).

## Opcodes

All multi-byte integers are **little-endian**. Addresses are SPI-flash
addresses (0..0x1FFFFF on SST25VF016B).

### 0x01 — INFO

| Direction | Body |
|---|---|
| Request | (empty) |
| Reply (`0x41`) | `jedec_id (4) sector_size (4) flash_size (4) version (4)` |

`version` is firmware build-tag (currently `0x56324653` = "V2FS" until we
add a per-handler tag).

### 0x02 — READ

| Direction | Body |
|---|---|
| Request | `addr (4) len (2)` — len ≤ 4096 (one sector) |
| Reply (`0x42`) | `chunk_seq (1) chunk_count (1) data (≤256)` |

For `len > 256`, device sends multiple reply frames with sequential
`chunk_seq` (0..N-1) and identical `chunk_count = N`. The outer SysEx `seq`
field stays constant across all chunks of the same READ.

### 0x03 — HASH_RANGE

| Direction | Body |
|---|---|
| Request | `addr (4) len (4) sectorbuf_sram (4) flags (1)` |
| Reply (`0x43`) | `whole_crc (4) sector_count (2)` |

Computes CRC-32 (poly `0xEDB88320`, init `0xFFFFFFFF`, final XOR
`0xFFFFFFFF` — matches Python `zlib.crc32`) over `[addr, addr+len)`.

If `flags & 0x01`, also writes per-4 KB-sector CRCs (`sector_count` u32
LE values) to SRAM at `sectorbuf_sram`. Caller must follow up with a READ
of `sectorbuf_sram` to retrieve the per-sector CRCs.

`sectorbuf_sram` defaults to `0x2B000` (the shared `WRITE_BUF` /
`SECTOR_CRC_BUF` slot). Host MAY pass any 4 KB-aligned SRAM address it
trusts.

Implementation calls bootloader's ROM CRC routine at `0x20000DA4`,
matching v2 driver's `V2_CMD_HASH_RANGE`.

### 0x04 — ERASE_SECTOR

| Direction | Body |
|---|---|
| Request | `addr (4) count (2)` — number of consecutive 4 KB sectors |
| Reply (`0x44`) | `status (4)` |

Calls `sst25xx_sector_erase(driver_ctx, addr + n*0x1000)` for n in
`[0..count)`. `status` is the rc from the first failing erase, or 0.
Refuses any range overlapping golden copy `0x10000..0x40000`
(`status = 0xDEAD0001 = ERR_GOLDEN_PROTECT`).

### 0x05 — WRITE_AAI

| Direction | Body |
|---|---|
| Request | `addr (4) data (1..256)` |
| Reply (`0x45`) | `status (4)` |

Calls `sst25xx_aai_write(driver_ctx, addr, data, len)` directly. Stock fw
handles WREN/WRDI/RDSR-poll-per-pair internally — full eCos sync.
Refuses any range overlapping golden copy.

For larger updates the host issues multiple WRITE_AAI frames sequentially.
A 4 KB sector takes 16 frames (256 B each); ~300 ms total at typical
SysEx throughput.

### 0x06 — REBOOT

| Direction | Body |
|---|---|
| Request | `mode (1)` — 0 = soft reboot, 1 = re-enumerate USB only |
| Reply (`0x46`) | `status (4)` (sent before reset takes effect) |

## Errors

Reply with `op | 0x80`. Body = `error_code (4)`.

| Code | Mnemonic |
|---|---|
| `0xDEAD0001` | `ERR_GOLDEN_PROTECT` |
| `0xDEAD0002` | `ERR_BAD_BODY` |
| `0xDEAD0003` | `ERR_ERASE_FAIL` |
| `0xDEAD0004` | `ERR_AAI_FAIL` |
| `0xDEAD0005` | `ERR_BAD_CRC` |
| `0xDEAD0006` | `ERR_TRUNCATED` |
| `0xDEAD00FF` | `ERR_UNKNOWN_OPCODE` |

(Same value space as `sram_flash_handler.c` for cross-path parity.)

## Sequencing notes

- **One request in flight at a time.** Device handler runs serially in
  `midi_engine_thread @ 0x5010`; pipelining requests will overflow the
  RX ringbuf. Host MUST wait for matching `seq`-tagged reply before
  sending the next request.
- **READ chunk reordering**: device emits chunks in order; host MAY
  assume `chunk_seq` is monotonically increasing per `seq`.
- **HASH_RANGE → READ pipeline**: HASH_RANGE returns immediately after
  computing whole-image CRC; per-sector CRCs are flushed to SRAM as part
  of the same call (single SPI sweep). The follow-up READ to drain the
  sector-buffer is a normal READ frame.

## Worked example: single-sector update

```
1. host  →  HASH_RANGE addr=0x40000 len=0x4B000 sectorbuf=0x2B000 flags=1
   dev   →  whole_crc=0xDEADBEEF sector_count=75
2. host  →  READ addr=0x2B000 len=300         (75 × 4-byte CRCs)
   dev   →  300 bytes of per-sector CRCs (chunked)
3. host computes diff vs reference image, finds 1 sector at 0x42000.
4. host  →  ERASE_SECTOR addr=0x42000 count=1
   dev   →  status=0
5. host  →  WRITE_AAI addr=0x42000 data=<256 bytes>
   dev   →  status=0
   ... 16 frames total for 4 KB ...
6. host  →  HASH_RANGE addr=0x42000 len=0x1000 sectorbuf=0 flags=0
   dev   →  whole_crc=<sector hash>     # confirms write
```

## Implementation footprint

- Patch-zone code:
  - `firmware/patch/midi_sysex_filter.c`: ~1.2 KB (parser + dispatcher + 7-of-8 codec + CRC8 + HASH_RANGE).
  - `firmware/midi_flash_dispatch.c`: ~600 B (`flash_op_*`, sst25xx wrappers + golden guard).
  - Total patch-zone usage 2026-05-05 build: 8712 B / 9216 B available (`PATCH_ZONE_SIZE = 0x2400` at `0x32600`).
- DCP-handler (recovery) zone: 728 B at `0x31480..0x317A8`, fits in 1 KB DEADBEEF zone.
- SRAM scratch (eCos-context): `0x2B000-0x2C000` (4 KB) reused by the SysEx
  filter for WRITE_BUF / SECTOR_CRC_BUF / DATA_BUF (`firmware/patch/midi_sysex_filter.c`).

## Host implementation

| File | Role |
|---|---|
| `firmware/midi_flash.py` | Main host CLI (info / read / hash / erase / write / update / reboot). Uses pyusb on bulk EP3 directly to bypass ALSA SysEx truncation. |
| `firmware/sector_diff.py` | Transport-agnostic sector-diff (`zlib_rom_crc32`, `batch_diff`, `filter_ops_by_diff`). Used by both v2 mailbox driver and `midi_flash.py`. |
| `firmware/midi_flash_stress.py` | Stress harness — `update --ref X --iters N` with timing + failure summary. |
| `firmware/midi_flash_selftest.py` | Hardware-free protocol self-tests: pack78, CRC8, frame round-trip, USB-MIDI 1.0 packet framing, sector_diff against a mocked device. Run before bench time. |

## Bring-up sequence (one-time install)

```bash
# 1. Verify the protocol layer first (no hardware):
python3 firmware/midi_flash_selftest.py

# 2. Build patched SPI image with the SysEx hook baked in:
make -C firmware/patch
python3 firmware/patch/hooks.py patch
# → firmware/blender_spi_patched.bin

# 3. Flash it via JTAG (the existing recovery path does this):
python3 firmware/inject_flash_handler.py
blender-ctl usb flash update firmware/blender_spi_patched.bin

# 4. Reboot, then talk to the device entirely via USB MIDI:
python3 firmware/midi_flash.py info
```

## Routine update

```bash
# After step 4 above is done once, JTAG is no longer required:
python3 firmware/midi_flash.py update --ref firmware/blender_primary_fixed.bin
```

The `update` subcommand:
1. Issues `HASH_RANGE` over the whole image with `flags=PER_SECTOR`.
2. If whole-image CRC matches the host-computed reference, exits with no writes.
3. Otherwise drains per-sector CRCs from `WRITE_BUF + 0x400`, computes the
   diff set, and issues `ERASE_SECTOR` + `WRITE_AAI×16` per differing
   4 KB sector.
4. Re-issues `HASH_RANGE` to confirm.
