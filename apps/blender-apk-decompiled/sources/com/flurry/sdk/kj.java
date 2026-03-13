package com.flurry.sdk;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

/* JADX INFO: loaded from: classes.dex */
public class kj<ObjectType> {
    private static final String a = "kj";
    private static final byte[] b = {113, -92, -8, 125, 121, 107, -65, -61, -74, -114, -32, 0, -57, -87, -35, -56, -6, -52, 51, 126, -104, 49, 79, -52, 118, -84, 99, -52, -14, -126, -27, -64};
    private String c;
    private kz<ObjectType> d;

    private static void c(byte[] bArr) {
        if (bArr == null) {
            return;
        }
        int length = bArr.length;
        int length2 = b.length;
        for (int i = 0; i < length; i++) {
            bArr[i] = (byte) ((bArr[i] ^ b[i % length2]) ^ ((i * 31) % 251));
        }
    }

    private static void d(byte[] bArr) {
        c(bArr);
    }

    public static int a(byte[] bArr) {
        if (bArr == null) {
            return 0;
        }
        jx jxVar = new jx();
        jxVar.update(bArr);
        return jxVar.b();
    }

    public kj(String str, kz<ObjectType> kzVar) {
        this.c = str;
        this.d = kzVar;
    }

    public final byte[] a(ObjectType objecttype) throws IOException {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        this.d.a(byteArrayOutputStream, objecttype);
        byte[] byteArray = byteArrayOutputStream.toByteArray();
        kf.a(3, a, "Encoding " + this.c + ": " + new String(byteArray));
        kx kxVar = new kx(new kv());
        ByteArrayOutputStream byteArrayOutputStream2 = new ByteArrayOutputStream();
        kxVar.a(byteArrayOutputStream2, byteArray);
        byte[] byteArray2 = byteArrayOutputStream2.toByteArray();
        c(byteArray2);
        return byteArray2;
    }

    public final ObjectType b(byte[] bArr) throws IOException {
        if (bArr == null) {
            throw new IOException("Decoding: " + this.c + ": Nothing to decode");
        }
        d(bArr);
        byte[] bArr2 = (byte[]) new kx(new kv()).a(new ByteArrayInputStream(bArr));
        kf.a(3, a, "Decoding: " + this.c + ": " + new String(bArr2));
        return this.d.a(new ByteArrayInputStream(bArr2));
    }
}
