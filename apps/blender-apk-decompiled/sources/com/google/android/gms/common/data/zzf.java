package com.google.android.gms.common.data;

import android.database.CursorWindow;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import com.google.android.gms.common.internal.Hide;
import com.google.android.gms.internal.zzbgm;

/* JADX INFO: loaded from: classes.dex */
@Hide
public final class zzf implements Parcelable.Creator<DataHolder> {
    @Override // android.os.Parcelable.Creator
    public final /* synthetic */ DataHolder createFromParcel(Parcel parcel) {
        int iZzd = zzbgm.zzd(parcel);
        int iZzg = 0;
        int iZzg2 = 0;
        String[] strArrZzaa = null;
        CursorWindow[] cursorWindowArr = null;
        Bundle bundleZzs = null;
        while (parcel.dataPosition() < iZzd) {
            int i = parcel.readInt();
            int i2 = 65535 & i;
            if (i2 != 1000) {
                switch (i2) {
                    case 1:
                        strArrZzaa = zzbgm.zzaa(parcel, i);
                        break;
                    case 2:
                        cursorWindowArr = (CursorWindow[]) zzbgm.zzb(parcel, i, CursorWindow.CREATOR);
                        break;
                    case 3:
                        iZzg2 = zzbgm.zzg(parcel, i);
                        break;
                    case 4:
                        bundleZzs = zzbgm.zzs(parcel, i);
                        break;
                    default:
                        zzbgm.zzb(parcel, i);
                        break;
                }
            } else {
                iZzg = zzbgm.zzg(parcel, i);
            }
        }
        zzbgm.zzaf(parcel, iZzd);
        DataHolder dataHolder = new DataHolder(iZzg, strArrZzaa, cursorWindowArr, iZzg2, bundleZzs);
        dataHolder.zzali();
        return dataHolder;
    }

    @Override // android.os.Parcelable.Creator
    public final /* synthetic */ DataHolder[] newArray(int i) {
        return new DataHolder[i];
    }
}
