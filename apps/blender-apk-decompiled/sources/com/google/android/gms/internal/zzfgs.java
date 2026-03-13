package com.google.android.gms.internal;

import android.support.v7.widget.ActivityChooserView;
import java.io.IOException;
import java.io.Serializable;
import java.util.Collection;
import java.util.Iterator;

/* JADX INFO: loaded from: classes.dex */
public abstract class zzfgs implements Serializable, Iterable<Byte> {
    public static final zzfgs zzpnw = new zzfgz(zzfhz.EMPTY_BYTE_ARRAY);
    private static final zzfgw zzpnx;
    private int zzmmk = 0;

    static {
        zzfgt zzfgtVar = null;
        zzpnx = zzfgo.zzcxm() ? new zzfha(zzfgtVar) : new zzfgu(zzfgtVar);
    }

    zzfgs() {
    }

    private static zzfgs zza(Iterator<zzfgs> it, int i) {
        if (i <= 0) {
            throw new IllegalArgumentException(String.format("length (%s) must be >= 1", Integer.valueOf(i)));
        }
        if (i == 1) {
            return it.next();
        }
        int i2 = i >>> 1;
        zzfgs zzfgsVarZza = zza(it, i2);
        zzfgs zzfgsVarZza2 = zza(it, i - i2);
        if (ActivityChooserView.ActivityChooserViewAdapter.MAX_ACTIVITY_COUNT_UNLIMITED - zzfgsVarZza.size() >= zzfgsVarZza2.size()) {
            return zzfjq.zza(zzfgsVarZza, zzfgsVarZza2);
        }
        int size = zzfgsVarZza.size();
        int size2 = zzfgsVarZza2.size();
        StringBuilder sb = new StringBuilder(53);
        sb.append("ByteString would be too long: ");
        sb.append(size);
        sb.append("+");
        sb.append(size2);
        throw new IllegalArgumentException(sb.toString());
    }

    static void zzab(int i, int i2) {
        if (((i2 - (i + 1)) | i) < 0) {
            if (i < 0) {
                StringBuilder sb = new StringBuilder(22);
                sb.append("Index < 0: ");
                sb.append(i);
                throw new ArrayIndexOutOfBoundsException(sb.toString());
            }
            StringBuilder sb2 = new StringBuilder(40);
            sb2.append("Index > length: ");
            sb2.append(i);
            sb2.append(", ");
            sb2.append(i2);
            throw new ArrayIndexOutOfBoundsException(sb2.toString());
        }
    }

    public static zzfgs zzaz(byte[] bArr) {
        return zzf(bArr, 0, bArr.length);
    }

    static zzfgs zzba(byte[] bArr) {
        return new zzfgz(bArr);
    }

    public static zzfgs zzf(byte[] bArr, int i, int i2) {
        return new zzfgz(zzpnx.zzg(bArr, i, i2));
    }

    public static zzfgs zzg(Iterable<zzfgs> iterable) {
        int size = ((Collection) iterable).size();
        return size == 0 ? zzpnw : zza(iterable.iterator(), size);
    }

    static int zzh(int i, int i2, int i3) {
        int i4 = i2 - i;
        if ((i | i2 | i4 | (i3 - i2)) >= 0) {
            return i4;
        }
        if (i < 0) {
            StringBuilder sb = new StringBuilder(32);
            sb.append("Beginning index: ");
            sb.append(i);
            sb.append(" < 0");
            throw new IndexOutOfBoundsException(sb.toString());
        }
        if (i2 < i) {
            StringBuilder sb2 = new StringBuilder(66);
            sb2.append("Beginning index larger than ending index: ");
            sb2.append(i);
            sb2.append(", ");
            sb2.append(i2);
            throw new IndexOutOfBoundsException(sb2.toString());
        }
        StringBuilder sb3 = new StringBuilder(37);
        sb3.append("End index: ");
        sb3.append(i2);
        sb3.append(" >= ");
        sb3.append(i3);
        throw new IndexOutOfBoundsException(sb3.toString());
    }

    static zzfgx zzle(int i) {
        return new zzfgx(i, null);
    }

    public static zzfgs zztv(String str) {
        return new zzfgz(str.getBytes(zzfhz.UTF_8));
    }

    public abstract boolean equals(Object obj);

    public final int hashCode() {
        int iZzg = this.zzmmk;
        if (iZzg == 0) {
            int size = size();
            iZzg = zzg(size, 0, size);
            if (iZzg == 0) {
                iZzg = 1;
            }
            this.zzmmk = iZzg;
        }
        return iZzg;
    }

    public final boolean isEmpty() {
        return size() == 0;
    }

    @Override // java.lang.Iterable
    public /* synthetic */ Iterator<Byte> iterator() {
        return new zzfgt(this);
    }

    public abstract int size();

    public final byte[] toByteArray() {
        int size = size();
        if (size == 0) {
            return zzfhz.EMPTY_BYTE_ARRAY;
        }
        byte[] bArr = new byte[size];
        zzb(bArr, 0, 0, size);
        return bArr;
    }

    public final String toString() {
        return String.format("<ByteString@%s size=%d>", Integer.toHexString(System.identityHashCode(this)), Integer.valueOf(size()));
    }

    abstract void zza(zzfgr zzfgrVar) throws IOException;

    public final void zza(byte[] bArr, int i, int i2, int i3) {
        zzh(i, i + i3, size());
        zzh(i2, i2 + i3, bArr.length);
        if (i3 > 0) {
            zzb(bArr, i, i2, i3);
        }
    }

    public abstract zzfgs zzaa(int i, int i2);

    protected abstract void zzb(byte[] bArr, int i, int i2, int i3);

    public abstract zzfhb zzcxq();

    protected abstract int zzcxr();

    protected abstract boolean zzcxs();

    protected final int zzcxt() {
        return this.zzmmk;
    }

    protected abstract int zzg(int i, int i2, int i3);

    public abstract byte zzld(int i);
}
