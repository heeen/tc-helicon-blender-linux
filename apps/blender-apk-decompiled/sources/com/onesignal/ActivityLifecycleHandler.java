package com.onesignal;

import android.app.Activity;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import com.onesignal.OneSignal;

/* JADX INFO: loaded from: classes.dex */
class ActivityLifecycleHandler {
    static Activity curActivity;
    static FocusHandlerThread focusHandlerThread = new FocusHandlerThread();
    private static ActivityAvailableListener mActivityAvailableListener;
    static boolean nextResumeIsFirstActivity;

    interface ActivityAvailableListener {
        void available(Activity activity);
    }

    static void onActivityCreated(Activity activity) {
    }

    static void onActivityStarted(Activity activity) {
    }

    ActivityLifecycleHandler() {
    }

    static void setActivityAvailableListener(ActivityAvailableListener activityAvailableListener) {
        if (curActivity != null) {
            activityAvailableListener.available(curActivity);
            mActivityAvailableListener = activityAvailableListener;
        } else {
            mActivityAvailableListener = activityAvailableListener;
        }
    }

    public static void removeActivityAvailableListener(ActivityAvailableListener activityAvailableListener) {
        mActivityAvailableListener = null;
    }

    private static void setCurActivity(Activity activity) {
        curActivity = activity;
        if (mActivityAvailableListener != null) {
            mActivityAvailableListener.available(curActivity);
        }
    }

    static void onActivityResumed(Activity activity) {
        setCurActivity(activity);
        logCurActivity();
        handleFocus();
    }

    static void onActivityPaused(Activity activity) {
        if (activity == curActivity) {
            curActivity = null;
            handleLostFocus();
        }
        logCurActivity();
    }

    static void onActivityStopped(Activity activity) {
        OneSignal.Log(OneSignal.LOG_LEVEL.DEBUG, "onActivityStopped: " + activity.getClass().getName());
        if (activity == curActivity) {
            curActivity = null;
            handleLostFocus();
        }
        logCurActivity();
    }

    static void onActivityDestroyed(Activity activity) {
        OneSignal.Log(OneSignal.LOG_LEVEL.DEBUG, "onActivityDestroyed: " + activity.getClass().getName());
        if (activity == curActivity) {
            curActivity = null;
            handleLostFocus();
        }
        logCurActivity();
    }

    private static void logCurActivity() {
        String str;
        OneSignal.LOG_LEVEL log_level = OneSignal.LOG_LEVEL.DEBUG;
        StringBuilder sb = new StringBuilder();
        sb.append("curActivity is NOW: ");
        if (curActivity != null) {
            str = "" + curActivity.getClass().getName() + ":" + curActivity;
        } else {
            str = "null";
        }
        sb.append(str);
        OneSignal.Log(log_level, sb.toString());
    }

    private static void handleLostFocus() {
        focusHandlerThread.runRunnable(new AppFocusRunnable());
    }

    private static void handleFocus() {
        if (focusHandlerThread.hasBackgrounded() || nextResumeIsFirstActivity) {
            nextResumeIsFirstActivity = false;
            focusHandlerThread.resetBackgroundState();
            OneSignal.onAppFocus();
            return;
        }
        focusHandlerThread.stopScheduledRunnable();
    }

    static class FocusHandlerThread extends HandlerThread {
        private AppFocusRunnable appFocusRunnable;
        Handler mHandler;

        FocusHandlerThread() {
            super("FocusHandlerThread");
            this.mHandler = null;
            start();
            this.mHandler = new Handler(getLooper());
        }

        Looper getHandlerLooper() {
            return this.mHandler.getLooper();
        }

        void resetBackgroundState() {
            if (this.appFocusRunnable != null) {
                this.appFocusRunnable.backgrounded = false;
            }
        }

        void stopScheduledRunnable() {
            this.mHandler.removeCallbacksAndMessages(null);
        }

        void runRunnable(AppFocusRunnable appFocusRunnable) {
            if (this.appFocusRunnable == null || !this.appFocusRunnable.backgrounded || this.appFocusRunnable.completed) {
                this.appFocusRunnable = appFocusRunnable;
                this.mHandler.removeCallbacksAndMessages(null);
                this.mHandler.postDelayed(appFocusRunnable, 2000L);
            }
        }

        boolean hasBackgrounded() {
            return this.appFocusRunnable != null && this.appFocusRunnable.backgrounded;
        }
    }

    private static class AppFocusRunnable implements Runnable {
        private boolean backgrounded;
        private boolean completed;

        private AppFocusRunnable() {
        }

        @Override // java.lang.Runnable
        public void run() {
            if (ActivityLifecycleHandler.curActivity != null) {
                return;
            }
            this.backgrounded = true;
            OneSignal.onAppLostFocus();
            this.completed = true;
        }
    }
}
