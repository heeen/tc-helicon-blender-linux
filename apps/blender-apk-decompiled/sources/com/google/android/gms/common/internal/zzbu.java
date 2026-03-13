package com.google.android.gms.common.internal;

import android.os.IBinder;
import android.os.Parcel;
import android.os.Parcelable;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.internal.zzbgm;

/* JADX INFO: loaded from: classes.dex */
@Hide
public final class zzbu implements Parcelable.Creator<zzbt> {
    @Override // android.os.Parcelable.Creator
    public final /* synthetic */ zzbt createFromParcel(Parcel parcel) {
        int iZzd = zzbgm.zzd(parcel);
        IBinder iBinderZzr = null;
        ConnectionResult connectionResult = null;
        int iZzg = 0;
        boolean zZzc = false;
        boolean zZzc2 = false;
        while (parcel.dataPosition() < iZzd) {
            int i = parcel.readInt();
            switch (65535 & i) {
                case 1:
                    iZzg = zzbgm.zzg(parcel, i);
                    break;
                case 2:
                    iBinderZzr = zzbgm.zzr(parcel, i);
                    break;
                case 3:
                    connectionResult = (ConnectionResult) zzbgm.zza(parcel, i, ConnectionResult.CREATOR);
                    break;
                case 4:
                    zZzc = zzbgm.zzc(parcel, i);
                    break;
                case 5:
                    zZzc2 = zzbgm.zzc(parcel, i);
                    break;
                default:
                    zzbgm.zzb(parcel, i);
                    break;
            }
        }
        zzbgm.zzaf(parcel, iZzd);
        return new zzbt(iZzg, iBinderZzr, connectionResult, zZzc, zZzc2);
    }

    @Override // android.os.Parcelable.Creator
    public final /* synthetic */ zzbt[] newArray(int i) {
        return new zzbt[i];
    }
}
