package com.google.android.gms.internal;

import android.os.RemoteException;

/* JADX INFO: loaded from: classes.dex */
public final class zzcch extends zzcce<Integer> {
    public zzcch(int i, String str, Integer num) {
        super(0, str, num);
    }

    /* JADX INFO: Access modifiers changed from: private */
    @Override // com.google.android.gms.internal.zzcce
    /* JADX INFO: renamed from: zzc, reason: merged with bridge method [inline-methods] */
    public final Integer zza(zzccm zzccmVar) {
        try {
            return Integer.valueOf(zzccmVar.getIntFlagValue(getKey(), zzje().intValue(), getSource()));
        } catch (RemoteException unused) {
            return zzje();
        }
    }
}
