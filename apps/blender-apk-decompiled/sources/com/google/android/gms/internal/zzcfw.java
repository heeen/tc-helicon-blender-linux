package com.google.android.gms.internal;

import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.Parcelable;
import com.google.android.gms.common.internal.Hide;

/* JADX INFO: loaded from: classes.dex */
@Hide
public final class zzcfw extends zzbgl {
    public static final Parcelable.Creator<zzcfw> CREATOR = new zzcfx();
    private int zzitp;
    private zzcfu zzitq;
    private com.google.android.gms.location.zzr zzitr;
    private zzcgr zzits;

    zzcfw(int i, zzcfu zzcfuVar, IBinder iBinder, IBinder iBinder2) {
        this.zzitp = i;
        this.zzitq = zzcfuVar;
        zzcgr zzcgtVar = null;
        this.zzitr = iBinder == null ? null : com.google.android.gms.location.zzs.zzbd(iBinder);
        if (iBinder2 != null && iBinder2 != null) {
            IInterface iInterfaceQueryLocalInterface = iBinder2.queryLocalInterface("com.google.android.gms.location.internal.IFusedLocationProviderCallback");
            zzcgtVar = iInterfaceQueryLocalInterface instanceof zzcgr ? (zzcgr) iInterfaceQueryLocalInterface : new zzcgt(iBinder2);
        }
        this.zzits = zzcgtVar;
    }

    @Override // android.os.Parcelable
    @Hide
    public final void writeToParcel(Parcel parcel, int i) {
        int iZze = zzbgo.zze(parcel);
        zzbgo.zzc(parcel, 1, this.zzitp);
        zzbgo.zza(parcel, 2, (Parcelable) this.zzitq, i, false);
        zzbgo.zza(parcel, 3, this.zzitr == null ? null : this.zzitr.asBinder(), false);
        zzbgo.zza(parcel, 4, this.zzits != null ? this.zzits.asBinder() : null, false);
        zzbgo.zzai(parcel, iZze);
    }
}
