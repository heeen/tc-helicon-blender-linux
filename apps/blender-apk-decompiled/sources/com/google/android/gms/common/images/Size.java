package com.google.android.gms.common.images;

/* JADX INFO: loaded from: classes.dex */
public final class Size {
    private final int zzalt;
    private final int zzalu;

    public Size(int i, int i2) {
        this.zzalt = i;
        this.zzalu = i2;
    }

    public static Size parseSize(String str) throws NumberFormatException {
        if (str == null) {
            throw new IllegalArgumentException("string must not be null");
        }
        int iIndexOf = str.indexOf(42);
        if (iIndexOf < 0) {
            iIndexOf = str.indexOf(120);
        }
        if (iIndexOf < 0) {
            throw zzgm(str);
        }
        try {
            return new Size(Integer.parseInt(str.substring(0, iIndexOf)), Integer.parseInt(str.substring(iIndexOf + 1)));
        } catch (NumberFormatException unused) {
            throw zzgm(str);
        }
    }

    private static NumberFormatException zzgm(String str) {
        StringBuilder sb = new StringBuilder(String.valueOf(str).length() + 16);
        sb.append("Invalid Size: \"");
        sb.append(str);
        sb.append("\"");
        throw new NumberFormatException(sb.toString());
    }

    public final boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (this == obj) {
            return true;
        }
        if (obj instanceof Size) {
            Size size = (Size) obj;
            if (this.zzalt == size.zzalt && this.zzalu == size.zzalu) {
                return true;
            }
        }
        return false;
    }

    public final int getHeight() {
        return this.zzalu;
    }

    public final int getWidth() {
        return this.zzalt;
    }

    public final int hashCode() {
        return ((this.zzalt >>> 16) | (this.zzalt << 16)) ^ this.zzalu;
    }

    public final String toString() {
        int i = this.zzalt;
        int i2 = this.zzalu;
        StringBuilder sb = new StringBuilder(23);
        sb.append(i);
        sb.append("x");
        sb.append(i2);
        return sb.toString();
    }
}
