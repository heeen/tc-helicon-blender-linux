package com.flurry.sdk;

import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.DataOutputStream;
import java.io.IOException;

/* JADX INFO: loaded from: classes.dex */
public final class it {
    public String a;
    private int b;
    private long c;
    private String d;
    private String e;
    private Throwable f;

    public it(int i, long j, String str, String str2, String str3, Throwable th) {
        this.b = i;
        this.c = j;
        this.a = str;
        this.d = str2;
        this.e = str3;
        this.f = th;
    }

    /* JADX WARN: Multi-variable type inference failed */
    /* JADX WARN: Type inference failed for: r1v0 */
    /* JADX WARN: Type inference failed for: r1v1 */
    /* JADX WARN: Type inference failed for: r1v10 */
    /* JADX WARN: Type inference failed for: r1v15 */
    /* JADX WARN: Type inference failed for: r1v16 */
    /* JADX WARN: Type inference failed for: r1v17 */
    /* JADX WARN: Type inference failed for: r1v18 */
    /* JADX WARN: Type inference failed for: r1v19 */
    /* JADX WARN: Type inference failed for: r1v2, types: [java.io.Closeable] */
    /* JADX WARN: Type inference failed for: r1v3 */
    /* JADX WARN: Type inference failed for: r1v4 */
    /* JADX WARN: Type inference failed for: r3v0 */
    /* JADX WARN: Type inference failed for: r3v1, types: [java.io.Closeable] */
    /* JADX WARN: Type inference failed for: r3v2, types: [java.io.Closeable, java.io.DataOutputStream] */
    public final byte[] a() throws Throwable {
        ?? dataOutputStream;
        byte[] byteArray;
        ByteArrayOutputStream byteArrayOutputStream;
        ?? r1;
        ?? r12 = 0;
        ?? r13 = 0;
        try {
            try {
                byteArrayOutputStream = new ByteArrayOutputStream();
                dataOutputStream = new DataOutputStream(byteArrayOutputStream);
            } catch (Throwable th) {
                th = th;
                dataOutputStream = r12;
            }
        } catch (IOException unused) {
        }
        try {
            dataOutputStream.writeShort(this.b);
            dataOutputStream.writeLong(this.c);
            dataOutputStream.writeUTF(this.a);
            dataOutputStream.writeUTF(this.d);
            dataOutputStream.writeUTF(this.e);
            Throwable th2 = this.f;
            if (th2 != null) {
                if ("uncaught".equals(this.a)) {
                    dataOutputStream.writeByte(3);
                } else {
                    dataOutputStream.writeByte(2);
                }
                dataOutputStream.writeByte(2);
                StringBuilder sb = new StringBuilder("");
                String property = System.getProperty("line.separator");
                for (StackTraceElement stackTraceElement : this.f.getStackTrace()) {
                    sb.append(stackTraceElement);
                    sb.append(property);
                }
                if (this.f.getCause() != null) {
                    sb.append(property);
                    sb.append("Caused by: ");
                    for (StackTraceElement stackTraceElement2 : this.f.getCause().getStackTrace()) {
                        sb.append(stackTraceElement2);
                        sb.append(property);
                    }
                }
                byte[] bytes = sb.toString().getBytes();
                int length = bytes.length;
                dataOutputStream.writeInt(length);
                dataOutputStream.write(bytes);
                r1 = length;
            } else {
                dataOutputStream.writeByte(1);
                dataOutputStream.writeByte(0);
                r1 = th2;
            }
            dataOutputStream.flush();
            byteArray = byteArrayOutputStream.toByteArray();
            lr.a((Closeable) dataOutputStream);
            r12 = r1;
        } catch (IOException unused2) {
            r13 = dataOutputStream;
            byteArray = new byte[0];
            lr.a((Closeable) r13);
            r12 = r13;
        } catch (Throwable th3) {
            th = th3;
            lr.a((Closeable) dataOutputStream);
            throw th;
        }
        return byteArray;
    }
}
