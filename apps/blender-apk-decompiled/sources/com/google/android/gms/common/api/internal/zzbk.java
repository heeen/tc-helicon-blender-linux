package com.google.android.gms.common.api.internal;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;

/* JADX INFO: loaded from: classes.dex */
final class zzbk extends Handler {
    private /* synthetic */ zzbi zzfze;

    /* JADX WARN: 'super' call moved to the top of the method (can break code semantics) */
    zzbk(zzbi zzbiVar, Looper looper) {
        super(looper);
        this.zzfze = zzbiVar;
    }

    @Override // android.os.Handler
    public final void handleMessage(Message message) {
        switch (message.what) {
            case 1:
                ((zzbj) message.obj).zzc(this.zzfze);
                return;
            case 2:
                throw ((RuntimeException) message.obj);
            default:
                int i = message.what;
                StringBuilder sb = new StringBuilder(31);
                sb.append("Unknown message id: ");
                sb.append(i);
                Log.w("GACStateManager", sb.toString());
                return;
        }
    }
}
