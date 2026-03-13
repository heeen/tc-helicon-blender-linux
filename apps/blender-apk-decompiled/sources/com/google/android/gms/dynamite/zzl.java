package com.google.android.gms.dynamite;

import android.os.IBinder;
import android.os.Parcel;
import android.os.RemoteException;
import com.google.android.gms.dynamic.IObjectWrapper;
import com.google.android.gms.internal.zzev;
import com.google.android.gms.internal.zzex;

/* JADX INFO: loaded from: classes.dex */
public final class zzl extends zzev implements zzk {
    zzl(IBinder iBinder) {
        super(iBinder, "com.google.android.gms.dynamite.IDynamiteLoader");
    }

    @Override // com.google.android.gms.dynamite.zzk
    public final int zza(IObjectWrapper iObjectWrapper, String str, boolean z) throws RemoteException {
        Parcel parcelZzbc = zzbc();
        zzex.zza(parcelZzbc, iObjectWrapper);
        parcelZzbc.writeString(str);
        zzex.zza(parcelZzbc, z);
        Parcel parcelZza = zza(3, parcelZzbc);
        int i = parcelZza.readInt();
        parcelZza.recycle();
        return i;
    }

    @Override // com.google.android.gms.dynamite.zzk
    public final IObjectWrapper zza(IObjectWrapper iObjectWrapper, String str, int i) throws RemoteException {
        Parcel parcelZzbc = zzbc();
        zzex.zza(parcelZzbc, iObjectWrapper);
        parcelZzbc.writeString(str);
        parcelZzbc.writeInt(i);
        Parcel parcelZza = zza(2, parcelZzbc);
        IObjectWrapper iObjectWrapperZzaq = IObjectWrapper.zza.zzaq(parcelZza.readStrongBinder());
        parcelZza.recycle();
        return iObjectWrapperZzaq;
    }
}
