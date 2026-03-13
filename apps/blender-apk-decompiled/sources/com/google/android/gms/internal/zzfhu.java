package com.google.android.gms.internal;

import com.google.android.gms.internal.zzfhu;
import com.google.android.gms.internal.zzfhu.zza;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/* JADX INFO: loaded from: classes.dex */
public abstract class zzfhu<MessageType extends zzfhu<MessageType, BuilderType>, BuilderType extends zza<MessageType, BuilderType>> extends zzfgj<MessageType, BuilderType> {
    private static Map<Object, zzfhu<?, ?>> zzppj = new ConcurrentHashMap();
    protected zzfko zzpph = zzfko.zzdca();
    protected int zzppi = -1;

    public static abstract class zza<MessageType extends zzfhu<MessageType, BuilderType>, BuilderType extends zza<MessageType, BuilderType>> extends zzfgk<MessageType, BuilderType> {
        private final MessageType zzppk;
        protected MessageType zzppl;
        private boolean zzppm = false;

        protected zza(MessageType messagetype) {
            this.zzppk = messagetype;
            this.zzppl = (MessageType) messagetype.zza(zzg.zzppx, null, null);
        }

        private static void zza(MessageType messagetype, MessageType messagetype2) {
            zzf zzfVar = zzf.zzppq;
            messagetype.zza(zzg.zzpps, zzfVar, messagetype2);
            messagetype.zzpph = zzfVar.zza(messagetype.zzpph, messagetype2.zzpph);
        }

        /* JADX INFO: Access modifiers changed from: private */
        @Override // com.google.android.gms.internal.zzfgk, com.google.android.gms.internal.zzfjd
        /* JADX INFO: renamed from: zzd, reason: merged with bridge method [inline-methods] */
        public final BuilderType zzb(zzfhb zzfhbVar, zzfhm zzfhmVar) throws IOException {
            zzczv();
            try {
                this.zzppl.zza(zzg.zzppv, zzfhbVar, zzfhmVar);
                return this;
            } catch (RuntimeException e) {
                if (e.getCause() instanceof IOException) {
                    throw ((IOException) e.getCause());
                }
                throw e;
            }
        }

        @Override // com.google.android.gms.internal.zzfgk
        public /* synthetic */ Object clone() throws CloneNotSupportedException {
            zza zzaVar = (zza) this.zzppk.zza(zzg.zzppy, null, null);
            if (!this.zzppm) {
                MessageType messagetype = this.zzppl;
                messagetype.zza(zzg.zzppw, null, null);
                messagetype.zzpph.zzbkr();
                this.zzppm = true;
            }
            zzaVar.zza((zzfhu) this.zzppl);
            return zzaVar;
        }

        @Override // com.google.android.gms.internal.zzfje
        public final boolean isInitialized() {
            return zzfhu.zza(this.zzppl, false);
        }

        @Override // com.google.android.gms.internal.zzfgk
        /* JADX INFO: renamed from: zza */
        public final /* synthetic */ zzfgk zzb(zzfhb zzfhbVar, zzfhm zzfhmVar) throws IOException {
            return (zza) zzb(zzfhbVar, zzfhmVar);
        }

        @Override // com.google.android.gms.internal.zzfgk
        public final BuilderType zza(MessageType messagetype) {
            zzczv();
            zza(this.zzppl, messagetype);
            return this;
        }

        @Override // com.google.android.gms.internal.zzfgk
        /* JADX INFO: renamed from: zzcxj */
        public final /* synthetic */ zzfgk clone() {
            return (zza) clone();
        }

        @Override // com.google.android.gms.internal.zzfje
        public final /* synthetic */ zzfjc zzczu() {
            return this.zzppk;
        }

        protected final void zzczv() {
            if (this.zzppm) {
                MessageType messagetype = (MessageType) this.zzppl.zza(zzg.zzppx, null, null);
                zza(messagetype, this.zzppl);
                this.zzppl = messagetype;
                this.zzppm = false;
            }
        }

        public final MessageType zzczw() {
            if (this.zzppm) {
                return this.zzppl;
            }
            MessageType messagetype = this.zzppl;
            messagetype.zza(zzg.zzppw, null, null);
            messagetype.zzpph.zzbkr();
            this.zzppm = true;
            return this.zzppl;
        }

        public final MessageType zzczx() {
            if (!this.zzppm) {
                MessageType messagetype = this.zzppl;
                messagetype.zza(zzg.zzppw, null, null);
                messagetype.zzpph.zzbkr();
                this.zzppm = true;
            }
            MessageType messagetype2 = this.zzppl;
            boolean zBooleanValue = Boolean.TRUE.booleanValue();
            byte bByteValue = ((Byte) messagetype2.zza(zzg.zzppt, null, null)).byteValue();
            if (bByteValue != 1) {
                if (bByteValue == 0) {
                    z = false;
                } else {
                    z = messagetype2.zza(zzg.zzppr, Boolean.FALSE, null) != null;
                    if (zBooleanValue) {
                        messagetype2.zza(zzg.zzppu, z ? messagetype2 : null, null);
                    }
                }
            }
            if (z) {
                return messagetype2;
            }
            throw new zzfkm(messagetype2);
        }

        @Override // com.google.android.gms.internal.zzfjd
        public final /* synthetic */ zzfjc zzczy() {
            if (this.zzppm) {
                return this.zzppl;
            }
            MessageType messagetype = this.zzppl;
            messagetype.zza(zzg.zzppw, null, null);
            messagetype.zzpph.zzbkr();
            this.zzppm = true;
            return this.zzppl;
        }

        @Override // com.google.android.gms.internal.zzfjd
        public final /* synthetic */ zzfjc zzczz() {
            if (!this.zzppm) {
                MessageType messagetype = this.zzppl;
                messagetype.zza(zzg.zzppw, null, null);
                messagetype.zzpph.zzbkr();
                this.zzppm = true;
            }
            MessageType messagetype2 = this.zzppl;
            boolean zBooleanValue = Boolean.TRUE.booleanValue();
            byte bByteValue = ((Byte) messagetype2.zza(zzg.zzppt, null, null)).byteValue();
            if (bByteValue != 1) {
                if (bByteValue == 0) {
                    z = false;
                } else {
                    z = messagetype2.zza(zzg.zzppr, Boolean.FALSE, null) != null;
                    if (zBooleanValue) {
                        messagetype2.zza(zzg.zzppu, z ? messagetype2 : null, null);
                    }
                }
            }
            if (z) {
                return messagetype2;
            }
            throw new zzfkm(messagetype2);
        }
    }

    public static class zzb<T extends zzfhu<T, ?>> extends zzfgm<T> {
        private T zzppk;

        public zzb(T t) {
            this.zzppk = t;
        }

        @Override // com.google.android.gms.internal.zzfjl
        public final /* synthetic */ Object zze(zzfhb zzfhbVar, zzfhm zzfhmVar) throws zzfie {
            return zzfhu.zza(this.zzppk, zzfhbVar, zzfhmVar);
        }
    }

    static class zzc implements zzh {
        static final zzc zzppn = new zzc();
        private static zzfhv zzppo = new zzfhv();

        private zzc() {
        }

        @Override // com.google.android.gms.internal.zzfhu.zzh
        public final double zza(boolean z, double d, boolean z2, double d2) {
            if (z == z2 && d == d2) {
                return d;
            }
            throw zzppo;
        }

        @Override // com.google.android.gms.internal.zzfhu.zzh
        public final int zza(boolean z, int i, boolean z2, int i2) {
            if (z == z2 && i == i2) {
                return i;
            }
            throw zzppo;
        }

        @Override // com.google.android.gms.internal.zzfhu.zzh
        public final long zza(boolean z, long j, boolean z2, long j2) {
            if (z == z2 && j == j2) {
                return j;
            }
            throw zzppo;
        }

        @Override // com.google.android.gms.internal.zzfhu.zzh
        public final zzfgs zza(boolean z, zzfgs zzfgsVar, boolean z2, zzfgs zzfgsVar2) {
            if (z == z2 && zzfgsVar.equals(zzfgsVar2)) {
                return zzfgsVar;
            }
            throw zzppo;
        }

        @Override // com.google.android.gms.internal.zzfhu.zzh
        public final zzfic zza(zzfic zzficVar, zzfic zzficVar2) {
            if (zzficVar.equals(zzficVar2)) {
                return zzficVar;
            }
            throw zzppo;
        }

        @Override // com.google.android.gms.internal.zzfhu.zzh
        public final <T> zzfid<T> zza(zzfid<T> zzfidVar, zzfid<T> zzfidVar2) {
            if (zzfidVar.equals(zzfidVar2)) {
                return zzfidVar;
            }
            throw zzppo;
        }

        @Override // com.google.android.gms.internal.zzfhu.zzh
        public final <K, V> zzfiw<K, V> zza(zzfiw<K, V> zzfiwVar, zzfiw<K, V> zzfiwVar2) {
            if (zzfiwVar.equals(zzfiwVar2)) {
                return zzfiwVar;
            }
            throw zzppo;
        }

        @Override // com.google.android.gms.internal.zzfhu.zzh
        public final <T extends zzfjc> T zza(T t, T t2) {
            if (t == null && t2 == null) {
                return null;
            }
            if (t == null || t2 == null) {
                throw zzppo;
            }
            zzfhu zzfhuVar = (zzfhu) t;
            if (zzfhuVar != t2 && ((zzfhu) zzfhuVar.zza(zzg.zzppz, (Object) null, (Object) null)).getClass().isInstance(t2)) {
                zzfhu zzfhuVar2 = (zzfhu) t2;
                zzfhuVar.zza(zzg.zzpps, this, zzfhuVar2);
                zzfhuVar.zzpph = zza(zzfhuVar.zzpph, zzfhuVar2.zzpph);
            }
            return t;
        }

        @Override // com.google.android.gms.internal.zzfhu.zzh
        public final zzfko zza(zzfko zzfkoVar, zzfko zzfkoVar2) {
            if (zzfkoVar.equals(zzfkoVar2)) {
                return zzfkoVar;
            }
            throw zzppo;
        }

        @Override // com.google.android.gms.internal.zzfhu.zzh
        public final Object zza(boolean z, Object obj, Object obj2) {
            if (z && obj.equals(obj2)) {
                return obj;
            }
            throw zzppo;
        }

        @Override // com.google.android.gms.internal.zzfhu.zzh
        public final String zza(boolean z, String str, boolean z2, String str2) {
            if (z == z2 && str.equals(str2)) {
                return str;
            }
            throw zzppo;
        }

        @Override // com.google.android.gms.internal.zzfhu.zzh
        public final boolean zza(boolean z, boolean z2, boolean z3, boolean z4) {
            if (z == z3 && z2 == z4) {
                return z2;
            }
            throw zzppo;
        }

        @Override // com.google.android.gms.internal.zzfhu.zzh
        public final Object zzb(boolean z, Object obj, Object obj2) {
            if (z && obj.equals(obj2)) {
                return obj;
            }
            throw zzppo;
        }

        @Override // com.google.android.gms.internal.zzfhu.zzh
        public final Object zzc(boolean z, Object obj, Object obj2) {
            if (z && obj.equals(obj2)) {
                return obj;
            }
            throw zzppo;
        }

        @Override // com.google.android.gms.internal.zzfhu.zzh
        public final Object zzd(boolean z, Object obj, Object obj2) {
            if (z && obj.equals(obj2)) {
                return obj;
            }
            throw zzppo;
        }

        @Override // com.google.android.gms.internal.zzfhu.zzh
        public final void zzdn(boolean z) {
            if (z) {
                throw zzppo;
            }
        }

        @Override // com.google.android.gms.internal.zzfhu.zzh
        public final Object zze(boolean z, Object obj, Object obj2) {
            if (z && obj.equals(obj2)) {
                return obj;
            }
            throw zzppo;
        }

        @Override // com.google.android.gms.internal.zzfhu.zzh
        public final Object zzf(boolean z, Object obj, Object obj2) {
            if (z && obj.equals(obj2)) {
                return obj;
            }
            throw zzppo;
        }

        @Override // com.google.android.gms.internal.zzfhu.zzh
        public final Object zzg(boolean z, Object obj, Object obj2) {
            if (z) {
                zzfhu zzfhuVar = (zzfhu) obj;
                zzfjc zzfjcVar = (zzfjc) obj2;
                boolean z2 = true;
                if (zzfhuVar != zzfjcVar) {
                    if (((zzfhu) zzfhuVar.zza(zzg.zzppz, (Object) null, (Object) null)).getClass().isInstance(zzfjcVar)) {
                        zzfhu zzfhuVar2 = (zzfhu) zzfjcVar;
                        zzfhuVar.zza(zzg.zzpps, this, zzfhuVar2);
                        zzfhuVar.zzpph = zza(zzfhuVar.zzpph, zzfhuVar2.zzpph);
                    } else {
                        z2 = false;
                    }
                }
                if (z2) {
                    return obj;
                }
            }
            throw zzppo;
        }
    }

    public static abstract class zzd<MessageType extends zzd<MessageType, BuilderType>, BuilderType> extends zzfhu<MessageType, BuilderType> implements zzfje {
        protected zzfhq<Object> zzppp = zzfhq.zzczj();
    }

    static class zze implements zzh {
        int zzmci = 0;

        zze() {
        }

        @Override // com.google.android.gms.internal.zzfhu.zzh
        public final double zza(boolean z, double d, boolean z2, double d2) {
            this.zzmci = (this.zzmci * 53) + zzfhz.zzdf(Double.doubleToLongBits(d));
            return d;
        }

        @Override // com.google.android.gms.internal.zzfhu.zzh
        public final int zza(boolean z, int i, boolean z2, int i2) {
            this.zzmci = (this.zzmci * 53) + i;
            return i;
        }

        @Override // com.google.android.gms.internal.zzfhu.zzh
        public final long zza(boolean z, long j, boolean z2, long j2) {
            this.zzmci = (this.zzmci * 53) + zzfhz.zzdf(j);
            return j;
        }

        @Override // com.google.android.gms.internal.zzfhu.zzh
        public final zzfgs zza(boolean z, zzfgs zzfgsVar, boolean z2, zzfgs zzfgsVar2) {
            this.zzmci = (this.zzmci * 53) + zzfgsVar.hashCode();
            return zzfgsVar;
        }

        @Override // com.google.android.gms.internal.zzfhu.zzh
        public final zzfic zza(zzfic zzficVar, zzfic zzficVar2) {
            this.zzmci = (this.zzmci * 53) + zzficVar.hashCode();
            return zzficVar;
        }

        @Override // com.google.android.gms.internal.zzfhu.zzh
        public final <T> zzfid<T> zza(zzfid<T> zzfidVar, zzfid<T> zzfidVar2) {
            this.zzmci = (this.zzmci * 53) + zzfidVar.hashCode();
            return zzfidVar;
        }

        @Override // com.google.android.gms.internal.zzfhu.zzh
        public final <K, V> zzfiw<K, V> zza(zzfiw<K, V> zzfiwVar, zzfiw<K, V> zzfiwVar2) {
            this.zzmci = (this.zzmci * 53) + zzfiwVar.hashCode();
            return zzfiwVar;
        }

        @Override // com.google.android.gms.internal.zzfhu.zzh
        public final <T extends zzfjc> T zza(T t, T t2) {
            int iHashCode;
            if (t == null) {
                iHashCode = 37;
            } else if (t instanceof zzfhu) {
                zzfhu zzfhuVar = (zzfhu) t;
                if (zzfhuVar.zzpno == 0) {
                    int i = this.zzmci;
                    this.zzmci = 0;
                    zzfhuVar.zza(zzg.zzpps, this, zzfhuVar);
                    zzfhuVar.zzpph = zza(zzfhuVar.zzpph, zzfhuVar.zzpph);
                    zzfhuVar.zzpno = this.zzmci;
                    this.zzmci = i;
                }
                iHashCode = zzfhuVar.zzpno;
            } else {
                iHashCode = t.hashCode();
            }
            this.zzmci = (this.zzmci * 53) + iHashCode;
            return t;
        }

        @Override // com.google.android.gms.internal.zzfhu.zzh
        public final zzfko zza(zzfko zzfkoVar, zzfko zzfkoVar2) {
            this.zzmci = (this.zzmci * 53) + zzfkoVar.hashCode();
            return zzfkoVar;
        }

        @Override // com.google.android.gms.internal.zzfhu.zzh
        public final Object zza(boolean z, Object obj, Object obj2) {
            this.zzmci = (this.zzmci * 53) + zzfhz.zzdo(((Boolean) obj).booleanValue());
            return obj;
        }

        @Override // com.google.android.gms.internal.zzfhu.zzh
        public final String zza(boolean z, String str, boolean z2, String str2) {
            this.zzmci = (this.zzmci * 53) + str.hashCode();
            return str;
        }

        @Override // com.google.android.gms.internal.zzfhu.zzh
        public final boolean zza(boolean z, boolean z2, boolean z3, boolean z4) {
            this.zzmci = (this.zzmci * 53) + zzfhz.zzdo(z2);
            return z2;
        }

        @Override // com.google.android.gms.internal.zzfhu.zzh
        public final Object zzb(boolean z, Object obj, Object obj2) {
            this.zzmci = (this.zzmci * 53) + ((Integer) obj).intValue();
            return obj;
        }

        @Override // com.google.android.gms.internal.zzfhu.zzh
        public final Object zzc(boolean z, Object obj, Object obj2) {
            this.zzmci = (this.zzmci * 53) + zzfhz.zzdf(Double.doubleToLongBits(((Double) obj).doubleValue()));
            return obj;
        }

        @Override // com.google.android.gms.internal.zzfhu.zzh
        public final Object zzd(boolean z, Object obj, Object obj2) {
            this.zzmci = (this.zzmci * 53) + zzfhz.zzdf(((Long) obj).longValue());
            return obj;
        }

        @Override // com.google.android.gms.internal.zzfhu.zzh
        public final void zzdn(boolean z) {
            if (z) {
                throw new IllegalStateException();
            }
        }

        @Override // com.google.android.gms.internal.zzfhu.zzh
        public final Object zze(boolean z, Object obj, Object obj2) {
            this.zzmci = (this.zzmci * 53) + obj.hashCode();
            return obj;
        }

        @Override // com.google.android.gms.internal.zzfhu.zzh
        public final Object zzf(boolean z, Object obj, Object obj2) {
            this.zzmci = (this.zzmci * 53) + obj.hashCode();
            return obj;
        }

        @Override // com.google.android.gms.internal.zzfhu.zzh
        public final Object zzg(boolean z, Object obj, Object obj2) {
            return zza((zzfjc) obj, (zzfjc) obj2);
        }
    }

    public static class zzf implements zzh {
        public static final zzf zzppq = new zzf();

        private zzf() {
        }

        @Override // com.google.android.gms.internal.zzfhu.zzh
        public final double zza(boolean z, double d, boolean z2, double d2) {
            return z2 ? d2 : d;
        }

        @Override // com.google.android.gms.internal.zzfhu.zzh
        public final int zza(boolean z, int i, boolean z2, int i2) {
            return z2 ? i2 : i;
        }

        @Override // com.google.android.gms.internal.zzfhu.zzh
        public final long zza(boolean z, long j, boolean z2, long j2) {
            return z2 ? j2 : j;
        }

        @Override // com.google.android.gms.internal.zzfhu.zzh
        public final zzfgs zza(boolean z, zzfgs zzfgsVar, boolean z2, zzfgs zzfgsVar2) {
            return z2 ? zzfgsVar2 : zzfgsVar;
        }

        @Override // com.google.android.gms.internal.zzfhu.zzh
        public final zzfic zza(zzfic zzficVar, zzfic zzficVar2) {
            int size = zzficVar.size();
            int size2 = zzficVar2.size();
            if (size > 0 && size2 > 0) {
                if (!zzficVar.zzcxk()) {
                    zzficVar = zzficVar.zzmo(size2 + size);
                }
                zzficVar.addAll(zzficVar2);
            }
            return size > 0 ? zzficVar : zzficVar2;
        }

        @Override // com.google.android.gms.internal.zzfhu.zzh
        public final <T> zzfid<T> zza(zzfid<T> zzfidVar, zzfid<T> zzfidVar2) {
            int size = zzfidVar.size();
            int size2 = zzfidVar2.size();
            if (size > 0 && size2 > 0) {
                if (!zzfidVar.zzcxk()) {
                    zzfidVar = zzfidVar.zzmo(size2 + size);
                }
                zzfidVar.addAll(zzfidVar2);
            }
            return size > 0 ? zzfidVar : zzfidVar2;
        }

        @Override // com.google.android.gms.internal.zzfhu.zzh
        public final <K, V> zzfiw<K, V> zza(zzfiw<K, V> zzfiwVar, zzfiw<K, V> zzfiwVar2) {
            if (!zzfiwVar2.isEmpty()) {
                if (!zzfiwVar.isMutable()) {
                    zzfiwVar = zzfiwVar.zzdau();
                }
                zzfiwVar.zza(zzfiwVar2);
            }
            return zzfiwVar;
        }

        @Override // com.google.android.gms.internal.zzfhu.zzh
        public final <T extends zzfjc> T zza(T t, T t2) {
            return (t == null || t2 == null) ? t != null ? t : t2 : (T) t.zzczt().zzd(t2).zzczz();
        }

        @Override // com.google.android.gms.internal.zzfhu.zzh
        public final zzfko zza(zzfko zzfkoVar, zzfko zzfkoVar2) {
            return zzfkoVar2 == zzfko.zzdca() ? zzfkoVar : zzfko.zzb(zzfkoVar, zzfkoVar2);
        }

        @Override // com.google.android.gms.internal.zzfhu.zzh
        public final Object zza(boolean z, Object obj, Object obj2) {
            return obj2;
        }

        @Override // com.google.android.gms.internal.zzfhu.zzh
        public final String zza(boolean z, String str, boolean z2, String str2) {
            return z2 ? str2 : str;
        }

        @Override // com.google.android.gms.internal.zzfhu.zzh
        public final boolean zza(boolean z, boolean z2, boolean z3, boolean z4) {
            return z3 ? z4 : z2;
        }

        @Override // com.google.android.gms.internal.zzfhu.zzh
        public final Object zzb(boolean z, Object obj, Object obj2) {
            return obj2;
        }

        @Override // com.google.android.gms.internal.zzfhu.zzh
        public final Object zzc(boolean z, Object obj, Object obj2) {
            return obj2;
        }

        @Override // com.google.android.gms.internal.zzfhu.zzh
        public final Object zzd(boolean z, Object obj, Object obj2) {
            return obj2;
        }

        @Override // com.google.android.gms.internal.zzfhu.zzh
        public final void zzdn(boolean z) {
        }

        @Override // com.google.android.gms.internal.zzfhu.zzh
        public final Object zze(boolean z, Object obj, Object obj2) {
            return obj2;
        }

        @Override // com.google.android.gms.internal.zzfhu.zzh
        public final Object zzf(boolean z, Object obj, Object obj2) {
            return obj2;
        }

        @Override // com.google.android.gms.internal.zzfhu.zzh
        public final Object zzg(boolean z, Object obj, Object obj2) {
            return z ? zza((zzfjc) obj, (zzfjc) obj2) : obj2;
        }
    }

    public enum zzg {
        public static final int zzppr = 1;
        public static final int zzpps = 2;
        public static final int zzppt = 3;
        public static final int zzppu = 4;
        public static final int zzppv = 5;
        public static final int zzppw = 6;
        public static final int zzppx = 7;
        public static final int zzppy = 8;
        public static final int zzppz = 9;
        public static final int zzpqa = 10;
        private static final /* synthetic */ int[] zzpqb = {zzppr, zzpps, zzppt, zzppu, zzppv, zzppw, zzppx, zzppy, zzppz, zzpqa};
        public static final int zzpqc = 1;
        private static int zzpqd = 2;
        private static final /* synthetic */ int[] zzpqe = {zzpqc, zzpqd};
        public static final int zzpqf = 1;
        public static final int zzpqg = 2;
        private static final /* synthetic */ int[] zzpqh = {zzpqf, zzpqg};

        public static int[] values$50KLMJ33DTMIUPRFDTJMOP9FE1P6UT3FC9QMCBQ7CLN6ASJ1EHIM8JB5EDPM2PR59HKN8P949LIN8Q3FCHA6UIBEEPNMMP9R0() {
            return (int[]) zzpqb.clone();
        }
    }

    public interface zzh {
        double zza(boolean z, double d, boolean z2, double d2);

        int zza(boolean z, int i, boolean z2, int i2);

        long zza(boolean z, long j, boolean z2, long j2);

        zzfgs zza(boolean z, zzfgs zzfgsVar, boolean z2, zzfgs zzfgsVar2);

        zzfic zza(zzfic zzficVar, zzfic zzficVar2);

        <T> zzfid<T> zza(zzfid<T> zzfidVar, zzfid<T> zzfidVar2);

        <K, V> zzfiw<K, V> zza(zzfiw<K, V> zzfiwVar, zzfiw<K, V> zzfiwVar2);

        <T extends zzfjc> T zza(T t, T t2);

        zzfko zza(zzfko zzfkoVar, zzfko zzfkoVar2);

        Object zza(boolean z, Object obj, Object obj2);

        String zza(boolean z, String str, boolean z2, String str2);

        boolean zza(boolean z, boolean z2, boolean z3, boolean z4);

        Object zzb(boolean z, Object obj, Object obj2);

        Object zzc(boolean z, Object obj, Object obj2);

        Object zzd(boolean z, Object obj, Object obj2);

        void zzdn(boolean z);

        Object zze(boolean z, Object obj, Object obj2);

        Object zzf(boolean z, Object obj, Object obj2);

        Object zzg(boolean z, Object obj, Object obj2);
    }

    protected static <T extends zzfhu<T, ?>> T zza(T t, zzfgs zzfgsVar) throws zzfie {
        boolean z;
        zzfmg zzfmgVar = (T) zza(t, zzfgsVar, zzfhm.zzczf());
        if (zzfmgVar != null) {
            boolean zBooleanValue = Boolean.TRUE.booleanValue();
            byte bByteValue = ((Byte) zzfmgVar.zza(zzg.zzppt, (Object) null, (Object) null)).byteValue();
            if (bByteValue == 1) {
                z = true;
            } else if (bByteValue == 0) {
                z = false;
            } else {
                z = zzfmgVar.zza(zzg.zzppr, Boolean.FALSE, (Object) null) != null;
                if (zBooleanValue) {
                    zzfmgVar.zza(zzg.zzppu, z ? zzfmgVar : null, (Object) null);
                }
            }
            if (!z) {
                throw new zzfkm(zzfmgVar).zzdbz().zzi(zzfmgVar);
            }
        }
        if (zzfmgVar != null) {
            boolean zBooleanValue2 = Boolean.TRUE.booleanValue();
            byte bByteValue2 = ((Byte) zzfmgVar.zza(zzg.zzppt, (Object) null, (Object) null)).byteValue();
            if (bByteValue2 == 1) {
                z = true;
            } else if (bByteValue2 != 0) {
                z = zzfmgVar.zza(zzg.zzppr, Boolean.FALSE, (Object) null) != null;
                if (zBooleanValue2) {
                    zzfmgVar.zza(zzg.zzppu, z ? zzfmgVar : null, (Object) null);
                }
            }
            if (!z) {
                throw new zzfkm(zzfmgVar).zzdbz().zzi(zzfmgVar);
            }
        }
        return zzfmgVar;
    }

    private static <T extends zzfhu<T, ?>> T zza(T t, zzfgs zzfgsVar, zzfhm zzfhmVar) throws zzfie {
        try {
            zzfhb zzfhbVarZzcxq = zzfgsVar.zzcxq();
            T t2 = (T) zza(t, zzfhbVarZzcxq, zzfhmVar);
            try {
                zzfhbVarZzcxq.zzlf(0);
                return t2;
            } catch (zzfie e) {
                throw e.zzi(t2);
            }
        } catch (zzfie e2) {
            throw e2;
        }
    }

    static <T extends zzfhu<T, ?>> T zza(T t, zzfhb zzfhbVar, zzfhm zzfhmVar) throws zzfie {
        T t2 = (T) t.zza(zzg.zzppx, null, null);
        try {
            t2.zza(zzg.zzppv, zzfhbVar, zzfhmVar);
            t2.zza(zzg.zzppw, null, null);
            t2.zzpph.zzbkr();
            return t2;
        } catch (RuntimeException e) {
            if (e.getCause() instanceof zzfie) {
                throw ((zzfie) e.getCause());
            }
            throw e;
        }
    }

    protected static <T extends zzfhu<T, ?>> T zza(T t, byte[] bArr) throws zzfie {
        zzfmg zzfmgVar = (T) zza(t, bArr, zzfhm.zzczf());
        if (zzfmgVar != null) {
            boolean zBooleanValue = Boolean.TRUE.booleanValue();
            byte bByteValue = ((Byte) zzfmgVar.zza(zzg.zzppt, (Object) null, (Object) null)).byteValue();
            if (bByteValue == 1) {
                z = true;
            } else if (bByteValue != 0) {
                z = zzfmgVar.zza(zzg.zzppr, Boolean.FALSE, (Object) null) != null;
                if (zBooleanValue) {
                    zzfmgVar.zza(zzg.zzppu, z ? zzfmgVar : null, (Object) null);
                }
            }
            if (!z) {
                throw new zzfkm(zzfmgVar).zzdbz().zzi(zzfmgVar);
            }
        }
        return zzfmgVar;
    }

    private static <T extends zzfhu<T, ?>> T zza(T t, byte[] bArr, zzfhm zzfhmVar) throws zzfie {
        try {
            zzfhb zzfhbVarZzbb = zzfhb.zzbb(bArr);
            T t2 = (T) zza(t, zzfhbVarZzbb, zzfhmVar);
            try {
                zzfhbVarZzbb.zzlf(0);
                return t2;
            } catch (zzfie e) {
                throw e.zzi(t2);
            }
        } catch (zzfie e2) {
            throw e2;
        }
    }

    static Object zza(Method method, Object obj, Object... objArr) {
        try {
            return method.invoke(obj, objArr);
        } catch (IllegalAccessException e) {
            throw new RuntimeException("Couldn't use Java reflection to implement protocol message reflection.", e);
        } catch (InvocationTargetException e2) {
            Throwable cause = e2.getCause();
            if (cause instanceof RuntimeException) {
                throw ((RuntimeException) cause);
            }
            if (cause instanceof Error) {
                throw ((Error) cause);
            }
            throw new RuntimeException("Unexpected exception thrown by generated accessor method.", cause);
        }
    }

    protected static final <T extends zzfhu<T, ?>> boolean zza(T t, boolean z) {
        byte bByteValue = ((Byte) t.zza(zzg.zzppt, null, null)).byteValue();
        if (bByteValue == 1) {
            return true;
        }
        return (bByteValue == 0 || t.zza(zzg.zzppr, Boolean.FALSE, null) == null) ? false : true;
    }

    protected static zzfic zzczr() {
        return zzfhy.zzdad();
    }

    protected static <E> zzfid<E> zzczs() {
        return zzfjo.zzdbg();
    }

    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!((zzfhu) zza(zzg.zzppz, (Object) null, (Object) null)).getClass().isInstance(obj)) {
            return false;
        }
        try {
            zzc zzcVar = zzc.zzppn;
            zzfhu zzfhuVar = (zzfhu) obj;
            zza(zzg.zzpps, zzcVar, zzfhuVar);
            this.zzpph = zzcVar.zza(this.zzpph, zzfhuVar.zzpph);
            return true;
        } catch (zzfhv unused) {
            return false;
        }
    }

    public int hashCode() {
        if (this.zzpno != 0) {
            return this.zzpno;
        }
        zze zzeVar = new zze();
        zza(zzg.zzpps, zzeVar, this);
        this.zzpph = zzeVar.zza(this.zzpph, this.zzpph);
        this.zzpno = zzeVar.zzmci;
        return this.zzpno;
    }

    @Override // com.google.android.gms.internal.zzfje
    public final boolean isInitialized() {
        boolean zBooleanValue = Boolean.TRUE.booleanValue();
        byte bByteValue = ((Byte) zza(zzg.zzppt, (Object) null, (Object) null)).byteValue();
        if (bByteValue == 1) {
            return true;
        }
        if (bByteValue == 0) {
            return false;
        }
        boolean z = zza(zzg.zzppr, Boolean.FALSE, (Object) null) != null;
        if (zBooleanValue) {
            zza(zzg.zzppu, z ? this : null, (Object) null);
        }
        return z;
    }

    public String toString() {
        return zzfjf.zza(this, super.toString());
    }

    protected abstract Object zza(int i, Object obj, Object obj2);

    @Override // com.google.android.gms.internal.zzfjc
    public void zza(zzfhg zzfhgVar) throws IOException {
        zzfjn.zzdbf().zzl(getClass()).zza(this, zzfhi.zzb(zzfhgVar));
    }

    protected final boolean zza(int i, zzfhb zzfhbVar) throws IOException {
        if ((i & 7) == 4) {
            return false;
        }
        if (this.zzpph == zzfko.zzdca()) {
            this.zzpph = zzfko.zzdcb();
        }
        return this.zzpph.zzb(i, zzfhbVar);
    }

    @Override // com.google.android.gms.internal.zzfjc
    public final zzfjl<MessageType> zzczq() {
        return (zzfjl) zza(zzg.zzpqa, (Object) null, (Object) null);
    }

    @Override // com.google.android.gms.internal.zzfjc
    public final /* synthetic */ zzfjd zzczt() {
        zza zzaVar = (zza) zza(zzg.zzppy, (Object) null, (Object) null);
        zzaVar.zza(this);
        return zzaVar;
    }

    @Override // com.google.android.gms.internal.zzfje
    public final /* synthetic */ zzfjc zzczu() {
        return (zzfhu) zza(zzg.zzppz, (Object) null, (Object) null);
    }

    @Override // com.google.android.gms.internal.zzfjc
    public int zzhs() {
        if (this.zzppi == -1) {
            this.zzppi = zzfjn.zzdbf().zzl(getClass()).zzct(this);
        }
        return this.zzppi;
    }
}
