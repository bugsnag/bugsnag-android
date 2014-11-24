package com.bugsnag.android;

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
            Stacktrace stacktrace = new Stacktrace(config, currentEx.getStackTrace());
            writer.beginObject()
                .name("errorClass").value(currentEx.getClass().getName())
                .name("message").value(currentEx.getLocalizedMessage())
                .name("stacktrace").value(stacktrace)
            .endObject();

            currentEx = currentEx.getCause();
        }

        writer.endArray();
    }
}
