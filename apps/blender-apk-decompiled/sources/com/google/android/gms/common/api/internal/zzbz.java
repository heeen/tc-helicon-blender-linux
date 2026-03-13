package com.google.android.gms.common.api.internal;

import android.content.Context;
import android.content.res.Resources;
import android.text.TextUtils;
import com.google.android.gms.R;
import com.google.android.gms.common.api.Status;

/* JADX INFO: loaded from: classes.dex */
@Deprecated
public final class zzbz {
    private static final Object sLock = new Object();
    private static zzbz zzgah;
    private final String mAppId;
    private final Status zzgai;
    private final boolean zzgaj;
    private final boolean zzgak;

    private zzbz(Context context) {
        Resources resources = context.getResources();
        int identifier = resources.getIdentifier("google_app_measurement_enable", "integer", resources.getResourcePackageName(R.string.common_google_play_services_unknown_issue));
        if (identifier != 0) {
            z = resources.getInteger(identifier) != 0;
            this.zzgak = !z;
        } else {
            this.zzgak = false;
        }
        this.zzgaj = z;
        String strZzcr = com.google.android.gms.common.internal.zzbf.zzcr(context);
        strZzcr = strZzcr == null ? new com.google.android.gms.common.internal.zzca(context).getString("google_app_id") : strZzcr;
        if (TextUtils.isEmpty(strZzcr)) {
            this.zzgai = new Status(10, "Missing google app id value from from string resources with name google_app_id.");
            this.mAppId = null;
        } else {
            this.mAppId = strZzcr;
            this.zzgai = Status.zzftq;
        }
    }

    public static String zzakq() {
        return zzgi("getGoogleAppId").mAppId;
    }

    public static boolean zzakr() {
        return zzgi("isMeasurementExplicitlyDisabled").zzgak;
    }

    public static Status zzcl(Context context) {
        Status status;
        com.google.android.gms.common.internal.zzbq.checkNotNull(context, "Context must not be null.");
        synchronized (sLock) {
            if (zzgah == null) {
                zzgah = new zzbz(context);
            }
            status = zzgah.zzgai;
        }
        return status;
    }

    private static zzbz zzgi(String str) {
        zzbz zzbzVar;
        synchronized (sLock) {
            if (zzgah == null) {
                StringBuilder sb = new StringBuilder(String.valueOf(str).length() + 34);
                sb.append("Initialize must be called before ");
                sb.append(str);
                sb.append(".");
                throw new IllegalStateException(sb.toString());
            }
            zzbzVar = zzgah;
        }
        return zzbzVar;
    }
}
