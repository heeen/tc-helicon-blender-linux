package com.airbnb.lottie.parser;

import android.util.JsonReader;
import com.airbnb.lottie.LottieComposition;
import com.airbnb.lottie.model.animatable.AnimatableFloatValue;
import com.airbnb.lottie.model.animatable.AnimatableTransform;
import com.airbnb.lottie.model.content.Repeater;
import java.io.IOException;

/* JADX INFO: loaded from: classes.dex */
class RepeaterParser {
    private RepeaterParser() {
    }

    static Repeater parse(JsonReader jsonReader, LottieComposition lottieComposition) throws IOException {
        String strNextString = null;
        AnimatableFloatValue animatableFloatValue = null;
        AnimatableFloatValue animatableFloatValue2 = null;
        AnimatableTransform animatableTransform = null;
        while (jsonReader.hasNext()) {
            String strNextName = jsonReader.nextName();
            byte b = -1;
            int iHashCode = strNextName.hashCode();
            if (iHashCode != 99) {
                if (iHashCode != 111) {
                    if (iHashCode != 3519) {
                        if (iHashCode == 3710 && strNextName.equals("tr")) {
                            b = 3;
                        }
                    } else if (strNextName.equals("nm")) {
                        b = 0;
                    }
                } else if (strNextName.equals("o")) {
                    b = 2;
                }
            } else if (strNextName.equals("c")) {
                b = 1;
            }
            switch (b) {
                case 0:
                    strNextString = jsonReader.nextString();
                    break;
                case 1:
                    animatableFloatValue = AnimatableValueParser.parseFloat(jsonReader, lottieComposition, false);
                    break;
                case 2:
                    animatableFloatValue2 = AnimatableValueParser.parseFloat(jsonReader, lottieComposition, false);
                    break;
                case 3:
                    animatableTransform = AnimatableTransformParser.parse(jsonReader, lottieComposition);
                    break;
                default:
                    jsonReader.skipValue();
                    break;
            }
        }
        return new Repeater(strNextString, animatableFloatValue, animatableFloatValue2, animatableTransform);
    }
}
