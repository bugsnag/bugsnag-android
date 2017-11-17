package com.bugsnag.android;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;

import java.io.IOException;

/**
 * Information and associated diagnostics relating to a handled or unhandled
 * Exception.
 * <p>
 * <p>This object is made available in BeforeNotify callbacks, so you can
 * inspect and modify it before it is delivered to Bugsnag.
 *
 * @see BeforeNotify
 */
public class Error implements JsonStream.Streamable {
    private static final String PAYLOAD_VERSION = "3";

    @NonNull
    final Configuration config;
    private AppData appData;
    private DeviceData deviceData;
    private AppState appState;
    private DeviceState deviceState;
    private Breadcrumbs breadcrumbs;
    private User user;
    private final Throwable exception;
    private Severity severity = Severity.WARNING;
    @NonNull private MetaData metaData = new MetaData();
    private String groupingHash;
    private String context;
    private final HandledState handledState;

    Error(@NonNull Configuration config, @NonNull Throwable exception, HandledState handledState, Severity severity) {
        this.config = config;
        this.exception = exception;
        this.handledState = handledState;
        this.severity = severity;
    }

    @Override
    public void toStream(@NonNull JsonStream writer) throws IOException {
        // Merge error metaData into global metadata and apply filters
        MetaData mergedMetaData = MetaData.merge(config.getMetaData(), metaData);

        // Write error basics
        writer.beginObject();
        writer.name("payloadVersion").value(PAYLOAD_VERSION);
        writer.name("context").value(getContext());
        writer.name("metaData").value(mergedMetaData);

        writer.name("severity").value(severity);
        writer.name("severityReason").value(handledState);
        writer.name("unhandled").value(handledState.isUnhandled());

        if (config.getProjectPackages() != null) {
            writer.name("projectPackages").beginArray();
            for (String projectPackage : config.getProjectPackages()) {
                writer.value(projectPackage);
            }
            writer.endArray();
        }

        // Write exception info
        writer.name("exceptions").value(new Exceptions(config, exception));

        // Write user info
        writer.name("user").value(user);

        // Write diagnostics
        writer.name("app").value(appData);
        writer.name("appState").value(appState);
        writer.name("device").value(deviceData);
        writer.name("deviceState").value(deviceState);
        writer.name("breadcrumbs").value(breadcrumbs);
        writer.name("groupingHash").value(groupingHash);
        if (config.getSendThreads()) {
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
     * @param context what was happening at the time of a crash
     */
    public void setContext(String context) {
        this.context = context;
    }

    /**
     * Get the context associated with this Error.
     */
    @Nullable
    public String getContext() {
        if (!TextUtils.isEmpty(context)) {
            return context;
        } else if (config.getContext() != null) {
            return config.getContext();
        } else if (appState != null) {
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
     * @param groupingHash a string to use when grouping errors
     */
    public void setGroupingHash(String groupingHash) {
        this.groupingHash = groupingHash;
    }

    /**
     * Set the Severity of this Error.
     * <p>
     * By default, unhandled exceptions will be Severity.ERROR and handled
     * exceptions sent with bugsnag.notify will be Severity.WARNING.
     *
     * @param severity the severity of this error
     * @see Severity
     */
    public void setSeverity(@Nullable Severity severity) {
        if (severity != null) {
            this.severity = severity;
            this.handledState.setCurrentSeverity(severity);
        }
    }

    /**
     * Get the Severity of this Error.
     *
     * @see Severity
     */
    @Nullable
    public Severity getSeverity() {
        return severity;
    }

    /**
     * Set user information associated with this Error
     *
     * @param id    the id of the user
     * @param email the email address of the user
     * @param name  the name of the user
     */
    public void setUser(String id, String email, String name) {
        this.user = new User(id, email, name);
    }

    /**
     * @return user information associated with this Error
     */
    public User getUser() {
        return user;
    }

    /**
     * Set user id associated with this Error
     *
     * @param id the id of the user
     */
    public void setUserId(String id) {
        this.user = new User(this.user);
        this.user.setId(id);
    }

    /**
     * Set user email address associated with this Error
     *
     * @param email the email address of the user
     */
    public void setUserEmail(String email) {
        this.user = new User(this.user);
        this.user.setEmail(email);
    }

    /**
     * Set user name associated with this Error
     *
     * @param name the name of the user
     */
    public void setUserName(String name) {
        this.user = new User(this.user);
        this.user.setName(name);
    }

    /**
     * Add additional diagnostic information to send with this Error.
     * Diagnostic information is collected in "tabs" on your dashboard.
     * <p>
     * For example:
     * <p>
     * error.addToTab("account", "name", "Acme Co.");
     * error.addToTab("account", "payingCustomer", true);
     *
     * @param tabName the dashboard tab to add diagnostic data to
     * @param key     the name of the diagnostic information
     * @param value   the contents of the diagnostic information
     */
    public void addToTab(String tabName, String key, Object value) {
        metaData.addToTab(tabName, key, value);
    }

    /**
     * Remove a tab of app-wide diagnostic information from this Error
     *
     * @param tabName the dashboard tab to remove diagnostic data from
     */
    public void clearTab(String tabName) {
        metaData.clearTab(tabName);
    }

    /**
     * Get any additional diagnostic MetaData currently attached to this Error.
     * <p>
     * This will contain any MetaData set by setMetaData or addToTab.
     *
     * @see Error#setMetaData
     * @see Error#addToTab
     */
    @NonNull
    public MetaData getMetaData() {
        return metaData;
    }

    /**
     * Set additional diagnostic MetaData to send with this Error. This will
     * be merged with any global MetaData you set on the Client.
     * <p>
     * Note: This will overwrite any MetaData you provided using
     * Bugsnag.notify, so it is recommended to use addToTab instead.
     *
     * @param metaData additional diagnostic data to send with this Error
     * @see Error#addToTab
     * @see Error#getMetaData
     */
    public void setMetaData(@NonNull MetaData metaData) {
        //noinspection ConstantConditions
        if (metaData == null) {
            this.metaData = new MetaData();
        } else {
            this.metaData = metaData;
        }
    }

    /**
     * Get the class name from the exception contained in this Error report.
     */
    public String getExceptionName() {
        if (exception instanceof BugsnagException) {
            return ((BugsnagException) exception).getName();
        } else {
            return exception.getClass().getName();
        }
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

    /**
     * Sets the device ID. This can be set to null for privacy concerns.
     *
     * @param id the device id
     */
    public void setDeviceId(@Nullable String id) {
        deviceData.id = id;
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
        return config.shouldIgnoreClass(getExceptionName());
    }

    static class Builder {
        private final Configuration config;
        private final Throwable exception;
        private Severity severity = Severity.WARNING;
        private MetaData metaData;
        private String attributeValue;

        @HandledState.SeverityReason
        private String severityReasonType;

        Builder(@NonNull Configuration config, @NonNull Throwable exception) {
            this.config = config;
            this.exception = exception;
            this.severityReasonType = HandledState.REASON_USER_SPECIFIED; // default
        }

        Builder(@NonNull Configuration config, @NonNull String name,
               @NonNull String message, @NonNull StackTraceElement[] frames) {
            this(config, new BugsnagException(name, message, frames));
        }

        Builder severityReasonType(@HandledState.SeverityReason String severityReasonType) {
            this.severityReasonType = severityReasonType;
            return this;
        }

        Builder attributeValue(String value) {
            this.attributeValue = value;
            return this;
        }

        Builder severity(Severity severity) {
            this.severity = severity;
            return this;
        }

        Builder metaData(MetaData metaData) {
            this.metaData = metaData;
            return this;
        }

        Error build() {
            HandledState handledState =
                HandledState.newInstance(severityReasonType, severity, attributeValue);
            Error error = new Error(config, exception, handledState, severity);

            if (metaData != null) {
                error.setMetaData(metaData);
            }
            return error;
        }
    }
}
