package com.flurry.sdk;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;

/* JADX INFO: loaded from: classes.dex */
public class ks {
    private static final String c = "ks";
    String a;
    byte[] b;

    /* synthetic */ ks(byte b) {
        this();
    }

    public static class a implements kz<ks> {
        @Override // com.flurry.sdk.kz
        public final /* synthetic */ void a(OutputStream outputStream, ks ksVar) throws IOException {
            ks ksVar2 = ksVar;
            if (outputStream == null || ksVar2 == null) {
                return;
            }
            DataOutputStream dataOutputStream = new DataOutputStream(outputStream) { // from class: com.flurry.sdk.ks.a.1
                @Override // java.io.FilterOutputStream, java.io.OutputStream, java.io.Closeable, java.lang.AutoCloseable
                public final void close() {
                }
            };
            dataOutputStream.writeShort(ksVar2.b.length);
            dataOutputStream.write(ksVar2.b);
            dataOutputStream.writeShort(0);
            dataOutputStream.flush();
        }

        @Override // com.flurry.sdk.kz
        public final /* synthetic */ ks a(InputStream inputStream) throws IOException {
            if (inputStream == null) {
                return null;
            }
            DataInputStream dataInputStream = new DataInputStream(inputStream) { // from class: com.flurry.sdk.ks.a.2
                @Override // java.io.FilterInputStream, java.io.InputStream, java.io.Closeable, java.lang.AutoCloseable
                public final void close() {
                }
            };
            ks ksVar = new ks((byte) 0);
            int i = dataInputStream.readShort();
            if (i == 0) {
                return null;
            }
            ksVar.b = new byte[i];
            dataInputStream.readFully(ksVar.b);
            dataInputStream.readUnsignedShort();
            return ksVar;
        }
    }

    private ks() {
        this.a = null;
        this.b = null;
    }

    public ks(byte[] bArr) {
        this.a = null;
        this.b = null;
        this.a = UUID.randomUUID().toString();
        this.b = bArr;
    }

    public static String a(String str) {
        return ".yflurrydatasenderblock." + str;
    }
}
