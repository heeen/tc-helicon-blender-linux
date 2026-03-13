package com.google.android.gms.location;

import android.app.PendingIntent;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.Nullable;
import com.google.android.gms.common.internal.Hide;
import com.google.android.gms.common.internal.zzbq;
import com.google.android.gms.internal.zzbgl;
import com.google.android.gms.internal.zzbgo;
import java.util.Collections;
import java.util.List;

/* JADX INFO: loaded from: classes.dex */
@Hide
public final class zzal extends zzbgl {
    public static final Parcelable.Creator<zzal> CREATOR = new zzam();

    @Hide
    private final String mTag;
    private final PendingIntent zzekd;
    private final List<String> zzitf;

    @Hide
    zzal(@Nullable List<String> list, @Nullable PendingIntent pendingIntent, String str) {
        this.zzitf = list == null ? Collections.emptyList() : Collections.unmodifiableList(list);
        this.zzekd = pendingIntent;
        this.mTag = str;
    }

    public static zzal zzad(List<String> list) {
        zzbq.checkNotNull(list, "geofence can't be null.");
        zzbq.checkArgument(!list.isEmpty(), "Geofences must contains at least one id.");
        return new zzal(list, null, "");
    }

    public static zzal zzb(PendingIntent pendingIntent) {
        zzbq.checkNotNull(pendingIntent, "PendingIntent can not be null.");
        return new zzal(null, pendingIntent, "");
    }

    @Override // android.os.Parcelable
    public final void writeToParcel(Parcel parcel, int i) {
        int iZze = zzbgo.zze(parcel);
        zzbgo.zzb(parcel, 1, this.zzitf, false);
        zzbgo.zza(parcel, 2, (Parcelable) this.zzekd, i, false);
        zzbgo.zza(parcel, 3, this.mTag, false);
        zzbgo.zzai(parcel, iZze);
    }
}
