package android.support.v13.view;

import android.app.Activity;
import android.os.Build;
import android.support.annotation.Nullable;
import android.support.annotation.RestrictTo;
import android.view.DragAndDropPermissions;
import android.view.DragEvent;

/* JADX INFO: loaded from: classes.dex */
public final class DragAndDropPermissionsCompat {
    private Object mDragAndDropPermissions;

    private DragAndDropPermissionsCompat(Object obj) {
        this.mDragAndDropPermissions = obj;
    }

    @Nullable
    @RestrictTo({RestrictTo.Scope.LIBRARY_GROUP})
    public static DragAndDropPermissionsCompat request(Activity activity, DragEvent dragEvent) {
        DragAndDropPermissions dragAndDropPermissionsRequestDragAndDropPermissions;
        if (Build.VERSION.SDK_INT < 24 || (dragAndDropPermissionsRequestDragAndDropPermissions = activity.requestDragAndDropPermissions(dragEvent)) == null) {
            return null;
        }
        return new DragAndDropPermissionsCompat(dragAndDropPermissionsRequestDragAndDropPermissions);
    }

    public void release() {
        if (Build.VERSION.SDK_INT >= 24) {
            ((DragAndDropPermissions) this.mDragAndDropPermissions).release();
        }
    }
}
