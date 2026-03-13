package com.google.android.gms.common.internal;

import android.accounts.Account;
import android.os.Bundle;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.Parcelable;
import com.google.android.gms.common.api.Scope;
import com.google.android.gms.internal.zzbgl;
import com.google.android.gms.internal.zzbgo;

/* JADX INFO: loaded from: classes.dex */
public final class zzz extends zzbgl {
    public static final Parcelable.Creator<zzz> CREATOR = new zzaa();
    private int version;
    private int zzggb;
    private int zzggc;
    String zzggd;
    IBinder zzgge;
    Scope[] zzggf;
    Bundle zzggg;
    Account zzggh;
    com.google.android.gms.common.zzc[] zzggi;

    public zzz(int i) {
        this.version = 3;
        this.zzggc = com.google.android.gms.common.zzf.GOOGLE_PLAY_SERVICES_VERSION_CODE;
        this.zzggb = i;
    }

    zzz(int i, int i2, int i3, String str, IBinder iBinder, Scope[] scopeArr, Bundle bundle, Account account, com.google.android.gms.common.zzc[] zzcVarArr) {
        this.version = i;
        this.zzggb = i2;
        this.zzggc = i3;
        if ("com.google.android.gms".equals(str)) {
            this.zzggd = "com.google.android.gms";
        } else {
            this.zzggd = str;
        }
        if (i < 2) {
            Account accountZza = null;
            zzan zzapVar = null;
            if (iBinder != null) {
                if (iBinder != null) {
                    IInterface iInterfaceQueryLocalInterface = iBinder.queryLocalInterface("com.google.android.gms.common.internal.IAccountAccessor");
                    zzapVar = iInterfaceQueryLocalInterface instanceof zzan ? (zzan) iInterfaceQueryLocalInterface : new zzap(iBinder);
                }
                accountZza = zza.zza(zzapVar);
            }
            this.zzggh = accountZza;
        } else {
            this.zzgge = iBinder;
            this.zzggh = account;
        }
        this.zzggf = scopeArr;
        this.zzggg = bundle;
        this.zzggi = zzcVarArr;
    }

    @Override // android.os.Parcelable
    public final void writeToParcel(Parcel parcel, int i) {
        int iZze = zzbgo.zze(parcel);
        zzbgo.zzc(parcel, 1, this.version);
        zzbgo.zzc(parcel, 2, this.zzggb);
        zzbgo.zzc(parcel, 3, this.zzggc);
        zzbgo.zza(parcel, 4, this.zzggd, false);
        zzbgo.zza(parcel, 5, this.zzgge, false);
        zzbgo.zza(parcel, 6, (Parcelable[]) this.zzggf, i, false);
        zzbgo.zza(parcel, 7, this.zzggg, false);
        zzbgo.zza(parcel, 8, (Parcelable) this.zzggh, i, false);
        zzbgo.zza(parcel, 10, (Parcelable[]) this.zzggi, i, false);
        zzbgo.zzai(parcel, iZze);
    }
}
