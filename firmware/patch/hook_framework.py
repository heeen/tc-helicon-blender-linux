#!/usr/bin/env python3
"""
Firmware hook framework for ARM926EJ-S (DICE3) targets.

Automates the generation of function hooks:
  - Reads displaced instruction bytes from firmware body
  - Validates they are safe to relocate (no PC-relative ops)
  - Generates assembly stubs: displaced instruction + call to handler + jump-back
  - Manages patch zone allocation for multiple hooks
  - Applies B/BL trampolines to the firmware binary
  - Computes CRC32 checkwords for bootloader verification

All addresses are SRAM addresses (where code runs after bootloader copy).
SPI and file offsets are derived via the address mapping constants.

Usage:
    from hook_framework import HookEngine, Hook, PatchProject, BinaryPatch

    project = PatchProject(
        hooks=[Hook(name="my_hook", target=0x4FAC, handler="my_handler")],
        spi_image="../blender_spi_flash.bin",
        patched_spi="../blender_spi_patched.bin",
        hook_bin="hooks.bin",
        elf_path="hooks.elf",
        build_dir=".",
        engine_kwargs=dict(patch_zone_start=0x2AA84, patch_zone_size=0x800),
    )
    project.main()
"""

from __future__ import annotations

import struct
import subprocess
import sys
import zlib
from dataclasses import dataclass, field
from pathlib import Path
from typing import NamedTuple


# ── Address mapping ──────────────────────────────────────────────────────────

SRAM_LOAD_ADDR   = 0x200       # Bootloader copies body here
SPI_BODY_START   = 0x40030     # SPI address of body start
SPI_HEADER_ADDR  = 0x40000     # SPI address of primary header (0x30 bytes)
SPI_HEADER_SIZE  = 0x30
SECTOR_SIZE      = 0x1000


def sram_to_file(addr: int) -> int:
    """SRAM address → file offset within body."""
    return addr - SRAM_LOAD_ADDR


def file_to_sram(off: int) -> int:
    """File offset within body → SRAM address."""
    return off + SRAM_LOAD_ADDR


def sram_to_spi(addr: int) -> int:
    """SRAM address → SPI flash address."""
    return addr - SRAM_LOAD_ADDR + SPI_BODY_START


def spi_to_sram(addr: int) -> int:
    """SPI flash address → SRAM address."""
    return addr - SPI_BODY_START + SRAM_LOAD_ADDR


# ── ARM instruction analysis ────────────────────────────────────────────────

def check_pc_relative_arm(word: int) -> str | None:
    """Check if a 32-bit ARM instruction uses PC-relative addressing.

    Returns a description string if PC-relative, None if safe to relocate.
    ARM926EJ-S: PC reads as current instruction address + 8.
    """
    cond = (word >> 28) & 0xF

    # B / BL: cccc 101L oooo oooo oooo oooo oooo oooo
    if (word & 0x0E000000) == 0x0A000000:
        link = (word >> 24) & 1
        return "BL (branch with link)" if link else "B (branch)"

    # BX / BLX register: cccc 0001 0010 xxxx xxxx xxxx 00L1 xxxx
    if (word & 0x0FFFFFD0) == 0x012FFF10:
        # BX Rm — safe to relocate (register-indirect, not PC-relative)
        # unless Rm is PC (rare/useless)
        rm = word & 0xF
        if rm == 15:
            return "BX PC"
        return None

    # Data processing (ALU): cccc 00I opcode S Rn Rd operand2
    if (word & 0x0C000000) == 0x00000000:
        rn = (word >> 16) & 0xF
        rd = (word >> 12) & 0xF
        is_imm = (word >> 25) & 1

        # Check Rn == PC (source register)
        opcode = (word >> 21) & 0xF
        # MOV/MVN don't use Rn, TST/TEQ/CMP/CMN don't write Rd
        uses_rn = opcode not in (0xD, 0xF)  # MOV, MVN
        if uses_rn and rn == 15:
            return f"ALU op with Rn=PC (opcode={opcode:#x})"

        # Check Rm == PC in register operand2
        if not is_imm:
            rm = word & 0xF
            if rm == 15:
                return f"ALU op with Rm=PC (opcode={opcode:#x})"

    # LDR/STR: cccc 01I P U B W L Rn Rd offset
    if (word & 0x0C000000) == 0x04000000:
        rn = (word >> 16) & 0xF
        if rn == 15:
            load = (word >> 20) & 1
            return "LDR Rd,[PC,#imm]" if load else "STR Rd,[PC,#imm]"

    # LDR/STR halfword/signed: cccc 000P U I W L Rn Rd xxxx 1SS1 xxxx
    if (word & 0x0E000090) == 0x00000090:
        rn = (word >> 16) & 0xF
        if rn == 15:
            return "LDRH/STRH/LDRSB/LDRSH with Rn=PC"

    # LDM/STM: cccc 100P U S W L Rn register_list
    if (word & 0x0E000000) == 0x08000000:
        reg_list = word & 0xFFFF
        if reg_list & (1 << 15):
            load = (word >> 20) & 1
            if load:
                return "LDM with PC in register list"

    # MCR/MRC with PC — extremely rare, skip for now

    return None


def validate_displaced(words: list[tuple[int, int]]) -> list[str]:
    """Validate that all instruction words can be safely displaced.

    Args:
        words: list of (sram_addr, instruction_word) tuples.

    Returns:
        List of error strings (empty if all safe).
    """
    errors = []
    for addr, word in words:
        reason = check_pc_relative_arm(word)
        if reason:
            errors.append(f"  0x{addr:08X} [{word:08X}]: PC-relative — {reason}")
    return errors


# ── ARM branch encoding ─────────────────────────────────────────────────────

def encode_arm_b(from_addr: int, to_addr: int, cond: int = 0xE) -> int:
    """Encode an ARM B (branch) instruction.

    ARM pipeline: PC = addr + 8, so offset = to - from - 8.
    Encoding: cccc 1010 oooo oooo oooo oooo oooo oooo
    Offset field: signed 24-bit, shifted left 2 (±32MB range).
    """
    offset = to_addr - from_addr - 8
    if offset < -(1 << 25) or offset >= (1 << 25):
        raise ValueError(f"B offset {offset:#x} out of range (±32MB)")
    if offset & 3:
        raise ValueError(f"B target must be word-aligned (offset={offset:#x})")
    imm24 = (offset >> 2) & 0x00FFFFFF
    return (cond << 28) | 0x0A000000 | imm24


def encode_arm_bl(from_addr: int, to_addr: int, cond: int = 0xE) -> int:
    """Encode an ARM BL (branch with link) instruction.

    Same as B but with L bit set.
    Encoding: cccc 1011 oooo oooo oooo oooo oooo oooo
    """
    offset = to_addr - from_addr - 8
    if offset < -(1 << 25) or offset >= (1 << 25):
        raise ValueError(f"BL offset {offset:#x} out of range (±32MB)")
    if offset & 3:
        raise ValueError(f"BL target must be word-aligned (offset={offset:#x})")
    imm24 = (offset >> 2) & 0x00FFFFFF
    return (cond << 28) | 0x0B000000 | imm24


def decode_arm_branch_target(word: int, addr: int) -> int | None:
    """Decode B/BL target address. Returns None if not a branch."""
    if (word & 0x0E000000) != 0x0A000000:
        return None
    imm24 = word & 0x00FFFFFF
    # Sign-extend 24-bit to 32-bit
    if imm24 & 0x800000:
        imm24 |= 0xFF000000
        imm24 = imm24 - 0x100000000  # make negative
    return addr + 8 + (imm24 << 2)


def is_arm_bl(word: int) -> bool:
    """Check if word is a BL instruction (any condition)."""
    return (word & 0x0F000000) == 0x0B000000


def is_arm_b(word: int) -> bool:
    """Check if word is a B instruction (any condition, not BL)."""
    return (word & 0x0F000000) == 0x0A000000


# ── CRC32 ────────────────────────────────────────────────────────────────────

def compute_crc_checkword(body_without_crc: bytes) -> bytes:
    """Compute 4-byte CRC checkword that makes CRC32(body + checkword) == 0xFFFFFFFF.

    The DICE3 bootloader verifies CRC32 of the entire body (content + checkword).
    A valid image has CRC32 == 0xFFFFFFFF after processing all bytes.

    Uses meet-in-the-middle: forward 2 bytes from CRC state, reverse 2 bytes
    from target, find matching intermediate state. O(65536) time.
    """
    # zlib.crc32(data, v) uses internal register = v ^ 0xFFFFFFFF.
    # We work with internal registers directly for the table-based computation.
    crc_zlib = zlib.crc32(body_without_crc, 0xFFFFFFFF) & 0xFFFFFFFF
    crc = crc_zlib ^ 0xFFFFFFFF       # internal register after body
    target = 0xFFFFFFFF ^ 0xFFFFFFFF  # = 0 (want zlib to return 0xFFFFFFFF)

    # Build CRC32 table (poly 0xEDB88320, reflected)
    table = []
    for i in range(256):
        c = i
        for _ in range(8):
            c = (0xEDB88320 ^ (c >> 1)) if (c & 1) else (c >> 1)
        table.append(c)

    # Reverse lookup: top byte of table entry → table index
    # CRC-32 table entries have unique top bytes (property of the polynomial)
    top_to_idx = [0] * 256
    for i in range(256):
        top_to_idx[(table[i] >> 24) & 0xFF] = i

    # Forward pass: all possible states after processing bytes b0, b1
    forward: dict[int, tuple[int, int]] = {}
    for b0 in range(256):
        s1 = table[(crc ^ b0) & 0xFF] ^ (crc >> 8)
        for b1 in range(256):
            s2 = table[(s1 ^ b1) & 0xFF] ^ (s1 >> 8)
            if s2 not in forward:
                forward[s2] = (b0, b1)

    # Backward pass: reverse 2 bytes from target, match against forward set
    # Reverse step: given s_new, choose lo_byte (bottom byte of s_old)
    #   idx = top_to_idx[(s_new >> 24)]  (unique, from table top-byte property)
    #   s_old = ((s_new ^ table[idx]) << 8) | lo_byte
    #   byte_processed = idx ^ lo_byte
    idx3 = top_to_idx[(target >> 24) & 0xFF]
    for lo3 in range(256):
        s3 = ((target ^ table[idx3]) << 8) | lo3
        b3 = idx3 ^ lo3

        idx2 = top_to_idx[(s3 >> 24) & 0xFF]
        for lo2 in range(256):
            s2 = ((s3 ^ table[idx2]) << 8) | lo2
            b2 = idx2 ^ lo2

            if s2 in forward:
                b0, b1 = forward[s2]
                result = bytes([b0, b1, b2, b3])
                check = zlib.crc32(body_without_crc + result, 0xFFFFFFFF) & 0xFFFFFFFF
                if check != 0xFFFFFFFF:
                    raise RuntimeError(f"CRC checkword verification failed: {check:#010x}")
                return result

    raise RuntimeError("No CRC checkword found (should not happen)")


# ── SPI image helpers ────────────────────────────────────────────────────────

def read_spi_body(spi_path: Path) -> tuple[bytes, int]:
    """Read firmware body from SPI image.

    Returns (body_bytes, size_words) where body includes the CRC checkword.
    """
    spi = spi_path.read_bytes()
    if len(spi) < SPI_HEADER_ADDR + 8:
        raise ValueError(f"SPI image too small: {len(spi)} bytes")

    magic, size_words = struct.unpack_from('<II', spi, SPI_HEADER_ADDR)
    body_bytes = size_words * 4
    body = spi[SPI_BODY_START:SPI_BODY_START + body_bytes]
    if len(body) < body_bytes:
        raise ValueError(f"SPI image truncated: need {body_bytes} body bytes, "
                         f"have {len(body)}")
    return body, size_words


def write_spi_image(spi_path: Path, output_path: Path,
                    new_body: bytes, new_size_words: int) -> None:
    """Write patched SPI image with updated body and header size_words."""
    spi = bytearray(spi_path.read_bytes())

    # Update size_words in header
    struct.pack_into('<I', spi, SPI_HEADER_ADDR + 4, new_size_words)

    # Write body
    end = SPI_BODY_START + len(new_body)
    if end > len(spi):
        spi.extend(b'\xff' * (end - len(spi)))
    spi[SPI_BODY_START:SPI_BODY_START + len(new_body)] = new_body

    output_path.write_bytes(spi)


# ── Hook definition ──────────────────────────────────────────────────────────

@dataclass
class Hook:
    """Definition of a single function hook.

    Attributes:
        name:       Unique identifier (used in generated labels).
        target:     SRAM address of the instruction to hook.
        handler:    C/asm label of the handler function.
        displace:   Bytes to displace (default 4 = one ARM instruction).
        mode:       "before"  — call handler, then execute original (always)
                    "filter"  — call handler; if r0==0 execute original, else return
                    "replace" — handler IS the replacement (no original execution)
    """
    name: str
    target: int
    handler: str
    displace: int = 4
    mode: str = "before"

    # Populated by engine
    _displaced_bytes: bytes = field(default=b'', repr=False)
    _displaced_words: list = field(default_factory=list, repr=False)
    _is_bl: bool = field(default=False, repr=False)
    _bl_target: int = field(default=0, repr=False)
    _stub_addr: int = 0
    _stub_size: int = 0


# ── Binary patch ─────────────────────────────────────────────────────────────

class BinaryPatch(NamedTuple):
    """A single binary patch to apply to the firmware body.

    addr:       SRAM address of the patch site.
    old_bytes:  Expected original bytes (for verification).
    new_bytes:  Replacement bytes.
    desc:       Human-readable description.
    """
    addr: int
    old_bytes: bytes
    new_bytes: bytes
    desc: str


# ── Hook engine ──────────────────────────────────────────────────────────────

class HookEngine:
    """Manages multiple firmware hooks: validation, stub generation, patching.

    Operates on the firmware body (the bytes copied to SRAM by bootloader).
    All addresses are SRAM addresses.
    """

    def __init__(self, body: bytes, *,
                 patch_zone_start: int,
                 patch_zone_size: int):
        """
        Args:
            body:              Firmware body bytes (content + CRC, as in SPI image).
            patch_zone_start:  First usable SRAM address for stubs/handlers.
            patch_zone_size:   Size of the patch zone in bytes.
        """
        self.body = bytearray(body)
        self.hooks: list[Hook] = []
        self.patch_zone_start = patch_zone_start
        self.patch_zone_end = patch_zone_start + patch_zone_size
        self.patch_zone_size = patch_zone_size
        self._alloc_ptr = patch_zone_start

        print(f"Body: {len(self.body)} bytes (0x{len(self.body):X})")
        print(f"Patch zone: 0x{patch_zone_start:05X}–0x{self.patch_zone_end:05X} "
              f"({patch_zone_size} bytes)")

    def read_word(self, sram_addr: int) -> int:
        """Read a 32-bit word from body at given SRAM address."""
        off = sram_to_file(sram_addr)
        return struct.unpack_from('<I', self.body, off)[0]

    def write_word(self, sram_addr: int, value: int) -> None:
        """Write a 32-bit word to body at given SRAM address."""
        off = sram_to_file(sram_addr)
        struct.pack_into('<I', self.body, off, value)

    def add_hook(self, hook: Hook) -> None:
        """Add a hook, read displaced bytes, validate, and allocate stub space."""
        off = sram_to_file(hook.target)
        if off < 0 or off + hook.displace > len(self.body):
            raise ValueError(f"Hook '{hook.name}': target 0x{hook.target:05X} "
                             f"outside body range")

        if hook.displace % 4 != 0:
            raise ValueError(f"Hook '{hook.name}': displace must be multiple of 4 "
                             f"(ARM instructions are 4 bytes)")

        # Read displaced instruction(s)
        hook._displaced_bytes = bytes(self.body[off:off + hook.displace])
        hook._displaced_words = [
            (hook.target + i, struct.unpack_from('<I', hook._displaced_bytes, i)[0])
            for i in range(0, hook.displace, 4)
        ]

        # Check if first instruction is BL (special handling needed)
        first_word = hook._displaced_words[0][1]
        if is_arm_bl(first_word):
            hook._is_bl = True
            hook._bl_target = decode_arm_branch_target(first_word, hook.target)
            if hook.displace > 4:
                raise ValueError(
                    f"Hook '{hook.name}': BL displacement only supports 4 bytes "
                    f"(got {hook.displace})")

        # Validate PC-relative safety (skip B/BL — handled specially above)
        check_words = [(a, w) for a, w in hook._displaced_words
                       if not (is_arm_b(w) or is_arm_bl(w))]
        errors = validate_displaced(check_words)
        if errors:
            raise ValueError(
                f"Hook '{hook.name}': cannot safely displace at "
                f"0x{hook.target:05X}:\n" + '\n'.join(errors))

        # Estimate stub size
        if hook.mode == "replace":
            hook._stub_size = 4  # single B instruction
        elif hook.mode == "before":
            if hook._is_bl:
                # push {lr}, bl handler, pop {lr}, b <bl_target>
                hook._stub_size = 16
            else:
                # push {lr}, bl handler, pop {lr}, <displaced>, b <target+4>
                hook._stub_size = 12 + hook.displace + 4
        else:  # filter
            if hook._is_bl:
                # push {r0-r3,r12,lr}, bl handler, cmp, pop, beq, bx lr, b <bl_target>
                hook._stub_size = 28
            else:
                # push {r0-r3,r12,lr}, bl handler, cmp, pop, beq, bx lr, <displaced>, b <target+4>
                hook._stub_size = 24 + hook.displace + 4

        # Align to 4 bytes
        hook._stub_size = (hook._stub_size + 3) & ~3

        # Allocate in patch zone
        hook._stub_addr = self._alloc_ptr
        self._alloc_ptr += hook._stub_size
        if self._alloc_ptr > self.patch_zone_end:
            raise ValueError(
                f"Hook '{hook.name}': patch zone exhausted "
                f"(need {self._alloc_ptr - self.patch_zone_start} bytes, "
                f"have {self.patch_zone_size})")

        self.hooks.append(hook)

        desc = f"[{first_word:08X}]"
        if hook._is_bl:
            desc += f" bl 0x{hook._bl_target:05X}"
        print(f"  Hook '{hook.name}': 0x{hook.target:05X} → stub@0x{hook._stub_addr:05X} "
              f"(mode={hook.mode}, {desc})")

    def generate(self, output_path: Path | str) -> str:
        """Generate ARM assembly source with all hook stubs.

        Returns the assembly source as a string.
        """
        lines = [
            "/* Auto-generated by hook_framework.py — do not edit manually. */",
            "",
            "    .arm",
            "    .cpu arm926ej-s",
            "",
        ]

        for hook in self.hooks:
            lines.extend(self._gen_stub(hook))

        src = '\n'.join(lines) + '\n'
        Path(output_path).write_text(src)
        print(f"Generated: {output_path} ({len(src)} bytes, {len(self.hooks)} stubs)")
        return src

    def _gen_stub(self, hook: Hook) -> list[str]:
        """Generate assembly for one hook stub."""
        target = hook.target
        jumpback = target + hook.displace

        lines = [
            f"/* ── Hook: {hook.name} ──────────────────── */",
            f"/* Target: 0x{target:05X}, mode: {hook.mode} */",
        ]
        if hook._is_bl:
            lines.append(f"/* Displaced: bl 0x{hook._bl_target:05X} */")
        lines.append(f"/* Jump-back: 0x{jumpback:05X} */")
        lines += [
            "",
            f"    .section .text.hook_{hook.name}, \"ax\", %progbits",
            f"    .global _hook_{hook.name}_stub",
            f"    .type _hook_{hook.name}_stub, %function",
            f"    .align 2",
            f"",
            f"_hook_{hook.name}_stub:",
        ]

        if hook.mode == "replace":
            lines += [
                f"    b     {hook.handler}",
                f"    .size _hook_{hook.name}_stub, . - _hook_{hook.name}_stub",
                "",
            ]
            return lines

        if hook.mode == "before":
            lines += [
                f"    stmfd sp!, {{lr}}",
                f"    bl    {hook.handler}",
                f"    ldmfd sp!, {{lr}}",
            ]
            if hook._is_bl:
                # BL displaced: tail-call original target (LR set by trampoline)
                lines.append(f"    b     FUN_{hook._bl_target:x}")
            else:
                # Execute displaced instruction(s), then jump back
                for _addr, word in hook._displaced_words:
                    lines.append(f"    .word 0x{word:08X}    /* displaced */")
                lines.append(f"    ldr   pc, ={jumpback}")
                lines.append(f"    .ltorg")
            lines += [
                f"    .size _hook_{hook.name}_stub, . - _hook_{hook.name}_stub",
                "",
            ]
            return lines

        # filter mode
        lines += [
            f"    /* Save caller-saved regs + lr */",
            f"    stmfd sp!, {{r0-r3, r12, lr}}",
            f"    bl    {hook.handler}",
            f"    cmp   r0, #0",
            f"    ldmfd sp!, {{r0-r3, r12, lr}}",
            f"    beq   .L_{hook.name}_passthrough",
            f"    /* Handler intercepted — return to caller */",
            f"    bx    lr",
            f"",
            f".L_{hook.name}_passthrough:",
        ]
        if hook._is_bl:
            lines.append(f"    b     FUN_{hook._bl_target:x}")
        else:
            for _addr, word in hook._displaced_words:
                lines.append(f"    .word 0x{word:08X}    /* displaced */")
            lines.append(f"    ldr   pc, ={jumpback}")
            lines.append(f"    .ltorg")
        lines += [
            f"    .size _hook_{hook.name}_stub, . - _hook_{hook.name}_stub",
            "",
        ]
        return lines

    def generate_linker_script(self, output_path: Path | str, handler_sources: list[str],
                               extra_symbols: str = "") -> str:
        """Generate linker script for the hook binary.

        Args:
            output_path:     Where to write the .ld file.
            handler_sources: List of source files (for comment only).
            extra_symbols:   Additional linker symbols (e.g. INCLUDE directive).
        """
        # Calculate required patch zone size from actual allocations
        used = self._alloc_ptr - self.patch_zone_start
        # Add generous padding for handler code
        zone_len = max(self.patch_zone_size, used + 2048)

        lines = [
            f"/* Auto-generated by hook_framework.py — do not edit manually. */",
            f"",
        ]
        if extra_symbols:
            lines.append(extra_symbols)
            lines.append("")

        lines += [
            f"ENTRY(_hook_{self.hooks[0].name}_stub)" if self.hooks else "/* no hooks */",
            f"",
            f"MEMORY {{",
            f"    PATCH (rwx) : ORIGIN = 0x{self.patch_zone_start:05X}, LENGTH = {zone_len}",
            f"}}",
            f"",
            f"SECTIONS {{",
        ]

        # Each hook stub gets its own section for ordering
        for hook in self.hooks:
            lines += [
                f"    .text.hook_{hook.name} 0x{hook._stub_addr:05X} : {{",
                f"        *(.text.hook_{hook.name})",
                f"    }} > PATCH",
                f"",
            ]

        lines += [
            f"    .text : {{",
            f"        *(.text*)",
            f"        *(.rodata*)",
            f"    }} > PATCH",
            f"",
            f"    .data : {{",
            f"        *(.data*)",
            f"    }} > PATCH",
            f"",
            f"    .bss : {{",
            f"        *(.bss*)",
            f"        *(COMMON)",
            f"    }} > PATCH",
            f"",
            f"    /DISCARD/ : {{",
            f"        *(.ARM.attributes)",
            f"        *(.ARM.exidx*)",
            f"        *(.comment)",
            f"    }}",
            f"}}",
            f"",
        ]

        src = '\n'.join(lines)
        Path(output_path).write_text(src)
        print(f"Generated: {output_path}")
        return src

    def patch_body(self, hook_bin: bytes) -> bytes:
        """Apply hooks to body: splice compiled stubs and write trampolines.

        Returns the new body (content + CRC) ready for SPI write.
        """
        # Work on body content (everything except CRC)
        content = bytearray(self.body[:-4])

        # Extend body to fit patch zone
        patch_file_off = sram_to_file(self.patch_zone_start)
        if patch_file_off > len(content):
            content.extend(b'\x00' * (patch_file_off - len(content)))

        # Splice hook binary at patch zone start
        end_off = patch_file_off + len(hook_bin)
        if end_off > len(content):
            content.extend(b'\x00' * (end_off - len(content)))
        content[patch_file_off:patch_file_off + len(hook_bin)] = hook_bin
        print(f"Spliced hook binary: {len(hook_bin)} bytes at SRAM "
              f"0x{self.patch_zone_start:05X}")

        # Pad content to 4-byte alignment (required: bootloader reads size_words*4)
        pad = (4 - len(content) % 4) % 4
        if pad:
            content.extend(b'\x00' * pad)

        # Write trampolines for each hook
        for hook in self.hooks:
            off = sram_to_file(hook.target)

            if hook._is_bl:
                # BL hook: replace with BL <stub> (preserves LR semantics)
                tramp = encode_arm_bl(hook.target, hook._stub_addr)
            else:
                # Non-BL hook: replace with B <stub>
                tramp = encode_arm_b(hook.target, hook._stub_addr)

            # Verify original bytes still intact
            current = struct.unpack_from('<I', content, off)[0]
            expected = hook._displaced_words[0][1]
            if current != expected:
                print(f"WARNING: Hook '{hook.name}': word at 0x{hook.target:05X} "
                      f"is {current:#010x}, expected {expected:#010x}. Already patched?")

            struct.pack_into('<I', content, off, tramp)
            insn = "BL" if hook._is_bl else "B"
            print(f"  Trampoline: 0x{hook.target:05X} → {insn} 0x{hook._stub_addr:05X} "
                  f"({tramp:#010x})")

        # Compute new CRC
        new_crc = compute_crc_checkword(bytes(content))
        new_body = bytes(content) + new_crc

        # Verify
        check = zlib.crc32(new_body, 0xFFFFFFFF) & 0xFFFFFFFF
        if check != 0xFFFFFFFF:
            raise RuntimeError(f"CRC verification failed: {check:#010x}")
        print(f"CRC: OK (body = {len(new_body)} bytes, "
              f"size_words = {len(new_body) // 4:#x})")

        return new_body

    def summary(self) -> str:
        lines = [f"", f"Hooks ({len(self.hooks)}):"]
        for h in self.hooks:
            lines.append(
                f"  {h.name:24s}  0x{h.target:05X} → stub@0x{h._stub_addr:05X}  "
                f"mode={h.mode}  handler={h.handler}")
        used = self._alloc_ptr - self.patch_zone_start
        lines.append(f"Patch zone: {used}/{self.patch_zone_size} bytes "
                     f"({used * 100 // self.patch_zone_size}%)")
        return '\n'.join(lines)


# ── OpenOCD client (for JTAG injection) ─────────────────────────────────────

class OpenOCDClient:
    """TCL socket interface to a running OpenOCD instance."""

    def __init__(self, host='localhost', port=6666, timeout=30):
        import socket
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

    def resume(self):
        return self.cmd('resume')

    def mdw(self, addr: int, count: int = 1) -> list[int]:
        result = self.cmd(f'mdw {addr:#x} {count}')
        values = []
        for line in result.splitlines():
            if ':' in line:
                values.extend(int(w, 16) for w in line.split(':', 1)[1].strip().split())
        return values

    def mww(self, addr: int, value: int):
        return self.cmd(f'mww {addr:#x} {value:#x}')

    def load_image(self, path: str, addr: int):
        return self.cmd(f'load_image {path} {addr:#x} bin')

    def invalidate_icache(self):
        self.cmd("arm mcr 15 0 7 5 0 0")

    def close(self):
        self.sock.close()


# ── Patch project ────────────────────────────────────────────────────────────

class PatchProject:
    """Top-level project for a DICE3 firmware patch.

    Commands: validate, generate, patch, inject
    """

    def __init__(
        self,
        hooks: list[Hook],
        binary_patches: list[BinaryPatch] | None = None,
        spi_image: str | Path = "",
        patched_spi: str | Path = "",
        hook_bin: str | Path = "hooks.bin",
        elf_path: str | Path = "hooks.elf",
        build_dir: str | Path = ".",
        engine_kwargs: dict | None = None,
        firmware_symbols: str | Path | None = None,
        identity_check: tuple[int, int] | None = None,
        dcp_handler_list: int | None = None,
    ):
        self.hooks = hooks
        self.binary_patches = binary_patches or []
        self.spi_image = Path(spi_image) if spi_image else None
        self.patched_spi = Path(patched_spi) if patched_spi else None
        self.hook_bin = Path(hook_bin)
        self.elf_path = Path(elf_path)
        self.build_dir = Path(build_dir)
        self.hooks_asm = self.build_dir / "hooks_gen.S"
        self.patch_ld = self.build_dir / "patch.ld"
        self.engine_kwargs = engine_kwargs or {}
        self.firmware_symbols = firmware_symbols
        self.identity_check = identity_check
        self.dcp_handler_list = dcp_handler_list

    def _load_body(self, require_crc: bool = False) -> bytes:
        if not self.spi_image or not self.spi_image.exists():
            raise FileNotFoundError(f"SPI image not found: {self.spi_image}")
        body, size_words = read_spi_body(self.spi_image)
        crc = zlib.crc32(body, 0xFFFFFFFF) & 0xFFFFFFFF
        self._crc_ok = (crc == 0xFFFFFFFF)
        print(f"SPI image: {self.spi_image}")
        print(f"  Body: {len(body)} bytes (size_words={size_words:#x})")
        print(f"  CRC32: {crc:#010x} {'OK' if self._crc_ok else 'FAIL'}")
        if not self._crc_ok:
            if require_crc:
                raise ValueError("Body CRC check failed — image may be corrupt")
            print("  WARNING: CRC mismatch (SPI image was modified during recovery)")
            print("  Hook validation will proceed but patch command needs a valid image")
        return body

    def _build_engine(self, require_crc: bool = False) -> HookEngine:
        body = self._load_body(require_crc=require_crc)
        engine = HookEngine(body, **self.engine_kwargs)
        for hook in self.hooks:
            engine.add_hook(hook)
        return engine

    def read_elf_symbols(self) -> dict[str, int]:
        """Read symbol addresses from ELF via nm."""
        if not self.elf_path.exists():
            return {}
        result = subprocess.run(
            ['arm-none-eabi-nm', str(self.elf_path)],
            capture_output=True, text=True)
        if result.returncode != 0:
            return {}
        symbols: dict[str, int] = {}
        for line in result.stdout.strip().split('\n'):
            parts = line.strip().split()
            if len(parts) == 3:
                symbols[parts[2]] = int(parts[0], 16)
        return symbols

    def fix_stub_addresses(self, engine: HookEngine, symbols: dict[str, int]) -> None:
        """Fix hook stub addresses from actual ELF symbols."""
        for hook in engine.hooks:
            sym = f"_hook_{hook.name}_stub"
            if sym in symbols:
                actual = symbols[sym]
                if hook._stub_addr != actual:
                    print(f"  Fix {hook.name} stub: "
                          f"0x{hook._stub_addr:05X} → 0x{actual:05X}")
                    hook._stub_addr = actual

    def cmd_validate(self) -> None:
        engine = self._build_engine()
        print(engine.summary())
        print("\nAll hook points validated OK.")

    def cmd_generate(self) -> None:
        engine = self._build_engine()

        # Generate assembly stubs
        engine.generate(self.hooks_asm)

        # Generate linker script
        extra = ""
        if self.firmware_symbols:
            extra = f'INCLUDE "{self.firmware_symbols}"'
        engine.generate_linker_script(self.patch_ld, [], extra_symbols=extra)

        print(engine.summary())
        print(f"\nNext: write handlers in handlers.c, then: make && {sys.argv[0]} patch")

    def cmd_patch(self) -> None:
        if not self.hook_bin.exists():
            print(f"ERROR: {self.hook_bin} not found. Run 'make' first.",
                  file=sys.stderr)
            sys.exit(1)

        engine = self._build_engine(require_crc=True)
        symbols = self.read_elf_symbols()
        self.fix_stub_addresses(engine, symbols)

        hook_bin_data = self.hook_bin.read_bytes()
        new_body = engine.patch_body(hook_bin_data)

        # Apply binary patches
        if self.binary_patches:
            body_arr = bytearray(new_body[:-4])  # strip CRC
            for bp in self.binary_patches:
                off = sram_to_file(bp.addr)
                current = body_arr[off:off + len(bp.old_bytes)]
                if current != bp.old_bytes:
                    print(f"WARNING: bytes at 0x{bp.addr:05X} are "
                          f"{current.hex()}, expected {bp.old_bytes.hex()}")
                body_arr[off:off + len(bp.new_bytes)] = bp.new_bytes
                print(f"  Patch: 0x{bp.addr:05X} [{bp.old_bytes.hex()}→"
                      f"{bp.new_bytes.hex()}] {bp.desc}")
            # Recompute CRC
            new_crc = compute_crc_checkword(bytes(body_arr))
            new_body = bytes(body_arr) + new_crc

        new_size_words = len(new_body) // 4

        # Write patched SPI image
        if self.patched_spi and self.spi_image:
            write_spi_image(self.spi_image, self.patched_spi, new_body, new_size_words)
            print(f"\nWrote: {self.patched_spi} "
                  f"(size_words={new_size_words:#x})")

        # Also write body-only file for JTAG injection
        body_bin = self.build_dir / "patched_body.bin"
        body_bin.write_bytes(new_body)

        print(engine.summary())
        print(self._size_report(engine))

    def cmd_inject(self) -> None:
        """JTAG injection: write stubs + handlers to SRAM, patch hook live."""
        if not self.hook_bin.exists():
            print(f"ERROR: {self.hook_bin} not found. Run 'make' first.",
                  file=sys.stderr)
            sys.exit(1)

        engine = self._build_engine()
        symbols = self.read_elf_symbols()
        self.fix_stub_addresses(engine, symbols)

        print("\n" + "=" * 60)
        print("JTAG Injection — Live SRAM Patching")
        print("=" * 60)

        ocd = OpenOCDClient()
        try:
            self._do_inject(ocd, engine)
        finally:
            ocd.close()

    def _do_inject(self, ocd: OpenOCDClient, engine: HookEngine) -> None:
        import time

        ocd.halt()
        time.sleep(0.2)

        # Identity check
        if self.identity_check:
            addr, expected = self.identity_check
            actual = ocd.mdw(addr)[0]
            print(f"\nIdentity check @ 0x{addr:05X}: {actual:#010x} "
                  f"(expect {expected:#010x})")
            if actual != expected:
                print("[!] Firmware mismatch — aborting")
                ocd.resume()
                return
            print("  OK")

        # Load hook binary to patch zone
        hook_bin_path = str(self.hook_bin.resolve())
        print(f"\nLoading hook binary to SRAM 0x{engine.patch_zone_start:05X}...")
        result = ocd.load_image(hook_bin_path, engine.patch_zone_start)
        print(f"  {result}")

        # Verify first word
        check = ocd.mdw(engine.patch_zone_start)[0]
        print(f"  Verify: 0x{engine.patch_zone_start:05X} = {check:#010x}")

        # Clear BSS (done_flag, mailbox, handler node — must be zero for init)
        # Read BSS section bounds from ELF via objdump
        import subprocess as _sp
        _r = _sp.run(['arm-none-eabi-objdump', '-h', str(self.elf_path)],
                     capture_output=True, text=True)
        for line in _r.stdout.splitlines():
            if '.bss' in line:
                parts = line.split()
                # Format: idx name size vma lma offset align
                bss_size = int(parts[2], 16)
                bss_addr = int(parts[3], 16)
                print(f"\nClearing BSS: 0x{bss_addr:05X} ({bss_size} bytes)")
                for addr in range(bss_addr, bss_addr + bss_size, 4):
                    ocd.mww(addr, 0)
                break

        # Write trampolines
        print("\nWriting trampolines...")
        for hook in engine.hooks:
            if hook._is_bl:
                tramp = encode_arm_bl(hook.target, hook._stub_addr)
            else:
                tramp = encode_arm_b(hook.target, hook._stub_addr)
            ocd.mww(hook.target, tramp)
            insn = "BL" if hook._is_bl else "B"
            print(f"  0x{hook.target:05X}: {insn} 0x{hook._stub_addr:05X} ({tramp:#010x})")

        # Direct DCP handler registration via memory writes.
        # The main loop may be blocked in cyg_flag_wait, so the trampoline
        # hook at 0x4FAC may never fire. Register the handler directly by
        # building a node in BSS and linking it into the DCP handler list.
        symbols = self.read_elf_symbols()
        flash_handler_addr = symbols.get('flash_handler')
        node_addr = symbols.get('node.0') or symbols.get('node')
        dcp_handler_list = self.dcp_handler_list or symbols.get('dcp_handler_list')
        if flash_handler_addr and node_addr and dcp_handler_list:
            old_head = ocd.mdw(dcp_handler_list)[0]
            # Build node: {next, category(u16), padding(u16), handler, context}
            ocd.mww(node_addr + 0, old_head)       # next = old list head
            ocd.mww(node_addr + 4, 0x0000081F)      # category=0x81F (u16 LE), padding=0
            ocd.mww(node_addr + 8, flash_handler_addr)  # handler
            ocd.mww(node_addr + 12, 0)              # context = NULL
            # Link into list
            ocd.mww(dcp_handler_list, node_addr)
            print(f"\nDirect DCP registration:")
            print(f"  node@0x{node_addr:05X}: handler=0x{flash_handler_addr:05X} "
                  f"cat=0x81F next=0x{old_head:05X}")
            print(f"  Linked into list at 0x{dcp_handler_list:05X}")

            # Set done_flag so flash_handler_init skips re-registration
            done_flag_addr = symbols.get('done_flag')
            if done_flag_addr:
                ocd.mww(done_flag_addr, 0x444F4E45)  # DONE_MAGIC

            # Install software_reboot as CLI 'reset' callback (BSS pointer at 0x320DC)
            reboot_addr = symbols.get('software_reboot')
            if reboot_addr:
                ocd.mww(0x320DC, reboot_addr)
                print(f"  CLI 'reset' → software_reboot@0x{reboot_addr:05X}")
        else:
            print("\nWARNING: Could not find flash_handler/node symbols for direct DCP registration")

        # Invalidate I-cache + D-cache and resume
        ocd.invalidate_icache()
        ocd.cmd("arm mcr 15 0 7 6 0 0")  # D-cache
        print("\nCaches invalidated, resuming...")
        ocd.resume()

        print("\n" + "=" * 60)
        print("Injection complete — hooks are live")
        print("=" * 60)

    def _size_report(self, engine: HookEngine) -> str:
        if not self.elf_path.exists():
            return ""
        result = subprocess.run(
            ['arm-none-eabi-size', str(self.elf_path)],
            capture_output=True, text=True)
        if result.returncode != 0:
            return ""
        return f"\n{result.stdout.strip()}"

    def main(self) -> None:
        if len(sys.argv) < 2:
            print(f"Usage: {sys.argv[0]} <validate|generate|patch|inject>")
            sys.exit(1)

        cmd = sys.argv[1]
        cmds = {
            'validate': self.cmd_validate,
            'generate': self.cmd_generate,
            'patch': self.cmd_patch,
            'inject': self.cmd_inject,
        }
        if cmd not in cmds:
            print(f"Unknown command: {cmd}")
            print(f"Available: {', '.join(cmds)}")
            sys.exit(1)
        cmds[cmd]()
