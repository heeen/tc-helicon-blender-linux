package com.google.android.gms.internal;

import com.google.android.gms.internal.zzflm;
import java.io.IOException;

/* JADX INFO: loaded from: classes.dex */
public abstract class zzflm<M extends zzflm<M>> extends zzfls {
    protected zzflo zzpvl;

    public final <T> T zza(zzfln<M, T> zzflnVar) {
        zzflp zzflpVarZzmz;
        if (this.zzpvl == null || (zzflpVarZzmz = this.zzpvl.zzmz(zzflnVar.tag >>> 3)) == null) {
            return null;
        }
        return (T) zzflpVarZzmz.zzb(zzflnVar);
    }

    @Override // com.google.android.gms.internal.zzfls
    public void zza(zzflk zzflkVar) throws IOException {
        if (this.zzpvl == null) {
            return;
        }
        for (int i = 0; i < this.zzpvl.size(); i++) {
            this.zzpvl.zzna(i).zza(zzflkVar);
        }
    }

    protected final boolean zza(zzflj zzfljVar, int i) throws IOException {
        int position = zzfljVar.getPosition();
        if (!zzfljVar.zzlg(i)) {
            return false;
        }
        int i2 = i >>> 3;
        zzflu zzfluVar = new zzflu(i, zzfljVar.zzao(position, zzfljVar.getPosition() - position));
        zzflp zzflpVarZzmz = null;
        if (this.zzpvl == null) {
            this.zzpvl = new zzflo();
        } else {
            zzflpVarZzmz = this.zzpvl.zzmz(i2);
        }
        if (zzflpVarZzmz == null) {
            zzflpVarZzmz = new zzflp();
            this.zzpvl.zza(i2, zzflpVarZzmz);
        }
        zzflpVarZzmz.zza(zzfluVar);
        return true;
    }

    @Override // com.google.android.gms.internal.zzfls
    /* JADX INFO: renamed from: zzdck, reason: merged with bridge method [inline-methods] */
    public M clone() throws CloneNotSupportedException {
        M m = (M) super.clone();
        zzflq.zza(this, m);
        return m;
    }

    @Override // com.google.android.gms.internal.zzfls
    /* JADX INFO: renamed from: zzdcl */
    public /* synthetic */ zzfls clone() throws CloneNotSupportedException {
        return (zzflm) clone();
    }

    @Override // com.google.android.gms.internal.zzfls
    protected int zzq() {
        if (this.zzpvl == null) {
            return 0;
        }
        int iZzq = 0;
        for (int i = 0; i < this.zzpvl.size(); i++) {
            iZzq += this.zzpvl.zzna(i).zzq();
        }
        return iZzq;
    }
}
