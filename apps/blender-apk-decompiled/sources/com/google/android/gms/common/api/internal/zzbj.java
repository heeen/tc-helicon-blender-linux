package com.google.android.gms.common.api.internal;

/* JADX INFO: loaded from: classes.dex */
abstract class zzbj {
    private final zzbh zzfzd;

    protected zzbj(zzbh zzbhVar) {
        this.zzfzd = zzbhVar;
    }

    protected abstract void zzajj();

    public final void zzc(zzbi zzbiVar) {
        zzbiVar.zzfwa.lock();
        try {
            if (zzbiVar.zzfyz == this.zzfzd) {
                zzajj();
            }
        } finally {
            zzbiVar.zzfwa.unlock();
        }
    }
}
