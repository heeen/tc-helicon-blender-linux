package com.google.android.gms.common.api.internal;

import android.content.Context;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.util.ArrayMap;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.Api;
import com.google.android.gms.common.api.Result;
import com.google.android.gms.common.api.Scope;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.internal.zzbic;
import com.google.android.gms.internal.zzcyj;
import com.google.android.gms.internal.zzcyk;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;

/* JADX INFO: loaded from: classes.dex */
public final class zzaa implements zzcc {
    private final Looper zzalj;
    private final zzbm zzfsq;
    private final Lock zzfwa;
    private final com.google.android.gms.common.internal.zzr zzfwf;
    private final Map<Api<?>, Boolean> zzfwi;
    private final zzba zzfwj;
    private final com.google.android.gms.common.zzf zzfwk;
    private final Condition zzfwl;
    private final boolean zzfwm;
    private final boolean zzfwn;
    private boolean zzfwp;
    private Map<zzh<?>, ConnectionResult> zzfwq;
    private Map<zzh<?>, ConnectionResult> zzfwr;
    private zzad zzfws;
    private ConnectionResult zzfwt;
    private final Map<Api.zzc<?>, zzz<?>> zzfwg = new HashMap();
    private final Map<Api.zzc<?>, zzz<?>> zzfwh = new HashMap();
    private final Queue<zzm<?, ?>> zzfwo = new LinkedList();

    public zzaa(Context context, Lock lock, Looper looper, com.google.android.gms.common.zzf zzfVar, Map<Api.zzc<?>, Api.zze> map, com.google.android.gms.common.internal.zzr zzrVar, Map<Api<?>, Boolean> map2, Api.zza<? extends zzcyj, zzcyk> zzaVar, ArrayList<zzt> arrayList, zzba zzbaVar, boolean z) {
        boolean z2;
        boolean z3;
        boolean z4;
        this.zzfwa = lock;
        this.zzalj = looper;
        this.zzfwl = lock.newCondition();
        this.zzfwk = zzfVar;
        this.zzfwj = zzbaVar;
        this.zzfwi = map2;
        this.zzfwf = zzrVar;
        this.zzfwm = z;
        HashMap map3 = new HashMap();
        for (Api<?> api : map2.keySet()) {
            map3.put(api.zzahm(), api);
        }
        HashMap map4 = new HashMap();
        ArrayList<zzt> arrayList2 = arrayList;
        int size = arrayList2.size();
        int i = 0;
        while (i < size) {
            zzt zztVar = arrayList2.get(i);
            i++;
            zzt zztVar2 = zztVar;
            map4.put(zztVar2.zzfop, zztVar2);
        }
        boolean z5 = true;
        boolean z6 = false;
        boolean z7 = false;
        for (Map.Entry<Api.zzc<?>, Api.zze> entry : map.entrySet()) {
            Api api2 = (Api) map3.get(entry.getKey());
            Api.zze value = entry.getValue();
            if (!value.zzahn()) {
                z2 = z6;
                z3 = z7;
                z4 = false;
            } else if (this.zzfwi.get(api2).booleanValue()) {
                z4 = z5;
                z3 = z7;
                z2 = true;
            } else {
                z4 = z5;
                z3 = true;
                z2 = true;
            }
            zzz<?> zzzVar = new zzz<>(context, api2, looper, value, (zzt) map4.get(api2), zzrVar, zzaVar);
            this.zzfwg.put(entry.getKey(), zzzVar);
            if (value.zzacc()) {
                this.zzfwh.put(entry.getKey(), zzzVar);
            }
            z7 = z3;
            z5 = z4;
            z6 = z2;
        }
        this.zzfwn = (!z6 || z5 || z7) ? false : true;
        this.zzfsq = zzbm.zzajy();
    }

    static /* synthetic */ boolean zza(zzaa zzaaVar, boolean z) {
        zzaaVar.zzfwp = false;
        return false;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public final boolean zza(zzz<?> zzzVar, ConnectionResult connectionResult) {
        return !connectionResult.isSuccess() && !connectionResult.hasResolution() && this.zzfwi.get(zzzVar.zzaht()).booleanValue() && zzzVar.zzaix().zzahn() && this.zzfwk.isUserResolvableError(connectionResult.getErrorCode());
    }

    private final boolean zzaiy() {
        this.zzfwa.lock();
        try {
            if (this.zzfwp && this.zzfwm) {
                Iterator<Api.zzc<?>> it = this.zzfwh.keySet().iterator();
                while (it.hasNext()) {
                    ConnectionResult connectionResultZzb = zzb(it.next());
                    if (connectionResultZzb == null || !connectionResultZzb.isSuccess()) {
                    }
                }
                this.zzfwa.unlock();
                return true;
            }
            return false;
        } finally {
            this.zzfwa.unlock();
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public final void zzaiz() {
        Set<Scope> hashSet;
        zzba zzbaVar;
        if (this.zzfwf == null) {
            zzbaVar = this.zzfwj;
            hashSet = Collections.emptySet();
        } else {
            hashSet = new HashSet<>(this.zzfwf.zzamf());
            Map<Api<?>, com.google.android.gms.common.internal.zzt> mapZzamh = this.zzfwf.zzamh();
            for (Api<?> api : mapZzamh.keySet()) {
                ConnectionResult connectionResult = getConnectionResult(api);
                if (connectionResult != null && connectionResult.isSuccess()) {
                    hashSet.addAll(mapZzamh.get(api).zzenh);
                }
            }
            zzbaVar = this.zzfwj;
        }
        zzbaVar.zzfyk = hashSet;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public final void zzaja() {
        while (!this.zzfwo.isEmpty()) {
            zze(this.zzfwo.remove());
        }
        this.zzfwj.zzk(null);
    }

    /* JADX INFO: Access modifiers changed from: private */
    @Nullable
    public final ConnectionResult zzajb() {
        ConnectionResult connectionResult = null;
        int i = 0;
        int i2 = 0;
        ConnectionResult connectionResult2 = null;
        for (zzz<?> zzzVar : this.zzfwg.values()) {
            Api<?> apiZzaht = zzzVar.zzaht();
            ConnectionResult connectionResult3 = this.zzfwq.get(zzzVar.zzahv());
            if (!connectionResult3.isSuccess() && (!this.zzfwi.get(apiZzaht).booleanValue() || connectionResult3.hasResolution() || this.zzfwk.isUserResolvableError(connectionResult3.getErrorCode()))) {
                if (connectionResult3.getErrorCode() == 4 && this.zzfwm) {
                    int priority = apiZzaht.zzahk().getPriority();
                    if (connectionResult2 == null || i2 > priority) {
                        connectionResult2 = connectionResult3;
                        i2 = priority;
                    }
                } else {
                    int priority2 = apiZzaht.zzahk().getPriority();
                    if (connectionResult == null || i > priority2) {
                        connectionResult = connectionResult3;
                        i = priority2;
                    }
                }
            }
        }
        return (connectionResult == null || connectionResult2 == null || i <= i2) ? connectionResult : connectionResult2;
    }

    @Nullable
    private final ConnectionResult zzb(@NonNull Api.zzc<?> zzcVar) {
        this.zzfwa.lock();
        try {
            zzz<?> zzzVar = this.zzfwg.get(zzcVar);
            if (this.zzfwq != null && zzzVar != null) {
                return this.zzfwq.get(zzzVar.zzahv());
            }
            this.zzfwa.unlock();
            return null;
        } finally {
            this.zzfwa.unlock();
        }
    }

    private final <T extends zzm<? extends Result, ? extends Api.zzb>> boolean zzg(@NonNull T t) {
        Api.zzc<?> zzcVarZzahm = t.zzahm();
        ConnectionResult connectionResultZzb = zzb(zzcVarZzahm);
        if (connectionResultZzb == null || connectionResultZzb.getErrorCode() != 4) {
            return false;
        }
        t.zzu(new Status(4, null, this.zzfsq.zza(this.zzfwg.get(zzcVarZzahm).zzahv(), System.identityHashCode(this.zzfwj))));
        return true;
    }

    @Override // com.google.android.gms.common.api.internal.zzcc
    public final ConnectionResult blockingConnect() {
        connect();
        while (isConnecting()) {
            try {
                this.zzfwl.await();
            } catch (InterruptedException unused) {
                Thread.currentThread().interrupt();
                return new ConnectionResult(15, null);
            }
        }
        return isConnected() ? ConnectionResult.zzfqt : this.zzfwt != null ? this.zzfwt : new ConnectionResult(13, null);
    }

    @Override // com.google.android.gms.common.api.internal.zzcc
    public final ConnectionResult blockingConnect(long j, TimeUnit timeUnit) {
        connect();
        long nanos = timeUnit.toNanos(j);
        while (isConnecting()) {
            if (nanos <= 0) {
                disconnect();
                return new ConnectionResult(14, null);
            }
            try {
                nanos = this.zzfwl.awaitNanos(nanos);
            } catch (InterruptedException unused) {
                Thread.currentThread().interrupt();
                return new ConnectionResult(15, null);
            }
            Thread.currentThread().interrupt();
            return new ConnectionResult(15, null);
        }
        return isConnected() ? ConnectionResult.zzfqt : this.zzfwt != null ? this.zzfwt : new ConnectionResult(13, null);
    }

    @Override // com.google.android.gms.common.api.internal.zzcc
    public final void connect() {
        this.zzfwa.lock();
        try {
            if (!this.zzfwp) {
                this.zzfwp = true;
                this.zzfwq = null;
                this.zzfwr = null;
                this.zzfws = null;
                this.zzfwt = null;
                this.zzfsq.zzaih();
                this.zzfsq.zza(this.zzfwg.values()).addOnCompleteListener(new zzbic(this.zzalj), new zzac(this));
            }
        } finally {
            this.zzfwa.unlock();
        }
    }

    @Override // com.google.android.gms.common.api.internal.zzcc
    public final void disconnect() {
        this.zzfwa.lock();
        try {
            this.zzfwp = false;
            this.zzfwq = null;
            this.zzfwr = null;
            if (this.zzfws != null) {
                this.zzfws.cancel();
                this.zzfws = null;
            }
            this.zzfwt = null;
            while (!this.zzfwo.isEmpty()) {
                zzm<?, ?> zzmVarRemove = this.zzfwo.remove();
                zzmVarRemove.zza((zzdn) null);
                zzmVarRemove.cancel();
            }
            this.zzfwl.signalAll();
        } finally {
            this.zzfwa.unlock();
        }
    }

    @Override // com.google.android.gms.common.api.internal.zzcc
    public final void dump(String str, FileDescriptor fileDescriptor, PrintWriter printWriter, String[] strArr) {
    }

    @Override // com.google.android.gms.common.api.internal.zzcc
    @Nullable
    public final ConnectionResult getConnectionResult(@NonNull Api<?> api) {
        return zzb(api.zzahm());
    }

    /* JADX WARN: Removed duplicated region for block: B:8:0x000f  */
    @Override // com.google.android.gms.common.api.internal.zzcc
    /*
        Code decompiled incorrectly, please refer to instructions dump.
        To view partially-correct add '--show-bad-code' argument
    */
    public final boolean isConnected() {
        /*
            r1 = this;
            java.util.concurrent.locks.Lock r0 = r1.zzfwa
            r0.lock()
            java.util.Map<com.google.android.gms.common.api.internal.zzh<?>, com.google.android.gms.common.ConnectionResult> r0 = r1.zzfwq     // Catch: java.lang.Throwable -> L16
            if (r0 == 0) goto Lf
            com.google.android.gms.common.ConnectionResult r0 = r1.zzfwt     // Catch: java.lang.Throwable -> L16
            if (r0 != 0) goto Lf
            r0 = 1
            goto L10
        Lf:
            r0 = 0
        L10:
            java.util.concurrent.locks.Lock r1 = r1.zzfwa
            r1.unlock()
            return r0
        L16:
            r0 = move-exception
            java.util.concurrent.locks.Lock r1 = r1.zzfwa
            r1.unlock()
            throw r0
        */
        throw new UnsupportedOperationException("Method not decompiled: com.google.android.gms.common.api.internal.zzaa.isConnected():boolean");
    }

    /* JADX WARN: Removed duplicated region for block: B:8:0x000f  */
    @Override // com.google.android.gms.common.api.internal.zzcc
    /*
        Code decompiled incorrectly, please refer to instructions dump.
        To view partially-correct add '--show-bad-code' argument
    */
    public final boolean isConnecting() {
        /*
            r1 = this;
            java.util.concurrent.locks.Lock r0 = r1.zzfwa
            r0.lock()
            java.util.Map<com.google.android.gms.common.api.internal.zzh<?>, com.google.android.gms.common.ConnectionResult> r0 = r1.zzfwq     // Catch: java.lang.Throwable -> L16
            if (r0 != 0) goto Lf
            boolean r0 = r1.zzfwp     // Catch: java.lang.Throwable -> L16
            if (r0 == 0) goto Lf
            r0 = 1
            goto L10
        Lf:
            r0 = 0
        L10:
            java.util.concurrent.locks.Lock r1 = r1.zzfwa
            r1.unlock()
            return r0
        L16:
            r0 = move-exception
            java.util.concurrent.locks.Lock r1 = r1.zzfwa
            r1.unlock()
            throw r0
        */
        throw new UnsupportedOperationException("Method not decompiled: com.google.android.gms.common.api.internal.zzaa.isConnecting():boolean");
    }

    @Override // com.google.android.gms.common.api.internal.zzcc
    public final boolean zza(zzcu zzcuVar) {
        this.zzfwa.lock();
        try {
            if (!this.zzfwp || zzaiy()) {
                this.zzfwa.unlock();
                return false;
            }
            this.zzfsq.zzaih();
            this.zzfws = new zzad(this, zzcuVar);
            this.zzfsq.zza(this.zzfwh.values()).addOnCompleteListener(new zzbic(this.zzalj), this.zzfws);
            this.zzfwa.unlock();
            return true;
        } catch (Throwable th) {
            this.zzfwa.unlock();
            throw th;
        }
    }

    @Override // com.google.android.gms.common.api.internal.zzcc
    public final void zzaia() {
        this.zzfwa.lock();
        try {
            this.zzfsq.zzaia();
            if (this.zzfws != null) {
                this.zzfws.cancel();
                this.zzfws = null;
            }
            if (this.zzfwr == null) {
                this.zzfwr = new ArrayMap(this.zzfwh.size());
            }
            ConnectionResult connectionResult = new ConnectionResult(4);
            Iterator<zzz<?>> it = this.zzfwh.values().iterator();
            while (it.hasNext()) {
                this.zzfwr.put(it.next().zzahv(), connectionResult);
            }
            if (this.zzfwq != null) {
                this.zzfwq.putAll(this.zzfwr);
            }
        } finally {
            this.zzfwa.unlock();
        }
    }

    @Override // com.google.android.gms.common.api.internal.zzcc
    public final void zzais() {
    }

    @Override // com.google.android.gms.common.api.internal.zzcc
    public final <A extends Api.zzb, R extends Result, T extends zzm<R, A>> T zzd(@NonNull T t) {
        if (this.zzfwm && zzg(t)) {
            return t;
        }
        if (isConnected()) {
            this.zzfwj.zzfyp.zzb(t);
            return (T) this.zzfwg.get(t.zzahm()).zza(t);
        }
        this.zzfwo.add(t);
        return t;
    }

    @Override // com.google.android.gms.common.api.internal.zzcc
    public final <A extends Api.zzb, T extends zzm<? extends Result, A>> T zze(@NonNull T t) {
        Api.zzc<A> zzcVarZzahm = t.zzahm();
        if (this.zzfwm && zzg(t)) {
            return t;
        }
        this.zzfwj.zzfyp.zzb(t);
        return (T) this.zzfwg.get(zzcVarZzahm).zzb(t);
    }
}
