package com.bugsnag.android;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;

import java.util.Locale;
import java.util.Collections;
import java.util.Map;
import java.util.Observable;

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
public class Client extends Observable {


    private static final boolean BLOCKING = true;
    private static final String SHARED_PREF_KEY = "com.bugsnag.android";
    private static final String USER_ID_KEY = "user.id";
    private static final String USER_NAME_KEY = "user.name";
    private static final String USER_EMAIL_KEY = "user.email";

    protected final Configuration config;
    private final Context appContext;
    protected final AppData appData;
    protected final DeviceData deviceData;
    final Breadcrumbs breadcrumbs;
    protected final User user = new User();
    protected final ErrorStore errorStore;

    /**
     * Initialize a Bugsnag client
     *
     * @param androidContext an Android context, usually <code>this</code>
     */
    public Client(@NonNull Context androidContext) {
        this(androidContext, null, true);
    }

    /**
     * Initialize a Bugsnag client
     *
     * @param androidContext an Android context, usually <code>this</code>
     * @param apiKey         your Bugsnag API key from your Bugsnag dashboard
     */
    public Client(@NonNull Context androidContext, @Nullable String apiKey) {
        this(androidContext, apiKey, true);
    }

    /**
     * Initialize a Bugsnag client
     *
     * @param androidContext         an Android context, usually <code>this</code>
     * @param apiKey                 your Bugsnag API key from your Bugsnag dashboard
     * @param enableExceptionHandler should we automatically handle uncaught exceptions?
     */
    public Client(@NonNull Context androidContext, @Nullable String apiKey, boolean enableExceptionHandler) {
        this(androidContext, createNewConfiguration(androidContext, apiKey, enableExceptionHandler));
    }

    /**
     * Initialize a Bugsnag client
     *
     * @param androidContext an Android context, usually <code>this</code>
     * @param configuration  a configuration for the Client
     */
    public Client(@NonNull Context androidContext, @NonNull Configuration configuration) {

        appContext = androidContext.getApplicationContext();

        config = configuration;

        String buildUUID = null;
        try {
            ApplicationInfo ai = appContext.getPackageManager().getApplicationInfo(appContext.getPackageName(), PackageManager.GET_META_DATA);
            buildUUID = ai.metaData.getString("com.bugsnag.android.BUILD_UUID");
        } catch (Exception ignore) {
        }
        if (buildUUID != null) {
            config.setBuildUUID(buildUUID);
        }

        // Set up and collect constant app and device diagnostics
        appData = new AppData(appContext, config);
        deviceData = new DeviceData(appContext);
        AppState.init();

        // Set up breadcrumbs
        breadcrumbs = new Breadcrumbs();

        // Set sensible defaults
        setProjectPackages(appContext.getPackageName());

        if (config.getPersistUserBetweenSessions()) {
            // Check to see if a user was stored in the SharedPreferences
            SharedPreferences sharedPref = appContext.getSharedPreferences(SHARED_PREF_KEY, Context.MODE_PRIVATE);
            user.setId(sharedPref.getString(USER_ID_KEY, deviceData.getUserId()));
            user.setName(sharedPref.getString(USER_NAME_KEY, null));
            user.setEmail(sharedPref.getString(USER_EMAIL_KEY, null));
        } else {
            user.setId(deviceData.getUserId());
        }

        // Create the error store that is used in the exception handler
        errorStore = new ErrorStore(config, appContext);

        // Install a default exception handler with this client
        if (config.getEnableExceptionHandler()) {
            enableExceptionHandler();
        }

        // Flush any on-disk errors
        errorStore.flush();
    }

    public void notifyBugsnagObservers(NotifyType type) {
        setChanged();
        super.notifyObservers(type.getValue());
    }

    /**
     * Creates a new configuration object based on the provided parameters
     * will read the API key from the manifest file if it is not provided
     *
     * @param androidContext         The context of the application
     * @param apiKey                 The API key to use
     * @param enableExceptionHandler should we automatically handle uncaught exceptions?
     * @return The created config
     */
    private static Configuration createNewConfiguration(@NonNull Context androidContext, String apiKey, boolean enableExceptionHandler) {
        Context appContext = androidContext.getApplicationContext();

        // Attempt to load API key from AndroidManifest.xml if not passed in
        if (TextUtils.isEmpty(apiKey)) {
            try {
                ApplicationInfo ai = appContext.getPackageManager().getApplicationInfo(appContext.getPackageName(), PackageManager.GET_META_DATA);
                apiKey = ai.metaData.getString("com.bugsnag.android.API_KEY");
            } catch (Exception ignore) {
            }
        }

        if (apiKey == null) {
            throw new NullPointerException("You must provide a Bugsnag API key");
        }

        // Build a configuration object
        Configuration newConfig = new Configuration(apiKey);

        newConfig.setEnableExceptionHandler(enableExceptionHandler);

        return newConfig;
    }

    /**
     * Set the application version sent to Bugsnag. By default we'll pull this
     * from your AndroidManifest.xml
     *
     * @param appVersion the app version to send
     */
    public void setAppVersion(String appVersion) {
        config.setAppVersion(appVersion);
    }

    /**
     * Gets the context to be sent to Bugsnag.
     *
     * @return Context
     */
    public String getContext() {
        return config.getContext();
    }

    /**
     * Set the context sent to Bugsnag. By default we'll attempt to detect the
     * name of the top-most activity at the time of a report, and use this
     * as the context, but sometime this is not possible.
     *
     * @param context set what was happening at the time of a crash
     */
    public void setContext(String context) {
        config.setContext(context);
    }

    /**
     * Set the endpoint to send data to. By default we'll send reports to
     * the standard https://notify.bugsnag.com endpoint, but you can override
     * this if you are using Bugsnag Enterprise to point to your own Bugsnag
     * endpoint.
     *
     * @param endpoint the custom endpoint to send report to
     */
    public void setEndpoint(String endpoint) {
        config.setEndpoint(endpoint);
    }

    /**
     * Set the buildUUID to your own value. This is used to identify proguard
     * mapping files in the case that you publish multiple different apps with
     * the same appId and versionCode. The default value is read from the
     * com.bugsnag.android.BUILD_UUID meta-data field in your app manifest.
     *
     * @param buildUUID the buildUUID.
     */
    public void setBuildUUID(final String buildUUID) {
        config.setBuildUUID(buildUUID);
    }


    /**
     * Set which keys should be filtered when sending metaData to Bugsnag.
     * Use this when you want to ensure sensitive information, such as passwords
     * or credit card information is stripped from metaData you send to Bugsnag.
     * Any keys in metaData which contain these strings will be marked as
     * [FILTERED] when send to Bugsnag.
     * <p/>
     * For example:
     * <p/>
     * client.setFilters("password", "credit_card");
     *
     * @param filters a list of keys to filter from metaData
     */
    public void setFilters(String... filters) {
        config.setFilters(filters);
    }

    /**
     * Set which exception classes should be ignored (not sent) by Bugsnag.
     * <p/>
     * For example:
     * <p/>
     * client.setIgnoreClasses("java.lang.RuntimeException");
     *
     * @param ignoreClasses a list of exception classes to ignore
     */
    public void setIgnoreClasses(String... ignoreClasses) {
        config.setIgnoreClasses(ignoreClasses);
    }

    /**
     * Set for which releaseStages errors should be sent to Bugsnag.
     * Use this to stop errors from development builds being sent.
     * <p/>
     * For example:
     * <p/>
     * client.setNotifyReleaseStages("production");
     *
     * @param notifyReleaseStages a list of releaseStages to notify for
     * @see #setReleaseStage
     */
    public void setNotifyReleaseStages(String... notifyReleaseStages) {
        config.setNotifyReleaseStages(notifyReleaseStages);
    }

    /**
     * Set which packages should be considered part of your application.
     * Bugsnag uses this to help with error grouping, and stacktrace display.
     * <p/>
     * For example:
     * <p/>
     * client.setProjectPackages("com.example.myapp");
     * <p/>
     * By default, we'll mark the current package name as part of you app.
     *
     * @param projectPackages a list of package names
     */
    public void setProjectPackages(String... projectPackages) {
        config.setProjectPackages(projectPackages);
    }

    /**
     * Set the current "release stage" of your application.
     * By default, we'll set this to "development" for debug builds and
     * "production" for non-debug builds.
     *
     * @param releaseStage the release stage of the app
     * @see #setNotifyReleaseStages
     */
    public void setReleaseStage(String releaseStage) {
        config.setReleaseStage(releaseStage);
    }

    /**
     * Set whether to send thread-state with report.
     * By default, this will be true.
     *
     * @param sendThreads should we send thread-state with report?
     */
    public void setSendThreads(boolean sendThreads) {
        config.setSendThreads(sendThreads);
    }

    /**
     * Set details of the user currently using your application.
     * You can search for this information in your Bugsnag dashboard.
     * <p/>
     * For example:
     * <p/>
     * client.setUser("12345", "james@example.com", "James Smith");
     *
     * @param id    a unique identifier of the current user (defaults to a unique id)
     * @param email the email address of the current user
     * @param name  the name of the current user
     */
    public void setUser(String id, String email, String name) {
        setUserId(id);
        setUserEmail(email);
        setUserName(name);
    }

    /**
     * Removes the current user data and sets it back to defaults
     */
    public void clearUser() {
        user.setId(deviceData.getUserId());
        user.setEmail(null);
        user.setName(null);

        SharedPreferences sharedPref = appContext.getSharedPreferences(SHARED_PREF_KEY, Context.MODE_PRIVATE);
        sharedPref.edit()
            .remove(USER_ID_KEY)
            .remove(USER_EMAIL_KEY)
            .remove(USER_NAME_KEY)
            .commit();
        notifyBugsnagObservers(NotifyType.USER);
    }

    /**
     * Set a unique identifier for the user currently using your application.
     * By default, this will be an automatically generated unique id
     * You can search for this information in your Bugsnag dashboard.
     *
     * @param id a unique identifier of the current user
     */
    public void setUserId(String id) {
        setUserId(id, true);
    }

    /**
     * Sets the user ID with the option to not notify any NDK components of the change
     *
     * @param id a unique identifier of the current user
     * @param notify whether or not to notify NDK components
     */
    void setUserId(String id, boolean notify) {
        user.setId(id);

        if (config.getPersistUserBetweenSessions()) {
            storeInSharedPrefs(USER_ID_KEY, id);
        }

        if (notify) {
            notifyBugsnagObservers(NotifyType.USER);
        }
    }

    /**
     * Set the email address of the current user.
     * You can search for this information in your Bugsnag dashboard.
     *
     * @param email the email address of the current user
     */
    public void setUserEmail(String email) {
        setUserEmail(email, true);
    }

    /**
     * Sets the user email with the option to not notify any NDK components of the change
     *
     * @param email the email address of the current user
     * @param notify whether or not to notify NDK components
     */
    void setUserEmail(String email, boolean notify) {
        user.setEmail(email);

        if (config.getPersistUserBetweenSessions()) {
            storeInSharedPrefs(USER_EMAIL_KEY, email);
        }

        if (notify) {
            notifyBugsnagObservers(NotifyType.USER);
        }
    }

    /**
     * Set the name of the current user.
     * You can search for this information in your Bugsnag dashboard.
     *
     * @param name the name of the current user
     */
    public void setUserName(String name) {
        setUserName(name, true);
    }

    /**
     * Sets the user name with the option to not notify any NDK components of the change
     *
     * @param name the name of the current user
     * @param notify whether or not to notify NDK components
     */
    void setUserName(String name, boolean notify) {
        user.setName(name);

        if (config.getPersistUserBetweenSessions()) {
            storeInSharedPrefs(USER_NAME_KEY, name);
        }

        if (notify) {
            notifyBugsnagObservers(NotifyType.USER);
        }
    }

    /**
     * Add a "before notify" callback, to execute code before every
     * report to Bugsnag.
     * <p/>
     * You can use this to add or modify information attached to an error
     * before it is sent to your dashboard. You can also return
     * <code>false</code> from any callback to halt execution.
     * <p/>
     * For example:
     * <p/>
     * client.beforeNotify(new BeforeNotify() {
     * public boolean run(Error error) {
     * error.setSeverity(Severity.INFO);
     * return true;
     * }
     * })
     *
     * @param beforeNotify a callback to run before sending errors to Bugsnag
     * @see BeforeNotify
     */
    public void beforeNotify(BeforeNotify beforeNotify) {
        config.beforeNotify(beforeNotify);
    }

    /**
     * Notify Bugsnag of a handled exception
     *
     * @param exception the exception to send to Bugsnag
     */
    public void notify(Throwable exception) {
        Error error = new Error(config, exception);
        notify(error, !BLOCKING, null);
    }

    /**
     * Notify Bugsnag of a handled exception
     *
     * @param exception the exception to send to Bugsnag
     */
    public void notifyBlocking(Throwable exception) {
        Error error = new Error(config, exception);
        notify(error, BLOCKING, null);
    }

    /**
     * Notify Bugsnag of a handled exception
     *
     * @param exception the exception to send to Bugsnag
     * @param callback callback invoked on the generated error report for
     *                 additional modification
     */
    public void notify(Throwable exception, Callback callback) {
        Error error = new Error(config, exception);
        notify(error, !BLOCKING, callback);
    }

    /**
     * Notify Bugsnag of a handled exception
     *
     * @param exception the exception to send to Bugsnag
     * @param callback callback invoked on the generated error report for
     *                 additional modification
     */
    public void notifyBlocking(Throwable exception, Callback callback) {
        Error error = new Error(config, exception);
        notify(error, BLOCKING, callback);
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
    public void notify(String name, String message, StackTraceElement[] stacktrace, Callback callback) {
        Error error = new Error(config, name, message, stacktrace);
        notify(error, !BLOCKING, callback);
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
    public void notifyBlocking(String name, String message, StackTraceElement[] stacktrace, Callback callback) {
        Error error = new Error(config, name, message, stacktrace);
        notify(error, BLOCKING, callback);
    }

    /**
     * Notify Bugsnag of a handled exception
     *
     * @param exception the exception to send to Bugsnag
     * @param severity  the severity of the error, one of Severity.ERROR,
     *                  Severity.WARNING or Severity.INFO
     */
    public void notify(Throwable exception, Severity severity) {
        Error error = new Error(config, exception);
        error.setSeverity(severity);
        notify(error, !BLOCKING);
    }

    /**
     * Notify Bugsnag of a handled exception
     *
     * @param exception the exception to send to Bugsnag
     * @param severity  the severity of the error, one of Severity.ERROR,
     *                  Severity.WARNING or Severity.INFO
     */
    public void notifyBlocking(Throwable exception, Severity severity) {
        Error error = new Error(config, exception);
        error.setSeverity(severity);
        notify(error, BLOCKING);
    }

    /**
     * Add diagnostic information to every error report.
     * Diagnostic information is collected in "tabs" on your dashboard.
     * <p/>
     * For example:
     * <p/>
     * client.addToTab("account", "name", "Acme Co.");
     * client.addToTab("account", "payingCustomer", true);
     *
     * @param tab   the dashboard tab to add diagnostic data to
     * @param key   the name of the diagnostic information
     * @param value the contents of the diagnostic information
     */
    public void addToTab(String tab, String key, Object value) {
        config.getMetaData().addToTab(tab, key, value);
    }

    /**
     * Remove a tab of app-wide diagnostic information
     *
     * @param tabName the dashboard tab to remove diagnostic data from
     */
    public void clearTab(String tabName) {
        config.getMetaData().clearTab(tabName);
    }

    /**
     * Get the global diagnostic information currently stored in MetaData.
     *
     * @see MetaData
     */
    public MetaData getMetaData() {
        return config.getMetaData();
    }

    /**
     * Set the global diagnostic information to be send with every error.
     *
     * @see MetaData
     */
    public void setMetaData(MetaData metaData) {
        config.setMetaData(metaData);
    }

    /**
     * Leave a "breadcrumb" log message, representing an action that occurred
     * in your app, to aid with debugging.
     *
     * @param breadcrumb the log message to leave (max 140 chars)
     */
    public void leaveBreadcrumb(String breadcrumb) {
        breadcrumbs.add(breadcrumb);
        notifyBugsnagObservers(NotifyType.BREADCRUMB);
    }

    public void leaveBreadcrumb(String name, BreadcrumbType type, Map<String, String> metadata) {
        leaveBreadcrumb(name, type, metadata, true);
    }

    void leaveBreadcrumb(String name,
                         BreadcrumbType type,
                         Map<String, String> metadata,
                         boolean notify) {
        breadcrumbs.add(name, type, metadata);

        if (notify) {
            notifyBugsnagObservers(NotifyType.BREADCRUMB);
        }
    }

    /**
     * Set the maximum number of breadcrumbs to keep and sent to Bugsnag.
     * By default, we'll keep and send the 20 most recent breadcrumb log
     * messages.
     *
     * @param numBreadcrumbs number of breadcrumb log messages to send
     */
    public void setMaxBreadcrumbs(int numBreadcrumbs) {
        breadcrumbs.setSize(numBreadcrumbs);
    }

    /**
     * Clear any breadcrumbs that have been left so far.
     */
    public void clearBreadcrumbs() {
        breadcrumbs.clear();
        notifyBugsnagObservers(NotifyType.BREADCRUMB);
    }

    /**
     * Enable automatic reporting of unhandled exceptions.
     * By default, this is automatically enabled in the constructor.
     */
    public void enableExceptionHandler() {
        ExceptionHandler.enable(this);
    }

    /**
     * Disable automatic reporting of unhandled exceptions.
     */
    public void disableExceptionHandler() {
        ExceptionHandler.disable(this);
    }

    private void notify(Error error, boolean blocking) {
        notify(error, blocking, null);
    }

    private void notify(Error error, boolean blocking, Callback callback) {
        // Don't notify if this error class should be ignored
        if (error.shouldIgnoreClass()) {
            return;
        }

        // Don't notify unless releaseStage is in notifyReleaseStages
        if (!config.shouldNotifyForReleaseStage(appData.getReleaseStage())) {
            return;
        }

        // Capture the state of the app and device and attach diagnostics to the error
        error.setAppData(appData);
        error.setDeviceData(deviceData);
        error.setAppState(new AppState(appContext));
        error.setDeviceState(new DeviceState(appContext));

        // Attach breadcrumbs to the error
        error.setBreadcrumbs(breadcrumbs);

        // Attach user info to the error
        error.setUser(user);

        // Run beforeNotify tasks, don't notify if any return true
        if (!runBeforeNotifyTasks(error)) {
            Logger.info("Skipping notification - beforeNotify task returned false");
            return;
        }

        // Build the report
        Report report = new Report(config.getApiKey(), error);

        if (callback != null) {
            callback.beforeNotify(report);
        }

        if (blocking) {
            deliver(report, error);
        } else {
            final Report finalReport = report;
            final Error finalError = error;
            // Attempt to send the report in the background
            Async.run(new Runnable() {
                @Override
                public void run() {
                    deliver(finalReport, finalError);
                }
            });
        }

        // Add a breadcrumb for this error occurring
        breadcrumbs.add(error.getExceptionName(), BreadcrumbType.ERROR, Collections.singletonMap("message", error.getExceptionMessage()));
    }

    void deliver(Report report, Error error) {
        try {
            HttpClient.post(config.getEndpoint(), report);
            Logger.info(String.format(Locale.US, "Sent 1 new error to Bugsnag"));
        } catch (HttpClient.NetworkException e) {
            Logger.info("Could not send error(s) to Bugsnag, saving to disk to send later");

            // Save error to disk for later sending
            errorStore.write(error);
        } catch (HttpClient.BadResponseException e) {
            Logger.info("Bad response when sending data to Bugsnag");
        } catch (Exception e) {
            Logger.warn("Problem sending error to Bugsnag", e);
        }
    }

    private boolean runBeforeNotifyTasks(Error error) {
        for (BeforeNotify beforeNotify : config.getBeforeNotifyTasks()) {
            try {
                if (!beforeNotify.run(error)) {
                    return false;
                }
            } catch (Throwable ex) {
                Logger.warn("BeforeNotify threw an Exception", ex);
            }
        }

        // By default, allow the error to be sent if there were no objections
        return true;
    }

    /**
     * Stores the given key value pair into shared preferences
     * @param key The key to store
     * @param value The value to store
     * @return Whether the value was stored successfully or not
     */
    private boolean storeInSharedPrefs(String key, String value) {
        SharedPreferences sharedPref = appContext.getSharedPreferences(SHARED_PREF_KEY, Context.MODE_PRIVATE);
        return sharedPref.edit().putString(key, value).commit();
    }

    /**
     * Notify Bugsnag of a handled exception
     *
     * @param exception the exception to send to Bugsnag
     * @param metaData  additional information to send with the exception
     *
     * @deprecated Use {@link #notify(Throwable,Callback)}
     *             to send and modify error reports
     */
    public void notify(Throwable exception, MetaData metaData) {
        Error error = new Error(config, exception);
        error.setMetaData(metaData);
        notify(error, !BLOCKING);
    }

    /**
     * Notify Bugsnag of a handled exception
     *
     * @param exception the exception to send to Bugsnag
     * @param metaData  additional information to send with the exception
     *
     * @deprecated Use {@link #notify(Throwable,Callback)}
     *             to send and modify error reports
     */
    public void notifyBlocking(Throwable exception, MetaData metaData) {
        Error error = new Error(config, exception);
        error.setMetaData(metaData);
        notify(error, BLOCKING);
    }

    /**
     * Notify Bugsnag of a handled exception
     *
     * @param exception the exception to send to Bugsnag
     * @param severity  the severity of the error, one of Severity.ERROR,
     *                  Severity.WARNING or Severity.INFO
     * @param metaData  additional information to send with the exception
     *
     * @deprecated Use {@link #notify(Throwable,Callback)} to send and
     *             modify error reports
     */
    @Deprecated
    public void notify(Throwable exception, Severity severity, MetaData metaData) {
        Error error = new Error(config, exception);
        error.setSeverity(severity);
        error.setMetaData(metaData);
        notify(error, !BLOCKING);
    }

    /**
     * Notify Bugsnag of a handled exception
     *
     * @param exception the exception to send to Bugsnag
     * @param severity  the severity of the error, one of Severity.ERROR,
     *                  Severity.WARNING or Severity.INFO
     * @param metaData  additional information to send with the exception
     *
     * @deprecated Use {@link #notifyBlocking(Throwable,Callback)} to send
     *             and modify error reports
     */
    @Deprecated
    public void notifyBlocking(Throwable exception, Severity severity, MetaData metaData) {
        Error error = new Error(config, exception);
        error.setSeverity(severity);
        error.setMetaData(metaData);
        notify(error, BLOCKING);
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
     *
     * @deprecated Use {@link #notify(String,String,StackTraceElement[],Callback)}
     *             to send and modify error reports
     */
    @Deprecated
    public void notify(String name, String message, StackTraceElement[] stacktrace, Severity severity, MetaData metaData) {
        Error error = new Error(config, name, message, stacktrace);
        error.setSeverity(severity);
        error.setMetaData(metaData);
        notify(error, !BLOCKING);
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
     *
     * @deprecated Use {@link #notifyBlocking(String,String,StackTraceElement[],Callback)}
     *             to send and modify error reports
     */
    @Deprecated
    public void notifyBlocking(String name, String message, StackTraceElement[] stacktrace, Severity severity, MetaData metaData) {
        Error error = new Error(config, name, message, stacktrace);
        error.setSeverity(severity);
        error.setMetaData(metaData);
        notify(error, BLOCKING);
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
     *
     * @deprecated Use {@link #notify(String,String,StackTraceElement[],Callback)}
     *             to send and modify error reports
     */
    @Deprecated
    public void notify(String name, String message, String context, StackTraceElement[] stacktrace, Severity severity, MetaData metaData) {
        Error error = new Error(config, name, message, stacktrace);
        error.setSeverity(severity);
        error.setMetaData(metaData);
        error.setContext(context);
        notify(error, !BLOCKING);
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
     *
     * @deprecated Use {@link #notifyBlocking(String,String,StackTraceElement[],Callback)}
     *             to send and modify error reports
     */
    @Deprecated
    public void notifyBlocking(String name, String message, String context, StackTraceElement[] stacktrace, Severity severity, MetaData metaData) {
        Error error = new Error(config, name, message, stacktrace);
        error.setSeverity(severity);
        error.setMetaData(metaData);
        error.setContext(context);
        notify(error, BLOCKING);
    }
}
