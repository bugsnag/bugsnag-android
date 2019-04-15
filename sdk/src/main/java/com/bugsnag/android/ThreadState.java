package com.bugsnag.android;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

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

    private final CachedThread[] cachedThreads;

    public ThreadState(@NonNull Configuration config,
                       @NonNull Thread currentThread,
                       @NonNull Map<Thread, StackTraceElement[]> stackTraces,
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
        Thread[] threads = sortThreadsById(stackTraces);
        this.cachedThreads = new CachedThread[threads.length];
        for (int i = 0; i < threads.length; i++) {
            Thread thread = threads[i];
            this.cachedThreads[i] = new CachedThread(config, thread.getId(), thread.getName(),
                THREAD_TYPE, thread.getId() == currentThreadId,
                stackTraces.get(thread));
        }
    }

    ThreadState(@NonNull CachedThread[] cachedThreads) {
        this.cachedThreads = cachedThreads;
    }

    /**
     * Returns an array of threads sorted by thread id
     *
     * @param liveThreads all live threads
     */
    private Thread[] sortThreadsById(Map<Thread, StackTraceElement[]> liveThreads) {
        Set<Thread> threadSet = liveThreads.keySet();

        Thread[] threads = threadSet.toArray(new Thread[0]);
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
        for (CachedThread thread : cachedThreads) {
            writer.value(thread);
        }
        writer.endArray();
    }
}
