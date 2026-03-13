package com.google.android.gms.dynamite;

import android.content.Context;
import com.google.android.gms.dynamite.DynamiteModule;

/* JADX INFO: loaded from: classes.dex */
final class zzb implements DynamiteModule.zzd {
    zzb() {
    }

    @Override // com.google.android.gms.dynamite.DynamiteModule.zzd
    public final zzj zza(Context context, String str, zzi zziVar) throws DynamiteModule.zzc {
        zzj zzjVar = new zzj();
        zzjVar.zzhdt = zziVar.zzc(context, str, true);
        if (zzjVar.zzhdt != 0) {
            zzjVar.zzhdu = 1;
        } else {
            zzjVar.zzhds = zziVar.zzx(context, str);
            if (zzjVar.zzhds != 0) {
                zzjVar.zzhdu = -1;
            }
        }
        return zzjVar;
    }
}
