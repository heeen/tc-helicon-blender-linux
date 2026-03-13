package com.airbnb.lottie.parser;

import android.util.JsonReader;
import com.airbnb.lottie.LottieComposition;
import com.airbnb.lottie.model.animatable.AnimatableColorValue;
import com.airbnb.lottie.model.animatable.AnimatableFloatValue;
import com.airbnb.lottie.model.animatable.AnimatableTextProperties;
import java.io.IOException;

/* JADX INFO: loaded from: classes.dex */
public class AnimatableTextPropertiesParser {
    private AnimatableTextPropertiesParser() {
    }

    public static AnimatableTextProperties parse(JsonReader jsonReader, LottieComposition lottieComposition) throws IOException {
        jsonReader.beginObject();
        AnimatableTextProperties animatableTextProperties = null;
        while (jsonReader.hasNext()) {
            String strNextName = jsonReader.nextName();
            byte b = -1;
            if (strNextName.hashCode() == 97 && strNextName.equals("a")) {
                b = 0;
            }
            if (b == 0) {
                animatableTextProperties = parseAnimatableTextProperties(jsonReader, lottieComposition);
            } else {
                jsonReader.skipValue();
            }
        }
        jsonReader.endObject();
        return animatableTextProperties == null ? new AnimatableTextProperties(null, null, null, null) : animatableTextProperties;
    }

    private static AnimatableTextProperties parseAnimatableTextProperties(JsonReader jsonReader, LottieComposition lottieComposition) throws IOException {
        jsonReader.beginObject();
        AnimatableColorValue color = null;
        AnimatableColorValue color2 = null;
        AnimatableFloatValue animatableFloatValue = null;
        AnimatableFloatValue animatableFloatValue2 = null;
        while (jsonReader.hasNext()) {
            String strNextName = jsonReader.nextName();
            byte b = -1;
            int iHashCode = strNextName.hashCode();
            if (iHashCode != 116) {
                if (iHashCode != 3261) {
                    if (iHashCode != 3664) {
                        if (iHashCode == 3684 && strNextName.equals("sw")) {
                            b = 2;
                        }
                    } else if (strNextName.equals("sc")) {
                        b = 1;
                    }
                } else if (strNextName.equals("fc")) {
                    b = 0;
                }
            } else if (strNextName.equals("t")) {
                b = 3;
            }
            switch (b) {
                case 0:
                    color = AnimatableValueParser.parseColor(jsonReader, lottieComposition);
                    break;
                case 1:
                    color2 = AnimatableValueParser.parseColor(jsonReader, lottieComposition);
                    break;
                case 2:
                    animatableFloatValue = AnimatableValueParser.parseFloat(jsonReader, lottieComposition);
                    break;
                case 3:
                    animatableFloatValue2 = AnimatableValueParser.parseFloat(jsonReader, lottieComposition);
                    break;
                default:
                    jsonReader.skipValue();
                    break;
            }
        }
        jsonReader.endObject();
        return new AnimatableTextProperties(color, color2, animatableFloatValue, animatableFloatValue2);
    }
}
