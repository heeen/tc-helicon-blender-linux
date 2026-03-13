package com.google.android.gms.common.internal;

import android.os.IBinder;
import android.os.Parcel;
import android.os.Parcelable;
import com.google.android.gms.common.annotation.KeepName;

/* JADX INFO: loaded from: classes.dex */
@KeepName
public final class BinderWrapper implements Parcelable {
    public static final Parcelable.Creator<BinderWrapper> CREATOR = new zzq();
    private IBinder zzgfp;

    public BinderWrapper() {
        this.zzgfp = null;
    }

    public BinderWrapper(IBinder iBinder) {
        this.zzgfp = null;
        this.zzgfp = iBinder;
    }

    private BinderWrapper(Parcel parcel) {
        this.zzgfp = null;
        this.zzgfp = parcel.readStrongBinder();
    }

    /* synthetic */ BinderWrapper(Parcel parcel, zzq zzqVar) {
        this(parcel);
    }

    @Override // android.os.Parcelable
    public final int describeContents() {
        return 0;
    }

    @Override // android.os.Parcelable
    public final void writeToParcel(Parcel parcel, int i) {
        parcel.writeStrongBinder(this.zzgfp);
    }
}
