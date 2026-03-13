package com.flurry.sdk;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Looper;
import android.support.v4.app.NotificationCompat;
import android.text.TextUtils;
import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;

/* JADX INFO: loaded from: classes.dex */
public final class lr {
    private static final String a = "lr";

    public static void a() {
        if (Looper.getMainLooper().getThread() != Thread.currentThread()) {
            throw new IllegalStateException("Must be called from the main thread!");
        }
    }

    public static void b() {
        if (Looper.getMainLooper().getThread() == Thread.currentThread()) {
            throw new IllegalStateException("Must be called from a background thread!");
        }
    }

    public static String a(String str) {
        Uri uri;
        if (TextUtils.isEmpty(str) || (uri = Uri.parse(str)) == null || uri.getScheme() != null) {
            return str;
        }
        return "http://" + str;
    }

    public static String c(String str) {
        try {
            return URLEncoder.encode(str, "UTF-8");
        } catch (UnsupportedEncodingException unused) {
            kf.a(5, a, "Cannot encode '" + str + "'");
            return "";
        }
    }

    public static String d(String str) {
        try {
            return URLDecoder.decode(str, "UTF-8");
        } catch (UnsupportedEncodingException unused) {
            kf.a(5, a, "Cannot decode '" + str + "'");
            return "";
        }
    }

    public static void a(Closeable closeable) {
        if (closeable != null) {
            try {
                closeable.close();
            } catch (Throwable unused) {
            }
        }
    }

    public static byte[] e(String str) {
        if (TextUtils.isEmpty(str)) {
            return null;
        }
        try {
            return str.getBytes("UTF-8");
        } catch (UnsupportedEncodingException e) {
            kf.a(5, a, "Unsupported UTF-8: " + e.getMessage());
            return null;
        }
    }

    public static byte[] f(String str) {
        try {
            MessageDigest messageDigest = MessageDigest.getInstance("SHA-1");
            messageDigest.update(str.getBytes(), 0, str.length());
            return messageDigest.digest();
        } catch (NoSuchAlgorithmException e) {
            kf.a(6, a, "Unsupported SHA1: " + e.getMessage());
            return null;
        }
    }

    public static String a(byte[] bArr) {
        StringBuilder sb = new StringBuilder(bArr.length * 2);
        char[] cArr = {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f'};
        for (byte b : bArr) {
            sb.append(cArr[(byte) ((b & 240) >> 4)]);
            sb.append(cArr[(byte) (b & 15)]);
        }
        return sb.toString();
    }

    public static String b(byte[] bArr) {
        if (bArr == null) {
            return null;
        }
        try {
            return new String(bArr, "ISO-8859-1");
        } catch (UnsupportedEncodingException e) {
            kf.a(5, a, "Unsupported ISO-8859-1:" + e.getMessage());
            return null;
        }
    }

    public static boolean a(long j) {
        return j == 0 || System.currentTimeMillis() <= j;
    }

    public static boolean a(Intent intent) {
        return jr.a().a.getPackageManager().queryIntentActivities(intent, 65536).size() > 0;
    }

    public static String g(String str) {
        return str.replace("\\b", "").replace("\\n", "").replace("\\r", "").replace("\\t", "").replace("\\", "\\\\").replace("'", "\\'").replace("\"", "\\\"");
    }

    public static Map<String, String> h(String str) {
        HashMap map = new HashMap();
        if (!TextUtils.isEmpty(str)) {
            for (String str2 : str.split("&")) {
                String[] strArrSplit = str2.split("=");
                if (!strArrSplit[0].equals(NotificationCompat.CATEGORY_EVENT)) {
                    map.put(d(strArrSplit[0]), d(strArrSplit[1]));
                }
            }
        }
        return map;
    }

    public static long i(String str) {
        if (str == null) {
            return 0L;
        }
        long jCharAt = 1125899906842597L;
        int length = str.length();
        for (int i = 0; i < length; i++) {
            jCharAt = (jCharAt * 31) + ((long) str.charAt(i));
        }
        return jCharAt;
    }

    public static long a(InputStream inputStream, OutputStream outputStream) throws IOException {
        byte[] bArr = new byte[1024];
        long j = 0;
        while (true) {
            int i = inputStream.read(bArr);
            if (i < 0) {
                return j;
            }
            outputStream.write(bArr, 0, i);
            j += (long) i;
        }
    }

    public static byte[] a(InputStream inputStream) throws IOException {
        if (inputStream == null) {
            return null;
        }
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        a(inputStream, byteArrayOutputStream);
        return byteArrayOutputStream.toByteArray();
    }

    public static double a(double d) {
        return Math.round(d * Math.pow(10.0d, 3.0d)) / Math.pow(10.0d, 3.0d);
    }

    public static boolean a(Context context, String str) {
        if (context == null || TextUtils.isEmpty(str)) {
            return false;
        }
        try {
            return context.getPackageManager().checkPermission(str, context.getPackageName()) == 0;
        } catch (Exception e) {
            kf.a(6, a, "Error occured when checking if app has permission.  Error: " + e.getMessage());
            return false;
        }
    }

    public static String b(String str) {
        return str == null ? "" : str.length() <= 255 ? str : str.substring(0, 255);
    }
}
