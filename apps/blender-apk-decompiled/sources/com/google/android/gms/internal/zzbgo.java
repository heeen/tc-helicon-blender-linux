package com.google.android.gms.internal;

import android.os.Bundle;
import android.os.IBinder;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.v4.internal.view.SupportMenu;
import java.util.List;

/* JADX INFO: loaded from: classes.dex */
public final class zzbgo {
    public static void zza(Parcel parcel, int i, byte b) {
        zzb(parcel, i, 4);
        parcel.writeInt(b);
    }

    public static void zza(Parcel parcel, int i, double d) {
        zzb(parcel, i, 8);
        parcel.writeDouble(d);
    }

    public static void zza(Parcel parcel, int i, float f) {
        zzb(parcel, i, 4);
        parcel.writeFloat(f);
    }

    public static void zza(Parcel parcel, int i, long j) {
        zzb(parcel, i, 8);
        parcel.writeLong(j);
    }

    public static void zza(Parcel parcel, int i, Bundle bundle, boolean z) {
        if (bundle == null) {
            return;
        }
        int iZzag = zzag(parcel, i);
        parcel.writeBundle(bundle);
        zzah(parcel, iZzag);
    }

    public static void zza(Parcel parcel, int i, IBinder iBinder, boolean z) {
        if (iBinder == null) {
            return;
        }
        int iZzag = zzag(parcel, i);
        parcel.writeStrongBinder(iBinder);
        zzah(parcel, iZzag);
    }

    public static void zza(Parcel parcel, int i, Parcel parcel2, boolean z) {
        if (parcel2 == null) {
            return;
        }
        int iZzag = zzag(parcel, 2);
        parcel.appendFrom(parcel2, 0, parcel2.dataSize());
        zzah(parcel, iZzag);
    }

    public static void zza(Parcel parcel, int i, Parcelable parcelable, int i2, boolean z) {
        if (parcelable == null) {
            if (z) {
                zzb(parcel, i, 0);
            }
        } else {
            int iZzag = zzag(parcel, i);
            parcelable.writeToParcel(parcel, i2);
            zzah(parcel, iZzag);
        }
    }

    public static void zza(Parcel parcel, int i, Boolean bool, boolean z) {
        if (bool == null) {
            return;
        }
        zzb(parcel, 3, 4);
        parcel.writeInt(bool.booleanValue() ? 1 : 0);
    }

    public static void zza(Parcel parcel, int i, Double d, boolean z) {
        if (d == null) {
            return;
        }
        zzb(parcel, i, 8);
        parcel.writeDouble(d.doubleValue());
    }

    public static void zza(Parcel parcel, int i, Float f, boolean z) {
        if (f == null) {
            return;
        }
        zzb(parcel, i, 4);
        parcel.writeFloat(f.floatValue());
    }

    public static void zza(Parcel parcel, int i, Integer num, boolean z) {
        if (num == null) {
            return;
        }
        zzb(parcel, i, 4);
        parcel.writeInt(num.intValue());
    }

    public static void zza(Parcel parcel, int i, Long l, boolean z) {
        if (l == null) {
            return;
        }
        zzb(parcel, i, 8);
        parcel.writeLong(l.longValue());
    }

    public static void zza(Parcel parcel, int i, String str, boolean z) {
        if (str == null) {
            if (z) {
                zzb(parcel, i, 0);
            }
        } else {
            int iZzag = zzag(parcel, i);
            parcel.writeString(str);
            zzah(parcel, iZzag);
        }
    }

    public static void zza(Parcel parcel, int i, List<Integer> list, boolean z) {
        if (list == null) {
            return;
        }
        int iZzag = zzag(parcel, i);
        int size = list.size();
        parcel.writeInt(size);
        for (int i2 = 0; i2 < size; i2++) {
            parcel.writeInt(list.get(i2).intValue());
        }
        zzah(parcel, iZzag);
    }

    public static void zza(Parcel parcel, int i, short s) {
        zzb(parcel, 3, 4);
        parcel.writeInt(s);
    }

    public static void zza(Parcel parcel, int i, boolean z) {
        zzb(parcel, i, 4);
        parcel.writeInt(z ? 1 : 0);
    }

    public static void zza(Parcel parcel, int i, byte[] bArr, boolean z) {
        if (bArr == null) {
            if (z) {
                zzb(parcel, i, 0);
            }
        } else {
            int iZzag = zzag(parcel, i);
            parcel.writeByteArray(bArr);
            zzah(parcel, iZzag);
        }
    }

    public static void zza(Parcel parcel, int i, float[] fArr, boolean z) {
        if (fArr == null) {
            return;
        }
        int iZzag = zzag(parcel, 7);
        parcel.writeFloatArray(fArr);
        zzah(parcel, iZzag);
    }

    public static void zza(Parcel parcel, int i, int[] iArr, boolean z) {
        if (iArr == null) {
            return;
        }
        int iZzag = zzag(parcel, i);
        parcel.writeIntArray(iArr);
        zzah(parcel, iZzag);
    }

    public static void zza(Parcel parcel, int i, long[] jArr, boolean z) {
        if (jArr == null) {
            return;
        }
        int iZzag = zzag(parcel, i);
        parcel.writeLongArray(jArr);
        zzah(parcel, iZzag);
    }

    public static <T extends Parcelable> void zza(Parcel parcel, int i, T[] tArr, int i2, boolean z) {
        if (tArr == null) {
            return;
        }
        int iZzag = zzag(parcel, i);
        parcel.writeInt(tArr.length);
        for (T t : tArr) {
            if (t == null) {
                parcel.writeInt(0);
            } else {
                zza(parcel, t, i2);
            }
        }
        zzah(parcel, iZzag);
    }

    public static void zza(Parcel parcel, int i, String[] strArr, boolean z) {
        if (strArr == null) {
            return;
        }
        int iZzag = zzag(parcel, i);
        parcel.writeStringArray(strArr);
        zzah(parcel, iZzag);
    }

    public static void zza(Parcel parcel, int i, boolean[] zArr, boolean z) {
        if (zArr == null) {
            return;
        }
        int iZzag = zzag(parcel, i);
        parcel.writeBooleanArray(zArr);
        zzah(parcel, iZzag);
    }

    public static void zza(Parcel parcel, int i, byte[][] bArr, boolean z) {
        if (bArr == null) {
            return;
        }
        int iZzag = zzag(parcel, i);
        parcel.writeInt(bArr.length);
        for (byte[] bArr2 : bArr) {
            parcel.writeByteArray(bArr2);
        }
        zzah(parcel, iZzag);
    }

    private static <T extends Parcelable> void zza(Parcel parcel, T t, int i) {
        int iDataPosition = parcel.dataPosition();
        parcel.writeInt(1);
        int iDataPosition2 = parcel.dataPosition();
        t.writeToParcel(parcel, i);
        int iDataPosition3 = parcel.dataPosition();
        parcel.setDataPosition(iDataPosition);
        parcel.writeInt(iDataPosition3 - iDataPosition2);
        parcel.setDataPosition(iDataPosition3);
    }

    private static int zzag(Parcel parcel, int i) {
        parcel.writeInt(i | SupportMenu.CATEGORY_MASK);
        parcel.writeInt(0);
        return parcel.dataPosition();
    }

    private static void zzah(Parcel parcel, int i) {
        int iDataPosition = parcel.dataPosition();
        parcel.setDataPosition(i - 4);
        parcel.writeInt(iDataPosition - i);
        parcel.setDataPosition(iDataPosition);
    }

    public static void zzai(Parcel parcel, int i) {
        zzah(parcel, i);
    }

    private static void zzb(Parcel parcel, int i, int i2) {
        if (i2 < 65535) {
            parcel.writeInt(i | (i2 << 16));
        } else {
            parcel.writeInt(i | SupportMenu.CATEGORY_MASK);
            parcel.writeInt(i2);
        }
    }

    public static void zzb(Parcel parcel, int i, List<String> list, boolean z) {
        if (list == null) {
            return;
        }
        int iZzag = zzag(parcel, i);
        parcel.writeStringList(list);
        zzah(parcel, iZzag);
    }

    public static void zzc(Parcel parcel, int i, int i2) {
        zzb(parcel, i, 4);
        parcel.writeInt(i2);
    }

    public static <T extends Parcelable> void zzc(Parcel parcel, int i, List<T> list, boolean z) {
        if (list == null) {
            if (z) {
                zzb(parcel, i, 0);
                return;
            }
            return;
        }
        int iZzag = zzag(parcel, i);
        int size = list.size();
        parcel.writeInt(size);
        for (int i2 = 0; i2 < size; i2++) {
            T t = list.get(i2);
            if (t == null) {
                parcel.writeInt(0);
            } else {
                zza(parcel, t, 0);
            }
        }
        zzah(parcel, iZzag);
    }

    public static void zzd(Parcel parcel, int i, List list, boolean z) {
        if (list == null) {
            return;
        }
        int iZzag = zzag(parcel, i);
        parcel.writeList(list);
        zzah(parcel, iZzag);
    }

    public static int zze(Parcel parcel) {
        return zzag(parcel, 20293);
    }
}
