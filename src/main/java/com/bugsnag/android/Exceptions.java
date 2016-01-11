package com.bugsnag.android;

import android.support.annotation.NonNull;

import java.io.IOException;

/**
 * Unwrap and serialize exception information and any "cause" exceptions.
 */
class Exceptions implements JsonStream.Streamable {
    private final Configuration config;
    private Throwable exception;
    private String name;
    private String message;
    private StackTraceElement[] frames;

    Exceptions(Configuration config, Throwable exception) {
        this.config = config;
        this.exception = exception;
    }

    Exceptions(Configuration config, String name, String message, StackTraceElement[] frames) {
        this.config = config;
        this.name = name;
        this.message = message;
        this.frames = frames;
    }

    public void toStream(@NonNull JsonStream writer) throws IOException {
        writer.beginArray();

        if(exception != null) {
            // Unwrap any "cause" exceptions
            Throwable currentEx = exception;
            while(currentEx != null) {
                exceptionToStream(writer, currentEx.getClass().getName(), currentEx.getLocalizedMessage(), currentEx.getStackTrace());
                currentEx = currentEx.getCause();
            }
        } else {
            exceptionToStream(writer, name, message, frames);
        }

        writer.endArray();
    }

    private void exceptionToStream(JsonStream writer, String name, String message, StackTraceElement[] frames) throws IOException {
        Stacktrace stacktrace = new Stacktrace(config, frames);
        writer.beginObject();
            writer.name("errorClass").value(name);
            writer.name("message").value(message);
            writer.name("stacktrace").value(stacktrace);
        writer.endObject();
    }
}
