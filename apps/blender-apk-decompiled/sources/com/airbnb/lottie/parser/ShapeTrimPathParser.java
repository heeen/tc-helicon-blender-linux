package com.airbnb.lottie.parser;

import android.util.JsonReader;
import com.airbnb.lottie.LottieComposition;
import com.airbnb.lottie.model.animatable.AnimatableFloatValue;
import com.airbnb.lottie.model.content.ShapeTrimPath;
import java.io.IOException;

/* JADX INFO: loaded from: classes.dex */
class ShapeTrimPathParser {
    private ShapeTrimPathParser() {
    }

    static ShapeTrimPath parse(JsonReader jsonReader, LottieComposition lottieComposition) throws IOException {
        String strNextString = null;
        ShapeTrimPath.Type typeForId = null;
        AnimatableFloatValue animatableFloatValue = null;
        AnimatableFloatValue animatableFloatValue2 = null;
        AnimatableFloatValue animatableFloatValue3 = null;
        while (jsonReader.hasNext()) {
            String strNextName = jsonReader.nextName();
            byte b = -1;
            int iHashCode = strNextName.hashCode();
            if (iHashCode != 101) {
                if (iHashCode != 109) {
                    if (iHashCode != 111) {
                        if (iHashCode != 115) {
                            if (iHashCode == 3519 && strNextName.equals("nm")) {
                                b = 3;
                            }
                        } else if (strNextName.equals("s")) {
                            b = 0;
                        }
                    } else if (strNextName.equals("o")) {
                        b = 2;
                    }
                } else if (strNextName.equals("m")) {
                    b = 4;
                }
            } else if (strNextName.equals("e")) {
                b = 1;
            }
            switch (b) {
                case 0:
                    animatableFloatValue = AnimatableValueParser.parseFloat(jsonReader, lottieComposition, false);
                    break;
                case 1:
                    animatableFloatValue2 = AnimatableValueParser.parseFloat(jsonReader, lottieComposition, false);
                    break;
                case 2:
                    animatableFloatValue3 = AnimatableValueParser.parseFloat(jsonReader, lottieComposition, false);
                    break;
                case 3:
                    strNextString = jsonReader.nextString();
                    break;
                case 4:
                    typeForId = ShapeTrimPath.Type.forId(jsonReader.nextInt());
                    break;
                default:
                    jsonReader.skipValue();
                    break;
            }
        }
        return new ShapeTrimPath(strNextString, typeForId, animatableFloatValue, animatableFloatValue2, animatableFloatValue3);
    }
}
