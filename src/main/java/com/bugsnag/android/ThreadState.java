package com.bugsnag.android;

import java.util.Arrays;
import java.util.Comparator;
import java.util.Map;

/**
 * Capture and serialize the state of all threads at the time of an exception.
 */
class ThreadState implements JsonStream.Streamable {
    Configuration config;

    ThreadState(Configuration config) {
        this.config = config;
    }

    public void toStream(JsonStream writer) {
        long currentId = Thread.currentThread().getId();
        Map<Thread,StackTraceElement[]> liveThreads = Thread.getAllStackTraces();

        Object[] keys = liveThreads.keySet().toArray();
        Arrays.sort(keys, new Comparator<Object>(){
            public int compare(Object a, Object b) {
                return Long.valueOf(((Thread) a).getId()).compareTo(((Thread)b).getId());
            }
        });

        writer.beginArray();
        for(int i = 0; i < keys.length; i++) {
            Thread thread = (Thread)keys[i];

            // Don't show the current stacktrace here. It'll point at this method
            // rather than at the point they crashed.
            if (thread.getId() != currentId) {
                StackTraceElement[] stacktrace = liveThreads.get(thread);

                writer.beginObject();
                    writer.name("id").value(thread.getId());
                    writer.name("name").value(thread.getName());
                    writer.name("stacktrace").value(new Stacktrace(config, stacktrace));
                writer.endObject();
            }
        }
        writer.endArray();
    }
}
