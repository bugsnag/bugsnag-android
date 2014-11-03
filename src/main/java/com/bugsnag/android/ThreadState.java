package com.bugsnag.android;

import java.util.Arrays;
import java.util.Comparator;
import java.util.Map;

class ThreadState implements JsonStreamer.Streamable {
    Configuration config;

    public ThreadState(Configuration config) {
        this.config = config;
    }

    public void toStream(JsonStreamer writer) {
        long currentId = Thread.currentThread().getId();
        Map<Thread,StackTraceElement[]> liveThreads = Thread.getAllStackTraces();

        Object[] keys = liveThreads.keySet().toArray();
        Arrays.sort(keys, new Comparator<Object>(){
            public int compare(Object a, Object b) {
                return new Long(((Thread)a).getId()).compareTo(((Thread)b).getId());
            }
        });

        writer.beginArray();
        for(int i = 0; i < keys.length; i++) {
            Thread thread = (Thread)keys[i];

            // Don't show the current stacktrace here. It'll point at this method
            // rather than at the point they crashed.
            if (thread.getId() != currentId) {
                StackTraceElement[] stacktrace = liveThreads.get(thread);

                writer.beginObject()
                    .name("id").value(thread.getId())
                    .name("name").value(thread.getName())
                    .name("stacktrace").value(new Stacktrace(config, stacktrace))
                .endObject();
            }
        }
        writer.endArray();
    }
}
