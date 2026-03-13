package com.google.android.gms.dynamic;

import android.content.Context;
import android.os.IBinder;
import com.google.android.gms.common.internal.zzbq;
import com.google.android.gms.common.zzs;

/* JADX INFO: loaded from: classes.dex */
public abstract class zzp<T> {
    private final String zzhcz;
    private T zzhda;

    protected zzp(String str) {
        this.zzhcz = str;
    }

    protected final T zzdg(Context context) throws zzq {
        if (this.zzhda == null) {
            zzbq.checkNotNull(context);
            Context remoteContext = zzs.getRemoteContext(context);
            if (remoteContext == null) {
                throw new zzq("Could not get remote context.");
            }
            try {
                this.zzhda = zze((IBinder) remoteContext.getClassLoader().loadClass(this.zzhcz).newInstance());
            } catch (ClassNotFoundException e) {
                throw new zzq("Could not load creator class.", e);
            } catch (IllegalAccessException e2) {
                throw new zzq("Could not access creator.", e2);
            } catch (InstantiationException e3) {
                throw new zzq("Could not instantiate creator.", e3);
            }
        }
        return this.zzhda;
    }

    protected abstract T zze(IBinder iBinder);
}
