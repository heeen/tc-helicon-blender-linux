package com.google.firebase.internal;

import android.support.annotation.Nullable;
import com.google.android.gms.common.internal.zzbg;
import java.util.Arrays;

/* JADX INFO: loaded from: classes.dex */
public final class zzc {
    private String zzeia;

    public zzc(@Nullable String str) {
        this.zzeia = str;
    }

    public final boolean equals(Object obj) {
        if (obj instanceof zzc) {
            return zzbg.equal(this.zzeia, ((zzc) obj).zzeia);
        }
        return false;
    }

    @Nullable
    public final String getToken() {
        return this.zzeia;
    }

    public final int hashCode() {
        return Arrays.hashCode(new Object[]{this.zzeia});
    }

    public final String toString() {
        return zzbg.zzx(this).zzg("token", this.zzeia).toString();
    }
}
