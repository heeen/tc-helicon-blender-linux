package com.google.android.gms.internal;

/* JADX WARN: Enum visitor error
jadx.core.utils.exceptions.JadxRuntimeException: Init of enum field 'zzpug' uses external variables
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
public class zzfky {
    public static final zzfky zzpty = new zzfky("DOUBLE", 0, zzfld.DOUBLE, 1);
    public static final zzfky zzptz = new zzfky("FLOAT", 1, zzfld.FLOAT, 5);
    public static final zzfky zzpua = new zzfky("INT64", 2, zzfld.LONG, 0);
    public static final zzfky zzpub = new zzfky("UINT64", 3, zzfld.LONG, 0);
    public static final zzfky zzpuc = new zzfky("INT32", 4, zzfld.INT, 0);
    public static final zzfky zzpud = new zzfky("FIXED64", 5, zzfld.LONG, 1);
    public static final zzfky zzpue = new zzfky("FIXED32", 6, zzfld.INT, 5);
    public static final zzfky zzpuf = new zzfky("BOOL", 7, zzfld.BOOLEAN, 0);
    public static final zzfky zzpug;
    public static final zzfky zzpuh;
    public static final zzfky zzpui;
    public static final zzfky zzpuj;
    public static final zzfky zzpuk;
    public static final zzfky zzpul;
    public static final zzfky zzpum;
    public static final zzfky zzpun;
    public static final zzfky zzpuo;
    public static final zzfky zzpup;
    private static final /* synthetic */ zzfky[] zzpus;
    private final zzfld zzpuq;
    private final int zzpur;

    static {
        final int i = 2;
        final int i2 = 3;
        final String str = "STRING";
        final zzfld zzfldVar = zzfld.STRING;
        final int i3 = 8;
        zzpug = new zzfky(str, i3, zzfldVar, i) { // from class: com.google.android.gms.internal.zzfkz
            {
                int i4 = 8;
                int i5 = 2;
                zzfkx zzfkxVar = null;
            }
        };
        final String str2 = "GROUP";
        final zzfld zzfldVar2 = zzfld.MESSAGE;
        final int i4 = 9;
        zzpuh = new zzfky(str2, i4, zzfldVar2, i2) { // from class: com.google.android.gms.internal.zzfla
            {
                int i5 = 9;
                int i6 = 3;
                zzfkx zzfkxVar = null;
            }
        };
        final String str3 = "MESSAGE";
        final zzfld zzfldVar3 = zzfld.MESSAGE;
        final int i5 = 10;
        zzpui = new zzfky(str3, i5, zzfldVar3, i) { // from class: com.google.android.gms.internal.zzflb
            {
                int i6 = 10;
                int i7 = 2;
                zzfkx zzfkxVar = null;
            }
        };
        final String str4 = "BYTES";
        final zzfld zzfldVar4 = zzfld.BYTE_STRING;
        final int i6 = 11;
        zzpuj = new zzfky(str4, i6, zzfldVar4, i) { // from class: com.google.android.gms.internal.zzflc
            {
                int i7 = 11;
                int i8 = 2;
                zzfkx zzfkxVar = null;
            }
        };
        zzpuk = new zzfky("UINT32", 12, zzfld.INT, 0);
        zzpul = new zzfky("ENUM", 13, zzfld.ENUM, 0);
        zzpum = new zzfky("SFIXED32", 14, zzfld.INT, 5);
        zzpun = new zzfky("SFIXED64", 15, zzfld.LONG, 1);
        zzpuo = new zzfky("SINT32", 16, zzfld.INT, 0);
        zzpup = new zzfky("SINT64", 17, zzfld.LONG, 0);
        zzpus = new zzfky[]{zzpty, zzptz, zzpua, zzpub, zzpuc, zzpud, zzpue, zzpuf, zzpug, zzpuh, zzpui, zzpuj, zzpuk, zzpul, zzpum, zzpun, zzpuo, zzpup};
    }

    private zzfky(String str, int i, zzfld zzfldVar, int i2) {
        this.zzpuq = zzfldVar;
        this.zzpur = i2;
    }

    /* synthetic */ zzfky(String str, int i, zzfld zzfldVar, int i2, zzfkx zzfkxVar) {
        this(str, i, zzfldVar, i2);
    }

    public static zzfky[] values() {
        return (zzfky[]) zzpus.clone();
    }

    public final zzfld zzdci() {
        return this.zzpuq;
    }

    public final int zzdcj() {
        return this.zzpur;
    }
}
