package com.google.android.gms.internal;

import com.google.android.gms.common.internal.Hide;

/* JADX INFO: loaded from: classes.dex */
public abstract class zzbhs extends zzbhp implements zzbgp {
    @Override // android.os.Parcelable
    @Hide
    public final int describeContents() {
        return 0;
    }

    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!getClass().isInstance(obj)) {
            return false;
        }
        zzbhp zzbhpVar = (zzbhp) obj;
        for (zzbhq<?, ?> zzbhqVar : zzabz().values()) {
            if (zza(zzbhqVar)) {
                if (!zzbhpVar.zza(zzbhqVar) || !zzb(zzbhqVar).equals(zzbhpVar.zzb(zzbhqVar))) {
                    return false;
                }
            } else if (zzbhpVar.zza(zzbhqVar)) {
                return false;
            }
        }
        return true;
    }

    public int hashCode() {
        int iHashCode = 0;
        for (zzbhq<?, ?> zzbhqVar : zzabz().values()) {
            if (zza(zzbhqVar)) {
                iHashCode = (iHashCode * 31) + zzb(zzbhqVar).hashCode();
            }
        }
        return iHashCode;
    }

    @Override // com.google.android.gms.internal.zzbhp
    public Object zzgx(String str) {
        return null;
    }

    @Override // com.google.android.gms.internal.zzbhp
    public boolean zzgy(String str) {
        return false;
    }
}
