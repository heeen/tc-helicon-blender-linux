package com.flurry.sdk;

import com.flurry.sdk.ij;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/* JADX INFO: loaded from: classes.dex */
public class ii extends kp {
    private static final String t = "com.flurry.sdk.ii";
    public ArrayList<ij> a;
    final long b;
    final int c;
    final int d;
    final ip e;
    final Map<String, String> f;
    long g;
    int h;
    int i;
    String j;
    String k;
    boolean l;
    public im m;
    private final int u = 1000;
    private final int v = 30000;

    public ii(String str, long j, String str2, long j2, int i, int i2, ip ipVar, Map<String, String> map, int i3, int i4, String str3) {
        a(str2);
        this.n = j2;
        a_();
        this.k = str;
        this.b = j;
        this.s = i;
        this.c = i;
        this.d = i2;
        this.e = ipVar;
        this.f = map;
        this.h = i3;
        this.i = i4;
        this.j = str3;
        this.g = 30000L;
        this.a = new ArrayList<>();
    }

    @Override // com.flurry.sdk.kp
    public final void a_() {
        super.a_();
        if (this.p != 1) {
            this.g *= 3;
        }
    }

    public final synchronized void c() {
        this.m.c();
    }

    public final void d() {
        Iterator<ij> it = this.a.iterator();
        while (it.hasNext()) {
            it.next().l = this;
        }
    }

    public static class a implements kz<ii> {
        ky<ij> a = new ky<>(new ij.a());

        @Override // com.flurry.sdk.kz
        public final /* synthetic */ void a(OutputStream outputStream, ii iiVar) throws IOException {
            ii iiVar2 = iiVar;
            if (outputStream == null || iiVar2 == null) {
                return;
            }
            DataOutputStream dataOutputStream = new DataOutputStream(outputStream) { // from class: com.flurry.sdk.ii.a.1
                @Override // java.io.FilterOutputStream, java.io.OutputStream, java.io.Closeable, java.lang.AutoCloseable
                public final void close() {
                }
            };
            if (iiVar2.k != null) {
                dataOutputStream.writeUTF(iiVar2.k);
            } else {
                dataOutputStream.writeUTF("");
            }
            if (iiVar2.r != null) {
                dataOutputStream.writeUTF(iiVar2.r);
            } else {
                dataOutputStream.writeUTF("");
            }
            dataOutputStream.writeLong(iiVar2.n);
            dataOutputStream.writeInt(iiVar2.p);
            dataOutputStream.writeLong(iiVar2.b);
            dataOutputStream.writeInt(iiVar2.c);
            dataOutputStream.writeInt(iiVar2.d);
            dataOutputStream.writeInt(iiVar2.e.e);
            Map map = iiVar2.f;
            if (map != null) {
                dataOutputStream.writeInt(iiVar2.f.size());
                for (String str : iiVar2.f.keySet()) {
                    dataOutputStream.writeUTF(str);
                    dataOutputStream.writeUTF((String) map.get(str));
                }
            } else {
                dataOutputStream.writeInt(0);
            }
            dataOutputStream.writeLong(iiVar2.g);
            dataOutputStream.writeInt(iiVar2.h);
            dataOutputStream.writeInt(iiVar2.i);
            if (iiVar2.j != null) {
                dataOutputStream.writeUTF(iiVar2.j);
            } else {
                dataOutputStream.writeUTF("");
            }
            dataOutputStream.writeBoolean(iiVar2.l);
            dataOutputStream.flush();
            this.a.a(outputStream, (List<ij>) iiVar2.a);
        }

        @Override // com.flurry.sdk.kz
        public final /* synthetic */ ii a(InputStream inputStream) throws IOException {
            HashMap map;
            if (inputStream == null) {
                return null;
            }
            DataInputStream dataInputStream = new DataInputStream(inputStream) { // from class: com.flurry.sdk.ii.a.2
                @Override // java.io.FilterInputStream, java.io.InputStream, java.io.Closeable, java.lang.AutoCloseable
                public final void close() {
                }
            };
            String utf = dataInputStream.readUTF();
            String str = utf.equals("") ? null : utf;
            String utf2 = dataInputStream.readUTF();
            long j = dataInputStream.readLong();
            int i = dataInputStream.readInt();
            long j2 = dataInputStream.readLong();
            int i2 = dataInputStream.readInt();
            int i3 = dataInputStream.readInt();
            ip ipVarA = ip.a(dataInputStream.readInt());
            int i4 = dataInputStream.readInt();
            if (i4 != 0) {
                HashMap map2 = new HashMap();
                int i5 = 0;
                while (i5 < i4) {
                    map2.put(dataInputStream.readUTF(), dataInputStream.readUTF());
                    i5++;
                    i4 = i4;
                }
                map = map2;
            } else {
                map = null;
            }
            long j3 = dataInputStream.readLong();
            int i6 = dataInputStream.readInt();
            int i7 = dataInputStream.readInt();
            String utf3 = dataInputStream.readUTF();
            String str2 = utf3.equals("") ? null : utf3;
            boolean z = dataInputStream.readBoolean();
            ii iiVar = new ii(str, j2, utf2, j, i2, i3, ipVarA, map, i6, i7, str2);
            iiVar.g = j3;
            iiVar.l = z;
            iiVar.p = i;
            iiVar.a = (ArrayList) this.a.a(inputStream);
            iiVar.d();
            return iiVar;
        }
    }
}
