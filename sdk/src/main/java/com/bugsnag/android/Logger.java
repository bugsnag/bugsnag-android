package com.bugsnag.android;

import android.util.Log;

import com.facebook.infer.annotation.ThreadSafe;

@ThreadSafe
final class Logger {

    private static final String LOG_TAG = "Bugsnag";
    private static volatile boolean enabled = true;

    private Logger() {
    }

    static void info(String message) {
        if (enabled) {
            Log.i(LOG_TAG, message);
        }
    }

    static void warn(String message) {
        if (enabled) {
            Log.w(LOG_TAG, message);
        }
    }

    static void warn(String message, Throwable throwable) {
        if (enabled) {
            Log.w(LOG_TAG, message, throwable);
        }
    }

    static void setEnabled(boolean enabled) {
        Logger.enabled = enabled;
    }

}
