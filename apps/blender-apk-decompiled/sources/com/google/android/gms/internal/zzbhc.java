package com.google.android.gms.internal;

import android.os.Parcel;
import android.os.RemoteException;

/* JADX INFO: loaded from: classes.dex */
public abstract class zzbhc extends zzew implements zzbhb {
    public zzbhc() {
        attachInterface(this, "com.google.android.gms.common.internal.service.ICommonCallbacks");
    }

    @Override // android.os.Binder
    public boolean onTransact(int i, Parcel parcel, Parcel parcel2, int i2) throws RemoteException {
        if (zza(i, parcel, parcel2, i2)) {
            return true;
        }
        if (i != 1) {
            return false;
        }
        zzch(parcel.readInt());
        parcel2.writeNoException();
        return true;
    }
}
