package com.google.android.gms.location;

import android.os.Parcel;
import android.os.Parcelable;
import com.google.android.gms.common.internal.Hide;
import com.google.android.gms.internal.zzbgm;

/* JADX INFO: loaded from: classes.dex */
@Hide
public final class zzk implements Parcelable.Creator<zzj> {
    @Override // android.os.Parcelable.Creator
    public final /* synthetic */ zzj createFromParcel(Parcel parcel) {
        int iZzd = zzbgm.zzd(parcel);
        boolean zZzc = true;
        long jZzi = 50;
        float fZzl = 0.0f;
        long jZzi2 = Long.MAX_VALUE;
        int iZzg = Integer.MAX_VALUE;
        while (parcel.dataPosition() < iZzd) {
            int i = parcel.readInt();
            switch (65535 & i) {
                case 1:
                    zZzc = zzbgm.zzc(parcel, i);
                    break;
                case 2:
                    jZzi = zzbgm.zzi(parcel, i);
                    break;
                case 3:
                    fZzl = zzbgm.zzl(parcel, i);
                    break;
                case 4:
                    jZzi2 = zzbgm.zzi(parcel, i);
                    break;
                case 5:
                    iZzg = zzbgm.zzg(parcel, i);
                    break;
                default:
                    zzbgm.zzb(parcel, i);
                    break;
            }
        }
        zzbgm.zzaf(parcel, iZzd);
        return new zzj(zZzc, jZzi, fZzl, jZzi2, iZzg);
    }

    @Override // android.os.Parcelable.Creator
    public final /* synthetic */ zzj[] newArray(int i) {
        return new zzj[i];
    }
}
