package com.google.android.gms.ads.identifier;

import android.net.Uri;
import android.util.Log;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Map;

/* JADX INFO: loaded from: classes.dex */
final class zza extends Thread {
    private /* synthetic */ Map zzamz;

    zza(AdvertisingIdClient advertisingIdClient, Map map) {
        this.zzamz = map;
    }

    @Override // java.lang.Thread, java.lang.Runnable
    public final void run() {
        String str;
        String message;
        StringBuilder sb;
        String str2;
        Exception exc;
        new zzc();
        Map map = this.zzamz;
        Uri.Builder builderBuildUpon = Uri.parse("https://pagead2.googlesyndication.com/pagead/gen_204?id=gmob-apps").buildUpon();
        for (String str3 : map.keySet()) {
            builderBuildUpon.appendQueryParameter(str3, (String) map.get(str3));
        }
        String string = builderBuildUpon.build().toString();
        try {
            try {
                HttpURLConnection httpURLConnection = (HttpURLConnection) new URL(string).openConnection();
                try {
                    int responseCode = httpURLConnection.getResponseCode();
                    if (responseCode < 200 || responseCode >= 300) {
                        StringBuilder sb2 = new StringBuilder(String.valueOf(string).length() + 65);
                        sb2.append("Received non-success response code ");
                        sb2.append(responseCode);
                        sb2.append(" from pinging URL: ");
                        sb2.append(string);
                        Log.w("HttpUrlPinger", sb2.toString());
                    }
                } finally {
                    httpURLConnection.disconnect();
                }
            } catch (IOException | RuntimeException e) {
                str = "HttpUrlPinger";
                message = e.getMessage();
                sb = new StringBuilder(String.valueOf(string).length() + 27 + String.valueOf(message).length());
                str2 = "Error while pinging URL: ";
                exc = e;
                sb.append(str2);
                sb.append(string);
                sb.append(". ");
                sb.append(message);
                Log.w(str, sb.toString(), exc);
            }
        } catch (IndexOutOfBoundsException e2) {
            str = "HttpUrlPinger";
            message = e2.getMessage();
            sb = new StringBuilder(String.valueOf(string).length() + 32 + String.valueOf(message).length());
            str2 = "Error while parsing ping URL: ";
            exc = e2;
            sb.append(str2);
            sb.append(string);
            sb.append(". ");
            sb.append(message);
            Log.w(str, sb.toString(), exc);
        }
    }
}
