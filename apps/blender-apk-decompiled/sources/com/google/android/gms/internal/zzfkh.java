package com.google.android.gms.internal;

/* JADX INFO: loaded from: classes.dex */
final class zzfkh {
    static String zzbd(zzfgs zzfgsVar) {
        String str;
        zzfki zzfkiVar = new zzfki(zzfgsVar);
        StringBuilder sb = new StringBuilder(zzfkiVar.size());
        for (int i = 0; i < zzfkiVar.size(); i++) {
            int iZzld = zzfkiVar.zzld(i);
            if (iZzld == 34) {
                str = "\\\"";
            } else if (iZzld == 39) {
                str = "\\'";
            } else if (iZzld != 92) {
                switch (iZzld) {
                    case 7:
                        str = "\\a";
                        break;
                    case 8:
                        str = "\\b";
                        break;
                    case 9:
                        str = "\\t";
                        break;
                    case 10:
                        str = "\\n";
                        break;
                    case 11:
                        str = "\\v";
                        break;
                    case 12:
                        str = "\\f";
                        break;
                    case 13:
                        str = "\\r";
                        break;
                    default:
                        if (iZzld < 32 || iZzld > 126) {
                            sb.append('\\');
                            sb.append((char) (((iZzld >>> 6) & 3) + 48));
                            sb.append((char) (((iZzld >>> 3) & 7) + 48));
                            iZzld = (iZzld & 7) + 48;
                        }
                        sb.append((char) iZzld);
                        continue;
                        break;
                }
            } else {
                str = "\\\\";
            }
            sb.append(str);
        }
        return sb.toString();
    }
}
