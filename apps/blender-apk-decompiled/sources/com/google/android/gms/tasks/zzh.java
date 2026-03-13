package com.google.android.gms.tasks;

/* JADX INFO: loaded from: classes.dex */
final class zzh implements Runnable {
    private /* synthetic */ Task zzldy;
    private /* synthetic */ zzg zzlee;

    zzh(zzg zzgVar, Task task) {
        this.zzlee = zzgVar;
        this.zzldy = task;
    }

    @Override // java.lang.Runnable
    public final void run() {
        synchronized (this.zzlee.mLock) {
            if (this.zzlee.zzled != null) {
                this.zzlee.zzled.onFailure(this.zzldy.getException());
            }
        }
    }
}
