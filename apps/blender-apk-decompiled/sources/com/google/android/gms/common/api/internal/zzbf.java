package com.google.android.gms.common.api.internal;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;

/* JADX INFO: loaded from: classes.dex */
final class zzbf extends Handler {
    private /* synthetic */ zzba zzfyr;

    /* JADX WARN: 'super' call moved to the top of the method (can break code semantics) */
    zzbf(zzba zzbaVar, Looper looper) {
        super(looper);
        this.zzfyr = zzbaVar;
    }

    @Override // android.os.Handler
    public final void handleMessage(Message message) {
        switch (message.what) {
            case 1:
                this.zzfyr.zzajr();
                break;
            case 2:
                this.zzfyr.resume();
                break;
            default:
                int i = message.what;
                StringBuilder sb = new StringBuilder(31);
                sb.append("Unknown message id: ");
                sb.append(i);
                Log.w("GoogleApiClientImpl", sb.toString());
                break;
        }
    }
}
