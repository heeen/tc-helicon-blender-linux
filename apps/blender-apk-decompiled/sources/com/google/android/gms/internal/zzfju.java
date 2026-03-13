package com.google.android.gms.internal;

import com.flurry.android.Constants;
import java.io.IOException;
import java.io.InputStream;

/* JADX INFO: loaded from: classes.dex */
final class zzfju extends InputStream {
    private int mark;
    private zzfjt zzpsa;
    private zzfgy zzpsb;
    private int zzpsc;
    private int zzpsd;
    private int zzpse;
    private /* synthetic */ zzfjq zzpsf;

    public zzfju(zzfjq zzfjqVar) {
        this.zzpsf = zzfjqVar;
        initialize();
    }

    private final void initialize() {
        this.zzpsa = new zzfjt(this.zzpsf);
        this.zzpsb = (zzfgy) this.zzpsa.next();
        this.zzpsc = this.zzpsb.size();
        this.zzpsd = 0;
        this.zzpse = 0;
    }

    private final void zzdbj() {
        if (this.zzpsb == null || this.zzpsd != this.zzpsc) {
            return;
        }
        this.zzpse += this.zzpsc;
        this.zzpsd = 0;
        if (this.zzpsa.hasNext()) {
            this.zzpsb = (zzfgy) this.zzpsa.next();
            this.zzpsc = this.zzpsb.size();
        } else {
            this.zzpsb = null;
            this.zzpsc = 0;
        }
    }

    private final int zzk(byte[] bArr, int i, int i2) {
        int i3 = i;
        int i4 = i2;
        while (true) {
            if (i4 <= 0) {
                break;
            }
            zzdbj();
            if (this.zzpsb != null) {
                int iMin = Math.min(this.zzpsc - this.zzpsd, i4);
                if (bArr != null) {
                    this.zzpsb.zza(bArr, this.zzpsd, i3, iMin);
                    i3 += iMin;
                }
                this.zzpsd += iMin;
                i4 -= iMin;
            } else if (i4 == i2) {
                return -1;
            }
        }
        return i2 - i4;
    }

    @Override // java.io.InputStream
    public final int available() throws IOException {
        return this.zzpsf.size() - (this.zzpse + this.zzpsd);
    }

    @Override // java.io.InputStream
    public final void mark(int i) {
        this.mark = this.zzpse + this.zzpsd;
    }

    @Override // java.io.InputStream
    public final boolean markSupported() {
        return true;
    }

    @Override // java.io.InputStream
    public final int read() throws IOException {
        zzdbj();
        if (this.zzpsb == null) {
            return -1;
        }
        zzfgy zzfgyVar = this.zzpsb;
        int i = this.zzpsd;
        this.zzpsd = i + 1;
        return zzfgyVar.zzld(i) & Constants.UNKNOWN;
    }

    @Override // java.io.InputStream
    public final int read(byte[] bArr, int i, int i2) {
        if (bArr == null) {
            throw new NullPointerException();
        }
        if (i < 0 || i2 < 0 || i2 > bArr.length - i) {
            throw new IndexOutOfBoundsException();
        }
        return zzk(bArr, i, i2);
    }

    @Override // java.io.InputStream
    public final synchronized void reset() {
        initialize();
        zzk(null, 0, this.mark);
    }

    @Override // java.io.InputStream
    public final long skip(long j) {
        if (j < 0) {
            throw new IndexOutOfBoundsException();
        }
        if (j > 2147483647L) {
            j = 2147483647L;
        }
        return zzk(null, 0, (int) j);
    }
}
