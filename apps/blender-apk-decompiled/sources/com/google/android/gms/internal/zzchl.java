package com.google.android.gms.internal;

import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.Nullable;
import com.google.android.gms.common.internal.Hide;
import com.google.android.gms.common.internal.zzbg;
import com.google.android.gms.location.LocationRequest;
import java.util.Collections;
import java.util.List;

/* JADX INFO: loaded from: classes.dex */
@Hide
public final class zzchl extends zzbgl {

    @Nullable
    private String mTag;

    @Nullable
    private String zzeqs;
    private List<zzcfs> zzira;
    private LocationRequest zzium;
    private boolean zziun;
    private boolean zziuo;
    private boolean zziup;
    private boolean zziuq = true;
    static final List<zzcfs> zzitm = Collections.emptyList();
    public static final Parcelable.Creator<zzchl> CREATOR = new zzchm();

    zzchl(LocationRequest locationRequest, List<zzcfs> list, @Nullable String str, boolean z, boolean z2, boolean z3, String str2) {
        this.zzium = locationRequest;
        this.zzira = list;
        this.mTag = str;
        this.zziun = z;
        this.zziuo = z2;
        this.zziup = z3;
        this.zzeqs = str2;
    }

    @Deprecated
    public static zzchl zza(LocationRequest locationRequest) {
        return new zzchl(locationRequest, zzitm, null, false, false, false, null);
    }

    public final boolean equals(Object obj) {
        if (!(obj instanceof zzchl)) {
            return false;
        }
        zzchl zzchlVar = (zzchl) obj;
        return zzbg.equal(this.zzium, zzchlVar.zzium) && zzbg.equal(this.zzira, zzchlVar.zzira) && zzbg.equal(this.mTag, zzchlVar.mTag) && this.zziun == zzchlVar.zziun && this.zziuo == zzchlVar.zziuo && this.zziup == zzchlVar.zziup && zzbg.equal(this.zzeqs, zzchlVar.zzeqs);
    }

    public final int hashCode() {
        return this.zzium.hashCode();
    }

    public final String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(this.zzium.toString());
        if (this.mTag != null) {
            sb.append(" tag=");
            sb.append(this.mTag);
        }
        if (this.zzeqs != null) {
            sb.append(" moduleId=");
            sb.append(this.zzeqs);
        }
        sb.append(" hideAppOps=");
        sb.append(this.zziun);
        sb.append(" clients=");
        sb.append(this.zzira);
        sb.append(" forceCoarseLocation=");
        sb.append(this.zziuo);
        if (this.zziup) {
            sb.append(" exemptFromBackgroundThrottle");
        }
        return sb.toString();
    }

    @Override // android.os.Parcelable
    public final void writeToParcel(Parcel parcel, int i) {
        int iZze = zzbgo.zze(parcel);
        zzbgo.zza(parcel, 1, (Parcelable) this.zzium, i, false);
        zzbgo.zzc(parcel, 5, this.zzira, false);
        zzbgo.zza(parcel, 6, this.mTag, false);
        zzbgo.zza(parcel, 7, this.zziun);
        zzbgo.zza(parcel, 8, this.zziuo);
        zzbgo.zza(parcel, 9, this.zziup);
        zzbgo.zza(parcel, 10, this.zzeqs, false);
        zzbgo.zzai(parcel, iZze);
    }
}
