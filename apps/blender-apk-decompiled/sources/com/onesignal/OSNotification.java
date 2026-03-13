package com.onesignal;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/* JADX INFO: loaded from: classes.dex */
public class OSNotification {
    public int androidNotificationId;
    public DisplayType displayType;
    public List<OSNotificationPayload> groupedNotifications;
    public boolean isAppInFocus;
    public OSNotificationPayload payload;
    public boolean shown;

    public enum DisplayType {
        Notification,
        InAppAlert,
        None
    }

    public OSNotification() {
    }

    public OSNotification(JSONObject jSONObject) {
        this.isAppInFocus = jSONObject.optBoolean("isAppInFocus");
        this.shown = jSONObject.optBoolean("shown", this.shown);
        this.androidNotificationId = jSONObject.optInt("androidNotificationId");
        this.displayType = DisplayType.values()[jSONObject.optInt("displayType")];
        if (jSONObject.has("groupedNotifications")) {
            JSONArray jSONArrayOptJSONArray = jSONObject.optJSONArray("groupedNotifications");
            this.groupedNotifications = new ArrayList();
            for (int i = 0; i < jSONArrayOptJSONArray.length(); i++) {
                this.groupedNotifications.add(new OSNotificationPayload(jSONArrayOptJSONArray.optJSONObject(i)));
            }
        }
        if (jSONObject.has("payload")) {
            this.payload = new OSNotificationPayload(jSONObject.optJSONObject("payload"));
        }
    }

    @Deprecated
    public String stringify() {
        JSONObject jSONObject = toJSONObject();
        try {
            if (jSONObject.has("additionalData")) {
                jSONObject.put("additionalData", jSONObject.optJSONObject("additionalData").toString());
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return jSONObject.toString();
    }

    public JSONObject toJSONObject() {
        JSONObject jSONObject = new JSONObject();
        try {
            jSONObject.put("isAppInFocus", this.isAppInFocus);
            jSONObject.put("shown", this.shown);
            jSONObject.put("androidNotificationId", this.androidNotificationId);
            jSONObject.put("displayType", this.displayType.ordinal());
            if (this.groupedNotifications != null) {
                JSONArray jSONArray = new JSONArray();
                Iterator<OSNotificationPayload> it = this.groupedNotifications.iterator();
                while (it.hasNext()) {
                    jSONArray.put(it.next().toJSONObject());
                }
                jSONObject.put("groupedNotifications", jSONArray);
            }
            jSONObject.put("payload", this.payload.toJSONObject());
        } catch (Throwable th) {
            th.printStackTrace();
        }
        return jSONObject;
    }
}
