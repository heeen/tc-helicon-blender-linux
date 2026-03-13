package com.google.android.gms.internal;

import java.io.IOException;

/* JADX INFO: loaded from: classes.dex */
public abstract class zzfls {
    protected volatile int zzpnr = -1;

    public static final <T extends zzfls> T zza(T t, byte[] bArr) throws zzflr {
        return (T) zza(t, bArr, 0, bArr.length);
    }

    private static <T extends zzfls> T zza(T t, byte[] bArr, int i, int i2) throws zzflr {
        try {
            zzflj zzfljVarZzo = zzflj.zzo(bArr, 0, i2);
            t.zza(zzfljVarZzo);
            zzfljVarZzo.zzlf(0);
            return t;
        } catch (zzflr e) {
            throw e;
        } catch (IOException e2) {
            throw new RuntimeException("Reading from a byte array threw an IOException (should never happen).", e2);
        }
    }

    public static final byte[] zzc(zzfls zzflsVar) {
        byte[] bArr = new byte[zzflsVar.zzhs()];
        try {
            zzflk zzflkVarZzp = zzflk.zzp(bArr, 0, bArr.length);
            zzflsVar.zza(zzflkVarZzp);
            zzflkVarZzp.zzcyx();
            return bArr;
        } catch (IOException e) {
            throw new RuntimeException("Serializing to a byte array threw an IOException (should never happen).", e);
        }
    }

    public String toString() {
        return zzflt.zzd(this);
    }

    public abstract zzfls zza(zzflj zzfljVar) throws IOException;

    public void zza(zzflk zzflkVar) throws IOException {
    }

    @Override // 
    /* JADX INFO: renamed from: zzdcl, reason: merged with bridge method [inline-methods] */
    public zzfls clone() throws CloneNotSupportedException {
        return (zzfls) super.clone();
    }

    public final int zzdcr() {
        if (this.zzpnr < 0) {
            zzhs();
        }
        return this.zzpnr;
    }

    public final int zzhs() {
        int iZzq = zzq();
        this.zzpnr = iZzq;
        return iZzq;
    }

    protected int zzq() {
        return 0;
    }
}
