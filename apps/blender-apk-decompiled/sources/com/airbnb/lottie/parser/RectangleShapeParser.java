package com.airbnb.lottie.parser;

import android.graphics.PointF;
import android.util.JsonReader;
import com.airbnb.lottie.LottieComposition;
import com.airbnb.lottie.model.animatable.AnimatableFloatValue;
import com.airbnb.lottie.model.animatable.AnimatablePointValue;
import com.airbnb.lottie.model.animatable.AnimatableValue;
import com.airbnb.lottie.model.content.RectangleShape;
import java.io.IOException;

/* JADX INFO: loaded from: classes.dex */
class RectangleShapeParser {
    private RectangleShapeParser() {
    }

    /* JADX WARN: Failed to restore switch over string. Please report as a decompilation issue */
    static RectangleShape parse(JsonReader jsonReader, LottieComposition lottieComposition) throws IOException {
        String strNextString = null;
        AnimatableValue<PointF, PointF> splitPath = null;
        AnimatablePointValue point = null;
        AnimatableFloatValue animatableFloatValue = null;
        while (jsonReader.hasNext()) {
            String strNextName = jsonReader.nextName();
            byte b = -1;
            int iHashCode = strNextName.hashCode();
            if (iHashCode != 112) {
                if (iHashCode != 3519) {
                    switch (iHashCode) {
                        case 114:
                            if (strNextName.equals("r")) {
                                b = 3;
                            }
                            break;
                        case 115:
                            if (strNextName.equals("s")) {
                                b = 2;
                            }
                            break;
                    }
                } else if (strNextName.equals("nm")) {
                    b = 0;
                }
            } else if (strNextName.equals("p")) {
                b = 1;
            }
            switch (b) {
                case 0:
                    strNextString = jsonReader.nextString();
                    break;
                case 1:
                    splitPath = AnimatablePathValueParser.parseSplitPath(jsonReader, lottieComposition);
                    break;
                case 2:
                    point = AnimatableValueParser.parsePoint(jsonReader, lottieComposition);
                    break;
                case 3:
                    animatableFloatValue = AnimatableValueParser.parseFloat(jsonReader, lottieComposition);
                    break;
                default:
                    jsonReader.skipValue();
                    break;
            }
        }
        return new RectangleShape(strNextString, splitPath, point, animatableFloatValue);
    }
}
