package com.bugsnag.android;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

    @NonNull
    private Map<String, Object> appData = new HashMap<>();

    @NonNull
    private Map<String, Object> deviceData = new HashMap<>();

    @NonNull
    private User user = new User();

    @Nullable
    private Severity severity;

    @NonNull
    private MetaData metaData = new MetaData();

    @Nullable
    private String groupingHash;

    @Nullable
    private String context;

    private List<String> failureReasons = new ArrayList<>();

    @NonNull
    final Configuration config;
    private String[] projectPackages;
    private final Exceptions exceptions;
    private Breadcrumbs breadcrumbs;
    private final BugsnagException exception;
    private final HandledState handledState;
    private final Session session;
    private final ThreadState threadState;
    private boolean incomplete = false;

    Error(@NonNull Configuration config, @NonNull Throwable exc,
          HandledState handledState, @NonNull Severity severity,
          Session session, ThreadState threadState) {
        this.threadState = threadState;
        this.config = config;

        if (exc instanceof BugsnagException) {
            this.exception = (BugsnagException) exc;
        } else {
            this.exception = new BugsnagException(exc);
        }
        this.handledState = handledState;
        this.severity = severity;
        this.session = session;

        projectPackages = config.getProjectPackages();
        exceptions = new Exceptions(config, exception);
    }

    @Override
    public void toStream(@NonNull JsonStream writer) throws IOException {
        // Merge error metaData into global metadata and apply filters
        MetaData mergedMetaData = MetaData.merge(config.getMetaData(), metaData);

        // Write error basics
        writer.beginObject();
        writer.name("context").value(context);
        writer.name("metaData").value(mergedMetaData);

        writer.name("severity").value(severity);
        writer.name("severityReason").value(handledState);
        writer.name("unhandled").value(handledState.isUnhandled());
        writer.name("incomplete").value(incomplete);

        if (projectPackages != null) {
            writer.name("projectPackages").beginArray();
            for (String projectPackage : projectPackages) {
                writer.value(projectPackage);
            }
            writer.endArray();
        }

        // Write exception info
        writer.name("exceptions").value(exceptions);

        // Write user info
        writer.name("user").value(user);

        // Write diagnostics
        writer.name("app").value(appData);
        writer.name("device").value(deviceData);
        writer.name("breadcrumbs").value(breadcrumbs);
        writer.name("groupingHash").value(groupingHash);

        if (config.getSendThreads()) {
            writer.name("threads").value(threadState);
        }

        if (!failureReasons.isEmpty()) {
            writer.name("failureReasons").beginArray();

            for (String reason : failureReasons) {
                writer.value(reason);
            }
            writer.endArray();
        }

        if (session != null) {
            writer.name("session").beginObject();
            writer.name("id").value(session.getId());
            writer.name("startedAt").value(DateUtils.toIso8601(session.getStartedAt()));

            writer.name("events").beginObject();
            writer.name("handled").value(session.getHandledCount());
            writer.name("unhandled").value(session.getUnhandledCount());
            writer.endObject();
            writer.endObject();
        }

        writer.endObject();
    }

    boolean isIncomplete() {
        return incomplete;
    }

    void setIncomplete(boolean incomplete) {
        this.incomplete = incomplete;
    }

    /**
     * Override the context sent to Bugsnag with this Error. By default we'll
     * attempt to detect the name of the top-most Activity when this error
     * occurred, and use this as the context, but sometimes this is not
     * possible.
     *
     * @param context what was happening at the time of a crash
     */
    public void setContext(@Nullable String context) {
        this.context = context;
    }

    /**
     * Get the context associated with this Error.
     */
    @Nullable
    public String getContext() {
        return context;
    }

    /**
     * Set a custom grouping hash to use when grouping this Error on the
     * Bugsnag dashboard. By default, we use a combination of error class
     * and top-most stacktrace line to calculate this, and we do not recommend
     * you override this.
     *
     * @param groupingHash a string to use when grouping errors
     */
    public void setGroupingHash(@Nullable String groupingHash) {
        this.groupingHash = groupingHash;
    }

    /**
     * Get the grouping hash associated with this Error.
     *
     * @return the grouping hash, if set
     */
    @Nullable
    public String getGroupingHash() {
        return groupingHash;
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
    public void setUser(@Nullable String id, @Nullable String email, @Nullable String name) {
        this.user = new User(id, email, name);
    }

    void setUser(@NonNull User user) {
        this.user = user;
    }

    /**
     * @return user information associated with this Error
     */
    @NonNull
    public User getUser() {
        return user;
    }

    /**
     * Set user id associated with this Error
     *
     * @param id the id of the user
     */
    public void setUserId(@Nullable String id) {
        this.user = new User(this.user);
        this.user.setId(id);
    }

    /**
     * Set user email address associated with this Error
     *
     * @param email the email address of the user
     */
    public void setUserEmail(@Nullable String email) {
        this.user = new User(this.user);
        this.user.setEmail(email);
    }

    /**
     * Set user name associated with this Error
     *
     * @param name the name of the user
     */
    public void setUserName(@Nullable String name) {
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
    public void addToTab(@NonNull String tabName, @NonNull String key, @Nullable Object value) {
        metaData.addToTab(tabName, key, value);
    }

    /**
     * Remove a tab of app-wide diagnostic information from this Error
     *
     * @param tabName the dashboard tab to remove diagnostic data from
     */
    public void clearTab(@NonNull String tabName) {
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
    @NonNull
    public String getExceptionName() {
        return exception.getName();
    }

    /**
     * Sets the class name from the exception contained in this Error report.
     */
    public void setExceptionName(@NonNull String exceptionName) {
        exception.setName(exceptionName);
    }

    /**
     * Get the message from the exception contained in this Error report.
     */
    @NonNull
    public String getExceptionMessage() {
        String msg = exception.getMessage();
        return msg != null ? msg : "";
    }

    /**
     * Sets the message from the exception contained in this Error report.
     */
    public void setExceptionMessage(@NonNull String exceptionMessage) {
        exception.setMessage(exceptionMessage);
    }

    /**
     * The {@linkplain Throwable exception} which triggered this Error report.
     */
    @NonNull
    public Throwable getException() {
        return exception;
    }

    /**
     * Sets the device ID. This can be set to null for privacy concerns.
     *
     * @param id the device id
     */
    public void setDeviceId(@Nullable String id) {
        deviceData.put("id", id);
    }

    /**
     * Retrieves the map of data associated with this error
     *
     * @return the app metadata
     */
    @NonNull
    Map<String, Object> getAppData() {
        return appData;
    }
    /**
     * Retrieves the {@link DeviceData} associated with this error
     *
     * @return the device metadata
     */

    @NonNull
    public Map<String, Object> getDeviceData() {
        return deviceData;
    }

    void setAppData(@NonNull Map<String, Object> appData) {
        this.appData = appData;
    }

    void setDeviceData(@NonNull Map<String, Object> deviceData) {
        this.deviceData = deviceData;
    }

    void setBreadcrumbs(Breadcrumbs breadcrumbs) {
        this.breadcrumbs = breadcrumbs;
    }

    boolean shouldIgnoreClass() {
        return config.shouldIgnoreClass(getExceptionName());
    }

    @NonNull
    public HandledState getHandledState() {
        return handledState;
    }

    Exceptions getExceptions() {
        return exceptions;
    }

    Session getSession() {
        return session;
    }

    String[] getProjectPackages() {
        return projectPackages;
    }

    void setProjectPackages(String[] projectPackages) {
        this.projectPackages = projectPackages;
        if (exceptions != null) {
            exceptions.setProjectPackages(projectPackages);
        }
    }

    void addFailureReason(String failureReason) {
        failureReasons.add(failureReason);
    }

    static class Builder {
        private final Configuration config;
        private final Throwable exception;
        private final SessionTracker sessionTracker;
        private final ThreadState threadState;
        private Severity severity = Severity.WARNING;
        private MetaData metaData;
        private String attributeValue;

        @HandledState.SeverityReason
        private String severityReasonType;

        Builder(@NonNull Configuration config,
                @NonNull Throwable exception,
                SessionTracker sessionTracker,
                @NonNull Thread thread,
                boolean unhandled) {
            Throwable exc = unhandled ? exception : null;
            this.threadState = new ThreadState(config, thread, Thread.getAllStackTraces(), exc);
            this.config = config;
            this.exception = exception;
            this.severityReasonType = HandledState.REASON_USER_SPECIFIED; // default
            this.sessionTracker = sessionTracker;
        }

        Builder(@NonNull Configuration config, @NonNull String name,
                @NonNull String message, @NonNull StackTraceElement[] frames,
                SessionTracker sessionTracker, Thread thread) {
            this(config, new BugsnagException(name, message, frames), sessionTracker,
                thread, false);
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
            Session session = getSession(handledState);

            Error error = new Error(config, exception, handledState,
                severity, session, threadState);

            if (metaData != null) {
                error.setMetaData(metaData);
            }
            return error;
        }

        private Session getSession(HandledState handledState) {
            if (sessionTracker == null) {
                return null;
            }
            Session currentSession = sessionTracker.getCurrentSession();
            Session reportedSession = null;

            if (currentSession == null) {
                return null;
            }
            if (config.getAutoCaptureSessions() || !currentSession.isAutoCaptured()) {
                if (handledState.isUnhandled()) {
                    reportedSession = sessionTracker.incrementUnhandledAndCopy();
                } else {
                    reportedSession = sessionTracker.incrementHandledAndCopy();
                }
            }
            return reportedSession;
        }
    }
}
