package com.google.android.gms.internal;

import java.io.IOException;
import java.io.OutputStream;

/* JADX INFO: loaded from: classes.dex */
public interface zzfjc extends zzfje {
    byte[] toByteArray();

    zzfgs toByteString();

    void writeTo(OutputStream outputStream) throws IOException;

    void zza(zzfhg zzfhgVar) throws IOException;

    zzfjl<? extends zzfjc> zzczq();

    zzfjd zzczt();

    int zzhs();
}
