package com.google.android.gms.common.api.internal;

import android.support.annotation.WorkerThread;

/* JADX INFO: loaded from: classes.dex */
abstract class zzay implements Runnable {
    private /* synthetic */ zzao zzfxt;

    private zzay(zzao zzaoVar) {
        this.zzfxt = zzaoVar;
    }

    /* synthetic */ zzay(zzao zzaoVar, zzap zzapVar) {
        this(zzaoVar);
    }

    @Override // java.lang.Runnable
    @WorkerThread
    public void run() {
        this.zzfxt.zzfwa.lock();
        try {
            try {
                if (!Thread.interrupted()) {
                    zzajj();
                }
            } catch (RuntimeException e) {
                this.zzfxt.zzfxd.zzb(e);
            }
        } finally {
            this.zzfxt.zzfwa.unlock();
        }
    }

    @WorkerThread
    protected abstract void zzajj();
}
