package io.github.douglasjunior.androidSimpleTooltip;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.support.annotation.ColorRes;
import android.support.annotation.DrawableRes;
import android.support.annotation.StyleRes;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.FrameLayout;
import android.widget.TextView;

/* JADX INFO: loaded from: classes.dex */
public final class SimpleTooltipUtils {
    private SimpleTooltipUtils() {
    }

    public static RectF calculeRectOnScreen(View view) {
        view.getLocationOnScreen(new int[2]);
        return new RectF(r0[0], r0[1], r0[0] + view.getMeasuredWidth(), r0[1] + view.getMeasuredHeight());
    }

    public static RectF calculeRectInWindow(View view) {
        view.getLocationInWindow(new int[2]);
        return new RectF(r0[0], r0[1], r0[0] + view.getMeasuredWidth(), r0[1] + view.getMeasuredHeight());
    }

    public static float dpFromPx(float f) {
        return f / Resources.getSystem().getDisplayMetrics().density;
    }

    public static float pxFromDp(float f) {
        return f * Resources.getSystem().getDisplayMetrics().density;
    }

    public static void setWidth(View view, float f) {
        ViewGroup.LayoutParams layoutParams = view.getLayoutParams();
        if (layoutParams == null) {
            layoutParams = new ViewGroup.LayoutParams((int) f, view.getHeight());
        } else {
            layoutParams.width = (int) f;
        }
        view.setLayoutParams(layoutParams);
    }

    public static int tooltipGravityToArrowDirection(int i) {
        if (i == 17) {
            return 1;
        }
        if (i == 48) {
            return 3;
        }
        if (i == 80) {
            return 1;
        }
        if (i == 8388611) {
            return 2;
        }
        if (i == 8388613) {
            return 0;
        }
        throw new IllegalArgumentException("Gravity must have be CENTER, START, END, TOP or BOTTOM.");
    }

    public static void setX(View view, int i) {
        if (Build.VERSION.SDK_INT >= 11) {
            view.setX(i);
            return;
        }
        ViewGroup.MarginLayoutParams orCreateMarginLayoutParams = getOrCreateMarginLayoutParams(view);
        orCreateMarginLayoutParams.leftMargin = i - view.getLeft();
        view.setLayoutParams(orCreateMarginLayoutParams);
    }

    public static void setY(View view, int i) {
        if (Build.VERSION.SDK_INT >= 11) {
            view.setY(i);
            return;
        }
        ViewGroup.MarginLayoutParams orCreateMarginLayoutParams = getOrCreateMarginLayoutParams(view);
        orCreateMarginLayoutParams.topMargin = i - view.getTop();
        view.setLayoutParams(orCreateMarginLayoutParams);
    }

    private static ViewGroup.MarginLayoutParams getOrCreateMarginLayoutParams(View view) {
        ViewGroup.LayoutParams layoutParams = view.getLayoutParams();
        if (layoutParams != null) {
            if (layoutParams instanceof ViewGroup.MarginLayoutParams) {
                return (ViewGroup.MarginLayoutParams) layoutParams;
            }
            return new ViewGroup.MarginLayoutParams(layoutParams);
        }
        return new ViewGroup.MarginLayoutParams(view.getWidth(), view.getHeight());
    }

    public static void removeOnGlobalLayoutListener(View view, ViewTreeObserver.OnGlobalLayoutListener onGlobalLayoutListener) {
        if (Build.VERSION.SDK_INT >= 16) {
            view.getViewTreeObserver().removeOnGlobalLayoutListener(onGlobalLayoutListener);
        } else {
            view.getViewTreeObserver().removeGlobalOnLayoutListener(onGlobalLayoutListener);
        }
    }

    public static void setTextAppearance(TextView textView, @StyleRes int i) {
        if (Build.VERSION.SDK_INT >= 23) {
            textView.setTextAppearance(i);
        } else {
            textView.setTextAppearance(textView.getContext(), i);
        }
    }

    public static int getColor(Context context, @ColorRes int i) {
        if (Build.VERSION.SDK_INT >= 23) {
            return context.getColor(i);
        }
        return context.getResources().getColor(i);
    }

    public static Drawable getDrawable(Context context, @DrawableRes int i) {
        if (Build.VERSION.SDK_INT >= 23) {
            return context.getDrawable(i);
        }
        return context.getResources().getDrawable(i);
    }

    public static ViewGroup findFrameLayout(View view) {
        ViewGroup viewGroup = (ViewGroup) view.getRootView();
        return (viewGroup.getChildCount() == 1 && (viewGroup.getChildAt(0) instanceof FrameLayout)) ? (ViewGroup) viewGroup.getChildAt(0) : viewGroup;
    }
}
