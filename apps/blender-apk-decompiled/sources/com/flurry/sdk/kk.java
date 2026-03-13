package com.flurry.sdk;

import android.text.TextUtils;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/* JADX INFO: loaded from: classes.dex */
public class kk {
    private final Pattern a = Pattern.compile(".*?(%\\{\\w+\\}).*?");

    public final String a(String str) {
        Matcher matcher = this.a.matcher(str);
        if (matcher.matches()) {
            return matcher.group(1);
        }
        return null;
    }

    public static boolean a(String str, String str2) {
        if (TextUtils.isEmpty(str2)) {
            return false;
        }
        return str2.equals("%{" + str + "}");
    }
}
