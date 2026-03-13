package com.google.android.gms.common.api.internal;

import android.os.Looper;
import android.support.annotation.NonNull;

/* JADX INFO: loaded from: classes.dex */
public final class zzci<L> {
    private final zzcj zzgas;
    private volatile L zzgat;
    private final zzck<L> zzgau;

    zzci(@NonNull Looper looper, @NonNull L l, @NonNull String str) {
        this.zzgas = new zzcj(this, looper);
        this.zzgat = (L) com.google.android.gms.common.internal.zzbq.checkNotNull(l, "Listener must not be null");
        this.zzgau = new zzck<>(l, com.google.android.gms.common.internal.zzbq.zzgv(str));
    }

    public final void clear() {
        this.zzgat = null;
    }

    public final void zza(zzcl<? super L> zzclVar) {
        com.google.android.gms.common.internal.zzbq.checkNotNull(zzclVar, "Notifier must not be null");
        this.zzgas.sendMessage(this.zzgas.obtainMessage(1, zzclVar));
    }

    public final boolean zzafr() {
        return this.zzgat != null;
    }

    @NonNull
    public final zzck<L> zzakx() {
        return this.zzgau;
    }

    final void zzb(zzcl<? super L> zzclVar) {
        L l = this.zzgat;
        if (l == null) {
            zzclVar.zzajh();
            return;
        }
        try {
            zzclVar.zzu(l);
        } catch (RuntimeException e) {
            zzclVar.zzajh();
            throw e;
        }
    }
}
