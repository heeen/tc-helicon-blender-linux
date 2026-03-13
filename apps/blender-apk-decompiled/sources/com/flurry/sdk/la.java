package com.flurry.sdk;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/* JADX INFO: loaded from: classes.dex */
public final class la implements kz<String> {
    @Override // com.flurry.sdk.kz
    public final /* synthetic */ void a(OutputStream outputStream, String str) throws IOException {
        String str2 = str;
        if (outputStream == null || str2 == null) {
            return;
        }
        byte[] bytes = str2.getBytes("utf-8");
        outputStream.write(bytes, 0, bytes.length);
    }

    @Override // com.flurry.sdk.kz
    public final /* synthetic */ String a(InputStream inputStream) throws IOException {
        if (inputStream == null) {
            return null;
        }
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        lr.a(inputStream, byteArrayOutputStream);
        return byteArrayOutputStream.toString();
    }
}
