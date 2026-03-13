package com.google.android.gms.common;

import android.content.Context;
import android.os.RemoteException;
import android.util.Log;
import com.google.android.gms.common.internal.Hide;
import com.google.android.gms.common.internal.zzba;
import com.google.android.gms.common.internal.zzbb;
import com.google.android.gms.common.internal.zzbq;
import com.google.android.gms.dynamite.DynamiteModule;

/* JADX INFO: loaded from: classes.dex */
@Hide
final class zzg {
    private static volatile zzba zzfra;
    private static final Object zzfrb = new Object();
    private static Context zzfrc;

    static zzp zza(String str, zzh zzhVar, boolean z) {
        String str2;
        try {
            if (zzfra == null) {
                zzbq.checkNotNull(zzfrc);
                synchronized (zzfrb) {
                    if (zzfra == null) {
                        zzfra = zzbb.zzan(DynamiteModule.zza(zzfrc, DynamiteModule.zzhdl, "com.google.android.gms.googlecertificates").zzhk("com.google.android.gms.common.GoogleCertificatesImpl"));
                    }
                }
            }
            zzbq.checkNotNull(zzfrc);
            try {
                if (zzfra.zza(new zzn(str, zzhVar, z), com.google.android.gms.dynamic.zzn.zzz(zzfrc.getPackageManager()))) {
                    return zzp.zzahj();
                }
                return zzp.zza(str, zzhVar, z, !z && zza(str, zzhVar, true).zzfrm);
            } catch (RemoteException e) {
                e = e;
                Log.e("GoogleCertificates", "Failed to get Google certificates from remote", e);
                str2 = "module call";
                return zzp.zzd(str2, e);
            }
        } catch (DynamiteModule.zzc e2) {
            e = e2;
            str2 = "module init";
            return zzp.zzd(str2, e);
        }
    }

    static synchronized void zzch(Context context) {
        if (zzfrc != null) {
            Log.w("GoogleCertificates", "GoogleCertificates has been initialized already");
        } else if (context != null) {
            zzfrc = context.getApplicationContext();
        }
    }
}
