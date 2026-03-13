package com.flurry.sdk;

import android.text.TextUtils;

/* JADX INFO: loaded from: classes.dex */
public abstract class jz {
    protected String g;

    public jz(String str) {
        this.g = "com.flurry.android.sdk.ReplaceMeWithAProperEventName";
        if (TextUtils.isEmpty(str)) {
            throw new IllegalArgumentException("Event must have a name!");
        }
        this.g = str;
    }

    public final String a() {
        return this.g;
    }

    public final void b() {
        kb.a().a(this);
    }
}
