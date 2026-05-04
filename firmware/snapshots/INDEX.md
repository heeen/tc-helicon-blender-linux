# Register State Snapshots

Dated JTAG `mdw` register dumps capturing the device's full peripheral
state at specific moments. Used as ground truth for register layouts and
to diff state between boot stages.

## Captures

| File | Date | Chip state | Captured for |
|---|---|---|---|
| `register_state_tcat-boot_20260425T155207.md` | 2026-04-25 15:52 | TCAT-BOOT shell (PC≈0x20000C00, ROM polling loop) | Reference for stage-1 ROM register state — peripherals reset, PLL off |
| `register_state_ecos.md` | 2026-04-21 | eCos firmware running, early vector / task-switch area (PC≈0x0000063C) | Reference for fully-booted register state, contrasted against TCAT-BOOT |
| `register_state_ecos-now_20260425T233205.md` | 2026-04-25 23:32 | eCos firmware later in run (PC=0x2D81C, MIDI loop context) | Snapshot during MIDI activity to confirm long-run register stability |
| `register_state_reboot_20260425T234940.md` | 2026-04-25 23:49 | CPU at PC=0x4 (vector table during reboot exception) | Snapshot of state during reboot teardown for the v2 reboot stub work |

## Usage

These are objective JTAG dumps (each row = address + value at that
moment). They are NOT to be used as canonical register names — for that,
see `firmware/hardware-reference.md`. Several name labels in older
captures (especially in `Registers.md`'s comparison table that derived
from these) are wrong; the actual canonical names come from Ghidra
decompiles of the code that writes those registers.

## Adding new captures

Use `firmware/probe_full_reg_dump.py` and follow the existing
naming convention `register_state_<context>_YYYYMMDDTHHMMSS.md`.
Update this INDEX with a one-line description of the chip state
when captured.
