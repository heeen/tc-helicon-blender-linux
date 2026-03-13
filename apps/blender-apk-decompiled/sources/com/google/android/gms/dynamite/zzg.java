package com.google.android.gms.dynamite;

import android.content.Context;
import com.google.android.gms.dynamite.DynamiteModule;

/* JADX INFO: loaded from: classes.dex */
final class zzg implements DynamiteModule.zzd {
    zzg() {
    }

    @Override // com.google.android.gms.dynamite.DynamiteModule.zzd
    public final zzj zza(Context context, String str, zzi zziVar) throws DynamiteModule.zzc {
        zzj zzjVar = new zzj();
        zzjVar.zzhds = zziVar.zzx(context, str);
        zzjVar.zzhdt = zzjVar.zzhds != 0 ? zziVar.zzc(context, str, false) : zziVar.zzc(context, str, true);
        if (zzjVar.zzhds == 0 && zzjVar.zzhdt == 0) {
            zzjVar.zzhdu = 0;
        } else if (zzjVar.zzhdt >= zzjVar.zzhds) {
            zzjVar.zzhdu = 1;
        } else {
            zzjVar.zzhdu = -1;
        }
        return zzjVar;
    }
}
