package com.flurry.sdk;

import java.io.Closeable;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/* JADX INFO: loaded from: classes.dex */
public final class kx<ObjectType> extends kw<ObjectType> {
    public kx(kz<ObjectType> kzVar) {
        super(kzVar);
    }

    @Override // com.flurry.sdk.kw, com.flurry.sdk.kz
    public final void a(OutputStream outputStream, ObjectType objecttype) throws Throwable {
        GZIPOutputStream gZIPOutputStream;
        if (outputStream != null) {
            GZIPOutputStream gZIPOutputStream2 = null;
            try {
                gZIPOutputStream = new GZIPOutputStream(outputStream);
            } catch (Throwable th) {
                th = th;
            }
            try {
                super.a(gZIPOutputStream, objecttype);
                lr.a(gZIPOutputStream);
            } catch (Throwable th2) {
                th = th2;
                gZIPOutputStream2 = gZIPOutputStream;
                lr.a(gZIPOutputStream2);
                throw th;
            }
        }
    }

    @Override // com.flurry.sdk.kw, com.flurry.sdk.kz
    public final ObjectType a(InputStream inputStream) throws Throwable {
        GZIPInputStream gZIPInputStream = null;
        if (inputStream == null) {
            return null;
        }
        try {
            GZIPInputStream gZIPInputStream2 = new GZIPInputStream(inputStream);
            try {
                ObjectType objecttype = (ObjectType) super.a(gZIPInputStream2);
                lr.a((Closeable) gZIPInputStream2);
                return objecttype;
            } catch (Throwable th) {
                th = th;
                gZIPInputStream = gZIPInputStream2;
                lr.a((Closeable) gZIPInputStream);
                throw th;
            }
        } catch (Throwable th2) {
            th = th2;
        }
    }
}
