package com.google.android.gms.internal;

import java.util.ArrayList;
import java.util.List;

/* JADX INFO: loaded from: classes.dex */
final class zzfjo<E> extends zzfgn<E> {
    private static final zzfjo<Object> zzprp;
    private final List<E> zzprq;

    static {
        zzfjo<Object> zzfjoVar = new zzfjo<>();
        zzprp = zzfjoVar;
        zzfjoVar.zzbkr();
    }

    zzfjo() {
        this(new ArrayList(10));
    }

    private zzfjo(List<E> list) {
        this.zzprq = list;
    }

    public static <E> zzfjo<E> zzdbg() {
        return (zzfjo<E>) zzprp;
    }

    @Override // com.google.android.gms.internal.zzfgn, java.util.AbstractList, java.util.List
    public final void add(int i, E e) {
        zzcxl();
        this.zzprq.add(i, e);
        this.modCount++;
    }

    @Override // java.util.AbstractList, java.util.List
    public final E get(int i) {
        return this.zzprq.get(i);
    }

    @Override // com.google.android.gms.internal.zzfgn, java.util.AbstractList, java.util.List
    public final E remove(int i) {
        zzcxl();
        E eRemove = this.zzprq.remove(i);
        this.modCount++;
        return eRemove;
    }

    @Override // com.google.android.gms.internal.zzfgn, java.util.AbstractList, java.util.List
    public final E set(int i, E e) {
        zzcxl();
        E e2 = this.zzprq.set(i, e);
        this.modCount++;
        return e2;
    }

    @Override // java.util.AbstractCollection, java.util.Collection, java.util.List
    public final int size() {
        return this.zzprq.size();
    }

    @Override // com.google.android.gms.internal.zzfid
    public final /* synthetic */ zzfid zzmo(int i) {
        if (i < size()) {
            throw new IllegalArgumentException();
        }
        ArrayList arrayList = new ArrayList(i);
        arrayList.addAll(this.zzprq);
        return new zzfjo(arrayList);
    }
}
