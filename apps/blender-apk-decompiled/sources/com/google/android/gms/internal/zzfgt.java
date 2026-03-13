package com.google.android.gms.internal;

import java.util.Iterator;
import java.util.NoSuchElementException;

/* JADX INFO: loaded from: classes.dex */
final class zzfgt implements Iterator {
    private final int limit;
    private int position = 0;
    private /* synthetic */ zzfgs zzpny;

    zzfgt(zzfgs zzfgsVar) {
        this.zzpny = zzfgsVar;
        this.limit = this.zzpny.size();
    }

    private final byte nextByte() {
        try {
            zzfgs zzfgsVar = this.zzpny;
            int i = this.position;
            this.position = i + 1;
            return zzfgsVar.zzld(i);
        } catch (IndexOutOfBoundsException e) {
            throw new NoSuchElementException(e.getMessage());
        }
    }

    @Override // java.util.Iterator
    public final boolean hasNext() {
        return this.position < this.limit;
    }

    @Override // java.util.Iterator
    public final /* synthetic */ Object next() {
        return Byte.valueOf(nextByte());
    }

    @Override // java.util.Iterator
    public final void remove() {
        throw new UnsupportedOperationException();
    }
}
