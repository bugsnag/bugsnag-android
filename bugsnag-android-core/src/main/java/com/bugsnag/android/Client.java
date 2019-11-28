package com.bugsnag.android;

import static com.bugsnag.android.HandledState.REASON_HANDLED_EXCEPTION;
import static com.bugsnag.android.HandledState.REASON_UNHANDLED_EXCEPTION;
import static com.bugsnag.android.ImmutableConfigKt.sanitiseConfiguration;

import com.bugsnag.android.NativeInterface.Message;

import android.annotation.SuppressLint;
import android.app.Application;
import android.content.Context;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.storage.StorageManager;
import android.view.OrientationEventListener;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import kotlin.Unit;
import kotlin.jvm.functions.Function1;

import java.io.File;
import java.io.IOException;
import java.lang.Thread;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;
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
public class Client extends Observable implements Observer, MetadataAware, CallbackAware,
        UserAware {

    private static final boolean BLOCKING = true;
    private static final String SHARED_PREF_KEY = "com.bugsnag.android";

    static final String INTERNAL_DIAGNOSTICS_TAB = "BugsnagDiagnostics";

    final Configuration clientState;
    final ImmutableConfig immutableConfig;

    final UserState userState;

    final Context appContext;

    @NonNull
    protected final DeviceData deviceData;

    @NonNull
    protected final AppData appData;

    @NonNull
    final BreadcrumbState breadcrumbState;

    @NonNull
    protected final EventStore eventStore;

    final SessionStore sessionStore;

    final SystemBroadcastReceiver systemBroadcastReceiver;
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
                    eventStore.flushAsync();
                }
                return null;
            }
        });

        // set sensible defaults for delivery/project packages etc if not set
        sanitiseConfiguration(appContext, configuration, connectivity);
        clientState = configuration;
        immutableConfig = ImmutableConfigKt.convertToImmutableConfig(configuration);

        sessionTracker = new SessionTracker(immutableConfig, clientState, this, sessionStore);
        systemBroadcastReceiver = new SystemBroadcastReceiver(this);

        // Set up and collect constant app and device diagnostics
        sharedPrefs = appContext.getSharedPreferences(SHARED_PREF_KEY, Context.MODE_PRIVATE);

        appData = new AppData(appContext, appContext.getPackageManager(),
                immutableConfig, sessionTracker);

        userRepository = new UserRepository(sharedPrefs,
                immutableConfig.getPersistUserBetweenSessions());
        userState = new UserState(userRepository);

        String id = userState.getUser().getId();
        DeviceBuildInfo info = DeviceBuildInfo.Companion.defaultInfo();
        deviceData = new DeviceData(connectivity, appContext, appContext.getResources(), id, info);

        // Set up breadcrumbs
        breadcrumbState = new BreadcrumbState(immutableConfig.getMaxBreadcrumbs());

        if (appContext instanceof Application) {
            Application application = (Application) appContext;
            application.registerActivityLifecycleCallbacks(sessionTracker);
        } else {
            Logger.warn("Bugsnag is unable to setup automatic activity lifecycle "
                + "breadcrumbs on API Levels below 14.");
        }

        // Create the error store that is used in the exception handler
        FileStore.Delegate delegate = new EventStore.Delegate() {
            @Override
            public void onErrorIOFailure(Exception exc, File errorFile, String context) {
                // send an internal error to bugsnag with no cache
                HandledState handledState = HandledState.newInstance(REASON_UNHANDLED_EXCEPTION);
                Event err = new Event(exc, immutableConfig, handledState);
                err.setContext(context);

                err.addMetadata(INTERNAL_DIAGNOSTICS_TAB, "canRead", errorFile.canRead());
                err.addMetadata(INTERNAL_DIAGNOSTICS_TAB, "canWrite", errorFile.canWrite());
                err.addMetadata(INTERNAL_DIAGNOSTICS_TAB, "exists", errorFile.exists());

                @SuppressLint("UsableSpace") // storagemanager alternative API requires API 26
                long usableSpace = appContext.getCacheDir().getUsableSpace();
                err.addMetadata(INTERNAL_DIAGNOSTICS_TAB, "usableSpace", usableSpace);
                err.addMetadata(INTERNAL_DIAGNOSTICS_TAB, "filename", errorFile.getName());
                err.addMetadata(INTERNAL_DIAGNOSTICS_TAB, "fileLength", errorFile.length());
                recordStorageCacheBehavior(err);
                Client.this.reportInternalBugsnagError(err);
            }
        };
        eventStore = new EventStore(immutableConfig, clientState, appContext, delegate);

        // Install a default exception handler with this client
        if (immutableConfig.getAutoDetectErrors()) {
            new ExceptionHandler(this);
        }

        // register a receiver for automatic breadcrumbs

        try {
            Async.run(new Runnable() {
                @Override
                public void run() {
                    IntentFilter intentFilter = SystemBroadcastReceiver.getIntentFilter();
                    appContext.registerReceiver(systemBroadcastReceiver, intentFilter);
                }
            });
        } catch (RejectedExecutionException ex) {
            Logger.warn("Failed to register for automatic breadcrumb broadcasts", ex);
        }
        connectivity.registerForNetworkChanges();

        Logger.setEnabled(immutableConfig.getLoggingEnabled());
        configuration.getMetadata().addObserver(this);
        breadcrumbState.addObserver(this);
        sessionTracker.addObserver(this);
        userState.addObserver(this);

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

        // filter out any disabled breadcrumb types
        addOnBreadcrumb(new OnBreadcrumb() {
            @Override
            public boolean run(@NonNull Breadcrumb breadcrumb) {
                return immutableConfig.getEnabledBreadcrumbTypes().contains(breadcrumb.getType());
            }
        });

        // Flush any on-disk errors
        eventStore.flushOnLaunch();
        loadPlugins();
        leaveBreadcrumb("Bugsnag loaded");
    }

    void recordStorageCacheBehavior(Event event) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            File cacheDir = appContext.getCacheDir();
            File errDir = new File(cacheDir, "bugsnag-errors");

            try {
                boolean tombstone = storageManager.isCacheBehaviorTombstone(errDir);
                boolean group = storageManager.isCacheBehaviorGroup(errDir);
                event.addMetadata(INTERNAL_DIAGNOSTICS_TAB, "cacheTombstone", tombstone);
                event.addMetadata(INTERNAL_DIAGNOSTICS_TAB, "cacheGroup", group);
            } catch (IOException exc) {
                Logger.warn("Failed to record cache behaviour, skipping diagnostics", exc);
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
        setChanged();
        super.notifyObservers(arg);
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
        setChanged();
        notifyObservers(new NativeInterface.Message(
                NativeInterface.MessageType.UPDATE_CONTEXT, context));
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
    @Override
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
    @Override
    public User getUser() {
        return userState.getUser();
    }

    /**
     * Set a unique identifier for the user currently using your application.
     * By default, this will be an automatically generated unique id
     * You can search for this information in your Bugsnag dashboard.
     *
     * @param id a unique identifier of the current user
     */
    @Override
    public void setUserId(@Nullable String id) {
        userState.setUserId(id);
    }

    /**
     * Set the email address of the current user.
     * You can search for this information in your Bugsnag dashboard.
     *
     * @param email the email address of the current user
     */
    @Override
    public void setUserEmail(@Nullable String email) {
        userState.setUserEmail(email);
    }

    /**
     * Set the name of the current user.
     * You can search for this information in your Bugsnag dashboard.
     *
     * @param name the name of the current user
     */
    @Override
    public void setUserName(@Nullable String name) {
        userState.setUserName(name);
    }


    /**
     * Add a "on error" callback, to execute code at the point where an error report is
     * captured in Bugsnag.
     * <p>
     * You can use this to add or modify information attached to an Event
     * before it is sent to your dashboard. You can also return
     * <code>false</code> from any callback to prevent delivery. "on error"
     * callbacks do not run before reports generated in the event
     * of immediate app termination from crashes in C/C++ code.
     * <p>
     * For example:
     * <p>
     * Bugsnag.addOnError(new OnError() {
     * public boolean run(Event event) {
     * event.setSeverity(Severity.INFO);
     * return true;
     * }
     * })
     *
     * @param onError a callback to run before sending errors to Bugsnag
     * @see OnError
     */
    @Override
    public void addOnError(@NonNull OnError onError) {
        clientState.addOnError(onError);
    }

    @Override
    public void removeOnError(@NonNull OnError onError) {
        clientState.removeOnError(onError);
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
     * Bugsnag.onBreadcrumb(new OnBreadcrumb() {
     * public boolean run(Breadcrumb breadcrumb) {
     * return false; // ignore the breadcrumb
     * }
     * })
     *
     * @param onBreadcrumb a callback to run before a breadcrumb is captured
     * @see OnBreadcrumb
     */
    @Override
    public void addOnBreadcrumb(@NonNull OnBreadcrumb onBreadcrumb) {
        clientState.addOnBreadcrumb(onBreadcrumb);
    }

    @Override
    public void removeOnBreadcrumb(@NonNull OnBreadcrumb onBreadcrumb) {
        clientState.removeOnBreadcrumb(onBreadcrumb);
    }

    @Override
    public void addOnSession(@NonNull OnSession onSession) {
        clientState.addOnSession(onSession);
    }

    @Override
    public void removeOnSession(@NonNull OnSession onSession) {
        clientState.removeOnSession(onSession);
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
    public void notify(@NonNull Throwable exc, @Nullable OnError onError) {
        HandledState handledState = HandledState.newInstance(REASON_HANDLED_EXCEPTION);
        Event event = new Event(exc, immutableConfig, handledState, clientState.getMetadata());
        notifyInternal(event, DeliveryStyle.ASYNC, onError);
    }

    /**
     * Notify Bugsnag of an error
     *
     * @param name       the error name or class
     * @param message    the error message
     * @param stacktrace the stackframes associated with the error
     */
    public void notify(@NonNull String name,
                       @NonNull String message,
                       @NonNull StackTraceElement[] stacktrace) {
        notify(name, message, stacktrace, null);
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
    public void notify(@NonNull String name,
                       @NonNull String message,
                       @NonNull StackTraceElement[] stacktrace,
                       @Nullable OnError onError) {
        HandledState handledState = HandledState.newInstance(REASON_HANDLED_EXCEPTION);
        Stacktrace trace = new Stacktrace(stacktrace, immutableConfig.getProjectPackages());
        Error err = new Error(name, message, trace.getTrace());
        Metadata metadata = clientState.getMetadata();
        Event event = new Event(null, immutableConfig, handledState, metadata);
        event.setErrors(Collections.singletonList(err));
        notifyInternal(event, DeliveryStyle.ASYNC, onError);
    }

    /**
     * Caches an error then attempts to notify.
     *
     * Should only ever be called from the {@link ExceptionHandler}.
     */
    void notifyUnhandledException(@NonNull Throwable exc, Metadata metadata,
                                  @HandledState.SeverityReason String severityReason,
                                  @Nullable String attributeValue, Thread thread) {
        HandledState handledState
                = HandledState.newInstance(severityReason, Severity.ERROR, attributeValue);
        ThreadState threadState = new ThreadState(immutableConfig, exc, thread);
        Event event = new Event(exc, immutableConfig, handledState,
                clientState.getMetadata());
        notifyInternal(event, DeliveryStyle.ASYNC_WITH_CACHE, null);
    }

    void notifyInternal(@NonNull Event event,
                        @NonNull DeliveryStyle style,
                        @Nullable OnError onError) {
        // Don't notify if this event class should be ignored
        if (event.shouldIgnoreClass()) {
            return;
        }

        if (!immutableConfig.shouldNotifyForReleaseStage()) {
            return;
        }

        // get session for event
        Session currentSession = sessionTracker.getCurrentSession();

        if (currentSession != null
                && (immutableConfig.getAutoTrackSessions() || !currentSession.isAutoCaptured())) {
            event.setSession(currentSession);
        }

        // Capture the state of the app and device and attach diagnostics to the event
        Map<String, Object> errorDeviceData = deviceData.getDeviceData();
        event.setDevice(errorDeviceData);
        event.addMetadata("device", null, deviceData.getDeviceMetadata());

        // add additional info that belongs in metadata
        // generate new object each time, as this can be mutated by end-users
        Map<String, Object> errorAppData = appData.getAppData();
        event.setApp(errorAppData);
        event.addMetadata("app", null, appData.getAppDataMetadata());

        // Attach breadcrumbState to the event
        event.setBreadcrumbs(new ArrayList<>(breadcrumbState.getStore()));

        // Attach user info to the event
        User user = userState.getUser();
        event.setUser(user.getId(), user.getEmail(), user.getName());

        // Attach default context from active activity
        if (Intrinsics.isEmpty(event.getContext())) {
            String context = clientState.getContext();
            event.setContext(context != null ? context : appData.getActiveScreenClass());
        }

        // Run on error tasks, don't notify if any return false
        if (!runOnErrorTasks(event) || (onError != null && !onError.run(event))) {
            Logger.info("Skipping notification - onError task returned false");
            return;
        }

        // Build the report
        Report report = new Report(immutableConfig.getApiKey(), event);

        if (event.getSession() != null && currentSession != null) {
            setChanged();

            if (event.isUnhandled()) {
                event.setSession(currentSession.incrementUnhandledAndCopy());
                notifyObservers(new Message(
                    NativeInterface.MessageType.NOTIFY_UNHANDLED, null));
            } else {
                event.setSession(currentSession.incrementHandledAndCopy());
                notifyObservers(new Message(
                    NativeInterface.MessageType.NOTIFY_HANDLED,  null));
            }
        }

        switch (style) {
            case ASYNC:
                deliverReportAsync(event, report);
                break;
            case ASYNC_WITH_CACHE:
                eventStore.write(event);
                eventStore.flushAsync();
                break;
            default:
                break;
        }
    }

    /**
     * Reports an event that occurred within the notifier to bugsnag. A lean event report will be
     * generated and sent asynchronously with no callbacks, retry attempts, or writing to disk.
     * This is intended for internal use only, and reports will not be visible to end-users.
     */
    void reportInternalBugsnagError(@NonNull Event event) {
        Map<String, Object> app = appData.getAppDataSummary();
        app.put("duration", AppData.getDurationMs());
        app.put("durationInForeground", appData.calculateDurationInForeground());
        app.put("inForeground", sessionTracker.isInForeground());
        event.setApp(app);

        Map<String, Object> device = deviceData.getDeviceDataSummary();
        device.put("freeDisk", deviceData.calculateFreeDisk());
        event.setDevice(device);

        Notifier notifier = Notifier.INSTANCE;
        event.addMetadata(INTERNAL_DIAGNOSTICS_TAB, "notifierName", notifier.getName());
        event.addMetadata(INTERNAL_DIAGNOSTICS_TAB, "notifierVersion", notifier.getVersion());
        event.addMetadata(INTERNAL_DIAGNOSTICS_TAB, "apiKey", immutableConfig.getApiKey());

        Object packageName = appData.getAppData().get("packageName");
        event.addMetadata(INTERNAL_DIAGNOSTICS_TAB, "packageName", packageName);

        final Report report = new Report(null, event);
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
                            headers.remove("Bugsnag-Api-Key");
                            DefaultDelivery defaultDelivery = (DefaultDelivery) delivery;
                            defaultDelivery.deliver(params.getEndpoint(), report, headers);
                        }

                    } catch (Exception exception) {
                        Logger.warn("Failed to report internal event to Bugsnag", exception);
                    }
                }
            });
        } catch (RejectedExecutionException ignored) {
            // drop internal report
        }
    }

    private void deliverReportAsync(@NonNull Event event, Report report) {
        final Report finalReport = report;
        final Event finalEvent = event;

        // Attempt to send the report in the background
        try {
            Async.run(new Runnable() {
                @Override
                public void run() {
                    deliver(finalReport, finalEvent);
                }
            });
        } catch (RejectedExecutionException exception) {
            eventStore.write(event);
            Logger.warn("Exceeded max queue count, saving to disk to send later");
        }
    }

    private void leaveErrorBreadcrumb(@NonNull Event event) {
        // Add a breadcrumb for this event occurring
        List<Error> errors = event.getErrors();

        if (errors.size() > 0) {
            String name = errors.get(0).getErrorClass();
            String msg = errors.get(0).getErrorMessage();
            Map<String, Object> message = Collections.<String, Object>singletonMap("message", msg);
            breadcrumbState.add(new Breadcrumb(name, BreadcrumbType.ERROR, message, new Date()));
        }
    }

    @NonNull
    Collection<Breadcrumb> getBreadcrumbs() {
        return new ArrayList<>(breadcrumbState.getStore());
    }

    @NonNull
    AppData getAppData() {
        return appData;
    }

    @NonNull
    DeviceData getDeviceData() {
        return deviceData;
    }

    @Override
    public void addMetadata(@NonNull String section, @NonNull Map<String, ?> value) {
        clientState.getMetadata().addMetadata(section, value);
    }

    @Override
    public void addMetadata(@NonNull String section, @Nullable String key, @Nullable Object value) {
        clientState.getMetadata().addMetadata(section, key, value);
    }

    @Override
    public void clearMetadata(@NonNull String section) {
        clearMetadata(section, null);
    }

    @Override
    public void clearMetadata(@NonNull String section, @Nullable String key) {
        clientState.getMetadata().clearMetadata(section, key);
    }

    @Nullable
    @Override
    public Object getMetadata(@NonNull String section) {
        return getMetadata(section, null);
    }

    @Override
    @Nullable
    public Object getMetadata(@NonNull String section, @Nullable String key) {
        return clientState.getMetadata().getMetadata(section, key);
    }

    /**
     * Leave a "breadcrumb" log message, representing an action that occurred
     * in your app, to aid with debugging.
     *
     * @param message the log message to leave (max 140 chars)
     */
    public void leaveBreadcrumb(@NonNull String message) {
        leaveBreadcrumbInternal(new Breadcrumb(message));
    }

    /**
     * Leave a "breadcrumb" log message, representing an action which occurred
     * in your app, to aid with debugging.
     */
    public void leaveBreadcrumb(@NonNull String message,
                                @NonNull BreadcrumbType type,
                                @NonNull Map<String, Object> metadata) {
        leaveBreadcrumbInternal(new Breadcrumb(message, type, metadata, new Date()));
    }

    private void leaveBreadcrumbInternal(Breadcrumb crumb) {
        if (runOnBreadcrumbTasks(crumb)) {
            breadcrumbState.add(crumb);
        }
    }

    void deliver(@NonNull Report report, @NonNull Event event) {
        DeliveryParams deliveryParams = immutableConfig.errorApiDeliveryParams();
        Delivery delivery = immutableConfig.getDelivery();
        DeliveryStatus deliveryStatus = delivery.deliver(report, deliveryParams);

        switch (deliveryStatus) {
            case DELIVERED:
                Logger.info("Sent 1 new event to Bugsnag");
                leaveErrorBreadcrumb(event);
                break;
            case UNDELIVERED:
                Logger.warn("Could not send event(s) to Bugsnag,"
                        + " saving to disk to send later");
                eventStore.write(event);
                leaveErrorBreadcrumb(event);
                break;
            case FAILURE:
                Logger.warn("Problem sending event to Bugsnag");
                break;
            default:
                break;
        }
    }

    SessionTracker getSessionTracker() {
        return sessionTracker;
    }

    private boolean runOnErrorTasks(Event event) {
        for (OnError onError : clientState.getOnErrorTasks()) {
            try {
                if (!onError.run(event)) {
                    return false;
                }
            } catch (Throwable ex) {
                Logger.warn("OnError threw an Exception", ex);
            }
        }

        // By default, allow the event to be sent if there were no objections
        return true;
    }

    private boolean runOnBreadcrumbTasks(@NonNull Breadcrumb breadcrumb) {
        Collection<OnBreadcrumb> tasks = clientState.getBreadcrumbCallbacks();
        for (OnBreadcrumb callback : tasks) {
            try {
                if (!callback.run(breadcrumb)) {
                    return false;
                }
            } catch (Throwable ex) {
                Logger.warn("OnBreadcrumb threw an Exception", ex);
            }
        }
        return true;
    }

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
                appContext.unregisterReceiver(systemBroadcastReceiver);
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
    Configuration getConfiguration() {
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
