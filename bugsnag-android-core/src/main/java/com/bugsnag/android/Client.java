package com.bugsnag.android;

import static com.bugsnag.android.ManifestConfigLoader.BUILD_UUID;
import static com.bugsnag.android.MapUtils.getStringFromMap;

import com.bugsnag.android.NativeInterface.Message;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
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

    static final String INTERNAL_DIAGNOSTICS_TAB = "BugsnagDiagnostics";

    final Configuration clientState;
    final ImmutableConfig immutableConfig;

    final Context appContext;

    @NonNull
    protected final DeviceData deviceData;

    @NonNull
    protected final AppData appData;

    @NonNull
    final Breadcrumbs breadcrumbs;

    @NonNull
    private User user;

    @NonNull
    protected final ErrorStore errorStore;

    final SessionStore sessionStore;

    final EventReceiver eventReceiver;
    final SessionTracker sessionTracker;
    final SharedPreferences sharedPrefs;

    private final OrientationEventListener orientationListener;
    private final Connectivity connectivity;
    private UserRepository userRepository;
    final StorageManager storageManager;

    /**
     * Initialize a Bugsnag client
     *
     * @param androidContext an Android context, usually <code>this</code>
     */
    public Client(@NonNull Context androidContext) {
        this(androidContext, new ManifestConfigLoader().load(androidContext));
    }

    /**
     * Initialize a Bugsnag client
     *
     * @param androidContext an Android context, usually <code>this</code>
     * @param apiKey         your Bugsnag API key from your Bugsnag dashboard
     */
    public Client(@NonNull Context androidContext, @NonNull String apiKey) {
        this(androidContext, new Configuration(apiKey));
    }

    /**
     * Initialize a Bugsnag client
     *
     * @param androidContext an Android context, usually <code>this</code>
     * @param configuration  a configuration for the Client
     */
    public Client(@NonNull Context androidContext, @NonNull final Configuration configuration) {
        warnIfNotAppContext(androidContext);
        appContext = androidContext.getApplicationContext();
        sessionStore = new SessionStore(appContext, null);
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

        // set sensible defaults for delivery/project packages etc if not set
        sanitiseConfiguration(configuration);
        clientState = configuration;
        immutableConfig = ImmutableConfigKt.convertToImmutableConfig(configuration);

        sessionTracker = new SessionTracker(immutableConfig, clientState, this, sessionStore);
        eventReceiver = new EventReceiver(this);

        // Set up and collect constant app and device diagnostics
        sharedPrefs = appContext.getSharedPreferences(SHARED_PREF_KEY, Context.MODE_PRIVATE);

        appData = new AppData(appContext, appContext.getPackageManager(),
                immutableConfig, sessionTracker);
        Resources resources = appContext.getResources();

        userRepository = new UserRepository(sharedPrefs,
                immutableConfig.getPersistUserBetweenSessions());
        setUserInternal(userRepository.load());

        deviceData = new DeviceData(connectivity, appContext, resources, user.installId);

        // Set up breadcrumbs
        breadcrumbs = new Breadcrumbs(immutableConfig.getMaxBreadcrumbs());

        if (appContext instanceof Application) {
            Application application = (Application) appContext;
            application.registerActivityLifecycleCallbacks(sessionTracker);
        } else {
            Logger.warn("Bugsnag is unable to setup automatic activity lifecycle "
                + "breadcrumbs on API Levels below 14.");
        }

        // Create the error store that is used in the exception handler
        FileStore.Delegate delegate = new ErrorStore.Delegate() {
            @Override
            public void onErrorIOFailure(Exception exc, File errorFile, String context) {
                // send an internal error to bugsnag with no cache
                Thread thread = Thread.currentThread();
                Error err = new Error.Builder(immutableConfig, exc, null, thread,
                        true, new MetaData()).build();
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
        };
        errorStore = new ErrorStore(immutableConfig, clientState, appContext, delegate);

        // Install a default exception handler with this client
        if (immutableConfig.getAutoDetectErrors()) {
            new ExceptionHandler(this);
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

        Logger.setEnabled(immutableConfig.getLoggingEnabled());

        configuration.addObserver(this);
        breadcrumbs.addObserver(this);
        sessionTracker.addObserver(this);

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

    @SuppressWarnings("deprecation")
    private void sanitiseConfiguration(@NonNull Configuration configuration) {
        if (configuration.getDelivery() == null) {
            configuration.setDelivery(new DefaultDelivery(connectivity));
        }

        String packageName = appContext.getPackageName();

        if (configuration.getVersionCode() == null || configuration.getVersionCode() == 0) {
            try {
                PackageManager packageManager = appContext.getPackageManager();
                PackageInfo packageInfo = packageManager.getPackageInfo(packageName, 0);
                configuration.setVersionCode(packageInfo.versionCode);
            } catch (Exception ignore) {
                Logger.warn("Bugsnag is unable to read version code from manifest.");
            }
        }

        // Set sensible defaults if project packages not already set
        if (configuration.getProjectPackages().isEmpty()) {
            configuration.setProjectPackages(Collections.singleton(packageName));
        }

        // populate from manifest (in the case where the constructor was called directly by the
        // User or no UUID was supplied)
        if (configuration.getBuildUuid() == null) {
            String buildUuid = null;
            try {
                PackageManager packageManager = appContext.getPackageManager();
                ApplicationInfo ai = packageManager.getApplicationInfo(
                        packageName, PackageManager.GET_META_DATA);
                buildUuid = ai.metaData.getString(BUILD_UUID);
            } catch (Exception ignore) {
                Logger.warn("Bugsnag is unable to read build UUID from manifest.");
            }
            if (buildUuid != null) {
                configuration.setBuildUuid(buildUuid);
            }
        }
    }

    private void loadPlugins() {
        NativeInterface.setClient(this);
        BugsnagPluginInterface pluginInterface = BugsnagPluginInterface.INSTANCE;

        if (immutableConfig.getAutoDetectNdkCrashes()) {
            try {
                pluginInterface.registerPlugin(Class.forName("com.bugsnag.android.NdkPlugin"));
            } catch (ClassNotFoundException exc) {
                Logger.warn("bugsnag-plugin-android-ndk artefact not found on classpath, "
                    + "NDK errors will not be captured.");
            }
        }
        if (immutableConfig.getAutoDetectAnrs()) {
            try {
                pluginInterface.registerPlugin(Class.forName("com.bugsnag.android.AnrPlugin"));
            } catch (ClassNotFoundException exc) {
                Logger.warn("bugsnag-plugin-android-anr artefact not found on classpath, "
                    + "ANR errors will not be captured.");
            }
        }
        pluginInterface.loadPlugins(this);
    }

    void sendNativeSetupNotification() {
        setChanged();
        ArrayList<Object> messageArgs = new ArrayList<>();
        messageArgs.add(clientState);

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
     * {@link #setAutoTrackSessions(boolean)} if you call this method.
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
     * @see Configuration#setAutoTrackSessions(boolean)
     */
    public void startSession() {
        sessionTracker.startSession(false);
    }

    /**
     * Stops tracking a session. You should disable automatic session tracking via
     * {@link #setAutoTrackSessions(boolean)} if you call this method.
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
     * @see Configuration#setAutoTrackSessions(boolean)
     */
    public final void stopSession() {
        sessionTracker.stopSession();
    }

    /**
     * Resumes a session which has previously been stopped, or starts a new session if none exists.
     * If a session has already been resumed or started and has not been stopped, calling this
     * method will have no effect. You should disable automatic session tracking via
     * {@link #setAutoTrackSessions(boolean)} if you call this method.
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
     * @see Configuration#setAutoTrackSessions(boolean)
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
     * Gets the context to be sent to Bugsnag.
     *
     * @return Context
     */
    @Nullable public String getContext() {
        return clientState.getContext();
    }

    /**
     * Set the context sent to Bugsnag. By default we'll attempt to detect the
     * name of the top-most activity at the time of a report, and use this
     * as the context, but sometime this is not possible.
     *
     * @param context set what was happening at the time of a crash
     */
    public void setContext(@Nullable String context) {
        clientState.setContext(context);
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

    private void setUserInternal(User user) {
        this.user = user;
        user.addObserver(this);
    }

    /**
     * Removes the current user data and sets it back to defaults
     */
    public void clearUser() {
        user.setId(getStringFromMap("id", deviceData.getDeviceData()));
        user.setEmail(null);
        user.setName(null);
        userRepository.save(user);
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
        userRepository.save(user);
    }

    /**
     * Set the email address of the current user.
     * You can search for this information in your Bugsnag dashboard.
     *
     * @param email the email address of the current user
     */
    public void setUserEmail(@Nullable String email) {
        user.setEmail(email);
        userRepository.save(user);
    }

    /**
     * Set the name of the current user.
     * You can search for this information in your Bugsnag dashboard.
     *
     * @param name the name of the current user
     */
    public void setUserName(@Nullable String name) {
        user.setName(name);
        userRepository.save(user);
    }

    /**
     * Add a "before notify" callback, to execute code at the point where an error report is
     * captured in Bugsnag.
     * <p>
     * You can use this to add or modify information attached to an error
     * before it is sent to your dashboard. You can also return
     * <code>false</code> from any callback to prevent delivery. "Before
     * notify" callbacks do not run before reports generated in the event
     * of immediate app termination from crashes in C/C++ code.
     * <p>
     * For example:
     * <p>
     * Bugsnag.addBeforeNotify(new BeforeNotify() {
     * public boolean run(Error error) {
     * error.setSeverity(Severity.INFO);
     * return true;
     * }
     * })
     *
     * @param beforeNotify a callback to run before sending errors to Bugsnag
     * @see BeforeNotify
     */
    public void addBeforeNotify(@NonNull BeforeNotify beforeNotify) {
        clientState.addBeforeNotify(beforeNotify);
    }

    /**
     * Add a "before send" callback, to execute code before sending a
     * report to Bugsnag.
     * <p>
     * You can use this to add or modify information attached to an error
     * before it is sent to your dashboard. You can also return
     * <code>false</code> from any callback to prevent delivery.
     * <p>
     * For example:
     * <p>
     * Bugsnag.addBeforeSend(new BeforeSend() {
     * public boolean run(Error error) {
     * error.setSeverity(Severity.INFO);
     * return true;
     * }
     * })
     *
     * @param beforeSend a callback to run before sending errors to Bugsnag
     * @see BeforeSend
     */
    public void addBeforeSend(@NonNull BeforeSend beforeSend) {
        clientState.addBeforeSend(beforeSend);
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
        clientState.beforeRecordBreadcrumb(beforeRecordBreadcrumb);
    }

    /**
     * Notify Bugsnag of a handled exception
     *
     * @param exception the exception to send to Bugsnag
     */
    public void notify(@NonNull Throwable exception) {
        Error error = new Error.Builder(immutableConfig, exception, sessionTracker,
            Thread.currentThread(), false, clientState.getMetaData())
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
        Error error = new Error.Builder(immutableConfig, exception, sessionTracker,
            Thread.currentThread(), false, clientState.getMetaData())
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
        Error error = new Error.Builder(immutableConfig, name, message, stacktrace,
            sessionTracker, Thread.currentThread(), clientState.getMetaData())
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
        Error error = new Error.Builder(immutableConfig, exception, sessionTracker,
            Thread.currentThread(), false, clientState.getMetaData())
            .severity(severity)
            .build();
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

        if (!immutableConfig.shouldNotifyForReleaseStage()) {
            return;
        }

        // Capture the state of the app and device and attach diagnostics to the error
        Map<String, Object> errorDeviceData = deviceData.getDeviceData();
        error.setDeviceData(errorDeviceData);
        error.getMetaData().store.put("device", deviceData.getDeviceMetaData());


        // add additional info that belongs in metadata
        // generate new object each time, as this can be mutated by end-users
        Map<String, Object> errorAppData = appData.getAppData();
        error.setAppData(errorAppData);
        error.getMetaData().store.put("app", appData.getAppDataMetaData());

        // Attach breadcrumbs to the error
        error.setBreadcrumbs(breadcrumbs);

        // Attach user info to the error
        error.setUser(user);

        // Attach default context from active activity
        if (TextUtils.isEmpty(error.getContext())) {
            String context = clientState.getContext();
            error.setContext(context != null ? context : appData.getActiveScreenClass());
        }

        // Run beforeNotify tasks, don't notify if any return true
        if (!runBeforeNotifyTasks(error)) {
            Logger.info("Skipping notification - beforeNotify task returned false");
            return;
        }

        // Build the report
        Report report = new Report(immutableConfig.getApiKey(), error);

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
        metaData.addToTab(INTERNAL_DIAGNOSTICS_TAB, "apiKey", immutableConfig.getApiKey());

        Object packageName = appData.getAppData().get("packageName");
        metaData.addToTab(INTERNAL_DIAGNOSTICS_TAB, "packageName", packageName);

        final Report report = new Report(null, error);
        try {
            Async.run(new Runnable() {
                @Override
                public void run() {
                    try {
                        Delivery delivery = immutableConfig.getDelivery();
                        DeliveryParams params = immutableConfig.errorApiDeliveryParams();

                        // can only modify headers if DefaultDelivery is in use
                        if (delivery instanceof DefaultDelivery) {
                            Map<String, String> headers = params.getHeaders();
                            headers.put("Bugsnag-Internal-Error", "true");
                            headers.remove(Configuration.HEADER_API_KEY);
                            DefaultDelivery defaultDelivery = (DefaultDelivery) delivery;
                            defaultDelivery.deliver(params.getEndpoint(), report, headers);
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
        Error error = new Error.Builder(immutableConfig, exception, sessionTracker,
            Thread.currentThread(), false, clientState.getMetaData())
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
        Error error = new Error.Builder(immutableConfig, exception, sessionTracker,
            Thread.currentThread(), false, clientState.getMetaData())
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
        Error error = new Error.Builder(immutableConfig, name, message,
            stacktrace, sessionTracker, Thread.currentThread(), clientState.getMetaData())
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
    public void notifyBlocking(@NonNull Throwable exception, @NonNull Severity severity) {
        Error error = new Error.Builder(immutableConfig, exception,
            sessionTracker, Thread.currentThread(), false, clientState.getMetaData())
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
        Error error = new Error.Builder(immutableConfig, exception,
            sessionTracker, Thread.currentThread(), false, clientState.getMetaData())
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
        clientState.getMetaData().addToTab(tab, key, value);
    }

    /**
     * Remove a tab of app-wide diagnostic information
     *
     * @param tabName the dashboard tab to remove diagnostic data from
     */
    public void clearTab(@NonNull String tabName) {
        clientState.getMetaData().clearTab(tabName);
    }

    /**
     * Get the global diagnostic information currently stored in MetaData.
     *
     * @see MetaData
     */
    @NonNull public MetaData getMetaData() {
        return clientState.getMetaData();
    }

    /**
     * Set the global diagnostic information to be send with every error.
     *
     * @see MetaData
     */
    public void setMetaData(@NonNull MetaData metaData) {
        clientState.setMetaData(metaData);
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
     * Clear any breadcrumbs that have been left so far.
     */
    public void clearBreadcrumbs() {
        breadcrumbs.clear();
    }

    void deliver(@NonNull Report report, @NonNull Error error) {
        if (!runBeforeSendTasks(report)) {
            Logger.info("Skipping notification - beforeSend task returned false");
            return;
        }

        DeliveryParams deliveryParams = immutableConfig.errorApiDeliveryParams();
        Delivery delivery = immutableConfig.getDelivery();
        DeliveryStatus deliveryStatus = delivery.deliver(report, deliveryParams);

        switch (deliveryStatus) {
            case DELIVERED:
                Logger.info("Sent 1 new error to Bugsnag");
                leaveErrorBreadcrumb(error);
                break;
            case UNDELIVERED:
                if (!report.isCachingDisabled()) {
                    Logger.warn("Could not send error(s) to Bugsnag,"
                            + " saving to disk to send later");
                    errorStore.write(error);
                    leaveErrorBreadcrumb(error);
                }
                break;
            case FAILURE:
                Logger.warn("Problem sending error to Bugsnag");
                break;
            default:
                break;
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
        Error error = new Error.Builder(immutableConfig, exception,
            sessionTracker, thread, true, clientState.getMetaData())
            .severity(severity)
            .metaData(metaData)
            .severityReasonType(severityReason)
            .attributeValue(attributeValue)
            .build();

        notify(error, DeliveryStyle.ASYNC_WITH_CACHE, null);
    }

    private boolean runBeforeSendTasks(Report report) {
        for (BeforeSend beforeSend : clientState.getBeforeSendTasks()) {
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
        for (BeforeNotify beforeNotify : clientState.getBeforeNotifyTasks()) {
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
        Collection<BeforeRecordBreadcrumb> tasks = clientState.getBeforeRecordBreadcrumbTasks();
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
     * Returns the configuration used to initialise the client
     * @return the config
     */
    @NonNull
    public BugsnagConfiguration getConfiguration() {
        return clientState;
    }

    ImmutableConfig getConfig() {
        return immutableConfig;
    }

    void setBinaryArch(String binaryArch) {
        getAppData().setBinaryArch(binaryArch);
    }

    void close() {
        orientationListener.disable();
        connectivity.unregisterForNetworkChanges();
    }
}
