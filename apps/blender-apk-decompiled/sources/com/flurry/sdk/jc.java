package com.flurry.sdk;

import android.text.TextUtils;
import java.util.Arrays;

/* JADX INFO: loaded from: classes.dex */
public class jc {
    private static String a = "com.flurry.sdk.jc";

    public static String a(String str) {
        String str2 = "a=" + jr.a().d;
        if (TextUtils.isEmpty(str)) {
            return str2;
        }
        return String.format("%s&%s", str2, "cid=" + b(str));
    }

    private static String b(String str) {
        byte[] bArr = null;
        if (str != null && str.trim().length() > 0) {
            try {
                byte[] bArrF = lr.f(str);
                if (bArrF != null && bArrF.length == 20) {
                    try {
                        kf.a(5, a, "syndication hashedId is:" + new String(bArrF));
                        bArr = bArrF;
                    } catch (Exception unused) {
                        bArr = bArrF;
                        kf.a(6, a, "Exception in getHashedSyndicationIdString()");
                    }
                } else {
                    kf.a(6, a, "sha1 is not 20 bytes long: " + Arrays.toString(bArrF));
                }
            } catch (Exception unused2) {
            }
        }
        return lr.a(bArr);
    }
}
