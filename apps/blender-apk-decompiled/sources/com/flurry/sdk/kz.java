package com.flurry.sdk;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/* JADX INFO: loaded from: classes.dex */
public interface kz<ObjectType> {
    ObjectType a(InputStream inputStream) throws IOException;

    void a(OutputStream outputStream, ObjectType objecttype) throws IOException;
}
