package com.google.android.gms.internal;

import android.support.v7.widget.ActivityChooserView;
import java.util.Arrays;
import java.util.Collection;
import java.util.RandomAccess;

/* JADX INFO: loaded from: classes.dex */
final class zzfhy extends zzfgn<Integer> implements zzfic, zzfjm, RandomAccess {
    private static final zzfhy zzpqk;
    private int size;
    private int[] zzpql;

    static {
        zzfhy zzfhyVar = new zzfhy();
        zzpqk = zzfhyVar;
        zzfhyVar.zzbkr();
    }

    zzfhy() {
        this(new int[10], 0);
    }

    private zzfhy(int[] iArr, int i) {
        this.zzpql = iArr;
        this.size = i;
    }

    private final void zzal(int i, int i2) {
        zzcxl();
        if (i < 0 || i > this.size) {
            throw new IndexOutOfBoundsException(zzmn(i));
        }
        if (this.size < this.zzpql.length) {
            System.arraycopy(this.zzpql, i, this.zzpql, i + 1, this.size - i);
        } else {
            int[] iArr = new int[((this.size * 3) / 2) + 1];
            System.arraycopy(this.zzpql, 0, iArr, 0, i);
            System.arraycopy(this.zzpql, i, iArr, i + 1, this.size - i);
            this.zzpql = iArr;
        }
        this.zzpql[i] = i2;
        this.size++;
        this.modCount++;
    }

    public static zzfhy zzdad() {
        return zzpqk;
    }

    private final void zzmm(int i) {
        if (i < 0 || i >= this.size) {
            throw new IndexOutOfBoundsException(zzmn(i));
        }
    }

    private final String zzmn(int i) {
        int i2 = this.size;
        StringBuilder sb = new StringBuilder(35);
        sb.append("Index:");
        sb.append(i);
        sb.append(", Size:");
        sb.append(i2);
        return sb.toString();
    }

    @Override // com.google.android.gms.internal.zzfgn, java.util.AbstractList, java.util.List
    public final /* synthetic */ void add(int i, Object obj) {
        zzal(i, ((Integer) obj).intValue());
    }

    @Override // com.google.android.gms.internal.zzfgn, java.util.AbstractCollection, java.util.Collection, java.util.List
    public final boolean addAll(Collection<? extends Integer> collection) {
        zzcxl();
        zzfhz.checkNotNull(collection);
        if (!(collection instanceof zzfhy)) {
            return super.addAll(collection);
        }
        zzfhy zzfhyVar = (zzfhy) collection;
        if (zzfhyVar.size == 0) {
            return false;
        }
        if (ActivityChooserView.ActivityChooserViewAdapter.MAX_ACTIVITY_COUNT_UNLIMITED - this.size < zzfhyVar.size) {
            throw new OutOfMemoryError();
        }
        int i = this.size + zzfhyVar.size;
        if (i > this.zzpql.length) {
            this.zzpql = Arrays.copyOf(this.zzpql, i);
        }
        System.arraycopy(zzfhyVar.zzpql, 0, this.zzpql, this.size, zzfhyVar.size);
        this.size = i;
        this.modCount++;
        return true;
    }

    @Override // com.google.android.gms.internal.zzfgn, java.util.AbstractList, java.util.Collection, java.util.List
    public final boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof zzfhy)) {
            return super.equals(obj);
        }
        zzfhy zzfhyVar = (zzfhy) obj;
        if (this.size != zzfhyVar.size) {
            return false;
        }
        int[] iArr = zzfhyVar.zzpql;
        for (int i = 0; i < this.size; i++) {
            if (this.zzpql[i] != iArr[i]) {
                return false;
            }
        }
        return true;
    }

    @Override // java.util.AbstractList, java.util.List
    public final /* synthetic */ Object get(int i) {
        return Integer.valueOf(getInt(i));
    }

    @Override // com.google.android.gms.internal.zzfic
    public final int getInt(int i) {
        zzmm(i);
        return this.zzpql[i];
    }

    @Override // com.google.android.gms.internal.zzfgn, java.util.AbstractList, java.util.Collection, java.util.List
    public final int hashCode() {
        int i = 1;
        for (int i2 = 0; i2 < this.size; i2++) {
            i = (i * 31) + this.zzpql[i2];
        }
        return i;
    }

    @Override // com.google.android.gms.internal.zzfgn, java.util.AbstractList, java.util.List
    public final /* synthetic */ Object remove(int i) {
        zzcxl();
        zzmm(i);
        int i2 = this.zzpql[i];
        System.arraycopy(this.zzpql, i + 1, this.zzpql, i, this.size - i);
        this.size--;
        this.modCount++;
        return Integer.valueOf(i2);
    }

    @Override // com.google.android.gms.internal.zzfgn, java.util.AbstractCollection, java.util.Collection, java.util.List
    public final boolean remove(Object obj) {
        zzcxl();
        for (int i = 0; i < this.size; i++) {
            if (obj.equals(Integer.valueOf(this.zzpql[i]))) {
                System.arraycopy(this.zzpql, i + 1, this.zzpql, i, this.size - i);
                this.size--;
                this.modCount++;
                return true;
            }
        }
        return false;
    }

    @Override // com.google.android.gms.internal.zzfgn, java.util.AbstractList, java.util.List
    public final /* synthetic */ Object set(int i, Object obj) {
        int iIntValue = ((Integer) obj).intValue();
        zzcxl();
        zzmm(i);
        int i2 = this.zzpql[i];
        this.zzpql[i] = iIntValue;
        return Integer.valueOf(i2);
    }

    @Override // java.util.AbstractCollection, java.util.Collection, java.util.List
    public final int size() {
        return this.size;
    }

    @Override // com.google.android.gms.internal.zzfid
    /* JADX INFO: renamed from: zzmk, reason: merged with bridge method [inline-methods] */
    public final zzfic zzmo(int i) {
        if (i >= this.size) {
            return new zzfhy(Arrays.copyOf(this.zzpql, i), this.size);
        }
        throw new IllegalArgumentException();
    }

    @Override // com.google.android.gms.internal.zzfic
    public final void zzml(int i) {
        zzal(this.size, i);
    }
}
