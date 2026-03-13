package com.google.android.gms.location;

import android.content.Intent;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.Nullable;
import com.google.android.gms.common.internal.Hide;
import com.google.android.gms.common.internal.zzbg;
import com.google.android.gms.common.internal.zzbq;
import com.google.android.gms.internal.zzbgl;
import com.google.android.gms.internal.zzbgo;
import com.google.android.gms.internal.zzbgq;
import com.google.android.gms.internal.zzcfs;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.TreeSet;

/* JADX INFO: loaded from: classes.dex */
public class ActivityTransitionRequest extends zzbgl {
    public static final Parcelable.Creator<ActivityTransitionRequest> CREATOR = new zzf();
    public static final Comparator<ActivityTransition> IS_SAME_TRANSITION = new zze();

    @Nullable
    private final String mTag;
    private final List<ActivityTransition> zziqz;
    private final List<zzcfs> zzira;

    public ActivityTransitionRequest(List<ActivityTransition> list) {
        this(list, null, null);
    }

    @Hide
    public ActivityTransitionRequest(List<ActivityTransition> list, @Nullable String str, @Nullable List<zzcfs> list2) {
        zzbq.checkNotNull(list, "transitions can't be null");
        zzbq.checkArgument(list.size() > 0, "transitions can't be empty.");
        TreeSet treeSet = new TreeSet(IS_SAME_TRANSITION);
        for (ActivityTransition activityTransition : list) {
            zzbq.checkArgument(treeSet.add(activityTransition), String.format("Found duplicated transition: %s.", activityTransition));
        }
        this.zziqz = Collections.unmodifiableList(list);
        this.mTag = str;
        this.zzira = list2 == null ? Collections.emptyList() : Collections.unmodifiableList(list2);
    }

    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj != null && getClass() == obj.getClass()) {
            ActivityTransitionRequest activityTransitionRequest = (ActivityTransitionRequest) obj;
            if (zzbg.equal(this.zziqz, activityTransitionRequest.zziqz) && zzbg.equal(this.mTag, activityTransitionRequest.mTag) && zzbg.equal(this.zzira, activityTransitionRequest.zzira)) {
                return true;
            }
        }
        return false;
    }

    public int hashCode() {
        return (((this.zziqz.hashCode() * 31) + (this.mTag != null ? this.mTag.hashCode() : 0)) * 31) + (this.zzira != null ? this.zzira.hashCode() : 0);
    }

    public void serializeToIntentExtra(Intent intent) {
        zzbgq.zza(this, intent, "com.google.android.location.internal.EXTRA_ACTIVITY_TRANSITION_REQUEST");
    }

    public String toString() {
        String strValueOf = String.valueOf(this.zziqz);
        String str = this.mTag;
        String strValueOf2 = String.valueOf(this.zzira);
        StringBuilder sb = new StringBuilder(String.valueOf(strValueOf).length() + 61 + String.valueOf(str).length() + String.valueOf(strValueOf2).length());
        sb.append("ActivityTransitionRequest [mTransitions=");
        sb.append(strValueOf);
        sb.append(", mTag='");
        sb.append(str);
        sb.append('\'');
        sb.append(", mClients=");
        sb.append(strValueOf2);
        sb.append(']');
        return sb.toString();
    }

    @Override // android.os.Parcelable
    public void writeToParcel(Parcel parcel, int i) {
        int iZze = zzbgo.zze(parcel);
        zzbgo.zzc(parcel, 1, this.zziqz, false);
        zzbgo.zza(parcel, 2, this.mTag, false);
        zzbgo.zzc(parcel, 3, this.zzira, false);
        zzbgo.zzai(parcel, iZze);
    }
}
