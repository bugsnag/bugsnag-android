package com.bugsnag.android;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

class Async {

    private static final Executor executor = Executors.newCachedThreadPool();

    static void run(Runnable task) {
        executor.execute(task);
    }
}
