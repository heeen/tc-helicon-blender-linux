package com.flurry.sdk;

import android.content.Context;
import android.os.Build;
import android.os.Environment;
import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;

/* JADX INFO: loaded from: classes.dex */
public final class lq {
    private static String a = "lq";

    public static File a() {
        Context context = jr.a().a;
        File externalFilesDir = null;
        if ("mounted".equals(Environment.getExternalStorageState()) && (Build.VERSION.SDK_INT >= 19 || context.checkCallingOrSelfPermission("android.permission.WRITE_EXTERNAL_STORAGE") == 0)) {
            externalFilesDir = context.getExternalFilesDir(null);
        }
        return externalFilesDir == null ? context.getFilesDir() : externalFilesDir;
    }

    public static File b() {
        Context context = jr.a().a;
        File externalCacheDir = (!"mounted".equals(Environment.getExternalStorageState()) || (Build.VERSION.SDK_INT < 19 && context.checkCallingOrSelfPermission("android.permission.WRITE_EXTERNAL_STORAGE") != 0)) ? null : context.getExternalCacheDir();
        return externalCacheDir == null ? context.getCacheDir() : externalCacheDir;
    }

    public static boolean a(File file) {
        if (file == null || file.getAbsoluteFile() == null) {
            return false;
        }
        File parentFile = file.getParentFile();
        if (parentFile == null || parentFile.mkdirs() || parentFile.isDirectory()) {
            return true;
        }
        kf.a(6, a, "Unable to create persistent dir: " + parentFile);
        return false;
    }

    public static boolean b(File file) {
        if (file != null && file.isDirectory()) {
            for (String str : file.list()) {
                if (!b(new File(file, str))) {
                    return false;
                }
            }
        }
        return file.delete();
    }

    @Deprecated
    public static String c(File file) throws Throwable {
        FileInputStream fileInputStream;
        StringBuilder sb;
        if (file == null || !file.exists()) {
            kf.a(4, a, "Persistent file doesn't exist.");
            return null;
        }
        kf.a(4, a, "Loading persistent data: " + file.getAbsolutePath());
        try {
            fileInputStream = new FileInputStream(file);
            try {
                try {
                    sb = new StringBuilder();
                    byte[] bArr = new byte[1024];
                    while (true) {
                        int i = fileInputStream.read(bArr);
                        if (i <= 0) {
                            break;
                        }
                        sb.append(new String(bArr, 0, i));
                    }
                    lr.a((Closeable) fileInputStream);
                } catch (Throwable th) {
                    th = th;
                    kf.a(6, a, "Error when loading persistent file", th);
                    lr.a((Closeable) fileInputStream);
                    sb = null;
                }
            } catch (Throwable th2) {
                th = th2;
                lr.a((Closeable) fileInputStream);
                throw th;
            }
        } catch (Throwable th3) {
            th = th3;
            fileInputStream = null;
        }
        if (sb != null) {
            return sb.toString();
        }
        return null;
    }

    @Deprecated
    public static void a(File file, String str) throws Throwable {
        FileOutputStream fileOutputStream;
        if (file == null) {
            kf.a(4, a, "No persistent file specified.");
            return;
        }
        if (str == null) {
            kf.a(4, a, "No data specified; deleting persistent file: " + file.getAbsolutePath());
            file.delete();
            return;
        }
        kf.a(4, a, "Writing persistent data: " + file.getAbsolutePath());
        FileOutputStream fileOutputStream2 = null;
        try {
            try {
                fileOutputStream = new FileOutputStream(file);
            } catch (Throwable th) {
                th = th;
            }
        } catch (Throwable th2) {
            th = th2;
        }
        try {
            fileOutputStream.write(str.getBytes());
            lr.a(fileOutputStream);
        } catch (Throwable th3) {
            th = th3;
            fileOutputStream2 = fileOutputStream;
            kf.a(6, a, "Error writing persistent file", th);
            lr.a(fileOutputStream2);
        }
    }
}
