package com.google.android.gms.common.util;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.WorkSource;
import android.support.annotation.Nullable;
import android.util.Log;
import com.google.android.gms.internal.zzbih;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/* JADX INFO: loaded from: classes.dex */
public final class zzaa {
    private static final Method zzgli = zzanz();
    private static final Method zzglj = zzaoa();
    private static final Method zzglk = zzaob();
    private static final Method zzgll = zzaoc();
    private static final Method zzglm = zzaod();

    private static int zza(WorkSource workSource) {
        if (zzglk != null) {
            try {
                return ((Integer) zzglk.invoke(workSource, new Object[0])).intValue();
            } catch (Exception e) {
                Log.wtf("WorkSourceUtil", "Unable to assign blame through WorkSource", e);
            }
        }
        return 0;
    }

    @Nullable
    private static String zza(WorkSource workSource, int i) {
        if (zzglm == null) {
            return null;
        }
        try {
            return (String) zzglm.invoke(workSource, Integer.valueOf(i));
        } catch (Exception e) {
            Log.wtf("WorkSourceUtil", "Unable to assign blame through WorkSource", e);
            return null;
        }
    }

    private static Method zzanz() {
        try {
            return WorkSource.class.getMethod("add", Integer.TYPE);
        } catch (Exception unused) {
            return null;
        }
    }

    private static Method zzaoa() {
        if (zzs.zzanu()) {
            try {
                return WorkSource.class.getMethod("add", Integer.TYPE, String.class);
            } catch (Exception unused) {
            }
        }
        return null;
    }

    private static Method zzaob() {
        try {
            return WorkSource.class.getMethod("size", new Class[0]);
        } catch (Exception unused) {
            return null;
        }
    }

    private static Method zzaoc() {
        try {
            return WorkSource.class.getMethod("get", Integer.TYPE);
        } catch (Exception unused) {
            return null;
        }
    }

    private static Method zzaod() {
        if (zzs.zzanu()) {
            try {
                return WorkSource.class.getMethod("getName", Integer.TYPE);
            } catch (Exception unused) {
            }
        }
        return null;
    }

    public static List<String> zzb(@Nullable WorkSource workSource) {
        int iZza = workSource == null ? 0 : zza(workSource);
        if (iZza == 0) {
            return Collections.emptyList();
        }
        ArrayList arrayList = new ArrayList();
        for (int i = 0; i < iZza; i++) {
            String strZza = zza(workSource, i);
            if (!zzw.zzhb(strZza)) {
                arrayList.add(strZza);
            }
        }
        return arrayList;
    }

    public static boolean zzda(Context context) {
        return (context == null || context.getPackageManager() == null || zzbih.zzdd(context).checkPermission("android.permission.UPDATE_DEVICE_STATS", context.getPackageName()) != 0) ? false : true;
    }

    /* JADX WARN: Unsupported multi-entry loop pattern (BACK_EDGE: B:9:0x0021 -> B:14:0x003a). Please report as a decompilation issue!!! */
    private static WorkSource zze(int i, String str) {
        WorkSource workSource = new WorkSource();
        try {
        } catch (Exception e) {
            Log.wtf("WorkSourceUtil", "Unable to assign blame through WorkSource", e);
        }
        if (zzglj == null) {
            if (zzgli != null) {
                zzgli.invoke(workSource, Integer.valueOf(i));
            }
            return workSource;
        }
        if (str == null) {
            str = "";
        }
        zzglj.invoke(workSource, Integer.valueOf(i), str);
        return workSource;
    }

    @Nullable
    public static WorkSource zzw(Context context, @Nullable String str) {
        if (context != null && context.getPackageManager() != null && str != null) {
            try {
                ApplicationInfo applicationInfo = zzbih.zzdd(context).getApplicationInfo(str, 0);
                if (applicationInfo != null) {
                    return zze(applicationInfo.uid, str);
                }
                String strValueOf = String.valueOf(str);
                Log.e("WorkSourceUtil", strValueOf.length() != 0 ? "Could not get applicationInfo from package: ".concat(strValueOf) : new String("Could not get applicationInfo from package: "));
                return null;
            } catch (PackageManager.NameNotFoundException unused) {
                String strValueOf2 = String.valueOf(str);
                Log.e("WorkSourceUtil", strValueOf2.length() != 0 ? "Could not find package: ".concat(strValueOf2) : new String("Could not find package: "));
            }
        }
        return null;
    }
}
