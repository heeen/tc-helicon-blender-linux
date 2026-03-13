package com.airbnb.lottie.parser;

import android.graphics.PointF;
import android.util.JsonReader;
import android.util.JsonToken;
import android.util.Log;
import com.airbnb.lottie.L;
import com.airbnb.lottie.LottieComposition;
import com.airbnb.lottie.model.animatable.AnimatableFloatValue;
import com.airbnb.lottie.model.animatable.AnimatableIntegerValue;
import com.airbnb.lottie.model.animatable.AnimatablePathValue;
import com.airbnb.lottie.model.animatable.AnimatableScaleValue;
import com.airbnb.lottie.model.animatable.AnimatableTransform;
import com.airbnb.lottie.model.animatable.AnimatableValue;
import com.airbnb.lottie.value.ScaleXY;
import java.io.IOException;

/* JADX INFO: loaded from: classes.dex */
public class AnimatableTransformParser {
    private AnimatableTransformParser() {
    }

    public static AnimatableTransform parse(JsonReader jsonReader, LottieComposition lottieComposition) throws IOException {
        boolean z = jsonReader.peek() == JsonToken.BEGIN_OBJECT;
        if (z) {
            jsonReader.beginObject();
        }
        AnimatablePathValue animatablePathValue = null;
        AnimatableScaleValue animatableScaleValue = null;
        AnimatableIntegerValue animatableIntegerValue = null;
        AnimatableValue<PointF, PointF> splitPath = null;
        AnimatableFloatValue animatableFloatValue = null;
        AnimatableFloatValue animatableFloatValue2 = null;
        AnimatableFloatValue animatableFloatValue3 = null;
        while (jsonReader.hasNext()) {
            switch (jsonReader.nextName()) {
                case "a":
                    jsonReader.beginObject();
                    while (jsonReader.hasNext()) {
                        if (jsonReader.nextName().equals("k")) {
                            animatablePathValue = AnimatablePathValueParser.parse(jsonReader, lottieComposition);
                        } else {
                            jsonReader.skipValue();
                        }
                    }
                    jsonReader.endObject();
                    continue;
                    break;
                case "p":
                    splitPath = AnimatablePathValueParser.parseSplitPath(jsonReader, lottieComposition);
                    continue;
                    break;
                case "s":
                    animatableScaleValue = AnimatableValueParser.parseScale(jsonReader, lottieComposition);
                    continue;
                    break;
                case "rz":
                    lottieComposition.addWarning("Lottie doesn't support 3D layers.");
                    break;
                case "r":
                    break;
                case "o":
                    animatableIntegerValue = AnimatableValueParser.parseInteger(jsonReader, lottieComposition);
                    continue;
                    break;
                case "so":
                    animatableFloatValue2 = AnimatableValueParser.parseFloat(jsonReader, lottieComposition, false);
                    continue;
                    break;
                case "eo":
                    animatableFloatValue3 = AnimatableValueParser.parseFloat(jsonReader, lottieComposition, false);
                    continue;
                    break;
                default:
                    jsonReader.skipValue();
                    continue;
                    break;
            }
            animatableFloatValue = AnimatableValueParser.parseFloat(jsonReader, lottieComposition, false);
        }
        if (z) {
            jsonReader.endObject();
        }
        if (animatablePathValue == null) {
            Log.w(L.TAG, "Layer has no transform property. You may be using an unsupported layer type such as a camera.");
            animatablePathValue = new AnimatablePathValue();
        }
        AnimatablePathValue animatablePathValue2 = animatablePathValue;
        if (animatableScaleValue == null) {
            animatableScaleValue = new AnimatableScaleValue(new ScaleXY(1.0f, 1.0f));
        }
        AnimatableScaleValue animatableScaleValue2 = animatableScaleValue;
        if (animatableIntegerValue == null) {
            animatableIntegerValue = new AnimatableIntegerValue();
        }
        return new AnimatableTransform(animatablePathValue2, splitPath, animatableScaleValue2, animatableFloatValue, animatableIntegerValue, animatableFloatValue2, animatableFloatValue3);
    }
}
