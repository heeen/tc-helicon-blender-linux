package com.google.android.gms.internal;

import android.accounts.Account;
import android.content.Context;
import android.os.Bundle;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Looper;
import android.os.RemoteException;
import android.util.Log;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.internal.zzbq;
import com.google.android.gms.common.internal.zzbr;

/* JADX INFO: loaded from: classes.dex */
public final class zzcyt extends com.google.android.gms.common.internal.zzab<zzcyr> implements zzcyj {
    private final com.google.android.gms.common.internal.zzr zzfwf;
    private Integer zzgft;
    private final boolean zzklw;
    private final Bundle zzklx;

    private zzcyt(Context context, Looper looper, boolean z, com.google.android.gms.common.internal.zzr zzrVar, Bundle bundle, GoogleApiClient.ConnectionCallbacks connectionCallbacks, GoogleApiClient.OnConnectionFailedListener onConnectionFailedListener) {
        super(context, looper, 44, zzrVar, connectionCallbacks, onConnectionFailedListener);
        this.zzklw = true;
        this.zzfwf = zzrVar;
        this.zzklx = bundle;
        this.zzgft = zzrVar.zzamm();
    }

    public zzcyt(Context context, Looper looper, boolean z, com.google.android.gms.common.internal.zzr zzrVar, zzcyk zzcykVar, GoogleApiClient.ConnectionCallbacks connectionCallbacks, GoogleApiClient.OnConnectionFailedListener onConnectionFailedListener) {
        this(context, looper, true, zzrVar, zza(zzrVar), connectionCallbacks, onConnectionFailedListener);
    }

    public static Bundle zza(com.google.android.gms.common.internal.zzr zzrVar) {
        zzcyk zzcykVarZzaml = zzrVar.zzaml();
        Integer numZzamm = zzrVar.zzamm();
        Bundle bundle = new Bundle();
        bundle.putParcelable("com.google.android.gms.signin.internal.clientRequestedAccount", zzrVar.getAccount());
        if (numZzamm != null) {
            bundle.putInt("com.google.android.gms.common.internal.ClientSettings.sessionId", numZzamm.intValue());
        }
        if (zzcykVarZzaml != null) {
            bundle.putBoolean("com.google.android.gms.signin.internal.offlineAccessRequested", zzcykVarZzaml.zzbeu());
            bundle.putBoolean("com.google.android.gms.signin.internal.idTokenRequested", zzcykVarZzaml.isIdTokenRequested());
            bundle.putString("com.google.android.gms.signin.internal.serverClientId", zzcykVarZzaml.getServerClientId());
            bundle.putBoolean("com.google.android.gms.signin.internal.usePromptModeForAuthCode", true);
            bundle.putBoolean("com.google.android.gms.signin.internal.forceCodeForRefreshToken", zzcykVarZzaml.zzbev());
            bundle.putString("com.google.android.gms.signin.internal.hostedDomain", zzcykVarZzaml.zzbew());
            bundle.putBoolean("com.google.android.gms.signin.internal.waitForAccessTokenRefresh", zzcykVarZzaml.zzbex());
            if (zzcykVarZzaml.zzbey() != null) {
                bundle.putLong("com.google.android.gms.signin.internal.authApiSignInModuleVersion", zzcykVarZzaml.zzbey().longValue());
            }
            if (zzcykVarZzaml.zzbez() != null) {
                bundle.putLong("com.google.android.gms.signin.internal.realClientLibraryVersion", zzcykVarZzaml.zzbez().longValue());
            }
        }
        return bundle;
    }

    @Override // com.google.android.gms.internal.zzcyj
    public final void connect() {
        zza(new com.google.android.gms.common.internal.zzm(this));
    }

    @Override // com.google.android.gms.internal.zzcyj
    public final void zza(com.google.android.gms.common.internal.zzan zzanVar, boolean z) {
        try {
            ((zzcyr) zzalw()).zza(zzanVar, this.zzgft.intValue(), z);
        } catch (RemoteException unused) {
            Log.w("SignInClientImpl", "Remote service probably died when saveDefaultAccount is called");
        }
    }

    @Override // com.google.android.gms.internal.zzcyj
    public final void zza(zzcyp zzcypVar) {
        zzbq.checkNotNull(zzcypVar, "Expecting a valid ISignInCallbacks");
        try {
            Account accountZzamd = this.zzfwf.zzamd();
            ((zzcyr) zzalw()).zza(new zzcyu(new zzbr(accountZzamd, this.zzgft.intValue(), "<<default account>>".equals(accountZzamd.name) ? com.google.android.gms.auth.api.signin.internal.zzaa.zzbs(getContext()).zzacx() : null)), zzcypVar);
        } catch (RemoteException e) {
            Log.w("SignInClientImpl", "Remote service probably died when signIn is called");
            try {
                zzcypVar.zzb(new zzcyw(8));
            } catch (RemoteException unused) {
                Log.wtf("SignInClientImpl", "ISignInCallbacks#onSignInComplete should be executed from the same process, unexpected RemoteException.", e);
            }
        }
    }

    @Override // com.google.android.gms.common.internal.zzd
    protected final Bundle zzabt() {
        if (!getContext().getPackageName().equals(this.zzfwf.zzami())) {
            this.zzklx.putString("com.google.android.gms.signin.internal.realClientPackageName", this.zzfwf.zzami());
        }
        return this.zzklx;
    }

    @Override // com.google.android.gms.common.internal.zzd, com.google.android.gms.common.api.Api.zze
    public final boolean zzacc() {
        return this.zzklw;
    }

    @Override // com.google.android.gms.internal.zzcyj
    public final void zzbet() {
        try {
            ((zzcyr) zzalw()).zzev(this.zzgft.intValue());
        } catch (RemoteException unused) {
            Log.w("SignInClientImpl", "Remote service probably died when clearAccountFromSessionStore is called");
        }
    }

    @Override // com.google.android.gms.common.internal.zzd
    protected final /* synthetic */ IInterface zzd(IBinder iBinder) {
        if (iBinder == null) {
            return null;
        }
        IInterface iInterfaceQueryLocalInterface = iBinder.queryLocalInterface("com.google.android.gms.signin.internal.ISignInService");
        return iInterfaceQueryLocalInterface instanceof zzcyr ? (zzcyr) iInterfaceQueryLocalInterface : new zzcys(iBinder);
    }

    @Override // com.google.android.gms.common.internal.zzd
    protected final String zzhm() {
        return "com.google.android.gms.signin.service.START";
    }

    @Override // com.google.android.gms.common.internal.zzd
    protected final String zzhn() {
        return "com.google.android.gms.signin.internal.ISignInService";
    }
}
