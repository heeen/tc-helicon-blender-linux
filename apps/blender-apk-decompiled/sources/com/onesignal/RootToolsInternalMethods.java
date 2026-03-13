package com.onesignal;

import java.io.File;

/* JADX INFO: loaded from: classes.dex */
class RootToolsInternalMethods {
    RootToolsInternalMethods() {
    }

    static boolean isRooted() {
        for (String str : new String[]{"/sbin/", "/system/bin/", "/system/xbin/", "/data/local/xbin/", "/data/local/bin/", "/system/sd/xbin/", "/system/bin/failsafe/", "/data/local/"}) {
            if (new File(str + "su").exists()) {
                return true;
            }
        }
        return false;
    }
}
