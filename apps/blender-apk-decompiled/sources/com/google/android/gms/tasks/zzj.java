package com.google.android.gms.tasks;

/* JADX INFO: loaded from: classes.dex */
final class zzj implements Runnable {
    private /* synthetic */ Task zzldy;
    private /* synthetic */ zzi zzleg;

    zzj(zzi zziVar, Task task) {
        this.zzleg = zziVar;
        this.zzldy = task;
    }

    @Override // java.lang.Runnable
    public final void run() {
        synchronized (this.zzleg.mLock) {
            if (this.zzleg.zzlef != null) {
                this.zzleg.zzlef.onSuccess(this.zzldy.getResult());
            }
        }
    }
}
