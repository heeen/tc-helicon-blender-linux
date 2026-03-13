package com.google.android.gms.clearcut;

import android.content.Context;
import android.content.pm.PackageManager;
import android.util.Log;
import com.google.android.gms.common.annotation.KeepForSdk;
import com.google.android.gms.common.api.Api;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResults;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.common.internal.Hide;
import com.google.android.gms.common.internal.zzbq;
import com.google.android.gms.common.util.zzi;
import com.google.android.gms.internal.zzbfi;
import com.google.android.gms.internal.zzbfn;
import com.google.android.gms.internal.zzbft;
import com.google.android.gms.internal.zzbfv;
import com.google.android.gms.internal.zzfmr;
import com.google.android.gms.phenotype.ExperimentTokens;
import java.util.ArrayList;
import java.util.TimeZone;

/* JADX INFO: loaded from: classes.dex */
@KeepForSdk
public final class ClearcutLogger {
    private final String packageName;
    private final com.google.android.gms.common.util.zze zzdir;
    private final int zzfpc;
    private String zzfpd;
    private int zzfpe;
    private int zzfpi;
    private final com.google.android.gms.clearcut.zzb zzfpj;
    private final zza zzfpl;

    @Hide
    private static Api.zzf<zzbfn> zzegu = new Api.zzf<>();

    @Hide
    private static Api.zza<zzbfn, Api.ApiOptions.NoOptions> zzegv = new com.google.android.gms.clearcut.zza();

    @Deprecated
    public static final Api<Api.ApiOptions.NoOptions> API = new Api<>("ClearcutLogger.API", zzegv, zzegu);
    private static final ExperimentTokens[] zzfoz = new ExperimentTokens[0];
    private static final String[] zzfpa = new String[0];
    private static final byte[][] zzfpb = new byte[0][];
    private String zzfpf = null;
    private String zzfpg = null;
    private final boolean zzfph = true;
    private zzc zzfpk = new zzc();

    public class LogEventBuilder {
        private String zzfpd;
        private int zzfpe;
        private String zzfpf;
        private String zzfpg;
        private int zzfpi;
        private final zzb zzfpm;
        private ArrayList<Integer> zzfpn;
        private ArrayList<String> zzfpo;
        private ArrayList<Integer> zzfpp;
        private ArrayList<ExperimentTokens> zzfpq;
        private ArrayList<byte[]> zzfpr;
        private boolean zzfps;
        private final zzfmr zzfpt;
        private boolean zzfpu;

        private LogEventBuilder(ClearcutLogger clearcutLogger, byte[] bArr) {
            this(bArr, (zzb) null);
        }

        private LogEventBuilder(byte[] bArr, zzb zzbVar) {
            this.zzfpe = ClearcutLogger.this.zzfpe;
            this.zzfpd = ClearcutLogger.this.zzfpd;
            ClearcutLogger clearcutLogger = ClearcutLogger.this;
            this.zzfpf = null;
            ClearcutLogger clearcutLogger2 = ClearcutLogger.this;
            this.zzfpg = null;
            this.zzfpi = 0;
            this.zzfpn = null;
            this.zzfpo = null;
            this.zzfpp = null;
            this.zzfpq = null;
            this.zzfpr = null;
            this.zzfps = true;
            this.zzfpt = new zzfmr();
            this.zzfpu = false;
            this.zzfpf = null;
            this.zzfpg = null;
            this.zzfpt.zzpyu = ClearcutLogger.this.zzdir.currentTimeMillis();
            this.zzfpt.zzpyv = ClearcutLogger.this.zzdir.elapsedRealtime();
            zzfmr zzfmrVar = this.zzfpt;
            zzc unused = ClearcutLogger.this.zzfpk;
            zzfmrVar.zzpzg = TimeZone.getDefault().getOffset(this.zzfpt.zzpyu) / 1000;
            if (bArr != null) {
                this.zzfpt.zzpzb = bArr;
            }
            this.zzfpm = null;
        }

        /* synthetic */ LogEventBuilder(ClearcutLogger clearcutLogger, byte[] bArr, com.google.android.gms.clearcut.zza zzaVar) {
            this(clearcutLogger, bArr);
        }

        @KeepForSdk
        public void log() {
            if (this.zzfpu) {
                throw new IllegalStateException("do not reuse LogEventBuilder");
            }
            this.zzfpu = true;
            zze zzeVar = new zze(new zzbfv(ClearcutLogger.this.packageName, ClearcutLogger.this.zzfpc, this.zzfpe, this.zzfpd, this.zzfpf, this.zzfpg, ClearcutLogger.this.zzfph, 0), this.zzfpt, null, null, ClearcutLogger.zzc((ArrayList) null), null, ClearcutLogger.zzc((ArrayList) null), null, null, this.zzfps);
            zzbfv zzbfvVar = zzeVar.zzfpz;
            if (ClearcutLogger.this.zzfpl.zzg(zzbfvVar.zzfpd, zzbfvVar.zzfpe)) {
                ClearcutLogger.this.zzfpj.zza(zzeVar);
            } else {
                PendingResults.zza(Status.zzftq, (GoogleApiClient) null);
            }
        }
    }

    public interface zza {
        boolean zzg(String str, int i);
    }

    public interface zzb {
        byte[] zzahc();
    }

    public static class zzc {
    }

    private ClearcutLogger(Context context, int i, String str, String str2, String str3, boolean z, com.google.android.gms.clearcut.zzb zzbVar, com.google.android.gms.common.util.zze zzeVar, zzc zzcVar, zza zzaVar) {
        this.zzfpe = -1;
        this.zzfpi = 0;
        this.packageName = context.getPackageName();
        this.zzfpc = zzca(context);
        this.zzfpe = -1;
        this.zzfpd = str;
        this.zzfpj = zzbVar;
        this.zzdir = zzeVar;
        this.zzfpi = 0;
        this.zzfpl = zzaVar;
        zzbq.checkArgument(true, "can't be anonymous with an upload account");
    }

    @KeepForSdk
    public static ClearcutLogger anonymousLogger(Context context, String str) {
        return new ClearcutLogger(context, -1, str, null, null, true, zzbfi.zzcb(context), zzi.zzanq(), null, new zzbft(context));
    }

    private static int[] zzb(ArrayList<Integer> arrayList) {
        if (arrayList == null) {
            return null;
        }
        int[] iArr = new int[arrayList.size()];
        ArrayList<Integer> arrayList2 = arrayList;
        int size = arrayList2.size();
        int i = 0;
        int i2 = 0;
        while (i < size) {
            Integer num = arrayList2.get(i);
            i++;
            iArr[i2] = num.intValue();
            i2++;
        }
        return iArr;
    }

    static /* synthetic */ int[] zzc(ArrayList arrayList) {
        return zzb((ArrayList<Integer>) null);
    }

    private static int zzca(Context context) {
        try {
            return context.getPackageManager().getPackageInfo(context.getPackageName(), 0).versionCode;
        } catch (PackageManager.NameNotFoundException e) {
            Log.wtf("ClearcutLogger", "This can't happen.", e);
            return 0;
        }
    }

    @KeepForSdk
    public final LogEventBuilder newEvent(byte[] bArr) {
        return new LogEventBuilder(this, bArr, (com.google.android.gms.clearcut.zza) null);
    }
}
