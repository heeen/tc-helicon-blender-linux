package com.google.android.gms.internal;

import android.os.RemoteException;
import android.util.Log;
import com.google.android.gms.clearcut.ClearcutLogger;
import com.google.android.gms.common.api.Api;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.Result;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.common.internal.Hide;

/* JADX INFO: loaded from: classes.dex */
final class zzbfl extends com.google.android.gms.common.api.internal.zzm<Status, zzbfn> {
    private final com.google.android.gms.clearcut.zze zzfqh;

    zzbfl(com.google.android.gms.clearcut.zze zzeVar, GoogleApiClient googleApiClient) {
        super(ClearcutLogger.API, googleApiClient);
        this.zzfqh = zzeVar;
    }

    @Override // com.google.android.gms.common.api.internal.zzm, com.google.android.gms.common.api.internal.zzn
    @Hide
    public final /* bridge */ /* synthetic */ void setResult(Object obj) {
        super.setResult((Status) obj);
    }

    @Override // com.google.android.gms.common.api.internal.zzm
    protected final /* synthetic */ void zza(Api.zzb zzbVar) throws RemoteException {
        zzbfn zzbfnVar = (zzbfn) zzbVar;
        zzbfm zzbfmVar = new zzbfm(this);
        try {
            com.google.android.gms.clearcut.zze zzeVar = this.zzfqh;
            if (zzeVar.zzfpm != null && zzeVar.zzfpt.zzpzb.length == 0) {
                zzeVar.zzfpt.zzpzb = zzeVar.zzfpm.zzahc();
            }
            if (zzeVar.zzfqg != null && zzeVar.zzfpt.zzpzi.length == 0) {
                zzeVar.zzfpt.zzpzi = zzeVar.zzfqg.zzahc();
            }
            zzeVar.zzfqa = zzfls.zzc(zzeVar.zzfpt);
            ((zzbfr) zzbfnVar.zzalw()).zza(zzbfmVar, this.zzfqh);
        } catch (RuntimeException e) {
            Log.e("ClearcutLoggerApiImpl", "derived ClearcutLogger.MessageProducer ", e);
            zzu(new Status(10, "MessageProducer"));
        }
    }

    @Override // com.google.android.gms.common.api.internal.BasePendingResult
    protected final /* synthetic */ Result zzb(Status status) {
        return status;
    }
}
