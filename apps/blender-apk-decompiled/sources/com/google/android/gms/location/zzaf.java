package com.google.android.gms.location;

import android.os.Parcel;
import android.os.Parcelable;
import com.google.android.gms.common.internal.Hide;
import com.google.android.gms.internal.zzbgm;

/* JADX INFO: loaded from: classes.dex */
@Hide
public final class zzaf implements Parcelable.Creator<zzae> {
    @Override // android.os.Parcelable.Creator
    public final /* synthetic */ zzae createFromParcel(Parcel parcel) {
        int iZzd = zzbgm.zzd(parcel);
        String strZzq = "";
        String strZzq2 = "";
        String strZzq3 = "";
        while (parcel.dataPosition() < iZzd) {
            int i = parcel.readInt();
            int i2 = 65535 & i;
            if (i2 != 5) {
                switch (i2) {
                    case 1:
                        strZzq2 = zzbgm.zzq(parcel, i);
                        break;
                    case 2:
                        strZzq3 = zzbgm.zzq(parcel, i);
                        break;
                    default:
                        zzbgm.zzb(parcel, i);
                        break;
                }
            } else {
                strZzq = zzbgm.zzq(parcel, i);
            }
        }
        zzbgm.zzaf(parcel, iZzd);
        return new zzae(strZzq, strZzq2, strZzq3);
    }

    @Override // android.os.Parcelable.Creator
    public final /* synthetic */ zzae[] newArray(int i) {
        return new zzae[i];
    }
}
