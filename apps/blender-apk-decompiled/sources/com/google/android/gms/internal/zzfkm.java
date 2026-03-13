package com.google.android.gms.internal;

import java.util.List;

/* JADX INFO: loaded from: classes.dex */
public final class zzfkm extends RuntimeException {
    private final List<String> zzpsz;

    public zzfkm(zzfjc zzfjcVar) {
        super("Message was missing required fields.  (Lite runtime could not determine which fields were missing).");
        this.zzpsz = null;
    }

    public final zzfie zzdbz() {
        return new zzfie(getMessage());
    }
}
