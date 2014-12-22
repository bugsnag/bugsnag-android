package com.bugsnag.android;

/**
 * Information and associated diagnostics relating to a handled or unhandled
 * Exception.
 *
 * <p>This object is made available in BeforeNotify callbacks, so you can
 * inspect and modify it before it is delivered to Bugsnag.
 *
 * @see BeforeNotify
 */
public class Error implements JsonStream.Streamable {
    private static final String PAYLOAD_VERSION = "2";

    private Configuration config;
    private AppData appData;
    private DeviceData deviceData;
    private AppState appState;
    private DeviceState deviceState;
    private Breadcrumbs breadcrumbs;
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
        writer.beginObject();
            writer.name("payloadVersion").value(PAYLOAD_VERSION);
            writer.name("exceptions").value(new ExceptionChain(config, exception));
            writer.name("context").value(getContext());
            writer.name("severity").value(severity);
            writer.name("metaData").value(mergedMetaData);

            // Write user info
            if(user != null) {
                writer.name("user").value(user);
            }

            // Write diagnostics
            if(appData != null) {
                writer.name("app").value(appData);
            }

            if(appState != null) {
                writer.name("appState").value(appState);
            }

            if(deviceData != null) {
                writer.name("device").value(deviceData);
            }

            if(deviceState != null) {
                writer.name("deviceState").value(deviceState);
            }

            if(breadcrumbs != null) {
                writer.name("breadcrumbs").value(breadcrumbs);
            }

            if(groupingHash != null) {
                writer.name("groupingHash").value(groupingHash);
            }

            if(config.sendThreads) {
                writer.name("threads").value(new ThreadState(config));
            }

        writer.endObject();
    }

    /**
     * Override the context sent to Bugsnag with this Error. By default we'll
     * attempt to detect the name of the top-most Activity when this error
     * occurred, and use this as the context, but sometimes this is not
     * possible.
     *
     * @param  context  what was happening at the time of a crash
     */
    public void setContext(String context) {
        this.context = context;
    }

    /**
     * Get the context associated with this Error.
     */
    public String getContext() {
        if(context != null) {
            return context;
        } else if (config.context != null) {
            return config.context;
        } else if (appState != null){
            return appState.getActiveScreenClass();
        } else {
            return null;
        }
    }

    /**
     * Set a custom grouping hash to use when grouping this Error on the
     * Bugsnag dashboard. By default, we use a combination of error class
     * and top-most stacktrace line to calculate this, and we do not recommend
     * you override this.
     *
     * @param  groupingHash  a string to use when grouping errors
     */
    public void setGroupingHash(String groupingHash) {
        this.groupingHash = groupingHash;
    }

    /**
     * Set the Severity of this Error.
     *
     * By default, unhandled exceptions will be Severity.ERROR and handled
     * exceptions sent with bugsnag.notify will be Severity.WARNING.
     *
     * @param  severity  the severity of this error
     * @see    Severity
     */
    public void setSeverity(Severity severity) {
        this.severity = severity;
    }

    /**
     * Set user information associated with this Error
     *
     * @param  id     the id of the user
     * @param  email  the email address of the user
     * @param  name   the name of the user
     */
    public void setUser(String id, String email, String name) {
        this.user = new User(id, email, name);
    }

    /**
     * Set user id associated with this Error
     *
     * @param  id  the id of the user
     */
    public void setUserId(String id) {
        this.user = new User(this.user);
        this.user.setId(id);
    }

    /**
     * Set user email address associated with this Error
     *
     * @param  email  the email address of the user
     */
    public void setUserEmail(String email) {
        this.user = new User(this.user);
        this.user.setEmail(email);
    }

    /**
     * Set user name associated with this Error
     *
     * @param  name  the name of the user
     */
    public void setUserName(String name) {
        this.user = new User(this.user);
        this.user.setName(name);
    }

    /**
     * Add additional diagnostic information to send with this Error.
     * Diagnostic information is collected in "tabs" on your dashboard.
     *
     * For example:
     *
     *     error.addToTab("account", "name", "Acme Co.");
     *     error.addToTab("account", "payingCustomer", true);
     *
     * @param  tab    the dashboard tab to add diagnostic data to
     * @param  key    the name of the diagnostic information
     * @param  value  the contents of the diagnostic information
     */
    public void addToTab(String tabName, String key, Object value) {
        metaData.addToTab(tabName, key, value);
    }

    /**
     * Remove a tab of app-wide diagnostic information from this Error
     *
     * @param  tabName  the dashboard tab to remove diagnostic data from
     */
    public void clearTab(String tabName) {
        metaData.clearTab(tabName);
    }

    /**
     * Get any additional diagnostic MetaData currently attached to this Error.
     *
     * This will contain any MetaData set by setMetaData or addToTab.
     *
     * @see Error#setMetaData
     * @see Error#addToTab
     */
    public MetaData getMetaData() {
        return metaData;
    }

    /**
     * Set additional diagnostic MetaData to send with this Error. This will
     * be merged with any global MetaData you set on the Client.
     *
     * Note: This will overwrite any MetaData you provided using
     * Bugsnag.notify, so it is recommended to use addToTab instead.
     *
     * @param  metaData  additional diagnostic data to send with this Error
     * @see    Error#addToTab
     * @see    Error#getMetaData
     */
    public void setMetaData(MetaData metaData) {
        this.metaData = metaData;
    }

    /**
     * Get the class name from the exception contained in this Error report.
     */
    public String getExceptionName() {
        return exception.getClass().getName();
    }

    /**
     * Get the message from the exception contained in this Error report.
     */
    public String getExceptionMessage() {
        return exception.getLocalizedMessage();
    }
    
    /**
     * The {@linkplain Throwable exception} which triggered this Error report.
     */
    public Throwable getException() {
        return exception;
    }

    void setAppData(AppData appData) {
        this.appData = appData;
    }

    void setDeviceData(DeviceData deviceData) {
        this.deviceData = deviceData;
    }

    void setAppState(AppState appState) {
        this.appState = appState;
    }

    void setDeviceState(DeviceState deviceState) {
        this.deviceState = deviceState;
    }

    void setUser(User user) {
        this.user = user;
    }

    void setBreadcrumbs(Breadcrumbs breadcrumbs) {
        this.breadcrumbs = breadcrumbs;
    }

    boolean shouldIgnoreClass() {
        return config.shouldIgnoreClass(exception.getClass().getName());
    }
}
