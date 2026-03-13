package com.google.android.gms.internal;

import android.os.Parcel;
import android.os.Parcelable;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.internal.Hide;
import com.google.android.gms.common.internal.zzbt;

/* JADX INFO: loaded from: classes.dex */
@Hide
public final class zzcyx implements Parcelable.Creator<zzcyw> {
    @Override // android.os.Parcelable.Creator
    public final /* synthetic */ zzcyw createFromParcel(Parcel parcel) {
        int iZzd = zzbgm.zzd(parcel);
        ConnectionResult connectionResult = null;
        int iZzg = 0;
        zzbt zzbtVar = null;
        while (parcel.dataPosition() < iZzd) {
            int i = parcel.readInt();
            switch (65535 & i) {
                case 1:
                    iZzg = zzbgm.zzg(parcel, i);
                    break;
                case 2:
                    connectionResult = (ConnectionResult) zzbgm.zza(parcel, i, ConnectionResult.CREATOR);
                    break;
                case 3:
                    zzbtVar = (zzbt) zzbgm.zza(parcel, i, zzbt.CREATOR);
                    break;
                default:
                    zzbgm.zzb(parcel, i);
                    break;
            }
        }
        zzbgm.zzaf(parcel, iZzd);
        return new zzcyw(iZzg, connectionResult, zzbtVar);
    }

    @Override // android.os.Parcelable.Creator
    public final /* synthetic */ zzcyw[] newArray(int i) {
        return new zzcyw[i];
    }
}
