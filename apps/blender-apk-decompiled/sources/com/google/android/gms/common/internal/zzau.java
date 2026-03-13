package com.google.android.gms.common.internal;

import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.RemoteException;
import com.google.android.gms.dynamic.IObjectWrapper;
import com.google.android.gms.internal.zzew;
import com.google.android.gms.internal.zzex;

/* JADX INFO: loaded from: classes.dex */
public abstract class zzau extends zzew implements zzat {
    public zzau() {
        attachInterface(this, "com.google.android.gms.common.internal.ICertData");
    }

    public static zzat zzam(IBinder iBinder) {
        if (iBinder == null) {
            return null;
        }
        IInterface iInterfaceQueryLocalInterface = iBinder.queryLocalInterface("com.google.android.gms.common.internal.ICertData");
        return iInterfaceQueryLocalInterface instanceof zzat ? (zzat) iInterfaceQueryLocalInterface : new zzav(iBinder);
    }

    @Override // android.os.Binder
    public boolean onTransact(int i, Parcel parcel, Parcel parcel2, int i2) throws RemoteException {
        if (zza(i, parcel, parcel2, i2)) {
            return true;
        }
        switch (i) {
            case 1:
                IObjectWrapper iObjectWrapperZzahg = zzahg();
                parcel2.writeNoException();
                zzex.zza(parcel2, iObjectWrapperZzahg);
                return true;
            case 2:
                int iZzahh = zzahh();
                parcel2.writeNoException();
                parcel2.writeInt(iZzahh);
                return true;
            default:
                return false;
        }
    }
}
