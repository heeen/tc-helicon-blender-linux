package com.flurry.sdk;

import android.net.Uri;
import android.text.TextUtils;
import android.webkit.MimeTypeMap;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.regex.Pattern;

/* JADX INFO: loaded from: classes.dex */
public final class ly {
    private static final Pattern a = Pattern.compile("/");

    public static String a(String str) {
        URI uriI;
        int iIndexOf;
        if (TextUtils.isEmpty(str) || (uriI = i(str)) == null) {
            return str;
        }
        String scheme = uriI.getScheme();
        if (TextUtils.isEmpty(scheme)) {
            return "http" + str;
        }
        String lowerCase = scheme.toLowerCase();
        if (scheme == null || scheme.equals(lowerCase) || (iIndexOf = str.indexOf(scheme)) < 0) {
            return str;
        }
        return lowerCase + str.substring(iIndexOf + scheme.length());
    }

    public static String b(String str) {
        URI uriI;
        URI uriA;
        if (TextUtils.isEmpty(str) || (uriI = i(str)) == null) {
            return str;
        }
        URI uriNormalize = uriI.normalize();
        return (uriNormalize.isOpaque() || (uriA = a(uriNormalize.getScheme(), uriNormalize.getAuthority(), "/", null, null)) == null) ? str : uriA.toString();
    }

    public static String c(String str) {
        URI uriI;
        URI uriResolve;
        if (TextUtils.isEmpty(str) || (uriI = i(str)) == null) {
            return str;
        }
        URI uriNormalize = uriI.normalize();
        return (uriNormalize.isOpaque() || (uriResolve = uriNormalize.resolve("./")) == null) ? str : uriResolve.toString();
    }

    public static String a(String str, String str2) {
        URI uriI;
        if (TextUtils.isEmpty(str) || TextUtils.isEmpty(str2) || (uriI = i(str)) == null) {
            return str;
        }
        URI uriNormalize = uriI.normalize();
        URI uriI2 = i(str2);
        if (uriI2 == null) {
            return str;
        }
        URI uriNormalize2 = uriI2.normalize();
        if (uriNormalize.isOpaque() || uriNormalize2.isOpaque()) {
            return str;
        }
        String scheme = uriNormalize.getScheme();
        String scheme2 = uriNormalize2.getScheme();
        if (scheme2 == null && scheme != null) {
            return str;
        }
        if (scheme2 != null && !scheme2.equals(scheme)) {
            return str;
        }
        String authority = uriNormalize.getAuthority();
        String authority2 = uriNormalize2.getAuthority();
        if (authority2 == null && authority != null) {
            return str;
        }
        if (authority2 != null && !authority2.equals(authority)) {
            return str;
        }
        String path = uriNormalize.getPath();
        String path2 = uriNormalize2.getPath();
        String[] strArrSplit = a.split(path, -1);
        String[] strArrSplit2 = a.split(path2, -1);
        int length = strArrSplit.length;
        int length2 = strArrSplit2.length;
        int i = 0;
        while (i < length2 && i < length && strArrSplit[i].equals(strArrSplit2[i])) {
            i++;
        }
        String query = uriNormalize.getQuery();
        String fragment = uriNormalize.getFragment();
        StringBuilder sb = new StringBuilder();
        if (i == length2 && i == length) {
            String query2 = uriNormalize2.getQuery();
            String fragment2 = uriNormalize2.getFragment();
            boolean zEquals = TextUtils.equals(query, query2);
            boolean zEquals2 = TextUtils.equals(fragment, fragment2);
            if (zEquals && zEquals2) {
                fragment = null;
                query = null;
            } else {
                String str3 = (!zEquals || TextUtils.isEmpty(fragment)) ? query : null;
                if (TextUtils.isEmpty(str3) && TextUtils.isEmpty(fragment)) {
                    sb.append(strArrSplit[length - 1]);
                } else {
                    query = str3;
                }
            }
        } else {
            int i2 = length - 1;
            int i3 = length2 - 1;
            for (int i4 = i; i4 < i3; i4++) {
                sb.append("..");
                sb.append("/");
            }
            while (i < i2) {
                sb.append(strArrSplit[i]);
                sb.append("/");
                i++;
            }
            if (i < length) {
                sb.append(strArrSplit[i]);
            }
            if (sb.length() == 0) {
                sb.append(".");
                sb.append("/");
            }
        }
        URI uriA = a(null, null, sb.toString(), query, fragment);
        return uriA == null ? str : uriA.toString();
    }

    private static URI i(String str) {
        try {
            return new URI(str);
        } catch (URISyntaxException unused) {
            return null;
        }
    }

    private static URI a(String str, String str2, String str3, String str4, String str5) {
        try {
            return new URI(str, str2, str3, str4, str5);
        } catch (URISyntaxException unused) {
            return null;
        }
    }

    public static boolean d(String str) {
        if (TextUtils.isEmpty(str)) {
            return false;
        }
        Uri uri = Uri.parse(str);
        return uri.getScheme() != null && uri.getScheme().equals("market");
    }

    public static boolean e(String str) {
        if (TextUtils.isEmpty(str)) {
            return false;
        }
        Uri uri = Uri.parse(str);
        if (uri.getScheme() != null) {
            return uri.getScheme().equals("http") || uri.getScheme().equals("https");
        }
        return false;
    }

    public static boolean f(String str) {
        if (TextUtils.isEmpty(str)) {
            return false;
        }
        Uri uri = Uri.parse(str);
        return uri.getHost() != null && uri.getHost().equals("play.google.com") && uri.getScheme() != null && uri.getScheme().startsWith("http");
    }

    public static boolean g(String str) {
        String mimeTypeFromExtension;
        return (TextUtils.isEmpty(str) || (mimeTypeFromExtension = MimeTypeMap.getSingleton().getMimeTypeFromExtension(MimeTypeMap.getFileExtensionFromUrl(str))) == null || !mimeTypeFromExtension.startsWith("video/")) ? false : true;
    }

    public static boolean h(String str) {
        URI uriI;
        if (TextUtils.isEmpty(str) || (uriI = i(str)) == null) {
            return false;
        }
        return uriI.getScheme() == null || "http".equalsIgnoreCase(uriI.getScheme()) || "https".equalsIgnoreCase(uriI.getScheme());
    }

    public static String b(String str, String str2) {
        if (!TextUtils.isEmpty(str)) {
            try {
                if (new URI(str).isAbsolute() || TextUtils.isEmpty(str2)) {
                    return str;
                }
                URI uri = new URI(str2);
                return uri.getScheme() + "://" + uri.getHost() + str;
            } catch (Exception unused) {
            }
        }
        return str;
    }
}
