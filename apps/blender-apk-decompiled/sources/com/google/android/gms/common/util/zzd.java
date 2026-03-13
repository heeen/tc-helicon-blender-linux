package com.google.android.gms.common.util;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.annotation.Nullable;
import com.google.android.gms.common.internal.Hide;
import com.google.android.gms.internal.zzbih;

/* JADX INFO: loaded from: classes.dex */
@Hide
public final class zzd {
    public static int zzt(Context context, String str) {
        Bundle bundle;
        PackageInfo packageInfoZzu = zzu(context, str);
        if (packageInfoZzu == null || packageInfoZzu.applicationInfo == null || (bundle = packageInfoZzu.applicationInfo.metaData) == null) {
            return -1;
        }
        return bundle.getInt("com.google.android.gms.version", -1);
    }

    @Nullable
    private static PackageInfo zzu(Context context, String str) {
        try {
            return zzbih.zzdd(context).getPackageInfo(str, 128);
        } catch (PackageManager.NameNotFoundException unused) {
            return null;
        }
    }

    public static boolean zzv(Context context, String str) {
        "com.google.android.gms".equals(str);
        return (zzbih.zzdd(context).getApplicationInfo(str, 0).flags & 2097152) != 0;
    }
}
