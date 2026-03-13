package com.google.android.gms.internal;

import java.lang.reflect.Field;
import java.nio.Buffer;
import java.nio.ByteOrder;
import java.security.AccessController;
import java.util.logging.Level;
import java.util.logging.Logger;
import sun.misc.Unsafe;

/* JADX INFO: loaded from: classes.dex */
final class zzfkq {
    private static final boolean zzpop;
    private static final zzd zzptf;
    private static final boolean zzptg;
    private static final long zzpth;
    private static final long zzpti;
    private static final long zzptj;
    private static final long zzptk;
    private static final long zzptl;
    private static final long zzptm;
    private static final long zzptn;
    private static final long zzpto;
    private static final long zzptp;
    private static final long zzptq;
    private static final long zzptr;
    private static final long zzpts;
    private static final long zzptt;
    private static final long zzptu;
    private static final boolean zzptv;
    private static final Logger logger = Logger.getLogger(zzfkq.class.getName());
    private static final Unsafe zzmdw = zzdcf();
    private static final Class<?> zzpnt = zzfgo.zzcxn();
    private static final boolean zzptd = zzp(Long.TYPE);
    private static final boolean zzpte = zzp(Integer.TYPE);

    static final class zza extends zzd {
        zza(Unsafe unsafe) {
            super(unsafe);
        }

        @Override // com.google.android.gms.internal.zzfkq.zzd
        public final void zze(Object obj, long j, byte b) {
            if (zzfkq.zzptv) {
                zzfkq.zza(obj, j, b);
            } else {
                zzfkq.zzb(obj, j, b);
            }
        }

        @Override // com.google.android.gms.internal.zzfkq.zzd
        public final byte zzf(Object obj, long j) {
            return zzfkq.zzptv ? zzfkq.zzb(obj, j) : zzfkq.zzc(obj, j);
        }
    }

    static final class zzb extends zzd {
        zzb(Unsafe unsafe) {
            super(unsafe);
        }

        @Override // com.google.android.gms.internal.zzfkq.zzd
        public final void zze(Object obj, long j, byte b) {
            if (zzfkq.zzptv) {
                zzfkq.zza(obj, j, b);
            } else {
                zzfkq.zzb(obj, j, b);
            }
        }

        @Override // com.google.android.gms.internal.zzfkq.zzd
        public final byte zzf(Object obj, long j) {
            return zzfkq.zzptv ? zzfkq.zzb(obj, j) : zzfkq.zzc(obj, j);
        }
    }

    static final class zzc extends zzd {
        zzc(Unsafe unsafe) {
            super(unsafe);
        }

        @Override // com.google.android.gms.internal.zzfkq.zzd
        public final void zze(Object obj, long j, byte b) {
            this.zzptw.putByte(obj, j, b);
        }

        @Override // com.google.android.gms.internal.zzfkq.zzd
        public final byte zzf(Object obj, long j) {
            return this.zzptw.getByte(obj, j);
        }
    }

    static abstract class zzd {
        Unsafe zzptw;

        zzd(Unsafe unsafe) {
            this.zzptw = unsafe;
        }

        public final int zza(Object obj, long j) {
            return this.zzptw.getInt(obj, j);
        }

        public abstract void zze(Object obj, long j, byte b);

        public abstract byte zzf(Object obj, long j);
    }

    static {
        Field fieldZza;
        zzd zzcVar = null;
        if (zzmdw != null) {
            if (!zzfgo.zzcxm()) {
                zzcVar = new zzc(zzmdw);
            } else if (zzptd) {
                zzcVar = new zzb(zzmdw);
            } else if (zzpte) {
                zzcVar = new zza(zzmdw);
            }
        }
        zzptf = zzcVar;
        zzptg = zzdch();
        zzpop = zzdcg();
        zzpth = zzn(byte[].class);
        zzpti = zzn(boolean[].class);
        zzptj = zzo(boolean[].class);
        zzptk = zzn(int[].class);
        zzptl = zzo(int[].class);
        zzptm = zzn(long[].class);
        zzptn = zzo(long[].class);
        zzpto = zzn(float[].class);
        zzptp = zzo(float[].class);
        zzptq = zzn(double[].class);
        zzptr = zzo(double[].class);
        zzpts = zzn(Object[].class);
        zzptt = zzo(Object[].class);
        if (!zzfgo.zzcxm() || (fieldZza = zza((Class<?>) Buffer.class, "effectiveDirectAddress")) == null) {
            fieldZza = zza((Class<?>) Buffer.class, "address");
        }
        zzptu = (fieldZza == null || zzptf == null) ? -1L : zzptf.zzptw.objectFieldOffset(fieldZza);
        zzptv = ByteOrder.nativeOrder() == ByteOrder.BIG_ENDIAN;
    }

    private zzfkq() {
    }

    static int zza(Object obj, long j) {
        return zzptf.zza(obj, j);
    }

    private static Field zza(Class<?> cls, String str) {
        try {
            Field declaredField = cls.getDeclaredField(str);
            declaredField.setAccessible(true);
            return declaredField;
        } catch (Throwable unused) {
            return null;
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static void zza(Object obj, long j, byte b) {
        long j2 = (-4) & j;
        int iZza = zza(obj, j2);
        int i = ((~((int) j)) & 3) << 3;
        zza(obj, j2, ((255 & b) << i) | (iZza & (~(255 << i))));
    }

    private static void zza(Object obj, long j, int i) {
        zzptf.zzptw.putInt(obj, j, i);
    }

    static void zza(byte[] bArr, long j, byte b) {
        zzptf.zze(bArr, zzpth + j, b);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static byte zzb(Object obj, long j) {
        return (byte) (zza(obj, (-4) & j) >>> ((int) (((~j) & 3) << 3)));
    }

    static byte zzb(byte[] bArr, long j) {
        return zzptf.zzf(bArr, zzpth + j);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static void zzb(Object obj, long j, byte b) {
        long j2 = (-4) & j;
        int i = (((int) j) & 3) << 3;
        zza(obj, j2, ((255 & b) << i) | (zza(obj, j2) & (~(255 << i))));
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static byte zzc(Object obj, long j) {
        return (byte) (zza(obj, (-4) & j) >>> ((int) ((j & 3) << 3)));
    }

    static boolean zzdcd() {
        return zzpop;
    }

    static boolean zzdce() {
        return zzptg;
    }

    private static Unsafe zzdcf() {
        try {
            return (Unsafe) AccessController.doPrivileged(new zzfkr());
        } catch (Throwable unused) {
            return null;
        }
    }

    private static boolean zzdcg() {
        if (zzmdw == null) {
            return false;
        }
        try {
            Class<?> cls = zzmdw.getClass();
            cls.getMethod("objectFieldOffset", Field.class);
            cls.getMethod("arrayBaseOffset", Class.class);
            cls.getMethod("arrayIndexScale", Class.class);
            cls.getMethod("getInt", Object.class, Long.TYPE);
            cls.getMethod("putInt", Object.class, Long.TYPE, Integer.TYPE);
            cls.getMethod("getLong", Object.class, Long.TYPE);
            cls.getMethod("putLong", Object.class, Long.TYPE, Long.TYPE);
            cls.getMethod("getObject", Object.class, Long.TYPE);
            cls.getMethod("putObject", Object.class, Long.TYPE, Object.class);
            if (zzfgo.zzcxm()) {
                return true;
            }
            cls.getMethod("getByte", Object.class, Long.TYPE);
            cls.getMethod("putByte", Object.class, Long.TYPE, Byte.TYPE);
            cls.getMethod("getBoolean", Object.class, Long.TYPE);
            cls.getMethod("putBoolean", Object.class, Long.TYPE, Boolean.TYPE);
            cls.getMethod("getFloat", Object.class, Long.TYPE);
            cls.getMethod("putFloat", Object.class, Long.TYPE, Float.TYPE);
            cls.getMethod("getDouble", Object.class, Long.TYPE);
            cls.getMethod("putDouble", Object.class, Long.TYPE, Double.TYPE);
            return true;
        } catch (Throwable th) {
            Logger logger2 = logger;
            Level level = Level.WARNING;
            String strValueOf = String.valueOf(th);
            StringBuilder sb = new StringBuilder(String.valueOf(strValueOf).length() + 71);
            sb.append("platform method missing - proto runtime falling back to safer methods: ");
            sb.append(strValueOf);
            logger2.logp(level, "com.google.protobuf.UnsafeUtil", "supportsUnsafeArrayOperations", sb.toString());
            return false;
        }
    }

    private static boolean zzdch() {
        if (zzmdw == null) {
            return false;
        }
        try {
            Class<?> cls = zzmdw.getClass();
            cls.getMethod("objectFieldOffset", Field.class);
            cls.getMethod("getLong", Object.class, Long.TYPE);
            if (zzfgo.zzcxm()) {
                return true;
            }
            cls.getMethod("getByte", Long.TYPE);
            cls.getMethod("putByte", Long.TYPE, Byte.TYPE);
            cls.getMethod("getInt", Long.TYPE);
            cls.getMethod("putInt", Long.TYPE, Integer.TYPE);
            cls.getMethod("getLong", Long.TYPE);
            cls.getMethod("putLong", Long.TYPE, Long.TYPE);
            cls.getMethod("copyMemory", Long.TYPE, Long.TYPE, Long.TYPE);
            cls.getMethod("copyMemory", Object.class, Long.TYPE, Object.class, Long.TYPE, Long.TYPE);
            return true;
        } catch (Throwable th) {
            Logger logger2 = logger;
            Level level = Level.WARNING;
            String strValueOf = String.valueOf(th);
            StringBuilder sb = new StringBuilder(String.valueOf(strValueOf).length() + 71);
            sb.append("platform method missing - proto runtime falling back to safer methods: ");
            sb.append(strValueOf);
            logger2.logp(level, "com.google.protobuf.UnsafeUtil", "supportsUnsafeByteBufferOperations", sb.toString());
            return false;
        }
    }

    private static int zzn(Class<?> cls) {
        if (zzpop) {
            return zzptf.zzptw.arrayBaseOffset(cls);
        }
        return -1;
    }

    private static int zzo(Class<?> cls) {
        if (zzpop) {
            return zzptf.zzptw.arrayIndexScale(cls);
        }
        return -1;
    }

    private static boolean zzp(Class<?> cls) {
        if (!zzfgo.zzcxm()) {
            return false;
        }
        try {
            Class<?> cls2 = zzpnt;
            cls2.getMethod("peekLong", cls, Boolean.TYPE);
            cls2.getMethod("pokeLong", cls, Long.TYPE, Boolean.TYPE);
            cls2.getMethod("pokeInt", cls, Integer.TYPE, Boolean.TYPE);
            cls2.getMethod("peekInt", cls, Boolean.TYPE);
            cls2.getMethod("pokeByte", cls, Byte.TYPE);
            cls2.getMethod("peekByte", cls);
            cls2.getMethod("pokeByteArray", cls, byte[].class, Integer.TYPE, Integer.TYPE);
            cls2.getMethod("peekByteArray", cls, byte[].class, Integer.TYPE, Integer.TYPE);
            return true;
        } catch (Throwable unused) {
            return false;
        }
    }
}
