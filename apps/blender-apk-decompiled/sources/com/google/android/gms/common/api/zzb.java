package com.google.android.gms.common.api;

import android.os.Bundle;
import com.google.android.gms.common.api.Result;
import com.google.android.gms.common.data.AbstractDataBuffer;
import com.google.android.gms.common.data.DataBuffer;
import com.google.android.gms.common.internal.Hide;
import java.util.Iterator;

/* JADX INFO: loaded from: classes.dex */
@Hide
public class zzb<T, R extends AbstractDataBuffer<T> & Result> extends Response<R> implements DataBuffer<T> {
    @Hide
    public zzb() {
    }

    @Override // com.google.android.gms.common.data.DataBuffer
    public void close() {
        ((AbstractDataBuffer) getResult()).close();
    }

    @Override // com.google.android.gms.common.data.DataBuffer
    public T get(int i) {
        return (T) ((AbstractDataBuffer) getResult()).get(i);
    }

    @Override // com.google.android.gms.common.data.DataBuffer
    public int getCount() {
        return ((AbstractDataBuffer) getResult()).getCount();
    }

    @Override // com.google.android.gms.common.data.DataBuffer
    public boolean isClosed() {
        return ((AbstractDataBuffer) getResult()).isClosed();
    }

    @Override // com.google.android.gms.common.data.DataBuffer, java.lang.Iterable
    public Iterator<T> iterator() {
        return ((AbstractDataBuffer) getResult()).iterator();
    }

    @Override // com.google.android.gms.common.data.DataBuffer, com.google.android.gms.common.api.Releasable
    public void release() {
        ((AbstractDataBuffer) getResult()).release();
    }

    @Override // com.google.android.gms.common.data.DataBuffer
    public Iterator<T> singleRefIterator() {
        return ((AbstractDataBuffer) getResult()).singleRefIterator();
    }

    @Override // com.google.android.gms.common.data.DataBuffer
    public final Bundle zzahs() {
        return ((AbstractDataBuffer) getResult()).zzahs();
    }
}
