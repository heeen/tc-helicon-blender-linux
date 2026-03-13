package com.google.android.gms.internal;

import android.os.RemoteException;

/* JADX INFO: loaded from: classes.dex */
public final class zzccg extends zzcce<Boolean> {
    public zzccg(int i, String str, Boolean bool) {
        super(0, str, bool);
    }

    /* JADX INFO: Access modifiers changed from: private */
    @Override // com.google.android.gms.internal.zzcce
    /* JADX INFO: renamed from: zzb, reason: merged with bridge method [inline-methods] */
    public final Boolean zza(zzccm zzccmVar) {
        try {
            return Boolean.valueOf(zzccmVar.getBooleanFlagValue(getKey(), zzje().booleanValue(), getSource()));
        } catch (RemoteException unused) {
            return zzje();
        }
    }
}
