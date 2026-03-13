package com.google.android.gms.tasks;

/* JADX INFO: loaded from: classes.dex */
final class zzb implements Runnable {
    private /* synthetic */ Task zzldy;
    private /* synthetic */ zza zzldz;

    zzb(zza zzaVar, Task task) {
        this.zzldz = zzaVar;
        this.zzldy = task;
    }

    @Override // java.lang.Runnable
    public final void run() {
        try {
            this.zzldz.zzldx.setResult(this.zzldz.zzldw.then(this.zzldy));
        } catch (RuntimeExecutionException e) {
            if (e.getCause() instanceof Exception) {
                this.zzldz.zzldx.setException((Exception) e.getCause());
            } else {
                this.zzldz.zzldx.setException(e);
            }
        } catch (Exception e2) {
            this.zzldz.zzldx.setException(e2);
        }
    }
}
