package com.google.android.gms.internal;

import android.content.Intent;
import android.os.Parcel;
import android.os.Parcelable;
import com.google.android.gms.common.internal.Hide;

/* JADX INFO: loaded from: classes.dex */
@Hide
public final class zzcyn implements Parcelable.Creator<zzcym> {
    @Override // android.os.Parcelable.Creator
    public final /* synthetic */ zzcym createFromParcel(Parcel parcel) {
        int iZzd = zzbgm.zzd(parcel);
        int iZzg = 0;
        Intent intent = null;
        int iZzg2 = 0;
        while (parcel.dataPosition() < iZzd) {
            int i = parcel.readInt();
            switch (65535 & i) {
                case 1:
                    iZzg = zzbgm.zzg(parcel, i);
                    break;
                case 2:
                    iZzg2 = zzbgm.zzg(parcel, i);
                    break;
                case 3:
                    intent = (Intent) zzbgm.zza(parcel, i, Intent.CREATOR);
                    break;
                default:
                    zzbgm.zzb(parcel, i);
                    break;
            }
        }
        zzbgm.zzaf(parcel, iZzd);
        return new zzcym(iZzg, iZzg2, intent);
    }

    @Override // android.os.Parcelable.Creator
    public final /* synthetic */ zzcym[] newArray(int i) {
        return new zzcym[i];
    }
}
