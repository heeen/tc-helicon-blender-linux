package com.airbnb.lottie.parser;

import android.graphics.Color;
import android.support.annotation.IntRange;
import android.support.v4.media.MediaDescriptionCompat;
import android.util.JsonReader;
import android.util.JsonToken;
import com.airbnb.lottie.model.content.GradientColor;
import com.airbnb.lottie.utils.MiscUtils;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/* JADX INFO: loaded from: classes.dex */
public class GradientColorParser implements ValueParser<GradientColor> {
    private int colorPoints;

    public GradientColorParser(int i) {
        this.colorPoints = i;
    }

    /* JADX WARN: Can't rename method to resolve collision */
    @Override // com.airbnb.lottie.parser.ValueParser
    public GradientColor parse(JsonReader jsonReader, float f) throws IOException {
        ArrayList arrayList = new ArrayList();
        boolean z = jsonReader.peek() == JsonToken.BEGIN_ARRAY;
        if (z) {
            jsonReader.beginArray();
        }
        while (jsonReader.hasNext()) {
            arrayList.add(Float.valueOf((float) jsonReader.nextDouble()));
        }
        if (z) {
            jsonReader.endArray();
        }
        if (this.colorPoints == -1) {
            this.colorPoints = arrayList.size() / 4;
        }
        float[] fArr = new float[this.colorPoints];
        int[] iArr = new int[this.colorPoints];
        int i = 0;
        int i2 = 0;
        for (int i3 = 0; i3 < this.colorPoints * 4; i3++) {
            int i4 = i3 / 4;
            double dFloatValue = arrayList.get(i3).floatValue();
            switch (i3 % 4) {
                case 0:
                    fArr[i4] = (float) dFloatValue;
                    break;
                case 1:
                    i = (int) (dFloatValue * 255.0d);
                    break;
                case 2:
                    i2 = (int) (dFloatValue * 255.0d);
                    break;
                case 3:
                    iArr[i4] = Color.argb(255, i, i2, (int) (dFloatValue * 255.0d));
                    break;
            }
        }
        GradientColor gradientColor = new GradientColor(fArr, iArr);
        addOpacityStopsToGradientIfNeeded(gradientColor, arrayList);
        return gradientColor;
    }

    private void addOpacityStopsToGradientIfNeeded(GradientColor gradientColor, List<Float> list) {
        int i = this.colorPoints * 4;
        if (list.size() <= i) {
            return;
        }
        int size = (list.size() - i) / 2;
        double[] dArr = new double[size];
        double[] dArr2 = new double[size];
        int i2 = 0;
        while (i < list.size()) {
            if (i % 2 == 0) {
                dArr[i2] = list.get(i).floatValue();
            } else {
                dArr2[i2] = list.get(i).floatValue();
                i2++;
            }
            i++;
        }
        for (int i3 = 0; i3 < gradientColor.getSize(); i3++) {
            int i4 = gradientColor.getColors()[i3];
            gradientColor.getColors()[i3] = Color.argb(getOpacityAtPosition(gradientColor.getPositions()[i3], dArr, dArr2), Color.red(i4), Color.green(i4), Color.blue(i4));
        }
    }

    @IntRange(from = MediaDescriptionCompat.BT_FOLDER_TYPE_MIXED, to = 255)
    private int getOpacityAtPosition(double d, double[] dArr, double[] dArr2) {
        for (int i = 1; i < dArr.length; i++) {
            int i2 = i - 1;
            double d2 = dArr[i2];
            double d3 = dArr[i];
            if (dArr[i] >= d) {
                return (int) (MiscUtils.lerp(dArr2[i2], dArr2[i], (d - d2) / (d3 - d2)) * 255.0d);
            }
        }
        return (int) (dArr2[dArr2.length - 1] * 255.0d);
    }
}
