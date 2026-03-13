package com.google.android.gms.common.internal;

import android.accounts.Account;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Parcel;
import android.os.Parcelable;
import com.google.android.gms.common.api.Scope;
import com.google.android.gms.internal.zzbgm;

/* JADX INFO: loaded from: classes.dex */
@Hide
public final class zzaa implements Parcelable.Creator<zzz> {
    @Override // android.os.Parcelable.Creator
    public final /* synthetic */ zzz createFromParcel(Parcel parcel) {
        int iZzd = zzbgm.zzd(parcel);
        int iZzg = 0;
        int iZzg2 = 0;
        int iZzg3 = 0;
        String strZzq = null;
        IBinder iBinderZzr = null;
        Scope[] scopeArr = null;
        Bundle bundleZzs = null;
        Account account = null;
        com.google.android.gms.common.zzc[] zzcVarArr = null;
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
                    iZzg3 = zzbgm.zzg(parcel, i);
                    break;
                case 4:
                    strZzq = zzbgm.zzq(parcel, i);
                    break;
                case 5:
                    iBinderZzr = zzbgm.zzr(parcel, i);
                    break;
                case 6:
                    scopeArr = (Scope[]) zzbgm.zzb(parcel, i, Scope.CREATOR);
                    break;
                case 7:
                    bundleZzs = zzbgm.zzs(parcel, i);
                    break;
                case 8:
                    account = (Account) zzbgm.zza(parcel, i, Account.CREATOR);
                    break;
                case 9:
                default:
                    zzbgm.zzb(parcel, i);
                    break;
                case 10:
                    zzcVarArr = (com.google.android.gms.common.zzc[]) zzbgm.zzb(parcel, i, com.google.android.gms.common.zzc.CREATOR);
                    break;
            }
        }
        zzbgm.zzaf(parcel, iZzd);
        return new zzz(iZzg, iZzg2, iZzg3, strZzq, iBinderZzr, scopeArr, bundleZzs, account, zzcVarArr);
    }

    @Override // android.os.Parcelable.Creator
    public final /* synthetic */ zzz[] newArray(int i) {
        return new zzz[i];
    }
}
