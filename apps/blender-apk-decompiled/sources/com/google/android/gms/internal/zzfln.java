package com.google.android.gms.internal;

import com.google.android.gms.internal.zzflm;
import java.io.IOException;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.List;

/* JADX INFO: loaded from: classes.dex */
public final class zzfln<M extends zzflm<M>, T> {
    public final int tag;
    private int type;
    protected final Class<T> zznro;
    private zzfhu<?, ?> zzppk;
    protected final boolean zzpvm;

    private zzfln(int i, Class<T> cls, int i2, boolean z) {
        this(11, cls, null, i2, false);
    }

    private zzfln(int i, Class<T> cls, zzfhu<?, ?> zzfhuVar, int i2, boolean z) {
        this.type = i;
        this.zznro = cls;
        this.tag = i2;
        this.zzpvm = false;
        this.zzppk = null;
    }

    public static <M extends zzflm<M>, T extends zzfls> zzfln<M, T> zza(int i, Class<T> cls, long j) {
        return new zzfln<>(11, cls, (int) j, false);
    }

    private final Object zzbj(zzflj zzfljVar) {
        Class componentType = this.zzpvm ? this.zznro.getComponentType() : this.zznro;
        try {
            switch (this.type) {
                case 10:
                    zzfls zzflsVar = (zzfls) componentType.newInstance();
                    zzfljVar.zza(zzflsVar, this.tag >>> 3);
                    return zzflsVar;
                case 11:
                    zzfls zzflsVar2 = (zzfls) componentType.newInstance();
                    zzfljVar.zza(zzflsVar2);
                    return zzflsVar2;
                default:
                    int i = this.type;
                    StringBuilder sb = new StringBuilder(24);
                    sb.append("Unknown type ");
                    sb.append(i);
                    throw new IllegalArgumentException(sb.toString());
            }
        } catch (IOException e) {
            throw new IllegalArgumentException("Error reading extension field", e);
        } catch (IllegalAccessException e2) {
            String strValueOf = String.valueOf(componentType);
            StringBuilder sb2 = new StringBuilder(String.valueOf(strValueOf).length() + 33);
            sb2.append("Error creating instance of class ");
            sb2.append(strValueOf);
            throw new IllegalArgumentException(sb2.toString(), e2);
        } catch (InstantiationException e3) {
            String strValueOf2 = String.valueOf(componentType);
            StringBuilder sb3 = new StringBuilder(String.valueOf(strValueOf2).length() + 33);
            sb3.append("Error creating instance of class ");
            sb3.append(strValueOf2);
            throw new IllegalArgumentException(sb3.toString(), e3);
        }
    }

    public final boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof zzfln)) {
            return false;
        }
        zzfln zzflnVar = (zzfln) obj;
        return this.type == zzflnVar.type && this.zznro == zzflnVar.zznro && this.tag == zzflnVar.tag && this.zzpvm == zzflnVar.zzpvm;
    }

    public final int hashCode() {
        return ((((((this.type + 1147) * 31) + this.zznro.hashCode()) * 31) + this.tag) * 31) + (this.zzpvm ? 1 : 0);
    }

    protected final void zza(Object obj, zzflk zzflkVar) {
        try {
            zzflkVar.zzmy(this.tag);
            switch (this.type) {
                case 10:
                    int i = this.tag >>> 3;
                    ((zzfls) obj).zza(zzflkVar);
                    zzflkVar.zzac(i, 4);
                    return;
                case 11:
                    zzflkVar.zzb((zzfls) obj);
                    return;
                default:
                    int i2 = this.type;
                    StringBuilder sb = new StringBuilder(24);
                    sb.append("Unknown type ");
                    sb.append(i2);
                    throw new IllegalArgumentException(sb.toString());
            }
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    final T zzbq(List<zzflu> list) {
        if (list == null) {
            return null;
        }
        if (!this.zzpvm) {
            if (list.isEmpty()) {
                return null;
            }
            return this.zznro.cast(zzbj(zzflj.zzbe(list.get(list.size() - 1).zzjwl)));
        }
        ArrayList arrayList = new ArrayList();
        for (int i = 0; i < list.size(); i++) {
            zzflu zzfluVar = list.get(i);
            if (zzfluVar.zzjwl.length != 0) {
                arrayList.add(zzbj(zzflj.zzbe(zzfluVar.zzjwl)));
            }
        }
        int size = arrayList.size();
        if (size == 0) {
            return null;
        }
        T tCast = this.zznro.cast(Array.newInstance(this.zznro.getComponentType(), size));
        for (int i2 = 0; i2 < size; i2++) {
            Array.set(tCast, i2, arrayList.get(i2));
        }
        return tCast;
    }

    protected final int zzcw(Object obj) {
        int i = this.tag >>> 3;
        switch (this.type) {
            case 10:
                return (zzflk.zzlw(i) << 1) + ((zzfls) obj).zzhs();
            case 11:
                return zzflk.zzb(i, (zzfls) obj);
            default:
                int i2 = this.type;
                StringBuilder sb = new StringBuilder(24);
                sb.append("Unknown type ");
                sb.append(i2);
                throw new IllegalArgumentException(sb.toString());
        }
    }
}
