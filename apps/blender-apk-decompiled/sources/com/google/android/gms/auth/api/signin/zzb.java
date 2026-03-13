package com.google.android.gms.auth.api.signin;

import android.net.Uri;
import android.os.Parcel;
import android.os.Parcelable;
import com.google.android.gms.common.api.Scope;
import com.google.android.gms.common.internal.Hide;
import com.google.android.gms.internal.zzbgm;
import java.util.ArrayList;

/* JADX INFO: loaded from: classes.dex */
@Hide
public final class zzb implements Parcelable.Creator<GoogleSignInAccount> {
    @Override // android.os.Parcelable.Creator
    public final /* synthetic */ GoogleSignInAccount createFromParcel(Parcel parcel) {
        int iZzd = zzbgm.zzd(parcel);
        String strZzq = null;
        String strZzq2 = null;
        String strZzq3 = null;
        String strZzq4 = null;
        Uri uri = null;
        String strZzq5 = null;
        String strZzq6 = null;
        ArrayList arrayListZzc = null;
        String strZzq7 = null;
        String strZzq8 = null;
        int iZzg = 0;
        long jZzi = 0;
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
                    strZzq2 = zzbgm.zzq(parcel, i);
                    break;
                case 4:
                    strZzq3 = zzbgm.zzq(parcel, i);
                    break;
                case 5:
                    strZzq4 = zzbgm.zzq(parcel, i);
                    break;
                case 6:
                    uri = (Uri) zzbgm.zza(parcel, i, Uri.CREATOR);
                    break;
                case 7:
                    strZzq5 = zzbgm.zzq(parcel, i);
                    break;
                case 8:
                    jZzi = zzbgm.zzi(parcel, i);
                    break;
                case 9:
                    strZzq6 = zzbgm.zzq(parcel, i);
                    break;
                case 10:
                    arrayListZzc = zzbgm.zzc(parcel, i, Scope.CREATOR);
                    break;
                case 11:
                    strZzq7 = zzbgm.zzq(parcel, i);
                    break;
                case 12:
                    strZzq8 = zzbgm.zzq(parcel, i);
                    break;
                default:
                    zzbgm.zzb(parcel, i);
                    break;
            }
        }
        zzbgm.zzaf(parcel, iZzd);
        return new GoogleSignInAccount(iZzg, strZzq, strZzq2, strZzq3, strZzq4, uri, strZzq5, jZzi, strZzq6, arrayListZzc, strZzq7, strZzq8);
    }

    @Override // android.os.Parcelable.Creator
    public final /* synthetic */ GoogleSignInAccount[] newArray(int i) {
        return new GoogleSignInAccount[i];
    }
}
