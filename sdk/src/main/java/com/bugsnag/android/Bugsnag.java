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
public final class Bugsnag {

    @Nullable
    @SuppressLint("StaticFieldLeak")
    static Client client;

    private Bugsnag() {
    }

    /**
     * Initialize the static Bugsnag client
     *
     * @param androidContext an Android context, usually <code>this</code>
     */
    @Nullable
    public static Client init(@NonNull Context androidContext) {
        client = new Client(androidContext);
        NativeInterface.configureClientObservers(client);
        return client;
    }

    /**
     * Initialize the static Bugsnag client
     *
     * @param androidContext an Android context, usually <code>this</code>
     * @param apiKey         your Bugsnag API key from your Bugsnag dashboard
     */
    @Nullable
    public static Client init(@NonNull Context androidContext, @Nullable String apiKey) {
        client = new Client(androidContext, apiKey);
        NativeInterface.configureClientObservers(client);
        return client;
    }

    /**
     * Initialize the static Bugsnag client
     *
     * @param androidContext         an Android context, usually <code>this</code>
     * @param apiKey                 your Bugsnag API key from your Bugsnag dashboard
     * @param enableExceptionHandler should we automatically handle uncaught exceptions?
     */
    @Nullable
    public static Client init(@NonNull Context androidContext, @Nullable String apiKey, boolean enableExceptionHandler) {
        client = new Client(androidContext, apiKey, enableExceptionHandler);
        NativeInterface.configureClientObservers(client);
        return client;
    }

    /**
     * Initialize the static Bugsnag client
     *
     * @param androidContext an Android context, usually <code>this</code>
     * @param config         a configuration for the Client
     */
    @Nullable
    public static Client init(@NonNull Context androidContext, @NonNull Configuration config) {
        client = new Client(androidContext, config);
        NativeInterface.configureClientObservers(client);
        return client;
    }

    /**
     * Set the application version sent to Bugsnag. By default we'll pull this
     * from your AndroidManifest.xml
     *
     * @param appVersion the app version to send
     */
    public static void setAppVersion(final String appVersion) {
        getClient().setAppVersion(appVersion);
    }

    /**
     * Gets the context to be sent to Bugsnag.
     *
     * @return Context
     */
    public static String getContext() {
        return getClient().getContext();
    }

    /**
     * Set the context sent to Bugsnag. By default we'll attempt to detect the
     * name of the top-most activity at the time of a report, and use this
     * as the context, but sometime this is not possible.
     *
     * @param context set what was happening at the time of a crash
     */
    public static void setContext(final String context) {
        getClient().setContext(context);
    }

    /**
     * Set the endpoint to send data to. By default we'll send reports to
     * the standard https://notify.bugsnag.com endpoint, but you can override
     * this if you are using Bugsnag Enterprise to point to your own Bugsnag
     * endpoint.
     *
     * @param endpoint the custom endpoint to send report to
     * @deprecated use {@link com.bugsnag.android.Configuration#setEndpoint(String)} instead.
     */
    @Deprecated
    public static void setEndpoint(final String endpoint) {
        getClient().setEndpoint(endpoint);
    }

    /**
     * Set the buildUUID to your own value. This is used to identify proguard
     * mapping files in the case that you publish multiple different apps with
     * the same appId and versionCode. The default value is read from the
     * com.bugsnag.android.BUILD_UUID meta-data field in your app manifest.
     *
     * @param buildUUID the buildUUID.
     */
    public static void setBuildUUID(final String buildUUID) {
        getClient().setBuildUUID(buildUUID);
    }

    /**
     * Set which keys should be filtered when sending metaData to Bugsnag.
     * Use this when you want to ensure sensitive information, such as passwords
     * or credit card information is stripped from metaData you send to Bugsnag.
     * Any keys in metaData which contain these strings will be marked as
     * [FILTERED] when send to Bugsnag.
     * <p>
     * For example:
     * <p>
     * Bugsnag.setFilters("password", "credit_card");
     *
     * @param filters a list of keys to filter from metaData
     */
    public static void setFilters(final String... filters) {
        getClient().setFilters(filters);
    }

    /**
     * Set which exception classes should be ignored (not sent) by Bugsnag.
     * <p>
     * For example:
     * <p>
     * Bugsnag.setIgnoreClasses("java.lang.RuntimeException");
     *
     * @param ignoreClasses a list of exception classes to ignore
     */
    public static void setIgnoreClasses(final String... ignoreClasses) {
        getClient().setIgnoreClasses(ignoreClasses);
    }

    /**
     * Set for which releaseStages errors should be sent to Bugsnag.
     * Use this to stop errors from development builds being sent.
     * <p>
     * For example:
     * <p>
     * Bugsnag.setNotifyReleaseStages("production");
     *
     * @param notifyReleaseStages a list of releaseStages to notify for
     * @see #setReleaseStage
     */
    public static void setNotifyReleaseStages(final String... notifyReleaseStages) {
        getClient().setNotifyReleaseStages(notifyReleaseStages);
    }

    /**
     * Set which packages should be considered part of your application.
     * Bugsnag uses this to help with error grouping, and stacktrace display.
     * <p>
     * For example:
     * <p>
     * Bugsnag.setProjectPackages("com.example.myapp");
     * <p>
     * By default, we'll mark the current package name as part of you app.
     *
     * @param projectPackages a list of package names
     */
    public static void setProjectPackages(final String... projectPackages) {
        getClient().setProjectPackages(projectPackages);
    }

    /**
     * Set the current "release stage" of your application.
     * By default, we'll set this to "development" for debug builds and
     * "production" for non-debug builds.
     * <p>
     * If the release stage is set to "production", logging will automatically be disabled.
     *
     * @param releaseStage the release stage of the app
     * @see #setNotifyReleaseStages {@link #setLoggingEnabled(boolean)}
     */
    public static void setReleaseStage(final String releaseStage) {
        getClient().setReleaseStage(releaseStage);
    }

    /**
     * Set whether to send thread-state with report.
     * By default, this will be true.
     *
     * @param sendThreads should we send thread-state with report?
     */
    public static void setSendThreads(final boolean sendThreads) {
        getClient().setSendThreads(sendThreads);
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
    public static void setUser(final String id, final String email, final String name) {
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
    public static void setUserId(final String id) {
        getClient().setUserId(id);
    }

    /**
     * Set the email address of the current user.
     * You can search for this information in your Bugsnag dashboard.
     *
     * @param email the email address of the current user
     */
    public static void setUserEmail(final String email) {
        getClient().setUserEmail(email);
    }

    /**
     * Set the name of the current user.
     * You can search for this information in your Bugsnag dashboard.
     *
     * @param name the name of the current user
     */
    public static void setUserName(final String name) {
        getClient().setUserName(name);
    }

    /**
     * Replaces the Default HTTP Client with a custom implementation. This allows for custom
     * requirements such as certificate pinning to be achieved.
     * <p/>
     * <p>
     * The client implementation, and must be capable of sending Error Reports to the Bugsnag API,
     * as documented here: <a href="https://docs.bugsnag.com/api/error-reporting/">
     * https://docs.bugsnag.com/api/error-reporting/</a>
     *
     * @param errorReportApiClient the custom HTTP client implementation
     */
    public static void setErrorReportApiClient(@NonNull ErrorReportApiClient errorReportApiClient) {
        getClient().setErrorReportApiClient(errorReportApiClient);
    }

    /**
     * Add a "before notify" callback, to execute code before every
     * report to Bugsnag.
     * <p>
     * You can use this to add or modify information attached to an error
     * before it is sent to your dashboard. You can also return
     * <code>false</code> from any callback to halt execution.
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
    public static void beforeNotify(final BeforeNotify beforeNotify) {
        getClient().beforeNotify(beforeNotify);
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
    public static void notify(@NonNull final Throwable exception, final Callback callback) {
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
    public static void notify(@NonNull String name, @NonNull String message, @NonNull StackTraceElement[] stacktrace, Callback callback) {
        getClient().notify(name, message, stacktrace, callback);
    }

    /**
     * Notify Bugsnag of a handled exception
     *
     * @param exception the exception to send to Bugsnag
     * @param severity  the severity of the error, one of Severity.ERROR,
     *                  Severity.WARNING or Severity.INFO
     */
    public static void notify(@NonNull final Throwable exception, final Severity severity) {
        getClient().notify(exception, severity);
    }

    /**
     * Notify Bugsnag of a handled exception
     *
     * @param exception the exception to send to Bugsnag
     * @param metaData  additional information to send with the exception
     * @deprecated Use {@link #notify(Throwable, Callback)}
     * to send and modify error reports
     */
    public static void notify(@NonNull final Throwable exception,
                              @NonNull final MetaData metaData) {
        getClient().notify(exception, new Callback() {
            @Override
            public void beforeNotify(@NonNull Report report) {
                report.getError().setMetaData(metaData);
            }
        });
    }

    /**
     * Notify Bugsnag of a handled exception
     *
     * @param exception the exception to send to Bugsnag
     * @param severity  the severity of the error, one of Severity.ERROR,
     *                  Severity.WARNING or Severity.INFO
     * @param metaData  additional information to send with the exception
     * @deprecated Use {@link #notify(Throwable, Callback)}
     * to send and modify error reports
     */
    @Deprecated
    public static void notify(@NonNull final Throwable exception, final Severity severity,
                              @NonNull final MetaData metaData) {
        getClient().notify(exception, new Callback() {
            @Override
            public void beforeNotify(@NonNull Report report) {
                report.getError().setSeverity(severity);
                report.getError().setMetaData(metaData);
            }
        });
    }

    /**
     * Notify Bugsnag of an error
     *
     * @param name       the error name or class
     * @param message    the error message
     * @param stacktrace the stackframes associated with the error
     * @param severity   the severity of the error, one of Severity.ERROR,
     *                   Severity.WARNING or Severity.INFO
     * @param metaData   additional information to send with the exception
     * @deprecated Use {@link #notify(String, String, StackTraceElement[], Callback)}
     * to send and modify error reports
     */
    @Deprecated
    public static void notify(@NonNull String name, @NonNull String message,
                              @NonNull StackTraceElement[] stacktrace, Severity severity,
                              @NonNull MetaData metaData) {
        final Severity finalSeverity = severity;
        final MetaData finalMetaData = metaData;
        getClient().notify(name, message, stacktrace, new Callback() {
            @Override
            public void beforeNotify(@NonNull Report report) {
                report.getError().setSeverity(finalSeverity);
                report.getError().setMetaData(finalMetaData);
            }
        });
    }

    /**
     * Notify Bugsnag of an error
     *
     * @param name       the error name or class
     * @param message    the error message
     * @param context    the error context
     * @param stacktrace the stackframes associated with the error
     * @param severity   the severity of the error, one of Severity.ERROR,
     *                   Severity.WARNING or Severity.INFO
     * @param metaData   additional information to send with the exception
     * @deprecated Use {@link #notify(String, String, StackTraceElement[], Callback)}
     * to send and modify error reports
     */
    @Deprecated
    public static void notify(@NonNull String name, @NonNull String message, String context,
                              @NonNull StackTraceElement[] stacktrace, Severity severity,
                              @NonNull MetaData metaData) {
        final String finalContext = context;
        final Severity finalSeverity = severity;
        final MetaData finalMetaData = metaData;
        getClient().notify(name, message, stacktrace, new Callback() {
            @Override
            public void beforeNotify(@NonNull Report report) {
                report.getError().setSeverity(finalSeverity);
                report.getError().setMetaData(finalMetaData);
                report.getError().setContext(finalContext);
            }
        });
    }

    /**
     * Intended for use by other clients (React Native/Unity). Calling this method directly from
     * Android is not supported.
     */
    public static void internalClientNotify(@NonNull final Throwable exception,
                                            Map<String, Object> clientData,
                                            boolean blocking,
                                            Callback callback) {
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
    public static void addToTab(final String tab, final String key, final Object value) {
        getClient().addToTab(tab, key, value);
    }

    /**
     * Remove a tab of app-wide diagnostic information
     *
     * @param tabName the dashboard tab to remove diagnostic data from
     */
    public static void clearTab(String tabName) {
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
    public static void leaveBreadcrumb(@NonNull String name, @NonNull BreadcrumbType type, @NonNull Map<String, String> metadata) {
        getClient().leaveBreadcrumb(name, type, metadata);
    }

    /**
     * Set the maximum number of breadcrumbs to keep and sent to Bugsnag.
     * By default, we'll keep and send the 20 most recent breadcrumb log
     * messages.
     *
     * @param numBreadcrumbs number of breadcrumb log messages to send
     */
    public static void setMaxBreadcrumbs(int numBreadcrumbs) {
        getClient().setMaxBreadcrumbs(numBreadcrumbs);
    }

    /**
     * Clear any breadcrumbs that have been left so far.
     */
    public static void clearBreadcrumbs() {
        getClient().clearBreadcrumbs();
    }

    /**
     * Enable automatic reporting of unhandled exceptions.
     * By default, this is automatically enabled in the constructor.
     */
    public static void enableExceptionHandler() {
        getClient().enableExceptionHandler();
    }

    /**
     * Disable automatic reporting of unhandled exceptions.
     */
    public static void disableExceptionHandler() {
        getClient().disableExceptionHandler();
    }

    /**
     * Sets whether the SDK should write logs. In production apps, it is recommended that this
     * should be set to false.
     * <p>
     * Logging is enabled by default unless the release stage is set to 'production', in which case
     * it will be disabled.
     *
     * @param enabled true if logging is enabled
     */
    public static void setLoggingEnabled(boolean enabled) {
        getClient().setLoggingEnabled(enabled);
    }

    /**
     * Get the current Bugsnag Client instance.
     */
    @NonNull
    public static Client getClient() {
        if (client == null) {
            throw new IllegalStateException("You must call Bugsnag.init before any other Bugsnag methods");
        }

        return client;
    }
}
