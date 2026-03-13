package com.google.android.gms.common.data;

import android.content.ContentValues;
import android.os.Parcel;
import android.os.Parcelable;
import com.google.android.gms.common.data.DataHolder;
import com.google.android.gms.common.internal.Hide;
import com.google.android.gms.internal.zzbgp;

/* JADX INFO: loaded from: classes.dex */
@Hide
public class zzd<T extends zzbgp> extends AbstractDataBuffer<T> {
    private static final String[] zzgcj = {"data"};
    private final Parcelable.Creator<T> zzgck;

    public zzd(DataHolder dataHolder, Parcelable.Creator<T> creator) {
        super(dataHolder);
        this.zzgck = creator;
    }

    public static <T extends zzbgp> void zza(DataHolder.zza zzaVar, T t) {
        Parcel parcelObtain = Parcel.obtain();
        t.writeToParcel(parcelObtain, 0);
        ContentValues contentValues = new ContentValues();
        contentValues.put("data", parcelObtain.marshall());
        zzaVar.zza(contentValues);
        parcelObtain.recycle();
    }

    public static DataHolder.zza zzalh() {
        return DataHolder.zzb(zzgcj);
    }

    @Override // com.google.android.gms.common.data.AbstractDataBuffer, com.google.android.gms.common.data.DataBuffer
    /* JADX INFO: renamed from: zzbx, reason: merged with bridge method [inline-methods] */
    public T get(int i) {
        byte[] bArrZzg = this.zzfxb.zzg("data", i, this.zzfxb.zzby(i));
        Parcel parcelObtain = Parcel.obtain();
        parcelObtain.unmarshall(bArrZzg, 0, bArrZzg.length);
        parcelObtain.setDataPosition(0);
        T tCreateFromParcel = this.zzgck.createFromParcel(parcelObtain);
        parcelObtain.recycle();
        return tCreateFromParcel;
    }
}
