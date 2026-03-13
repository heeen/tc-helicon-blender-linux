package com.google.android.gms.phenotype;

import java.util.Comparator;

/* JADX INFO: loaded from: classes.dex */
final class zzj implements Comparator<zzi> {
    zzj() {
    }

    @Override // java.util.Comparator
    public final /* synthetic */ int compare(zzi zziVar, zzi zziVar2) {
        zzi zziVar3 = zziVar;
        zzi zziVar4 = zziVar2;
        return zziVar3.zzkgp == zziVar4.zzkgp ? zziVar3.name.compareTo(zziVar4.name) : zziVar3.zzkgp - zziVar4.zzkgp;
    }
}
