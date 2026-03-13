package com.google.android.gms.common.api.internal;

/* JADX INFO: loaded from: classes.dex */
final class zzbs implements com.google.android.gms.common.internal.zzp {
    final /* synthetic */ zzbo zzgaa;

    zzbs(zzbo zzboVar) {
        this.zzgaa = zzboVar;
    }

    @Override // com.google.android.gms.common.internal.zzp
    public final void zzako() {
        this.zzgaa.zzfzq.mHandler.post(new zzbt(this));
    }
}
