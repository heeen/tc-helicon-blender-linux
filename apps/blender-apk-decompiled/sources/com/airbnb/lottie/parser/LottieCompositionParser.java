package com.airbnb.lottie.parser;

import android.graphics.Rect;
import android.support.v4.util.LongSparseArray;
import android.support.v4.util.SparseArrayCompat;
import android.util.JsonReader;
import com.airbnb.lottie.L;
import com.airbnb.lottie.LottieComposition;
import com.airbnb.lottie.model.Font;
import com.airbnb.lottie.model.FontCharacter;
import com.airbnb.lottie.model.layer.Layer;
import com.airbnb.lottie.utils.Utils;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/* JADX INFO: loaded from: classes.dex */
public class LottieCompositionParser {
    private LottieCompositionParser() {
    }

    public static LottieComposition parse(JsonReader jsonReader) throws IOException {
        SparseArrayCompat<FontCharacter> sparseArrayCompat;
        HashMap map;
        float fDpScale = Utils.dpScale();
        LongSparseArray<Layer> longSparseArray = new LongSparseArray<>();
        ArrayList arrayList = new ArrayList();
        HashMap map2 = new HashMap();
        HashMap map3 = new HashMap();
        HashMap map4 = new HashMap();
        SparseArrayCompat<FontCharacter> sparseArrayCompat2 = new SparseArrayCompat<>();
        LottieComposition lottieComposition = new LottieComposition();
        jsonReader.beginObject();
        float fNextDouble = 0.0f;
        float fNextDouble2 = 0.0f;
        float fNextDouble3 = 0.0f;
        int iNextInt = 0;
        int iNextInt2 = 0;
        while (jsonReader.hasNext()) {
            switch (jsonReader.nextName()) {
                case "w":
                    sparseArrayCompat = sparseArrayCompat2;
                    map = map4;
                    iNextInt = jsonReader.nextInt();
                    continue;
                    map4 = map;
                    sparseArrayCompat2 = sparseArrayCompat;
                    break;
                case "h":
                    sparseArrayCompat = sparseArrayCompat2;
                    map = map4;
                    iNextInt2 = jsonReader.nextInt();
                    continue;
                    map4 = map;
                    sparseArrayCompat2 = sparseArrayCompat;
                    break;
                case "ip":
                    sparseArrayCompat = sparseArrayCompat2;
                    map = map4;
                    fNextDouble = (float) jsonReader.nextDouble();
                    continue;
                    map4 = map;
                    sparseArrayCompat2 = sparseArrayCompat;
                    break;
                case "op":
                    sparseArrayCompat = sparseArrayCompat2;
                    map = map4;
                    fNextDouble2 = (float) jsonReader.nextDouble();
                    continue;
                    map4 = map;
                    sparseArrayCompat2 = sparseArrayCompat;
                    break;
                case "fr":
                    fNextDouble3 = (float) jsonReader.nextDouble();
                    break;
                case "v":
                    String[] strArrSplit = jsonReader.nextString().split("\\.");
                    if (!Utils.isAtLeastVersion(Integer.parseInt(strArrSplit[0]), Integer.parseInt(strArrSplit[1]), Integer.parseInt(strArrSplit[2]), 4, 4, 0)) {
                        lottieComposition.addWarning("Lottie only supports bodymovin >= 4.4.0");
                        break;
                    }
                    break;
                case "layers":
                    parseLayers(jsonReader, lottieComposition, arrayList, longSparseArray);
                    break;
                case "assets":
                    parseAssets(jsonReader, lottieComposition, map2, map3);
                    break;
                case "fonts":
                    parseFonts(jsonReader, map4);
                    break;
                case "chars":
                    parseChars(jsonReader, lottieComposition, sparseArrayCompat2);
                    break;
                default:
                    sparseArrayCompat = sparseArrayCompat2;
                    map = map4;
                    jsonReader.skipValue();
                    continue;
                    map4 = map;
                    sparseArrayCompat2 = sparseArrayCompat;
                    break;
            }
            sparseArrayCompat = sparseArrayCompat2;
            map = map4;
            map4 = map;
            sparseArrayCompat2 = sparseArrayCompat;
        }
        jsonReader.endObject();
        lottieComposition.init(new Rect(0, 0, (int) (iNextInt * fDpScale), (int) (iNextInt2 * fDpScale)), fNextDouble, fNextDouble2, fNextDouble3, arrayList, longSparseArray, map2, map3, sparseArrayCompat2, map4);
        return lottieComposition;
    }

    private static void parseLayers(JsonReader jsonReader, LottieComposition lottieComposition, List<Layer> list, LongSparseArray<Layer> longSparseArray) throws IOException {
        jsonReader.beginArray();
        int i = 0;
        while (jsonReader.hasNext()) {
            Layer layer = LayerParser.parse(jsonReader, lottieComposition);
            if (layer.getLayerType() == Layer.LayerType.Image) {
                i++;
            }
            list.add(layer);
            longSparseArray.put(layer.getId(), layer);
            if (i > 4) {
                L.warn("You have " + i + " images. Lottie should primarily be used with shapes. If you are using Adobe Illustrator, convert the Illustrator layers to shape layers.");
            }
        }
        jsonReader.endArray();
    }

    /* JADX WARN: Removed duplicated region for block: B:39:0x0082  */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
        To view partially-correct add '--show-bad-code' argument
    */
    private static void parseAssets(android.util.JsonReader r12, com.airbnb.lottie.LottieComposition r13, java.util.Map<java.lang.String, java.util.List<com.airbnb.lottie.model.layer.Layer>> r14, java.util.Map<java.lang.String, com.airbnb.lottie.LottieImageAsset> r15) throws java.io.IOException {
        /*
            Method dump skipped, instruction units count: 244
            To view this dump add '--comments-level debug' option
        */
        throw new UnsupportedOperationException("Method not decompiled: com.airbnb.lottie.parser.LottieCompositionParser.parseAssets(android.util.JsonReader, com.airbnb.lottie.LottieComposition, java.util.Map, java.util.Map):void");
    }

    private static void parseFonts(JsonReader jsonReader, Map<String, Font> map) throws IOException {
        jsonReader.beginObject();
        while (jsonReader.hasNext()) {
            String strNextName = jsonReader.nextName();
            byte b = -1;
            if (strNextName.hashCode() == 3322014 && strNextName.equals("list")) {
                b = 0;
            }
            if (b == 0) {
                jsonReader.beginArray();
                while (jsonReader.hasNext()) {
                    Font font = FontParser.parse(jsonReader);
                    map.put(font.getName(), font);
                }
                jsonReader.endArray();
            } else {
                jsonReader.skipValue();
            }
        }
        jsonReader.endObject();
    }

    private static void parseChars(JsonReader jsonReader, LottieComposition lottieComposition, SparseArrayCompat<FontCharacter> sparseArrayCompat) throws IOException {
        jsonReader.beginArray();
        while (jsonReader.hasNext()) {
            FontCharacter fontCharacter = FontCharacterParser.parse(jsonReader, lottieComposition);
            sparseArrayCompat.put(fontCharacter.hashCode(), fontCharacter);
        }
        jsonReader.endArray();
    }
}
