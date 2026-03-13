package com.google.android.gms.dynamite;

import android.content.Context;
import android.database.Cursor;
import android.os.IBinder;
import android.os.IInterface;
import android.os.RemoteException;
import android.util.Log;
import com.google.android.gms.common.internal.Hide;
import com.google.android.gms.common.internal.zzbq;
import com.google.android.gms.common.util.DynamiteApi;
import com.google.android.gms.dynamic.IObjectWrapper;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;

/* JADX INFO: loaded from: classes.dex */
@Hide
public final class DynamiteModule {
    private static Boolean zzhdc;
    private static zzk zzhdd;
    private static zzm zzhde;
    private static String zzhdf;
    private static final ThreadLocal<zza> zzhdg = new ThreadLocal<>();
    private static final zzi zzhdh = new com.google.android.gms.dynamite.zza();
    public static final zzd zzhdi = new com.google.android.gms.dynamite.zzb();
    private static zzd zzhdj = new com.google.android.gms.dynamite.zzc();
    public static final zzd zzhdk = new com.google.android.gms.dynamite.zzd();
    public static final zzd zzhdl = new zze();
    public static final zzd zzhdm = new zzf();
    public static final zzd zzhdn = new zzg();
    private final Context zzhdo;

    @DynamiteApi
    public static class DynamiteLoaderClassLoader {
        public static ClassLoader sClassLoader;
    }

    static class zza {
        public Cursor zzhdp;

        private zza() {
        }

        /* synthetic */ zza(com.google.android.gms.dynamite.zza zzaVar) {
            this();
        }
    }

    static class zzb implements zzi {
        private final int zzhdq;
        private final int zzhdr = 0;

        public zzb(int i, int i2) {
            this.zzhdq = i;
        }

        @Override // com.google.android.gms.dynamite.zzi
        public final int zzc(Context context, String str, boolean z) {
            return 0;
        }

        @Override // com.google.android.gms.dynamite.zzi
        public final int zzx(Context context, String str) {
            return this.zzhdq;
        }
    }

    public static class zzc extends Exception {
        private zzc(String str) {
            super(str);
        }

        /* synthetic */ zzc(String str, com.google.android.gms.dynamite.zza zzaVar) {
            this(str);
        }

        private zzc(String str, Throwable th) {
            super(str, th);
        }

        /* synthetic */ zzc(String str, Throwable th, com.google.android.gms.dynamite.zza zzaVar) {
            this(str, th);
        }
    }

    public interface zzd {
        zzj zza(Context context, String str, zzi zziVar) throws zzc;
    }

    private DynamiteModule(Context context) {
        this.zzhdo = (Context) zzbq.checkNotNull(context);
    }

    private static Context zza(Context context, String str, int i, Cursor cursor, zzm zzmVar) {
        try {
            return (Context) com.google.android.gms.dynamic.zzn.zzy(zzmVar.zza(com.google.android.gms.dynamic.zzn.zzz(context), str, i, com.google.android.gms.dynamic.zzn.zzz(cursor)));
        } catch (Exception e) {
            String strValueOf = String.valueOf(e.toString());
            Log.e("DynamiteModule", strValueOf.length() != 0 ? "Failed to load DynamiteLoader: ".concat(strValueOf) : new String("Failed to load DynamiteLoader: "));
            return null;
        }
    }

    public static DynamiteModule zza(Context context, zzd zzdVar, String str) throws zzc {
        DynamiteModule dynamiteModuleZzz;
        zza zzaVar = zzhdg.get();
        com.google.android.gms.dynamite.zza zzaVar2 = null;
        zza zzaVar3 = new zza(zzaVar2);
        zzhdg.set(zzaVar3);
        try {
            zzj zzjVarZza = zzdVar.zza(context, str, zzhdh);
            int i = zzjVarZza.zzhds;
            int i2 = zzjVarZza.zzhdt;
            StringBuilder sb = new StringBuilder(String.valueOf(str).length() + 68 + String.valueOf(str).length());
            sb.append("Considering local module ");
            sb.append(str);
            sb.append(":");
            sb.append(i);
            sb.append(" and remote module ");
            sb.append(str);
            sb.append(":");
            sb.append(i2);
            Log.i("DynamiteModule", sb.toString());
            if (zzjVarZza.zzhdu == 0 || ((zzjVarZza.zzhdu == -1 && zzjVarZza.zzhds == 0) || (zzjVarZza.zzhdu == 1 && zzjVarZza.zzhdt == 0))) {
                int i3 = zzjVarZza.zzhds;
                int i4 = zzjVarZza.zzhdt;
                StringBuilder sb2 = new StringBuilder(91);
                sb2.append("No acceptable module found. Local version is ");
                sb2.append(i3);
                sb2.append(" and remote version is ");
                sb2.append(i4);
                sb2.append(".");
                throw new zzc(sb2.toString(), zzaVar2);
            }
            if (zzjVarZza.zzhdu == -1) {
                dynamiteModuleZzz = zzz(context, str);
            } else {
                if (zzjVarZza.zzhdu != 1) {
                    int i5 = zzjVarZza.zzhdu;
                    StringBuilder sb3 = new StringBuilder(47);
                    sb3.append("VersionPolicy returned invalid code:");
                    sb3.append(i5);
                    throw new zzc(sb3.toString(), zzaVar2);
                }
                try {
                    DynamiteModule dynamiteModuleZza = zza(context, str, zzjVarZza.zzhdt);
                    if (zzaVar3.zzhdp != null) {
                        zzaVar3.zzhdp.close();
                    }
                    zzhdg.set(zzaVar);
                    return dynamiteModuleZza;
                } catch (zzc e) {
                    String strValueOf = String.valueOf(e.getMessage());
                    Log.w("DynamiteModule", strValueOf.length() != 0 ? "Failed to load remote module: ".concat(strValueOf) : new String("Failed to load remote module: "));
                    if (zzjVarZza.zzhds == 0 || zzdVar.zza(context, str, new zzb(zzjVarZza.zzhds, 0)).zzhdu != -1) {
                        throw new zzc("Remote load failed. No local fallback found.", e, zzaVar2);
                    }
                    dynamiteModuleZzz = zzz(context, str);
                    if (zzaVar3.zzhdp != null) {
                    }
                }
            }
            return dynamiteModuleZzz;
        } finally {
            if (zzaVar3.zzhdp != null) {
                zzaVar3.zzhdp.close();
            }
            zzhdg.set(zzaVar);
        }
    }

    private static DynamiteModule zza(Context context, String str, int i) throws zzc {
        Boolean bool;
        synchronized (DynamiteModule.class) {
            bool = zzhdc;
        }
        if (bool != null) {
            return bool.booleanValue() ? zzc(context, str, i) : zzb(context, str, i);
        }
        throw new zzc("Failed to determine which loading route to use.", (com.google.android.gms.dynamite.zza) null);
    }

    private static void zza(ClassLoader classLoader) throws zzc {
        zzm zznVar;
        com.google.android.gms.dynamite.zza zzaVar = null;
        try {
            IBinder iBinder = (IBinder) classLoader.loadClass("com.google.android.gms.dynamiteloader.DynamiteLoaderV2").getConstructor(new Class[0]).newInstance(new Object[0]);
            if (iBinder == null) {
                zznVar = null;
            } else {
                IInterface iInterfaceQueryLocalInterface = iBinder.queryLocalInterface("com.google.android.gms.dynamite.IDynamiteLoaderV2");
                zznVar = iInterfaceQueryLocalInterface instanceof zzm ? (zzm) iInterfaceQueryLocalInterface : new zzn(iBinder);
            }
            zzhde = zznVar;
        } catch (ClassNotFoundException | IllegalAccessException | InstantiationException | NoSuchMethodException | InvocationTargetException e) {
            throw new zzc("Failed to instantiate dynamite loader", e, zzaVar);
        }
    }

    private static DynamiteModule zzb(Context context, String str, int i) throws zzc {
        StringBuilder sb = new StringBuilder(String.valueOf(str).length() + 51);
        sb.append("Selected remote version of ");
        sb.append(str);
        sb.append(", version >= ");
        sb.append(i);
        Log.i("DynamiteModule", sb.toString());
        zzk zzkVarZzdh = zzdh(context);
        com.google.android.gms.dynamite.zza zzaVar = null;
        if (zzkVarZzdh == null) {
            throw new zzc("Failed to create IDynamiteLoader.", zzaVar);
        }
        try {
            IObjectWrapper iObjectWrapperZza = zzkVarZzdh.zza(com.google.android.gms.dynamic.zzn.zzz(context), str, i);
            if (com.google.android.gms.dynamic.zzn.zzy(iObjectWrapperZza) != null) {
                return new DynamiteModule((Context) com.google.android.gms.dynamic.zzn.zzy(iObjectWrapperZza));
            }
            throw new zzc("Failed to load remote module.", zzaVar);
        } catch (RemoteException e) {
            throw new zzc("Failed to load remote module.", e, zzaVar);
        }
    }

    public static int zzc(Context context, String str, boolean z) {
        Class<?> clsLoadClass;
        Field declaredField;
        Boolean bool;
        synchronized (DynamiteModule.class) {
            Boolean bool2 = zzhdc;
            if (bool2 == null) {
                try {
                    clsLoadClass = context.getApplicationContext().getClassLoader().loadClass(DynamiteLoaderClassLoader.class.getName());
                    declaredField = clsLoadClass.getDeclaredField("sClassLoader");
                } catch (ClassNotFoundException | IllegalAccessException | NoSuchFieldException e) {
                    String strValueOf = String.valueOf(e);
                    StringBuilder sb = new StringBuilder(String.valueOf(strValueOf).length() + 30);
                    sb.append("Failed to load module via V2: ");
                    sb.append(strValueOf);
                    Log.w("DynamiteModule", sb.toString());
                    bool2 = Boolean.FALSE;
                }
                synchronized (clsLoadClass) {
                    ClassLoader classLoader = (ClassLoader) declaredField.get(null);
                    if (classLoader != null) {
                        if (classLoader != ClassLoader.getSystemClassLoader()) {
                            try {
                                zza(classLoader);
                            } catch (zzc unused) {
                            }
                            bool = Boolean.TRUE;
                        }
                        bool2 = bool;
                        zzhdc = bool2;
                    } else if ("com.google.android.gms".equals(context.getApplicationContext().getPackageName())) {
                        declaredField.set(null, ClassLoader.getSystemClassLoader());
                    } else {
                        try {
                            int iZze = zze(context, str, z);
                            if (zzhdf != null && !zzhdf.isEmpty()) {
                                zzh zzhVar = new zzh(zzhdf, ClassLoader.getSystemClassLoader());
                                zza(zzhVar);
                                declaredField.set(null, zzhVar);
                                zzhdc = Boolean.TRUE;
                                return iZze;
                            }
                            return iZze;
                        } catch (zzc unused2) {
                            declaredField.set(null, ClassLoader.getSystemClassLoader());
                        }
                    }
                    bool = Boolean.FALSE;
                    bool2 = bool;
                    zzhdc = bool2;
                }
            }
            if (!bool2.booleanValue()) {
                return zzd(context, str, z);
            }
            try {
                return zze(context, str, z);
            } catch (zzc e2) {
                String strValueOf2 = String.valueOf(e2.getMessage());
                Log.w("DynamiteModule", strValueOf2.length() != 0 ? "Failed to retrieve remote module version: ".concat(strValueOf2) : new String("Failed to retrieve remote module version: "));
                return 0;
            }
        }
    }

    private static DynamiteModule zzc(Context context, String str, int i) throws zzc {
        zzm zzmVar;
        StringBuilder sb = new StringBuilder(String.valueOf(str).length() + 51);
        sb.append("Selected remote version of ");
        sb.append(str);
        sb.append(", version >= ");
        sb.append(i);
        Log.i("DynamiteModule", sb.toString());
        synchronized (DynamiteModule.class) {
            zzmVar = zzhde;
        }
        com.google.android.gms.dynamite.zza zzaVar = null;
        if (zzmVar == null) {
            throw new zzc("DynamiteLoaderV2 was not cached.", zzaVar);
        }
        zza zzaVar2 = zzhdg.get();
        if (zzaVar2 == null || zzaVar2.zzhdp == null) {
            throw new zzc("No result cursor", zzaVar);
        }
        Context contextZza = zza(context.getApplicationContext(), str, i, zzaVar2.zzhdp, zzmVar);
        if (contextZza != null) {
            return new DynamiteModule(contextZza);
        }
        throw new zzc("Failed to get module context", zzaVar);
    }

    private static int zzd(Context context, String str, boolean z) {
        zzk zzkVarZzdh = zzdh(context);
        if (zzkVarZzdh == null) {
            return 0;
        }
        try {
            return zzkVarZzdh.zza(com.google.android.gms.dynamic.zzn.zzz(context), str, z);
        } catch (RemoteException e) {
            String strValueOf = String.valueOf(e.getMessage());
            Log.w("DynamiteModule", strValueOf.length() != 0 ? "Failed to retrieve remote module version: ".concat(strValueOf) : new String("Failed to retrieve remote module version: "));
            return 0;
        }
    }

    private static zzk zzdh(Context context) {
        zzk zzlVar;
        synchronized (DynamiteModule.class) {
            if (zzhdd != null) {
                return zzhdd;
            }
            if (com.google.android.gms.common.zzf.zzahf().isGooglePlayServicesAvailable(context) != 0) {
                return null;
            }
            try {
                IBinder iBinder = (IBinder) context.createPackageContext("com.google.android.gms", 3).getClassLoader().loadClass("com.google.android.gms.chimera.container.DynamiteLoaderImpl").newInstance();
                if (iBinder == null) {
                    zzlVar = null;
                } else {
                    IInterface iInterfaceQueryLocalInterface = iBinder.queryLocalInterface("com.google.android.gms.dynamite.IDynamiteLoader");
                    zzlVar = iInterfaceQueryLocalInterface instanceof zzk ? (zzk) iInterfaceQueryLocalInterface : new zzl(iBinder);
                }
                if (zzlVar != null) {
                    zzhdd = zzlVar;
                    return zzlVar;
                }
            } catch (Exception e) {
                String strValueOf = String.valueOf(e.getMessage());
                Log.e("DynamiteModule", strValueOf.length() != 0 ? "Failed to load IDynamiteLoader from GmsCore: ".concat(strValueOf) : new String("Failed to load IDynamiteLoader from GmsCore: "));
            }
            return null;
        }
    }

    /* JADX WARN: Multi-variable type inference failed */
    /* JADX WARN: Removed duplicated region for block: B:48:0x00a6  */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
        To view partially-correct add '--show-bad-code' argument
    */
    private static int zze(android.content.Context r8, java.lang.String r9, boolean r10) throws java.lang.Throwable {
        /*
            r0 = 0
            android.content.ContentResolver r1 = r8.getContentResolver()     // Catch: java.lang.Throwable -> L91 java.lang.Exception -> L93
            if (r10 == 0) goto La
            java.lang.String r8 = "api_force_staging"
            goto Lc
        La:
            java.lang.String r8 = "api"
        Lc:
            java.lang.String r10 = java.lang.String.valueOf(r8)     // Catch: java.lang.Throwable -> L91 java.lang.Exception -> L93
            int r10 = r10.length()     // Catch: java.lang.Throwable -> L91 java.lang.Exception -> L93
            int r10 = r10 + 42
            java.lang.String r2 = java.lang.String.valueOf(r9)     // Catch: java.lang.Throwable -> L91 java.lang.Exception -> L93
            int r2 = r2.length()     // Catch: java.lang.Throwable -> L91 java.lang.Exception -> L93
            int r10 = r10 + r2
            java.lang.StringBuilder r2 = new java.lang.StringBuilder     // Catch: java.lang.Throwable -> L91 java.lang.Exception -> L93
            r2.<init>(r10)     // Catch: java.lang.Throwable -> L91 java.lang.Exception -> L93
            java.lang.String r10 = "content://com.google.android.gms.chimera/"
            r2.append(r10)     // Catch: java.lang.Throwable -> L91 java.lang.Exception -> L93
            r2.append(r8)     // Catch: java.lang.Throwable -> L91 java.lang.Exception -> L93
            java.lang.String r8 = "/"
            r2.append(r8)     // Catch: java.lang.Throwable -> L91 java.lang.Exception -> L93
            r2.append(r9)     // Catch: java.lang.Throwable -> L91 java.lang.Exception -> L93
            java.lang.String r8 = r2.toString()     // Catch: java.lang.Throwable -> L91 java.lang.Exception -> L93
            android.net.Uri r2 = android.net.Uri.parse(r8)     // Catch: java.lang.Throwable -> L91 java.lang.Exception -> L93
            r3 = 0
            r4 = 0
            r5 = 0
            r6 = 0
            android.database.Cursor r8 = r1.query(r2, r3, r4, r5, r6)     // Catch: java.lang.Throwable -> L91 java.lang.Exception -> L93
            if (r8 == 0) goto L82
            boolean r9 = r8.moveToFirst()     // Catch: java.lang.Throwable -> L79 java.lang.Exception -> L7d
            if (r9 == 0) goto L82
            r9 = 0
            int r9 = r8.getInt(r9)     // Catch: java.lang.Throwable -> L79 java.lang.Exception -> L7d
            if (r9 <= 0) goto L73
            java.lang.Class<com.google.android.gms.dynamite.DynamiteModule> r10 = com.google.android.gms.dynamite.DynamiteModule.class
            monitor-enter(r10)     // Catch: java.lang.Throwable -> L79 java.lang.Exception -> L7d
            r1 = 2
            java.lang.String r1 = r8.getString(r1)     // Catch: java.lang.Throwable -> L70
            com.google.android.gms.dynamite.DynamiteModule.zzhdf = r1     // Catch: java.lang.Throwable -> L70
            monitor-exit(r10)     // Catch: java.lang.Throwable -> L70
            java.lang.ThreadLocal<com.google.android.gms.dynamite.DynamiteModule$zza> r10 = com.google.android.gms.dynamite.DynamiteModule.zzhdg     // Catch: java.lang.Throwable -> L79 java.lang.Exception -> L7d
            java.lang.Object r10 = r10.get()     // Catch: java.lang.Throwable -> L79 java.lang.Exception -> L7d
            com.google.android.gms.dynamite.DynamiteModule$zza r10 = (com.google.android.gms.dynamite.DynamiteModule.zza) r10     // Catch: java.lang.Throwable -> L79 java.lang.Exception -> L7d
            if (r10 == 0) goto L73
            android.database.Cursor r1 = r10.zzhdp     // Catch: java.lang.Throwable -> L79 java.lang.Exception -> L7d
            if (r1 != 0) goto L73
            r10.zzhdp = r8     // Catch: java.lang.Throwable -> L79 java.lang.Exception -> L7d
            r8 = r0
            goto L73
        L70:
            r9 = move-exception
            monitor-exit(r10)     // Catch: java.lang.Throwable -> L70
            throw r9     // Catch: java.lang.Throwable -> L79 java.lang.Exception -> L7d
        L73:
            if (r8 == 0) goto L78
            r8.close()
        L78:
            return r9
        L79:
            r9 = move-exception
            r0 = r8
            r8 = r9
            goto La4
        L7d:
            r9 = move-exception
            r7 = r9
            r9 = r8
            r8 = r7
            goto L95
        L82:
            java.lang.String r9 = "DynamiteModule"
            java.lang.String r10 = "Failed to retrieve remote module version."
            android.util.Log.w(r9, r10)     // Catch: java.lang.Throwable -> L79 java.lang.Exception -> L7d
            com.google.android.gms.dynamite.DynamiteModule$zzc r9 = new com.google.android.gms.dynamite.DynamiteModule$zzc     // Catch: java.lang.Throwable -> L79 java.lang.Exception -> L7d
            java.lang.String r10 = "Failed to connect to dynamite module ContentResolver."
            r9.<init>(r10, r0)     // Catch: java.lang.Throwable -> L79 java.lang.Exception -> L7d
            throw r9     // Catch: java.lang.Throwable -> L79 java.lang.Exception -> L7d
        L91:
            r8 = move-exception
            goto La4
        L93:
            r8 = move-exception
            r9 = r0
        L95:
            boolean r10 = r8 instanceof com.google.android.gms.dynamite.DynamiteModule.zzc     // Catch: java.lang.Throwable -> La2
            if (r10 == 0) goto L9a
            throw r8     // Catch: java.lang.Throwable -> La2
        L9a:
            com.google.android.gms.dynamite.DynamiteModule$zzc r10 = new com.google.android.gms.dynamite.DynamiteModule$zzc     // Catch: java.lang.Throwable -> La2
            java.lang.String r1 = "V2 version check failed"
            r10.<init>(r1, r8, r0)     // Catch: java.lang.Throwable -> La2
            throw r10     // Catch: java.lang.Throwable -> La2
        La2:
            r8 = move-exception
            r0 = r9
        La4:
            if (r0 == 0) goto La9
            r0.close()
        La9:
            throw r8
        */
        throw new UnsupportedOperationException("Method not decompiled: com.google.android.gms.dynamite.DynamiteModule.zze(android.content.Context, java.lang.String, boolean):int");
    }

    public static int zzx(Context context, String str) {
        try {
            ClassLoader classLoader = context.getApplicationContext().getClassLoader();
            StringBuilder sb = new StringBuilder(String.valueOf(str).length() + 61);
            sb.append("com.google.android.gms.dynamite.descriptors.");
            sb.append(str);
            sb.append(".ModuleDescriptor");
            Class<?> clsLoadClass = classLoader.loadClass(sb.toString());
            Field declaredField = clsLoadClass.getDeclaredField("MODULE_ID");
            Field declaredField2 = clsLoadClass.getDeclaredField("MODULE_VERSION");
            if (declaredField.get(null).equals(str)) {
                return declaredField2.getInt(null);
            }
            String strValueOf = String.valueOf(declaredField.get(null));
            StringBuilder sb2 = new StringBuilder(String.valueOf(strValueOf).length() + 51 + String.valueOf(str).length());
            sb2.append("Module descriptor id '");
            sb2.append(strValueOf);
            sb2.append("' didn't match expected id '");
            sb2.append(str);
            sb2.append("'");
            Log.e("DynamiteModule", sb2.toString());
            return 0;
        } catch (ClassNotFoundException unused) {
            StringBuilder sb3 = new StringBuilder(String.valueOf(str).length() + 45);
            sb3.append("Local module descriptor class for ");
            sb3.append(str);
            sb3.append(" not found.");
            Log.w("DynamiteModule", sb3.toString());
            return 0;
        } catch (Exception e) {
            String strValueOf2 = String.valueOf(e.getMessage());
            Log.e("DynamiteModule", strValueOf2.length() != 0 ? "Failed to load module descriptor class: ".concat(strValueOf2) : new String("Failed to load module descriptor class: "));
            return 0;
        }
    }

    public static int zzy(Context context, String str) {
        return zzc(context, str, false);
    }

    private static DynamiteModule zzz(Context context, String str) {
        String strValueOf = String.valueOf(str);
        Log.i("DynamiteModule", strValueOf.length() != 0 ? "Selected local version of ".concat(strValueOf) : new String("Selected local version of "));
        return new DynamiteModule(context.getApplicationContext());
    }

    public final Context zzarl() {
        return this.zzhdo;
    }

    public final IBinder zzhk(String str) throws zzc {
        try {
            return (IBinder) this.zzhdo.getClassLoader().loadClass(str).newInstance();
        } catch (ClassNotFoundException | IllegalAccessException | InstantiationException e) {
            String strValueOf = String.valueOf(str);
            throw new zzc(strValueOf.length() != 0 ? "Failed to instantiate module class: ".concat(strValueOf) : new String("Failed to instantiate module class: "), e, null);
        }
    }
}
