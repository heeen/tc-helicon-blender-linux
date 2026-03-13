package com.google.android.gms.common.api;

import android.support.v4.util.ArrayMap;
import android.text.TextUtils;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.Api;
import com.google.android.gms.common.api.internal.zzh;
import com.google.android.gms.common.internal.Hide;
import com.google.android.gms.common.internal.zzbq;
import java.util.ArrayList;

/* JADX INFO: loaded from: classes.dex */
public class AvailabilityException extends Exception {
    private final ArrayMap<zzh<?>, ConnectionResult> zzfse;

    @Hide
    public AvailabilityException(ArrayMap<zzh<?>, ConnectionResult> arrayMap) {
        this.zzfse = arrayMap;
    }

    public ConnectionResult getConnectionResult(GoogleApi<? extends Api.ApiOptions> googleApi) {
        Object objZzahv = googleApi.zzahv();
        zzbq.checkArgument(this.zzfse.get(objZzahv) != null, "The given API was not part of the availability request.");
        return this.zzfse.get(objZzahv);
    }

    @Override // java.lang.Throwable
    public String getMessage() {
        ArrayList arrayList = new ArrayList();
        boolean z = true;
        for (zzh<?> zzhVar : this.zzfse.keySet()) {
            ConnectionResult connectionResult = this.zzfse.get(zzhVar);
            if (connectionResult.isSuccess()) {
                z = false;
            }
            String strZzaig = zzhVar.zzaig();
            String strValueOf = String.valueOf(connectionResult);
            StringBuilder sb = new StringBuilder(String.valueOf(strZzaig).length() + 2 + String.valueOf(strValueOf).length());
            sb.append(strZzaig);
            sb.append(": ");
            sb.append(strValueOf);
            arrayList.add(sb.toString());
        }
        StringBuilder sb2 = new StringBuilder();
        sb2.append(z ? "None of the queried APIs are available. " : "Some of the queried APIs are unavailable. ");
        sb2.append(TextUtils.join("; ", arrayList));
        return sb2.toString();
    }

    @Hide
    public final ArrayMap<zzh<?>, ConnectionResult> zzahr() {
        return this.zzfse;
    }
}
