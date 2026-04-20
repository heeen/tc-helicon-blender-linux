/*
 * dice_usb_regs.h — TCAT DICE3 USB controller register definitions
 *
 * Cross-referenced from:
 *   - Linux kernel ChipIdea driver: drivers/usb/chipidea/bits.h, udc.h, core.c
 *   - Ghidra decompilation of usb_hw_controller_init (0x214A8),
 *     usb_hw_dsr_interrupt_dispatch (0x21E78), usb_hw_ep_read_start (0x2115C)
 *   - Live register reads via DCP diagnostic opcode 8
 *
 * The DICE3 USB controller is ChipIdea-based with TCAT proprietary extensions.
 * Standard ChipIdea operational registers at op_base = USB_BASE + 0x10.
 * TCAT extension registers at USB_BASE + 0x800.
 *
 * Per-endpoint QH register banks (corrected 2026-04-20 from ISR decomp at
 * 0x21E78 and init function at 0x214A8):
 *   IN bank  (device→host, USB IN)  at USB_BASE + 0x900  ← TCAT_QH_IN_BASE
 *   OUT bank (host→device, USB OUT) at USB_BASE + 0xB00  ← TCAT_QH_OUT_BASE
 *
 * Evidence: init LOOP 1 assigns 0x900-bank pointers to slots read by
 * read_start's IN direction (slot+0x74) and by ISR's low-16-bits completion
 * branch which is the IN direction path. LOOP 2 assigns 0xB00 bank to
 * slot+0x1F4 which is the OUT direction path. Also, MIDI TX (EP2 IN, known
 * working) has its runtime QH pointer at 0x90000940 — in the 0x900 bank —
 * confirming 0x900 is IN. Historical versions of this header labeled these
 * backwards; see firmware/usb_dma_ep_ghidra.txt.
 */

#ifndef DICE_USB_REGS_H
#define DICE_USB_REGS_H

#include <stdint.h>

/* ════════════════════════════════════════════════════════════════════
 * Controller Base
 * ════════════════════════════════════════════════════════════════════ */

#define USB_BASE            0x90000000
/* ChipIdea-style operational window (ENDPTSETUPSTAT … ENDPTCTRLn). */
#define USB_OP_BASE         (USB_BASE + 0x10u)

/* ════════════════════════════════════════════════════════════════════
 * TCAT DICE3 USB Registers (verified addresses from Ghidra + UART dump)
 *
 * The TCAT map differs from textbook ChipIdea: USBCMD at +0x08, but
 * USBSTS/USBINTR at +0x14/+0x18 (gap at +0x0C). Offset +0x14 is also what
 * JTAG scripts use as “flush all EPs” (see reboot_stub / jtag_flash_*.py) —
 * not the same semantics as Linux’s USBSTS at that offset on other cores.
 * ENDPTPRIME/STAT/COMPLETE at USB_OP_BASE+0x70..0x7C are used in diagnostics;
 * compare against TCAT_EP_COMP_* at 0x818/0x81C for the proprietary path.
 * ════════════════════════════════════════════════════════════════════ */

#define USB_USBCMD          (*(volatile uint32_t *)(USB_BASE + 0x08))
#define USB_USBSTS          (*(volatile uint32_t *)(USB_BASE + 0x14))
#define USB_USBINTR         (*(volatile uint32_t *)(USB_BASE + 0x18))
#define USB_FRINDEX         (*(volatile uint32_t *)(USB_BASE + 0x1C))
#define USB_DEVICEADDR      (*(volatile uint32_t *)(USB_BASE + 0x24))
#define USB_ENDPTLISTADDR   (*(volatile uint32_t *)(USB_BASE + 0x28))

/* USBCMD bits */
#define USBCMD_RS           (1u << 0)   /* Run/Stop */
#define USBCMD_RST          (1u << 1)   /* Controller Reset */
#define USBCMD_SUTW         (1u << 13)  /* Setup Trip Wire */
#define USBCMD_ATDTW        (1u << 14)  /* Add dTD Trip Wire */

/* USBSTS / USBINTR bits (ISR reads USBSTS & USBINTR) */
#define USBi_UI             (1u << 0)   /* USB Interrupt (transfer complete) */
#define USBi_UEI            (1u << 1)   /* USB Error Interrupt */
#define USBi_PCI            (1u << 2)   /* Port Change Interrupt */
#define USBi_SEI            (1u << 4)   /* System Error / SOF */
#define USBi_URI            (1u << 6)   /* USB Reset Interrupt */
#define USBi_SLI            (1u << 8)   /* Sleep/Suspend Interrupt */

/* Standard ChipIdea endpoint registers (op_base relative) */
#define USB_ENDPTSETUPSTAT  (*(volatile uint32_t *)(USB_OP_BASE + 0x6C))  /* 0x9000007C */
#define USB_ENDPTPRIME      (*(volatile uint32_t *)(USB_OP_BASE + 0x70))  /* 0x90000080 */
#define USB_ENDPTFLUSH      (*(volatile uint32_t *)(USB_OP_BASE + 0x74))  /* 0x90000084 */
#define USB_ENDPTSTAT       (*(volatile uint32_t *)(USB_OP_BASE + 0x78))  /* 0x90000088 */
#define USB_ENDPTCOMPLETE   (*(volatile uint32_t *)(USB_OP_BASE + 0x7C))  /* 0x9000008C */
#define USB_ENDPTCTRL(n)    (*(volatile uint32_t *)(USB_OP_BASE + 0x80 + (n)*4))

/* ENDPTCTRL bit definitions (per-endpoint, from linux/drivers/usb/chipidea/bits.h) */
#define ENDPTCTRL_RXS       (1u << 0)   /* RX Endpoint Stall */
#define ENDPTCTRL_RXT_MASK  (3u << 2)   /* RX Endpoint Type */
#define ENDPTCTRL_RXT_BULK  (2u << 2)   /* RX Type = Bulk */
#define ENDPTCTRL_RXT_INT   (3u << 2)   /* RX Type = Interrupt */
#define ENDPTCTRL_RXR       (1u << 6)   /* RX Data Toggle Reset */
#define ENDPTCTRL_RXE       (1u << 7)   /* RX Endpoint Enable */
#define ENDPTCTRL_TXS       (1u << 16)  /* TX Endpoint Stall */
#define ENDPTCTRL_TXT_MASK  (3u << 18)  /* TX Endpoint Type */
#define ENDPTCTRL_TXT_BULK  (2u << 18)  /* TX Type = Bulk */
#define ENDPTCTRL_TXR       (1u << 22)  /* TX Data Toggle Reset */
#define ENDPTCTRL_TXE       (1u << 23)  /* TX Endpoint Enable */

/*
 * ENDPTPRIME / ENDPTSTAT / ENDPTCOMPLETE bit layout:
 *   Bits 0-5:  RX endpoints (USB OUT, device receives)
 *   Bits 16-21: TX endpoints (USB IN, device transmits)
 *
 * hw_ep_bit(num, dir) = (dir == TX) ? num + 16 : num
 */
#define EP_BIT_RX(n)  (1u << (n))        /* RX/OUT bit for endpoint n */
#define EP_BIT_TX(n)  (1u << ((n) + 16)) /* TX/IN bit for endpoint n */

/* ════════════════════════════════════════════════════════════════════
 * TCAT DICE3 Extension Registers (USB_BASE + 0x800)
 * NOT standard ChipIdea — proprietary to TCAT/DICE3 ASIC
 * ════════════════════════════════════════════════════════════════════ */

#define TCAT_USBMODE        (*(volatile uint32_t *)(USB_BASE + 0x800))
#define TCAT_PORTSC         (*(volatile uint32_t *)(USB_BASE + 0x808))
/* 2026-04-20 correction: 0x810 and 0x814 labels are swapped from the earlier
 * (ISR-decomp-contradicting) convention. The ISR 0x40000 branch (IN direction
 * completion, low 16 bits of COMP_STATUS) reads 0x90000810 as its enable mask;
 * the 0x80000 branch (OUT completion, high 16 bits) reads 0x90000814. So:
 *   0x810 = TCAT_EP_TX_EN (USB IN direction enable — device TX to host)
 *   0x814 = TCAT_EP_RX_EN (USB OUT direction enable — device RX from host)
 * Likewise, 0x834 is cleared on IN completion and set by dma_start_rx (which
 * is only called by the IN-direction submit wrapper at 0x21A6C despite
 * Ghidra's "_rx" naming) — so 0x834 is the IN async prime, not OUT. */
#define TCAT_EP_TX_EN       (*(volatile uint32_t *)(USB_BASE + 0x810))  /* per-EP IN/TX enable */
#define TCAT_EP_RX_EN       (*(volatile uint32_t *)(USB_BASE + 0x814))  /* per-EP OUT/RX enable */
#define TCAT_EP_COMP_STATUS (*(volatile uint32_t *)(USB_BASE + 0x818))  /* completion status (W1C) */
#define TCAT_EP_COMP_ENABLE (*(volatile uint32_t *)(USB_BASE + 0x81C))  /* completion IRQ enable */
#define TCAT_EP_ASYNC_PRIME (*(volatile uint32_t *)(USB_BASE + 0x834))  /* async IN prime trigger */

/* Undocumented / reserved in this header (not yet named from firmware):
 *   0x90000820–0x90000833, 0x90000838–0x900008FF (gaps around ASYNC_PRIME).
 *   0x90000000–0x90000007: ID/capability on some ChipIdea cores — not declared. */

/*
 * TCAT_EP_COMP_ENABLE / TCAT_EP_COMP_STATUS bit layout:
 *
 * The bit mapping is from the CONTROLLER perspective (reversed from USB direction):
 *   Bits 0-5:   USB IN endpoints (device TX) — controller "receives" completion
 *   Bits 16-21: USB OUT endpoints (device RX) — controller "transmits" to memory
 *
 * ISR processes:
 *   USB IN completions:  (COMP_STATUS & COMP_ENABLE) & 0xFFFF       (lower 16)
 *   USB OUT completions: (COMP_STATUS & COMP_ENABLE) >> 16          (upper 16)
 *
 * Example: EP3 OUT (MIDI RX, USB host→device) = bit 19 (= 3 + 16)
 *          EP2 IN  (MIDI TX, USB device→host) = bit 2
 */
#define TCAT_COMP_EP_IN(n)   (1u << (n))        /* USB IN / device TX */
#define TCAT_COMP_EP_OUT(n)  (1u << ((n) + 16)) /* USB OUT / device RX */

/* ════════════════════════════════════════════════════════════════════
 * TCAT Per-Endpoint QH Register Banks (CORRECTED 2026-04-20)
 *
 * Two banks, 0x20 bytes per endpoint:
 *   IN bank  (device→host, USB IN,  controller TX): USB_BASE + 0x900
 *   OUT bank (host→device, USB OUT, controller RX): USB_BASE + 0xB00
 *
 * EP0 is at bank base. EP1-6 at base + ep_num * 0x20.
 *
 * Evidence: see file header comment and firmware/usb_dma_ep_ghidra.txt.
 * Previous (inverted) labeling caused the EP3 OUT qh_ptr = 0x90000B60 to
 * look like "IN bank" when it was actually the correct OUT bank value.
 *
 * QH register block (8 words, 0x20 bytes):
 *   [0] +0x00: Capabilities (max_pkt, type, flags); bit 31=IN_USE, bit 26=ACTIVE
 *   [1] +0x04: (current/reserved)
 *   [2] +0x08: DMA overlay / active status (bit 7 = TD_STATUS_ACTIVE)
 *   [3] +0x0C: (reserved)
 *   [4] +0x10: Transfer size + multi count (set by dma_start)
 *   [5] +0x14: (stale data)
 *   [6] +0x18: Status field (bit 4 = idle/inactive, bit 7 = armed)
 *   [7] +0x1C: (stale data)
 * ════════════════════════════════════════════════════════════════════ */

#define TCAT_QH_IN_BASE     (USB_BASE + 0x900)  /* device→host (USB IN) */
#define TCAT_QH_OUT_BASE    (USB_BASE + 0xB00)  /* host→device (USB OUT) */
#define TCAT_QH_STRIDE      0x20

/* QH register access — returns pointer to QH[0] for given EP */
#define TCAT_QH_IN(ep_num)  ((volatile uint32_t *)(TCAT_QH_IN_BASE  + (ep_num) * TCAT_QH_STRIDE))
#define TCAT_QH_OUT(ep_num) ((volatile uint32_t *)(TCAT_QH_OUT_BASE + (ep_num) * TCAT_QH_STRIDE))

/* EP3 specific — MIDI endpoints */
#define TCAT_QH_EP3_IN      TCAT_QH_IN(3)   /* 0x90000960 — (not EP3 IN in current config; EP3 is OUT only) */
#define TCAT_QH_EP3_OUT     TCAT_QH_OUT(3)  /* 0x90000B60 — MIDI RX (bulk OUT) */

/* QH[0] control bits */
#define QH_IN_USE           (1u << 31)  /* "in use" — prevents usb_hw_ep_read_start reinit */
#define QH_ACTIVE           (1u << 26)  /* active transfer — HW clears on completion */
#define QH_ACTIVE_IOC       0x84000000u /* active + IOC combined (written by dma_start) */
#define QH_IOC              (1u << 27)  /* interrupt on completion */
#define QH_CAP28            (1u << 28)  /* capability flag */
#define QH_IOS              (1u << 15)  /* interrupt on setup */
#define QH_BULK             (1u << 19)  /* bulk transfer type */
#define QH_MAX_PKT_MASK     0x03FF0000u /* bits 25:16 = max packet size */
#define QH_MAX_PKT(sz)      (((sz) & 0x3FF) << 16)

/* ════════════════════════════════════════════════════════════════════
 * USB Endpoint Descriptor Constants
 * Reference: linux/include/uapi/linux/usb/ch9.h, eCos usb.h
 * ════════════════════════════════════════════════════════════════════ */

#define USB_DT_ENDPOINT          5
#define USB_DT_ENDPOINT_SIZE     7

#define USB_DIR_OUT              0x00
#define USB_DIR_IN               0x80
#define USB_ENDPOINT_DIR_MASK    0x80
#define USB_ENDPOINT_NUMBER_MASK 0x0F

#define USB_ENDPOINT_XFER_CONTROL  0
#define USB_ENDPOINT_XFER_ISOC     1
#define USB_ENDPOINT_XFER_BULK     2
#define USB_ENDPOINT_XFER_INT      3

/* ════════════════════════════════════════════════════════════════════
 * USB Device States (eCos)
 * Reference: ecos/packages/io/usb/slave/current/include/usbs.h
 * ════════════════════════════════════════════════════════════════════ */

#define USBS_STATE_DETACHED      0x01
#define USBS_STATE_ATTACHED      0x02
#define USBS_STATE_POWERED       0x03
#define USBS_STATE_DEFAULT       0x04
#define USBS_STATE_ADDRESSED     0x05
#define USBS_STATE_CONFIGURED    0x06
#define USBS_STATE_MASK          0x7F
#define USBS_STATE_SUSPENDED     (1u << 7)

/* ════════════════════════════════════════════════════════════════════
 * Endpoint Handle Struct (used by usb_endpoint_submit_transfer)
 * Located at device_ctx + ep_idx*0x40 + 0x1D8 (IN/TX side)
 * ════════════════════════════════════════════════════════════════════ */

typedef struct {
    void   (*start_fn)(void *ep);          /* +0x00: calls usb_hw_ep_start_transfer */
    void   (*set_halted_fn)(void *ep, int); /* +0x04 */
    void   (*complete_fn)(void *ctx, int);  /* +0x08: non-zero = transfer in flight */
    void    *complete_data;                 /* +0x0C: callback context */
    void    *buffer;                        /* +0x10: DMA target */
    uint32_t buffer_size;                   /* +0x14 */
    uint32_t halted;                        /* +0x18 */
    volatile uint32_t *qh_ptr;             /* +0x1C: QH register (0x90000Bxx or 0x9xx) */
    uint8_t  state;                         /* +0x20: 0=idle, 2=configured, 3=dma_active */
    uint8_t  _pad21[3];                     /* +0x21 */
    uint32_t flags;                         /* +0x24: ep_num | (attr << 8) */
    void    *dma_buf_copy;                  /* +0x28: copy during DMA */
    uint32_t dma_size_copy;                 /* +0x2C */
    uint32_t _reserved[2];                  /* +0x30 */
    uint16_t max_pkt;                       /* +0x38: used by dma_start; decompiler also
                                              references ushort at ep+0x3C via param_1+8 */
} ep_handle_t;

#define EP_STATE_IDLE        0
#define EP_STATE_CONFIGURED  2
#define EP_STATE_DMA_ACTIVE  3

/* ════════════════════════════════════════════════════════════════════
 * MIDI Streaming State (at 0x29CA4, ~0x478 bytes)
 * ════════════════════════════════════════════════════════════════════ */

typedef struct {
    uint32_t current_state;                 /* +0x00 */
    uint32_t rx_alt_active;                 /* +0x04 */
    uint32_t tx_alt_active;                 /* +0x08 */
    int32_t  cable_route[4];                /* +0x0C..+0x18 */
    uint8_t  speed;                         /* +0x1C: 0=HS, 2=FS */
    uint8_t  _pad1d[3];                     /* +0x1D */
    uint8_t  ep_descriptor[7];              /* +0x20: built by descriptor_build */
    uint8_t  _pad27;                        /* +0x27 */
    void    *cable_config_ptr;              /* +0x28 */
    uint32_t sync_mode;                     /* +0x2C */
} midi_streaming_state_t;
/* Distant fields: +0x460 = dcp_tx_endpoint, +0x474 = usb_device_obj_ptr */

/* ════════════════════════════════════════════════════════════════════
 * USB-MIDI Packet Buffer State (at 0x30D64, 0x210 bytes)
 * ════════════════════════════════════════════════════════════════════ */

typedef struct {
    uint8_t  pkt_buf[0x200];                /* +0x000: USB-MIDI event buffer */
    uint32_t total_packets;                 /* +0x200 */
    uint32_t current_index;                 /* +0x204 */
    void    *rx_ep_handle;                  /* +0x208: non-zero = armed */
    uint8_t  cable_count;                   /* +0x20C */
    uint8_t  overflow_cable;                /* +0x20D: 0xFF = none */
    uint8_t  _pad[2];                       /* +0x20E */
} midi_pkt_state_t;

/* ════════════════════════════════════════════════════════════════════
 * ChipIdea dTD Token Bits
 * Reference: linux/drivers/usb/chipidea/udc.h
 * ════════════════════════════════════════════════════════════════════ */

#define TD_STATUS_ACTIVE    (1u << 7)
#define TD_STATUS_HALTED    (1u << 6)
#define TD_STATUS_DT_ERR    (1u << 5)
#define TD_STATUS_TR_ERR    (1u << 3)
#define TD_IOC              (1u << 15)
#define TD_TOTAL_BYTES_MASK (0x7FFFu << 16)

/* ════════════════════════════════════════════════════════════════════
 * Firmware Patch Points
 * ════════════════════════════════════════════════════════════════════ */

/* High Speed MIDI skip bug in usb_midi_set_interface_handler (0x19BCC).
 * At 0x19BE4: `bne 0x19CAC` skips ALL MIDI endpoint arming at High Speed.
 * Blender negotiates HS → MIDI EPs never armed → bulk OUT NAKs.
 * Fix: NOP the branch so MIDI works at both HS and FS. */
#define MIDI_HS_SKIP_ADDR    0x19BE4
#define MIDI_HS_SKIP_BNE     0x1A000030u  /* original: bne 0x19CAC */
#define ARM_NOP              0xE1A00000u  /* mov r0, r0 */

#endif /* DICE_USB_REGS_H */

