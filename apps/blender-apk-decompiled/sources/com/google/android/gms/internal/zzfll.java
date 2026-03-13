package com.google.android.gms.internal;

import java.io.IOException;

/* JADX INFO: loaded from: classes.dex */
public final class zzfll extends IOException {
    zzfll(int i, int i2) {
        StringBuilder sb = new StringBuilder(108);
        sb.append("CodedOutputStream was writing to a flat byte array and ran out of space (pos ");
        sb.append(i);
        sb.append(" limit ");
        sb.append(i2);
        sb.append(").");
        super(sb.toString());
    }
}
