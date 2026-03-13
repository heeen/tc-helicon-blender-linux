package com.google.android.gms.internal;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import javax.net.ssl.SSLSocketFactory;

/* JADX INFO: loaded from: classes.dex */
public final class zzas extends zzai {
    private final zzat zzcg;
    private final SSLSocketFactory zzch;

    public zzas() {
        this(null);
    }

    private zzas(zzat zzatVar) {
        this(null, null);
    }

    private zzas(zzat zzatVar, SSLSocketFactory sSLSocketFactory) {
        this.zzcg = null;
        this.zzch = null;
    }

    private static InputStream zza(HttpURLConnection httpURLConnection) {
        try {
            return httpURLConnection.getInputStream();
        } catch (IOException unused) {
            return httpURLConnection.getErrorStream();
        }
    }

    private static void zza(HttpURLConnection httpURLConnection, zzr<?> zzrVar) throws zza, IOException {
        byte[] bArrZzf = zzrVar.zzf();
        if (bArrZzf != null) {
            httpURLConnection.setDoOutput(true);
            String strValueOf = String.valueOf("UTF-8");
            httpURLConnection.addRequestProperty("Content-Type", strValueOf.length() != 0 ? "application/x-www-form-urlencoded; charset=".concat(strValueOf) : new String("application/x-www-form-urlencoded; charset="));
            DataOutputStream dataOutputStream = new DataOutputStream(httpURLConnection.getOutputStream());
            dataOutputStream.write(bArrZzf);
            dataOutputStream.close();
        }
    }

    private static List<zzl> zzc(Map<String, List<String>> map) {
        ArrayList arrayList = new ArrayList(map.size());
        for (Map.Entry<String, List<String>> entry : map.entrySet()) {
            if (entry.getKey() != null) {
                Iterator<String> it = entry.getValue().iterator();
                while (it.hasNext()) {
                    arrayList.add(new zzl(entry.getKey(), it.next()));
                }
            }
        }
        return arrayList;
    }

    /* JADX WARN: Removed duplicated region for block: B:33:0x00be  */
    /* JADX WARN: Removed duplicated region for block: B:49:0x00fc  */
    @Override // com.google.android.gms.internal.zzai
    /*
        Code decompiled incorrectly, please refer to instructions dump.
        To view partially-correct add '--show-bad-code' argument
    */
    public final com.google.android.gms.internal.zzaq zza(com.google.android.gms.internal.zzr<?> r6, java.util.Map<java.lang.String, java.lang.String> r7) throws com.google.android.gms.internal.zza, java.io.IOException {
        /*
            Method dump skipped, instruction units count: 282
            To view this dump add '--comments-level debug' option
        */
        throw new UnsupportedOperationException("Method not decompiled: com.google.android.gms.internal.zzas.zza(com.google.android.gms.internal.zzr, java.util.Map):com.google.android.gms.internal.zzaq");
    }
}
