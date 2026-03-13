package com.google.android.gms.common.internal;

import android.os.IBinder;
import android.os.Parcel;
import android.os.RemoteException;

/* JADX INFO: loaded from: classes.dex */
final class zzaz implements zzay {
    private final IBinder zzala;

    zzaz(IBinder iBinder) {
        this.zzala = iBinder;
    }

    @Override // android.os.IInterface
    public final IBinder asBinder() {
        return this.zzala;
    }

    @Override // com.google.android.gms.common.internal.zzay
    public final void zza(zzaw zzawVar, zzz zzzVar) throws RemoteException {
        Parcel parcelObtain = Parcel.obtain();
        Parcel parcelObtain2 = Parcel.obtain();
        try {
            parcelObtain.writeInterfaceToken("com.google.android.gms.common.internal.IGmsServiceBroker");
            parcelObtain.writeStrongBinder(zzawVar.asBinder());
            parcelObtain.writeInt(1);
            zzzVar.writeToParcel(parcelObtain, 0);
            this.zzala.transact(46, parcelObtain, parcelObtain2, 0);
            parcelObtain2.readException();
        } finally {
            parcelObtain2.recycle();
            parcelObtain.recycle();
        }
    }
}
