package com.google.android.gms.common.internal;

import android.os.Parcel;
import android.os.Parcelable;
import com.google.android.gms.common.api.Scope;
import com.google.android.gms.internal.zzbgm;

/* JADX INFO: loaded from: classes.dex */
@Hide
public final class zzbw implements Parcelable.Creator<zzbv> {
    @Override // android.os.Parcelable.Creator
    public final /* synthetic */ zzbv createFromParcel(Parcel parcel) {
        int iZzd = zzbgm.zzd(parcel);
        int iZzg = 0;
        int iZzg2 = 0;
        Scope[] scopeArr = null;
        int iZzg3 = 0;
        while (parcel.dataPosition() < iZzd) {
            int i = parcel.readInt();
            switch (65535 & i) {
                case 1:
                    iZzg = zzbgm.zzg(parcel, i);
                    break;
                case 2:
                    iZzg3 = zzbgm.zzg(parcel, i);
                    break;
                case 3:
                    iZzg2 = zzbgm.zzg(parcel, i);
                    break;
                case 4:
                    scopeArr = (Scope[]) zzbgm.zzb(parcel, i, Scope.CREATOR);
                    break;
                default:
                    zzbgm.zzb(parcel, i);
                    break;
            }
        }
        zzbgm.zzaf(parcel, iZzd);
        return new zzbv(iZzg, iZzg3, iZzg2, scopeArr);
    }

    @Override // android.os.Parcelable.Creator
    public final /* synthetic */ zzbv[] newArray(int i) {
        return new zzbv[i];
    }
}
