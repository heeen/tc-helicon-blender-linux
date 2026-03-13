package com.google.android.gms.dynamic;

/* JADX INFO: loaded from: classes.dex */
final class zzg implements zzi {
    private /* synthetic */ zza zzhct;

    zzg(zza zzaVar) {
        this.zzhct = zzaVar;
    }

    @Override // com.google.android.gms.dynamic.zzi
    public final int getState() {
        return 4;
    }

    @Override // com.google.android.gms.dynamic.zzi
    public final void zzb(LifecycleDelegate lifecycleDelegate) {
        this.zzhct.zzhcp.onStart();
    }
}
