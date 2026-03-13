package com.google.android.gms.auth.api.signin;

import com.google.android.gms.common.api.Scope;
import java.util.Comparator;

/* JADX INFO: loaded from: classes.dex */
final /* synthetic */ class zza implements Comparator {
    static final Comparator zzemo = new zza();

    private zza() {
    }

    @Override // java.util.Comparator
    public final int compare(Object obj, Object obj2) {
        return ((Scope) obj).zzaie().compareTo(((Scope) obj2).zzaie());
    }
}
