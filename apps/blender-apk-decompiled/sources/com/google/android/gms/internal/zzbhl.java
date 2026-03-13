package com.google.android.gms.internal;

import android.os.Parcel;
import android.os.Parcelable;
import android.util.SparseArray;
import java.util.ArrayList;
import java.util.HashMap;

/* JADX INFO: loaded from: classes.dex */
public final class zzbhl extends zzbgl implements zzbhr<String, Integer> {
    public static final Parcelable.Creator<zzbhl> CREATOR = new zzbhn();
    private int zzehz;
    private final HashMap<String, Integer> zzgij;
    private final SparseArray<String> zzgik;
    private final ArrayList<zzbhm> zzgil;

    public zzbhl() {
        this.zzehz = 1;
        this.zzgij = new HashMap<>();
        this.zzgik = new SparseArray<>();
        this.zzgil = null;
    }

    zzbhl(int i, ArrayList<zzbhm> arrayList) {
        this.zzehz = i;
        this.zzgij = new HashMap<>();
        this.zzgik = new SparseArray<>();
        this.zzgil = null;
        zzd(arrayList);
    }

    private final void zzd(ArrayList<zzbhm> arrayList) {
        ArrayList<zzbhm> arrayList2 = arrayList;
        int size = arrayList2.size();
        int i = 0;
        while (i < size) {
            zzbhm zzbhmVar = arrayList2.get(i);
            i++;
            zzbhm zzbhmVar2 = zzbhmVar;
            zzi(zzbhmVar2.zzgim, zzbhmVar2.zzgin);
        }
    }

    @Override // com.google.android.gms.internal.zzbhr
    public final /* synthetic */ String convertBack(Integer num) {
        String str = this.zzgik.get(num.intValue());
        return (str == null && this.zzgij.containsKey("gms_unknown")) ? "gms_unknown" : str;
    }

    @Override // android.os.Parcelable
    public final void writeToParcel(Parcel parcel, int i) {
        int iZze = zzbgo.zze(parcel);
        zzbgo.zzc(parcel, 1, this.zzehz);
        ArrayList arrayList = new ArrayList();
        for (String str : this.zzgij.keySet()) {
            arrayList.add(new zzbhm(str, this.zzgij.get(str).intValue()));
        }
        zzbgo.zzc(parcel, 2, arrayList, false);
        zzbgo.zzai(parcel, iZze);
    }

    public final zzbhl zzi(String str, int i) {
        this.zzgij.put(str, Integer.valueOf(i));
        this.zzgik.put(i, str);
        return this;
    }
}
