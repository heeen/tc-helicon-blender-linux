package com.google.android.gms.internal;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.Map;
import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.conn.ConnectTimeoutException;

/* JADX INFO: loaded from: classes.dex */
final class zzah extends zzai {
    private final zzar zzbo;

    zzah(zzar zzarVar) {
        this.zzbo = zzarVar;
    }

    @Override // com.google.android.gms.internal.zzai
    public final zzaq zza(zzr<?> zzrVar, Map<String, String> map) throws zza, IOException {
        try {
            HttpResponse httpResponseZzb = this.zzbo.zzb(zzrVar, map);
            int statusCode = httpResponseZzb.getStatusLine().getStatusCode();
            Header[] allHeaders = httpResponseZzb.getAllHeaders();
            ArrayList arrayList = new ArrayList(allHeaders.length);
            for (Header header : allHeaders) {
                arrayList.add(new zzl(header.getName(), header.getValue()));
            }
            if (httpResponseZzb.getEntity() == null) {
                return new zzaq(statusCode, arrayList);
            }
            long contentLength = httpResponseZzb.getEntity().getContentLength();
            if (((int) contentLength) == contentLength) {
                return new zzaq(statusCode, arrayList, (int) httpResponseZzb.getEntity().getContentLength(), httpResponseZzb.getEntity().getContent());
            }
            StringBuilder sb = new StringBuilder(40);
            sb.append("Response too large: ");
            sb.append(contentLength);
            throw new IOException(sb.toString());
        } catch (ConnectTimeoutException e) {
            throw new SocketTimeoutException(e.getMessage());
        }
    }
}
