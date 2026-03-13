package android.arch.lifecycle;

import android.arch.core.executor.ArchTaskExecutor;
import android.arch.core.internal.SafeIterableMap;
import android.arch.lifecycle.Lifecycle;
import android.support.annotation.MainThread;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import java.util.Map;

/* JADX INFO: loaded from: classes.dex */
public abstract class LiveData<T> {
    private static final Object NOT_SET = new Object();
    static final int START_VERSION = -1;
    private boolean mDispatchInvalidated;
    private boolean mDispatchingValue;
    private final Object mDataLock = new Object();
    private SafeIterableMap<Observer<T>, LiveData<T>.ObserverWrapper> mObservers = new SafeIterableMap<>();
    private int mActiveCount = 0;
    private volatile Object mData = NOT_SET;
    private volatile Object mPendingData = NOT_SET;
    private int mVersion = -1;
    private final Runnable mPostValueRunnable = new Runnable() { // from class: android.arch.lifecycle.LiveData.1
        /* JADX WARN: Multi-variable type inference failed */
        @Override // java.lang.Runnable
        public void run() {
            Object obj;
            synchronized (LiveData.this.mDataLock) {
                obj = LiveData.this.mPendingData;
                LiveData.this.mPendingData = LiveData.NOT_SET;
            }
            LiveData.this.setValue(obj);
        }
    };

    protected void onActive() {
    }

    protected void onInactive() {
    }

    private void considerNotify(LiveData<T>.ObserverWrapper observerWrapper) {
        if (observerWrapper.mActive) {
            if (!observerWrapper.shouldBeActive()) {
                observerWrapper.activeStateChanged(false);
            } else {
                if (observerWrapper.mLastVersion >= this.mVersion) {
                    return;
                }
                observerWrapper.mLastVersion = this.mVersion;
                observerWrapper.mObserver.onChanged((T) this.mData);
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void dispatchingValue(@Nullable LiveData<T>.ObserverWrapper observerWrapper) {
        if (this.mDispatchingValue) {
            this.mDispatchInvalidated = true;
            return;
        }
        this.mDispatchingValue = true;
        do {
            this.mDispatchInvalidated = false;
            if (observerWrapper != null) {
                considerNotify(observerWrapper);
                observerWrapper = null;
            } else {
                SafeIterableMap<Observer<T>, LiveData<T>.ObserverWrapper>.IteratorWithAdditions iteratorWithAdditions = this.mObservers.iteratorWithAdditions();
                while (iteratorWithAdditions.hasNext()) {
                    considerNotify((ObserverWrapper) iteratorWithAdditions.next().getValue());
                    if (this.mDispatchInvalidated) {
                        break;
                    }
                }
            }
        } while (this.mDispatchInvalidated);
        this.mDispatchingValue = false;
    }

    @MainThread
    public void observe(@NonNull LifecycleOwner lifecycleOwner, @NonNull Observer<T> observer) {
        if (lifecycleOwner.getLifecycle().getCurrentState() == Lifecycle.State.DESTROYED) {
            return;
        }
        LifecycleBoundObserver lifecycleBoundObserver = new LifecycleBoundObserver(lifecycleOwner, observer);
        LiveData<T>.ObserverWrapper observerWrapperPutIfAbsent = this.mObservers.putIfAbsent(observer, lifecycleBoundObserver);
        if (observerWrapperPutIfAbsent != null && !observerWrapperPutIfAbsent.isAttachedTo(lifecycleOwner)) {
            throw new IllegalArgumentException("Cannot add the same observer with different lifecycles");
        }
        if (observerWrapperPutIfAbsent != null) {
            return;
        }
        lifecycleOwner.getLifecycle().addObserver(lifecycleBoundObserver);
    }

    @MainThread
    public void observeForever(@NonNull Observer<T> observer) {
        AlwaysActiveObserver alwaysActiveObserver = new AlwaysActiveObserver(observer);
        LiveData<T>.ObserverWrapper observerWrapperPutIfAbsent = this.mObservers.putIfAbsent(observer, alwaysActiveObserver);
        if (observerWrapperPutIfAbsent != null && (observerWrapperPutIfAbsent instanceof LifecycleBoundObserver)) {
            throw new IllegalArgumentException("Cannot add the same observer with different lifecycles");
        }
        if (observerWrapperPutIfAbsent != null) {
            return;
        }
        alwaysActiveObserver.activeStateChanged(true);
    }

    @MainThread
    public void removeObserver(@NonNull Observer<T> observer) {
        assertMainThread("removeObserver");
        LiveData<T>.ObserverWrapper observerWrapperRemove = this.mObservers.remove(observer);
        if (observerWrapperRemove == null) {
            return;
        }
        observerWrapperRemove.detachObserver();
        observerWrapperRemove.activeStateChanged(false);
    }

    @MainThread
    public void removeObservers(@NonNull LifecycleOwner lifecycleOwner) {
        assertMainThread("removeObservers");
        for (Map.Entry<Observer<T>, LiveData<T>.ObserverWrapper> entry : this.mObservers) {
            if (entry.getValue().isAttachedTo(lifecycleOwner)) {
                removeObserver(entry.getKey());
            }
        }
    }

    protected void postValue(T t) {
        boolean z;
        synchronized (this.mDataLock) {
            z = this.mPendingData == NOT_SET;
            this.mPendingData = t;
        }
        if (z) {
            ArchTaskExecutor.getInstance().postToMainThread(this.mPostValueRunnable);
        }
    }

    @MainThread
    protected void setValue(T t) {
        assertMainThread("setValue");
        this.mVersion++;
        this.mData = t;
        dispatchingValue(null);
    }

    @Nullable
    public T getValue() {
        T t = (T) this.mData;
        if (t != NOT_SET) {
            return t;
        }
        return null;
    }

    int getVersion() {
        return this.mVersion;
    }

    public boolean hasObservers() {
        return this.mObservers.size() > 0;
    }

    public boolean hasActiveObservers() {
        return this.mActiveCount > 0;
    }

    class LifecycleBoundObserver extends LiveData<T>.ObserverWrapper implements GenericLifecycleObserver {

        @NonNull
        final LifecycleOwner mOwner;

        LifecycleBoundObserver(@NonNull LifecycleOwner lifecycleOwner, Observer<T> observer) {
            super(observer);
            this.mOwner = lifecycleOwner;
        }

        @Override // android.arch.lifecycle.LiveData.ObserverWrapper
        boolean shouldBeActive() {
            return this.mOwner.getLifecycle().getCurrentState().isAtLeast(Lifecycle.State.STARTED);
        }

        @Override // android.arch.lifecycle.GenericLifecycleObserver
        public void onStateChanged(LifecycleOwner lifecycleOwner, Lifecycle.Event event) {
            if (this.mOwner.getLifecycle().getCurrentState() == Lifecycle.State.DESTROYED) {
                LiveData.this.removeObserver(this.mObserver);
            } else {
                activeStateChanged(shouldBeActive());
            }
        }

        @Override // android.arch.lifecycle.LiveData.ObserverWrapper
        boolean isAttachedTo(LifecycleOwner lifecycleOwner) {
            return this.mOwner == lifecycleOwner;
        }

        @Override // android.arch.lifecycle.LiveData.ObserverWrapper
        void detachObserver() {
            this.mOwner.getLifecycle().removeObserver(this);
        }
    }

    private abstract class ObserverWrapper {
        boolean mActive;
        int mLastVersion = -1;
        final Observer<T> mObserver;

        void detachObserver() {
        }

        boolean isAttachedTo(LifecycleOwner lifecycleOwner) {
            return false;
        }

        abstract boolean shouldBeActive();

        ObserverWrapper(Observer<T> observer) {
            this.mObserver = observer;
        }

        void activeStateChanged(boolean z) {
            if (z == this.mActive) {
                return;
            }
            this.mActive = z;
            boolean z2 = LiveData.this.mActiveCount == 0;
            LiveData.this.mActiveCount += this.mActive ? 1 : -1;
            if (z2 && this.mActive) {
                LiveData.this.onActive();
            }
            if (LiveData.this.mActiveCount == 0 && !this.mActive) {
                LiveData.this.onInactive();
            }
            if (this.mActive) {
                LiveData.this.dispatchingValue(this);
            }
        }
    }

    private class AlwaysActiveObserver extends LiveData<T>.ObserverWrapper {
        @Override // android.arch.lifecycle.LiveData.ObserverWrapper
        boolean shouldBeActive() {
            return true;
        }

        AlwaysActiveObserver(Observer<T> observer) {
            super(observer);
        }
    }

    private static void assertMainThread(String str) {
        if (ArchTaskExecutor.getInstance().isMainThread()) {
            return;
        }
        throw new IllegalStateException("Cannot invoke " + str + " on a background thread");
    }
}
