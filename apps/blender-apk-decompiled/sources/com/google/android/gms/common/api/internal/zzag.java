package com.google.android.gms.common.api.internal;

import android.support.annotation.NonNull;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.TaskCompletionSource;

/* JADX INFO: Add missing generic type declarations: [TResult] */
/* JADX INFO: loaded from: classes.dex */
final class zzag<TResult> implements OnCompleteListener<TResult> {
    private /* synthetic */ TaskCompletionSource zzeuo;
    private /* synthetic */ zzae zzfwz;

    zzag(zzae zzaeVar, TaskCompletionSource taskCompletionSource) {
        this.zzfwz = zzaeVar;
        this.zzeuo = taskCompletionSource;
    }

    @Override // com.google.android.gms.tasks.OnCompleteListener
    public final void onComplete(@NonNull Task<TResult> task) {
        this.zzfwz.zzfwx.remove(this.zzeuo);
    }
}
