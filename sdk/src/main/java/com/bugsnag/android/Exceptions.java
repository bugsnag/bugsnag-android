package com.bugsnag.android;

import android.support.annotation.NonNull;

import java.io.IOException;

/**
 * Unwrap and serialize exception information and any "cause" exceptions.
 */
class Exceptions implements JsonStream.Streamable {
    private final Configuration config;
    private final Throwable exception;

    Exceptions(Configuration config, Throwable exception) {
        this.config = config;
        this.exception = exception;
    }

    @Override
    public void toStream(@NonNull JsonStream writer) throws IOException {
        writer.beginArray();

        // Unwrap any "cause" exceptions
        Throwable currentEx = exception;
        while (currentEx != null) {
            if (currentEx instanceof JsonStream.Streamable) {
                ((JsonStream.Streamable) currentEx).toStream(writer);
            } else {
                exceptionToStream(writer, getExceptionName(currentEx), currentEx.getLocalizedMessage(), currentEx.getStackTrace());
            }
            currentEx = currentEx.getCause();
        }

        writer.endArray();
    }

    /**
     * Get the class name from the exception contained in this Error report.
     */
    private String getExceptionName(@NonNull Throwable t) {
        if (t instanceof BugsnagException) {
            return ((BugsnagException) t).getName();
        } else {
            return t.getClass().getName();
        }
    }

    private void exceptionToStream(@NonNull JsonStream writer, String name, String message, StackTraceElement[] frames) throws IOException {
        Stacktrace stacktrace = new Stacktrace(config, frames);
        writer.beginObject();
        writer.name("errorClass").value(name);
        writer.name("message").value(message);
        writer.name("type").value(config.defaultExceptionType);
        writer.name("stacktrace").value(stacktrace);
        writer.endObject();
    }
}
