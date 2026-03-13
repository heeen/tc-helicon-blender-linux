package com.google.android.gms.internal;

import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Stack;

/* JADX INFO: loaded from: classes.dex */
final class zzfjt implements Iterator<zzfgy> {
    private final Stack<zzfjq> zzpry;
    private zzfgy zzprz;

    private zzfjt(zzfgs zzfgsVar) {
        this.zzpry = new Stack<>();
        this.zzprz = zzbc(zzfgsVar);
    }

    private final zzfgy zzbc(zzfgs zzfgsVar) {
        while (zzfgsVar instanceof zzfjq) {
            zzfjq zzfjqVar = (zzfjq) zzfgsVar;
            this.zzpry.push(zzfjqVar);
            zzfgsVar = zzfjqVar.zzprt;
        }
        return (zzfgy) zzfgsVar;
    }

    private final zzfgy zzdbi() {
        while (!this.zzpry.isEmpty()) {
            zzfgy zzfgyVarZzbc = zzbc(this.zzpry.pop().zzpru);
            if (!zzfgyVarZzbc.isEmpty()) {
                return zzfgyVarZzbc;
            }
        }
        return null;
    }

    @Override // java.util.Iterator
    public final boolean hasNext() {
        return this.zzprz != null;
    }

    @Override // java.util.Iterator
    public final /* synthetic */ zzfgy next() {
        if (this.zzprz == null) {
            throw new NoSuchElementException();
        }
        zzfgy zzfgyVar = this.zzprz;
        this.zzprz = zzdbi();
        return zzfgyVar;
    }

    @Override // java.util.Iterator
    public final void remove() {
        throw new UnsupportedOperationException();
    }
}
