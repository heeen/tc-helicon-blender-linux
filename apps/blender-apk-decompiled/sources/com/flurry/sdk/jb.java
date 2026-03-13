package com.flurry.sdk;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

/* JADX INFO: loaded from: classes.dex */
public class jb {
    private static final String d = "jb";
    boolean a;
    long b;
    final List<iy> c = new ArrayList();

    public static class a implements kz<jb> {
        @Override // com.flurry.sdk.kz
        public final /* synthetic */ jb a(InputStream inputStream) throws IOException {
            if (inputStream == null) {
                return null;
            }
            DataInputStream dataInputStream = new DataInputStream(inputStream) { // from class: com.flurry.sdk.jb.a.1
                @Override // java.io.FilterInputStream, java.io.InputStream, java.io.Closeable, java.lang.AutoCloseable
                public final void close() {
                }
            };
            jb jbVar = new jb();
            dataInputStream.readUTF();
            dataInputStream.readUTF();
            jbVar.a = dataInputStream.readBoolean();
            jbVar.b = dataInputStream.readLong();
            while (true) {
                int unsignedShort = dataInputStream.readUnsignedShort();
                if (unsignedShort == 0) {
                    return jbVar;
                }
                byte[] bArr = new byte[unsignedShort];
                dataInputStream.readFully(bArr);
                jbVar.c.add(0, new iy(bArr));
            }
        }

        @Override // com.flurry.sdk.kz
        public final /* synthetic */ void a(OutputStream outputStream, jb jbVar) throws IOException {
            throw new UnsupportedOperationException("Serialization not supported");
        }
    }
}
