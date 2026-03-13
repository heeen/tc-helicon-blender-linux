package com.google.android.gms.common.api;

import com.google.android.gms.common.api.internal.BasePendingResult;
import java.util.ArrayList;
import java.util.List;

/* JADX INFO: loaded from: classes.dex */
public final class Batch extends BasePendingResult<BatchResult> {
    private final Object mLock;
    private int zzfsf;
    private boolean zzfsg;
    private boolean zzfsh;
    private final PendingResult<?>[] zzfsi;

    public static final class Builder {
        private GoogleApiClient zzfap;
        private List<PendingResult<?>> zzfsk = new ArrayList();

        public Builder(GoogleApiClient googleApiClient) {
            this.zzfap = googleApiClient;
        }

        public final <R extends Result> BatchResultToken<R> add(PendingResult<R> pendingResult) {
            BatchResultToken<R> batchResultToken = new BatchResultToken<>(this.zzfsk.size());
            this.zzfsk.add(pendingResult);
            return batchResultToken;
        }

        public final Batch build() {
            return new Batch(this.zzfsk, this.zzfap, null);
        }
    }

    private Batch(List<PendingResult<?>> list, GoogleApiClient googleApiClient) {
        super(googleApiClient);
        this.mLock = new Object();
        this.zzfsf = list.size();
        this.zzfsi = new PendingResult[this.zzfsf];
        if (list.isEmpty()) {
            setResult(new BatchResult(Status.zzftq, this.zzfsi));
            return;
        }
        for (int i = 0; i < list.size(); i++) {
            PendingResult<?> pendingResult = list.get(i);
            this.zzfsi[i] = pendingResult;
            pendingResult.zza(new zza(this));
        }
    }

    /* synthetic */ Batch(List list, GoogleApiClient googleApiClient, zza zzaVar) {
        this(list, googleApiClient);
    }

    static /* synthetic */ boolean zza(Batch batch, boolean z) {
        batch.zzfsh = true;
        return true;
    }

    static /* synthetic */ int zzb(Batch batch) {
        int i = batch.zzfsf;
        batch.zzfsf = i - 1;
        return i;
    }

    static /* synthetic */ boolean zzb(Batch batch, boolean z) {
        batch.zzfsg = true;
        return true;
    }

    @Override // com.google.android.gms.common.api.internal.BasePendingResult, com.google.android.gms.common.api.PendingResult
    public final void cancel() {
        super.cancel();
        for (PendingResult<?> pendingResult : this.zzfsi) {
            pendingResult.cancel();
        }
    }

    @Override // com.google.android.gms.common.api.internal.BasePendingResult
    /* JADX INFO: renamed from: createFailedResult, reason: merged with bridge method [inline-methods] */
    public final BatchResult zzb(Status status) {
        return new BatchResult(status, this.zzfsi);
    }
}
