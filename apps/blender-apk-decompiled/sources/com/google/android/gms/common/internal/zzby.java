package com.google.android.gms.common.internal;

import android.R;
import android.content.Context;
import android.util.AttributeSet;
import android.widget.Button;

/* JADX INFO: loaded from: classes.dex */
public final class zzby extends Button {
    public zzby(Context context) {
        this(context, null);
    }

    private zzby(Context context, AttributeSet attributeSet) {
        super(context, null, R.attr.buttonStyle);
    }

    private static int zzf(int i, int i2, int i3, int i4) {
        switch (i) {
            case 0:
                return i2;
            case 1:
                return i3;
            case 2:
                return i4;
            default:
                StringBuilder sb = new StringBuilder(33);
                sb.append("Unknown color scheme: ");
                sb.append(i);
                throw new IllegalStateException(sb.toString());
        }
    }

    /* JADX WARN: Removed duplicated region for block: B:17:0x00b6  */
    /* JADX WARN: Removed duplicated region for block: B:19:? A[RETURN, SYNTHETIC] */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
        To view partially-correct add '--show-bad-code' argument
    */
    public final void zza(android.content.res.Resources r5, int r6, int r7) {
        /*
            Method dump skipped, instruction units count: 208
            To view this dump add '--comments-level debug' option
        */
        throw new UnsupportedOperationException("Method not decompiled: com.google.android.gms.common.internal.zzby.zza(android.content.res.Resources, int, int):void");
    }
}
