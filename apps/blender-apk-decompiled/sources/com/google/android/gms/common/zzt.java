package com.google.android.gms.common;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.util.Log;
import com.google.android.gms.common.internal.Hide;
import com.google.android.gms.common.internal.zzbq;
import com.google.android.gms.internal.zzbih;

/* JADX INFO: loaded from: classes.dex */
@Hide
public class zzt {
    private static zzt zzfrx;
    private final Context mContext;

    private zzt(Context context) {
        this.mContext = context.getApplicationContext();
    }

    @Hide
    private static zzh zza(PackageInfo packageInfo, zzh... zzhVarArr) {
        if (packageInfo.signatures == null) {
            return null;
        }
        if (packageInfo.signatures.length != 1) {
            Log.w("GoogleSignatureVerifier", "Package has more than one signature.");
            return null;
        }
        zzi zziVar = new zzi(packageInfo.signatures[0].toByteArray());
        for (int i = 0; i < zzhVarArr.length; i++) {
            if (zzhVarArr[i].equals(zziVar)) {
                return zzhVarArr[i];
            }
        }
        return null;
    }

    @Hide
    public static boolean zza(PackageInfo packageInfo, boolean z) {
        if (packageInfo != null && packageInfo.signatures != null) {
            if (zza(packageInfo, z ? zzk.zzfrh : new zzh[]{zzk.zzfrh[0]}) != null) {
                return true;
            }
        }
        return false;
    }

    public static zzt zzcj(Context context) {
        zzbq.checkNotNull(context);
        synchronized (zzt.class) {
            if (zzfrx == null) {
                zzg.zzch(context);
                zzfrx = new zzt(context);
            }
        }
        return zzfrx;
    }

    private final zzp zzgh(String str) {
        String str2;
        try {
            PackageInfo packageInfo = zzbih.zzdd(this.mContext).getPackageInfo(str, 64);
            boolean zZzci = zzs.zzci(this.mContext);
            if (packageInfo == null) {
                str2 = "null pkg";
            } else if (packageInfo.signatures.length != 1) {
                str2 = "single cert required";
            } else {
                zzi zziVar = new zzi(packageInfo.signatures[0].toByteArray());
                String str3 = packageInfo.packageName;
                zzp zzpVarZza = zzg.zza(str3, zziVar, zZzci);
                if (!zzpVarZza.zzfrm || packageInfo.applicationInfo == null || (packageInfo.applicationInfo.flags & 2) == 0 || (zZzci && !zzg.zza(str3, zziVar, false).zzfrm)) {
                    return zzpVarZza;
                }
                str2 = "debuggable release cert app rejected";
            }
            return zzp.zzgg(str2);
        } catch (PackageManager.NameNotFoundException unused) {
            String strValueOf = String.valueOf(str);
            return zzp.zzgg(strValueOf.length() != 0 ? "no pkg ".concat(strValueOf) : new String("no pkg "));
        }
    }

    @Hide
    public final boolean zza(PackageInfo packageInfo) {
        if (packageInfo == null) {
            return false;
        }
        if (zza(packageInfo, false)) {
            return true;
        }
        if (zza(packageInfo, true)) {
            if (zzs.zzci(this.mContext)) {
                return true;
            }
            Log.w("GoogleSignatureVerifier", "Test-keys aren't accepted on this build.");
        }
        return false;
    }

    @Hide
    public final boolean zzbp(int i) {
        zzp zzpVarZzgg;
        String[] packagesForUid = zzbih.zzdd(this.mContext).getPackagesForUid(i);
        if (packagesForUid == null || packagesForUid.length == 0) {
            zzpVarZzgg = zzp.zzgg("no pkgs");
        } else {
            zzpVarZzgg = null;
            for (String str : packagesForUid) {
                zzpVarZzgg = zzgh(str);
                if (zzpVarZzgg.zzfrm) {
                    break;
                }
            }
        }
        if (!zzpVarZzgg.zzfrm) {
            if (zzpVarZzgg.cause != null) {
                Log.d("GoogleCertificatesRslt", zzpVarZzgg.getErrorMessage(), zzpVarZzgg.cause);
            } else {
                Log.d("GoogleCertificatesRslt", zzpVarZzgg.getErrorMessage());
            }
        }
        return zzpVarZzgg.zzfrm;
    }
}
