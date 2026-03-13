package com.airbnb.lottie.parser;

import android.graphics.PointF;
import android.util.JsonReader;
import com.airbnb.lottie.LottieComposition;
import com.airbnb.lottie.model.animatable.AnimatableFloatValue;
import com.airbnb.lottie.model.animatable.AnimatableValue;
import com.airbnb.lottie.model.content.PolystarShape;
import java.io.IOException;

/* JADX INFO: loaded from: classes.dex */
class PolystarShapeParser {
    private PolystarShapeParser() {
    }

    static PolystarShape parse(JsonReader jsonReader, LottieComposition lottieComposition) throws IOException {
        String strNextString = null;
        PolystarShape.Type typeForValue = null;
        AnimatableFloatValue animatableFloatValue = null;
        AnimatableValue<PointF, PointF> splitPath = null;
        AnimatableFloatValue animatableFloatValue2 = null;
        AnimatableFloatValue animatableFloatValue3 = null;
        AnimatableFloatValue animatableFloatValue4 = null;
        AnimatableFloatValue animatableFloatValue5 = null;
        AnimatableFloatValue animatableFloatValue6 = null;
        while (jsonReader.hasNext()) {
            switch (jsonReader.nextName()) {
                case "nm":
                    strNextString = jsonReader.nextString();
                    break;
                case "sy":
                    typeForValue = PolystarShape.Type.forValue(jsonReader.nextInt());
                    break;
                case "pt":
                    animatableFloatValue = AnimatableValueParser.parseFloat(jsonReader, lottieComposition, false);
                    break;
                case "p":
                    splitPath = AnimatablePathValueParser.parseSplitPath(jsonReader, lottieComposition);
                    break;
                case "r":
                    animatableFloatValue2 = AnimatableValueParser.parseFloat(jsonReader, lottieComposition, false);
                    break;
                case "or":
                    animatableFloatValue4 = AnimatableValueParser.parseFloat(jsonReader, lottieComposition);
                    break;
                case "os":
                    animatableFloatValue6 = AnimatableValueParser.parseFloat(jsonReader, lottieComposition, false);
                    break;
                case "ir":
                    animatableFloatValue3 = AnimatableValueParser.parseFloat(jsonReader, lottieComposition);
                    break;
                case "is":
                    animatableFloatValue5 = AnimatableValueParser.parseFloat(jsonReader, lottieComposition, false);
                    break;
                default:
                    jsonReader.skipValue();
                    break;
            }
        }
        return new PolystarShape(strNextString, typeForValue, animatableFloatValue, splitPath, animatableFloatValue2, animatableFloatValue3, animatableFloatValue4, animatableFloatValue5, animatableFloatValue6);
    }
}
