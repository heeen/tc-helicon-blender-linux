package com.google.android.gms.phenotype;

import android.os.Parcel;
import android.os.Parcelable;
import com.google.android.gms.common.annotation.KeepForSdk;
import com.google.android.gms.internal.zzbgl;
import com.google.android.gms.internal.zzbgo;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;

/* JADX INFO: loaded from: classes.dex */
@KeepForSdk
public class Configuration extends zzbgl implements Comparable<Configuration> {

    @KeepForSdk
    public static final Parcelable.Creator<Configuration> CREATOR = new zzc();
    private Map<String, zzi> zzfqk = new TreeMap();
    private int zzkfq;
    private zzi[] zzkfr;
    private String[] zzkfs;

    public Configuration(int i, zzi[] zziVarArr, String[] strArr) {
        this.zzkfq = i;
        this.zzkfr = zziVarArr;
        for (zzi zziVar : zziVarArr) {
            this.zzfqk.put(zziVar.name, zziVar);
        }
        this.zzkfs = strArr;
        if (this.zzkfs != null) {
            Arrays.sort(this.zzkfs);
        }
    }

    @Override // java.lang.Comparable
    public /* synthetic */ int compareTo(Configuration configuration) {
        return this.zzkfq - configuration.zzkfq;
    }

    public boolean equals(Object obj) {
        if (obj instanceof Configuration) {
            Configuration configuration = (Configuration) obj;
            if (this.zzkfq == configuration.zzkfq && zzn.equals(this.zzfqk, configuration.zzfqk) && Arrays.equals(this.zzkfs, configuration.zzkfs)) {
                return true;
            }
        }
        return false;
    }

    public String toString() {
        StringBuilder sb = new StringBuilder("Configuration(");
        sb.append(this.zzkfq);
        sb.append(", ");
        sb.append("(");
        Iterator<zzi> it = this.zzfqk.values().iterator();
        while (it.hasNext()) {
            sb.append(it.next());
            sb.append(", ");
        }
        sb.append(")");
        sb.append(", ");
        sb.append("(");
        if (this.zzkfs != null) {
            for (String str : this.zzkfs) {
                sb.append(str);
                sb.append(", ");
            }
        } else {
            sb.append("null");
        }
        sb.append(")");
        sb.append(")");
        return sb.toString();
    }

    @Override // android.os.Parcelable
    public void writeToParcel(Parcel parcel, int i) {
        int iZze = zzbgo.zze(parcel);
        zzbgo.zzc(parcel, 2, this.zzkfq);
        zzbgo.zza(parcel, 3, (Parcelable[]) this.zzkfr, i, false);
        zzbgo.zza(parcel, 4, this.zzkfs, false);
        zzbgo.zzai(parcel, iZze);
    }
}
