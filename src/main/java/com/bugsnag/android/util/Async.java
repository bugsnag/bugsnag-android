package com.bugsnag.android;

import android.os.AsyncTask;

class Async {
    static void run(final Runnable task) {
        new AsyncTask <Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... voi) {
                try {
                    task.run();
                } catch (Exception e) {
                    Logger.warn("Error running async task", e);
                }
                return null;
            }
        }.execute();
    }
}
