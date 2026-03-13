package com.google.android.gms.common.util;

import android.annotation.TargetApi;
import android.content.Context;
import android.util.Log;
import java.io.File;

/* JADX INFO: loaded from: classes.dex */
public final class zzx {
    @TargetApi(21)
    public static File getNoBackupFilesDir(Context context) {
        return zzs.zzanx() ? context.getNoBackupFilesDir() : zzd(new File(context.getApplicationInfo().dataDir, "no_backup"));
    }

    private static synchronized File zzd(File file) {
        if (file.exists() || file.mkdirs()) {
            return file;
        }
        if (file.exists()) {
            return file;
        }
        String strValueOf = String.valueOf(file.getPath());
        Log.w("SupportV4Utils", strValueOf.length() != 0 ? "Unable to create no-backup dir ".concat(strValueOf) : new String("Unable to create no-backup dir "));
        return null;
    }
}
