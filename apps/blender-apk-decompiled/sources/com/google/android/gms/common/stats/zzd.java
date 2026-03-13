package com.google.android.gms.common.stats;

import android.os.Parcel;
import android.os.Parcelable;
import com.google.android.gms.common.internal.Hide;
import com.google.android.gms.internal.zzbgm;
import java.util.ArrayList;

/* JADX INFO: loaded from: classes.dex */
@Hide
public final class zzd implements Parcelable.Creator<WakeLockEvent> {
    @Override // android.os.Parcelable.Creator
    public final /* synthetic */ WakeLockEvent createFromParcel(Parcel parcel) {
        int iZzd = zzbgm.zzd(parcel);
        long jZzi = 0;
        long jZzi2 = 0;
        long jZzi3 = 0;
        int iZzg = 0;
        int iZzg2 = 0;
        int iZzg3 = 0;
        int iZzg4 = 0;
        String strZzq = null;
        ArrayList<String> arrayListZzac = null;
        String strZzq2 = null;
        String strZzq3 = null;
        String strZzq4 = null;
        String strZzq5 = null;
        float fZzl = 0.0f;
        while (parcel.dataPosition() < iZzd) {
            int i = parcel.readInt();
            switch (65535 & i) {
                case 1:
                    iZzg = zzbgm.zzg(parcel, i);
                    break;
                case 2:
                    jZzi = zzbgm.zzi(parcel, i);
                    break;
                case 3:
                case 7:
                case 9:
                default:
                    zzbgm.zzb(parcel, i);
                    break;
                case 4:
                    strZzq = zzbgm.zzq(parcel, i);
                    break;
                case 5:
                    iZzg3 = zzbgm.zzg(parcel, i);
                    break;
                case 6:
                    arrayListZzac = zzbgm.zzac(parcel, i);
                    break;
                case 8:
                    jZzi2 = zzbgm.zzi(parcel, i);
                    break;
                case 10:
                    strZzq3 = zzbgm.zzq(parcel, i);
                    break;
                case 11:
                    iZzg2 = zzbgm.zzg(parcel, i);
                    break;
                case 12:
                    strZzq2 = zzbgm.zzq(parcel, i);
                    break;
                case 13:
                    strZzq4 = zzbgm.zzq(parcel, i);
                    break;
                case 14:
                    iZzg4 = zzbgm.zzg(parcel, i);
                    break;
                case 15:
                    fZzl = zzbgm.zzl(parcel, i);
                    break;
                case 16:
                    jZzi3 = zzbgm.zzi(parcel, i);
                    break;
                case 17:
                    strZzq5 = zzbgm.zzq(parcel, i);
                    break;
            }
        }
        zzbgm.zzaf(parcel, iZzd);
        return new WakeLockEvent(iZzg, jZzi, iZzg2, strZzq, iZzg3, arrayListZzac, strZzq2, jZzi2, iZzg4, strZzq3, strZzq4, fZzl, jZzi3, strZzq5);
    }

    @Override // android.os.Parcelable.Creator
    public final /* synthetic */ WakeLockEvent[] newArray(int i) {
        return new WakeLockEvent[i];
    }
}
