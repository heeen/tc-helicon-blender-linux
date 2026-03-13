package com.google.android.gms.internal;

import android.os.Handler;
import java.util.concurrent.Executor;

/* JADX INFO: loaded from: classes.dex */
public final class zzi implements zzaa {
    private final Executor zzv;

    public zzi(Handler handler) {
        this.zzv = new zzj(this, handler);
    }

    @Override // com.google.android.gms.internal.zzaa
    public final void zza(zzr<?> zzrVar, zzae zzaeVar) {
        zzrVar.zzb("post-error");
        this.zzv.execute(new zzk(this, zzrVar, zzx.zzc(zzaeVar), null));
    }

    @Override // com.google.android.gms.internal.zzaa
    public final void zza(zzr<?> zzrVar, zzx<?> zzxVar, Runnable runnable) {
        zzrVar.zzj();
        zzrVar.zzb("post-response");
        this.zzv.execute(new zzk(this, zzrVar, zzxVar, runnable));
    }

    @Override // com.google.android.gms.internal.zzaa
    public final void zzb(zzr<?> zzrVar, zzx<?> zzxVar) {
        zza(zzrVar, zzxVar, null);
    }
}
