package com.onesignal.shortcutbadger.impl;

import android.annotation.TargetApi;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import com.onesignal.shortcutbadger.Badger;
import com.onesignal.shortcutbadger.ShortcutBadgeException;
import com.onesignal.shortcutbadger.util.BroadcastHelper;
import com.onesignal.shortcutbadger.util.CloseHelper;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.List;

/* JADX INFO: loaded from: classes.dex */
public class OPPOHomeBader implements Badger {
    private static final String INTENT_ACTION = "com.oppo.unsettledevent";
    private static final String INTENT_EXTRA_BADGEUPGRADE_COUNT = "app_badge_count";
    private static final String INTENT_EXTRA_BADGE_COUNT = "number";
    private static final String INTENT_EXTRA_BADGE_UPGRADENUMBER = "upgradeNumber";
    private static final String INTENT_EXTRA_PACKAGENAME = "pakeageName";
    private static final String PROVIDER_CONTENT_URI = "content://com.android.badge/badge";
    private static int ROMVERSION = -1;

    @Override // com.onesignal.shortcutbadger.Badger
    @TargetApi(11)
    public void executeBadge(Context context, ComponentName componentName, int i) throws ShortcutBadgeException {
        if (i == 0) {
            i = -1;
        }
        Intent intent = new Intent(INTENT_ACTION);
        intent.putExtra(INTENT_EXTRA_PACKAGENAME, componentName.getPackageName());
        intent.putExtra(INTENT_EXTRA_BADGE_COUNT, i);
        intent.putExtra(INTENT_EXTRA_BADGE_UPGRADENUMBER, i);
        if (BroadcastHelper.canResolveBroadcast(context, intent)) {
            context.sendBroadcast(intent);
        } else if (getSupportVersion() == 6) {
            Bundle bundle = new Bundle();
            bundle.putInt(INTENT_EXTRA_BADGEUPGRADE_COUNT, i);
            context.getContentResolver().call(Uri.parse(PROVIDER_CONTENT_URI), "setAppBadgeCount", (String) null, bundle);
        }
    }

    @Override // com.onesignal.shortcutbadger.Badger
    public List<String> getSupportLaunchers() {
        return Collections.singletonList("com.oppo.launcher");
    }

    private int getSupportVersion() throws Throwable {
        int iIntValue;
        if (ROMVERSION >= 0) {
            return ROMVERSION;
        }
        try {
            iIntValue = ((Integer) executeClassLoad(getClass("com.color.os.ColorBuild"), "getColorOSVERSION", null, null)).intValue();
        } catch (Exception unused) {
            iIntValue = 0;
        }
        if (iIntValue == 0) {
            try {
                String systemProperty = getSystemProperty("ro.build.version.opporom");
                if (systemProperty.startsWith("V1.4")) {
                    return 3;
                }
                if (systemProperty.startsWith("V2.0")) {
                    return 4;
                }
                if (systemProperty.startsWith("V2.1")) {
                    return 5;
                }
            } catch (Exception unused2) {
            }
        }
        ROMVERSION = iIntValue;
        return ROMVERSION;
    }

    private Object executeClassLoad(Class cls, String str, Class[] clsArr, Object[] objArr) {
        Method method;
        if (cls != null && !checkObjExists(str) && (method = getMethod(cls, str, clsArr)) != null) {
            method.setAccessible(true);
            try {
                return method.invoke(null, objArr);
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            } catch (InvocationTargetException e2) {
                e2.printStackTrace();
            }
        }
        return null;
    }

    private Method getMethod(Class cls, String str, Class[] clsArr) {
        if (cls == null || checkObjExists(str)) {
            return null;
        }
        try {
            try {
                cls.getMethods();
                cls.getDeclaredMethods();
                return cls.getDeclaredMethod(str, clsArr);
            } catch (Exception unused) {
                if (cls.getSuperclass() != null) {
                    return getMethod(cls.getSuperclass(), str, clsArr);
                }
                return null;
            }
        } catch (Exception unused2) {
            return cls.getMethod(str, clsArr);
        }
    }

    private Class getClass(String str) {
        try {
            return Class.forName(str);
        } catch (ClassNotFoundException unused) {
            return null;
        }
    }

    private boolean checkObjExists(Object obj) {
        return obj == null || obj.toString().equals("") || obj.toString().trim().equals("null");
    }

    private String getSystemProperty(String str) throws Throwable {
        BufferedReader bufferedReader;
        BufferedReader bufferedReader2 = null;
        try {
            bufferedReader = new BufferedReader(new InputStreamReader(Runtime.getRuntime().exec("getprop " + str).getInputStream()), 1024);
            try {
                String line = bufferedReader.readLine();
                bufferedReader.close();
                CloseHelper.closeQuietly(bufferedReader);
                return line;
            } catch (IOException unused) {
                CloseHelper.closeQuietly(bufferedReader);
                return null;
            } catch (Throwable th) {
                th = th;
                bufferedReader2 = bufferedReader;
                CloseHelper.closeQuietly(bufferedReader2);
                throw th;
            }
        } catch (IOException unused2) {
            bufferedReader = null;
        } catch (Throwable th2) {
            th = th2;
        }
    }
}
