package tchelicon.com.blenderappandroid;

import android.os.CountDownTimer;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;
import tchelicon.com.blenderappandroid.BLECommand;

/* JADX INFO: loaded from: classes.dex */
public class BLECommandQueue extends Thread {
    private static final String TAG = "BLECommandQueue";
    public static int timeoutDuration = 200;
    BLECommandQueueCallback callback;
    BLECommand lastCommandAttemptedToSend;
    CountDownTimer timeoutTimer;
    ReentrantLock queueLock = new ReentrantLock();
    Condition CommandAvailable = this.queueLock.newCondition();
    Condition CommandComplete = this.queueLock.newCondition();
    List<BLECommand> queue = new ArrayList();
    boolean quitting = false;

    public void init(BLECommandQueueCallback bLECommandQueueCallback) {
        this.callback = bLECommandQueueCallback;
    }

    @Override // java.lang.Thread, java.lang.Runnable
    public void run() {
        this.queueLock.lock();
        while (!this.quitting) {
            try {
                try {
                    if (this.queue.size() == 0) {
                        this.CommandAvailable.await();
                    }
                    if (this.quitting) {
                        break;
                    }
                    popSend();
                    this.CommandComplete.await();
                } catch (InterruptedException unused) {
                    Log.d(TAG, "Run InterruptedException");
                }
            } finally {
                this.queueLock.unlock();
            }
        }
    }

    public void queue(BLECommand bLECommand) {
        this.queueLock.lock();
        try {
            this.queue.add(bLECommand);
            this.CommandAvailable.signal();
        } finally {
            this.queueLock.unlock();
        }
    }

    public void popSend() {
        this.queueLock.lock();
        try {
            BLECommand bLECommandRemove = this.queue.remove(0);
            this.lastCommandAttemptedToSend = bLECommandRemove;
            sendCommandToAndroid(bLECommandRemove);
        } finally {
            this.queueLock.unlock();
        }
    }

    public void sendCommandToAndroid(final BLECommand bLECommand) {
        startTimer();
        if (this.callback != null) {
            new Handler(Looper.getMainLooper()).post(new Runnable() { // from class: tchelicon.com.blenderappandroid.BLECommandQueue.1
                @Override // java.lang.Runnable
                public void run() {
                    BLECommandQueue.this.callback.executeCommand(bLECommand);
                }
            });
        }
    }

    public void commandCompleted() {
        if (this.timeoutTimer != null) {
            this.timeoutTimer.cancel();
        }
        this.queueLock.lock();
        try {
            this.CommandComplete.signal();
        } finally {
            this.queueLock.unlock();
        }
    }

    public void startTimer() {
        new Handler(Looper.getMainLooper()).post(new Runnable() { // from class: tchelicon.com.blenderappandroid.BLECommandQueue.2
            /* JADX WARN: Type inference failed for: r7v0, types: [tchelicon.com.blenderappandroid.BLECommandQueue$2$1] */
            @Override // java.lang.Runnable
            public void run() {
                if (BLECommandQueue.this.timeoutTimer != null) {
                    BLECommandQueue.this.timeoutTimer.cancel();
                }
                BLECommandQueue.this.timeoutTimer = new CountDownTimer(BLECommandQueue.timeoutDuration, BLECommandQueue.timeoutDuration) { // from class: tchelicon.com.blenderappandroid.BLECommandQueue.2.1
                    @Override // android.os.CountDownTimer
                    public void onTick(long j) {
                    }

                    @Override // android.os.CountDownTimer
                    public void onFinish() {
                        try {
                            if (BLECommandQueue.this.lastCommandAttemptedToSend != null) {
                                Log.d(BLECommandQueue.TAG, "TIMED OUT command " + BLECommandQueue.this.lastCommandAttemptedToSend.command.name());
                            } else {
                                Log.d(BLECommandQueue.TAG, "TIMED OUT command");
                            }
                            BLECommandQueue.this.queueLock.lock();
                            try {
                                BLECommandQueue.this.CommandComplete.signal();
                                BLECommandQueue.this.queueLock.unlock();
                            } catch (Throwable th) {
                                BLECommandQueue.this.queueLock.unlock();
                                throw th;
                            }
                        } catch (Exception e) {
                            Log.e(BLECommandQueue.TAG, "TIMEOUT Error: " + e.toString());
                        }
                    }
                }.start();
            }
        });
    }

    public boolean checkQueueFor(BLECommand.Command command) {
        ReentrantLock reentrantLock;
        this.queueLock.lock();
        try {
            Iterator<BLECommand> it = this.queue.iterator();
            while (it.hasNext()) {
                if (it.next().command.equals(command)) {
                    return true;
                }
            }
            return false;
        } finally {
            this.queueLock.unlock();
        }
    }
}
