package com.google.firebase.messaging;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.util.Log;
import com.google.android.gms.common.internal.Hide;
import com.google.android.gms.internal.zzflr;
import com.google.android.gms.internal.zzfmu;
import com.google.android.gms.internal.zzfmv;
import com.google.android.gms.measurement.AppMeasurement;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

/* JADX INFO: loaded from: classes.dex */
@Hide
public final class zzc {
    @Nullable
    private static Object zza(@NonNull zzfmv zzfmvVar, @NonNull String str, @NonNull zzb zzbVar) {
        Object objNewInstance;
        String str2 = null;
        try {
            Class<?> cls = Class.forName("com.google.android.gms.measurement.AppMeasurement$ConditionalUserProperty");
            Bundle bundleZzay = zzay(zzfmvVar.zzpzs, zzfmvVar.zzpzt);
            objNewInstance = cls.getConstructor(new Class[0]).newInstance(new Object[0]);
            try {
                cls.getField("mOrigin").set(objNewInstance, str);
                cls.getField("mCreationTimestamp").set(objNewInstance, Long.valueOf(zzfmvVar.zzpzu));
                cls.getField("mName").set(objNewInstance, zzfmvVar.zzpzs);
                cls.getField("mValue").set(objNewInstance, zzfmvVar.zzpzt);
                if (!TextUtils.isEmpty(zzfmvVar.zzpzv)) {
                    str2 = zzfmvVar.zzpzv;
                }
                cls.getField("mTriggerEventName").set(objNewInstance, str2);
                cls.getField("mTimedOutEventName").set(objNewInstance, !TextUtils.isEmpty(zzfmvVar.zzqaa) ? zzfmvVar.zzqaa : zzbVar.zzbta());
                cls.getField("mTimedOutEventParams").set(objNewInstance, bundleZzay);
                cls.getField("mTriggerTimeout").set(objNewInstance, Long.valueOf(zzfmvVar.zzpzw));
                cls.getField("mTriggeredEventName").set(objNewInstance, !TextUtils.isEmpty(zzfmvVar.zzpzy) ? zzfmvVar.zzpzy : zzbVar.zzbsz());
                cls.getField("mTriggeredEventParams").set(objNewInstance, bundleZzay);
                cls.getField("mTimeToLive").set(objNewInstance, Long.valueOf(zzfmvVar.zzgoc));
                cls.getField("mExpiredEventName").set(objNewInstance, !TextUtils.isEmpty(zzfmvVar.zzqab) ? zzfmvVar.zzqab : zzbVar.zzbtb());
                cls.getField("mExpiredEventParams").set(objNewInstance, bundleZzay);
            } catch (Exception e) {
                e = e;
                Log.e("FirebaseAbtUtil", "Could not complete the operation due to an internal error.", e);
            }
        } catch (Exception e2) {
            e = e2;
            objNewInstance = null;
        }
        return objNewInstance;
    }

    private static String zza(@Nullable zzfmv zzfmvVar, @NonNull zzb zzbVar) {
        return (zzfmvVar == null || TextUtils.isEmpty(zzfmvVar.zzpzz)) ? zzbVar.zzbtc() : zzfmvVar.zzpzz;
    }

    private static List<Object> zza(@NonNull AppMeasurement appMeasurement, @NonNull String str) {
        List<Object> list;
        ArrayList arrayList = new ArrayList();
        try {
            Method declaredMethod = AppMeasurement.class.getDeclaredMethod("getConditionalUserProperties", String.class, String.class);
            declaredMethod.setAccessible(true);
            list = (List) declaredMethod.invoke(appMeasurement, str, "");
        } catch (Exception e) {
            Log.e("FirebaseAbtUtil", "Could not complete the operation due to an internal error.", e);
            list = arrayList;
        }
        if (Log.isLoggable("FirebaseAbtUtil", 2)) {
            int size = list.size();
            StringBuilder sb = new StringBuilder(String.valueOf(str).length() + 55);
            sb.append("Number of currently set _Es for origin: ");
            sb.append(str);
            sb.append(" is ");
            sb.append(size);
            Log.v("FirebaseAbtUtil", sb.toString());
        }
        return list;
    }

    private static void zza(@NonNull Context context, @NonNull String str, @NonNull String str2, @NonNull String str3, @NonNull String str4) {
        if (Log.isLoggable("FirebaseAbtUtil", 2)) {
            String strValueOf = String.valueOf(str);
            Log.v("FirebaseAbtUtil", strValueOf.length() != 0 ? "_CE(experimentId) called by ".concat(strValueOf) : new String("_CE(experimentId) called by "));
        }
        if (zzey(context)) {
            AppMeasurement appMeasurementZzde = zzde(context);
            try {
                Method declaredMethod = AppMeasurement.class.getDeclaredMethod("clearConditionalUserProperty", String.class, String.class, Bundle.class);
                declaredMethod.setAccessible(true);
                if (Log.isLoggable("FirebaseAbtUtil", 2)) {
                    StringBuilder sb = new StringBuilder(String.valueOf(str2).length() + 17 + String.valueOf(str3).length());
                    sb.append("Clearing _E: [");
                    sb.append(str2);
                    sb.append(", ");
                    sb.append(str3);
                    sb.append("]");
                    Log.v("FirebaseAbtUtil", sb.toString());
                }
                declaredMethod.invoke(appMeasurementZzde, str2, str4, zzay(str2, str3));
            } catch (Exception e) {
                Log.e("FirebaseAbtUtil", "Could not complete the operation due to an internal error.", e);
            }
        }
    }

    public static void zza(@NonNull Context context, @NonNull String str, @NonNull byte[] bArr, @NonNull zzb zzbVar, int i) {
        boolean z;
        if (Log.isLoggable("FirebaseAbtUtil", 2)) {
            String strValueOf = String.valueOf(str);
            Log.v("FirebaseAbtUtil", strValueOf.length() != 0 ? "_SE called by ".concat(strValueOf) : new String("_SE called by "));
        }
        if (zzey(context)) {
            AppMeasurement appMeasurementZzde = zzde(context);
            zzfmv zzfmvVarZzam = zzam(bArr);
            if (zzfmvVarZzam == null) {
                if (Log.isLoggable("FirebaseAbtUtil", 2)) {
                    Log.v("FirebaseAbtUtil", "_SE failed; either _P was not set, or we couldn't deserialize the _P.");
                    return;
                }
                return;
            }
            try {
                Class.forName("com.google.android.gms.measurement.AppMeasurement$ConditionalUserProperty");
                boolean z2 = false;
                for (Object obj : zza(appMeasurementZzde, str)) {
                    String strZzbe = zzbe(obj);
                    String strZzbf = zzbf(obj);
                    long jLongValue = ((Long) Class.forName("com.google.android.gms.measurement.AppMeasurement$ConditionalUserProperty").getField("mCreationTimestamp").get(obj)).longValue();
                    if (zzfmvVarZzam.zzpzs.equals(strZzbe) && zzfmvVarZzam.zzpzt.equals(strZzbf)) {
                        if (Log.isLoggable("FirebaseAbtUtil", 2)) {
                            StringBuilder sb = new StringBuilder(String.valueOf(strZzbe).length() + 23 + String.valueOf(strZzbf).length());
                            sb.append("_E is already set. [");
                            sb.append(strZzbe);
                            sb.append(", ");
                            sb.append(strZzbf);
                            sb.append("]");
                            Log.v("FirebaseAbtUtil", sb.toString());
                        }
                        z2 = true;
                    } else {
                        zzfmu[] zzfmuVarArr = zzfmvVarZzam.zzqad;
                        int length = zzfmuVarArr.length;
                        int i2 = 0;
                        while (true) {
                            if (i2 >= length) {
                                z = false;
                                break;
                            }
                            if (zzfmuVarArr[i2].zzpzs.equals(strZzbe)) {
                                if (Log.isLoggable("FirebaseAbtUtil", 2)) {
                                    StringBuilder sb2 = new StringBuilder(String.valueOf(strZzbe).length() + 33 + String.valueOf(strZzbf).length());
                                    sb2.append("_E is found in the _OE list. [");
                                    sb2.append(strZzbe);
                                    sb2.append(", ");
                                    sb2.append(strZzbf);
                                    sb2.append("]");
                                    Log.v("FirebaseAbtUtil", sb2.toString());
                                }
                                z = true;
                            } else {
                                i2++;
                            }
                        }
                        if (!z) {
                            if (zzfmvVarZzam.zzpzu > jLongValue) {
                                if (Log.isLoggable("FirebaseAbtUtil", 2)) {
                                    StringBuilder sb3 = new StringBuilder(String.valueOf(strZzbe).length() + 115 + String.valueOf(strZzbf).length());
                                    sb3.append("Clearing _E as it was not in the _OE list, andits start time is older than the start time of the _E to be set. [");
                                    sb3.append(strZzbe);
                                    sb3.append(", ");
                                    sb3.append(strZzbf);
                                    sb3.append("]");
                                    Log.v("FirebaseAbtUtil", sb3.toString());
                                }
                                zza(context, str, strZzbe, strZzbf, zza(zzfmvVarZzam, zzbVar));
                            } else if (Log.isLoggable("FirebaseAbtUtil", 2)) {
                                StringBuilder sb4 = new StringBuilder(String.valueOf(strZzbe).length() + 109 + String.valueOf(strZzbf).length());
                                sb4.append("_E was not found in the _OE list, but not clearing it as it has a new start time than the _E to be set.  [");
                                sb4.append(strZzbe);
                                sb4.append(", ");
                                sb4.append(strZzbf);
                                sb4.append("]");
                                Log.v("FirebaseAbtUtil", sb4.toString());
                            }
                        }
                    }
                }
                if (!z2) {
                    zza(appMeasurementZzde, context, str, zzfmvVarZzam, zzbVar, 1);
                    return;
                }
                if (Log.isLoggable("FirebaseAbtUtil", 2)) {
                    String str2 = zzfmvVarZzam.zzpzs;
                    String str3 = zzfmvVarZzam.zzpzt;
                    StringBuilder sb5 = new StringBuilder(String.valueOf(str2).length() + 44 + String.valueOf(str3).length());
                    sb5.append("_E is already set. Not setting it again [");
                    sb5.append(str2);
                    sb5.append(", ");
                    sb5.append(str3);
                    sb5.append("]");
                    Log.v("FirebaseAbtUtil", sb5.toString());
                }
            } catch (Exception e) {
                Log.e("FirebaseAbtUtil", "Could not complete the operation due to an internal error.", e);
            }
        }
    }

    private static void zza(@NonNull AppMeasurement appMeasurement, @NonNull Context context, @NonNull String str, @NonNull zzfmv zzfmvVar, @NonNull zzb zzbVar, int i) {
        if (Log.isLoggable("FirebaseAbtUtil", 2)) {
            String str2 = zzfmvVar.zzpzs;
            String str3 = zzfmvVar.zzpzt;
            StringBuilder sb = new StringBuilder(String.valueOf(str2).length() + 7 + String.valueOf(str3).length());
            sb.append("_SEI: ");
            sb.append(str2);
            sb.append(" ");
            sb.append(str3);
            Log.v("FirebaseAbtUtil", sb.toString());
        }
        try {
            Class.forName("com.google.android.gms.measurement.AppMeasurement$ConditionalUserProperty");
            List<Object> listZza = zza(appMeasurement, str);
            if (zza(appMeasurement, str).size() >= zzb(appMeasurement, str)) {
                if ((zzfmvVar.zzqac != 0 ? zzfmvVar.zzqac : 1) != 1) {
                    if (Log.isLoggable("FirebaseAbtUtil", 2)) {
                        String str4 = zzfmvVar.zzpzs;
                        String str5 = zzfmvVar.zzpzt;
                        StringBuilder sb2 = new StringBuilder(String.valueOf(str4).length() + 44 + String.valueOf(str5).length());
                        sb2.append("_E won't be set due to overflow policy. [");
                        sb2.append(str4);
                        sb2.append(", ");
                        sb2.append(str5);
                        sb2.append("]");
                        Log.v("FirebaseAbtUtil", sb2.toString());
                        return;
                    }
                    return;
                }
                Object obj = listZza.get(0);
                String strZzbe = zzbe(obj);
                String strZzbf = zzbf(obj);
                if (Log.isLoggable("FirebaseAbtUtil", 2)) {
                    StringBuilder sb3 = new StringBuilder(String.valueOf(strZzbe).length() + 38);
                    sb3.append("Clearing _E due to overflow policy: [");
                    sb3.append(strZzbe);
                    sb3.append("]");
                    Log.v("FirebaseAbtUtil", sb3.toString());
                }
                zza(context, str, strZzbe, strZzbf, zza(zzfmvVar, zzbVar));
            }
            for (Object obj2 : listZza) {
                String strZzbe2 = zzbe(obj2);
                String strZzbf2 = zzbf(obj2);
                if (strZzbe2.equals(zzfmvVar.zzpzs) && !strZzbf2.equals(zzfmvVar.zzpzt) && Log.isLoggable("FirebaseAbtUtil", 2)) {
                    StringBuilder sb4 = new StringBuilder(String.valueOf(strZzbe2).length() + 77 + String.valueOf(strZzbf2).length());
                    sb4.append("Clearing _E, as only one _V of the same _E can be set atany given time: [");
                    sb4.append(strZzbe2);
                    sb4.append(", ");
                    sb4.append(strZzbf2);
                    sb4.append("].");
                    Log.v("FirebaseAbtUtil", sb4.toString());
                    zza(context, str, strZzbe2, strZzbf2, zza(zzfmvVar, zzbVar));
                }
            }
            Object objZza = zza(zzfmvVar, str, zzbVar);
            if (objZza != null) {
                try {
                    Method declaredMethod = AppMeasurement.class.getDeclaredMethod("setConditionalUserProperty", Class.forName("com.google.android.gms.measurement.AppMeasurement$ConditionalUserProperty"));
                    declaredMethod.setAccessible(true);
                    declaredMethod.invoke(appMeasurement, objZza);
                    return;
                } catch (Exception e) {
                    Log.e("FirebaseAbtUtil", "Could not complete the operation due to an internal error.", e);
                    return;
                }
            }
            if (Log.isLoggable("FirebaseAbtUtil", 2)) {
                String str6 = zzfmvVar.zzpzs;
                String str7 = zzfmvVar.zzpzt;
                StringBuilder sb5 = new StringBuilder(String.valueOf(str6).length() + 42 + String.valueOf(str7).length());
                sb5.append("Could not create _CUP for: [");
                sb5.append(str6);
                sb5.append(", ");
                sb5.append(str7);
                sb5.append("]. Skipping.");
                Log.v("FirebaseAbtUtil", sb5.toString());
            }
        } catch (Exception e2) {
            Log.e("FirebaseAbtUtil", "Could not complete the operation due to an internal error.", e2);
        }
    }

    @Nullable
    private static zzfmv zzam(@NonNull byte[] bArr) {
        try {
            return zzfmv.zzbi(bArr);
        } catch (zzflr unused) {
            return null;
        }
    }

    private static Bundle zzay(@NonNull String str, @NonNull String str2) {
        Bundle bundle = new Bundle();
        bundle.putString(str, str2);
        return bundle;
    }

    private static int zzb(@NonNull AppMeasurement appMeasurement, @NonNull String str) {
        try {
            Method declaredMethod = AppMeasurement.class.getDeclaredMethod("getMaxUserProperties", String.class);
            declaredMethod.setAccessible(true);
            return ((Integer) declaredMethod.invoke(appMeasurement, str)).intValue();
        } catch (Exception e) {
            Log.e("FirebaseAbtUtil", "Could not complete the operation due to an internal error.", e);
            return 20;
        }
    }

    private static String zzbe(@NonNull Object obj) throws IllegalAccessException, NoSuchFieldException, ClassNotFoundException {
        return (String) Class.forName("com.google.android.gms.measurement.AppMeasurement$ConditionalUserProperty").getField("mName").get(obj);
    }

    private static String zzbf(@NonNull Object obj) throws IllegalAccessException, NoSuchFieldException, ClassNotFoundException {
        return (String) Class.forName("com.google.android.gms.measurement.AppMeasurement$ConditionalUserProperty").getField("mValue").get(obj);
    }

    @Nullable
    private static AppMeasurement zzde(Context context) {
        try {
            return AppMeasurement.getInstance(context);
        } catch (NoClassDefFoundError unused) {
            return null;
        }
    }

    private static boolean zzey(Context context) {
        if (zzde(context) == null) {
            if (Log.isLoggable("FirebaseAbtUtil", 2)) {
                Log.v("FirebaseAbtUtil", "Firebase Analytics not available");
            }
            return false;
        }
        try {
            Class.forName("com.google.android.gms.measurement.AppMeasurement$ConditionalUserProperty");
            return true;
        } catch (ClassNotFoundException unused) {
            if (Log.isLoggable("FirebaseAbtUtil", 2)) {
                Log.v("FirebaseAbtUtil", "Firebase Analytics library is missing support for abt. Please update to a more recent version.");
            }
            return false;
        }
    }
}
