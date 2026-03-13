package com.google.android.gms.dynamite;

import android.os.IBinder;
import android.os.Parcel;
import android.os.RemoteException;
import com.google.android.gms.dynamic.IObjectWrapper;
import com.google.android.gms.internal.zzev;
import com.google.android.gms.internal.zzex;

/* JADX INFO: loaded from: classes.dex */
public final class zzn extends zzev implements zzm {
    zzn(IBinder iBinder) {
        super(iBinder, "com.google.android.gms.dynamite.IDynamiteLoaderV2");
    }

    @Override // com.google.android.gms.dynamite.zzm
    public final IObjectWrapper zza(IObjectWrapper iObjectWrapper, String str, int i, IObjectWrapper iObjectWrapper2) throws RemoteException {
        Parcel parcelZzbc = zzbc();
        zzex.zza(parcelZzbc, iObjectWrapper);
        parcelZzbc.writeString(str);
        parcelZzbc.writeInt(i);
        zzex.zza(parcelZzbc, iObjectWrapper2);
        Parcel parcelZza = zza(2, parcelZzbc);
        IObjectWrapper iObjectWrapperZzaq = IObjectWrapper.zza.zzaq(parcelZza.readStrongBinder());
        parcelZza.recycle();
        return iObjectWrapperZzaq;
    }
}
