package com.google.android.gms.internal;

import android.os.Parcel;

/* JADX INFO: loaded from: classes.dex */
public final class zzbgn extends RuntimeException {
    public zzbgn(String str, Parcel parcel) {
        int iDataPosition = parcel.dataPosition();
        int iDataSize = parcel.dataSize();
        StringBuilder sb = new StringBuilder(String.valueOf(str).length() + 41);
        sb.append(str);
        sb.append(" Parcel: pos=");
        sb.append(iDataPosition);
        sb.append(" size=");
        sb.append(iDataSize);
        super(sb.toString());
    }
}
