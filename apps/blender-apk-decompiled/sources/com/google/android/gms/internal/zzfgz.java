package com.google.android.gms.internal;

import java.io.IOException;

/* JADX INFO: loaded from: classes.dex */
class zzfgz extends zzfgy {
    protected final byte[] zzjwl;

    zzfgz(byte[] bArr) {
        this.zzjwl = bArr;
    }

    @Override // com.google.android.gms.internal.zzfgs
    public final boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (!(obj instanceof zzfgs) || size() != ((zzfgs) obj).size()) {
            return false;
        }
        if (size() == 0) {
            return true;
        }
        if (!(obj instanceof zzfgz)) {
            return obj.equals(this);
        }
        zzfgz zzfgzVar = (zzfgz) obj;
        int iZzcxt = zzcxt();
        int iZzcxt2 = zzfgzVar.zzcxt();
        if (iZzcxt == 0 || iZzcxt2 == 0 || iZzcxt == iZzcxt2) {
            return zza(zzfgzVar, 0, size());
        }
        return false;
    }

    @Override // com.google.android.gms.internal.zzfgs
    public int size() {
        return this.zzjwl.length;
    }

    @Override // com.google.android.gms.internal.zzfgs
    final void zza(zzfgr zzfgrVar) throws IOException {
        zzfgrVar.zze(this.zzjwl, zzcxu(), size());
    }

    @Override // com.google.android.gms.internal.zzfgy
    final boolean zza(zzfgs zzfgsVar, int i, int i2) {
        if (i2 > zzfgsVar.size()) {
            int size = size();
            StringBuilder sb = new StringBuilder(40);
            sb.append("Length too large: ");
            sb.append(i2);
            sb.append(size);
            throw new IllegalArgumentException(sb.toString());
        }
        int i3 = i + i2;
        if (i3 > zzfgsVar.size()) {
            int size2 = zzfgsVar.size();
            StringBuilder sb2 = new StringBuilder(59);
            sb2.append("Ran off end of other: ");
            sb2.append(i);
            sb2.append(", ");
            sb2.append(i2);
            sb2.append(", ");
            sb2.append(size2);
            throw new IllegalArgumentException(sb2.toString());
        }
        if (!(zzfgsVar instanceof zzfgz)) {
            return zzfgsVar.zzaa(i, i3).equals(zzaa(0, i2));
        }
        zzfgz zzfgzVar = (zzfgz) zzfgsVar;
        byte[] bArr = this.zzjwl;
        byte[] bArr2 = zzfgzVar.zzjwl;
        int iZzcxu = zzcxu() + i2;
        int iZzcxu2 = zzcxu();
        int iZzcxu3 = zzfgzVar.zzcxu() + i;
        while (iZzcxu2 < iZzcxu) {
            if (bArr[iZzcxu2] != bArr2[iZzcxu3]) {
                return false;
            }
            iZzcxu2++;
            iZzcxu3++;
        }
        return true;
    }

    @Override // com.google.android.gms.internal.zzfgs
    public final zzfgs zzaa(int i, int i2) {
        int iZzh = zzh(i, i2, size());
        return iZzh == 0 ? zzfgs.zzpnw : new zzfgv(this.zzjwl, zzcxu() + i, iZzh);
    }

    @Override // com.google.android.gms.internal.zzfgs
    protected void zzb(byte[] bArr, int i, int i2, int i3) {
        System.arraycopy(this.zzjwl, i, bArr, i2, i3);
    }

    @Override // com.google.android.gms.internal.zzfgs
    public final zzfhb zzcxq() {
        return zzfhb.zzb(this.zzjwl, zzcxu(), size(), true);
    }

    protected int zzcxu() {
        return 0;
    }

    @Override // com.google.android.gms.internal.zzfgs
    protected final int zzg(int i, int i2, int i3) {
        return zzfhz.zza(i, this.zzjwl, zzcxu() + i2, i3);
    }

    @Override // com.google.android.gms.internal.zzfgs
    public byte zzld(int i) {
        return this.zzjwl[i];
    }
}
