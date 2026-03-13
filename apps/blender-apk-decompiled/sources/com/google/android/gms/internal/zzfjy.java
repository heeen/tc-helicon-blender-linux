package com.google.android.gms.internal;

import java.lang.Comparable;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

/* JADX INFO: loaded from: classes.dex */
class zzfjy<K extends Comparable<K>, V> extends AbstractMap<K, V> {
    private boolean zzldh;
    private final int zzpsk;
    private List<zzfkd> zzpsl;
    private Map<K, V> zzpsm;
    private volatile zzfkf zzpsn;
    private Map<K, V> zzpso;

    private zzfjy(int i) {
        this.zzpsk = i;
        this.zzpsl = Collections.emptyList();
        this.zzpsm = Collections.emptyMap();
        this.zzpso = Collections.emptyMap();
    }

    /* synthetic */ zzfjy(int i, zzfjz zzfjzVar) {
        this(i);
    }

    private final int zza(K k) {
        int size = this.zzpsl.size() - 1;
        if (size >= 0) {
            int iCompareTo = k.compareTo((Comparable) this.zzpsl.get(size).getKey());
            if (iCompareTo > 0) {
                return -(size + 2);
            }
            if (iCompareTo == 0) {
                return size;
            }
        }
        int i = 0;
        while (i <= size) {
            int i2 = (i + size) / 2;
            int iCompareTo2 = k.compareTo((Comparable) this.zzpsl.get(i2).getKey());
            if (iCompareTo2 < 0) {
                size = i2 - 1;
            } else {
                if (iCompareTo2 <= 0) {
                    return i2;
                }
                i = i2 + 1;
            }
        }
        return -(i + 1);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public final void zzdbr() {
        if (this.zzldh) {
            throw new UnsupportedOperationException();
        }
    }

    private final SortedMap<K, V> zzdbs() {
        zzdbr();
        if (this.zzpsm.isEmpty() && !(this.zzpsm instanceof TreeMap)) {
            this.zzpsm = new TreeMap();
            this.zzpso = ((TreeMap) this.zzpsm).descendingMap();
        }
        return (SortedMap) this.zzpsm;
    }

    static <FieldDescriptorType extends zzfhs<FieldDescriptorType>> zzfjy<FieldDescriptorType, Object> zzmq(int i) {
        return new zzfjz(i);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public final V zzms(int i) {
        zzdbr();
        V v = (V) this.zzpsl.remove(i).getValue();
        if (!this.zzpsm.isEmpty()) {
            Iterator<Map.Entry<K, V>> it = zzdbs().entrySet().iterator();
            this.zzpsl.add(new zzfkd(this, it.next()));
            it.remove();
        }
        return v;
    }

    @Override // java.util.AbstractMap, java.util.Map
    public void clear() {
        zzdbr();
        if (!this.zzpsl.isEmpty()) {
            this.zzpsl.clear();
        }
        if (this.zzpsm.isEmpty()) {
            return;
        }
        this.zzpsm.clear();
    }

    @Override // java.util.AbstractMap, java.util.Map
    public boolean containsKey(Object obj) {
        Comparable comparable = (Comparable) obj;
        return zza(comparable) >= 0 || this.zzpsm.containsKey(comparable);
    }

    @Override // java.util.AbstractMap, java.util.Map
    public Set<Map.Entry<K, V>> entrySet() {
        if (this.zzpsn == null) {
            this.zzpsn = new zzfkf(this, null);
        }
        return this.zzpsn;
    }

    @Override // java.util.AbstractMap, java.util.Map
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof zzfjy)) {
            return super.equals(obj);
        }
        zzfjy zzfjyVar = (zzfjy) obj;
        int size = size();
        if (size != zzfjyVar.size()) {
            return false;
        }
        int iZzdbp = zzdbp();
        if (iZzdbp != zzfjyVar.zzdbp()) {
            return entrySet().equals(zzfjyVar.entrySet());
        }
        for (int i = 0; i < iZzdbp; i++) {
            if (!zzmr(i).equals(zzfjyVar.zzmr(i))) {
                return false;
            }
        }
        if (iZzdbp != size) {
            return this.zzpsm.equals(zzfjyVar.zzpsm);
        }
        return true;
    }

    @Override // java.util.AbstractMap, java.util.Map
    public V get(Object obj) {
        Comparable comparable = (Comparable) obj;
        int iZza = zza(comparable);
        return iZza >= 0 ? (V) this.zzpsl.get(iZza).getValue() : this.zzpsm.get(comparable);
    }

    @Override // java.util.AbstractMap, java.util.Map
    public int hashCode() {
        int iZzdbp = zzdbp();
        int iHashCode = 0;
        for (int i = 0; i < iZzdbp; i++) {
            iHashCode += this.zzpsl.get(i).hashCode();
        }
        return this.zzpsm.size() > 0 ? iHashCode + this.zzpsm.hashCode() : iHashCode;
    }

    public final boolean isImmutable() {
        return this.zzldh;
    }

    @Override // java.util.AbstractMap, java.util.Map
    public V remove(Object obj) {
        zzdbr();
        Comparable comparable = (Comparable) obj;
        int iZza = zza(comparable);
        if (iZza >= 0) {
            return zzms(iZza);
        }
        if (this.zzpsm.isEmpty()) {
            return null;
        }
        return this.zzpsm.remove(comparable);
    }

    @Override // java.util.AbstractMap, java.util.Map
    public int size() {
        return this.zzpsl.size() + this.zzpsm.size();
    }

    /* JADX WARN: Multi-variable type inference failed */
    @Override // java.util.AbstractMap, java.util.Map
    /* JADX INFO: renamed from: zza, reason: merged with bridge method [inline-methods] */
    public final V put(K k, V v) {
        zzdbr();
        int iZza = zza(k);
        if (iZza >= 0) {
            return (V) this.zzpsl.get(iZza).setValue(v);
        }
        zzdbr();
        if (this.zzpsl.isEmpty() && !(this.zzpsl instanceof ArrayList)) {
            this.zzpsl = new ArrayList(this.zzpsk);
        }
        int i = -(iZza + 1);
        if (i >= this.zzpsk) {
            return zzdbs().put(k, v);
        }
        if (this.zzpsl.size() == this.zzpsk) {
            zzfkd zzfkdVarRemove = this.zzpsl.remove(this.zzpsk - 1);
            zzdbs().put((Comparable) zzfkdVarRemove.getKey(), zzfkdVarRemove.getValue());
        }
        this.zzpsl.add(i, new zzfkd(this, k, v));
        return null;
    }

    public void zzbkr() {
        if (this.zzldh) {
            return;
        }
        this.zzpsm = this.zzpsm.isEmpty() ? Collections.emptyMap() : Collections.unmodifiableMap(this.zzpsm);
        this.zzpso = this.zzpso.isEmpty() ? Collections.emptyMap() : Collections.unmodifiableMap(this.zzpso);
        this.zzldh = true;
    }

    public final int zzdbp() {
        return this.zzpsl.size();
    }

    public final Iterable<Map.Entry<K, V>> zzdbq() {
        return this.zzpsm.isEmpty() ? zzfka.zzdbt() : this.zzpsm.entrySet();
    }

    public final Map.Entry<K, V> zzmr(int i) {
        return this.zzpsl.get(i);
    }
}
