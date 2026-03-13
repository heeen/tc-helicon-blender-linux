package com.onesignal;

import android.app.Activity;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import com.onesignal.ActivityLifecycleHandler;
import com.onesignal.AndroidSupportV4Compat;

/* JADX INFO: loaded from: classes.dex */
public class PermissionsActivity extends Activity {
    private static final int REQUEST_LOCATION = 2;
    private static ActivityLifecycleHandler.ActivityAvailableListener activityAvailableListener;
    static boolean answered;
    static boolean waiting;

    @Override // android.app.Activity
    protected void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        OneSignal.setAppContext(this);
        if (bundle != null && bundle.getBoolean("android:hasCurrentPermissionsRequest", false)) {
            waiting = true;
        } else {
            requestPermission();
        }
    }

    @Override // android.app.Activity
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        if (OneSignal.initDone) {
            requestPermission();
        }
    }

    private void requestPermission() {
        if (Build.VERSION.SDK_INT < 23) {
            finish();
        } else {
            if (waiting) {
                return;
            }
            waiting = true;
            AndroidSupportV4Compat.ActivityCompat.requestPermissions(this, new String[]{LocationGMS.requestPermission}, 2);
        }
    }

    @Override // android.app.Activity
    public void onRequestPermissionsResult(int i, @NonNull String[] strArr, @NonNull int[] iArr) {
        answered = true;
        waiting = false;
        if (i == 2) {
            if (iArr.length > 0 && iArr[0] == 0) {
                LocationGMS.startGetLocation();
            } else {
                LocationGMS.fireFailedComplete();
            }
        }
        ActivityLifecycleHandler.removeActivityAvailableListener(activityAvailableListener);
        finish();
    }

    static void startPrompt() {
        if (waiting || answered) {
            return;
        }
        activityAvailableListener = new ActivityLifecycleHandler.ActivityAvailableListener() { // from class: com.onesignal.PermissionsActivity.1
            @Override // com.onesignal.ActivityLifecycleHandler.ActivityAvailableListener
            public void available(Activity activity) {
                if (activity.getClass().equals(PermissionsActivity.class)) {
                    return;
                }
                Intent intent = new Intent(activity, (Class<?>) PermissionsActivity.class);
                intent.setFlags(131072);
                activity.startActivity(intent);
            }
        };
        ActivityLifecycleHandler.setActivityAvailableListener(activityAvailableListener);
    }
}
