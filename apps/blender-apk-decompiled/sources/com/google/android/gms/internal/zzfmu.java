package com.google.android.gms.internal;

import java.io.IOException;

/* JADX INFO: loaded from: classes.dex */
public final class zzfmu extends zzflm<zzfmu> {
    private static volatile zzfmu[] zzpzr;
    public String zzpzs = "";

    public zzfmu() {
        this.zzpvl = null;
        this.zzpnr = -1;
    }

    public static zzfmu[] zzddi() {
        if (zzpzr == null) {
            synchronized (zzflq.zzpvt) {
                if (zzpzr == null) {
                    zzpzr = new zzfmu[0];
                }
            }
        }
        return zzpzr;
    }

    @Override // com.google.android.gms.internal.zzfls
    public final /* synthetic */ zzfls zza(zzflj zzfljVar) throws IOException {
        while (true) {
            int iZzcxx = zzfljVar.zzcxx();
            if (iZzcxx == 0) {
                return this;
            }
            if (iZzcxx == 10) {
                this.zzpzs = zzfljVar.readString();
            } else if (!super.zza(zzfljVar, iZzcxx)) {
                return this;
            }
        }
    }

    @Override // com.google.android.gms.internal.zzflm, com.google.android.gms.internal.zzfls
    public final void zza(zzflk zzflkVar) throws IOException {
        if (this.zzpzs != null && !this.zzpzs.equals("")) {
            zzflkVar.zzp(1, this.zzpzs);
        }
        super.zza(zzflkVar);
    }

    @Override // com.google.android.gms.internal.zzflm, com.google.android.gms.internal.zzfls
    protected final int zzq() {
        int iZzq = super.zzq();
        return (this.zzpzs == null || this.zzpzs.equals("")) ? iZzq : iZzq + zzflk.zzq(1, this.zzpzs);
    }
}
