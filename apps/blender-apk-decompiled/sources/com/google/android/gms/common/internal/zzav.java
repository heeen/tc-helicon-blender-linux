package com.google.android.gms.common.internal;

import android.os.IBinder;
import android.os.Parcel;
import android.os.RemoteException;
import com.google.android.gms.dynamic.IObjectWrapper;
import com.google.android.gms.internal.zzev;

/* JADX INFO: loaded from: classes.dex */
public final class zzav extends zzev implements zzat {
    zzav(IBinder iBinder) {
        super(iBinder, "com.google.android.gms.common.internal.ICertData");
    }

    @Override // com.google.android.gms.common.internal.zzat
    public final IObjectWrapper zzahg() throws RemoteException {
        Parcel parcelZza = zza(1, zzbc());
        IObjectWrapper iObjectWrapperZzaq = IObjectWrapper.zza.zzaq(parcelZza.readStrongBinder());
        parcelZza.recycle();
        return iObjectWrapperZzaq;
    }

    @Override // com.google.android.gms.common.internal.zzat
    public final int zzahh() throws RemoteException {
        Parcel parcelZza = zza(2, zzbc());
        int i = parcelZza.readInt();
        parcelZza.recycle();
        return i;
    }
}
