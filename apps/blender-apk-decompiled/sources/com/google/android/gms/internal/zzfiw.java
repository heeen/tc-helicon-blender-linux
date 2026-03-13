package com.google.android.gms.internal;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/* JADX INFO: loaded from: classes.dex */
public final class zzfiw<K, V> extends LinkedHashMap<K, V> {
    private static final zzfiw zzprd;
    private boolean zzpnq;

    static {
        zzfiw zzfiwVar = new zzfiw();
        zzprd = zzfiwVar;
        zzfiwVar.zzpnq = false;
    }

    private zzfiw() {
        this.zzpnq = true;
    }

    private zzfiw(Map<K, V> map) {
        super(map);
        this.zzpnq = true;
    }

    private static int zzcs(Object obj) {
        if (obj instanceof byte[]) {
            return zzfhz.hashCode((byte[]) obj);
        }
        if (obj instanceof zzfia) {
            throw new UnsupportedOperationException();
        }
        return obj.hashCode();
    }

    public static <K, V> zzfiw<K, V> zzdat() {
        return zzprd;
    }

    private final void zzdav() {
        if (!this.zzpnq) {
            throw new UnsupportedOperationException();
        }
    }

    @Override // java.util.LinkedHashMap, java.util.HashMap, java.util.AbstractMap, java.util.Map
    public final void clear() {
        zzdav();
        super.clear();
    }

    @Override // java.util.LinkedHashMap, java.util.HashMap, java.util.AbstractMap, java.util.Map
    public final Set<Map.Entry<K, V>> entrySet() {
        return isEmpty() ? Collections.emptySet() : super.entrySet();
    }

    /* JADX WARN: Removed duplicated region for block: B:25:0x005c A[RETURN] */
    @Override // java.util.AbstractMap, java.util.Map
    /*
        Code decompiled incorrectly, please refer to instructions dump.
        To view partially-correct add '--show-bad-code' argument
    */
    public final boolean equals(java.lang.Object r6) {
        /*
            r5 = this;
            boolean r0 = r6 instanceof java.util.Map
            r1 = 0
            if (r0 == 0) goto L5d
            java.util.Map r6 = (java.util.Map) r6
            r0 = 1
            if (r5 == r6) goto L59
            int r2 = r5.size()
            int r3 = r6.size()
            if (r2 == r3) goto L16
        L14:
            r5 = r1
            goto L5a
        L16:
            java.util.Set r5 = r5.entrySet()
            java.util.Iterator r5 = r5.iterator()
        L1e:
            boolean r2 = r5.hasNext()
            if (r2 == 0) goto L59
            java.lang.Object r2 = r5.next()
            java.util.Map$Entry r2 = (java.util.Map.Entry) r2
            java.lang.Object r3 = r2.getKey()
            boolean r3 = r6.containsKey(r3)
            if (r3 != 0) goto L35
            goto L14
        L35:
            java.lang.Object r3 = r2.getValue()
            java.lang.Object r2 = r2.getKey()
            java.lang.Object r2 = r6.get(r2)
            boolean r4 = r3 instanceof byte[]
            if (r4 == 0) goto L52
            boolean r4 = r2 instanceof byte[]
            if (r4 == 0) goto L52
            byte[] r3 = (byte[]) r3
            byte[] r2 = (byte[]) r2
            boolean r2 = java.util.Arrays.equals(r3, r2)
            goto L56
        L52:
            boolean r2 = r3.equals(r2)
        L56:
            if (r2 != 0) goto L1e
            goto L14
        L59:
            r5 = r0
        L5a:
            if (r5 == 0) goto L5d
            return r0
        L5d:
            return r1
        */
        throw new UnsupportedOperationException("Method not decompiled: com.google.android.gms.internal.zzfiw.equals(java.lang.Object):boolean");
    }

    @Override // java.util.AbstractMap, java.util.Map
    public final int hashCode() {
        int iZzcs = 0;
        for (Map.Entry<K, V> entry : entrySet()) {
            iZzcs += zzcs(entry.getValue()) ^ zzcs(entry.getKey());
        }
        return iZzcs;
    }

    public final boolean isMutable() {
        return this.zzpnq;
    }

    @Override // java.util.HashMap, java.util.AbstractMap, java.util.Map
    public final V put(K k, V v) {
        zzdav();
        zzfhz.checkNotNull(k);
        zzfhz.checkNotNull(v);
        return (V) super.put(k, v);
    }

    @Override // java.util.HashMap, java.util.AbstractMap, java.util.Map
    public final void putAll(Map<? extends K, ? extends V> map) {
        zzdav();
        for (K k : map.keySet()) {
            zzfhz.checkNotNull(k);
            zzfhz.checkNotNull(map.get(k));
        }
        super.putAll(map);
    }

    @Override // java.util.HashMap, java.util.AbstractMap, java.util.Map
    public final V remove(Object obj) {
        zzdav();
        return (V) super.remove(obj);
    }

    public final void zza(zzfiw<K, V> zzfiwVar) {
        zzdav();
        if (zzfiwVar.isEmpty()) {
            return;
        }
        putAll(zzfiwVar);
    }

    public final void zzbkr() {
        this.zzpnq = false;
    }

    public final zzfiw<K, V> zzdau() {
        return isEmpty() ? new zzfiw<>() : new zzfiw<>(this);
    }
}
