package com.google.android.gms.common.stats;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.util.Log;
import java.util.Collections;
import java.util.List;

/* JADX INFO: loaded from: classes.dex */
public final class zza {
    private static volatile zza zzgjh;
    private final List<String> zzgjj = Collections.EMPTY_LIST;
    private final List<String> zzgjk = Collections.EMPTY_LIST;
    private final List<String> zzgjl = Collections.EMPTY_LIST;
    private final List<String> zzgjm = Collections.EMPTY_LIST;
    private static final Object zzggs = new Object();
    private static boolean zzgji = false;

    private zza() {
    }

    public static zza zzanm() {
        if (zzgjh == null) {
            synchronized (zzggs) {
                if (zzgjh == null) {
                    zzgjh = new zza();
                }
            }
        }
        return zzgjh;
    }

    public final boolean zza(Context context, Intent intent, ServiceConnection serviceConnection, int i) {
        return zza(context, context.getClass().getName(), intent, serviceConnection, i);
    }

    public final boolean zza(Context context, String str, Intent intent, ServiceConnection serviceConnection, int i) {
        ComponentName component = intent.getComponent();
        if (!(component == null ? false : com.google.android.gms.common.util.zzd.zzv(context, component.getPackageName()))) {
            return context.bindService(intent, serviceConnection, i);
        }
        Log.w("ConnectionTracker", "Attempted to bind to a service in a STOPPED package.");
        return false;
    }
}
