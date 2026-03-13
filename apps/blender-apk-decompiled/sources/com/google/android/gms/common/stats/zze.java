package com.google.android.gms.common.stats;

import android.content.Context;
import android.content.Intent;
import android.os.SystemClock;
import android.text.TextUtils;
import android.util.Log;
import com.google.android.gms.common.util.zzk;
import java.util.List;

/* JADX INFO: loaded from: classes.dex */
public final class zze {
    private static Boolean zzgkj;
    private static zze zzgki = new zze();
    private static boolean zzgji = false;

    public static void zza(Context context, String str, int i, String str2, String str3, String str4, int i2, List<String> list) {
        zza(context, str, 8, str2, str3, str4, i2, list, 0L);
    }

    public static void zza(Context context, String str, int i, String str2, String str3, String str4, int i2, List<String> list, long j) {
        List<String> list2 = list;
        if (zzgkj == null) {
            zzgkj = false;
        }
        if (zzgkj.booleanValue()) {
            if (TextUtils.isEmpty(str)) {
                String strValueOf = String.valueOf(str);
                Log.e("WakeLockTracker", strValueOf.length() != 0 ? "missing wakeLock key. ".concat(strValueOf) : new String("missing wakeLock key. "));
                return;
            }
            long jCurrentTimeMillis = System.currentTimeMillis();
            if (7 == i || 8 == i || 10 == i || 11 == i) {
                if (list2 != null && list.size() == 1 && "com.google.android.gms".equals(list2.get(0))) {
                    list2 = null;
                }
                List<String> list3 = list2;
                long jElapsedRealtime = SystemClock.elapsedRealtime();
                int iZzcy = zzk.zzcy(context);
                String packageName = context.getPackageName();
                try {
                    context.startService(new Intent().setComponent(zzb.zzgjn).putExtra("com.google.android.gms.common.stats.EXTRA_LOG_EVENT", new WakeLockEvent(jCurrentTimeMillis, i, str2, i2, list3, str, jElapsedRealtime, iZzcy, str3, "com.google.android.gms".equals(packageName) ? null : packageName, zzk.zzcz(context), j, str4)));
                } catch (Exception e) {
                    Log.wtf("WakeLockTracker", e);
                }
            }
        }
    }

    public static zze zzanp() {
        return zzgki;
    }
}
