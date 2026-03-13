package android.support.design.widget;

import android.content.Context;
import android.content.res.TypedArray;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.support.annotation.RestrictTo;
import android.support.annotation.VisibleForTesting;
import android.support.design.R;
import android.support.design.widget.CoordinatorLayout;
import android.support.v4.math.MathUtils;
import android.support.v4.view.AbsSavedState;
import android.support.v4.view.ViewCompat;
import android.support.v4.widget.ViewDragHelper;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.ViewParent;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.ref.WeakReference;

/* JADX INFO: loaded from: classes.dex */
public class BottomSheetBehavior<V extends View> extends CoordinatorLayout.Behavior<V> {
    private static final float HIDE_FRICTION = 0.1f;
    private static final float HIDE_THRESHOLD = 0.5f;
    public static final int PEEK_HEIGHT_AUTO = -1;
    public static final int STATE_COLLAPSED = 4;
    public static final int STATE_DRAGGING = 1;
    public static final int STATE_EXPANDED = 3;
    public static final int STATE_HIDDEN = 5;
    public static final int STATE_SETTLING = 2;
    int mActivePointerId;
    private BottomSheetCallback mCallback;
    private final ViewDragHelper.Callback mDragCallback;
    boolean mHideable;
    private boolean mIgnoreEvents;
    private int mInitialY;
    private int mLastNestedScrollDy;
    int mMaxOffset;
    private float mMaximumVelocity;
    int mMinOffset;
    private boolean mNestedScrolled;
    WeakReference<View> mNestedScrollingChildRef;
    int mParentHeight;
    private int mPeekHeight;
    private boolean mPeekHeightAuto;
    private int mPeekHeightMin;
    private boolean mSkipCollapsed;
    int mState;
    boolean mTouchingScrollingChild;
    private VelocityTracker mVelocityTracker;
    ViewDragHelper mViewDragHelper;
    WeakReference<V> mViewRef;

    public static abstract class BottomSheetCallback {
        public abstract void onSlide(@NonNull View view, float f);

        public abstract void onStateChanged(@NonNull View view, int i);
    }

    @Retention(RetentionPolicy.SOURCE)
    @RestrictTo({RestrictTo.Scope.LIBRARY_GROUP})
    public @interface State {
    }

    public BottomSheetBehavior() {
        this.mState = 4;
        this.mDragCallback = new ViewDragHelper.Callback() { // from class: android.support.design.widget.BottomSheetBehavior.2
            @Override // android.support.v4.widget.ViewDragHelper.Callback
            public boolean tryCaptureView(View view, int i) {
                View view2;
                if (BottomSheetBehavior.this.mState == 1 || BottomSheetBehavior.this.mTouchingScrollingChild) {
                    return false;
                }
                if (BottomSheetBehavior.this.mState == 3 && BottomSheetBehavior.this.mActivePointerId == i && (view2 = BottomSheetBehavior.this.mNestedScrollingChildRef.get()) != null && view2.canScrollVertically(-1)) {
                    return false;
                }
                return BottomSheetBehavior.this.mViewRef != null && BottomSheetBehavior.this.mViewRef.get() == view;
            }

            @Override // android.support.v4.widget.ViewDragHelper.Callback
            public void onViewPositionChanged(View view, int i, int i2, int i3, int i4) {
                BottomSheetBehavior.this.dispatchOnSlide(i2);
            }

            @Override // android.support.v4.widget.ViewDragHelper.Callback
            public void onViewDragStateChanged(int i) {
                if (i == 1) {
                    BottomSheetBehavior.this.setStateInternal(1);
                }
            }

            /* JADX WARN: Removed duplicated region for block: B:20:0x005a  */
            /* JADX WARN: Removed duplicated region for block: B:21:0x006b  */
            @Override // android.support.v4.widget.ViewDragHelper.Callback
            /*
                Code decompiled incorrectly, please refer to instructions dump.
                To view partially-correct add '--show-bad-code' argument
            */
            public void onViewReleased(android.view.View r4, float r5, float r6) {
                /*
                    r3 = this;
                    r5 = 0
                    int r0 = (r6 > r5 ? 1 : (r6 == r5 ? 0 : -1))
                    r1 = 4
                    r2 = 3
                    if (r0 >= 0) goto Ld
                    android.support.design.widget.BottomSheetBehavior r5 = android.support.design.widget.BottomSheetBehavior.this
                    int r5 = r5.mMinOffset
                Lb:
                    r1 = r2
                    goto L4c
                Ld:
                    android.support.design.widget.BottomSheetBehavior r0 = android.support.design.widget.BottomSheetBehavior.this
                    boolean r0 = r0.mHideable
                    if (r0 == 0) goto L21
                    android.support.design.widget.BottomSheetBehavior r0 = android.support.design.widget.BottomSheetBehavior.this
                    boolean r0 = r0.shouldHide(r4, r6)
                    if (r0 == 0) goto L21
                    android.support.design.widget.BottomSheetBehavior r5 = android.support.design.widget.BottomSheetBehavior.this
                    int r5 = r5.mParentHeight
                    r1 = 5
                    goto L4c
                L21:
                    int r5 = (r6 > r5 ? 1 : (r6 == r5 ? 0 : -1))
                    if (r5 != 0) goto L48
                    int r5 = r4.getTop()
                    android.support.design.widget.BottomSheetBehavior r6 = android.support.design.widget.BottomSheetBehavior.this
                    int r6 = r6.mMinOffset
                    int r6 = r5 - r6
                    int r6 = java.lang.Math.abs(r6)
                    android.support.design.widget.BottomSheetBehavior r0 = android.support.design.widget.BottomSheetBehavior.this
                    int r0 = r0.mMaxOffset
                    int r5 = r5 - r0
                    int r5 = java.lang.Math.abs(r5)
                    if (r6 >= r5) goto L43
                    android.support.design.widget.BottomSheetBehavior r5 = android.support.design.widget.BottomSheetBehavior.this
                    int r5 = r5.mMinOffset
                    goto Lb
                L43:
                    android.support.design.widget.BottomSheetBehavior r5 = android.support.design.widget.BottomSheetBehavior.this
                    int r5 = r5.mMaxOffset
                    goto L4c
                L48:
                    android.support.design.widget.BottomSheetBehavior r5 = android.support.design.widget.BottomSheetBehavior.this
                    int r5 = r5.mMaxOffset
                L4c:
                    android.support.design.widget.BottomSheetBehavior r6 = android.support.design.widget.BottomSheetBehavior.this
                    android.support.v4.widget.ViewDragHelper r6 = r6.mViewDragHelper
                    int r0 = r4.getLeft()
                    boolean r5 = r6.settleCapturedViewAt(r0, r5)
                    if (r5 == 0) goto L6b
                    android.support.design.widget.BottomSheetBehavior r5 = android.support.design.widget.BottomSheetBehavior.this
                    r6 = 2
                    r5.setStateInternal(r6)
                    android.support.design.widget.BottomSheetBehavior$SettleRunnable r5 = new android.support.design.widget.BottomSheetBehavior$SettleRunnable
                    android.support.design.widget.BottomSheetBehavior r3 = android.support.design.widget.BottomSheetBehavior.this
                    r5.<init>(r4, r1)
                    android.support.v4.view.ViewCompat.postOnAnimation(r4, r5)
                    goto L70
                L6b:
                    android.support.design.widget.BottomSheetBehavior r3 = android.support.design.widget.BottomSheetBehavior.this
                    r3.setStateInternal(r1)
                L70:
                    return
                */
                throw new UnsupportedOperationException("Method not decompiled: android.support.design.widget.BottomSheetBehavior.AnonymousClass2.onViewReleased(android.view.View, float, float):void");
            }

            @Override // android.support.v4.widget.ViewDragHelper.Callback
            public int clampViewPositionVertical(View view, int i, int i2) {
                return MathUtils.clamp(i, BottomSheetBehavior.this.mMinOffset, BottomSheetBehavior.this.mHideable ? BottomSheetBehavior.this.mParentHeight : BottomSheetBehavior.this.mMaxOffset);
            }

            @Override // android.support.v4.widget.ViewDragHelper.Callback
            public int clampViewPositionHorizontal(View view, int i, int i2) {
                return view.getLeft();
            }

            @Override // android.support.v4.widget.ViewDragHelper.Callback
            public int getViewVerticalDragRange(View view) {
                if (BottomSheetBehavior.this.mHideable) {
                    return BottomSheetBehavior.this.mParentHeight - BottomSheetBehavior.this.mMinOffset;
                }
                return BottomSheetBehavior.this.mMaxOffset - BottomSheetBehavior.this.mMinOffset;
            }
        };
    }

    public BottomSheetBehavior(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
        this.mState = 4;
        this.mDragCallback = new ViewDragHelper.Callback() { // from class: android.support.design.widget.BottomSheetBehavior.2
            @Override // android.support.v4.widget.ViewDragHelper.Callback
            public boolean tryCaptureView(View view, int i) {
                View view2;
                if (BottomSheetBehavior.this.mState == 1 || BottomSheetBehavior.this.mTouchingScrollingChild) {
                    return false;
                }
                if (BottomSheetBehavior.this.mState == 3 && BottomSheetBehavior.this.mActivePointerId == i && (view2 = BottomSheetBehavior.this.mNestedScrollingChildRef.get()) != null && view2.canScrollVertically(-1)) {
                    return false;
                }
                return BottomSheetBehavior.this.mViewRef != null && BottomSheetBehavior.this.mViewRef.get() == view;
            }

            @Override // android.support.v4.widget.ViewDragHelper.Callback
            public void onViewPositionChanged(View view, int i, int i2, int i3, int i4) {
                BottomSheetBehavior.this.dispatchOnSlide(i2);
            }

            @Override // android.support.v4.widget.ViewDragHelper.Callback
            public void onViewDragStateChanged(int i) {
                if (i == 1) {
                    BottomSheetBehavior.this.setStateInternal(1);
                }
            }

            @Override // android.support.v4.widget.ViewDragHelper.Callback
            public void onViewReleased(View v, float v2, float v3) {
                /*
                    this = this;
                    r5 = 0
                    int r0 = (r6 > r5 ? 1 : (r6 == r5 ? 0 : -1))
                    r1 = 4
                    r2 = 3
                    if (r0 >= 0) goto Ld
                    android.support.design.widget.BottomSheetBehavior r5 = android.support.design.widget.BottomSheetBehavior.this
                    int r5 = r5.mMinOffset
                Lb:
                    r1 = r2
                    goto L4c
                Ld:
                    android.support.design.widget.BottomSheetBehavior r0 = android.support.design.widget.BottomSheetBehavior.this
                    boolean r0 = r0.mHideable
                    if (r0 == 0) goto L21
                    android.support.design.widget.BottomSheetBehavior r0 = android.support.design.widget.BottomSheetBehavior.this
                    boolean r0 = r0.shouldHide(r4, r6)
                    if (r0 == 0) goto L21
                    android.support.design.widget.BottomSheetBehavior r5 = android.support.design.widget.BottomSheetBehavior.this
                    int r5 = r5.mParentHeight
                    r1 = 5
                    goto L4c
                L21:
                    int r5 = (r6 > r5 ? 1 : (r6 == r5 ? 0 : -1))
                    if (r5 != 0) goto L48
                    int r5 = r4.getTop()
                    android.support.design.widget.BottomSheetBehavior r6 = android.support.design.widget.BottomSheetBehavior.this
                    int r6 = r6.mMinOffset
                    int r6 = r5 - r6
                    int r6 = java.lang.Math.abs(r6)
                    android.support.design.widget.BottomSheetBehavior r0 = android.support.design.widget.BottomSheetBehavior.this
                    int r0 = r0.mMaxOffset
                    int r5 = r5 - r0
                    int r5 = java.lang.Math.abs(r5)
                    if (r6 >= r5) goto L43
                    android.support.design.widget.BottomSheetBehavior r5 = android.support.design.widget.BottomSheetBehavior.this
                    int r5 = r5.mMinOffset
                    goto Lb
                L43:
                    android.support.design.widget.BottomSheetBehavior r5 = android.support.design.widget.BottomSheetBehavior.this
                    int r5 = r5.mMaxOffset
                    goto L4c
                L48:
                    android.support.design.widget.BottomSheetBehavior r5 = android.support.design.widget.BottomSheetBehavior.this
                    int r5 = r5.mMaxOffset
                L4c:
                    android.support.design.widget.BottomSheetBehavior r6 = android.support.design.widget.BottomSheetBehavior.this
                    android.support.v4.widget.ViewDragHelper r6 = r6.mViewDragHelper
                    int r0 = r4.getLeft()
                    boolean r5 = r6.settleCapturedViewAt(r0, r5)
                    if (r5 == 0) goto L6b
                    android.support.design.widget.BottomSheetBehavior r5 = android.support.design.widget.BottomSheetBehavior.this
                    r6 = 2
                    r5.setStateInternal(r6)
                    android.support.design.widget.BottomSheetBehavior$SettleRunnable r5 = new android.support.design.widget.BottomSheetBehavior$SettleRunnable
                    android.support.design.widget.BottomSheetBehavior r3 = android.support.design.widget.BottomSheetBehavior.this
                    r5.<init>(r4, r1)
                    android.support.v4.view.ViewCompat.postOnAnimation(r4, r5)
                    goto L70
                L6b:
                    android.support.design.widget.BottomSheetBehavior r3 = android.support.design.widget.BottomSheetBehavior.this
                    r3.setStateInternal(r1)
                L70:
                    return
                */
                throw new UnsupportedOperationException("Method not decompiled: android.support.design.widget.BottomSheetBehavior.AnonymousClass2.onViewReleased(android.view.View, float, float):void");
            }

            @Override // android.support.v4.widget.ViewDragHelper.Callback
            public int clampViewPositionVertical(View view, int i, int i2) {
                return MathUtils.clamp(i, BottomSheetBehavior.this.mMinOffset, BottomSheetBehavior.this.mHideable ? BottomSheetBehavior.this.mParentHeight : BottomSheetBehavior.this.mMaxOffset);
            }

            @Override // android.support.v4.widget.ViewDragHelper.Callback
            public int clampViewPositionHorizontal(View view, int i, int i2) {
                return view.getLeft();
            }

            @Override // android.support.v4.widget.ViewDragHelper.Callback
            public int getViewVerticalDragRange(View view) {
                if (BottomSheetBehavior.this.mHideable) {
                    return BottomSheetBehavior.this.mParentHeight - BottomSheetBehavior.this.mMinOffset;
                }
                return BottomSheetBehavior.this.mMaxOffset - BottomSheetBehavior.this.mMinOffset;
            }
        };
        TypedArray typedArrayObtainStyledAttributes = context.obtainStyledAttributes(attributeSet, R.styleable.BottomSheetBehavior_Layout);
        TypedValue typedValuePeekValue = typedArrayObtainStyledAttributes.peekValue(R.styleable.BottomSheetBehavior_Layout_behavior_peekHeight);
        if (typedValuePeekValue != null && typedValuePeekValue.data == -1) {
            setPeekHeight(typedValuePeekValue.data);
        } else {
            setPeekHeight(typedArrayObtainStyledAttributes.getDimensionPixelSize(R.styleable.BottomSheetBehavior_Layout_behavior_peekHeight, -1));
        }
        setHideable(typedArrayObtainStyledAttributes.getBoolean(R.styleable.BottomSheetBehavior_Layout_behavior_hideable, false));
        setSkipCollapsed(typedArrayObtainStyledAttributes.getBoolean(R.styleable.BottomSheetBehavior_Layout_behavior_skipCollapsed, false));
        typedArrayObtainStyledAttributes.recycle();
        this.mMaximumVelocity = ViewConfiguration.get(context).getScaledMaximumFlingVelocity();
    }

    @Override // android.support.design.widget.CoordinatorLayout.Behavior
    public Parcelable onSaveInstanceState(CoordinatorLayout coordinatorLayout, V v) {
        return new SavedState(super.onSaveInstanceState(coordinatorLayout, v), this.mState);
    }

    @Override // android.support.design.widget.CoordinatorLayout.Behavior
    public void onRestoreInstanceState(CoordinatorLayout coordinatorLayout, V v, Parcelable parcelable) {
        SavedState savedState = (SavedState) parcelable;
        super.onRestoreInstanceState(coordinatorLayout, v, savedState.getSuperState());
        if (savedState.state == 1 || savedState.state == 2) {
            this.mState = 4;
        } else {
            this.mState = savedState.state;
        }
    }

    @Override // android.support.design.widget.CoordinatorLayout.Behavior
    public boolean onLayoutChild(CoordinatorLayout coordinatorLayout, V v, int i) {
        int iMax;
        if (ViewCompat.getFitsSystemWindows(coordinatorLayout) && !ViewCompat.getFitsSystemWindows(v)) {
            ViewCompat.setFitsSystemWindows(v, true);
        }
        int top = v.getTop();
        coordinatorLayout.onLayoutChild(v, i);
        this.mParentHeight = coordinatorLayout.getHeight();
        if (this.mPeekHeightAuto) {
            if (this.mPeekHeightMin == 0) {
                this.mPeekHeightMin = coordinatorLayout.getResources().getDimensionPixelSize(R.dimen.design_bottom_sheet_peek_height_min);
            }
            iMax = Math.max(this.mPeekHeightMin, this.mParentHeight - ((coordinatorLayout.getWidth() * 9) / 16));
        } else {
            iMax = this.mPeekHeight;
        }
        this.mMinOffset = Math.max(0, this.mParentHeight - v.getHeight());
        this.mMaxOffset = Math.max(this.mParentHeight - iMax, this.mMinOffset);
        if (this.mState == 3) {
            ViewCompat.offsetTopAndBottom(v, this.mMinOffset);
        } else if (this.mHideable && this.mState == 5) {
            ViewCompat.offsetTopAndBottom(v, this.mParentHeight);
        } else if (this.mState == 4) {
            ViewCompat.offsetTopAndBottom(v, this.mMaxOffset);
        } else if (this.mState == 1 || this.mState == 2) {
            ViewCompat.offsetTopAndBottom(v, top - v.getTop());
        }
        if (this.mViewDragHelper == null) {
            this.mViewDragHelper = ViewDragHelper.create(coordinatorLayout, this.mDragCallback);
        }
        this.mViewRef = new WeakReference<>(v);
        this.mNestedScrollingChildRef = new WeakReference<>(findScrollingChild(v));
        return true;
    }

    /* JADX WARN: Removed duplicated region for block: B:31:0x006d  */
    @Override // android.support.design.widget.CoordinatorLayout.Behavior
    /*
        Code decompiled incorrectly, please refer to instructions dump.
        To view partially-correct add '--show-bad-code' argument
    */
    public boolean onInterceptTouchEvent(android.support.design.widget.CoordinatorLayout r8, V r9, android.view.MotionEvent r10) {
        /*
            Method dump skipped, instruction units count: 204
            To view this dump add '--comments-level debug' option
        */
        throw new UnsupportedOperationException("Method not decompiled: android.support.design.widget.BottomSheetBehavior.onInterceptTouchEvent(android.support.design.widget.CoordinatorLayout, android.view.View, android.view.MotionEvent):boolean");
    }

    @Override // android.support.design.widget.CoordinatorLayout.Behavior
    public boolean onTouchEvent(CoordinatorLayout coordinatorLayout, V v, MotionEvent motionEvent) {
        if (!v.isShown()) {
            return false;
        }
        int actionMasked = motionEvent.getActionMasked();
        if (this.mState == 1 && actionMasked == 0) {
            return true;
        }
        if (this.mViewDragHelper != null) {
            this.mViewDragHelper.processTouchEvent(motionEvent);
        }
        if (actionMasked == 0) {
            reset();
        }
        if (this.mVelocityTracker == null) {
            this.mVelocityTracker = VelocityTracker.obtain();
        }
        this.mVelocityTracker.addMovement(motionEvent);
        if (actionMasked == 2 && !this.mIgnoreEvents && Math.abs(this.mInitialY - motionEvent.getY()) > this.mViewDragHelper.getTouchSlop()) {
            this.mViewDragHelper.captureChildView(v, motionEvent.getPointerId(motionEvent.getActionIndex()));
        }
        return !this.mIgnoreEvents;
    }

    @Override // android.support.design.widget.CoordinatorLayout.Behavior
    public boolean onStartNestedScroll(CoordinatorLayout coordinatorLayout, V v, View view, View view2, int i) {
        this.mLastNestedScrollDy = 0;
        this.mNestedScrolled = false;
        return (i & 2) != 0;
    }

    @Override // android.support.design.widget.CoordinatorLayout.Behavior
    public void onNestedPreScroll(CoordinatorLayout coordinatorLayout, V v, View view, int i, int i2, int[] iArr) {
        if (view != this.mNestedScrollingChildRef.get()) {
            return;
        }
        int top = v.getTop();
        int i3 = top - i2;
        if (i2 > 0) {
            if (i3 < this.mMinOffset) {
                iArr[1] = top - this.mMinOffset;
                ViewCompat.offsetTopAndBottom(v, -iArr[1]);
                setStateInternal(3);
            } else {
                iArr[1] = i2;
                ViewCompat.offsetTopAndBottom(v, -i2);
                setStateInternal(1);
            }
        } else if (i2 < 0 && !view.canScrollVertically(-1)) {
            if (i3 <= this.mMaxOffset || this.mHideable) {
                iArr[1] = i2;
                ViewCompat.offsetTopAndBottom(v, -i2);
                setStateInternal(1);
            } else {
                iArr[1] = top - this.mMaxOffset;
                ViewCompat.offsetTopAndBottom(v, -iArr[1]);
                setStateInternal(4);
            }
        }
        dispatchOnSlide(v.getTop());
        this.mLastNestedScrollDy = i2;
        this.mNestedScrolled = true;
    }

    @Override // android.support.design.widget.CoordinatorLayout.Behavior
    public void onStopNestedScroll(CoordinatorLayout coordinatorLayout, V v, View view) {
        int i;
        int i2 = 3;
        if (v.getTop() == this.mMinOffset) {
            setStateInternal(3);
            return;
        }
        if (this.mNestedScrollingChildRef != null && view == this.mNestedScrollingChildRef.get() && this.mNestedScrolled) {
            if (this.mLastNestedScrollDy > 0) {
                i = this.mMinOffset;
            } else if (this.mHideable && shouldHide(v, getYVelocity())) {
                i = this.mParentHeight;
                i2 = 5;
            } else {
                if (this.mLastNestedScrollDy == 0) {
                    int top = v.getTop();
                    if (Math.abs(top - this.mMinOffset) < Math.abs(top - this.mMaxOffset)) {
                        i = this.mMinOffset;
                    } else {
                        i = this.mMaxOffset;
                    }
                } else {
                    i = this.mMaxOffset;
                }
                i2 = 4;
            }
            if (this.mViewDragHelper.smoothSlideViewTo(v, v.getLeft(), i)) {
                setStateInternal(2);
                ViewCompat.postOnAnimation(v, new SettleRunnable(v, i2));
            } else {
                setStateInternal(i2);
            }
            this.mNestedScrolled = false;
        }
    }

    @Override // android.support.design.widget.CoordinatorLayout.Behavior
    public boolean onNestedPreFling(CoordinatorLayout coordinatorLayout, V v, View view, float f, float f2) {
        return view == this.mNestedScrollingChildRef.get() && (this.mState != 3 || super.onNestedPreFling(coordinatorLayout, v, view, f, f2));
    }

    /* JADX WARN: Removed duplicated region for block: B:12:0x0015  */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
        To view partially-correct add '--show-bad-code' argument
    */
    public final void setPeekHeight(int r4) {
        /*
            r3 = this;
            r0 = 1
            r1 = 0
            r2 = -1
            if (r4 != r2) goto Lc
            boolean r4 = r3.mPeekHeightAuto
            if (r4 != 0) goto L15
            r3.mPeekHeightAuto = r0
            goto L24
        Lc:
            boolean r2 = r3.mPeekHeightAuto
            if (r2 != 0) goto L17
            int r2 = r3.mPeekHeight
            if (r2 == r4) goto L15
            goto L17
        L15:
            r0 = r1
            goto L24
        L17:
            r3.mPeekHeightAuto = r1
            int r1 = java.lang.Math.max(r1, r4)
            r3.mPeekHeight = r1
            int r1 = r3.mParentHeight
            int r1 = r1 - r4
            r3.mMaxOffset = r1
        L24:
            if (r0 == 0) goto L3c
            int r4 = r3.mState
            r0 = 4
            if (r4 != r0) goto L3c
            java.lang.ref.WeakReference<V extends android.view.View> r4 = r3.mViewRef
            if (r4 == 0) goto L3c
            java.lang.ref.WeakReference<V extends android.view.View> r3 = r3.mViewRef
            java.lang.Object r3 = r3.get()
            android.view.View r3 = (android.view.View) r3
            if (r3 == 0) goto L3c
            r3.requestLayout()
        L3c:
            return
        */
        throw new UnsupportedOperationException("Method not decompiled: android.support.design.widget.BottomSheetBehavior.setPeekHeight(int):void");
    }

    public final int getPeekHeight() {
        if (this.mPeekHeightAuto) {
            return -1;
        }
        return this.mPeekHeight;
    }

    public void setHideable(boolean z) {
        this.mHideable = z;
    }

    public boolean isHideable() {
        return this.mHideable;
    }

    public void setSkipCollapsed(boolean z) {
        this.mSkipCollapsed = z;
    }

    public boolean getSkipCollapsed() {
        return this.mSkipCollapsed;
    }

    public void setBottomSheetCallback(BottomSheetCallback bottomSheetCallback) {
        this.mCallback = bottomSheetCallback;
    }

    public final void setState(final int i) {
        if (i == this.mState) {
            return;
        }
        if (this.mViewRef == null) {
            if (i == 4 || i == 3 || (this.mHideable && i == 5)) {
                this.mState = i;
                return;
            }
            return;
        }
        final V v = this.mViewRef.get();
        if (v == null) {
            return;
        }
        ViewParent parent = v.getParent();
        if (parent != null && parent.isLayoutRequested() && ViewCompat.isAttachedToWindow(v)) {
            v.post(new Runnable() { // from class: android.support.design.widget.BottomSheetBehavior.1
                @Override // java.lang.Runnable
                public void run() {
                    BottomSheetBehavior.this.startSettlingAnimation(v, i);
                }
            });
        } else {
            startSettlingAnimation(v, i);
        }
    }

    public final int getState() {
        return this.mState;
    }

    void setStateInternal(int i) {
        if (this.mState == i) {
            return;
        }
        this.mState = i;
        V v = this.mViewRef.get();
        if (v == null || this.mCallback == null) {
            return;
        }
        this.mCallback.onStateChanged(v, i);
    }

    private void reset() {
        this.mActivePointerId = -1;
        if (this.mVelocityTracker != null) {
            this.mVelocityTracker.recycle();
            this.mVelocityTracker = null;
        }
    }

    boolean shouldHide(View view, float f) {
        if (this.mSkipCollapsed) {
            return true;
        }
        return view.getTop() >= this.mMaxOffset && Math.abs((((float) view.getTop()) + (f * HIDE_FRICTION)) - ((float) this.mMaxOffset)) / ((float) this.mPeekHeight) > HIDE_THRESHOLD;
    }

    @VisibleForTesting
    View findScrollingChild(View view) {
        if (ViewCompat.isNestedScrollingEnabled(view)) {
            return view;
        }
        if (!(view instanceof ViewGroup)) {
            return null;
        }
        ViewGroup viewGroup = (ViewGroup) view;
        int childCount = viewGroup.getChildCount();
        for (int i = 0; i < childCount; i++) {
            View viewFindScrollingChild = findScrollingChild(viewGroup.getChildAt(i));
            if (viewFindScrollingChild != null) {
                return viewFindScrollingChild;
            }
        }
        return null;
    }

    private float getYVelocity() {
        this.mVelocityTracker.computeCurrentVelocity(1000, this.mMaximumVelocity);
        return this.mVelocityTracker.getYVelocity(this.mActivePointerId);
    }

    void startSettlingAnimation(View view, int i) {
        int i2;
        if (i == 4) {
            i2 = this.mMaxOffset;
        } else if (i == 3) {
            i2 = this.mMinOffset;
        } else if (this.mHideable && i == 5) {
            i2 = this.mParentHeight;
        } else {
            throw new IllegalArgumentException("Illegal state argument: " + i);
        }
        if (this.mViewDragHelper.smoothSlideViewTo(view, view.getLeft(), i2)) {
            setStateInternal(2);
            ViewCompat.postOnAnimation(view, new SettleRunnable(view, i));
        } else {
            setStateInternal(i);
        }
    }

    void dispatchOnSlide(int i) {
        V v = this.mViewRef.get();
        if (v == null || this.mCallback == null) {
            return;
        }
        if (i > this.mMaxOffset) {
            this.mCallback.onSlide(v, (this.mMaxOffset - i) / (this.mParentHeight - this.mMaxOffset));
        } else {
            this.mCallback.onSlide(v, (this.mMaxOffset - i) / (this.mMaxOffset - this.mMinOffset));
        }
    }

    @VisibleForTesting
    int getPeekHeightMin() {
        return this.mPeekHeightMin;
    }

    private class SettleRunnable implements Runnable {
        private final int mTargetState;
        private final View mView;

        SettleRunnable(View view, int i) {
            this.mView = view;
            this.mTargetState = i;
        }

        @Override // java.lang.Runnable
        public void run() {
            if (BottomSheetBehavior.this.mViewDragHelper != null && BottomSheetBehavior.this.mViewDragHelper.continueSettling(true)) {
                ViewCompat.postOnAnimation(this.mView, this);
            } else {
                BottomSheetBehavior.this.setStateInternal(this.mTargetState);
            }
        }
    }

    protected static class SavedState extends AbsSavedState {
        public static final Parcelable.Creator<SavedState> CREATOR = new Parcelable.ClassLoaderCreator<SavedState>() { // from class: android.support.design.widget.BottomSheetBehavior.SavedState.1
            /* JADX WARN: Can't rename method to resolve collision */
            @Override // android.os.Parcelable.ClassLoaderCreator
            public SavedState createFromParcel(Parcel parcel, ClassLoader classLoader) {
                return new SavedState(parcel, classLoader);
            }

            @Override // android.os.Parcelable.Creator
            public SavedState createFromParcel(Parcel parcel) {
                return new SavedState(parcel, (ClassLoader) null);
            }

            @Override // android.os.Parcelable.Creator
            public SavedState[] newArray(int i) {
                return new SavedState[i];
            }
        };
        final int state;

        public SavedState(Parcel parcel) {
            this(parcel, (ClassLoader) null);
        }

        public SavedState(Parcel parcel, ClassLoader classLoader) {
            super(parcel, classLoader);
            this.state = parcel.readInt();
        }

        public SavedState(Parcelable parcelable, int i) {
            super(parcelable);
            this.state = i;
        }

        @Override // android.support.v4.view.AbsSavedState, android.os.Parcelable
        public void writeToParcel(Parcel parcel, int i) {
            super.writeToParcel(parcel, i);
            parcel.writeInt(this.state);
        }
    }

    public static <V extends View> BottomSheetBehavior<V> from(V v) {
        ViewGroup.LayoutParams layoutParams = v.getLayoutParams();
        if (!(layoutParams instanceof CoordinatorLayout.LayoutParams)) {
            throw new IllegalArgumentException("The view is not a child of CoordinatorLayout");
        }
        CoordinatorLayout.Behavior behavior = ((CoordinatorLayout.LayoutParams) layoutParams).getBehavior();
        if (!(behavior instanceof BottomSheetBehavior)) {
            throw new IllegalArgumentException("The view is not associated with BottomSheetBehavior");
        }
        return (BottomSheetBehavior) behavior;
    }
}
