package com.google.android.gms.common.api.internal;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.RemoteException;
import android.support.annotation.NonNull;
import android.util.Log;
import android.util.Pair;
import com.google.android.gms.common.annotation.KeepName;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.Releasable;
import com.google.android.gms.common.api.Result;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.ResultTransform;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.common.api.TransformedResult;
import com.google.android.gms.common.internal.Hide;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

/* JADX INFO: loaded from: classes.dex */
@Hide
@KeepName
public abstract class BasePendingResult<R extends Result> extends PendingResult<R> {
    static final ThreadLocal<Boolean> zzfvb = new zzs();

    @KeepName
    private zzb mResultGuardian;
    private Status mStatus;
    private boolean zzam;
    private final CountDownLatch zzapc;
    private R zzftm;
    private final Object zzfvc;

    @Hide
    private zza<R> zzfvd;
    private WeakReference<GoogleApiClient> zzfve;
    private final ArrayList<PendingResult.zza> zzfvf;
    private ResultCallback<? super R> zzfvg;
    private final AtomicReference<zzdn> zzfvh;
    private volatile boolean zzfvi;
    private boolean zzfvj;
    private com.google.android.gms.common.internal.zzaq zzfvk;
    private volatile zzdh<R> zzfvl;
    private boolean zzfvm;

    @Hide
    public static class zza<R extends Result> extends Handler {
        public zza() {
            this(Looper.getMainLooper());
        }

        public zza(Looper looper) {
            super(looper);
        }

        /* JADX WARN: Multi-variable type inference failed */
        @Override // android.os.Handler
        public final void handleMessage(Message message) {
            switch (message.what) {
                case 1:
                    Pair pair = (Pair) message.obj;
                    ResultCallback resultCallback = (ResultCallback) pair.first;
                    Result result = (Result) pair.second;
                    try {
                        resultCallback.onResult(result);
                        return;
                    } catch (RuntimeException e) {
                        BasePendingResult.zzd(result);
                        throw e;
                    }
                case 2:
                    ((BasePendingResult) message.obj).zzv(Status.zzftt);
                    return;
                default:
                    int i = message.what;
                    StringBuilder sb = new StringBuilder(45);
                    sb.append("Don't know how to handle message: ");
                    sb.append(i);
                    Log.wtf("BasePendingResult", sb.toString(), new Exception());
                    return;
            }
        }

        public final void zza(ResultCallback<? super R> resultCallback, R r) {
            sendMessage(obtainMessage(1, new Pair(resultCallback, r)));
        }
    }

    final class zzb {
        private zzb() {
        }

        /* synthetic */ zzb(BasePendingResult basePendingResult, zzs zzsVar) {
            this();
        }

        protected final void finalize() throws Throwable {
            BasePendingResult.zzd(BasePendingResult.this.zzftm);
            super.finalize();
        }
    }

    @Deprecated
    BasePendingResult() {
        this.zzfvc = new Object();
        this.zzapc = new CountDownLatch(1);
        this.zzfvf = new ArrayList<>();
        this.zzfvh = new AtomicReference<>();
        this.zzfvm = false;
        this.zzfvd = new zza<>(Looper.getMainLooper());
        this.zzfve = new WeakReference<>(null);
    }

    @Deprecated
    protected BasePendingResult(Looper looper) {
        this.zzfvc = new Object();
        this.zzapc = new CountDownLatch(1);
        this.zzfvf = new ArrayList<>();
        this.zzfvh = new AtomicReference<>();
        this.zzfvm = false;
        this.zzfvd = new zza<>(looper);
        this.zzfve = new WeakReference<>(null);
    }

    protected BasePendingResult(GoogleApiClient googleApiClient) {
        this.zzfvc = new Object();
        this.zzapc = new CountDownLatch(1);
        this.zzfvf = new ArrayList<>();
        this.zzfvh = new AtomicReference<>();
        this.zzfvm = false;
        this.zzfvd = new zza<>(googleApiClient != null ? googleApiClient.getLooper() : Looper.getMainLooper());
        this.zzfve = new WeakReference<>(googleApiClient);
    }

    private final R get() {
        R r;
        synchronized (this.zzfvc) {
            com.google.android.gms.common.internal.zzbq.zza(!this.zzfvi, "Result has already been consumed.");
            com.google.android.gms.common.internal.zzbq.zza(isReady(), "Result is not ready.");
            r = this.zzftm;
            this.zzftm = null;
            this.zzfvg = null;
            this.zzfvi = true;
        }
        zzdn andSet = this.zzfvh.getAndSet(null);
        if (andSet != null) {
            andSet.zzc(this);
        }
        return r;
    }

    private final void zzc(R r) {
        this.zzftm = r;
        zzs zzsVar = null;
        this.zzfvk = null;
        this.zzapc.countDown();
        this.mStatus = this.zzftm.getStatus();
        if (this.zzam) {
            this.zzfvg = null;
        } else if (this.zzfvg != null) {
            this.zzfvd.removeMessages(2);
            this.zzfvd.zza(this.zzfvg, get());
        } else if (this.zzftm instanceof Releasable) {
            this.mResultGuardian = new zzb(this, zzsVar);
        }
        ArrayList<PendingResult.zza> arrayList = this.zzfvf;
        int size = arrayList.size();
        int i = 0;
        while (i < size) {
            PendingResult.zza zzaVar = arrayList.get(i);
            i++;
            zzaVar.zzr(this.mStatus);
        }
        this.zzfvf.clear();
    }

    @Hide
    public static void zzd(Result result) {
        if (result instanceof Releasable) {
            try {
                ((Releasable) result).release();
            } catch (RuntimeException e) {
                String strValueOf = String.valueOf(result);
                StringBuilder sb = new StringBuilder(String.valueOf(strValueOf).length() + 18);
                sb.append("Unable to release ");
                sb.append(strValueOf);
                Log.w("BasePendingResult", sb.toString(), e);
            }
        }
    }

    @Override // com.google.android.gms.common.api.PendingResult
    public final R await() {
        com.google.android.gms.common.internal.zzbq.zzgw("await must not be called on the UI thread");
        com.google.android.gms.common.internal.zzbq.zza(!this.zzfvi, "Result has already been consumed");
        com.google.android.gms.common.internal.zzbq.zza(this.zzfvl == null, "Cannot await if then() has been called.");
        try {
            this.zzapc.await();
        } catch (InterruptedException unused) {
            zzv(Status.zzftr);
        }
        com.google.android.gms.common.internal.zzbq.zza(isReady(), "Result is not ready.");
        return (R) get();
    }

    @Override // com.google.android.gms.common.api.PendingResult
    public final R await(long j, TimeUnit timeUnit) {
        if (j > 0) {
            com.google.android.gms.common.internal.zzbq.zzgw("await must not be called on the UI thread when time is greater than zero.");
        }
        com.google.android.gms.common.internal.zzbq.zza(!this.zzfvi, "Result has already been consumed.");
        com.google.android.gms.common.internal.zzbq.zza(this.zzfvl == null, "Cannot await if then() has been called.");
        try {
            if (!this.zzapc.await(j, timeUnit)) {
                zzv(Status.zzftt);
            }
        } catch (InterruptedException unused) {
            zzv(Status.zzftr);
        }
        com.google.android.gms.common.internal.zzbq.zza(isReady(), "Result is not ready.");
        return (R) get();
    }

    @Override // com.google.android.gms.common.api.PendingResult
    public void cancel() {
        synchronized (this.zzfvc) {
            if (!this.zzam && !this.zzfvi) {
                if (this.zzfvk != null) {
                    try {
                        this.zzfvk.cancel();
                    } catch (RemoteException unused) {
                    }
                }
                zzd(this.zzftm);
                this.zzam = true;
                zzc(zzb(Status.zzftu));
            }
        }
    }

    @Override // com.google.android.gms.common.api.PendingResult
    public boolean isCanceled() {
        boolean z;
        synchronized (this.zzfvc) {
            z = this.zzam;
        }
        return z;
    }

    @Hide
    public final boolean isReady() {
        return this.zzapc.getCount() == 0;
    }

    @Hide
    public final void setResult(R r) {
        synchronized (this.zzfvc) {
            if (this.zzfvj || this.zzam) {
                zzd(r);
                return;
            }
            isReady();
            com.google.android.gms.common.internal.zzbq.zza(!isReady(), "Results have already been set");
            com.google.android.gms.common.internal.zzbq.zza(!this.zzfvi, "Result has already been consumed");
            zzc(r);
        }
    }

    @Override // com.google.android.gms.common.api.PendingResult
    public final void setResultCallback(ResultCallback<? super R> resultCallback) {
        synchronized (this.zzfvc) {
            try {
                if (resultCallback == null) {
                    this.zzfvg = null;
                    return;
                }
                boolean z = true;
                com.google.android.gms.common.internal.zzbq.zza(!this.zzfvi, "Result has already been consumed.");
                if (this.zzfvl != null) {
                    z = false;
                }
                com.google.android.gms.common.internal.zzbq.zza(z, "Cannot set callbacks if then() has been called.");
                if (isCanceled()) {
                    return;
                }
                if (isReady()) {
                    this.zzfvd.zza(resultCallback, get());
                } else {
                    this.zzfvg = resultCallback;
                }
            } catch (Throwable th) {
                throw th;
            }
        }
    }

    @Override // com.google.android.gms.common.api.PendingResult
    public final void setResultCallback(ResultCallback<? super R> resultCallback, long j, TimeUnit timeUnit) {
        synchronized (this.zzfvc) {
            try {
                if (resultCallback == null) {
                    this.zzfvg = null;
                    return;
                }
                boolean z = true;
                com.google.android.gms.common.internal.zzbq.zza(!this.zzfvi, "Result has already been consumed.");
                if (this.zzfvl != null) {
                    z = false;
                }
                com.google.android.gms.common.internal.zzbq.zza(z, "Cannot set callbacks if then() has been called.");
                if (isCanceled()) {
                    return;
                }
                if (isReady()) {
                    this.zzfvd.zza(resultCallback, get());
                } else {
                    this.zzfvg = resultCallback;
                    zza<R> zzaVar = this.zzfvd;
                    zzaVar.sendMessageDelayed(zzaVar.obtainMessage(2, this), timeUnit.toMillis(j));
                }
            } catch (Throwable th) {
                throw th;
            }
        }
    }

    @Override // com.google.android.gms.common.api.PendingResult
    @Hide
    public <S extends Result> TransformedResult<S> then(ResultTransform<? super R, ? extends S> resultTransform) {
        TransformedResult<S> transformedResultThen;
        com.google.android.gms.common.internal.zzbq.zza(!this.zzfvi, "Result has already been consumed.");
        synchronized (this.zzfvc) {
            com.google.android.gms.common.internal.zzbq.zza(this.zzfvl == null, "Cannot call then() twice.");
            com.google.android.gms.common.internal.zzbq.zza(this.zzfvg == null, "Cannot call then() if callbacks are set.");
            com.google.android.gms.common.internal.zzbq.zza(!this.zzam, "Cannot call then() if result was canceled.");
            this.zzfvm = true;
            this.zzfvl = new zzdh<>(this.zzfve);
            transformedResultThen = this.zzfvl.then(resultTransform);
            if (isReady()) {
                this.zzfvd.zza(this.zzfvl, get());
            } else {
                this.zzfvg = this.zzfvl;
            }
        }
        return transformedResultThen;
    }

    @Override // com.google.android.gms.common.api.PendingResult
    @Hide
    public final void zza(PendingResult.zza zzaVar) {
        com.google.android.gms.common.internal.zzbq.checkArgument(zzaVar != null, "Callback cannot be null.");
        synchronized (this.zzfvc) {
            if (isReady()) {
                zzaVar.zzr(this.mStatus);
            } else {
                this.zzfvf.add(zzaVar);
            }
        }
    }

    @Hide
    public final void zza(zzdn zzdnVar) {
        this.zzfvh.set(zzdnVar);
    }

    @Hide
    protected final void zza(com.google.android.gms.common.internal.zzaq zzaqVar) {
        synchronized (this.zzfvc) {
            this.zzfvk = zzaqVar;
        }
    }

    @Override // com.google.android.gms.common.api.PendingResult
    @Hide
    public final Integer zzaid() {
        return null;
    }

    @Hide
    public final boolean zzaip() {
        boolean zIsCanceled;
        synchronized (this.zzfvc) {
            if (this.zzfve.get() == null || !this.zzfvm) {
                cancel();
            }
            zIsCanceled = isCanceled();
        }
        return zIsCanceled;
    }

    @Hide
    public final void zzaiq() {
        this.zzfvm = this.zzfvm || zzfvb.get().booleanValue();
    }

    @Hide
    @NonNull
    protected abstract R zzb(Status status);

    @Hide
    public final void zzv(Status status) {
        synchronized (this.zzfvc) {
            if (!isReady()) {
                setResult(zzb(status));
                this.zzfvj = true;
            }
        }
    }
}
