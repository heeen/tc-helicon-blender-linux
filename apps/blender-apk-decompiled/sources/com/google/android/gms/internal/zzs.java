package com.google.android.gms.internal;

/* JADX INFO: loaded from: classes.dex */
final class zzs implements Runnable {
    private /* synthetic */ String zzas;
    private /* synthetic */ long zzat;
    private /* synthetic */ zzr zzau;

    zzs(zzr zzrVar, String str, long j) {
        this.zzau = zzrVar;
        this.zzas = str;
        this.zzat = j;
    }

    @Override // java.lang.Runnable
    public final void run() {
        this.zzau.zzae.zza(this.zzas, this.zzat);
        this.zzau.zzae.zzc(toString());
    }
}
