package com.bugsnag.android;

import android.support.annotation.NonNull;

import java.io.IOException;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

/**
 * Capture and serialize the state of all threads at the time of an exception.
 */
class ThreadState implements JsonStream.Streamable {
    private static final String THREAD_TYPE = "android";

    final Configuration config;
    private final Thread[] threads;
    private final Map<Thread, StackTraceElement[]> stackTraces;

    ThreadState(Configuration config) {
        this.config = config;
        stackTraces = Thread.getAllStackTraces();
        threads = sanitiseThreads(Thread.currentThread().getId(), stackTraces);
    }

    /**
     * Returns an array of threads excluding the current thread, sorted by thread id
     *
     * @param currentThreadId the current thread id
     * @param liveThreads     all live threads
     */
    private Thread[] sanitiseThreads(long currentThreadId,
                                     Map<Thread, StackTraceElement[]> liveThreads) {
        Set<Thread> threadSet = liveThreads.keySet();

        // remove current thread
        for (Iterator<Thread> iterator = threadSet.iterator(); iterator.hasNext(); ) {
            Thread thread = iterator.next();
            if (thread.getId() == currentThreadId) {
                iterator.remove();
            }
        }

        Thread[] threads = threadSet.toArray(new Thread[threadSet.size()]);
        Arrays.sort(threads, new Comparator<Thread>() {
            public int compare(@NonNull Thread lhs, @NonNull Thread rhs) {
                return Long.valueOf(lhs.getId()).compareTo(rhs.getId());
            }
        });
        return threads;
    }

    @Override
    public void toStream(@NonNull JsonStream writer) throws IOException {
        writer.beginArray();
        for (Thread thread : threads) {
            writer.beginObject();
            writer.name("id").value(thread.getId());
            writer.name("name").value(thread.getName());
            writer.name("type").value(THREAD_TYPE);

            StackTraceElement[] stacktrace = stackTraces.get(thread);
            writer.name("stacktrace").value(new Stacktrace(config, stacktrace));
            writer.endObject();
        }
        writer.endArray();
    }
}
