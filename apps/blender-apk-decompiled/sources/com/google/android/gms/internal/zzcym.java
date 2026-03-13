package com.google.android.gms.internal;

import android.content.Intent;
import android.os.Parcel;
import android.os.Parcelable;
import com.google.android.gms.common.api.Result;
import com.google.android.gms.common.api.Status;

/* JADX INFO: loaded from: classes.dex */
public final class zzcym extends zzbgl implements Result {
    public static final Parcelable.Creator<zzcym> CREATOR = new zzcyn();
    private int zzehz;
    private int zzklu;
    private Intent zzklv;

    public zzcym() {
        this(0, null);
    }

    zzcym(int i, int i2, Intent intent) {
        this.zzehz = i;
        this.zzklu = i2;
        this.zzklv = intent;
    }

    private zzcym(int i, Intent intent) {
        this(2, 0, null);
    }

    @Override // com.google.android.gms.common.api.Result
    public final Status getStatus() {
        return this.zzklu == 0 ? Status.zzftq : Status.zzftu;
    }

    @Override // android.os.Parcelable
    public final void writeToParcel(Parcel parcel, int i) {
        int iZze = zzbgo.zze(parcel);
        zzbgo.zzc(parcel, 1, this.zzehz);
        zzbgo.zzc(parcel, 2, this.zzklu);
        zzbgo.zza(parcel, 3, (Parcelable) this.zzklv, i, false);
        zzbgo.zzai(parcel, iZze);
    }
}
