package com.flurry.sdk;

import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.util.zip.CRC32;

/* JADX INFO: loaded from: classes.dex */
public final class jx extends MessageDigest {
    private CRC32 a;

    public jx() {
        super("CRC");
        this.a = new CRC32();
    }

    @Override // java.security.MessageDigestSpi
    protected final void engineReset() {
        this.a.reset();
    }

    @Override // java.security.MessageDigestSpi
    protected final void engineUpdate(byte b) {
        this.a.update(b);
    }

    @Override // java.security.MessageDigestSpi
    protected final void engineUpdate(byte[] bArr, int i, int i2) {
        this.a.update(bArr, i, i2);
    }

    @Override // java.security.MessageDigestSpi
    protected final byte[] engineDigest() {
        long value = this.a.getValue();
        return new byte[]{(byte) (((-16777216) & value) >> 24), (byte) ((16711680 & value) >> 16), (byte) ((65280 & value) >> 8), (byte) ((value & 255) >> 0)};
    }

    public final byte[] a() {
        return engineDigest();
    }

    public final int b() {
        return ByteBuffer.wrap(engineDigest()).getInt();
    }
}
