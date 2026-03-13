package com.flurry.sdk;

import android.os.Build;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.security.DigestOutputStream;
import java.util.List;
import java.util.Map;

/* JADX INFO: loaded from: classes.dex */
public class iw {
    private static final String b = "iw";
    public byte[] a;

    public iw(String str, String str2, boolean z, boolean z2, long j, long j2, List<iy> list, Map<jm, byte[]> map, Map<String, List<String>> map2, Map<String, List<String>> map3, Map<String, Map<String, String>> map4, long j3) throws Throwable {
        Throwable th;
        DataOutputStream dataOutputStream;
        Throwable th2;
        DataOutputStream dataOutputStream2;
        byte[] byteArray;
        this.a = null;
        try {
            jx jxVar = new jx();
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            DigestOutputStream digestOutputStream = new DigestOutputStream(byteArrayOutputStream, jxVar);
            dataOutputStream = new DataOutputStream(digestOutputStream);
            try {
                dataOutputStream.writeShort(30);
                dataOutputStream.writeShort(0);
                dataOutputStream.writeLong(0L);
                dataOutputStream.writeShort(0);
                dataOutputStream.writeShort(3);
                dataOutputStream.writeShort(js.a());
                dataOutputStream.writeLong(j3);
                dataOutputStream.writeUTF(str);
                dataOutputStream.writeUTF(str2);
                dataOutputStream.writeShort(map.size());
                for (Map.Entry<jm, byte[]> entry : map.entrySet()) {
                    dataOutputStream.writeShort(entry.getKey().c);
                    byte[] value = entry.getValue();
                    dataOutputStream.writeShort(value.length);
                    dataOutputStream.write(value);
                }
                dataOutputStream.writeByte(0);
                dataOutputStream.writeBoolean(z);
                dataOutputStream.writeBoolean(z2);
                dataOutputStream.writeLong(j);
                dataOutputStream.writeLong(j2);
                dataOutputStream.writeShort(6);
                dataOutputStream.writeUTF("device.model");
                dataOutputStream.writeUTF(Build.MODEL);
                dataOutputStream.writeByte(0);
                dataOutputStream.writeUTF("build.brand");
                dataOutputStream.writeUTF(Build.BRAND);
                dataOutputStream.writeByte(0);
                dataOutputStream.writeUTF("build.id");
                dataOutputStream.writeUTF(Build.ID);
                dataOutputStream.writeByte(0);
                dataOutputStream.writeUTF("version.release");
                dataOutputStream.writeUTF(Build.VERSION.RELEASE);
                dataOutputStream.writeByte(0);
                dataOutputStream.writeUTF("build.device");
                dataOutputStream.writeUTF(Build.DEVICE);
                dataOutputStream.writeByte(0);
                dataOutputStream.writeUTF("build.product");
                dataOutputStream.writeUTF(Build.PRODUCT);
                dataOutputStream.writeByte(0);
                dataOutputStream.writeShort(map2 != null ? map2.keySet().size() : 0);
                if (map2 != null) {
                    kf.a(3, b, "sending referrer values because it exists");
                    for (Map.Entry<String, List<String>> entry2 : map2.entrySet()) {
                        kf.a(3, b, "Referrer Entry:  " + entry2.getKey() + "=" + entry2.getValue());
                        dataOutputStream.writeUTF(entry2.getKey());
                        kf.a(3, b, "referrer key is :" + entry2.getKey());
                        dataOutputStream.writeShort(entry2.getValue().size());
                        for (String str3 : entry2.getValue()) {
                            dataOutputStream.writeUTF(str3);
                            kf.a(3, b, "referrer value is :" + str3);
                        }
                    }
                }
                dataOutputStream.writeBoolean(false);
                int size = map3 != null ? map3.keySet().size() : 0;
                kf.a(3, b, "optionsMapSize is:  " + size);
                dataOutputStream.writeShort(size);
                if (map3 != null) {
                    kf.a(3, b, "sending launch options");
                    for (Map.Entry<String, List<String>> entry3 : map3.entrySet()) {
                        kf.a(3, b, "Launch Options Key:  " + entry3.getKey());
                        dataOutputStream.writeUTF(entry3.getKey());
                        dataOutputStream.writeShort(entry3.getValue().size());
                        for (String str4 : entry3.getValue()) {
                            dataOutputStream.writeUTF(str4);
                            kf.a(3, b, "Launch Options value is :" + str4);
                        }
                    }
                }
                int size2 = map4 != null ? map4.keySet().size() : 0;
                kf.a(3, b, "numOriginAttributions is:  " + size);
                dataOutputStream.writeShort(size2);
                if (map4 != null) {
                    for (Map.Entry<String, Map<String, String>> entry4 : map4.entrySet()) {
                        kf.a(3, b, "Origin Atttribute Key:  " + entry4.getKey());
                        dataOutputStream.writeUTF(entry4.getKey());
                        dataOutputStream.writeShort(entry4.getValue().size());
                        kf.a(3, b, "Origin Attribute Map Size for " + entry4.getKey() + ":  " + entry4.getValue().size());
                        for (Map.Entry<String, String> entry5 : entry4.getValue().entrySet()) {
                            kf.a(3, b, "Origin Atttribute for " + entry4.getKey() + ":  " + entry5.getKey() + ":" + entry5.getValue());
                            dataOutputStream.writeUTF(entry5.getKey() != null ? entry5.getKey() : "");
                            dataOutputStream.writeUTF(entry5.getValue() != null ? entry5.getValue() : "");
                        }
                    }
                }
                dataOutputStream.writeUTF(lo.a(jr.a().a));
                int size3 = list.size();
                dataOutputStream.writeShort(size3);
                for (int i = 0; i < size3; i++) {
                    dataOutputStream.write(list.get(i).a);
                }
                dataOutputStream.writeShort(0);
                digestOutputStream.on(false);
                dataOutputStream.write(jxVar.a());
                dataOutputStream.close();
                byteArray = byteArrayOutputStream.toByteArray();
                lr.a(dataOutputStream);
            } catch (Throwable th3) {
                th = th3;
                lr.a(dataOutputStream);
                throw th;
            }
        } catch (Throwable th4) {
            th = th4;
            dataOutputStream = null;
        }
        this.a = byteArray;
    }
}
