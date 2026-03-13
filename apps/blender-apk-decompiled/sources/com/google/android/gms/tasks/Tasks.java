package com.google.android.gms.tasks;

import android.support.annotation.NonNull;
import com.google.android.gms.common.internal.zzbq;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/* JADX INFO: loaded from: classes.dex */
public final class Tasks {

    static final class zza implements zzb {
        private final CountDownLatch zzapc;

        private zza() {
            this.zzapc = new CountDownLatch(1);
        }

        /* synthetic */ zza(zzq zzqVar) {
            this();
        }

        public final void await() throws InterruptedException {
            this.zzapc.await();
        }

        public final boolean await(long j, TimeUnit timeUnit) throws InterruptedException {
            return this.zzapc.await(j, timeUnit);
        }

        @Override // com.google.android.gms.tasks.OnFailureListener
        public final void onFailure(@NonNull Exception exc) {
            this.zzapc.countDown();
        }

        @Override // com.google.android.gms.tasks.OnSuccessListener
        public final void onSuccess(Object obj) {
            this.zzapc.countDown();
        }
    }

    interface zzb extends OnFailureListener, OnSuccessListener<Object> {
    }

    static final class zzc implements zzb {
        private final Object mLock = new Object();
        private final zzp<Void> zzlel;
        private Exception zzleq;
        private final int zzlet;
        private int zzleu;
        private int zzlev;

        public zzc(int i, zzp<Void> zzpVar) {
            this.zzlet = i;
            this.zzlel = zzpVar;
        }

        private final void zzblg() {
            if (this.zzleu + this.zzlev == this.zzlet) {
                if (this.zzleq == null) {
                    this.zzlel.setResult(null);
                    return;
                }
                zzp<Void> zzpVar = this.zzlel;
                int i = this.zzlev;
                int i2 = this.zzlet;
                StringBuilder sb = new StringBuilder(54);
                sb.append(i);
                sb.append(" out of ");
                sb.append(i2);
                sb.append(" underlying tasks failed");
                zzpVar.setException(new ExecutionException(sb.toString(), this.zzleq));
            }
        }

        @Override // com.google.android.gms.tasks.OnFailureListener
        public final void onFailure(@NonNull Exception exc) {
            synchronized (this.mLock) {
                this.zzlev++;
                this.zzleq = exc;
                zzblg();
            }
        }

        @Override // com.google.android.gms.tasks.OnSuccessListener
        public final void onSuccess(Object obj) {
            synchronized (this.mLock) {
                this.zzleu++;
                zzblg();
            }
        }
    }

    private Tasks() {
    }

    public static <TResult> TResult await(@NonNull Task<TResult> task) throws ExecutionException, InterruptedException {
        zzbq.zzgw("Must not be called on the main application thread");
        zzbq.checkNotNull(task, "Task must not be null");
        if (task.isComplete()) {
            return (TResult) zzc(task);
        }
        zza zzaVar = new zza(null);
        zza(task, zzaVar);
        zzaVar.await();
        return (TResult) zzc(task);
    }

    public static <TResult> TResult await(@NonNull Task<TResult> task, long j, @NonNull TimeUnit timeUnit) throws ExecutionException, InterruptedException, TimeoutException {
        zzbq.zzgw("Must not be called on the main application thread");
        zzbq.checkNotNull(task, "Task must not be null");
        zzbq.checkNotNull(timeUnit, "TimeUnit must not be null");
        if (task.isComplete()) {
            return (TResult) zzc(task);
        }
        zza zzaVar = new zza(null);
        zza(task, zzaVar);
        if (zzaVar.await(j, timeUnit)) {
            return (TResult) zzc(task);
        }
        throw new TimeoutException("Timed out waiting for Task");
    }

    public static <TResult> Task<TResult> call(@NonNull Callable<TResult> callable) {
        return call(TaskExecutors.MAIN_THREAD, callable);
    }

    public static <TResult> Task<TResult> call(@NonNull Executor executor, @NonNull Callable<TResult> callable) {
        zzbq.checkNotNull(executor, "Executor must not be null");
        zzbq.checkNotNull(callable, "Callback must not be null");
        zzp zzpVar = new zzp();
        executor.execute(new zzq(zzpVar, callable));
        return zzpVar;
    }

    public static <TResult> Task<TResult> forException(@NonNull Exception exc) {
        zzp zzpVar = new zzp();
        zzpVar.setException(exc);
        return zzpVar;
    }

    public static <TResult> Task<TResult> forResult(TResult tresult) {
        zzp zzpVar = new zzp();
        zzpVar.setResult(tresult);
        return zzpVar;
    }

    public static Task<Void> whenAll(Collection<? extends Task<?>> collection) {
        if (collection.isEmpty()) {
            return forResult(null);
        }
        Iterator<? extends Task<?>> it = collection.iterator();
        while (it.hasNext()) {
            if (it.next() == null) {
                throw new NullPointerException("null tasks are not accepted");
            }
        }
        zzp zzpVar = new zzp();
        zzc zzcVar = new zzc(collection.size(), zzpVar);
        Iterator<? extends Task<?>> it2 = collection.iterator();
        while (it2.hasNext()) {
            zza(it2.next(), zzcVar);
        }
        return zzpVar;
    }

    public static Task<Void> whenAll(Task<?>... taskArr) {
        return taskArr.length == 0 ? forResult(null) : whenAll(Arrays.asList(taskArr));
    }

    public static Task<List<Task<?>>> whenAllComplete(Collection<? extends Task<?>> collection) {
        return whenAll(collection).continueWith(new zzs(collection));
    }

    public static Task<List<Task<?>>> whenAllComplete(Task<?>... taskArr) {
        return whenAllComplete(Arrays.asList(taskArr));
    }

    public static <TResult> Task<List<TResult>> whenAllSuccess(Collection<? extends Task<?>> collection) {
        return (Task<List<TResult>>) whenAll(collection).continueWith(new zzr(collection));
    }

    public static <TResult> Task<List<TResult>> whenAllSuccess(Task<?>... taskArr) {
        return whenAllSuccess(Arrays.asList(taskArr));
    }

    private static void zza(Task<?> task, zzb zzbVar) {
        task.addOnSuccessListener(TaskExecutors.zzlem, zzbVar);
        task.addOnFailureListener(TaskExecutors.zzlem, zzbVar);
    }

    private static <TResult> TResult zzc(Task<TResult> task) throws ExecutionException {
        if (task.isSuccessful()) {
            return task.getResult();
        }
        throw new ExecutionException(task.getException());
    }
}
