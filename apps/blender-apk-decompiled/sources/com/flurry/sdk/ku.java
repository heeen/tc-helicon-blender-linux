package com.flurry.sdk;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/* JADX INFO: loaded from: classes.dex */
public final class ku {
    String a;

    /* synthetic */ ku(byte b) {
        this();
    }

    public static class a implements kz<ku> {
        @Override // com.flurry.sdk.kz
        public final /* synthetic */ void a(OutputStream outputStream, ku kuVar) throws IOException {
            ku kuVar2 = kuVar;
            if (outputStream == null || kuVar2 == null) {
                return;
            }
            DataOutputStream dataOutputStream = new DataOutputStream(outputStream) { // from class: com.flurry.sdk.ku.a.1
                @Override // java.io.FilterOutputStream, java.io.OutputStream, java.io.Closeable, java.lang.AutoCloseable
                public final void close() {
                }
            };
            dataOutputStream.writeUTF(kuVar2.a);
            dataOutputStream.flush();
        }

        @Override // com.flurry.sdk.kz
        public final /* synthetic */ ku a(InputStream inputStream) throws IOException {
            if (inputStream == null) {
                return null;
            }
            DataInputStream dataInputStream = new DataInputStream(inputStream) { // from class: com.flurry.sdk.ku.a.2
                @Override // java.io.FilterInputStream, java.io.InputStream, java.io.Closeable, java.lang.AutoCloseable
                public final void close() {
                }
            };
            ku kuVar = new ku((byte) 0);
            kuVar.a = dataInputStream.readUTF();
            return kuVar;
        }
    }

    private ku() {
    }

    public ku(String str) {
        this.a = str;
    }
}
