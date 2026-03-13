package com.flurry.sdk;

import android.provider.Settings;
import android.text.TextUtils;
import com.flurry.sdk.le;
import java.io.Closeable;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/* JADX INFO: loaded from: classes.dex */
public class je {
    private static final String b = "je";
    private static je c;
    public final Map<jm, byte[]> a;
    private final Set<String> d;
    private final ka<le> e;
    private a f;
    private jo g;
    private String h;

    enum a {
        NONE,
        ADVERTISING,
        DEVICE,
        REPORTED_IDS,
        FINISHED
    }

    public static synchronized je a() {
        if (c == null) {
            c = new je();
        }
        return c;
    }

    public static void b() {
        c = null;
    }

    private je() {
        HashSet hashSet = new HashSet();
        hashSet.add("null");
        hashSet.add("9774d56d682e549c");
        hashSet.add("dead00beef");
        this.d = Collections.unmodifiableSet(hashSet);
        this.a = new HashMap();
        this.e = new ka<le>() { // from class: com.flurry.sdk.je.1
            @Override // com.flurry.sdk.ka
            public final /* synthetic */ void a(jz jzVar) {
                if (AnonymousClass4.a[((le) jzVar).c - 1] == 1 && je.this.c()) {
                    jr.a().b(new lw() { // from class: com.flurry.sdk.je.1.1
                        @Override // com.flurry.sdk.lw
                        public final void a() {
                            je.this.e();
                        }
                    });
                }
            }
        };
        this.f = a.NONE;
        kb.a().a("com.flurry.android.sdk.FlurrySessionEvent", this.e);
        jr.a().b(new lw() { // from class: com.flurry.sdk.je.2
            @Override // com.flurry.sdk.lw
            public final void a() throws Throwable {
                je.b(je.this);
            }
        });
    }

    public final boolean c() {
        return a.FINISHED.equals(this.f);
    }

    public final boolean d() {
        return this.g == null || !this.g.b;
    }

    /* JADX INFO: renamed from: com.flurry.sdk.je$4, reason: invalid class name */
    static /* synthetic */ class AnonymousClass4 {
        static final /* synthetic */ int[] a;

        static {
            try {
                b[a.NONE.ordinal()] = 1;
            } catch (NoSuchFieldError unused) {
            }
            try {
                b[a.ADVERTISING.ordinal()] = 2;
            } catch (NoSuchFieldError unused2) {
            }
            try {
                b[a.DEVICE.ordinal()] = 3;
            } catch (NoSuchFieldError unused3) {
            }
            try {
                b[a.REPORTED_IDS.ordinal()] = 4;
            } catch (NoSuchFieldError unused4) {
            }
            a = new int[le.a.a().length];
            try {
                a[le.a.a - 1] = 1;
            } catch (NoSuchFieldError unused5) {
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void e() {
        lr.b();
        if (ls.a(jr.a().a)) {
            this.g = ls.b(jr.a().a);
            if (c()) {
                h();
                kb.a().a(new jg());
            }
        }
    }

    private static void a(String str, File file) throws Throwable {
        DataOutputStream dataOutputStream;
        DataOutputStream dataOutputStream2 = null;
        try {
            try {
                dataOutputStream = new DataOutputStream(new FileOutputStream(file));
            } catch (Throwable th) {
                th = th;
            }
        } catch (Throwable th2) {
            th = th2;
        }
        try {
            dataOutputStream.writeInt(1);
            dataOutputStream.writeUTF(str);
            lr.a(dataOutputStream);
        } catch (Throwable th3) {
            th = th3;
            dataOutputStream2 = dataOutputStream;
            kf.a(6, b, "Error when saving deviceId", th);
            lr.a(dataOutputStream2);
        }
    }

    /* JADX WARN: Multi-variable type inference failed */
    /* JADX WARN: Type inference failed for: r2v0, types: [boolean] */
    /* JADX WARN: Type inference failed for: r2v1 */
    /* JADX WARN: Type inference failed for: r2v10 */
    /* JADX WARN: Type inference failed for: r2v2, types: [java.io.Closeable] */
    /* JADX WARN: Type inference failed for: r2v4 */
    /* JADX WARN: Type inference failed for: r2v6, types: [java.io.Closeable] */
    /* JADX WARN: Type inference failed for: r2v8 */
    /* JADX WARN: Type inference failed for: r2v9 */
    private static String f() throws Throwable {
        DataInputStream dataInputStream;
        File fileStreamPath = jr.a().a.getFileStreamPath(".flurryb.");
        String utf = null;
        if (fileStreamPath != null) {
            ?? Exists = fileStreamPath.exists();
            try {
                if (Exists != 0) {
                    try {
                        dataInputStream = new DataInputStream(new FileInputStream(fileStreamPath));
                        try {
                            if (1 != dataInputStream.readInt()) {
                                Exists = dataInputStream;
                            } else {
                                utf = dataInputStream.readUTF();
                                Exists = dataInputStream;
                            }
                        } catch (Throwable th) {
                            th = th;
                            kf.a(6, b, "Error when loading deviceId", th);
                            Exists = dataInputStream;
                        }
                    } catch (Throwable th2) {
                        th = th2;
                        dataInputStream = null;
                    }
                    lr.a((Closeable) Exists);
                    return utf;
                }
            } catch (Throwable th3) {
                th = th3;
            }
        }
        return null;
    }

    /* JADX WARN: Multi-variable type inference failed */
    /* JADX WARN: Type inference failed for: r0v10 */
    /* JADX WARN: Type inference failed for: r0v13, types: [java.io.Closeable] */
    /* JADX WARN: Type inference failed for: r0v15 */
    /* JADX WARN: Type inference failed for: r0v16 */
    /* JADX WARN: Type inference failed for: r0v17 */
    /* JADX WARN: Type inference failed for: r0v18 */
    /* JADX WARN: Type inference failed for: r0v7, types: [boolean] */
    /* JADX WARN: Type inference failed for: r0v8 */
    /* JADX WARN: Type inference failed for: r0v9, types: [java.io.Closeable] */
    private String g() throws Throwable {
        String[] list;
        DataInputStream dataInputStream;
        File filesDir = jr.a().a.getFilesDir();
        String utf = null;
        if (filesDir == null || (list = filesDir.list(new FilenameFilter() { // from class: com.flurry.sdk.je.3
            @Override // java.io.FilenameFilter
            public final boolean accept(File file, String str) {
                return str.startsWith(".flurryagent.");
            }
        })) == null || list.length == 0) {
            return null;
        }
        File fileStreamPath = jr.a().a.getFileStreamPath(list[0]);
        if (fileStreamPath != null) {
            ?? Exists = fileStreamPath.exists();
            try {
                if (Exists != 0) {
                    try {
                        dataInputStream = new DataInputStream(new FileInputStream(fileStreamPath));
                        try {
                            if (46586 == dataInputStream.readUnsignedShort() && 2 == dataInputStream.readUnsignedShort()) {
                                dataInputStream.readUTF();
                                utf = dataInputStream.readUTF();
                                Exists = dataInputStream;
                            } else {
                                Exists = dataInputStream;
                            }
                        } catch (Throwable th) {
                            th = th;
                            kf.a(6, b, "Error when loading deviceId", th);
                            Exists = dataInputStream;
                        }
                    } catch (Throwable th2) {
                        th = th2;
                        Exists = 0;
                        lr.a((Closeable) Exists);
                        throw th;
                    }
                    lr.a((Closeable) Exists);
                    return utf;
                }
            } catch (Throwable th3) {
                th = th3;
            }
        }
        return null;
    }

    private void h() {
        String str = this.g == null ? null : this.g.a;
        if (str != null) {
            kf.a(3, b, "Fetched advertising id");
            this.a.put(jm.AndroidAdvertisingId, lr.e(str));
        }
        String str2 = this.h;
        if (str2 != null) {
            kf.a(3, b, "Fetched device id");
            this.a.put(jm.DeviceId, lr.e(str2));
        }
    }

    static /* synthetic */ void b(je jeVar) throws Throwable {
        String strF;
        while (!a.FINISHED.equals(jeVar.f)) {
            switch (jeVar.f) {
                case NONE:
                    jeVar.f = a.ADVERTISING;
                    break;
                case ADVERTISING:
                    jeVar.f = a.DEVICE;
                    break;
                case DEVICE:
                    jeVar.f = a.REPORTED_IDS;
                    break;
                case REPORTED_IDS:
                    jeVar.f = a.FINISHED;
                    break;
            }
            try {
            } catch (Exception e) {
                kf.a(4, b, "Exception during id fetch:" + jeVar.f + ", " + e);
            }
            switch (jeVar.f) {
                case ADVERTISING:
                    jeVar.e();
                    continue;
                case DEVICE:
                    lr.b();
                    String string = Settings.Secure.getString(jr.a().a.getContentResolver(), "android_id");
                    boolean z = false;
                    if (!TextUtils.isEmpty(string)) {
                        if (!jeVar.d.contains(string.toLowerCase(Locale.US))) {
                            z = true;
                        }
                    }
                    if (z) {
                        strF = "AND" + string;
                    } else {
                        strF = null;
                    }
                    if (TextUtils.isEmpty(strF)) {
                        strF = f();
                        if (TextUtils.isEmpty(strF)) {
                            strF = jeVar.g();
                            if (TextUtils.isEmpty(strF)) {
                                strF = "ID" + Long.toString(Double.doubleToLongBits(Math.random()) + ((System.nanoTime() + (lr.i(lo.a(jr.a().a)) * 37)) * 37), 16);
                            }
                            if (!TextUtils.isEmpty(strF)) {
                                File fileStreamPath = jr.a().a.getFileStreamPath(".flurryb.");
                                if (lq.a(fileStreamPath)) {
                                    a(strF, fileStreamPath);
                                }
                            }
                        }
                    }
                    jeVar.h = strF;
                    continue;
                case REPORTED_IDS:
                    jeVar.h();
                    continue;
                default:
                    continue;
            }
            kf.a(4, b, "Exception during id fetch:" + jeVar.f + ", " + e);
        }
        kb.a().a(new jf());
    }
}
