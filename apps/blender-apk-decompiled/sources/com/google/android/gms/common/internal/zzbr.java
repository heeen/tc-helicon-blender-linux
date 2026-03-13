package com.google.android.gms.common.internal;

import android.accounts.Account;
import android.os.Parcel;
import android.os.Parcelable;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.internal.zzbgl;
import com.google.android.gms.internal.zzbgo;

/* JADX INFO: loaded from: classes.dex */
public final class zzbr extends zzbgl {
    public static final Parcelable.Creator<zzbr> CREATOR = new zzbs();
    private final Account zzeho;
    private int zzehz;
    private final int zzghv;
    private final GoogleSignInAccount zzghw;

    zzbr(int i, Account account, int i2, GoogleSignInAccount googleSignInAccount) {
        this.zzehz = i;
        this.zzeho = account;
        this.zzghv = i2;
        this.zzghw = googleSignInAccount;
    }

    public zzbr(Account account, int i, GoogleSignInAccount googleSignInAccount) {
        this(2, account, i, googleSignInAccount);
    }

    @Override // android.os.Parcelable
    public final void writeToParcel(Parcel parcel, int i) {
        int iZze = zzbgo.zze(parcel);
        zzbgo.zzc(parcel, 1, this.zzehz);
        zzbgo.zza(parcel, 2, (Parcelable) this.zzeho, i, false);
        zzbgo.zzc(parcel, 3, this.zzghv);
        zzbgo.zza(parcel, 4, (Parcelable) this.zzghw, i, false);
        zzbgo.zzai(parcel, iZze);
    }
}
