package com.google.android.gms.common.data;

import com.google.android.gms.common.internal.Hide;
import java.util.NoSuchElementException;

/* JADX INFO: loaded from: classes.dex */
@Hide
public final class zzh<T> extends zzb<T> {
    private T zzgda;

    public zzh(DataBuffer<T> dataBuffer) {
        super(dataBuffer);
    }

    @Override // com.google.android.gms.common.data.zzb, java.util.Iterator
    public final T next() {
        if (!hasNext()) {
            int i = this.zzgcf;
            StringBuilder sb = new StringBuilder(46);
            sb.append("Cannot advance the iterator beyond ");
            sb.append(i);
            throw new NoSuchElementException(sb.toString());
        }
        this.zzgcf++;
        if (this.zzgcf == 0) {
            this.zzgda = this.zzgce.get(0);
            if (!(this.zzgda instanceof zzc)) {
                String strValueOf = String.valueOf(this.zzgda.getClass());
                StringBuilder sb2 = new StringBuilder(String.valueOf(strValueOf).length() + 44);
                sb2.append("DataBuffer reference of type ");
                sb2.append(strValueOf);
                sb2.append(" is not movable");
                throw new IllegalStateException(sb2.toString());
            }
        } else {
            ((zzc) this.zzgda).zzbw(this.zzgcf);
        }
        return this.zzgda;
    }
}
