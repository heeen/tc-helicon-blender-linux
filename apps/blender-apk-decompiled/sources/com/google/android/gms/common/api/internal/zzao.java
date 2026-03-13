package com.google.android.gms.common.api.internal;

import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.Api;
import com.google.android.gms.common.api.Result;
import com.google.android.gms.common.api.Scope;
import com.google.android.gms.internal.zzcyj;
import com.google.android.gms.internal.zzcyk;
import com.google.android.gms.internal.zzcyw;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Future;
import java.util.concurrent.locks.Lock;

/* JADX INFO: loaded from: classes.dex */
public final class zzao implements zzbh {
    private final Context mContext;
    private final Api.zza<? extends zzcyj, zzcyk> zzfth;
    private final Lock zzfwa;
    private final com.google.android.gms.common.internal.zzr zzfwf;
    private final Map<Api<?>, Boolean> zzfwi;
    private final com.google.android.gms.common.zzf zzfwk;
    private ConnectionResult zzfwt;
    private final zzbi zzfxd;
    private int zzfxg;
    private int zzfxi;
    private zzcyj zzfxl;
    private boolean zzfxm;
    private boolean zzfxn;
    private boolean zzfxo;
    private com.google.android.gms.common.internal.zzan zzfxp;
    private boolean zzfxq;
    private boolean zzfxr;
    private int zzfxh = 0;
    private final Bundle zzfxj = new Bundle();
    private final Set<Api.zzc> zzfxk = new HashSet();
    private ArrayList<Future<?>> zzfxs = new ArrayList<>();

    public zzao(zzbi zzbiVar, com.google.android.gms.common.internal.zzr zzrVar, Map<Api<?>, Boolean> map, com.google.android.gms.common.zzf zzfVar, Api.zza<? extends zzcyj, zzcyk> zzaVar, Lock lock, Context context) {
        this.zzfxd = zzbiVar;
        this.zzfwf = zzrVar;
        this.zzfwi = map;
        this.zzfwk = zzfVar;
        this.zzfth = zzaVar;
        this.zzfwa = lock;
        this.mContext = context;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public final void zza(zzcyw zzcywVar) {
        if (zzbs(0)) {
            ConnectionResult connectionResultZzain = zzcywVar.zzain();
            if (!connectionResultZzain.isSuccess()) {
                if (!zzd(connectionResultZzain)) {
                    zze(connectionResultZzain);
                    return;
                } else {
                    zzajn();
                    zzajl();
                    return;
                }
            }
            com.google.android.gms.common.internal.zzbt zzbtVarZzbfa = zzcywVar.zzbfa();
            ConnectionResult connectionResultZzain2 = zzbtVarZzbfa.zzain();
            if (connectionResultZzain2.isSuccess()) {
                this.zzfxo = true;
                this.zzfxp = zzbtVarZzbfa.zzamy();
                this.zzfxq = zzbtVarZzbfa.zzamz();
                this.zzfxr = zzbtVarZzbfa.zzana();
                zzajl();
                return;
            }
            String strValueOf = String.valueOf(connectionResultZzain2);
            StringBuilder sb = new StringBuilder(String.valueOf(strValueOf).length() + 48);
            sb.append("Sign-in succeeded with resolve account failure: ");
            sb.append(strValueOf);
            Log.wtf("GoogleApiClientConnecting", sb.toString(), new Exception());
            zze(connectionResultZzain2);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public final boolean zzajk() {
        ConnectionResult connectionResult;
        this.zzfxi--;
        if (this.zzfxi > 0) {
            return false;
        }
        if (this.zzfxi < 0) {
            Log.w("GoogleApiClientConnecting", this.zzfxd.zzfvq.zzaju());
            Log.wtf("GoogleApiClientConnecting", "GoogleApiClient received too many callbacks for the given step. Clients may be in an unexpected state; GoogleApiClient will now disconnect.", new Exception());
            connectionResult = new ConnectionResult(8, null);
        } else {
            if (this.zzfwt == null) {
                return true;
            }
            this.zzfxd.zzfzb = this.zzfxg;
            connectionResult = this.zzfwt;
        }
        zze(connectionResult);
        return false;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public final void zzajl() {
        if (this.zzfxi != 0) {
            return;
        }
        if (!this.zzfxn || this.zzfxo) {
            ArrayList arrayList = new ArrayList();
            this.zzfxh = 1;
            this.zzfxi = this.zzfxd.zzfyj.size();
            for (Api.zzc<?> zzcVar : this.zzfxd.zzfyj.keySet()) {
                if (!this.zzfxd.zzfyy.containsKey(zzcVar)) {
                    arrayList.add(this.zzfxd.zzfyj.get(zzcVar));
                } else if (zzajk()) {
                    zzajm();
                }
            }
            if (arrayList.isEmpty()) {
                return;
            }
            this.zzfxs.add(zzbl.zzajx().submit(new zzau(this, arrayList)));
        }
    }

    private final void zzajm() {
        this.zzfxd.zzajw();
        zzbl.zzajx().execute(new zzap(this));
        if (this.zzfxl != null) {
            if (this.zzfxq) {
                this.zzfxl.zza(this.zzfxp, this.zzfxr);
            }
            zzbk(false);
        }
        Iterator<Api.zzc<?>> it = this.zzfxd.zzfyy.keySet().iterator();
        while (it.hasNext()) {
            this.zzfxd.zzfyj.get(it.next()).disconnect();
        }
        this.zzfxd.zzfzc.zzk(this.zzfxj.isEmpty() ? null : this.zzfxj);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public final void zzajn() {
        this.zzfxn = false;
        this.zzfxd.zzfvq.zzfyk = Collections.emptySet();
        for (Api.zzc<?> zzcVar : this.zzfxk) {
            if (!this.zzfxd.zzfyy.containsKey(zzcVar)) {
                this.zzfxd.zzfyy.put(zzcVar, new ConnectionResult(17, null));
            }
        }
    }

    private final void zzajo() {
        ArrayList<Future<?>> arrayList = this.zzfxs;
        int size = arrayList.size();
        int i = 0;
        while (i < size) {
            Future<?> future = arrayList.get(i);
            i++;
            future.cancel(true);
        }
        this.zzfxs.clear();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public final Set<Scope> zzajp() {
        if (this.zzfwf == null) {
            return Collections.emptySet();
        }
        HashSet hashSet = new HashSet(this.zzfwf.zzamf());
        Map<Api<?>, com.google.android.gms.common.internal.zzt> mapZzamh = this.zzfwf.zzamh();
        for (Api<?> api : mapZzamh.keySet()) {
            if (!this.zzfxd.zzfyy.containsKey(api.zzahm())) {
                hashSet.addAll(mapZzamh.get(api).zzenh);
            }
        }
        return hashSet;
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* JADX WARN: Removed duplicated region for block: B:12:0x0024  */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
        To view partially-correct add '--show-bad-code' argument
    */
    public final void zzb(com.google.android.gms.common.ConnectionResult r5, com.google.android.gms.common.api.Api<?> r6, boolean r7) {
        /*
            r4 = this;
            com.google.android.gms.common.api.Api$zzd r0 = r6.zzahk()
            int r0 = r0.getPriority()
            r1 = 0
            r2 = 1
            if (r7 == 0) goto L24
            boolean r7 = r5.hasResolution()
            if (r7 == 0) goto L14
        L12:
            r7 = r2
            goto L22
        L14:
            com.google.android.gms.common.zzf r7 = r4.zzfwk
            int r3 = r5.getErrorCode()
            android.content.Intent r7 = r7.zzbo(r3)
            if (r7 == 0) goto L21
            goto L12
        L21:
            r7 = r1
        L22:
            if (r7 == 0) goto L2d
        L24:
            com.google.android.gms.common.ConnectionResult r7 = r4.zzfwt
            if (r7 == 0) goto L2c
            int r7 = r4.zzfxg
            if (r0 >= r7) goto L2d
        L2c:
            r1 = r2
        L2d:
            if (r1 == 0) goto L33
            r4.zzfwt = r5
            r4.zzfxg = r0
        L33:
            com.google.android.gms.common.api.internal.zzbi r4 = r4.zzfxd
            java.util.Map<com.google.android.gms.common.api.Api$zzc<?>, com.google.android.gms.common.ConnectionResult> r4 = r4.zzfyy
            com.google.android.gms.common.api.Api$zzc r6 = r6.zzahm()
            r4.put(r6, r5)
            return
        */
        throw new UnsupportedOperationException("Method not decompiled: com.google.android.gms.common.api.internal.zzao.zzb(com.google.android.gms.common.ConnectionResult, com.google.android.gms.common.api.Api, boolean):void");
    }

    private final void zzbk(boolean z) {
        if (this.zzfxl != null) {
            if (this.zzfxl.isConnected() && z) {
                this.zzfxl.zzbet();
            }
            this.zzfxl.disconnect();
            this.zzfxp = null;
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public final boolean zzbs(int i) {
        if (this.zzfxh == i) {
            return true;
        }
        Log.w("GoogleApiClientConnecting", this.zzfxd.zzfvq.zzaju());
        String strValueOf = String.valueOf(this);
        StringBuilder sb = new StringBuilder(String.valueOf(strValueOf).length() + 23);
        sb.append("Unexpected callback in ");
        sb.append(strValueOf);
        Log.w("GoogleApiClientConnecting", sb.toString());
        int i2 = this.zzfxi;
        StringBuilder sb2 = new StringBuilder(33);
        sb2.append("mRemainingConnections=");
        sb2.append(i2);
        Log.w("GoogleApiClientConnecting", sb2.toString());
        String strZzbt = zzbt(this.zzfxh);
        String strZzbt2 = zzbt(i);
        StringBuilder sb3 = new StringBuilder(String.valueOf(strZzbt).length() + 70 + String.valueOf(strZzbt2).length());
        sb3.append("GoogleApiClient connecting is in step ");
        sb3.append(strZzbt);
        sb3.append(" but received callback for step ");
        sb3.append(strZzbt2);
        Log.wtf("GoogleApiClientConnecting", sb3.toString(), new Exception());
        zze(new ConnectionResult(8, null));
        return false;
    }

    private static String zzbt(int i) {
        switch (i) {
            case 0:
                return "STEP_SERVICE_BINDINGS_AND_SIGN_IN";
            case 1:
                return "STEP_GETTING_REMOTE_SERVICE";
            default:
                return "UNKNOWN";
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public final boolean zzd(ConnectionResult connectionResult) {
        return this.zzfxm && !connectionResult.hasResolution();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public final void zze(ConnectionResult connectionResult) {
        zzajo();
        zzbk(!connectionResult.hasResolution());
        this.zzfxd.zzg(connectionResult);
        this.zzfxd.zzfzc.zzc(connectionResult);
    }

    @Override // com.google.android.gms.common.api.internal.zzbh
    public final void begin() {
        this.zzfxd.zzfyy.clear();
        this.zzfxn = false;
        zzap zzapVar = null;
        this.zzfwt = null;
        this.zzfxh = 0;
        this.zzfxm = true;
        this.zzfxo = false;
        this.zzfxq = false;
        HashMap map = new HashMap();
        boolean z = false;
        for (Api<?> api : this.zzfwi.keySet()) {
            Api.zze zzeVar = this.zzfxd.zzfyj.get(api.zzahm());
            z |= api.zzahk().getPriority() == 1;
            boolean zBooleanValue = this.zzfwi.get(api).booleanValue();
            if (zzeVar.zzacc()) {
                this.zzfxn = true;
                if (zBooleanValue) {
                    this.zzfxk.add(api.zzahm());
                } else {
                    this.zzfxm = false;
                }
            }
            map.put(zzeVar, new zzaq(this, api, zBooleanValue));
        }
        if (z) {
            this.zzfxn = false;
        }
        if (this.zzfxn) {
            this.zzfwf.zzc(Integer.valueOf(System.identityHashCode(this.zzfxd.zzfvq)));
            zzax zzaxVar = new zzax(this, zzapVar);
            this.zzfxl = (zzcyj) this.zzfth.zza(this.mContext, this.zzfxd.zzfvq.getLooper(), this.zzfwf, this.zzfwf.zzaml(), zzaxVar, zzaxVar);
        }
        this.zzfxi = this.zzfxd.zzfyj.size();
        this.zzfxs.add(zzbl.zzajx().submit(new zzar(this, map)));
    }

    @Override // com.google.android.gms.common.api.internal.zzbh
    public final void connect() {
    }

    @Override // com.google.android.gms.common.api.internal.zzbh
    public final boolean disconnect() {
        zzajo();
        zzbk(true);
        this.zzfxd.zzg(null);
        return true;
    }

    @Override // com.google.android.gms.common.api.internal.zzbh
    public final void onConnected(Bundle bundle) {
        if (zzbs(1)) {
            if (bundle != null) {
                this.zzfxj.putAll(bundle);
            }
            if (zzajk()) {
                zzajm();
            }
        }
    }

    @Override // com.google.android.gms.common.api.internal.zzbh
    public final void onConnectionSuspended(int i) {
        zze(new ConnectionResult(8, null));
    }

    @Override // com.google.android.gms.common.api.internal.zzbh
    public final void zza(ConnectionResult connectionResult, Api<?> api, boolean z) {
        if (zzbs(1)) {
            zzb(connectionResult, api, z);
            if (zzajk()) {
                zzajm();
            }
        }
    }

    @Override // com.google.android.gms.common.api.internal.zzbh
    public final <A extends Api.zzb, R extends Result, T extends zzm<R, A>> T zzd(T t) {
        this.zzfxd.zzfvq.zzfwo.add(t);
        return t;
    }

    @Override // com.google.android.gms.common.api.internal.zzbh
    public final <A extends Api.zzb, T extends zzm<? extends Result, A>> T zze(T t) {
        throw new IllegalStateException("GoogleApiClient is not connected yet.");
    }
}
