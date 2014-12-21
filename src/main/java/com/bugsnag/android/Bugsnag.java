package com.bugsnag.android;

import android.content.Context;

/**
 * Static access to a Bugsnag Client, the easiest way to use Bugsnag in your Android app.
 * For example:
 *
 *     Bugsnag.init(this, "your-api-key");
 *     Bugsnag.notify(new RuntimeException("something broke!"));
 *
 * @see Client
 */
public final class Bugsnag {
    private static Client client;
    private Bugsnag() {}

    /**
     * Initialize the static Bugsnag client
     *
     * @param  androidContext  an Android context, usually <code>this</code>
     * @param  apiKey          your Bugsnag API key from your Bugsnag dashboard
     */
    public static Client init(Context androidContext, String apiKey) {
        return init(androidContext, apiKey, true);
    }

    /**
     * Initialize the static Bugsnag client
     *
     * @param  androidContext          an Android context, usually <code>this</code>
     * @param  apiKey                  your Bugsnag API key from your Bugsnag dashboard
     * @param  enableExceptionHandler  should we automatically handle uncaught exceptions?
     */
    public static Client init(Context androidContext, String apiKey, boolean enableExceptionHandler) {
        client = new Client(androidContext, apiKey, enableExceptionHandler);
        return client;
    }

    /**
     * Set the application version sent to Bugsnag. By default we'll pull this
     * from your AndroidManifest.xml
     *
     * @param  appVersion  the app version to send
     */
    public static void setAppVersion(final String appVersion) {
        getClient().setAppVersion(appVersion);
    }

    /**
     * Set the context sent to Bugsnag. By default we'll attempt to detect the
     * name of the top-most activity at the time of a notification, and use this
     * as the context, but sometime this is not possible.
     *
     * @param  context  set what was happening at the time of a crash
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
     * @param  endpoint  the custom endpoint to send notifications to
     */
    public static void setEndpoint(final String endpoint) {
        getClient().setEndpoint(endpoint);
    }

    /**
     * Set which keys should be filtered when sending metaData to Bugsnag.
     * Use this when you want to ensure sensitive information, such as passwords
     * or credit card information is stripped from metaData you send to Bugsnag.
     * Any keys in metaData which contain these strings will be marked as
     * [FILTERED] when send to Bugsnag.
     *
     * For example:
     *
     *     Bugsnag.setFilters("password", "credit_card");
     *
     * @param  filters  a list of keys to filter from metaData
     */
    public static void setFilters(final String... filters) {
        getClient().setFilters(filters);
    }

    /**
     * Set which exception classes should be ignored (not sent) by Bugsnag.
     *
     * For example:
     *
     *     Bugsnag.setIgnoreClasses("java.lang.RuntimeException");
     *
     * @param  ignoreClasses  a list of exception classes to ignore
     */
    public static void setIgnoreClasses(final String... ignoreClasses) {
        getClient().setIgnoreClasses(ignoreClasses);
    }

    /**
     * Set for which releaseStages errors should be sent to Bugsnag.
     * Use this to stop errors from development builds being sent.
     *
     * For example:
     *
     *     Bugsnag.setNotifyReleaseStages("production");
     *
     * @param  notifyReleaseStages  a list of releaseStages to notify for
     * @see    #setReleaseStage
     */
    public static void setNotifyReleaseStages(final String... notifyReleaseStages) {
        getClient().setNotifyReleaseStages(notifyReleaseStages);
    }

    /**
     * Set which packages should be considered part of your application.
     * Bugsnag uses this to help with error grouping, and stacktrace display.
     *
     * For example:
     *
     *     Bugsnag.setProjectPackages("com.example.myapp");
     *
     * By default, we'll mark the current package name as part of you app.
     *
     * @param  projectPackages  a list of package names
     */
    public static void setProjectPackages(final String... projectPackages) {
        getClient().setProjectPackages(projectPackages);
    }

    /**
     * Set the current "release stage" of your application.
     * By default, we'll set this to "development" for debug builds and
     * "production" for non-debug builds.
     *
     * @param  releaseStage  the release stage of the app
     * @see    #setNotifyReleaseStages
     */
    public static void setReleaseStage(final String releaseStage) {
        getClient().setReleaseStage(releaseStage);
    }

    /**
     * Set whether to send thread-state with notifications.
     * By default, this will be true.
     *
     * @param  sendThreads  should we send thread-state with notifications?
     */
    public static void setSendThreads(final boolean sendThreads) {
        getClient().setSendThreads(sendThreads);
    }

    /**
     * Set details of the user currently using your application.
     * You can search for this information in your Bugsnag dashboard.
     *
     * For example:
     *
     *     Bugsnag.setUser("12345", "james@example.com", "James Smith");
     *
     * @param  id     a unique identifier of the current user (defaults to a unique id)
     * @param  email  the email address of the current user
     * @param  name   the name of the current user
     */
    public static void setUser(final String id, final String email, final String name) {
        getClient().setUser(id, email, name);
    }

    /**
     * Set a unique identifier for the user currently using your application.
     * By default, this will be an automatically generated unique id
     * You can search for this information in your Bugsnag dashboard.
     *
     * @param  id  a unique identifier of the current user
     */
    public static void setUserId(final String id) {
        getClient().setUserId(id);
    }

    /**
     * Set the email address of the current user.
     * You can search for this information in your Bugsnag dashboard.
     *
     * @param  email  the email address of the current user
     */
    public static void setUserEmail(final String email) {
        getClient().setUserEmail(email);
    }

    /**
     * Set the name of the current user.
     * You can search for this information in your Bugsnag dashboard.
     *
     * @param  name   the name of the current user
     */
    public static void setUserName(final String name) {
        getClient().setUserName(name);
    }

    /**
     * Add a "before notify" callback, to execute code before every
     * notification to Bugsnag.
     *
     * You can use this to add or modify information attached to an error
     * before it is sent to your dashboard. You can also return
     * <code>false</code> from any callback to halt execution.
     *
     * For example:
     *
     *     Bugsnag.beforeNotify(new BeforeNotify() {
     *         public boolean run(Error error) {
     *             error.setSeverity(Severity.INFO);
     *             return true;
     *         }
     *     })
     *
     * @param  beforeNotify  a callback to run before sending errors to Bugsnag
     * @see    BeforeNotify
     */
    public static void beforeNotify(final BeforeNotify beforeNotify) {
        getClient().beforeNotify(beforeNotify);
    }

    /**
     * Notify Bugsnag of a handled exception
     *
     * @param  exception  the exception to send to Bugsnag
     */
    public static void notify(final Throwable exception) {
        getClient().notify(exception);
    }

    /**
     * Notify Bugsnag of a handled exception
     *
     * @param  exception  the exception to send to Bugsnag
     * @param  severity   the severity of the error, one of Severity.ERROR,
     *                    Severity.WARNING or Severity.INFO
     */
    public static void notify(final Throwable exception, final Severity severity) {
        getClient().notify(exception, severity);
    }

    /**
     * Notify Bugsnag of a handled exception
     *
     * @param  exception  the exception to send to Bugsnag
     * @param  metaData   additional information to send with the exception
     */
    public static void notify(final Throwable exception, final MetaData metaData) {
        getClient().notify(exception, metaData);
    }

    /**
     * Notify Bugsnag of a handled exception
     *
     * @param  exception  the exception to send to Bugsnag
     * @param  severity   the severity of the error, one of Severity.ERROR,
     *                    Severity.WARNING or Severity.INFO
     * @param  metaData   additional information to send with the exception
     */
    public static void notify(final Throwable exception, final Severity severity, final MetaData metaData) {
        getClient().notify(exception, severity, metaData);
    }

    /**
     * Add diagnostic information to every error report.
     * Diagnostic information is collected in "tabs" on your dashboard.
     *
     * For example:
     *
     *     Bugsnag.addToTab("account", "name", "Acme Co.");
     *     Bugsnag.addToTab("account", "payingCustomer", true);
     *
     * @param  tab    the dashboard tab to add diagnostic data to
     * @param  key    the name of the diagnostic information
     * @param  value  the contents of the diagnostic information
     */
    public static void addToTab(final String tab, final String key, final Object value) {
        getClient().addToTab(tab, key, value);
    }

    /**
     * Remove a tab of app-wide diagnostic information
     *
     * @param  tabName  the dashboard tab to remove diagnostic data from
     */
    public void clearTab(String tabName) {
        getClient().clearTab(tabName);
    }

    /**
     * Get the global diagnostic information currently stored in MetaData.
     *
     * @see  MetaData
     */
    public static MetaData getMetaData() {
        return getClient().getMetaData();
    }

    /**
     * Set the global diagnostic information to be send with every error.
     *
     * @see  MetaData
     */
    public static void setMetaData(final MetaData metaData) {
        getClient().setMetaData(metaData);
    }

    /**
     * Leave a "breadcrumb" log message, representing an action that occurred
     * in your app, to aid with debugging.
     *
     * @param  message  the log message to leave (max 140 chars)
     */
    public static void leaveBreadcrumb(String message) {
        getClient().leaveBreadcrumb(message);
    }

    /**
     * Set the maximum number of breadcrumbs to keep and sent to Bugsnag.
     * By default, we'll keep and send the 20 most recent breadcrumb log
     * messages.
     *
     * @param  numBreadcrumbs  number of breadcrumb log messages to send
     */
    public void setMaxBreadcrumbs(int numBreadcrumbs) {
        getClient().setMaxBreadcrumbs(numBreadcrumbs);
    }

    /**
     * Clear any breadcrumbs that have been left so far.
     */
    public void clearBreadcrumbs() {
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
    public void disableExceptionHandler() {
        getClient().disableExceptionHandler();
    }

    /**
     * Get the current Bugsnag Client instance.
     */
    public static Client getClient() {
        if(client == null) {
            throw new IllegalStateException("You must call Bugsnag.init before any other Bugsnag methods");
        }

        return client;
    }
}
