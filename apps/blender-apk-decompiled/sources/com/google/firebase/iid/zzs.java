package com.google.firebase.iid;

import android.os.Bundle;

/* JADX INFO: loaded from: classes.dex */
final class zzs extends zzt<Void> {
    zzs(int i, int i2, Bundle bundle) {
        super(i, 2, bundle);
    }

    @Override // com.google.firebase.iid.zzt
    final boolean zzaww() {
        return true;
    }

    @Override // com.google.firebase.iid.zzt
    final void zzx(Bundle bundle) {
        if (bundle.getBoolean("ack", false)) {
            finish(null);
        } else {
            zzb(new zzu(4, "Invalid response to one way request"));
        }
    }
}
