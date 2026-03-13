package com.google.android.gms.internal;

import android.content.Context;
import android.util.Log;
import com.google.android.gms.clearcut.ClearcutLogger;
import com.google.android.gms.phenotype.Phenotype;
import com.google.android.gms.phenotype.PhenotypeFlag;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;

/* JADX INFO: loaded from: classes.dex */
public final class zzbft implements ClearcutLogger.zza {
    private static final Charset UTF_8 = Charset.forName("UTF-8");
    private static final PhenotypeFlag.Factory zzfqj = new PhenotypeFlag.Factory(Phenotype.getContentProviderUri("com.google.android.gms.clearcut.public")).withGservicePrefix("gms:playlog:service:sampling_").withPhenotypePrefix("LogSampling__");
    private static Map<String, PhenotypeFlag<String>> zzfqk = null;
    private static Boolean zzfql = null;
    private static Long zzfqm = null;
    private final Context zzaiq;

    public zzbft(Context context) {
        this.zzaiq = context;
        if (zzfqk == null) {
            zzfqk = new HashMap();
        }
        if (this.zzaiq != null) {
            PhenotypeFlag.maybeInit(this.zzaiq);
        }
    }

    private static boolean zzcc(Context context) {
        if (zzfql == null) {
            zzfql = Boolean.valueOf(zzbih.zzdd(context).checkCallingOrSelfPermission("com.google.android.providers.gsf.permission.READ_GSERVICES") == 0);
        }
        return zzfql.booleanValue();
    }

    private static zzbfu zzge(String str) {
        if (str == null) {
            return null;
        }
        String strSubstring = "";
        int iIndexOf = str.indexOf(44);
        int i = 0;
        if (iIndexOf >= 0) {
            strSubstring = str.substring(0, iIndexOf);
            i = iIndexOf + 1;
        }
        String str2 = strSubstring;
        int iIndexOf2 = str.indexOf(47, i);
        if (iIndexOf2 <= 0) {
            String strValueOf = String.valueOf(str);
            Log.e("LogSamplerImpl", strValueOf.length() != 0 ? "Failed to parse the rule: ".concat(strValueOf) : new String("Failed to parse the rule: "));
            return null;
        }
        try {
            long j = Long.parseLong(str.substring(i, iIndexOf2));
            long j2 = Long.parseLong(str.substring(iIndexOf2 + 1));
            if (j >= 0 && j2 >= 0) {
                return new zzbfu(str2, j, j2);
            }
            StringBuilder sb = new StringBuilder(72);
            sb.append("negative values not supported: ");
            sb.append(j);
            sb.append("/");
            sb.append(j2);
            Log.e("LogSamplerImpl", sb.toString());
            return null;
        } catch (NumberFormatException e) {
            String strValueOf2 = String.valueOf(str);
            Log.e("LogSamplerImpl", strValueOf2.length() != 0 ? "parseLong() failed while parsing: ".concat(strValueOf2) : new String("parseLong() failed while parsing: "), e);
            return null;
        }
    }

    @Override // com.google.android.gms.clearcut.ClearcutLogger.zza
    public final boolean zzg(String str, int i) {
        long jLongValue;
        ByteBuffer byteBufferPutLong;
        String str2 = null;
        if (str == null || str.isEmpty()) {
            str = i >= 0 ? String.valueOf(i) : null;
        }
        if (str == null) {
            return true;
        }
        if (this.zzaiq != null && zzcc(this.zzaiq)) {
            PhenotypeFlag<String> phenotypeFlagCreateFlag = zzfqk.get(str);
            if (phenotypeFlagCreateFlag == null) {
                phenotypeFlagCreateFlag = zzfqj.createFlag(str, null);
                zzfqk.put(str, phenotypeFlagCreateFlag);
            }
            str2 = phenotypeFlagCreateFlag.get();
        }
        zzbfu zzbfuVarZzge = zzge(str2);
        if (zzbfuVarZzge == null) {
            return true;
        }
        String str3 = zzbfuVarZzge.zzfqn;
        Context context = this.zzaiq;
        if (zzfqm != null) {
            jLongValue = zzfqm.longValue();
        } else if (context != null) {
            zzfqm = zzcc(context) ? Long.valueOf(zzdnm.getLong(context.getContentResolver(), "android_id", 0L)) : 0L;
            jLongValue = zzfqm.longValue();
        } else {
            jLongValue = 0;
        }
        if (str3 == null || str3.isEmpty()) {
            byteBufferPutLong = ByteBuffer.allocate(8).putLong(jLongValue);
        } else {
            byte[] bytes = str3.getBytes(UTF_8);
            byteBufferPutLong = ByteBuffer.allocate(bytes.length + 8);
            byteBufferPutLong.put(bytes);
            byteBufferPutLong.putLong(jLongValue);
        }
        long jZzi = zzbfo.zzi(byteBufferPutLong.array());
        long j = zzbfuVarZzge.zzfqo;
        long j2 = zzbfuVarZzge.zzfqp;
        if (j >= 0 && j2 >= 0) {
            if (j2 > 0) {
                return ((jZzi > 0L ? 1 : (jZzi == 0L ? 0 : -1)) >= 0 ? jZzi % j2 : (((Long.MAX_VALUE % j2) + 1) + ((jZzi & Long.MAX_VALUE) % j2)) % j2) < j;
            }
            return false;
        }
        StringBuilder sb = new StringBuilder(72);
        sb.append("negative values not supported: ");
        sb.append(j);
        sb.append("/");
        sb.append(j2);
        throw new IllegalArgumentException(sb.toString());
    }
}
