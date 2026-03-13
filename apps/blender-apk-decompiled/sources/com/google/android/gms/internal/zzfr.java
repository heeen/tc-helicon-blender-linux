package com.google.android.gms.internal;

import android.os.IBinder;
import android.os.Parcel;
import android.os.RemoteException;

/* JADX INFO: loaded from: classes.dex */
public final class zzfr extends zzev implements zzfp {
    zzfr(IBinder iBinder) {
        super(iBinder, "com.google.android.gms.ads.identifier.internal.IAdvertisingIdService");
    }

    @Override // com.google.android.gms.internal.zzfp
    public final String getId() throws RemoteException {
        Parcel parcelZza = zza(1, zzbc());
        String string = parcelZza.readString();
        parcelZza.recycle();
        return string;
    }

    @Override // com.google.android.gms.internal.zzfp
    public final boolean zzb(boolean z) throws RemoteException {
        Parcel parcelZzbc = zzbc();
        zzex.zza(parcelZzbc, true);
        Parcel parcelZza = zza(2, parcelZzbc);
        boolean zZza = zzex.zza(parcelZza);
        parcelZza.recycle();
        return zZza;
    }

    @Override // com.google.android.gms.internal.zzfp
    public final boolean zzbn() throws RemoteException {
        Parcel parcelZza = zza(6, zzbc());
        boolean zZza = zzex.zza(parcelZza);
        parcelZza.recycle();
        return zZza;
    }
}
