package com.bugsnag.android;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

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

    private final Thread[] threads;

    public ThreadState(@NonNull ImmutableConfig config,
                       @NonNull java.lang.Thread currentThread,
                       @NonNull Map<java.lang.Thread, StackTraceElement[]> stackTraces,
                       @Nullable Throwable exc) {

        // API 24/25 don't record the currentThread, add it in manually
        // https://issuetracker.google.com/issues/64122757
        if (!stackTraces.containsKey(currentThread)) {
            stackTraces.put(currentThread, currentThread.getStackTrace());
        }
        if (exc != null) { // unhandled errors use the exception trace
            stackTraces.put(currentThread, exc.getStackTrace());
        }

        long currentThreadId = currentThread.getId();
        java.lang.Thread[] threads = sortThreadsById(stackTraces);
        this.threads = new Thread[threads.length];
        for (int i = 0; i < threads.length; i++) {
            java.lang.Thread thread = threads[i];
            this.threads[i] = new Thread(config, thread.getId(), thread.getName(),
                THREAD_TYPE, thread.getId() == currentThreadId,
                stackTraces.get(thread));
        }
    }

    ThreadState(@NonNull Thread[] threads) {
        this.threads = threads;
    }

    /**
     * Returns an array of threads sorted by thread id
     *
     * @param liveThreads all live threads
     */
    private java.lang.Thread[] sortThreadsById(Map<java.lang.Thread, StackTraceElement[]> liveThreads) {
        Set<java.lang.Thread> threadSet = liveThreads.keySet();

        java.lang.Thread[] threads = threadSet.toArray(new java.lang.Thread[0]);
        Arrays.sort(threads, new Comparator<java.lang.Thread>() {
            public int compare(@NonNull java.lang.Thread lhs, @NonNull java.lang.Thread rhs) {
                return Long.valueOf(lhs.getId()).compareTo(rhs.getId());
            }
        });
        return threads;
    }

    @Override
    public void toStream(@NonNull JsonStream writer) throws IOException {
        writer.beginArray();
        for (Thread thread : threads) {
            writer.value(thread);
        }
        writer.endArray();
    }
}
