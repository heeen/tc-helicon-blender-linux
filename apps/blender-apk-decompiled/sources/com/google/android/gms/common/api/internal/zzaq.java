package com.google.android.gms.common.api.internal;

import android.os.Looper;
import android.support.annotation.NonNull;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.Api;
import java.lang.ref.WeakReference;

/* JADX INFO: loaded from: classes.dex */
final class zzaq implements com.google.android.gms.common.internal.zzj {
    private final Api<?> zzfop;
    private final boolean zzfvo;
    private final WeakReference<zzao> zzfxu;

    public zzaq(zzao zzaoVar, Api<?> api, boolean z) {
        this.zzfxu = new WeakReference<>(zzaoVar);
        this.zzfop = api;
        this.zzfvo = z;
    }

    @Override // com.google.android.gms.common.internal.zzj
    public final void zzf(@NonNull ConnectionResult connectionResult) {
        zzao zzaoVar = this.zzfxu.get();
        if (zzaoVar == null) {
            return;
        }
        com.google.android.gms.common.internal.zzbq.zza(Looper.myLooper() == zzaoVar.zzfxd.zzfvq.getLooper(), "onReportServiceBinding must be called on the GoogleApiClient handler thread");
        zzaoVar.zzfwa.lock();
        try {
            if (zzaoVar.zzbs(0)) {
                if (!connectionResult.isSuccess()) {
                    zzaoVar.zzb(connectionResult, this.zzfop, this.zzfvo);
                }
                if (zzaoVar.zzajk()) {
                    zzaoVar.zzajl();
                }
            }
        } finally {
            zzaoVar.zzfwa.unlock();
        }
    }
}
