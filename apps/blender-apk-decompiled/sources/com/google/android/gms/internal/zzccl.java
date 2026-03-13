package com.google.android.gms.internal;

import android.content.Context;
import android.os.RemoteException;
import android.util.Log;
import com.google.android.gms.common.internal.Hide;
import com.google.android.gms.dynamite.DynamiteModule;
import com.google.android.gms.dynamite.descriptors.com.google.android.gms.flags.ModuleDescriptor;

/* JADX INFO: loaded from: classes.dex */
@Hide
public final class zzccl {
    private boolean zzarf = false;
    private zzccm zzhqe = null;

    public final void initialize(Context context) {
        synchronized (this) {
            if (this.zzarf) {
                return;
            }
            try {
                this.zzhqe = zzccn.asInterface(DynamiteModule.zza(context, DynamiteModule.zzhdn, ModuleDescriptor.MODULE_ID).zzhk("com.google.android.gms.flags.impl.FlagProviderImpl"));
                this.zzhqe.init(com.google.android.gms.dynamic.zzn.zzz(context));
                this.zzarf = true;
            } catch (RemoteException | DynamiteModule.zzc e) {
                Log.w("FlagValueProvider", "Failed to initialize flags module.", e);
            }
        }
    }

    public final <T> T zzb(zzcce<T> zzcceVar) {
        synchronized (this) {
            if (this.zzarf) {
                return zzcceVar.zza(this.zzhqe);
            }
            return zzcceVar.zzje();
        }
    }
}
