package com.google.android.gms.location;

import android.os.Parcel;
import android.os.Parcelable;
import com.google.android.gms.common.internal.Hide;
import com.google.android.gms.common.internal.zzbq;
import com.google.android.gms.internal.zzbgl;
import com.google.android.gms.internal.zzbgo;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.Arrays;

/* JADX INFO: loaded from: classes.dex */
public class ActivityTransition extends zzbgl {
    public static final int ACTIVITY_TRANSITION_ENTER = 0;
    public static final int ACTIVITY_TRANSITION_EXIT = 1;
    public static final Parcelable.Creator<ActivityTransition> CREATOR = new zzc();
    private final int zziqw;
    private final int zziqx;

    public static class Builder {
        private int zziqw = -1;
        private int zziqx = -1;

        public ActivityTransition build() {
            zzbq.zza(this.zziqw != -1, "Activity type not set.");
            zzbq.zza(this.zziqx != -1, "Activity transition type not set.");
            return new ActivityTransition(this.zziqw, this.zziqx);
        }

        public Builder setActivityTransition(int i) {
            ActivityTransition.zzeh(i);
            this.zziqx = i;
            return this;
        }

        public Builder setActivityType(int i) {
            DetectedActivity.zzei(i);
            this.zziqw = i;
            return this;
        }
    }

    @Retention(RetentionPolicy.SOURCE)
    public @interface SupportedActivityTransition {
    }

    @Hide
    ActivityTransition(int i, int i2) {
        this.zziqw = i;
        this.zziqx = i2;
    }

    @Hide
    public static void zzeh(int i) {
        boolean z = i >= 0 && i <= 1;
        StringBuilder sb = new StringBuilder(41);
        sb.append("Transition type ");
        sb.append(i);
        sb.append(" is not valid.");
        zzbq.checkArgument(z, sb.toString());
    }

    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof ActivityTransition)) {
            return false;
        }
        ActivityTransition activityTransition = (ActivityTransition) obj;
        return this.zziqw == activityTransition.zziqw && this.zziqx == activityTransition.zziqx;
    }

    public int getActivityType() {
        return this.zziqw;
    }

    public int getTransitionType() {
        return this.zziqx;
    }

    public int hashCode() {
        return Arrays.hashCode(new Object[]{Integer.valueOf(this.zziqw), Integer.valueOf(this.zziqx)});
    }

    public String toString() {
        int i = this.zziqw;
        int i2 = this.zziqx;
        StringBuilder sb = new StringBuilder(75);
        sb.append("ActivityTransition [mActivityType=");
        sb.append(i);
        sb.append(", mTransitionType=");
        sb.append(i2);
        sb.append(']');
        return sb.toString();
    }

    @Override // android.os.Parcelable
    public void writeToParcel(Parcel parcel, int i) {
        int iZze = zzbgo.zze(parcel);
        zzbgo.zzc(parcel, 1, getActivityType());
        zzbgo.zzc(parcel, 2, getTransitionType());
        zzbgo.zzai(parcel, iZze);
    }
}
