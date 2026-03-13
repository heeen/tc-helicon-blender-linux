package com.google.android.gms.internal;

import android.os.RemoteException;

/* JADX INFO: loaded from: classes.dex */
public final class zzcci extends zzcce<Long> {
    public zzcci(int i, String str, Long l) {
        super(0, str, l);
    }

    /* JADX INFO: Access modifiers changed from: private */
    @Override // com.google.android.gms.internal.zzcce
    /* JADX INFO: renamed from: zzd, reason: merged with bridge method [inline-methods] */
    public final Long zza(zzccm zzccmVar) {
        try {
            return Long.valueOf(zzccmVar.getLongFlagValue(getKey(), zzje().longValue(), getSource()));
        } catch (RemoteException unused) {
            return zzje();
        }
    }
}
