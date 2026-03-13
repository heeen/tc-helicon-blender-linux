package com.google.android.gms.internal;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;

/* JADX INFO: loaded from: classes.dex */
final class zzao extends FilterInputStream {
    private long bytesRead;
    private final long zzcb;

    zzao(InputStream inputStream, long j) {
        super(inputStream);
        this.zzcb = j;
    }

    @Override // java.io.FilterInputStream, java.io.InputStream
    public final int read() throws IOException {
        int i = super.read();
        if (i != -1) {
            this.bytesRead++;
        }
        return i;
    }

    @Override // java.io.FilterInputStream, java.io.InputStream
    public final int read(byte[] bArr, int i, int i2) throws IOException {
        int i3 = super.read(bArr, i, i2);
        if (i3 != -1) {
            this.bytesRead += (long) i3;
        }
        return i3;
    }

    final long zzn() {
        return this.zzcb - this.bytesRead;
    }
}
