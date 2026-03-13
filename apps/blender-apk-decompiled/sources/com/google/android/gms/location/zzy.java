package com.google.android.gms.location;

import android.location.Location;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.RemoteException;
import com.google.android.gms.internal.zzew;
import com.google.android.gms.internal.zzex;

/* JADX INFO: loaded from: classes.dex */
public abstract class zzy extends zzew implements zzx {
    public zzy() {
        attachInterface(this, "com.google.android.gms.location.ILocationListener");
    }

    public static zzx zzbf(IBinder iBinder) {
        if (iBinder == null) {
            return null;
        }
        IInterface iInterfaceQueryLocalInterface = iBinder.queryLocalInterface("com.google.android.gms.location.ILocationListener");
        return iInterfaceQueryLocalInterface instanceof zzx ? (zzx) iInterfaceQueryLocalInterface : new zzz(iBinder);
    }

    @Override // android.os.Binder
    public boolean onTransact(int i, Parcel parcel, Parcel parcel2, int i2) throws RemoteException {
        if (zza(i, parcel, parcel2, i2)) {
            return true;
        }
        if (i != 1) {
            return false;
        }
        onLocationChanged((Location) zzex.zza(parcel, Location.CREATOR));
        return true;
    }
}
