package com.google.android.gms.location;

import android.os.Parcel;
import android.os.Parcelable;
import com.google.android.gms.common.internal.Hide;
import com.google.android.gms.common.internal.zzbq;
import com.google.android.gms.internal.zzbgl;
import com.google.android.gms.internal.zzbgo;
import com.google.android.gms.internal.zzchp;
import java.util.ArrayList;
import java.util.List;

/* JADX INFO: loaded from: classes.dex */
public class GeofencingRequest extends zzbgl {
    public static final Parcelable.Creator<GeofencingRequest> CREATOR = new zzq();
    public static final int INITIAL_TRIGGER_DWELL = 4;
    public static final int INITIAL_TRIGGER_ENTER = 1;
    public static final int INITIAL_TRIGGER_EXIT = 2;

    @Hide
    private final String mTag;
    private final List<zzchp> zzirz;
    private final int zzisa;

    public static final class Builder {
        private final List<zzchp> zzirz = new ArrayList();
        private int zzisa = 5;
        private String mTag = "";

        public final Builder addGeofence(Geofence geofence) {
            zzbq.checkNotNull(geofence, "geofence can't be null.");
            zzbq.checkArgument(geofence instanceof zzchp, "Geofence must be created using Geofence.Builder.");
            this.zzirz.add((zzchp) geofence);
            return this;
        }

        public final Builder addGeofences(List<Geofence> list) {
            if (list != null && !list.isEmpty()) {
                for (Geofence geofence : list) {
                    if (geofence != null) {
                        addGeofence(geofence);
                    }
                }
            }
            return this;
        }

        public final GeofencingRequest build() {
            zzbq.checkArgument(!this.zzirz.isEmpty(), "No geofence has been added to this request.");
            return new GeofencingRequest(this.zzirz, this.zzisa, this.mTag);
        }

        public final Builder setInitialTrigger(int i) {
            this.zzisa = i & 7;
            return this;
        }
    }

    GeofencingRequest(List<zzchp> list, int i, String str) {
        this.zzirz = list;
        this.zzisa = i;
        this.mTag = str;
    }

    public List<Geofence> getGeofences() {
        ArrayList arrayList = new ArrayList();
        arrayList.addAll(this.zzirz);
        return arrayList;
    }

    public int getInitialTrigger() {
        return this.zzisa;
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("GeofencingRequest[");
        sb.append("geofences=");
        sb.append(this.zzirz);
        int i = this.zzisa;
        StringBuilder sb2 = new StringBuilder(30);
        sb2.append(", initialTrigger=");
        sb2.append(i);
        sb2.append(", ");
        sb.append(sb2.toString());
        String strValueOf = String.valueOf(this.mTag);
        sb.append(strValueOf.length() != 0 ? "tag=".concat(strValueOf) : new String("tag="));
        sb.append("]");
        return sb.toString();
    }

    @Override // android.os.Parcelable
    public void writeToParcel(Parcel parcel, int i) {
        int iZze = zzbgo.zze(parcel);
        zzbgo.zzc(parcel, 1, this.zzirz, false);
        zzbgo.zzc(parcel, 2, getInitialTrigger());
        zzbgo.zza(parcel, 3, this.mTag, false);
        zzbgo.zzai(parcel, iZze);
    }
}
