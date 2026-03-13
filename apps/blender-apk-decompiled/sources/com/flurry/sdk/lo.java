package com.flurry.sdk;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;

/* JADX INFO: loaded from: classes.dex */
public final class lo {
    private static final String a = "lo";

    private static PackageInfo d(Context context) {
        if (context != null) {
            try {
                return context.getPackageManager().getPackageInfo(context.getPackageName(), 20815);
            } catch (PackageManager.NameNotFoundException unused) {
                kf.a(a, "Cannot find package info for package: " + context.getPackageName());
            }
        }
        return null;
    }

    private static ApplicationInfo e(Context context) {
        if (context != null) {
            try {
                return context.getPackageManager().getApplicationInfo(context.getPackageName(), 128);
            } catch (PackageManager.NameNotFoundException unused) {
                kf.a(a, "Cannot find application info for package: " + context.getPackageName());
            }
        }
        return null;
    }

    public static String a(Context context) {
        PackageInfo packageInfoD = d(context);
        return (packageInfoD == null || packageInfoD.packageName == null) ? "" : packageInfoD.packageName;
    }

    public static String b(Context context) {
        PackageInfo packageInfoD = d(context);
        return (packageInfoD == null || packageInfoD.versionName == null) ? "" : packageInfoD.versionName;
    }

    public static Bundle c(Context context) {
        ApplicationInfo applicationInfoE = e(context);
        return (applicationInfoE == null || applicationInfoE.metaData == null) ? Bundle.EMPTY : applicationInfoE.metaData;
    }
}
