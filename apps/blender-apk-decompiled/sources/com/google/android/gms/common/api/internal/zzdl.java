package com.google.android.gms.common.api.internal;

/* JADX INFO: loaded from: classes.dex */
final class zzdl implements zzdn {
    private /* synthetic */ zzdk zzgbu;

    zzdl(zzdk zzdkVar) {
        this.zzgbu = zzdkVar;
    }

    @Override // com.google.android.gms.common.api.internal.zzdn
    public final void zzc(BasePendingResult<?> basePendingResult) {
        this.zzgbu.zzgbs.remove(basePendingResult);
    }
}
