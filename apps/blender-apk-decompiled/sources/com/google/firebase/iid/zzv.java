package com.google.firebase.iid;

import android.os.Bundle;

/* JADX INFO: loaded from: classes.dex */
final class zzv extends zzt<Bundle> {
    zzv(int i, int i2, Bundle bundle) {
        super(i, 1, bundle);
    }

    @Override // com.google.firebase.iid.zzt
    final boolean zzaww() {
        return false;
    }

    @Override // com.google.firebase.iid.zzt
    final void zzx(Bundle bundle) {
        Bundle bundle2 = bundle.getBundle("data");
        if (bundle2 == null) {
            bundle2 = Bundle.EMPTY;
        }
        finish(bundle2);
    }
}
