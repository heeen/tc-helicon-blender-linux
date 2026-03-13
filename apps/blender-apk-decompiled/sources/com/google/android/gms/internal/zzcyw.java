package com.google.android.gms.internal;

import android.os.Parcel;
import android.os.Parcelable;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.internal.zzbt;

/* JADX INFO: loaded from: classes.dex */
public final class zzcyw extends zzbgl {
    public static final Parcelable.Creator<zzcyw> CREATOR = new zzcyx();
    private int zzehz;
    private final ConnectionResult zzfuw;
    private final zzbt zzklz;

    public zzcyw(int i) {
        this(new ConnectionResult(8, null), null);
    }

    zzcyw(int i, ConnectionResult connectionResult, zzbt zzbtVar) {
        this.zzehz = i;
        this.zzfuw = connectionResult;
        this.zzklz = zzbtVar;
    }

    private zzcyw(ConnectionResult connectionResult, zzbt zzbtVar) {
        this(1, connectionResult, null);
    }

    @Override // android.os.Parcelable
    public final void writeToParcel(Parcel parcel, int i) {
        int iZze = zzbgo.zze(parcel);
        zzbgo.zzc(parcel, 1, this.zzehz);
        zzbgo.zza(parcel, 2, (Parcelable) this.zzfuw, i, false);
        zzbgo.zza(parcel, 3, (Parcelable) this.zzklz, i, false);
        zzbgo.zzai(parcel, iZze);
    }

    public final ConnectionResult zzain() {
        return this.zzfuw;
    }

    public final zzbt zzbfa() {
        return this.zzklz;
    }
}
