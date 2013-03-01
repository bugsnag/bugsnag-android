package com.bugsnag.android;

import android.util.Log;

class Logger extends com.bugsnag.Logger {
    @Override
    public void debug(String message) {
        Log.d(LOG_PREFIX, message);
    }

    @Override
    public void info(String message) {
        Log.i(LOG_PREFIX, message);
    }

    @Override
    public void warn(String message) {
        Log.w(LOG_PREFIX, message);
    }

    @Override
    public void warn(String message, Throwable e) {
        Log.w(LOG_PREFIX, message, e);
    }
}