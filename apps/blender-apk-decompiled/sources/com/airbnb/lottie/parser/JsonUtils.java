package com.airbnb.lottie.parser;

import android.graphics.Color;
import android.graphics.PointF;
import android.support.annotation.ColorInt;
import android.util.JsonReader;
import android.util.JsonToken;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/* JADX INFO: loaded from: classes.dex */
class JsonUtils {
    private JsonUtils() {
    }

    @ColorInt
    static int jsonToColor(JsonReader jsonReader) throws IOException {
        jsonReader.beginArray();
        int iNextDouble = (int) (jsonReader.nextDouble() * 255.0d);
        int iNextDouble2 = (int) (jsonReader.nextDouble() * 255.0d);
        int iNextDouble3 = (int) (jsonReader.nextDouble() * 255.0d);
        while (jsonReader.hasNext()) {
            jsonReader.skipValue();
        }
        jsonReader.endArray();
        return Color.argb(255, iNextDouble, iNextDouble2, iNextDouble3);
    }

    static List<PointF> jsonToPoints(JsonReader jsonReader, float f) throws IOException {
        ArrayList arrayList = new ArrayList();
        jsonReader.beginArray();
        while (jsonReader.peek() == JsonToken.BEGIN_ARRAY) {
            jsonReader.beginArray();
            arrayList.add(jsonToPoint(jsonReader, f));
            jsonReader.endArray();
        }
        jsonReader.endArray();
        return arrayList;
    }

    /* JADX INFO: renamed from: com.airbnb.lottie.parser.JsonUtils$1, reason: invalid class name */
    static /* synthetic */ class AnonymousClass1 {
        static final /* synthetic */ int[] $SwitchMap$android$util$JsonToken = new int[JsonToken.values().length];

        static {
            try {
                $SwitchMap$android$util$JsonToken[JsonToken.NUMBER.ordinal()] = 1;
            } catch (NoSuchFieldError unused) {
            }
            try {
                $SwitchMap$android$util$JsonToken[JsonToken.BEGIN_ARRAY.ordinal()] = 2;
            } catch (NoSuchFieldError unused2) {
            }
            try {
                $SwitchMap$android$util$JsonToken[JsonToken.BEGIN_OBJECT.ordinal()] = 3;
            } catch (NoSuchFieldError unused3) {
            }
        }
    }

    static PointF jsonToPoint(JsonReader jsonReader, float f) throws IOException {
        switch (AnonymousClass1.$SwitchMap$android$util$JsonToken[jsonReader.peek().ordinal()]) {
            case 1:
                return jsonNumbersToPoint(jsonReader, f);
            case 2:
                return jsonArrayToPoint(jsonReader, f);
            case 3:
                return jsonObjectToPoint(jsonReader, f);
            default:
                throw new IllegalArgumentException("Unknown point starts with " + jsonReader.peek());
        }
    }

    private static PointF jsonNumbersToPoint(JsonReader jsonReader, float f) throws IOException {
        float fNextDouble = (float) jsonReader.nextDouble();
        float fNextDouble2 = (float) jsonReader.nextDouble();
        while (jsonReader.hasNext()) {
            jsonReader.skipValue();
        }
        return new PointF(fNextDouble * f, fNextDouble2 * f);
    }

    private static PointF jsonArrayToPoint(JsonReader jsonReader, float f) throws IOException {
        jsonReader.beginArray();
        float fNextDouble = (float) jsonReader.nextDouble();
        float fNextDouble2 = (float) jsonReader.nextDouble();
        while (jsonReader.peek() != JsonToken.END_ARRAY) {
            jsonReader.skipValue();
        }
        jsonReader.endArray();
        return new PointF(fNextDouble * f, fNextDouble2 * f);
    }

    private static PointF jsonObjectToPoint(JsonReader jsonReader, float f) throws IOException {
        jsonReader.beginObject();
        float fValueFromObject = 0.0f;
        float fValueFromObject2 = 0.0f;
        while (jsonReader.hasNext()) {
            switch (jsonReader.nextName()) {
                case "x":
                    fValueFromObject = valueFromObject(jsonReader);
                    break;
                case "y":
                    fValueFromObject2 = valueFromObject(jsonReader);
                    break;
                default:
                    jsonReader.skipValue();
                    break;
            }
        }
        jsonReader.endObject();
        return new PointF(fValueFromObject * f, fValueFromObject2 * f);
    }

    static float valueFromObject(JsonReader jsonReader) throws IOException {
        JsonToken jsonTokenPeek = jsonReader.peek();
        switch (AnonymousClass1.$SwitchMap$android$util$JsonToken[jsonTokenPeek.ordinal()]) {
            case 1:
                return (float) jsonReader.nextDouble();
            case 2:
                jsonReader.beginArray();
                float fNextDouble = (float) jsonReader.nextDouble();
                while (jsonReader.hasNext()) {
                    jsonReader.skipValue();
                }
                jsonReader.endArray();
                return fNextDouble;
            default:
                throw new IllegalArgumentException("Unknown value for token of type " + jsonTokenPeek);
        }
    }
}
