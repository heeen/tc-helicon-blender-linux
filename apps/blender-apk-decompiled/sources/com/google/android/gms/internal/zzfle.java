package com.google.android.gms.internal;

import java.io.IOException;

/* JADX WARN: Enum visitor error
jadx.core.utils.exceptions.JadxRuntimeException: Init of enum field 'zzpve' uses external variables
	at jadx.core.dex.visitors.EnumVisitor.createEnumFieldByConstructor(EnumVisitor.java:451)
	at jadx.core.dex.visitors.EnumVisitor.processEnumFieldByField(EnumVisitor.java:372)
	at jadx.core.dex.visitors.EnumVisitor.processEnumFieldByWrappedInsn(EnumVisitor.java:337)
	at jadx.core.dex.visitors.EnumVisitor.extractEnumFieldsFromFilledArray(EnumVisitor.java:322)
	at jadx.core.dex.visitors.EnumVisitor.extractEnumFieldsFromInsn(EnumVisitor.java:262)
	at jadx.core.dex.visitors.EnumVisitor.convertToEnum(EnumVisitor.java:151)
	at jadx.core.dex.visitors.EnumVisitor.visit(EnumVisitor.java:100)
 */
/* JADX WARN: Failed to restore enum class, 'enum' modifier and super class removed */
/* JADX INFO: loaded from: classes.dex */
abstract class zzfle {
    private static zzfle zzpve;
    public static final zzfle zzpvf;
    private static zzfle zzpvg;
    private static final /* synthetic */ zzfle[] zzpvh;

    static {
        final String str = "LOOSE";
        final int i = 0;
        zzpve = new zzfle(str, i) { // from class: com.google.android.gms.internal.zzflf
            {
                int i2 = 0;
                zzfkx zzfkxVar = null;
            }

            @Override // com.google.android.gms.internal.zzfle
            final Object zza(zzfhb zzfhbVar) throws IOException {
                return zzfhbVar.readString();
            }
        };
        final String str2 = "STRICT";
        final int i2 = 1;
        zzpvf = new zzfle(str2, i2) { // from class: com.google.android.gms.internal.zzflg
            {
                int i3 = 1;
                zzfkx zzfkxVar = null;
            }

            @Override // com.google.android.gms.internal.zzfle
            final Object zza(zzfhb zzfhbVar) throws IOException {
                return zzfhbVar.zzcye();
            }
        };
        final String str3 = "LAZY";
        final int i3 = 2;
        zzpvg = new zzfle(str3, i3) { // from class: com.google.android.gms.internal.zzflh
            {
                int i4 = 2;
                zzfkx zzfkxVar = null;
            }

            @Override // com.google.android.gms.internal.zzfle
            final Object zza(zzfhb zzfhbVar) throws IOException {
                return zzfhbVar.zzcyf();
            }
        };
        zzpvh = new zzfle[]{zzpve, zzpvf, zzpvg};
    }

    private zzfle(String str, int i) {
    }

    /* synthetic */ zzfle(String str, int i, zzfkx zzfkxVar) {
        this(str, i);
    }

    public static zzfle[] values() {
        return (zzfle[]) zzpvh.clone();
    }

    abstract Object zza(zzfhb zzfhbVar) throws IOException;
}
