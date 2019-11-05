package com.bugsnag.android;

import android.annotation.SuppressLint;
import android.content.Context;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

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
    static Logger logger = DebugLogger.INSTANCE;

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
        return init(androidContext, new ManifestConfigLoader().load(androidContext));
    }

    /**
     * Initialize the static Bugsnag client
     *
     * @param androidContext an Android context, usually <code>this</code>
     * @param apiKey         your Bugsnag API key from your Bugsnag dashboard
     */
    @NonNull
    public static Client init(@NonNull Context androidContext, @NonNull String apiKey) {
        return init(androidContext, new Configuration(apiKey));
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
        logger.w("It appears that Bugsnag.init() was called more than once. "
            + "Subsequent calls have no effect, but may indicate that Bugsnag is not integrated in "
            + "an Application subclass, which can lead to undesired behaviour.");
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

    @NonNull
    public static User getUser() {
        return getClient().getUser();
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
     * Add a "on error" callback, to execute code at the point where an error report is
     * captured in Bugsnag.
     * <p>
     * You can use this to add or modify information attached to an error
     * before it is sent to your dashboard. You can also return
     * <code>false</code> from any callback to prevent delivery. "on error"
     * callbacks do not run before reports generated in the event
     * of immediate app termination from crashes in C/C++ code.
     * <p>
     * For example:
     * <p>
     * Bugsnag.addOnError(new OnError() {
     * public boolean run(Event error) {
     * error.setSeverity(Severity.INFO);
     * return true;
     * }
     * })
     *
     * @param onError a callback to run before sending errors to Bugsnag
     * <p/>
     * @see OnError
     */
    public static void addOnError(@NonNull OnError onError) {
        getClient().addOnError(onError);
    }

    public static void removeOnError(@NonNull OnError onError) {
        getClient().removeOnError(onError);
    }

    /**
     * Add a "before breadcrumb" callback, to execute code before every
     * breadcrumb captured by Bugsnag.
     * <p>
     * You can use this to modify breadcrumbState before they are stored by Bugsnag.
     * You can also return <code>false</code> from any callback to ignore a breadcrumb.
     * <p>
     * For example:
     * <p>
     * Bugsnag.onBreadcrumb(new OnBreadcrumb() {
     * public boolean run(Breadcrumb breadcrumb) {
     * return false; // ignore the breadcrumb
     * }
     * })
     *
     * @param onBreadcrumb a callback to run before a breadcrumb is captured
     * @see OnBreadcrumb
     */
    public static void addOnBreadcrumb(@NonNull final OnBreadcrumb onBreadcrumb) {
        getClient().addOnBreadcrumb(onBreadcrumb);
    }

    public static void removeOnBreadcrumb(@NonNull OnBreadcrumb onBreadcrumb) {
        getClient().removeOnBreadcrumb(onBreadcrumb);
    }

    public static void addOnSession(@NonNull OnSession onSession) {
        getClient().addOnSession(onSession);
    }

    public static void removeOnSession(@NonNull OnSession onSession) {
        getClient().removeOnSession(onSession);
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
     * @param onError  callback invoked on the generated error report for
     *                  additional modification
     */
    public static void notify(@NonNull final Throwable exception,
                              @Nullable final OnError onError) {
        getClient().notify(exception, onError);
    }

    /**
     * Notify Bugsnag of an error
     *
     * @param name       the error name or class
     * @param message    the error message
     * @param stacktrace the stackframes associated with the error
     *
     */
    public static void notify(@NonNull String name,
                              @NonNull String message,
                              @NonNull StackTraceElement[] stacktrace) {
        getClient().notify(name, message, stacktrace);
    }

    /**
     * Notify Bugsnag of an error
     *
     * @param name       the error name or class
     * @param message    the error message
     * @param stacktrace the stackframes associated with the error
     * @param onError   callback invoked on the generated error report for
     *                   additional modification
     */
    public static void notify(@NonNull String name,
                              @NonNull String message,
                              @NonNull StackTraceElement[] stacktrace,
                              @Nullable OnError onError) {
        getClient().notify(name, message, stacktrace, onError);
    }

    public static void addMetadata(@NonNull String section, @Nullable Object value) {
        getClient().addMetadata(section, value);
    }

    public static void addMetadata(@NonNull String section, @Nullable String key,
                                   @Nullable Object value) {
        getClient().addMetadata(section, key, value);
    }

    public static void clearMetadata(@NonNull String section) {
        getClient().clearMetadata(section);
    }

    public static void clearMetadata(@NonNull String section, @Nullable String key) {
        getClient().clearMetadata(section, key);
    }

    @Nullable
    public static Object getMetadata(@NonNull String section) {
        return getClient().getMetadata(section);
    }

    @Nullable
    public static Object getMetadata(@NonNull String section, @Nullable String key) {
        return getClient().getMetadata(section, key);
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
     * @param message     A short label
     * @param type     A category for the breadcrumb
     * @param metadata Additional diagnostic information about the app environment
     */
    public static void leaveBreadcrumb(@NonNull String message,
                                       @NonNull BreadcrumbType type,
                                       @NonNull Map<String, Object> metadata) {
        getClient().leaveBreadcrumb(message, type, metadata);
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
    public static void startSession() {
        getClient().startSession();
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
     * @see #startSession()
     * @see #pauseSession()
     * @see Configuration#setAutoTrackSessions(boolean)
     *
     * @return true if a previous session was resumed, false if a new session was started.
     */
    public static boolean resumeSession() {
        return getClient().resumeSession();
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
    public static void pauseSession() {
        getClient().pauseSession();
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
