package com.google.android.gms.internal;

import java.io.PrintStream;
import java.io.PrintWriter;

/* JADX INFO: loaded from: classes.dex */
final class zzdyv extends zzdyr {
    zzdyv() {
    }

    @Override // com.google.android.gms.internal.zzdyr
    public final void zza(Throwable th, PrintStream printStream) {
        th.printStackTrace(printStream);
    }

    @Override // com.google.android.gms.internal.zzdyr
    public final void zza(Throwable th, PrintWriter printWriter) {
        th.printStackTrace(printWriter);
    }
}
