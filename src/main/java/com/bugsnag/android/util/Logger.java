package com.bugsnag.android;

import android.util.Log;

class Logger {
    private static final String LOG_PREFIX = "Bugsnag";

    static void debug(String message) {
        Log.d(LOG_PREFIX, message);
    }

    static void info(String message) {
        Log.i(LOG_PREFIX, message);
    }

    static void warn(String message) {
        Log.w(LOG_PREFIX, message);
    }

    static void warn(String message, Throwable e) {
        Log.w(LOG_PREFIX, message, e);
    }

    static void warn(Throwable e) {
        warn("error in bugsnag", e);
    }
}
