package com.google.android.gms.common.internal;

import android.accounts.Account;
import android.os.Binder;
import android.os.RemoteException;
import android.util.Log;

/* JADX INFO: loaded from: classes.dex */
@Hide
public final class zza extends zzao {
    public static Account zza(zzan zzanVar) {
        if (zzanVar != null) {
            long jClearCallingIdentity = Binder.clearCallingIdentity();
            try {
                return zzanVar.getAccount();
            } catch (RemoteException unused) {
                Log.w("AccountAccessor", "Remote account accessor probably died");
                return null;
            } finally {
                Binder.restoreCallingIdentity(jClearCallingIdentity);
            }
        }
        return null;
    }

    public final boolean equals(Object obj) {
        throw new NoSuchMethodError();
    }

    @Override // com.google.android.gms.common.internal.zzan
    public final Account getAccount() {
        throw new NoSuchMethodError();
    }
}
