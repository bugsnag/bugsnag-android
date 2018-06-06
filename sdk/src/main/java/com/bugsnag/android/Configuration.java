package com.bugsnag.android;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;

import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Observable;
import java.util.Observer;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * User-specified configuration storage object, contains information
 * specified at the client level, api-key and endpoint configuration.
 */
public class Configuration extends Observable implements Observer {

    private static final String HEADER_API_PAYLOAD_VERSION = "Bugsnag-Payload-Version";
    private static final String HEADER_API_KEY = "Bugsnag-Api-Key";
    private static final String HEADER_BUGSNAG_SENT_AT = "Bugsnag-Sent-At";

    @NonNull
    private final String apiKey;
    private String buildUuid;
    private String appVersion;
    private String context;
    private volatile String endpoint = "https://notify.bugsnag.com";
    private volatile String sessionEndpoint = "https://sessions.bugsnag.com";

    private String[] ignoreClasses;
    @Nullable
    private String[] notifyReleaseStages = null;
    private String[] projectPackages;
    private String releaseStage;
    private boolean sendThreads = true;
    private boolean enableExceptionHandler = true;
    private boolean persistUserBetweenSessions = false;
    private long launchCrashThresholdMs = 5 * 1000;
    private boolean autoCaptureSessions = true;
    private boolean automaticallyCollectBreadcrumbs = true;

    @NonNull
    String defaultExceptionType = "android";

    @NonNull
    private MetaData metaData;
    private final Collection<BeforeNotify> beforeNotifyTasks = new ConcurrentLinkedQueue<>();
    private final Collection<BeforeRecordBreadcrumb> beforeRecordBreadcrumbTasks
        = new ConcurrentLinkedQueue<>();
    private String codeBundleId;
    private String notifierType;

    private Delivery delivery;

    /**
     * Construct a new Bugsnag configuration object
     *
     * @param apiKey The API key to send reports to
     */
    public Configuration(@NonNull String apiKey) {
        this.apiKey = apiKey;
        this.metaData = new MetaData();
        this.metaData.addObserver(this);
    }

    /**
     * Gets the API key to send reports to
     *
     * @return API key
     */
    @NonNull
    public String getApiKey() {
        return apiKey;
    }

    /**
     * Gets the application version sent to Bugsnag.
     *
     * @return App Version
     */
    public String getAppVersion() {
        return appVersion;
    }

    /**
     * Set the application version sent to Bugsnag. By default we'll pull this
     * from your AndroidManifest.xml
     *
     * @param appVersion the app version to send
     */
    public void setAppVersion(String appVersion) {
        this.appVersion = appVersion;
        notifyBugsnagObservers(NotifyType.APP);
    }

    /**
     * Gets the context to be sent to Bugsnag.
     *
     * @return Context
     */
    public String getContext() {
        return context;
    }

    /**
     * Set the context sent to Bugsnag. By default we'll attempt to detect the
     * name of the top-most activity at the time of a report, and use this
     * as the context, but sometime this is not possible.
     *
     * @param context set what was happening at the time of a crash
     */
    public void setContext(String context) {
        this.context = context;
        notifyBugsnagObservers(NotifyType.CONTEXT);
    }

    /**
     * Get the endpoint to send data
     *
     * @return Endpoint
     */
    public String getEndpoint() {
        return endpoint;
    }

    /**
     * Set the endpoint to send data to. By default we'll send reports to
     * the standard https://notify.bugsnag.com endpoint, but you can override
     * this if you are using Bugsnag Enterprise to point to your own Bugsnag
     * endpoint.
     *
     * @param endpoint the custom endpoint to send report to
     * @deprecated use {@link com.bugsnag.android.Configuration#setEndpoints(String, String)}
     */
    @Deprecated
    public void setEndpoint(String endpoint) {
        this.endpoint = endpoint;
    }

    /**
     * Set the endpoints to send data to. By default we'll send error reports to
     * https://notify.bugsnag.com, and sessions to https://sessions.bugsnag.com, but you can
     * override this if you are using Bugsnag Enterprise to point to your own Bugsnag endpoint.
     *
     * Please note that it is recommended that you set both endpoints. If the notify endpoint is
     * missing, an exception will be thrown. If the session endpoint is missing, a warning will be
     * logged and sessions will not be sent automatically.
     *
     * @param notify the notify endpoint
     * @param sessions the sessions endpoint
     *
     * @throws IllegalArgumentException if the notify endpoint is empty or null
     */
    public void setEndpoints(@NonNull String notify, @NonNull String sessions)
        throws IllegalArgumentException {

        if (TextUtils.isEmpty(notify)) {
            throw new IllegalArgumentException("Notify endpoint cannot be empty or null.");
        } else {
            this.endpoint = notify;
        }

        boolean invalidSessionsEndpoint = TextUtils.isEmpty(sessions);

        if (invalidSessionsEndpoint) {
            Logger.warn("The session tracking endpoint has not been set. "
                + "Session tracking is disabled");
            this.sessionEndpoint = null;
            this.autoCaptureSessions = false;
        } else {
            this.sessionEndpoint = sessions;
        }
    }

    /**
     * Gets the Session Tracking API endpoint
     *
     * @return the endpoint
     */
    public String getSessionEndpoint() {
        return sessionEndpoint;
    }

    /**
     * Set the endpoint to send Session Tracking data to. By default we'll send reports to
     * the standard https://sessions.bugsnag.com endpoint, but you can override
     * this if you are using Bugsnag Enterprise to point to your own Bugsnag
     * endpoint.
     *
     * @param endpoint the custom endpoint to send session data to
     * @deprecated use {@link com.bugsnag.android.Configuration#setEndpoints(String, String)}
     */
    @Deprecated
    public void setSessionEndpoint(String endpoint) {
        this.sessionEndpoint = endpoint;
    }

    /**
     * Get the buildUUID.
     *
     * @return build UUID
     */
    @SuppressWarnings("checkstyle:AbbreviationAsWordInName")
    public String getBuildUUID() {
        return buildUuid;
    }

    /**
     * Set the buildUUID to your own value. This is used to identify proguard
     * mapping files in the case that you publish multiple different apps with
     * the same appId and versionCode. The default value is read from the
     * com.bugsnag.android.BUILD_UUID meta-data field in your app manifest.
     *
     * @param buildUuid the buildUUID.
     */
    @SuppressWarnings("checkstyle:AbbreviationAsWordInName")
    public void setBuildUUID(String buildUuid) {
        this.buildUuid = buildUuid;
        notifyBugsnagObservers(NotifyType.APP);
    }

    /**
     * Get which keys should be filtered when sending metaData to Bugsnag
     *
     * @return Filters
     */
    public String[] getFilters() {
        return metaData.getFilters();
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
    public void setFilters(String[] filters) {
        this.metaData.setFilters(filters);
    }

    /**
     * Get which exception classes should be ignored (not sent) by Bugsnag.
     *
     * @return Ignore classes
     */
    public String[] getIgnoreClasses() {
        return ignoreClasses;
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
    public void setIgnoreClasses(String[] ignoreClasses) {
        this.ignoreClasses = ignoreClasses;
    }

    /**
     * Get for which releaseStages errors should be sent to Bugsnag.
     *
     * @return Notify release stages
     */
    @Nullable
    public String[] getNotifyReleaseStages() {
        return notifyReleaseStages;
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
    public void setNotifyReleaseStages(@Nullable String[] notifyReleaseStages) {
        this.notifyReleaseStages = notifyReleaseStages;
        notifyBugsnagObservers(NotifyType.RELEASE_STAGES);
    }

    /**
     * Get which packages should be considered part of your application.
     *
     * @return packages
     */
    public String[] getProjectPackages() {
        return projectPackages;
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
    public void setProjectPackages(String[] projectPackages) {
        this.projectPackages = projectPackages;
    }

    /**
     * Get the current "release stage" of your application.
     *
     * @return release stage
     */
    public String getReleaseStage() {
        return releaseStage;
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
        this.releaseStage = releaseStage;
        notifyBugsnagObservers(NotifyType.APP);
    }

    /**
     * Get whether to send thread-state with report.
     *
     * @return send threads
     */
    public boolean getSendThreads() {
        return sendThreads;
    }

    /**
     * Set whether to send thread-state with report.
     * By default, this will be true.
     *
     * @param sendThreads should we send thread-state with report?
     */
    public void setSendThreads(boolean sendThreads) {
        this.sendThreads = sendThreads;
    }

    /**
     * Get whether or not Bugsnag should automatically handle uncaught exceptions
     *
     * @return should Bugsnag automatically handle uncaught exceptions
     */
    public boolean getEnableExceptionHandler() {
        return enableExceptionHandler;
    }

    /**
     * Set whether or not Bugsnag should automatically handle uncaught exceptions
     *
     * @param enableExceptionHandler should Bugsnag automatically handle uncaught exceptions
     */
    public void setEnableExceptionHandler(boolean enableExceptionHandler) {
        this.enableExceptionHandler = enableExceptionHandler;
    }

    /**
     * Get whether or not User sessions are captured automatically.
     *
     * @return true if sessions are captured automatically
     */
    public boolean shouldAutoCaptureSessions() {
        return autoCaptureSessions;
    }

    /**
     * Sets whether or not Bugsnag should automatically capture and report User sessions whenever
     * the app enters the foreground.
     * <p>
     * By default this behavior is disabled.
     *
     * @param autoCapture whether sessions should be captured automatically
     */
    public void setAutoCaptureSessions(boolean autoCapture) {
        this.autoCaptureSessions = autoCapture;
    }

    /**
     * Gets any meta data associated with the error
     *
     * @return meta data
     */
    @NonNull
    protected MetaData getMetaData() {
        return metaData;
    }

    /**
     * Sets any meta data associated with the error
     *
     * @param metaData meta data
     */
    protected void setMetaData(@NonNull MetaData metaData) {
        this.metaData.deleteObserver(this);

        //noinspection ConstantConditions
        if (metaData == null) {
            this.metaData = new MetaData();
        } else {
            this.metaData = metaData;
        }

        this.metaData.addObserver(this);
        notifyBugsnagObservers(NotifyType.META);
    }

    /**
     * Gets any before notify tasks to run
     *
     * @return the before notify tasks
     */
    @NonNull
    protected Collection<BeforeNotify> getBeforeNotifyTasks() {
        return beforeNotifyTasks;
    }

    /**
     * Get whether or not Bugsnag should persist user information between application settings
     *
     * @return whether or not Bugsnag should persist user information
     */
    public boolean getPersistUserBetweenSessions() {
        return persistUserBetweenSessions;
    }

    /**
     * Set whether or not Bugsnag should persist user information between application settings
     * if set then any user information set will be re-used until
     *
     * @param persistUserBetweenSessions whether or not Bugsnag should persist user information
     * @see Client#clearUser() is called
     */
    public void setPersistUserBetweenSessions(boolean persistUserBetweenSessions) {
        this.persistUserBetweenSessions = persistUserBetweenSessions;
    }

    /**
     * Retrieves the threshold in ms for an uncaught error to be considered as a crash on launch.
     *
     * @return the threshold in ms
     */
    public long getLaunchCrashThresholdMs() {
        return launchCrashThresholdMs;
    }

    /**
     * Sets the threshold in ms for an uncaught error to be considered as a crash on launch.
     * If a crash is detected on launch, Bugsnag will attempt to send the report synchronously.
     * <p>
     * The app's launch time is tracked as the time at which {@link Bugsnag#init(Context)} was
     * called.
     * <p>
     * By default, this value is set at 5,000ms.
     *
     * @param launchCrashThresholdMs the threshold in ms. Any value below 0 will default to 0.
     */
    public void setLaunchCrashThresholdMs(long launchCrashThresholdMs) {
        if (launchCrashThresholdMs <= 0) {
            this.launchCrashThresholdMs = 0;
        } else {
            this.launchCrashThresholdMs = launchCrashThresholdMs;
        }
    }

    /**
     * Returns whether automatic breadcrumb capture or common application events is enabled.
     * @return true if automatic capture is enabled, otherwise false.
     */
    public boolean isAutomaticallyCollectingBreadcrumbs() {
        return automaticallyCollectBreadcrumbs;
    }

    /**
     * By default, we will automatically add breadcrumbs for common application events,
     * such as activity lifecycle events, and system intents.
     * To disable this behavior, set this property to false.
     *
     * @param automaticallyCollectBreadcrumbs whether breadcrumbs should be automatically captured
     *                                        or not
     */
    public void setAutomaticallyCollectBreadcrumbs(boolean automaticallyCollectBreadcrumbs) {
        this.automaticallyCollectBreadcrumbs = automaticallyCollectBreadcrumbs;
    }

    /**
     * Intended for internal use only - sets the type of the notifier (e.g. Android, React Native)
     * @param notifierType the notifier type
     */
    public void setNotifierType(String notifierType) {
        this.notifierType = notifierType;
    }

    /**
     * Intended for internal use only - sets the code bundle id for React Native
     * @param codeBundleId the code bundle id
     */
    public void setCodeBundleId(String codeBundleId) {
        this.codeBundleId = codeBundleId;
    }

    String getCodeBundleId() {
        return codeBundleId;
    }

    String getNotifierType() {
        return notifierType;
    }

    /**
     * Retrieves the delivery used to make HTTP requests to Bugsnag.
     *
     * @return the current delivery
     */
    @NonNull
    public Delivery getDelivery() {
        return delivery;
    }

    /**
     * Sets the delivery used to make HTTP requests to Bugsnag. A default implementation is
     * provided, but you may wish to use your own implementation if you have requirements such
     * as pinning SSL certificates, for example.
     * <p>
     * Any custom implementation must be capable of sending
     * <a href="https://docs.bugsnag.com/api/error-reporting/">Error Reports</a>
     * and <a href="https://docs.bugsnag.com/api/sessions/">Sessions</a> as
     * documented at <a href="https://docs.bugsnag.com/api/">https://docs.bugsnag.com/api/</a>
     *
     * @param delivery the custom HTTP client implementation
     */
    public void setDelivery(@NonNull Delivery delivery) {
        //noinspection ConstantConditions
        if (delivery == null) {
            throw new IllegalArgumentException("Delivery cannot be null");
        }
        this.delivery = delivery;
    }

    /**
     * Supplies the headers which must be used in any request sent to the Error Reporting API.
     *
     * @return the HTTP headers
     */
    public Map<String, String> getErrorApiHeaders() {
        Map<String, String> map = new HashMap<>();
        map.put(HEADER_API_PAYLOAD_VERSION, "4.0");
        map.put(HEADER_API_KEY, apiKey);
        map.put(HEADER_BUGSNAG_SENT_AT, DateUtils.toIso8601(new Date()));
        return map;
    }

    /**
     * Supplies the headers which must be used in any request sent to the Session Tracking API.
     *
     * @return the HTTP headers
     */
    public Map<String, String> getSessionApiHeaders() {
        Map<String, String> map = new HashMap<>();
        map.put(HEADER_API_PAYLOAD_VERSION, "1.0");
        map.put(HEADER_API_KEY, apiKey);
        map.put(HEADER_BUGSNAG_SENT_AT, DateUtils.toIso8601(new Date()));
        return map;
    }

    /**
     * Checks if the given release stage should be notified or not
     *
     * @param releaseStage the release stage to check
     * @return true if the release state should be notified else false
     */
    protected boolean shouldNotifyForReleaseStage(String releaseStage) {
        if (this.notifyReleaseStages == null) {
            return true;
        }

        List<String> stages = Arrays.asList(this.notifyReleaseStages);
        return stages.contains(releaseStage);
    }

    /**
     * Checks if the given exception class should be ignored or not
     *
     * @param className the exception class to check
     * @return true if the exception class should be ignored else false
     */
    protected boolean shouldIgnoreClass(String className) {
        if (this.ignoreClasses == null) {
            return false;
        }

        List<String> classes = Arrays.asList(this.ignoreClasses);
        return classes.contains(className);
    }

    /**
     * Adds a new before notify task
     *
     * @param beforeNotify the new before notify task
     */
    protected void beforeNotify(BeforeNotify beforeNotify) {
        if (!beforeNotifyTasks.contains(beforeNotify)) {
            beforeNotifyTasks.add(beforeNotify);
        }
    }

    /**
     * Adds a new before breadcrumb task
     *
     * @param beforeRecordBreadcrumb the new before breadcrumb task
     */
    protected void beforeRecordBreadcrumb(BeforeRecordBreadcrumb beforeRecordBreadcrumb) {
        if (!beforeRecordBreadcrumbTasks.contains(beforeRecordBreadcrumb)) {
            beforeRecordBreadcrumbTasks.add(beforeRecordBreadcrumb);
        }
    }

    /**
     * Checks if the given class name should be marked as in the project or not
     *
     * @param className the class to check
     * @return true if the class should be considered in the project else false
     */
    protected boolean inProject(@NonNull String className) {
        if (projectPackages != null) {
            for (String packageName : projectPackages) {
                if (packageName != null && className.startsWith(packageName)) {
                    return true;
                }
            }
        }

        return false;
    }

    private void notifyBugsnagObservers(@NonNull NotifyType type) {
        setChanged();
        super.notifyObservers(type.getValue());
    }

    @Override
    public void update(Observable observable, Object arg) {
        if (arg instanceof Integer) {
            NotifyType type = NotifyType.fromInt((Integer) arg);

            if (type != null) {
                notifyBugsnagObservers(type);
            }
        }
    }

    /**
     * Gets any before breadcrumb tasks to run
     *
     * @return the before breadcrumb tasks
     */
    protected Collection<BeforeRecordBreadcrumb> getBeforeRecordBreadcrumbTasks() {
        return beforeRecordBreadcrumbTasks;
    }
}
