package com.google.android.gms.internal;

import android.os.Handler;
import android.os.Looper;
import android.support.annotation.NonNull;
import java.util.concurrent.Executor;

/* JADX INFO: loaded from: classes.dex */
public final class zzbic implements Executor {
    private final Handler mHandler;

    public zzbic(Looper looper) {
        this.mHandler = new Handler(looper);
    }

    @Override // java.util.concurrent.Executor
    public final void execute(@NonNull Runnable runnable) {
        this.mHandler.post(runnable);
    }
}
