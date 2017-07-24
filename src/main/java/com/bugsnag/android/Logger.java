package com.bugsnag.android;

import android.util.Log;

class Logger {
    private static final String LOG_TAG = "Bugsnag";
    private static boolean enabled = true;

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

    static void warn(String message, Throwable e) {
        if (enabled) {
            Log.w(LOG_TAG, message, e);
        }
    }

    static void setEnabled(boolean enabled) {
        Logger.enabled = enabled;
    }

}
