package com.google.android.gms.common.util;

import android.os.Process;
import android.os.StrictMode;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

/* JADX INFO: loaded from: classes.dex */
public final class zzu {
    private static String zzglf;
    private static final int zzglg = Process.myPid();

    public static String zzany() {
        if (zzglf == null) {
            zzglf = zzci(zzglg);
        }
        return zzglf;
    }

    private static String zzci(int i) throws Throwable {
        BufferedReader bufferedReader;
        StrictMode.ThreadPolicy threadPolicyAllowThreadDiskReads;
        BufferedReader bufferedReader2 = null;
        if (i <= 0) {
            return null;
        }
        try {
            threadPolicyAllowThreadDiskReads = StrictMode.allowThreadDiskReads();
            try {
                StringBuilder sb = new StringBuilder(25);
                sb.append("/proc/");
                sb.append(i);
                sb.append("/cmdline");
                bufferedReader = new BufferedReader(new FileReader(sb.toString()));
            } catch (Throwable th) {
                StrictMode.setThreadPolicy(threadPolicyAllowThreadDiskReads);
                throw th;
            }
        } catch (IOException unused) {
            bufferedReader = null;
        } catch (Throwable th2) {
            th = th2;
        }
        try {
            StrictMode.setThreadPolicy(threadPolicyAllowThreadDiskReads);
            String strTrim = bufferedReader.readLine().trim();
            zzp.closeQuietly(bufferedReader);
            return strTrim;
        } catch (IOException unused2) {
            zzp.closeQuietly(bufferedReader);
            return null;
        } catch (Throwable th3) {
            th = th3;
            bufferedReader2 = bufferedReader;
            zzp.closeQuietly(bufferedReader2);
            throw th;
        }
    }
}
