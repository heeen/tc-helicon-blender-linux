ENTRY(_start)
SECTIONS {
    . = 0x2C000;
    .text.entry : { *(.text.entry) }
    .text : { *(.text .text.* .rodata .rodata.*) }
    .data : { *(.data .data.*) }
    .bss : { *(.bss .bss.* COMMON) }
    /DISCARD/ : { *(.ARM.attributes .comment) }
}
