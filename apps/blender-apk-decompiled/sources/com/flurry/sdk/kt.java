package com.flurry.sdk;

import com.flurry.sdk.ks;
import com.flurry.sdk.ku;
import java.io.Closeable;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;

/* JADX INFO: loaded from: classes.dex */
public class kt {
    public static final Integer a = 50;
    private static final String d = "kt";
    String b;
    LinkedHashMap<String, List<String>> c;

    public kt(String str) throws Throwable {
        this.b = str + "Main";
        b(this.b);
    }

    private void b(String str) throws Throwable {
        this.c = new LinkedHashMap<>();
        ArrayList<String> arrayList = new ArrayList();
        if (i(str)) {
            List<String> listJ = j(str);
            if (listJ != null && listJ.size() > 0) {
                arrayList.addAll(listJ);
                Iterator it = arrayList.iterator();
                while (it.hasNext()) {
                    c((String) it.next());
                }
            }
            h(str);
        } else {
            List list = (List) new jy(jr.a().a.getFileStreamPath(d(this.b)), str, 1, new lc<List<ku>>() { // from class: com.flurry.sdk.kt.1
                @Override // com.flurry.sdk.lc
                public final kz<List<ku>> a(int i) {
                    return new ky(new ku.a());
                }
            }).a();
            if (list == null) {
                kf.c(d, "New main file also not found. returning..");
                return;
            } else {
                Iterator it2 = list.iterator();
                while (it2.hasNext()) {
                    arrayList.add(((ku) it2.next()).a);
                }
            }
        }
        for (String str2 : arrayList) {
            this.c.put(str2, g(str2));
        }
    }

    private void c(String str) throws Throwable {
        List<String> listJ = j(str);
        if (listJ == null) {
            kf.c(d, "No old file to replace");
            return;
        }
        for (String str2 : listJ) {
            byte[] bArrK = k(str2);
            if (bArrK == null) {
                kf.a(6, d, "File does not exist");
            } else {
                a(str2, bArrK);
                lr.b();
                String str3 = d;
                StringBuilder sb = new StringBuilder("Deleting  block File for ");
                sb.append(str2);
                sb.append(" file name:");
                sb.append(jr.a().a.getFileStreamPath(".flurrydatasenderblock." + str2));
                kf.a(5, str3, sb.toString());
                File fileStreamPath = jr.a().a.getFileStreamPath(".flurrydatasenderblock." + str2);
                if (fileStreamPath.exists()) {
                    boolean zDelete = fileStreamPath.delete();
                    kf.a(5, d, "Found file for " + str2 + ". Deleted - " + zDelete);
                }
            }
        }
        if (listJ != null) {
            a(str, listJ, ".YFlurrySenderIndex.info.");
            h(str);
        }
    }

    private static String d(String str) {
        return ".YFlurrySenderIndex.info." + str;
    }

    private synchronized void a() {
        LinkedList linkedList = new LinkedList(this.c.keySet());
        new jy(jr.a().a.getFileStreamPath(d(this.b)), ".YFlurrySenderIndex.info.", 1, new lc<List<ku>>() { // from class: com.flurry.sdk.kt.4
            @Override // com.flurry.sdk.lc
            public final kz<List<ku>> a(int i) {
                return new ky(new ku.a());
            }
        }).b();
        if (!linkedList.isEmpty()) {
            a(this.b, linkedList, this.b);
        }
    }

    public final synchronized void a(ks ksVar, String str) {
        boolean z;
        kf.a(4, d, "addBlockInfo" + str);
        String str2 = ksVar.a;
        List<String> linkedList = this.c.get(str);
        if (linkedList == null) {
            kf.a(4, d, "New Data Key");
            linkedList = new LinkedList<>();
            z = true;
        } else {
            z = false;
        }
        linkedList.add(str2);
        if (linkedList.size() > a.intValue()) {
            e(linkedList.get(0));
            linkedList.remove(0);
        }
        this.c.put(str, linkedList);
        a(str, linkedList, ".YFlurrySenderIndex.info.");
        if (z) {
            a();
        }
    }

    private boolean e(String str) {
        return new jy(jr.a().a.getFileStreamPath(ks.a(str)), ".yflurrydatasenderblock.", 1, new lc<ks>() { // from class: com.flurry.sdk.kt.2
            @Override // com.flurry.sdk.lc
            public final kz<ks> a(int i) {
                return new ks.a();
            }
        }).b();
    }

    public final boolean a(String str, String str2) {
        boolean zRemove;
        List<String> list = this.c.get(str2);
        if (list != null) {
            e(str);
            zRemove = list.remove(str);
        } else {
            zRemove = false;
        }
        if (list != null && !list.isEmpty()) {
            this.c.put(str2, list);
            a(str2, list, ".YFlurrySenderIndex.info.");
        } else {
            f(str2);
        }
        return zRemove;
    }

    public final List<String> a(String str) {
        return this.c.get(str);
    }

    private synchronized boolean f(String str) {
        boolean zB;
        lr.b();
        jy jyVar = new jy(jr.a().a.getFileStreamPath(d(str)), ".YFlurrySenderIndex.info.", 1, new lc<List<ku>>() { // from class: com.flurry.sdk.kt.3
            @Override // com.flurry.sdk.lc
            public final kz<List<ku>> a(int i) {
                return new ky(new ku.a());
            }
        });
        List<String> listA = a(str);
        if (listA != null) {
            kf.a(4, d, "discardOutdatedBlocksForDataKey: notSentBlocks = " + listA.size());
            for (String str2 : listA) {
                e(str2);
                kf.a(4, d, "discardOutdatedBlocksForDataKey: removed block = " + str2);
            }
        }
        this.c.remove(str);
        zB = jyVar.b();
        a();
        return zB;
    }

    private synchronized List<String> g(String str) {
        ArrayList arrayList;
        lr.b();
        kf.a(5, d, "Reading Index File for " + str + " file name:" + jr.a().a.getFileStreamPath(d(str)));
        List list = (List) new jy(jr.a().a.getFileStreamPath(d(str)), ".YFlurrySenderIndex.info.", 1, new lc<List<ku>>() { // from class: com.flurry.sdk.kt.5
            @Override // com.flurry.sdk.lc
            public final kz<List<ku>> a(int i) {
                return new ky(new ku.a());
            }
        }).a();
        arrayList = new ArrayList();
        Iterator it = list.iterator();
        while (it.hasNext()) {
            arrayList.add(((ku) it.next()).a);
        }
        return arrayList;
    }

    private synchronized void a(String str, byte[] bArr) {
        lr.b();
        kf.a(5, d, "Saving Block File for " + str + " file name:" + jr.a().a.getFileStreamPath(ks.a(str)));
        new jy(jr.a().a.getFileStreamPath(ks.a(str)), ".yflurrydatasenderblock.", 1, new lc<ks>() { // from class: com.flurry.sdk.kt.6
            @Override // com.flurry.sdk.lc
            public final kz<ks> a(int i) {
                return new ks.a();
            }
        }).a(new ks(bArr));
    }

    private static void h(String str) {
        lr.b();
        String str2 = d;
        StringBuilder sb = new StringBuilder("Deleting Index File for ");
        sb.append(str);
        sb.append(" file name:");
        sb.append(jr.a().a.getFileStreamPath(".FlurrySenderIndex.info." + str));
        kf.a(5, str2, sb.toString());
        File fileStreamPath = jr.a().a.getFileStreamPath(".FlurrySenderIndex.info." + str);
        if (fileStreamPath.exists()) {
            boolean zDelete = fileStreamPath.delete();
            kf.a(5, d, "Found file for " + str + ". Deleted - " + zDelete);
        }
    }

    private synchronized void a(String str, List<String> list, String str2) {
        lr.b();
        kf.a(5, d, "Saving Index File for " + str + " file name:" + jr.a().a.getFileStreamPath(d(str)));
        jy jyVar = new jy(jr.a().a.getFileStreamPath(d(str)), str2, 1, new lc<List<ku>>() { // from class: com.flurry.sdk.kt.7
            @Override // com.flurry.sdk.lc
            public final kz<List<ku>> a(int i) {
                return new ky(new ku.a());
            }
        });
        ArrayList arrayList = new ArrayList();
        Iterator<String> it = list.iterator();
        while (it.hasNext()) {
            arrayList.add(new ku(it.next()));
        }
        jyVar.a(arrayList);
    }

    private synchronized boolean i(String str) {
        File fileStreamPath;
        fileStreamPath = jr.a().a.getFileStreamPath(".FlurrySenderIndex.info." + str);
        kf.a(5, d, "isOldIndexFilePresent: for " + str + fileStreamPath.exists());
        return fileStreamPath.exists();
    }

    private synchronized List<String> j(String str) {
        ArrayList arrayList;
        DataInputStream dataInputStream;
        lr.b();
        String str2 = d;
        StringBuilder sb = new StringBuilder("Reading Index File for ");
        sb.append(str);
        sb.append(" file name:");
        sb.append(jr.a().a.getFileStreamPath(".FlurrySenderIndex.info." + str));
        kf.a(5, str2, sb.toString());
        File fileStreamPath = jr.a().a.getFileStreamPath(".FlurrySenderIndex.info." + str);
        DataInputStream dataInputStream2 = null;
        if (fileStreamPath.exists()) {
            kf.a(5, d, "Reading Index File for " + str + " Found file.");
            try {
                try {
                    dataInputStream = new DataInputStream(new FileInputStream(fileStreamPath));
                    try {
                        try {
                            int unsignedShort = dataInputStream.readUnsignedShort();
                            if (unsignedShort != 0) {
                                arrayList = new ArrayList(unsignedShort);
                                for (int i = 0; i < unsignedShort; i++) {
                                    try {
                                        int unsignedShort2 = dataInputStream.readUnsignedShort();
                                        kf.a(4, d, "read iter " + i + " dataLength = " + unsignedShort2);
                                        byte[] bArr = new byte[unsignedShort2];
                                        dataInputStream.readFully(bArr);
                                        arrayList.add(new String(bArr));
                                    } catch (Throwable th) {
                                        th = th;
                                        dataInputStream2 = dataInputStream;
                                        kf.a(6, d, "Error when loading persistent file", th);
                                        lr.a((Closeable) dataInputStream2);
                                        return arrayList;
                                    }
                                }
                                dataInputStream.readUnsignedShort();
                                lr.a((Closeable) dataInputStream);
                            } else {
                                lr.a((Closeable) dataInputStream);
                                return null;
                            }
                        } catch (Throwable th2) {
                            th = th2;
                            lr.a((Closeable) dataInputStream);
                            throw th;
                        }
                    } catch (Throwable th3) {
                        th = th3;
                        arrayList = null;
                    }
                } catch (Throwable th4) {
                    th = th4;
                    arrayList = null;
                }
            } catch (Throwable th5) {
                th = th5;
                dataInputStream = dataInputStream2;
            }
        } else {
            kf.a(5, d, "Agent cache file doesn't exist.");
            arrayList = null;
        }
        return arrayList;
    }

    private static byte[] k(String str) throws Throwable {
        byte[] bArr;
        lr.b();
        String str2 = d;
        StringBuilder sb = new StringBuilder("Reading block File for ");
        sb.append(str);
        sb.append(" file name:");
        sb.append(jr.a().a.getFileStreamPath(".flurrydatasenderblock." + str));
        kf.a(5, str2, sb.toString());
        File fileStreamPath = jr.a().a.getFileStreamPath(".flurrydatasenderblock." + str);
        DataInputStream dataInputStream = null;
        if (fileStreamPath.exists()) {
            kf.a(5, d, "Reading Index File for " + str + " Found file.");
            try {
                try {
                    DataInputStream dataInputStream2 = new DataInputStream(new FileInputStream(fileStreamPath));
                    try {
                        try {
                            int unsignedShort = dataInputStream2.readUnsignedShort();
                            if (unsignedShort != 0) {
                                bArr = new byte[unsignedShort];
                                try {
                                    dataInputStream2.readFully(bArr);
                                    dataInputStream2.readUnsignedShort();
                                    lr.a((Closeable) dataInputStream2);
                                    return bArr;
                                } catch (Throwable th) {
                                    th = th;
                                    dataInputStream = dataInputStream2;
                                    kf.a(6, d, "Error when loading persistent file", th);
                                    lr.a((Closeable) dataInputStream);
                                    return bArr;
                                }
                            }
                            lr.a((Closeable) dataInputStream2);
                            return null;
                        } catch (Throwable th2) {
                            th = th2;
                            dataInputStream = dataInputStream2;
                            lr.a((Closeable) dataInputStream);
                            throw th;
                        }
                    } catch (Throwable th3) {
                        th = th3;
                        bArr = null;
                    }
                } catch (Throwable th4) {
                    th = th4;
                }
            } catch (Throwable th5) {
                th = th5;
                bArr = null;
            }
        } else {
            kf.a(4, d, "Agent cache file doesn't exist.");
            return null;
        }
    }
}
