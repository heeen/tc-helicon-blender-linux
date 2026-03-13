package com.google.android.gms.common;

/* JADX INFO: loaded from: classes.dex */
final class zzr extends zzp {
    private final String packageName;
    private final zzh zzfro;
    private final boolean zzfrp;
    private final boolean zzfrq;

    private zzr(String str, zzh zzhVar, boolean z, boolean z2) {
        super(false, null, null);
        this.packageName = str;
        this.zzfro = zzhVar;
        this.zzfrp = z;
        this.zzfrq = z2;
    }

    @Override // com.google.android.gms.common.zzp
    final String getErrorMessage() {
        String str = this.zzfrq ? "debug cert rejected" : "not whitelisted";
        String str2 = this.packageName;
        String strZzn = com.google.android.gms.common.util.zzm.zzn(com.google.android.gms.common.util.zza.zzeq("SHA-1").digest(this.zzfro.getBytes()));
        boolean z = this.zzfrp;
        StringBuilder sb = new StringBuilder(String.valueOf(str).length() + 44 + String.valueOf(str2).length() + String.valueOf(strZzn).length());
        sb.append(str);
        sb.append(": pkg=");
        sb.append(str2);
        sb.append(", sha1=");
        sb.append(strZzn);
        sb.append(", atk=");
        sb.append(z);
        sb.append(", ver=12211278.false");
        return sb.toString();
    }
}
