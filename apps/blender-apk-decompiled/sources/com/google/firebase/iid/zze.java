package com.google.firebase.iid;

import android.content.Intent;
import android.util.Log;

/* JADX INFO: loaded from: classes.dex */
final class zze implements Runnable {
    private /* synthetic */ Intent val$intent;
    private /* synthetic */ zzd zzoki;

    zze(zzd zzdVar, Intent intent) {
        this.zzoki = zzdVar;
        this.val$intent = intent;
    }

    @Override // java.lang.Runnable
    public final void run() {
        String action = this.val$intent.getAction();
        StringBuilder sb = new StringBuilder(String.valueOf(action).length() + 61);
        sb.append("Service took too long to process intent: ");
        sb.append(action);
        sb.append(" App may get closed.");
        Log.w("EnhancedIntentService", sb.toString());
        this.zzoki.finish();
    }
}
