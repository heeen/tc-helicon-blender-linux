package com.google.android.gms.dynamic;

import android.os.Bundle;
import java.util.Iterator;

/* JADX INFO: Add missing generic type declarations: [T] */
/* JADX INFO: loaded from: classes.dex */
final class zzb<T> implements zzo<T> {
    private /* synthetic */ zza zzhct;

    zzb(zza zzaVar) {
        this.zzhct = zzaVar;
    }

    /* JADX WARN: Incorrect types in method signature: (TT;)V */
    @Override // com.google.android.gms.dynamic.zzo
    public final void zza(LifecycleDelegate lifecycleDelegate) {
        this.zzhct.zzhcp = lifecycleDelegate;
        Iterator it = this.zzhct.zzhcr.iterator();
        while (it.hasNext()) {
            ((zzi) it.next()).zzb(this.zzhct.zzhcp);
        }
        this.zzhct.zzhcr.clear();
        zza.zza(this.zzhct, (Bundle) null);
    }
}
