package com.google.android.gms.common.api.internal;

import android.os.IBinder;
import java.lang.ref.WeakReference;
import java.util.NoSuchElementException;

/* JADX INFO: loaded from: classes.dex */
final class zzdm implements IBinder.DeathRecipient, zzdn {
    private final WeakReference<BasePendingResult<?>> zzgbv;
    private final WeakReference<com.google.android.gms.common.api.zze> zzgbw;
    private final WeakReference<IBinder> zzgbx;

    private zzdm(BasePendingResult<?> basePendingResult, com.google.android.gms.common.api.zze zzeVar, IBinder iBinder) {
        this.zzgbw = new WeakReference<>(zzeVar);
        this.zzgbv = new WeakReference<>(basePendingResult);
        this.zzgbx = new WeakReference<>(iBinder);
    }

    /* synthetic */ zzdm(BasePendingResult basePendingResult, com.google.android.gms.common.api.zze zzeVar, IBinder iBinder, zzdl zzdlVar) {
        this(basePendingResult, null, iBinder);
    }

    private final void zzale() {
        BasePendingResult<?> basePendingResult = this.zzgbv.get();
        com.google.android.gms.common.api.zze zzeVar = this.zzgbw.get();
        if (zzeVar != null && basePendingResult != null) {
            zzeVar.remove(basePendingResult.zzaid().intValue());
        }
        IBinder iBinder = this.zzgbx.get();
        if (iBinder != null) {
            try {
                iBinder.unlinkToDeath(this, 0);
            } catch (NoSuchElementException unused) {
            }
        }
    }

    @Override // android.os.IBinder.DeathRecipient
    public final void binderDied() {
        zzale();
    }

    @Override // com.google.android.gms.common.api.internal.zzdn
    public final void zzc(BasePendingResult<?> basePendingResult) {
        zzale();
    }
}
