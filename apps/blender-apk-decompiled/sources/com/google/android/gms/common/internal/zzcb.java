package com.google.android.gms.common.internal;

import android.content.Context;
import android.content.res.Resources;
import android.util.AttributeSet;
import android.util.Log;
import android.util.TypedValue;

/* JADX INFO: loaded from: classes.dex */
@Hide
public final class zzcb {
    public static String zza(String str, String str2, Context context, AttributeSet attributeSet, boolean z, boolean z2, String str3) {
        String attributeValue = attributeSet == null ? null : attributeSet.getAttributeValue(str, str2);
        if (attributeValue == null || !attributeValue.startsWith("@string/")) {
            return attributeValue;
        }
        String strSubstring = attributeValue.substring(8);
        String packageName = context.getPackageName();
        TypedValue typedValue = new TypedValue();
        try {
            Resources resources = context.getResources();
            StringBuilder sb = new StringBuilder(String.valueOf(packageName).length() + 8 + String.valueOf(strSubstring).length());
            sb.append(packageName);
            sb.append(":string/");
            sb.append(strSubstring);
            resources.getValue(sb.toString(), typedValue, true);
        } catch (Resources.NotFoundException unused) {
            StringBuilder sb2 = new StringBuilder(String.valueOf(str2).length() + 30 + String.valueOf(attributeValue).length());
            sb2.append("Could not find resource for ");
            sb2.append(str2);
            sb2.append(": ");
            sb2.append(attributeValue);
            Log.w(str3, sb2.toString());
        }
        if (typedValue.string != null) {
            return typedValue.string.toString();
        }
        String strValueOf = String.valueOf(typedValue);
        StringBuilder sb3 = new StringBuilder(String.valueOf(str2).length() + 28 + String.valueOf(strValueOf).length());
        sb3.append("Resource ");
        sb3.append(str2);
        sb3.append(" was not a string: ");
        sb3.append(strValueOf);
        Log.w(str3, sb3.toString());
        return attributeValue;
    }
}
