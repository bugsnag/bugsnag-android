package com.bugsnag.android;

import android.os.AsyncTask;

class Async {
    public static void run(final Runnable task) {
        new AsyncTask <Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... voi) {
                task.run();
                return null;
            }
        }.execute();
    }
}
