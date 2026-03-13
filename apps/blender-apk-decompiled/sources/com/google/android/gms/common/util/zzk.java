package com.google.android.gms.common.util;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.PowerManager;
import android.os.SystemClock;

/* JADX INFO: loaded from: classes.dex */
public final class zzk {
    private static long zzgkw;
    private static IntentFilter zzgkv = new IntentFilter("android.intent.action.BATTERY_CHANGED");
    private static float zzgkx = Float.NaN;

    @TargetApi(20)
    public static int zzcy(Context context) {
        if (context == null || context.getApplicationContext() == null) {
            return -1;
        }
        Intent intentRegisterReceiver = context.getApplicationContext().registerReceiver(null, zzgkv);
        int i = ((intentRegisterReceiver == null ? 0 : intentRegisterReceiver.getIntExtra("plugged", 0)) & 7) != 0 ? 1 : 0;
        PowerManager powerManager = (PowerManager) context.getSystemService("power");
        if (powerManager == null) {
            return -1;
        }
        return ((zzs.zzanw() ? powerManager.isInteractive() : powerManager.isScreenOn() ? 1 : 0) << 1) | i;
    }

    public static synchronized float zzcz(Context context) {
        if (SystemClock.elapsedRealtime() - zzgkw < 60000 && !Float.isNaN(zzgkx)) {
            return zzgkx;
        }
        if (context.getApplicationContext().registerReceiver(null, zzgkv) != null) {
            zzgkx = r6.getIntExtra("level", -1) / r6.getIntExtra("scale", -1);
        }
        zzgkw = SystemClock.elapsedRealtime();
        return zzgkx;
    }
}
