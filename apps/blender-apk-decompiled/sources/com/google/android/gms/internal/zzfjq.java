package com.google.android.gms.internal;

import android.support.v7.widget.ActivityChooserView;
import java.io.IOException;
import java.util.ArrayList;

/* JADX INFO: loaded from: classes.dex */
final class zzfjq extends zzfgs {
    private static final int[] zzprr;
    private final int zzprs;
    private final zzfgs zzprt;
    private final zzfgs zzpru;
    private final int zzprv;
    private final int zzprw;

    static {
        ArrayList arrayList = new ArrayList();
        int i = 1;
        int i2 = 1;
        while (i > 0) {
            arrayList.add(Integer.valueOf(i));
            int i3 = i2 + i;
            i2 = i;
            i = i3;
        }
        arrayList.add(Integer.valueOf(ActivityChooserView.ActivityChooserViewAdapter.MAX_ACTIVITY_COUNT_UNLIMITED));
        zzprr = new int[arrayList.size()];
        for (int i4 = 0; i4 < zzprr.length; i4++) {
            zzprr[i4] = ((Integer) arrayList.get(i4)).intValue();
        }
    }

    private zzfjq(zzfgs zzfgsVar, zzfgs zzfgsVar2) {
        this.zzprt = zzfgsVar;
        this.zzpru = zzfgsVar2;
        this.zzprv = zzfgsVar.size();
        this.zzprs = this.zzprv + zzfgsVar2.size();
        this.zzprw = Math.max(zzfgsVar.zzcxr(), zzfgsVar2.zzcxr()) + 1;
    }

    static zzfgs zza(zzfgs zzfgsVar, zzfgs zzfgsVar2) {
        if (zzfgsVar2.size() == 0) {
            return zzfgsVar;
        }
        if (zzfgsVar.size() == 0) {
            return zzfgsVar2;
        }
        int size = zzfgsVar.size() + zzfgsVar2.size();
        if (size < 128) {
            return zzb(zzfgsVar, zzfgsVar2);
        }
        if (zzfgsVar instanceof zzfjq) {
            zzfjq zzfjqVar = (zzfjq) zzfgsVar;
            if (zzfjqVar.zzpru.size() + zzfgsVar2.size() < 128) {
                return new zzfjq(zzfjqVar.zzprt, zzb(zzfjqVar.zzpru, zzfgsVar2));
            }
            if (zzfjqVar.zzprt.zzcxr() > zzfjqVar.zzpru.zzcxr() && zzfjqVar.zzcxr() > zzfgsVar2.zzcxr()) {
                return new zzfjq(zzfjqVar.zzprt, new zzfjq(zzfjqVar.zzpru, zzfgsVar2));
            }
        }
        return size >= zzprr[Math.max(zzfgsVar.zzcxr(), zzfgsVar2.zzcxr()) + 1] ? new zzfjq(zzfgsVar, zzfgsVar2) : new zzfjs().zzc(zzfgsVar, zzfgsVar2);
    }

    private static zzfgs zzb(zzfgs zzfgsVar, zzfgs zzfgsVar2) {
        int size = zzfgsVar.size();
        int size2 = zzfgsVar2.size();
        byte[] bArr = new byte[size + size2];
        zzfgsVar.zza(bArr, 0, 0, size);
        zzfgsVar2.zza(bArr, 0, size, size2);
        return zzfgs.zzba(bArr);
    }

    @Override // com.google.android.gms.internal.zzfgs
    public final boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (!(obj instanceof zzfgs)) {
            return false;
        }
        zzfgs zzfgsVar = (zzfgs) obj;
        if (this.zzprs != zzfgsVar.size()) {
            return false;
        }
        if (this.zzprs == 0) {
            return true;
        }
        int iZzcxt = zzcxt();
        int iZzcxt2 = zzfgsVar.zzcxt();
        if (iZzcxt != 0 && iZzcxt2 != 0 && iZzcxt != iZzcxt2) {
            return false;
        }
        zzfjr zzfjrVar = null;
        zzfjt zzfjtVar = new zzfjt(this);
        zzfgy next = zzfjtVar.next();
        zzfjt zzfjtVar2 = new zzfjt(zzfgsVar);
        zzfgy next2 = zzfjtVar2.next();
        int i = 0;
        int i2 = 0;
        int i3 = 0;
        while (true) {
            int size = next.size() - i;
            int size2 = next2.size() - i2;
            int iMin = Math.min(size, size2);
            if (!(i == 0 ? next.zza(next2, i2, iMin) : next2.zza(next, i, iMin))) {
                return false;
            }
            i3 += iMin;
            if (i3 >= this.zzprs) {
                if (i3 == this.zzprs) {
                    return true;
                }
                throw new IllegalStateException();
            }
            if (iMin == size) {
                next = zzfjtVar.next();
                i = 0;
            } else {
                i += iMin;
                next = next;
            }
            if (iMin == size2) {
                next2 = zzfjtVar2.next();
                i2 = 0;
            } else {
                i2 += iMin;
            }
        }
    }

    @Override // com.google.android.gms.internal.zzfgs
    public final int size() {
        return this.zzprs;
    }

    @Override // com.google.android.gms.internal.zzfgs
    final void zza(zzfgr zzfgrVar) throws IOException {
        this.zzprt.zza(zzfgrVar);
        this.zzpru.zza(zzfgrVar);
    }

    @Override // com.google.android.gms.internal.zzfgs
    public final zzfgs zzaa(int i, int i2) {
        int iZzh = zzh(i, i2, this.zzprs);
        if (iZzh == 0) {
            return zzfgs.zzpnw;
        }
        if (iZzh == this.zzprs) {
            return this;
        }
        if (i2 <= this.zzprv) {
            return this.zzprt.zzaa(i, i2);
        }
        if (i >= this.zzprv) {
            return this.zzpru.zzaa(i - this.zzprv, i2 - this.zzprv);
        }
        zzfgs zzfgsVar = this.zzprt;
        return new zzfjq(zzfgsVar.zzaa(i, zzfgsVar.size()), this.zzpru.zzaa(0, i2 - this.zzprv));
    }

    @Override // com.google.android.gms.internal.zzfgs
    protected final void zzb(byte[] bArr, int i, int i2, int i3) {
        if (i + i3 <= this.zzprv) {
            this.zzprt.zzb(bArr, i, i2, i3);
        } else {
            if (i >= this.zzprv) {
                this.zzpru.zzb(bArr, i - this.zzprv, i2, i3);
                return;
            }
            int i4 = this.zzprv - i;
            this.zzprt.zzb(bArr, i, i2, i4);
            this.zzpru.zzb(bArr, 0, i2 + i4, i3 - i4);
        }
    }

    @Override // com.google.android.gms.internal.zzfgs
    public final zzfhb zzcxq() {
        return zzfhb.zzh(new zzfju(this));
    }

    @Override // com.google.android.gms.internal.zzfgs
    protected final int zzcxr() {
        return this.zzprw;
    }

    @Override // com.google.android.gms.internal.zzfgs
    protected final boolean zzcxs() {
        return this.zzprs >= zzprr[this.zzprw];
    }

    @Override // com.google.android.gms.internal.zzfgs
    protected final int zzg(int i, int i2, int i3) {
        if (i2 + i3 <= this.zzprv) {
            return this.zzprt.zzg(i, i2, i3);
        }
        if (i2 >= this.zzprv) {
            return this.zzpru.zzg(i, i2 - this.zzprv, i3);
        }
        int i4 = this.zzprv - i2;
        return this.zzpru.zzg(this.zzprt.zzg(i, i2, i4), 0, i3 - i4);
    }

    @Override // com.google.android.gms.internal.zzfgs
    public final byte zzld(int i) {
        zzab(i, this.zzprs);
        return i < this.zzprv ? this.zzprt.zzld(i) : this.zzpru.zzld(i - this.zzprv);
    }
}
