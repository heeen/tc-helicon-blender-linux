package com.google.android.gms.tasks;

import android.support.annotation.NonNull;
import java.util.concurrent.Executor;

/* JADX INFO: loaded from: classes.dex */
final class zza<TResult, TContinuationResult> implements zzm<TResult> {
    private final Executor zzkou;
    private final Continuation<TResult, TContinuationResult> zzldw;
    private final zzp<TContinuationResult> zzldx;

    public zza(@NonNull Executor executor, @NonNull Continuation<TResult, TContinuationResult> continuation, @NonNull zzp<TContinuationResult> zzpVar) {
        this.zzkou = executor;
        this.zzldw = continuation;
        this.zzldx = zzpVar;
    }

    @Override // com.google.android.gms.tasks.zzm
    public final void cancel() {
        throw new UnsupportedOperationException();
    }

    @Override // com.google.android.gms.tasks.zzm
    public final void onComplete(@NonNull Task<TResult> task) {
        this.zzkou.execute(new zzb(this, task));
    }
}
