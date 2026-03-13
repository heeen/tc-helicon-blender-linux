package com.google.android.gms.internal;

import android.os.Parcel;
import android.os.Parcelable;
import com.google.android.gms.common.internal.Hide;

/* JADX INFO: loaded from: classes.dex */
@Hide
public final class zzbib implements Parcelable.Creator<zzbia> {
    @Override // android.os.Parcelable.Creator
    public final /* synthetic */ zzbia createFromParcel(Parcel parcel) {
        int iZzd = zzbgm.zzd(parcel);
        Parcel parcelZzad = null;
        int iZzg = 0;
        zzbhv zzbhvVar = null;
        while (parcel.dataPosition() < iZzd) {
            int i = parcel.readInt();
            switch (65535 & i) {
                case 1:
                    iZzg = zzbgm.zzg(parcel, i);
                    break;
                case 2:
                    parcelZzad = zzbgm.zzad(parcel, i);
                    break;
                case 3:
                    zzbhvVar = (zzbhv) zzbgm.zza(parcel, i, zzbhv.CREATOR);
                    break;
                default:
                    zzbgm.zzb(parcel, i);
                    break;
            }
        }
        zzbgm.zzaf(parcel, iZzd);
        return new zzbia(iZzg, parcelZzad, zzbhvVar);
    }

    @Override // android.os.Parcelable.Creator
    public final /* synthetic */ zzbia[] newArray(int i) {
        return new zzbia[i];
    }
}
