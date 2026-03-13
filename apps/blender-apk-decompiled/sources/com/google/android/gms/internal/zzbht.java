package com.google.android.gms.internal;

import android.os.Parcel;
import android.os.Parcelable;
import com.google.android.gms.common.internal.Hide;

/* JADX INFO: loaded from: classes.dex */
@Hide
public final class zzbht implements Parcelable.Creator<zzbhq> {
    @Override // android.os.Parcelable.Creator
    public final /* synthetic */ zzbhq createFromParcel(Parcel parcel) {
        int iZzd = zzbgm.zzd(parcel);
        String strZzq = null;
        String strZzq2 = null;
        zzbhj zzbhjVar = null;
        int iZzg = 0;
        int iZzg2 = 0;
        boolean zZzc = false;
        int iZzg3 = 0;
        boolean zZzc2 = false;
        int iZzg4 = 0;
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
                    zZzc = zzbgm.zzc(parcel, i);
                    break;
                case 4:
                    iZzg3 = zzbgm.zzg(parcel, i);
                    break;
                case 5:
                    zZzc2 = zzbgm.zzc(parcel, i);
                    break;
                case 6:
                    strZzq = zzbgm.zzq(parcel, i);
                    break;
                case 7:
                    iZzg4 = zzbgm.zzg(parcel, i);
                    break;
                case 8:
                    strZzq2 = zzbgm.zzq(parcel, i);
                    break;
                case 9:
                    zzbhjVar = (zzbhj) zzbgm.zza(parcel, i, zzbhj.CREATOR);
                    break;
                default:
                    zzbgm.zzb(parcel, i);
                    break;
            }
        }
        zzbgm.zzaf(parcel, iZzd);
        return new zzbhq(iZzg, iZzg2, zZzc, iZzg3, zZzc2, strZzq, iZzg4, strZzq2, zzbhjVar);
    }

    @Override // android.os.Parcelable.Creator
    public final /* synthetic */ zzbhq[] newArray(int i) {
        return new zzbhq[i];
    }
}
