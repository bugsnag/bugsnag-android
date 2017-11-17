package com.bugsnag.android;

import android.app.Application;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;

import java.util.Collections;
import java.util.Date;
import java.util.Map;
import java.util.Observable;
import java.util.Observer;
import java.util.concurrent.RejectedExecutionException;

enum DeliveryStyle {
    SAME_THREAD,
    ASYNC,
    ASYNC_WITH_CACHE
}

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
public class Client extends Observable implements Observer {

    private static final boolean BLOCKING = true;
    private static final String SHARED_PREF_KEY = "com.bugsnag.android";
    private static final String BUGSNAG_NAMESPACE = "com.bugsnag.android";
    private static final String USER_ID_KEY = "user.id";
    private static final String USER_NAME_KEY = "user.name";
    private static final String USER_EMAIL_KEY = "user.email";

    static final String MF_API_KEY = BUGSNAG_NAMESPACE + ".API_KEY";
    static final String MF_BUILD_UUID = BUGSNAG_NAMESPACE + ".BUILD_UUID";
    static final String MF_APP_VERSION = BUGSNAG_NAMESPACE + ".APP_VERSION";
    static final String MF_ENDPOINT = BUGSNAG_NAMESPACE + ".ENDPOINT";
    static final String MF_RELEASE_STAGE = BUGSNAG_NAMESPACE + ".RELEASE_STAGE";
    static final String MF_SEND_THREADS = BUGSNAG_NAMESPACE + ".SEND_THREADS";
    static final String MF_ENABLE_EXCEPTION_HANDLER = BUGSNAG_NAMESPACE + ".ENABLE_EXCEPTION_HANDLER";
    static final String MF_PERSIST_USER_BETWEEN_SESSIONS = BUGSNAG_NAMESPACE + ".PERSIST_USER_BETWEEN_SESSIONS";


    @NonNull
    protected final Configuration config;
    private final Context appContext;
    @NonNull
    protected final AppData appData;
    @NonNull
    protected final DeviceData deviceData;
    @NonNull
    final Breadcrumbs breadcrumbs;
    protected final User user = new User();
    @NonNull
    protected final ErrorStore errorStore;

    private final long launchTimeMs;

    private final EventReceiver eventReceiver = new EventReceiver();
    private ErrorReportApiClient errorReportApiClient;

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
        this(androidContext, configuration, new Date());
    }

    Client(@NonNull Context androidContext, @NonNull Configuration configuration, Date time) {
        launchTimeMs = time.getTime();
        warnIfNotAppContext(androidContext);
        appContext = androidContext.getApplicationContext();
        ConnectivityManager cm = (ConnectivityManager) appContext.getSystemService(Context.CONNECTIVITY_SERVICE);
        errorReportApiClient = new DefaultHttpClient(cm);

        if (appContext instanceof Application) {
            Application application = (Application) appContext;
            application.registerActivityLifecycleCallbacks(new LifecycleBreadcrumbLogger());
        } else {
            Logger.warn("Bugsnag is unable to setup automatic activity lifecycle breadcrumbs on API " +
                "Levels below 14.");
        }

        config = configuration;

        // populate from manifest (in the case where the constructor was called directly by the
        // User or no UUID was supplied)
        if (config.getBuildUUID() == null) {
            String buildUUID = null;
            try {
                ApplicationInfo ai = appContext.getPackageManager().getApplicationInfo(appContext.getPackageName(), PackageManager.GET_META_DATA);
                buildUUID = ai.metaData.getString(MF_BUILD_UUID);
            } catch (Exception ignore) {
            }
            if (buildUUID != null) {
                config.setBuildUUID(buildUUID);
            }
        }

        // Set up and collect constant app and device diagnostics
        SharedPreferences sharedPref = appContext.getSharedPreferences(SHARED_PREF_KEY, Context.MODE_PRIVATE);

        appData = new AppData(appContext, config);
        deviceData = new DeviceData(appContext, sharedPref);

        // Set up breadcrumbs
        breadcrumbs = new Breadcrumbs();

        // Set sensible defaults
        setProjectPackages(appContext.getPackageName());

        if (config.getPersistUserBetweenSessions()) {
            // Check to see if a user was stored in the SharedPreferences
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

        // register a receiver for automatic breadcrumbs

        Async.run(new Runnable() {
            @Override
            public void run() {
                appContext.registerReceiver(eventReceiver, EventReceiver.getIntentFilter());
                appContext.registerReceiver(new ConnectivityChangeReceiver(), new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION));
            }
        });

        config.addObserver(this);

        // Flush any on-disk errors
        errorStore.flushOnLaunch(errorReportApiClient);

        boolean isNotProduction = !AppData.RELEASE_STAGE_PRODUCTION.equals(AppData.guessReleaseStage(appContext));
        Logger.setEnabled(isNotProduction);
    }

    private class ConnectivityChangeReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo networkInfo = cm.getActiveNetworkInfo();
            boolean retryReports = networkInfo != null && networkInfo.isConnectedOrConnecting();

            if (retryReports) {
                errorStore.flushAsync(errorReportApiClient);
            }
        }
    }

    public void notifyBugsnagObservers(@NonNull NotifyType type) {
        setChanged();
        super.notifyObservers(type.getValue());
    }

    @Override
    public void update(Observable o, Object arg) {
        if (arg instanceof Integer) {
            NotifyType type = NotifyType.fromInt((Integer) arg);

            if (type != null) {
                notifyBugsnagObservers(type);
            }
        }
    }

    /**
     * Creates a new configuration object based on the provided parameters
     * will read the API key and other configuration values from the manifest if it is not provided
     *
     * @param androidContext         The context of the application
     * @param apiKey                 The API key to use
     * @param enableExceptionHandler should we automatically handle uncaught exceptions?
     * @return The created config
     */
    @NonNull
    private static Configuration createNewConfiguration(@NonNull Context androidContext, String apiKey, boolean enableExceptionHandler) {
        Context appContext = androidContext.getApplicationContext();

        // Attempt to load API key and other config from AndroidManifest.xml, if not passed in
        boolean loadFromManifest = TextUtils.isEmpty(apiKey);

        if (loadFromManifest) {
            try {
                ApplicationInfo ai = appContext.getPackageManager().getApplicationInfo(appContext.getPackageName(), PackageManager.GET_META_DATA);
                Bundle data = ai.metaData;
                apiKey = data.getString(MF_API_KEY);
            } catch (Exception ignore) {
            }
        }

        if (apiKey == null) {
            throw new NullPointerException("You must provide a Bugsnag API key");
        }

        // Build a configuration object
        Configuration newConfig = new Configuration(apiKey);
        newConfig.setEnableExceptionHandler(enableExceptionHandler);

        if (loadFromManifest) {
            try {
                ApplicationInfo ai = appContext.getPackageManager().getApplicationInfo(appContext.getPackageName(), PackageManager.GET_META_DATA);
                Bundle data = ai.metaData;
                populateConfigFromManifest(newConfig, data);
            } catch (Exception ignore) {
            }
        }
        return newConfig;
    }

    /**
     * Populates the config with meta-data values supplied from the manifest as a Bundle.
     *
     * @param config the config to mutate
     * @param data   the manifest bundle
     * @return the updated config
     */
    @NonNull
    static Configuration populateConfigFromManifest(@NonNull Configuration config, @NonNull Bundle data) {
        config.setBuildUUID(data.getString(MF_BUILD_UUID));
        config.setAppVersion(data.getString(MF_APP_VERSION));
        config.setReleaseStage(data.getString(MF_RELEASE_STAGE));

        String endpoint = data.getString(MF_ENDPOINT);

        if (endpoint != null) {
            config.setEndpoint(endpoint);
        }

        config.setSendThreads(data.getBoolean(MF_SEND_THREADS, true));
        config.setPersistUserBetweenSessions(data.getBoolean(MF_PERSIST_USER_BETWEEN_SESSIONS, false));
        config.setEnableExceptionHandler(data.getBoolean(MF_ENABLE_EXCEPTION_HANDLER, true));
        return config;
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
        Logger.setEnabled(!AppData.RELEASE_STAGE_PRODUCTION.equals(releaseStage));
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
     * @param id     a unique identifier of the current user
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
     * @param email  the email address of the current user
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
     * @param name   the name of the current user
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

    @SuppressWarnings("ConstantConditions")
    void setErrorReportApiClient(@NonNull ErrorReportApiClient errorReportApiClient) {
        if (errorReportApiClient == null) {
            throw new IllegalArgumentException("ErrorReportApiClient cannot be null.");
        }
        this.errorReportApiClient = errorReportApiClient;
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
    public void notify(@NonNull Throwable exception) {
        Error error = new Error.Builder(config, exception)
            .severityReasonType(HandledState.REASON_HANDLED_EXCEPTION)
            .build();
        notify(error, !BLOCKING);
    }

    /**
     * Notify Bugsnag of a handled exception
     *
     * @param exception the exception to send to Bugsnag
     */
    public void notifyBlocking(@NonNull Throwable exception) {
        Error error = new Error.Builder(config, exception)
            .severityReasonType(HandledState.REASON_HANDLED_EXCEPTION)
            .build();
        notify(error, BLOCKING);
    }

    /**
     * Notify Bugsnag of a handled exception
     *
     * @param exception the exception to send to Bugsnag
     * @param callback  callback invoked on the generated error report for
     *                  additional modification
     */
    public void notify(@NonNull Throwable exception, Callback callback) {
        Error error = new Error.Builder(config, exception)
            .severityReasonType(HandledState.REASON_HANDLED_EXCEPTION)
            .build();
        notify(error, DeliveryStyle.ASYNC, callback);
    }

    /**
     * Notify Bugsnag of a handled exception
     *
     * @param exception the exception to send to Bugsnag
     * @param callback  callback invoked on the generated error report for
     *                  additional modification
     */
    public void notifyBlocking(@NonNull Throwable exception, Callback callback) {
        Error error = new Error.Builder(config, exception)
            .severityReasonType(HandledState.REASON_HANDLED_EXCEPTION)
            .build();
        notify(error, DeliveryStyle.SAME_THREAD, callback);
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
    public void notify(@NonNull String name, @NonNull String message, @NonNull StackTraceElement[] stacktrace, Callback callback) {
        Error error = new Error.Builder(config, name, message, stacktrace)
            .severityReasonType(HandledState.REASON_HANDLED_EXCEPTION)
            .build();
        notify(error, DeliveryStyle.ASYNC, callback);
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
    public void notifyBlocking(@NonNull String name, @NonNull String message, @NonNull StackTraceElement[] stacktrace, Callback callback) {
        Error error = new Error.Builder(config, name, message, stacktrace)
            .severityReasonType(HandledState.REASON_HANDLED_EXCEPTION)
            .build();
        notify(error, DeliveryStyle.SAME_THREAD, callback);
    }

    /**
     * Notify Bugsnag of a handled exception
     *
     * @param exception the exception to send to Bugsnag
     * @param severity  the severity of the error, one of Severity.ERROR,
     *                  Severity.WARNING or Severity.INFO
     */
    public void notify(@NonNull Throwable exception, Severity severity) {
        Error error = new Error.Builder(config, exception)
            .severity(severity)
            .build();
        notify(error, !BLOCKING);
    }

    /**
     * Notify Bugsnag of a handled exception
     *
     * @param exception the exception to send to Bugsnag
     * @param severity  the severity of the error, one of Severity.ERROR,
     *                  Severity.WARNING or Severity.INFO
     */
    public void notifyBlocking(@NonNull Throwable exception, Severity severity) {
        Error error = new Error.Builder(config, exception)
            .severity(severity)
            .build();
        notify(error, BLOCKING);
    }

    public void internalClientNotify(@NonNull Throwable exception,
                              Map<String, Object> clientData,
                              boolean blocking,
                              Callback callback) {
        String severity = getKeyFromClientData(clientData, "severity", true);
        String severityReason = getKeyFromClientData(clientData, "severityReason", true);
        String logLevel = getKeyFromClientData(clientData, "logLevel", false);

        String msg = String.format("Internal client notify, severity = '%s'," +
            " severityReason = '%s'", severity, severityReason);
        Logger.info(msg);

        @SuppressWarnings("WrongConstant")
        Error error = new Error.Builder(config, exception)
            .severity(Severity.fromString(severity))
            .severityReasonType(severityReason)
            .attributeValue(logLevel)
            .build();

        DeliveryStyle deliveryStyle = blocking ? DeliveryStyle.SAME_THREAD : DeliveryStyle.ASYNC;
        notify(error, deliveryStyle, callback);
    }

    @NonNull
    private String getKeyFromClientData(Map<String, Object> clientData, String key, boolean required) {
        Object value = clientData.get(key);
        if (value instanceof String) {
             return (String) value;
        } else if (required) {
            throw new IllegalStateException("Failed to set " + key + " in client data!");
        }
        return null;
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
    @NonNull public MetaData getMetaData() {
        return config.getMetaData();
    }

    /**
     * Set the global diagnostic information to be send with every error.
     *
     * @see MetaData
     */
    public void setMetaData(@NonNull MetaData metaData) {
        config.setMetaData(metaData);
    }

    /**
     * Leave a "breadcrumb" log message, representing an action that occurred
     * in your app, to aid with debugging.
     *
     * @param breadcrumb the log message to leave (max 140 chars)
     */
    public void leaveBreadcrumb(@NonNull String breadcrumb) {
        breadcrumbs.add(breadcrumb);
        notifyBugsnagObservers(NotifyType.BREADCRUMB);
    }

    public void leaveBreadcrumb(@NonNull String name, @NonNull BreadcrumbType type, @NonNull Map<String, String> metadata) {
        leaveBreadcrumb(name, type, metadata, true);
    }

    void leaveBreadcrumb(@NonNull String name,
                         @NonNull BreadcrumbType type,
                         @NonNull Map<String, String> metadata,
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

    private void notify(@NonNull Error error, boolean blocking) {
        DeliveryStyle style = blocking ? DeliveryStyle.SAME_THREAD : DeliveryStyle.ASYNC;
        notify(error, style, null);
    }

    void notify(@NonNull Error error, @NonNull DeliveryStyle style, @Nullable Callback callback) {
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

        switch (style) {
            case SAME_THREAD:
                deliver(report, error);
                break;
            case ASYNC:
                final Report finalReport = report;
                final Error finalError = error;

                // Attempt to send the report in the background
                try {
                    Async.run(new Runnable() {
                        @Override
                        public void run() {
                            deliver(finalReport, finalError);
                        }
                    });
                } catch (RejectedExecutionException e) {
                    errorStore.write(error);
                    Logger.warn("Exceeded max queue count, saving to disk to send later");
                }
                break;
            case ASYNC_WITH_CACHE:
                errorStore.write(error);
                errorStore.flushAsync(errorReportApiClient);
        }

        // Add a breadcrumb for this error occurring
        breadcrumbs.add(error.getExceptionName(), BreadcrumbType.ERROR, Collections.singletonMap("message", error.getExceptionMessage()));
    }

    void deliver(@NonNull Report report, @NonNull Error error) {
        try {
            errorReportApiClient.postReport(config.getEndpoint(), report);
            Logger.info("Sent 1 new error to Bugsnag");
        } catch (DefaultHttpClient.NetworkException e) {
            Logger.info("Could not send error(s) to Bugsnag, saving to disk to send later");

            // Save error to disk for later sending
            errorStore.write(error);
        } catch (DefaultHttpClient.BadResponseException e) {
            Logger.info("Bad response when sending data to Bugsnag");
        } catch (Exception e) {
            Logger.warn("Problem sending error to Bugsnag", e);
        }
    }

    /**
     * Caches an error then attempts to notify.
     *
     * Should only ever be called from the {@link ExceptionHandler}.
     */
    void cacheAndNotify(@NonNull Throwable exception, Severity severity, MetaData metaData,
                        @HandledState.SeverityReason String severityReason,
                        @Nullable String attributeValue) {
        Error error = new Error.Builder(config, exception)
            .severity(severity)
            .metaData(metaData)
            .severityReasonType(severityReason)
            .attributeValue(attributeValue)
            .build();

        notify(error, DeliveryStyle.ASYNC_WITH_CACHE, null);
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
     *
     * @param key   The key to store
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
     * @deprecated Use {@link #notify(Throwable, Callback)}
     * to send and modify error reports
     */
    public void notify(@NonNull Throwable exception,
                       @NonNull MetaData metaData) {
        Error error = new Error.Builder(config, exception)
            .metaData(metaData)
            .severityReasonType(HandledState.REASON_HANDLED_EXCEPTION)
            .build();
        notify(error, !BLOCKING);
    }

    /**
     * Notify Bugsnag of a handled exception
     *
     * @param exception the exception to send to Bugsnag
     * @param metaData  additional information to send with the exception
     * @deprecated Use {@link #notify(Throwable, Callback)}
     * to send and modify error reports
     */
    public void notifyBlocking(@NonNull Throwable exception,
                               @NonNull MetaData metaData) {
        Error error = new Error.Builder(config, exception)
            .severityReasonType(HandledState.REASON_HANDLED_EXCEPTION)
            .metaData(metaData)
            .build();
        notify(error, BLOCKING);
    }

    /**
     * Retrieves the time at which the client was launched
     *
     * @return the ms since the java epoch
     */
    public long getLaunchTimeMs() {
        return launchTimeMs;
    }

    /**
     * Notify Bugsnag of a handled exception
     *
     * @param exception the exception to send to Bugsnag
     * @param severity  the severity of the error, one of Severity.ERROR,
     *                  Severity.WARNING or Severity.INFO
     * @param metaData  additional information to send with the exception
     * @deprecated Use {@link #notify(Throwable, Callback)} to send and
     * modify error reports
     */
    @Deprecated
    public void notify(@NonNull Throwable exception, Severity severity,
                       @NonNull MetaData metaData) {
        Error error = new Error.Builder(config, exception)
            .metaData(metaData)
            .severity(severity)
            .build();
        notify(error, !BLOCKING);
    }

    /**
     * Notify Bugsnag of a handled exception
     *
     * @param exception the exception to send to Bugsnag
     * @param severity  the severity of the error, one of Severity.ERROR,
     *                  Severity.WARNING or Severity.INFO
     * @param metaData  additional information to send with the exception
     * @deprecated Use {@link #notifyBlocking(Throwable, Callback)} to send
     * and modify error reports
     */
    @Deprecated
    public void notifyBlocking(@NonNull Throwable exception, Severity severity,
                               @NonNull MetaData metaData) {
        Error error = new Error.Builder(config, exception)
            .metaData(metaData)
            .severity(severity)
            .build();
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
     * @deprecated Use {@link #notify(String, String, StackTraceElement[], Callback)}
     * to send and modify error reports
     */
    @Deprecated
    public void notify(@NonNull String name, @NonNull String message,
                       @NonNull StackTraceElement[] stacktrace, Severity severity,
                       @NonNull MetaData metaData) {
        Error error = new Error.Builder(config, name, message, stacktrace)
            .severity(severity)
            .metaData(metaData)
            .build();
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
     * @deprecated Use {@link #notifyBlocking(String, String, StackTraceElement[], Callback)}
     * to send and modify error reports
     */
    @Deprecated
    public void notifyBlocking(@NonNull String name, @NonNull String message,
                               @NonNull StackTraceElement[] stacktrace, Severity severity,
                               @NonNull MetaData metaData) {
        Error error = new Error.Builder(config, name, message, stacktrace)
            .severity(severity)
            .metaData(metaData)
            .build();
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
     * @deprecated Use {@link #notify(String, String, StackTraceElement[], Callback)}
     * to send and modify error reports
     */
    @Deprecated
    public void notify(@NonNull String name, @NonNull String message, String context,
                       @NonNull StackTraceElement[] stacktrace, Severity severity,
                       @NonNull MetaData metaData) {
        Error error = new Error.Builder(config, name, message, stacktrace)
            .severity(severity)
            .metaData(metaData)
            .build();
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
     * @deprecated Use {@link #notifyBlocking(String, String, StackTraceElement[], Callback)}
     * to send and modify error reports
     */
    @Deprecated
    public void notifyBlocking(@NonNull String name, @NonNull String message, String context,
                               @NonNull StackTraceElement[] stacktrace, Severity severity,
                               @NonNull MetaData metaData) {
        Error error = new Error.Builder(config, name, message, stacktrace)
            .severity(severity)
            .metaData(metaData)
            .build();
        error.setContext(context);
        notify(error, BLOCKING);
    }

    /**
     * Finalize by removing the receiver
     *
     * @throws Throwable if something goes wrong
     */
    protected void finalize() throws Throwable {
        if (eventReceiver != null) {
            try {
                appContext.unregisterReceiver(eventReceiver);
            } catch (IllegalArgumentException e) {
                Logger.warn("Receiver not registered");
            }
        }
        super.finalize();
    }

    private static void warnIfNotAppContext(Context androidContext) {
        if (!(androidContext instanceof Application)) {
            Logger.warn("Warning - Non-Application context detected! Please ensure that you are " +
                "initializing Bugsnag from a custom Application class.");
        }
    }

    /**
     * Sets whether the SDK should write logs. In production apps, it is recommended that this
     * should be set to false.
     * <p>
     * Logging is enabled by default unless the release stage is set to 'production', in which case
     * it will be disabled.
     *
     * @param loggingEnabled true if logging is enabled
     */
    public void setLoggingEnabled(boolean loggingEnabled) {
        Logger.setEnabled(loggingEnabled);
    }

}
