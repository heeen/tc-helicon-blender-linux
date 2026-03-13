package com.google.android.gms.common.data;

import android.content.ContentValues;
import android.database.CharArrayBuffer;
import android.database.CursorIndexOutOfBoundsException;
import android.database.CursorWindow;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;
import com.google.android.gms.common.annotation.KeepName;
import com.google.android.gms.common.internal.Hide;
import com.google.android.gms.common.internal.zzbq;
import com.google.android.gms.internal.zzbgl;
import com.google.android.gms.internal.zzbgo;
import java.io.Closeable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/* JADX INFO: loaded from: classes.dex */
@Hide
@KeepName
public final class DataHolder extends zzbgl implements Closeable {
    public static final Parcelable.Creator<DataHolder> CREATOR = new zzf();
    private static final zza zzgcs = new zze(new String[0], null);
    private boolean mClosed;
    private final int zzcc;
    private int zzehz;
    private final String[] zzgcl;
    private Bundle zzgcm;
    private final CursorWindow[] zzgcn;
    private final Bundle zzgco;
    private int[] zzgcp;
    int zzgcq;
    private boolean zzgcr;

    public static class zza {
        private final String[] zzgcl;
        private final ArrayList<HashMap<String, Object>> zzgct;
        private final String zzgcu;
        private final HashMap<Object, Integer> zzgcv;
        private boolean zzgcw;
        private String zzgcx;

        private zza(String[] strArr, String str) {
            this.zzgcl = (String[]) zzbq.checkNotNull(strArr);
            this.zzgct = new ArrayList<>();
            this.zzgcu = str;
            this.zzgcv = new HashMap<>();
            this.zzgcw = false;
            this.zzgcx = null;
        }

        /* synthetic */ zza(String[] strArr, String str, zze zzeVar) {
            this(strArr, null);
        }

        public zza zza(ContentValues contentValues) {
            com.google.android.gms.common.internal.zzc.zzv(contentValues);
            HashMap<String, Object> map = new HashMap<>(contentValues.size());
            for (Map.Entry<String, Object> entry : contentValues.valueSet()) {
                map.put(entry.getKey(), entry.getValue());
            }
            return zza(map);
        }

        public zza zza(HashMap<String, Object> map) {
            Object obj;
            int iIntValue;
            com.google.android.gms.common.internal.zzc.zzv(map);
            if (this.zzgcu == null || (obj = map.get(this.zzgcu)) == null) {
                iIntValue = -1;
            } else {
                Integer num = this.zzgcv.get(obj);
                if (num == null) {
                    this.zzgcv.put(obj, Integer.valueOf(this.zzgct.size()));
                    iIntValue = -1;
                } else {
                    iIntValue = num.intValue();
                }
            }
            if (iIntValue == -1) {
                this.zzgct.add(map);
            } else {
                this.zzgct.remove(iIntValue);
                this.zzgct.add(iIntValue, map);
            }
            this.zzgcw = false;
            return this;
        }

        /* JADX WARN: Multi-variable type inference failed */
        public final DataHolder zzca(int i) {
            return new DataHolder(this, 0, (Bundle) null, (zze) (0 == true ? 1 : 0));
        }
    }

    public static class zzb extends RuntimeException {
        public zzb(String str) {
            super(str);
        }
    }

    DataHolder(int i, String[] strArr, CursorWindow[] cursorWindowArr, int i2, Bundle bundle) {
        this.mClosed = false;
        this.zzgcr = true;
        this.zzehz = i;
        this.zzgcl = strArr;
        this.zzgcn = cursorWindowArr;
        this.zzcc = i2;
        this.zzgco = bundle;
    }

    private DataHolder(zza zzaVar, int i, Bundle bundle) {
        this(zzaVar.zzgcl, zza(zzaVar, -1), i, (Bundle) null);
    }

    /* synthetic */ DataHolder(zza zzaVar, int i, Bundle bundle, zze zzeVar) {
        this(zzaVar, 0, null);
    }

    private DataHolder(String[] strArr, CursorWindow[] cursorWindowArr, int i, Bundle bundle) {
        this.mClosed = false;
        this.zzgcr = true;
        this.zzehz = 1;
        this.zzgcl = (String[]) zzbq.checkNotNull(strArr);
        this.zzgcn = (CursorWindow[]) zzbq.checkNotNull(cursorWindowArr);
        this.zzcc = i;
        this.zzgco = bundle;
        zzali();
    }

    private static CursorWindow[] zza(zza zzaVar, int i) {
        long jLongValue;
        if (zzaVar.zzgcl.length == 0) {
            return new CursorWindow[0];
        }
        ArrayList arrayList = zzaVar.zzgct;
        int size = arrayList.size();
        CursorWindow cursorWindow = new CursorWindow(false);
        ArrayList arrayList2 = new ArrayList();
        arrayList2.add(cursorWindow);
        cursorWindow.setNumColumns(zzaVar.zzgcl.length);
        boolean z = false;
        CursorWindow cursorWindow2 = cursorWindow;
        int i2 = 0;
        while (i2 < size) {
            try {
                if (!cursorWindow2.allocRow()) {
                    StringBuilder sb = new StringBuilder(72);
                    sb.append("Allocating additional cursor window for large data set (row ");
                    sb.append(i2);
                    sb.append(")");
                    Log.d("DataHolder", sb.toString());
                    cursorWindow2 = new CursorWindow(false);
                    cursorWindow2.setStartPosition(i2);
                    cursorWindow2.setNumColumns(zzaVar.zzgcl.length);
                    arrayList2.add(cursorWindow2);
                    if (!cursorWindow2.allocRow()) {
                        Log.e("DataHolder", "Unable to allocate row to hold data.");
                        arrayList2.remove(cursorWindow2);
                        return (CursorWindow[]) arrayList2.toArray(new CursorWindow[arrayList2.size()]);
                    }
                }
                Map map = (Map) arrayList.get(i2);
                boolean zPutDouble = true;
                for (int i3 = 0; i3 < zzaVar.zzgcl.length && zPutDouble; i3++) {
                    String str = zzaVar.zzgcl[i3];
                    Object obj = map.get(str);
                    if (obj == null) {
                        zPutDouble = cursorWindow2.putNull(i2, i3);
                    } else if (obj instanceof String) {
                        zPutDouble = cursorWindow2.putString((String) obj, i2, i3);
                    } else {
                        if (obj instanceof Long) {
                            jLongValue = ((Long) obj).longValue();
                        } else if (obj instanceof Integer) {
                            zPutDouble = cursorWindow2.putLong(((Integer) obj).intValue(), i2, i3);
                        } else if (obj instanceof Boolean) {
                            jLongValue = ((Boolean) obj).booleanValue() ? 1L : 0L;
                        } else if (obj instanceof byte[]) {
                            zPutDouble = cursorWindow2.putBlob((byte[]) obj, i2, i3);
                        } else if (obj instanceof Double) {
                            zPutDouble = cursorWindow2.putDouble(((Double) obj).doubleValue(), i2, i3);
                        } else {
                            if (!(obj instanceof Float)) {
                                String strValueOf = String.valueOf(obj);
                                StringBuilder sb2 = new StringBuilder(String.valueOf(str).length() + 32 + String.valueOf(strValueOf).length());
                                sb2.append("Unsupported object for column ");
                                sb2.append(str);
                                sb2.append(": ");
                                sb2.append(strValueOf);
                                throw new IllegalArgumentException(sb2.toString());
                            }
                            zPutDouble = cursorWindow2.putDouble(((Float) obj).floatValue(), i2, i3);
                        }
                        zPutDouble = cursorWindow2.putLong(jLongValue, i2, i3);
                    }
                }
                if (zPutDouble) {
                    z = false;
                } else {
                    if (z) {
                        throw new zzb("Could not add the value to a new CursorWindow. The size of value may be larger than what a CursorWindow can handle.");
                    }
                    StringBuilder sb3 = new StringBuilder(74);
                    sb3.append("Couldn't populate window data for row ");
                    sb3.append(i2);
                    sb3.append(" - allocating new window.");
                    Log.d("DataHolder", sb3.toString());
                    cursorWindow2.freeLastRow();
                    cursorWindow2 = new CursorWindow(false);
                    cursorWindow2.setStartPosition(i2);
                    cursorWindow2.setNumColumns(zzaVar.zzgcl.length);
                    arrayList2.add(cursorWindow2);
                    i2--;
                    z = true;
                }
                i2++;
            } catch (RuntimeException e) {
                int size2 = arrayList2.size();
                for (int i4 = 0; i4 < size2; i4++) {
                    ((CursorWindow) arrayList2.get(i4)).close();
                }
                throw e;
            }
        }
        return (CursorWindow[]) arrayList2.toArray(new CursorWindow[arrayList2.size()]);
    }

    /* JADX WARN: Multi-variable type inference failed */
    public static zza zzb(String[] strArr) {
        return new zza(strArr, null, 0 == true ? 1 : 0);
    }

    public static DataHolder zzbz(int i) {
        return new DataHolder(zzgcs, i, null);
    }

    private final void zzh(String str, int i) {
        if (this.zzgcm == null || !this.zzgcm.containsKey(str)) {
            String strValueOf = String.valueOf(str);
            throw new IllegalArgumentException(strValueOf.length() != 0 ? "No such column: ".concat(strValueOf) : new String("No such column: "));
        }
        if (isClosed()) {
            throw new IllegalArgumentException("Buffer is closed.");
        }
        if (i < 0 || i >= this.zzgcq) {
            throw new CursorIndexOutOfBoundsException(i, this.zzgcq);
        }
    }

    @Override // java.io.Closeable, java.lang.AutoCloseable
    public final void close() {
        synchronized (this) {
            if (!this.mClosed) {
                this.mClosed = true;
                for (int i = 0; i < this.zzgcn.length; i++) {
                    this.zzgcn[i].close();
                }
            }
        }
    }

    protected final void finalize() throws Throwable {
        try {
            if (this.zzgcr && this.zzgcn.length > 0 && !isClosed()) {
                close();
                String string = toString();
                StringBuilder sb = new StringBuilder(String.valueOf(string).length() + 178);
                sb.append("Internal data leak within a DataBuffer object detected!  Be sure to explicitly call release() on all DataBuffer extending objects when you are done with them. (internal object: ");
                sb.append(string);
                sb.append(")");
                Log.e("DataBuffer", sb.toString());
            }
        } finally {
            super.finalize();
        }
    }

    public final int getCount() {
        return this.zzgcq;
    }

    public final int getStatusCode() {
        return this.zzcc;
    }

    public final boolean isClosed() {
        boolean z;
        synchronized (this) {
            z = this.mClosed;
        }
        return z;
    }

    @Override // android.os.Parcelable
    public final void writeToParcel(Parcel parcel, int i) {
        int iZze = zzbgo.zze(parcel);
        zzbgo.zza(parcel, 1, this.zzgcl, false);
        zzbgo.zza(parcel, 2, (Parcelable[]) this.zzgcn, i, false);
        zzbgo.zzc(parcel, 3, this.zzcc);
        zzbgo.zza(parcel, 4, this.zzgco, false);
        zzbgo.zzc(parcel, 1000, this.zzehz);
        zzbgo.zzai(parcel, iZze);
        if ((i & 1) != 0) {
            close();
        }
    }

    public final void zza(String str, int i, int i2, CharArrayBuffer charArrayBuffer) {
        zzh(str, i);
        this.zzgcn[i2].copyStringToBuffer(i, this.zzgcm.getInt(str), charArrayBuffer);
    }

    @Hide
    public final Bundle zzahs() {
        return this.zzgco;
    }

    public final void zzali() {
        this.zzgcm = new Bundle();
        for (int i = 0; i < this.zzgcl.length; i++) {
            this.zzgcm.putInt(this.zzgcl[i], i);
        }
        this.zzgcp = new int[this.zzgcn.length];
        int numRows = 0;
        for (int i2 = 0; i2 < this.zzgcn.length; i2++) {
            this.zzgcp[i2] = numRows;
            numRows += this.zzgcn[i2].getNumRows() - (numRows - this.zzgcn[i2].getStartPosition());
        }
        this.zzgcq = numRows;
    }

    public final long zzb(String str, int i, int i2) {
        zzh(str, i);
        return this.zzgcn[i2].getLong(i, this.zzgcm.getInt(str));
    }

    @Hide
    public final int zzby(int i) {
        int i2 = 0;
        zzbq.checkState(i >= 0 && i < this.zzgcq);
        while (true) {
            if (i2 >= this.zzgcp.length) {
                break;
            }
            if (i < this.zzgcp[i2]) {
                i2--;
                break;
            }
            i2++;
        }
        return i2 == this.zzgcp.length ? i2 - 1 : i2;
    }

    public final int zzc(String str, int i, int i2) {
        zzh(str, i);
        return this.zzgcn[i2].getInt(i, this.zzgcm.getInt(str));
    }

    public final String zzd(String str, int i, int i2) {
        zzh(str, i);
        return this.zzgcn[i2].getString(i, this.zzgcm.getInt(str));
    }

    public final boolean zze(String str, int i, int i2) {
        zzh(str, i);
        return Long.valueOf(this.zzgcn[i2].getLong(i, this.zzgcm.getInt(str))).longValue() == 1;
    }

    public final float zzf(String str, int i, int i2) {
        zzh(str, i);
        return this.zzgcn[i2].getFloat(i, this.zzgcm.getInt(str));
    }

    public final byte[] zzg(String str, int i, int i2) {
        zzh(str, i);
        return this.zzgcn[i2].getBlob(i, this.zzgcm.getInt(str));
    }

    public final boolean zzgj(String str) {
        return this.zzgcm.containsKey(str);
    }

    public final boolean zzh(String str, int i, int i2) {
        zzh(str, i);
        return this.zzgcn[i2].isNull(i, this.zzgcm.getInt(str));
    }
}
