package com.google.android.gms.location;

import android.content.Intent;
import android.location.Location;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.NonNull;
import com.google.android.gms.common.internal.Hide;
import com.google.android.gms.common.internal.ReflectedParcelable;
import com.google.android.gms.internal.zzbgl;
import com.google.android.gms.internal.zzbgo;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

/* JADX INFO: loaded from: classes.dex */
public final class LocationResult extends zzbgl implements ReflectedParcelable {
    private final List<Location> zzism;
    static final List<Location> zzisl = Collections.emptyList();
    public static final Parcelable.Creator<LocationResult> CREATOR = new zzac();

    @Hide
    LocationResult(List<Location> list) {
        this.zzism = list;
    }

    public static LocationResult create(List<Location> list) {
        if (list == null) {
            list = zzisl;
        }
        return new LocationResult(list);
    }

    public static LocationResult extractResult(Intent intent) {
        if (hasResult(intent)) {
            return (LocationResult) intent.getExtras().getParcelable("com.google.android.gms.location.EXTRA_LOCATION_RESULT");
        }
        return null;
    }

    public static boolean hasResult(Intent intent) {
        if (intent == null) {
            return false;
        }
        return intent.hasExtra("com.google.android.gms.location.EXTRA_LOCATION_RESULT");
    }

    public final boolean equals(Object obj) {
        if (!(obj instanceof LocationResult)) {
            return false;
        }
        LocationResult locationResult = (LocationResult) obj;
        if (locationResult.zzism.size() != this.zzism.size()) {
            return false;
        }
        Iterator<Location> it = locationResult.zzism.iterator();
        Iterator<Location> it2 = this.zzism.iterator();
        while (it.hasNext()) {
            if (it2.next().getTime() != it.next().getTime()) {
                return false;
            }
        }
        return true;
    }

    public final Location getLastLocation() {
        int size = this.zzism.size();
        if (size == 0) {
            return null;
        }
        return this.zzism.get(size - 1);
    }

    @NonNull
    public final List<Location> getLocations() {
        return this.zzism;
    }

    public final int hashCode() {
        Iterator<Location> it = this.zzism.iterator();
        int i = 17;
        while (it.hasNext()) {
            long time = it.next().getTime();
            i = (i * 31) + ((int) (time ^ (time >>> 32)));
        }
        return i;
    }

    public final String toString() {
        String strValueOf = String.valueOf(this.zzism);
        StringBuilder sb = new StringBuilder(String.valueOf(strValueOf).length() + 27);
        sb.append("LocationResult[locations: ");
        sb.append(strValueOf);
        sb.append("]");
        return sb.toString();
    }

    @Override // android.os.Parcelable
    public final void writeToParcel(Parcel parcel, int i) {
        int iZze = zzbgo.zze(parcel);
        zzbgo.zzc(parcel, 1, getLocations(), false);
        zzbgo.zzai(parcel, iZze);
    }
}
