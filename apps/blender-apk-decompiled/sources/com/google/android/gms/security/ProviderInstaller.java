package com.google.android.gms.security;

import android.content.Context;
import android.content.Intent;
import android.util.Log;
import com.google.android.gms.common.GooglePlayServicesNotAvailableException;
import com.google.android.gms.common.GooglePlayServicesRepairableException;
import com.google.android.gms.common.internal.zzbq;
import com.google.android.gms.common.zzf;
import com.google.android.gms.common.zzs;
import java.lang.reflect.Method;

/* JADX INFO: loaded from: classes.dex */
public class ProviderInstaller {
    public static final String PROVIDER_NAME = "GmsCore_OpenSSL";
    private static final zzf zzklk = zzf.zzahf();
    private static final Object sLock = new Object();
    private static Method zzkll = null;

    public interface ProviderInstallListener {
        void onProviderInstallFailed(int i, Intent intent);

        void onProviderInstalled();
    }

    public static void installIfNeeded(Context context) throws GooglePlayServicesRepairableException, GooglePlayServicesNotAvailableException {
        zzbq.checkNotNull(context, "Context must not be null");
        zzf.zzce(context);
        Context remoteContext = zzs.getRemoteContext(context);
        if (remoteContext == null) {
            Log.e("ProviderInstaller", "Failed to get remote context");
            throw new GooglePlayServicesNotAvailableException(8);
        }
        synchronized (sLock) {
            try {
                try {
                    if (zzkll == null) {
                        zzkll = remoteContext.getClassLoader().loadClass("com.google.android.gms.common.security.ProviderInstallerImpl").getMethod("insertProvider", Context.class);
                    }
                    zzkll.invoke(null, remoteContext);
                } catch (Exception e) {
                    String strValueOf = String.valueOf(e.getMessage());
                    Log.e("ProviderInstaller", strValueOf.length() != 0 ? "Failed to install provider: ".concat(strValueOf) : new String("Failed to install provider: "));
                    throw new GooglePlayServicesNotAvailableException(8);
                }
            } catch (Throwable th) {
                throw th;
            }
        }
    }

    public static void installIfNeededAsync(Context context, ProviderInstallListener providerInstallListener) {
        zzbq.checkNotNull(context, "Context must not be null");
        zzbq.checkNotNull(providerInstallListener, "Listener must not be null");
        zzbq.zzgn("Must be called on the UI thread");
        new zza(context, providerInstallListener).execute(new Void[0]);
    }
}
