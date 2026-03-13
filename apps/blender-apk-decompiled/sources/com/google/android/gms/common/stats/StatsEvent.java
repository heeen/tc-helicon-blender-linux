package com.google.android.gms.common.stats;

import com.google.android.gms.common.internal.ReflectedParcelable;
import com.google.android.gms.internal.zzbgl;

/* JADX INFO: loaded from: classes.dex */
public abstract class StatsEvent extends zzbgl implements ReflectedParcelable {
    public abstract int getEventType();

    public abstract long getTimeMillis();

    public String toString() {
        long timeMillis = getTimeMillis();
        int eventType = getEventType();
        long jZzann = zzann();
        String strZzano = zzano();
        StringBuilder sb = new StringBuilder(String.valueOf(strZzano).length() + 53);
        sb.append(timeMillis);
        sb.append("\t");
        sb.append(eventType);
        sb.append("\t");
        sb.append(jZzann);
        sb.append(strZzano);
        return sb.toString();
    }

    public abstract long zzann();

    public abstract String zzano();
}
