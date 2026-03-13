package com.google.firebase.iid;

import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.PowerManager;
import android.util.Log;
import java.io.IOException;

/* JADX INFO: loaded from: classes.dex */
final class zzac implements Runnable {
    private final zzw zzokq;
    private final long zzolp;
    private final PowerManager.WakeLock zzolq = ((PowerManager) getContext().getSystemService("power")).newWakeLock(1, "fiid-sync");
    private final FirebaseInstanceId zzolr;

    zzac(FirebaseInstanceId firebaseInstanceId, zzw zzwVar, long j) {
        this.zzolr = firebaseInstanceId;
        this.zzokq = zzwVar;
        this.zzolp = j;
        this.zzolq.setReferenceCounted(false);
    }

    private final boolean zzclt() {
        zzab zzabVarZzclc = this.zzolr.zzclc();
        if (zzabVarZzclc != null && !zzabVarZzclc.zzru(this.zzokq.zzclm())) {
            return true;
        }
        try {
            String strZzcld = this.zzolr.zzcld();
            if (strZzcld == null) {
                Log.e("FirebaseInstanceId", "Token retrieval failed: null");
                return false;
            }
            if (Log.isLoggable("FirebaseInstanceId", 3)) {
                Log.d("FirebaseInstanceId", "Token successfully retrieved");
            }
            if (zzabVarZzclc == null || (zzabVarZzclc != null && !strZzcld.equals(zzabVarZzclc.zzlnm))) {
                Context context = getContext();
                Intent intent = new Intent("com.google.firebase.iid.TOKEN_REFRESH");
                Intent intent2 = new Intent("com.google.firebase.INSTANCE_ID_EVENT");
                intent2.setClass(context, FirebaseInstanceIdReceiver.class);
                intent2.putExtra("wrapped_intent", intent);
                context.sendBroadcast(intent2);
            }
            return true;
        } catch (IOException | SecurityException e) {
            String strValueOf = String.valueOf(e.getMessage());
            Log.e("FirebaseInstanceId", strValueOf.length() != 0 ? "Token retrieval failed: ".concat(strValueOf) : new String("Token retrieval failed: "));
            return false;
        }
    }

    private final boolean zzclu() {
        while (true) {
            synchronized (this.zzolr) {
                String strZzcls = FirebaseInstanceId.zzcle().zzcls();
                if (strZzcls == null) {
                    Log.d("FirebaseInstanceId", "topic sync succeeded");
                    return true;
                }
                if (!zzrv(strZzcls)) {
                    return false;
                }
                FirebaseInstanceId.zzcle().zzro(strZzcls);
            }
        }
    }

    private final boolean zzrv(String str) {
        String str2;
        String str3;
        String[] strArrSplit = str.split("!");
        if (strArrSplit.length == 2) {
            String str4 = strArrSplit[0];
            String str5 = strArrSplit[1];
            byte b = -1;
            try {
                int iHashCode = str4.hashCode();
                if (iHashCode != 83) {
                    if (iHashCode == 85 && str4.equals("U")) {
                        b = 1;
                    }
                } else if (str4.equals("S")) {
                    b = 0;
                }
                switch (b) {
                    case 0:
                        this.zzolr.zzrm(str5);
                        if (FirebaseInstanceId.zzclf()) {
                            str2 = "FirebaseInstanceId";
                            str3 = "subscribe operation succeeded";
                            Log.d(str2, str3);
                        }
                        break;
                    case 1:
                        this.zzolr.zzrn(str5);
                        if (FirebaseInstanceId.zzclf()) {
                            str2 = "FirebaseInstanceId";
                            str3 = "unsubscribe operation succeeded";
                            Log.d(str2, str3);
                        }
                        break;
                }
            } catch (IOException e) {
                String strValueOf = String.valueOf(e.getMessage());
                Log.e("FirebaseInstanceId", strValueOf.length() != 0 ? "Topic sync failed: ".concat(strValueOf) : new String("Topic sync failed: "));
                return false;
            }
        }
        return true;
    }

    final Context getContext() {
        return this.zzolr.getApp().getApplicationContext();
    }

    @Override // java.lang.Runnable
    public final void run() {
        FirebaseInstanceId firebaseInstanceId;
        this.zzolq.acquire();
        try {
            boolean z = true;
            this.zzolr.zzcy(true);
            if (this.zzokq.zzcll() == 0) {
                z = false;
            }
            if (z) {
                if (!zzclv()) {
                    new zzad(this).zzclw();
                } else if (zzclt() && zzclu()) {
                    firebaseInstanceId = this.zzolr;
                } else {
                    this.zzolr.zzcd(this.zzolp);
                }
            }
            firebaseInstanceId = this.zzolr;
            firebaseInstanceId.zzcy(false);
        } finally {
            this.zzolq.release();
        }
    }

    final boolean zzclv() {
        ConnectivityManager connectivityManager = (ConnectivityManager) getContext().getSystemService("connectivity");
        NetworkInfo activeNetworkInfo = connectivityManager != null ? connectivityManager.getActiveNetworkInfo() : null;
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }
}
