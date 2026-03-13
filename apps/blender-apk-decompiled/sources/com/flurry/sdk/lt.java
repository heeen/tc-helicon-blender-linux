package com.flurry.sdk;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/* JADX INFO: loaded from: classes.dex */
public final class lt {
    public static void a(JSONObject jSONObject, String str, Object obj) throws JSONException, NullPointerException {
        if (obj == null) {
            jSONObject.put(str, JSONObject.NULL);
        } else {
            jSONObject.put(str, obj);
        }
    }

    public static void a(JSONObject jSONObject, String str, String str2) throws JSONException, IOException {
        if (str2 != null) {
            jSONObject.put(str, str2);
        } else {
            jSONObject.put(str, JSONObject.NULL);
        }
    }

    public static void a(JSONObject jSONObject, String str, float f) throws JSONException, IOException {
        jSONObject.putOpt(str, Float.valueOf(f));
    }

    public static Map<String, String> a(JSONObject jSONObject) throws JSONException {
        HashMap map = new HashMap();
        Iterator<String> itKeys = jSONObject.keys();
        while (itKeys.hasNext()) {
            String next = itKeys.next();
            if (!(next instanceof String)) {
                throw new JSONException("JSONObject contains unsupported type for key in the map.");
            }
            String str = next;
            Object obj = jSONObject.get(str);
            if (!(obj instanceof String)) {
                throw new JSONException("JSONObject contains unsupported type for value in the map.");
            }
            map.put(str, (String) obj);
        }
        return map;
    }

    public static List<JSONObject> a(JSONArray jSONArray) throws JSONException {
        ArrayList arrayList = new ArrayList();
        for (int i = 0; i < jSONArray.length(); i++) {
            Object obj = jSONArray.get(i);
            if (!(obj instanceof JSONObject)) {
                throw new JSONException("Array contains unsupported objects. JSONArray param must contain JSON object.");
            }
            arrayList.add((JSONObject) obj);
        }
        return arrayList;
    }

    public static List<String> b(JSONArray jSONArray) throws JSONException {
        ArrayList arrayList = new ArrayList();
        for (int i = 0; i < jSONArray.length(); i++) {
            Object obj = jSONArray.get(i);
            if (!(obj instanceof String)) {
                throw new JSONException("Array contains unsupported objects. JSONArray param must contain String object.");
            }
            arrayList.add((String) obj);
        }
        return arrayList;
    }
}
