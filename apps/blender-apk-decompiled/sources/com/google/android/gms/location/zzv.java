package com.google.android.gms.location;

import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.RemoteException;
import com.google.android.gms.internal.zzew;
import com.google.android.gms.internal.zzex;

/* JADX INFO: loaded from: classes.dex */
public abstract class zzv extends zzew implements zzu {
    public zzv() {
        attachInterface(this, "com.google.android.gms.location.ILocationCallback");
    }

    public static zzu zzbe(IBinder iBinder) {
        if (iBinder == null) {
            return null;
        }
        IInterface iInterfaceQueryLocalInterface = iBinder.queryLocalInterface("com.google.android.gms.location.ILocationCallback");
        return iInterfaceQueryLocalInterface instanceof zzu ? (zzu) iInterfaceQueryLocalInterface : new zzw(iBinder);
    }

    @Override // android.os.Binder
    public boolean onTransact(int i, Parcel parcel, Parcel parcel2, int i2) throws RemoteException {
        if (zza(i, parcel, parcel2, i2)) {
            return true;
        }
        switch (i) {
            case 1:
                onLocationResult((LocationResult) zzex.zza(parcel, LocationResult.CREATOR));
                return true;
            case 2:
                onLocationAvailability((LocationAvailability) zzex.zza(parcel, LocationAvailability.CREATOR));
                return true;
            default:
                return false;
        }
    }
}
