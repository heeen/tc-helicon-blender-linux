package com.google.android.gms.internal;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/* JADX INFO: loaded from: classes.dex */
final class zzeeg<A, B, C> {
    private final Map<B, C> values;
    private final List<A> zzmyy;
    private final zzedt<A, B> zzmyz;
    private zzeed<A, C> zzmza;
    private zzeed<A, C> zzmzb;

    private zzeeg(List<A> list, Map<B, C> map, zzedt<A, B> zzedtVar) {
        this.zzmyy = list;
        this.values = map;
        this.zzmyz = zzedtVar;
    }

    private final C zzbt(A a) {
        return this.values.get(this.zzmyz.zzbo(a));
    }

    public static <A, B, C> zzeee<A, C> zzc(List<A> list, Map<B, C> map, zzedt<A, B> zzedtVar, Comparator<A> comparator) {
        int i;
        zzeeg zzeegVar = new zzeeg(list, map, zzedtVar);
        Collections.sort(list, comparator);
        int size = list.size();
        for (zzeej zzeejVar : new zzeeh(list.size())) {
            size -= zzeejVar.zzmzf;
            if (zzeejVar.zzmze) {
                i = zzeea.zzmyt;
            } else {
                zzeegVar.zzf(zzeea.zzmyt, zzeejVar.zzmzf, size);
                size -= zzeejVar.zzmzf;
                i = zzeea.zzmys;
            }
            zzeegVar.zzf(i, zzeejVar.zzmzf, size);
        }
        return new zzeee<>(zzeegVar.zzmza == null ? zzedy.zzbvx() : zzeegVar.zzmza, comparator);
    }

    private final void zzf(int i, int i2, int i3) {
        zzedz<A, C> zzedzVarZzx = zzx(i3 + 1, i2 - 1);
        A a = this.zzmyy.get(i3);
        zzeed<A, C> zzeecVar = i == zzeea.zzmys ? new zzeec<>(a, zzbt(a), null, zzedzVarZzx) : new zzedx<>(a, zzbt(a), null, zzedzVarZzx);
        if (this.zzmza == null) {
            this.zzmza = zzeecVar;
        } else {
            this.zzmzb.zza(zzeecVar);
        }
        this.zzmzb = zzeecVar;
    }

    private final zzedz<A, C> zzx(int i, int i2) {
        if (i2 == 0) {
            return zzedy.zzbvx();
        }
        if (i2 == 1) {
            A a = this.zzmyy.get(i);
            return new zzedx(a, zzbt(a), null, null);
        }
        int i3 = i2 / 2;
        int i4 = i + i3;
        zzedz<A, C> zzedzVarZzx = zzx(i, i3);
        zzedz<A, C> zzedzVarZzx2 = zzx(i4 + 1, i3);
        A a2 = this.zzmyy.get(i4);
        return new zzedx(a2, zzbt(a2), zzedzVarZzx, zzedzVarZzx2);
    }
}
