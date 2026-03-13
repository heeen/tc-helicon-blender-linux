package android.support.v4.graphics;

import android.content.Context;
import android.content.res.AssetManager;
import android.content.res.Resources;
import android.graphics.Typeface;
import android.graphics.fonts.FontVariationAxis;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;
import android.support.annotation.RestrictTo;
import android.support.v4.content.res.FontResourcesParserCompat;
import android.util.Log;
import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.ByteBuffer;

/* JADX INFO: loaded from: classes.dex */
@RequiresApi(26)
@RestrictTo({RestrictTo.Scope.LIBRARY_GROUP})
public class TypefaceCompatApi26Impl extends TypefaceCompatApi21Impl {
    private static final String ABORT_CREATION_METHOD = "abortCreation";
    private static final String ADD_FONT_FROM_ASSET_MANAGER_METHOD = "addFontFromAssetManager";
    private static final String ADD_FONT_FROM_BUFFER_METHOD = "addFontFromBuffer";
    private static final String CREATE_FROM_FAMILIES_WITH_DEFAULT_METHOD = "createFromFamiliesWithDefault";
    private static final String FONT_FAMILY_CLASS = "android.graphics.FontFamily";
    private static final String FREEZE_METHOD = "freeze";
    private static final int RESOLVE_BY_FONT_TABLE = -1;
    private static final String TAG = "TypefaceCompatApi26Impl";
    private static final Method sAbortCreation;
    private static final Method sAddFontFromAssetManager;
    private static final Method sAddFontFromBuffer;
    private static final Method sCreateFromFamiliesWithDefault;
    private static final Class sFontFamily;
    private static final Constructor sFontFamilyCtor;
    private static final Method sFreeze;

    static {
        Class<?> cls;
        Method declaredMethod;
        Method method;
        Method method2;
        Method method3;
        Method method4;
        Constructor<?> constructor = null;
        try {
            cls = Class.forName(FONT_FAMILY_CLASS);
            Constructor<?> constructor2 = cls.getConstructor(new Class[0]);
            method = cls.getMethod(ADD_FONT_FROM_ASSET_MANAGER_METHOD, AssetManager.class, String.class, Integer.TYPE, Boolean.TYPE, Integer.TYPE, Integer.TYPE, Integer.TYPE, FontVariationAxis[].class);
            method2 = cls.getMethod(ADD_FONT_FROM_BUFFER_METHOD, ByteBuffer.class, Integer.TYPE, FontVariationAxis[].class, Integer.TYPE, Integer.TYPE);
            method3 = cls.getMethod(FREEZE_METHOD, new Class[0]);
            method4 = cls.getMethod(ABORT_CREATION_METHOD, new Class[0]);
            declaredMethod = Typeface.class.getDeclaredMethod(CREATE_FROM_FAMILIES_WITH_DEFAULT_METHOD, Array.newInstance(cls, 1).getClass(), Integer.TYPE, Integer.TYPE);
            declaredMethod.setAccessible(true);
            constructor = constructor2;
        } catch (ClassNotFoundException | NoSuchMethodException e) {
            Log.e(TAG, "Unable to collect necessary methods for class " + e.getClass().getName(), e);
            cls = null;
            declaredMethod = null;
            method = null;
            method2 = null;
            method3 = null;
            method4 = null;
        }
        sFontFamilyCtor = constructor;
        sFontFamily = cls;
        sAddFontFromAssetManager = method;
        sAddFontFromBuffer = method2;
        sFreeze = method3;
        sAbortCreation = method4;
        sCreateFromFamiliesWithDefault = declaredMethod;
    }

    private static boolean isFontFamilyPrivateAPIAvailable() {
        if (sAddFontFromAssetManager == null) {
            Log.w(TAG, "Unable to collect necessary private methods. Fallback to legacy implementation.");
        }
        return sAddFontFromAssetManager != null;
    }

    private static Object newFamily() {
        try {
            return sFontFamilyCtor.newInstance(new Object[0]);
        } catch (IllegalAccessException | InstantiationException | InvocationTargetException e) {
            throw new RuntimeException(e);
        }
    }

    private static boolean addFontFromAssetManager(Context context, Object obj, String str, int i, int i2, int i3) {
        try {
            return ((Boolean) sAddFontFromAssetManager.invoke(obj, context.getAssets(), str, 0, false, Integer.valueOf(i), Integer.valueOf(i2), Integer.valueOf(i3), null)).booleanValue();
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException(e);
        }
    }

    private static boolean addFontFromBuffer(Object obj, ByteBuffer byteBuffer, int i, int i2, int i3) {
        try {
            return ((Boolean) sAddFontFromBuffer.invoke(obj, byteBuffer, Integer.valueOf(i), null, Integer.valueOf(i2), Integer.valueOf(i3))).booleanValue();
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException(e);
        }
    }

    private static Typeface createFromFamiliesWithDefault(Object obj) {
        try {
            Object objNewInstance = Array.newInstance((Class<?>) sFontFamily, 1);
            Array.set(objNewInstance, 0, obj);
            return (Typeface) sCreateFromFamiliesWithDefault.invoke(null, objNewInstance, -1, -1);
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException(e);
        }
    }

    private static boolean freeze(Object obj) {
        try {
            return ((Boolean) sFreeze.invoke(obj, new Object[0])).booleanValue();
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException(e);
        }
    }

    private static void abortCreation(Object obj) {
        try {
            sAbortCreation.invoke(obj, new Object[0]);
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException(e);
        }
    }

    @Override // android.support.v4.graphics.TypefaceCompatBaseImpl, android.support.v4.graphics.TypefaceCompat.TypefaceCompatImpl
    public Typeface createFromFontFamilyFilesResourceEntry(Context context, FontResourcesParserCompat.FontFamilyFilesResourceEntry fontFamilyFilesResourceEntry, Resources resources, int i) {
        if (!isFontFamilyPrivateAPIAvailable()) {
            return super.createFromFontFamilyFilesResourceEntry(context, fontFamilyFilesResourceEntry, resources, i);
        }
        Object objNewFamily = newFamily();
        for (FontResourcesParserCompat.FontFileResourceEntry fontFileResourceEntry : fontFamilyFilesResourceEntry.getEntries()) {
            if (!addFontFromAssetManager(context, objNewFamily, fontFileResourceEntry.getFileName(), 0, fontFileResourceEntry.getWeight(), fontFileResourceEntry.isItalic() ? 1 : 0)) {
                abortCreation(objNewFamily);
                return null;
            }
        }
        if (freeze(objNewFamily)) {
            return createFromFamiliesWithDefault(objNewFamily);
        }
        return null;
    }

    /* JADX WARN: Removed duplicated region for block: B:24:0x0054  */
    /* JADX WARN: Removed duplicated region for block: B:62:? A[Catch: IOException -> 0x0063, SYNTHETIC, TRY_LEAVE, TryCatch #2 {IOException -> 0x0063, blocks: (B:8:0x0014, B:11:0x0022, B:15:0x0045, B:25:0x0056, B:29:0x005f, B:28:0x005b, B:30:0x0062), top: B:53:0x0014, inners: #0 }] */
    @Override // android.support.v4.graphics.TypefaceCompatApi21Impl, android.support.v4.graphics.TypefaceCompatBaseImpl, android.support.v4.graphics.TypefaceCompat.TypefaceCompatImpl
    /*
        Code decompiled incorrectly, please refer to instructions dump.
        To view partially-correct add '--show-bad-code' argument
    */
    public android.graphics.Typeface createFromFontInfo(android.content.Context r9, @android.support.annotation.Nullable android.os.CancellationSignal r10, @android.support.annotation.NonNull android.support.v4.provider.FontsContractCompat.FontInfo[] r11, int r12) throws java.lang.Throwable {
        /*
            r8 = this;
            int r0 = r11.length
            r1 = 1
            r2 = 0
            if (r0 >= r1) goto L6
            return r2
        L6:
            boolean r0 = isFontFamilyPrivateAPIAvailable()
            if (r0 != 0) goto L64
            android.support.v4.provider.FontsContractCompat$FontInfo r8 = r8.findBestInfo(r11, r12)
            android.content.ContentResolver r9 = r9.getContentResolver()
            android.net.Uri r11 = r8.getUri()     // Catch: java.io.IOException -> L63
            java.lang.String r12 = "r"
            android.os.ParcelFileDescriptor r9 = r9.openFileDescriptor(r11, r12, r10)     // Catch: java.io.IOException -> L63
            if (r9 != 0) goto L26
            if (r9 == 0) goto L25
            r9.close()     // Catch: java.io.IOException -> L63
        L25:
            return r2
        L26:
            android.graphics.Typeface$Builder r10 = new android.graphics.Typeface$Builder     // Catch: java.lang.Throwable -> L49 java.lang.Throwable -> L4c
            java.io.FileDescriptor r11 = r9.getFileDescriptor()     // Catch: java.lang.Throwable -> L49 java.lang.Throwable -> L4c
            r10.<init>(r11)     // Catch: java.lang.Throwable -> L49 java.lang.Throwable -> L4c
            int r11 = r8.getWeight()     // Catch: java.lang.Throwable -> L49 java.lang.Throwable -> L4c
            android.graphics.Typeface$Builder r10 = r10.setWeight(r11)     // Catch: java.lang.Throwable -> L49 java.lang.Throwable -> L4c
            boolean r8 = r8.isItalic()     // Catch: java.lang.Throwable -> L49 java.lang.Throwable -> L4c
            android.graphics.Typeface$Builder r8 = r10.setItalic(r8)     // Catch: java.lang.Throwable -> L49 java.lang.Throwable -> L4c
            android.graphics.Typeface r8 = r8.build()     // Catch: java.lang.Throwable -> L49 java.lang.Throwable -> L4c
            if (r9 == 0) goto L48
            r9.close()     // Catch: java.io.IOException -> L63
        L48:
            return r8
        L49:
            r8 = move-exception
            r10 = r2
            goto L52
        L4c:
            r8 = move-exception
            throw r8     // Catch: java.lang.Throwable -> L4e
        L4e:
            r10 = move-exception
            r7 = r10
            r10 = r8
            r8 = r7
        L52:
            if (r9 == 0) goto L62
            if (r10 == 0) goto L5f
            r9.close()     // Catch: java.lang.Throwable -> L5a java.io.IOException -> L63
            goto L62
        L5a:
            r9 = move-exception
            r10.addSuppressed(r9)     // Catch: java.io.IOException -> L63
            goto L62
        L5f:
            r9.close()     // Catch: java.io.IOException -> L63
        L62:
            throw r8     // Catch: java.io.IOException -> L63
        L63:
            return r2
        L64:
            java.util.Map r8 = android.support.v4.provider.FontsContractCompat.prepareFontData(r9, r11, r10)
            java.lang.Object r9 = newFamily()
            int r10 = r11.length
            r0 = 0
            r3 = r0
        L6f:
            if (r0 >= r10) goto L9a
            r4 = r11[r0]
            android.net.Uri r5 = r4.getUri()
            java.lang.Object r5 = r8.get(r5)
            java.nio.ByteBuffer r5 = (java.nio.ByteBuffer) r5
            if (r5 != 0) goto L80
            goto L97
        L80:
            int r3 = r4.getTtcIndex()
            int r6 = r4.getWeight()
            boolean r4 = r4.isItalic()
            boolean r3 = addFontFromBuffer(r9, r5, r3, r6, r4)
            if (r3 != 0) goto L96
            abortCreation(r9)
            return r2
        L96:
            r3 = r1
        L97:
            int r0 = r0 + 1
            goto L6f
        L9a:
            if (r3 != 0) goto La0
            abortCreation(r9)
            return r2
        La0:
            boolean r8 = freeze(r9)
            if (r8 != 0) goto La7
            return r2
        La7:
            android.graphics.Typeface r8 = createFromFamiliesWithDefault(r9)
            android.graphics.Typeface r8 = android.graphics.Typeface.create(r8, r12)
            return r8
        */
        throw new UnsupportedOperationException("Method not decompiled: android.support.v4.graphics.TypefaceCompatApi26Impl.createFromFontInfo(android.content.Context, android.os.CancellationSignal, android.support.v4.provider.FontsContractCompat$FontInfo[], int):android.graphics.Typeface");
    }

    @Override // android.support.v4.graphics.TypefaceCompatBaseImpl, android.support.v4.graphics.TypefaceCompat.TypefaceCompatImpl
    @Nullable
    public Typeface createFromResourcesFontFile(Context context, Resources resources, int i, String str, int i2) {
        if (!isFontFamilyPrivateAPIAvailable()) {
            return super.createFromResourcesFontFile(context, resources, i, str, i2);
        }
        Object objNewFamily = newFamily();
        if (!addFontFromAssetManager(context, objNewFamily, str, 0, -1, -1)) {
            abortCreation(objNewFamily);
            return null;
        }
        if (freeze(objNewFamily)) {
            return createFromFamiliesWithDefault(objNewFamily);
        }
        return null;
    }
}
