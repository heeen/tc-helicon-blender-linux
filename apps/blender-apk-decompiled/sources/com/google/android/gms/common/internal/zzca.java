package com.google.android.gms.common.internal;

import android.content.Context;
import android.content.res.Resources;
import com.google.android.gms.R;

/* JADX INFO: loaded from: classes.dex */
@Hide
public final class zzca {
    private final Resources zzgid;
    private final String zzgie;

    public zzca(Context context) {
        zzbq.checkNotNull(context);
        this.zzgid = context.getResources();
        this.zzgie = this.zzgid.getResourcePackageName(R.string.common_google_play_services_unknown_issue);
    }

    public final String getString(String str) {
        int identifier = this.zzgid.getIdentifier(str, "string", this.zzgie);
        if (identifier == 0) {
            return null;
        }
        return this.zzgid.getString(identifier);
    }
}
