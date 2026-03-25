#!/usr/bin/env python3
"""Inject DCP flash handler (0x81F) into running firmware via eCos hook.

Uses the same displaced-instruction technique as restore_sector.py:

  1. Let primary firmware boot normally (15s) — full eCos + SPI driver init
  2. JTAG halt
  3. Load handler code (registration shim + flash handler) into SRAM
  4. Clear done flag + mailbox
  5. Patch ONE instruction: at 0x2e48, replace `bl 0x14dc` with `bl 0x31C00`
  6. Invalidate I-cache, resume
  7. eCos main loop calls our shim → registers DCP handler → calls original fn
  8. Poll done flag for completion
  9. Restore original instruction at 0x2e48
 10. Verify: send DCP 0x81F000 (INFO) via USB

After injection, handler stays registered in RAM until power cycle.
Use `blender-ctl usb flash info` to test.

Usage:
    python3 firmware/inject_flash_handler.py                    # OpenOCD (default)
    python3 firmware/inject_flash_handler.py --backend bmp      # BMP probe
    python3 firmware/inject_flash_handler.py --dry-run          # print GDB script
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
from pathlib import Path

FIRMWARE_DIR = Path(__file__).parent
OPENOCD_CFG = FIRMWARE_DIR.parent / 'jtag' / 'miolink-dice3-openocd.cfg'

# ── SRAM layout (must match sram_flash_handler.c) ─────────────────────

CODE_ADDR      = 0x31480   # handler code (DEADBEEF zone, proven working)
DONE_FLAG_ADDR = 0x2B200   # safe from stack growth (26KB below SP)
NODE_ADDR      = 0x2B210   # safe from stack growth
MAILBOX_ADDR   = 0x2B220

DONE_MAGIC     = 0x444F4E45  # "DONE"
MBOX_MAGIC     = 0x444F4E45  # same — set when registration complete

# Hook site — primary firmware event loop
# `bl FUN_2f38` at SRAM 0x4FAC, last call in the main event loop.
# Our shim checks done-flag: first call registers handler, subsequent calls
# skip registration and tail-call the original FUN_2f38.
HOOK_ADDR        = 0x4FAC    # address of `bl 0x2F38` in primary firmware
HOOK_TARGET      = CODE_ADDR # our registration shim
DISPLACED_FN     = 0x2F38    # original target of the hooked bl

# ARM bl encoding: bl from HOOK_ADDR to HOOK_TARGET
_BL_OFFSET       = ((HOOK_TARGET - HOOK_ADDR - 8) >> 2) & 0x00FFFFFF
BL_HOOK_INSN     = 0xEB000000 | _BL_OFFSET

# Original instruction at 0x4FAC: bl 0x2F38
_BL_ORIG_OFFSET  = ((DISPLACED_FN - HOOK_ADDR - 8) >> 2) & 0x00FFFFFF
BL_ORIG_INSN     = 0xEB000000 | _BL_ORIG_OFFSET

# Firmware identity check — first word of sst25xx_aai_write (primary firmware)
FN_AAI_WRITE_ADDR       = 0x8968
FN_AAI_WRITE_FIRST_WORD = 0xe92d4ff0

# Primary firmware addresses (from Ghidra analysis)
FN_DCP_REGISTER_HANDLER = 0x1BEE0
DCP_STATE               = 0x30F78
HANDLER_LIST_OFFSET     = 0x44C   # dcp_state->handler_list at state+0x44C
FLASH_CATEGORY          = 0x81F

# Handler function entry (shim at 0x31480, flash_handler at 0x31500)
HANDLER_FN_ADDR         = 0x31500

# ── Build handler binary ──────────────────────────────────────────────

def build_handler() -> Path:
    """Compile sram_flash_handler.c if needed, return path to .bin."""
    code_bin  = FIRMWARE_DIR / 'sram_flash_handler.bin'
    c_src     = FIRMWARE_DIR / 'sram_flash_handler.c'
    ld_script = FIRMWARE_DIR / 'sram_handler.ld'
    elf       = FIRMWARE_DIR / 'sram_flash_handler.elf'

    needs_build = (
        not code_bin.exists()
        or c_src.stat().st_mtime > code_bin.stat().st_mtime
        or ld_script.stat().st_mtime > code_bin.stat().st_mtime
    )

    if needs_build:
        print('Building SRAM handler (C)...')
        subprocess.run([
            'arm-none-eabi-gcc', '-march=armv5te', '-marm', '-mno-thumb-interwork',
            '-Os', '-nostdlib', '-ffreestanding', '-Wall',
            '-T', str(ld_script),
            '-o', str(elf), str(c_src)
        ], check=True)
        subprocess.run([
            'arm-none-eabi-objcopy', '-O', 'binary', str(elf), str(code_bin)
        ], check=True)

        # Show size info
        result = subprocess.run(
            ['arm-none-eabi-size', str(elf)],
            capture_output=True, text=True
        )
        if result.returncode == 0:
            print(result.stdout.strip())

    print(f'Handler binary: {code_bin} ({code_bin.stat().st_size} bytes)')
    return code_bin


# ── OpenOCD TCL client ─────────────────────────────────────────────────

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

    def close(self):
        self.sock.close()


# ── OpenOCD injection ──────────────────────────────────────────────────

def run_openocd_inject(ocd: OpenOCDTcl, code_bin: Path) -> bool:
    """Load handler to SRAM, register via memory writes only (no code exec)."""

    print()
    print("=" * 60)
    print("DCP Flash Handler Injection — Memory Write Only")
    print("=" * 60)

    # ── Phase 0: Let eCos boot fully ──
    ocd.halt()
    time.sleep(0.2)
    print("\nResuming for 15s (eCos boot + SPI driver init)...")
    ocd.resume()
    time.sleep(15)
    ocd.halt()
    time.sleep(0.2)

    # ── Phase 1: Verify firmware identity (SRAM, post-boot) ──
    try:
        xip_words = ocd.mdw(FN_AAI_WRITE_ADDR)
    except Exception as e:
        print(f"\n[!] Cannot read target memory: {e}")
        return False
    xip_word = xip_words[0] if xip_words else 0
    print(f"\nSRAM @ {FN_AAI_WRITE_ADDR:#x}: {xip_word:#010x} (expect {FN_AAI_WRITE_FIRST_WORD:#010x})")
    if xip_word != FN_AAI_WRITE_FIRST_WORD:
        print("[!] Firmware mismatch — wrong image loaded!")
        return False
    print("  Firmware identity verified OK")

    # ── Phase 2: Load handler code to SRAM ──
    print("\n--- Loading SRAM payload ---")

    result = ocd.load_image(str(code_bin), CODE_ADDR)
    print(f"  Code: {result}")
    code_check = ocd.mdw(CODE_ADDR)[0]
    print(f"  Verify: first word @ {CODE_ADDR:#x} = {code_check:#010x}")

    # ── Phase 3: Build handler node ──
    # Node layout: [next:4, category:2, pad:2, handler:4, context:4]
    print(f"\n--- Building handler node @ {NODE_ADDR:#x} ---")

    # Read current handler list head from dcp_state + 0x44C
    handler_list_addr = DCP_STATE + HANDLER_LIST_OFFSET
    old_head = ocd.mdw(handler_list_addr, 1)[0]
    print(f"  Handler list @ {handler_list_addr:#x}: head={old_head:#010x}")

    # If our node is already the head (re-injection), follow its next pointer
    # to get the real previous head and avoid a self-referencing cycle.
    if old_head == NODE_ADDR:
        old_head = ocd.mdw(NODE_ADDR, 1)[0]  # node->next
        print(f"  Re-injection detected, using node->next={old_head:#010x}")

    # Write node: next = old_head
    ocd.mww(NODE_ADDR, old_head)
    # category = 0x81F, pad = 0
    ocd.mww(NODE_ADDR + 4, FLASH_CATEGORY)
    # handler function pointer
    ocd.mww(NODE_ADDR + 8, HANDLER_FN_ADDR)
    # context = NULL
    ocd.mww(NODE_ADDR + 12, 0)

    print(f"  Node: next={old_head:#010x} cat={FLASH_CATEGORY:#06x} handler={HANDLER_FN_ADDR:#010x}")

    # ── Phase 4: Link node into handler list ──
    # This is the atomic operation: write our node address as the new list head
    print(f"\n--- Linking node into DCP handler list ---")
    ocd.mww(handler_list_addr, NODE_ADDR)

    # Verify
    new_head = ocd.mdw(handler_list_addr, 1)[0]
    print(f"  Handler list head: {new_head:#010x} (expect {NODE_ADDR:#010x})")

    # Verify node contents
    node_words = ocd.mdw(NODE_ADDR, 4)
    print(f"  Node readback: next={node_words[0]:#010x} cat={node_words[1] & 0xFFFF:#06x} "
          f"handler={node_words[2]:#010x} ctx={node_words[3]:#010x}")

    success = (new_head == NODE_ADDR and (node_words[1] & 0xFFFF) == FLASH_CATEGORY)

    # ── Phase 5: Invalidate I-cache and resume ──
    # Critical: SRAM was DEADBEEF before we wrote code. The I-cache may
    # have stale DEADBEEF entries. Must invalidate before the DCP thread
    # tries to execute our handler.
    print("\n--- Invalidating I-cache and resuming ---")
    ocd.cmd("arm mcr 15 0 7 5 0 0")  # MCR p15,0,R0,c7,c5,0 (invalidate entire I-cache)
    print("  I-cache invalidated")
    ocd.resume()

    if success:
        print()
        print("=" * 60)
        print("SUCCESS — DCP handler 0x81F registered")
        print("Test: blender-ctl usb flash info")
        print("Handler lives in SRAM — survives until power cycle")
        print("=" * 60)
    else:
        print()
        print("=" * 60)
        print("FAILED — handler list not updated correctly")
        print("=" * 60)

    return success


# ── BMP GDB script backend ────────────────────────────────────────────

def generate_gdb_script(code_file: str, port: str) -> str:
    """Generate GDB/Python script for BMP backend."""

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
NODE_ADDR    = {NODE_ADDR:#x}
HOOK_ADDR    = {HOOK_ADDR:#x}
BL_HOOK_INSN = {BL_HOOK_INSN:#x}
BL_ORIG_INSN = {BL_ORIG_INSN:#x}
DONE_MAGIC   = {DONE_MAGIC:#x}

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
print("DCP Flash Handler Injection — eCos Hook via BMP")
print("=" * 60)

# Let eCos boot first (firmware body loaded to SRAM by bootloader)
print("\\nResuming for 15s (eCos boot)...")
interrupt_after(15)
gdb.execute("continue")

# Verify firmware identity (SRAM, post-boot)
try:
    word = read_word({FN_AAI_WRITE_ADDR:#x})
    print(f"\\nSRAM @ {FN_AAI_WRITE_ADDR:#x}: {{word:#010x}} (expect {FN_AAI_WRITE_FIRST_WORD:#010x})")
    if word != {FN_AAI_WRITE_FIRST_WORD:#x}:
        print("[!] Firmware mismatch!")
        gdb.execute("detach")
        gdb.execute("quit")
except Exception as e:
    print(f"[!] Cannot read SRAM: {{e}}")
    gdb.execute("quit")
print("Firmware identity verified OK")

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
for attempt in range(10):
    interrupt_after(1)
    gdb.execute("continue")

    done_val = read_word(DONE_FLAG)
    if done_val == DONE_MAGIC:
        print(f"  [{{attempt+1}}] DONE — handler registered!")
        done = True
        break

    mbox = read_word(MAILBOX)
    print(f"  [{{attempt+1}}] done={{done_val:#010x}} mbox={{mbox:#010x}}")

# Restore original instruction
print("\\n--- Restoring original instruction ---")
write_word(HOOK_ADDR, BL_ORIG_INSN)
restored = read_word(HOOK_ADDR)
print(f"  0x{{HOOK_ADDR:x}}: {{restored:#010x}} (restored)")

# Check handler node
print(f"\\n--- Handler node @ 0x{{NODE_ADDR:x}} ---")
node = bytes(inf.read_memory(NODE_ADDR, 16))
next_ptr, cat_pad, func_ptr, ctx_ptr = struct.unpack('<4I', node)
cat = cat_pad & 0xFFFF
print(f"  next:     {{next_ptr:#010x}}")
print(f"  category: {{cat:#06x}}")
print(f"  handler:  {{func_ptr:#010x}}")
print(f"  context:  {{ctx_ptr:#010x}}")

if done:
    print()
    print("=" * 60)
    print("SUCCESS — DCP handler 0x81F registered")
    print("Test: blender-ctl usb flash info")
    print("=" * 60)
else:
    print()
    print("=" * 60)
    print("FAILED — handler registration did not complete")
    print("=" * 60)

end

detach
quit
'''


# ── Shared helpers ─────────────────────────────────────────────────────

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
        description='Inject DCP flash handler via eCos in-context hook')
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
    parser.add_argument('--yes', '-y', action='store_true',
                        help='Skip confirmation prompt')
    args = parser.parse_args()

    code_bin = build_handler()

    print(f'\nReady to inject DCP flash handler (category 0x81F)')
    print(f'  Hook: 0x{HOOK_ADDR:x} bl 0x{DISPLACED_FN:x} -> bl 0x{HOOK_TARGET:x}')
    print(f'  Code: {code_bin.stat().st_size} bytes at 0x{CODE_ADDR:x}')
    print(f'  Node: 0x{NODE_ADDR:x}')
    print(f'  Backend: {args.backend}')
    print(f'  Method: eCos in-context hook (RAM-only, reverts on power cycle)')

    if not args.yes:
        resp = input('\nContinue? [y/N] ')
        if resp.lower() != 'y':
            print('Aborted.')
            sys.exit(0)

    if args.backend == 'openocd':
        _run_openocd_backend(args, code_bin)
    else:
        _run_bmp_backend(args, code_bin)


def _run_openocd_backend(args, code_bin):
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
            success = run_openocd_inject(ocd, code_bin)
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


def _run_bmp_backend(args, code_bin):
    gdb_script = generate_gdb_script(str(code_bin), port=args.port)

    if args.dry_run:
        print('\n--- Generated GDB script ---')
        print(gdb_script)
        out = FIRMWARE_DIR / 'inject_flash_handler.gdb'
        out.write_text(gdb_script)
        print(f'\nSaved to: {out}', file=sys.stderr)
        return

    with tempfile.TemporaryDirectory(prefix='blender_inject_') as tmpdir:
        gdb_script_path = os.path.join(tmpdir, 'inject.gdb')
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
