package com.flurry.sdk;

import android.location.Location;
import java.util.Map;

/* JADX INFO: loaded from: classes.dex */
public class ie extends kk {
    private static final String a = "ie";

    public final String a(String str, Map<String, String> map) {
        String strA = a(str);
        while (strA != null) {
            if (a("timestamp_epoch_millis", strA)) {
                String strValueOf = String.valueOf(System.currentTimeMillis());
                kf.a(3, a, "Replacing param timestamp_epoch_millis with: " + strValueOf);
                str = str.replace(strA, lr.c(strValueOf));
            } else if (a("session_duration_millis", strA)) {
                jd.a();
                String string = Long.toString(jd.f());
                kf.a(3, a, "Replacing param session_duration_millis with: " + string);
                str = str.replace(strA, lr.c(string));
            } else if (a("fg_timespent_millis", strA)) {
                jd.a();
                String string2 = Long.toString(jd.f());
                kf.a(3, a, "Replacing param fg_timespent_millis with: " + string2);
                str = str.replace(strA, lr.c(string2));
            } else if (a("install_referrer", strA)) {
                String strB = new hl().b();
                if (strB == null) {
                    strB = "";
                }
                kf.a(3, a, "Replacing param install_referrer with: " + strB);
                str = str.replace(strA, lr.c(strB));
            } else if (a("geo_latitude", strA)) {
                Location locationE = ji.a().e();
                String str2 = "";
                if (locationE != null) {
                    str2 = "" + lr.a(locationE.getLatitude());
                }
                kf.a(3, a, "Replacing param geo_latitude with: " + str2);
                str = str.replace(strA, lr.c(str2));
            } else if (a("geo_longitude", strA)) {
                Location locationE2 = ji.a().e();
                String str3 = "";
                if (locationE2 != null) {
                    str3 = "" + lr.a(locationE2.getLongitude());
                }
                kf.a(3, a, "Replacing param geo_longitude with: " + str3);
                str = str.replace(strA, lr.c(str3));
            } else if (a("publisher_user_id", strA)) {
                String str4 = (String) li.a().a("UserId");
                kf.a(3, a, "Replacing param publisher_user_id with: " + str4);
                str = str.replace(strA, lr.c(str4));
            } else if (a("event_name", strA)) {
                if (map.containsKey("event_name")) {
                    kf.a(3, a, "Replacing param event_name with: " + map.get("event_name"));
                    str = str.replace(strA, lr.c(map.get("event_name")));
                } else {
                    kf.a(3, a, "Replacing param event_name with empty string");
                    str = str.replace(strA, "");
                }
            } else if (a("event_time_millis", strA)) {
                if (map.containsKey("event_time_millis")) {
                    kf.a(3, a, "Replacing param event_time_millis with: " + map.get("event_time_millis"));
                    str = str.replace(strA, lr.c(map.get("event_time_millis")));
                } else {
                    kf.a(3, a, "Replacing param event_time_millis with empty string");
                    str = str.replace(strA, "");
                }
            } else {
                kf.a(3, a, "Unknown param: " + strA);
                str = str.replace(strA, "");
            }
            strA = a(str);
        }
        return str;
    }
}
