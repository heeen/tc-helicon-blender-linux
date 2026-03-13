package com.google.android.gms.common.internal;

import android.os.Parcel;
import android.os.Parcelable;
import com.google.android.gms.common.api.Scope;
import com.google.android.gms.internal.zzbgl;
import com.google.android.gms.internal.zzbgo;

/* JADX INFO: loaded from: classes.dex */
public final class zzbv extends zzbgl {
    public static final Parcelable.Creator<zzbv> CREATOR = new zzbw();
    private int zzehz;
    private final int zzghz;
    private final int zzgia;

    @Deprecated
    private final Scope[] zzgib;

    zzbv(int i, int i2, int i3, Scope[] scopeArr) {
        this.zzehz = i;
        this.zzghz = i2;
        this.zzgia = i3;
        this.zzgib = scopeArr;
    }

    public zzbv(int i, int i2, Scope[] scopeArr) {
        this(1, i, i2, null);
    }

    @Override // android.os.Parcelable
    public final void writeToParcel(Parcel parcel, int i) {
        int iZze = zzbgo.zze(parcel);
        zzbgo.zzc(parcel, 1, this.zzehz);
        zzbgo.zzc(parcel, 2, this.zzghz);
        zzbgo.zzc(parcel, 3, this.zzgia);
        zzbgo.zza(parcel, 4, (Parcelable[]) this.zzgib, i, false);
        zzbgo.zzai(parcel, iZze);
    }
}
