package com.google.android.gms.dynamic;

/* JADX INFO: loaded from: classes.dex */
final class zzh implements zzi {
    private /* synthetic */ zza zzhct;

    zzh(zza zzaVar) {
        this.zzhct = zzaVar;
    }

    @Override // com.google.android.gms.dynamic.zzi
    public final int getState() {
        return 5;
    }

    @Override // com.google.android.gms.dynamic.zzi
    public final void zzb(LifecycleDelegate lifecycleDelegate) {
        this.zzhct.zzhcp.onResume();
    }
}
