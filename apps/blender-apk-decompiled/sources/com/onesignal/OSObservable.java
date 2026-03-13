package com.onesignal;

import java.lang.ref.WeakReference;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/* JADX INFO: loaded from: classes.dex */
class OSObservable<ObserverType, StateType> {
    private boolean fireOnMainThread;
    private String methodName;
    private List<Object> observers = new ArrayList();

    OSObservable(String str, boolean z) {
        this.methodName = str;
        this.fireOnMainThread = z;
    }

    void addObserver(ObserverType observertype) {
        this.observers.add(new WeakReference(observertype));
    }

    void addObserverStrong(ObserverType observertype) {
        this.observers.add(observertype);
    }

    void removeObserver(ObserverType observertype) {
        for (int i = 0; i < this.observers.size(); i++) {
            if (((WeakReference) this.observers.get(i)).get().equals(observertype)) {
                this.observers.remove(i);
                return;
            }
        }
    }

    boolean notifyChange(final StateType statetype) {
        Iterator<Object> it = this.observers.iterator();
        boolean z = false;
        while (it.hasNext()) {
            final Object next = it.next();
            if (next instanceof WeakReference) {
                next = ((WeakReference) next).get();
            }
            if (next != null) {
                try {
                    final Method declaredMethod = next.getClass().getDeclaredMethod(this.methodName, statetype.getClass());
                    declaredMethod.setAccessible(true);
                    if (this.fireOnMainThread) {
                        OSUtils.runOnMainUIThread(new Runnable() { // from class: com.onesignal.OSObservable.1
                            @Override // java.lang.Runnable
                            public void run() {
                                try {
                                    declaredMethod.invoke(next, statetype);
                                } catch (Throwable th) {
                                    th.printStackTrace();
                                }
                            }
                        });
                    } else {
                        declaredMethod.invoke(next, statetype);
                    }
                    z = true;
                } catch (Throwable th) {
                    th.printStackTrace();
                }
            }
        }
        return z;
    }
}
