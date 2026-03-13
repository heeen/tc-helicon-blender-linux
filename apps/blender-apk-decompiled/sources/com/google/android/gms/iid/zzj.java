package com.google.android.gms.iid;

import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.RemoteException;
import com.google.android.gms.internal.zzew;

/* JADX INFO: loaded from: classes.dex */
public abstract class zzj extends zzew implements zzi {
    public static zzi zzbc(IBinder iBinder) {
        if (iBinder == null) {
            return null;
        }
        IInterface iInterfaceQueryLocalInterface = iBinder.queryLocalInterface("com.google.android.gms.iid.IMessengerCompat");
        return iInterfaceQueryLocalInterface instanceof zzi ? (zzi) iInterfaceQueryLocalInterface : new zzk(iBinder);
    }

    @Override // android.os.Binder
    public boolean onTransact(int i, Parcel parcel, Parcel parcel2, int i2) throws RemoteException {
        throw new NoSuchMethodError();
    }
}
