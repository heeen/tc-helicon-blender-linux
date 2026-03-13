package com.flurry.sdk;

import com.flurry.sdk.jb;
import java.io.Closeable;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;

/* JADX INFO: loaded from: classes.dex */
public final class hn {
    private static final String a = "hn";

    /* JADX WARN: Multi-variable type inference failed */
    /* JADX WARN: Type inference failed for: r9v0, types: [java.io.File] */
    /* JADX WARN: Type inference failed for: r9v2 */
    /* JADX WARN: Type inference failed for: r9v4, types: [java.io.Closeable] */
    /* JADX WARN: Type inference failed for: r9v7 */
    public static jb a(File file) throws Throwable {
        FileInputStream fileInputStream;
        Throwable th;
        DataInputStream dataInputStream;
        if (file == 0 || !file.exists()) {
            return null;
        }
        jb.a aVar = new jb.a();
        try {
            try {
                fileInputStream = new FileInputStream((File) file);
            } catch (Throwable th2) {
                th = th2;
            }
        } catch (Exception e) {
            e = e;
            dataInputStream = null;
            fileInputStream = null;
        } catch (Throwable th3) {
            fileInputStream = null;
            th = th3;
            file = 0;
        }
        try {
            dataInputStream = new DataInputStream(fileInputStream);
            try {
                if (dataInputStream.readUnsignedShort() != 46586) {
                    kf.a(3, a, "Unexpected file type");
                } else {
                    int unsignedShort = dataInputStream.readUnsignedShort();
                    if (unsignedShort == 2) {
                        jb jbVar = (jb) aVar.a(dataInputStream);
                        lr.a((Closeable) dataInputStream);
                        lr.a((Closeable) fileInputStream);
                        return jbVar;
                    }
                    kf.a(6, a, "Unknown agent file version: " + unsignedShort);
                }
                lr.a((Closeable) dataInputStream);
                lr.a((Closeable) fileInputStream);
                return null;
            } catch (Exception e2) {
                e = e2;
                kf.a(3, a, "Error loading legacy agent data.", e);
                lr.a((Closeable) dataInputStream);
                lr.a((Closeable) fileInputStream);
                return null;
            }
        } catch (Exception e3) {
            e = e3;
            dataInputStream = null;
        } catch (Throwable th4) {
            th = th4;
            file = 0;
            lr.a((Closeable) file);
            lr.a((Closeable) fileInputStream);
            throw th;
        }
    }
}
