package com.google.android.gms.internal;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Map;
import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.ProtocolVersion;
import org.apache.http.entity.BasicHttpEntity;
import org.apache.http.message.BasicHeader;
import org.apache.http.message.BasicHttpResponse;
import org.apache.http.message.BasicStatusLine;

/* JADX INFO: loaded from: classes.dex */
public abstract class zzai implements zzar {
    public abstract zzaq zza(zzr<?> zzrVar, Map<String, String> map) throws zza, IOException;

    @Override // com.google.android.gms.internal.zzar
    @Deprecated
    public final HttpResponse zzb(zzr<?> zzrVar, Map<String, String> map) throws zza, IOException {
        zzaq zzaqVarZza = zza(zzrVar, map);
        BasicHttpResponse basicHttpResponse = new BasicHttpResponse(new BasicStatusLine(new ProtocolVersion("HTTP", 1, 1), zzaqVarZza.getStatusCode(), ""));
        ArrayList arrayList = new ArrayList();
        for (zzl zzlVar : zzaqVarZza.zzp()) {
            arrayList.add(new BasicHeader(zzlVar.getName(), zzlVar.getValue()));
        }
        basicHttpResponse.setHeaders((Header[]) arrayList.toArray(new Header[arrayList.size()]));
        InputStream content = zzaqVarZza.getContent();
        if (content != null) {
            BasicHttpEntity basicHttpEntity = new BasicHttpEntity();
            basicHttpEntity.setContent(content);
            basicHttpEntity.setContentLength(zzaqVarZza.getContentLength());
            basicHttpResponse.setEntity(basicHttpEntity);
        }
        return basicHttpResponse;
    }
}
