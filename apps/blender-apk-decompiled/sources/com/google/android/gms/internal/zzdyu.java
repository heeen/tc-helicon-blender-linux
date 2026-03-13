package com.google.android.gms.internal;

import java.io.PrintStream;
import java.io.PrintWriter;
import java.util.List;

/* JADX INFO: loaded from: classes.dex */
final class zzdyu extends zzdyr {
    private final zzdys zzmml = new zzdys();

    zzdyu() {
    }

    @Override // com.google.android.gms.internal.zzdyr
    public final void zza(Throwable th, PrintStream printStream) {
        th.printStackTrace(printStream);
        List<Throwable> listZza = this.zzmml.zza(th, false);
        if (listZza == null) {
            return;
        }
        synchronized (listZza) {
            for (Throwable th2 : listZza) {
                printStream.print("Suppressed: ");
                th2.printStackTrace(printStream);
            }
        }
    }

    @Override // com.google.android.gms.internal.zzdyr
    public final void zza(Throwable th, PrintWriter printWriter) {
        th.printStackTrace(printWriter);
        List<Throwable> listZza = this.zzmml.zza(th, false);
        if (listZza == null) {
            return;
        }
        synchronized (listZza) {
            for (Throwable th2 : listZza) {
                printWriter.print("Suppressed: ");
                th2.printStackTrace(printWriter);
            }
        }
    }
}
