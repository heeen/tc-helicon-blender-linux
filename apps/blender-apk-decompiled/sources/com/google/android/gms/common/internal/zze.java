package com.google.android.gms.common.internal;

import android.app.PendingIntent;
import android.os.Bundle;
import android.os.IInterface;
import android.support.annotation.BinderThread;
import com.google.android.gms.common.ConnectionResult;

/* JADX INFO: loaded from: classes.dex */
@Hide
abstract class zze extends zzi<Boolean> {
    private int statusCode;
    private Bundle zzgfj;
    private /* synthetic */ zzd zzgfk;

    /* JADX WARN: 'super' call moved to the top of the method (can break code semantics) */
    @BinderThread
    protected zze(zzd zzdVar, int i, Bundle bundle) {
        super(zzdVar, true);
        this.zzgfk = zzdVar;
        this.statusCode = i;
        this.zzgfj = bundle;
    }

    protected abstract boolean zzama();

    @Override // com.google.android.gms.common.internal.zzi
    protected final void zzamb() {
    }

    protected abstract void zzj(ConnectionResult connectionResult);

    @Override // com.google.android.gms.common.internal.zzi
    protected final /* synthetic */ void zzw(Boolean bool) {
        if (bool == null) {
            this.zzgfk.zza(1, (IInterface) null);
            return;
        }
        int i = this.statusCode;
        if (i == 0) {
            if (zzama()) {
                return;
            }
            this.zzgfk.zza(1, (IInterface) null);
            zzj(new ConnectionResult(8, null));
            return;
        }
        if (i == 10) {
            this.zzgfk.zza(1, (IInterface) null);
            throw new IllegalStateException("A fatal developer error has occurred. Check the logs for further information.");
        }
        this.zzgfk.zza(1, (IInterface) null);
        zzj(new ConnectionResult(this.statusCode, this.zzgfj != null ? (PendingIntent) this.zzgfj.getParcelable("pendingIntent") : null));
    }
}
