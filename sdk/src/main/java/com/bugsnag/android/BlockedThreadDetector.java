package com.bugsnag.android;

import android.app.ActivityManager;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Process;
import android.os.SystemClock;
import android.support.annotation.Nullable;

import java.util.List;

/**
 * Detects whether a given thread is blocked by continuously posting a {@link Runnable} to it
 * from a watcher thread, invoking a delegate if the message is not processed within
 * a configured interval.
 */
final class BlockedThreadDetector {

    static final int MIN_CHECK_INTERVAL_MS = 1000;

    interface Delegate {

        /**
         * Invoked when a given thread has been unable to execute a {@link Runnable} within
         * the {@link #blockedThresholdMs}
         *
         * @param thread the thread being monitored
         */
        void onThreadBlocked(Thread thread);
    }

    final Looper looper;
    final long checkIntervalMs;
    final long blockedThresholdMs;
    final Handler uiHandler;
    final Handler watchdogHandler;
    private final HandlerThread watchdogHandlerThread;
    final Delegate delegate;
    final ActivityManager activityManager;

    volatile long lastUpdateMs;
    volatile boolean isAlreadyBlocked = false;

    BlockedThreadDetector(long blockedThresholdMs,
                          Looper looper,
                          ActivityManager activityManager,
                          Delegate delegate) {
        this(blockedThresholdMs, MIN_CHECK_INTERVAL_MS, looper, activityManager, delegate);
    }

    BlockedThreadDetector(long blockedThresholdMs,
                          long checkIntervalMs,
                          Looper looper,
                          ActivityManager activityManager,
                          Delegate delegate) {
        if ((blockedThresholdMs <= 0 || checkIntervalMs <= 0
            || looper == null || delegate == null)) {
            throw new IllegalArgumentException();
        }
        this.blockedThresholdMs = blockedThresholdMs;
        this.checkIntervalMs = checkIntervalMs;
        this.looper = looper;
        this.delegate = delegate;
        this.uiHandler = new Handler(looper);
        this.activityManager = activityManager;

        watchdogHandlerThread = new HandlerThread("bugsnag-anr-watchdog");
        watchdogHandlerThread.start();
        watchdogHandler = new Handler(watchdogHandlerThread.getLooper());
    }

    void start() {
        updateLivenessTimestamp();
        uiHandler.post(livenessCheck);
        watchdogHandler.postDelayed(watchdogCheck, calculateNextCheckIn());
    }

    void updateLivenessTimestamp() {
        lastUpdateMs = SystemClock.uptimeMillis();
    }

    final Runnable livenessCheck = new Runnable() {
        @Override
        public void run() {
            updateLivenessTimestamp();
            uiHandler.postDelayed(this, checkIntervalMs);
        }
    };

    final Runnable watchdogCheck = new Runnable() {
        @Override
        public void run() {
            checkIfThreadBlocked();
            watchdogHandler.postDelayed(this, calculateNextCheckIn());
        }
    };

    long calculateNextCheckIn() {
        long currentUptimeMs = SystemClock.uptimeMillis();
        return Math.max(lastUpdateMs + blockedThresholdMs - currentUptimeMs, 0);
    }

    void checkIfThreadBlocked() {
        long delta = SystemClock.uptimeMillis() - lastUpdateMs;
        boolean inForeground = isInForeground();

        if (inForeground && delta > blockedThresholdMs) {
            if (!isAlreadyBlocked) {
                delegate.onThreadBlocked(looper.getThread());
            }
            isAlreadyBlocked = true; // prevents duplicate reports for the same ANR
        } else {
            isAlreadyBlocked = false;
        }
    }

    private boolean isInForeground() {
        ActivityManager.RunningAppProcessInfo info = getProcessInfo();

        if (info != null) {
            return info.importance <= ActivityManager.RunningAppProcessInfo.IMPORTANCE_VISIBLE;
        } else { // prefer a potential false positive if process info not available
            return true;
        }
    }

    private ActivityManager.RunningAppProcessInfo getProcessInfo() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            ActivityManager.RunningAppProcessInfo info =
                new ActivityManager.RunningAppProcessInfo();
            ActivityManager.getMyMemoryState(info);
            return info;
        } else {
            return getProcessInfoPreApi16();
        }
    }

    @Nullable
    private ActivityManager.RunningAppProcessInfo getProcessInfoPreApi16() {
        List<ActivityManager.RunningAppProcessInfo> appProcesses;

        try {
            appProcesses = activityManager.getRunningAppProcesses();
        } catch (SecurityException exc) {
            return null;
        }

        if (appProcesses != null) {
            int pid = Process.myPid();

            for (ActivityManager.RunningAppProcessInfo appProcess : appProcesses) {
                if (pid == appProcess.pid) {
                    return appProcess;
                }
            }
        }
        return null;
    }
}
