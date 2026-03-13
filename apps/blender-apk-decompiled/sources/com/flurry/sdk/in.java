package com.flurry.sdk;

import android.os.Build;
import com.flurry.sdk.io;
import com.flurry.sdk.kl;
import com.flurry.sdk.kn;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.zip.CRC32;

/* JADX INFO: loaded from: classes.dex */
public class in {
    public static final String b = "com.flurry.sdk.in";
    private static in c;
    public String a;
    private jy<List<io>> d;
    private List<io> e;
    private boolean f;

    private in() {
    }

    public static synchronized in a() {
        if (c == null) {
            in inVar = new in();
            c = inVar;
            inVar.d = new jy<>(jr.a().a.getFileStreamPath(".yflurrypulselogging." + Long.toString(lr.i(jr.a().d), 16)), ".yflurrypulselogging.", 1, new lc<List<io>>() { // from class: com.flurry.sdk.in.1
                @Override // com.flurry.sdk.lc
                public final kz<List<io>> a(int i) {
                    return new ky(new io.a());
                }
            });
            inVar.f = ((Boolean) li.a().a("UseHttps")).booleanValue();
            kf.a(4, b, "initSettings, UseHttps = " + inVar.f);
            inVar.e = inVar.d.a();
            if (inVar.e == null) {
                inVar.e = new ArrayList();
            }
        }
        return c;
    }

    public final synchronized void a(im imVar) {
        try {
            this.e.add(new io(imVar.d()));
            kf.a(4, b, "Saving persistent Pulse logging data.");
            this.d.a(this.e);
        } catch (IOException unused) {
            kf.a(6, b, "Error when generating pulse log report in addReport part");
        }
    }

    private byte[] d() throws Throwable {
        DataOutputStream dataOutputStream;
        ByteArrayOutputStream byteArrayOutputStream;
        DataOutputStream dataOutputStream2 = null;
        try {
            try {
                byteArrayOutputStream = new ByteArrayOutputStream();
                dataOutputStream = new DataOutputStream(byteArrayOutputStream);
            } catch (IOException e) {
                e = e;
            }
        } catch (Throwable th) {
            th = th;
            dataOutputStream = dataOutputStream2;
        }
        try {
            if (this.e != null && !this.e.isEmpty()) {
                dataOutputStream.writeShort(1);
                dataOutputStream.writeShort(1);
                dataOutputStream.writeLong(System.currentTimeMillis());
                dataOutputStream.writeUTF(jr.a().d);
                dataOutputStream.writeUTF(jn.a().i());
                dataOutputStream.writeShort(js.a());
                dataOutputStream.writeShort(3);
                jn.a();
                dataOutputStream.writeUTF(jn.d());
                dataOutputStream.writeBoolean(je.a().d());
                ArrayList<ht> arrayList = new ArrayList();
                for (Map.Entry entry : Collections.unmodifiableMap(je.a().a).entrySet()) {
                    ht htVar = new ht();
                    htVar.a = ((jm) entry.getKey()).c;
                    if (((jm) entry.getKey()).d) {
                        htVar.b = new String((byte[]) entry.getValue());
                    } else {
                        htVar.b = lr.b((byte[]) entry.getValue());
                    }
                    arrayList.add(htVar);
                }
                dataOutputStream.writeShort(arrayList.size());
                for (ht htVar2 : arrayList) {
                    dataOutputStream.writeShort(htVar2.a);
                    byte[] bytes = htVar2.b.getBytes();
                    dataOutputStream.writeShort(bytes.length);
                    dataOutputStream.write(bytes);
                }
                dataOutputStream.writeShort(6);
                dataOutputStream.writeShort(ig.MODEL.h);
                dataOutputStream.writeUTF(Build.MODEL);
                dataOutputStream.writeShort(ig.BRAND.h);
                dataOutputStream.writeUTF(Build.BOARD);
                dataOutputStream.writeShort(ig.ID.h);
                dataOutputStream.writeUTF(Build.ID);
                dataOutputStream.writeShort(ig.DEVICE.h);
                dataOutputStream.writeUTF(Build.DEVICE);
                dataOutputStream.writeShort(ig.PRODUCT.h);
                dataOutputStream.writeUTF(Build.PRODUCT);
                dataOutputStream.writeShort(ig.VERSION_RELEASE.h);
                dataOutputStream.writeUTF(Build.VERSION.RELEASE);
                dataOutputStream.writeShort(this.e.size());
                Iterator<io> it = this.e.iterator();
                while (it.hasNext()) {
                    dataOutputStream.write(it.next().a);
                }
                byte[] byteArray = byteArrayOutputStream.toByteArray();
                CRC32 crc32 = new CRC32();
                crc32.update(byteArray);
                dataOutputStream.writeInt((int) crc32.getValue());
                byte[] byteArray2 = byteArrayOutputStream.toByteArray();
                lr.a(dataOutputStream);
                return byteArray2;
            }
            byte[] byteArray3 = byteArrayOutputStream.toByteArray();
            lr.a(dataOutputStream);
            return byteArray3;
        } catch (IOException e2) {
            e = e2;
            dataOutputStream2 = dataOutputStream;
            kf.a(6, b, "Error when generating report", e);
            throw e;
        } catch (Throwable th2) {
            th = th2;
            lr.a(dataOutputStream);
            throw th;
        }
    }

    /* JADX WARN: Multi-variable type inference failed */
    private synchronized void a(byte[] bArr) {
        if (!jk.a().b) {
            kf.a(5, b, "Reports were not sent! No Internet connection!");
            return;
        }
        if (bArr != 0 && bArr.length != 0) {
            String str = this.a != null ? this.a : "https://data.flurry.com/pcr.do";
            kf.a(4, b, "PulseLoggingManager: start upload data " + Arrays.toString(bArr) + " to " + str);
            kl klVar = new kl();
            klVar.f = str;
            klVar.w = 100000;
            klVar.g = kn.a.kPost;
            klVar.j = true;
            klVar.a("Content-Type", "application/octet-stream");
            klVar.c = new kv();
            klVar.b = bArr;
            klVar.a = new kl.a<byte[], Void>() { // from class: com.flurry.sdk.in.2
                @Override // com.flurry.sdk.kl.a
                public final /* synthetic */ void a(kl<byte[], Void> klVar2, Void r5) throws Throwable {
                    int i = klVar2.p;
                    if (i <= 0) {
                        kf.e(in.b, "Server Error: " + i);
                        return;
                    }
                    if (i < 200 || i >= 300) {
                        kf.a(3, in.b, "Pulse logging report sent unsuccessfully, HTTP response:" + i);
                        return;
                    }
                    kf.a(3, in.b, "Pulse logging report sent successfully HTTP response:" + i);
                    in.this.e.clear();
                    in.this.d.a(in.this.e);
                }
            };
            jp.a().a(this, klVar);
            return;
        }
        kf.a(3, b, "No report need be sent");
    }

    public final synchronized void b() {
        try {
            a(d());
        } catch (IOException unused) {
            kf.a(6, b, "Report not send due to exception in generate data");
        }
    }
}
