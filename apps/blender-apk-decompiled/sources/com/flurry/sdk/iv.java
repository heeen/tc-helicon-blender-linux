package com.flurry.sdk;

import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/* JADX INFO: loaded from: classes.dex */
public final class iv {
    public String a;
    public boolean b;
    public boolean c;
    public long d;
    private final Map<String, String> e = new HashMap();
    private int f;
    private long g;

    public iv(int i, String str, Map<String, String> map, long j, boolean z) {
        this.f = i;
        this.a = str;
        if (map != null) {
            this.e.putAll(map);
        }
        this.g = j;
        this.b = z;
        if (this.b) {
            this.c = false;
        } else {
            this.c = true;
        }
    }

    public final void a(long j) {
        this.c = true;
        this.d = j - this.g;
        kf.a(3, "FlurryAgent", "Ended event '" + this.a + "' (" + this.g + ") after " + this.d + "ms");
    }

    public final synchronized void a(Map<String, String> map) {
        if (map != null) {
            this.e.putAll(map);
        }
    }

    public final synchronized Map<String, String> a() {
        return new HashMap(this.e);
    }

    public final synchronized void b(Map<String, String> map) {
        this.e.clear();
        this.e.putAll(map);
    }

    /* JADX WARN: Multi-variable type inference failed */
    /* JADX WARN: Type inference failed for: r0v18 */
    /* JADX WARN: Type inference failed for: r0v5, types: [java.io.Closeable] */
    /* JADX WARN: Type inference failed for: r0v9 */
    /* JADX WARN: Type inference failed for: r2v0 */
    /* JADX WARN: Type inference failed for: r2v1, types: [java.io.Closeable] */
    /* JADX WARN: Type inference failed for: r2v2, types: [java.io.Closeable, java.io.DataOutputStream] */
    public final synchronized byte[] b() {
        byte[] byteArray;
        ?? dataOutputStream;
        Throwable th;
        ByteArrayOutputStream byteArrayOutputStream;
        byteArray = null;
        ?? r0 = 0;
        try {
            try {
                byteArrayOutputStream = new ByteArrayOutputStream();
                dataOutputStream = new DataOutputStream(byteArrayOutputStream);
            } catch (Throwable th2) {
                dataOutputStream = byteArray;
                th = th2;
            }
        } catch (IOException unused) {
        }
        try {
            dataOutputStream.writeShort(this.f);
            dataOutputStream.writeUTF(this.a);
            dataOutputStream.writeShort(this.e.size());
            for (Map.Entry<String, String> entry : this.e.entrySet()) {
                dataOutputStream.writeUTF(lr.b(entry.getKey()));
                dataOutputStream.writeUTF(lr.b(entry.getValue()));
            }
            dataOutputStream.writeLong(this.g);
            dataOutputStream.writeLong(this.d);
            dataOutputStream.flush();
            byteArray = byteArrayOutputStream.toByteArray();
            lr.a((Closeable) dataOutputStream);
        } catch (IOException unused2) {
            r0 = dataOutputStream;
            byte[] bArr = new byte[0];
            lr.a((Closeable) r0);
            byteArray = bArr;
        } catch (Throwable th3) {
            th = th3;
            lr.a((Closeable) dataOutputStream);
            throw th;
        }
        return byteArray;
    }
}
