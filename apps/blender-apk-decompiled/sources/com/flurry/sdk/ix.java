package com.flurry.sdk;

import android.widget.Toast;
import com.flurry.sdk.kl;
import com.flurry.sdk.kn;
import com.flurry.sdk.lj;
import java.util.Arrays;

/* JADX INFO: loaded from: classes.dex */
public class ix extends kr implements lj.a {
    private static final String a = "ix";
    private static String f = "http://data.flurry.com/aap.do";
    private static String g = "https://data.flurry.com/aap.do";
    private String h;
    private boolean i;

    public ix() {
        this((byte) 0);
    }

    private ix(byte b) {
        super("Analytics", ix.class.getSimpleName());
        this.e = "AnalyticsData_";
        li liVarA = li.a();
        this.i = ((Boolean) liVarA.a("UseHttps")).booleanValue();
        liVarA.a("UseHttps", (lj.a) this);
        kf.a(4, a, "initSettings, UseHttps = " + this.i);
        String str = (String) liVarA.a("ReportUrl");
        liVarA.a("ReportUrl", (lj.a) this);
        b(str);
        kf.a(4, a, "initSettings, ReportUrl = " + str);
        b();
    }

    /* JADX WARN: Removed duplicated region for block: B:13:0x0023  */
    @Override // com.flurry.sdk.lj.a
    /*
        Code decompiled incorrectly, please refer to instructions dump.
        To view partially-correct add '--show-bad-code' argument
    */
    public final void a(java.lang.String r3, java.lang.Object r4) {
        /*
            r2 = this;
            int r0 = r3.hashCode()
            r1 = -239660092(0xfffffffff1b713c4, float:-1.8131089E30)
            if (r0 == r1) goto L19
            r1 = 1650629499(0x62629b7b, float:1.0450419E21)
            if (r0 == r1) goto Lf
            goto L23
        Lf:
            java.lang.String r0 = "ReportUrl"
            boolean r3 = r3.equals(r0)
            if (r3 == 0) goto L23
            r3 = 1
            goto L24
        L19:
            java.lang.String r0 = "UseHttps"
            boolean r3 = r3.equals(r0)
            if (r3 == 0) goto L23
            r3 = 0
            goto L24
        L23:
            r3 = -1
        L24:
            r0 = 4
            switch(r3) {
                case 0: goto L4a;
                case 1: goto L31;
                default: goto L28;
            }
        L28:
            r2 = 6
            java.lang.String r3 = com.flurry.sdk.ix.a
            java.lang.String r4 = "onSettingUpdate internal error!"
            com.flurry.sdk.kf.a(r2, r3, r4)
            return
        L31:
            java.lang.String r4 = (java.lang.String) r4
            r2.b(r4)
            java.lang.String r2 = com.flurry.sdk.ix.a
            java.lang.StringBuilder r3 = new java.lang.StringBuilder
            java.lang.String r1 = "onSettingUpdate, ReportUrl = "
            r3.<init>(r1)
            r3.append(r4)
            java.lang.String r3 = r3.toString()
            com.flurry.sdk.kf.a(r0, r2, r3)
            return
        L4a:
            java.lang.Boolean r4 = (java.lang.Boolean) r4
            boolean r3 = r4.booleanValue()
            r2.i = r3
            java.lang.String r3 = com.flurry.sdk.ix.a
            java.lang.StringBuilder r4 = new java.lang.StringBuilder
            java.lang.String r1 = "onSettingUpdate, UseHttps = "
            r4.<init>(r1)
            boolean r2 = r2.i
            r4.append(r2)
            java.lang.String r2 = r4.toString()
            com.flurry.sdk.kf.a(r0, r3, r2)
            return
        */
        throw new UnsupportedOperationException("Method not decompiled: com.flurry.sdk.ix.a(java.lang.String, java.lang.Object):void");
    }

    private void b(String str) {
        if (str != null && !str.endsWith(".do")) {
            kf.a(5, a, "overriding analytics agent report URL without an endpoint, are you sure?");
        }
        this.h = str;
    }

    /* JADX INFO: Access modifiers changed from: protected */
    @Override // com.flurry.sdk.kr
    public final void a(String str, String str2, final int i) {
        jr.a().b(new lw() { // from class: com.flurry.sdk.ix.2
            @Override // com.flurry.sdk.lw
            public final void a() {
                if (i == 200) {
                    hk.a();
                    ja jaVarC = hk.c();
                    if (jaVarC != null) {
                        jaVarC.j = true;
                    }
                }
            }
        });
        super.a(str, str2, i);
    }

    /* JADX INFO: Access modifiers changed from: protected */
    /* JADX WARN: Multi-variable type inference failed */
    @Override // com.flurry.sdk.kr
    public final void a(byte[] bArr, final String str, final String str2) {
        String str3;
        if (this.h != null) {
            str3 = this.h;
        } else if (this.i) {
            str3 = g;
        } else {
            str3 = f;
        }
        kf.a(4, a, "FlurryDataSender: start upload data " + Arrays.toString(bArr) + " with id = " + str + " to " + str3);
        kl klVar = new kl();
        klVar.f = str3;
        klVar.w = 100000;
        klVar.g = kn.a.kPost;
        klVar.a("Content-Type", "application/octet-stream");
        klVar.c = new kv();
        klVar.b = bArr;
        klVar.a = new kl.a<byte[], Void>() { // from class: com.flurry.sdk.ix.1
            @Override // com.flurry.sdk.kl.a
            public final /* synthetic */ void a(kl<byte[], Void> klVar2, Void r4) {
                final int i = klVar2.p;
                if (i > 0) {
                    kf.e(ix.a, "Analytics report sent.");
                    kf.a(3, ix.a, "FlurryDataSender: report " + str + " sent. HTTP response: " + i);
                    if (kf.c() <= 3 && kf.d()) {
                        jr.a().a(new Runnable() { // from class: com.flurry.sdk.ix.1.1
                            @Override // java.lang.Runnable
                            public final void run() {
                                Toast.makeText(jr.a().a, "SD HTTP Response Code: " + i, 0).show();
                            }
                        });
                    }
                    ix.this.a(str, str2, i);
                    ix.this.b();
                    return;
                }
                ix.this.a(str);
            }
        };
        jp.a().a(this, klVar);
    }
}
