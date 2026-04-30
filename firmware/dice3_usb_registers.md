# DICE3 USB Controller — Synopsys DesignWare USB 2.0 OTG (DWC2)

**IP identification** (corrected 2026-04-28):

The USB controller at `0x90000000` is **Synopsys DesignWare USB 2.0 OTG (DWC2),
v3.20a**, in device mode. It is **not** ChipIdea, and there is no "TCAT
proprietary completion engine" — what previous reverse engineering called the
TCAT extension block at `+0x800` and the "QH banks" at `+0x900` / `+0xB00` are
DWC2's standard device-mode register windows.

Evidence:

- `GSNPSID @ 0x90000040 = 0x4F54320A` — ASCII `"OT2\n"`, the Synopsys DWC2 OTG
  v3.20a identification signature.
- The init sequence in `usb_hw_ep_table_init` @ `0x214A8` writes the DWC2
  global register layout: `GRSTCTL` self-clearing FIFO flush bits at `0x010`,
  `GRXFSIZ` / `GNPTXFSIZ` / `GDFIFOCFG` at `0x024` / `0x028` / `0x05C`, and
  `GINTMSK` at `0x018` with the canonical DWC2 device interrupt mask
  `0x708E3C16` (USBRst | EnumDone | IEPInt | OEPInt | …).
- `usb_hw_ep_dma_start_rx` @ `0x21930` writes per-EP `DOEPCTLn` / `DOEPTSIZn`
  fields (EPENA bit 31, CNAK bit 26, PktCnt bits 28:19, XferSize bits 18:0).
- The "ChipIdea ENDPTPRIME / ENDPTFLUSH / ENDPTSTAT / ENDPTCOMPLETE / ENDPTCTRL"
  registers read as zeros at runtime not because TCAT bypassed them — those
  addresses do not contain ChipIdea registers at all in this IP.

Useful Linux kernel reference: `drivers/usb/dwc2/{core.h,gadget.c,hw.h}`.

## Base addresses

```
USB_BASE      = 0x90000000
GLOBAL block  = USB_BASE + 0x000  (DWC2 global registers)
DEVICE block  = USB_BASE + 0x800  (DWC2 device-mode registers)
DIEP bank     = USB_BASE + 0x900  (per-EP IN-direction register windows, stride 0x20)
DOEP bank     = USB_BASE + 0xB00  (per-EP OUT-direction register windows, stride 0x20)
```

There is no `op_base = USB_BASE + 0x10` operational sub-window — that is a
ChipIdea / EHCI concept that does not apply.

## Global registers (`USB_BASE + 0x000`)

| Offset | DWC2 name | Notes |
|--------|-----------|-------|
| 0x000  | GOTGCTL   | OTG control / status. Reads sticky bits set during reset. |
| 0x004  | GOTGINT   | OTG interrupt status (W1C). Init writes `0xFFFFFFFF` to clear. |
| 0x008  | GAHBCFG   | Bus access control. bit 0 = GlblIntrMsk, bits 4:1 = HBstLen, bit 5 = DMAEn. Init clears DMAEn (bit 5) initially, then sets GlblIntrMsk (bit 0). |
| 0x00C  | GUSBCFG   | PHY config (TermSel/PhyIf/UTMISel/etc.). |
| 0x010  | GRSTCTL   | Reset control. bit 4 = RxFFlsh, bit 5 = TxFFlsh, bit 10 = DMAReq, bit 31 = AHBIdle. **Self-clearing.** Init writes `0x420` (TxFFlsh+DMAReq) and waits for bit 5 to clear, then `0x10` (RxFFlsh) and waits for bit 4 to clear. |
| 0x014  | GINTSTS   | Global interrupt status (W1C). Init writes `0xFFFFFFFF`. |
| 0x018  | GINTMSK   | Global interrupt mask. Init OR-writes `0x708E3C16` = ModeMis \| OTGInt \| RxFLvl \| ErlySusp \| USBSusp \| USBRst \| EnumDone \| IEPInt \| OEPInt \| IISOIXFR \| PrtInt \| SessReqInt \| WkUpInt \| ResetDet. |
| 0x01C  | GRXSTSR   | RX FIFO status (read pop). |
| 0x020  | GRXSTSP   | RX FIFO status (pop). |
| 0x024  | GRXFSIZ   | RX FIFO depth (in dwords). Init = `0x100` (256 dwords). |
| 0x028  | GNPTXFSIZ | NP TX FIFO: bits 31:16 = depth, bits 15:0 = start address. Init = `0x100100` (depth=0x10, start=0x100). |
| 0x02C  | GNPTXSTS  | NP TX FIFO status. |
| 0x040  | GSNPSID   | Synopsys ID. Reads `0x4F54320A` ("OT2\n") — DWC2 v3.20a. |
| 0x044  | GHWCFG1   | Hardware feature flags 1. |
| 0x048  | GHWCFG2   | Hardware feature flags 2. |
| 0x04C  | GHWCFG3   | bits 31:16 = total FIFO depth in dwords. |
| 0x050  | GHWCFG4   | Hardware feature flags 4 (DescDMA support, etc.). |
| 0x054  | GLPMCFG   | Link Power Management config. |
| 0x05C  | GDFIFOCFG | Init = `(GHWCFG3 >> 16) | 0x1100000` (EPInfoBaseAddr=0x110, GDFIFOCfg=GHWCFG3.FIFODepth). |
| 0x100..0x118 | DIEPTXFn / HPTXFSIZ | Per-IN-EP TX FIFO sizes. Init writes 5 entries `0x800110, +0x80, +0x100, +0x180, +0x200` and a special `0x400390` for EP6. |

## Device registers (`USB_BASE + 0x800`)

| Offset | DWC2 name | Notes |
|--------|-----------|-------|
| 0x800 | DCFG       | Device config. bit 23 = DescDMA (scatter/gather). bits 1:0 = DevSpd. **DescDMA is cleared at init** — buffer DMA mode, not descriptor DMA. |
| 0x804 | DCTL       | Device control. bit 1 = SftDiscon (soft disconnect). |
| 0x808 | DSTS       | Device status. bits 2:1 = EnumSpd (0=HS, 1=FS, 2=LS, 3=FS-on-HS-PHY). |
| 0x810 | DIEPMSK    | Per-IN-EP interrupt mask (XferCompl/EPDisbld/AHBErr/Timeout/INTknTXFEmp/INTknEPMis/INEPNakEff). Live ~`0xAF` when MIDI is up. |
| 0x814 | DOEPMSK    | Per-OUT-EP interrupt mask (XferCompl/EPDisbld/AHBErr/SETUP/OUTTknEPDis/StsPhseRcvd). Live ~`0x2F`. |
| 0x818 | DAINT      | All-EP interrupt status. **Bits 0..15 = IN EPs** (per-EP XferCompl/etc. asserted), **bits 16..31 = OUT EPs**. |
| 0x81C | DAINTMSK   | All-EP interrupt mask, same layout. Stock: `0x00090007` = EP0/1/2 IN + EP0,EP3 OUT. |
| 0x820 | DTKNQR1    | Device IN token sequence (debug). |
| 0x828 | DVBUSDIS   | VBUS discharge time. |
| 0x830 | DTHRCTL    | Threshold control. Live ~`0x0C100020`. |
| 0x834 | DIEPEMPMSK | Per-EP TX-FIFO empty mask. The eCos driver also pokes `1<<ep_num` here on RX-direction `dma_start_rx` — this is a TCAT/eCos quirk; standard DWC2 only uses this register for IN-direction TX-FIFO-empty interrupts. |

### What was previously called "TCAT extension block"

The earlier doc described `0x90000800..0x900008FF` as a TCAT proprietary block.
That was wrong — it is the standard DWC2 device-mode register block. Mapping:

| Old label | Address | Actual DWC2 register |
|-----------|---------|----------------------|
| `TCAT_USBMODE`        | 0x800 | DCFG |
| `TCAT_PORTSC`         | 0x808 | DSTS |
| `TCAT_EP_TX_EN`       | 0x810 | DIEPMSK |
| `TCAT_EP_RX_EN`       | 0x814 | DOEPMSK |
| `TCAT_EP_COMP_STATUS` | 0x818 | DAINT |
| `TCAT_EP_COMP_ENABLE` | 0x81C | DAINTMSK |
| `TCAT_DMA_CFG`        | 0x830 | DTHRCTL |
| `TCAT_EP_ASYNC_PRIME` | 0x834 | DIEPEMPMSK |

`DAINT` / `DAINTMSK` bit numbering matches the old "low 16 = IN, high 16 = OUT"
layout exactly — that is also DWC2 standard.

## Warm reboot handoff note (2026-04-29)

Soft reboot (`reboot_common.c::usb_hw_reset`) now treats USB as a warm handoff:

- Mask all DWC2 interrupt enables (`GINTMSK`, `DAINTMSK`, `DIEPMSK`, `DOEPMSK`,
  `DIEPEMPMSK`) and clear pending status (`GINTSTS`, `GOTGINT`, `DAINT`).
- Assert `DCTL.SFTDISCON` and hold it long enough to make disconnect
  deterministic before jumping to TCAT bootloader.
- Quiesce non-control EPs and W1C their `DIEPINTn`/`DOEPINTn`.
- Do best-effort FIFO flush (`GRSTCTL.RXFFLSH`/`TXFFLSH`) only if `CSFTRST` is
  not already latched and `AHBIDLE` is observed.
- Issue `DCTL.CGNPINNAK|CGOUTNAK` before handoff so a prior global NAK state
  cannot persist into next-stage USB init.
- Intentionally **do not** assert `GRSTCTL.CSFTRST` in warm reboot path because
  captures on this target show bit0 can stick high and poison next-stage USB init.

USB patch dependency note:

- The runtime patch path (`firmware/patch/handlers.c::boot_init`) re-ORs device
  interrupt masks (`DOEPMSK`, `DAINTMSK`) during boot, so reboot teardown is free
  to zero mask registers aggressively without requiring restore-in-stub logic.

## Per-endpoint register banks

Each endpoint has a 0x20-byte register window. **There is no SRAM-resident
queue head** (despite the eCos source calling these pointers `qh_ptr`); they
point directly into MMIO.

### IN-direction bank (`USB_BASE + 0x900`, stride 0x20)

```
DIEPCTL0   @ 0x900    DIEPCTLn   @ 0x900 + n*0x20    (e.g. EP3 IN @ 0x960)
DIEPINT0   @ 0x908    DIEPINTn   @ 0x908 + n*0x20
DIEPTSIZ0  @ 0x910    DIEPTSIZn  @ 0x910 + n*0x20
DIEPDMA0   @ 0x914    DIEPDMAn   @ 0x914 + n*0x20
DTXFSTS0   @ 0x918    DTXFSTSn   @ 0x918 + n*0x20
```

### OUT-direction bank (`USB_BASE + 0xB00`, stride 0x20)

```
DOEPCTL0   @ 0xB00    DOEPCTLn   @ 0xB00 + n*0x20    (e.g. EP3 OUT @ 0xB60)
DOEPINT0   @ 0xB08    DOEPINTn   @ 0xB08 + n*0x20
DOEPTSIZ0  @ 0xB10    DOEPTSIZn  @ 0xB10 + n*0x20
DOEPDMA0   @ 0xB14    DOEPDMAn   @ 0xB14 + n*0x20
```

### DIEPCTLn / DOEPCTLn bits (DWC2 standard)

| Bits | Field | Notes |
|------|-------|-------|
| 10:0  | MPS         | Max packet size (Bulk/Iso/Int) |
| 14:11 | TxFNum      | TX FIFO number (IN only) |
| 15    | USBActEp    | Endpoint active |
| 16    | NAKSts (RO) | NAK is currently being sent |
| 17    | DPID/EONUM  | Data toggle (Bulk) / Even-odd frame (Iso/Int) |
| 19:18 | EPType      | 0=Ctrl, 1=Iso, 2=Bulk, 3=Int |
| 20    | NAKSts (RO) on some variants | |
| 21    | Stall       | Send STALL handshake |
| 26    | CNAK        | Clear NAK (W1S) |
| 27    | SNAK        | Set NAK (W1S) |
| 28    | SetD0PID    | Set DPID=0 (W1S, Bulk only) |
| 29    | SetD1PID    | Set DPID=1 (W1S, Bulk only) |
| 30    | EPDis       | Disable endpoint |
| 31    | EPEna       | Enable endpoint |

### DIEPTSIZn / DOEPTSIZn bits

| Bits | Field |
|------|-------|
| 18:0  | XferSize (transfer size in bytes) |
| 28:19 | PktCnt (packets to transfer) |
| 30:29 | MC (multi-count, IN) / RxDPID (OUT) |

### DIEPINTn / DOEPINTn bits (W1C)

OUT-direction (DOEPINTn):

| Bit | Name | Meaning |
|-----|------|---------|
| 0  | XferCompl    | Transfer completed |
| 1  | EPDisbld     | Endpoint disabled |
| 2  | AHBErr       | AHB error |
| 3  | SETUP        | SETUP phase done (EP0) |
| 4  | OUTTknEPDis  | OUT token received while EP disabled |
| 5  | StsPhseRcvd  | Status phase received |
| 13 | NAKIntrpt    | NAK interrupt |
| 14 | NYETIntrpt   | NYET interrupt |
| 15 | SETUP_RCVD   | SETUP packet received (sticky) |

## DCP READ snapshot decode

When DCP opcode 8 dumps live USB regs, decode against the table above. Stock
boot, MIDI configured at HS, no traffic:

| Address | Value | Decode |
|---------|-------|--------|
| 0x90000000 | 0x000D0000 | GOTGCTL — sticky bits from OTG init |
| 0x90000040 | 0x4F54320A | GSNPSID = "OT2\n" (DWC2 v3.20a) |
| 0x90000800 | 0x08100010 | DCFG: DescDMA off (bit 23=0), DevSpd field set |
| 0x90000810 | 0xAF       | DIEPMSK |
| 0x90000814 | 0x2F       | DOEPMSK |
| 0x9000081C | 0x00090007 | DAINTMSK: EP0/1/2 IN + EP0,EP3 OUT |
| 0x90000B60 | 0x80088200 | DOEPCTL3: EPEna(31) | USBActEp(15) | EPType=Bulk(18) | MPS=0x200 |
| 0x90000B70 | 0x00080200 | DOEPTSIZ3: PktCnt=1, XferSize=512 |

## DMA descriptor list at `0x90000104..0x118`

Init programs five entries (`0x800110, +0x80, +0x100, +0x180, +0x200`) and one
special (`0x400390`) at `+0x118`. These are DIEPTXFn TX-FIFO size words — DWC2
v3.20a defines DIEPTXF1..DIEPTXF15 in this range, with DIEPTXF[i] holding
`{TxFifoStartAddr[31:16], TxFifoDepth[15:0]}`. Re-read with that lens.

## High-Speed MIDI skip bug (unchanged)

In `usb_midi_set_interface_handler` @ `0x19BCC`, at address **`0x19BE4`**:
```
cmp r3, #2          ; speed byte: 0=HS, 2=FS
bne 0x19CAC         ; if NOT Full Speed → skip ALL MIDI endpoint setup
```

The firmware only arms MIDI endpoints at Full Speed; Blender negotiates HS, so
both MIDI bulk endpoints are never armed via the firmware's own
SET_CONFIGURATION handler.

**Fix:** NOP the `bne` at `0x19BE4` (`0x1A000030` → `0xE1A00000`) in `boot_init`
before USB enumeration.

## USB hardware driver functions (`0x20000-0x22000`)

| Address | Name | Touches (DWC2 names) |
|---------|------|----------------------|
| 0x20EAC | usb_hw_ep_dma_start | DOEPCTLn / DIEPCTLn (no DIEPEMPMSK poke) |
| 0x20F28 | usb_hw_ep_halt_set | per-EP CTL via pointer |
| 0x20FBC | usb_hw_ep_state_set | GINTSTS, GINTMSK |
| 0x21098 | usb_hw_ep0_stall | EP0 CTL |
| 0x21104 | usb_hw_ep_write_start | calls dma_start |
| 0x2115C | usb_hw_ep_read_start | GRSTCTL?, DAINTMSK, per-EP CTL |
| 0x213DC | usb_hw_set_device_address | DCFG |
| 0x2140C | usb_hw_ep_start_transfer | calls dma_start |
| 0x214A8 | usb_hw_ep_table_init | full DWC2 init (FIFO sizing, GINTMSK, DCFG, GAHBCFG bring-up) |
| 0x217A4 | usb_hw_isr_check_and_mask | GINTSTS, GINTMSK |
| 0x217DC | usb_hw_ep_dma_continue | **DIEPEMPMSK** (re-arm) |
| 0x21930 | usb_hw_ep_dma_start_rx | per-EP CTL/TSIZ + **DIEPEMPMSK** (initial prime) |
| 0x21A00 | usb_hw_ep0_read_start | calls dma_start_rx |
| 0x21A6C | usb_hw_ep_start_transfer_rx | calls dma_start_rx |
| 0x21AE4 | usb_hw_ep_rx_halt_set | DIEPEMPMSK, DAINTMSK |
| 0x21BA4 | usb_hw_ep_rx_enable | per-EP CTL |
| 0x21CE4 | usb_hw_ep_halt_all | calls halt functions |
| 0x21DCC | usb_hw_ep_tx_enable | per-EP CTL (`0x380` region in old notes is per-EP+TXFIFO, not a separate block) |
| 0x21E78 | usb_hw_dsr_interrupt_dispatch | DAINT / DAINTMSK / per-EP DOEPINTn / DIEPINTn |

## Connection vtable at `0x2A918` (unchanged)

| Slot | Address | Function |
|------|---------|----------|
| [0] | 0x214A8 | usb_hw_ep_table_init |
| [1] | 0x213DC | usb_hw_set_device_address |
| [2] | 0x2115C | usb_hw_ep_read_start |
| [3] | 0x21CE4 | usb_hw_ep_halt_all |
| [4] | 0x21104 | usb_hw_ep_write_start |
| [5] | 0x21A00 | usb_hw_ep0_read_start |
| [6] | 0x2194C | usb_hw_ep0_status_phase |
| [7] | 0x210E4 | Mid-body: TX path in ep_read_start |
| [8] | 0x210C4 | Mid-body: RX path in ep_read_start |

## Implications for the EP3 OUT investigation

- `qp = 0x90000B60` for EP3 OUT is **DOEPCTL3** — the correct register, not a
  "wrong bank" pointer. Plan Phase 1 ("QH bank labeling contradiction")
  resolves: there is no QH at all.
- `q0`/`q2`/`q4`/`q6` in EC1 dumps map to DOEPCTL3 / DOEPINT3 / DOEPTSIZ3 /
  DOEPDMA3.
- `DCFG.DescDMA` is **cleared at boot**, so DOEPDMA3 holds a raw buffer
  pointer, not a dTD. Patching a DescDMA descriptor is the wrong fix.
- Live snapshot already showed DOEPCTL3=0x80088200 (EPEna|USBActEp|Bulk|MPS=512)
  and DOEPTSIZ3=0x00080200 (1 pkt × 512 B) but DOEPINT3=0 and DAINT=0 — the
  EP is correctly armed but the host token never reaches EP-level logic. Next
  question: GAHBCFG.DMAEn state, TX FIFO partitioning that may be starving the
  RX path, or a missing GUSBCFG / GLPMCFG bit. Keep diagnosing with DWC2
  semantics.
