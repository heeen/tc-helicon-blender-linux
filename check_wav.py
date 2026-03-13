import struct, wave, sys
w = wave.open(sys.argv[1], "rb")
f = w.readframes(min(w.getnframes(), 4800))
w.close()
s = struct.unpack("<" + "i" * (len(f) // 4), f)
mx = max(abs(x) for x in s)
nz = sum(1 for x in s if x != 0)
print(f"max={mx}, nonzero={nz}/{len(s)}")
