package com.google.android.gms.tasks;

import android.support.annotation.NonNull;

/* JADX INFO: loaded from: classes.dex */
public class TaskCompletionSource<TResult> {
    private final zzp<TResult> zzlel = new zzp<>();

    @NonNull
    public Task<TResult> getTask() {
        return this.zzlel;
    }

    public void setException(@NonNull Exception exc) {
        this.zzlel.setException(exc);
    }

    public void setResult(TResult tresult) {
        this.zzlel.setResult(tresult);
    }

    public boolean trySetException(@NonNull Exception exc) {
        return this.zzlel.trySetException(exc);
    }

    public boolean trySetResult(TResult tresult) {
        return this.zzlel.trySetResult(tresult);
    }
}
