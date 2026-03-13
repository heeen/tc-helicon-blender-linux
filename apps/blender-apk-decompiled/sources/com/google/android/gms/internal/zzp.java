package com.google.android.gms.internal;

import android.support.v7.widget.helper.ItemTouchHelper;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/* JADX INFO: loaded from: classes.dex */
public final class zzp {
    public final List<zzl> allHeaders;
    public final byte[] data;
    public final int statusCode;
    public final Map<String, String> zzab;
    public final boolean zzac;
    private long zzad;

    private zzp(int i, byte[] bArr, Map<String, String> map, List<zzl> list, boolean z, long j) {
        this.statusCode = i;
        this.data = bArr;
        this.zzab = map;
        this.allHeaders = list == null ? null : Collections.unmodifiableList(list);
        this.zzac = z;
        this.zzad = j;
    }

    @Deprecated
    public zzp(int i, byte[] bArr, Map<String, String> map, boolean z, long j) {
        this(i, bArr, map, zza(map), z, j);
    }

    public zzp(int i, byte[] bArr, boolean z, long j, List<zzl> list) {
        this(i, bArr, zza(list), list, z, j);
    }

    @Deprecated
    public zzp(byte[] bArr, Map<String, String> map) {
        this(ItemTouchHelper.Callback.DEFAULT_DRAG_ANIMATION_DURATION, bArr, map, false, 0L);
    }

    private static List<zzl> zza(Map<String, String> map) {
        if (map == null) {
            return null;
        }
        if (map.isEmpty()) {
            return Collections.emptyList();
        }
        ArrayList arrayList = new ArrayList(map.size());
        for (Map.Entry<String, String> entry : map.entrySet()) {
            arrayList.add(new zzl(entry.getKey(), entry.getValue()));
        }
        return arrayList;
    }

    private static Map<String, String> zza(List<zzl> list) {
        if (list == null) {
            return null;
        }
        if (list.isEmpty()) {
            return Collections.emptyMap();
        }
        TreeMap treeMap = new TreeMap(String.CASE_INSENSITIVE_ORDER);
        for (zzl zzlVar : list) {
            treeMap.put(zzlVar.getName(), zzlVar.getValue());
        }
        return treeMap;
    }
}
