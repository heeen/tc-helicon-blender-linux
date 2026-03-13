package com.google.android.gms.tasks;

/* JADX INFO: loaded from: classes.dex */
final class zzd implements Runnable {
    private /* synthetic */ Task zzldy;
    private /* synthetic */ zzc zzlea;

    zzd(zzc zzcVar, Task task) {
        this.zzlea = zzcVar;
        this.zzldy = task;
    }

    @Override // java.lang.Runnable
    public final void run() {
        try {
            Task task = (Task) this.zzlea.zzldw.then(this.zzldy);
            if (task == null) {
                this.zzlea.onFailure(new NullPointerException("Continuation returned null"));
            } else {
                task.addOnSuccessListener(TaskExecutors.zzlem, this.zzlea);
                task.addOnFailureListener(TaskExecutors.zzlem, this.zzlea);
            }
        } catch (RuntimeExecutionException e) {
            if (e.getCause() instanceof Exception) {
                this.zzlea.zzldx.setException((Exception) e.getCause());
            } else {
                this.zzlea.zzldx.setException(e);
            }
        } catch (Exception e2) {
            this.zzlea.zzldx.setException(e2);
        }
    }
}
