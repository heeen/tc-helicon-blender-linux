package com.google.android.gms.tasks;

import android.support.annotation.NonNull;
import java.util.concurrent.Executor;

/* JADX INFO: loaded from: classes.dex */
final class zzk<TResult, TContinuationResult> implements OnFailureListener, OnSuccessListener<TContinuationResult>, zzm<TResult> {
    private final Executor zzkou;
    private final zzp<TContinuationResult> zzldx;
    private final SuccessContinuation<TResult, TContinuationResult> zzleh;

    public zzk(@NonNull Executor executor, @NonNull SuccessContinuation<TResult, TContinuationResult> successContinuation, @NonNull zzp<TContinuationResult> zzpVar) {
        this.zzkou = executor;
        this.zzleh = successContinuation;
        this.zzldx = zzpVar;
    }

    @Override // com.google.android.gms.tasks.zzm
    public final void cancel() {
        throw new UnsupportedOperationException();
    }

    @Override // com.google.android.gms.tasks.zzm
    public final void onComplete(@NonNull Task<TResult> task) {
        this.zzkou.execute(new zzl(this, task));
    }

    @Override // com.google.android.gms.tasks.OnFailureListener
    public final void onFailure(@NonNull Exception exc) {
        this.zzldx.setException(exc);
    }

    @Override // com.google.android.gms.tasks.OnSuccessListener
    public final void onSuccess(TContinuationResult tcontinuationresult) {
        this.zzldx.setResult(tcontinuationresult);
    }
}
