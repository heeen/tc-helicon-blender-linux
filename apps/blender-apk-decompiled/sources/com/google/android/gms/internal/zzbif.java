package com.google.android.gms.internal;

import android.content.Context;

/* JADX INFO: loaded from: classes.dex */
public final class zzbif {
    private static Context zzglq;
    private static Boolean zzglr;

    public static synchronized boolean zzdb(Context context) {
        boolean zValueOf;
        Context applicationContext = context.getApplicationContext();
        if (zzglq != null && zzglr != null && zzglq == applicationContext) {
            return zzglr.booleanValue();
        }
        zzglr = null;
        if (!com.google.android.gms.common.util.zzs.isAtLeastO()) {
            try {
                context.getClassLoader().loadClass("com.google.android.instantapps.supervisor.InstantAppsRuntime");
                zzglr = true;
            } catch (ClassNotFoundException unused) {
                zValueOf = false;
                zzglr = zValueOf;
            }
            zzglq = applicationContext;
            return zzglr.booleanValue();
        }
        zValueOf = Boolean.valueOf(applicationContext.getPackageManager().isInstantApp());
        zzglr = zValueOf;
        zzglq = applicationContext;
        return zzglr.booleanValue();
    }
}
