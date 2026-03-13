package com.google.android.gms.common.api.internal;

import com.google.android.gms.common.ConnectionResult;
import java.util.Collections;

/* JADX INFO: loaded from: classes.dex */
final class zzbv implements Runnable {
    private /* synthetic */ ConnectionResult zzgab;
    private /* synthetic */ zzbu zzgae;

    zzbv(zzbu zzbuVar, ConnectionResult connectionResult) {
        this.zzgae = zzbuVar;
        this.zzgab = connectionResult;
    }

    @Override // java.lang.Runnable
    public final void run() {
        if (!this.zzgab.isSuccess()) {
            ((zzbo) this.zzgae.zzfzq.zzfwg.get(this.zzgae.zzfsn)).onConnectionFailed(this.zzgab);
            return;
        }
        zzbu.zza(this.zzgae, true);
        if (this.zzgae.zzfwd.zzacc()) {
            this.zzgae.zzakp();
        } else {
            this.zzgae.zzfwd.zza(null, Collections.emptySet());
        }
    }
}
