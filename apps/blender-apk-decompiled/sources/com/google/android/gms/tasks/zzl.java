package com.google.android.gms.tasks;

/* JADX INFO: loaded from: classes.dex */
final class zzl implements Runnable {
    private /* synthetic */ Task zzldy;
    private /* synthetic */ zzk zzlei;

    zzl(zzk zzkVar, Task task) {
        this.zzlei = zzkVar;
        this.zzldy = task;
    }

    @Override // java.lang.Runnable
    public final void run() {
        try {
            Task taskThen = this.zzlei.zzleh.then(this.zzldy.getResult());
            if (taskThen == null) {
                this.zzlei.onFailure(new NullPointerException("Continuation returned null"));
            } else {
                taskThen.addOnSuccessListener(TaskExecutors.zzlem, this.zzlei);
                taskThen.addOnFailureListener(TaskExecutors.zzlem, this.zzlei);
            }
        } catch (RuntimeExecutionException e) {
            if (e.getCause() instanceof Exception) {
                this.zzlei.onFailure((Exception) e.getCause());
            } else {
                this.zzlei.onFailure(e);
            }
        } catch (Exception e2) {
            this.zzlei.onFailure(e2);
        }
    }
}
