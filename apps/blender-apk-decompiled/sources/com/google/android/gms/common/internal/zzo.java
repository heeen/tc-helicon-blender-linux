package com.google.android.gms.common.internal;

import android.os.Bundle;
import android.support.annotation.BinderThread;
import android.support.annotation.Nullable;
import com.google.android.gms.common.ConnectionResult;

/* JADX INFO: loaded from: classes.dex */
@Hide
public final class zzo extends zze {
    private /* synthetic */ zzd zzgfk;

    /* JADX WARN: 'super' call moved to the top of the method (can break code semantics) */
    @BinderThread
    public zzo(zzd zzdVar, @Nullable int i, Bundle bundle) {
        super(zzdVar, i, null);
        this.zzgfk = zzdVar;
    }

    @Override // com.google.android.gms.common.internal.zze
    protected final boolean zzama() {
        this.zzgfk.zzgew.zzf(ConnectionResult.zzfqt);
        return true;
    }

    @Override // com.google.android.gms.common.internal.zze
    protected final void zzj(ConnectionResult connectionResult) {
        this.zzgfk.zzgew.zzf(connectionResult);
        this.zzgfk.onConnectionFailed(connectionResult);
    }
}
