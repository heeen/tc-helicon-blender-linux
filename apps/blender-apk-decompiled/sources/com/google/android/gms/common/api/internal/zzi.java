package com.google.android.gms.common.api.internal;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;
import android.util.SparseArray;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import java.io.FileDescriptor;
import java.io.PrintWriter;

/* JADX INFO: loaded from: classes.dex */
public class zzi extends zzo {
    private final SparseArray<zza> zzfuf;

    class zza implements GoogleApiClient.OnConnectionFailedListener {
        public final int zzfug;
        public final GoogleApiClient zzfuh;
        public final GoogleApiClient.OnConnectionFailedListener zzfui;

        public zza(int i, GoogleApiClient googleApiClient, GoogleApiClient.OnConnectionFailedListener onConnectionFailedListener) {
            this.zzfug = i;
            this.zzfuh = googleApiClient;
            this.zzfui = onConnectionFailedListener;
            googleApiClient.registerConnectionFailedListener(this);
        }

        @Override // com.google.android.gms.common.api.GoogleApiClient.OnConnectionFailedListener
        public final void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
            String strValueOf = String.valueOf(connectionResult);
            StringBuilder sb = new StringBuilder(String.valueOf(strValueOf).length() + 27);
            sb.append("beginFailureResolution for ");
            sb.append(strValueOf);
            Log.d("AutoManageHelper", sb.toString());
            zzi.this.zzb(connectionResult, this.zzfug);
        }
    }

    private zzi(zzcf zzcfVar) {
        super(zzcfVar);
        this.zzfuf = new SparseArray<>();
        this.zzgam.zza("AutoManageHelper", this);
    }

    public static zzi zza(zzce zzceVar) {
        zzcf zzcfVarZzb = zzb(zzceVar);
        zzi zziVar = (zzi) zzcfVarZzb.zza("AutoManageHelper", zzi.class);
        return zziVar != null ? zziVar : new zzi(zzcfVarZzb);
    }

    @Nullable
    private final zza zzbr(int i) {
        if (this.zzfuf.size() <= i) {
            return null;
        }
        return this.zzfuf.get(this.zzfuf.keyAt(i));
    }

    @Override // com.google.android.gms.common.api.internal.LifecycleCallback
    public final void dump(String str, FileDescriptor fileDescriptor, PrintWriter printWriter, String[] strArr) {
        for (int i = 0; i < this.zzfuf.size(); i++) {
            zza zzaVarZzbr = zzbr(i);
            if (zzaVarZzbr != null) {
                printWriter.append((CharSequence) str).append("GoogleApiClient #").print(zzaVarZzbr.zzfug);
                printWriter.println(":");
                zzaVarZzbr.zzfuh.dump(String.valueOf(str).concat("  "), fileDescriptor, printWriter, strArr);
            }
        }
    }

    @Override // com.google.android.gms.common.api.internal.zzo, com.google.android.gms.common.api.internal.LifecycleCallback
    public final void onStart() {
        super.onStart();
        boolean z = this.mStarted;
        String strValueOf = String.valueOf(this.zzfuf);
        StringBuilder sb = new StringBuilder(String.valueOf(strValueOf).length() + 14);
        sb.append("onStart ");
        sb.append(z);
        sb.append(" ");
        sb.append(strValueOf);
        Log.d("AutoManageHelper", sb.toString());
        if (this.zzfut.get() == null) {
            for (int i = 0; i < this.zzfuf.size(); i++) {
                zza zzaVarZzbr = zzbr(i);
                if (zzaVarZzbr != null) {
                    zzaVarZzbr.zzfuh.connect();
                }
            }
        }
    }

    @Override // com.google.android.gms.common.api.internal.zzo, com.google.android.gms.common.api.internal.LifecycleCallback
    public final void onStop() {
        super.onStop();
        for (int i = 0; i < this.zzfuf.size(); i++) {
            zza zzaVarZzbr = zzbr(i);
            if (zzaVarZzbr != null) {
                zzaVarZzbr.zzfuh.disconnect();
            }
        }
    }

    public final void zza(int i, GoogleApiClient googleApiClient, GoogleApiClient.OnConnectionFailedListener onConnectionFailedListener) {
        com.google.android.gms.common.internal.zzbq.checkNotNull(googleApiClient, "GoogleApiClient instance cannot be null");
        boolean z = this.zzfuf.indexOfKey(i) < 0;
        StringBuilder sb = new StringBuilder(54);
        sb.append("Already managing a GoogleApiClient with id ");
        sb.append(i);
        com.google.android.gms.common.internal.zzbq.zza(z, sb.toString());
        zzp zzpVar = this.zzfut.get();
        boolean z2 = this.mStarted;
        String strValueOf = String.valueOf(zzpVar);
        StringBuilder sb2 = new StringBuilder(String.valueOf(strValueOf).length() + 49);
        sb2.append("starting AutoManage for client ");
        sb2.append(i);
        sb2.append(" ");
        sb2.append(z2);
        sb2.append(" ");
        sb2.append(strValueOf);
        Log.d("AutoManageHelper", sb2.toString());
        this.zzfuf.put(i, new zza(i, googleApiClient, onConnectionFailedListener));
        if (this.mStarted && zzpVar == null) {
            String strValueOf2 = String.valueOf(googleApiClient);
            StringBuilder sb3 = new StringBuilder(String.valueOf(strValueOf2).length() + 11);
            sb3.append("connecting ");
            sb3.append(strValueOf2);
            Log.d("AutoManageHelper", sb3.toString());
            googleApiClient.connect();
        }
    }

    @Override // com.google.android.gms.common.api.internal.zzo
    protected final void zza(ConnectionResult connectionResult, int i) {
        Log.w("AutoManageHelper", "Unresolved error while connecting client. Stopping auto-manage.");
        if (i < 0) {
            Log.wtf("AutoManageHelper", "AutoManageLifecycleHelper received onErrorResolutionFailed callback but no failing client ID is set", new Exception());
            return;
        }
        zza zzaVar = this.zzfuf.get(i);
        if (zzaVar != null) {
            zzbq(i);
            GoogleApiClient.OnConnectionFailedListener onConnectionFailedListener = zzaVar.zzfui;
            if (onConnectionFailedListener != null) {
                onConnectionFailedListener.onConnectionFailed(connectionResult);
            }
        }
    }

    @Override // com.google.android.gms.common.api.internal.zzo
    protected final void zzaih() {
        for (int i = 0; i < this.zzfuf.size(); i++) {
            zza zzaVarZzbr = zzbr(i);
            if (zzaVarZzbr != null) {
                zzaVarZzbr.zzfuh.connect();
            }
        }
    }

    public final void zzbq(int i) {
        zza zzaVar = this.zzfuf.get(i);
        this.zzfuf.remove(i);
        if (zzaVar != null) {
            zzaVar.zzfuh.unregisterConnectionFailedListener(zzaVar);
            zzaVar.zzfuh.disconnect();
        }
    }
}
