package com.bugsnag.android;

public class Error implements JsonStream.Streamable {
    private static String PAYLOAD_VERSION = "2";

    private Configuration config;
    private Diagnostics diagnostics;
    private Throwable exception;
    private Severity severity;
    private MetaData metaData;
    private String groupingHash;
    private String context;

    Error(Configuration config, Diagnostics diagnostics, Throwable exception, Severity severity, MetaData metaData) {
        this.config = config;
        this.diagnostics = diagnostics;
        this.exception = exception;
        this.severity = severity;
        this.metaData = metaData;

        if(this.metaData == null) {
            this.metaData = new MetaData();
        }
    }

    public void toStream(JsonStream writer) {
        writer.beginObject()
            .name("context").value(getContext())
            .name("payloadVersion").value(PAYLOAD_VERSION)
            .name("severity").value(severity);

        // Write exceptions
        writer.name("exceptions").beginArray();
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

        // Write diagnostics
        writer
            .name("app").value(diagnostics.getAppData())
            .name("appState").value(diagnostics.getAppState())
            .name("device").value(diagnostics.getDeviceData())
            .name("deviceState").value(diagnostics.getDeviceState());

        // Write metaData
        // TODO: Merge config.metaData with error.metaData
        // TODO: Apply filters
        writer.name("metaData").value(metaData);

        // TODO: user

        if(groupingHash != null) {
            writer.name("groupingHash").value(groupingHash);
        }

        if(config.sendThreads) {
            writer.name("threads").value(new ThreadState(config));
        }

        writer.endObject();
    }

    public void setContext(String context) {
        this.context = context;
    }

    public void setGroupingHash(String groupingHash) {
        this.groupingHash = groupingHash;
    }

    public void setMetaData(MetaData metaData) {
        this.metaData = metaData;
    }

    public void setSeverity(Severity severity) {
        this.severity = severity;
    }

    public void addToTab(String tabName, String key, Object value) {
        metaData.addToTab(tabName, key, value);
    }

    public void clearTab(String tabName) {
        metaData.clearTab(tabName);
    }

    public String getExceptionName() {
        return exception.getClass().getName();
    }

    public String getExceptionMessage() {
        return exception.getLocalizedMessage();
    }

    boolean shouldIgnore() {
        return !config.shouldNotify() || config.shouldIgnore(exception.getClass().getName());
    }

    private String getContext() {
        if(context != null) {
            return context;
        } else {
            return config.context;
        }
    }
}
