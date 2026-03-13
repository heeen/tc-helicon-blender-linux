package com.flurry.sdk;

import android.annotation.SuppressLint;
import android.app.KeyguardManager;
import android.graphics.Point;
import android.os.Build;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.WindowManager;
import java.lang.reflect.Method;

/* JADX INFO: loaded from: classes.dex */
public final class lp {
    public static boolean a() {
        return ((KeyguardManager) jr.a().a.getSystemService("keyguard")).inKeyguardRestrictedInputMode();
    }

    @SuppressLint({"NewApi"})
    public static Point b() {
        Display defaultDisplay = ((WindowManager) jr.a().a.getSystemService("window")).getDefaultDisplay();
        Point point = new Point();
        if (Build.VERSION.SDK_INT >= 17) {
            defaultDisplay.getRealSize(point);
        } else if (Build.VERSION.SDK_INT >= 14) {
            try {
                Method method = Display.class.getMethod("getRawHeight", new Class[0]);
                point.x = ((Integer) Display.class.getMethod("getRawWidth", new Class[0]).invoke(defaultDisplay, new Object[0])).intValue();
                point.y = ((Integer) method.invoke(defaultDisplay, new Object[0])).intValue();
            } catch (Throwable unused) {
                defaultDisplay.getSize(point);
            }
        } else if (Build.VERSION.SDK_INT >= 13) {
            defaultDisplay.getSize(point);
        } else {
            point.x = defaultDisplay.getWidth();
            point.y = defaultDisplay.getHeight();
        }
        return point;
    }

    public static DisplayMetrics c() {
        Display defaultDisplay = ((WindowManager) jr.a().a.getSystemService("window")).getDefaultDisplay();
        DisplayMetrics displayMetrics = new DisplayMetrics();
        defaultDisplay.getMetrics(displayMetrics);
        return displayMetrics;
    }

    @SuppressLint({"NewApi"})
    public static DisplayMetrics d() {
        Display defaultDisplay = ((WindowManager) jr.a().a.getSystemService("window")).getDefaultDisplay();
        if (Build.VERSION.SDK_INT >= 17) {
            DisplayMetrics displayMetrics = new DisplayMetrics();
            defaultDisplay.getRealMetrics(displayMetrics);
            return displayMetrics;
        }
        if (Build.VERSION.SDK_INT >= 14) {
            try {
                DisplayMetrics displayMetrics2 = new DisplayMetrics();
                Display.class.getMethod("getRealMetrics", new Class[0]).invoke(defaultDisplay, displayMetrics2);
                return displayMetrics2;
            } catch (Exception unused) {
            }
        }
        return c();
    }

    public static int a(int i) {
        return Math.round(i / d().density);
    }

    public static int b(int i) {
        return Math.round(i * d().density);
    }

    public static int e() {
        Point pointB = b();
        if (pointB.x == pointB.y) {
            return 3;
        }
        return pointB.x < pointB.y ? 1 : 2;
    }
}
