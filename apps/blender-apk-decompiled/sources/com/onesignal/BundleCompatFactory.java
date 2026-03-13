package com.onesignal;

import android.os.Build;

/* JADX INFO: compiled from: BundleCompat.java */
/* JADX INFO: loaded from: classes.dex */
class BundleCompatFactory {
    BundleCompatFactory() {
    }

    static BundleCompat getInstance() {
        if (Build.VERSION.SDK_INT >= 26) {
            return new BundleCompatPersistableBundle();
        }
        return new BundleCompatBundle();
    }
}
