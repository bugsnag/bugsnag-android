package com.bugsnag.android;

import android.os.AsyncTask;

class Async {
    static void run(final Runnable task, boolean synchronous) {
        if(synchronous) {
            task.run();
        } else {
            new AsyncTask <Void, Void, Void>() {
                @Override
                protected Void doInBackground(Void... voi) {
                    task.run();
                    return null;
                }
            }.execute();
        }
    }
}
