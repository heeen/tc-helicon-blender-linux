package com.google.android.gms.common;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import com.google.android.gms.common.internal.Hide;
import com.google.android.gms.common.internal.zzak;
import com.google.android.gms.internal.zzbih;

/* JADX INFO: loaded from: classes.dex */
@Hide
public class zzf {
    public static final String GOOGLE_PLAY_SERVICES_PACKAGE = "com.google.android.gms";
    public static final int GOOGLE_PLAY_SERVICES_VERSION_CODE = zzs.GOOGLE_PLAY_SERVICES_VERSION_CODE;
    private static final zzf zzfqz = new zzf();

    zzf() {
    }

    @Hide
    @Nullable
    public static Intent zza(Context context, int i, @Nullable String str) {
        switch (i) {
            case 1:
            case 2:
                return (context == null || !com.google.android.gms.common.util.zzj.zzcv(context)) ? zzak.zzt("com.google.android.gms", zzq(context, str)) : zzak.zzamw();
            case 3:
                return zzak.zzgt("com.google.android.gms");
            default:
                return null;
        }
    }

    public static zzf zzahf() {
        return zzfqz;
    }

    public static int zzc(Context context, int i) {
        int iZzc = zzs.zzc(context, i);
        if (zzs.zzd(context, iZzc)) {
            return 18;
        }
        return iZzc;
    }

    @Hide
    public static void zzce(Context context) throws GooglePlayServicesRepairableException, GooglePlayServicesNotAvailableException {
        zzs.zzbo(context);
    }

    @Hide
    public static void zzcf(Context context) {
        zzs.zzcf(context);
    }

    @Hide
    public static int zzcg(Context context) {
        return zzs.zzcg(context);
    }

    @Hide
    public static boolean zzd(Context context, int i) {
        return zzs.zzd(context, i);
    }

    private static String zzq(@Nullable Context context, @Nullable String str) {
        StringBuilder sb = new StringBuilder();
        sb.append("gcore_");
        sb.append(GOOGLE_PLAY_SERVICES_VERSION_CODE);
        sb.append("-");
        if (!TextUtils.isEmpty(str)) {
            sb.append(str);
        }
        sb.append("-");
        if (context != null) {
            sb.append(context.getPackageName());
        }
        sb.append("-");
        if (context != null) {
            try {
                sb.append(zzbih.zzdd(context).getPackageInfo(context.getPackageName(), 0).versionCode);
            } catch (PackageManager.NameNotFoundException unused) {
            }
        }
        return sb.toString();
    }

    @Nullable
    public PendingIntent getErrorResolutionPendingIntent(Context context, int i, int i2) {
        return zza(context, i, i2, null);
    }

    public String getErrorString(int i) {
        return zzs.getErrorString(i);
    }

    public int isGooglePlayServicesAvailable(Context context) {
        return zzc(context, -1);
    }

    public boolean isUserResolvableError(int i) {
        return zzs.isUserRecoverableError(i);
    }

    @Hide
    @Nullable
    public final PendingIntent zza(Context context, int i, int i2, @Nullable String str) {
        Intent intentZza = zza(context, i, str);
        if (intentZza == null) {
            return null;
        }
        return PendingIntent.getActivity(context, i2, intentZza, 268435456);
    }

    @Hide
    @Nullable
    @Deprecated
    public final Intent zzbo(int i) {
        return zza(null, i, null);
    }
}
