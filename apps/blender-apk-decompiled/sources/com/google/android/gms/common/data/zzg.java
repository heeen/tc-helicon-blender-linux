package com.google.android.gms.common.data;

import com.google.android.gms.common.internal.Hide;
import java.util.ArrayList;

/* JADX INFO: loaded from: classes.dex */
@Hide
public abstract class zzg<T> extends AbstractDataBuffer<T> {
    private boolean zzgcy;
    private ArrayList<Integer> zzgcz;

    protected zzg(DataHolder dataHolder) {
        super(dataHolder);
        this.zzgcy = false;
    }

    private final void zzalk() {
        synchronized (this) {
            if (!this.zzgcy) {
                int i = this.zzfxb.zzgcq;
                this.zzgcz = new ArrayList<>();
                if (i > 0) {
                    this.zzgcz.add(0);
                    String strZzalj = zzalj();
                    String strZzd = this.zzfxb.zzd(strZzalj, 0, this.zzfxb.zzby(0));
                    for (int i2 = 1; i2 < i; i2++) {
                        int iZzby = this.zzfxb.zzby(i2);
                        String strZzd2 = this.zzfxb.zzd(strZzalj, i2, iZzby);
                        if (strZzd2 == null) {
                            StringBuilder sb = new StringBuilder(String.valueOf(strZzalj).length() + 78);
                            sb.append("Missing value for markerColumn: ");
                            sb.append(strZzalj);
                            sb.append(", at row: ");
                            sb.append(i2);
                            sb.append(", for window: ");
                            sb.append(iZzby);
                            throw new NullPointerException(sb.toString());
                        }
                        if (!strZzd2.equals(strZzd)) {
                            this.zzgcz.add(Integer.valueOf(i2));
                            strZzd = strZzd2;
                        }
                    }
                }
                this.zzgcy = true;
            }
        }
    }

    private final int zzcb(int i) {
        if (i >= 0 && i < this.zzgcz.size()) {
            return this.zzgcz.get(i).intValue();
        }
        StringBuilder sb = new StringBuilder(53);
        sb.append("Position ");
        sb.append(i);
        sb.append(" is out of bounds for this buffer");
        throw new IllegalArgumentException(sb.toString());
    }

    @Override // com.google.android.gms.common.data.AbstractDataBuffer, com.google.android.gms.common.data.DataBuffer
    public final T get(int i) {
        int iIntValue;
        zzalk();
        int iZzcb = zzcb(i);
        if (i < 0 || i == this.zzgcz.size()) {
            iIntValue = 0;
        } else {
            iIntValue = (i == this.zzgcz.size() - 1 ? this.zzfxb.zzgcq : this.zzgcz.get(i + 1).intValue()) - this.zzgcz.get(i).intValue();
            if (iIntValue == 1) {
                this.zzfxb.zzby(zzcb(i));
            }
        }
        return zzl(iZzcb, iIntValue);
    }

    @Override // com.google.android.gms.common.data.AbstractDataBuffer, com.google.android.gms.common.data.DataBuffer
    public int getCount() {
        zzalk();
        return this.zzgcz.size();
    }

    @Hide
    protected abstract String zzalj();

    @Hide
    protected abstract T zzl(int i, int i2);
}
