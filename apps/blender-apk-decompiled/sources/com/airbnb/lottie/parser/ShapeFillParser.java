package com.airbnb.lottie.parser;

/* JADX INFO: loaded from: classes.dex */
class ShapeFillParser {
    private ShapeFillParser() {
    }

    /* JADX WARN: Removed duplicated region for block: B:31:0x005f  */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
        To view partially-correct add '--show-bad-code' argument
    */
    static com.airbnb.lottie.model.content.ShapeFill parse(android.util.JsonReader r11, com.airbnb.lottie.LottieComposition r12) throws java.io.IOException {
        /*
            r0 = 0
            r1 = 1
            r2 = 0
            r5 = r0
            r4 = r2
            r7 = r4
            r8 = r7
            r2 = r1
        L8:
            boolean r3 = r11.hasNext()
            if (r3 == 0) goto L84
            java.lang.String r3 = r11.nextName()
            r6 = -1
            int r9 = r3.hashCode()
            r10 = -396065730(0xffffffffe864843e, float:-4.316556E24)
            if (r9 == r10) goto L55
            r10 = 99
            if (r9 == r10) goto L4b
            r10 = 111(0x6f, float:1.56E-43)
            if (r9 == r10) goto L41
            r10 = 114(0x72, float:1.6E-43)
            if (r9 == r10) goto L37
            r10 = 3519(0xdbf, float:4.931E-42)
            if (r9 == r10) goto L2d
            goto L5f
        L2d:
            java.lang.String r9 = "nm"
            boolean r3 = r3.equals(r9)
            if (r3 == 0) goto L5f
            r3 = r0
            goto L60
        L37:
            java.lang.String r9 = "r"
            boolean r3 = r3.equals(r9)
            if (r3 == 0) goto L5f
            r3 = 4
            goto L60
        L41:
            java.lang.String r9 = "o"
            boolean r3 = r3.equals(r9)
            if (r3 == 0) goto L5f
            r3 = 2
            goto L60
        L4b:
            java.lang.String r9 = "c"
            boolean r3 = r3.equals(r9)
            if (r3 == 0) goto L5f
            r3 = r1
            goto L60
        L55:
            java.lang.String r9 = "fillEnabled"
            boolean r3 = r3.equals(r9)
            if (r3 == 0) goto L5f
            r3 = 3
            goto L60
        L5f:
            r3 = r6
        L60:
            switch(r3) {
                case 0: goto L7e;
                case 1: goto L78;
                case 2: goto L72;
                case 3: goto L6c;
                case 4: goto L67;
                default: goto L63;
            }
        L63:
            r11.skipValue()
            goto L8
        L67:
            int r2 = r11.nextInt()
            goto L8
        L6c:
            boolean r3 = r11.nextBoolean()
            r5 = r3
            goto L8
        L72:
            com.airbnb.lottie.model.animatable.AnimatableIntegerValue r3 = com.airbnb.lottie.parser.AnimatableValueParser.parseInteger(r11, r12)
            r8 = r3
            goto L8
        L78:
            com.airbnb.lottie.model.animatable.AnimatableColorValue r3 = com.airbnb.lottie.parser.AnimatableValueParser.parseColor(r11, r12)
            r7 = r3
            goto L8
        L7e:
            java.lang.String r3 = r11.nextString()
            r4 = r3
            goto L8
        L84:
            if (r2 != r1) goto L8a
            android.graphics.Path$FillType r11 = android.graphics.Path.FillType.WINDING
        L88:
            r6 = r11
            goto L8d
        L8a:
            android.graphics.Path$FillType r11 = android.graphics.Path.FillType.EVEN_ODD
            goto L88
        L8d:
            com.airbnb.lottie.model.content.ShapeFill r11 = new com.airbnb.lottie.model.content.ShapeFill
            r3 = r11
            r3.<init>(r4, r5, r6, r7, r8)
            return r11
        */
        throw new UnsupportedOperationException("Method not decompiled: com.airbnb.lottie.parser.ShapeFillParser.parse(android.util.JsonReader, com.airbnb.lottie.LottieComposition):com.airbnb.lottie.model.content.ShapeFill");
    }
}
