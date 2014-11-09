package com.bugsnag.android;

class ExceptionStack implements JsonStream.Streamable {
    Configuration config;
    Throwable exception;

    public ExceptionStack(Configuration config, Throwable exception) {
        this.config = config;
        this.exception = exception;
    }

    public void toStream(JsonStream writer) {
        writer.array();

        Throwable currentEx = this.exception;
        while(currentEx != null) {
            Stacktrace stacktrace = new Stacktrace(config, currentEx.getStackTrace());
            writer.object()
                .name("errorClass").value(currentEx.getClass().getName())
                .name("message").value(currentEx.getLocalizedMessage())
                .name("stacktrace").value(stacktrace)
            .endObject();

            currentEx = currentEx.getCause();
        }

        writer.endArray();
    }
}
