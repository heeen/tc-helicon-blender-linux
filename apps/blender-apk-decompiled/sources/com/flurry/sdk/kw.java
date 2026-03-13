package com.flurry.sdk;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/* JADX INFO: loaded from: classes.dex */
public class kw<ObjectType> implements kz<ObjectType> {
    protected final kz<ObjectType> a;

    public kw(kz<ObjectType> kzVar) {
        this.a = kzVar;
    }

    @Override // com.flurry.sdk.kz
    public void a(OutputStream outputStream, ObjectType objecttype) throws IOException {
        if (this.a == null || outputStream == null || objecttype == null) {
            return;
        }
        this.a.a(outputStream, objecttype);
    }

    @Override // com.flurry.sdk.kz
    public ObjectType a(InputStream inputStream) throws IOException {
        if (this.a == null || inputStream == null) {
            return null;
        }
        return this.a.a(inputStream);
    }
}
