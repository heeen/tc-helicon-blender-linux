package com.onesignal;

import java.util.Iterator;
import java.util.Set;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/* JADX INFO: loaded from: classes.dex */
class JSONUtils {
    JSONUtils() {
    }

    static JSONObject generateJsonDiff(JSONObject jSONObject, JSONObject jSONObject2, JSONObject jSONObject3, Set<String> set) {
        if (jSONObject == null) {
            return null;
        }
        if (jSONObject2 == null) {
            return jSONObject3;
        }
        Iterator<String> itKeys = jSONObject2.keys();
        JSONObject jSONObject4 = jSONObject3 != null ? jSONObject3 : new JSONObject();
        while (itKeys.hasNext()) {
            try {
                String next = itKeys.next();
                Object obj = jSONObject2.get(next);
                if (jSONObject.has(next)) {
                    if (obj instanceof JSONObject) {
                        String string = generateJsonDiff(jSONObject.getJSONObject(next), (JSONObject) obj, (jSONObject3 == null || !jSONObject3.has(next)) ? null : jSONObject3.getJSONObject(next), set).toString();
                        if (!string.equals("{}")) {
                            jSONObject4.put(next, new JSONObject(string));
                        }
                    } else if (obj instanceof JSONArray) {
                        handleJsonArray(next, (JSONArray) obj, jSONObject.getJSONArray(next), jSONObject4);
                    } else if (set != null && set.contains(next)) {
                        jSONObject4.put(next, obj);
                    } else {
                        Object obj2 = jSONObject.get(next);
                        if (!obj.equals(obj2)) {
                            if (!(obj2 instanceof Integer) || "".equals(obj)) {
                                jSONObject4.put(next, obj);
                            } else if (((Number) obj2).doubleValue() != ((Number) obj).doubleValue()) {
                                jSONObject4.put(next, obj);
                            }
                        }
                    }
                } else if (obj instanceof JSONObject) {
                    jSONObject4.put(next, new JSONObject(obj.toString()));
                } else if (obj instanceof JSONArray) {
                    handleJsonArray(next, (JSONArray) obj, null, jSONObject4);
                } else {
                    jSONObject4.put(next, obj);
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        return jSONObject4;
    }

    private static void handleJsonArray(String str, JSONArray jSONArray, JSONArray jSONArray2, JSONObject jSONObject) throws JSONException {
        if (str.endsWith("_a") || str.endsWith("_d")) {
            jSONObject.put(str, jSONArray);
            return;
        }
        String stringNE = toStringNE(jSONArray);
        JSONArray jSONArray3 = new JSONArray();
        JSONArray jSONArray4 = new JSONArray();
        String stringNE2 = jSONArray2 == null ? null : toStringNE(jSONArray2);
        for (int i = 0; i < jSONArray.length(); i++) {
            String str2 = (String) jSONArray.get(i);
            if (jSONArray2 == null || !stringNE2.contains(str2)) {
                jSONArray3.put(str2);
            }
        }
        if (jSONArray2 != null) {
            for (int i2 = 0; i2 < jSONArray2.length(); i2++) {
                String string = jSONArray2.getString(i2);
                if (!stringNE.contains(string)) {
                    jSONArray4.put(string);
                }
            }
        }
        if (!jSONArray3.toString().equals("[]")) {
            jSONObject.put(str + "_a", jSONArray3);
        }
        if (jSONArray4.toString().equals("[]")) {
            return;
        }
        jSONObject.put(str + "_d", jSONArray4);
    }

    static String toStringNE(JSONArray jSONArray) {
        String str = "[";
        int i = 0;
        while (i < jSONArray.length()) {
            try {
                i++;
                str = str + "\"" + jSONArray.getString(i) + "\"";
            } catch (Throwable unused) {
            }
        }
        return str + "]";
    }

    static JSONObject getJSONObjectWithoutBlankValues(JSONObject jSONObject, String str) {
        if (!jSONObject.has(str)) {
            return null;
        }
        JSONObject jSONObject2 = new JSONObject();
        JSONObject jSONObjectOptJSONObject = jSONObject.optJSONObject(str);
        Iterator<String> itKeys = jSONObjectOptJSONObject.keys();
        while (itKeys.hasNext()) {
            String next = itKeys.next();
            try {
                Object obj = jSONObjectOptJSONObject.get(next);
                if (!"".equals(obj)) {
                    jSONObject2.put(next, obj);
                }
            } catch (Throwable unused) {
            }
        }
        return jSONObject2;
    }
}
