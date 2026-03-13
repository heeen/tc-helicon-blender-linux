package com.google.android.gms.common.internal;

import com.google.android.gms.common.api.Response;
import com.google.android.gms.common.api.Result;

/* JADX INFO: Add missing generic type declarations: [R, T] */
/* JADX INFO: loaded from: classes.dex */
final class zzbm<R, T> implements zzbo<R, T> {
    private /* synthetic */ Response zzghu;

    zzbm(Response response) {
        this.zzghu = response;
    }

    @Override // com.google.android.gms.common.internal.zzbo
    public final /* synthetic */ Object zzb(Result result) {
        this.zzghu.setResult(result);
        return this.zzghu;
    }
}
