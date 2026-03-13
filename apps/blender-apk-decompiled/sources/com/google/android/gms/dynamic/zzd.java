package com.google.android.gms.dynamic;

import android.os.Bundle;

/* JADX INFO: loaded from: classes.dex */
final class zzd implements zzi {
    private /* synthetic */ Bundle zzaik;
    private /* synthetic */ zza zzhct;

    zzd(zza zzaVar, Bundle bundle) {
        this.zzhct = zzaVar;
        this.zzaik = bundle;
    }

    @Override // com.google.android.gms.dynamic.zzi
    public final int getState() {
        return 1;
    }

    @Override // com.google.android.gms.dynamic.zzi
    public final void zzb(LifecycleDelegate lifecycleDelegate) {
        this.zzhct.zzhcp.onCreate(this.zzaik);
    }
}
