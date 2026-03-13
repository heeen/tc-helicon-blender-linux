package com.google.android.gms.clearcut;

import android.os.Parcel;
import android.os.Parcelable;
import com.google.android.gms.internal.zzbgl;
import com.google.android.gms.internal.zzbgo;
import java.util.Arrays;

/* JADX INFO: loaded from: classes.dex */
public final class zzc extends zzbgl {
    public static final Parcelable.Creator<zzc> CREATOR = new zzd();
    private boolean zzfpw;
    private long zzfpx;
    private long zzfpy;

    public zzc(boolean z, long j, long j2) {
        this.zzfpw = z;
        this.zzfpx = j;
        this.zzfpy = j2;
    }

    public final boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof zzc) {
            zzc zzcVar = (zzc) obj;
            if (this.zzfpw == zzcVar.zzfpw && this.zzfpx == zzcVar.zzfpx && this.zzfpy == zzcVar.zzfpy) {
                return true;
            }
        }
        return false;
    }

    public final int hashCode() {
        return Arrays.hashCode(new Object[]{Boolean.valueOf(this.zzfpw), Long.valueOf(this.zzfpx), Long.valueOf(this.zzfpy)});
    }

    public final String toString() {
        return "CollectForDebugParcelable[skipPersistentStorage: " + this.zzfpw + ",collectForDebugStartTimeMillis: " + this.zzfpx + ",collectForDebugExpiryTimeMillis: " + this.zzfpy + "]";
    }

    @Override // android.os.Parcelable
    public final void writeToParcel(Parcel parcel, int i) {
        int iZze = zzbgo.zze(parcel);
        zzbgo.zza(parcel, 1, this.zzfpw);
        zzbgo.zza(parcel, 2, this.zzfpy);
        zzbgo.zza(parcel, 3, this.zzfpx);
        zzbgo.zzai(parcel, iZze);
    }
}
