package com.bugsnag.android;

import androidx.annotation.NonNull;

import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * A representation of a thread recorded in a {@link Report}
 */
class CachedThread implements JsonStream.Streamable {

    private final long id;
    private final String name;
    private final String type;
    private final boolean isErrorReportingThread;
    private Stacktrace stacktrace;

    CachedThread(Configuration config, long id, String name, String type,
                 boolean isErrorReportingThread, StackTraceElement[] frames) {
        this(id, name, type, isErrorReportingThread,
                new Stacktrace(frames, config.getProjectPackages()));
    }

    CachedThread(long id, String name, String type,
                 boolean isErrorReportingThread, List<Map<String, Object>> customFrames) {
        this(id, name, type, isErrorReportingThread, new Stacktrace(customFrames));
    }

    private CachedThread(long id, String name, String type,
                         boolean isErrorReportingThread, Stacktrace stackTrace) {
        this.id = id;
        this.name = name;
        this.type = type;
        this.isErrorReportingThread = isErrorReportingThread;
        this.stacktrace = stackTrace;
    }

    @Override
    public void toStream(@NonNull JsonStream writer) throws IOException {
        writer.beginObject();
        writer.name("id").value(id);
        writer.name("name").value(name);
        writer.name("type").value(type);
        writer.name("stacktrace").value(stacktrace);
        if (isErrorReportingThread) {
            writer.name("errorReportingThread").value(true);
        }
        writer.endObject();
    }
}
