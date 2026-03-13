package com.onesignal;

import android.os.PersistableBundle;
import android.support.annotation.RequiresApi;

/* JADX INFO: compiled from: BundleCompat.java */
/* JADX INFO: loaded from: classes.dex */
@RequiresApi(api = 22)
class BundleCompatPersistableBundle implements BundleCompat<PersistableBundle> {
    private PersistableBundle mBundle;

    BundleCompatPersistableBundle() {
        this.mBundle = new PersistableBundle();
    }

    BundleCompatPersistableBundle(PersistableBundle persistableBundle) {
        this.mBundle = persistableBundle;
    }

    @Override // com.onesignal.BundleCompat
    public void putString(String str, String str2) {
        this.mBundle.putString(str, str2);
    }

    @Override // com.onesignal.BundleCompat
    public void putInt(String str, Integer num) {
        this.mBundle.putInt(str, num.intValue());
    }

    @Override // com.onesignal.BundleCompat
    public void putLong(String str, Long l) {
        this.mBundle.putLong(str, l.longValue());
    }

    @Override // com.onesignal.BundleCompat
    public void putBoolean(String str, Boolean bool) {
        this.mBundle.putBoolean(str, bool.booleanValue());
    }

    @Override // com.onesignal.BundleCompat
    public String getString(String str) {
        return this.mBundle.getString(str);
    }

    @Override // com.onesignal.BundleCompat
    public Integer getInt(String str) {
        return Integer.valueOf(this.mBundle.getInt(str));
    }

    @Override // com.onesignal.BundleCompat
    public Long getLong(String str) {
        return Long.valueOf(this.mBundle.getLong(str));
    }

    @Override // com.onesignal.BundleCompat
    public boolean getBoolean(String str) {
        return this.mBundle.getBoolean(str);
    }

    @Override // com.onesignal.BundleCompat
    public boolean getBoolean(String str, boolean z) {
        return this.mBundle.getBoolean(str, z);
    }

    @Override // com.onesignal.BundleCompat
    public boolean containsKey(String str) {
        return this.mBundle.containsKey(str);
    }

    /* JADX WARN: Can't rename method to resolve collision */
    @Override // com.onesignal.BundleCompat
    public PersistableBundle getBundle() {
        return this.mBundle;
    }
}
