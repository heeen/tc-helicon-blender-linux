package com.google.android.gms.dynamic;

import android.app.Activity;
import android.os.Bundle;

/* JADX INFO: loaded from: classes.dex */
final class zzc implements zzi {
    private /* synthetic */ Activity val$activity;
    private /* synthetic */ Bundle zzaik;
    private /* synthetic */ zza zzhct;
    private /* synthetic */ Bundle zzhcu;

    zzc(zza zzaVar, Activity activity, Bundle bundle, Bundle bundle2) {
        this.zzhct = zzaVar;
        this.val$activity = activity;
        this.zzhcu = bundle;
        this.zzaik = bundle2;
    }

    @Override // com.google.android.gms.dynamic.zzi
    public final int getState() {
        return 0;
    }

    @Override // com.google.android.gms.dynamic.zzi
    public final void zzb(LifecycleDelegate lifecycleDelegate) {
        this.zzhct.zzhcp.onInflate(this.val$activity, this.zzhcu, this.zzaik);
    }
}
