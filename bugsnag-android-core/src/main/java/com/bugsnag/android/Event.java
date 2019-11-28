package com.bugsnag.android;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.lang.Thread;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * Information and associated diagnostics relating to a handled or unhandled
 * Exception.
 * <p>
 * <p>This object is made available in OnError callbacks, so you can
 * inspect and modify it before it is delivered to Bugsnag.
 *
 * @see OnError
 */
public class Event implements JsonStream.Streamable, MetadataAware {

    @NonNull
    private Map<String, Object> appData = new HashMap<>();

    @NonNull
    private Map<String, Object> deviceData = new HashMap<>();

    @NonNull
    private User user = new User();

    @Nullable
    private Severity severity;

    @NonNull
    private Metadata metadata;

    @Nullable
    private String groupingHash;

    @Nullable
    private String context;

    @NonNull
    final ImmutableConfig config;
    private Collection<String> projectPackages;
    private final Exceptions exceptions;
    private BreadcrumbState breadcrumbState;
    private final BugsnagException exception;
    private final HandledState handledState;
    private final Session session;
    private final ThreadState threadState;
    private boolean incomplete = false;

    Event(@NonNull ImmutableConfig config, @NonNull Throwable exc,
          HandledState handledState, @NonNull Severity severity,
          Session session, ThreadState threadState, @NonNull Metadata metadata) {
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
        this.metadata = metadata;
        exceptions = new Exceptions(config, exception);
    }

    @Override
    public void toStream(@NonNull JsonStream writer) throws IOException {
        // Write error basics
        writer.beginObject();
        writer.name("context").value(context);
        writer.name("metaData").value(metadata);

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
        writer.name("breadcrumbs").value(breadcrumbState);
        writer.name("groupingHash").value(groupingHash);

        if (config.getSendThreads()) {
            writer.name("threads").value(threadState);
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
     * Override the context sent to Bugsnag with this Event. By default we'll
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
     * Get the context associated with this Event.
     */
    @Nullable
    public String getContext() {
        return context;
    }

    /**
     * Set a custom grouping hash to use when grouping this Event on the
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
     * Get the grouping hash associated with this Event.
     *
     * @return the grouping hash, if set
     */
    @Nullable
    public String getGroupingHash() {
        return groupingHash;
    }

    /**
     * Set the Severity of this Event.
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
     * Get the Severity of this Event.
     *
     * @see Severity
     */
    @Nullable
    public Severity getSeverity() {
        return severity;
    }

    /**
     * Set user information associated with this Event
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
     * @return user information associated with this Event
     */
    @NonNull
    public User getUser() {
        return user;
    }

    /**
     * Set user id associated with this Event
     *
     * @param id the id of the user
     */
    public void setUserId(@Nullable String id) {
        this.user = new User(id, user.getEmail(), user.getName());
    }

    /**
     * Set user email address associated with this Event
     *
     * @param email the email address of the user
     */
    public void setUserEmail(@Nullable String email) {
        this.user = new User(user.getId(), email, user.getName());
    }

    /**
     * Set user name associated with this Event
     *
     * @param name the name of the user
     */
    public void setUserName(@Nullable String name) {
        this.user = new User(user.getId(), user.getEmail(), name);
    }

    @Override
    public void addMetadata(@NonNull String section, @NonNull Map<String, ?> value) {
        metadata.addMetadata(section, value);
    }

    @Override
    public void addMetadata(@NotNull String section,
                            @Nullable String key,
                            @Nullable Object value) {
        metadata.addMetadata(section, key, value);
    }

    @Override
    public void clearMetadata(@NonNull String section) {
        clearMetadata(section, null);
    }

    @Override
    public void clearMetadata(@NotNull String section,
                              @Nullable String key) {
        metadata.clearMetadata(section, key);
    }

    @Nullable
    @Override
    public Object getMetadata(@NonNull String section) {
        return getMetadata(section, null);
    }


    @Nullable
    @Override
    public Object getMetadata(@NotNull String section,
                              @Nullable String key) {
        return metadata.getMetadata(section, key);
    }

    /**
     * Get the class name from the exception contained in this Event report.
     */
    @NonNull
    public String getExceptionName() {
        return exception.getName();
    }

    /**
     * Sets the class name from the exception contained in this Event report.
     */
    public void setExceptionName(@NonNull String exceptionName) {
        exception.setName(exceptionName);
    }

    /**
     * Get the message from the exception contained in this Event report.
     */
    @NonNull
    public String getExceptionMessage() {
        String msg = exception.getMessage();
        return msg != null ? msg : "";
    }

    /**
     * Sets the message from the exception contained in this Event report.
     */
    public void setExceptionMessage(@NonNull String exceptionMessage) {
        exception.setMessage(exceptionMessage);
    }

    /**
     * The {@linkplain Throwable exception} which triggered this Event report.
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

    void setBreadcrumbs(BreadcrumbState breadcrumbState) {
        this.breadcrumbState = breadcrumbState;
    }

    boolean shouldIgnoreClass() {
        return config.getIgnoreClasses().contains(getExceptionName());
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

    Collection<String> getProjectPackages() {
        return projectPackages;
    }

    void setProjectPackages(Collection<String> projectPackages) {
        this.projectPackages = projectPackages;
        if (exceptions != null) {
            exceptions.setProjectPackages(projectPackages);
        }
    }

    static class Builder {
        private final ImmutableConfig config;
        private final Throwable exception;
        private final SessionTracker sessionTracker;
        private final ThreadState threadState;
        private Severity severity = Severity.WARNING;
        private Metadata metadata = new Metadata();
        private Metadata globalMetadata;
        private String attributeValue;

        @HandledState.SeverityReason
        private String severityReasonType;

        Builder(@NonNull ImmutableConfig config,
                @NonNull Throwable exception,
                SessionTracker sessionTracker,
                @NonNull Thread thread,
                boolean unhandled,
                Metadata globalMetadata) {
            Throwable exc = unhandled ? exception : null;
            this.threadState = new ThreadState(config, exc, thread);
            this.config = config;
            this.exception = exception;
            this.severityReasonType = HandledState.REASON_USER_SPECIFIED; // default
            this.sessionTracker = sessionTracker;
            this.globalMetadata = globalMetadata;
        }

        Builder(@NonNull ImmutableConfig config, @NonNull String name,
                @NonNull String message, @NonNull StackTraceElement[] frames,
                SessionTracker sessionTracker, Thread thread, Metadata globalMetadata) {
            this(config, new BugsnagException(name, message, frames), sessionTracker,
                thread, false, globalMetadata);
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

        Builder metadata(Metadata metadata) {
            this.metadata = metadata;
            return this;
        }

        Event build() {
            HandledState handledState =
                HandledState.newInstance(severityReasonType, severity, attributeValue);
            Session session = getSession(handledState);
            Metadata metadata = Metadata.Companion.merge(globalMetadata, this.metadata);
            return new Event(config, exception, handledState,
                severity, session, threadState, metadata);
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
            if (config.getAutoTrackSessions() || !currentSession.isAutoCaptured()) {
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
