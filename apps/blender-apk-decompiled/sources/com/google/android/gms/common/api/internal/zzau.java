package com.google.android.gms.common.api.internal;

import android.support.annotation.WorkerThread;
import com.google.android.gms.common.api.Api;
import java.util.ArrayList;

/* JADX INFO: loaded from: classes.dex */
final class zzau extends zzay {
    private /* synthetic */ zzao zzfxt;
    private final ArrayList<Api.zze> zzfxz;

    /* JADX WARN: 'super' call moved to the top of the method (can break code semantics) */
    public zzau(zzao zzaoVar, ArrayList<Api.zze> arrayList) {
        super(zzaoVar, null);
        this.zzfxt = zzaoVar;
        this.zzfxz = arrayList;
    }

    @Override // com.google.android.gms.common.api.internal.zzay
    @WorkerThread
    public final void zzajj() {
        this.zzfxt.zzfxd.zzfvq.zzfyk = this.zzfxt.zzajp();
        ArrayList<Api.zze> arrayList = this.zzfxz;
        int size = arrayList.size();
        int i = 0;
        while (i < size) {
            Api.zze zzeVar = arrayList.get(i);
            i++;
            zzeVar.zza(this.zzfxt.zzfxp, this.zzfxt.zzfxd.zzfvq.zzfyk);
        }
    }
}
