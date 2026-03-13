package com.google.android.gms.internal;

import android.database.ContentObserver;
import android.os.Handler;

/* JADX INFO: loaded from: classes.dex */
final class zzdnn extends ContentObserver {
    zzdnn(Handler handler) {
        super(null);
    }

    @Override // android.database.ContentObserver
    public final void onChange(boolean z) {
        zzdnm.zzlxi.set(true);
    }
}
