package com.bugsnag.android;

public class Error implements JsonStream.Streamable {
    private static final String PAYLOAD_VERSION = "2";

    private Configuration config;
    private Diagnostics diagnostics;
    private User user;
    private Throwable exception;
    private Severity severity = Severity.WARNING;
    private MetaData metaData = new MetaData();
    private String groupingHash;
    private String context;

    Error(Configuration config, Throwable exception) {
        this.config = config;
        this.exception = exception;
    }

    public void toStream(JsonStream writer) {
        // Merge error metaData into global metadata and apply filters
        MetaData mergedMetaData = MetaData.merge(config.metaData, metaData);
        mergedMetaData.setFilters(config.filters);

        // Write error basics
        writer.beginObject()
            .name("payloadVersion").value(PAYLOAD_VERSION)
            .name("exceptions").value(new ExceptionChain(config, exception))
            .name("context").value(getContext())
            .name("severity").value(severity)
            .name("metaData").value(mergedMetaData);

            // Write user info
            if(user != null) {
                writer.name("user").value(user);
            }

            // Write diagnostics
            if(diagnostics != null) {
                writer
                    .name("app").value(diagnostics.getAppData())
                    .name("appState").value(diagnostics.getAppState())
                    .name("device").value(diagnostics.getDeviceData())
                    .name("deviceState").value(diagnostics.getDeviceState());
            }

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

    public String getContext() {
        if(context != null) {
            return context;
        } else {
            return config.context;
        }
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

    public String getExceptionName() {
        return exception.getClass().getName();
    }

    public String getExceptionMessage() {
        return exception.getLocalizedMessage();
    }

    void setDiagnostics(Diagnostics diagnostics) {
        this.diagnostics = diagnostics;
    }

    void setUser(User user) {
        this.user = user;
    }

    boolean shouldIgnoreClass() {
        return config.shouldIgnoreClass(exception.getClass().getName());
    }
}
