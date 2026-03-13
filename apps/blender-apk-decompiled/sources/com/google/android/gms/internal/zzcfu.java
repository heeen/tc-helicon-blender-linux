package com.google.android.gms.internal;

import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.Nullable;
import com.google.android.gms.common.internal.Hide;
import com.google.android.gms.common.internal.zzbg;
import java.util.Collections;
import java.util.List;

/* JADX INFO: loaded from: classes.dex */
@Hide
public final class zzcfu extends zzbgl {

    @Nullable
    private String mTag;
    private List<zzcfs> zzira;
    private com.google.android.gms.location.zzj zzito;
    static final List<zzcfs> zzitm = Collections.emptyList();
    static final com.google.android.gms.location.zzj zzitn = new com.google.android.gms.location.zzj();
    public static final Parcelable.Creator<zzcfu> CREATOR = new zzcfv();

    zzcfu(com.google.android.gms.location.zzj zzjVar, List<zzcfs> list, String str) {
        this.zzito = zzjVar;
        this.zzira = list;
        this.mTag = str;
    }

    public final boolean equals(Object obj) {
        if (!(obj instanceof zzcfu)) {
            return false;
        }
        zzcfu zzcfuVar = (zzcfu) obj;
        return zzbg.equal(this.zzito, zzcfuVar.zzito) && zzbg.equal(this.zzira, zzcfuVar.zzira) && zzbg.equal(this.mTag, zzcfuVar.mTag);
    }

    public final int hashCode() {
        return this.zzito.hashCode();
    }

    @Override // android.os.Parcelable
    public final void writeToParcel(Parcel parcel, int i) {
        int iZze = zzbgo.zze(parcel);
        zzbgo.zza(parcel, 1, (Parcelable) this.zzito, i, false);
        zzbgo.zzc(parcel, 2, this.zzira, false);
        zzbgo.zza(parcel, 3, this.mTag, false);
        zzbgo.zzai(parcel, iZze);
    }
}
