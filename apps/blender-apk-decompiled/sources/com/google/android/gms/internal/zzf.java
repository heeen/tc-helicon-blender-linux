package com.google.android.gms.internal;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/* JADX INFO: loaded from: classes.dex */
final class zzf implements zzt {
    private final Map<String, List<zzr<?>>> zzp = new HashMap();
    private final zzd zzq;

    zzf(zzd zzdVar) {
        this.zzq = zzdVar;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public final synchronized boolean zzb(zzr<?> zzrVar) {
        String url = zzrVar.getUrl();
        if (!this.zzp.containsKey(url)) {
            this.zzp.put(url, null);
            zzrVar.zza((zzt) this);
            if (zzaf.DEBUG) {
                zzaf.zzb("new request, sending to network %s", url);
            }
            return false;
        }
        List<zzr<?>> arrayList = this.zzp.get(url);
        if (arrayList == null) {
            arrayList = new ArrayList<>();
        }
        zzrVar.zzb("waiting-for-response");
        arrayList.add(zzrVar);
        this.zzp.put(url, arrayList);
        if (zzaf.DEBUG) {
            zzaf.zzb("Request for cacheKey=%s is in flight, putting on hold.", url);
        }
        return true;
    }

    @Override // com.google.android.gms.internal.zzt
    public final synchronized void zza(zzr<?> zzrVar) {
        String url = zzrVar.getUrl();
        List<zzr<?>> listRemove = this.zzp.remove(url);
        if (listRemove != null && !listRemove.isEmpty()) {
            if (zzaf.DEBUG) {
                zzaf.zza("%d waiting requests for cacheKey=%s; resend to network", Integer.valueOf(listRemove.size()), url);
            }
            zzr<?> zzrVarRemove = listRemove.remove(0);
            this.zzp.put(url, listRemove);
            zzrVarRemove.zza((zzt) this);
            try {
                this.zzq.zzi.put(zzrVarRemove);
            } catch (InterruptedException e) {
                zzaf.zzc("Couldn't add request to queue. %s", e.toString());
                Thread.currentThread().interrupt();
                this.zzq.quit();
            }
        }
    }

    @Override // com.google.android.gms.internal.zzt
    public final void zza(zzr<?> zzrVar, zzx<?> zzxVar) {
        List<zzr<?>> listRemove;
        if (zzxVar.zzbg == null || zzxVar.zzbg.zza()) {
            zza(zzrVar);
            return;
        }
        String url = zzrVar.getUrl();
        synchronized (this) {
            listRemove = this.zzp.remove(url);
        }
        if (listRemove != null) {
            if (zzaf.DEBUG) {
                zzaf.zza("Releasing %d waiting requests for cacheKey=%s.", Integer.valueOf(listRemove.size()), url);
            }
            Iterator<zzr<?>> it = listRemove.iterator();
            while (it.hasNext()) {
                this.zzq.zzk.zzb(it.next(), zzxVar);
            }
        }
    }
}
