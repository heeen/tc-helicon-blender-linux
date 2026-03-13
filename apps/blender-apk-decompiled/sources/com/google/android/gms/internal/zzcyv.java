package com.google.android.gms.internal;

import android.os.Parcel;
import android.os.Parcelable;
import com.google.android.gms.common.internal.Hide;
import com.google.android.gms.common.internal.zzbr;

/* JADX INFO: loaded from: classes.dex */
@Hide
public final class zzcyv implements Parcelable.Creator<zzcyu> {
    @Override // android.os.Parcelable.Creator
    public final /* synthetic */ zzcyu createFromParcel(Parcel parcel) {
        int iZzd = zzbgm.zzd(parcel);
        int iZzg = 0;
        zzbr zzbrVar = null;
        while (parcel.dataPosition() < iZzd) {
            int i = parcel.readInt();
            switch (65535 & i) {
                case 1:
                    iZzg = zzbgm.zzg(parcel, i);
                    break;
                case 2:
                    zzbrVar = (zzbr) zzbgm.zza(parcel, i, zzbr.CREATOR);
                    break;
                default:
                    zzbgm.zzb(parcel, i);
                    break;
            }
        }
        zzbgm.zzaf(parcel, iZzd);
        return new zzcyu(iZzg, zzbrVar);
    }

    @Override // android.os.Parcelable.Creator
    public final /* synthetic */ zzcyu[] newArray(int i) {
        return new zzcyu[i];
    }
}
