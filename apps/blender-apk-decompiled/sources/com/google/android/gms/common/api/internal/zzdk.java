package com.google.android.gms.common.api.internal;

import android.os.IBinder;
import android.os.RemoteException;
import com.google.android.gms.common.api.Api;
import com.google.android.gms.common.api.Result;
import com.google.android.gms.common.api.Status;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

/* JADX INFO: loaded from: classes.dex */
public final class zzdk {
    public static final Status zzgbq = new Status(8, "The connection to Google Play services was lost");
    private static final BasePendingResult<?>[] zzgbr = new BasePendingResult[0];
    private final Map<Api.zzc<?>, Api.zze> zzfyj;
    final Set<BasePendingResult<?>> zzgbs = Collections.synchronizedSet(Collections.newSetFromMap(new WeakHashMap()));
    private final zzdn zzgbt = new zzdl(this);

    public zzdk(Map<Api.zzc<?>, Api.zze> map) {
        this.zzfyj = map;
    }

    /* JADX WARN: Multi-variable type inference failed */
    public final void release() {
        for (BasePendingResult basePendingResult : (BasePendingResult[]) this.zzgbs.toArray(zzgbr)) {
            com.google.android.gms.common.api.zze zzeVar = null;
            Object[] objArr = 0;
            Object[] objArr2 = 0;
            Object[] objArr3 = 0;
            Object[] objArr4 = 0;
            basePendingResult.zza((zzdn) null);
            if (basePendingResult.zzaid() == null) {
                if (basePendingResult.zzaip()) {
                }
            } else {
                basePendingResult.setResultCallback(null);
                IBinder iBinderZzaho = this.zzfyj.get(((zzm) basePendingResult).zzahm()).zzaho();
                if (basePendingResult.isReady()) {
                    basePendingResult.zza(new zzdm(basePendingResult, objArr4 == true ? 1 : 0, iBinderZzaho, objArr3 == true ? 1 : 0));
                } else {
                    if (iBinderZzaho == null || !iBinderZzaho.isBinderAlive()) {
                        basePendingResult.zza((zzdn) null);
                    } else {
                        zzdm zzdmVar = new zzdm(basePendingResult, objArr2 == true ? 1 : 0, iBinderZzaho, objArr == true ? 1 : 0);
                        basePendingResult.zza(zzdmVar);
                        try {
                            iBinderZzaho.linkToDeath(zzdmVar, 0);
                        } catch (RemoteException unused) {
                            basePendingResult.cancel();
                            zzeVar.remove(basePendingResult.zzaid().intValue());
                        }
                    }
                    basePendingResult.cancel();
                    zzeVar.remove(basePendingResult.zzaid().intValue());
                }
            }
            this.zzgbs.remove(basePendingResult);
        }
    }

    public final void zzald() {
        for (BasePendingResult basePendingResult : (BasePendingResult[]) this.zzgbs.toArray(zzgbr)) {
            basePendingResult.zzv(zzgbq);
        }
    }

    final void zzb(BasePendingResult<? extends Result> basePendingResult) {
        this.zzgbs.add(basePendingResult);
        basePendingResult.zza(this.zzgbt);
    }
}
