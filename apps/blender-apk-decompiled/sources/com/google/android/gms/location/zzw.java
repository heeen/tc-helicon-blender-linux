package com.google.android.gms.location;

import android.os.IBinder;
import android.os.Parcel;
import android.os.RemoteException;
import com.google.android.gms.internal.zzev;
import com.google.android.gms.internal.zzex;

/* JADX INFO: loaded from: classes.dex */
public final class zzw extends zzev implements zzu {
    zzw(IBinder iBinder) {
        super(iBinder, "com.google.android.gms.location.ILocationCallback");
    }

    @Override // com.google.android.gms.location.zzu
    public final void onLocationAvailability(LocationAvailability locationAvailability) throws RemoteException {
        Parcel parcelZzbc = zzbc();
        zzex.zza(parcelZzbc, locationAvailability);
        zzc(2, parcelZzbc);
    }

    @Override // com.google.android.gms.location.zzu
    public final void onLocationResult(LocationResult locationResult) throws RemoteException {
        Parcel parcelZzbc = zzbc();
        zzex.zza(parcelZzbc, locationResult);
        zzc(1, parcelZzbc);
    }
}
