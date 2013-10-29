package com.bugsnag.android.utils;

import android.os.AsyncTask;

import com.bugsnag.android.Logger;

public class Async {
    public static Logger logger = null;

    public static void safeAsync(final Runnable delegate) {
        new AsyncTask <Void, Void, Void>() {
            protected Void doInBackground(Void... voi) {
                try {
                    delegate.run();
                } catch (Exception e) {
                    if(logger != null) logger.warn("Error in bugsnag", e);
                }

                return null;
            }
        }.execute();
    }
}