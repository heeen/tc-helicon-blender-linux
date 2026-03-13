package com.flurry.android;

import android.content.Context;
import android.location.Criteria;
import android.location.Location;
import android.os.Build;
import android.text.TextUtils;
import com.flurry.sdk.hk;
import com.flurry.sdk.ja;
import com.flurry.sdk.jd;
import com.flurry.sdk.jq;
import com.flurry.sdk.jr;
import com.flurry.sdk.js;
import com.flurry.sdk.jt;
import com.flurry.sdk.jz;
import com.flurry.sdk.ka;
import com.flurry.sdk.kb;
import com.flurry.sdk.kf;
import com.flurry.sdk.le;
import com.flurry.sdk.lf;
import com.flurry.sdk.li;
import com.flurry.sdk.lr;
import com.flurry.sdk.mb;
import java.util.Date;
import java.util.Map;

/* JADX INFO: loaded from: classes.dex */
public final class FlurryAgent {
    private static final String a = "FlurryAgent";
    private static final ka<le> b = new ka<le>() { // from class: com.flurry.android.FlurryAgent.1
        @Override // com.flurry.sdk.ka
        public final /* synthetic */ void a(jz jzVar) {
            final le leVar = (le) jzVar;
            jr.a().a(new Runnable() { // from class: com.flurry.android.FlurryAgent.1.1
                @Override // java.lang.Runnable
                public final void run() {
                    if (AnonymousClass2.a[leVar.c - 1] == 1 && FlurryAgent.c != null) {
                        FlurryAgent.c.onSessionStarted();
                    }
                }
            });
        }
    };
    private static FlurryAgentListener c = null;
    private static boolean d = false;
    private static int e = 5;
    private static long f = 10000;
    private static boolean g = true;
    private static boolean h = false;
    private static String i = null;

    /* JADX INFO: renamed from: com.flurry.android.FlurryAgent$2, reason: invalid class name */
    static /* synthetic */ class AnonymousClass2 {
        static final /* synthetic */ int[] a = new int[le.a.a().length];

        static {
            try {
                a[le.a.b - 1] = 1;
            } catch (NoSuchFieldError unused) {
            }
        }
    }

    private FlurryAgent() {
    }

    @Deprecated
    public static synchronized void init(Context context, String str) {
        if (Build.VERSION.SDK_INT < 10) {
            kf.b(a, "Device SDK Version older than 10");
            return;
        }
        if (context == null) {
            throw new NullPointerException("Null context");
        }
        if (TextUtils.isEmpty(str)) {
            throw new IllegalArgumentException("API key not specified");
        }
        if (jr.a() != null) {
            kf.e(a, "Flurry is already initialized");
        }
        try {
            mb.a();
            jr.a(context, str);
        } catch (Throwable th) {
            kf.a(a, "", th);
        }
        kf.e(a, "'init' method is deprecated.");
    }

    public static int getAgentVersion() {
        return js.a();
    }

    public static String getReleaseVersion() {
        return js.b();
    }

    @Deprecated
    public static void setFlurryAgentListener(FlurryAgentListener flurryAgentListener) {
        if (Build.VERSION.SDK_INT < 10) {
            kf.b(a, "Device SDK Version older than 10");
            return;
        }
        if (flurryAgentListener == null) {
            kf.b(a, "Listener cannot be null");
            kb.a().b("com.flurry.android.sdk.FlurrySessionEvent", b);
        } else {
            c = flurryAgentListener;
            kb.a().a("com.flurry.android.sdk.FlurrySessionEvent", b);
            kf.e(a, "'setFlurryAgentListener' method is deprecated.");
        }
    }

    @Deprecated
    public static void setLogEnabled(boolean z) {
        if (Build.VERSION.SDK_INT < 10) {
            kf.b(a, "Device SDK Version older than 10");
            return;
        }
        if (z) {
            kf.b();
        } else {
            kf.a();
        }
        kf.e(a, "'setLogEnabled' method is deprecated.");
    }

    @Deprecated
    public static void setLogLevel(int i2) {
        if (Build.VERSION.SDK_INT < 10) {
            kf.b(a, "Device SDK Version older than 10");
        } else {
            kf.a(i2);
            kf.e(a, "'setLogLevel' method is deprecated.");
        }
    }

    public static void setVersionName(String str) {
        if (Build.VERSION.SDK_INT < 10) {
            kf.b(a, "Device SDK Version older than 10");
        } else if (str == null) {
            kf.b(a, "String versionName passed to setVersionName was null.");
        } else {
            li.a().a("VersionName", str);
        }
    }

    public static void setReportLocation(boolean z) {
        if (Build.VERSION.SDK_INT < 10) {
            kf.b(a, "Device SDK Version older than 10");
        } else {
            li.a().a("ReportLocation", Boolean.valueOf(z));
        }
    }

    public static void setLocation(float f2, float f3) {
        if (Build.VERSION.SDK_INT < 10) {
            kf.b(a, "Device SDK Version older than 10");
            return;
        }
        Location location = new Location("Explicit");
        location.setLatitude(f2);
        location.setLongitude(f3);
        li.a().a("ExplicitLocation", location);
    }

    public static void clearLocation() {
        if (Build.VERSION.SDK_INT < 10) {
            kf.b(a, "Device SDK Version older than 10");
        } else {
            li.a().a("ExplicitLocation", (Object) null);
        }
    }

    @Deprecated
    public static void setContinueSessionMillis(long j) {
        if (Build.VERSION.SDK_INT < 10) {
            kf.b(a, "Device SDK Version older than 10");
            return;
        }
        if (j < 5000) {
            kf.b(a, "Invalid time set for session resumption: " + j);
            return;
        }
        li.a().a("ContinueSessionMillis", Long.valueOf(j));
        kf.e(a, "'setContinueSessionMillis' method is deprecated.");
    }

    public static void setLogEvents(boolean z) {
        if (Build.VERSION.SDK_INT < 10) {
            kf.b(a, "Device SDK Version older than 10");
        } else {
            li.a().a("LogEvents", Boolean.valueOf(z));
        }
    }

    @Deprecated
    public static void setCaptureUncaughtExceptions(boolean z) {
        if (Build.VERSION.SDK_INT < 10) {
            kf.b(a, "Device SDK Version older than 10");
        } else {
            li.a().a("CaptureUncaughtExceptions", Boolean.valueOf(z));
            kf.e(a, "'setCaptureUncaughtExceptions' method is deprecated.");
        }
    }

    public static void addOrigin(String str, String str2) {
        addOrigin(str, str2, null);
    }

    public static void addOrigin(String str, String str2, Map<String, String> map) {
        if (Build.VERSION.SDK_INT < 10) {
            kf.b(a, "Device SDK Version older than 10");
            return;
        }
        if (TextUtils.isEmpty(str)) {
            throw new IllegalArgumentException("originName not specified");
        }
        if (TextUtils.isEmpty(str2)) {
            throw new IllegalArgumentException("originVersion not specified");
        }
        try {
            jt.a().a(str, str2, map);
        } catch (Throwable th) {
            kf.a(a, "", th);
        }
    }

    @Deprecated
    public static void setPulseEnabled(boolean z) {
        if (Build.VERSION.SDK_INT < 10) {
            kf.b(a, "Device SDK Version older than 10");
            return;
        }
        li.a().a("ProtonEnabled", Boolean.valueOf(z));
        if (!z) {
            li.a().a("analyticsEnabled", (Object) true);
        }
        kf.e(a, "'setPulseEnabled' method is deprecated.");
    }

    @Deprecated
    public static void onStartSession(Context context, String str) {
        if (Build.VERSION.SDK_INT < 10) {
            kf.b(a, "Device SDK Version older than 10");
            return;
        }
        if (context == null) {
            throw new NullPointerException("Null context");
        }
        if (TextUtils.isEmpty(str)) {
            throw new IllegalArgumentException("Api key not specified");
        }
        if (jr.a() == null) {
            throw new IllegalStateException("Flurry SDK must be initialized before starting a session");
        }
        try {
            lf.a().b(context);
        } catch (Throwable th) {
            kf.a(a, "", th);
        }
        kf.e(a, "'onStartSession' method is deprecated.");
    }

    public static void onStartSession(Context context) {
        if (Build.VERSION.SDK_INT < 10) {
            kf.b(a, "Device SDK Version older than 10");
            return;
        }
        if (context == null) {
            throw new NullPointerException("Null context");
        }
        if (jr.a() == null) {
            throw new IllegalStateException("Flurry SDK must be initialized before starting a session");
        }
        try {
            lf.a().b(context);
        } catch (Throwable th) {
            kf.a(a, "", th);
        }
    }

    public static void onEndSession(Context context) {
        if (Build.VERSION.SDK_INT < 10) {
            kf.b(a, "Device SDK Version older than 10");
            return;
        }
        if (context == null) {
            throw new NullPointerException("Null context");
        }
        if (jr.a() == null) {
            throw new IllegalStateException("Flurry SDK must be initialized before ending a session");
        }
        try {
            lf.a().c(context);
        } catch (Throwable th) {
            kf.a(a, "", th);
        }
    }

    public static boolean isSessionActive() {
        if (Build.VERSION.SDK_INT < 10) {
            kf.b(a, "Device SDK Version older than 10");
            return false;
        }
        try {
            return lf.a().d();
        } catch (Throwable th) {
            kf.a(a, "", th);
            return false;
        }
    }

    public static String getSessionId() {
        if (Build.VERSION.SDK_INT < 10) {
            kf.b(a, "Device SDK Version older than 10");
            return null;
        }
        if (jr.a() == null) {
            throw new IllegalStateException("Flurry SDK must be initialized before starting a session");
        }
        try {
            jd.a();
            return jd.c();
        } catch (Throwable th) {
            kf.a(a, "", th);
            return null;
        }
    }

    public static FlurryEventRecordStatus logEvent(String str) {
        FlurryEventRecordStatus flurryEventRecordStatus = FlurryEventRecordStatus.kFlurryEventFailed;
        if (Build.VERSION.SDK_INT < 10) {
            kf.b(a, "Device SDK Version older than 10");
            return flurryEventRecordStatus;
        }
        if (str == null) {
            kf.b(a, "String eventId passed to logEvent was null.");
            return flurryEventRecordStatus;
        }
        try {
            hk.a();
            ja jaVarC = hk.c();
            return jaVarC != null ? jaVarC.a(str, (Map<String, String>) null, false) : FlurryEventRecordStatus.kFlurryEventFailed;
        } catch (Throwable th) {
            kf.a(a, "Failed to log event: " + str, th);
            return flurryEventRecordStatus;
        }
    }

    public static FlurryEventRecordStatus logEvent(String str, Map<String, String> map) {
        FlurryEventRecordStatus flurryEventRecordStatus = FlurryEventRecordStatus.kFlurryEventFailed;
        if (Build.VERSION.SDK_INT < 10) {
            kf.b(a, "Device SDK Version older than 10");
            return flurryEventRecordStatus;
        }
        if (str == null) {
            kf.b(a, "String eventId passed to logEvent was null.");
            return flurryEventRecordStatus;
        }
        if (map == null) {
            kf.b(a, "String parameters passed to logEvent was null.");
            return flurryEventRecordStatus;
        }
        try {
            hk.a();
            ja jaVarC = hk.c();
            return jaVarC != null ? jaVarC.a(str, map, false) : FlurryEventRecordStatus.kFlurryEventFailed;
        } catch (Throwable th) {
            kf.a(a, "Failed to log event: " + str, th);
            return flurryEventRecordStatus;
        }
    }

    public static FlurryEventRecordStatus logEvent(FlurrySyndicationEventName flurrySyndicationEventName, String str, Map<String, String> map) {
        FlurryEventRecordStatus flurryEventRecordStatus = FlurryEventRecordStatus.kFlurryEventFailed;
        if (Build.VERSION.SDK_INT < 10) {
            kf.b(a, "Device SDK Version older than 10");
            return flurryEventRecordStatus;
        }
        if (flurrySyndicationEventName == null) {
            kf.b(a, "String eventName passed to logEvent was null.");
            return flurryEventRecordStatus;
        }
        if (TextUtils.isEmpty(str)) {
            kf.b(a, "String syndicationId passed to logEvent was null or empty.");
            return flurryEventRecordStatus;
        }
        if (map == null) {
            kf.b(a, "String parameters passed to logEvent was null.");
            return flurryEventRecordStatus;
        }
        try {
            hk.a();
            String string = flurrySyndicationEventName.toString();
            ja jaVarC = hk.c();
            return jaVarC != null ? jaVarC.a(string, str, map) : FlurryEventRecordStatus.kFlurryEventFailed;
        } catch (Throwable th) {
            kf.a(a, "Failed to log event: " + flurrySyndicationEventName.toString(), th);
            return flurryEventRecordStatus;
        }
    }

    public static FlurryEventRecordStatus logEvent(String str, boolean z) {
        FlurryEventRecordStatus flurryEventRecordStatus = FlurryEventRecordStatus.kFlurryEventFailed;
        if (Build.VERSION.SDK_INT < 10) {
            kf.b(a, "Device SDK Version older than 10");
            return flurryEventRecordStatus;
        }
        if (str == null) {
            kf.b(a, "String eventId passed to logEvent was null.");
            return flurryEventRecordStatus;
        }
        try {
            hk.a();
            ja jaVarC = hk.c();
            return jaVarC != null ? jaVarC.a(str, (Map<String, String>) null, z) : FlurryEventRecordStatus.kFlurryEventFailed;
        } catch (Throwable th) {
            kf.a(a, "Failed to log event: " + str, th);
            return flurryEventRecordStatus;
        }
    }

    public static FlurryEventRecordStatus logEvent(String str, Map<String, String> map, boolean z) {
        FlurryEventRecordStatus flurryEventRecordStatus = FlurryEventRecordStatus.kFlurryEventFailed;
        if (Build.VERSION.SDK_INT < 10) {
            kf.b(a, "Device SDK Version older than 10");
            return flurryEventRecordStatus;
        }
        if (str == null) {
            kf.b(a, "String eventId passed to logEvent was null.");
            return flurryEventRecordStatus;
        }
        if (map == null) {
            kf.b(a, "String parameters passed to logEvent was null.");
            return flurryEventRecordStatus;
        }
        try {
            hk.a();
            ja jaVarC = hk.c();
            return jaVarC != null ? jaVarC.a(str, map, z) : FlurryEventRecordStatus.kFlurryEventFailed;
        } catch (Throwable th) {
            kf.a(a, "Failed to log event: " + str, th);
            return flurryEventRecordStatus;
        }
    }

    public static void endTimedEvent(String str) {
        if (Build.VERSION.SDK_INT < 10) {
            kf.b(a, "Device SDK Version older than 10");
            return;
        }
        if (str == null) {
            kf.b(a, "String eventId passed to endTimedEvent was null.");
            return;
        }
        try {
            hk.a();
            ja jaVarC = hk.c();
            if (jaVarC != null) {
                jaVarC.a(str, (Map<String, String>) null);
            }
        } catch (Throwable th) {
            kf.a(a, "Failed to signify the end of event: " + str, th);
        }
    }

    public static void endTimedEvent(String str, Map<String, String> map) {
        if (Build.VERSION.SDK_INT < 10) {
            kf.b(a, "Device SDK Version older than 10");
            return;
        }
        if (str == null) {
            kf.b(a, "String eventId passed to endTimedEvent was null.");
            return;
        }
        if (map == null) {
            kf.b(a, "String eventId passed to endTimedEvent was null.");
            return;
        }
        try {
            hk.a();
            ja jaVarC = hk.c();
            if (jaVarC != null) {
                jaVarC.a(str, map);
            }
        } catch (Throwable th) {
            kf.a(a, "Failed to signify the end of event: " + str, th);
        }
    }

    @Deprecated
    public static void onError(String str, String str2, String str3) {
        if (Build.VERSION.SDK_INT < 10) {
            kf.b(a, "Device SDK Version older than 10");
            return;
        }
        if (str == null) {
            kf.b(a, "String errorId passed to onError was null.");
            return;
        }
        if (str2 == null) {
            kf.b(a, "String message passed to onError was null.");
            return;
        }
        if (str3 == null) {
            kf.b(a, "String errorClass passed to onError was null.");
            return;
        }
        try {
            hk.a();
            StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
            if (stackTrace != null && stackTrace.length > 2) {
                StackTraceElement[] stackTraceElementArr = new StackTraceElement[stackTrace.length - 2];
                System.arraycopy(stackTrace, 2, stackTraceElementArr, 0, stackTraceElementArr.length);
                stackTrace = stackTraceElementArr;
            }
            Throwable th = new Throwable(str2);
            th.setStackTrace(stackTrace);
            ja jaVarC = hk.c();
            if (jaVarC != null) {
                jaVarC.a(str, str2, str3, th);
            }
        } catch (Throwable th2) {
            kf.a(a, "", th2);
        }
        kf.e(a, "'onError' method is deprecated.");
    }

    public static void onError(String str, String str2, Throwable th) {
        if (Build.VERSION.SDK_INT < 10) {
            kf.b(a, "Device SDK Version older than 10");
            return;
        }
        if (str == null) {
            kf.b(a, "String errorId passed to onError was null.");
            return;
        }
        if (str2 == null) {
            kf.b(a, "String message passed to onError was null.");
            return;
        }
        if (th == null) {
            kf.b(a, "Throwable passed to onError was null.");
            return;
        }
        try {
            hk.a();
            hk.a(str, str2, th);
        } catch (Throwable th2) {
            kf.a(a, "", th2);
        }
    }

    @Deprecated
    public static void onEvent(String str) {
        if (Build.VERSION.SDK_INT < 10) {
            kf.b(a, "Device SDK Version older than 10");
            return;
        }
        if (str == null) {
            kf.b(a, "String eventId passed to onEvent was null.");
            return;
        }
        try {
            hk.a();
            ja jaVarC = hk.c();
            if (jaVarC != null) {
                jaVarC.a(str, (Map<String, String>) null, false);
            }
        } catch (Throwable th) {
            kf.a(a, "", th);
        }
        kf.e(a, "'onEvent' method is deprecated.");
    }

    @Deprecated
    public static void onEvent(String str, Map<String, String> map) {
        if (Build.VERSION.SDK_INT < 10) {
            kf.b(a, "Device SDK Version older than 10");
            return;
        }
        if (str == null) {
            kf.b(a, "String eventId passed to onEvent was null.");
            return;
        }
        if (map == null) {
            kf.b(a, "Parameters Map passed to onEvent was null.");
            return;
        }
        try {
            hk.a();
            ja jaVarC = hk.c();
            if (jaVarC != null) {
                jaVarC.a(str, map, false);
            }
        } catch (Throwable th) {
            kf.a(a, "", th);
        }
        kf.e(a, "'onEvent' method is deprecated.");
    }

    public static void onPageView() {
        if (Build.VERSION.SDK_INT < 10) {
            kf.b(a, "Device SDK Version older than 10");
            return;
        }
        try {
            hk.a();
            ja jaVarC = hk.c();
            if (jaVarC != null) {
                jaVarC.c();
            }
        } catch (Throwable th) {
            kf.a(a, "", th);
        }
    }

    @Deprecated
    public static void setLocationCriteria(Criteria criteria) {
        if (Build.VERSION.SDK_INT < 10) {
            kf.b(a, "Device SDK Version older than 10");
        }
        kf.e(a, "'setLocationCriteria' method is deprecated.");
    }

    public static void setAge(int i2) {
        if (Build.VERSION.SDK_INT < 10) {
            kf.b(a, "Device SDK Version older than 10");
        } else {
            if (i2 <= 0 || i2 >= 110) {
                return;
            }
            li.a().a("Age", Long.valueOf(new Date(new Date(System.currentTimeMillis() - (((long) i2) * 31449600000L)).getYear(), 1, 1).getTime()));
        }
    }

    public static void setGender(byte b2) {
        if (Build.VERSION.SDK_INT < 10) {
            kf.b(a, "Device SDK Version older than 10");
        }
        switch (b2) {
            case 0:
            case 1:
                li.a().a("Gender", Byte.valueOf(b2));
                break;
            default:
                li.a().a("Gender", (Object) (byte) -1);
                break;
        }
    }

    public static void setUserId(String str) {
        if (Build.VERSION.SDK_INT < 10) {
            kf.b(a, "Device SDK Version older than 10");
        } else if (str == null) {
            kf.b(a, "String userId passed to setUserId was null.");
        } else {
            li.a().a("UserId", lr.b(str));
        }
    }

    public static void setSessionOrigin(String str, String str2) {
        if (Build.VERSION.SDK_INT < 10) {
            kf.b(a, "Device SDK Version older than 10");
            return;
        }
        if (TextUtils.isEmpty(str)) {
            kf.b(a, "String originName passed to setSessionOrigin was null or empty.");
            return;
        }
        if (jr.a() == null) {
            throw new IllegalStateException("Flurry SDK must be initialized before starting a session");
        }
        jd.a();
        jq jqVarI = jd.i();
        if (jqVarI != null) {
            jqVarI.a(str);
        }
        jd.a();
        jq jqVarI2 = jd.i();
        if (jqVarI2 != null) {
            jqVarI2.b(str2);
        }
    }

    public static void addSessionProperty(String str, String str2) {
        if (Build.VERSION.SDK_INT < 10) {
            kf.b(a, "Device SDK Version older than 10");
            return;
        }
        if (TextUtils.isEmpty(str) || TextUtils.isEmpty(str2)) {
            kf.b(a, "String name or value passed to addSessionProperty was null or empty.");
            return;
        }
        if (jr.a() == null) {
            throw new IllegalStateException("Flurry SDK must be initialized before starting a session");
        }
        jd.a();
        jq jqVarI = jd.i();
        if (jqVarI != null) {
            jqVarI.a(str, str2);
        }
    }

    public static class Builder {
        private static FlurryAgentListener a;
        private boolean b = false;
        private int c = 5;
        private long d = 10000;
        private boolean e = true;
        private boolean f = false;

        public Builder withListener(FlurryAgentListener flurryAgentListener) {
            a = flurryAgentListener;
            return this;
        }

        public Builder withLogEnabled(boolean z) {
            this.b = z;
            return this;
        }

        public Builder withLogLevel(int i) {
            this.c = i;
            return this;
        }

        public Builder withContinueSessionMillis(long j) {
            this.d = j;
            return this;
        }

        public Builder withCaptureUncaughtExceptions(boolean z) {
            this.e = z;
            return this;
        }

        public Builder withPulseEnabled(boolean z) {
            this.f = z;
            return this;
        }

        public void build(Context context, String str) {
            FlurryAgent.a(a, this.b, this.c, this.d, this.e, this.f, context, str);
        }
    }

    static /* synthetic */ void a(FlurryAgentListener flurryAgentListener, boolean z, int i2, long j, boolean z2, boolean z3, Context context, String str) {
        c = flurryAgentListener;
        setFlurryAgentListener(flurryAgentListener);
        d = z;
        setLogEnabled(z);
        e = i2;
        setLogLevel(i2);
        f = j;
        setContinueSessionMillis(j);
        g = z2;
        setCaptureUncaughtExceptions(z2);
        h = z3;
        setPulseEnabled(z3);
        i = str;
        init(context, i);
    }
}
