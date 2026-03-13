package com.google.android.gms.dynamite;

import android.content.Context;
import com.google.android.gms.dynamite.DynamiteModule;

/* JADX INFO: loaded from: classes.dex */
final class zzd implements DynamiteModule.zzd {
    zzd() {
    }

    @Override // com.google.android.gms.dynamite.DynamiteModule.zzd
    public final zzj zza(Context context, String str, zzi zziVar) throws DynamiteModule.zzc {
        int i;
        zzj zzjVar = new zzj();
        zzjVar.zzhds = zziVar.zzx(context, str);
        zzjVar.zzhdt = zziVar.zzc(context, str, true);
        if (zzjVar.zzhds == 0 && zzjVar.zzhdt == 0) {
            i = 0;
        } else {
            if (zzjVar.zzhds < zzjVar.zzhdt) {
                zzjVar.zzhdu = 1;
                return zzjVar;
            }
            i = -1;
        }
        zzjVar.zzhdu = i;
        return zzjVar;
    }
}
