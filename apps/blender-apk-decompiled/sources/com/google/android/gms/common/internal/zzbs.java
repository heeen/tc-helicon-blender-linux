package com.google.android.gms.common.internal;

import android.accounts.Account;
import android.os.Parcel;
import android.os.Parcelable;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.internal.zzbgm;

/* JADX INFO: loaded from: classes.dex */
@Hide
public final class zzbs implements Parcelable.Creator<zzbr> {
    @Override // android.os.Parcelable.Creator
    public final /* synthetic */ zzbr createFromParcel(Parcel parcel) {
        int iZzd = zzbgm.zzd(parcel);
        Account account = null;
        int iZzg = 0;
        GoogleSignInAccount googleSignInAccount = null;
        int iZzg2 = 0;
        while (parcel.dataPosition() < iZzd) {
            int i = parcel.readInt();
            switch (65535 & i) {
                case 1:
                    iZzg = zzbgm.zzg(parcel, i);
                    break;
                case 2:
                    account = (Account) zzbgm.zza(parcel, i, Account.CREATOR);
                    break;
                case 3:
                    iZzg2 = zzbgm.zzg(parcel, i);
                    break;
                case 4:
                    googleSignInAccount = (GoogleSignInAccount) zzbgm.zza(parcel, i, GoogleSignInAccount.CREATOR);
                    break;
                default:
                    zzbgm.zzb(parcel, i);
                    break;
            }
        }
        zzbgm.zzaf(parcel, iZzd);
        return new zzbr(iZzg, account, iZzg2, googleSignInAccount);
    }

    @Override // android.os.Parcelable.Creator
    public final /* synthetic */ zzbr[] newArray(int i) {
        return new zzbr[i];
    }
}
