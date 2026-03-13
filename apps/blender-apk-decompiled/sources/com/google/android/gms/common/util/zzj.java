package com.google.android.gms.common.util;

import android.annotation.TargetApi;
import android.content.Context;

/* JADX INFO: loaded from: classes.dex */
public final class zzj {
    private static Boolean zzgkq;
    private static Boolean zzgkr;
    private static Boolean zzgks;
    private static Boolean zzgkt;
    private static Boolean zzgku;

    /* JADX WARN: Removed duplicated region for block: B:23:0x003e  */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
        To view partially-correct add '--show-bad-code' argument
    */
    public static boolean zza(android.content.res.Resources r4) {
        /*
            r0 = 0
            if (r4 != 0) goto L4
            return r0
        L4:
            java.lang.Boolean r1 = com.google.android.gms.common.util.zzj.zzgkq
            if (r1 != 0) goto L45
            android.content.res.Configuration r1 = r4.getConfiguration()
            int r1 = r1.screenLayout
            r1 = r1 & 15
            r2 = 3
            r3 = 1
            if (r1 <= r2) goto L16
            r1 = r3
            goto L17
        L16:
            r1 = r0
        L17:
            if (r1 != 0) goto L3e
            java.lang.Boolean r1 = com.google.android.gms.common.util.zzj.zzgkr
            if (r1 != 0) goto L36
            android.content.res.Configuration r4 = r4.getConfiguration()
            int r1 = r4.screenLayout
            r1 = r1 & 15
            if (r1 > r2) goto L2f
            int r4 = r4.smallestScreenWidthDp
            r1 = 600(0x258, float:8.41E-43)
            if (r4 < r1) goto L2f
            r4 = r3
            goto L30
        L2f:
            r4 = r0
        L30:
            java.lang.Boolean r4 = java.lang.Boolean.valueOf(r4)
            com.google.android.gms.common.util.zzj.zzgkr = r4
        L36:
            java.lang.Boolean r4 = com.google.android.gms.common.util.zzj.zzgkr
            boolean r4 = r4.booleanValue()
            if (r4 == 0) goto L3f
        L3e:
            r0 = r3
        L3f:
            java.lang.Boolean r4 = java.lang.Boolean.valueOf(r0)
            com.google.android.gms.common.util.zzj.zzgkq = r4
        L45:
            java.lang.Boolean r4 = com.google.android.gms.common.util.zzj.zzgkq
            boolean r4 = r4.booleanValue()
            return r4
        */
        throw new UnsupportedOperationException("Method not decompiled: com.google.android.gms.common.util.zzj.zza(android.content.res.Resources):boolean");
    }

    @TargetApi(20)
    public static boolean zzcu(Context context) {
        if (zzgks == null) {
            zzgks = Boolean.valueOf(zzs.zzanw() && context.getPackageManager().hasSystemFeature("android.hardware.type.watch"));
        }
        return zzgks.booleanValue();
    }

    @TargetApi(24)
    public static boolean zzcv(Context context) {
        return (!zzs.isAtLeastN() || zzcw(context)) && zzcu(context);
    }

    @TargetApi(21)
    public static boolean zzcw(Context context) {
        if (zzgkt == null) {
            zzgkt = Boolean.valueOf(zzs.zzanx() && context.getPackageManager().hasSystemFeature("cn.google"));
        }
        return zzgkt.booleanValue();
    }

    public static boolean zzcx(Context context) {
        if (zzgku == null) {
            zzgku = Boolean.valueOf(context.getPackageManager().hasSystemFeature("android.hardware.type.iot") || context.getPackageManager().hasSystemFeature("android.hardware.type.embedded"));
        }
        return zzgku.booleanValue();
    }
}
