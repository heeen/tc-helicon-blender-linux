package com.google.android.gms.tasks;

/* JADX INFO: loaded from: classes.dex */
final class zzf implements Runnable {
    private /* synthetic */ Task zzldy;
    private /* synthetic */ zze zzlec;

    zzf(zze zzeVar, Task task) {
        this.zzlec = zzeVar;
        this.zzldy = task;
    }

    @Override // java.lang.Runnable
    public final void run() {
        synchronized (this.zzlec.mLock) {
            if (this.zzlec.zzleb != null) {
                this.zzlec.zzleb.onComplete(this.zzldy);
            }
        }
    }
}
