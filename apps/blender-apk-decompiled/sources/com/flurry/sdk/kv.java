package com.flurry.sdk;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/* JADX INFO: loaded from: classes.dex */
public final class kv implements kz<byte[]> {
    @Override // com.flurry.sdk.kz
    public final /* synthetic */ void a(OutputStream outputStream, byte[] bArr) throws IOException {
        byte[] bArr2 = bArr;
        if (outputStream == null || bArr2 == null) {
            return;
        }
        outputStream.write(bArr2, 0, bArr2.length);
    }

    @Override // com.flurry.sdk.kz
    public final /* synthetic */ byte[] a(InputStream inputStream) throws IOException {
        if (inputStream == null) {
            return null;
        }
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        lr.a(inputStream, byteArrayOutputStream);
        return byteArrayOutputStream.toByteArray();
    }
}
