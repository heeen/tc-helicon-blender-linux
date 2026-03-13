package com.google.android.gms.common.api.internal;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;

/* JADX INFO: loaded from: classes.dex */
final class zzcj extends Handler {
    private /* synthetic */ zzci zzgav;

    /* JADX WARN: 'super' call moved to the top of the method (can break code semantics) */
    public zzcj(zzci zzciVar, Looper looper) {
        super(looper);
        this.zzgav = zzciVar;
    }

    @Override // android.os.Handler
    public final void handleMessage(Message message) {
        com.google.android.gms.common.internal.zzbq.checkArgument(message.what == 1);
        this.zzgav.zzb((zzcl) message.obj);
    }
}
