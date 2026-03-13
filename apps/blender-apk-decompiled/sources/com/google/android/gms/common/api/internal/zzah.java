package com.google.android.gms.common.api.internal;

import android.app.Activity;
import android.support.v4.util.ArraySet;
import com.google.android.gms.common.ConnectionResult;

/* JADX INFO: loaded from: classes.dex */
public class zzah extends zzo {
    private zzbm zzfsq;
    private final ArraySet<zzh<?>> zzfxa;

    private zzah(zzcf zzcfVar) {
        super(zzcfVar);
        this.zzfxa = new ArraySet<>();
        this.zzgam.zza("ConnectionlessLifecycleHelper", this);
    }

    public static void zza(Activity activity, zzbm zzbmVar, zzh<?> zzhVar) {
        zzcf zzcfVarZzo = zzo(activity);
        zzah zzahVar = (zzah) zzcfVarZzo.zza("ConnectionlessLifecycleHelper", zzah.class);
        if (zzahVar == null) {
            zzahVar = new zzah(zzcfVarZzo);
        }
        zzahVar.zzfsq = zzbmVar;
        com.google.android.gms.common.internal.zzbq.checkNotNull(zzhVar, "ApiKey cannot be null");
        zzahVar.zzfxa.add(zzhVar);
        zzbmVar.zza(zzahVar);
    }

    private final void zzajg() {
        if (this.zzfxa.isEmpty()) {
            return;
        }
        this.zzfsq.zza(this);
    }

    @Override // com.google.android.gms.common.api.internal.LifecycleCallback
    public final void onResume() {
        super.onResume();
        zzajg();
    }

    @Override // com.google.android.gms.common.api.internal.zzo, com.google.android.gms.common.api.internal.LifecycleCallback
    public final void onStart() {
        super.onStart();
        zzajg();
    }

    @Override // com.google.android.gms.common.api.internal.zzo, com.google.android.gms.common.api.internal.LifecycleCallback
    public final void onStop() {
        super.onStop();
        this.zzfsq.zzb(this);
    }

    @Override // com.google.android.gms.common.api.internal.zzo
    protected final void zza(ConnectionResult connectionResult, int i) {
        this.zzfsq.zza(connectionResult, i);
    }

    @Override // com.google.android.gms.common.api.internal.zzo
    protected final void zzaih() {
        this.zzfsq.zzaih();
    }

    final ArraySet<zzh<?>> zzajf() {
        return this.zzfxa;
    }
}
