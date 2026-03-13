package com.airbnb.lottie;

import android.support.annotation.RestrictTo;
import android.support.v4.os.TraceCompat;
import android.util.Log;

/* JADX INFO: loaded from: classes.dex */
@RestrictTo({RestrictTo.Scope.LIBRARY})
public class L {
    public static final boolean DBG = false;
    private static final int MAX_DEPTH = 20;
    public static final String TAG = "LOTTIE";
    private static int depthPastMaxDepth = 0;
    private static String[] sections = null;
    private static long[] startTimeNs = null;
    private static int traceDepth = 0;
    private static boolean traceEnabled = false;

    public static void warn(String str) {
        Log.w(TAG, str);
    }

    public static void setTraceEnabled(boolean z) {
        if (traceEnabled == z) {
            return;
        }
        traceEnabled = z;
        if (traceEnabled) {
            sections = new String[20];
            startTimeNs = new long[20];
        }
    }

    public static void beginSection(String str) {
        if (traceEnabled) {
            if (traceDepth == 20) {
                depthPastMaxDepth++;
                return;
            }
            sections[traceDepth] = str;
            startTimeNs[traceDepth] = System.nanoTime();
            TraceCompat.beginSection(str);
            traceDepth++;
        }
    }

    public static float endSection(String str) {
        if (depthPastMaxDepth > 0) {
            depthPastMaxDepth--;
            return 0.0f;
        }
        if (!traceEnabled) {
            return 0.0f;
        }
        traceDepth--;
        if (traceDepth == -1) {
            throw new IllegalStateException("Can't end trace section. There are none.");
        }
        if (!str.equals(sections[traceDepth])) {
            throw new IllegalStateException("Unbalanced trace call " + str + ". Expected " + sections[traceDepth] + ".");
        }
        TraceCompat.endSection();
        return (System.nanoTime() - startTimeNs[traceDepth]) / 1000000.0f;
    }
}
