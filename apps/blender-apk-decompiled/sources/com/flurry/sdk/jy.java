package com.flurry.sdk;

import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

/* JADX INFO: loaded from: classes.dex */
public class jy<T> {
    private static final String a = "jy";
    private final File b;
    private final kz<T> c;

    public jy(File file, String str, int i, lc<T> lcVar) {
        this.b = file;
        this.c = new kx(new lb(str, i, lcVar));
    }

    public final T a() throws Throwable {
        FileInputStream fileInputStream;
        T t = null;
        if (this.b == null) {
            return null;
        }
        if (!this.b.exists()) {
            kf.a(5, a, "No data to read for file:" + this.b.getName());
            return null;
        }
        boolean z = false;
        try {
            try {
                fileInputStream = new FileInputStream(this.b);
                try {
                    T tA = this.c.a(fileInputStream);
                    lr.a((Closeable) fileInputStream);
                    t = tA;
                } catch (Exception e) {
                    e = e;
                    kf.a(3, a, "Error reading data file:" + this.b.getName(), e);
                    z = true;
                    lr.a((Closeable) fileInputStream);
                }
            } catch (Throwable th) {
                th = th;
                lr.a((Closeable) fileInputStream);
                throw th;
            }
        } catch (Exception e2) {
            e = e2;
            fileInputStream = null;
        } catch (Throwable th2) {
            th = th2;
            fileInputStream = null;
            lr.a((Closeable) fileInputStream);
            throw th;
        }
        if (z) {
            kf.a(3, a, "Deleting data file:" + this.b.getName());
            this.b.delete();
        }
        return t;
    }

    public final void a(T t) throws Throwable {
        boolean z = true;
        if (t == null) {
            kf.a(3, a, "No data to write for file:" + this.b.getName());
        } else {
            FileOutputStream fileOutputStream = null;
            try {
                try {
                    if (!lq.a(this.b)) {
                        throw new IOException("Cannot create parent directory!");
                    }
                    FileOutputStream fileOutputStream2 = new FileOutputStream(this.b);
                    try {
                        this.c.a(fileOutputStream2, t);
                        lr.a(fileOutputStream2);
                        z = false;
                    } catch (Exception e) {
                        e = e;
                        fileOutputStream = fileOutputStream2;
                        kf.a(3, a, "Error writing data file:" + this.b.getName(), e);
                        lr.a(fileOutputStream);
                    } catch (Throwable th) {
                        th = th;
                        fileOutputStream = fileOutputStream2;
                        lr.a(fileOutputStream);
                        throw th;
                    }
                } catch (Throwable th2) {
                    th = th2;
                }
            } catch (Exception e2) {
                e = e2;
            }
        }
        if (z) {
            kf.a(3, a, "Deleting data file:" + this.b.getName());
            this.b.delete();
        }
    }

    public final boolean b() {
        if (this.b == null) {
            return false;
        }
        return this.b.delete();
    }
}
