package com.bugsnag.android;

import android.util.Log;

public class Logger {
    protected static final String LOG_PREFIX = "Bugsnag";

    public static void debug(String message) {
        Log.d(LOG_PREFIX, message);
    }

    public static void info(String message) {
        Log.i(LOG_PREFIX, message);
    }

    public static void warn(String message) {
        Log.w(LOG_PREFIX, message);
    }

    public static void warn(String message, Throwable e) {
        Log.w(LOG_PREFIX, message, e);
    }

    public static void warn(Throwable e) {
        warn("error in bugsnag", e);
    }
}
