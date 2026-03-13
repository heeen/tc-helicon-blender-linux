package com.google.firebase.iid;

import android.os.Binder;
import android.os.Process;
import android.util.Log;
import com.google.android.gms.common.internal.Hide;

/* JADX INFO: loaded from: classes.dex */
@Hide
public final class zzf extends Binder {
    private final zzb zzokj;

    zzf(zzb zzbVar) {
        this.zzokj = zzbVar;
    }

    public final void zza(zzd zzdVar) {
        if (Binder.getCallingUid() != Process.myUid()) {
            throw new SecurityException("Binding only allowed within app");
        }
        if (Log.isLoggable("EnhancedIntentService", 3)) {
            Log.d("EnhancedIntentService", "service received new intent via bind strategy");
        }
        if (this.zzokj.zzq(zzdVar.intent)) {
            zzdVar.finish();
            return;
        }
        if (Log.isLoggable("EnhancedIntentService", 3)) {
            Log.d("EnhancedIntentService", "intent being queued for bg execution");
        }
        this.zzokj.zzimc.execute(new zzg(this, zzdVar));
    }
}
