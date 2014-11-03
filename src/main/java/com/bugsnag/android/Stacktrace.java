package com.bugsnag.android;

class Stacktrace implements JsonStreamer.Streamable {
    Configuration config;
    StackTraceElement[] stacktrace;

    public Stacktrace(Configuration config, StackTraceElement[] stacktrace) {
        this.config = config;
        this.stacktrace = stacktrace;
    }

    public void toStream(JsonStreamer writer) {
        writer.beginArray();

        for(StackTraceElement el : stacktrace) {
            try {
                writer.beginObject()
                    .name("method").value(el.getClassName() + "." + el.getMethodName())
                    .name("file").value(el.getFileName() == null ? "Unknown" : el.getFileName())
                    .name("lineNumber").value(el.getLineNumber());

                // Check if line is inProject
                if(config.projectPackages != null) {
                    for(String packageName : config.projectPackages) {
                        if(packageName != null && el.getClassName().startsWith(packageName)) {
                            writer.name("inProject").value(true);
                            break;
                        }
                    }
                }

                writer.endObject();
            } catch(Exception lineEx) {
                lineEx.printStackTrace(System.err);
            }
        }

        writer.endArray();
    }
}
