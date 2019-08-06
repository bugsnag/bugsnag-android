package com.bugsnag.android;

import android.support.annotation.NonNull;

import java.io.IOException;

/**
 * Unwrap and serialize exception information and any "cause" exceptions.
 */
class Exceptions implements JsonStream.Streamable {
    private final BugsnagException exception;
    private String exceptionType;
    private String[] projectPackages;

    Exceptions(Configuration config, BugsnagException exception) {
        this.exception = exception;
        exceptionType = Configuration.DEFAULT_EXCEPTION_TYPE;
        projectPackages = config.getProjectPackages();
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
                String exceptionName = getExceptionName(currentEx);
                String localizedMessage = currentEx.getLocalizedMessage();
                StackTraceElement[] stackTrace = currentEx.getStackTrace();
                exceptionToStream(writer, exceptionName, localizedMessage, stackTrace);
            }
            currentEx = currentEx.getCause();
        }

        writer.endArray();
    }

    BugsnagException getException() {
        return exception;
    }

    String getExceptionType() {
        return exceptionType;
    }

    void setExceptionType(@NonNull String type) {
        exceptionType = type;
    }

    String[] getProjectPackages() {
        return projectPackages;
    }

    void setProjectPackages(String[] projectPackages) {
        this.projectPackages = projectPackages;
    }

    /**
     * Get the class name from the exception contained in this Error report.
     */
    private String getExceptionName(@NonNull Throwable throwable) {
        if (throwable instanceof BugsnagException) {
            return ((BugsnagException) throwable).getName();
        } else {
            return throwable.getClass().getName();
        }
    }

    private void exceptionToStream(@NonNull JsonStream writer,
                                   String name,
                                   String message,
                                   StackTraceElement[] frames) throws IOException {
        writer.beginObject();
        writer.name("errorClass").value(name);
        writer.name("message").value(message);
        writer.name("type").value(exceptionType);

        Stacktrace stacktrace = new Stacktrace(frames, projectPackages);
        writer.name("stacktrace").value(stacktrace);
        writer.endObject();
    }
}
