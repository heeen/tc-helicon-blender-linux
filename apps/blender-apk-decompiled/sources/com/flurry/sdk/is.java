package com.flurry.sdk;

import android.text.TextUtils;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/* JADX INFO: loaded from: classes.dex */
public class is implements kz<hs> {
    private static final String a = "is";

    @Override // com.flurry.sdk.kz
    public final /* synthetic */ hs a(InputStream inputStream) throws IOException {
        return b(inputStream);
    }

    private static hs b(InputStream inputStream) throws IOException {
        if (inputStream == null) {
            return null;
        }
        String str = new String(lr.a(inputStream));
        kf.a(5, a, "Proton response string: " + str);
        hs hsVar = new hs();
        try {
            JSONObject jSONObject = new JSONObject(str);
            hsVar.a = jSONObject.optLong("issued_at", -1L);
            hsVar.b = jSONObject.optLong("refresh_ttl", 3600L);
            hsVar.c = jSONObject.optLong("expiration_ttl", 86400L);
            JSONObject jSONObjectOptJSONObject = jSONObject.optJSONObject("global_settings");
            hsVar.d = new hz();
            if (jSONObjectOptJSONObject != null) {
                hsVar.d.a = b(jSONObjectOptJSONObject.optString("log_level"));
            }
            JSONObject jSONObjectOptJSONObject2 = jSONObject.optJSONObject("pulse");
            hq hqVar = new hq();
            if (jSONObjectOptJSONObject2 != null) {
                a(hqVar, jSONObjectOptJSONObject2.optJSONArray("callbacks"));
                hqVar.b = jSONObjectOptJSONObject2.optInt("max_callback_retries", 3);
                hqVar.c = jSONObjectOptJSONObject2.optInt("max_callback_attempts_per_report", 15);
                hqVar.d = jSONObjectOptJSONObject2.optInt("max_report_delay_seconds", 600);
                hqVar.e = jSONObjectOptJSONObject2.optString("agent_report_url", "");
            }
            hsVar.e = hqVar;
            JSONObject jSONObjectOptJSONObject3 = jSONObject.optJSONObject("analytics");
            hsVar.f = new ic();
            if (jSONObjectOptJSONObject3 != null) {
                hsVar.f.b = jSONObjectOptJSONObject3.optBoolean("analytics_enabled", true);
                hsVar.f.a = jSONObjectOptJSONObject3.optInt("max_session_properties", 10);
            }
            return hsVar;
        } catch (JSONException e) {
            throw new IOException("Exception while deserialize: ", e);
        }
    }

    private static void a(hq hqVar, JSONArray jSONArray) throws JSONException {
        JSONObject jSONObjectOptJSONObject;
        if (jSONArray != null) {
            ArrayList arrayList = new ArrayList();
            for (int i = 0; i < jSONArray.length(); i++) {
                JSONObject jSONObjectOptJSONObject2 = jSONArray.optJSONObject(i);
                if (jSONObjectOptJSONObject2 != null) {
                    hp hpVar = new hp();
                    hpVar.b = jSONObjectOptJSONObject2.optString("partner", "");
                    a(hpVar, jSONObjectOptJSONObject2.optJSONArray("events"));
                    hpVar.d = a(jSONObjectOptJSONObject2.optString("method"));
                    hpVar.e = jSONObjectOptJSONObject2.optString("uri_template", "");
                    JSONObject jSONObjectOptJSONObject3 = jSONObjectOptJSONObject2.optJSONObject("body_template");
                    if (jSONObjectOptJSONObject3 != null) {
                        String strOptString = jSONObjectOptJSONObject3.optString("string", "null");
                        if (!strOptString.equals("null")) {
                            hpVar.f = strOptString;
                        }
                    }
                    hpVar.g = jSONObjectOptJSONObject2.optInt("max_redirects", 5);
                    hpVar.h = jSONObjectOptJSONObject2.optInt("connect_timeout", 20);
                    hpVar.i = jSONObjectOptJSONObject2.optInt("request_timeout", 20);
                    hpVar.a = jSONObjectOptJSONObject2.optLong("callback_id", -1L);
                    JSONObject jSONObjectOptJSONObject4 = jSONObjectOptJSONObject2.optJSONObject("headers");
                    if (jSONObjectOptJSONObject4 != null && (jSONObjectOptJSONObject = jSONObjectOptJSONObject4.optJSONObject("map")) != null) {
                        hpVar.j = lt.a(jSONObjectOptJSONObject);
                    }
                    arrayList.add(hpVar);
                }
            }
            hqVar.a = arrayList;
        }
    }

    private static void a(hp hpVar, JSONArray jSONArray) {
        String[] strArr;
        if (jSONArray != null) {
            ArrayList arrayList = null;
            for (int i = 0; i < jSONArray.length(); i++) {
                JSONObject jSONObjectOptJSONObject = jSONArray.optJSONObject(i);
                if (jSONObjectOptJSONObject != null) {
                    if (jSONObjectOptJSONObject.has("string")) {
                        if (arrayList == null) {
                            arrayList = new ArrayList();
                        }
                        hv hvVar = new hv();
                        hvVar.a = jSONObjectOptJSONObject.optString("string", "");
                        arrayList.add(hvVar);
                    } else if (jSONObjectOptJSONObject.has("com.flurry.proton.generated.avro.v2.EventParameterCallbackTrigger")) {
                        if (arrayList == null) {
                            arrayList = new ArrayList();
                        }
                        JSONObject jSONObjectOptJSONObject2 = jSONObjectOptJSONObject.optJSONObject("com.flurry.proton.generated.avro.v2.EventParameterCallbackTrigger");
                        if (jSONObjectOptJSONObject2 != null) {
                            hw hwVar = new hw();
                            hwVar.a = jSONObjectOptJSONObject2.optString("event_name", "");
                            hwVar.c = jSONObjectOptJSONObject2.optString("event_parameter_name", "");
                            JSONArray jSONArrayOptJSONArray = jSONObjectOptJSONObject2.optJSONArray("event_parameter_values");
                            if (jSONArrayOptJSONArray != null) {
                                strArr = new String[jSONArrayOptJSONArray.length()];
                                for (int i2 = 0; i2 < jSONArrayOptJSONArray.length(); i2++) {
                                    strArr[i2] = jSONArrayOptJSONArray.optString(i2, "");
                                }
                            } else {
                                strArr = new String[0];
                            }
                            hwVar.d = strArr;
                            arrayList.add(hwVar);
                        }
                    }
                }
            }
            hpVar.c = arrayList;
        }
    }

    private static ip a(String str) {
        ip ipVar = ip.GET;
        try {
            return !TextUtils.isEmpty(str) ? (ip) Enum.valueOf(ip.class, str) : ipVar;
        } catch (Exception unused) {
            return ipVar;
        }
    }

    private static ia b(String str) {
        ia iaVar = ia.OFF;
        try {
            return !TextUtils.isEmpty(str) ? (ia) Enum.valueOf(ia.class, str) : iaVar;
        } catch (Exception unused) {
            return iaVar;
        }
    }

    @Override // com.flurry.sdk.kz
    public final /* synthetic */ void a(OutputStream outputStream, hs hsVar) throws IOException {
        throw new IOException("Serialize not supported for response");
    }
}
