package com.google.android.gms.common.internal;

import android.os.IBinder;
import android.os.Parcel;
import android.os.RemoteException;
import com.google.android.gms.dynamic.IObjectWrapper;
import com.google.android.gms.internal.zzev;
import com.google.android.gms.internal.zzex;

/* JADX INFO: loaded from: classes.dex */
public final class zzbe extends zzev implements zzbd {
    zzbe(IBinder iBinder) {
        super(iBinder, "com.google.android.gms.common.internal.ISignInButtonCreator");
    }

    @Override // com.google.android.gms.common.internal.zzbd
    public final IObjectWrapper zza(IObjectWrapper iObjectWrapper, zzbv zzbvVar) throws RemoteException {
        Parcel parcelZzbc = zzbc();
        zzex.zza(parcelZzbc, iObjectWrapper);
        zzex.zza(parcelZzbc, zzbvVar);
        Parcel parcelZza = zza(2, parcelZzbc);
        IObjectWrapper iObjectWrapperZzaq = IObjectWrapper.zza.zzaq(parcelZza.readStrongBinder());
        parcelZza.recycle();
        return iObjectWrapperZzaq;
    }
}
