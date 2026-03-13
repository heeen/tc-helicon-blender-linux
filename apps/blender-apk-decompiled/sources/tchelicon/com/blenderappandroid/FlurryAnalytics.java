package tchelicon.com.blenderappandroid;

import com.flurry.android.FlurryAgent;
import java.util.HashMap;

/* JADX INFO: loaded from: classes.dex */
public class FlurryAnalytics {
    public static String distribution(float f) {
        if (f < 0.0f) {
            return "Negative";
        }
        double d = f;
        return d < 0.1d ? "0-10%" : d < 0.2d ? "10-20%" : d < 0.3d ? "20-30%" : d < 0.4d ? "30-40%" : d < 0.5d ? "40-50%" : d < 0.6d ? "50-60%" : d < 0.7d ? "60-70%" : d < 0.8d ? "70-80%" : d < 0.9d ? "80-90%" : d <= 0.1d ? "90-100%" : "";
    }

    public static String distribution(int i) {
        return i < 0 ? "Negative" : i < 10 ? "0-10%" : i < 20 ? "10-20%" : i < 30 ? "20-30%" : i < 40 ? "30-40%" : i < 50 ? "40-50%" : i < 60 ? "50-60%" : i < 70 ? "60-70%" : i < 80 ? "70-80%" : i < 90 ? "80-90%" : i <= 100 ? "90-100%" : "";
    }

    public static void analytics(String str, String str2, String str3) {
        HashMap map = new HashMap();
        map.put(str2, str3);
        FlurryAgent.logEvent(str, map);
    }

    public static void analyticsLevels(String str, String str2, int i) {
        analytics(str, str2, distribution(i));
    }

    public static void analyticsLevels(String str, String str2, float f) {
        analytics(str, str2, distribution(f));
    }
}
