package com.bugsnag.android;

import android.support.annotation.NonNull;

import java.io.IOException;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Map;
import java.util.Set;

/**
 * Capture and serialize the state of all threads at the time of an exception.
 */
class ThreadState implements JsonStream.Streamable {
    private static final String THREAD_TYPE = "android";

    final Configuration config;

    ThreadState(Configuration config) {
        this.config = config;
    }

    @Override
    public void toStream(@NonNull JsonStream writer) throws IOException {
        long currentId = Thread.currentThread().getId();
        Map<Thread, StackTraceElement[]> liveThreads = Thread.getAllStackTraces();

        Set<Thread> threadSet = liveThreads.keySet();
        Thread[] keys = threadSet.toArray(new Thread[threadSet.size()]);
        Arrays.sort(keys, new Comparator<Thread>() {
            public int compare(@NonNull Thread a, @NonNull Thread b) {
                return Long.valueOf(a.getId()).compareTo(b.getId());
            }
        });

        writer.beginArray();
        for (Thread thread : keys) {
            // Don't show the current stacktrace here. It'll point at this method
            // rather than at the point they crashed.
            if (thread.getId() != currentId) {
                StackTraceElement[] stacktrace = liveThreads.get(thread);

                writer.beginObject();
                writer.name("id").value(thread.getId());
                writer.name("name").value(thread.getName());
                writer.name("type").value(THREAD_TYPE);
                writer.name("stacktrace").value(new Stacktrace(config, stacktrace));
                writer.endObject();
            }
        }
        writer.endArray();
    }
}
