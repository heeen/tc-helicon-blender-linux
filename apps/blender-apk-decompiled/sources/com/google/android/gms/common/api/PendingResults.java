package com.google.android.gms.common.api;

import android.os.Looper;
import com.google.android.gms.common.api.internal.BasePendingResult;
import com.google.android.gms.common.api.internal.zzco;
import com.google.android.gms.common.api.internal.zzdb;
import com.google.android.gms.common.internal.Hide;
import com.google.android.gms.common.internal.zzbq;

/* JADX INFO: loaded from: classes.dex */
public final class PendingResults {

    static final class zza<R extends Result> extends BasePendingResult<R> {
        private final R zzftl;

        public zza(R r) {
            super(Looper.getMainLooper());
            this.zzftl = r;
        }

        @Override // com.google.android.gms.common.api.internal.BasePendingResult
        protected final R zzb(Status status) {
            if (status.getStatusCode() == this.zzftl.getStatus().getStatusCode()) {
                return this.zzftl;
            }
            throw new UnsupportedOperationException("Creating failed results is not supported");
        }
    }

    static final class zzb<R extends Result> extends BasePendingResult<R> {
        private final R zzftm;

        public zzb(GoogleApiClient googleApiClient, R r) {
            super(googleApiClient);
            this.zzftm = r;
        }

        @Override // com.google.android.gms.common.api.internal.BasePendingResult
        protected final R zzb(Status status) {
            return this.zzftm;
        }
    }

    static final class zzc<R extends Result> extends BasePendingResult<R> {
        public zzc(GoogleApiClient googleApiClient) {
            super(googleApiClient);
        }

        @Override // com.google.android.gms.common.api.internal.BasePendingResult
        protected final R zzb(Status status) {
            throw new UnsupportedOperationException("Creating failed results is not supported");
        }
    }

    private PendingResults() {
    }

    public static PendingResult<Status> canceledPendingResult() {
        zzdb zzdbVar = new zzdb(Looper.getMainLooper());
        zzdbVar.cancel();
        return zzdbVar;
    }

    public static <R extends Result> PendingResult<R> canceledPendingResult(R r) {
        zzbq.checkNotNull(r, "Result must not be null");
        zzbq.checkArgument(r.getStatus().getStatusCode() == 16, "Status code must be CommonStatusCodes.CANCELED");
        zza zzaVar = new zza(r);
        zzaVar.cancel();
        return zzaVar;
    }

    public static <R extends Result> OptionalPendingResult<R> immediatePendingResult(R r) {
        zzbq.checkNotNull(r, "Result must not be null");
        zzc zzcVar = new zzc(null);
        zzcVar.setResult(r);
        return new zzco(zzcVar);
    }

    public static PendingResult<Status> immediatePendingResult(Status status) {
        zzbq.checkNotNull(status, "Result must not be null");
        zzdb zzdbVar = new zzdb(Looper.getMainLooper());
        zzdbVar.setResult(status);
        return zzdbVar;
    }

    @Hide
    public static <R extends Result> PendingResult<R> zza(R r, GoogleApiClient googleApiClient) {
        zzbq.checkNotNull(r, "Result must not be null");
        zzbq.checkArgument(!r.getStatus().isSuccess(), "Status code must not be SUCCESS");
        zzb zzbVar = new zzb(googleApiClient, r);
        zzbVar.setResult(r);
        return zzbVar;
    }

    @Hide
    public static PendingResult<Status> zza(Status status, GoogleApiClient googleApiClient) {
        zzbq.checkNotNull(status, "Result must not be null");
        zzdb zzdbVar = new zzdb(googleApiClient);
        zzdbVar.setResult(status);
        return zzdbVar;
    }

    @Hide
    public static <R extends Result> OptionalPendingResult<R> zzb(R r, GoogleApiClient googleApiClient) {
        zzbq.checkNotNull(r, "Result must not be null");
        zzc zzcVar = new zzc(googleApiClient);
        zzcVar.setResult(r);
        return new zzco(zzcVar);
    }
}
