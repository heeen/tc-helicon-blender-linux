/*
 * sram_flash_restore.c — eCos in-context SPI sector repair hook
 *
 * Runs as a displaced-instruction hook inside the eCos main loop thread.
 * The host patches `bl 0x14dc` at 0x2e48 to `bl 0x31C00` (this code).
 * On first call: erase + write corrupted sectors, set done flag.
 * On subsequent calls: skip repair, call original FUN_14dc, return.
 *
 * Because we execute inside a normal eCos thread context, everything
 * works: IRQs, scheduler, DMA, SPI driver — no fighting the RTOS.
 *
 * Build:
 *   arm-none-eabi-gcc -march=armv5te -marm -mno-thumb-interwork \
 *     -Os -nostdlib -ffreestanding -T firmware/sram_restore.ld \
 *     -o firmware/sram_flash_restore.elf firmware/sram_flash_restore.c
 *   arm-none-eabi-objcopy -O binary \
 *     firmware/sram_flash_restore.elf firmware/sram_flash_restore.bin
 *
 * SRAM layout (must match restore_sector.py):
 *   0x31B00  Mailbox (32 bytes)
 *   0x31C00  This code (capacity 0x600 = 1536 bytes)
 *   0x32200  Done flag (4 bytes)
 *   0x32210  Sector table (populated by host)
 *   0x32300  RTT control block (48 bytes)
 *   0x32340  RTT ring buffer (704 bytes)
 *   0x33000+ Sector data (4KB per sector, loaded by host)
 */

#include <stdint.h>

/* ── Firmware functions (addresses resolved by linker via firmware_symbols.ld) */

extern int  sst25xx_sector_erase(void *ctx, uint32_t addr);
extern int  sst25xx_aai_write(void *ctx, uint32_t addr, const void *buf, uint32_t len);
extern void dma_transfer_execute(void *dma_ch, uint32_t flag, uint32_t count,
                                  const void *tx_buf, void *rx_buf);
extern void led_animation_engine(void);
extern char sst25xx_driver_ctx;  /* linker symbol — take address with & */

#define DRIVER_CTX       ((void *)&sst25xx_driver_ctx)
#define DMA_CH           (*(void **)((uint8_t *)DRIVER_CTX + 0x18))
#define SECTOR_SIZE      0x1000

#define DONE_MAGIC       0x52455044  /* "REPD" */

/* ── Memory-mapped structures ────────────────────────────── */

struct mailbox {
    volatile uint32_t magic;      /* 0x464C5348 = "FLSH" when done */
    volatile uint32_t num_ops;
    volatile uint32_t num_ok;
    volatile uint32_t num_fail;
    volatile uint32_t last_r0;
    volatile uint32_t phase;      /* 0=init, 1=erase, 2=write, 3=done */
    volatile uint32_t cur_addr;
    volatile uint32_t reserved;
};

struct sector_entry {
    uint32_t spi_addr;
    uint32_t data_ptr;   /* SRAM address of 4KB data */
};

struct sector_table {
    uint32_t num_sectors;
    struct sector_entry entries[];
};

/* RTT control block — matches Segger RTT spec */
struct rtt_cb {
    char     id[16];             /* "SEGGER RTT\0..." */
    uint32_t max_up;             /* 1 */
    uint32_t max_down;           /* 0 */
    /* aUp[0]: */
    const char *name;
    char       *buf;
    uint32_t    buf_size;
    volatile uint32_t wr_off;
    volatile uint32_t rd_off;
    uint32_t    flags;
};

/* Fixed addresses (must match restore_sector.py) */
#define MAILBOX       ((struct mailbox *)     0x31B00)
#define DONE_FLAG     ((volatile uint32_t *)  0x32200)
#define SECTOR_TBL    ((struct sector_table *)0x32210)
#define RTT_CB_ADDR   ((struct rtt_cb *)      0x32300)
#define RTT_BUF       ((char *)               0x32340)
#define RTT_BUF_SIZE  704

/* ── RTT output functions ────────────────────────────────── */

static void rtt_init(void) {
    struct rtt_cb *cb = RTT_CB_ADDR;

    /* Magic: "SEGGER RTT\0\0\0\0\0\0" */
    cb->id[0]  = 'S'; cb->id[1]  = 'E'; cb->id[2]  = 'G'; cb->id[3]  = 'G';
    cb->id[4]  = 'E'; cb->id[5]  = 'R'; cb->id[6]  = ' '; cb->id[7]  = 'R';
    cb->id[8]  = 'T'; cb->id[9]  = 'T'; cb->id[10] = 0;   cb->id[11] = 0;
    cb->id[12] = 0;   cb->id[13] = 0;   cb->id[14] = 0;   cb->id[15] = 0;

    cb->max_up   = 1;
    cb->max_down = 0;
    cb->name     = "Terminal";
    cb->buf      = RTT_BUF;
    cb->buf_size = RTT_BUF_SIZE;
    cb->wr_off   = 0;
    cb->rd_off   = 0;
    cb->flags    = 0;  /* SKIP mode */
}

static void rtt_putchar(char c) {
    struct rtt_cb *cb = RTT_CB_ADDR;
    uint32_t wr = cb->wr_off;
    RTT_BUF[wr] = c;
    wr++;
    if (wr >= RTT_BUF_SIZE) wr = 0;
    cb->wr_off = wr;
}

static void rtt_puts(const char *s) {
    while (*s) rtt_putchar(*s++);
}

static void rtt_puthex32(uint32_t val) {
    static const char hex[] = "0123456789abcdef";
    rtt_putchar('0');
    rtt_putchar('x');
    for (int i = 28; i >= 0; i -= 4)
        rtt_putchar(hex[(val >> i) & 0xf]);
}

static void rtt_putdec(uint32_t val) {
    if (val == 0) { rtt_putchar('0'); return; }
    static const uint32_t powers[] = {1000000, 100000, 10000, 1000, 100, 10, 1};
    int started = 0;
    for (int i = 0; i < 7; i++) {
        uint32_t d = 0;
        while (val >= powers[i]) { val -= powers[i]; d++; }
        if (d || started) { rtt_putchar('0' + d); started = 1; }
    }
}

/* ── SPI flash block protection clear ────────────────────── */

/*
 * SST25VF016B requires EWSR (0x50) before WRSR to clear block protection.
 * Standard WREN (0x06) does NOT enable WRSR on this chip — only EWSR does.
 *
 * The firmware has no unlock function, and the eCos sst25xx_unlock() uses
 * WREN which is wrong for SST25VF016B. We send raw SPI commands via
 * dma_transfer_execute (the TCAT DMA-based SPI transfer function).
 *
 * Calling convention (from Ghidra disassembly of spi_flash_cmd_wren):
 *   r0 = DMA channel handle (driver_ctx + 0x18)
 *   r1 = 0
 *   r2 = byte count
 *   r3 = TX buffer pointer
 *   [sp+0] = RX buffer pointer (NULL for TX-only)
 */
static void clear_block_protection(void) {
    uint8_t ewsr = 0x50;
    uint8_t wrsr[2] = { 0x01, 0x00 };

    /* EWSR(0x50) + WRSR(0x01,0x00) — clear all BP bits.
     * SST25VF016B requires EWSR (not WREN) before WRSR. */
    dma_transfer_execute(DMA_CH, 0, 1, &ewsr, 0);
    dma_transfer_execute(DMA_CH, 0, 2, wrsr, 0);

    /* Wait for WRSR completion (max 10ms per datasheet) */
    for (volatile int i = 0; i < 500000; i++) { __asm__ volatile(""); }
}

/* ── Repair logic ────────────────────────────────────────── */

void do_repair(void) {
    struct mailbox *mb = MAILBOX;
    struct sector_table *tbl = SECTOR_TBL;

    /* Init mailbox */
    mb->magic    = 0;
    mb->num_ops  = 0;
    mb->num_ok   = 0;
    mb->num_fail = 0;
    mb->last_r0  = 0;
    mb->phase    = 0;
    mb->cur_addr = 0;

    /* Init RTT */
    rtt_init();
    rtt_puts("=== SPI Flash Repair (eCos hook) ===\n");

    /* Clear SPI flash block protection — SST25VF016B BP bits prevent erase */
    clear_block_protection();

    /* Report CPSR for diagnostics */
    {
        uint32_t cpsr;
        __asm__ volatile("mrs %0, cpsr" : "=r"(cpsr));
        rtt_puts("CPSR=");
        rtt_puthex32(cpsr);
        rtt_puts(" mode=");
        rtt_puthex32(cpsr & 0x1f);
        rtt_puts("\n");
    }

    rtt_puts("sectors=");
    rtt_putdec(tbl->num_sectors);
    rtt_putchar('\n');

    /* Process each sector */
    for (uint32_t i = 0; i < tbl->num_sectors; i++) {
        uint32_t spi_addr = tbl->entries[i].spi_addr;
        void    *data     = (void *)(uintptr_t)tbl->entries[i].data_ptr;
        int      ret;

        mb->cur_addr = spi_addr;

        /* ── Erase ───────────────────────────────────── */
        mb->phase = 1;
        rtt_puts("E ");
        rtt_puthex32(spi_addr);
        rtt_puts("...");

        ret = sst25xx_sector_erase(DRIVER_CTX, spi_addr);
        mb->last_r0 = (uint32_t)ret;
        mb->num_ops++;

        if (ret == 0) {
            mb->num_ok++;
            rtt_puts("OK\n");
        } else {
            mb->num_fail++;
            rtt_puts("FAIL r0=");
            rtt_puthex32((uint32_t)ret);
            rtt_puts("\n");
            continue;  /* skip write if erase failed */
        }

        /* ── Write ───────────────────────────────────── */
        mb->phase = 2;
        rtt_puts("W ");
        rtt_puthex32(spi_addr);
        rtt_puts("...");

        ret = sst25xx_aai_write(DRIVER_CTX, spi_addr, data, SECTOR_SIZE);
        mb->last_r0 = (uint32_t)ret;
        mb->num_ops++;

        if (ret == 0) {
            mb->num_ok++;
            rtt_puts("OK\n");
        } else {
            mb->num_fail++;
            rtt_puts("FAIL r0=");
            rtt_puthex32((uint32_t)ret);
            rtt_puts("\n");
        }
    }

    /* Summary */
    rtt_puts("DONE ");
    rtt_putdec(mb->num_ok);
    rtt_putchar('/');
    rtt_putdec(mb->num_ops);
    rtt_puts(" ops OK\n");

    /* Write completion */
    mb->phase = 3;
    mb->magic = 0x464C5348;  /* "FLSH" */
    *DONE_FLAG = DONE_MAGIC;
}

/* ── Entry point (naked trampoline) ──────────────────────── */
/*
 * Called via bl from 0x2c88 (replaces `bl 0x46c4` in golden firmware loop).
 * lr = return address in golden main loop (0x2c8c).
 *
 * On first call: run repair, then tail-call led_animation_engine.
 * On subsequent calls: just tail-call led_animation_engine.
 *
 * We save/restore r0-r3 (argument registers) so the displaced function
 * sees the exact same register state as before the hook.
 */
void _start(void) __attribute__((naked, section(".text.entry")));
void _start(void) {
    __asm__ volatile(
        "push  {r0-r3, r4-r11, lr}\n" /* save args + callee-saved + lr */
        /* Check done flag */
        "ldr   r0, =0x32080\n"        /* DONE_FLAG address */
        "ldr   r1, [r0]\n"
        "ldr   r2, =0x52455044\n"     /* DONE_MAGIC */
        "cmp   r1, r2\n"
        "beq   1f\n"                  /* skip repair if already done */
        "bl    do_repair\n"
        "1:\n"
        "pop   {r0-r3, r4-r11, lr}\n" /* restore everything */
        "b     led_animation_engine\n"            /* tail-call displaced function */
    );
}
