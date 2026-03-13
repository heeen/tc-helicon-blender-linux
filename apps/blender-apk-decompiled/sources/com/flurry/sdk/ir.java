package com.flurry.sdk;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/* JADX INFO: loaded from: classes.dex */
public class ir implements kz<hr> {
    private static final String a = "ir";

    @Override // com.flurry.sdk.kz
    public final /* synthetic */ void a(OutputStream outputStream, hr hrVar) throws IOException {
        JSONObject jSONObject;
        hr hrVar2 = hrVar;
        if (outputStream == null || hrVar2 == null) {
            return;
        }
        DataOutputStream dataOutputStream = new DataOutputStream(outputStream) { // from class: com.flurry.sdk.ir.1
            @Override // java.io.FilterOutputStream, java.io.OutputStream, java.io.Closeable, java.lang.AutoCloseable
            public final void close() {
            }
        };
        JSONObject jSONObject2 = new JSONObject();
        try {
            try {
                a(jSONObject2, "project_key", hrVar2.a);
                a(jSONObject2, "bundle_id", hrVar2.b);
                a(jSONObject2, "app_version", hrVar2.c);
                jSONObject2.put("sdk_version", hrVar2.d);
                jSONObject2.put("platform", hrVar2.e);
                a(jSONObject2, "platform_version", hrVar2.f);
                jSONObject2.put("limit_ad_tracking", hrVar2.g);
                JSONObject jSONObject3 = null;
                if (hrVar2.h == null || hrVar2.h.a == null) {
                    jSONObject = null;
                } else {
                    jSONObject = new JSONObject();
                    JSONObject jSONObject4 = new JSONObject();
                    a(jSONObject4, "model", hrVar2.h.a.a);
                    a(jSONObject4, "brand", hrVar2.h.a.b);
                    a(jSONObject4, "id", hrVar2.h.a.c);
                    a(jSONObject4, "device", hrVar2.h.a.d);
                    a(jSONObject4, "product", hrVar2.h.a.e);
                    a(jSONObject4, "version_release", hrVar2.h.a.f);
                    jSONObject.put("com.flurry.proton.generated.avro.v2.AndroidTags", jSONObject4);
                }
                if (jSONObject != null) {
                    jSONObject2.put("device_tags", jSONObject);
                } else {
                    jSONObject2.put("device_tags", JSONObject.NULL);
                }
                JSONArray jSONArray = new JSONArray();
                for (ht htVar : hrVar2.i) {
                    JSONObject jSONObject5 = new JSONObject();
                    jSONObject5.put("type", htVar.a);
                    a(jSONObject5, "id", htVar.b);
                    jSONArray.put(jSONObject5);
                }
                jSONObject2.put("device_ids", jSONArray);
                if (hrVar2.j != null && hrVar2.j.a != null) {
                    jSONObject3 = new JSONObject();
                    JSONObject jSONObject6 = new JSONObject();
                    jSONObject6.putOpt("latitude", Double.valueOf(hrVar2.j.a.a));
                    jSONObject6.putOpt("longitude", Double.valueOf(hrVar2.j.a.b));
                    jSONObject6.putOpt("accuracy", Float.valueOf(hrVar2.j.a.c));
                    jSONObject3.put("com.flurry.proton.generated.avro.v2.Geolocation", jSONObject6);
                }
                if (jSONObject3 != null) {
                    jSONObject2.put("geo", jSONObject3);
                } else {
                    jSONObject2.put("geo", JSONObject.NULL);
                }
                JSONObject jSONObject7 = new JSONObject();
                if (hrVar2.k != null) {
                    a(jSONObject7, "string", hrVar2.k.a);
                    jSONObject2.put("publisher_user_id", jSONObject7);
                } else {
                    jSONObject2.put("publisher_user_id", JSONObject.NULL);
                }
                kf.a(5, a, "Proton Request String: " + jSONObject2.toString());
                dataOutputStream.write(jSONObject2.toString().getBytes());
                dataOutputStream.flush();
            } catch (JSONException e) {
                throw new IOException("Invalid Json", e);
            }
        } finally {
            dataOutputStream.close();
        }
    }

    private static void a(JSONObject jSONObject, String str, String str2) throws JSONException, IOException {
        if (str2 != null) {
            jSONObject.put(str, str2);
        } else {
            jSONObject.put(str, JSONObject.NULL);
        }
    }

    @Override // com.flurry.sdk.kz
    public final /* synthetic */ hr a(InputStream inputStream) throws IOException {
        throw new IOException("Deserialize not supported for request");
    }
}
