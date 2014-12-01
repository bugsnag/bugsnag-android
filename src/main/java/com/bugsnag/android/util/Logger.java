package com.bugsnag.android;

import android.util.Log;

class Logger {
    private static final String LOG_TAG = "Bugsnag";

    static void info(String message) {
        Log.i(LOG_TAG, message);
    }

    static void warn(String message) {
        Log.w(LOG_TAG, message);
    }

    static void warn(String message, Throwable e) {
        Log.w(LOG_TAG, message, e);
    }
}
