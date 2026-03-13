package com.google.android.gms.internal;

/* JADX INFO: loaded from: classes.dex */
public final class zzh implements zzab {
    private int zzr;
    private int zzs;
    private final int zzt;
    private final float zzu;

    public zzh() {
        this(2500, 1, 1.0f);
    }

    private zzh(int i, int i2, float f) {
        this.zzr = 2500;
        this.zzt = 1;
        this.zzu = 1.0f;
    }

    @Override // com.google.android.gms.internal.zzab
    public final void zza(zzae zzaeVar) throws zzae {
        this.zzs++;
        this.zzr = (int) (this.zzr + (this.zzr * this.zzu));
        if (!(this.zzs <= this.zzt)) {
            throw zzaeVar;
        }
    }

    @Override // com.google.android.gms.internal.zzab
    public final int zzb() {
        return this.zzr;
    }

    @Override // com.google.android.gms.internal.zzab
    public final int zzc() {
        return this.zzs;
    }
}
