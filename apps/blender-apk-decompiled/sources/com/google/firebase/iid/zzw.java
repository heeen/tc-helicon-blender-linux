package com.google.firebase.iid;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.util.Base64;
import android.util.Log;
import com.google.android.gms.common.internal.Hide;
import com.google.firebase.FirebaseApp;
import java.security.KeyPair;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;

/* JADX INFO: loaded from: classes.dex */
@Hide
public final class zzw {
    private final Context zzaiq;
    private String zzcs;
    private String zzold;
    private int zzole;
    private int zzolf = 0;

    public zzw(Context context) {
        this.zzaiq = context;
    }

    public static String zzb(KeyPair keyPair) {
        try {
            byte[] bArrDigest = MessageDigest.getInstance("SHA1").digest(keyPair.getPublic().getEncoded());
            bArrDigest[0] = (byte) ((bArrDigest[0] & 15) + 112);
            return Base64.encodeToString(bArrDigest, 0, 8, 11);
        } catch (NoSuchAlgorithmException unused) {
            Log.w("FirebaseInstanceId", "Unexpected error, device missing required algorithms");
            return null;
        }
    }

    private final synchronized void zzclp() {
        PackageInfo packageInfoZzog = zzog(this.zzaiq.getPackageName());
        if (packageInfoZzog != null) {
            this.zzold = Integer.toString(packageInfoZzog.versionCode);
            this.zzcs = packageInfoZzog.versionName;
        }
    }

    public static String zzf(FirebaseApp firebaseApp) {
        String gcmSenderId = firebaseApp.getOptions().getGcmSenderId();
        if (gcmSenderId != null) {
            return gcmSenderId;
        }
        String applicationId = firebaseApp.getOptions().getApplicationId();
        if (!applicationId.startsWith("1:")) {
            return applicationId;
        }
        String[] strArrSplit = applicationId.split(":");
        if (strArrSplit.length < 2) {
            return null;
        }
        String str = strArrSplit[1];
        if (str.isEmpty()) {
            return null;
        }
        return str;
    }

    private final PackageInfo zzog(String str) {
        try {
            return this.zzaiq.getPackageManager().getPackageInfo(str, 0);
        } catch (PackageManager.NameNotFoundException e) {
            String strValueOf = String.valueOf(e);
            StringBuilder sb = new StringBuilder(String.valueOf(strValueOf).length() + 23);
            sb.append("Failed to find package ");
            sb.append(strValueOf);
            Log.w("FirebaseInstanceId", sb.toString());
            return null;
        }
    }

    public final synchronized int zzcll() {
        if (this.zzolf != 0) {
            return this.zzolf;
        }
        PackageManager packageManager = this.zzaiq.getPackageManager();
        if (packageManager.checkPermission("com.google.android.c2dm.permission.SEND", "com.google.android.gms") == -1) {
            Log.e("FirebaseInstanceId", "Google Play services missing or without correct permission.");
            return 0;
        }
        if (!com.google.android.gms.common.util.zzs.isAtLeastO()) {
            Intent intent = new Intent("com.google.android.c2dm.intent.REGISTER");
            intent.setPackage("com.google.android.gms");
            List<ResolveInfo> listQueryIntentServices = packageManager.queryIntentServices(intent, 0);
            if (listQueryIntentServices != null && listQueryIntentServices.size() > 0) {
                this.zzolf = 1;
                return this.zzolf;
            }
        }
        Intent intent2 = new Intent("com.google.iid.TOKEN_REQUEST");
        intent2.setPackage("com.google.android.gms");
        List<ResolveInfo> listQueryBroadcastReceivers = packageManager.queryBroadcastReceivers(intent2, 0);
        if (listQueryBroadcastReceivers != null && listQueryBroadcastReceivers.size() > 0) {
            this.zzolf = 2;
            return this.zzolf;
        }
        Log.w("FirebaseInstanceId", "Failed to resolve IID implementation package, falling back");
        if (com.google.android.gms.common.util.zzs.isAtLeastO()) {
            this.zzolf = 2;
        } else {
            this.zzolf = 1;
        }
        return this.zzolf;
    }

    public final synchronized String zzclm() {
        if (this.zzold == null) {
            zzclp();
        }
        return this.zzold;
    }

    public final synchronized String zzcln() {
        if (this.zzcs == null) {
            zzclp();
        }
        return this.zzcs;
    }

    public final synchronized int zzclo() {
        PackageInfo packageInfoZzog;
        if (this.zzole == 0 && (packageInfoZzog = zzog("com.google.android.gms")) != null) {
            this.zzole = packageInfoZzog.versionCode;
        }
        return this.zzole;
    }
}
