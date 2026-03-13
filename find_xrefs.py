from ghidra.program.model.symbol import ReferenceManager
from ghidra.program.model.address import AddressFactory

af = currentProgram.getAddressFactory()
rm = currentProgram.getReferenceManager()
fm = currentProgram.getFunctionManager()

targets = [0x23042, 0x26f2a, 0x26e62, 0x26eb9, 0x23d2c]

for addr_val in targets:
    addr = af.getDefaultAddressSpace().getAddress(addr_val)
    refs = rm.getReferencesTo(addr)
    println("=== Xrefs to 0x%x ===" % addr_val)
    for ref in refs:
        from_addr = ref.getFromAddress()
        func = fm.getFunctionContaining(from_addr)
        func_name = func.getName() if func else "unknown"
        func_entry = func.getEntryPoint() if func else "?"
        println("  From: %s in %s @ %s" % (from_addr, func_name, func_entry))
