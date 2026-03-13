package com.google.firebase.iid;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;

/* JADX INFO: loaded from: classes.dex */
final class zzy extends Handler {
    private /* synthetic */ zzx zzoli;

    /* JADX WARN: 'super' call moved to the top of the method (can break code semantics) */
    zzy(zzx zzxVar, Looper looper) {
        super(looper);
        this.zzoli = zzxVar;
    }

    @Override // android.os.Handler
    public final void handleMessage(Message message) {
        this.zzoli.zze(message);
    }
}
