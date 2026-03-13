package com.google.android.gms.common.api;

import android.os.Looper;
import com.google.android.gms.common.api.GoogleApi;
import com.google.android.gms.common.api.internal.zzda;
import com.google.android.gms.common.internal.zzbq;

/* JADX INFO: loaded from: classes.dex */
public final class zzd {
    private Looper zzalj;
    private zzda zzfsp;

    public final zzd zza(Looper looper) {
        zzbq.checkNotNull(looper, "Looper must not be null.");
        this.zzalj = looper;
        return this;
    }

    public final zzd zza(zzda zzdaVar) {
        zzbq.checkNotNull(zzdaVar, "StatusExceptionMapper must not be null.");
        this.zzfsp = zzdaVar;
        return this;
    }

    public final GoogleApi.zza zzahy() {
        if (this.zzfsp == null) {
            this.zzfsp = new com.google.android.gms.common.api.internal.zzg();
        }
        if (this.zzalj == null) {
            this.zzalj = Looper.getMainLooper();
        }
        return new GoogleApi.zza(this.zzfsp, this.zzalj);
    }
}
