package com.google.android.gms.internal;

import android.os.Parcel;
import android.os.Parcelable;
import com.google.android.gms.common.internal.zzbq;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/* JADX INFO: loaded from: classes.dex */
public final class zzbhv extends zzbgl {
    public static final Parcelable.Creator<zzbhv> CREATOR = new zzbhy();
    private int zzehz;
    private final HashMap<String, Map<String, zzbhq<?, ?>>> zzgiy;
    private final ArrayList<zzbhw> zzgiz = null;
    private final String zzgja;

    zzbhv(int i, ArrayList<zzbhw> arrayList, String str) {
        this.zzehz = i;
        HashMap<String, Map<String, zzbhq<?, ?>>> map = new HashMap<>();
        int size = arrayList.size();
        for (int i2 = 0; i2 < size; i2++) {
            zzbhw zzbhwVar = arrayList.get(i2);
            map.put(zzbhwVar.className, zzbhwVar.zzank());
        }
        this.zzgiy = map;
        this.zzgja = (String) zzbq.checkNotNull(str);
        zzani();
    }

    private final void zzani() {
        Iterator<String> it = this.zzgiy.keySet().iterator();
        while (it.hasNext()) {
            Map<String, zzbhq<?, ?>> map = this.zzgiy.get(it.next());
            Iterator<String> it2 = map.keySet().iterator();
            while (it2.hasNext()) {
                map.get(it2.next()).zza(this);
            }
        }
    }

    public final String toString() {
        StringBuilder sb = new StringBuilder();
        for (String str : this.zzgiy.keySet()) {
            sb.append(str);
            sb.append(":\n");
            Map<String, zzbhq<?, ?>> map = this.zzgiy.get(str);
            for (String str2 : map.keySet()) {
                sb.append("  ");
                sb.append(str2);
                sb.append(": ");
                sb.append(map.get(str2));
            }
        }
        return sb.toString();
    }

    @Override // android.os.Parcelable
    public final void writeToParcel(Parcel parcel, int i) {
        int iZze = zzbgo.zze(parcel);
        zzbgo.zzc(parcel, 1, this.zzehz);
        ArrayList arrayList = new ArrayList();
        for (String str : this.zzgiy.keySet()) {
            arrayList.add(new zzbhw(str, this.zzgiy.get(str)));
        }
        zzbgo.zzc(parcel, 2, arrayList, false);
        zzbgo.zza(parcel, 3, this.zzgja, false);
        zzbgo.zzai(parcel, iZze);
    }

    public final String zzanj() {
        return this.zzgja;
    }

    public final Map<String, zzbhq<?, ?>> zzgz(String str) {
        return this.zzgiy.get(str);
    }
}
