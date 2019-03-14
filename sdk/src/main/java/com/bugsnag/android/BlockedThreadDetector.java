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

    private static final int DEFAULT_CHECK_INTERVAL_MS = 1000;

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
    final Handler handler;
    final Delegate delegate;

    volatile long lastUpdateMs;
    volatile boolean isAlreadyBlocked = false;

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

    void updateLivenessTimestamp() {
        lastUpdateMs = SystemClock.elapsedRealtime();
    }

    void start() {
        updateLivenessTimestamp();
        handler.post(livenessCheck);
        watcherThread.start();
    }

    final Runnable livenessCheck = new Runnable() {
        @Override
        public void run() {
            updateLivenessTimestamp();
            handler.postDelayed(this, checkIntervalMs);
        }
    };

    final Thread watcherThread = new Thread() {
        @Override
        public void run() {
            while (!isInterrupted()) {
                // when we would next consider the app blocked if no timestamp updates take place
                long now = SystemClock.elapsedRealtime();
                long nextCheckIn = Math.max(lastUpdateMs + blockedThresholdMs - now, 0);

                try {
                    Thread.sleep(nextCheckIn); // throttle checks to the configured threshold
                } catch (InterruptedException exc) {
                    interrupt();
                }
                checkIfThreadBlocked();
            }
        }

        private void checkIfThreadBlocked() {
            long delta = SystemClock.elapsedRealtime() - lastUpdateMs;

            if (delta > blockedThresholdMs) {
                if (!isAlreadyBlocked) {
                    delegate.onThreadBlocked(looper.getThread());
                }
                isAlreadyBlocked = true; // prevents duplicate reports for the same ANR
            } else {
                isAlreadyBlocked = false;
            }
        }
    };
}
