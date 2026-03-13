package com.google.android.gms.internal;

import android.os.Process;

/* JADX INFO: loaded from: classes.dex */
final class zzbie implements Runnable {
    private final int mPriority;
    private final Runnable zzy;

    public zzbie(Runnable runnable, int i) {
        this.zzy = runnable;
        this.mPriority = i;
    }

    @Override // java.lang.Runnable
    public final void run() {
        Process.setThreadPriority(this.mPriority);
        this.zzy.run();
    }
}
