package com.google.android.gms.common.api.internal;

import android.support.annotation.NonNull;
import android.support.annotation.WorkerThread;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.Api;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/* JADX INFO: loaded from: classes.dex */
final class zzar extends zzay {
    final /* synthetic */ zzao zzfxt;
    private final Map<Api.zze, zzaq> zzfxv;

    /* JADX WARN: 'super' call moved to the top of the method (can break code semantics) */
    public zzar(zzao zzaoVar, Map<Api.zze, zzaq> map) {
        super(zzaoVar, null);
        this.zzfxt = zzaoVar;
        this.zzfxv = map;
    }

    private final int zza(@NonNull Api.zze zzeVar, @NonNull Map<Api.zze, Integer> map) {
        int iZzc;
        com.google.android.gms.common.internal.zzbq.checkNotNull(zzeVar);
        com.google.android.gms.common.internal.zzbq.checkNotNull(map);
        if (!zzeVar.zzahn()) {
            return 0;
        }
        if (map.containsKey(zzeVar)) {
            return map.get(zzeVar).intValue();
        }
        Iterator<Api.zze> it = map.keySet().iterator();
        if (it.hasNext()) {
            Api.zze next = it.next();
            next.zzahq();
            zzeVar.zzahq();
            iZzc = map.get(next).intValue();
        } else {
            iZzc = -1;
        }
        if (iZzc == -1) {
            iZzc = com.google.android.gms.common.zzf.zzc(this.zzfxt.mContext, zzeVar.zzahq());
        }
        map.put(zzeVar, Integer.valueOf(iZzc));
        return iZzc;
    }

    @Override // com.google.android.gms.common.api.internal.zzay
    @WorkerThread
    public final void zzajj() {
        ArrayList arrayList = new ArrayList();
        ArrayList arrayList2 = new ArrayList();
        for (Api.zze zzeVar : this.zzfxv.keySet()) {
            if (!zzeVar.zzahn() || this.zzfxv.get(zzeVar).zzfvo) {
                arrayList2.add(zzeVar);
            } else {
                arrayList.add(zzeVar);
            }
        }
        HashMap map = new HashMap(this.zzfxv.size());
        int iZza = -1;
        int i = 0;
        if (!arrayList.isEmpty()) {
            ArrayList arrayList3 = arrayList;
            int size = arrayList3.size();
            while (i < size) {
                Object obj = arrayList3.get(i);
                i++;
                iZza = zza((Api.zze) obj, map);
                if (iZza != 0) {
                    break;
                }
            }
        } else {
            ArrayList arrayList4 = arrayList2;
            int size2 = arrayList4.size();
            while (i < size2) {
                Object obj2 = arrayList4.get(i);
                i++;
                iZza = zza((Api.zze) obj2, map);
                if (iZza == 0) {
                    break;
                }
            }
        }
        if (iZza != 0) {
            this.zzfxt.zzfxd.zza(new zzas(this, this.zzfxt, new ConnectionResult(iZza, null)));
            return;
        }
        if (this.zzfxt.zzfxn) {
            this.zzfxt.zzfxl.connect();
        }
        for (Api.zze zzeVar2 : this.zzfxv.keySet()) {
            zzaq zzaqVar = this.zzfxv.get(zzeVar2);
            if (!zzeVar2.zzahn() || zza(zzeVar2, map) == 0) {
                zzeVar2.zza(zzaqVar);
            } else {
                this.zzfxt.zzfxd.zza(new zzat(this, this.zzfxt, zzaqVar));
            }
        }
    }
}
