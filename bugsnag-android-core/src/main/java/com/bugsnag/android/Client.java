package com.bugsnag.android;

import com.bugsnag.android.internal.ImmutableConfig;
import com.bugsnag.android.internal.StateObserver;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * A Bugsnag Client instance allows you to use Bugsnag in your Android app.
 * Typically you'd instead use the static access provided in the Bugsnag class.
 * <p/>
 * Example usage:
 * <p/>
 * Client client = new Client(this, "your-api-key");
 * client.notify(new RuntimeException("something broke!"));
 *
 * @see Bugsnag
 */
@SuppressWarnings({"checkstyle:JavadocTagContinuationIndentation", "ConstantConditions"})
public class Client implements MetadataAware, CallbackAware, UserAware {

    final ClientInternal impl;

    /**
     * Initialize a Bugsnag client
     *
     * @param androidContext an Android context, usually <code>this</code>
     */
    public Client(@NonNull Context androidContext) {
        this(androidContext, Configuration.load(androidContext));
    }

    /**
     * Initialize a Bugsnag client
     *
     * @param androidContext an Android context, usually <code>this</code>
     * @param apiKey         your Bugsnag API key from your Bugsnag dashboard
     */
    public Client(@NonNull Context androidContext, @NonNull String apiKey) {
        this(androidContext, Configuration.load(androidContext, apiKey));
    }

    /**
     * Initialize a Bugsnag client
     *
     * @param androidContext an Android context, usually <code>this</code>
     * @param configuration  a configuration for the Client
     */
    public Client(@NonNull Context androidContext, @NonNull final Configuration configuration) {
        this.impl = new ClientInternal(androidContext, configuration, this);
        impl.start();
    }

    @VisibleForTesting
    Client(ClientInternal impl) {
        this.impl = impl;
    }

    /**
     * Starts tracking a new session. You should disable automatic session tracking via
     * {@link Configuration#setAutoTrackSessions(boolean)} if you call this method.
     * <p/>
     * You should call this at the appropriate time in your application when you wish to start a
     * session. Any subsequent errors which occur in your application will still be reported to
     * Bugsnag but will not count towards your application's
     * <a href="https://docs.bugsnag.com/product/releases/releases-dashboard/#stability-score">
     * stability score</a>. This will start a new session even if there is already an existing
     * session; you should call {@link #resumeSession()} if you only want to start a session
     * when one doesn't already exist.
     *
     * @see #resumeSession()
     * @see #pauseSession()
     * @see Configuration#setAutoTrackSessions(boolean)
     */
    public void startSession() {
        impl.startSession();
    }

    /**
     * Pauses tracking of a session. You should disable automatic session tracking via
     * {@link Configuration#setAutoTrackSessions(boolean)} if you call this method.
     * <p/>
     * You should call this at the appropriate time in your application when you wish to pause a
     * session. Any subsequent errors which occur in your application will still be reported to
     * Bugsnag but will not count towards your application's
     * <a href="https://docs.bugsnag.com/product/releases/releases-dashboard/#stability-score">
     * stability score</a>. This can be advantageous if, for example, you do not wish the
     * stability score to include crashes in a background service.
     *
     * @see #startSession()
     * @see #resumeSession()
     * @see Configuration#setAutoTrackSessions(boolean)
     */
    public void pauseSession() {
        impl.pauseSession();
    }

    /**
     * Resumes a session which has previously been paused, or starts a new session if none exists.
     * If a session has already been resumed or started and has not been paused, calling this
     * method will have no effect. You should disable automatic session tracking via
     * {@link Configuration#setAutoTrackSessions(boolean)} if you call this method.
     * <p/>
     * It's important to note that sessions are stored in memory for the lifetime of the
     * application process and are not persisted on disk. Therefore calling this method on app
     * startup would start a new session, rather than continuing any previous session.
     * <p/>
     * You should call this at the appropriate time in your application when you wish to resume
     * a previously started session. Any subsequent errors which occur in your application will
     * still be reported to Bugsnag but will not count towards your application's
     * <a href="https://docs.bugsnag.com/product/releases/releases-dashboard/#stability-score">
     * stability score</a>.
     *
     * @return true if a previous session was resumed, false if a new session was started.
     * @see #startSession()
     * @see #pauseSession()
     * @see Configuration#setAutoTrackSessions(boolean)
     */
    public boolean resumeSession() {
        return impl.resumeSession();
    }

    /**
     * Bugsnag uses the concept of "contexts" to help display and group your errors. Contexts
     * represent what was happening in your application at the time an error occurs.
     * <p>
     * In an android app the "context" is automatically set as the foreground Activity.
     * If you would like to set this value manually, you should alter this property.
     */
    @Nullable
    public String getContext() {
        return impl.getContext();
    }

    /**
     * Bugsnag uses the concept of "contexts" to help display and group your errors. Contexts
     * represent what was happening in your application at the time an error occurs.
     * <p>
     * In an android app the "context" is automatically set as the foreground Activity.
     * If you would like to set this value manually, you should alter this property.
     */
    public void setContext(@Nullable String context) {
        impl.setContext(context);
    }

    /**
     * Sets the user associated with the event.
     */
    @Override
    public void setUser(@Nullable String id, @Nullable String email, @Nullable String name) {
        impl.setUser(id, email, name);
    }

    /**
     * Returns the currently set User information.
     */
    @NonNull
    @Override
    public User getUser() {
        return impl.getUser();
    }

    /**
     * Add a "on error" callback, to execute code at the point where an error report is
     * captured in Bugsnag.
     * <p>
     * You can use this to add or modify information attached to an Event
     * before it is sent to your dashboard. You can also return
     * <code>false</code> from any callback to prevent delivery. "on error"
     * callbacks do not run before reports generated in the event
     * of immediate app termination from crashes in C/C++ code.
     * <p>
     * For example:
     * <p>
     * Bugsnag.addOnError(new OnErrorCallback() {
     * public boolean run(Event event) {
     * event.setSeverity(Severity.INFO);
     * return true;
     * }
     * })
     *
     * @param onError a callback to run before sending errors to Bugsnag
     * @see OnErrorCallback
     */
    @Override
    public void addOnError(@NonNull OnErrorCallback onError) {
        if (onError != null) {
            impl.addOnError(onError);
        } else {
            logNull("addOnError");
        }
    }

    /**
     * Removes a previously added "on error" callback
     *
     * @param onError the callback to remove
     */
    @Override
    public void removeOnError(@NonNull OnErrorCallback onError) {
        if (onError != null) {
            impl.removeOnError(onError);
        } else {
            logNull("removeOnError");
        }
    }

    /**
     * Add an "on breadcrumb" callback, to execute code before every
     * breadcrumb captured by Bugsnag.
     * <p>
     * You can use this to modify breadcrumbs before they are stored by Bugsnag.
     * You can also return <code>false</code> from any callback to ignore a breadcrumb.
     * <p>
     * For example:
     * <p>
     * Bugsnag.onBreadcrumb(new OnBreadcrumbCallback() {
     * public boolean run(Breadcrumb breadcrumb) {
     * return false; // ignore the breadcrumb
     * }
     * })
     *
     * @param onBreadcrumb a callback to run before a breadcrumb is captured
     * @see OnBreadcrumbCallback
     */
    @Override
    public void addOnBreadcrumb(@NonNull OnBreadcrumbCallback onBreadcrumb) {
        if (onBreadcrumb != null) {
            impl.addOnBreadcrumb(onBreadcrumb);
        } else {
            logNull("addOnBreadcrumb");
        }
    }

    /**
     * Removes a previously added "on breadcrumb" callback
     *
     * @param onBreadcrumb the callback to remove
     */
    @Override
    public void removeOnBreadcrumb(@NonNull OnBreadcrumbCallback onBreadcrumb) {
        if (onBreadcrumb != null) {
            impl.removeOnBreadcrumb(onBreadcrumb);
        } else {
            logNull("removeOnBreadcrumb");
        }
    }

    /**
     * Add an "on session" callback, to execute code before every
     * session captured by Bugsnag.
     * <p>
     * You can use this to modify sessions before they are stored by Bugsnag.
     * You can also return <code>false</code> from any callback to ignore a session.
     * <p>
     * For example:
     * <p>
     * Bugsnag.onSession(new OnSessionCallback() {
     * public boolean run(Session session) {
     * return false; // ignore the session
     * }
     * })
     *
     * @param onSession a callback to run before a session is captured
     * @see OnSessionCallback
     */
    @Override
    public void addOnSession(@NonNull OnSessionCallback onSession) {
        if (onSession != null) {
            impl.addOnSession(onSession);
        } else {
            logNull("addOnSession");
        }
    }

    /**
     * Removes a previously added "on session" callback
     *
     * @param onSession the callback to remove
     */
    @Override
    public void removeOnSession(@NonNull OnSessionCallback onSession) {
        if (onSession != null) {
            impl.removeOnSession(onSession);
        } else {
            logNull("removeOnSession");
        }
    }

    /**
     * Notify Bugsnag of a handled exception
     *
     * @param exception the exception to send to Bugsnag
     */
    public void notify(@NonNull Throwable exception) {
        notify(exception, null);
    }

    /**
     * Notify Bugsnag of a handled exception
     *
     * @param exc     the exception to send to Bugsnag
     * @param onError callback invoked on the generated error report for
     *                additional modification
     */
    public void notify(@NonNull Throwable exc, @Nullable OnErrorCallback onError) {
        if (exc != null) {
            impl.notify(exc, onError);
        } else {
            logNull("notify");
        }
    }

    /**
     * Returns the current buffer of breadcrumbs that will be sent with captured events. This
     * ordered list represents the most recent breadcrumbs to be captured up to the limit
     * set in {@link Configuration#getMaxBreadcrumbs()}.
     * <p>
     * The returned collection is readonly and mutating the list will cause no effect on the
     * Client's state. If you wish to alter the breadcrumbs collected by the Client then you should
     * use {@link Configuration#setEnabledBreadcrumbTypes(Set)} and
     * {@link Configuration#addOnBreadcrumb(OnBreadcrumbCallback)} instead.
     *
     * @return a list of collected breadcrumbs
     */
    @NonNull
    public List<Breadcrumb> getBreadcrumbs() {
        return impl.getBreadcrumbs();
    }

    /**
     * Adds a map of multiple metadata key-value pairs to the specified section.
     */
    @Override
    public void addMetadata(@NonNull String section, @NonNull Map<String, ?> value) {
        if (section != null && value != null) {
            impl.addMetadata(section, value);
        } else {
            logNull("addMetadata");
        }
    }

    /**
     * Adds the specified key and value in the specified section. The value can be of
     * any primitive type or a collection such as a map, set or array.
     */
    @Override
    public void addMetadata(@NonNull String section, @NonNull String key, @Nullable Object value) {
        if (section != null && key != null) {
            impl.addMetadata(section, key, value);
        } else {
            logNull("addMetadata");
        }
    }

    /**
     * Removes all the data from the specified section.
     */
    @Override
    public void clearMetadata(@NonNull String section) {
        if (section != null) {
            impl.clearMetadata(section);
        } else {
            logNull("clearMetadata");
        }
    }

    /**
     * Removes data with the specified key from the specified section.
     */
    @Override
    public void clearMetadata(@NonNull String section, @NonNull String key) {
        if (section != null && key != null) {
            impl.clearMetadata(section, key);
        } else {
            logNull("clearMetadata");
        }
    }

    /**
     * Returns a map of data in the specified section.
     */
    @Nullable
    @Override
    public Map<String, Object> getMetadata(@NonNull String section) {
        if (section != null) {
            return impl.getMetadata(section);
        } else {
            logNull("getMetadata");
            return null;
        }
    }

    /**
     * Returns the value of the specified key in the specified section.
     */
    @Override
    @Nullable
    public Object getMetadata(@NonNull String section, @NonNull String key) {
        if (section != null && key != null) {
            return impl.getMetadata(section, key);
        } else {
            logNull("getMetadata");
            return null;
        }
    }

    /**
     * Leave a "breadcrumb" log message, representing an action that occurred
     * in your app, to aid with debugging.
     *
     * @param message the log message to leave
     */
    public void leaveBreadcrumb(@NonNull String message) {
        if (message != null) {
            impl.leaveBreadcrumb(message);
        } else {
            logNull("leaveBreadcrumb");
        }
    }

    /**
     * Leave a "breadcrumb" log message representing an action or event which
     * occurred in your app, to aid with debugging
     *
     * @param message  A short label
     * @param metadata Additional diagnostic information about the app environment
     * @param type     A category for the breadcrumb
     */
    public void leaveBreadcrumb(@NonNull String message,
                                @NonNull Map<String, Object> metadata,
                                @NonNull BreadcrumbType type) {
        if (message != null && type != null && metadata != null) {
            impl.leaveBreadcrumb(message, metadata, type);
        } else {
            logNull("leaveBreadcrumb");
        }
    }

    /**
     * Retrieves information about the last launch of the application, if it has been run before.
     * <p>
     * For example, this allows checking whether the app crashed on its last launch, which could
     * be used to perform conditional behaviour to recover from crashes, such as clearing the
     * app data cache.
     */
    @Nullable
    public LastRunInfo getLastRunInfo() {
        return impl.getLastRunInfo();
    }

    /**
     * Informs Bugsnag that the application has finished launching. Once this has been called
     * {@link AppWithState#isLaunching()} will always be false in any new error reports,
     * and synchronous delivery will not be attempted on the next launch for any fatal crashes.
     * <p>
     * By default this method will be called after Bugsnag is initialized when
     * {@link Configuration#getLaunchDurationMillis()} has elapsed. Invoking this method manually
     * has precedence over the value supplied via the launchDurationMillis configuration option.
     */
    public void markLaunchCompleted() {
        impl.markLaunchCompleted();
    }


    /*
     *
     * Start non-public APIs (in the next major version these should be moved)
     *
     *
     */


    private void logNull(String property) {
        impl.getLogger().e("Invalid null value supplied to client." + property + ", ignoring");
    }

    /**
     * Caches an error then attempts to notify.
     * <p>
     * Should only ever be called from the {@link ExceptionHandler}.
     */
    void notifyUnhandledException(@NonNull Throwable exc, Metadata metadata,
                                  @SeverityReason.SeverityReasonType String severityReason,
                                  @Nullable String attributeValue) {
        impl.notifyUnhandledException(exc, metadata, severityReason, attributeValue);
    }

    void populateAndNotifyAndroidEvent(@NonNull Event event,
                                       @Nullable OnErrorCallback onError) {
        impl.populateAndNotifyAndroidEvent(event, onError);
    }

    void notifyInternal(@NonNull Event event,
                        @Nullable OnErrorCallback onError) {
        impl.notifyInternal(event, onError);
    }

    @NonNull
    AppDataCollector getAppDataCollector() {
        return impl.getAppDataCollector();
    }

    @NonNull
    DeviceDataCollector getDeviceDataCollector() {
        return impl.getDeviceDataCollector();
    }

    // cast map to retain original signature until next major version bump, as this
    // method signature is used by Unity/React native
    @NonNull
    @SuppressWarnings({"unchecked", "rawtypes", "OverloadMethodsDeclarationOrder"})
    Map<String, Object> getMetadata() {
        return (Map) impl.getMetadata();
    }

    /**
     * Intended for internal use only - leaves a breadcrumb if the type is enabled for automatic
     * breadcrumbs.
     *
     * @param message  A short label
     * @param type     A category for the breadcrumb
     * @param metadata Additional diagnostic information about the app environment
     */
    void leaveAutoBreadcrumb(@NonNull String message,
                             @NonNull BreadcrumbType type,
                             @NonNull Map<String, Object> metadata) {
        impl.leaveAutoBreadcrumb(message, type, metadata);
    }

    void setupNdkPlugin() {
        impl.setupNdkPlugin();
    }

    void addObserver(StateObserver observer) {
        impl.addObserver(observer);
    }

    void removeObserver(StateObserver observer) {
        impl.removeObserver(observer);
    }

    /**
     * Sends initial state values for Metadata/User/Context to any registered observers.
     */
    void syncInitialState() {
        impl.syncInitialState();
    }

    SessionTracker getSessionTracker() {
        return impl.getSessionTracker();
    }

    @NonNull
    EventStore getEventStore() {
        return impl.getEventStore();
    }

    ImmutableConfig getConfig() {
        return impl.getConfig();
    }

    void setBinaryArch(String binaryArch) {
        impl.setBinaryArch(binaryArch);
    }

    Context getAppContext() {
        return impl.getAppContext();
    }

    /**
     * Intended for internal use only - sets the code bundle id for React Native
     */
    @Nullable
    String getCodeBundleId() {
        return impl.getCodeBundleId();
    }

    /**
     * Intended for internal use only - sets the code bundle id for React Native
     */
    void setCodeBundleId(@Nullable String codeBundleId) {
        impl.setCodeBundleId(codeBundleId);
    }

    void addRuntimeVersionInfo(@NonNull String key, @NonNull String value) {
        impl.addRuntimeVersionInfo(key, value);
    }

    @VisibleForTesting
    void close() {
        impl.close();
    }

    Logger getLogger() {
        return impl.getLogger();
    }

    BackgroundTaskService getBgTaskService() {
        return impl.getBgTaskService();
    }

    /**
     * Retrieves an instantiated plugin of the given type, or null if none has been created
     */
    @SuppressWarnings("rawtypes")
    @Nullable
    Plugin getPlugin(@NonNull Class clz) {
        return impl.getPlugin(clz);
    }

    void setNotifier(Notifier notifier) {
        impl.setNotifier(notifier);
    }

    NotifierState getNotifierState() {
        return impl.getNotifierState();
    }

    MetadataState getMetadataState() {
        return impl.getMetadataState();
    }

    ContextState getContextState() {
        return impl.getContextState();
    }

    LaunchCrashTracker getLaunchCrashTracker() {
        return impl.getLaunchCrashTracker();
    }

    MemoryTrimState getMemoryTrimState() {
        return impl.getMemoryTrimState();
    }

    BreadcrumbState getBreadcrumbState() {
        return impl.getBreadcrumbState();
    }

    void setAutoNotify(boolean autoNotify) {
        impl.setAutoNotify(autoNotify);
    }

    void setAutoDetectAnrs(boolean autoDetectAnrs) {
        impl.setAutoDetectAnrs(autoDetectAnrs);
    }

}
