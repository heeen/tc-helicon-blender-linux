package com.google.android.gms.phenotype;

import android.database.ContentObserver;
import android.os.Handler;

/* JADX INFO: loaded from: classes.dex */
final class zzb extends ContentObserver {
    private /* synthetic */ zza zzkfy;

    /* JADX WARN: 'super' call moved to the top of the method (can break code semantics) */
    zzb(zza zzaVar, Handler handler) {
        super(null);
        this.zzkfy = zzaVar;
    }

    @Override // android.database.ContentObserver
    public final void onChange(boolean z) {
        this.zzkfy.zzbef();
    }
}
