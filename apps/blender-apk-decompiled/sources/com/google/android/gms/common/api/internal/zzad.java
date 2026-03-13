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
final class zzad implements OnCompleteListener<Map<zzh<?>, String>> {
    private /* synthetic */ zzaa zzfwu;
    private zzcu zzfwv;

    zzad(zzaa zzaaVar, zzcu zzcuVar) {
        this.zzfwu = zzaaVar;
        this.zzfwv = zzcuVar;
    }

    final void cancel() {
        this.zzfwv.zzacm();
    }

    @Override // com.google.android.gms.tasks.OnCompleteListener
    public final void onComplete(@NonNull Task<Map<zzh<?>, String>> task) {
        Map map;
        zzcu zzcuVar;
        this.zzfwu.zzfwa.lock();
        try {
            if (this.zzfwu.zzfwp) {
                if (task.isSuccessful()) {
                    this.zzfwu.zzfwr = new ArrayMap(this.zzfwu.zzfwh.size());
                    Iterator it = this.zzfwu.zzfwh.values().iterator();
                    while (it.hasNext()) {
                        this.zzfwu.zzfwr.put(((zzz) it.next()).zzahv(), ConnectionResult.zzfqt);
                    }
                } else if (task.getException() instanceof AvailabilityException) {
                    AvailabilityException availabilityException = (AvailabilityException) task.getException();
                    if (this.zzfwu.zzfwn) {
                        this.zzfwu.zzfwr = new ArrayMap(this.zzfwu.zzfwh.size());
                        for (zzz zzzVar : this.zzfwu.zzfwh.values()) {
                            Object objZzahv = zzzVar.zzahv();
                            ConnectionResult connectionResult = availabilityException.getConnectionResult(zzzVar);
                            if (this.zzfwu.zza((zzz<?>) zzzVar, connectionResult)) {
                                map = this.zzfwu.zzfwr;
                                connectionResult = new ConnectionResult(16);
                            } else {
                                map = this.zzfwu.zzfwr;
                            }
                            map.put(objZzahv, connectionResult);
                        }
                    } else {
                        this.zzfwu.zzfwr = availabilityException.zzahr();
                    }
                } else {
                    Log.e("ConnectionlessGAC", "Unexpected availability exception", task.getException());
                    this.zzfwu.zzfwr = Collections.emptyMap();
                }
                if (this.zzfwu.isConnected()) {
                    this.zzfwu.zzfwq.putAll(this.zzfwu.zzfwr);
                    if (this.zzfwu.zzajb() == null) {
                        this.zzfwu.zzaiz();
                        this.zzfwu.zzaja();
                        this.zzfwu.zzfwl.signalAll();
                    }
                }
                zzcuVar = this.zzfwv;
            } else {
                zzcuVar = this.zzfwv;
            }
            zzcuVar.zzacm();
        } finally {
            this.zzfwu.zzfwa.unlock();
        }
    }
}
