package com.bugsnag.android;

import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.support.annotation.NonNull;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

final class AppNotRespondingMonitor {

    private static final int CHECK_INTERVAL_MS = 5;

    interface Delegate {

        /**
         * Invoked when an ANR occurs
         *
         * @param mainThread the thread being monitored
         */
        void onAppNotResponding(Thread mainThread);
    }

    private final ByteBuffer sentinelBuffer;
    private final Handler threadHandler;
    private final Delegate delegate;
    // Character written by the SIGQUIT handler to indicate that an ANR has occurred
    private static final char anrIndicator = 'a';

    private final Runnable checker = new Runnable() {
        @Override
        public void run() {
            try {
                ByteBuffer buffer = getSentinelBuffer();
                char indicator = buffer.getChar(0);
                if (indicator == anrIndicator) {
                    getDelegate().onAppNotResponding(Looper.getMainLooper().getThread());
                    buffer.putChar(0, '\0');
                }
            } catch (IndexOutOfBoundsException ex) {
                // Buffer is empty
            } finally {
                getHandler().postDelayed(this, CHECK_INTERVAL_MS);
            }
        }
    };

    AppNotRespondingMonitor(@NonNull Delegate delegate) {
        sentinelBuffer = ByteBuffer.allocateDirect(8).order(ByteOrder.nativeOrder());
        HandlerThread watchdogHandlerThread = new HandlerThread("bugsnag-anr-watchdog");
        watchdogHandlerThread.start();
        threadHandler = new Handler(watchdogHandlerThread.getLooper());
        this.delegate = delegate;
    }

    void start() {
        threadHandler.postDelayed(checker, CHECK_INTERVAL_MS);
    }

    @NonNull
    Handler getHandler() {
        return threadHandler;
    }

    @NonNull
    Delegate getDelegate() {
        return delegate;
    }

    @NonNull
    ByteBuffer getSentinelBuffer() {
        return sentinelBuffer;
    }
}
