
MEMORY { STUB (rx) : ORIGIN = 0x2aa88, LENGTH = 152 }
SECTIONS { .text : { *(.text*) } > STUB /DISCARD/ : { *(.ARM.*) *(.comment) } }
