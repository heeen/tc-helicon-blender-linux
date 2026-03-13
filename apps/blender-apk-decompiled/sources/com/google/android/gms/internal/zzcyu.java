package com.google.android.gms.internal;

import android.os.Parcel;
import android.os.Parcelable;
import com.google.android.gms.common.internal.zzbr;

/* JADX INFO: loaded from: classes.dex */
public final class zzcyu extends zzbgl {
    public static final Parcelable.Creator<zzcyu> CREATOR = new zzcyv();
    private int zzehz;
    private zzbr zzkly;

    zzcyu(int i, zzbr zzbrVar) {
        this.zzehz = i;
        this.zzkly = zzbrVar;
    }

    public zzcyu(zzbr zzbrVar) {
        this(1, zzbrVar);
    }

    @Override // android.os.Parcelable
    public final void writeToParcel(Parcel parcel, int i) {
        int iZze = zzbgo.zze(parcel);
        zzbgo.zzc(parcel, 1, this.zzehz);
        zzbgo.zza(parcel, 2, (Parcelable) this.zzkly, i, false);
        zzbgo.zzai(parcel, iZze);
    }
}
