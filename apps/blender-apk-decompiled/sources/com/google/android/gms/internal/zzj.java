package com.google.android.gms.internal;

import android.os.Handler;
import java.util.concurrent.Executor;

/* JADX INFO: loaded from: classes.dex */
final class zzj implements Executor {
    private /* synthetic */ Handler val$handler;

    zzj(zzi zziVar, Handler handler) {
        this.val$handler = handler;
    }

    @Override // java.util.concurrent.Executor
    public final void execute(Runnable runnable) {
        this.val$handler.post(runnable);
    }
}
