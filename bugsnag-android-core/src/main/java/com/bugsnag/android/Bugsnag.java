package com.bugsnag.android;

import android.annotation.SuppressLint;
import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.util.Map;

/**
 * Static access to a Bugsnag Client, the easiest way to use Bugsnag in your Android app.
 * For example:
 * <p>
 * Bugsnag.init(this, "your-api-key");
 * Bugsnag.notify(new RuntimeException("something broke!"));
 *
 * @see Client
 */
@SuppressWarnings("checkstyle:JavadocTagContinuationIndentation")
public final class Bugsnag {

    private static final Object lock = new Object();

    @SuppressLint("StaticFieldLeak")
    static Client client;

    private Bugsnag() {
    }

    /**
     * Initialize the static Bugsnag client
     *
     * @param androidContext an Android context, usually <code>this</code>
     */
    @NonNull
    public static Client init(@NonNull Context androidContext) {
        String apiKey = null;
        return init(androidContext, apiKey);
    }

    /**
     * Initialize the static Bugsnag client
     *
     * @param androidContext an Android context, usually <code>this</code>
     * @param apiKey         your Bugsnag API key from your Bugsnag dashboard
     */
    @NonNull
    public static Client init(@NonNull Context androidContext, @Nullable String apiKey) {
        Configuration config
            = ConfigFactory.createNewConfiguration(androidContext, apiKey, true);
        return init(androidContext, config);
    }

    /**
     * Initialize the static Bugsnag client
     *
     * @param androidContext an Android context, usually <code>this</code>
     * @param config         a configuration for the Client
     */
    @NonNull
    public static Client init(@NonNull Context androidContext, @NonNull Configuration config) {
        synchronized (lock) {
            if (client == null) {
                client = new Client(androidContext, config);
            } else {
                logClientInitWarning();
            }
        }
        return client;
    }

    private static void logClientInitWarning() {
        Logger.warn("It appears that Bugsnag.init() was called more than once. Subsequent "
            + "calls have no effect, but may indicate that Bugsnag is not integrated in an"
            + " Application subclass, which can lead to undesired behaviour.");
    }

    /**
     * Gets the context to be sent to Bugsnag.
     *
     * @return Context
     */
    @Nullable public static String getContext() {
        return getClient().getContext();
    }

    /**
     * Set the context sent to Bugsnag. By default we'll attempt to detect the
     * name of the top-most activity at the time of a report, and use this
     * as the context, but sometime this is not possible.
     *
     * @param context set what was happening at the time of a crash
     */
    public static void setContext(@Nullable final String context) {
        getClient().setContext(context);
    }

    /**
     * Set details of the user currently using your application.
     * You can search for this information in your Bugsnag dashboard.
     * <p>
     * For example:
     * <p>
     * Bugsnag.setUser("12345", "james@example.com", "James Smith");
     *
     * @param id    a unique identifier of the current user (defaults to a unique id)
     * @param email the email address of the current user
     * @param name  the name of the current user
     */
    public static void setUser(@Nullable final String id,
                               @Nullable final String email,
                               @Nullable final String name) {
        getClient().setUser(id, email, name);
    }

    /**
     * Removes the current user data and sets it back to defaults
     */
    public static void clearUser() {
        getClient().clearUser();
    }

    /**
     * Set a unique identifier for the user currently using your application.
     * By default, this will be an automatically generated unique id
     * You can search for this information in your Bugsnag dashboard.
     *
     * @param id a unique identifier of the current user
     */
    public static void setUserId(@Nullable final String id) {
        getClient().setUserId(id);
    }

    /**
     * Set the email address of the current user.
     * You can search for this information in your Bugsnag dashboard.
     *
     * @param email the email address of the current user
     */
    public static void setUserEmail(@Nullable final String email) {
        getClient().setUserEmail(email);
    }

    /**
     * Set the name of the current user.
     * You can search for this information in your Bugsnag dashboard.
     *
     * @param name the name of the current user
     */
    public static void setUserName(@Nullable final String name) {
        getClient().setUserName(name);
    }

    /**
     * Add a "before notify" callback, to execute code before sending
     * reports to Bugsnag.
     * <p>
     * You can use this to add or modify information attached to an error
     * before it is sent to your dashboard. You can also return
     * <code>false</code> from any callback to prevent delivery. "Before
     * notify" callbacks do not run before reports generated in the event
     * of immediate app termination from crashes in C/C++ code.
     * <p>
     * For example:
     * <p>
     * Bugsnag.beforeNotify(new BeforeNotify() {
     * public boolean run(Error error) {
     * error.setSeverity(Severity.INFO);
     * return true;
     * }
     * })
     *
     * @param beforeNotify a callback to run before sending errors to Bugsnag
     * @see BeforeNotify
     */
    public static void beforeNotify(@NonNull final BeforeNotify beforeNotify) {
        getClient().beforeNotify(beforeNotify);
    }

    /**
     * Add a "before breadcrumb" callback, to execute code before every
     * breadcrumb captured by Bugsnag.
     * <p>
     * You can use this to modify breadcrumbs before they are stored by Bugsnag.
     * You can also return <code>false</code> from any callback to ignore a breadcrumb.
     * <p>
     * For example:
     * <p>
     * Bugsnag.beforeRecordBreadcrumb(new BeforeRecordBreadcrumb() {
     * public boolean shouldRecord(Breadcrumb breadcrumb) {
     * return false; // ignore the breadcrumb
     * }
     * })
     *
     * @param beforeRecordBreadcrumb a callback to run before a breadcrumb is captured
     * @see BeforeRecordBreadcrumb
     */
    public static void beforeRecordBreadcrumb(
        @NonNull final BeforeRecordBreadcrumb beforeRecordBreadcrumb) {
        getClient().beforeRecordBreadcrumb(beforeRecordBreadcrumb);
    }

    /**
     * Notify Bugsnag of a handled exception
     *
     * @param exception the exception to send to Bugsnag
     */
    public static void notify(@NonNull final Throwable exception) {
        getClient().notify(exception);
    }

    /**
     * Notify Bugsnag of a handled exception
     *
     * @param exception the exception to send to Bugsnag
     * @param callback  callback invoked on the generated error report for
     *                  additional modification
     */
    public static void notify(@NonNull final Throwable exception,
                              @Nullable final Callback callback) {
        getClient().notify(exception, callback);
    }

    /**
     * Notify Bugsnag of an error
     *
     * @param name       the error name or class
     * @param message    the error message
     * @param stacktrace the stackframes associated with the error
     * @param callback   callback invoked on the generated error report for
     *                   additional modification
     */
    public static void notify(@NonNull String name,
                              @NonNull String message,
                              @NonNull StackTraceElement[] stacktrace,
                              @Nullable Callback callback) {
        getClient().notify(name, message, stacktrace, callback);
    }

    /**
     * Notify Bugsnag of a handled exception
     *
     * @param exception the exception to send to Bugsnag
     * @param severity  the severity of the error, one of Severity.ERROR,
     *                  Severity.WARNING or Severity.INFO
     */
    public static void notify(@NonNull final Throwable exception,
                              @NonNull final Severity severity) {
        getClient().notify(exception, severity);
    }

    /**
     * Intended for use by other clients (React Native/Unity). Calling this method directly from
     * Android is not supported.
     */
    public static void internalClientNotify(@NonNull final Throwable exception,
                                            @NonNull Map<String, Object> clientData,
                                            boolean blocking,
                                            @Nullable Callback callback) {
        getClient().internalClientNotify(exception, clientData, blocking, callback);
    }

    /**
     * Add diagnostic information to every error report.
     * Diagnostic information is collected in "tabs" on your dashboard.
     * <p>
     * For example:
     * <p>
     * Bugsnag.addToTab("account", "name", "Acme Co.");
     * Bugsnag.addToTab("account", "payingCustomer", true);
     *
     * @param tab   the dashboard tab to add diagnostic data to
     * @param key   the name of the diagnostic information
     * @param value the contents of the diagnostic information
     */
    public static void addToTab(@NonNull final String tab,
                                @NonNull final String key,
                                @Nullable final Object value) {
        getClient().addToTab(tab, key, value);
    }

    /**
     * Remove a tab of app-wide diagnostic information
     *
     * @param tabName the dashboard tab to remove diagnostic data from
     */
    public static void clearTab(@NonNull String tabName) {
        getClient().clearTab(tabName);
    }

    /**
     * Get the global diagnostic information currently stored in MetaData.
     *
     * @see MetaData
     */
    @NonNull public static MetaData getMetaData() {
        return getClient().getMetaData();
    }

    /**
     * Set the global diagnostic information to be send with every error.
     *
     * @see MetaData
     */
    public static void setMetaData(@NonNull final MetaData metaData) {
        getClient().setMetaData(metaData);
    }

    /**
     * Leave a "breadcrumb" log message, representing an action that occurred
     * in your app, to aid with debugging.
     *
     * @param message the log message to leave (max 140 chars)
     */
    public static void leaveBreadcrumb(@NonNull String message) {
        getClient().leaveBreadcrumb(message);
    }

    /**
     * Leave a "breadcrumb" log message representing an action or event which
     * occurred in your app, to aid with debugging
     *
     * @param name     A short label (max 32 chars)
     * @param type     A category for the breadcrumb
     * @param metadata Additional diagnostic information about the app environment
     */
    public static void leaveBreadcrumb(@NonNull String name,
                                       @NonNull BreadcrumbType type,
                                       @NonNull Map<String, String> metadata) {
        getClient().leaveBreadcrumb(name, type, metadata);
    }

    /**
     * Clear any breadcrumbs that have been left so far.
     */
    public static void clearBreadcrumbs() {
        getClient().clearBreadcrumbs();
    }

    /**
     * Starts tracking a new session. You should disable automatic session tracking via
     * {@link #setAutoCaptureSessions(boolean)} if you call this method.
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
     * @see #stopSession()
     * @see Configuration#setAutoCaptureSessions(boolean)
     */
    public static void startSession() {
        getClient().startSession();
    }

    /**
     * Resumes a session which has previously been stopped, or starts a new session if none exists.
     * If a session has already been resumed or started and has not been stopped, calling this
     * method will have no effect. You should disable automatic session tracking via
     * {@link #setAutoCaptureSessions(boolean)} if you call this method.
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
     * @see #startSession()
     * @see #stopSession()
     * @see Configuration#setAutoCaptureSessions(boolean)
     *
     * @return true if a previous session was resumed, false if a new session was started.
     */
    public static boolean resumeSession() {
        return getClient().resumeSession();
    }

    /**
     * Stops tracking a session. You should disable automatic session tracking via
     * {@link #setAutoCaptureSessions(boolean)} if you call this method.
     * <p/>
     * You should call this at the appropriate time in your application when you wish to stop a
     * session. Any subsequent errors which occur in your application will still be reported to
     * Bugsnag but will not count towards your application's
     * <a href="https://docs.bugsnag.com/product/releases/releases-dashboard/#stability-score">
     * stability score</a>. This can be advantageous if, for example, you do not wish the
     * stability score to include crashes in a background service.
     *
     * @see #startSession()
     * @see #resumeSession()
     * @see Configuration#setAutoCaptureSessions(boolean)
     */
    public static void stopSession() {
        getClient().stopSession();
    }

    /**
     * Get the current Bugsnag Client instance.
     */
    @NonNull
    public static Client getClient() {
        if (client == null) {
            throw new IllegalStateException("You must call Bugsnag.init before any"
                + " other Bugsnag methods");
        }

        return client;
    }
}
