package com.bugsnag.android;

/**
 * Unwrap and serialize exception information and any "cause" exceptions.
 */
class ExceptionChain implements JsonStream.Streamable {
    private Configuration config;
    private Throwable exception;

    ExceptionChain(Configuration config, Throwable exception) {
        this.config = config;
        this.exception = exception;
    }

    public void toStream(JsonStream writer) {
        writer.beginArray();

        Throwable currentEx = exception;
        while(currentEx != null) {
            // Write the current exception
            Stacktrace stacktrace = new Stacktrace(config, currentEx.getStackTrace());
            writer.beginObject();
                writer.name("errorClass").value(currentEx.getClass().getName());
                writer.name("message").value(currentEx.getLocalizedMessage());
                writer.name("stacktrace").value(stacktrace);
            writer.endObject();

            // Get the next "cause" exception
            currentEx = currentEx.getCause();
        }

        writer.endArray();
    }
}
