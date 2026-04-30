#ifndef BLENDER_PERIPH_LIB_H
#define BLENDER_PERIPH_LIB_H

#include <stdint.h>

#include "patch/dice_usb_regs.h"

#define BLENDER_VIC_INT_EN_CLR   (*(volatile uint32_t *)0xFFFFF014u)
#define BLENDER_VIC_SOFT_CLR     (*(volatile uint32_t *)0xFFFFF01Cu)
#define BLENDER_VIC_VECT_ADDR    (*(volatile uint32_t *)0xFFFFF030u)
#define BLENDER_VIC_VECT_SLOT(i) (*(volatile uint32_t *)(0xFFFFF100u + ((i) * 4u)))
#define BLENDER_VIC_VECT_CNTL(i) (*(volatile uint32_t *)(0xFFFFF200u + ((i) * 4u)))

#define BLENDER_DMA_BASE ((volatile uint32_t *)0x80000000u)

#define BLENDER_DWC2_EP_COUNT            7u   /* EP0..EP6 */
#define BLENDER_DWC2_POLL_LOOPS          200000u
#define BLENDER_DWC2_DISCONNECT_LOOPS    2000000u
#define BLENDER_DWC2_TXFNUM_ALL          0x10u

static inline void blender_periph_dwb(void) {
    __asm__ volatile("mcr p15, 0, %0, c7, c10, 4" ::"r"(0) : "memory");
}

static inline int blender_dwc2_wait_grstctl_mask(uint32_t mask,
                                                 uint32_t expect_set,
                                                 unsigned loops) {
    for (unsigned i = 0; i < loops; i++) {
        uint32_t v = USB_GRSTCTL;
        if (expect_set) {
            if (v & mask) return 1;
        } else {
            if ((v & mask) == 0u) return 1;
        }
    }
    return 0;
}

static inline void blender_dwc2_quiesce_active_eps(void) {
    for (unsigned ep = 1; ep < BLENDER_DWC2_EP_COUNT; ep++) {
        uint32_t diepctl = USB_DIEPCTL(ep);
        if (diepctl & DEPCTL_EPENA) {
            USB_DIEPCTL(ep) = diepctl | DEPCTL_SNAK | DEPCTL_EPDIS;
        } else {
            USB_DIEPCTL(ep) = diepctl | DEPCTL_SNAK;
        }
        USB_DIEPINT(ep) = 0xFFFFFFFFu;

        uint32_t doepctl = USB_DOEPCTL(ep);
        if (doepctl & DEPCTL_EPENA) {
            USB_DOEPCTL(ep) = doepctl | DEPCTL_SNAK | DEPCTL_EPDIS;
        } else {
            USB_DOEPCTL(ep) = doepctl | DEPCTL_SNAK;
        }
        USB_DOEPINT(ep) = 0xFFFFFFFFu;
    }
}

static inline void blender_usb_warm_handoff_reset(void) {
    USB_GAHBCFG &= ~GAHBCFG_GLBLINTRMSK;
    USB_GINTMSK = 0;
    USB_DAINTMSK = 0;
    USB_DIEPMSK = 0;
    USB_DOEPMSK = 0;
    USB_DIEPEMPMSK = 0;

    USB_GINTSTS = 0xFFFFFFFFu;
    USB_GOTGINT = 0xFFFFFFFFu;
    USB_DAINT = 0xFFFFFFFFu;

    USB_DCTL |= DCTL_SFTDISCON;
    for (volatile unsigned i = 0; i < BLENDER_DWC2_DISCONNECT_LOOPS; i++) {}

    blender_dwc2_quiesce_active_eps();

    USB_GRSTCTL = 0u;
    if ((USB_GRSTCTL & GRSTCTL_CSFTRST) == 0u &&
        blender_dwc2_wait_grstctl_mask(
            GRSTCTL_AHBIDLE, 1u, BLENDER_DWC2_POLL_LOOPS)) {
        USB_GRSTCTL = GRSTCTL_RXFFLSH;
        (void)blender_dwc2_wait_grstctl_mask(
            GRSTCTL_RXFFLSH, 0u, BLENDER_DWC2_POLL_LOOPS);

        USB_GRSTCTL =
            GRSTCTL_TXFFLSH | (BLENDER_DWC2_TXFNUM_ALL << GRSTCTL_TXFNUM_SHIFT);
        (void)blender_dwc2_wait_grstctl_mask(
            GRSTCTL_TXFFLSH, 0u, BLENDER_DWC2_POLL_LOOPS);
    }

    USB_GINTSTS = 0xFFFFFFFFu;
    USB_GOTGINT = 0xFFFFFFFFu;
    USB_DAINT = 0xFFFFFFFFu;
    USB_DCTL |= DCTL_CGNPINNAK | DCTL_CGOUTNAK;

    blender_periph_dwb();
    for (volatile int i = 0; i < 400000; i++) {}
}

static inline void blender_usb_compact_handoff_reset(void) {
    /* Minimal footprint variant used by SRAM-constrained payloads. */
    USB_GAHBCFG &= ~GAHBCFG_GLBLINTRMSK;
    USB_GINTMSK = 0;
    USB_DCTL |= DCTL_SFTDISCON;
    for (volatile unsigned i = 0; i < BLENDER_DWC2_POLL_LOOPS; i++) {}
    blender_periph_dwb();
}

static inline void blender_spi_ip_block_quiesce(volatile uint32_t *s) {
    for (volatile int i = 0; i < 100000; i++) {
        if (!(s[0x28 / 4] & 1u)) break;
    }
    s[0x10 / 4] = 0;
    s[0x08 / 4] = 0;
    s[0x2C / 4] = 0;
    s[0x4C / 4] = 0;
    s[0x50 / 4] = 0;
    s[0x54 / 4] = 0;
    s[0x00 / 4] = 0;
    s[0x04 / 4] = 0;
    s[0x18 / 4] = 0;
    s[0x34 / 4] = 0;
    blender_periph_dwb();
}

static inline void blender_spi_ip_drain_rx(volatile uint32_t *s) {
    for (int i = 0; i < 32; i++) {
        if (!(s[0x28 / 4] & 0x08u)) break;
        (void)s[0x60 / 4];
    }
}

static inline void blender_dma_engine_reset(volatile uint32_t *dma,
                                            unsigned channel_count) {
    dma[0x08 / 4] = 0;
    dma[0x10 / 4] = 0xFFu;
    for (unsigned ch = 0; ch < channel_count; ch++) {
        dma[0x30 / 4 + ch] = 1;
    }
    for (unsigned ch = 0; ch < channel_count; ch++) {
        volatile uint32_t *b = (volatile uint32_t *)(0x80000100u + ch * 0x20u);
        b[0x10 / 4] = 0;
        b[0x0C / 4] = 0;
        b[0x08 / 4] = 0;
        b[0x00 / 4] = 0;
        b[0x04 / 4] = 0;
    }
    dma[0x08 / 4] = 0;
    dma[0x10 / 4] = 0xFFu;
    blender_periph_dwb();
}

static inline void blender_timer_blocks_disable(void) {
    *(volatile uint32_t *)0xC2000008u = 0;
    *(volatile uint32_t *)0xC200002Cu = 0;
}

static inline void blender_mixer_block_quiesce(void) {
    volatile uint32_t *mx = (volatile uint32_t *)0xC4000000u;
    for (unsigned i = 0; i < 16; i++) mx[i] = 0;
    blender_periph_dwb();
}

static inline void blender_vic_clear(unsigned clear_vector_slots) {
    BLENDER_VIC_INT_EN_CLR = 0xFFFFFFFFu;
    BLENDER_VIC_SOFT_CLR = 0xFFFFFFFFu;
    if (clear_vector_slots) {
        for (unsigned i = 0; i < 16; i++) {
            BLENDER_VIC_VECT_SLOT(i) = 0;
            BLENDER_VIC_VECT_CNTL(i) = 0;
        }
        BLENDER_VIC_VECT_ADDR = 0;
    }
}

typedef struct {
    uint32_t spi_clk_div_raw;
    unsigned dma_channel_count;
    unsigned clear_vic_vector_slots;
} blender_periph_cfg_t;

static inline void blender_peripheral_full_teardown(
    const blender_periph_cfg_t *cfg, volatile uint32_t *flash_spi,
    volatile uint32_t *led_spi, volatile uint32_t *led_gpio_reg) {
    *(volatile uint32_t *)0xC9000014u = 0xABCD0000u | cfg->spi_clk_div_raw;
    blender_periph_dwb();

    blender_usb_warm_handoff_reset();

    blender_spi_ip_block_quiesce(led_spi);
    blender_spi_ip_drain_rx(led_spi);
    *led_gpio_reg = 0;
    led_spi[0x14 / 4] = 0xFF;

    blender_spi_ip_block_quiesce(flash_spi);
    blender_spi_ip_drain_rx(flash_spi);

    blender_dma_engine_reset(BLENDER_DMA_BASE, cfg->dma_channel_count);
    blender_vic_clear(cfg->clear_vic_vector_slots);
    blender_timer_blocks_disable();
    blender_mixer_block_quiesce();
    blender_periph_dwb();
}

#endif
