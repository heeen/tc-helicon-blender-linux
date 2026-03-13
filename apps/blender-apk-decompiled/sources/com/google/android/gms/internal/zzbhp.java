package com.google.android.gms.internal;

import com.google.android.gms.common.internal.zzbq;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/* JADX INFO: loaded from: classes.dex */
public abstract class zzbhp {
    /* JADX WARN: Multi-variable type inference failed */
    protected static <O, I> I zza(zzbhq<I, O> zzbhqVar, Object obj) {
        return ((zzbhq) zzbhqVar).zzgix != null ? zzbhqVar.convertBack(obj) : obj;
    }

    private static void zza(StringBuilder sb, zzbhq zzbhqVar, Object obj) {
        String string;
        if (zzbhqVar.zzgio == 11) {
            string = zzbhqVar.zzgiu.cast(obj).toString();
        } else if (zzbhqVar.zzgio != 7) {
            sb.append(obj);
            return;
        } else {
            sb.append("\"");
            sb.append(com.google.android.gms.common.util.zzq.zzha((String) obj));
            string = "\"";
        }
        sb.append(string);
    }

    private static void zza(StringBuilder sb, zzbhq zzbhqVar, ArrayList<Object> arrayList) {
        sb.append("[");
        int size = arrayList.size();
        for (int i = 0; i < size; i++) {
            if (i > 0) {
                sb.append(",");
            }
            Object obj = arrayList.get(i);
            if (obj != null) {
                zza(sb, zzbhqVar, obj);
            }
        }
        sb.append("]");
    }

    public String toString() {
        String str;
        String strZzj;
        Map<String, zzbhq<?, ?>> mapZzabz = zzabz();
        StringBuilder sb = new StringBuilder(100);
        for (String str2 : mapZzabz.keySet()) {
            zzbhq<?, ?> zzbhqVar = mapZzabz.get(str2);
            if (zza(zzbhqVar)) {
                Object objZza = zza(zzbhqVar, zzb(zzbhqVar));
                sb.append(sb.length() == 0 ? "{" : ",");
                sb.append("\"");
                sb.append(str2);
                sb.append("\":");
                if (objZza == null) {
                    str = "null";
                } else {
                    switch (zzbhqVar.zzgiq) {
                        case 8:
                            sb.append("\"");
                            strZzj = com.google.android.gms.common.util.zzc.zzj((byte[]) objZza);
                            break;
                        case 9:
                            sb.append("\"");
                            strZzj = com.google.android.gms.common.util.zzc.zzk((byte[]) objZza);
                            break;
                        case 10:
                            com.google.android.gms.common.util.zzr.zza(sb, (HashMap) objZza);
                            continue;
                        default:
                            if (zzbhqVar.zzgip) {
                                zza(sb, (zzbhq) zzbhqVar, (ArrayList<Object>) objZza);
                            } else {
                                zza(sb, zzbhqVar, objZza);
                                continue;
                            }
                            break;
                    }
                    sb.append(strZzj);
                    str = "\"";
                }
                sb.append(str);
            }
        }
        sb.append(sb.length() > 0 ? "}" : "{}");
        return sb.toString();
    }

    protected boolean zza(zzbhq zzbhqVar) {
        if (zzbhqVar.zzgiq != 11) {
            return zzgy(zzbhqVar.zzgis);
        }
        if (zzbhqVar.zzgir) {
            String str = zzbhqVar.zzgis;
            throw new UnsupportedOperationException("Concrete type arrays not supported");
        }
        String str2 = zzbhqVar.zzgis;
        throw new UnsupportedOperationException("Concrete types not supported");
    }

    public abstract Map<String, zzbhq<?, ?>> zzabz();

    protected Object zzb(zzbhq zzbhqVar) {
        String str = zzbhqVar.zzgis;
        if (zzbhqVar.zzgiu == null) {
            return zzgx(zzbhqVar.zzgis);
        }
        zzgx(zzbhqVar.zzgis);
        zzbq.zza(true, "Concrete field shouldn't be value object: %s", zzbhqVar.zzgis);
        boolean z = zzbhqVar.zzgir;
        try {
            char upperCase = Character.toUpperCase(str.charAt(0));
            String strSubstring = str.substring(1);
            StringBuilder sb = new StringBuilder(String.valueOf(strSubstring).length() + 4);
            sb.append("get");
            sb.append(upperCase);
            sb.append(strSubstring);
            return getClass().getMethod(sb.toString(), new Class[0]).invoke(this, new Object[0]);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    protected abstract Object zzgx(String str);

    protected abstract boolean zzgy(String str);
}
