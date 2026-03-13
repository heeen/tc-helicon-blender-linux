package com.google.android.gms.internal;

import android.os.Parcel;
import android.os.RemoteException;

/* JADX INFO: loaded from: classes.dex */
public abstract class zzcgs extends zzew implements zzcgr {
    public zzcgs() {
        attachInterface(this, "com.google.android.gms.location.internal.IFusedLocationProviderCallback");
    }

    @Override // android.os.Binder
    public boolean onTransact(int i, Parcel parcel, Parcel parcel2, int i2) throws RemoteException {
        if (zza(i, parcel, parcel2, i2)) {
            return true;
        }
        if (i != 1) {
            return false;
        }
        zza((zzcgl) zzex.zza(parcel, zzcgl.CREATOR));
        return true;
    }
}
