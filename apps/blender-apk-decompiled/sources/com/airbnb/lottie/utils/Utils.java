package com.airbnb.lottie.utils;

import android.content.res.Resources;
import android.graphics.Matrix;
import android.graphics.Path;
import android.graphics.PathMeasure;
import android.graphics.PointF;
import android.support.annotation.Nullable;
import com.airbnb.lottie.L;
import com.airbnb.lottie.animation.content.TrimPathContent;
import java.io.Closeable;

/* JADX INFO: loaded from: classes.dex */
public final class Utils {
    public static final int SECOND_IN_NANOS = 1000000000;
    private static final PathMeasure pathMeasure = new PathMeasure();
    private static final Path tempPath = new Path();
    private static final Path tempPath2 = new Path();
    private static final float[] points = new float[4];
    private static final float SQRT_2 = (float) Math.sqrt(2.0d);
    private static float dpScale = -1.0f;

    public static int hashFor(float f, float f2, float f3, float f4) {
        int i = f != 0.0f ? (int) (527 * f) : 17;
        if (f2 != 0.0f) {
            i = (int) (i * 31 * f2);
        }
        if (f3 != 0.0f) {
            i = (int) (i * 31 * f3);
        }
        return f4 != 0.0f ? (int) (i * 31 * f4) : i;
    }

    public static boolean isAtLeastVersion(int i, int i2, int i3, int i4, int i5, int i6) {
        if (i < i4) {
            return false;
        }
        if (i > i4) {
            return true;
        }
        if (i2 < i5) {
            return false;
        }
        return i2 > i5 || i3 >= i6;
    }

    private Utils() {
    }

    public static Path createPath(PointF pointF, PointF pointF2, PointF pointF3, PointF pointF4) {
        Path path = new Path();
        path.moveTo(pointF.x, pointF.y);
        if (pointF3 != null && pointF4 != null && (pointF3.length() != 0.0f || pointF4.length() != 0.0f)) {
            path.cubicTo(pointF3.x + pointF.x, pointF.y + pointF3.y, pointF2.x + pointF4.x, pointF2.y + pointF4.y, pointF2.x, pointF2.y);
        } else {
            path.lineTo(pointF2.x, pointF2.y);
        }
        return path;
    }

    public static void closeQuietly(Closeable closeable) {
        if (closeable != null) {
            try {
                closeable.close();
            } catch (RuntimeException e) {
                throw e;
            } catch (Exception unused) {
            }
        }
    }

    public static float getScale(Matrix matrix) {
        points[0] = 0.0f;
        points[1] = 0.0f;
        points[2] = SQRT_2;
        points[3] = SQRT_2;
        matrix.mapPoints(points);
        return ((float) Math.hypot(points[2] - points[0], points[3] - points[1])) / 2.0f;
    }

    public static void applyTrimPathIfNeeded(Path path, @Nullable TrimPathContent trimPathContent) {
        if (trimPathContent == null) {
            return;
        }
        applyTrimPathIfNeeded(path, trimPathContent.getStart().getValue().floatValue() / 100.0f, trimPathContent.getEnd().getValue().floatValue() / 100.0f, trimPathContent.getOffset().getValue().floatValue() / 360.0f);
    }

    public static void applyTrimPathIfNeeded(Path path, float f, float f2, float f3) {
        L.beginSection("applyTrimPathIfNeeded");
        pathMeasure.setPath(path, false);
        float length = pathMeasure.getLength();
        if (f == 1.0f && f2 == 0.0f) {
            L.endSection("applyTrimPathIfNeeded");
            return;
        }
        if (length < 1.0f || Math.abs((f2 - f) - 1.0f) < 0.01d) {
            L.endSection("applyTrimPathIfNeeded");
            return;
        }
        float f4 = f * length;
        float f5 = f2 * length;
        float f6 = f3 * length;
        float fMin = Math.min(f4, f5) + f6;
        float fMax = Math.max(f4, f5) + f6;
        if (fMin >= length && fMax >= length) {
            fMin = MiscUtils.floorMod(fMin, length);
            fMax = MiscUtils.floorMod(fMax, length);
        }
        if (fMin < 0.0f) {
            fMin = MiscUtils.floorMod(fMin, length);
        }
        if (fMax < 0.0f) {
            fMax = MiscUtils.floorMod(fMax, length);
        }
        if (fMin == fMax) {
            path.reset();
            L.endSection("applyTrimPathIfNeeded");
            return;
        }
        if (fMin >= fMax) {
            fMin -= length;
        }
        tempPath.reset();
        pathMeasure.getSegment(fMin, fMax, tempPath, true);
        if (fMax > length) {
            tempPath2.reset();
            pathMeasure.getSegment(0.0f, fMax % length, tempPath2, true);
            tempPath.addPath(tempPath2);
        } else if (fMin < 0.0f) {
            tempPath2.reset();
            pathMeasure.getSegment(fMin + length, length, tempPath2, true);
            tempPath.addPath(tempPath2);
        }
        path.set(tempPath);
        L.endSection("applyTrimPathIfNeeded");
    }

    public static float dpScale() {
        if (dpScale == -1.0f) {
            dpScale = Resources.getSystem().getDisplayMetrics().density;
        }
        return dpScale;
    }
}
