package com.google.android.gms.common.api.internal;

import android.support.annotation.NonNull;
import android.support.v4.util.ArrayMap;
import android.util.Log;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.AvailabilityException;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import java.util.Collections;
import java.util.Iterator;
import java.util.Map;

/* JADX INFO: loaded from: classes.dex */
final class zzac implements OnCompleteListener<Map<zzh<?>, String>> {
    private /* synthetic */ zzaa zzfwu;

    private zzac(zzaa zzaaVar) {
        this.zzfwu = zzaaVar;
    }

    @Override // com.google.android.gms.tasks.OnCompleteListener
    public final void onComplete(@NonNull Task<Map<zzh<?>, String>> task) {
        zzaa zzaaVar;
        ConnectionResult connectionResult;
        Map map;
        this.zzfwu.zzfwa.lock();
        try {
            if (this.zzfwu.zzfwp) {
                if (task.isSuccessful()) {
                    this.zzfwu.zzfwq = new ArrayMap(this.zzfwu.zzfwg.size());
                    Iterator it = this.zzfwu.zzfwg.values().iterator();
                    while (it.hasNext()) {
                        this.zzfwu.zzfwq.put(((zzz) it.next()).zzahv(), ConnectionResult.zzfqt);
                    }
                } else {
                    if (task.getException() instanceof AvailabilityException) {
                        AvailabilityException availabilityException = (AvailabilityException) task.getException();
                        if (this.zzfwu.zzfwn) {
                            this.zzfwu.zzfwq = new ArrayMap(this.zzfwu.zzfwg.size());
                            for (zzz zzzVar : this.zzfwu.zzfwg.values()) {
                                Object objZzahv = zzzVar.zzahv();
                                ConnectionResult connectionResult2 = availabilityException.getConnectionResult(zzzVar);
                                if (this.zzfwu.zza((zzz<?>) zzzVar, connectionResult2)) {
                                    map = this.zzfwu.zzfwq;
                                    connectionResult2 = new ConnectionResult(16);
                                } else {
                                    map = this.zzfwu.zzfwq;
                                }
                                map.put(objZzahv, connectionResult2);
                            }
                        } else {
                            this.zzfwu.zzfwq = availabilityException.zzahr();
                        }
                        zzaaVar = this.zzfwu;
                        connectionResult = this.zzfwu.zzajb();
                    } else {
                        Log.e("ConnectionlessGAC", "Unexpected availability exception", task.getException());
                        this.zzfwu.zzfwq = Collections.emptyMap();
                        zzaaVar = this.zzfwu;
                        connectionResult = new ConnectionResult(8);
                    }
                    zzaaVar.zzfwt = connectionResult;
                }
                if (this.zzfwu.zzfwr != null) {
                    this.zzfwu.zzfwq.putAll(this.zzfwu.zzfwr);
                    this.zzfwu.zzfwt = this.zzfwu.zzajb();
                }
                if (this.zzfwu.zzfwt == null) {
                    this.zzfwu.zzaiz();
                    this.zzfwu.zzaja();
                } else {
                    zzaa.zza(this.zzfwu, false);
                    this.zzfwu.zzfwj.zzc(this.zzfwu.zzfwt);
                }
                this.zzfwu.zzfwl.signalAll();
            }
        } finally {
            this.zzfwu.zzfwa.unlock();
        }
    }
}
