package com.bugsnag.android;

import android.support.annotation.NonNull;

import java.io.IOException;

/**
 * A representation of a thread recorded in a {@link Report}
 */
class CachedThread implements JsonStream.Streamable {
    private final long id;
    private final String name;
    private final String type;
    private final boolean isErrorReportingThread;
    private final StackTraceElement[] frames;
    private final Configuration config;

    CachedThread(Configuration config, long id, String name, String type,
                 boolean isErrorReportingThread, StackTraceElement[] frames) {
        this.id = id;
        this.config = config;
        this.name = name;
        this.type = type;
        this.isErrorReportingThread = isErrorReportingThread;
        this.frames = frames;
    }

    @Override
    public void toStream(@NonNull JsonStream writer) throws IOException {
        writer.beginObject();
        writer.name("id").value(id);
        writer.name("name").value(name);
        writer.name("type").value(type);
        writer.name("stacktrace").value(new Stacktrace(frames, config.getProjectPackages()));
        if (isErrorReportingThread) {
            writer.name("errorReportingThread").value(true);
        }
        writer.endObject();
    }
}
