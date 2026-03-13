package com.google.android.gms.internal;

import android.content.Intent;
import android.os.Parcel;
import android.os.Parcelable;
import com.google.android.gms.common.internal.Hide;
import com.google.android.gms.common.internal.zzbq;
import java.util.ArrayList;

/* JADX INFO: loaded from: classes.dex */
@Hide
public final class zzbgq {
    public static <T extends zzbgp> T zza(Intent intent, String str, Parcelable.Creator<T> creator) {
        byte[] byteArrayExtra = intent.getByteArrayExtra(str);
        if (byteArrayExtra == null) {
            return null;
        }
        return (T) zza(byteArrayExtra, creator);
    }

    public static <T extends zzbgp> T zza(byte[] bArr, Parcelable.Creator<T> creator) {
        zzbq.checkNotNull(creator);
        Parcel parcelObtain = Parcel.obtain();
        parcelObtain.unmarshall(bArr, 0, bArr.length);
        parcelObtain.setDataPosition(0);
        T tCreateFromParcel = creator.createFromParcel(parcelObtain);
        parcelObtain.recycle();
        return tCreateFromParcel;
    }

    public static <T extends zzbgp> void zza(T t, Intent intent, String str) {
        intent.putExtra(str, zza(t));
    }

    public static <T extends zzbgp> byte[] zza(T t) {
        Parcel parcelObtain = Parcel.obtain();
        t.writeToParcel(parcelObtain, 0);
        byte[] bArrMarshall = parcelObtain.marshall();
        parcelObtain.recycle();
        return bArrMarshall;
    }

    /* JADX WARN: Multi-variable type inference failed */
    public static <T extends zzbgp> ArrayList<T> zzb(Intent intent, String str, Parcelable.Creator<T> creator) {
        ArrayList arrayList = (ArrayList) intent.getSerializableExtra(str);
        if (arrayList == null) {
            return null;
        }
        ArrayList<T> arrayList2 = (ArrayList<T>) new ArrayList(arrayList.size());
        ArrayList arrayList3 = arrayList;
        int size = arrayList3.size();
        int i = 0;
        while (i < size) {
            Object obj = arrayList3.get(i);
            i++;
            arrayList2.add(zza((byte[]) obj, creator));
        }
        return arrayList2;
    }
}
