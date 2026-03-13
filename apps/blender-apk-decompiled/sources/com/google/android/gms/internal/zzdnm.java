package com.google.android.gms.internal;

import android.content.ContentResolver;
import android.database.Cursor;
import android.net.Uri;
import android.util.Log;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Pattern;

/* JADX INFO: loaded from: classes.dex */
public class zzdnm {
    private static HashMap<String, String> zzlxj;
    private static Object zzlxo;
    private static boolean zzlxp;
    private static Uri CONTENT_URI = Uri.parse("content://com.google.android.gsf.gservices");
    private static Uri zzlxf = Uri.parse("content://com.google.android.gsf.gservices/prefix");
    private static Pattern zzlxg = Pattern.compile("^(1|true|t|on|yes|y)$", 2);
    private static Pattern zzlxh = Pattern.compile("^(0|false|f|off|no|n)$", 2);
    private static final AtomicBoolean zzlxi = new AtomicBoolean();
    private static HashMap<String, Boolean> zzlxk = new HashMap<>();
    private static HashMap<String, Integer> zzlxl = new HashMap<>();
    private static HashMap<String, Long> zzlxm = new HashMap<>();
    private static HashMap<String, Float> zzlxn = new HashMap<>();
    private static String[] zzlxq = new String[0];

    public static long getLong(ContentResolver contentResolver, String str, long j) {
        Object objZzb = zzb(contentResolver);
        long j2 = 0;
        Long lValueOf = (Long) zza((HashMap<String, long>) zzlxm, str, 0L);
        if (lValueOf != null) {
            return lValueOf.longValue();
        }
        String strZza = zza(contentResolver, str, (String) null);
        if (strZza != null) {
            try {
                long j3 = Long.parseLong(strZza);
                lValueOf = Long.valueOf(j3);
                j2 = j3;
            } catch (NumberFormatException unused) {
            }
        }
        zza(objZzb, zzlxm, str, lValueOf);
        return j2;
    }

    private static <T> T zza(HashMap<String, T> map, String str, T t) {
        synchronized (zzdnm.class) {
            if (!map.containsKey(str)) {
                return null;
            }
            T t2 = map.get(str);
            if (t2 == null) {
                t2 = t;
            }
            return t2;
        }
    }

    public static String zza(ContentResolver contentResolver, String str, String str2) {
        synchronized (zzdnm.class) {
            zza(contentResolver);
            Object obj = zzlxo;
            if (zzlxj.containsKey(str)) {
                String str3 = zzlxj.get(str);
                if (str3 == null) {
                    str3 = null;
                }
                return str3;
            }
            for (String str4 : zzlxq) {
                if (str.startsWith(str4)) {
                    if (!zzlxp || zzlxj.isEmpty()) {
                        zzlxj.putAll(zza(contentResolver, zzlxq));
                        zzlxp = true;
                        if (zzlxj.containsKey(str)) {
                            String str5 = zzlxj.get(str);
                            if (str5 == null) {
                                str5 = null;
                            }
                            return str5;
                        }
                    }
                    return null;
                }
            }
            Cursor cursorQuery = contentResolver.query(CONTENT_URI, null, null, new String[]{str}, null);
            if (cursorQuery != null) {
                try {
                    if (cursorQuery.moveToFirst()) {
                        String string = cursorQuery.getString(1);
                        if (string != null && string.equals(null)) {
                            string = null;
                        }
                        zza(obj, str, string);
                        if (string == null) {
                            string = null;
                        }
                        return string;
                    }
                } finally {
                    if (cursorQuery != null) {
                        cursorQuery.close();
                    }
                }
            }
            zza(obj, str, (String) null);
            if (cursorQuery != null) {
                cursorQuery.close();
            }
            return null;
        }
    }

    private static Map<String, String> zza(ContentResolver contentResolver, String... strArr) {
        Cursor cursorQuery = contentResolver.query(zzlxf, null, null, strArr, null);
        TreeMap treeMap = new TreeMap();
        if (cursorQuery == null) {
            return treeMap;
        }
        while (cursorQuery.moveToNext()) {
            try {
                treeMap.put(cursorQuery.getString(0), cursorQuery.getString(1));
            } finally {
                cursorQuery.close();
            }
        }
        return treeMap;
    }

    private static void zza(ContentResolver contentResolver) {
        if (zzlxj == null) {
            zzlxi.set(false);
            zzlxj = new HashMap<>();
            zzlxo = new Object();
            zzlxp = false;
            contentResolver.registerContentObserver(CONTENT_URI, true, new zzdnn(null));
            return;
        }
        if (zzlxi.getAndSet(false)) {
            zzlxj.clear();
            zzlxk.clear();
            zzlxl.clear();
            zzlxm.clear();
            zzlxn.clear();
            zzlxo = new Object();
            zzlxp = false;
        }
    }

    private static void zza(Object obj, String str, String str2) {
        synchronized (zzdnm.class) {
            if (obj == zzlxo) {
                zzlxj.put(str, str2);
            }
        }
    }

    private static <T> void zza(Object obj, HashMap<String, T> map, String str, T t) {
        synchronized (zzdnm.class) {
            if (obj == zzlxo) {
                map.put(str, t);
                zzlxj.remove(str);
            }
        }
    }

    public static boolean zza(ContentResolver contentResolver, String str, boolean z) {
        Object objZzb = zzb(contentResolver);
        Boolean bool = (Boolean) zza(zzlxk, str, Boolean.valueOf(z));
        if (bool != null) {
            return bool.booleanValue();
        }
        String strZza = zza(contentResolver, str, (String) null);
        if (strZza != null && !strZza.equals("")) {
            if (zzlxg.matcher(strZza).matches()) {
                bool = true;
                z = true;
            } else if (zzlxh.matcher(strZza).matches()) {
                bool = false;
                z = false;
            } else {
                Log.w("Gservices", "attempt to read gservices key " + str + " (value \"" + strZza + "\") as boolean");
            }
        }
        zza(objZzb, zzlxk, str, bool);
        return z;
    }

    private static Object zzb(ContentResolver contentResolver) {
        Object obj;
        synchronized (zzdnm.class) {
            zza(contentResolver);
            obj = zzlxo;
        }
        return obj;
    }
}
