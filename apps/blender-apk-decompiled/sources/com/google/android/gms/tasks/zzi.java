package com.google.android.gms.tasks;

import android.support.annotation.NonNull;
import java.util.concurrent.Executor;

/* JADX INFO: loaded from: classes.dex */
final class zzi<TResult> implements zzm<TResult> {
    private final Object mLock = new Object();
    private final Executor zzkou;
    private OnSuccessListener<? super TResult> zzlef;

    public zzi(@NonNull Executor executor, @NonNull OnSuccessListener<? super TResult> onSuccessListener) {
        this.zzkou = executor;
        this.zzlef = onSuccessListener;
    }

    @Override // com.google.android.gms.tasks.zzm
    public final void cancel() {
        synchronized (this.mLock) {
            this.zzlef = null;
        }
    }

    @Override // com.google.android.gms.tasks.zzm
    public final void onComplete(@NonNull Task<TResult> task) {
        if (task.isSuccessful()) {
            synchronized (this.mLock) {
                if (this.zzlef == null) {
                    return;
                }
                this.zzkou.execute(new zzj(this, task));
            }
        }
    }
}
