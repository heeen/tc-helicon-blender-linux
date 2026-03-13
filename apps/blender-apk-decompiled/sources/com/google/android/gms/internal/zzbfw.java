package com.google.android.gms.internal;

import android.os.Parcel;
import android.os.Parcelable;
import com.google.android.gms.common.internal.Hide;

/* JADX INFO: loaded from: classes.dex */
@Hide
public final class zzbfw implements Parcelable.Creator<zzbfv> {
    @Override // android.os.Parcelable.Creator
    public final /* synthetic */ zzbfv createFromParcel(Parcel parcel) {
        int iZzd = zzbgm.zzd(parcel);
        int iZzg = 0;
        int iZzg2 = 0;
        boolean zZzc = false;
        int iZzg3 = 0;
        String strZzq = null;
        String strZzq2 = null;
        String strZzq3 = null;
        String strZzq4 = null;
        boolean zZzc2 = true;
        while (parcel.dataPosition() < iZzd) {
            int i = parcel.readInt();
            switch (65535 & i) {
                case 2:
                    strZzq = zzbgm.zzq(parcel, i);
                    break;
                case 3:
                    iZzg = zzbgm.zzg(parcel, i);
                    break;
                case 4:
                    iZzg2 = zzbgm.zzg(parcel, i);
                    break;
                case 5:
                    strZzq2 = zzbgm.zzq(parcel, i);
                    break;
                case 6:
                    strZzq3 = zzbgm.zzq(parcel, i);
                    break;
                case 7:
                    zZzc2 = zzbgm.zzc(parcel, i);
                    break;
                case 8:
                    strZzq4 = zzbgm.zzq(parcel, i);
                    break;
                case 9:
                    zZzc = zzbgm.zzc(parcel, i);
                    break;
                case 10:
                    iZzg3 = zzbgm.zzg(parcel, i);
                    break;
                default:
                    zzbgm.zzb(parcel, i);
                    break;
            }
        }
        zzbgm.zzaf(parcel, iZzd);
        return new zzbfv(strZzq, iZzg, iZzg2, strZzq2, strZzq3, zZzc2, strZzq4, zZzc, iZzg3);
    }

    @Override // android.os.Parcelable.Creator
    public final /* synthetic */ zzbfv[] newArray(int i) {
        return new zzbfv[i];
    }
}
