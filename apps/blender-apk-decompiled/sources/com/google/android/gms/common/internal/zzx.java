package com.google.android.gms.common.internal;

import android.content.Intent;
import android.support.v4.app.Fragment;

/* JADX INFO: loaded from: classes.dex */
final class zzx extends zzv {
    private /* synthetic */ Fragment val$fragment;
    private /* synthetic */ Intent val$intent;
    private /* synthetic */ int val$requestCode;

    zzx(Intent intent, Fragment fragment, int i) {
        this.val$intent = intent;
        this.val$fragment = fragment;
        this.val$requestCode = i;
    }

    @Override // com.google.android.gms.common.internal.zzv
    public final void zzamo() {
        if (this.val$intent != null) {
            this.val$fragment.startActivityForResult(this.val$intent, this.val$requestCode);
        }
    }
}
