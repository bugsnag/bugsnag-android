package com.bugsnag.android;

/**
 * Serialize an exception stacktrace and mark frames as "in-project"
 * where appropriate.
 */
class Stacktrace implements JsonStream.Streamable {
    Configuration config;
    StackTraceElement[] stacktrace;

    Stacktrace(Configuration config, StackTraceElement[] stacktrace) {
        this.config = config;
        this.stacktrace = stacktrace;
    }

    public void toStream(JsonStream writer) {
        writer.beginArray();

        for(StackTraceElement el : stacktrace) {
            try {
                writer.beginObject();
                    writer.name("method").value(el.getClassName() + "." + el.getMethodName());
                    writer.name("file").value(el.getFileName() == null ? "Unknown" : el.getFileName());
                    writer.name("lineNumber").value(el.getLineNumber());

                    if(config.inProject(el.getClassName())) {
                        writer.name("inProject").value(true);
                    }

                writer.endObject();
            } catch(Exception lineEx) {
                lineEx.printStackTrace(System.err);
            }
        }

        writer.endArray();
    }
}
