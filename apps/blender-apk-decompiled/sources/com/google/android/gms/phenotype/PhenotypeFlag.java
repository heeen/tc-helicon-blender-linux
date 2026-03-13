package com.google.android.gms.phenotype;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Binder;
import android.os.Build;
import android.os.UserManager;
import android.support.v4.content.PermissionChecker;
import android.util.Log;
import com.google.android.gms.common.annotation.KeepForSdk;
import com.google.android.gms.internal.zzdnm;
import com.google.android.gms.internal.zzdob;

/* JADX INFO: loaded from: classes.dex */
@KeepForSdk
@Deprecated
public abstract class PhenotypeFlag<T> {
    private final T zzinq;
    private final Factory zzkgu;
    final String zzkgv;
    private final String zzkgw;
    private T zzkgx;
    private static final Object zzkgs = new Object();

    @SuppressLint({"StaticFieldLeak"})
    private static Context zzaiq = null;
    private static boolean zzciw = false;
    private static Boolean zzkgt = null;

    @KeepForSdk
    public static class Factory {
        private final String zzkhb;
        private final Uri zzkhc;
        private final String zzkhd;
        private final String zzkhe;
        private final boolean zzkhf;
        private final boolean zzkhg;

        @KeepForSdk
        public Factory(Uri uri) {
            this(null, uri, "", "", false, false);
        }

        private Factory(String str, Uri uri, String str2, String str3, boolean z, boolean z2) {
            this.zzkhb = str;
            this.zzkhc = uri;
            this.zzkhd = str2;
            this.zzkhe = str3;
            this.zzkhf = z;
            this.zzkhg = z2;
        }

        @KeepForSdk
        public PhenotypeFlag<String> createFlag(String str, String str2) {
            return PhenotypeFlag.zza(this, str, str2);
        }

        @KeepForSdk
        public Factory withGservicePrefix(String str) {
            if (this.zzkhf) {
                throw new IllegalStateException("Cannot set GServices prefix and skip GServices");
            }
            return new Factory(this.zzkhb, this.zzkhc, str, this.zzkhe, this.zzkhf, this.zzkhg);
        }

        @KeepForSdk
        public Factory withPhenotypePrefix(String str) {
            return new Factory(this.zzkhb, this.zzkhc, this.zzkhd, str, this.zzkhf, this.zzkhg);
        }
    }

    interface zza<V> {
        V zzbel();
    }

    private PhenotypeFlag(Factory factory, String str, T t) {
        this.zzkgx = null;
        if (factory.zzkhb == null && factory.zzkhc == null) {
            throw new IllegalArgumentException("Must pass a valid SharedPreferences file name or ContentProvider URI");
        }
        if (factory.zzkhb != null && factory.zzkhc != null) {
            throw new IllegalArgumentException("Must pass one of SharedPreferences file name or ContentProvider URI");
        }
        this.zzkgu = factory;
        String strValueOf = String.valueOf(factory.zzkhd);
        String strValueOf2 = String.valueOf(str);
        this.zzkgw = strValueOf2.length() != 0 ? strValueOf.concat(strValueOf2) : new String(strValueOf);
        String strValueOf3 = String.valueOf(factory.zzkhe);
        String strValueOf4 = String.valueOf(str);
        this.zzkgv = strValueOf4.length() != 0 ? strValueOf3.concat(strValueOf4) : new String(strValueOf3);
        this.zzinq = t;
    }

    /* synthetic */ PhenotypeFlag(Factory factory, String str, Object obj, zzr zzrVar) {
        this(factory, str, obj);
    }

    @KeepForSdk
    public static void maybeInit(Context context) {
        Context applicationContext;
        zzdob.maybeInit(context);
        if (zzaiq == null) {
            zzdob.zzch(context);
            synchronized (zzkgs) {
                if ((Build.VERSION.SDK_INT < 24 || !context.isDeviceProtectedStorage()) && (applicationContext = context.getApplicationContext()) != null) {
                    context = applicationContext;
                }
                if (zzaiq != context) {
                    zzkgt = null;
                }
                zzaiq = context;
            }
            zzciw = false;
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static PhenotypeFlag<String> zza(Factory factory, String str, String str2) {
        return new zzs(factory, str, str2);
    }

    private static <V> V zza(zza<V> zzaVar) {
        try {
            return zzaVar.zzbel();
        } catch (SecurityException unused) {
            long jClearCallingIdentity = Binder.clearCallingIdentity();
            try {
                return zzaVar.zzbel();
            } finally {
                Binder.restoreCallingIdentity(jClearCallingIdentity);
            }
        }
    }

    @TargetApi(24)
    private final T zzbeh() {
        if (zzh("gms:phenotype:phenotype_flag:debug_bypass_phenotype", false)) {
            String strValueOf = String.valueOf(this.zzkgv);
            Log.w("PhenotypeFlag", strValueOf.length() != 0 ? "Bypass reading Phenotype values for flag: ".concat(strValueOf) : new String("Bypass reading Phenotype values for flag: "));
        } else if (this.zzkgu.zzkhc != null) {
            final com.google.android.gms.phenotype.zza zzaVarZza = com.google.android.gms.phenotype.zza.zza(zzaiq.getContentResolver(), this.zzkgu.zzkhc);
            String str = (String) zza(new zza(this, zzaVarZza) { // from class: com.google.android.gms.phenotype.zzo
                private final PhenotypeFlag zzkgy;
                private final zza zzkgz;

                {
                    this.zzkgy = this;
                    this.zzkgz = zzaVarZza;
                }

                @Override // com.google.android.gms.phenotype.PhenotypeFlag.zza
                public final Object zzbel() {
                    return this.zzkgz.zzbee().get(this.zzkgy.zzkgv);
                }
            });
            if (str != null) {
                return zzkz(str);
            }
        } else {
            if (this.zzkgu.zzkhb == null || !(Build.VERSION.SDK_INT < 24 || zzaiq.isDeviceProtectedStorage() || ((UserManager) zzaiq.getSystemService(UserManager.class)).isUserUnlocked())) {
                return null;
            }
            SharedPreferences sharedPreferences = zzaiq.getSharedPreferences(this.zzkgu.zzkhb, 0);
            if (sharedPreferences.contains(this.zzkgv)) {
                return zzb(sharedPreferences);
            }
        }
        return null;
    }

    private final T zzbei() {
        String str;
        if (this.zzkgu.zzkhf || !zzbej() || (str = (String) zza(new zza(this) { // from class: com.google.android.gms.phenotype.zzp
            private final PhenotypeFlag zzkgy;

            {
                this.zzkgy = this;
            }

            @Override // com.google.android.gms.phenotype.PhenotypeFlag.zza
            public final Object zzbel() {
                return this.zzkgy.zzbek();
            }
        })) == null) {
            return null;
        }
        return zzkz(str);
    }

    private static boolean zzbej() {
        if (zzkgt == null) {
            if (zzaiq == null) {
                return false;
            }
            zzkgt = Boolean.valueOf(PermissionChecker.checkCallingOrSelfPermission(zzaiq, "com.google.android.providers.gsf.permission.READ_GSERVICES") == 0);
        }
        return zzkgt.booleanValue();
    }

    static boolean zzh(final String str, boolean z) {
        final boolean z2 = false;
        if (zzbej()) {
            return ((Boolean) zza(new zza(str, z2) { // from class: com.google.android.gms.phenotype.zzq
                private final String zzdiu;
                private final boolean zzkha = false;

                {
                    this.zzdiu = str;
                }

                @Override // com.google.android.gms.phenotype.PhenotypeFlag.zza
                public final Object zzbel() {
                    return Boolean.valueOf(zzdnm.zza(PhenotypeFlag.zzaiq.getContentResolver(), this.zzdiu, this.zzkha));
                }
            })).booleanValue();
        }
        return false;
    }

    @KeepForSdk
    public T get() {
        if (zzaiq == null) {
            throw new IllegalStateException("Must call PhenotypeFlag.init() first");
        }
        if (this.zzkgu.zzkhg) {
            T tZzbei = zzbei();
            if (tZzbei != null) {
                return tZzbei;
            }
            T tZzbeh = zzbeh();
            if (tZzbeh != null) {
                return tZzbeh;
            }
        } else {
            T tZzbeh2 = zzbeh();
            if (tZzbeh2 != null) {
                return tZzbeh2;
            }
            T tZzbei2 = zzbei();
            if (tZzbei2 != null) {
                return tZzbei2;
            }
        }
        return this.zzinq;
    }

    public abstract T zzb(SharedPreferences sharedPreferences);

    final /* synthetic */ String zzbek() {
        return zzdnm.zza(zzaiq.getContentResolver(), this.zzkgw, (String) null);
    }

    public abstract T zzkz(String str);
}
