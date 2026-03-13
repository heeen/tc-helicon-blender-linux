package com.google.android.gms.flags.impl;

import android.content.Context;
import android.content.SharedPreferences;
import com.google.android.gms.common.internal.Hide;
import com.google.android.gms.internal.zzccq;

/* JADX INFO: loaded from: classes.dex */
@Hide
public final class zzj {
    private static SharedPreferences zzhqo;

    public static SharedPreferences zzdk(Context context) throws Exception {
        SharedPreferences sharedPreferences;
        synchronized (SharedPreferences.class) {
            if (zzhqo == null) {
                zzhqo = (SharedPreferences) zzccq.zzb(new zzk(context));
            }
            sharedPreferences = zzhqo;
        }
        return sharedPreferences;
    }
}
