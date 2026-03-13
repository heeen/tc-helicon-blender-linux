package com.google.android.gms.common.api.internal;

/* JADX INFO: loaded from: classes.dex */
final class zzw implements Runnable {
    private /* synthetic */ zzv zzfwc;

    zzw(zzv zzvVar) {
        this.zzfwc = zzvVar;
    }

    @Override // java.lang.Runnable
    public final void run() {
        this.zzfwc.zzfwa.lock();
        try {
            this.zzfwc.zzait();
        } finally {
            this.zzfwc.zzfwa.unlock();
        }
    }
}
