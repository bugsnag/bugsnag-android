package com.bugsnag.android;

import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;

/**
 * Detects whether a given thread is blocked by continuously posting a {@link Runnable} to it
 * from a watcher thread, invoking a delegate if the message is not processed within
 * a configured interval.
 */
final class BlockedThreadDetector {

    private static final int DEFAULT_CHECK_INTERVAL_MS = 15;

    interface Delegate {

        /**
         * Invoked when a given thread has been unable to execute a {@link Runnable} within
         * the {@link #blockedThresholdMs}
         *
         * @param thread the thread being monitored
         */
        void onThreadBlocked(Thread thread);
    }

    private final Delegate delegate;
    private final Looper looper;
    private final long checkIntervalMs;
    private final long blockedThresholdMs;
    private final Handler handler;

    private volatile long lastUpdateMs;

    BlockedThreadDetector(long blockedThresholdMs,
                          Looper looper,
                          Delegate delegate) {
        this(blockedThresholdMs, DEFAULT_CHECK_INTERVAL_MS, looper, delegate);
    }

    BlockedThreadDetector(long blockedThresholdMs,
                          long checkIntervalMs,
                          Looper looper,
                          Delegate delegate) {
        if ((blockedThresholdMs <= 0 || checkIntervalMs <= 0
            || looper == null || delegate == null
            || checkIntervalMs > blockedThresholdMs)) {
            throw new IllegalArgumentException();
        }
        this.blockedThresholdMs = blockedThresholdMs;
        this.checkIntervalMs = checkIntervalMs;
        this.looper = looper;
        this.delegate = delegate;
        this.handler = new Handler(looper);
    }

    private void updateLivenessTimestamp() {
        lastUpdateMs = SystemClock.elapsedRealtime();
    }

    void start() {
        updateLivenessTimestamp();
        watcherThread.start();
    }

    final Runnable livenessCheck = new Runnable() {
        @Override
        public void run() {
            updateLivenessTimestamp();
        }
    };

    final Thread watcherThread = new Thread() {
        @Override
        public void run() {
        }
    };
}
