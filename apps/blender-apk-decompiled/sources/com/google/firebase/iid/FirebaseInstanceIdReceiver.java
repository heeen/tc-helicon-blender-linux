package com.google.firebase.iid;

import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Parcelable;
import android.support.v4.content.WakefulBroadcastReceiver;
import android.util.Base64;
import android.util.Log;

/* JADX INFO: loaded from: classes.dex */
public final class FirebaseInstanceIdReceiver extends WakefulBroadcastReceiver {
    private static boolean zzijq = false;
    private static zzh zzoku;
    private static zzh zzokv;

    private final void zza(Context context, Intent intent, String str) {
        String str2 = null;
        intent.setComponent(null);
        intent.setPackage(context.getPackageName());
        if (Build.VERSION.SDK_INT <= 18) {
            intent.removeCategory(context.getPackageName());
        }
        String stringExtra = intent.getStringExtra("gcm.rawData64");
        boolean z = false;
        if (stringExtra != null) {
            intent.putExtra("rawData", Base64.decode(stringExtra, 0));
            intent.removeExtra("gcm.rawData64");
        }
        if ("google.com/iid".equals(intent.getStringExtra("from")) || "com.google.firebase.INSTANCE_ID_EVENT".equals(str)) {
            str2 = "com.google.firebase.INSTANCE_ID_EVENT";
        } else if ("com.google.android.c2dm.intent.RECEIVE".equals(str) || "com.google.firebase.MESSAGING_EVENT".equals(str)) {
            str2 = "com.google.firebase.MESSAGING_EVENT";
        } else {
            Log.d("FirebaseInstanceId", "Unexpected intent");
        }
        int iZzb = -1;
        if (str2 != null) {
            if (com.google.android.gms.common.util.zzs.isAtLeastO() && context.getApplicationInfo().targetSdkVersion >= 26) {
                z = true;
            }
            if (z) {
                if (isOrderedBroadcast()) {
                    setResultCode(-1);
                }
                zzai(context, str2).zza(intent, goAsync());
            } else {
                iZzb = zzz.zzclq().zzb(context, str2, intent);
            }
        }
        if (isOrderedBroadcast()) {
            setResultCode(iZzb);
        }
    }

    private static synchronized zzh zzai(Context context, String str) {
        if ("com.google.firebase.MESSAGING_EVENT".equals(str)) {
            if (zzokv == null) {
                zzokv = new zzh(context, str);
            }
            return zzokv;
        }
        if (zzoku == null) {
            zzoku = new zzh(context, str);
        }
        return zzoku;
    }

    @Override // android.content.BroadcastReceiver
    public final void onReceive(Context context, Intent intent) {
        if (intent == null) {
            return;
        }
        Parcelable parcelableExtra = intent.getParcelableExtra("wrapped_intent");
        if (parcelableExtra instanceof Intent) {
            zza(context, (Intent) parcelableExtra, intent.getAction());
        } else {
            zza(context, intent, intent.getAction());
        }
    }
}
