package com.google.android.gms.common.api;

import android.support.annotation.NonNull;
import com.google.android.gms.common.api.Result;

/* JADX INFO: loaded from: classes.dex */
public class Response<T extends Result> {
    private T zzftm;

    public Response() {
    }

    protected Response(@NonNull T t) {
        this.zzftm = t;
    }

    @NonNull
    protected T getResult() {
        return this.zzftm;
    }

    public void setResult(@NonNull T t) {
        this.zzftm = t;
    }
}
