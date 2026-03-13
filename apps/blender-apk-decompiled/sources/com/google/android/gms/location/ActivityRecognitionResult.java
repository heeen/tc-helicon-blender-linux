package com.google.android.gms.location;

import android.content.Intent;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.Nullable;
import com.google.android.gms.common.internal.Hide;
import com.google.android.gms.common.internal.ReflectedParcelable;
import com.google.android.gms.common.internal.zzbg;
import com.google.android.gms.common.internal.zzbq;
import com.google.android.gms.internal.zzbgl;
import com.google.android.gms.internal.zzbgo;
import com.google.android.gms.internal.zzbgq;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/* JADX INFO: loaded from: classes.dex */
public class ActivityRecognitionResult extends zzbgl implements ReflectedParcelable {
    public static final Parcelable.Creator<ActivityRecognitionResult> CREATOR = new zzb();
    private Bundle extras;
    private List<DetectedActivity> zziqs;
    private long zziqt;
    private long zziqu;
    private int zziqv;

    public ActivityRecognitionResult(DetectedActivity detectedActivity, long j, long j2) {
        this(detectedActivity, j, j2, 0, (Bundle) null);
    }

    @Hide
    private ActivityRecognitionResult(DetectedActivity detectedActivity, long j, long j2, int i, Bundle bundle) {
        this((List<DetectedActivity>) Collections.singletonList(detectedActivity), j, j2, 0, (Bundle) null);
    }

    public ActivityRecognitionResult(List<DetectedActivity> list, long j, long j2) {
        this(list, j, j2, 0, (Bundle) null);
    }

    @Hide
    public ActivityRecognitionResult(List<DetectedActivity> list, long j, long j2, int i, Bundle bundle) {
        zzbq.checkArgument(list != null && list.size() > 0, "Must have at least 1 detected activity");
        zzbq.checkArgument(j > 0 && j2 > 0, "Must set times");
        this.zziqs = list;
        this.zziqt = j;
        this.zziqu = j2;
        this.zziqv = i;
        this.extras = bundle;
    }

    /* JADX WARN: Removed duplicated region for block: B:11:0x0025  */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
        To view partially-correct add '--show-bad-code' argument
    */
    public static com.google.android.gms.location.ActivityRecognitionResult extractResult(android.content.Intent r3) {
        /*
            boolean r0 = hasResult(r3)
            r1 = 0
            if (r0 == 0) goto L25
            android.os.Bundle r0 = r3.getExtras()
            java.lang.String r2 = "com.google.android.location.internal.EXTRA_ACTIVITY_RESULT"
            java.lang.Object r0 = r0.get(r2)
            boolean r2 = r0 instanceof byte[]
            if (r2 == 0) goto L20
            byte[] r0 = (byte[]) r0
            android.os.Parcelable$Creator<com.google.android.gms.location.ActivityRecognitionResult> r2 = com.google.android.gms.location.ActivityRecognitionResult.CREATOR
            com.google.android.gms.internal.zzbgp r0 = com.google.android.gms.internal.zzbgq.zza(r0, r2)
        L1d:
            com.google.android.gms.location.ActivityRecognitionResult r0 = (com.google.android.gms.location.ActivityRecognitionResult) r0
            goto L26
        L20:
            boolean r2 = r0 instanceof com.google.android.gms.location.ActivityRecognitionResult
            if (r2 == 0) goto L25
            goto L1d
        L25:
            r0 = r1
        L26:
            if (r0 == 0) goto L29
            return r0
        L29:
            java.util.List r3 = zzk(r3)
            if (r3 == 0) goto L43
            boolean r0 = r3.isEmpty()
            if (r0 == 0) goto L36
            goto L43
        L36:
            int r0 = r3.size()
            int r0 = r0 + (-1)
            java.lang.Object r3 = r3.get(r0)
            com.google.android.gms.location.ActivityRecognitionResult r3 = (com.google.android.gms.location.ActivityRecognitionResult) r3
            return r3
        L43:
            return r1
        */
        throw new UnsupportedOperationException("Method not decompiled: com.google.android.gms.location.ActivityRecognitionResult.extractResult(android.content.Intent):com.google.android.gms.location.ActivityRecognitionResult");
    }

    public static boolean hasResult(Intent intent) {
        if (intent == null) {
            return false;
        }
        if (intent == null ? false : intent.hasExtra("com.google.android.location.internal.EXTRA_ACTIVITY_RESULT")) {
            return true;
        }
        List<ActivityRecognitionResult> listZzk = zzk(intent);
        return (listZzk == null || listZzk.isEmpty()) ? false : true;
    }

    @Hide
    private static boolean zzc(Bundle bundle, Bundle bundle2) {
        if (bundle == null && bundle2 == null) {
            return true;
        }
        if ((bundle == null && bundle2 != null) || ((bundle != null && bundle2 == null) || bundle.size() != bundle2.size())) {
            return false;
        }
        for (String str : bundle.keySet()) {
            if (!bundle2.containsKey(str)) {
                return false;
            }
            if (bundle.get(str) == null) {
                if (bundle2.get(str) != null) {
                    return false;
                }
            } else if (bundle.get(str) instanceof Bundle) {
                if (!zzc(bundle.getBundle(str), bundle2.getBundle(str))) {
                    return false;
                }
            } else if (!bundle.get(str).equals(bundle2.get(str))) {
                return false;
            }
        }
        return true;
    }

    @Hide
    @Nullable
    private static List<ActivityRecognitionResult> zzk(Intent intent) {
        if (intent == null ? false : intent.hasExtra("com.google.android.location.internal.EXTRA_ACTIVITY_RESULT_LIST")) {
            return zzbgq.zzb(intent, "com.google.android.location.internal.EXTRA_ACTIVITY_RESULT_LIST", CREATOR);
        }
        return null;
    }

    @Hide
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj != null && getClass() == obj.getClass()) {
            ActivityRecognitionResult activityRecognitionResult = (ActivityRecognitionResult) obj;
            if (this.zziqt == activityRecognitionResult.zziqt && this.zziqu == activityRecognitionResult.zziqu && this.zziqv == activityRecognitionResult.zziqv && zzbg.equal(this.zziqs, activityRecognitionResult.zziqs) && zzc(this.extras, activityRecognitionResult.extras)) {
                return true;
            }
        }
        return false;
    }

    public int getActivityConfidence(int i) {
        for (DetectedActivity detectedActivity : this.zziqs) {
            if (detectedActivity.getType() == i) {
                return detectedActivity.getConfidence();
            }
        }
        return 0;
    }

    public long getElapsedRealtimeMillis() {
        return this.zziqu;
    }

    public DetectedActivity getMostProbableActivity() {
        return this.zziqs.get(0);
    }

    public List<DetectedActivity> getProbableActivities() {
        return this.zziqs;
    }

    public long getTime() {
        return this.zziqt;
    }

    @Hide
    public int hashCode() {
        return Arrays.hashCode(new Object[]{Long.valueOf(this.zziqt), Long.valueOf(this.zziqu), Integer.valueOf(this.zziqv), this.zziqs, this.extras});
    }

    public String toString() {
        String strValueOf = String.valueOf(this.zziqs);
        long j = this.zziqt;
        long j2 = this.zziqu;
        StringBuilder sb = new StringBuilder(String.valueOf(strValueOf).length() + 124);
        sb.append("ActivityRecognitionResult [probableActivities=");
        sb.append(strValueOf);
        sb.append(", timeMillis=");
        sb.append(j);
        sb.append(", elapsedRealtimeMillis=");
        sb.append(j2);
        sb.append("]");
        return sb.toString();
    }

    @Override // android.os.Parcelable
    public void writeToParcel(Parcel parcel, int i) {
        int iZze = zzbgo.zze(parcel);
        zzbgo.zzc(parcel, 1, this.zziqs, false);
        zzbgo.zza(parcel, 2, this.zziqt);
        zzbgo.zza(parcel, 3, this.zziqu);
        zzbgo.zzc(parcel, 4, this.zziqv);
        zzbgo.zza(parcel, 5, this.extras, false);
        zzbgo.zzai(parcel, iZze);
    }
}
