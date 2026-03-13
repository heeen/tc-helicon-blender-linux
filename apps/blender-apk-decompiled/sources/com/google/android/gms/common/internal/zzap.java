package com.google.android.gms.common.internal;

import android.accounts.Account;
import android.os.IBinder;
import android.os.Parcel;
import android.os.RemoteException;
import com.google.android.gms.internal.zzev;
import com.google.android.gms.internal.zzex;

/* JADX INFO: loaded from: classes.dex */
public final class zzap extends zzev implements zzan {
    zzap(IBinder iBinder) {
        super(iBinder, "com.google.android.gms.common.internal.IAccountAccessor");
    }

    @Override // com.google.android.gms.common.internal.zzan
    public final Account getAccount() throws RemoteException {
        Parcel parcelZza = zza(2, zzbc());
        Account account = (Account) zzex.zza(parcelZza, Account.CREATOR);
        parcelZza.recycle();
        return account;
    }
}
