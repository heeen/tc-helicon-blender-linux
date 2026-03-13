package com.google.android.gms.internal;

import android.os.IBinder;
import android.os.Parcel;
import android.os.RemoteException;

/* JADX INFO: loaded from: classes.dex */
public final class zzbfs extends zzev implements zzbfr {
    zzbfs(IBinder iBinder) {
        super(iBinder, "com.google.android.gms.clearcut.internal.IClearcutLoggerService");
    }

    @Override // com.google.android.gms.internal.zzbfr
    public final void zza(zzbfp zzbfpVar, com.google.android.gms.clearcut.zze zzeVar) throws RemoteException {
        Parcel parcelZzbc = zzbc();
        zzex.zza(parcelZzbc, zzbfpVar);
        zzex.zza(parcelZzbc, zzeVar);
        zzc(1, parcelZzbc);
    }
}
