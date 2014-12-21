package com.bugsnag.android;

import android.content.Context;

/**
 * A Bugsnag Client instance allows you to use Bugsnag in your Android app.
 * Typically you'd instead use the static access provided in the Bugsnag class.
 *
 * Example usage:
 *
 *     Client client = new Client(this, "your-api-key");
 *     client.notify(new RuntimeException("something broke!"));
 *
 * @see Bugsnag
 */
public class Client {
    private Configuration config;
    private Context appContext;
    private AppData appData;
    private DeviceData deviceData;
    private Breadcrumbs breadcrumbs;
    private User user = new User();
    private ErrorStore errorStore;

    /**
     * Initialize a Bugsnag client
     *
     * @param  androidContext  an Android context, usually <code>this</code>
     * @param  apiKey          your Bugsnag API key from your Bugsnag dashboard
     */
    public Client(Context androidContext, String apiKey) {
        this(androidContext, apiKey, true);
    }

    /**
     * Initialize a Bugsnag client
     *
     * @param  androidContext          an Android context, usually <code>this</code>
     * @param  apiKey                  your Bugsnag API key from your Bugsnag dashboard
     * @param  enableExceptionHandler  should we automatically handle uncaught exceptions?
     */
    public Client(Context androidContext, String apiKey, boolean enableExceptionHandler) {
        if(androidContext == null) {
            throw new NullPointerException("You must provide a non-null android Context");
        }

        if(apiKey == null) {
            throw new NullPointerException("You must provide a Bugsnag API key");
        }

        // Build a configuration object
        config = new Configuration(apiKey);

        // Get the application context, many things need this
        appContext = androidContext.getApplicationContext();

        // Set up and collect constant app and device diagnostics
        appData = new AppData(appContext, config);
        deviceData = new DeviceData(appContext);
        AppState.init();

        // Set up breadcrumbs
        breadcrumbs = new Breadcrumbs();

        // Set sensible defaults
        setProjectPackages(appContext.getPackageName());
        setUserId(deviceData.getUserId());

        // Create the error store that is used in the exception handler
        errorStore = new ErrorStore(config, appContext);

        // Install a default exception handler with this client
        if(enableExceptionHandler) {
            enableExceptionHandler();
        }

        // Flush any on-disk errors
        errorStore.flush();
    }

    /**
     * Set the application version sent to Bugsnag. By default we'll pull this
     * from your AndroidManifest.xml
     *
     * @param  appVersion  the app version to send
     */
    public void setAppVersion(String appVersion) {
        config.appVersion = appVersion;
    }

    /**
     * Set the context sent to Bugsnag. By default we'll attempt to detect the
     * name of the top-most activity at the time of a notification, and use this
     * as the context, but sometime this is not possible.
     *
     * @param  context  set what was happening at the time of a crash
     */
    public void setContext(String context) {
        config.context = context;
    }

    /**
     * Set the endpoint to send data to. By default we'll send reports to
     * the standard https://notify.bugsnag.com endpoint, but you can override
     * this if you are using Bugsnag Enterprise to point to your own Bugsnag
     * endpoint.
     *
     * @param  endpoint  the custom endpoint to send notifications to
     */
    public void setEndpoint(String endpoint) {
        config.endpoint = endpoint;
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
     *     client.setFilters("password", "credit_card");
     *
     * @param  filters  a list of keys to filter from metaData
     */
    public void setFilters(String... filters) {
        config.filters = filters;
    }

    /**
     * Set which exception classes should be ignored (not sent) by Bugsnag.
     *
     * For example:
     *
     *     client.setIgnoreClasses("java.lang.RuntimeException");
     *
     * @param  ignoreClasses  a list of exception classes to ignore
     */
    public void setIgnoreClasses(String... ignoreClasses) {
        config.ignoreClasses = ignoreClasses;
    }

    /**
     * Set for which releaseStages errors should be sent to Bugsnag.
     * Use this to stop errors from development builds being sent.
     *
     * For example:
     *
     *     client.setNotifyReleaseStages("production");
     *
     * @param  notifyReleaseStages  a list of releaseStages to notify for
     * @see    #setReleaseStage
     */
    public void setNotifyReleaseStages(String... notifyReleaseStages) {
        config.notifyReleaseStages = notifyReleaseStages;
    }

    /**
     * Set which packages should be considered part of your application.
     * Bugsnag uses this to help with error grouping, and stacktrace display.
     *
     * For example:
     *
     *     client.setProjectPackages("com.example.myapp");
     *
     * By default, we'll mark the current package name as part of you app.
     *
     * @param  projectPackages  a list of package names
     */
    public void setProjectPackages(String... projectPackages) {
        config.projectPackages = projectPackages;
    }

    /**
     * Set the current "release stage" of your application.
     * By default, we'll set this to "development" for debug builds and
     * "production" for non-debug builds.
     *
     * @param  releaseStage  the release stage of the app
     * @see    #setNotifyReleaseStages
     */
    public void setReleaseStage(String releaseStage) {
        config.releaseStage = releaseStage;
    }

    /**
     * Set whether to send thread-state with notifications.
     * By default, this will be true.
     *
     * @param  sendThreads  should we send thread-state with notifications?
     */
    public void setSendThreads(boolean sendThreads) {
        config.sendThreads = sendThreads;
    }

    /**
     * Set details of the user currently using your application.
     * You can search for this information in your Bugsnag dashboard.
     *
     * For example:
     *
     *     client.setUser("12345", "james@example.com", "James Smith");
     *
     * @param  id     a unique identifier of the current user (defaults to a unique id)
     * @param  email  the email address of the current user
     * @param  name   the name of the current user
     */
    public void setUser(String id, String email, String name) {
        user.setId(id);
        user.setEmail(email);
        user.setName(name);
    }

    /**
     * Set a unique identifier for the user currently using your application.
     * By default, this will be an automatically generated unique id
     * You can search for this information in your Bugsnag dashboard.
     *
     * @param  id  a unique identifier of the current user
     */
    public void setUserId(String id) {
        user.setId(id);
    }

    /**
     * Set the email address of the current user.
     * You can search for this information in your Bugsnag dashboard.
     *
     * @param  email  the email address of the current user
     */
    public void setUserEmail(String email) {
        user.setEmail(email);
    }

    /**
     * Set the name of the current user.
     * You can search for this information in your Bugsnag dashboard.
     *
     * @param  name   the name of the current user
     */
    public void setUserName(String name) {
        user.setName(name);
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
     *     client.beforeNotify(new BeforeNotify() {
     *         public boolean run(Error error) {
     *             error.setSeverity(Severity.INFO);
     *             return true;
     *         }
     *     })
     *
     * @param  beforeNotify  a callback to run before sending errors to Bugsnag
     * @see    BeforeNotify
     */
    public void beforeNotify(BeforeNotify beforeNotify) {
        config.beforeNotify(beforeNotify);
    }

    /**
     * Notify Bugsnag of a handled exception
     *
     * @param  exception  the exception to send to Bugsnag
     */
    public void notify(Throwable exception) {
        Error error = new Error(config, exception);
        notify(error);
    }

    /**
     * Notify Bugsnag of a handled exception
     *
     * @param  exception  the exception to send to Bugsnag
     * @param  severity   the severity of the error, one of Severity.ERROR,
     *                    Severity.WARNING or Severity.INFO
     */
    public void notify(Throwable exception, Severity severity) {
        Error error = new Error(config, exception);
        error.setSeverity(severity);
        notify(error);
    }

    /**
     * Notify Bugsnag of a handled exception
     *
     * @param  exception  the exception to send to Bugsnag
     * @param  metaData   additional information to send with the exception
     */
    public void notify(Throwable exception, MetaData metaData) {
        Error error = new Error(config, exception);
        error.setMetaData(metaData);
        notify(error);
    }

    /**
     * Notify Bugsnag of a handled exception
     *
     * @param  exception  the exception to send to Bugsnag
     * @param  severity   the severity of the error, one of Severity.ERROR,
     *                    Severity.WARNING or Severity.INFO
     * @param  metaData   additional information to send with the exception
     */
    public void notify(Throwable exception, Severity severity, MetaData metaData) {
        Error error = new Error(config, exception);
        error.setSeverity(severity);
        error.setMetaData(metaData);
        notify(error);
    }

    /**
     * Add diagnostic information to every error report.
     * Diagnostic information is collected in "tabs" on your dashboard.
     *
     * For example:
     *
     *     client.addToTab("account", "name", "Acme Co.");
     *     client.addToTab("account", "payingCustomer", true);
     *
     * @param  tab    the dashboard tab to add diagnostic data to
     * @param  key    the name of the diagnostic information
     * @param  value  the contents of the diagnostic information
     */
    public void addToTab(String tab, String key, Object value) {
        config.metaData.addToTab(tab, key, value);
    }

    /**
     * Remove a tab of app-wide diagnostic information
     *
     * @param  tabName  the dashboard tab to remove diagnostic data from
     */
    public void clearTab(String tabName) {
        config.metaData.clearTab(tabName);
    }

    /**
     * Get the global diagnostic information currently stored in MetaData.
     *
     * @see  MetaData
     */
    public MetaData getMetaData() {
        return config.metaData;
    }

    /**
     * Set the global diagnostic information to be send with every error.
     *
     * @see  MetaData
     */
    public void setMetaData(MetaData metaData) {
        config.metaData = metaData;
    }

    /**
     * Leave a "breadcrumb" log message, representing an action that occurred
     * in your app, to aid with debugging.
     *
     * @param  breadcrumb  the log message to leave (max 140 chars)
     */
    public void leaveBreadcrumb(String breadcrumb) {
        breadcrumbs.add(breadcrumb);
    }

    /**
     * Set the maximum number of breadcrumbs to keep and sent to Bugsnag.
     * By default, we'll keep and send the 20 most recent breadcrumb log
     * messages.
     *
     * @param  numBreadcrumbs  number of breadcrumb log messages to send
     */
    public void setMaxBreadcrumbs(int numBreadcrumbs) {
        breadcrumbs.setSize(numBreadcrumbs);
    }

    /**
     * Clear any breadcrumbs that have been left so far.
     */
    public void clearBreadcrumbs() {
        breadcrumbs.clear();
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

    private void notify(final Error error) {
        // Don't notify if this error class should be ignored
        if(error.shouldIgnoreClass()) {
            return;
        }

        // Don't notify unless releaseStage is in notifyReleaseStages
        if(!config.shouldNotifyForReleaseStage(appData.getReleaseStage())) {
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
        if(!runBeforeNotifyTasks(error)) {
            Logger.info("Skipping notification - beforeNotify task returned false");
            return;
        }

        // Build the notification
        final Notification notification = new Notification(config);
        notification.addError(error);

        // Attempt to send the notification in the background
        Async.run(new Runnable() {
            @Override
            public void run() {
                try {
                    int errorCount = notification.deliver();
                    Logger.info(String.format("Sent %d new error(s) to Bugsnag", errorCount));
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
        });
    }

    private boolean runBeforeNotifyTasks(Error error) {
        for (BeforeNotify beforeNotify : config.beforeNotifyTasks) {
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
}
