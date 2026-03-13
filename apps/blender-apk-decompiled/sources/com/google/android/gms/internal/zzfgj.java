package com.google.android.gms.internal;

import com.google.android.gms.internal.zzfgj;
import com.google.android.gms.internal.zzfgk;
import java.io.IOException;
import java.io.OutputStream;
import java.util.List;

/* JADX INFO: loaded from: classes.dex */
public abstract class zzfgj<MessageType extends zzfgj<MessageType, BuilderType>, BuilderType extends zzfgk<MessageType, BuilderType>> implements zzfjc {
    private static boolean zzpnp = false;
    protected int zzpno = 0;

    protected static <T> void zza(Iterable<T> iterable, List<? super T> list) {
        zzfgk.zza(iterable, list);
    }

    @Override // com.google.android.gms.internal.zzfjc
    public final byte[] toByteArray() {
        try {
            byte[] bArr = new byte[zzhs()];
            zzfhg zzfhgVarZzbc = zzfhg.zzbc(bArr);
            zza(zzfhgVarZzbc);
            zzfhgVarZzbc.zzcyx();
            return bArr;
        } catch (IOException e) {
            String name = getClass().getName();
            StringBuilder sb = new StringBuilder(String.valueOf(name).length() + 62 + String.valueOf("byte array").length());
            sb.append("Serializing ");
            sb.append(name);
            sb.append(" to a ");
            sb.append("byte array");
            sb.append(" threw an IOException (should never happen).");
            throw new RuntimeException(sb.toString(), e);
        }
    }

    @Override // com.google.android.gms.internal.zzfjc
    public final zzfgs toByteString() {
        try {
            zzfgx zzfgxVarZzle = zzfgs.zzle(zzhs());
            zza(zzfgxVarZzle.zzcxw());
            return zzfgxVarZzle.zzcxv();
        } catch (IOException e) {
            String name = getClass().getName();
            StringBuilder sb = new StringBuilder(String.valueOf(name).length() + 62 + String.valueOf("ByteString").length());
            sb.append("Serializing ");
            sb.append(name);
            sb.append(" to a ");
            sb.append("ByteString");
            sb.append(" threw an IOException (should never happen).");
            throw new RuntimeException(sb.toString(), e);
        }
    }

    @Override // com.google.android.gms.internal.zzfjc
    public final void writeTo(OutputStream outputStream) throws IOException {
        zzfhg zzfhgVarZzb = zzfhg.zzb(outputStream, zzfhg.zzlr(zzhs()));
        zza(zzfhgVarZzb);
        zzfhgVarZzb.flush();
    }
}
