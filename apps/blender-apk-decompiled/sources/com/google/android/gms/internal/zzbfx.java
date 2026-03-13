package com.google.android.gms.internal;

import com.google.android.gms.common.internal.Hide;

/* JADX INFO: loaded from: classes.dex */
@Hide
public class zzbfx<T> {
    private String zzbkr;
    private T zzbks;
    private T zzgca = null;
    private static final Object sLock = new Object();
    private static zzbgd zzgby = null;
    private static int zzgbz = 0;
    private static String READ_PERMISSION = "com.google.android.providers.gsf.permission.READ_GSERVICES";

    protected zzbfx(String str, T t) {
        this.zzbkr = str;
        this.zzbks = t;
    }

    public static zzbfx<Float> zza(String str, Float f) {
        return new zzbgb(str, f);
    }

    public static zzbfx<Integer> zza(String str, Integer num) {
        return new zzbga(str, num);
    }

    public static zzbfx<Long> zza(String str, Long l) {
        return new zzbfz(str, l);
    }

    public static zzbfx<Boolean> zze(String str, boolean z) {
        return new zzbfy(str, Boolean.valueOf(z));
    }

    public static zzbfx<String> zzs(String str, String str2) {
        return new zzbgc(str, str2);
    }
}
