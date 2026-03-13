package com.google.android.gms.common.stats;

import android.os.Parcel;
import android.os.Parcelable;
import android.text.TextUtils;
import com.google.android.gms.internal.zzbgo;
import java.util.List;

/* JADX INFO: loaded from: classes.dex */
public final class WakeLockEvent extends StatsEvent {
    public static final Parcelable.Creator<WakeLockEvent> CREATOR = new zzd();
    private final long mTimeout;
    private int zzehz;
    private final long zzgjv;
    private int zzgjw;
    private final String zzgjx;
    private final String zzgjy;
    private final String zzgjz;
    private final int zzgka;
    private final List<String> zzgkb;
    private final String zzgkc;
    private final long zzgkd;
    private int zzgke;
    private final String zzgkf;
    private final float zzgkg;
    private long zzgkh;

    WakeLockEvent(int i, long j, int i2, String str, int i3, List<String> list, String str2, long j2, int i4, String str3, String str4, float f, long j3, String str5) {
        this.zzehz = i;
        this.zzgjv = j;
        this.zzgjw = i2;
        this.zzgjx = str;
        this.zzgjy = str3;
        this.zzgjz = str5;
        this.zzgka = i3;
        this.zzgkh = -1L;
        this.zzgkb = list;
        this.zzgkc = str2;
        this.zzgkd = j2;
        this.zzgke = i4;
        this.zzgkf = str4;
        this.zzgkg = f;
        this.mTimeout = j3;
    }

    public WakeLockEvent(long j, int i, String str, int i2, List<String> list, String str2, long j2, int i3, String str3, String str4, float f, long j3, String str5) {
        this(2, j, i, str, i2, list, str2, j2, i3, str3, str4, f, j3, str5);
    }

    @Override // com.google.android.gms.common.stats.StatsEvent
    public final int getEventType() {
        return this.zzgjw;
    }

    @Override // com.google.android.gms.common.stats.StatsEvent
    public final long getTimeMillis() {
        return this.zzgjv;
    }

    @Override // android.os.Parcelable
    public final void writeToParcel(Parcel parcel, int i) {
        int iZze = zzbgo.zze(parcel);
        zzbgo.zzc(parcel, 1, this.zzehz);
        zzbgo.zza(parcel, 2, getTimeMillis());
        zzbgo.zza(parcel, 4, this.zzgjx, false);
        zzbgo.zzc(parcel, 5, this.zzgka);
        zzbgo.zzb(parcel, 6, this.zzgkb, false);
        zzbgo.zza(parcel, 8, this.zzgkd);
        zzbgo.zza(parcel, 10, this.zzgjy, false);
        zzbgo.zzc(parcel, 11, getEventType());
        zzbgo.zza(parcel, 12, this.zzgkc, false);
        zzbgo.zza(parcel, 13, this.zzgkf, false);
        zzbgo.zzc(parcel, 14, this.zzgke);
        zzbgo.zza(parcel, 15, this.zzgkg);
        zzbgo.zza(parcel, 16, this.mTimeout);
        zzbgo.zza(parcel, 17, this.zzgjz, false);
        zzbgo.zzai(parcel, iZze);
    }

    @Override // com.google.android.gms.common.stats.StatsEvent
    public final long zzann() {
        return this.zzgkh;
    }

    @Override // com.google.android.gms.common.stats.StatsEvent
    public final String zzano() {
        String str = this.zzgjx;
        int i = this.zzgka;
        String strJoin = this.zzgkb == null ? "" : TextUtils.join(",", this.zzgkb);
        int i2 = this.zzgke;
        String str2 = this.zzgjy == null ? "" : this.zzgjy;
        String str3 = this.zzgkf == null ? "" : this.zzgkf;
        float f = this.zzgkg;
        String str4 = this.zzgjz == null ? "" : this.zzgjz;
        StringBuilder sb = new StringBuilder(String.valueOf(str).length() + 45 + String.valueOf(strJoin).length() + String.valueOf(str2).length() + String.valueOf(str3).length() + String.valueOf(str4).length());
        sb.append("\t");
        sb.append(str);
        sb.append("\t");
        sb.append(i);
        sb.append("\t");
        sb.append(strJoin);
        sb.append("\t");
        sb.append(i2);
        sb.append("\t");
        sb.append(str2);
        sb.append("\t");
        sb.append(str3);
        sb.append("\t");
        sb.append(f);
        sb.append("\t");
        sb.append(str4);
        return sb.toString();
    }
}
