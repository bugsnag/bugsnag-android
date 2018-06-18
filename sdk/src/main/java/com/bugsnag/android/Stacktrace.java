package com.bugsnag.android;

import android.support.annotation.NonNull;

import java.io.IOException;

/**
 * Serialize an exception stacktrace and mark frames as "in-project"
 * where appropriate.
 */
class Stacktrace implements JsonStream.Streamable {

    private static final int STACKTRACE_TRIM_LENGTH = 200;

    final Configuration config;
    final StackTraceElement[] stacktrace;

    Stacktrace(Configuration config, StackTraceElement[] stacktrace) {
        this.config = config;
        this.stacktrace = stacktrace;
    }

    @Override
    public void toStream(@NonNull JsonStream writer) throws IOException {
        writer.beginArray();

        for (int k = 0; k < stacktrace.length && k < STACKTRACE_TRIM_LENGTH; k++) {
            StackTraceElement el = stacktrace[k];
            try {
                writer.beginObject();
                writer.name("method").value(el.getClassName() + "." + el.getMethodName());
                writer.name("file").value(el.getFileName() == null ? "Unknown" : el.getFileName());
                writer.name("lineNumber").value(el.getLineNumber());

                if (config.inProject(el.getClassName())) {
                    writer.name("inProject").value(true);
                }

                writer.endObject();
            } catch (Exception lineEx) {
                Logger.warn("Failed to serialize stacktrace", lineEx);
            }
        }

        writer.endArray();
    }
}
