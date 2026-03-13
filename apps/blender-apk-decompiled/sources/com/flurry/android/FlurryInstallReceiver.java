package com.flurry.android;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import com.flurry.sdk.hl;
import com.flurry.sdk.kf;
import com.flurry.sdk.lr;

/* JADX INFO: loaded from: classes.dex */
public final class FlurryInstallReceiver extends BroadcastReceiver {
    static final String a = "FlurryInstallReceiver";

    @Override // android.content.BroadcastReceiver
    public final void onReceive(Context context, Intent intent) {
        kf.a(4, a, "Received an Install nofication of " + intent.getAction());
        String string = intent.getExtras().getString("referrer");
        kf.a(4, a, "Received an Install referrer of " + string);
        if (string == null || !"com.android.vending.INSTALL_REFERRER".equals(intent.getAction())) {
            kf.a(5, a, "referrer is null");
            return;
        }
        if (!string.contains("=")) {
            kf.a(4, a, "referrer is before decoding: " + string);
            string = lr.d(string);
            kf.a(4, a, "referrer is: " + string);
        }
        new hl(context).a(string);
    }
}
