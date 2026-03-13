package com.airbnb.lottie.parser;

import android.util.JsonReader;
import com.airbnb.lottie.model.DocumentData;
import java.io.IOException;

/* JADX INFO: loaded from: classes.dex */
public class DocumentDataParser implements ValueParser<DocumentData> {
    public static final DocumentDataParser INSTANCE = new DocumentDataParser();

    private DocumentDataParser() {
    }

    /* JADX WARN: Can't rename method to resolve collision */
    /* JADX WARN: Failed to restore switch over string. Please report as a decompilation issue */
    @Override // com.airbnb.lottie.parser.ValueParser
    public DocumentData parse(JsonReader jsonReader, float f) throws IOException {
        byte b;
        jsonReader.beginObject();
        boolean zNextBoolean = true;
        String strNextString = null;
        String strNextString2 = null;
        double dNextDouble = 0.0d;
        double dNextDouble2 = 0.0d;
        double dNextDouble3 = 0.0d;
        int iNextInt = 0;
        int iNextInt2 = 0;
        int iJsonToColor = 0;
        int iJsonToColor2 = 0;
        int iNextInt3 = 0;
        while (jsonReader.hasNext()) {
            String strNextName = jsonReader.nextName();
            int iHashCode = strNextName.hashCode();
            if (iHashCode == 102) {
                b = strNextName.equals("f") ? (byte) 1 : (byte) -1;
            } else if (iHashCode == 106) {
                b = strNextName.equals("j") ? (byte) 3 : (byte) -1;
            } else if (iHashCode == 3261) {
                b = strNextName.equals("fc") ? (byte) 7 : (byte) -1;
            } else if (iHashCode == 3452) {
                b = strNextName.equals("lh") ? (byte) 5 : (byte) -1;
            } else if (iHashCode == 3463) {
                b = strNextName.equals("ls") ? (byte) 6 : (byte) -1;
            } else if (iHashCode == 3543) {
                b = strNextName.equals("of") ? (byte) 10 : (byte) -1;
            } else if (iHashCode == 3664) {
                b = strNextName.equals("sc") ? (byte) 8 : (byte) -1;
            } else if (iHashCode == 3684) {
                b = strNextName.equals("sw") ? (byte) 9 : (byte) -1;
            } else if (iHashCode != 3710) {
                switch (iHashCode) {
                    case 115:
                        b = !strNextName.equals("s") ? (byte) -1 : (byte) 2;
                        break;
                    case 116:
                        b = !strNextName.equals("t") ? (byte) -1 : (byte) 0;
                        break;
                    default:
                        b = -1;
                        break;
                }
            } else {
                b = strNextName.equals("tr") ? (byte) 4 : (byte) -1;
            }
            switch (b) {
                case 0:
                    strNextString = jsonReader.nextString();
                    break;
                case 1:
                    strNextString2 = jsonReader.nextString();
                    break;
                case 2:
                    dNextDouble = jsonReader.nextDouble();
                    break;
                case 3:
                    iNextInt = jsonReader.nextInt();
                    break;
                case 4:
                    iNextInt2 = jsonReader.nextInt();
                    break;
                case 5:
                    dNextDouble2 = jsonReader.nextDouble();
                    break;
                case 6:
                    dNextDouble3 = jsonReader.nextDouble();
                    break;
                case 7:
                    iJsonToColor = JsonUtils.jsonToColor(jsonReader);
                    break;
                case 8:
                    iJsonToColor2 = JsonUtils.jsonToColor(jsonReader);
                    break;
                case 9:
                    iNextInt3 = jsonReader.nextInt();
                    break;
                case 10:
                    zNextBoolean = jsonReader.nextBoolean();
                    break;
                default:
                    jsonReader.skipValue();
                    break;
            }
        }
        jsonReader.endObject();
        return new DocumentData(strNextString, strNextString2, dNextDouble, iNextInt, iNextInt2, dNextDouble2, dNextDouble3, iJsonToColor, iJsonToColor2, iNextInt3, zNextBoolean);
    }
}
