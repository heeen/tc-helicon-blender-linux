package com.google.android.gms.dynamic;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.FrameLayout;

/* JADX INFO: loaded from: classes.dex */
final class zze implements zzi {
    private /* synthetic */ ViewGroup val$container;
    private /* synthetic */ Bundle zzaik;
    private /* synthetic */ zza zzhct;
    private /* synthetic */ FrameLayout zzhcv;
    private /* synthetic */ LayoutInflater zzhcw;

    zze(zza zzaVar, FrameLayout frameLayout, LayoutInflater layoutInflater, ViewGroup viewGroup, Bundle bundle) {
        this.zzhct = zzaVar;
        this.zzhcv = frameLayout;
        this.zzhcw = layoutInflater;
        this.val$container = viewGroup;
        this.zzaik = bundle;
    }

    @Override // com.google.android.gms.dynamic.zzi
    public final int getState() {
        return 2;
    }

    @Override // com.google.android.gms.dynamic.zzi
    public final void zzb(LifecycleDelegate lifecycleDelegate) {
        this.zzhcv.removeAllViews();
        this.zzhcv.addView(this.zzhct.zzhcp.onCreateView(this.zzhcw, this.val$container, this.zzaik));
    }
}
