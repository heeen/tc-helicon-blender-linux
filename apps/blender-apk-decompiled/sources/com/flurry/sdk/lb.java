package com.flurry.sdk;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/* JADX INFO: loaded from: classes.dex */
public final class lb<T> implements kz<T> {
    private final String a;
    private final int b;
    private final lc<T> c;

    public lb(String str, int i, lc<T> lcVar) {
        this.a = str;
        this.b = i;
        this.c = lcVar;
    }

    @Override // com.flurry.sdk.kz
    public final void a(OutputStream outputStream, T t) throws IOException {
        if (outputStream == null || this.c == null) {
            return;
        }
        DataOutputStream dataOutputStream = new DataOutputStream(outputStream) { // from class: com.flurry.sdk.lb.1
            @Override // java.io.FilterOutputStream, java.io.OutputStream, java.io.Closeable, java.lang.AutoCloseable
            public final void close() {
            }
        };
        dataOutputStream.writeUTF(this.a);
        dataOutputStream.writeInt(this.b);
        this.c.a(this.b).a(dataOutputStream, t);
        dataOutputStream.flush();
    }

    @Override // com.flurry.sdk.kz
    public final T a(InputStream inputStream) throws IOException {
        if (inputStream == null || this.c == null) {
            return null;
        }
        DataInputStream dataInputStream = new DataInputStream(inputStream) { // from class: com.flurry.sdk.lb.2
            @Override // java.io.FilterInputStream, java.io.InputStream, java.io.Closeable, java.lang.AutoCloseable
            public final void close() {
            }
        };
        String utf = dataInputStream.readUTF();
        if (!this.a.equals(utf)) {
            throw new IOException("Signature: " + utf + " is invalid");
        }
        return this.c.a(dataInputStream.readInt()).a(dataInputStream);
    }
}
