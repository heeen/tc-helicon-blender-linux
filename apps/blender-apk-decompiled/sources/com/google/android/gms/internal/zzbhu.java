package com.google.android.gms.internal;

import android.os.Parcel;
import android.os.Parcelable;
import com.google.android.gms.common.internal.Hide;

/* JADX INFO: loaded from: classes.dex */
@Hide
public final class zzbhu implements Parcelable.Creator<zzbhx> {
    @Override // android.os.Parcelable.Creator
    public final /* synthetic */ zzbhx createFromParcel(Parcel parcel) {
        int iZzd = zzbgm.zzd(parcel);
        String strZzq = null;
        int iZzg = 0;
        zzbhq zzbhqVar = null;
        while (parcel.dataPosition() < iZzd) {
            int i = parcel.readInt();
            switch (65535 & i) {
                case 1:
                    iZzg = zzbgm.zzg(parcel, i);
                    break;
                case 2:
                    strZzq = zzbgm.zzq(parcel, i);
                    break;
                case 3:
                    zzbhqVar = (zzbhq) zzbgm.zza(parcel, i, zzbhq.CREATOR);
                    break;
                default:
                    zzbgm.zzb(parcel, i);
                    break;
            }
        }
        zzbgm.zzaf(parcel, iZzd);
        return new zzbhx(iZzg, strZzq, zzbhqVar);
    }

    @Override // android.os.Parcelable.Creator
    public final /* synthetic */ zzbhx[] newArray(int i) {
        return new zzbhx[i];
    }
}
