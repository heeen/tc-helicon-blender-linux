package com.google.android.gms.common.images;

import android.net.Uri;
import com.google.android.gms.common.internal.Hide;
import com.google.android.gms.common.internal.zzbg;
import java.util.Arrays;

/* JADX INFO: loaded from: classes.dex */
@Hide
final class zzb {
    public final Uri uri;

    public zzb(Uri uri) {
        this.uri = uri;
    }

    public final boolean equals(Object obj) {
        if (!(obj instanceof zzb)) {
            return false;
        }
        if (this == obj) {
            return true;
        }
        return zzbg.equal(((zzb) obj).uri, this.uri);
    }

    public final int hashCode() {
        return Arrays.hashCode(new Object[]{this.uri});
    }
}
