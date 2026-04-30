/*
 * dice_usb_regs.h — Synopsys DesignWare USB 2.0 OTG (DWC2) v3.20a
 *
 * The USB controller at USB_BASE = 0x90000000 is DWC2 in device mode, NOT
 * ChipIdea. GSNPSID @ +0x40 reads 0x4F54320A ("OT2\n") — the canonical
 * Synopsys signature. Earlier reverse engineering called the +0x800 / +0x900 /
 * +0xB00 windows "TCAT proprietary" — that was wrong; they are DWC2's standard
 * device-mode and per-EP register banks.
 *
 * Reference: drivers/usb/dwc2/{core.h,gadget.c,hw.h} in the Linux kernel.
 *
 * Per-endpoint register windows (each 0x20 bytes, stride 0x20):
 *   IN-direction (device→host):  USB_BASE + 0x900 + n*0x20
 *   OUT-direction (host→device): USB_BASE + 0xB00 + n*0x20
 *
 * What the eCos driver calls a "qh_ptr" is actually a pointer into one of
 * those MMIO windows, NOT a pointer to an SRAM-resident queue head. There is
 * no dQH array in this design.
 *
 * Direction-naming history:
 *   Old firmware/patch/dice_usb_regs.h had several swaps over time
 *   (TCAT_QH_IN_BASE, TCAT_QH_OUT_BASE) trying to reconcile contradictory
 *   labels. The truth is: 0x900 is DIEPCTL bank (IN), 0x900+n*0x20 is the
 *   per-EP register window for IN endpoint n, and 0xB00 is the same for OUT.
 *   This matches the Linux DWC2 driver layout exactly.
 */

#ifndef DICE_USB_REGS_H
#define DICE_USB_REGS_H

#include <stdint.h>

/* ════════════════════════════════════════════════════════════════════
 * Controller Base
 * ════════════════════════════════════════════════════════════════════ */

#define USB_BASE                    0x90000000u

/* ════════════════════════════════════════════════════════════════════
 * Global registers (USB_BASE + 0x000..0x0FF)
 * ════════════════════════════════════════════════════════════════════ */

#define USB_GOTGCTL                 (*(volatile uint32_t *)(USB_BASE + 0x000))
#define USB_GOTGINT                 (*(volatile uint32_t *)(USB_BASE + 0x004))
#define USB_GAHBCFG                 (*(volatile uint32_t *)(USB_BASE + 0x008))
#define USB_GUSBCFG                 (*(volatile uint32_t *)(USB_BASE + 0x00C))
#define USB_GRSTCTL                 (*(volatile uint32_t *)(USB_BASE + 0x010))
#define USB_GINTSTS                 (*(volatile uint32_t *)(USB_BASE + 0x014))
#define USB_GINTMSK                 (*(volatile uint32_t *)(USB_BASE + 0x018))
#define USB_GRXSTSR                 (*(volatile uint32_t *)(USB_BASE + 0x01C))
#define USB_GRXSTSP                 (*(volatile uint32_t *)(USB_BASE + 0x020))
#define USB_GRXFSIZ                 (*(volatile uint32_t *)(USB_BASE + 0x024))
#define USB_GNPTXFSIZ               (*(volatile uint32_t *)(USB_BASE + 0x028))
#define USB_GNPTXSTS                (*(volatile uint32_t *)(USB_BASE + 0x02C))
#define USB_GSNPSID                 (*(volatile uint32_t *)(USB_BASE + 0x040))
#define USB_GHWCFG1                 (*(volatile uint32_t *)(USB_BASE + 0x044))
#define USB_GHWCFG2                 (*(volatile uint32_t *)(USB_BASE + 0x048))
#define USB_GHWCFG3                 (*(volatile uint32_t *)(USB_BASE + 0x04C))
#define USB_GHWCFG4                 (*(volatile uint32_t *)(USB_BASE + 0x050))
#define USB_GLPMCFG                 (*(volatile uint32_t *)(USB_BASE + 0x054))
#define USB_GDFIFOCFG               (*(volatile uint32_t *)(USB_BASE + 0x05C))

/* DIEPTXFn (per-IN-EP TX FIFO size) at 0x100..0x140, n=1..15.
 *   bits 31:16 = TxFifoStartAddr (in dwords, from FIFO start)
 *   bits 15:0  = TxFifoDepth (in dwords) */
#define USB_DIEPTXF(n)              (*(volatile uint32_t *)(USB_BASE + 0x100 + ((n) - 1) * 4))
#define USB_HPTXFSIZ                (*(volatile uint32_t *)(USB_BASE + 0x100))   /* host mode alias of DIEPTXF/HPTXFSIZ word */

#define DIEPTXF_DEP_SHIFT           16
#define DIEPTXF_DEP_MASK            (0xFFFFu << 16)
#define DIEPTXF_DEP(n)              ((uint32_t)(n) << 16)
#define DIEPTXF_STADDR_MASK         0xFFFFu
#define DIEPTXF_STADDR(n)           ((uint32_t)(n) & 0xFFFFu)
#define DIEPTXF_PACK(start, depth)  (DIEPTXF_DEP(depth) | DIEPTXF_STADDR(start))

/* GDFIFOCFG bits */
#define GDFIFOCFG_GDFIFOCFG_MASK    0xFFFFu
#define GDFIFOCFG_EPINFOBASE_SHIFT  16
#define GDFIFOCFG_EPINFOBASE_MASK   (0xFFFFu << 16)

/* GOTGCTL bits (OTG control / status) */
#define GOTGCTL_SESREQSCS           (1u << 0)
#define GOTGCTL_SESREQ              (1u << 1)
#define GOTGCTL_VBVALIDOVEN         (1u << 2)
#define GOTGCTL_VBVALIDOVVAL        (1u << 3)
#define GOTGCTL_BVALIDOVEN          (1u << 4)
#define GOTGCTL_BVALIDOVVAL         (1u << 5)
#define GOTGCTL_AVALIDOVEN          (1u << 6)
#define GOTGCTL_AVALIDOVVAL         (1u << 7)
#define GOTGCTL_HSTNEGSCS           (1u << 8)
#define GOTGCTL_HNPREQ              (1u << 9)
#define GOTGCTL_HSTSETHNPEN         (1u << 10)
#define GOTGCTL_DEVHNPEN            (1u << 11)
#define GOTGCTL_CONIDSTS            (1u << 16)  /* RO: 0=A-Device, 1=B-Device */
#define GOTGCTL_DBNCTIME            (1u << 17)  /* RO */
#define GOTGCTL_ASESVLD             (1u << 18)  /* RO host-only */
#define GOTGCTL_BSESVLD             (1u << 19)  /* RO device-only */
#define GOTGCTL_OTGVER              (1u << 20)  /* 0=OTG1.3, 1=OTG2.0 */

/* GOTGINT bits (W1C) */
#define GOTGINT_SESENDDET           (1u << 2)
#define GOTGINT_SESREQSUCSTSCHNG    (1u << 8)
#define GOTGINT_HSTNEGSUCSTSCHNG    (1u << 9)
#define GOTGINT_HSTNEGDET           (1u << 17)
#define GOTGINT_ADEVTOUTCHG         (1u << 18)
#define GOTGINT_DBNCEDONE           (1u << 19)

/* GRXSTSR / GRXSTSP fields (RX FIFO status, top-of-FIFO read) */
#define GRXSTS_CHEPNUM_MASK         0xFu               /* bits 3:0 */
#define GRXSTS_BCNT_SHIFT           4
#define GRXSTS_BCNT_MASK            (0x7FFu << 4)      /* bits 14:4 */
#define GRXSTS_DPID_SHIFT           15
#define GRXSTS_DPID_MASK            (0x3u << 15)       /* bits 16:15 */
#define GRXSTS_PKTSTS_SHIFT         17
#define GRXSTS_PKTSTS_MASK          (0xFu << 17)       /* bits 20:17 */
#define   GRXSTS_PKTSTS_GOUTNAK     1                  /* device-mode global OUT NAK */
#define   GRXSTS_PKTSTS_PKTRCV      2                  /* OUT data packet received */
#define   GRXSTS_PKTSTS_XFERCOMPL   3                  /* OUT transfer completed */
#define   GRXSTS_PKTSTS_SETUPCOMPL  4                  /* SETUP transaction completed */
#define   GRXSTS_PKTSTS_TGLERR      5                  /* host: data toggle error */
#define   GRXSTS_PKTSTS_SETUPRCVD   6                  /* SETUP data packet received */
#define   GRXSTS_PKTSTS_CHLT        7                  /* host: channel halted */
#define GRXSTS_FN_SHIFT             21
#define GRXSTS_FN_MASK              (0xFu << 21)       /* bits 24:21 */

/* GUSBCFG bits (subset) */
#define GUSBCFG_TOUTCAL_MASK        (7u << 0)
#define GUSBCFG_PHYIF16             (1u << 3)
#define GUSBCFG_ULPI_UTMI_SEL       (1u << 4)
#define GUSBCFG_PHYSEL              (1u << 6)
#define GUSBCFG_TRDTIM_MASK         (0xFu << 10)
#define GUSBCFG_FORCEHOST           (1u << 29)
#define GUSBCFG_FORCEDEV            (1u << 30)

/* GAHBCFG bits */
#define GAHBCFG_GLBLINTRMSK         (1u << 0)
#define GAHBCFG_HBSTLEN_MASK        (0xFu << 1)
#define GAHBCFG_DMAEN               (1u << 5)
#define GAHBCFG_NPTXFEMPLVL         (1u << 7)
#define GAHBCFG_PTXFEMPLVL          (1u << 8)

/* GRSTCTL bits */
#define GRSTCTL_CSFTRST             (1u << 0)
#define GRSTCTL_HSFTRST             (1u << 1)
#define GRSTCTL_RXFFLSH             (1u << 4)
#define GRSTCTL_TXFFLSH             (1u << 5)
#define GRSTCTL_TXFNUM_SHIFT        6
#define GRSTCTL_TXFNUM_MASK         (0x1Fu << 6)
#define GRSTCTL_DMAREQ              (1u << 10)
#define GRSTCTL_AHBIDLE             (1u << 31)

/* GINTSTS / GINTMSK bits (W1C in GINTSTS) */
#define GINT_MODEMIS                (1u << 1)
#define GINT_OTGINT                 (1u << 2)
#define GINT_SOF                    (1u << 3)
#define GINT_RXFLVL                 (1u << 4)
#define GINT_NPTXFEMP               (1u << 5)
#define GINT_GINNAKEFF              (1u << 6)
#define GINT_GOUTNAKEFF             (1u << 7)
#define GINT_ERLYSUSP               (1u << 10)
#define GINT_USBSUSP                (1u << 11)
#define GINT_USBRST                 (1u << 12)
#define GINT_ENUMDONE               (1u << 13)
#define GINT_ISOOUTDROP             (1u << 14)
#define GINT_EOPF                   (1u << 15)
#define GINT_IEPINT                 (1u << 18)
#define GINT_OEPINT                 (1u << 19)
#define GINT_INCOMPISOIN            (1u << 20)
#define GINT_INCOMPIP               (1u << 21)
#define GINT_RESETDET               (1u << 23)
#define GINT_PRTLINT                (1u << 24)
#define GINT_HCHINT                 (1u << 25)
#define GINT_PTXFEMP                (1u << 26)
#define GINT_CONIDSTSCHNG           (1u << 28)
#define GINT_DISCONNINT             (1u << 29)
#define GINT_SESSREQINT             (1u << 30)
#define GINT_WKUPINT                (1u << 31)

/* ════════════════════════════════════════════════════════════════════
 * Device-mode registers (USB_BASE + 0x800..0x83F)
 * ════════════════════════════════════════════════════════════════════ */

#define USB_DCFG                    (*(volatile uint32_t *)(USB_BASE + 0x800))
#define USB_DCTL                    (*(volatile uint32_t *)(USB_BASE + 0x804))
#define USB_DSTS                    (*(volatile uint32_t *)(USB_BASE + 0x808))
#define USB_DIEPMSK                 (*(volatile uint32_t *)(USB_BASE + 0x810))
#define USB_DOEPMSK                 (*(volatile uint32_t *)(USB_BASE + 0x814))
#define USB_DAINT                   (*(volatile uint32_t *)(USB_BASE + 0x818))
#define USB_DAINTMSK                (*(volatile uint32_t *)(USB_BASE + 0x81C))
#define USB_DTKNQR1                 (*(volatile uint32_t *)(USB_BASE + 0x820))
#define USB_DVBUSDIS                (*(volatile uint32_t *)(USB_BASE + 0x828))
#define USB_DVBUSPULSE              (*(volatile uint32_t *)(USB_BASE + 0x82C))
#define USB_DTHRCTL                 (*(volatile uint32_t *)(USB_BASE + 0x830))
#define USB_DIEPEMPMSK              (*(volatile uint32_t *)(USB_BASE + 0x834))

/* DCFG bits */
#define DCFG_DEVSPD_MASK            (3u << 0)
#define DCFG_DEVSPD_HS              (0u << 0)  /* HS PHY → HS */
#define DCFG_DEVSPD_HS_FS           (1u << 0)  /* HS PHY → FS */
#define DCFG_DEVSPD_LS              (2u << 0)
#define DCFG_DEVSPD_FS              (3u << 0)  /* FS PHY → FS */
#define DCFG_NZSTSOUTHSHK           (1u << 2)
#define DCFG_DEVADDR_SHIFT          4
#define DCFG_DEVADDR_MASK           (0x7Fu << 4)
#define DCFG_PERFRINT_SHIFT         11
#define DCFG_DESCDMA                (1u << 23)

/* DCTL bits */
#define DCTL_RWUSIG                 (1u << 0)
#define DCTL_SFTDISCON              (1u << 1)
#define DCTL_GNPINNAKEFF            (1u << 2)
#define DCTL_GOUTNAKEFF             (1u << 3)
#define DCTL_SGNPINNAK              (1u << 7)
#define DCTL_CGNPINNAK              (1u << 8)
#define DCTL_SGOUTNAK               (1u << 9)
#define DCTL_CGOUTNAK               (1u << 10)

/* DSTS bits */
#define DSTS_SUSPSTS                (1u << 0)
#define DSTS_ENUMSPD_SHIFT          1
#define DSTS_ENUMSPD_MASK           (3u << 1)
#define DSTS_ENUMSPD_HS             (0u << 1)
#define DSTS_ENUMSPD_FS             (1u << 1)
#define DSTS_ENUMSPD_LS             (2u << 1)
#define DSTS_ENUMSPD_FS_HS_PHY      (3u << 1)
#define DSTS_ERRTICERR              (1u << 3)

/* DAINT / DAINTMSK bit layout (low 16 = IN EP, high 16 = OUT EP) */
#define DAINT_IN(n)                 (1u << (n))
#define DAINT_OUT(n)                (1u << ((n) + 16))

/* DIEPMSK / DIEPINTn bits */
#define DIEP_XFERCOMPL              (1u << 0)
#define DIEP_EPDISBLD               (1u << 1)
#define DIEP_AHBERR                 (1u << 2)
#define DIEP_TIMEOUT                (1u << 3)
#define DIEP_INTKNTXFEMP            (1u << 4)
#define DIEP_INTKNEPMIS             (1u << 5)
#define DIEP_INEPNAKEFF             (1u << 6)
#define DIEP_TXFEMP                 (1u << 7)

/* DOEPMSK / DOEPINTn bits */
#define DOEP_XFERCOMPL              (1u << 0)
#define DOEP_EPDISBLD               (1u << 1)
#define DOEP_AHBERR                 (1u << 2)
#define DOEP_SETUP                  (1u << 3)  /* EP0 */
#define DOEP_OUTTKNEPDIS            (1u << 4)
#define DOEP_STSPHSERCVD            (1u << 5)
#define DOEP_NAKINTRPT              (1u << 13)
#define DOEP_NYETINTRPT             (1u << 14)
#define DOEP_SETUP_RCVD             (1u << 15)

/* ════════════════════════════════════════════════════════════════════
 * Per-endpoint register banks
 * ════════════════════════════════════════════════════════════════════ */

#define USB_DIEP_BASE               (USB_BASE + 0x900)   /* IN bank base (EP0) */
#define USB_DOEP_BASE               (USB_BASE + 0xB00)   /* OUT bank base (EP0) */
#define USB_EP_STRIDE               0x20

/* Word offsets within each per-EP window */
#define USB_EP_OFF_CTL              0x00
#define USB_EP_OFF_INT              0x08
#define USB_EP_OFF_TSIZ             0x10
#define USB_EP_OFF_DMA              0x14
#define USB_EP_OFF_DTXFSTS          0x18  /* IN-only: TX FIFO status */

/* Per-EP IN registers */
#define USB_DIEPCTL(n)              (*(volatile uint32_t *)(USB_DIEP_BASE + (n) * USB_EP_STRIDE + USB_EP_OFF_CTL))
#define USB_DIEPINT(n)              (*(volatile uint32_t *)(USB_DIEP_BASE + (n) * USB_EP_STRIDE + USB_EP_OFF_INT))
#define USB_DIEPTSIZ(n)             (*(volatile uint32_t *)(USB_DIEP_BASE + (n) * USB_EP_STRIDE + USB_EP_OFF_TSIZ))
#define USB_DIEPDMA(n)              (*(volatile uint32_t *)(USB_DIEP_BASE + (n) * USB_EP_STRIDE + USB_EP_OFF_DMA))
#define USB_DTXFSTS(n)              (*(volatile uint32_t *)(USB_DIEP_BASE + (n) * USB_EP_STRIDE + USB_EP_OFF_DTXFSTS))

/* Per-EP OUT registers */
#define USB_DOEPCTL(n)              (*(volatile uint32_t *)(USB_DOEP_BASE + (n) * USB_EP_STRIDE + USB_EP_OFF_CTL))
#define USB_DOEPINT(n)              (*(volatile uint32_t *)(USB_DOEP_BASE + (n) * USB_EP_STRIDE + USB_EP_OFF_INT))
#define USB_DOEPTSIZ(n)             (*(volatile uint32_t *)(USB_DOEP_BASE + (n) * USB_EP_STRIDE + USB_EP_OFF_TSIZ))
#define USB_DOEPDMA(n)              (*(volatile uint32_t *)(USB_DOEP_BASE + (n) * USB_EP_STRIDE + USB_EP_OFF_DMA))

/* DIEPCTLn / DOEPCTLn bits (DWC2 standard; some bits IN-only or OUT-only) */
#define DEPCTL_MPS_MASK             (0x7FFu << 0)
#define DEPCTL_MPS(sz)              ((sz) & 0x7FF)
#define DEPCTL_TXFNUM_SHIFT         11           /* IN only */
#define DEPCTL_TXFNUM_MASK          (0xFu << 11)
#define DEPCTL_USBACTEP             (1u << 15)
#define DEPCTL_NAKSTS               (1u << 16)   /* read-only: hw is NAKing */
#define DEPCTL_DPID                 (1u << 17)   /* Bulk: data toggle (read-only) */
#define DEPCTL_EPTYPE_SHIFT         18
#define DEPCTL_EPTYPE_MASK          (3u << 18)
#define DEPCTL_EPTYPE_CTRL          (0u << 18)
#define DEPCTL_EPTYPE_ISO           (1u << 18)
#define DEPCTL_EPTYPE_BULK          (2u << 18)
#define DEPCTL_EPTYPE_INTR          (3u << 18)
#define DEPCTL_STALL                (1u << 21)
#define DEPCTL_CNAK                 (1u << 26)
#define DEPCTL_SNAK                 (1u << 27)
#define DEPCTL_SETD0PID             (1u << 28)
#define DEPCTL_SETD1PID             (1u << 29)
#define DEPCTL_EPDIS                (1u << 30)
#define DEPCTL_EPENA                (1u << 31)

/* DIEPTSIZn / DOEPTSIZn bits */
#define DEPTSIZ_XFERSIZE_MASK       (0x7FFFFu << 0)
#define DEPTSIZ_XFERSIZE(sz)        ((sz) & 0x7FFFF)
#define DEPTSIZ_PKTCNT_SHIFT        19
#define DEPTSIZ_PKTCNT_MASK         (0x3FFu << 19)
#define DEPTSIZ_PKTCNT(n)           ((uint32_t)(n) << 19)
#define DEPTSIZ_MC_SHIFT            29           /* IN only (multi-count) */
#define DEPTSIZ_RXDPID_SHIFT        29           /* OUT iso/int (RX data PID) */

/* ════════════════════════════════════════════════════════════════════
 * USB Endpoint Descriptor Constants (eCos / linux/usb/ch9.h)
 * ════════════════════════════════════════════════════════════════════ */

#define USB_DT_ENDPOINT             5
#define USB_DT_ENDPOINT_SIZE        7

#define USB_DIR_OUT                 0x00
#define USB_DIR_IN                  0x80
#define USB_ENDPOINT_DIR_MASK       0x80
#define USB_ENDPOINT_NUMBER_MASK    0x0F

#define USB_ENDPOINT_XFER_CONTROL   0
#define USB_ENDPOINT_XFER_ISOC      1
#define USB_ENDPOINT_XFER_BULK      2
#define USB_ENDPOINT_XFER_INT       3

/* ════════════════════════════════════════════════════════════════════
 * USB Device States (eCos)
 * ════════════════════════════════════════════════════════════════════ */

#define USBS_STATE_DETACHED         0x01
#define USBS_STATE_ATTACHED         0x02
#define USBS_STATE_POWERED          0x03
#define USBS_STATE_DEFAULT          0x04
#define USBS_STATE_ADDRESSED        0x05
#define USBS_STATE_CONFIGURED       0x06
#define USBS_STATE_MASK             0x7F
#define USBS_STATE_SUSPENDED        (1u << 7)

/* ════════════════════════════════════════════════════════════════════
 * Endpoint Handle Struct (per-direction subslot in the eCos driver)
 *
 * The eCos driver holds two parallel arrays of these inside its controller
 * context (ctx + 0x58 for IN, ctx + 0x1D8 for OUT). The field originally
 * named `qh_ptr` is a pointer to the per-EP DWC2 register window, NOT to a
 * software queue head. There is no SRAM dQH in this design.
 * ════════════════════════════════════════════════════════════════════ */

typedef struct {
    void   (*start_fn)(void *ep);           /* +0x00 */
    void   (*set_halted_fn)(void *ep, int); /* +0x04 */
    void   (*complete_fn)(void *ctx, int);  /* +0x08 — non-zero = transfer in flight */
    void    *complete_data;                 /* +0x0C */
    void    *buffer;                        /* +0x10: DMA buffer (OUT) / source (IN) */
    uint32_t buffer_size;                   /* +0x14 */
    uint32_t halted;                        /* +0x18 */
    volatile uint32_t *ep_regs;             /* +0x1C: DIEPCTLn or DOEPCTLn window
                                                       (was "qh_ptr") */
    uint8_t  state;                         /* +0x20: 0=idle, 2=cfg, 3=DMA active */
    uint8_t  _pad21[3];
    uint32_t flags;                         /* +0x24: ep_num | (attr << 8) */
    void    *dma_buf_copy;                  /* +0x28 */
    uint32_t dma_size_copy;                 /* +0x2C */
    uint32_t _reserved[2];                  /* +0x30 */
    uint16_t max_pkt;                       /* +0x38 */
} ep_handle_t;

#define EP_STATE_IDLE               0
#define EP_STATE_CONFIGURED         2
#define EP_STATE_DMA_ACTIVE         3

/* ════════════════════════════════════════════════════════════════════
 * MIDI Streaming State (at 0x29CA4, ~0x478 bytes)
 * ════════════════════════════════════════════════════════════════════ */

typedef struct {
    uint32_t current_state;                 /* +0x00 */
    uint32_t rx_alt_active;                 /* +0x04 */
    uint32_t tx_alt_active;                 /* +0x08 */
    int32_t  cable_route[4];                /* +0x0C..+0x18 */
    uint8_t  speed;                         /* +0x1C: 0=HS, 2=FS */
    uint8_t  _pad1d[3];
    uint8_t  ep_descriptor[7];              /* +0x20: built by descriptor_build */
    uint8_t  _pad27;
    void    *cable_config_ptr;              /* +0x28 */
    uint32_t sync_mode;                     /* +0x2C */
} midi_streaming_state_t;
/* Distant fields: +0x460 = dcp_tx_endpoint, +0x474 = usb_device_obj_ptr */

/* ════════════════════════════════════════════════════════════════════
 * USB-MIDI Packet Buffer State (at 0x30D64, 0x210 bytes)
 * ════════════════════════════════════════════════════════════════════ */

typedef struct {
    uint8_t  pkt_buf[0x200];                /* +0x000 */
    uint32_t total_packets;                 /* +0x200 */
    uint32_t current_index;                 /* +0x204 */
    void    *rx_ep_handle;                  /* +0x208 — non-zero = armed */
    uint8_t  cable_count;                   /* +0x20C */
    uint8_t  overflow_cable;                /* +0x20D — 0xFF = none */
    uint8_t  _pad[2];
} midi_pkt_state_t;

/* ════════════════════════════════════════════════════════════════════
 * Firmware Patch Points
 * ════════════════════════════════════════════════════════════════════ */

/* High Speed MIDI skip bug in usb_midi_set_interface_handler (0x19BCC).
 * At 0x19BE4: `bne 0x19CAC` skips ALL MIDI endpoint arming at High Speed.
 * Blender negotiates HS → MIDI EPs never armed → bulk OUT NAKs.
 * Fix: NOP the branch so MIDI works at both HS and FS. */
#define MIDI_HS_SKIP_ADDR           0x19BE4
#define MIDI_HS_SKIP_BNE            0x1A000030u  /* original: bne 0x19CAC */
#define ARM_NOP                     0xE1A00000u  /* mov r0, r0 */

#endif /* DICE_USB_REGS_H */
