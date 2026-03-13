package com.google.android.gms.common.api.internal;

import android.content.Context;
import android.os.Bundle;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.Api;
import com.google.android.gms.common.api.Result;
import com.google.android.gms.internal.zzcyj;
import com.google.android.gms.internal.zzcyk;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;

/* JADX INFO: loaded from: classes.dex */
public final class zzbi implements zzcc, zzu {
    private final Context mContext;
    private Api.zza<? extends zzcyj, zzcyk> zzfth;
    final zzba zzfvq;
    private final Lock zzfwa;
    private com.google.android.gms.common.internal.zzr zzfwf;
    private Map<Api<?>, Boolean> zzfwi;
    private final com.google.android.gms.common.zzf zzfwk;
    final Map<Api.zzc<?>, Api.zze> zzfyj;
    private final Condition zzfyw;
    private final zzbk zzfyx;
    private volatile zzbh zzfyz;
    int zzfzb;
    final zzcd zzfzc;
    final Map<Api.zzc<?>, ConnectionResult> zzfyy = new HashMap();
    private ConnectionResult zzfza = null;

    public zzbi(Context context, zzba zzbaVar, Lock lock, Looper looper, com.google.android.gms.common.zzf zzfVar, Map<Api.zzc<?>, Api.zze> map, com.google.android.gms.common.internal.zzr zzrVar, Map<Api<?>, Boolean> map2, Api.zza<? extends zzcyj, zzcyk> zzaVar, ArrayList<zzt> arrayList, zzcd zzcdVar) {
        this.mContext = context;
        this.zzfwa = lock;
        this.zzfwk = zzfVar;
        this.zzfyj = map;
        this.zzfwf = zzrVar;
        this.zzfwi = map2;
        this.zzfth = zzaVar;
        this.zzfvq = zzbaVar;
        this.zzfzc = zzcdVar;
        ArrayList<zzt> arrayList2 = arrayList;
        int size = arrayList2.size();
        int i = 0;
        while (i < size) {
            zzt zztVar = arrayList2.get(i);
            i++;
            zztVar.zza(this);
        }
        this.zzfyx = new zzbk(this, looper);
        this.zzfyw = lock.newCondition();
        this.zzfyz = new zzaz(this);
    }

    @Override // com.google.android.gms.common.api.internal.zzcc
    public final ConnectionResult blockingConnect() {
        connect();
        while (isConnecting()) {
            try {
                this.zzfyw.await();
            } catch (InterruptedException unused) {
                Thread.currentThread().interrupt();
                return new ConnectionResult(15, null);
            }
        }
        return isConnected() ? ConnectionResult.zzfqt : this.zzfza != null ? this.zzfza : new ConnectionResult(13, null);
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
                nanos = this.zzfyw.awaitNanos(nanos);
            } catch (InterruptedException unused) {
                Thread.currentThread().interrupt();
                return new ConnectionResult(15, null);
            }
            Thread.currentThread().interrupt();
            return new ConnectionResult(15, null);
        }
        return isConnected() ? ConnectionResult.zzfqt : this.zzfza != null ? this.zzfza : new ConnectionResult(13, null);
    }

    @Override // com.google.android.gms.common.api.internal.zzcc
    public final void connect() {
        this.zzfyz.connect();
    }

    @Override // com.google.android.gms.common.api.internal.zzcc
    public final void disconnect() {
        if (this.zzfyz.disconnect()) {
            this.zzfyy.clear();
        }
    }

    @Override // com.google.android.gms.common.api.internal.zzcc
    public final void dump(String str, FileDescriptor fileDescriptor, PrintWriter printWriter, String[] strArr) {
        String strConcat = String.valueOf(str).concat("  ");
        printWriter.append((CharSequence) str).append("mState=").println(this.zzfyz);
        for (Api<?> api : this.zzfwi.keySet()) {
            printWriter.append((CharSequence) str).append((CharSequence) api.getName()).println(":");
            this.zzfyj.get(api.zzahm()).dump(strConcat, fileDescriptor, printWriter, strArr);
        }
    }

    @Override // com.google.android.gms.common.api.internal.zzcc
    @Nullable
    public final ConnectionResult getConnectionResult(@NonNull Api<?> api) {
        Api.zzc<?> zzcVarZzahm = api.zzahm();
        if (!this.zzfyj.containsKey(zzcVarZzahm)) {
            return null;
        }
        if (this.zzfyj.get(zzcVarZzahm).isConnected()) {
            return ConnectionResult.zzfqt;
        }
        if (this.zzfyy.containsKey(zzcVarZzahm)) {
            return this.zzfyy.get(zzcVarZzahm);
        }
        return null;
    }

    @Override // com.google.android.gms.common.api.internal.zzcc
    public final boolean isConnected() {
        return this.zzfyz instanceof zzal;
    }

    @Override // com.google.android.gms.common.api.internal.zzcc
    public final boolean isConnecting() {
        return this.zzfyz instanceof zzao;
    }

    @Override // com.google.android.gms.common.api.GoogleApiClient.ConnectionCallbacks
    public final void onConnected(@Nullable Bundle bundle) {
        this.zzfwa.lock();
        try {
            this.zzfyz.onConnected(bundle);
        } finally {
            this.zzfwa.unlock();
        }
    }

    @Override // com.google.android.gms.common.api.GoogleApiClient.ConnectionCallbacks
    public final void onConnectionSuspended(int i) {
        this.zzfwa.lock();
        try {
            this.zzfyz.onConnectionSuspended(i);
        } finally {
            this.zzfwa.unlock();
        }
    }

    @Override // com.google.android.gms.common.api.internal.zzu
    public final void zza(@NonNull ConnectionResult connectionResult, @NonNull Api<?> api, boolean z) {
        this.zzfwa.lock();
        try {
            this.zzfyz.zza(connectionResult, api, z);
        } finally {
            this.zzfwa.unlock();
        }
    }

    final void zza(zzbj zzbjVar) {
        this.zzfyx.sendMessage(this.zzfyx.obtainMessage(1, zzbjVar));
    }

    @Override // com.google.android.gms.common.api.internal.zzcc
    public final boolean zza(zzcu zzcuVar) {
        return false;
    }

    @Override // com.google.android.gms.common.api.internal.zzcc
    public final void zzaia() {
    }

    @Override // com.google.android.gms.common.api.internal.zzcc
    public final void zzais() {
        if (isConnected()) {
            ((zzal) this.zzfyz).zzaji();
        }
    }

    final void zzajv() {
        this.zzfwa.lock();
        try {
            this.zzfyz = new zzao(this, this.zzfwf, this.zzfwi, this.zzfwk, this.zzfth, this.zzfwa, this.mContext);
            this.zzfyz.begin();
            this.zzfyw.signalAll();
        } finally {
            this.zzfwa.unlock();
        }
    }

    final void zzajw() {
        this.zzfwa.lock();
        try {
            this.zzfvq.zzajs();
            this.zzfyz = new zzal(this);
            this.zzfyz.begin();
            this.zzfyw.signalAll();
        } finally {
            this.zzfwa.unlock();
        }
    }

    final void zzb(RuntimeException runtimeException) {
        this.zzfyx.sendMessage(this.zzfyx.obtainMessage(2, runtimeException));
    }

    @Override // com.google.android.gms.common.api.internal.zzcc
    public final <A extends Api.zzb, R extends Result, T extends zzm<R, A>> T zzd(@NonNull T t) {
        t.zzaiq();
        return (T) this.zzfyz.zzd(t);
    }

    @Override // com.google.android.gms.common.api.internal.zzcc
    public final <A extends Api.zzb, T extends zzm<? extends Result, A>> T zze(@NonNull T t) {
        t.zzaiq();
        return (T) this.zzfyz.zze(t);
    }

    final void zzg(ConnectionResult connectionResult) {
        this.zzfwa.lock();
        try {
            this.zzfza = connectionResult;
            this.zzfyz = new zzaz(this);
            this.zzfyz.begin();
            this.zzfyw.signalAll();
        } finally {
            this.zzfwa.unlock();
        }
    }
}
