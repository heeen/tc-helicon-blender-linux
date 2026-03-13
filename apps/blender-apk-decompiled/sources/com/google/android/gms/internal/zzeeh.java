package com.google.android.gms.internal;

import java.util.Iterator;

/* JADX INFO: loaded from: classes.dex */
final class zzeeh implements Iterable<zzeej> {
    private final int length;
    private long value;

    public zzeeh(int i) {
        int i2 = i + 1;
        this.length = (int) Math.floor(Math.log(i2) / Math.log(2.0d));
        this.value = (((long) Math.pow(2.0d, this.length)) - 1) & ((long) i2);
    }

    @Override // java.lang.Iterable
    public final Iterator<zzeej> iterator() {
        return new zzeei(this);
    }
}
