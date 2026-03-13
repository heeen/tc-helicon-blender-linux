package com.google.android.gms.common.api;

import android.os.Parcel;
import android.os.Parcelable;
import com.google.android.gms.common.internal.Hide;
import com.google.android.gms.common.internal.ReflectedParcelable;
import com.google.android.gms.common.internal.zzbq;
import com.google.android.gms.internal.zzbgl;
import com.google.android.gms.internal.zzbgo;

/* JADX INFO: loaded from: classes.dex */
public final class Scope extends zzbgl implements ReflectedParcelable {
    public static final Parcelable.Creator<Scope> CREATOR = new zzf();
    private int zzehz;
    private final String zzftp;

    Scope(int i, String str) {
        zzbq.zzh(str, "scopeUri must not be null or empty");
        this.zzehz = i;
        this.zzftp = str;
    }

    public Scope(String str) {
        this(1, str);
    }

    public final boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof Scope) {
            return this.zzftp.equals(((Scope) obj).zzftp);
        }
        return false;
    }

    public final int hashCode() {
        return this.zzftp.hashCode();
    }

    public final String toString() {
        return this.zzftp;
    }

    @Override // android.os.Parcelable
    public final void writeToParcel(Parcel parcel, int i) {
        int iZze = zzbgo.zze(parcel);
        zzbgo.zzc(parcel, 1, this.zzehz);
        zzbgo.zza(parcel, 2, this.zzftp, false);
        zzbgo.zzai(parcel, iZze);
    }

    @Hide
    public final String zzaie() {
        return this.zzftp;
    }
}
