import ghidra.app.script.GhidraScript;
import ghidra.program.model.address.*;
import ghidra.program.model.symbol.*;
import ghidra.program.model.listing.*;

public class FindXrefs extends GhidraScript {
    @Override
    public void run() throws Exception {
        ReferenceManager rm = currentProgram.getReferenceManager();
        FunctionManager fm = currentProgram.getFunctionManager();
        AddressFactory af = currentProgram.getAddressFactory();
        AddressSpace space = af.getDefaultAddressSpace();

        long[] targets = {0x23042, 0x26f2a, 0x26e62, 0x26eb9};
        String[] names = {"TCH-BLE", "com_port_error", "fss.c", "alloc_buffer_error"};

        for (int i = 0; i < targets.length; i++) {
            Address addr = space.getAddress(targets[i]);
            println("=== Xrefs to " + names[i] + " (0x" + Long.toHexString(targets[i]) + ") ===");
            Reference[] refs = rm.getReferencesTo(addr);
            for (Reference ref : refs) {
                Address fromAddr = ref.getFromAddress();
                Function func = fm.getFunctionContaining(fromAddr);
                String funcName = func != null ? func.getName() : "unknown";
                String funcEntry = func != null ? func.getEntryPoint().toString() : "?";
                println("  From: " + fromAddr + " in " + funcName + " @ " + funcEntry);
            }
        }
    }
}
