package android.support.transition;

import android.animation.LayoutTransition;
import android.support.annotation.NonNull;
import android.support.annotation.RequiresApi;
import android.util.Log;
import android.view.ViewGroup;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/* JADX INFO: loaded from: classes.dex */
@RequiresApi(14)
class ViewGroupUtilsApi14 implements ViewGroupUtilsImpl {
    private static final int LAYOUT_TRANSITION_CHANGING = 4;
    private static final String TAG = "ViewGroupUtilsApi14";
    private static Method sCancelMethod;
    private static boolean sCancelMethodFetched;
    private static LayoutTransition sEmptyLayoutTransition;
    private static Field sLayoutSuppressedField;
    private static boolean sLayoutSuppressedFieldFetched;

    ViewGroupUtilsApi14() {
    }

    @Override // android.support.transition.ViewGroupUtilsImpl
    public ViewGroupOverlayImpl getOverlay(@NonNull ViewGroup viewGroup) {
        return ViewGroupOverlayApi14.createFrom(viewGroup);
    }

    @Override // android.support.transition.ViewGroupUtilsImpl
    public void suppressLayout(@NonNull ViewGroup viewGroup, boolean z) {
        boolean z2 = false;
        if (sEmptyLayoutTransition == null) {
            sEmptyLayoutTransition = new LayoutTransition() { // from class: android.support.transition.ViewGroupUtilsApi14.1
                @Override // android.animation.LayoutTransition
                public boolean isChangingLayout() {
                    return true;
                }
            };
            sEmptyLayoutTransition.setAnimator(2, null);
            sEmptyLayoutTransition.setAnimator(0, null);
            sEmptyLayoutTransition.setAnimator(1, null);
            sEmptyLayoutTransition.setAnimator(3, null);
            sEmptyLayoutTransition.setAnimator(4, null);
        }
        if (z) {
            LayoutTransition layoutTransition = viewGroup.getLayoutTransition();
            if (layoutTransition != null) {
                if (layoutTransition.isRunning()) {
                    cancelLayoutTransition(layoutTransition);
                }
                if (layoutTransition != sEmptyLayoutTransition) {
                    viewGroup.setTag(R.id.transition_layout_save, layoutTransition);
                }
            }
            viewGroup.setLayoutTransition(sEmptyLayoutTransition);
            return;
        }
        viewGroup.setLayoutTransition(null);
        if (!sLayoutSuppressedFieldFetched) {
            try {
                sLayoutSuppressedField = ViewGroup.class.getDeclaredField("mLayoutSuppressed");
                sLayoutSuppressedField.setAccessible(true);
            } catch (NoSuchFieldException unused) {
                Log.i(TAG, "Failed to access mLayoutSuppressed field by reflection");
            }
            sLayoutSuppressedFieldFetched = true;
        }
        if (sLayoutSuppressedField != null) {
            try {
                boolean z3 = sLayoutSuppressedField.getBoolean(viewGroup);
                if (z3) {
                    try {
                        sLayoutSuppressedField.setBoolean(viewGroup, false);
                    } catch (IllegalAccessException unused2) {
                        z2 = z3;
                        Log.i(TAG, "Failed to get mLayoutSuppressed field by reflection");
                    }
                }
                z2 = z3;
            } catch (IllegalAccessException unused3) {
            }
        }
        if (z2) {
            viewGroup.requestLayout();
        }
        LayoutTransition layoutTransition2 = (LayoutTransition) viewGroup.getTag(R.id.transition_layout_save);
        if (layoutTransition2 != null) {
            viewGroup.setTag(R.id.transition_layout_save, null);
            viewGroup.setLayoutTransition(layoutTransition2);
        }
    }

    private static void cancelLayoutTransition(LayoutTransition layoutTransition) {
        if (!sCancelMethodFetched) {
            try {
                sCancelMethod = LayoutTransition.class.getDeclaredMethod("cancel", new Class[0]);
                sCancelMethod.setAccessible(true);
            } catch (NoSuchMethodException unused) {
                Log.i(TAG, "Failed to access cancel method by reflection");
            }
            sCancelMethodFetched = true;
        }
        if (sCancelMethod != null) {
            try {
                sCancelMethod.invoke(layoutTransition, new Object[0]);
            } catch (IllegalAccessException unused2) {
                Log.i(TAG, "Failed to access cancel method by reflection");
            } catch (InvocationTargetException unused3) {
                Log.i(TAG, "Failed to invoke cancel method by reflection");
            }
        }
    }
}
