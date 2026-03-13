package com.google.android.gms.common.api.internal;

import android.os.DeadObjectException;
import android.os.RemoteException;
import android.support.annotation.NonNull;
import com.google.android.gms.common.api.Api;
import com.google.android.gms.common.api.Api.zzb;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.Result;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.common.internal.Hide;

/* JADX INFO: loaded from: classes.dex */
public abstract class zzm<R extends Result, A extends Api.zzb> extends BasePendingResult<R> implements zzn<R> {
    private final Api<?> zzfop;
    private final Api.zzc<A> zzfus;

    @Deprecated
    protected zzm(@NonNull Api.zzc<A> zzcVar, @NonNull GoogleApiClient googleApiClient) {
        super((GoogleApiClient) com.google.android.gms.common.internal.zzbq.checkNotNull(googleApiClient, "GoogleApiClient must not be null"));
        this.zzfus = (Api.zzc) com.google.android.gms.common.internal.zzbq.checkNotNull(zzcVar);
        this.zzfop = null;
    }

    protected zzm(@NonNull Api<?> api, @NonNull GoogleApiClient googleApiClient) {
        super((GoogleApiClient) com.google.android.gms.common.internal.zzbq.checkNotNull(googleApiClient, "GoogleApiClient must not be null"));
        com.google.android.gms.common.internal.zzbq.checkNotNull(api, "Api must not be null");
        this.zzfus = (Api.zzc<A>) api.zzahm();
        this.zzfop = api;
    }

    private final void zzc(@NonNull RemoteException remoteException) {
        zzu(new Status(8, remoteException.getLocalizedMessage(), null));
    }

    @Override // com.google.android.gms.common.api.internal.zzn
    @Hide
    public /* bridge */ /* synthetic */ void setResult(Object obj) {
        super.setResult((Result) obj);
    }

    @Hide
    protected abstract void zza(@NonNull A a) throws RemoteException;

    @Hide
    public final Api.zzc<A> zzahm() {
        return this.zzfus;
    }

    @Hide
    public final Api<?> zzaht() {
        return this.zzfop;
    }

    @Hide
    public final void zzb(@NonNull A a) throws DeadObjectException {
        if (a instanceof com.google.android.gms.common.internal.zzbz) {
            a = com.google.android.gms.common.internal.zzbz.zzanb();
        }
        try {
            zza(a);
        } catch (DeadObjectException e) {
            zzc(e);
            throw e;
        } catch (RemoteException e2) {
            zzc(e2);
        }
    }

    @Override // com.google.android.gms.common.api.internal.zzn
    @Hide
    public final void zzu(@NonNull Status status) {
        com.google.android.gms.common.internal.zzbq.checkArgument(!status.isSuccess(), "Failed result must not be success");
        setResult((Result) zzb(status));
    }
}
