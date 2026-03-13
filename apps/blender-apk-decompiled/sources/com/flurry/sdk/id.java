package com.flurry.sdk;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/* JADX INFO: loaded from: classes.dex */
public final class id {
    long a;
    boolean b;
    byte[] c;

    public static class a implements kz<id> {
        @Override // com.flurry.sdk.kz
        public final /* synthetic */ void a(OutputStream outputStream, id idVar) throws IOException {
            id idVar2 = idVar;
            if (outputStream == null || idVar2 == null) {
                return;
            }
            DataOutputStream dataOutputStream = new DataOutputStream(outputStream) { // from class: com.flurry.sdk.id.a.1
                @Override // java.io.FilterOutputStream, java.io.OutputStream, java.io.Closeable, java.lang.AutoCloseable
                public final void close() {
                }
            };
            dataOutputStream.writeLong(idVar2.a);
            dataOutputStream.writeBoolean(idVar2.b);
            dataOutputStream.writeInt(idVar2.c.length);
            dataOutputStream.write(idVar2.c);
            dataOutputStream.flush();
        }

        @Override // com.flurry.sdk.kz
        public final /* synthetic */ id a(InputStream inputStream) throws IOException {
            if (inputStream == null) {
                return null;
            }
            DataInputStream dataInputStream = new DataInputStream(inputStream) { // from class: com.flurry.sdk.id.a.2
                @Override // java.io.FilterInputStream, java.io.InputStream, java.io.Closeable, java.lang.AutoCloseable
                public final void close() {
                }
            };
            id idVar = new id();
            idVar.a = dataInputStream.readLong();
            idVar.b = dataInputStream.readBoolean();
            idVar.c = new byte[dataInputStream.readInt()];
            dataInputStream.readFully(idVar.c);
            return idVar;
        }
    }
}
