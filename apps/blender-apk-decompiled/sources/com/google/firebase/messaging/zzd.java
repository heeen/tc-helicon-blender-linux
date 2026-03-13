package com.google.firebase.messaging;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Base64;
import android.util.Log;
import com.google.android.gms.common.internal.Hide;
import com.google.android.gms.measurement.AppMeasurement;
import com.onesignal.OneSignalDbContract;

/* JADX INFO: loaded from: classes.dex */
@Hide
final class zzd {
    private static void zzc(Context context, String str, Intent intent) {
        Bundle bundle = new Bundle();
        String stringExtra = intent.getStringExtra("google.c.a.c_id");
        if (stringExtra != null) {
            bundle.putString("_nmid", stringExtra);
        }
        String stringExtra2 = intent.getStringExtra("google.c.a.c_l");
        if (stringExtra2 != null) {
            bundle.putString("_nmn", stringExtra2);
        }
        String stringExtra3 = intent.getStringExtra("google.c.a.m_l");
        if (!TextUtils.isEmpty(stringExtra3)) {
            bundle.putString("label", stringExtra3);
        }
        String stringExtra4 = intent.getStringExtra("from");
        if (stringExtra4 == null || !stringExtra4.startsWith("/topics/")) {
            stringExtra4 = null;
        }
        if (stringExtra4 != null) {
            bundle.putString("_nt", stringExtra4);
        }
        try {
            bundle.putInt("_nmt", Integer.valueOf(intent.getStringExtra("google.c.a.ts")).intValue());
        } catch (NumberFormatException e) {
            Log.w("FirebaseMessaging", "Error while parsing timestamp in GCM event", e);
        }
        if (intent.hasExtra("google.c.a.udt")) {
            try {
                bundle.putInt("_ndt", Integer.valueOf(intent.getStringExtra("google.c.a.udt")).intValue());
            } catch (NumberFormatException e2) {
                Log.w("FirebaseMessaging", "Error while parsing use_device_time in GCM event", e2);
            }
        }
        if (Log.isLoggable("FirebaseMessaging", 3)) {
            String strValueOf = String.valueOf(bundle);
            StringBuilder sb = new StringBuilder(String.valueOf(str).length() + 22 + String.valueOf(strValueOf).length());
            sb.append("Sending event=");
            sb.append(str);
            sb.append(" params=");
            sb.append(strValueOf);
            Log.d("FirebaseMessaging", sb.toString());
        }
        AppMeasurement appMeasurementZzde = zzde(context);
        if (appMeasurementZzde != null) {
            appMeasurementZzde.logEventInternal("fcm", str, bundle);
        } else {
            Log.w("FirebaseMessaging", "Unable to log event: analytics library is missing");
        }
    }

    private static AppMeasurement zzde(Context context) {
        try {
            return AppMeasurement.getInstance(context);
        } catch (NoClassDefFoundError unused) {
            return null;
        }
    }

    public static void zzf(Context context, Intent intent) {
        String stringExtra = intent.getStringExtra("google.c.a.abt");
        if (stringExtra != null) {
            zzc.zza(context, "fcm", Base64.decode(stringExtra, 0), new zzb(), 1);
        }
        zzc(context, "_nr", intent);
    }

    public static void zzg(Context context, Intent intent) {
        if (intent != null) {
            if ("1".equals(intent.getStringExtra("google.c.a.tc"))) {
                AppMeasurement appMeasurementZzde = zzde(context);
                if (Log.isLoggable("FirebaseMessaging", 3)) {
                    Log.d("FirebaseMessaging", "Received event with track-conversion=true. Setting user property and reengagement event");
                }
                if (appMeasurementZzde != null) {
                    String stringExtra = intent.getStringExtra("google.c.a.c_id");
                    appMeasurementZzde.setUserPropertyInternal("fcm", "_ln", stringExtra);
                    Bundle bundle = new Bundle();
                    bundle.putString("source", "Firebase");
                    bundle.putString("medium", OneSignalDbContract.NotificationTable.TABLE_NAME);
                    bundle.putString("campaign", stringExtra);
                    appMeasurementZzde.logEventInternal("fcm", "_cmp", bundle);
                } else {
                    Log.w("FirebaseMessaging", "Unable to set user property for conversion tracking:  analytics library is missing");
                }
            } else if (Log.isLoggable("FirebaseMessaging", 3)) {
                Log.d("FirebaseMessaging", "Received event with track-conversion=false. Do not set user property");
            }
        }
        zzc(context, "_no", intent);
    }

    public static void zzh(Context context, Intent intent) {
        zzc(context, "_nd", intent);
    }

    public static void zzi(Context context, Intent intent) {
        zzc(context, "_nf", intent);
    }
}
