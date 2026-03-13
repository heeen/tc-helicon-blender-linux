package com.google.android.gms.common.internal;

import android.os.IBinder;
import android.os.Parcel;
import android.os.RemoteException;
import com.google.android.gms.dynamic.IObjectWrapper;
import com.google.android.gms.internal.zzev;
import com.google.android.gms.internal.zzex;

/* JADX INFO: loaded from: classes.dex */
public final class zzbc extends zzev implements zzba {
    zzbc(IBinder iBinder) {
        super(iBinder, "com.google.android.gms.common.internal.IGoogleCertificatesApi");
    }

    @Override // com.google.android.gms.common.internal.zzba
    public final boolean zza(com.google.android.gms.common.zzn zznVar, IObjectWrapper iObjectWrapper) throws RemoteException {
        Parcel parcelZzbc = zzbc();
        zzex.zza(parcelZzbc, zznVar);
        zzex.zza(parcelZzbc, iObjectWrapper);
        Parcel parcelZza = zza(5, parcelZzbc);
        boolean zZza = zzex.zza(parcelZza);
        parcelZza.recycle();
        return zZza;
    }
}
