package com.google.android.gms.internal;

import com.google.android.gms.internal.zzfhu;
import java.io.IOException;

/* JADX INFO: loaded from: classes.dex */
final class zzfhi implements zzfli {
    private final zzfhg zzpob;

    private zzfhi(zzfhg zzfhgVar) {
        this.zzpob = (zzfhg) zzfhz.zzc(zzfhgVar, "output");
        this.zzpob.zzpoq = this;
    }

    public static zzfhi zzb(zzfhg zzfhgVar) {
        return zzfhgVar.zzpoq != null ? zzfhgVar.zzpoq : new zzfhi(zzfhgVar);
    }

    @Override // com.google.android.gms.internal.zzfli
    public final void zzb(int i, Object obj) {
        try {
            if (obj instanceof zzfgs) {
                this.zzpob.zzb(i, (zzfgs) obj);
            } else {
                this.zzpob.zzb(i, (zzfjc) obj);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override // com.google.android.gms.internal.zzfli
    public final int zzcyz() {
        return zzfhu.zzg.zzpqf;
    }
}
