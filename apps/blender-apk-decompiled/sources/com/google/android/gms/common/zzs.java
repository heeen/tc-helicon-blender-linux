package com.google.android.gms.common;

import android.annotation.TargetApi;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageInstaller;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.os.Build;
import android.os.Bundle;
import android.os.UserManager;
import android.util.Log;
import com.google.android.gms.R;
import com.google.android.gms.common.internal.Hide;
import com.google.android.gms.common.internal.zzbf;
import com.google.android.gms.common.util.zzz;
import com.google.android.gms.internal.zzbih;
import com.onesignal.OneSignalDbContract;
import java.util.Iterator;
import java.util.concurrent.atomic.AtomicBoolean;

/* JADX INFO: loaded from: classes.dex */
@Hide
public class zzs {

    @Deprecated
    public static final String GOOGLE_PLAY_SERVICES_PACKAGE = "com.google.android.gms";

    @Deprecated
    public static final int GOOGLE_PLAY_SERVICES_VERSION_CODE = 12211000;
    public static final String GOOGLE_PLAY_STORE_PACKAGE = "com.android.vending";

    @Hide
    private static boolean zzfrr = false;

    @Hide
    private static boolean zzfrs = false;
    private static boolean zzfrt = false;
    private static boolean zzfru = false;
    static final AtomicBoolean zzfrv = new AtomicBoolean();
    private static final AtomicBoolean zzfrw = new AtomicBoolean();

    zzs() {
    }

    @Deprecated
    public static PendingIntent getErrorPendingIntent(int i, Context context, int i2) {
        return zzf.zzahf().getErrorResolutionPendingIntent(context, i, i2);
    }

    @Deprecated
    public static String getErrorString(int i) {
        return ConnectionResult.getStatusString(i);
    }

    public static Context getRemoteContext(Context context) {
        try {
            return context.createPackageContext("com.google.android.gms", 3);
        } catch (PackageManager.NameNotFoundException unused) {
            return null;
        }
    }

    public static Resources getRemoteResource(Context context) {
        try {
            return context.getPackageManager().getResourcesForApplication("com.google.android.gms");
        } catch (PackageManager.NameNotFoundException unused) {
            return null;
        }
    }

    @Deprecated
    public static int isGooglePlayServicesAvailable(Context context) {
        return zzc(context, -1);
    }

    @Deprecated
    public static boolean isUserRecoverableError(int i) {
        if (i == 9) {
            return true;
        }
        switch (i) {
            case 1:
            case 2:
            case 3:
                return true;
            default:
                return false;
        }
    }

    /* JADX WARN: Removed duplicated region for block: B:18:0x0039  */
    /* JADX WARN: Removed duplicated region for block: B:19:0x003e  */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
        To view partially-correct add '--show-bad-code' argument
    */
    private static int zza(android.content.Context r8, boolean r9, int r10, int r11) {
        /*
            r0 = -1
            r1 = 0
            r2 = 1
            if (r11 == r0) goto La
            if (r11 < 0) goto L8
            goto La
        L8:
            r3 = r1
            goto Lb
        La:
            r3 = r2
        Lb:
            com.google.android.gms.common.internal.zzbq.checkArgument(r3)
            android.content.pm.PackageManager r3 = r8.getPackageManager()
            r4 = 0
            r5 = 9
            if (r9 == 0) goto L28
            java.lang.String r4 = "com.android.vending"
            r6 = 8256(0x2040, float:1.1569E-41)
            android.content.pm.PackageInfo r4 = r3.getPackageInfo(r4, r6)     // Catch: android.content.pm.PackageManager.NameNotFoundException -> L20
            goto L28
        L20:
            java.lang.String r8 = "GooglePlayServicesUtil"
            java.lang.String r9 = "Google Play Store is missing."
        L24:
            android.util.Log.w(r8, r9)
            return r5
        L28:
            java.lang.String r6 = "com.google.android.gms"
            r7 = 64
            android.content.pm.PackageInfo r6 = r3.getPackageInfo(r6, r7)     // Catch: android.content.pm.PackageManager.NameNotFoundException -> La8
            com.google.android.gms.common.zzt.zzcj(r8)
            boolean r8 = com.google.android.gms.common.zzt.zza(r6, r2)
            if (r8 != 0) goto L3e
            java.lang.String r8 = "GooglePlayServicesUtil"
            java.lang.String r9 = "Google Play services signature invalid."
            goto L24
        L3e:
            if (r9 == 0) goto L59
            boolean r8 = com.google.android.gms.common.zzt.zza(r4, r2)
            if (r8 == 0) goto L54
            android.content.pm.Signature[] r8 = r4.signatures
            r8 = r8[r1]
            android.content.pm.Signature[] r9 = r6.signatures
            r9 = r9[r1]
            boolean r8 = r8.equals(r9)
            if (r8 != 0) goto L59
        L54:
            java.lang.String r8 = "GooglePlayServicesUtil"
            java.lang.String r9 = "Google Play Store signature invalid."
            goto L24
        L59:
            int r10 = r10 / 1000
            int r8 = r6.versionCode
            int r8 = r8 / 1000
            if (r8 >= r10) goto L8d
            if (r11 == r0) goto L67
            int r11 = r11 / 1000
            if (r8 >= r11) goto L8d
        L67:
            java.lang.String r8 = "GooglePlayServicesUtil"
            int r9 = com.google.android.gms.common.zzs.GOOGLE_PLAY_SERVICES_VERSION_CODE
            int r10 = r6.versionCode
            r11 = 77
            java.lang.StringBuilder r0 = new java.lang.StringBuilder
            r0.<init>(r11)
            java.lang.String r11 = "Google Play services out of date.  Requires "
            r0.append(r11)
            r0.append(r9)
            java.lang.String r9 = " but found "
            r0.append(r9)
            r0.append(r10)
            java.lang.String r9 = r0.toString()
            android.util.Log.w(r8, r9)
            r8 = 2
            return r8
        L8d:
            android.content.pm.ApplicationInfo r8 = r6.applicationInfo
            if (r8 != 0) goto La1
            java.lang.String r8 = "com.google.android.gms"
            android.content.pm.ApplicationInfo r8 = r3.getApplicationInfo(r8, r1)     // Catch: android.content.pm.PackageManager.NameNotFoundException -> L98
            goto La1
        L98:
            r8 = move-exception
            java.lang.String r9 = "GooglePlayServicesUtil"
            java.lang.String r10 = "Google Play services missing when getting application info."
            android.util.Log.wtf(r9, r10, r8)
            return r2
        La1:
            boolean r8 = r8.enabled
            if (r8 != 0) goto La7
            r8 = 3
            return r8
        La7:
            return r1
        La8:
            java.lang.String r8 = "GooglePlayServicesUtil"
            java.lang.String r9 = "Google Play services is missing."
            android.util.Log.w(r8, r9)
            return r2
        */
        throw new UnsupportedOperationException("Method not decompiled: com.google.android.gms.common.zzs.zza(android.content.Context, boolean, int, int):int");
    }

    @Hide
    @TargetApi(19)
    @Deprecated
    public static boolean zzb(Context context, int i, String str) {
        return zzz.zzb(context, i, str);
    }

    @Hide
    @Deprecated
    public static void zzbo(Context context) throws GooglePlayServicesRepairableException, GooglePlayServicesNotAvailableException {
        zzf.zzahf();
        int iZzc = zzf.zzc(context, -1);
        if (iZzc != 0) {
            zzf.zzahf();
            Intent intentZza = zzf.zza(context, iZzc, "e");
            StringBuilder sb = new StringBuilder(57);
            sb.append("GooglePlayServices not available due to error ");
            sb.append(iZzc);
            Log.e("GooglePlayServicesUtil", sb.toString());
            if (intentZza != null) {
                throw new GooglePlayServicesRepairableException(iZzc, "Google Play Services not available", intentZza);
            }
            throw new GooglePlayServicesNotAvailableException(iZzc);
        }
    }

    @Deprecated
    public static int zzc(Context context, int i) {
        try {
            context.getResources().getString(R.string.common_google_play_services_unknown_issue);
        } catch (Throwable unused) {
            Log.e("GooglePlayServicesUtil", "The Google Play services resources were not found. Check your project configuration to ensure that the resources are included.");
        }
        if (!"com.google.android.gms".equals(context.getPackageName()) && !zzfrw.get()) {
            int iZzcs = zzbf.zzcs(context);
            if (iZzcs == 0) {
                throw new IllegalStateException("A required meta-data tag in your app's AndroidManifest.xml does not exist.  You must have the following declaration within the <application> element:     <meta-data android:name=\"com.google.android.gms.version\" android:value=\"@integer/google_play_services_version\" />");
            }
            if (iZzcs != GOOGLE_PLAY_SERVICES_VERSION_CODE) {
                int i2 = GOOGLE_PLAY_SERVICES_VERSION_CODE;
                StringBuilder sb = new StringBuilder(320);
                sb.append("The meta-data tag in your app's AndroidManifest.xml does not have the right value.  Expected ");
                sb.append(i2);
                sb.append(" but found ");
                sb.append(iZzcs);
                sb.append(".  You must have the following declaration within the <application> element:     <meta-data android:name=\"com.google.android.gms.version\" android:value=\"@integer/google_play_services_version\" />");
                throw new IllegalStateException(sb.toString());
            }
        }
        return zza(context, (com.google.android.gms.common.util.zzj.zzcv(context) || com.google.android.gms.common.util.zzj.zzcx(context)) ? false : true, GOOGLE_PLAY_SERVICES_VERSION_CODE, i);
    }

    @Hide
    @Deprecated
    public static void zzcf(Context context) {
        if (zzfrv.getAndSet(true)) {
            return;
        }
        try {
            NotificationManager notificationManager = (NotificationManager) context.getSystemService(OneSignalDbContract.NotificationTable.TABLE_NAME);
            if (notificationManager != null) {
                notificationManager.cancel(10436);
            }
        } catch (SecurityException unused) {
        }
    }

    @Hide
    @Deprecated
    public static int zzcg(Context context) {
        try {
            return context.getPackageManager().getPackageInfo("com.google.android.gms", 0).versionCode;
        } catch (PackageManager.NameNotFoundException unused) {
            Log.w("GooglePlayServicesUtil", "Google Play services is missing.");
            return 0;
        }
    }

    @Hide
    public static boolean zzci(Context context) {
        try {
            if (!zzfru) {
                try {
                    PackageInfo packageInfo = zzbih.zzdd(context).getPackageInfo("com.google.android.gms", 64);
                    zzt.zzcj(context);
                    if (packageInfo == null || zzt.zza(packageInfo, false) || !zzt.zza(packageInfo, true)) {
                        zzfrt = false;
                    } else {
                        zzfrt = true;
                    }
                } catch (PackageManager.NameNotFoundException e) {
                    Log.w("GooglePlayServicesUtil", "Cannot find Google Play services package name.", e);
                }
            }
            return zzfrt || !"user".equals(Build.TYPE);
        } finally {
            zzfru = true;
        }
    }

    @Hide
    @Deprecated
    public static boolean zzd(Context context, int i) {
        if (i == 18) {
            return true;
        }
        if (i == 1) {
            return zzr(context, "com.google.android.gms");
        }
        return false;
    }

    @Hide
    @Deprecated
    public static boolean zze(Context context, int i) {
        return zzz.zze(context, i);
    }

    @TargetApi(21)
    static boolean zzr(Context context, String str) {
        ApplicationInfo applicationInfo;
        Bundle applicationRestrictions;
        boolean zEquals = str.equals("com.google.android.gms");
        if (com.google.android.gms.common.util.zzs.zzanx()) {
            try {
                Iterator<PackageInstaller.SessionInfo> it = context.getPackageManager().getPackageInstaller().getAllSessions().iterator();
                while (it.hasNext()) {
                    if (str.equals(it.next().getAppPackageName())) {
                        return true;
                    }
                }
            } catch (Exception unused) {
                return false;
            }
        }
        try {
            applicationInfo = context.getPackageManager().getApplicationInfo(str, 8192);
        } catch (PackageManager.NameNotFoundException unused2) {
        }
        if (zEquals) {
            return applicationInfo.enabled;
        }
        if (applicationInfo.enabled) {
            if (!(com.google.android.gms.common.util.zzs.zzanu() && (applicationRestrictions = ((UserManager) context.getSystemService("user")).getApplicationRestrictions(context.getPackageName())) != null && "true".equals(applicationRestrictions.getString("restricted_profile")))) {
                return true;
            }
        }
        return false;
    }
}
