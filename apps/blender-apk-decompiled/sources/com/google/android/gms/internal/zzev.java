package com.google.android.gms.internal;

import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.RemoteException;

/* JADX INFO: loaded from: classes.dex */
public class zzev implements IInterface {
    private final IBinder zzala;
    private final String zzalb;

    protected zzev(IBinder iBinder, String str) {
        this.zzala = iBinder;
        this.zzalb = str;
    }

    @Override // android.os.IInterface
    public IBinder asBinder() {
        return this.zzala;
    }

    protected final Parcel zza(int i, Parcel parcel) throws RemoteException {
        Parcel parcelObtain = Parcel.obtain();
        try {
            try {
                this.zzala.transact(i, parcel, parcelObtain, 0);
                parcelObtain.readException();
                return parcelObtain;
            } catch (RuntimeException e) {
                parcelObtain.recycle();
                throw e;
            }
        } finally {
            parcel.recycle();
        }
    }

    protected final void zzb(int i, Parcel parcel) throws RemoteException {
        Parcel parcelObtain = Parcel.obtain();
        try {
            this.zzala.transact(i, parcel, parcelObtain, 0);
            parcelObtain.readException();
        } finally {
            parcel.recycle();
            parcelObtain.recycle();
        }
    }

    protected final Parcel zzbc() {
        Parcel parcelObtain = Parcel.obtain();
        parcelObtain.writeInterfaceToken(this.zzalb);
        return parcelObtain;
    }

    protected final void zzc(int i, Parcel parcel) throws RemoteException {
        try {
            this.zzala.transact(i, parcel, null, 1);
        } finally {
            parcel.recycle();
        }
    }
}
