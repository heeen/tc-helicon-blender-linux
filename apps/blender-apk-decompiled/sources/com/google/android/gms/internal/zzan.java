package com.google.android.gms.internal;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;

/* JADX INFO: loaded from: classes.dex */
final class zzan {
    final String key;
    final String zza;
    final long zzb;
    final long zzc;
    long zzca;
    final long zzd;
    final long zze;
    final List<zzl> zzg;

    zzan(String str, zzc zzcVar) {
        this(str, zzcVar.zza, zzcVar.zzb, zzcVar.zzc, zzcVar.zzd, zzcVar.zze, zzcVar.zzg != null ? zzcVar.zzg : zzap.zza(zzcVar.zzf));
        this.zzca = zzcVar.data.length;
    }

    private zzan(String str, String str2, long j, long j2, long j3, long j4, List<zzl> list) {
        this.key = str;
        this.zza = "".equals(str2) ? null : str2;
        this.zzb = j;
        this.zzc = j2;
        this.zzd = j3;
        this.zze = j4;
        this.zzg = list;
    }

    static zzan zzc(zzao zzaoVar) throws IOException {
        if (zzam.zzb((InputStream) zzaoVar) == 538247942) {
            return new zzan(zzam.zza(zzaoVar), zzam.zza(zzaoVar), zzam.zzc(zzaoVar), zzam.zzc(zzaoVar), zzam.zzc(zzaoVar), zzam.zzc(zzaoVar), zzam.zzb(zzaoVar));
        }
        throw new IOException();
    }

    final boolean zza(OutputStream outputStream) {
        try {
            zzam.zza(outputStream, 538247942);
            zzam.zza(outputStream, this.key);
            zzam.zza(outputStream, this.zza == null ? "" : this.zza);
            zzam.zza(outputStream, this.zzb);
            zzam.zza(outputStream, this.zzc);
            zzam.zza(outputStream, this.zzd);
            zzam.zza(outputStream, this.zze);
            List<zzl> list = this.zzg;
            if (list != null) {
                zzam.zza(outputStream, list.size());
                for (zzl zzlVar : list) {
                    zzam.zza(outputStream, zzlVar.getName());
                    zzam.zza(outputStream, zzlVar.getValue());
                }
            } else {
                zzam.zza(outputStream, 0);
            }
            outputStream.flush();
            return true;
        } catch (IOException e) {
            zzaf.zzb("%s", e.toString());
            return false;
        }
    }
}
