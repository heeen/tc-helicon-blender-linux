package com.google.android.gms.internal;

import android.os.IBinder;
import android.os.Parcel;
import android.os.RemoteException;
import com.google.android.gms.dynamic.IObjectWrapper;

/* JADX INFO: loaded from: classes.dex */
public final class zzcco extends zzev implements zzccm {
    zzcco(IBinder iBinder) {
        super(iBinder, "com.google.android.gms.flags.IFlagProvider");
    }

    @Override // com.google.android.gms.internal.zzccm
    public final boolean getBooleanFlagValue(String str, boolean z, int i) throws RemoteException {
        Parcel parcelZzbc = zzbc();
        parcelZzbc.writeString(str);
        zzex.zza(parcelZzbc, z);
        parcelZzbc.writeInt(i);
        Parcel parcelZza = zza(2, parcelZzbc);
        boolean zZza = zzex.zza(parcelZza);
        parcelZza.recycle();
        return zZza;
    }

    @Override // com.google.android.gms.internal.zzccm
    public final int getIntFlagValue(String str, int i, int i2) throws RemoteException {
        Parcel parcelZzbc = zzbc();
        parcelZzbc.writeString(str);
        parcelZzbc.writeInt(i);
        parcelZzbc.writeInt(i2);
        Parcel parcelZza = zza(3, parcelZzbc);
        int i3 = parcelZza.readInt();
        parcelZza.recycle();
        return i3;
    }

    @Override // com.google.android.gms.internal.zzccm
    public final long getLongFlagValue(String str, long j, int i) throws RemoteException {
        Parcel parcelZzbc = zzbc();
        parcelZzbc.writeString(str);
        parcelZzbc.writeLong(j);
        parcelZzbc.writeInt(i);
        Parcel parcelZza = zza(4, parcelZzbc);
        long j2 = parcelZza.readLong();
        parcelZza.recycle();
        return j2;
    }

    @Override // com.google.android.gms.internal.zzccm
    public final String getStringFlagValue(String str, String str2, int i) throws RemoteException {
        Parcel parcelZzbc = zzbc();
        parcelZzbc.writeString(str);
        parcelZzbc.writeString(str2);
        parcelZzbc.writeInt(i);
        Parcel parcelZza = zza(5, parcelZzbc);
        String string = parcelZza.readString();
        parcelZza.recycle();
        return string;
    }

    @Override // com.google.android.gms.internal.zzccm
    public final void init(IObjectWrapper iObjectWrapper) throws RemoteException {
        Parcel parcelZzbc = zzbc();
        zzex.zza(parcelZzbc, iObjectWrapper);
        zzb(1, parcelZzbc);
    }
}
