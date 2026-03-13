package com.google.firebase.iid;

import android.content.Intent;

/* JADX INFO: loaded from: classes.dex */
final class zzc implements Runnable {
    private /* synthetic */ Intent val$intent;
    private /* synthetic */ Intent zzimg;
    private /* synthetic */ zzb zzokh;

    zzc(zzb zzbVar, Intent intent, Intent intent2) {
        this.zzokh = zzbVar;
        this.val$intent = intent;
        this.zzimg = intent2;
    }

    @Override // java.lang.Runnable
    public final void run() {
        this.zzokh.handleIntent(this.val$intent);
        this.zzokh.zzh(this.zzimg);
    }
}
