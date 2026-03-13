package io.github.douglasjunior.androidSimpleTooltip;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.PointF;
import android.graphics.RectF;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.support.annotation.ColorInt;
import android.support.annotation.DimenRes;
import android.support.annotation.Dimension;
import android.support.annotation.DrawableRes;
import android.support.annotation.IdRes;
import android.support.annotation.LayoutRes;
import android.support.annotation.StringRes;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.TextView;

/* JADX INFO: loaded from: classes.dex */
public class SimpleTooltip implements PopupWindow.OnDismissListener {
    private static final String TAG = "SimpleTooltip";
    private static final int mDefaultPopupWindowStyleRes = 16842870;
    private boolean dismissed;
    private final View mAnchorView;
    private final boolean mAnimated;
    private final long mAnimationDuration;
    private final ViewTreeObserver.OnGlobalLayoutListener mAnimationLayoutListener;
    private final float mAnimationPadding;
    private AnimatorSet mAnimator;
    private final int mArrowDirection;
    private final Drawable mArrowDrawable;
    private final float mArrowHeight;
    private final ViewTreeObserver.OnGlobalLayoutListener mArrowLayoutListener;
    private ImageView mArrowView;
    private final float mArrowWidth;
    private final ViewTreeObserver.OnGlobalLayoutListener mAutoDismissLayoutListener;
    private View mContentLayout;
    private final View mContentView;
    private final Context mContext;
    private final boolean mDismissOnInsideTouch;
    private final boolean mDismissOnOutsideTouch;
    private final boolean mFocusable;
    private final int mGravity;
    private int mHighlightShape;
    private final ViewTreeObserver.OnGlobalLayoutListener mLocationLayoutListener;
    private final float mMargin;
    private final float mMaxWidth;
    private final boolean mModal;
    private OnDismissListener mOnDismissListener;
    private OnShowListener mOnShowListener;
    private View mOverlay;
    private final boolean mOverlayMatchParent;
    private final float mOverlayOffset;
    private final View.OnTouchListener mOverlayTouchListener;
    private final float mPadding;
    private PopupWindow mPopupWindow;
    private ViewGroup mRootView;
    private final boolean mShowArrow;
    private final ViewTreeObserver.OnGlobalLayoutListener mShowLayoutListener;
    private final CharSequence mText;

    @IdRes
    private final int mTextViewId;
    private final boolean mTransparentOverlay;
    private static final int mDefaultTextAppearanceRes = R.style.simpletooltip_default;
    private static final int mDefaultBackgroundColorRes = R.color.simpletooltip_background;
    private static final int mDefaultTextColorRes = R.color.simpletooltip_text;
    private static final int mDefaultArrowColorRes = R.color.simpletooltip_arrow;
    private static final int mDefaultMarginRes = R.dimen.simpletooltip_margin;
    private static final int mDefaultPaddingRes = R.dimen.simpletooltip_padding;
    private static final int mDefaultAnimationPaddingRes = R.dimen.simpletooltip_animation_padding;
    private static final int mDefaultAnimationDurationRes = R.integer.simpletooltip_animation_duration;
    private static final int mDefaultArrowWidthRes = R.dimen.simpletooltip_arrow_width;
    private static final int mDefaultArrowHeightRes = R.dimen.simpletooltip_arrow_height;
    private static final int mDefaultOverlayOffsetRes = R.dimen.simpletooltip_overlay_offset;

    public interface OnDismissListener {
        void onDismiss(SimpleTooltip simpleTooltip);
    }

    public interface OnShowListener {
        void onShow(SimpleTooltip simpleTooltip);
    }

    private SimpleTooltip(Builder builder) {
        this.dismissed = false;
        this.mHighlightShape = 0;
        this.mOverlayTouchListener = new View.OnTouchListener() { // from class: io.github.douglasjunior.androidSimpleTooltip.SimpleTooltip.3
            @Override // android.view.View.OnTouchListener
            public boolean onTouch(View view, MotionEvent motionEvent) {
                return SimpleTooltip.this.mModal;
            }
        };
        this.mLocationLayoutListener = new ViewTreeObserver.OnGlobalLayoutListener() { // from class: io.github.douglasjunior.androidSimpleTooltip.SimpleTooltip.4
            @Override // android.view.ViewTreeObserver.OnGlobalLayoutListener
            public void onGlobalLayout() {
                PopupWindow popupWindow = SimpleTooltip.this.mPopupWindow;
                if (popupWindow == null || SimpleTooltip.this.dismissed) {
                    return;
                }
                if (SimpleTooltip.this.mMaxWidth > 0.0f && SimpleTooltip.this.mContentView.getWidth() > SimpleTooltip.this.mMaxWidth) {
                    SimpleTooltipUtils.setWidth(SimpleTooltip.this.mContentView, SimpleTooltip.this.mMaxWidth);
                    popupWindow.update(-2, -2);
                    return;
                }
                SimpleTooltipUtils.removeOnGlobalLayoutListener(popupWindow.getContentView(), this);
                popupWindow.getContentView().getViewTreeObserver().addOnGlobalLayoutListener(SimpleTooltip.this.mArrowLayoutListener);
                PointF pointFCalculePopupLocation = SimpleTooltip.this.calculePopupLocation();
                popupWindow.setClippingEnabled(true);
                popupWindow.update((int) pointFCalculePopupLocation.x, (int) pointFCalculePopupLocation.y, popupWindow.getWidth(), popupWindow.getHeight());
                popupWindow.getContentView().requestLayout();
                SimpleTooltip.this.createOverlay();
            }
        };
        this.mArrowLayoutListener = new ViewTreeObserver.OnGlobalLayoutListener() { // from class: io.github.douglasjunior.androidSimpleTooltip.SimpleTooltip.5
            @Override // android.view.ViewTreeObserver.OnGlobalLayoutListener
            public void onGlobalLayout() {
                float fWidth;
                float top;
                PopupWindow popupWindow = SimpleTooltip.this.mPopupWindow;
                if (popupWindow == null || SimpleTooltip.this.dismissed) {
                    return;
                }
                SimpleTooltipUtils.removeOnGlobalLayoutListener(popupWindow.getContentView(), this);
                popupWindow.getContentView().getViewTreeObserver().addOnGlobalLayoutListener(SimpleTooltip.this.mAnimationLayoutListener);
                popupWindow.getContentView().getViewTreeObserver().addOnGlobalLayoutListener(SimpleTooltip.this.mShowLayoutListener);
                if (SimpleTooltip.this.mShowArrow) {
                    RectF rectFCalculeRectOnScreen = SimpleTooltipUtils.calculeRectOnScreen(SimpleTooltip.this.mAnchorView);
                    RectF rectFCalculeRectOnScreen2 = SimpleTooltipUtils.calculeRectOnScreen(SimpleTooltip.this.mContentLayout);
                    if (SimpleTooltip.this.mArrowDirection == 1 || SimpleTooltip.this.mArrowDirection == 3) {
                        float paddingLeft = SimpleTooltip.this.mContentLayout.getPaddingLeft() + SimpleTooltipUtils.pxFromDp(2.0f);
                        float fWidth2 = ((rectFCalculeRectOnScreen2.width() / 2.0f) - (SimpleTooltip.this.mArrowView.getWidth() / 2.0f)) - (rectFCalculeRectOnScreen2.centerX() - rectFCalculeRectOnScreen.centerX());
                        if (fWidth2 > paddingLeft) {
                            fWidth = (((float) SimpleTooltip.this.mArrowView.getWidth()) + fWidth2) + paddingLeft > rectFCalculeRectOnScreen2.width() ? (rectFCalculeRectOnScreen2.width() - SimpleTooltip.this.mArrowView.getWidth()) - paddingLeft : fWidth2;
                        } else {
                            fWidth = paddingLeft;
                        }
                        top = (SimpleTooltip.this.mArrowDirection != 3 ? 1 : -1) + SimpleTooltip.this.mArrowView.getTop();
                    } else {
                        top = SimpleTooltip.this.mContentLayout.getPaddingTop() + SimpleTooltipUtils.pxFromDp(2.0f);
                        float fHeight = ((rectFCalculeRectOnScreen2.height() / 2.0f) - (SimpleTooltip.this.mArrowView.getHeight() / 2.0f)) - (rectFCalculeRectOnScreen2.centerY() - rectFCalculeRectOnScreen.centerY());
                        if (fHeight > top) {
                            top = (((float) SimpleTooltip.this.mArrowView.getHeight()) + fHeight) + top > rectFCalculeRectOnScreen2.height() ? (rectFCalculeRectOnScreen2.height() - SimpleTooltip.this.mArrowView.getHeight()) - top : fHeight;
                        }
                        fWidth = SimpleTooltip.this.mArrowView.getLeft() + (SimpleTooltip.this.mArrowDirection != 2 ? 1 : -1);
                    }
                    SimpleTooltipUtils.setX(SimpleTooltip.this.mArrowView, (int) fWidth);
                    SimpleTooltipUtils.setY(SimpleTooltip.this.mArrowView, (int) top);
                }
                popupWindow.getContentView().requestLayout();
            }
        };
        this.mShowLayoutListener = new ViewTreeObserver.OnGlobalLayoutListener() { // from class: io.github.douglasjunior.androidSimpleTooltip.SimpleTooltip.6
            @Override // android.view.ViewTreeObserver.OnGlobalLayoutListener
            public void onGlobalLayout() {
                PopupWindow popupWindow = SimpleTooltip.this.mPopupWindow;
                if (popupWindow == null || SimpleTooltip.this.dismissed) {
                    return;
                }
                SimpleTooltipUtils.removeOnGlobalLayoutListener(popupWindow.getContentView(), this);
                if (SimpleTooltip.this.mOnShowListener != null) {
                    SimpleTooltip.this.mOnShowListener.onShow(SimpleTooltip.this);
                }
                SimpleTooltip.this.mOnShowListener = null;
                SimpleTooltip.this.mContentLayout.setVisibility(0);
            }
        };
        this.mAnimationLayoutListener = new ViewTreeObserver.OnGlobalLayoutListener() { // from class: io.github.douglasjunior.androidSimpleTooltip.SimpleTooltip.7
            @Override // android.view.ViewTreeObserver.OnGlobalLayoutListener
            public void onGlobalLayout() {
                PopupWindow popupWindow = SimpleTooltip.this.mPopupWindow;
                if (popupWindow == null || SimpleTooltip.this.dismissed) {
                    return;
                }
                SimpleTooltipUtils.removeOnGlobalLayoutListener(popupWindow.getContentView(), this);
                if (SimpleTooltip.this.mAnimated) {
                    SimpleTooltip.this.startAnimation();
                }
                popupWindow.getContentView().requestLayout();
            }
        };
        this.mAutoDismissLayoutListener = new ViewTreeObserver.OnGlobalLayoutListener() { // from class: io.github.douglasjunior.androidSimpleTooltip.SimpleTooltip.9
            @Override // android.view.ViewTreeObserver.OnGlobalLayoutListener
            public void onGlobalLayout() {
                if (SimpleTooltip.this.mPopupWindow == null || SimpleTooltip.this.dismissed || SimpleTooltip.this.mRootView.isShown()) {
                    return;
                }
                SimpleTooltip.this.dismiss();
            }
        };
        this.mContext = builder.context;
        this.mGravity = builder.gravity;
        this.mArrowDirection = builder.arrowDirection;
        this.mDismissOnInsideTouch = builder.dismissOnInsideTouch;
        this.mDismissOnOutsideTouch = builder.dismissOnOutsideTouch;
        this.mModal = builder.modal;
        this.mContentView = builder.contentView;
        this.mTextViewId = builder.textViewId;
        this.mText = builder.text;
        this.mAnchorView = builder.anchorView;
        this.mTransparentOverlay = builder.transparentOverlay;
        this.mOverlayOffset = builder.overlayOffset;
        this.mOverlayMatchParent = builder.overlayMatchParent;
        this.mMaxWidth = builder.maxWidth;
        this.mShowArrow = builder.showArrow;
        this.mArrowWidth = builder.arrowWidth;
        this.mArrowHeight = builder.arrowHeight;
        this.mArrowDrawable = builder.arrowDrawable;
        this.mAnimated = builder.animated;
        this.mMargin = builder.margin;
        this.mPadding = builder.padding;
        this.mAnimationPadding = builder.animationPadding;
        this.mAnimationDuration = builder.animationDuration;
        this.mOnDismissListener = builder.onDismissListener;
        this.mOnShowListener = builder.onShowListener;
        this.mFocusable = builder.focusable;
        this.mRootView = SimpleTooltipUtils.findFrameLayout(this.mAnchorView);
        this.mHighlightShape = builder.highlightShape;
        init();
    }

    private void init() {
        configPopupWindow();
        configContentView();
    }

    private void configPopupWindow() {
        this.mPopupWindow = new PopupWindow(this.mContext, (AttributeSet) null, 16842870);
        this.mPopupWindow.setOnDismissListener(this);
        this.mPopupWindow.setWidth(-2);
        this.mPopupWindow.setHeight(-2);
        this.mPopupWindow.setBackgroundDrawable(new ColorDrawable(0));
        this.mPopupWindow.setOutsideTouchable(true);
        this.mPopupWindow.setTouchable(true);
        this.mPopupWindow.setTouchInterceptor(new View.OnTouchListener() { // from class: io.github.douglasjunior.androidSimpleTooltip.SimpleTooltip.1
            @Override // android.view.View.OnTouchListener
            public boolean onTouch(View view, MotionEvent motionEvent) {
                int x = (int) motionEvent.getX();
                int y = (int) motionEvent.getY();
                if (!SimpleTooltip.this.mDismissOnOutsideTouch && motionEvent.getAction() == 0 && (x < 0 || x >= SimpleTooltip.this.mContentLayout.getMeasuredWidth() || y < 0 || y >= SimpleTooltip.this.mContentLayout.getMeasuredHeight())) {
                    return true;
                }
                if (!SimpleTooltip.this.mDismissOnOutsideTouch && motionEvent.getAction() == 4) {
                    return true;
                }
                if (motionEvent.getAction() != 0 || !SimpleTooltip.this.mDismissOnInsideTouch) {
                    return false;
                }
                SimpleTooltip.this.dismiss();
                return true;
            }
        });
        this.mPopupWindow.setClippingEnabled(false);
        this.mPopupWindow.setFocusable(this.mFocusable);
    }

    public void show() {
        verifyDismissed();
        this.mContentLayout.getViewTreeObserver().addOnGlobalLayoutListener(this.mLocationLayoutListener);
        this.mContentLayout.getViewTreeObserver().addOnGlobalLayoutListener(this.mAutoDismissLayoutListener);
        this.mRootView.post(new Runnable() { // from class: io.github.douglasjunior.androidSimpleTooltip.SimpleTooltip.2
            @Override // java.lang.Runnable
            public void run() {
                if (SimpleTooltip.this.mRootView.isShown()) {
                    SimpleTooltip.this.mPopupWindow.showAtLocation(SimpleTooltip.this.mRootView, 0, SimpleTooltip.this.mRootView.getWidth(), SimpleTooltip.this.mRootView.getHeight());
                } else {
                    Log.e(SimpleTooltip.TAG, "Tooltip cannot be shown, root view is invalid or has been closed.");
                }
            }
        });
    }

    private void verifyDismissed() {
        if (this.dismissed) {
            throw new IllegalArgumentException("Tooltip has ben dismissed.");
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void createOverlay() {
        this.mOverlay = this.mTransparentOverlay ? new View(this.mContext) : new OverlayView(this.mContext, this.mAnchorView, this.mHighlightShape, this.mOverlayOffset);
        if (this.mOverlayMatchParent) {
            this.mOverlay.setLayoutParams(new ViewGroup.LayoutParams(-1, -1));
        } else {
            this.mOverlay.setLayoutParams(new ViewGroup.LayoutParams(this.mRootView.getWidth(), this.mRootView.getHeight()));
        }
        this.mOverlay.setOnTouchListener(this.mOverlayTouchListener);
        this.mRootView.addView(this.mOverlay);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public PointF calculePopupLocation() {
        PointF pointF = new PointF();
        RectF rectFCalculeRectInWindow = SimpleTooltipUtils.calculeRectInWindow(this.mAnchorView);
        PointF pointF2 = new PointF(rectFCalculeRectInWindow.centerX(), rectFCalculeRectInWindow.centerY());
        int i = this.mGravity;
        if (i == 17) {
            pointF.x = pointF2.x - (this.mPopupWindow.getContentView().getWidth() / 2.0f);
            pointF.y = pointF2.y - (this.mPopupWindow.getContentView().getHeight() / 2.0f);
        } else if (i == 48) {
            pointF.x = pointF2.x - (this.mPopupWindow.getContentView().getWidth() / 2.0f);
            pointF.y = (rectFCalculeRectInWindow.top - this.mPopupWindow.getContentView().getHeight()) - this.mMargin;
        } else if (i == 80) {
            pointF.x = pointF2.x - (this.mPopupWindow.getContentView().getWidth() / 2.0f);
            pointF.y = rectFCalculeRectInWindow.bottom + this.mMargin;
        } else if (i == 8388611) {
            pointF.x = (rectFCalculeRectInWindow.left - this.mPopupWindow.getContentView().getWidth()) - this.mMargin;
            pointF.y = pointF2.y - (this.mPopupWindow.getContentView().getHeight() / 2.0f);
        } else if (i == 8388613) {
            pointF.x = rectFCalculeRectInWindow.right + this.mMargin;
            pointF.y = pointF2.y - (this.mPopupWindow.getContentView().getHeight() / 2.0f);
        } else {
            throw new IllegalArgumentException("Gravity must have be CENTER, START, END, TOP or BOTTOM.");
        }
        return pointF;
    }

    private void configContentView() {
        LinearLayout.LayoutParams layoutParams;
        if (this.mContentView instanceof TextView) {
            ((TextView) this.mContentView).setText(this.mText);
        } else {
            TextView textView = (TextView) this.mContentView.findViewById(this.mTextViewId);
            if (textView != null) {
                textView.setText(this.mText);
            }
        }
        this.mContentView.setPadding((int) this.mPadding, (int) this.mPadding, (int) this.mPadding, (int) this.mPadding);
        LinearLayout linearLayout = new LinearLayout(this.mContext);
        linearLayout.setLayoutParams(new ViewGroup.LayoutParams(-2, -2));
        linearLayout.setOrientation((this.mArrowDirection == 0 || this.mArrowDirection == 2) ? 0 : 1);
        int i = (int) (this.mAnimated ? this.mAnimationPadding : 0.0f);
        linearLayout.setPadding(i, i, i, i);
        if (this.mShowArrow) {
            this.mArrowView = new ImageView(this.mContext);
            this.mArrowView.setImageDrawable(this.mArrowDrawable);
            if (this.mArrowDirection == 1 || this.mArrowDirection == 3) {
                layoutParams = new LinearLayout.LayoutParams((int) this.mArrowWidth, (int) this.mArrowHeight, 0.0f);
            } else {
                layoutParams = new LinearLayout.LayoutParams((int) this.mArrowHeight, (int) this.mArrowWidth, 0.0f);
            }
            layoutParams.gravity = 17;
            this.mArrowView.setLayoutParams(layoutParams);
            if (this.mArrowDirection == 3 || this.mArrowDirection == 2) {
                linearLayout.addView(this.mContentView);
                linearLayout.addView(this.mArrowView);
            } else {
                linearLayout.addView(this.mArrowView);
                linearLayout.addView(this.mContentView);
            }
        } else {
            linearLayout.addView(this.mContentView);
        }
        LinearLayout.LayoutParams layoutParams2 = new LinearLayout.LayoutParams(-2, -2, 0.0f);
        layoutParams2.gravity = 17;
        this.mContentView.setLayoutParams(layoutParams2);
        this.mContentLayout = linearLayout;
        this.mContentLayout.setVisibility(4);
        this.mPopupWindow.setContentView(this.mContentLayout);
    }

    public void dismiss() {
        if (this.dismissed) {
            return;
        }
        this.dismissed = true;
        if (this.mPopupWindow != null) {
            this.mPopupWindow.dismiss();
        }
    }

    public boolean isShowing() {
        return this.mPopupWindow != null && this.mPopupWindow.isShowing();
    }

    public <T extends View> T findViewById(int i) {
        return (T) this.mContentLayout.findViewById(i);
    }

    @Override // android.widget.PopupWindow.OnDismissListener
    public void onDismiss() {
        this.dismissed = true;
        if (Build.VERSION.SDK_INT >= 11 && this.mAnimator != null) {
            this.mAnimator.removeAllListeners();
            this.mAnimator.end();
            this.mAnimator.cancel();
            this.mAnimator = null;
        }
        if (this.mRootView != null && this.mOverlay != null) {
            this.mRootView.removeView(this.mOverlay);
        }
        this.mRootView = null;
        this.mOverlay = null;
        if (this.mOnDismissListener != null) {
            this.mOnDismissListener.onDismiss(this);
        }
        this.mOnDismissListener = null;
        SimpleTooltipUtils.removeOnGlobalLayoutListener(this.mPopupWindow.getContentView(), this.mLocationLayoutListener);
        SimpleTooltipUtils.removeOnGlobalLayoutListener(this.mPopupWindow.getContentView(), this.mArrowLayoutListener);
        SimpleTooltipUtils.removeOnGlobalLayoutListener(this.mPopupWindow.getContentView(), this.mShowLayoutListener);
        SimpleTooltipUtils.removeOnGlobalLayoutListener(this.mPopupWindow.getContentView(), this.mAnimationLayoutListener);
        SimpleTooltipUtils.removeOnGlobalLayoutListener(this.mPopupWindow.getContentView(), this.mAutoDismissLayoutListener);
        this.mPopupWindow = null;
    }

    /* JADX INFO: Access modifiers changed from: private */
    @TargetApi(11)
    public void startAnimation() {
        String str = (this.mGravity == 48 || this.mGravity == 80) ? "translationY" : "translationX";
        ObjectAnimator objectAnimatorOfFloat = ObjectAnimator.ofFloat(this.mContentLayout, str, -this.mAnimationPadding, this.mAnimationPadding);
        objectAnimatorOfFloat.setDuration(this.mAnimationDuration);
        objectAnimatorOfFloat.setInterpolator(new AccelerateDecelerateInterpolator());
        ObjectAnimator objectAnimatorOfFloat2 = ObjectAnimator.ofFloat(this.mContentLayout, str, this.mAnimationPadding, -this.mAnimationPadding);
        objectAnimatorOfFloat2.setDuration(this.mAnimationDuration);
        objectAnimatorOfFloat2.setInterpolator(new AccelerateDecelerateInterpolator());
        this.mAnimator = new AnimatorSet();
        this.mAnimator.playSequentially(objectAnimatorOfFloat, objectAnimatorOfFloat2);
        this.mAnimator.addListener(new AnimatorListenerAdapter() { // from class: io.github.douglasjunior.androidSimpleTooltip.SimpleTooltip.8
            @Override // android.animation.AnimatorListenerAdapter, android.animation.Animator.AnimatorListener
            public void onAnimationEnd(Animator animator) {
                if (SimpleTooltip.this.dismissed || !SimpleTooltip.this.isShowing()) {
                    return;
                }
                animator.start();
            }
        });
        this.mAnimator.start();
    }

    public static class Builder {
        private View anchorView;
        private long animationDuration;
        private int arrowColor;
        private Drawable arrowDrawable;
        private float arrowHeight;
        private float arrowWidth;
        private int backgroundColor;
        private View contentView;
        private final Context context;
        private boolean focusable;
        private float maxWidth;
        private OnDismissListener onDismissListener;
        private OnShowListener onShowListener;
        private int textColor;
        private boolean dismissOnInsideTouch = true;
        private boolean dismissOnOutsideTouch = true;
        private boolean modal = false;

        @IdRes
        private int textViewId = android.R.id.text1;
        private CharSequence text = "";
        private int arrowDirection = 4;
        private int gravity = 80;
        private boolean transparentOverlay = true;
        private float overlayOffset = -1.0f;
        private boolean overlayMatchParent = true;
        private boolean showArrow = true;
        private boolean animated = false;
        private float margin = -1.0f;
        private float padding = -1.0f;
        private float animationPadding = -1.0f;
        private int highlightShape = 0;

        public Builder(Context context) {
            this.context = context;
        }

        public SimpleTooltip build() throws IllegalArgumentException {
            validateArguments();
            if (this.backgroundColor == 0) {
                this.backgroundColor = SimpleTooltipUtils.getColor(this.context, SimpleTooltip.mDefaultBackgroundColorRes);
            }
            if (this.textColor == 0) {
                this.textColor = SimpleTooltipUtils.getColor(this.context, SimpleTooltip.mDefaultTextColorRes);
            }
            if (this.contentView == null) {
                TextView textView = new TextView(this.context);
                SimpleTooltipUtils.setTextAppearance(textView, SimpleTooltip.mDefaultTextAppearanceRes);
                textView.setBackgroundColor(this.backgroundColor);
                textView.setTextColor(this.textColor);
                this.contentView = textView;
            }
            if (this.arrowColor == 0) {
                this.arrowColor = SimpleTooltipUtils.getColor(this.context, SimpleTooltip.mDefaultArrowColorRes);
            }
            if (this.margin < 0.0f) {
                this.margin = this.context.getResources().getDimension(SimpleTooltip.mDefaultMarginRes);
            }
            if (this.padding < 0.0f) {
                this.padding = this.context.getResources().getDimension(SimpleTooltip.mDefaultPaddingRes);
            }
            if (this.animationPadding < 0.0f) {
                this.animationPadding = this.context.getResources().getDimension(SimpleTooltip.mDefaultAnimationPaddingRes);
            }
            if (this.animationDuration == 0) {
                this.animationDuration = this.context.getResources().getInteger(SimpleTooltip.mDefaultAnimationDurationRes);
            }
            if (Build.VERSION.SDK_INT < 11) {
                this.animated = false;
            }
            if (this.showArrow) {
                if (this.arrowDirection == 4) {
                    this.arrowDirection = SimpleTooltipUtils.tooltipGravityToArrowDirection(this.gravity);
                }
                if (this.arrowDrawable == null) {
                    this.arrowDrawable = new ArrowDrawable(this.arrowColor, this.arrowDirection);
                }
                if (this.arrowWidth == 0.0f) {
                    this.arrowWidth = this.context.getResources().getDimension(SimpleTooltip.mDefaultArrowWidthRes);
                }
                if (this.arrowHeight == 0.0f) {
                    this.arrowHeight = this.context.getResources().getDimension(SimpleTooltip.mDefaultArrowHeightRes);
                }
            }
            if (this.highlightShape < 0 || this.highlightShape > 1) {
                this.highlightShape = 0;
            }
            if (this.overlayOffset < 0.0f) {
                this.overlayOffset = this.context.getResources().getDimension(SimpleTooltip.mDefaultOverlayOffsetRes);
            }
            return new SimpleTooltip(this);
        }

        private void validateArguments() throws IllegalArgumentException {
            if (this.context == null) {
                throw new IllegalArgumentException("Context not specified.");
            }
            if (this.anchorView == null) {
                throw new IllegalArgumentException("Anchor view not specified.");
            }
        }

        public Builder contentView(TextView textView) {
            this.contentView = textView;
            this.textViewId = 0;
            return this;
        }

        public Builder contentView(View view, @IdRes int i) {
            this.contentView = view;
            this.textViewId = i;
            return this;
        }

        public Builder contentView(@LayoutRes int i, @IdRes int i2) {
            this.contentView = ((LayoutInflater) this.context.getSystemService("layout_inflater")).inflate(i, (ViewGroup) null, false);
            this.textViewId = i2;
            return this;
        }

        public Builder contentView(@LayoutRes int i) {
            this.contentView = ((LayoutInflater) this.context.getSystemService("layout_inflater")).inflate(i, (ViewGroup) null, false);
            this.textViewId = 0;
            return this;
        }

        public Builder dismissOnInsideTouch(boolean z) {
            this.dismissOnInsideTouch = z;
            return this;
        }

        public Builder dismissOnOutsideTouch(boolean z) {
            this.dismissOnOutsideTouch = z;
            return this;
        }

        public Builder modal(boolean z) {
            this.modal = z;
            return this;
        }

        public Builder text(CharSequence charSequence) {
            this.text = charSequence;
            return this;
        }

        public Builder text(@StringRes int i) {
            this.text = this.context.getString(i);
            return this;
        }

        public Builder anchorView(View view) {
            this.anchorView = view;
            return this;
        }

        public Builder gravity(int i) {
            this.gravity = i;
            return this;
        }

        public Builder arrowDirection(int i) {
            this.arrowDirection = i;
            return this;
        }

        public Builder transparentOverlay(boolean z) {
            this.transparentOverlay = z;
            return this;
        }

        public Builder maxWidth(@DimenRes int i) {
            this.maxWidth = this.context.getResources().getDimension(i);
            return this;
        }

        public Builder maxWidth(float f) {
            this.maxWidth = f;
            return this;
        }

        @TargetApi(11)
        public Builder animated(boolean z) {
            this.animated = z;
            return this;
        }

        @TargetApi(11)
        public Builder animationPadding(float f) {
            this.animationPadding = f;
            return this;
        }

        @TargetApi(11)
        public Builder animationPadding(@DimenRes int i) {
            this.animationPadding = this.context.getResources().getDimension(i);
            return this;
        }

        @TargetApi(11)
        public Builder animationDuration(long j) {
            this.animationDuration = j;
            return this;
        }

        public Builder padding(float f) {
            this.padding = f;
            return this;
        }

        public Builder padding(@DimenRes int i) {
            this.padding = this.context.getResources().getDimension(i);
            return this;
        }

        public Builder margin(float f) {
            this.margin = f;
            return this;
        }

        public Builder margin(@DimenRes int i) {
            this.margin = this.context.getResources().getDimension(i);
            return this;
        }

        public Builder textColor(int i) {
            this.textColor = i;
            return this;
        }

        public Builder backgroundColor(@ColorInt int i) {
            this.backgroundColor = i;
            return this;
        }

        public Builder showArrow(boolean z) {
            this.showArrow = z;
            return this;
        }

        public Builder arrowDrawable(Drawable drawable) {
            this.arrowDrawable = drawable;
            return this;
        }

        public Builder arrowDrawable(@DrawableRes int i) {
            this.arrowDrawable = SimpleTooltipUtils.getDrawable(this.context, i);
            return this;
        }

        public Builder arrowColor(@ColorInt int i) {
            this.arrowColor = i;
            return this;
        }

        public Builder arrowHeight(float f) {
            this.arrowHeight = f;
            return this;
        }

        public Builder arrowWidth(float f) {
            this.arrowWidth = f;
            return this;
        }

        public Builder onDismissListener(OnDismissListener onDismissListener) {
            this.onDismissListener = onDismissListener;
            return this;
        }

        public Builder onShowListener(OnShowListener onShowListener) {
            this.onShowListener = onShowListener;
            return this;
        }

        public Builder focusable(boolean z) {
            this.focusable = z;
            return this;
        }

        public Builder highlightShape(int i) {
            this.highlightShape = i;
            return this;
        }

        public Builder overlayOffset(@Dimension float f) {
            this.overlayOffset = f;
            return this;
        }

        public Builder overlayMatchParent(boolean z) {
            this.overlayMatchParent = z;
            return this;
        }
    }
}
