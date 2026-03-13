package com.google.android.gms.internal;

import android.os.Parcel;
import android.os.Parcelable;
import com.google.android.gms.common.internal.Hide;
import com.google.android.gms.location.Geofence;
import java.util.Locale;

/* JADX INFO: loaded from: classes.dex */
@Hide
public final class zzchp extends zzbgl implements Geofence {
    public static final Parcelable.Creator<zzchp> CREATOR = new zzchq();
    private final String zzcwj;
    private final int zziro;
    private final short zzirq;
    private final double zzirr;
    private final double zzirs;
    private final float zzirt;
    private final int zziru;
    private final int zzirv;
    private final long zziuu;

    public zzchp(String str, int i, short s, double d, double d2, float f, long j, int i2, int i3) {
        if (str == null || str.length() > 100) {
            String strValueOf = String.valueOf(str);
            throw new IllegalArgumentException(strValueOf.length() != 0 ? "requestId is null or too long: ".concat(strValueOf) : new String("requestId is null or too long: "));
        }
        if (f <= 0.0f) {
            StringBuilder sb = new StringBuilder(31);
            sb.append("invalid radius: ");
            sb.append(f);
            throw new IllegalArgumentException(sb.toString());
        }
        if (d > 90.0d || d < -90.0d) {
            StringBuilder sb2 = new StringBuilder(42);
            sb2.append("invalid latitude: ");
            sb2.append(d);
            throw new IllegalArgumentException(sb2.toString());
        }
        if (d2 > 180.0d || d2 < -180.0d) {
            StringBuilder sb3 = new StringBuilder(43);
            sb3.append("invalid longitude: ");
            sb3.append(d2);
            throw new IllegalArgumentException(sb3.toString());
        }
        int i4 = i & 7;
        if (i4 == 0) {
            StringBuilder sb4 = new StringBuilder(46);
            sb4.append("No supported transition specified: ");
            sb4.append(i);
            throw new IllegalArgumentException(sb4.toString());
        }
        this.zzirq = s;
        this.zzcwj = str;
        this.zzirr = d;
        this.zzirs = d2;
        this.zzirt = f;
        this.zziuu = j;
        this.zziro = i4;
        this.zziru = i2;
        this.zzirv = i3;
    }

    public static zzchp zzq(byte[] bArr) {
        Parcel parcelObtain = Parcel.obtain();
        parcelObtain.unmarshall(bArr, 0, bArr.length);
        parcelObtain.setDataPosition(0);
        zzchp zzchpVarCreateFromParcel = CREATOR.createFromParcel(parcelObtain);
        parcelObtain.recycle();
        return zzchpVarCreateFromParcel;
    }

    @Hide
    public final boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || !(obj instanceof zzchp)) {
            return false;
        }
        zzchp zzchpVar = (zzchp) obj;
        return this.zzirt == zzchpVar.zzirt && this.zzirr == zzchpVar.zzirr && this.zzirs == zzchpVar.zzirs && this.zzirq == zzchpVar.zzirq;
    }

    @Override // com.google.android.gms.location.Geofence
    public final String getRequestId() {
        return this.zzcwj;
    }

    @Hide
    public final int hashCode() {
        long jDoubleToLongBits = Double.doubleToLongBits(this.zzirr);
        int i = ((int) (jDoubleToLongBits ^ (jDoubleToLongBits >>> 32))) + 31;
        long jDoubleToLongBits2 = Double.doubleToLongBits(this.zzirs);
        return (((((((i * 31) + ((int) ((jDoubleToLongBits2 >>> 32) ^ jDoubleToLongBits2))) * 31) + Float.floatToIntBits(this.zzirt)) * 31) + this.zzirq) * 31) + this.zziro;
    }

    @Hide
    public final String toString() {
        Locale locale = Locale.US;
        Object[] objArr = new Object[9];
        objArr[0] = this.zzirq != 1 ? null : "CIRCLE";
        objArr[1] = this.zzcwj.replaceAll("\\p{C}", "?");
        objArr[2] = Integer.valueOf(this.zziro);
        objArr[3] = Double.valueOf(this.zzirr);
        objArr[4] = Double.valueOf(this.zzirs);
        objArr[5] = Float.valueOf(this.zzirt);
        objArr[6] = Integer.valueOf(this.zziru / 1000);
        objArr[7] = Integer.valueOf(this.zzirv);
        objArr[8] = Long.valueOf(this.zziuu);
        return String.format(locale, "Geofence[%s id:%s transitions:%d %.6f, %.6f %.0fm, resp=%ds, dwell=%dms, @%d]", objArr);
    }

    @Override // android.os.Parcelable
    public final void writeToParcel(Parcel parcel, int i) {
        int iZze = zzbgo.zze(parcel);
        zzbgo.zza(parcel, 1, getRequestId(), false);
        zzbgo.zza(parcel, 2, this.zziuu);
        zzbgo.zza(parcel, 3, this.zzirq);
        zzbgo.zza(parcel, 4, this.zzirr);
        zzbgo.zza(parcel, 5, this.zzirs);
        zzbgo.zza(parcel, 6, this.zzirt);
        zzbgo.zzc(parcel, 7, this.zziro);
        zzbgo.zzc(parcel, 8, this.zziru);
        zzbgo.zzc(parcel, 9, this.zzirv);
        zzbgo.zzai(parcel, iZze);
    }
}
