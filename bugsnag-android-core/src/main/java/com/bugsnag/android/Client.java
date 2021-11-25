package com.bugsnag.android;

import static com.bugsnag.android.ConfigFactory.MF_BUILD_UUID;
import static com.bugsnag.android.MapUtils.getStringFromMap;

import com.bugsnag.android.NativeInterface.Message;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.os.Build;
import android.os.storage.StorageManager;
import android.text.TextUtils;
import android.view.OrientationEventListener;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import kotlin.Unit;
import kotlin.jvm.functions.Function1;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Observable;
import java.util.Observer;
import java.util.concurrent.RejectedExecutionException;

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
@SuppressWarnings("checkstyle:JavadocTagContinuationIndentation")
public class Client extends Observable implements Observer {

    private static final boolean BLOCKING = true;
    private static final String SHARED_PREF_KEY = "com.bugsnag.android";
    private static final String USER_ID_KEY = "user.id";
    private static final String USER_NAME_KEY = "user.name";
    private static final String USER_EMAIL_KEY = "user.email";

    static final String INTERNAL_DIAGNOSTICS_TAB = "BugsnagDiagnostics";

    @NonNull
    protected final Configuration config;
    final Context appContext;

    @NonNull
    protected final DeviceData deviceData;

    @NonNull
    protected final AppData appData;

    @NonNull
    final Breadcrumbs breadcrumbs;

    @NonNull
    private final User user = new User();

    @NonNull
    protected final ErrorStore errorStore;

    final SessionStore sessionStore;

    final EventReceiver eventReceiver;
    final SessionTracker sessionTracker;
    final SharedPreferences sharedPrefs;

    private final OrientationEventListener orientationListener;
    private final Connectivity connectivity;
    final StorageManager storageManager;

    @Nullable
    private Class<?> ndkPluginClz;

    @Nullable
    private Class<?> anrPluginClz;

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
    public Client(@NonNull Context androidContext,
                  @Nullable String apiKey,
                  boolean enableExceptionHandler) {
        this(androidContext,
            ConfigFactory.createNewConfiguration(androidContext, apiKey, enableExceptionHandler));
    }

    /**
     * Initialize a Bugsnag client
     *
     * @param androidContext an Android context, usually <code>this</code>
     * @param configuration  a configuration for the Client
     */
    public Client(@NonNull Context androidContext, @NonNull Configuration configuration) {
        warnIfNotAppContext(androidContext);
        appContext = androidContext.getApplicationContext();
        config = configuration;
        sessionStore = new SessionStore(config, null);
        storageManager = (StorageManager) appContext.getSystemService(Context.STORAGE_SERVICE);

        connectivity = new ConnectivityCompat(appContext, new Function1<Boolean, Unit>() {
            @Override
            public Unit invoke(Boolean connected) {
                if (connected) {
                    errorStore.flushAsync();
                }
                return null;
            }
        });

        //noinspection ConstantConditions
        if (configuration.getDelivery() == null) {
            configuration.setDelivery(new DefaultDelivery(connectivity));
        }
        if (configuration.getPersistenceDirectory() == null) {
            configuration.setPersistenceDirectory(appContext.getCacheDir());
        }

        sessionTracker =
            new SessionTracker(configuration, this, sessionStore);
        eventReceiver = new EventReceiver(this);

        // Set up and collect constant app and device diagnostics
        sharedPrefs = appContext.getSharedPreferences(SHARED_PREF_KEY, Context.MODE_PRIVATE);

        appData = new AppData(appContext, appContext.getPackageManager(), config, sessionTracker);
        Resources resources = appContext.getResources();
        deviceData = new DeviceData(connectivity, this.appContext, resources, sharedPrefs);

        // Set up breadcrumbs
        breadcrumbs = new Breadcrumbs(configuration);

        // Set sensible defaults if project packages not already set
        if (config.getProjectPackages() == null) {
            setProjectPackages(appContext.getPackageName());
        }

        String deviceId = deviceData.getId();

        if (config.getPersistUserBetweenSessions()) {
            // Check to see if a user was stored in the SharedPreferences
            user.setId(sharedPrefs.getString(USER_ID_KEY, deviceId));
            user.setName(sharedPrefs.getString(USER_NAME_KEY, null));
            user.setEmail(sharedPrefs.getString(USER_EMAIL_KEY, null));
        } else {
            user.setId(deviceId);
        }

        if (appContext instanceof Application) {
            Application application = (Application) appContext;
            application.registerActivityLifecycleCallbacks(sessionTracker);
        } else {
            Logger.warn("Bugsnag is unable to setup automatic activity lifecycle "
                + "breadcrumbs on API Levels below 14.");
        }

        // populate from manifest (in the case where the constructor was called directly by the
        // User or no UUID was supplied)
        if (config.getBuildUUID() == null) {
            String buildUuid = null;
            try {
                PackageManager packageManager = appContext.getPackageManager();
                String packageName = appContext.getPackageName();
                ApplicationInfo ai =
                    packageManager.getApplicationInfo(packageName, PackageManager.GET_META_DATA);
                buildUuid = ai.metaData.getString(MF_BUILD_UUID);
            } catch (Exception ignore) {
                Logger.warn("Bugsnag is unable to read build UUID from manifest.");
            }
            if (buildUuid != null) {
                config.setBuildUUID(buildUuid);
            }
        }

        // Create the error store that is used in the exception handler
        errorStore = new ErrorStore(config, new ErrorStore.Delegate() {
            @Override
            public void onErrorIOFailure(Exception exc, File errorFile, String context) {
                // send an internal error to bugsnag with no cache
                Thread thread = Thread.currentThread();
                Error err = new Error.Builder(config, exc, null, thread, true).build();
                err.setContext(context);

                MetaData metaData = err.getMetaData();
                metaData.addToTab(INTERNAL_DIAGNOSTICS_TAB, "canRead", errorFile.canRead());
                metaData.addToTab(INTERNAL_DIAGNOSTICS_TAB, "canWrite", errorFile.canWrite());
                metaData.addToTab(INTERNAL_DIAGNOSTICS_TAB, "exists", errorFile.exists());

                @SuppressLint("UsableSpace") // storagemanager alternative API requires API 26
                long usableSpace = appContext.getCacheDir().getUsableSpace();
                metaData.addToTab(INTERNAL_DIAGNOSTICS_TAB, "usableSpace", usableSpace);
                metaData.addToTab(INTERNAL_DIAGNOSTICS_TAB, "filename", errorFile.getName());
                metaData.addToTab(INTERNAL_DIAGNOSTICS_TAB, "fileLength", errorFile.length());
                recordStorageCacheBehavior(metaData);
                Client.this.reportInternalBugsnagError(err);
            }
        });

        // Install a default exception handler with this client
        if (config.getEnableExceptionHandler()) {
            enableExceptionHandler();
        }

        try {
            ndkPluginClz = Class.forName("com.bugsnag.android.NdkPlugin");
        } catch (ClassNotFoundException exc) {
            Logger.warn("bugsnag-plugin-android-ndk artefact not found on classpath, "
                    + "NDK errors will not be captured.");
        }

        try {
            anrPluginClz = Class.forName("com.bugsnag.android.AnrPlugin");
        } catch (ClassNotFoundException exc) {
            Logger.warn("bugsnag-plugin-android-anr artefact not found on classpath, "
                    + "ANR errors will not be captured.");
        }

        // register a receiver for automatic breadcrumbs

        try {
            Async.run(new Runnable() {
                @Override
                public void run() {
                    appContext.registerReceiver(eventReceiver, EventReceiver.getIntentFilter());
                }
            });
        } catch (RejectedExecutionException ex) {
            Logger.warn("Failed to register for automatic breadcrumb broadcasts", ex);
        }
        connectivity.registerForNetworkChanges();

        boolean isNotProduction = !AppData.RELEASE_STAGE_PRODUCTION.equals(
            appData.guessReleaseStage());
        Logger.setEnabled(isNotProduction);

        config.addObserver(this);
        breadcrumbs.addObserver(this);
        sessionTracker.addObserver(this);
        user.addObserver(this);

        final Client client = this;
        orientationListener = new OrientationEventListener(appContext) {
            @Override
            public void onOrientationChanged(int orientation) {
                client.setChanged();
                client.notifyObservers(new Message(
                    NativeInterface.MessageType.UPDATE_ORIENTATION, orientation));
            }
        };
        try {
            orientationListener.enable();
        } catch (IllegalStateException ex) {
            Logger.warn("Failed to set up orientation tracking: " + ex);
        }

        // Flush any on-disk errors
        errorStore.flushOnLaunch();
        loadPlugins();

        // react to changes in config
        ClientConfigObserver observer = new ClientConfigObserver(this, config);
        config.addObserver(observer);
        client.addObserver(observer);
    }

    void recordStorageCacheBehavior(MetaData metaData) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            File cacheDir = appContext.getCacheDir();
            File errDir = new File(cacheDir, "bugsnag-errors");

            try {
                boolean tombstone = storageManager.isCacheBehaviorTombstone(errDir);
                boolean group = storageManager.isCacheBehaviorGroup(errDir);
                metaData.addToTab(INTERNAL_DIAGNOSTICS_TAB, "cacheTombstone", tombstone);
                metaData.addToTab(INTERNAL_DIAGNOSTICS_TAB, "cacheGroup", group);
            } catch (IOException exc) {
                Logger.warn("Failed to record cache behaviour, skipping diagnostics", exc);
            }
        }
    }

    private void loadPlugins() {
        NativeInterface.setClient(this);
        enableOrDisableNdkReporting();
        enableOrDisableAnrReporting();
        BugsnagPluginInterface.INSTANCE.loadRegisteredPlugins(this);
    }

    void enableOrDisableNdkReporting() {
        if (ndkPluginClz == null) {
            return;
        }
        if (config.getDetectNdkCrashes()) {
            BugsnagPluginInterface.INSTANCE.loadPlugin(this, ndkPluginClz);
        } else {
            BugsnagPluginInterface.INSTANCE.unloadPlugin(ndkPluginClz);
        }
    }

    void enableOrDisableAnrReporting() {
        if (anrPluginClz == null) {
            return;
        }
        if (config.getDetectAnrs()) {
            BugsnagPluginInterface.INSTANCE.loadPlugin(this, anrPluginClz);
        } else {
            BugsnagPluginInterface.INSTANCE.unloadPlugin(anrPluginClz);
        }
    }

    void sendNativeSetupNotification() {
        setChanged();
        ArrayList<Object> messageArgs = new ArrayList<>();
        messageArgs.add(config);

        super.notifyObservers(new Message(NativeInterface.MessageType.INSTALL, messageArgs));
        try {
            Async.run(new Runnable() {
                @Override
                public void run() {
                    enqueuePendingNativeReports();
                }
            });
        } catch (RejectedExecutionException ex) {
            Logger.warn("Failed to enqueue native reports, will retry next launch: ", ex);
        }
    }

    void enqueuePendingNativeReports() {
        setChanged();
        notifyObservers(new Message(
            NativeInterface.MessageType.DELIVER_PENDING, null));
    }

    @Override
    public void update(@NonNull Observable observable, @NonNull Object arg) {
        if (arg instanceof Message) {
            setChanged();
            super.notifyObservers(arg);
        }
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
    public void startSession() {
        sessionTracker.startSession(false);
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
    public final void stopSession() {
        sessionTracker.stopSession();
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
    public final boolean resumeSession() {
        return sessionTracker.resumeSession();
    }

    /**
     * Starts tracking a new session only if no sessions have yet been tracked
     *
     * This is an integration point for custom libraries implementing automatic session capture
     * which differs from the default activity-based initialization.
     */
    public void startFirstSession(@NonNull Activity activity) {
        sessionTracker.startFirstSession(activity);
    }

    /**
     * Set the application version sent to Bugsnag. By default we'll pull this
     * from your AndroidManifest.xml
     *
     * @param appVersion the app version to send
     */
    public void setAppVersion(@NonNull String appVersion) {
        config.setAppVersion(appVersion);
    }

    /**
     * Gets the context to be sent to Bugsnag.
     *
     * @return Context
     */
    @Nullable public String getContext() {
        return config.getContext();
    }

    /**
     * Set the context sent to Bugsnag. By default we'll attempt to detect the
     * name of the top-most activity at the time of a report, and use this
     * as the context, but sometime this is not possible.
     *
     * @param context set what was happening at the time of a crash
     */
    public void setContext(@Nullable String context) {
        config.setContext(context);
    }

    /**
     * Set the endpoint to send data to. By default we'll send reports to
     * the standard https://notify.bugsnag.com endpoint, but you can override
     * this if you are using Bugsnag Enterprise to point to your own Bugsnag
     * endpoint.
     *
     * @param endpoint the custom endpoint to send report to
     * @deprecated use {@link com.bugsnag.android.Configuration#setEndpoints(String, String)}
     * instead.
     */
    @Deprecated
    public void setEndpoint(@NonNull String endpoint) {
        config.setEndpoint(endpoint);
    }

    /**
     * Set the buildUUID to your own value. This is used to identify proguard
     * mapping files in the case that you publish multiple different apps with
     * the same appId and versionCode. The default value is read from the
     * com.bugsnag.android.BUILD_UUID meta-data field in your app manifest.
     *
     * @param buildUuid the buildUuid.
     */
    @SuppressWarnings("checkstyle:AbbreviationAsWordInName")
    public void setBuildUUID(@Nullable final String buildUuid) {
        config.setBuildUUID(buildUuid);
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
    public void setFilters(@Nullable String... filters) {
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
    public void setIgnoreClasses(@Nullable String... ignoreClasses) {
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
    public void setNotifyReleaseStages(@Nullable String... notifyReleaseStages) {
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
     * @deprecated use {{@link Configuration#setProjectPackages(String[])}} instead
     */
    @Deprecated
    public void setProjectPackages(@Nullable String... projectPackages) {
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
    public void setReleaseStage(@Nullable String releaseStage) {
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
     * Sets whether or not Bugsnag should automatically capture and report User sessions whenever
     * the app enters the foreground.
     * <p>
     * By default this behavior is enabled.
     *
     * @param autoCapture whether sessions should be captured automatically
     */
    public void setAutoCaptureSessions(boolean autoCapture) {
        config.setAutoCaptureSessions(autoCapture);

        if (autoCapture) { // track any existing sessions
            sessionTracker.onAutoCaptureEnabled();
        }
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
    public void setUser(@Nullable String id, @Nullable String email, @Nullable String name) {
        setUserId(id);
        setUserEmail(email);
        setUserName(name);
    }

    /**
     * Retrieves details of the user currently using your application.
     * You can search for this information in your Bugsnag dashboard.
     *
     * @return the current user
     */
    @NonNull
    public User getUser() {
        return user;
    }

    @NonNull
    @InternalApi
    public Collection<Breadcrumb> getBreadcrumbs() {
        return new ArrayList<>(breadcrumbs.store);
    }

    @NonNull
    @InternalApi
    public AppData getAppData() {
        return appData;
    }

    @NonNull
    @InternalApi
    public DeviceData getDeviceData() {
        return deviceData;
    }

    /**
     * Removes the current user data and sets it back to defaults
     */
    public void clearUser() {
        user.setId(getStringFromMap("id", deviceData.getDeviceData()));
        user.setEmail(null);
        user.setName(null);

        SharedPreferences sharedPref =
            appContext.getSharedPreferences(SHARED_PREF_KEY, Context.MODE_PRIVATE);
        sharedPref.edit()
            .remove(USER_ID_KEY)
            .remove(USER_EMAIL_KEY)
            .remove(USER_NAME_KEY)
            .apply();
    }

    /**
     * Set a unique identifier for the user currently using your application.
     * By default, this will be an automatically generated unique id
     * You can search for this information in your Bugsnag dashboard.
     *
     * @param id a unique identifier of the current user
     */
    public void setUserId(@Nullable String id) {
        user.setId(id);

        if (config.getPersistUserBetweenSessions()) {
            storeInSharedPrefs(USER_ID_KEY, id);
        }
    }

    /**
     * Set the email address of the current user.
     * You can search for this information in your Bugsnag dashboard.
     *
     * @param email the email address of the current user
     */
    public void setUserEmail(@Nullable String email) {
        user.setEmail(email);

        if (config.getPersistUserBetweenSessions()) {
            storeInSharedPrefs(USER_EMAIL_KEY, email);
        }
    }

    /**
     * Set the name of the current user.
     * You can search for this information in your Bugsnag dashboard.
     *
     * @param name the name of the current user
     */
    public void setUserName(@Nullable String name) {
        user.setName(name);

        if (config.getPersistUserBetweenSessions()) {
            storeInSharedPrefs(USER_NAME_KEY, name);
        }
    }

    DeliveryCompat getAndSetDeliveryCompat() {
        Delivery current = config.getDelivery();

        if (current instanceof DeliveryCompat) {
            return (DeliveryCompat)current;
        } else {
            DeliveryCompat compat = new DeliveryCompat();
            config.setDelivery(compat);
            return compat;
        }
    }

    @SuppressWarnings("ConstantConditions")
    @Deprecated
    void setErrorReportApiClient(@NonNull ErrorReportApiClient errorReportApiClient) {
        if (errorReportApiClient == null) {
            throw new IllegalArgumentException("ErrorReportApiClient cannot be null.");
        }
        DeliveryCompat compat = getAndSetDeliveryCompat();
        compat.errorReportApiClient = errorReportApiClient;
    }

    @SuppressWarnings("ConstantConditions")
    @Deprecated
    void setSessionTrackingApiClient(@NonNull SessionTrackingApiClient apiClient) {
        if (apiClient == null) {
            throw new IllegalArgumentException("SessionTrackingApiClient cannot be null.");
        }
        DeliveryCompat compat = getAndSetDeliveryCompat();
        compat.sessionTrackingApiClient = apiClient;
    }

    /**
     * Add a "before notify" callback, to execute code before sending
     * reports to Bugsnag.
     * <p/>
     * You can use this to add or modify information attached to an error
     * before it is sent to your dashboard. You can also return
     * <code>false</code> from any callback to prevent delivery. "Before
     * notify" callbacks do not run before reports generated in the event
     * of immediate app termination from crashes in C/C++ code.
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
    public void beforeNotify(@NonNull BeforeNotify beforeNotify) {
        config.beforeNotify(beforeNotify);
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
    public void beforeRecordBreadcrumb(@NonNull BeforeRecordBreadcrumb beforeRecordBreadcrumb) {
        config.beforeRecordBreadcrumb(beforeRecordBreadcrumb);
    }

    /**
     * Notify Bugsnag of a handled exception
     *
     * @param exception the exception to send to Bugsnag
     */
    public void notify(@NonNull Throwable exception) {
        Error error = new Error.Builder(config, exception, sessionTracker,
            Thread.currentThread(), false)
            .severityReasonType(HandledState.REASON_HANDLED_EXCEPTION)
            .build();
        notify(error, !BLOCKING);
    }

    /**
     * Notify Bugsnag of a handled exception
     *
     * @param exception the exception to send to Bugsnag
     * @param callback  callback invoked on the generated error report for
     *                  additional modification
     */
    public void notify(@NonNull Throwable exception, @Nullable Callback callback) {
        Error error = new Error.Builder(config, exception, sessionTracker,
            Thread.currentThread(), false)
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
    public void notify(@NonNull String name,
                       @NonNull String message,
                       @NonNull StackTraceElement[] stacktrace,
                       @Nullable Callback callback) {
        Error error = new Error.Builder(config, name, message, stacktrace,
            sessionTracker, Thread.currentThread())
            .severityReasonType(HandledState.REASON_HANDLED_EXCEPTION)
            .build();
        notify(error, DeliveryStyle.ASYNC, callback);
    }

    /**
     * Notify Bugsnag of a handled exception
     *
     * @param exception the exception to send to Bugsnag
     * @param severity  the severity of the error, one of Severity.ERROR,
     *                  Severity.WARNING or Severity.INFO
     */
    public void notify(@NonNull Throwable exception, @NonNull Severity severity) {
        Error error = new Error.Builder(config, exception, sessionTracker,
            Thread.currentThread(), false)
            .severity(severity)
            .build();
        notify(error, !BLOCKING);
    }

    /**
     * Notify Bugsnag of a handled exception
     *
     * @param exception the exception to send to Bugsnag
     * @param metaData  additional information to send with the exception
     * @deprecated Use {@link #notify(Throwable, Callback)} to send and modify error reports
     */
    @Deprecated
    public void notify(@NonNull Throwable exception,
                       @NonNull MetaData metaData) {
        Error error = new Error.Builder(config, exception, sessionTracker,
            Thread.currentThread(), false)
            .metaData(metaData)
            .severityReasonType(HandledState.REASON_HANDLED_EXCEPTION)
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
     * @deprecated Use {@link #notify(Throwable, Callback)} to send and modify error reports
     */
    @Deprecated
    public void notify(@NonNull Throwable exception, @NonNull Severity severity,
                       @NonNull MetaData metaData) {
        Error error = new Error.Builder(config, exception, sessionTracker,
            Thread.currentThread(), false)
            .metaData(metaData)
            .severity(severity)
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
     * @deprecated Use {@link #notify(String, String, StackTraceElement[], Callback)}
     * to send and modify error reports
     */
    @Deprecated
    public void notify(@NonNull String name, @NonNull String message,
                       @NonNull StackTraceElement[] stacktrace, @NonNull Severity severity,
                       @NonNull MetaData metaData) {
        Error error = new Error.Builder(config, name, message,
            stacktrace, sessionTracker, Thread.currentThread())
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
     * @param context    the error context
     * @param stacktrace the stackframes associated with the error
     * @param severity   the severity of the error, one of Severity.ERROR,
     *                   Severity.WARNING or Severity.INFO
     * @param metaData   additional information to send with the exception
     * @deprecated Use {@link #notify(String, String, StackTraceElement[], Callback)}
     * to send and modify error reports
     */
    @Deprecated
    public void notify(@NonNull String name,
                       @NonNull String message,
                       @Nullable String context,
                       @NonNull StackTraceElement[] stacktrace,
                       @NonNull Severity severity,
                       @NonNull MetaData metaData) {
        Error error = new Error.Builder(config, name, message,
            stacktrace, sessionTracker, Thread.currentThread())
            .severity(severity)
            .metaData(metaData)
            .build();
        error.setContext(context);
        notify(error, !BLOCKING);
    }

    private void notify(@NonNull Error error, boolean blocking) {
        DeliveryStyle style = blocking ? DeliveryStyle.SAME_THREAD : DeliveryStyle.ASYNC;
        notify(error, style, null);
    }

    void notify(@NonNull Error error,
                @NonNull DeliveryStyle style,
                @Nullable Callback callback) {
        // Don't notify if this error class should be ignored
        if (error.shouldIgnoreClass()) {
            return;
        }

        // generate new object each time, as this can be mutated by end-users
        Map<String, Object> errorAppData = appData.getAppData();

        // Don't notify unless releaseStage is in notifyReleaseStages
        String releaseStage = getStringFromMap("releaseStage", errorAppData);

        if (!config.shouldNotifyForReleaseStage(releaseStage)) {
            return;
        }

        // Capture the state of the app and device and attach diagnostics to the error
        Map<String, Object> errorDeviceData = deviceData.getDeviceData();
        error.setDeviceData(errorDeviceData);
        error.getMetaData().store.put("device", deviceData.getDeviceMetaData());


        // add additional info that belongs in metadata
        error.setAppData(errorAppData);
        error.getMetaData().store.put("app", appData.getAppDataMetaData());

        // Attach breadcrumbs to the error
        error.setBreadcrumbs(breadcrumbs);

        // Attach user info to the error
        error.setUser(user);

        // Attach default context from active activity
        if (TextUtils.isEmpty(error.getContext())) {
            String context = config.getContext();
            error.setContext(context != null ? context : appData.getActiveScreenClass());
        }

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

        if (error.getSession() != null) {
            setChanged();

            if (error.getHandledState().isUnhandled()) {
                notifyObservers(new Message(
                    NativeInterface.MessageType.NOTIFY_UNHANDLED, null));
            } else {
                notifyObservers(new Message(
                    NativeInterface.MessageType.NOTIFY_HANDLED, error.getExceptionName()));
            }
        }

        switch (style) {
            case SAME_THREAD:
                deliver(report, error);
                break;
            case NO_CACHE:
                report.setCachingDisabled(true);
                deliverReportAsync(error, report);
                break;
            case ASYNC:
                deliverReportAsync(error, report);
                break;
            case ASYNC_WITH_CACHE:
                errorStore.write(error);
                errorStore.flushAsync();
                break;
            default:
                break;
        }
    }

    /**
     * Reports an error that occurred within the notifier to bugsnag. A lean error report will be
     * generated and sent asynchronously with no callbacks, retry attempts, or writing to disk.
     * This is intended for internal use only, and reports will not be visible to end-users.
     */
    void reportInternalBugsnagError(@NonNull Error error) {
        Map<String, Object> app = appData.getAppDataSummary();
        app.put("duration", AppData.getDurationMs());
        app.put("durationInForeground", appData.calculateDurationInForeground());
        app.put("inForeground", sessionTracker.isInForeground());
        error.setAppData(app);

        Map<String, Object> device = deviceData.getDeviceDataSummary();
        device.put("freeDisk", deviceData.calculateFreeDisk());
        error.setDeviceData(device);

        MetaData metaData = error.getMetaData();
        Notifier notifier = Notifier.getInstance();
        metaData.addToTab(INTERNAL_DIAGNOSTICS_TAB, "notifierName", notifier.getName());
        metaData.addToTab(INTERNAL_DIAGNOSTICS_TAB, "notifierVersion", notifier.getVersion());
        metaData.addToTab(INTERNAL_DIAGNOSTICS_TAB, "apiKey", config.getApiKey());

        Object packageName = appData.getAppData().get("packageName");
        metaData.addToTab(INTERNAL_DIAGNOSTICS_TAB, "packageName", packageName);

        final Report report = new Report(null, error);
        try {
            Async.run(new Runnable() {
                @Override
                public void run() {
                    try {
                        Delivery delivery = config.getDelivery();

                        // can only modify headers if DefaultDelivery is in use
                        if (delivery instanceof DefaultDelivery) {
                            Map<String, String> headers = config.getErrorApiHeaders();
                            headers.put("Bugsnag-Internal-Error", "true");
                            headers.remove(Configuration.HEADER_API_KEY);
                            DefaultDelivery defaultDelivery = (DefaultDelivery) delivery;
                            defaultDelivery.deliver(config.getEndpoint(), report, headers);
                        }

                    } catch (Exception exception) {
                        Logger.warn("Failed to report internal error to Bugsnag", exception);
                    }
                }
            });
        } catch (RejectedExecutionException ignored) {
            // drop internal report
        }
    }

    private void deliverReportAsync(@NonNull Error error, Report report) {
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
        } catch (RejectedExecutionException exception) {
            errorStore.write(error);
            Logger.warn("Exceeded max queue count, saving to disk to send later");
        }
    }

    private void leaveErrorBreadcrumb(@NonNull Error error) {
        // Add a breadcrumb for this error occurring
        String exceptionMessage = error.getExceptionMessage();
        Map<String, String> message = Collections.singletonMap("message", exceptionMessage);
        breadcrumbs.add(new Breadcrumb(error.getExceptionName(), BreadcrumbType.ERROR, message));
    }

    /**
     * Notify Bugsnag of a handled exception
     *
     * @param exception the exception to send to Bugsnag
     */
    public void notifyBlocking(@NonNull Throwable exception) {
        Error error = new Error.Builder(config, exception, sessionTracker,
            Thread.currentThread(), false)
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
    public void notifyBlocking(@NonNull Throwable exception, @Nullable Callback callback) {
        Error error = new Error.Builder(config, exception, sessionTracker,
            Thread.currentThread(), false)
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
    public void notifyBlocking(@NonNull String name,
                               @NonNull String message,
                               @NonNull StackTraceElement[] stacktrace,
                               @Nullable Callback callback) {
        Error error = new Error.Builder(config, name, message,
            stacktrace, sessionTracker, Thread.currentThread())
            .severityReasonType(HandledState.REASON_HANDLED_EXCEPTION)
            .build();
        notify(error, DeliveryStyle.SAME_THREAD, callback);
    }

    /**
     * Notify Bugsnag of a handled exception
     *
     * @param exception the exception to send to Bugsnag
     * @param metaData  additional information to send with the exception
     * @deprecated Use {@link #notify(Throwable, Callback)} to send and modify error reports
     */
    @Deprecated
    public void notifyBlocking(@NonNull Throwable exception,
                               @NonNull MetaData metaData) {
        Error error = new Error.Builder(config, exception, sessionTracker,
            Thread.currentThread(), false)
            .severityReasonType(HandledState.REASON_HANDLED_EXCEPTION)
            .metaData(metaData)
            .build();
        notify(error, BLOCKING);
    }

    /**
     * Notify Bugsnag of a handled exception
     *
     * @param exception the exception to send to Bugsnag
     * @param severity  the severity of the error, one of Severity.ERROR,
     *                  Severity.WARNING or Severity.INFO
     * @param metaData  additional information to send with the exception
     * @deprecated Use {@link #notifyBlocking(Throwable, Callback)} to send and modify error reports
     */
    @Deprecated
    public void notifyBlocking(@NonNull Throwable exception, @NonNull Severity severity,
                               @NonNull MetaData metaData) {
        Error error = new Error.Builder(config, exception, sessionTracker,
            Thread.currentThread(), false)
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
     * @deprecated Use {@link #notifyBlocking(String, String, StackTraceElement[], Callback)}
     * to send and modify error reports
     */
    @Deprecated
    public void notifyBlocking(@NonNull String name,
                               @NonNull String message,
                               @NonNull StackTraceElement[] stacktrace,
                               @NonNull Severity severity,
                               @NonNull MetaData metaData) {
        Error error = new Error.Builder(config, name, message,
            stacktrace, sessionTracker, Thread.currentThread())
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
     * @deprecated Use {@link #notifyBlocking(String, String, StackTraceElement[], Callback)}
     * to send and modify error reports
     */
    @Deprecated
    public void notifyBlocking(@NonNull String name,
                               @NonNull String message,
                               @Nullable String context,
                               @NonNull StackTraceElement[] stacktrace,
                               @NonNull Severity severity,
                               @NonNull MetaData metaData) {
        Error error = new Error.Builder(config, name, message,
            stacktrace, sessionTracker, Thread.currentThread())
            .severity(severity)
            .metaData(metaData)
            .build();
        error.setContext(context);
        notify(error, BLOCKING);
    }

    /**
     * Notify Bugsnag of a handled exception
     *
     * @param exception the exception to send to Bugsnag
     * @param severity  the severity of the error, one of Severity.ERROR,
     *                  Severity.WARNING or Severity.INFO
     */
    public void notifyBlocking(@NonNull Throwable exception, @NonNull Severity severity) {
        Error error = new Error.Builder(config, exception,
            sessionTracker, Thread.currentThread(), false)
            .severity(severity)
            .build();
        notify(error, BLOCKING);
    }

    /**
     * Intended for internal use only
     *
     * @param exception the exception
     * @param clientData the clientdata
     * @param blocking whether to block when notifying
     * @param callback a callback when notifying
     */
    public void internalClientNotify(@NonNull Throwable exception,
                              @NonNull Map<String, Object> clientData,
                              boolean blocking,
                              @Nullable Callback callback) {
        String severity = getKeyFromClientData(clientData, "severity", true);
        String severityReason =
            getKeyFromClientData(clientData, "severityReason", true);
        String logLevel = getKeyFromClientData(clientData, "logLevel", false);

        String msg = String.format("Internal client notify, severity = '%s',"
            + " severityReason = '%s'", severity, severityReason);
        Logger.info(msg);

        @SuppressWarnings("WrongConstant")
        Error error = new Error.Builder(config, exception,
            sessionTracker, Thread.currentThread(), false)
            .severity(Severity.fromString(severity))
            .severityReasonType(severityReason)
            .attributeValue(logLevel)
            .build();

        DeliveryStyle deliveryStyle = blocking ? DeliveryStyle.SAME_THREAD : DeliveryStyle.ASYNC;
        notify(error, deliveryStyle, callback);
    }

    @NonNull
    private String getKeyFromClientData(Map<String, Object> clientData,
                                        String key,
                                        boolean required) {
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
    public void addToTab(@NonNull String tab, @NonNull String key, @Nullable Object value) {
        config.getMetaData().addToTab(tab, key, value);
    }

    /**
     * Remove a tab of app-wide diagnostic information
     *
     * @param tabName the dashboard tab to remove diagnostic data from
     */
    public void clearTab(@NonNull String tabName) {
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
        Breadcrumb crumb = new Breadcrumb(breadcrumb);

        if (runBeforeBreadcrumbTasks(crumb)) {
            breadcrumbs.add(crumb);
        }
    }

    /**
     * Leave a "breadcrumb" log message, representing an action which occurred
     * in your app, to aid with debugging.
     */
    public void leaveBreadcrumb(@NonNull String name,
                                @NonNull BreadcrumbType type,
                                @NonNull Map<String, String> metadata) {
        Breadcrumb crumb = new Breadcrumb(name, type, metadata);

        if (runBeforeBreadcrumbTasks(crumb)) {
            breadcrumbs.add(crumb);
        }
    }

    /**
     * Set the maximum number of breadcrumbs to keep and sent to Bugsnag.
     * By default, we'll keep and send the 20 most recent breadcrumb log
     * messages.
     *
     * @param numBreadcrumbs number of breadcrumb log messages to send
     * @deprecated use {@link Configuration#setMaxBreadcrumbs(int)} instead
     */
    @Deprecated
    public void setMaxBreadcrumbs(int numBreadcrumbs) {
        config.setMaxBreadcrumbs(numBreadcrumbs);
    }

    /**
     * Clear any breadcrumbs that have been left so far.
     */
    public void clearBreadcrumbs() {
        breadcrumbs.clear();
    }

    /**
     * Enable automatic reporting of ANRs.
     */
    void enableAnrReporting() {
        getConfig().setDetectAnrs(true);
        enableOrDisableAnrReporting();
    }

    /**
     * Disable automatic reporting of ANRs.
     */
    void disableAnrReporting() {
        getConfig().setDetectAnrs(false);
        enableOrDisableAnrReporting();
    }

    /**
     * Enable automatic reporting of C/C++ crashes.
     */
    void enableNdkCrashReporting() {
        getConfig().setDetectNdkCrashes(true);
        enableOrDisableNdkReporting();
    }

    /**
     * Disable automatic reporting of C/C++ crashes.
     */
    void disableNdkCrashReporting() {
        getConfig().setDetectNdkCrashes(false);
        enableOrDisableNdkReporting();
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

    void deliver(@NonNull Report report, @NonNull Error error) {
        if (!runBeforeSendTasks(report)) {
            Logger.info("Skipping notification - beforeSend task returned false");
            return;
        }
        try {
            config.getDelivery().deliver(report, config);
            Logger.info("Sent 1 new error to Bugsnag");
            leaveErrorBreadcrumb(error);
        } catch (DeliveryFailureException exception) {
            if (!report.isCachingDisabled()) {
                Logger.warn("Could not send error(s) to Bugsnag,"
                    + " saving to disk to send later", exception);
                errorStore.write(error);
                leaveErrorBreadcrumb(error);
            }
        } catch (Exception exception) {
            Logger.warn("Problem sending error to Bugsnag", exception);
        }
    }

    /**
     * Caches an error then attempts to notify.
     *
     * Should only ever be called from the {@link ExceptionHandler}.
     */
    void cacheAndNotify(@NonNull Throwable exception, Severity severity, MetaData metaData,
                        @HandledState.SeverityReason String severityReason,
                        @Nullable String attributeValue, Thread thread) {
        Error error = new Error.Builder(config, exception,
            sessionTracker, thread, true)
            .severity(severity)
            .metaData(metaData)
            .severityReasonType(severityReason)
            .attributeValue(attributeValue)
            .build();

        notify(error, DeliveryStyle.ASYNC_WITH_CACHE, null);
    }

    private boolean runBeforeSendTasks(Report report) {
        for (BeforeSend beforeSend : config.getBeforeSendTasks()) {
            try {
                if (!beforeSend.run(report)) {
                    return false;
                }
            } catch (Throwable ex) {
                Logger.warn("BeforeSend threw an Exception", ex);
            }
        }

        // By default, allow the error to be sent if there were no objections
        return true;
    }

    OrientationEventListener getOrientationListener() {
        return orientationListener; // this only exists for tests
    }

    SessionTracker getSessionTracker() {
        return sessionTracker;
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

    private boolean runBeforeBreadcrumbTasks(@NonNull Breadcrumb breadcrumb) {
        Collection<BeforeRecordBreadcrumb> tasks = config.getBeforeRecordBreadcrumbTasks();
        for (BeforeRecordBreadcrumb beforeRecordBreadcrumb : tasks) {
            try {
                if (!beforeRecordBreadcrumb.shouldRecord(breadcrumb)) {
                    return false;
                }
            } catch (Throwable ex) {
                Logger.warn("BeforeRecordBreadcrumb threw an Exception", ex);
            }
        }
        return true;
    }


    /**
     * Stores the given key value pair into shared preferences
     *
     * @param key   The key to store
     * @param value The value to store
     */
    private void storeInSharedPrefs(String key, String value) {
        SharedPreferences sharedPref =
            appContext.getSharedPreferences(SHARED_PREF_KEY, Context.MODE_PRIVATE);
        sharedPref.edit().putString(key, value).apply();
    }

    ErrorStore getErrorStore() {
        return errorStore;
    }

    /**
     * Finalize by removing the receiver
     *
     * @throws Throwable if something goes wrong
     */
    @SuppressWarnings("checkstyle:NoFinalizer")
    protected void finalize() throws Throwable {
        if (eventReceiver != null) {
            try {
                appContext.unregisterReceiver(eventReceiver);
            } catch (IllegalArgumentException exception) {
                Logger.warn("Receiver not registered");
            }
        }

        super.finalize();
    }

    private static void warnIfNotAppContext(Context androidContext) {
        if (!(androidContext instanceof Application)) {
            Logger.warn("Warning - Non-Application context detected! Please ensure that you are "
                + "initializing Bugsnag from a custom Application class.");
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

    /**
     * Returns the configuration used to initialise the client
     * @return the config
     */
    @NonNull
    public Configuration getConfig() {
        return config;
    }

    /**
     * Retrieves the time at which the client was launched
     *
     * @return the ms since the java epoch
     */
    public long getLaunchTimeMs() {
        return AppData.getDurationMs();
    }

    void setBinaryArch(String binaryArch) {
        getAppData().setBinaryArch(binaryArch);
    }

    void close() {
        orientationListener.disable();
        connectivity.unregisterForNetworkChanges();
    }
}
