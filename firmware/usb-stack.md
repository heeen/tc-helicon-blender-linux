# DICE3 USB Stack

Top-level overview of the DICE3 USB controller and Blender firmware's
USB stack. Consolidates the IP-identification + MIDI + register-mapping
content from:

- `dice3_usb_registers.md` (IP fingerprint + register layout)
- `EZR32WG_USB_DWC2.md` (Silicon Labs cross-reference manual)
- `usb_dwc2_mapping.md` (Blender field → DWC2 concept mapping)
- `usb_midi_stack_verified.md` (MIDI class vtable + EP3 fix)

For deeper technical references see:

- `usb_stack_init.md` — cold-boot enumeration call graph (~870 lines)
- `usb_stack_data.md` — data structures, SRAM layout, QH banks (~610 lines)
- `usb_stack_runtime.md` — packet flow, ISR dispatch, FIFO state (~710 lines)

Inline status tag convention: see `hardware-reference.md` header.

---

## 1. IP identification

The USB controller at `0x90000000` is **Synopsys DesignWare USB 2.0 OTG
(DWC2), v3.20a, in device mode**. Earlier docs called it "ChipIdea" or
"TCAT proprietary completion engine" — both wrong. [verified 2026-04-28
GSNPSID readback]

### Evidence

- `GSNPSID @ 0x90000040 = 0x4F54320A` — ASCII `"OT2\n"`, the Synopsys
  DWC2 OTG v3.20a identification signature [verified JTAG]
- `usb_hw_ep_table_init @ 0x214A8` writes the standard DWC2 global
  register layout: `GRSTCTL` self-clearing FIFO flush at `+0x010`,
  `GRXFSIZ` / `GNPTXFSIZ` / `GDFIFOCFG` at `+0x024 / +0x028 / +0x05C`,
  and `GINTMSK` at `+0x018` with the canonical DWC2 device interrupt
  mask `0x708E3C16` (USBRst | EnumDone | IEPInt | OEPInt | …) [verified
  Ghidra]
- `usb_hw_ep_dma_start_rx @ 0x21930` writes per-EP `DOEPCTLn` /
  `DOEPTSIZn` fields (EPENA bit 31, CNAK bit 26, PktCnt bits 28:19,
  XferSize bits 18:0) [verified Ghidra]

The "ChipIdea ENDPTPRIME / ENDPTFLUSH / ENDPTSTAT / ENDPTCOMPLETE /
ENDPTCTRL" registers read as zeros at runtime — not because TCAT
bypassed them, but because **those addresses don't contain ChipIdea
registers in this IP at all** [verified JTAG: zeros from boot, never
written by firmware].

### Cross-reference: EZR32WG datasheet

The Silicon Labs EZR32WG embeds the same Synopsys DWC2 core (EZR32WG-RM
chapter 15, p. 214 ff., explicitly credits Synopsys). EZR32WG's manual
is a more readable DWC2 reference than the Synopsys databook itself.

Differences EZR32WG vs DICE3:
1. **Wrapper**: EZR32WG puts a small Silicon Labs CTRL/STATUS wrapper
   at `+0x000..+0x018`, then the DWC2 core block starts at `+0x3C000`
   within the USB peripheral. DICE3 has **no wrapper** — the DWC2 core
   is mapped directly at `0x90000000`.
2. **PHY**: EZR32WG ships internal FS/LS-only PHY with on-chip 3.3 V
   regulator and VBUS sense. DICE3 has a different (HS-capable) PHY
   external to the DWC2 core (DSTS readouts show HS enumeration).
3. **Clocking**: EZR32WG runs the core at 48 MHz (`HFCORECLKUSBC`).
   DICE3 clocks USB from its own 0xC9 clock block (PLL → 200 MHz AHB).

Everything else — register layout, bit fields, feature set, init
sequence, transfer flows, interrupt handling — is the **same DWC2 core**.
[verified 2026-04-28 register-by-register mapping in
`EZR32WG_USB_DWC2.md` table; see archived file for full xref]

### DMA mode

DICE3 uses DWC2 **buffer-pointer DMA** (the simpler mode), NOT
descriptor DMA (DDMA). Evidence: `DCFG.DescDMA` is cleared at boot;
`DOEPDMAn` registers hold raw buffer pointers, not dTD lists. [verified
EZR32WG-RM p. 214 + `usb_hw_ep_dma_start_rx @ 0x21930` decompile]

The "dTD-like descriptor banks" at SRAM `0x90000B..` mentioned in
older docs are **TCAT-internal data structures**, NOT DWC2-spec
descriptors. The DWC2 core sees only the per-EP `DOEPDMAn` /
`DIEPDMAn` buffer pointers; TCAT firmware maintains its own queue
structures in SRAM that get translated into per-EP buffer-pointer
writes by the dispatch code. [verified Ghidra; conjecture name "dTD"
in older docs was misleading]

---

## 2. Address map summary

```
USB_BASE       = 0x90000000
GLOBAL block   = USB_BASE + 0x000   (DWC2 global registers, +0x000..+0x05C, +0x100..+0x118 TX FIFO size, etc.)
DEVICE block   = USB_BASE + 0x800   (DWC2 device-mode registers DCFG/DCTL/DSTS/DAINT/...)
DIEP bank      = USB_BASE + 0x900   (per-EP IN-direction register windows, stride 0x20)
DOEP bank      = USB_BASE + 0xB00   (per-EP OUT-direction register windows, stride 0x20)
```

[verified Ghidra `dice_usb_regs.h` + EZR32WG-RM p. 215 layout]

There is **no `op_base = USB_BASE + 0x10` operational sub-window** — that
is a ChipIdea/EHCI concept that does not apply here. [verified — older
docs called it that; corrected 2026-04-28]

### EP topology

DICE3 firmware programs exactly **EP1..EP6 in both directions** (IN +
OUT) plus EP0 (control). This matches EZR32WG's documented "6 IN/OUT
endpoints in addition to EP0" — the sharpest IP fingerprint for DWC2.
[verified Ghidra `usb_hw_ep_table_init @ 0x214A8`: hard-coded
`do { … } while (ep_num != 6);` loop]

| EP | Direction | Function (Blender) |
|---|---|---|
| 0 | IN/OUT | Control [verified] |
| 1 | IN/OUT | Audio streaming [verified Ghidra] |
| 2 | IN/OUT | DCP / vendor [verified Ghidra] |
| 3 | OUT | **MIDI bulk OUT (host→device)** — see § 4 [verified] |
| 3 | IN | MIDI bulk IN (device→host) [verified] |
| 4–6 | IN/OUT | Audio sub-streams [verified Ghidra] |

---

## 3. USB DMA architecture (vs the generic 0x80000000 DMA)

DICE3 has **two physically distinct DMA controllers** — see
`hardware-reference.md` § "DMA — two independent controllers". The
USB controller at `0x90000000` has its own internal DMA (used for EP
buffer transfers), separate from the generic AHB DMA at `0x80000000`.

USB registers that LOOK DMA-shaped (`0x90000104..0x90000118`) are
actually **DIEPTXF1..DIEPTXF6** — per-EP TX FIFO size config, NOT DMA
descriptor pointers. [verified 2026-04-28 cross-reference with
EZR32WG-RM]

SPI driver DMA hygiene (`DMA_ICLR`, channel arm semantics) has **no
effect on USB** and vice versa. The two engines are completely
independent. [verified 2026-04-22 live JTAG state]

---

## 4. MIDI class implementation (the critical EP3 path)

### USB class vtable at `0x2A158`

Registered during init. Called by `usb_control_endpoint_thread @ 0x2016C`
for class-specific USB requests.

| Idx | Offset | Address | Name | Trigger |
|---|---|---|---|---|
| 0 | +0x00 | 0x19FB8 | `usb_device_attach_handler` | Bus state (attach/detach/suspend/resume) |
| 1 | +0x04 | 0x196E0 | `vtable_fn_01_get_descriptors` | GET_DESCRIPTOR |
| 2 | +0x08 | 0x196EC | `vtable_fn_02_get_config` | GET_CONFIGURATION |
| 3 | +0x0C | 0x1B230 | `vtable_fn_03_get_string` | String descriptors |
| 4 | +0x10 | 0x1A1F8 | `usb_get_interface_handler` | GET_INTERFACE |
| 5 | +0x14 | 0x196F4 | `vtable_fn_05_get_alt_setting` | Get alt setting |
| 6 | +0x18 | 0x19E28 | `usb_set_interface_dispatch` | SET_INTERFACE (audio alt-settings only) |
| 7 | +0x1C | 0x19BCC | `usb_midi_set_interface_handler` | **SET_CONFIGURATION — arms MIDI EPs** |
| 8 | +0x20 | 0x1B3BC | `usb_audio_ctrl_iface_handler` | Class requests (0x21/0xA1) |

[verified Ghidra]

### MIDI EP arming — critical detail

`usb_midi_streaming_start @ 0x19C40` is **NOT a standalone function**.
It's a mid-body entry point within `usb_midi_set_interface_handler @
0x19BCC`. The function body spans `0x19BCC..0x19D54` with three entry
labels sharing fall-through code:

| Label | Address | Context |
|---|---|---|
| `usb_midi_set_interface_handler` | 0x19BCC | Vtable entry — checks config_value, calls `rx_stream_set_format` |
| `usb_midi_streaming_reconfigure` | 0x19BF8 | Mid-body — builds interrupt + TX EP descriptors |
| `usb_midi_streaming_start` | 0x19C40 | Mid-body — starts TX/RX endpoints, stores DCP handle |

**Calling 0x19C40 directly will crash** — it expects registers (R4, R5,
etc.) set up by the prologue at 0x19BCC. [verified Ghidra; failed
attempt logged in earlier sessions]

### EP3 OUT (MIDI bulk in from host) — the bug we hit

EP3 OUT was historically not delivering bulk data from host. Root cause
isolated to `slot+0x1F8` ("OUT start gate byte") — a TCAT-internal
gating field that **must be zero** before the OUT path is enabled.
Runtime symptom: non-zero blocks the path; clearing it to `0x00` is
required. The fix is in `firmware/patch/handlers.c` (the v2 firmware
patch, commit `23a59d9` "midi: bulk OUT EP3 host→device working").
[verified 2026-04-17 hardware test + Ghidra]

The auxiliary `slot+0x1FC` byte often tracks alongside `+0x1F8` and
clearing both stabilizes gate state. [verified empirically; underlying
state machine semantics conjecture]

---

## 5. Field mapping — Blender → DWC2 / Circle reference

For deeper register-level mapping see `usb_stack_init.md`,
`usb_stack_data.md`, and `usb_stack_runtime.md`. Quick reference:

### Endpoint handle (software object)

| Blender field | Address | Likely meaning | Status |
|---|---|---|---|
| `ep_h + 0x00` (`f=` in EC1) | EP start callback pointer | EP_ENABLE/CLEAR_NAK trigger | [verified High] |
| `ep_h + 0x04` (`fn4`) | "set halted"/state callback | EP disable/halt helper | [conjecture: Medium confidence] |
| `ep_h + 0x08` (`fn8`) | completion callback | OUT completion ISR callback | [verified High — observed at `0x1B9E4`] |
| `ep_h + 0x10` (`b=`) | transfer buffer pointer | DWC2 `DOEPDMA` payload pointer | [verified High] |
| `ep_h + 0x14` (`n=`) | transfer size bytes | DWC2 `DOEPTSIZ` payload length | [verified High] |
| `ep_h + 0x1C` (`qp=`) | QH/descriptor pointer | TCAT-internal queue context (not spec dTD) | [verified High; not Synopsys-spec descriptor] |
| `ep_h + 0x20` (`s=`) | endpoint runtime state | DWC2 EP state machine software mirror | [conjecture: Medium] |
| `ep_h + 0x24` (`F=` in EC2) | flags bitfield (low byte = critical gate) | DWC2 EP_ENABLE/NAK-state gate | [verified High — low byte must be `0x02` for working path] |
| `ep_h + 0x3C` (`mps=`) | max packet size | DWC2 `MPS` field in EP control | [verified High — `0x200` for HS bulk EP3 OUT] |

### TCAT extension bank at `USB_BASE + 0x800`

This is the **DWC2 device-mode register block (DCFG/DCTL/DSTS/DAINT)**,
NOT a TCAT extension. Earlier docs misidentified it. [outdated 2026-04-28
"TCAT extension bank" naming; corrected in `dice3_usb_registers.md`]

| Address | Standard DWC2 name | Function |
|---|---|---|
| `+0x810` | DCTL | Device control — IN/OUT mask bits |
| `+0x814` | DSTS | Device status |
| `+0x818` | DIEPMSK | IN endpoint interrupt mask |
| `+0x81C` | DOEPMSK | OUT endpoint interrupt mask |
| `+0x834` | DTKNQR1/2 | Token queue read 1/2 |

[verified DWC2 spec via EZR32WG-RM]

---

## 6. Init / Data / Runtime — see appendix files

The three deep-technical files (`usb_stack_init.md`,
`usb_stack_data.md`, `usb_stack_runtime.md`) cover:

- **init**: cold boot → scheduler → enumeration call graph, DCP handoff,
  all three entry-label variants of `usb_midi_set_interface_handler`
- **data**: USB stack data structures, SRAM memory layout, QH register
  banks (DIEP/DOEP), status bit maps
- **runtime**: packet flow through controller, FIFO/DMA state machine,
  ISR dispatch, QH status bits

These remain as separate files because they're each ~30–40 KB of dense
technical content with their own internal structure. The legend
convention in these files (`VERIFIED` / `VERIFIED-STATIC` /
`HYPOTHESIZED`) is compatible with this doc's `[verified]` /
`[conjecture]` tags — treat `HYPOTHESIZED` as `[conjecture]`.

---

## 7. Open / inconclusive items

- **TCAT-internal queue structures** at SRAM `0x90000B..` — byte layout
  partially decoded; never matched against DWC2 spec because it's not
  spec'd (TCAT-internal). [conjecture: layout]
- **EP3 `slot+0x1FC`** auxiliary gate byte — function unclear; clearing
  empirically helps. [conjecture: function]
- **HS PHY identity** — DICE3 has an external HS-capable PHY; vendor
  unknown. [conjecture]

The `flash-suggestions.md`-style "Arasan USB IP" reference was wrong —
the IP is verified Synopsys DWC2. Any "Arasan" mention near USB in
older docs is `[outdated 2026-04-28]`.
