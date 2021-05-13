package com.bugsnag.android;

import static com.bugsnag.android.ContextExtensionsKt.getActivityManagerFrom;
import static com.bugsnag.android.ContextExtensionsKt.getStorageManagerFrom;
import static com.bugsnag.android.ImmutableConfigKt.sanitiseConfiguration;
import static com.bugsnag.android.SeverityReason.REASON_HANDLED_EXCEPTION;

import android.app.ActivityManager;
import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.os.Environment;
import android.os.storage.StorageManager;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;

import kotlin.Unit;
import kotlin.jvm.functions.Function2;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Observer;
import java.util.Set;
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
@SuppressWarnings({"checkstyle:JavadocTagContinuationIndentation", "ConstantConditions"})
public class Client implements MetadataAware, CallbackAware, UserAware {

    final ImmutableConfig immutableConfig;

    final MetadataState metadataState;

    private final ContextState contextState;
    private final CallbackState callbackState;
    private final UserState userState;

    final Context appContext;

    @NonNull
    final DeviceDataCollector deviceDataCollector;

    @NonNull
    final AppDataCollector appDataCollector;

    @NonNull
    final BreadcrumbState breadcrumbState;

    @NonNull
    protected final EventStore eventStore;

    private final SessionStore sessionStore;

    final SessionTracker sessionTracker;

    private final SystemBroadcastReceiver systemBroadcastReceiver;
    private final ActivityBreadcrumbCollector activityBreadcrumbCollector;
    private final SessionLifecycleCallback sessionLifecycleCallback;

    private final Connectivity connectivity;

    @Nullable
    private final StorageManager storageManager;

    final Logger logger;
    final DeliveryDelegate deliveryDelegate;

    final ClientObservable clientObservable = new ClientObservable();
    private PluginClient pluginClient;

    final Notifier notifier = new Notifier();

    @Nullable
    final LastRunInfo lastRunInfo;
    final LastRunInfoStore lastRunInfoStore;
    final LaunchCrashTracker launchCrashTracker;
    final BackgroundTaskService bgTaskService = new BackgroundTaskService();

    /**
     * Initialize a Bugsnag client
     *
     * @param androidContext an Android context, usually <code>this</code>
     */
    public Client(@NonNull Context androidContext) {
        this(androidContext, Configuration.load(androidContext));
    }

    /**
     * Initialize a Bugsnag client
     *
     * @param androidContext an Android context, usually <code>this</code>
     * @param apiKey         your Bugsnag API key from your Bugsnag dashboard
     */
    public Client(@NonNull Context androidContext, @NonNull String apiKey) {
        this(androidContext, Configuration.load(androidContext, apiKey));
    }

    /**
     * Initialize a Bugsnag client
     *
     * @param androidContext an Android context, usually <code>this</code>
     * @param configuration  a configuration for the Client
     */
    public Client(@NonNull Context androidContext, @NonNull final Configuration configuration) {
        Context ctx = androidContext.getApplicationContext();
        appContext = ctx != null ? ctx : androidContext;

        connectivity = new ConnectivityCompat(appContext, new Function2<Boolean, String, Unit>() {
            @Override
            public Unit invoke(Boolean hasConnection, String networkState) {
                Map<String, Object> data = new HashMap<>();
                data.put("hasConnection", hasConnection);
                data.put("networkState", networkState);
                leaveAutoBreadcrumb("Connectivity changed", BreadcrumbType.STATE, data);
                if (hasConnection) {
                    eventStore.flushAsync();
                    sessionTracker.flushAsync();
                }
                return null;
            }
        });

        // set sensible defaults for delivery/project packages etc if not set
        immutableConfig = sanitiseConfiguration(appContext, configuration, connectivity);
        logger = immutableConfig.getLogger();
        warnIfNotAppContext(androidContext);

        // Set up breadcrumbs
        callbackState = configuration.impl.callbackState.copy();
        int maxBreadcrumbs = immutableConfig.getMaxBreadcrumbs();
        breadcrumbState = new BreadcrumbState(maxBreadcrumbs, callbackState, logger);

        storageManager = getStorageManagerFrom(appContext);

        contextState = new ContextState();
        contextState.setContext(configuration.getContext());

        sessionStore = new SessionStore(immutableConfig, logger, null);
        sessionTracker = new SessionTracker(immutableConfig, callbackState, this,
                sessionStore, logger, bgTaskService);
        metadataState = copyMetadataState(configuration);

        ActivityManager am = getActivityManagerFrom(appContext);

        launchCrashTracker = new LaunchCrashTracker(immutableConfig);
        appDataCollector = new AppDataCollector(appContext, appContext.getPackageManager(),
                immutableConfig, sessionTracker, am, launchCrashTracker, logger);

        // load the device + user information
        SharedPrefMigrator sharedPrefMigrator = new SharedPrefMigrator(appContext);
        DeviceIdStore deviceIdStore = new DeviceIdStore(appContext, sharedPrefMigrator, logger);
        String deviceId = deviceIdStore.loadDeviceId();
        UserStore userStore = new UserStore(immutableConfig, deviceId, sharedPrefMigrator, logger);
        userState = userStore.load(configuration.getUser());
        sharedPrefMigrator.deleteLegacyPrefs();

        DeviceBuildInfo info = DeviceBuildInfo.Companion.defaultInfo();
        Resources resources = appContext.getResources();
        deviceDataCollector = new DeviceDataCollector(connectivity, appContext,
                resources, deviceId, info, Environment.getDataDirectory(),
                new RootDetector(logger), bgTaskService, logger);

        if (appContext instanceof Application) {
            Application application = (Application) appContext;
            sessionLifecycleCallback = new SessionLifecycleCallback(sessionTracker);
            application.registerActivityLifecycleCallbacks(sessionLifecycleCallback);

            if (immutableConfig.shouldRecordBreadcrumbType(BreadcrumbType.STATE)) {
                this.activityBreadcrumbCollector = new ActivityBreadcrumbCollector(
                    new Function2<String, Map<String, ? extends Object>, Unit>() {
                        @SuppressWarnings("unchecked")
                        @Override
                        public Unit invoke(String activity, Map<String, ?> metadata) {
                            leaveBreadcrumb(activity, (Map<String, Object>) metadata,
                                    BreadcrumbType.STATE);
                            return null;
                        }
                    }
                );
                application.registerActivityLifecycleCallbacks(activityBreadcrumbCollector);
            } else {
                this.activityBreadcrumbCollector = null;
            }
        } else {
            this.activityBreadcrumbCollector = null;
            this.sessionLifecycleCallback = null;
        }

        InternalReportDelegate delegate = new InternalReportDelegate(appContext, logger,
                immutableConfig, storageManager, appDataCollector, deviceDataCollector,
                sessionTracker, notifier, bgTaskService);
        eventStore = new EventStore(immutableConfig, logger, notifier, bgTaskService, delegate);

        deliveryDelegate = new DeliveryDelegate(logger, eventStore,
                immutableConfig, breadcrumbState, notifier, bgTaskService);

        // Install a default exception handler with this client
        if (immutableConfig.getEnabledErrorTypes().getUnhandledExceptions()) {
            new ExceptionHandler(this, logger);
        }

        // register a receiver for automatic breadcrumbs
        systemBroadcastReceiver = SystemBroadcastReceiver.register(this, logger, bgTaskService);

        registerOrientationChangeListener();

        // load last run info
        lastRunInfoStore = new LastRunInfoStore(immutableConfig);
        lastRunInfo = loadLastRunInfo();

        // initialise plugins before attempting to flush any errors
        loadPlugins(configuration);

        connectivity.registerForNetworkChanges();

        // Flush any on-disk errors and sessions
        eventStore.flushOnLaunch();
        eventStore.flushAsync();
        sessionTracker.flushAsync();

        // leave auto breadcrumb
        Map<String, Object> data = Collections.emptyMap();
        leaveAutoBreadcrumb("Bugsnag loaded", BreadcrumbType.STATE, data);
        logger.d("Bugsnag loaded");
    }

    @VisibleForTesting
    Client(
            ImmutableConfig immutableConfig,
            MetadataState metadataState,
            ContextState contextState,
            CallbackState callbackState,
            UserState userState,
            Context appContext,
            @NonNull DeviceDataCollector deviceDataCollector,
            @NonNull AppDataCollector appDataCollector,
            @NonNull BreadcrumbState breadcrumbState,
            @NonNull EventStore eventStore,
            SessionStore sessionStore,
            SystemBroadcastReceiver systemBroadcastReceiver,
            SessionTracker sessionTracker,
            ActivityBreadcrumbCollector activityBreadcrumbCollector,
            SessionLifecycleCallback sessionLifecycleCallback,
            Connectivity connectivity,
            @Nullable StorageManager storageManager,
            Logger logger,
            DeliveryDelegate deliveryDelegate,
            LastRunInfoStore lastRunInfoStore,
            LaunchCrashTracker launchCrashTracker
    ) {
        this.immutableConfig = immutableConfig;
        this.metadataState = metadataState;
        this.contextState = contextState;
        this.callbackState = callbackState;
        this.userState = userState;
        this.appContext = appContext;
        this.deviceDataCollector = deviceDataCollector;
        this.appDataCollector = appDataCollector;
        this.breadcrumbState = breadcrumbState;
        this.eventStore = eventStore;
        this.sessionStore = sessionStore;
        this.systemBroadcastReceiver = systemBroadcastReceiver;
        this.sessionTracker = sessionTracker;
        this.activityBreadcrumbCollector = activityBreadcrumbCollector;
        this.sessionLifecycleCallback = sessionLifecycleCallback;
        this.connectivity = connectivity;
        this.storageManager = storageManager;
        this.logger = logger;
        this.deliveryDelegate = deliveryDelegate;
        this.lastRunInfoStore = lastRunInfoStore;
        this.launchCrashTracker = launchCrashTracker;
        this.lastRunInfo = null;
    }

    private LastRunInfo loadLastRunInfo() {
        LastRunInfo lastRunInfo = lastRunInfoStore.load();
        LastRunInfo currentRunInfo = new LastRunInfo(0, false, false);
        persistRunInfo(currentRunInfo);
        return lastRunInfo;
    }

    /**
     * Load information about the last run, and reset the persisted information to the defaults.
     */
    private void persistRunInfo(final LastRunInfo runInfo) {
        try {
            bgTaskService.submitTask(TaskType.IO, new Runnable() {
                @Override
                public void run() {
                    lastRunInfoStore.persist(runInfo);
                }
            });
        } catch (RejectedExecutionException exc) {
            logger.w("Failed to persist last run info", exc);
        }
    }

    private void loadPlugins(@NonNull Configuration configuration) {
        NativeInterface.setClient(this);
        Set<Plugin> userPlugins = configuration.getPlugins();
        pluginClient = new PluginClient(userPlugins, immutableConfig, logger);
        pluginClient.loadPlugins(this);
    }

    private void logNull(String property) {
        logger.e("Invalid null value supplied to client." + property + ", ignoring");
    }

    private MetadataState copyMetadataState(@NonNull Configuration configuration) {
        // performs deep copy of metadata to preserve immutability of Configuration interface
        Metadata orig = configuration.impl.metadataState.getMetadata();
        Metadata copy = orig.copy();
        return configuration.impl.metadataState.copy(copy);
    }

    private void registerOrientationChangeListener() {
        IntentFilter configFilter = new IntentFilter();
        configFilter.addAction(Intent.ACTION_CONFIGURATION_CHANGED);
        ConfigChangeReceiver receiver = new ConfigChangeReceiver(deviceDataCollector,
                new Function2<String, String, Unit>() {
                    @Override
                    public Unit invoke(String oldOrientation, String newOrientation) {
                        Map<String, Object> data = new HashMap<>();
                        data.put("from", oldOrientation);
                        data.put("to", newOrientation);
                        leaveAutoBreadcrumb("Orientation changed", BreadcrumbType.STATE, data);
                        clientObservable.postOrientationChange(newOrientation);
                        return null;
                    }
                }
        );
        ContextExtensionsKt.registerReceiverSafe(appContext, receiver, configFilter, logger);
    }

    void setupNdkPlugin() {
        String lastRunInfoPath = lastRunInfoStore.getFile().getAbsolutePath();
        int crashes = (lastRunInfo != null) ? lastRunInfo.getConsecutiveLaunchCrashes() : 0;
        clientObservable.postNdkInstall(immutableConfig, lastRunInfoPath, crashes);
        syncInitialState();
        clientObservable.postNdkDeliverPending();
    }

    void registerObserver(Observer observer) {
        metadataState.addObserver(observer);
        breadcrumbState.addObserver(observer);
        sessionTracker.addObserver(observer);
        clientObservable.addObserver(observer);
        userState.addObserver(observer);
        contextState.addObserver(observer);
        deliveryDelegate.addObserver(observer);
        launchCrashTracker.addObserver(observer);
    }

    /**
     * Sends initial state values for Metadata/User/Context to any registered observers.
     */
    void syncInitialState() {
        metadataState.emitObservableEvent();
        contextState.emitObservableEvent();
        userState.emitObservableEvent();
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
    public void startSession() {
        sessionTracker.startSession(false);
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
    public void pauseSession() {
        sessionTracker.pauseSession();
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
    public boolean resumeSession() {
        return sessionTracker.resumeSession();
    }

    /**
     * Bugsnag uses the concept of "contexts" to help display and group your errors. Contexts
     * represent what was happening in your application at the time an error occurs.
     *
     * In an android app the "context" is automatically set as the foreground Activity.
     * If you would like to set this value manually, you should alter this property.
     */
    @Nullable public String getContext() {
        return contextState.getContext();
    }

    /**
     * Bugsnag uses the concept of "contexts" to help display and group your errors. Contexts
     * represent what was happening in your application at the time an error occurs.
     *
     * In an android app the "context" is automatically set as the foreground Activity.
     * If you would like to set this value manually, you should alter this property.
     */
    public void setContext(@Nullable String context) {
        contextState.setContext(context);
    }

    /**
     * Sets the user associated with the event.
     */
    @Override
    public void setUser(@Nullable String id, @Nullable String email, @Nullable String name) {
        userState.setUser(new User(id, email, name));
    }

    /**
     * Returns the currently set User information.
     */
    @NonNull
    @Override
    public User getUser() {
        return userState.getUser();
    }

    /**
     * Add a "on error" callback, to execute code at the point where an error report is
     * captured in Bugsnag.
     *
     * You can use this to add or modify information attached to an Event
     * before it is sent to your dashboard. You can also return
     * <code>false</code> from any callback to prevent delivery. "on error"
     * callbacks do not run before reports generated in the event
     * of immediate app termination from crashes in C/C++ code.
     *
     * For example:
     *
     * Bugsnag.addOnError(new OnErrorCallback() {
     * public boolean run(Event event) {
     * event.setSeverity(Severity.INFO);
     * return true;
     * }
     * })
     *
     * @param onError a callback to run before sending errors to Bugsnag
     * @see OnErrorCallback
     */
    @Override
    public void addOnError(@NonNull OnErrorCallback onError) {
        if (onError != null) {
            callbackState.addOnError(onError);
        } else {
            logNull("addOnError");
        }
    }

    /**
     * Removes a previously added "on error" callback
     * @param onError the callback to remove
     */
    @Override
    public void removeOnError(@NonNull OnErrorCallback onError) {
        if (onError != null) {
            callbackState.removeOnError(onError);
        } else {
            logNull("removeOnError");
        }
    }

    /**
     * Add an "on breadcrumb" callback, to execute code before every
     * breadcrumb captured by Bugsnag.
     *
     * You can use this to modify breadcrumbs before they are stored by Bugsnag.
     * You can also return <code>false</code> from any callback to ignore a breadcrumb.
     *
     * For example:
     *
     * Bugsnag.onBreadcrumb(new OnBreadcrumbCallback() {
     * public boolean run(Breadcrumb breadcrumb) {
     * return false; // ignore the breadcrumb
     * }
     * })
     *
     * @param onBreadcrumb a callback to run before a breadcrumb is captured
     * @see OnBreadcrumbCallback
     */
    @Override
    public void addOnBreadcrumb(@NonNull OnBreadcrumbCallback onBreadcrumb) {
        if (onBreadcrumb != null) {
            callbackState.addOnBreadcrumb(onBreadcrumb);
        } else {
            logNull("addOnBreadcrumb");
        }
    }

    /**
     * Removes a previously added "on breadcrumb" callback
     * @param onBreadcrumb the callback to remove
     */
    @Override
    public void removeOnBreadcrumb(@NonNull OnBreadcrumbCallback onBreadcrumb) {
        if (onBreadcrumb != null) {
            callbackState.removeOnBreadcrumb(onBreadcrumb);
        } else {
            logNull("removeOnBreadcrumb");
        }
    }

    /**
     * Add an "on session" callback, to execute code before every
     * session captured by Bugsnag.
     *
     * You can use this to modify sessions before they are stored by Bugsnag.
     * You can also return <code>false</code> from any callback to ignore a session.
     *
     * For example:
     *
     * Bugsnag.onSession(new OnSessionCallback() {
     * public boolean run(Session session) {
     * return false; // ignore the session
     * }
     * })
     *
     * @param onSession a callback to run before a session is captured
     * @see OnSessionCallback
     */
    @Override
    public void addOnSession(@NonNull OnSessionCallback onSession) {
        if (onSession != null) {
            callbackState.addOnSession(onSession);
        } else {
            logNull("addOnSession");
        }
    }

    /**
     * Removes a previously added "on session" callback
     * @param onSession the callback to remove
     */
    @Override
    public void removeOnSession(@NonNull OnSessionCallback onSession) {
        if (onSession != null) {
            callbackState.removeOnSession(onSession);
        } else {
            logNull("removeOnSession");
        }
    }

    /**
     * Notify Bugsnag of a handled exception
     *
     * @param exception the exception to send to Bugsnag
     */
    public void notify(@NonNull Throwable exception) {
        notify(exception, null);
    }

    /**
     * Notify Bugsnag of a handled exception
     *
     * @param exc the exception to send to Bugsnag
     * @param onError  callback invoked on the generated error report for
     *                  additional modification
     */
    public void notify(@NonNull Throwable exc, @Nullable OnErrorCallback onError) {
        if (exc != null) {
            SeverityReason severityReason = SeverityReason.newInstance(REASON_HANDLED_EXCEPTION);
            Metadata metadata = metadataState.getMetadata();
            Event event = new Event(exc, immutableConfig, severityReason, metadata, logger);
            populateAndNotifyAndroidEvent(event, onError);
        } else {
            logNull("notify");
        }
    }

    /**
     * Caches an error then attempts to notify.
     *
     * Should only ever be called from the {@link ExceptionHandler}.
     */
    void notifyUnhandledException(@NonNull Throwable exc, Metadata metadata,
                                  @SeverityReason.SeverityReasonType String severityReason,
                                  @Nullable String attributeValue) {
        SeverityReason handledState
                = SeverityReason.newInstance(severityReason, Severity.ERROR, attributeValue);
        Metadata data = Metadata.Companion.merge(metadataState.getMetadata(), metadata);
        Event event = new Event(exc, immutableConfig, handledState, data, logger);
        populateAndNotifyAndroidEvent(event, null);

        // persist LastRunInfo so that on relaunch users can check the app crashed
        int consecutiveLaunchCrashes = lastRunInfo == null ? 0
                : lastRunInfo.getConsecutiveLaunchCrashes();
        boolean launching = launchCrashTracker.isLaunching();
        if (launching) {
            consecutiveLaunchCrashes += 1;
        }
        LastRunInfo runInfo = new LastRunInfo(consecutiveLaunchCrashes, true, launching);
        persistRunInfo(runInfo);

        // suspend execution of any further background tasks, waiting for previously
        // submitted ones to complete.
        bgTaskService.shutdown();
    }

    void populateAndNotifyAndroidEvent(@NonNull Event event,
                                       @Nullable OnErrorCallback onError) {
        // Capture the state of the app and device and attach diagnostics to the event
        event.setDevice(deviceDataCollector.generateDeviceWithState(new Date().getTime()));
        event.addMetadata("device", deviceDataCollector.getDeviceMetadata());

        // add additional info that belongs in metadata
        // generate new object each time, as this can be mutated by end-users
        event.setApp(appDataCollector.generateAppWithState());
        event.addMetadata("app", appDataCollector.getAppDataMetadata());

        // Attach breadcrumbState to the event
        event.setBreadcrumbs(new ArrayList<>(breadcrumbState.getStore()));

        // Attach user info to the event
        User user = userState.getUser();
        event.setUser(user.getId(), user.getEmail(), user.getName());

        // Attach default context from active activity
        if (Intrinsics.isEmpty(event.getContext())) {
            String context = contextState.getContext();
            event.setContext(context != null ? context : appDataCollector.getActiveScreenClass());
        }
        notifyInternal(event, onError);
    }

    void notifyInternal(@NonNull Event event,
                        @Nullable OnErrorCallback onError) {
        String type = event.getImpl().getSeverityReasonType();
        logger.d("Client#notifyInternal() - event captured by Client, type=" + type);
        // Don't notify if this event class should be ignored
        if (event.shouldDiscardClass()) {
            logger.d("Skipping notification - should not notify for this class");
            return;
        }

        if (!immutableConfig.shouldNotifyForReleaseStage()) {
            logger.d("Skipping notification - should not notify for this release stage");
            return;
        }

        // set the redacted keys on the event as this
        // will not have been set for RN/Unity events
        Set<String> redactedKeys = metadataState.getMetadata().getRedactedKeys();
        Metadata eventMetadata = event.getImpl().getMetadata();
        eventMetadata.setRedactedKeys(redactedKeys);

        // get session for event
        Session currentSession = sessionTracker.getCurrentSession();

        if (currentSession != null
                && (immutableConfig.getAutoTrackSessions() || !currentSession.isAutoCaptured())) {
            event.setSession(currentSession);
        }

        // Run on error tasks, don't notify if any return false
        if (!callbackState.runOnErrorTasks(event, logger)
                || (onError != null && !onError.onError(event))) {
            logger.d("Skipping notification - onError task returned false");
            return;
        }

        deliveryDelegate.deliver(event);
    }

    /**
     * Returns the current buffer of breadcrumbs that will be sent with captured events. This
     * ordered list represents the most recent breadcrumbs to be captured up to the limit
     * set in {@link Configuration#getMaxBreadcrumbs()}.
     *
     * The returned collection is readonly and mutating the list will cause no effect on the
     * Client's state. If you wish to alter the breadcrumbs collected by the Client then you should
     * use {@link Configuration#setEnabledBreadcrumbTypes(Set)} and
     * {@link Configuration#addOnBreadcrumb(OnBreadcrumbCallback)} instead.
     *
     * @return a list of collected breadcrumbs
     */
    @NonNull
    public List<Breadcrumb> getBreadcrumbs() {
        return new ArrayList<>(breadcrumbState.getStore());
    }

    @NonNull
    AppDataCollector getAppDataCollector() {
        return appDataCollector;
    }

    @NonNull
    DeviceDataCollector getDeviceDataCollector() {
        return deviceDataCollector;
    }

    /**
     * Adds a map of multiple metadata key-value pairs to the specified section.
     */
    @Override
    public void addMetadata(@NonNull String section, @NonNull Map<String, ?> value) {
        if (section != null && value != null) {
            metadataState.addMetadata(section, value);
        } else {
            logNull("addMetadata");
        }
    }

    /**
     * Adds the specified key and value in the specified section. The value can be of
     * any primitive type or a collection such as a map, set or array.
     */
    @Override
    public void addMetadata(@NonNull String section, @NonNull String key, @Nullable Object value) {
        if (section != null && key != null) {
            metadataState.addMetadata(section, key, value);

        } else {
            logNull("addMetadata");
        }
    }

    /**
     * Removes all the data from the specified section.
     */
    @Override
    public void clearMetadata(@NonNull String section) {
        if (section != null) {
            metadataState.clearMetadata(section);
        } else {
            logNull("clearMetadata");
        }
    }

    /**
     * Removes data with the specified key from the specified section.
     */
    @Override
    public void clearMetadata(@NonNull String section, @NonNull String key) {
        if (section != null && key != null) {
            metadataState.clearMetadata(section, key);
        } else {
            logNull("clearMetadata");
        }
    }

    /**
     * Returns a map of data in the specified section.
     */
    @Nullable
    @Override
    public Map<String, Object> getMetadata(@NonNull String section) {
        if (section != null) {
            return metadataState.getMetadata(section);
        } else {
            logNull("getMetadata");
            return null;
        }
    }

    /**
     * Returns the value of the specified key in the specified section.
     */
    @Override
    @Nullable
    public Object getMetadata(@NonNull String section, @NonNull String key) {
        if (section != null && key != null) {
            return metadataState.getMetadata(section, key);
        } else {
            logNull("getMetadata");
            return null;
        }
    }

    @NonNull
    Map<String, Object> getMetadata() {
        return metadataState.getMetadata().toMap();
    }

    /**
     * Leave a "breadcrumb" log message, representing an action that occurred
     * in your app, to aid with debugging.
     *
     * @param message the log message to leave
     */
    public void leaveBreadcrumb(@NonNull String message) {
        if (message != null) {
            breadcrumbState.add(new Breadcrumb(message, logger));
        } else {
            logNull("leaveBreadcrumb");
        }
    }

    /**
     * Leave a "breadcrumb" log message representing an action or event which
     * occurred in your app, to aid with debugging
     * @param message  A short label
     * @param metadata Additional diagnostic information about the app environment
     * @param type     A category for the breadcrumb
     */
    public void leaveBreadcrumb(@NonNull String message,
                                @NonNull Map<String, Object> metadata,
                                @NonNull BreadcrumbType type) {
        if (message != null && type != null && metadata != null) {
            breadcrumbState.add(new Breadcrumb(message, type, metadata, new Date(), logger));
        } else {
            logNull("leaveBreadcrumb");
        }
    }

    /**
     * Intended for internal use only - leaves a breadcrumb if the type is enabled for automatic
     * breadcrumbs.
     *
     * @param message  A short label
     * @param type     A category for the breadcrumb
     * @param metadata Additional diagnostic information about the app environment
     */
    void leaveAutoBreadcrumb(@NonNull String message,
                             @NonNull BreadcrumbType type,
                             @NonNull Map<String, Object> metadata) {
        if (immutableConfig.shouldRecordBreadcrumbType(type)) {
            breadcrumbState.add(new Breadcrumb(message, type, metadata, new Date(), logger));
        }
    }

    /**
     * Retrieves information about the last launch of the application, if it has been run before.
     *
     * For example, this allows checking whether the app crashed on its last launch, which could
     * be used to perform conditional behaviour to recover from crashes, such as clearing the
     * app data cache.
     */
    @Nullable
    public LastRunInfo getLastRunInfo() {
        return lastRunInfo;
    }

    /**
     * Informs Bugsnag that the application has finished launching. Once this has been called
     * {@link AppWithState#isLaunching()} will always be false in any new error reports,
     * and synchronous delivery will not be attempted on the next launch for any fatal crashes.
     *
     * By default this method will be called after Bugsnag is initialized when
     * {@link Configuration#getLaunchDurationMillis()} has elapsed. Invoking this method manually
     * has precedence over the value supplied via the launchDurationMillis configuration option.
     */
    public void markLaunchCompleted() {
        launchCrashTracker.markLaunchCompleted();
    }

    SessionTracker getSessionTracker() {
        return sessionTracker;
    }

    @NonNull
    EventStore getEventStore() {
        return eventStore;
    }

    /**
     * Finalize by removing the receiver
     *
     * @throws Throwable if something goes wrong
     */
    @SuppressWarnings("checkstyle:NoFinalizer")
    protected void finalize() throws Throwable {
        if (systemBroadcastReceiver != null) {
            try {
                ContextExtensionsKt.unregisterReceiverSafe(appContext,
                        systemBroadcastReceiver, logger);
            } catch (IllegalArgumentException exception) {
                logger.w("Receiver not registered");
            }
        }
        super.finalize();
    }

    private void warnIfNotAppContext(Context androidContext) {
        if (!(androidContext instanceof Application)) {
            logger.w("Warning - Non-Application context detected! Please ensure that you are "
                + "initializing Bugsnag from a custom Application class.");
        }
    }

    ImmutableConfig getConfig() {
        return immutableConfig;
    }

    void setBinaryArch(String binaryArch) {
        getAppDataCollector().setBinaryArch(binaryArch);
    }

    Context getAppContext() {
        return appContext;
    }

    /**
     * Intended for internal use only - sets the code bundle id for React Native
     */
    @Nullable
    String getCodeBundleId() {
        return appDataCollector.getCodeBundleId();
    }

    /**
     * Intended for internal use only - sets the code bundle id for React Native
     */
    void setCodeBundleId(@Nullable String codeBundleId) {
        appDataCollector.setCodeBundleId(codeBundleId);
    }

    void addRuntimeVersionInfo(@NonNull String key, @NonNull String value) {
        deviceDataCollector.addRuntimeVersionInfo(key, value);
    }

    @VisibleForTesting
    void close() {
        connectivity.unregisterForNetworkChanges();
        bgTaskService.shutdown();
    }

    Logger getLogger() {
        return logger;
    }

    /**
     * Retrieves an instantiated plugin of the given type, or null if none has been created
     */
    @SuppressWarnings("rawtypes")
    @Nullable
    Plugin getPlugin(@NonNull Class clz) {
        Set<Plugin> plugins = pluginClient.getPlugins();
        for (Plugin plugin : plugins) {
            if (plugin.getClass().equals(clz)) {
                return plugin;
            }
        }
        return null;
    }

    Notifier getNotifier() {
        return notifier;
    }

    MetadataState getMetadataState() {
        return metadataState;
    }
}
