package com.flurry.sdk;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/* JADX INFO: loaded from: classes.dex */
public final class io {
    byte[] a;

    public io() {
    }

    public io(byte[] bArr) {
        this.a = bArr;
    }

    public static class a implements kz<io> {
        @Override // com.flurry.sdk.kz
        public final /* synthetic */ void a(OutputStream outputStream, io ioVar) throws IOException {
            io ioVar2 = ioVar;
            if (outputStream == null || ioVar2 == null) {
                return;
            }
            DataOutputStream dataOutputStream = new DataOutputStream(outputStream) { // from class: com.flurry.sdk.io.a.1
                @Override // java.io.FilterOutputStream, java.io.OutputStream, java.io.Closeable, java.lang.AutoCloseable
                public final void close() {
                }
            };
            dataOutputStream.writeShort(ioVar2.a.length);
            dataOutputStream.write(ioVar2.a);
            dataOutputStream.writeShort(0);
            dataOutputStream.flush();
        }

        @Override // com.flurry.sdk.kz
        public final /* synthetic */ io a(InputStream inputStream) throws IOException {
            DataInputStream dataInputStream;
            int i;
            if (inputStream == null || (i = (dataInputStream = new DataInputStream(inputStream) { // from class: com.flurry.sdk.io.a.2
                @Override // java.io.FilterInputStream, java.io.InputStream, java.io.Closeable, java.lang.AutoCloseable
                public final void close() {
                }
            }).readShort()) == 0) {
                return null;
            }
            io ioVar = new io();
            ioVar.a = new byte[i];
            dataInputStream.readFully(ioVar.a);
            dataInputStream.readUnsignedShort();
            return ioVar;
        }
    }
}
