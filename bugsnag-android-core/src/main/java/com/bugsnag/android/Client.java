package com.bugsnag.android;

import static com.bugsnag.android.HandledState.REASON_HANDLED_EXCEPTION;
import static com.bugsnag.android.HandledState.REASON_UNHANDLED_EXCEPTION;

import com.bugsnag.android.NativeInterface.Message;

import android.annotation.SuppressLint;
import android.app.Application;
import android.content.Context;
import android.content.IntentFilter;
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
import java.lang.Thread;
import java.util.ArrayList;
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

    private static final String SHARED_PREF_KEY = "com.bugsnag.android";

    final ImmutableConfig immutableConfig;

    final CallbackState callbackState;
    final MetadataState metadataState;
    final ContextState contextState;
    final UserState userState;

    final Context appContext;

    @NonNull
    final DeviceData deviceData;

    @NonNull
    final AppData appData;

    @NonNull
    final BreadcrumbState breadcrumbState;

    @NonNull
    final EventStore eventStore;

    final SessionStore sessionStore;

    final SystemBroadcastReceiver systemBroadcastReceiver;
    final SessionTracker sessionTracker;
    final SharedPreferences sharedPrefs;

    private final OrientationEventListener orientationListener;
    private final Connectivity connectivity;
    private final UserRepository userRepository;
    final Logger logger;
    final ReportDeliveryDelegate reportDeliveryDelegate;

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
        logger = configuration.getLogger();
        warnIfNotAppContext(androidContext);
        appContext = androidContext.getApplicationContext();
        sessionStore = new SessionStore(appContext, logger, null);

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
        immutableConfig = ImmutableConfigKt.sanitiseConfiguration(appContext, configuration,
                connectivity, logger);

        callbackState = configuration.callbackState.copy();
        contextState = configuration.contextState.copy();
        contextState.addObserver(this);

        metadataState = configuration.metadataState.copy();
        metadataState.getMetadata().addObserver(this);
        // Set up and collect constant app and device diagnostics
        sharedPrefs = appContext.getSharedPreferences(SHARED_PREF_KEY, Context.MODE_PRIVATE);

        userRepository = new UserRepository(sharedPrefs,
                immutableConfig.getPersistUserBetweenSessions());
        userState = new UserState(userRepository);
        userState.addObserver(this);

        sessionTracker = new SessionTracker(immutableConfig, callbackState, this,
                sessionStore, logger);
        systemBroadcastReceiver = new SystemBroadcastReceiver(this, logger);

        appData = new AppData(appContext, appContext.getPackageManager(),
                immutableConfig, sessionTracker, logger);
        Resources resources = appContext.getResources();


        String id = userState.getUser().getId();
        deviceData = new DeviceData(connectivity, appContext, resources, id, logger);

        // Set up breadcrumbState
        breadcrumbState = new BreadcrumbState(immutableConfig.getMaxBreadcrumbs(), logger);

        if (appContext instanceof Application) {
            Application application = (Application) appContext;
            application.registerActivityLifecycleCallbacks(sessionTracker);
        } else {
            logger.w("Bugsnag is unable to setup automatic activity lifecycle "
                + "breadcrumbState on API Levels below 14.");
        }

        StorageManager storageManager
                = (StorageManager) appContext.getSystemService(Context.STORAGE_SERVICE);
        InternalReportDelegate delegate = new InternalReportDelegate(appContext,
                logger, immutableConfig, storageManager, appData, deviceData, sessionTracker);
        eventStore = new EventStore(immutableConfig, appContext, logger, delegate);

        reportDeliveryDelegate = new ReportDeliveryDelegate(logger, eventStore,
                immutableConfig, breadcrumbState);
        reportDeliveryDelegate.addObserver(this);

        // Install a default exception handler with this client
        if (immutableConfig.getAutoDetectErrors()) {
            new ExceptionHandler(this, logger);
        }

        // register a receiver for automatic breadcrumbState

        try {
            Async.run(new Runnable() {
                @Override
                public void run() {
                    IntentFilter intentFilter = SystemBroadcastReceiver.getIntentFilter();
                    appContext.registerReceiver(systemBroadcastReceiver, intentFilter);
                }
            });
        } catch (RejectedExecutionException ex) {
            logger.w("Failed to register for automatic breadcrumb broadcasts", ex);
        }
        connectivity.registerForNetworkChanges();

        configuration.addObserver(this);
        breadcrumbState.addObserver(this);
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
            logger.w("Failed to set up orientation tracking: " + ex);
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

    private void loadPlugins() {
        NativeInterface.setClient(this);
        BugsnagPluginInterface pluginInterface = BugsnagPluginInterface.INSTANCE;

        if (immutableConfig.getAutoDetectNdkCrashes()) {
            try {
                pluginInterface.registerPlugin(Class.forName("com.bugsnag.android.NdkPlugin"));
                logger.i("Registering NDK plugin");
            } catch (ClassNotFoundException exc) {
                logger.w("bugsnag-plugin-android-ndk artefact not found on classpath, "
                    + "NDK errors will not be captured.");
            }
        }
        if (immutableConfig.getAutoDetectAnrs()) {
            try {
                pluginInterface.registerPlugin(Class.forName("com.bugsnag.android.AnrPlugin"));
                logger.i("Registering ANR plugin");
            } catch (ClassNotFoundException exc) {
                logger.w("bugsnag-plugin-android-anr artefact not found on classpath, "
                    + "ANR errors will not be captured.");
            }
        }
        pluginInterface.loadPlugins(this);
    }

    void sendNativeSetupNotification() {
        setChanged();
        ArrayList<Object> messageArgs = new ArrayList<>();
        messageArgs.add(immutableConfig.getAutoDetectNdkCrashes());

        super.notifyObservers(new Message(NativeInterface.MessageType.INSTALL, messageArgs));
        try {
            Async.run(new Runnable() {
                @Override
                public void run() {
                    enqueuePendingNativeReports();
                }
            });
        } catch (RejectedExecutionException ex) {
            logger.w("Failed to enqueue native reports, will retry next launch: ", ex);
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
    public final void pauseSession() {
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
    public final boolean resumeSession() {
        return sessionTracker.resumeSession();
    }

    /**
     * Gets the context to be sent to Bugsnag.
     *
     * @return Context
     */
    @Nullable public String getContext() {
        return contextState.getContext();
    }

    /**
     * Set the context sent to Bugsnag. By default we'll attempt to detect the
     * name of the top-most activity at the time of a report, and use this
     * as the context, but sometime this is not possible.
     *
     * @param context set what was happening at the time of a crash
     */
    public void setContext(@Nullable String context) {
        contextState.setContext(context);
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
        userState.setUser(id, email, name);
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
     * You can use this to add or modify information attached to an error
     * before it is sent to your dashboard. You can also return
     * <code>false</code> from any callback to prevent delivery. "on error"
     * callbacks do not run before reports generated in the event
     * of immediate app termination from crashes in C/C++ code.
     * <p>
     * For example:
     * <p>
     * Bugsnag.addOnError(new OnError() {
     * public boolean run(Event error) {
     * error.setSeverity(Severity.INFO);
     * return true;
     * }
     * })
     *
     * @param onError a callback to run before sending errors to Bugsnag
     * @see OnError
     */
    @Override
    public void addOnError(@NonNull OnError onError) {
        callbackState.addOnError(onError);
    }

    @Override
    public void removeOnError(@NonNull OnError onError) {
        callbackState.removeOnError(onError);
    }

    /**
     * Add a "before breadcrumb" callback, to execute code before every
     * breadcrumb captured by Bugsnag.
     * <p>
     * You can use this to modify breadcrumbState before they are stored by Bugsnag.
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
        callbackState.addOnBreadcrumb(onBreadcrumb);
    }

    @Override
    public void removeOnBreadcrumb(@NonNull OnBreadcrumb onBreadcrumb) {
        callbackState.removeOnBreadcrumb(onBreadcrumb);
    }

    @Override
    public void addOnSession(@NonNull OnSession onSession) {
        callbackState.addOnSession(onSession);
    }

    @Override
    public void removeOnSession(@NonNull OnSession onSession) {
        callbackState.removeOnSession(onSession);
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
        Event event = new Event(exc, immutableConfig, handledState, metadataState.getMetadata());
        notifyInternal(event, onError);
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
        Event event = new Event(null, immutableConfig, handledState, metadataState.getMetadata());
        event.setErrors(Collections.singletonList(err));
        notifyInternal(event, onError);
    }

    /**
     * Caches an error then attempts to notify.
     *
     * Should only ever be called from the {@link ExceptionHandler}.
     */
    void notifyUnhandledException(@NonNull Throwable exc,
                                  @HandledState.SeverityReason String severityReason,
                                  @Nullable String attributeValue,
                                  Thread thread) {
        HandledState handledState
                = HandledState.newInstance(severityReason, Severity.ERROR, attributeValue);
        ThreadState threadState = new ThreadState(immutableConfig, exc, thread);
        Event event = new Event(exc, immutableConfig, handledState,
                metadataState.getMetadata(), threadState);
        notifyInternal(event, null);
    }

    void notifyInternal(@NonNull Event event,
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
        if (TextUtils.isEmpty(event.getContext())) {
            String context = contextState.getContext();
            event.setContext(context != null ? context : appData.getActiveScreenClass());
        }

        // Run on error tasks, don't notify if any return false
        if (!callbackState.runOnErrorTasks(event, logger)
                || (onError != null && !onError.run(event))) {
            logger.i("Skipping notification - onError task returned false");
            return;
        }
        reportDeliveryDelegate.deliverReport(event);
    }

    @Override
    public void addMetadata(@NonNull String section, @Nullable Object value) {
        addMetadata(section, null, value);
    }

    @Override
    public void addMetadata(@NonNull String section, @Nullable String key, @Nullable Object value) {
        metadataState.addMetadata(section, key, value);
    }

    @Override
    public void clearMetadata(@NonNull String section) {
        clearMetadata(section, null);
    }

    @Override
    public void clearMetadata(@NonNull String section, @Nullable String key) {
        metadataState.clearMetadata(section, key);
    }

    @Nullable
    @Override
    public Object getMetadata(@NonNull String section) {
        return getMetadata(section, null);
    }

    @Override
    @Nullable
    public Object getMetadata(@NonNull String section, @Nullable String key) {
        return metadataState.getMetadata(section, key);
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
        if (callbackState.runOnBreadcrumbTasks(crumb, logger)) {
            breadcrumbState.add(crumb);
        }
    }

    private void warnIfNotAppContext(Context androidContext) {
        if (!(androidContext instanceof Application)) {
            logger.w("Warning - Non-Application context detected! Please ensure that you are "
                + "initializing Bugsnag from a custom Application class.");
        }
    }

    void setBinaryArch(String binaryArch) {
        appData.setBinaryArch(binaryArch);
    }

    void close() {
        orientationListener.disable();
        connectivity.unregisterForNetworkChanges();
        if (systemBroadcastReceiver != null) {
            try {
                appContext.unregisterReceiver(systemBroadcastReceiver);
            } catch (IllegalArgumentException exception) {
                logger.w("Receiver not registered");
            }
        }
    }
}
