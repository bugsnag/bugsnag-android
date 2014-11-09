package com.bugsnag.android;

public class Error implements JsonStream.Streamable {
    private static String PAYLOAD_VERSION = "2";

    private Configuration config;
    private Throwable exception;
    private String groupingHash;
    private Severity severity;
    private String context;
    private MetaData metaData;

    Error(Configuration config, Throwable exception, Severity severity, MetaData metaData) {
        this.config = config;
        this.exception = exception;
        this.severity = severity;
        this.metaData = metaData;

        if(this.metaData == null) {
            this.metaData = new MetaData();
        }
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

    boolean shouldIgnore() {
        return !config.shouldNotify() || config.shouldIgnore(exception.getClass().getName());
    }

    public void toStream(JsonStream writer) {
        writer.object()
            .name("context").value(getContext())
            .name("exceptions").value(new ExceptionStack(config, exception))
            .name("payloadVersion").value(PAYLOAD_VERSION)
            .name("severity").value(severity);

        // TODO: metaData
        // TODO: user
        // TODO: diagnostics (app, appState, device, deviceState)

        if(groupingHash != null) {
            writer.name("groupingHash").value(groupingHash);
        }

        if(config.sendThreads) {
            writer.name("threads").value(new ThreadState(config));
        }

        writer.endObject();
    }

    private String getContext() {
        if(context != null) {
            return context;
        } else {
            return config.context;
        }
    }
}
