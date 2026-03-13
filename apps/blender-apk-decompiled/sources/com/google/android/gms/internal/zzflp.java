package com.google.android.gms.internal;

import java.io.IOException;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/* JADX INFO: loaded from: classes.dex */
final class zzflp implements Cloneable {
    private Object value;
    private zzfln<?, ?> zzpvr;
    private List<zzflu> zzpvs = new ArrayList();

    zzflp() {
    }

    private final byte[] toByteArray() throws IOException {
        byte[] bArr = new byte[zzq()];
        zza(zzflk.zzbf(bArr));
        return bArr;
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* JADX INFO: renamed from: zzdcm, reason: merged with bridge method [inline-methods] */
    public zzflp clone() {
        Object objClone;
        zzflp zzflpVar = new zzflp();
        try {
            zzflpVar.zzpvr = this.zzpvr;
            if (this.zzpvs == null) {
                zzflpVar.zzpvs = null;
            } else {
                zzflpVar.zzpvs.addAll(this.zzpvs);
            }
            if (this.value != null) {
                if (this.value instanceof zzfls) {
                    objClone = (zzfls) ((zzfls) this.value).clone();
                } else if (this.value instanceof byte[]) {
                    objClone = ((byte[]) this.value).clone();
                } else {
                    int i = 0;
                    if (this.value instanceof byte[][]) {
                        byte[][] bArr = (byte[][]) this.value;
                        byte[][] bArr2 = new byte[bArr.length][];
                        zzflpVar.value = bArr2;
                        while (i < bArr.length) {
                            bArr2[i] = (byte[]) bArr[i].clone();
                            i++;
                        }
                    } else if (this.value instanceof boolean[]) {
                        objClone = ((boolean[]) this.value).clone();
                    } else if (this.value instanceof int[]) {
                        objClone = ((int[]) this.value).clone();
                    } else if (this.value instanceof long[]) {
                        objClone = ((long[]) this.value).clone();
                    } else if (this.value instanceof float[]) {
                        objClone = ((float[]) this.value).clone();
                    } else if (this.value instanceof double[]) {
                        objClone = ((double[]) this.value).clone();
                    } else if (this.value instanceof zzfls[]) {
                        zzfls[] zzflsVarArr = (zzfls[]) this.value;
                        zzfls[] zzflsVarArr2 = new zzfls[zzflsVarArr.length];
                        zzflpVar.value = zzflsVarArr2;
                        while (i < zzflsVarArr.length) {
                            zzflsVarArr2[i] = (zzfls) zzflsVarArr[i].clone();
                            i++;
                        }
                    }
                }
                zzflpVar.value = objClone;
            }
            return zzflpVar;
        } catch (CloneNotSupportedException e) {
            throw new AssertionError(e);
        }
    }

    public final boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (!(obj instanceof zzflp)) {
            return false;
        }
        zzflp zzflpVar = (zzflp) obj;
        if (this.value != null && zzflpVar.value != null) {
            if (this.zzpvr != zzflpVar.zzpvr) {
                return false;
            }
            return !this.zzpvr.zznro.isArray() ? this.value.equals(zzflpVar.value) : this.value instanceof byte[] ? Arrays.equals((byte[]) this.value, (byte[]) zzflpVar.value) : this.value instanceof int[] ? Arrays.equals((int[]) this.value, (int[]) zzflpVar.value) : this.value instanceof long[] ? Arrays.equals((long[]) this.value, (long[]) zzflpVar.value) : this.value instanceof float[] ? Arrays.equals((float[]) this.value, (float[]) zzflpVar.value) : this.value instanceof double[] ? Arrays.equals((double[]) this.value, (double[]) zzflpVar.value) : this.value instanceof boolean[] ? Arrays.equals((boolean[]) this.value, (boolean[]) zzflpVar.value) : Arrays.deepEquals((Object[]) this.value, (Object[]) zzflpVar.value);
        }
        if (this.zzpvs != null && zzflpVar.zzpvs != null) {
            return this.zzpvs.equals(zzflpVar.zzpvs);
        }
        try {
            return Arrays.equals(toByteArray(), zzflpVar.toByteArray());
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    public final int hashCode() {
        try {
            return Arrays.hashCode(toByteArray()) + 527;
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    final void zza(zzflk zzflkVar) throws IOException {
        if (this.value == null) {
            for (zzflu zzfluVar : this.zzpvs) {
                zzflkVar.zzmy(zzfluVar.tag);
                zzflkVar.zzbh(zzfluVar.zzjwl);
            }
            return;
        }
        zzfln<?, ?> zzflnVar = this.zzpvr;
        Object obj = this.value;
        if (!zzflnVar.zzpvm) {
            zzflnVar.zza(obj, zzflkVar);
            return;
        }
        int length = Array.getLength(obj);
        for (int i = 0; i < length; i++) {
            Object obj2 = Array.get(obj, i);
            if (obj2 != null) {
                zzflnVar.zza(obj2, zzflkVar);
            }
        }
    }

    final void zza(zzflu zzfluVar) {
        this.zzpvs.add(zzfluVar);
    }

    /* JADX WARN: Multi-variable type inference failed */
    final <T> T zzb(zzfln<?, T> zzflnVar) {
        if (this.value == null) {
            this.zzpvr = zzflnVar;
            this.value = zzflnVar.zzbq(this.zzpvs);
            this.zzpvs = null;
        } else if (!this.zzpvr.equals(zzflnVar)) {
            throw new IllegalStateException("Tried to getExtension with a different Extension.");
        }
        return (T) this.value;
    }

    final int zzq() {
        if (this.value == null) {
            int iZzmf = 0;
            for (zzflu zzfluVar : this.zzpvs) {
                iZzmf += zzflk.zzmf(zzfluVar.tag) + 0 + zzfluVar.zzjwl.length;
            }
            return iZzmf;
        }
        zzfln<?, ?> zzflnVar = this.zzpvr;
        Object obj = this.value;
        if (!zzflnVar.zzpvm) {
            return zzflnVar.zzcw(obj);
        }
        int length = Array.getLength(obj);
        int iZzcw = 0;
        for (int i = 0; i < length; i++) {
            if (Array.get(obj, i) != null) {
                iZzcw += zzflnVar.zzcw(Array.get(obj, i));
            }
        }
        return iZzcw;
    }
}
