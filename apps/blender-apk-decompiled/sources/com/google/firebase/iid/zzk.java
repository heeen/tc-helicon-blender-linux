package com.google.firebase.iid;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.VisibleForTesting;
import android.util.Log;
import com.google.android.gms.common.internal.Hide;
import com.google.android.gms.tasks.Task;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

/* JADX INFO: loaded from: classes.dex */
@Hide
public final class zzk {
    private static zzk zzokw;
    private final Context zzaiq;
    private final ScheduledExecutorService zzind;
    private zzm zzokx = new zzm(this);
    private int zzinf = 1;

    @VisibleForTesting
    private zzk(Context context, ScheduledExecutorService scheduledExecutorService) {
        this.zzind = scheduledExecutorService;
        this.zzaiq = context.getApplicationContext();
    }

    private final synchronized <T> Task<T> zza(zzt<T> zztVar) {
        if (Log.isLoggable("MessengerIpcClient", 3)) {
            String strValueOf = String.valueOf(zztVar);
            StringBuilder sb = new StringBuilder(String.valueOf(strValueOf).length() + 9);
            sb.append("Queueing ");
            sb.append(strValueOf);
            Log.d("MessengerIpcClient", sb.toString());
        }
        if (!this.zzokx.zzb(zztVar)) {
            this.zzokx = new zzm(this);
            this.zzokx.zzb(zztVar);
        }
        return zztVar.zzgyc.getTask();
    }

    private final synchronized int zzaws() {
        int i;
        i = this.zzinf;
        this.zzinf = i + 1;
        return i;
    }

    public static synchronized zzk zzfa(Context context) {
        if (zzokw == null) {
            zzokw = new zzk(context, Executors.newSingleThreadScheduledExecutor());
        }
        return zzokw;
    }

    public final Task<Bundle> zzj(int i, Bundle bundle) {
        return zza(new zzv(zzaws(), 1, bundle));
    }

    public final Task<Void> zzm(int i, Bundle bundle) {
        return zza(new zzs(zzaws(), 2, bundle));
    }
}
