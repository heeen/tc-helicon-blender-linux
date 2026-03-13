package com.flurry.sdk;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/* JADX INFO: loaded from: classes.dex */
public class ij {
    private static final String m = "com.flurry.sdk.ij";
    public int a;
    public long b;
    public long c;
    public boolean d;
    public String g;
    public int h;
    public long i;
    public boolean j;
    public ii l;
    public long k = 0;
    public int e = 0;
    public ik f = ik.PENDING_COMPLETION;

    public ij(ii iiVar, long j, long j2, int i) {
        this.l = iiVar;
        this.b = j;
        this.c = j2;
        this.a = i;
    }

    public final void a() {
        this.l.a.add(this);
        if (this.d) {
            this.l.l = true;
        }
    }

    public static class a implements kz<ij> {
        @Override // com.flurry.sdk.kz
        public final /* synthetic */ void a(OutputStream outputStream, ij ijVar) throws IOException {
            ij ijVar2 = ijVar;
            if (outputStream == null || ijVar2 == null) {
                return;
            }
            DataOutputStream dataOutputStream = new DataOutputStream(outputStream) { // from class: com.flurry.sdk.ij.a.1
                @Override // java.io.FilterOutputStream, java.io.OutputStream, java.io.Closeable, java.lang.AutoCloseable
                public final void close() {
                }
            };
            dataOutputStream.writeInt(ijVar2.a);
            dataOutputStream.writeLong(ijVar2.b);
            dataOutputStream.writeLong(ijVar2.c);
            dataOutputStream.writeBoolean(ijVar2.d);
            dataOutputStream.writeInt(ijVar2.e);
            dataOutputStream.writeInt(ijVar2.f.e);
            if (ijVar2.g != null) {
                dataOutputStream.writeUTF(ijVar2.g);
            } else {
                dataOutputStream.writeUTF("");
            }
            dataOutputStream.writeInt(ijVar2.h);
            dataOutputStream.writeLong(ijVar2.i);
            dataOutputStream.writeBoolean(ijVar2.j);
            dataOutputStream.writeLong(ijVar2.k);
            dataOutputStream.flush();
        }

        @Override // com.flurry.sdk.kz
        public final /* synthetic */ ij a(InputStream inputStream) throws IOException {
            if (inputStream == null) {
                return null;
            }
            DataInputStream dataInputStream = new DataInputStream(inputStream) { // from class: com.flurry.sdk.ij.a.2
                @Override // java.io.FilterInputStream, java.io.InputStream, java.io.Closeable, java.lang.AutoCloseable
                public final void close() {
                }
            };
            int i = dataInputStream.readInt();
            long j = dataInputStream.readLong();
            long j2 = dataInputStream.readLong();
            boolean z = dataInputStream.readBoolean();
            int i2 = dataInputStream.readInt();
            ik ikVarA = ik.a(dataInputStream.readInt());
            String utf = dataInputStream.readUTF();
            int i3 = dataInputStream.readInt();
            long j3 = dataInputStream.readLong();
            boolean z2 = dataInputStream.readBoolean();
            long j4 = dataInputStream.readLong();
            ij ijVar = new ij(null, j, j2, i);
            ijVar.d = z;
            ijVar.e = i2;
            ijVar.f = ikVarA;
            ijVar.g = utf;
            ijVar.h = i3;
            ijVar.i = j3;
            ijVar.j = z2;
            ijVar.k = j4;
            return ijVar;
        }
    }
}
