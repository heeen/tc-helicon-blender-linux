package com.google.android.gms.internal;

import java.io.UnsupportedEncodingException;

/* JADX INFO: loaded from: classes.dex */
public class zzav extends zzr<String> {
    private final Object mLock;
    private zzz<String> zzci;

    public zzav(int i, String str, zzz<String> zzzVar, zzy zzyVar) {
        super(i, str, zzyVar);
        this.mLock = new Object();
        this.zzci = zzzVar;
    }

    @Override // com.google.android.gms.internal.zzr
    protected final zzx<String> zza(zzp zzpVar) {
        String str;
        try {
            str = new String(zzpVar.data, zzap.zzb(zzpVar.zzab));
        } catch (UnsupportedEncodingException unused) {
            str = new String(zzpVar.data);
        }
        return zzx.zza(str, zzap.zzb(zzpVar));
    }

    /* JADX INFO: Access modifiers changed from: protected */
    @Override // com.google.android.gms.internal.zzr
    /* JADX INFO: renamed from: zzh, reason: merged with bridge method [inline-methods] */
    public void zza(String str) {
        zzz<String> zzzVar;
        synchronized (this.mLock) {
            zzzVar = this.zzci;
        }
        if (zzzVar != null) {
            zzzVar.zzb(str);
        }
    }
}
