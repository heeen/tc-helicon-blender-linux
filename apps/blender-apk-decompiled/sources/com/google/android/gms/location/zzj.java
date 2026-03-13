package com.google.android.gms.location;

import android.os.Parcel;
import android.os.Parcelable;
import android.os.SystemClock;
import android.support.v7.widget.ActivityChooserView;
import com.google.android.gms.common.internal.Hide;
import com.google.android.gms.internal.zzbgl;
import com.google.android.gms.internal.zzbgo;
import java.util.Arrays;

/* JADX INFO: loaded from: classes.dex */
@Hide
public final class zzj extends zzbgl {
    public static final Parcelable.Creator<zzj> CREATOR = new zzk();

    @Hide
    private boolean zzirh;

    @Hide
    private long zziri;

    @Hide
    private float zzirj;

    @Hide
    private long zzirk;

    @Hide
    private int zzirl;

    public zzj() {
        this(true, 50L, 0.0f, Long.MAX_VALUE, ActivityChooserView.ActivityChooserViewAdapter.MAX_ACTIVITY_COUNT_UNLIMITED);
    }

    @Hide
    zzj(boolean z, long j, float f, long j2, int i) {
        this.zzirh = z;
        this.zziri = j;
        this.zzirj = f;
        this.zzirk = j2;
        this.zzirl = i;
    }

    public final boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof zzj)) {
            return false;
        }
        zzj zzjVar = (zzj) obj;
        return this.zzirh == zzjVar.zzirh && this.zziri == zzjVar.zziri && Float.compare(this.zzirj, zzjVar.zzirj) == 0 && this.zzirk == zzjVar.zzirk && this.zzirl == zzjVar.zzirl;
    }

    public final int hashCode() {
        return Arrays.hashCode(new Object[]{Boolean.valueOf(this.zzirh), Long.valueOf(this.zziri), Float.valueOf(this.zzirj), Long.valueOf(this.zzirk), Integer.valueOf(this.zzirl)});
    }

    public final String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("DeviceOrientationRequest[mShouldUseMag=");
        sb.append(this.zzirh);
        sb.append(" mMinimumSamplingPeriodMs=");
        sb.append(this.zziri);
        sb.append(" mSmallestAngleChangeRadians=");
        sb.append(this.zzirj);
        if (this.zzirk != Long.MAX_VALUE) {
            long jElapsedRealtime = this.zzirk - SystemClock.elapsedRealtime();
            sb.append(" expireIn=");
            sb.append(jElapsedRealtime);
            sb.append("ms");
        }
        if (this.zzirl != Integer.MAX_VALUE) {
            sb.append(" num=");
            sb.append(this.zzirl);
        }
        sb.append(']');
        return sb.toString();
    }

    @Override // android.os.Parcelable
    public final void writeToParcel(Parcel parcel, int i) {
        int iZze = zzbgo.zze(parcel);
        zzbgo.zza(parcel, 1, this.zzirh);
        zzbgo.zza(parcel, 2, this.zziri);
        zzbgo.zza(parcel, 3, this.zzirj);
        zzbgo.zza(parcel, 4, this.zzirk);
        zzbgo.zzc(parcel, 5, this.zzirl);
        zzbgo.zzai(parcel, iZze);
    }
}
