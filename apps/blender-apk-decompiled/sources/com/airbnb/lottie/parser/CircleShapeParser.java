package com.airbnb.lottie.parser;

/* JADX INFO: loaded from: classes.dex */
class CircleShapeParser {
    private CircleShapeParser() {
    }

    /* JADX WARN: Removed duplicated region for block: B:30:0x0054  */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
        To view partially-correct add '--show-bad-code' argument
    */
    static com.airbnb.lottie.model.content.CircleShape parse(android.util.JsonReader r10, com.airbnb.lottie.LottieComposition r11, int r12) throws java.io.IOException {
        /*
            r0 = 0
            r1 = 1
            r2 = 3
            if (r12 != r2) goto L7
            r12 = r1
            goto L8
        L7:
            r12 = r0
        L8:
            r3 = 0
            r5 = r12
            r12 = r3
            r4 = r12
        Lc:
            boolean r6 = r10.hasNext()
            if (r6 == 0) goto L75
            java.lang.String r6 = r10.nextName()
            r7 = -1
            int r8 = r6.hashCode()
            r9 = 100
            if (r8 == r9) goto L4a
            r9 = 112(0x70, float:1.57E-43)
            if (r8 == r9) goto L40
            r9 = 115(0x73, float:1.61E-43)
            if (r8 == r9) goto L36
            r9 = 3519(0xdbf, float:4.931E-42)
            if (r8 == r9) goto L2c
            goto L54
        L2c:
            java.lang.String r8 = "nm"
            boolean r6 = r6.equals(r8)
            if (r6 == 0) goto L54
            r6 = r0
            goto L55
        L36:
            java.lang.String r8 = "s"
            boolean r6 = r6.equals(r8)
            if (r6 == 0) goto L54
            r6 = 2
            goto L55
        L40:
            java.lang.String r8 = "p"
            boolean r6 = r6.equals(r8)
            if (r6 == 0) goto L54
            r6 = r1
            goto L55
        L4a:
            java.lang.String r8 = "d"
            boolean r6 = r6.equals(r8)
            if (r6 == 0) goto L54
            r6 = r2
            goto L55
        L54:
            r6 = r7
        L55:
            switch(r6) {
                case 0: goto L70;
                case 1: goto L6b;
                case 2: goto L66;
                case 3: goto L5c;
                default: goto L58;
            }
        L58:
            r10.skipValue()
            goto Lc
        L5c:
            int r5 = r10.nextInt()
            if (r5 != r2) goto L64
            r5 = r1
            goto Lc
        L64:
            r5 = r0
            goto Lc
        L66:
            com.airbnb.lottie.model.animatable.AnimatablePointValue r4 = com.airbnb.lottie.parser.AnimatableValueParser.parsePoint(r10, r11)
            goto Lc
        L6b:
            com.airbnb.lottie.model.animatable.AnimatableValue r12 = com.airbnb.lottie.parser.AnimatablePathValueParser.parseSplitPath(r10, r11)
            goto Lc
        L70:
            java.lang.String r3 = r10.nextString()
            goto Lc
        L75:
            com.airbnb.lottie.model.content.CircleShape r10 = new com.airbnb.lottie.model.content.CircleShape
            r10.<init>(r3, r12, r4, r5)
            return r10
        */
        throw new UnsupportedOperationException("Method not decompiled: com.airbnb.lottie.parser.CircleShapeParser.parse(android.util.JsonReader, com.airbnb.lottie.LottieComposition, int):com.airbnb.lottie.model.content.CircleShape");
    }
}
