package com.google.android.gms.internal;

/* JADX INFO: loaded from: classes.dex */
final class zzfgv extends zzfgz {
    private final int zzpnz;
    private final int zzpoa;

    zzfgv(byte[] bArr, int i, int i2) {
        super(bArr);
        zzh(i, i + i2, bArr.length);
        this.zzpnz = i;
        this.zzpoa = i2;
    }

    @Override // com.google.android.gms.internal.zzfgz, com.google.android.gms.internal.zzfgs
    public final int size() {
        return this.zzpoa;
    }

    @Override // com.google.android.gms.internal.zzfgz, com.google.android.gms.internal.zzfgs
    protected final void zzb(byte[] bArr, int i, int i2, int i3) {
        System.arraycopy(this.zzjwl, zzcxu() + i, bArr, i2, i3);
    }

    @Override // com.google.android.gms.internal.zzfgz
    protected final int zzcxu() {
        return this.zzpnz;
    }

    @Override // com.google.android.gms.internal.zzfgz, com.google.android.gms.internal.zzfgs
    public final byte zzld(int i) {
        zzab(i, size());
        return this.zzjwl[this.zzpnz + i];
    }
}
