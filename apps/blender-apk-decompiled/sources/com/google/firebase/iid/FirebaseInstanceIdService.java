package com.google.firebase.iid;

import android.content.Intent;
import android.support.annotation.WorkerThread;
import android.util.Log;
import com.google.android.gms.common.internal.Hide;

/* JADX INFO: loaded from: classes.dex */
public class FirebaseInstanceIdService extends zzb {
    @Override // com.google.firebase.iid.zzb
    @Hide
    public final void handleIntent(Intent intent) {
        if ("com.google.firebase.iid.TOKEN_REFRESH".equals(intent.getAction())) {
            onTokenRefresh();
            return;
        }
        String stringExtra = intent.getStringExtra("CMD");
        if (stringExtra != null) {
            if (Log.isLoggable("FirebaseInstanceId", 3)) {
                String strValueOf = String.valueOf(intent.getExtras());
                StringBuilder sb = new StringBuilder(String.valueOf(stringExtra).length() + 21 + String.valueOf(strValueOf).length());
                sb.append("Received command: ");
                sb.append(stringExtra);
                sb.append(" - ");
                sb.append(strValueOf);
                Log.d("FirebaseInstanceId", sb.toString());
            }
            if ("RST".equals(stringExtra) || "RST_FULL".equals(stringExtra)) {
                FirebaseInstanceId.getInstance().zzclg();
            } else if ("SYNC".equals(stringExtra)) {
                FirebaseInstanceId.getInstance().zzclh();
            }
        }
    }

    @WorkerThread
    public void onTokenRefresh() {
    }

    @Override // com.google.firebase.iid.zzb
    @Hide
    protected final Intent zzp(Intent intent) {
        return zzz.zzclq().zzolm.poll();
    }
}
