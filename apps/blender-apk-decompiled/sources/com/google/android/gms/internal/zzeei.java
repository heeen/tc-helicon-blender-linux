package com.google.android.gms.internal;

import java.util.Iterator;

/* JADX INFO: loaded from: classes.dex */
final class zzeei implements Iterator<zzeej> {
    private int zzmzc;
    private /* synthetic */ zzeeh zzmzd;

    zzeei(zzeeh zzeehVar) {
        this.zzmzd = zzeehVar;
        this.zzmzc = this.zzmzd.length - 1;
    }

    @Override // java.util.Iterator
    public final boolean hasNext() {
        return this.zzmzc >= 0;
    }

    @Override // java.util.Iterator
    public final /* synthetic */ zzeej next() {
        long j = this.zzmzd.value & ((long) (1 << this.zzmzc));
        zzeej zzeejVar = new zzeej();
        zzeejVar.zzmze = j == 0;
        zzeejVar.zzmzf = (int) Math.pow(2.0d, this.zzmzc);
        this.zzmzc--;
        return zzeejVar;
    }

    @Override // java.util.Iterator
    public final void remove() {
    }
}
