package com.google.android.gms.phenotype;

import android.os.Parcel;
import android.os.Parcelable;
import com.google.android.gms.common.internal.Hide;
import com.google.android.gms.internal.zzbgm;

/* JADX INFO: loaded from: classes.dex */
@Hide
public final class zzc implements Parcelable.Creator<Configuration> {
    @Override // android.os.Parcelable.Creator
    public final /* synthetic */ Configuration createFromParcel(Parcel parcel) {
        int iZzd = zzbgm.zzd(parcel);
        zzi[] zziVarArr = null;
        int iZzg = 0;
        String[] strArrZzaa = null;
        while (parcel.dataPosition() < iZzd) {
            int i = parcel.readInt();
            switch (65535 & i) {
                case 2:
                    iZzg = zzbgm.zzg(parcel, i);
                    break;
                case 3:
                    zziVarArr = (zzi[]) zzbgm.zzb(parcel, i, zzi.CREATOR);
                    break;
                case 4:
                    strArrZzaa = zzbgm.zzaa(parcel, i);
                    break;
                default:
                    zzbgm.zzb(parcel, i);
                    break;
            }
        }
        zzbgm.zzaf(parcel, iZzd);
        return new Configuration(iZzg, zziVarArr, strArrZzaa);
    }

    @Override // android.os.Parcelable.Creator
    public final /* synthetic */ Configuration[] newArray(int i) {
        return new Configuration[i];
    }
}
