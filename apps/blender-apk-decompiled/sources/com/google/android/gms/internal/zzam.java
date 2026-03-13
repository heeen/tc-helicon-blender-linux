package com.google.android.gms.internal;

import android.os.SystemClock;
import android.text.TextUtils;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/* JADX INFO: loaded from: classes.dex */
public final class zzam implements zzb {
    private final Map<String, zzan> zzbw;
    private long zzbx;
    private final File zzby;
    private final int zzbz;

    public zzam(File file) {
        this(file, 5242880);
    }

    private zzam(File file, int i) {
        this.zzbw = new LinkedHashMap(16, 0.75f, true);
        this.zzbx = 0L;
        this.zzby = file;
        this.zzbz = 5242880;
    }

    private final synchronized void remove(String str) {
        boolean zDelete = zze(str).delete();
        removeEntry(str);
        if (!zDelete) {
            zzaf.zzb("Could not delete cache entry for key=%s, filename=%s", str, zzd(str));
        }
    }

    private final void removeEntry(String str) {
        zzan zzanVarRemove = this.zzbw.remove(str);
        if (zzanVarRemove != null) {
            this.zzbx -= zzanVarRemove.zzca;
        }
    }

    private static int zza(InputStream inputStream) throws IOException {
        int i = inputStream.read();
        if (i != -1) {
            return i;
        }
        throw new EOFException();
    }

    private static InputStream zza(File file) throws FileNotFoundException {
        return new FileInputStream(file);
    }

    static String zza(zzao zzaoVar) throws IOException {
        return new String(zza(zzaoVar, zzc(zzaoVar)), "UTF-8");
    }

    static void zza(OutputStream outputStream, int i) throws IOException {
        outputStream.write(i & 255);
        outputStream.write((i >> 8) & 255);
        outputStream.write((i >> 16) & 255);
        outputStream.write(i >>> 24);
    }

    static void zza(OutputStream outputStream, long j) throws IOException {
        outputStream.write((byte) j);
        outputStream.write((byte) (j >>> 8));
        outputStream.write((byte) (j >>> 16));
        outputStream.write((byte) (j >>> 24));
        outputStream.write((byte) (j >>> 32));
        outputStream.write((byte) (j >>> 40));
        outputStream.write((byte) (j >>> 48));
        outputStream.write((byte) (j >>> 56));
    }

    static void zza(OutputStream outputStream, String str) throws IOException {
        byte[] bytes = str.getBytes("UTF-8");
        zza(outputStream, bytes.length);
        outputStream.write(bytes, 0, bytes.length);
    }

    private final void zza(String str, zzan zzanVar) {
        if (this.zzbw.containsKey(str)) {
            this.zzbx += zzanVar.zzca - this.zzbw.get(str).zzca;
        } else {
            this.zzbx += zzanVar.zzca;
        }
        this.zzbw.put(str, zzanVar);
    }

    private static byte[] zza(zzao zzaoVar, long j) throws IOException {
        long jZzn = zzaoVar.zzn();
        if (j >= 0 && j <= jZzn) {
            int i = (int) j;
            if (i == j) {
                byte[] bArr = new byte[i];
                new DataInputStream(zzaoVar).readFully(bArr);
                return bArr;
            }
        }
        StringBuilder sb = new StringBuilder(73);
        sb.append("streamToBytes length=");
        sb.append(j);
        sb.append(", maxLength=");
        sb.append(jZzn);
        throw new IOException(sb.toString());
    }

    static int zzb(InputStream inputStream) throws IOException {
        return (zza(inputStream) << 24) | zza(inputStream) | 0 | (zza(inputStream) << 8) | (zza(inputStream) << 16);
    }

    static List<zzl> zzb(zzao zzaoVar) throws IOException {
        int iZzb = zzb((InputStream) zzaoVar);
        List<zzl> listEmptyList = iZzb == 0 ? Collections.emptyList() : new ArrayList<>(iZzb);
        for (int i = 0; i < iZzb; i++) {
            listEmptyList.add(new zzl(zza(zzaoVar).intern(), zza(zzaoVar).intern()));
        }
        return listEmptyList;
    }

    static long zzc(InputStream inputStream) throws IOException {
        return (((long) zza(inputStream)) & 255) | 0 | ((((long) zza(inputStream)) & 255) << 8) | ((((long) zza(inputStream)) & 255) << 16) | ((((long) zza(inputStream)) & 255) << 24) | ((((long) zza(inputStream)) & 255) << 32) | ((((long) zza(inputStream)) & 255) << 40) | ((((long) zza(inputStream)) & 255) << 48) | ((255 & ((long) zza(inputStream))) << 56);
    }

    private static String zzd(String str) {
        int length = str.length() / 2;
        String strValueOf = String.valueOf(String.valueOf(str.substring(0, length).hashCode()));
        String strValueOf2 = String.valueOf(String.valueOf(str.substring(length).hashCode()));
        return strValueOf2.length() != 0 ? strValueOf.concat(strValueOf2) : new String(strValueOf);
    }

    private final File zze(String str) {
        return new File(this.zzby, zzd(str));
    }

    @Override // com.google.android.gms.internal.zzb
    public final synchronized void initialize() {
        long length;
        zzao zzaoVar;
        if (!this.zzby.exists()) {
            if (!this.zzby.mkdirs()) {
                zzaf.zzc("Unable to create cache dir %s", this.zzby.getAbsolutePath());
            }
            return;
        }
        File[] fileArrListFiles = this.zzby.listFiles();
        if (fileArrListFiles == null) {
            return;
        }
        for (File file : fileArrListFiles) {
            try {
                length = file.length();
                zzaoVar = new zzao(new BufferedInputStream(zza(file)), length);
            } catch (IOException unused) {
                file.delete();
            }
            try {
                zzan zzanVarZzc = zzan.zzc(zzaoVar);
                zzanVarZzc.zzca = length;
                zza(zzanVarZzc.key, zzanVarZzc);
                zzaoVar.close();
            } catch (Throwable th) {
                zzaoVar.close();
                throw th;
            }
        }
    }

    @Override // com.google.android.gms.internal.zzb
    public final synchronized zzc zza(String str) {
        zzan zzanVar = this.zzbw.get(str);
        if (zzanVar == null) {
            return null;
        }
        File fileZze = zze(str);
        try {
            zzao zzaoVar = new zzao(new BufferedInputStream(zza(fileZze)), fileZze.length());
            try {
                zzan zzanVarZzc = zzan.zzc(zzaoVar);
                if (!TextUtils.equals(str, zzanVarZzc.key)) {
                    zzaf.zzb("%s: key=%s, found=%s", fileZze.getAbsolutePath(), str, zzanVarZzc.key);
                    removeEntry(str);
                    return null;
                }
                byte[] bArrZza = zza(zzaoVar, zzaoVar.zzn());
                zzc zzcVar = new zzc();
                zzcVar.data = bArrZza;
                zzcVar.zza = zzanVar.zza;
                zzcVar.zzb = zzanVar.zzb;
                zzcVar.zzc = zzanVar.zzc;
                zzcVar.zzd = zzanVar.zzd;
                zzcVar.zze = zzanVar.zze;
                zzcVar.zzf = zzap.zza(zzanVar.zzg);
                zzcVar.zzg = Collections.unmodifiableList(zzanVar.zzg);
                return zzcVar;
            } finally {
                zzaoVar.close();
            }
        } catch (IOException e) {
            zzaf.zzb("%s: %s", fileZze.getAbsolutePath(), e.toString());
            remove(str);
            return null;
        }
    }

    @Override // com.google.android.gms.internal.zzb
    public final synchronized void zza(String str, zzc zzcVar) {
        if (this.zzbx + zzcVar.data.length >= this.zzbz) {
            if (zzaf.DEBUG) {
                zzaf.zza("Pruning old cache entries.", new Object[0]);
            }
            long j = this.zzbx;
            long jElapsedRealtime = SystemClock.elapsedRealtime();
            Iterator<Map.Entry<String, zzan>> it = this.zzbw.entrySet().iterator();
            int i = 0;
            while (it.hasNext()) {
                zzan value = it.next().getValue();
                if (zze(value.key).delete()) {
                    this.zzbx -= value.zzca;
                } else {
                    zzaf.zzb("Could not delete cache entry for key=%s, filename=%s", value.key, zzd(value.key));
                }
                it.remove();
                i++;
                if (this.zzbx + r6 < this.zzbz * 0.9f) {
                    break;
                }
            }
            if (zzaf.DEBUG) {
                zzaf.zza("pruned %d files, %d bytes, %d ms", Integer.valueOf(i), Long.valueOf(this.zzbx - j), Long.valueOf(SystemClock.elapsedRealtime() - jElapsedRealtime));
            }
        }
        File fileZze = zze(str);
        try {
            BufferedOutputStream bufferedOutputStream = new BufferedOutputStream(new FileOutputStream(fileZze));
            zzan zzanVar = new zzan(str, zzcVar);
            if (!zzanVar.zza(bufferedOutputStream)) {
                bufferedOutputStream.close();
                zzaf.zzb("Failed to write header for %s", fileZze.getAbsolutePath());
                throw new IOException();
            }
            bufferedOutputStream.write(zzcVar.data);
            bufferedOutputStream.close();
            zza(str, zzanVar);
        } catch (IOException unused) {
            if (fileZze.delete()) {
                return;
            }
            zzaf.zzb("Could not clean up file %s", fileZze.getAbsolutePath());
        }
    }
}
