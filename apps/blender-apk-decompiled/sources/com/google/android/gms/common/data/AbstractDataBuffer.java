package com.google.android.gms.common.data;

import android.os.Bundle;
import com.google.android.gms.common.internal.Hide;
import java.util.Iterator;

/* JADX INFO: loaded from: classes.dex */
public abstract class AbstractDataBuffer<T> implements DataBuffer<T> {

    @Hide
    protected final DataHolder zzfxb;

    @Hide
    protected AbstractDataBuffer(DataHolder dataHolder) {
        this.zzfxb = dataHolder;
    }

    @Override // com.google.android.gms.common.data.DataBuffer
    @Deprecated
    public final void close() {
        release();
    }

    @Override // com.google.android.gms.common.data.DataBuffer
    public abstract T get(int i);

    @Override // com.google.android.gms.common.data.DataBuffer
    public int getCount() {
        if (this.zzfxb == null) {
            return 0;
        }
        return this.zzfxb.zzgcq;
    }

    @Override // com.google.android.gms.common.data.DataBuffer
    @Deprecated
    public boolean isClosed() {
        return this.zzfxb == null || this.zzfxb.isClosed();
    }

    @Override // com.google.android.gms.common.data.DataBuffer, java.lang.Iterable
    public Iterator<T> iterator() {
        return new zzb(this);
    }

    @Override // com.google.android.gms.common.data.DataBuffer, com.google.android.gms.common.api.Releasable
    public void release() {
        if (this.zzfxb != null) {
            this.zzfxb.close();
        }
    }

    @Override // com.google.android.gms.common.data.DataBuffer
    public Iterator<T> singleRefIterator() {
        return new zzh(this);
    }

    @Override // com.google.android.gms.common.data.DataBuffer
    @Hide
    public final Bundle zzahs() {
        return this.zzfxb.zzahs();
    }
}
