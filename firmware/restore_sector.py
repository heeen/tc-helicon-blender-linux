#!/usr/bin/env python3
"""Restore SPI sectors 0x51000 and 0x52000 via eCos in-context hook.

Instead of launching standalone SRAM code (which crashed because the eCos
RTOS wasn't in the loop), this hooks into the running firmware's main loop:

  1. Let golden firmware boot normally (15s) — full eCos + SPI driver init
  2. JTAG halt
  3. Load repair code + sector data into SRAM
  4. Patch ONE instruction: at 0x2e48, replace `bl 0x14dc` with `bl 0x31C00`
  5. Invalidate I-cache
  6. Resume — eCos main loop thread calls our code on next iteration
  7. Repair code erases + writes sectors, sets done flag, calls original FUN_14dc
  8. Poll mailbox for completion

The repair code executes inside a normal eCos thread context with everything
working: IRQs, scheduler, DMA, SPI driver.

Sector 0x51000: Reconstructed from recovery image with BSS relocations.
Sector 0x52000: Original correct data from flash dump.

Usage:
    python3 firmware/restore_sector.py                        # OpenOCD (default)
    python3 firmware/restore_sector.py --backend bmp          # BMP probe
    python3 firmware/restore_sector.py --sector 0x52000       # single sector
    python3 firmware/restore_sector.py --dry-run              # print GDB script (BMP)
"""

import argparse
import os
import shutil
import socket
import struct
import subprocess
import sys
import tempfile
import time
import zlib
from pathlib import Path

FIRMWARE_DIR = Path(__file__).parent
OPENOCD_CFG = FIRMWARE_DIR.parent / 'jtag' / 'miolink-dice3-openocd.cfg'

# SPI flash layout
RECOVERY_BODY   = 0x10030
PRIMARY_HEADER  = 0x40000
PRIMARY_BODY    = 0x40030
SECTOR_SIZE     = 0x1000  # 4KB

# Sectors to restore
SECTOR_51000 = 0x51000
SECTOR_52000 = 0x52000

# Recovery reconstruction
PRIMARY_BODY_OFFSET  = SECTOR_51000 - PRIMARY_BODY
RECOVERY_BODY_OFFSET = PRIMARY_BODY_OFFSET - 0x430
RECOVERY_SPI_OFFSET  = RECOVERY_BODY + RECOVERY_BODY_OFFSET

RELOCATION_DELTA = -832
RELOCATION_INDICES = {
    247: (0x000305E0, 0x000302A0),
    261: (0x000305CC, 0x0003028C),
    262: (0x000305C8, 0x00030288),
    268: (0x000305CC, 0x0003028C),
    271: (0x000305CC, 0x0003028C),
    355: (0x00030408, 0x000300C8),
    356: (0x0003041C, 0x000300DC),
    461: (0x000306A8, 0x00030368),
    462: (0x00030628, 0x000302E8),
}

# SRAM layout (must match sram_flash_restore.c)
MAILBOX_ADDR     = 0x31B00
CODE_ADDR        = 0x31C00
DONE_FLAG_ADDR   = 0x32200
SECTOR_TABLE     = 0x32210
RTT_CB_ADDR      = 0x32300
RTT_BUF_ADDR     = 0x32340
RTT_BUF_SIZE     = 704
DATA_BASE        = 0x33000   # sector data loaded here (4KB per sector)

# Hook site — golden firmware main loop
HOOK_ADDR        = 0x2c88    # address of `bl 0x46c4` in golden firmware event loop
HOOK_TARGET      = CODE_ADDR # our repair code entry point

# ARM bl encoding: bl from HOOK_ADDR to HOOK_TARGET
# offset = (target - source - 8) >> 2, encoded in bottom 24 bits with 0xEB prefix
_BL_OFFSET       = ((HOOK_TARGET - HOOK_ADDR - 8) >> 2) & 0x00FFFFFF
BL_HOOK_INSN     = 0xEB000000 | _BL_OFFSET   # bl 0x31C00

# Original instruction at 0x2c88: bl 0x46c4
_BL_ORIG_OFFSET  = ((0x46c4 - HOOK_ADDR - 8) >> 2) & 0x00FFFFFF
BL_ORIG_INSN     = 0xEB000000 | _BL_ORIG_OFFSET  # bl 0x46c4

# Firmware identity check
FN_AAI_WRITE_ADDR = 0x8538
FN_AAI_WRITE_FIRST_WORD = 0xe92d4ff0


# ── OpenOCD TCL client ─────────────────────────────────────

class OpenOCDTcl:
    """Direct interface to OpenOCD via TCL port (6666)."""

    def __init__(self, host='localhost', port=6666, timeout=30):
        self.sock = socket.create_connection((host, port), timeout=timeout)
        self.sock.settimeout(timeout)

    def cmd(self, command: str) -> str:
        self.sock.sendall(command.encode('utf-8') + b'\x1a')
        buf = b''
        while b'\x1a' not in buf:
            chunk = self.sock.recv(4096)
            if not chunk:
                break
            buf += chunk
        return buf.split(b'\x1a')[0].decode('utf-8', errors='replace').strip()

    def halt(self):
        return self.cmd('halt')

    def resume(self, addr=None):
        if addr is not None:
            return self.cmd(f'resume {addr:#x}')
        return self.cmd('resume')

    def reg(self, name: str, value=None):
        if value is not None:
            return self.cmd(f'reg {name} {value:#x}')
        return self.cmd(f'reg {name}')

    def mdw(self, addr: int, count: int = 1) -> list:
        result = self.cmd(f'mdw {addr:#x} {count}')
        values = []
        for line in result.splitlines():
            if ':' in line:
                hex_words = line.split(':', 1)[1].strip().split()
                values.extend(int(w, 16) for w in hex_words)
        return values

    def mww(self, addr: int, value: int):
        return self.cmd(f'mww {addr:#x} {value:#x}')

    def load_image(self, path: str, addr: int):
        return self.cmd(f'load_image {path} {addr:#x} bin')

    def read_memory(self, addr: int, size: int) -> bytes:
        with tempfile.NamedTemporaryFile(suffix='.bin', delete=False) as f:
            tmp = f.name
        try:
            self.cmd(f'dump_image {tmp} {addr:#x} {size}')
            return Path(tmp).read_bytes()
        finally:
            try:
                os.unlink(tmp)
            except OSError:
                pass

    def close(self):
        self.sock.close()


# ── Sector data preparation ────────────────────────────────

def reconstruct_sector_51000(spi_flash: bytes) -> bytes:
    recovery_sector = spi_flash[RECOVERY_SPI_OFFSET:RECOVERY_SPI_OFFSET + SECTOR_SIZE]
    if len(recovery_sector) != SECTOR_SIZE:
        raise ValueError(f"Recovery sector too short: {len(recovery_sector)}")
    if all(b == 0xFF for b in recovery_sector):
        raise ValueError("Recovery sector is all 0xFF")

    words = list(struct.unpack(f'<{SECTOR_SIZE // 4}I', recovery_sector))
    for idx, (expected_recovery, expected_primary) in RELOCATION_INDICES.items():
        actual = words[idx]
        if actual != expected_recovery:
            raise ValueError(
                f"Word [{idx}]: expected 0x{expected_recovery:08x}, got 0x{actual:08x}")
        words[idx] = expected_primary

    return struct.pack(f'<{len(words)}I', *words)


def validate_crc(spi_flash: bytes, sector_51_data: bytes, sector_52_data: bytes) -> bool:
    hdr_magic, hdr_size = struct.unpack_from('<2I', spi_flash, PRIMARY_HEADER)
    body_bytes = hdr_size * 4
    print(f"  Primary body: {body_bytes} bytes (0x{body_bytes:x})")

    body = bytearray(spi_flash[PRIMARY_BODY:PRIMARY_BODY + body_bytes])
    off_51 = SECTOR_51000 - PRIMARY_BODY
    off_52 = SECTOR_52000 - PRIMARY_BODY
    body[off_51:off_51 + SECTOR_SIZE] = sector_51_data
    body[off_52:off_52 + SECTOR_SIZE] = sector_52_data

    crc = zlib.crc32(bytes(body), 0xFFFFFFFF) & 0xFFFFFFFF
    print(f"  CRC-32: 0x{crc:08x} (need 0xFFFFFFFF)")
    return crc == 0xFFFFFFFF


def build_sector_table(sectors: dict) -> bytes:
    entries = sorted(sectors.items())
    data = struct.pack('<I', len(entries))
    for i, (spi_addr, _sector_data) in enumerate(entries):
        data_ptr = DATA_BASE + i * SECTOR_SIZE
        data += struct.pack('<II', spi_addr, data_ptr)
    return data


# ── RTT reader ─────────────────────────────────────────────

def read_rtt_log(ocd: OpenOCDTcl) -> str:
    """Read RTT ring buffer from SRAM via mdw."""
    wroff_words = ocd.mdw(RTT_CB_ADDR + 0x24, 1)
    wroff = wroff_words[0] if wroff_words else 0
    if wroff == 0:
        return "(empty — WrOff=0)"

    buf_size = min(wroff, RTT_BUF_SIZE)
    rtt_text = ''
    for off in range(0, buf_size, 64):
        chunk_words = min(16, (buf_size - off + 3) // 4)
        words = ocd.mdw(RTT_BUF_ADDR + off, chunk_words)
        for w in words:
            for byte_idx in range(4):
                ch = (w >> (byte_idx * 8)) & 0xFF
                if 0x20 <= ch < 0x7F or ch == 0x0A:
                    rtt_text += chr(ch)
                elif ch == 0:
                    break
            if len(rtt_text) >= buf_size:
                break
    return rtt_text.strip() if rtt_text.strip() else "(no printable text)"


def read_mailbox(ocd: OpenOCDTcl) -> dict:
    """Read the 8-word mailbox from SRAM."""
    words = ocd.mdw(MAILBOX_ADDR, 8)
    keys = ['magic', 'num_ops', 'num_ok', 'num_fail', 'last_r0', 'phase', 'cur_addr', 'reserved']
    return dict(zip(keys, words))


# ── OpenOCD restore via eCos hook ──────────────────────────

def run_openocd_restore(ocd: OpenOCDTcl, code_bin: Path,
                        data_files: list, tbl_path: str,
                        sectors: dict):
    """Hook into eCos main loop and execute SPI sector repair."""

    print()
    print("=" * 60)
    print("SPI Flash Restore — eCos In-Context Hook")
    print("=" * 60)

    # ── Phase 0: Halt and verify firmware identity ──
    ocd.halt()
    time.sleep(0.2)

    try:
        xip_words = ocd.mdw(FN_AAI_WRITE_ADDR)
    except Exception as e:
        print(f"\n[!] Cannot read target memory: {e}")
        return False
    if not xip_words:
        print(f"\n[!] mdw returned empty")
        return False
    xip_word = xip_words[0]
    print(f"\nXIP @ {FN_AAI_WRITE_ADDR:#x}: {xip_word:#010x} (expect {FN_AAI_WRITE_FIRST_WORD:#010x})")
    if xip_word != FN_AAI_WRITE_FIRST_WORD:
        print("[!] Firmware mismatch — wrong image in XIP!")
        return False

    # ── Phase 1: Let eCos boot fully ──
    print("\nResuming for 15s (eCos boot + SPI driver init)...")
    ocd.resume()
    time.sleep(15)
    ocd.halt()
    time.sleep(0.2)

    cpsr_result = ocd.reg('cpsr')
    print(f"  CPSR: {cpsr_result}")

    # Verify the original instruction at hook site
    hook_words = ocd.mdw(HOOK_ADDR, 1)
    orig_insn = hook_words[0] if hook_words else 0
    print(f"\n  Hook site 0x{HOOK_ADDR:x}: {orig_insn:#010x} (expect {BL_ORIG_INSN:#010x})")
    if orig_insn != BL_ORIG_INSN:
        print(f"[!] Unexpected instruction at hook site!")
        print(f"    Expected: {BL_ORIG_INSN:#010x} (bl 0x14dc)")
        print(f"    Got:      {orig_insn:#010x}")
        print("    Aborting — hook site may be wrong or already patched")
        return False
    print("  Hook site verified OK")

    # Verify SPI driver context is initialized
    print("\n--- SPI Driver Diagnostics ---")
    driver_words = ocd.mdw(0x2A9C4, 4)
    print(f"  DRIVER_CTX @ 0x2A9C4: {' '.join(f'{w:#010x}' for w in driver_words)}")

    # ── Phase 2: Load code + data to SRAM ──
    print("\n--- Loading SRAM payload ---")

    result = ocd.load_image(str(code_bin), CODE_ADDR)
    print(f"  Code: {result}")
    code_check = ocd.mdw(CODE_ADDR)[0]
    print(f"  Verify: first word = {code_check:#010x}")

    result = ocd.load_image(tbl_path, SECTOR_TABLE)
    print(f"  Sector table: {result}")
    tbl_check = ocd.mdw(SECTOR_TABLE)[0]
    print(f"  Verify: num_sectors = {tbl_check}")

    for i, data_file in enumerate(data_files):
        addr = DATA_BASE + i * SECTOR_SIZE
        result = ocd.load_image(data_file, addr)
        print(f"  Sector data {i} @ {addr:#x}: {result}")

    # Clear mailbox and done flag
    for i in range(8):
        ocd.mww(MAILBOX_ADDR + i * 4, 0)
    ocd.mww(DONE_FLAG_ADDR, 0)
    print(f"  Mailbox + done flag cleared")

    # ── Phase 3: Patch hook and launch ──
    print("\n--- Patching hook ---")
    print(f"  0x{HOOK_ADDR:x}: {BL_ORIG_INSN:#010x} -> {BL_HOOK_INSN:#010x}")
    ocd.mww(HOOK_ADDR, BL_HOOK_INSN)

    # Verify patch took
    patched = ocd.mdw(HOOK_ADDR, 1)[0]
    if patched != BL_HOOK_INSN:
        print(f"[!] Patch verification failed: read back {patched:#010x}")
        return False
    print("  Patch verified OK")

    # Invalidate I-cache so CPU fetches the patched instruction
    ocd.cmd("arm mcr 15 0 7 5 0 0")
    print("  I-cache invalidated")

    # Resume — eCos scheduler runs, main loop hits our hook
    print("\n--- Resuming (eCos will call hook on next loop iteration) ---")
    ocd.resume()

    # ── Phase 4: Poll mailbox for completion ──
    phase_names = {0: 'init', 1: 'erase', 2: 'write', 3: 'done'}
    done = False

    for attempt in range(15):
        time.sleep(2)
        try:
            ocd.halt()
        except Exception as e:
            print(f"  halt failed: {e}")
            break
        time.sleep(0.1)

        # Read PC for diagnostics
        try:
            pc_result = ocd.reg('pc')
            pc_str = pc_result.split(':')[-1].strip() if ':' in pc_result else pc_result
        except Exception:
            pc_str = '?'

        # Check mailbox
        try:
            mb = read_mailbox(ocd)
        except Exception:
            mb = {'magic': 0, 'phase': 0, 'cur_addr': 0}

        if mb['magic'] == 0x464C5348:
            print(f"  [{attempt+1}] PC={pc_str} — COMPLETE")
            done = True
            break

        pname = phase_names.get(mb['phase'], f"?{mb['phase']}")
        print(f"  [{attempt+1}] PC={pc_str} phase={pname} addr={mb['cur_addr']:#010x}")

        # If PC is in exception vector area, something crashed
        try:
            pc_val = int(pc_str.replace('0x', '').replace(' ', ''), 16)
        except (ValueError, AttributeError):
            pc_val = -1
        if 0 <= pc_val < 0x100:
            print(f"  [!] PC at exception vector {pc_val:#x} — crash detected")
            break

        ocd.resume()

    # ── Phase 5: Results ──
    # Make sure we're halted for reading
    try:
        ocd.halt()
        time.sleep(0.1)
    except Exception:
        pass

    # Restore original instruction (cleanup)
    print("\n--- Restoring original instruction ---")
    ocd.mww(HOOK_ADDR, BL_ORIG_INSN)
    ocd.cmd("arm mcr 15 0 7 5 0 0")
    restored = ocd.mdw(HOOK_ADDR, 1)[0]
    print(f"  0x{HOOK_ADDR:x}: {restored:#010x} (restored)")

    # Read registers
    print("\n--- CPU State ---")
    for reg in ['cpsr', 'pc', 'lr', 'r0', 'sp']:
        try:
            val = ocd.reg(reg)
            print(f"  {reg}: {val}")
        except Exception:
            pass

    # RTT log
    print("\n--- RTT Log ---")
    rtt = read_rtt_log(ocd)
    print(rtt)
    print("--- End RTT ---")

    # Mailbox results
    print("\n--- Mailbox Results ---")
    mb = read_mailbox(ocd)
    magic_str = 'FLSH' if mb['magic'] == 0x464C5348 else 'INCOMPLETE'
    print(f"  Magic:     {mb['magic']:#010x} ({magic_str})")
    print(f"  Phase:     {mb['phase']} (0=init 1=erase 2=write 3=done)")
    print(f"  Ops:       {mb['num_ops']} total, {mb['num_ok']} OK, {mb['num_fail']} failed")
    print(f"  Last r0:   {mb['last_r0']:#010x}")
    print(f"  Last addr: {mb['cur_addr']:#010x}")

    if mb['magic'] == 0x464C5348 and mb['num_fail'] == 0:
        print()
        print("=" * 60)
        print("SUCCESS — all sectors written")
        print("Power cycle the device. Expected: lsusb shows PID 0x8FE1")
        print("=" * 60)
        return True
    elif mb['magic'] == 0x464C5348:
        print()
        print("=" * 60)
        print(f"COMPLETED WITH ERRORS — {mb['num_fail']} failures")
        print("=" * 60)
        return False
    else:
        print()
        print("=" * 60)
        print("DID NOT COMPLETE — check RTT output above")
        print(f"  Stuck at phase {mb['phase']}, SPI addr {mb['cur_addr']:#010x}")
        print("=" * 60)
        return False


# ── BMP GDB script ─────────────────────────────────────────

def generate_gdb_script(code_file: str, sector_data_files: list,
                        sector_table_file: str, port: str) -> str:
    """Generate GDB script for BMP backend using eCos hook approach."""

    restore_cmds = []
    for i, data_file in enumerate(sector_data_files):
        addr = DATA_BASE + i * SECTOR_SIZE
        restore_cmds.append(
            f'gdb.execute("restore {data_file} binary {addr:#x}", to_string=True)')
        restore_cmds.append(
            f'print(f"  Sector data {i}: loaded to {addr:#x}")')
    restore_block = '\n'.join(restore_cmds)

    return f'''set confirm off
set pagination off
target extended-remote {port}
monitor jtag_scan
attach 1
monitor reset
monitor jtag_scan
attach 1

python
import gdb
import struct
import threading
import time

inf = gdb.selected_inferior()

MAILBOX      = {MAILBOX_ADDR:#x}
CODE_ADDR    = {CODE_ADDR:#x}
DONE_FLAG    = {DONE_FLAG_ADDR:#x}
HOOK_ADDR    = {HOOK_ADDR:#x}
BL_HOOK_INSN = {BL_HOOK_INSN:#x}
BL_ORIG_INSN = {BL_ORIG_INSN:#x}
RTT_CB       = {RTT_CB_ADDR:#x}
RTT_BUF      = {RTT_BUF_ADDR:#x}
RTT_BUF_SZ   = {RTT_BUF_SIZE}

def interrupt_after(secs):
    def fn():
        time.sleep(secs)
        gdb.post_event(lambda: gdb.execute("interrupt"))
    threading.Thread(target=fn, daemon=True).start()

def read_word(addr):
    return struct.unpack('<I', bytes(inf.read_memory(addr, 4)))[0]

def write_word(addr, val):
    inf.write_memory(addr, struct.pack('<I', val))

print()
print("=" * 60)
print("SPI Flash Restore — eCos Hook via BMP")
print("=" * 60)

# Verify firmware identity
try:
    word = read_word({FN_AAI_WRITE_ADDR:#x})
    print(f"\\nXIP @ {FN_AAI_WRITE_ADDR:#x}: {{word:#010x}} (expect {FN_AAI_WRITE_FIRST_WORD:#010x})")
    if word != {FN_AAI_WRITE_FIRST_WORD:#x}:
        print("[!] Firmware mismatch!")
        gdb.execute("detach")
        gdb.execute("quit")
except Exception as e:
    print(f"[!] Cannot read XIP: {{e}}")
    gdb.execute("quit")

# Let eCos boot
print("\\nResuming for 15s (eCos boot)...")
interrupt_after(15)
gdb.execute("continue")

# Verify hook site
orig_insn = read_word(HOOK_ADDR)
print(f"\\nHook site 0x{{HOOK_ADDR:x}}: {{orig_insn:#010x}} (expect {{BL_ORIG_INSN:#010x}})")
if orig_insn != BL_ORIG_INSN:
    print("[!] Hook site mismatch — aborting")
    gdb.execute("detach")
    gdb.execute("quit")
print("Hook site verified OK")

# Load SRAM payload
print("\\n--- Loading SRAM payload ---")
gdb.execute("restore {code_file} binary {CODE_ADDR:#x}", to_string=True)
code_word = read_word(CODE_ADDR)
print(f"  Code: loaded to {CODE_ADDR:#x} (first word: {{code_word:#010x}})")

gdb.execute("restore {sector_table_file} binary {SECTOR_TABLE:#x}", to_string=True)
tbl_word = read_word({SECTOR_TABLE:#x})
print(f"  Sector table: loaded to {SECTOR_TABLE:#x} (num_sectors={{tbl_word}})")

{restore_block}

# Clear mailbox + done flag
inf.write_memory(MAILBOX, b'\\x00' * 32)
write_word(DONE_FLAG, 0)
print("  Mailbox + done flag cleared")

# Patch hook
print("\\n--- Patching hook ---")
print(f"  0x{{HOOK_ADDR:x}}: {{BL_ORIG_INSN:#010x}} -> {{BL_HOOK_INSN:#010x}}")
write_word(HOOK_ADDR, BL_HOOK_INSN)
patched = read_word(HOOK_ADDR)
if patched != BL_HOOK_INSN:
    print(f"[!] Patch failed: {{patched:#010x}}")
    gdb.execute("detach")
    gdb.execute("quit")
print("  Patch verified OK")

# Resume — main loop will call our hook
print("\\n--- Resuming (eCos hook active) ---")
done = False
for attempt in range(15):
    interrupt_after(2)
    gdb.execute("continue")

    magic = read_word(MAILBOX)
    if magic == 0x464C5348:
        print(f"  [{{attempt+1}}] COMPLETE")
        done = True
        break

    phase = read_word(MAILBOX + 0x14)
    cur_addr = read_word(MAILBOX + 0x18)
    phase_names = {{0: 'init', 1: 'erase', 2: 'write', 3: 'done'}}
    pname = phase_names.get(phase, f'?{{phase}}')
    print(f"  [{{attempt+1}}] phase={{pname}} addr={{cur_addr:#010x}}")

# Restore original instruction
print("\\n--- Restoring original instruction ---")
write_word(HOOK_ADDR, BL_ORIG_INSN)
restored = read_word(HOOK_ADDR)
print(f"  0x{{HOOK_ADDR:x}}: {{restored:#010x}} (restored)")

# RTT log
print("\\n--- RTT Log ---")
try:
    wroff = read_word(RTT_CB + 0x24)
    if wroff > 0:
        buf_size = min(wroff, RTT_BUF_SZ)
        rtt_data = bytes(inf.read_memory(RTT_BUF, buf_size))
        rtt_text = rtt_data.replace(b'\\x00', b'').decode('ascii', errors='replace')
        if rtt_text.strip():
            print(rtt_text)
        else:
            print("  (no text)")
    else:
        print("  (empty)")
except Exception as e:
    print(f"  (RTT error: {{e}})")
print("--- End RTT ---")

# Mailbox
print("\\n--- Mailbox Results ---")
mb = bytes(inf.read_memory(MAILBOX, 32))
magic, num_ops, num_ok, num_fail, last_r0, phase, cur_addr, _ = \\
    struct.unpack('<8I', mb)

magic_str = 'FLSH' if magic == 0x464C5348 else 'INCOMPLETE'
print(f"  Magic:     {{magic:#010x}} ({{magic_str}})")
print(f"  Phase:     {{phase}} (0=init 1=erase 2=write 3=done)")
print(f"  Ops:       {{num_ops}} total, {{num_ok}} OK, {{num_fail}} failed")
print(f"  Last r0:   {{last_r0:#010x}}")
print(f"  Last addr: {{cur_addr:#010x}}")

if magic == 0x464C5348 and num_fail == 0:
    print()
    print("=" * 60)
    print("SUCCESS — all sectors written")
    print("Power cycle the device. Expected: lsusb shows PID 0x8FE1")
    print("=" * 60)
elif magic == 0x464C5348:
    print()
    print("=" * 60)
    print(f"COMPLETED WITH ERRORS — {{num_fail}} failures")
    print("=" * 60)
else:
    print()
    print("=" * 60)
    print("DID NOT COMPLETE — check RTT output above")
    print(f"  Stuck at phase {{phase}}, SPI addr {{cur_addr:#010x}}")
    print("=" * 60)

end

detach
quit
'''


# ── Shared helpers ─────────────────────────────────────────

def wait_for_port(host: str, port: int, timeout: float = 10) -> bool:
    deadline = time.time() + timeout
    while time.time() < deadline:
        try:
            s = socket.create_connection((host, port), timeout=0.5)
            s.close()
            return True
        except (ConnectionRefusedError, socket.timeout, OSError):
            time.sleep(0.2)
    return False


def main():
    parser = argparse.ArgumentParser(
        description='Restore SPI sectors via eCos in-context hook')
    parser.add_argument('--backend', choices=['openocd', 'bmp'], default='openocd',
                        help='JTAG backend (default: openocd)')
    parser.add_argument('--port', default='/dev/ttyACM0',
                        help='BMP serial port for --backend bmp (default: /dev/ttyACM0)')
    parser.add_argument('--tcl-port', type=int, default=6666,
                        help='OpenOCD TCL port (default: 6666)')
    parser.add_argument('--openocd-cfg', type=Path, default=OPENOCD_CFG,
                        help=f'OpenOCD config file (default: {OPENOCD_CFG})')
    parser.add_argument('--dry-run', action='store_true',
                        help='Print GDB script without executing (BMP only)')
    parser.add_argument('--sector', type=lambda x: int(x, 0), default=None,
                        help='Restore single sector only (e.g., 0x52000)')
    parser.add_argument('--yes', '-y', action='store_true',
                        help='Skip confirmation prompt')
    parser.add_argument('--spi-flash', type=Path,
                        default=FIRMWARE_DIR / 'blender_spi_flash.bin',
                        help='SPI flash dump file')
    args = parser.parse_args()

    # Build SRAM program if needed
    code_bin = FIRMWARE_DIR / 'sram_flash_restore.bin'
    c_src = FIRMWARE_DIR / 'sram_flash_restore.c'
    ld_script = FIRMWARE_DIR / 'sram_restore.ld'
    elf = FIRMWARE_DIR / 'sram_flash_restore.elf'

    if not code_bin.exists() or (c_src.exists() and c_src.stat().st_mtime > code_bin.stat().st_mtime):
        print('Building SRAM program (C)...')
        subprocess.run([
            'arm-none-eabi-gcc', '-march=armv5te', '-marm', '-mno-thumb-interwork',
            '-Os', '-nostdlib', '-ffreestanding', '-Wall',
            '-T', str(ld_script),
            '-o', str(elf), str(c_src)
        ], check=True)
        subprocess.run([
            'arm-none-eabi-objcopy', '-O', 'binary', str(elf), str(code_bin)
        ], check=True)
    print(f'SRAM program: {code_bin} ({code_bin.stat().st_size} bytes)')

    # Read SPI flash dump
    print(f'Reading SPI flash dump: {args.spi_flash}')
    spi_flash = args.spi_flash.read_bytes()
    print(f'  Size: {len(spi_flash)} bytes')

    # Prepare sector data
    sectors = {}
    if args.sector is None or args.sector == SECTOR_51000:
        print(f'\nReconstructing sector 0x{SECTOR_51000:05X} from recovery...')
        sectors[SECTOR_51000] = reconstruct_sector_51000(spi_flash)
        print(f'  {len(RELOCATION_INDICES)} BSS relocations applied')

    if args.sector is None or args.sector == SECTOR_52000:
        print(f'\nExtracting sector 0x{SECTOR_52000:05X} from flash dump...')
        sector_52 = spi_flash[SECTOR_52000:SECTOR_52000 + SECTOR_SIZE]
        non_ff = sum(1 for b in sector_52 if b != 0xFF)
        print(f'  {non_ff} non-FF bytes')
        sectors[SECTOR_52000] = sector_52

    if not sectors:
        print('ERROR: No sectors to restore')
        sys.exit(1)

    # CRC validation
    print('\nValidating primary firmware CRC with patched sectors...')
    s51 = sectors.get(SECTOR_51000, spi_flash[SECTOR_51000:SECTOR_51000 + SECTOR_SIZE])
    s52 = sectors.get(SECTOR_52000, spi_flash[SECTOR_52000:SECTOR_52000 + SECTOR_SIZE])
    if not validate_crc(spi_flash, s51, s52):
        print('ERROR: CRC validation failed')
        sys.exit(1)
    print('  CRC PASSES')

    # Build sector table
    sector_table = build_sector_table(sectors)

    # Write temp files
    with tempfile.TemporaryDirectory(prefix='restore_sector_') as tmpdir:
        data_files = []
        for spi_addr, data in sorted(sectors.items()):
            path = os.path.join(tmpdir, f'sector_{spi_addr:05x}.bin')
            with open(path, 'wb') as f:
                f.write(data)
            data_files.append(path)
            words = struct.unpack('<4I', data[:16])
            print(f'  0x{spi_addr:05X}: {" ".join(f"0x{w:08x}" for w in words)}')

        tbl_path = os.path.join(tmpdir, 'sector_table.bin')
        with open(tbl_path, 'wb') as f:
            f.write(sector_table)

        print(f'\nReady to restore {len(sectors)} sector(s):')
        for spi_addr in sorted(sectors):
            print(f'  SPI 0x{spi_addr:05X}: erase + write {SECTOR_SIZE} bytes')
        print(f'\nBackend: {args.backend}')
        print(f'Method:  eCos in-context hook at 0x{HOOK_ADDR:x}')

        if not args.yes:
            resp = input('Continue? [y/N] ')
            if resp.lower() != 'y':
                print('Aborted.')
                sys.exit(0)

        if args.backend == 'openocd':
            _run_openocd_backend(args, code_bin, data_files, tbl_path, sectors)
        else:
            _run_bmp_backend(args, code_bin, data_files, tbl_path, tmpdir)


def _run_openocd_backend(args, code_bin, data_files, tbl_path, sectors):
    cfg = args.openocd_cfg
    if not cfg.exists():
        print(f'ERROR: OpenOCD config not found: {cfg}')
        sys.exit(1)

    print(f'\nStarting OpenOCD ({cfg.name})...')
    openocd_proc = subprocess.Popen(
        ['openocd', '-f', str(cfg)],
        stdout=subprocess.PIPE, stderr=subprocess.PIPE
    )

    try:
        if not wait_for_port('localhost', args.tcl_port):
            if openocd_proc.poll() is not None:
                stderr = openocd_proc.stderr.read().decode(errors='replace')
                print(f'ERROR: OpenOCD exited with code {openocd_proc.returncode}')
                print(stderr[-500:] if len(stderr) > 500 else stderr)
            else:
                print(f'ERROR: OpenOCD TCL port {args.tcl_port} not ready after 10s')
            sys.exit(1)
        print(f'  OpenOCD running (TCL port {args.tcl_port})')

        ocd = OpenOCDTcl(port=args.tcl_port)
        try:
            success = run_openocd_restore(ocd, code_bin, data_files, tbl_path, sectors)
            sys.exit(0 if success else 1)
        finally:
            ocd.close()

    finally:
        openocd_proc.terminate()
        try:
            openocd_proc.wait(timeout=5)
        except subprocess.TimeoutExpired:
            openocd_proc.kill()
            openocd_proc.wait()
        print('OpenOCD stopped.')


def _run_bmp_backend(args, code_bin, data_files, tbl_path, tmpdir):
    gdb_script = generate_gdb_script(
        str(code_bin), data_files, tbl_path, port=args.port)

    if args.dry_run:
        print('\n--- Generated GDB script ---')
        print(gdb_script)
        out = FIRMWARE_DIR / 'restore_sector.gdb'
        out.write_text(gdb_script)
        print(f'\nSaved to: {out}', file=sys.stderr)
        return

    gdb_script_path = os.path.join(tmpdir, 'restore.gdb')
    with open(gdb_script_path, 'w') as f:
        f.write(gdb_script)

    gdb_bin = shutil.which('arm-none-eabi-gdb') or shutil.which('gdb-multiarch')
    if not gdb_bin:
        print('ERROR: No ARM GDB found')
        sys.exit(1)

    print(f'\nUsing: {gdb_bin}')
    print(f'Port:  {args.port}')
    print()

    result = subprocess.run(
        [gdb_bin, '-batch', '-nx', '-x', gdb_script_path],
        cwd=str(FIRMWARE_DIR.parent)
    )

    if result.returncode != 0:
        print(f'\nGDB exited with code {result.returncode}')
        sys.exit(1)


if __name__ == '__main__':
    main()
