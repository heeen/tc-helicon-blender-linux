package com.google.android.gms.location.places;

import android.os.Parcel;
import android.os.Parcelable;
import android.support.v4.os.EnvironmentCompat;
import com.google.android.gms.common.internal.Hide;
import com.google.android.gms.common.internal.ReflectedParcelable;
import com.google.android.gms.common.internal.zzbg;
import com.google.android.gms.common.internal.zzbi;
import com.google.android.gms.internal.zzbgl;
import com.google.android.gms.internal.zzbgo;
import java.util.Arrays;

/* JADX INFO: loaded from: classes.dex */
public class PlaceReport extends zzbgl implements ReflectedParcelable {
    public static final Parcelable.Creator<PlaceReport> CREATOR = new zzl();
    private final String mTag;
    private final String zzdwr;
    private int zzehz;
    private final String zzivz;

    @Hide
    PlaceReport(int i, String str, String str2, String str3) {
        this.zzehz = i;
        this.zzivz = str;
        this.mTag = str2;
        this.zzdwr = str3;
    }

    /* JADX WARN: Can't fix incorrect switch cases order, some code will duplicate */
    /* JADX WARN: Removed duplicated region for block: B:23:0x0051  */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
        To view partially-correct add '--show-bad-code' argument
    */
    public static com.google.android.gms.location.places.PlaceReport create(java.lang.String r4, java.lang.String r5) {
        /*
            java.lang.String r0 = "unknown"
            com.google.android.gms.common.internal.zzbq.checkNotNull(r4)
            com.google.android.gms.common.internal.zzbq.zzgv(r5)
            com.google.android.gms.common.internal.zzbq.zzgv(r0)
            int r1 = r0.hashCode()
            r2 = 0
            r3 = 1
            switch(r1) {
                case -1436706272: goto L47;
                case -1194968642: goto L3d;
                case -284840886: goto L33;
                case -262743844: goto L29;
                case 1164924125: goto L1f;
                case 1287171955: goto L15;
                default: goto L14;
            }
        L14:
            goto L51
        L15:
            java.lang.String r1 = "inferredRadioSignals"
            boolean r1 = r0.equals(r1)
            if (r1 == 0) goto L51
            r1 = 3
            goto L52
        L1f:
            java.lang.String r1 = "inferredSnappedToRoad"
            boolean r1 = r0.equals(r1)
            if (r1 == 0) goto L51
            r1 = 5
            goto L52
        L29:
            java.lang.String r1 = "inferredReverseGeocoding"
            boolean r1 = r0.equals(r1)
            if (r1 == 0) goto L51
            r1 = 4
            goto L52
        L33:
            java.lang.String r1 = "unknown"
            boolean r1 = r0.equals(r1)
            if (r1 == 0) goto L51
            r1 = r2
            goto L52
        L3d:
            java.lang.String r1 = "userReported"
            boolean r1 = r0.equals(r1)
            if (r1 == 0) goto L51
            r1 = r3
            goto L52
        L47:
            java.lang.String r1 = "inferredGeofencing"
            boolean r1 = r0.equals(r1)
            if (r1 == 0) goto L51
            r1 = 2
            goto L52
        L51:
            r1 = -1
        L52:
            switch(r1) {
                case 0: goto L56;
                case 1: goto L56;
                case 2: goto L56;
                case 3: goto L56;
                case 4: goto L56;
                case 5: goto L56;
                default: goto L55;
            }
        L55:
            goto L57
        L56:
            r2 = r3
        L57:
            java.lang.String r1 = "Invalid source"
            com.google.android.gms.common.internal.zzbq.checkArgument(r2, r1)
            com.google.android.gms.location.places.PlaceReport r1 = new com.google.android.gms.location.places.PlaceReport
            r1.<init>(r3, r4, r5, r0)
            return r1
        */
        throw new UnsupportedOperationException("Method not decompiled: com.google.android.gms.location.places.PlaceReport.create(java.lang.String, java.lang.String):com.google.android.gms.location.places.PlaceReport");
    }

    @Hide
    public boolean equals(Object obj) {
        if (!(obj instanceof PlaceReport)) {
            return false;
        }
        PlaceReport placeReport = (PlaceReport) obj;
        return zzbg.equal(this.zzivz, placeReport.zzivz) && zzbg.equal(this.mTag, placeReport.mTag) && zzbg.equal(this.zzdwr, placeReport.zzdwr);
    }

    public String getPlaceId() {
        return this.zzivz;
    }

    public String getTag() {
        return this.mTag;
    }

    @Hide
    public int hashCode() {
        return Arrays.hashCode(new Object[]{this.zzivz, this.mTag, this.zzdwr});
    }

    @Hide
    public String toString() {
        zzbi zzbiVarZzx = zzbg.zzx(this);
        zzbiVarZzx.zzg("placeId", this.zzivz);
        zzbiVarZzx.zzg("tag", this.mTag);
        if (!EnvironmentCompat.MEDIA_UNKNOWN.equals(this.zzdwr)) {
            zzbiVarZzx.zzg("source", this.zzdwr);
        }
        return zzbiVarZzx.toString();
    }

    @Override // android.os.Parcelable
    @Hide
    public void writeToParcel(Parcel parcel, int i) {
        int iZze = zzbgo.zze(parcel);
        zzbgo.zzc(parcel, 1, this.zzehz);
        zzbgo.zza(parcel, 2, getPlaceId(), false);
        zzbgo.zza(parcel, 3, getTag(), false);
        zzbgo.zza(parcel, 4, this.zzdwr, false);
        zzbgo.zzai(parcel, iZze);
    }
}
