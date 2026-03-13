package com.google.android.gms.tasks;

import android.app.Activity;
import android.support.annotation.MainThread;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import com.google.android.gms.common.api.internal.LifecycleCallback;
import com.google.android.gms.common.api.internal.zzcf;
import com.google.android.gms.common.internal.zzbq;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.Executor;

/* JADX INFO: loaded from: classes.dex */
final class zzp<TResult> extends Task<TResult> {
    private final Object mLock = new Object();
    private final zzn<TResult> zzlen = new zzn<>();
    private boolean zzleo;
    private TResult zzlep;
    private Exception zzleq;

    static class zza extends LifecycleCallback {
        private final List<WeakReference<zzm<?>>> zzffp;

        private zza(zzcf zzcfVar) {
            super(zzcfVar);
            this.zzffp = new ArrayList();
            this.zzgam.zza("TaskOnStopCallback", this);
        }

        public static zza zzs(Activity activity) {
            zzcf zzcfVarZzo = zzo(activity);
            zza zzaVar = (zza) zzcfVarZzo.zza("TaskOnStopCallback", zza.class);
            return zzaVar == null ? new zza(zzcfVarZzo) : zzaVar;
        }

        @Override // com.google.android.gms.common.api.internal.LifecycleCallback
        @MainThread
        public final void onStop() {
            synchronized (this.zzffp) {
                Iterator<WeakReference<zzm<?>>> it = this.zzffp.iterator();
                while (it.hasNext()) {
                    zzm<?> zzmVar = it.next().get();
                    if (zzmVar != null) {
                        zzmVar.cancel();
                    }
                }
                this.zzffp.clear();
            }
        }

        public final <T> void zzb(zzm<T> zzmVar) {
            synchronized (this.zzffp) {
                this.zzffp.add(new WeakReference<>(zzmVar));
            }
        }
    }

    zzp() {
    }

    private final void zzbld() {
        zzbq.zza(this.zzleo, "Task is not yet complete");
    }

    private final void zzble() {
        zzbq.zza(!this.zzleo, "Task is already complete");
    }

    private final void zzblf() {
        synchronized (this.mLock) {
            if (this.zzleo) {
                this.zzlen.zzb(this);
            }
        }
    }

    @Override // com.google.android.gms.tasks.Task
    @NonNull
    public final Task<TResult> addOnCompleteListener(@NonNull Activity activity, @NonNull OnCompleteListener<TResult> onCompleteListener) {
        zze zzeVar = new zze(TaskExecutors.MAIN_THREAD, onCompleteListener);
        this.zzlen.zza(zzeVar);
        zza.zzs(activity).zzb(zzeVar);
        zzblf();
        return this;
    }

    @Override // com.google.android.gms.tasks.Task
    @NonNull
    public final Task<TResult> addOnCompleteListener(@NonNull OnCompleteListener<TResult> onCompleteListener) {
        return addOnCompleteListener(TaskExecutors.MAIN_THREAD, onCompleteListener);
    }

    @Override // com.google.android.gms.tasks.Task
    @NonNull
    public final Task<TResult> addOnCompleteListener(@NonNull Executor executor, @NonNull OnCompleteListener<TResult> onCompleteListener) {
        this.zzlen.zza(new zze(executor, onCompleteListener));
        zzblf();
        return this;
    }

    @Override // com.google.android.gms.tasks.Task
    @NonNull
    public final Task<TResult> addOnFailureListener(@NonNull Activity activity, @NonNull OnFailureListener onFailureListener) {
        zzg zzgVar = new zzg(TaskExecutors.MAIN_THREAD, onFailureListener);
        this.zzlen.zza(zzgVar);
        zza.zzs(activity).zzb(zzgVar);
        zzblf();
        return this;
    }

    @Override // com.google.android.gms.tasks.Task
    @NonNull
    public final Task<TResult> addOnFailureListener(@NonNull OnFailureListener onFailureListener) {
        return addOnFailureListener(TaskExecutors.MAIN_THREAD, onFailureListener);
    }

    @Override // com.google.android.gms.tasks.Task
    @NonNull
    public final Task<TResult> addOnFailureListener(@NonNull Executor executor, @NonNull OnFailureListener onFailureListener) {
        this.zzlen.zza(new zzg(executor, onFailureListener));
        zzblf();
        return this;
    }

    @Override // com.google.android.gms.tasks.Task
    @NonNull
    public final Task<TResult> addOnSuccessListener(@NonNull Activity activity, @NonNull OnSuccessListener<? super TResult> onSuccessListener) {
        zzi zziVar = new zzi(TaskExecutors.MAIN_THREAD, onSuccessListener);
        this.zzlen.zza(zziVar);
        zza.zzs(activity).zzb(zziVar);
        zzblf();
        return this;
    }

    @Override // com.google.android.gms.tasks.Task
    @NonNull
    public final Task<TResult> addOnSuccessListener(@NonNull OnSuccessListener<? super TResult> onSuccessListener) {
        return addOnSuccessListener(TaskExecutors.MAIN_THREAD, onSuccessListener);
    }

    @Override // com.google.android.gms.tasks.Task
    @NonNull
    public final Task<TResult> addOnSuccessListener(@NonNull Executor executor, @NonNull OnSuccessListener<? super TResult> onSuccessListener) {
        this.zzlen.zza(new zzi(executor, onSuccessListener));
        zzblf();
        return this;
    }

    @Override // com.google.android.gms.tasks.Task
    @NonNull
    public final <TContinuationResult> Task<TContinuationResult> continueWith(@NonNull Continuation<TResult, TContinuationResult> continuation) {
        return continueWith(TaskExecutors.MAIN_THREAD, continuation);
    }

    @Override // com.google.android.gms.tasks.Task
    @NonNull
    public final <TContinuationResult> Task<TContinuationResult> continueWith(@NonNull Executor executor, @NonNull Continuation<TResult, TContinuationResult> continuation) {
        zzp zzpVar = new zzp();
        this.zzlen.zza(new com.google.android.gms.tasks.zza(executor, continuation, zzpVar));
        zzblf();
        return zzpVar;
    }

    @Override // com.google.android.gms.tasks.Task
    @NonNull
    public final <TContinuationResult> Task<TContinuationResult> continueWithTask(@NonNull Continuation<TResult, Task<TContinuationResult>> continuation) {
        return continueWithTask(TaskExecutors.MAIN_THREAD, continuation);
    }

    @Override // com.google.android.gms.tasks.Task
    @NonNull
    public final <TContinuationResult> Task<TContinuationResult> continueWithTask(@NonNull Executor executor, @NonNull Continuation<TResult, Task<TContinuationResult>> continuation) {
        zzp zzpVar = new zzp();
        this.zzlen.zza(new zzc(executor, continuation, zzpVar));
        zzblf();
        return zzpVar;
    }

    @Override // com.google.android.gms.tasks.Task
    @Nullable
    public final Exception getException() {
        Exception exc;
        synchronized (this.mLock) {
            exc = this.zzleq;
        }
        return exc;
    }

    @Override // com.google.android.gms.tasks.Task
    public final TResult getResult() {
        TResult tresult;
        synchronized (this.mLock) {
            zzbld();
            if (this.zzleq != null) {
                throw new RuntimeExecutionException(this.zzleq);
            }
            tresult = this.zzlep;
        }
        return tresult;
    }

    @Override // com.google.android.gms.tasks.Task
    public final <X extends Throwable> TResult getResult(@NonNull Class<X> cls) throws Throwable {
        TResult tresult;
        synchronized (this.mLock) {
            zzbld();
            if (cls.isInstance(this.zzleq)) {
                throw cls.cast(this.zzleq);
            }
            if (this.zzleq != null) {
                throw new RuntimeExecutionException(this.zzleq);
            }
            tresult = this.zzlep;
        }
        return tresult;
    }

    @Override // com.google.android.gms.tasks.Task
    public final boolean isComplete() {
        boolean z;
        synchronized (this.mLock) {
            z = this.zzleo;
        }
        return z;
    }

    @Override // com.google.android.gms.tasks.Task
    public final boolean isSuccessful() {
        boolean z;
        synchronized (this.mLock) {
            z = this.zzleo && this.zzleq == null;
        }
        return z;
    }

    @Override // com.google.android.gms.tasks.Task
    @NonNull
    public final <TContinuationResult> Task<TContinuationResult> onSuccessTask(@NonNull SuccessContinuation<TResult, TContinuationResult> successContinuation) {
        return onSuccessTask(TaskExecutors.MAIN_THREAD, successContinuation);
    }

    @Override // com.google.android.gms.tasks.Task
    @NonNull
    public final <TContinuationResult> Task<TContinuationResult> onSuccessTask(Executor executor, SuccessContinuation<TResult, TContinuationResult> successContinuation) {
        zzp zzpVar = new zzp();
        this.zzlen.zza(new zzk(executor, successContinuation, zzpVar));
        zzblf();
        return zzpVar;
    }

    public final void setException(@NonNull Exception exc) {
        zzbq.checkNotNull(exc, "Exception must not be null");
        synchronized (this.mLock) {
            zzble();
            this.zzleo = true;
            this.zzleq = exc;
        }
        this.zzlen.zzb(this);
    }

    public final void setResult(TResult tresult) {
        synchronized (this.mLock) {
            zzble();
            this.zzleo = true;
            this.zzlep = tresult;
        }
        this.zzlen.zzb(this);
    }

    public final boolean trySetException(@NonNull Exception exc) {
        zzbq.checkNotNull(exc, "Exception must not be null");
        synchronized (this.mLock) {
            if (this.zzleo) {
                return false;
            }
            this.zzleo = true;
            this.zzleq = exc;
            this.zzlen.zzb(this);
            return true;
        }
    }

    public final boolean trySetResult(TResult tresult) {
        synchronized (this.mLock) {
            if (this.zzleo) {
                return false;
            }
            this.zzleo = true;
            this.zzlep = tresult;
            this.zzlen.zzb(this);
            return true;
        }
    }
}
