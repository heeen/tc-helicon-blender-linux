package com.google.android.gms.internal;

import android.os.IBinder;
import android.os.Parcel;
import android.os.RemoteException;

/* JADX INFO: loaded from: classes.dex */
public final class zzcys extends zzev implements zzcyr {
    zzcys(IBinder iBinder) {
        super(iBinder, "com.google.android.gms.signin.internal.ISignInService");
    }

    @Override // com.google.android.gms.internal.zzcyr
    public final void zza(com.google.android.gms.common.internal.zzan zzanVar, int i, boolean z) throws RemoteException {
        Parcel parcelZzbc = zzbc();
        zzex.zza(parcelZzbc, zzanVar);
        parcelZzbc.writeInt(i);
        zzex.zza(parcelZzbc, z);
        zzb(9, parcelZzbc);
    }

    @Override // com.google.android.gms.internal.zzcyr
    public final void zza(zzcyu zzcyuVar, zzcyp zzcypVar) throws RemoteException {
        Parcel parcelZzbc = zzbc();
        zzex.zza(parcelZzbc, zzcyuVar);
        zzex.zza(parcelZzbc, zzcypVar);
        zzb(12, parcelZzbc);
    }

    @Override // com.google.android.gms.internal.zzcyr
    public final void zzev(int i) throws RemoteException {
        Parcel parcelZzbc = zzbc();
        parcelZzbc.writeInt(i);
        zzb(7, parcelZzbc);
    }
}
