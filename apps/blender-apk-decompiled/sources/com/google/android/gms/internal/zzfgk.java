package com.google.android.gms.internal;

import com.google.android.gms.internal.zzfgj;
import com.google.android.gms.internal.zzfgk;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/* JADX INFO: loaded from: classes.dex */
public abstract class zzfgk<MessageType extends zzfgj<MessageType, BuilderType>, BuilderType extends zzfgk<MessageType, BuilderType>> implements zzfjd {
    protected static <T> void zza(Iterable<T> iterable, List<? super T> list) {
        zzfhz.checkNotNull(iterable);
        if (!(iterable instanceof zzfil)) {
            if (iterable instanceof zzfjm) {
                list.addAll((Collection) iterable);
                return;
            } else {
                zzb(iterable, list);
                return;
            }
        }
        List<?> listZzdap = ((zzfil) iterable).zzdap();
        zzfil zzfilVar = (zzfil) list;
        int size = list.size();
        for (Object obj : listZzdap) {
            if (obj == null) {
                int size2 = zzfilVar.size() - size;
                StringBuilder sb = new StringBuilder(37);
                sb.append("Element at index ");
                sb.append(size2);
                sb.append(" is null.");
                String string = sb.toString();
                for (int size3 = zzfilVar.size() - 1; size3 >= size; size3--) {
                    zzfilVar.remove(size3);
                }
                throw new NullPointerException(string);
            }
            if (obj instanceof zzfgs) {
                zzfilVar.zzba((zzfgs) obj);
            } else {
                zzfilVar.add((String) obj);
            }
        }
    }

    private static <T> void zzb(Iterable<T> iterable, List<? super T> list) {
        if ((list instanceof ArrayList) && (iterable instanceof Collection)) {
            ((ArrayList) list).ensureCapacity(list.size() + ((Collection) iterable).size());
        }
        int size = list.size();
        for (T t : iterable) {
            if (t == null) {
                int size2 = list.size() - size;
                StringBuilder sb = new StringBuilder(37);
                sb.append("Element at index ");
                sb.append(size2);
                sb.append(" is null.");
                String string = sb.toString();
                for (int size3 = list.size() - 1; size3 >= size; size3--) {
                    list.remove(size3);
                }
                throw new NullPointerException(string);
            }
            list.add(t);
        }
    }

    protected abstract BuilderType zza(MessageType messagetype);

    @Override // com.google.android.gms.internal.zzfjd
    /* JADX INFO: renamed from: zza, reason: merged with bridge method [inline-methods] */
    public abstract BuilderType zzb(zzfhb zzfhbVar, zzfhm zzfhmVar) throws IOException;

    @Override // 
    /* JADX INFO: renamed from: zzcxj, reason: merged with bridge method [inline-methods] */
    public abstract BuilderType clone();

    @Override // com.google.android.gms.internal.zzfjd
    public final /* synthetic */ zzfjd zzd(zzfjc zzfjcVar) {
        if (zzczu().getClass().isInstance(zzfjcVar)) {
            return zza((zzfgj) zzfjcVar);
        }
        throw new IllegalArgumentException("mergeFrom(MessageLite) can only merge messages of the same type.");
    }
}
