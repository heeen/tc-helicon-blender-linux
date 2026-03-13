package com.google.android.gms.common.api.internal;

import android.support.annotation.BinderThread;
import com.google.android.gms.internal.zzcyo;
import com.google.android.gms.internal.zzcyw;
import java.lang.ref.WeakReference;

/* JADX INFO: loaded from: classes.dex */
final class zzav extends zzcyo {
    private final WeakReference<zzao> zzfxu;

    zzav(zzao zzaoVar) {
        this.zzfxu = new WeakReference<>(zzaoVar);
    }

    @Override // com.google.android.gms.internal.zzcyo, com.google.android.gms.internal.zzcyp
    @BinderThread
    public final void zzb(zzcyw zzcywVar) {
        zzao zzaoVar = this.zzfxu.get();
        if (zzaoVar == null) {
            return;
        }
        zzaoVar.zzfxd.zza(new zzaw(this, zzaoVar, zzaoVar, zzcywVar));
    }
}
