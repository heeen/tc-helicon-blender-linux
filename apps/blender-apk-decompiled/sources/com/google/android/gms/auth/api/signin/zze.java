package com.google.android.gms.auth.api.signin;

import android.accounts.Account;
import android.os.Parcel;
import android.os.Parcelable;
import com.google.android.gms.auth.api.signin.internal.zzo;
import com.google.android.gms.common.api.Scope;
import com.google.android.gms.common.internal.Hide;
import com.google.android.gms.internal.zzbgm;
import java.util.ArrayList;

/* JADX INFO: loaded from: classes.dex */
@Hide
public final class zze implements Parcelable.Creator<GoogleSignInOptions> {
    @Override // android.os.Parcelable.Creator
    public final /* synthetic */ GoogleSignInOptions createFromParcel(Parcel parcel) {
        int iZzd = zzbgm.zzd(parcel);
        int iZzg = 0;
        boolean zZzc = false;
        boolean zZzc2 = false;
        boolean zZzc3 = false;
        ArrayList arrayListZzc = null;
        Account account = null;
        String strZzq = null;
        String strZzq2 = null;
        ArrayList arrayListZzc2 = null;
        while (parcel.dataPosition() < iZzd) {
            int i = parcel.readInt();
            switch (65535 & i) {
                case 1:
                    iZzg = zzbgm.zzg(parcel, i);
                    break;
                case 2:
                    arrayListZzc = zzbgm.zzc(parcel, i, Scope.CREATOR);
                    break;
                case 3:
                    account = (Account) zzbgm.zza(parcel, i, Account.CREATOR);
                    break;
                case 4:
                    zZzc = zzbgm.zzc(parcel, i);
                    break;
                case 5:
                    zZzc2 = zzbgm.zzc(parcel, i);
                    break;
                case 6:
                    zZzc3 = zzbgm.zzc(parcel, i);
                    break;
                case 7:
                    strZzq = zzbgm.zzq(parcel, i);
                    break;
                case 8:
                    strZzq2 = zzbgm.zzq(parcel, i);
                    break;
                case 9:
                    arrayListZzc2 = zzbgm.zzc(parcel, i, zzo.CREATOR);
                    break;
                default:
                    zzbgm.zzb(parcel, i);
                    break;
            }
        }
        zzbgm.zzaf(parcel, iZzd);
        return new GoogleSignInOptions(iZzg, (ArrayList<Scope>) arrayListZzc, account, zZzc, zZzc2, zZzc3, strZzq, strZzq2, (ArrayList<zzo>) arrayListZzc2);
    }

    @Override // android.os.Parcelable.Creator
    public final /* synthetic */ GoogleSignInOptions[] newArray(int i) {
        return new GoogleSignInOptions[i];
    }
}
