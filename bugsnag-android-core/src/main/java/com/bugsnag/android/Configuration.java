package com.bugsnag.android;

import android.content.Context;
import android.text.TextUtils;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Observable;
import java.util.Observer;
import java.util.Set;

/**
 * User-specified configuration storage object, contains information
 * specified at the client level, api-key and endpoint configuration.
 */
public class Configuration extends Observable implements Observer, CallbackAware {

    static final String HEADER_API_KEY = "Bugsnag-Api-Key";
    private static final int DEFAULT_MAX_SIZE = 25;
    static final String DEFAULT_EXCEPTION_TYPE = "android";

    @NonNull
    private final String apiKey;
    private final ClientState clientState;
    private String buildUuid;
    private String appVersion;
    private Integer versionCode = 0;
    private String context;

    private final Set<String> ignoreClasses = new HashSet<>();
    private final Set<String> enabledReleaseStages = new HashSet<>();
    private final Set<String> projectPackages = new HashSet<>();
    private final Set<BreadcrumbType> enabledBreadcrumbTypes = new HashSet<>();

    private String releaseStage;
    private boolean sendThreads = true;
    private boolean persistUserBetweenSessions = false;
    private long launchCrashThresholdMs = 5 * 1000;
    private boolean autoTrackSessions = true;
    private boolean autoCaptureBreadcrumbs = true;

    private boolean autoDetectAnrs = false;
    private boolean autoDetectNdkCrashes;
    private boolean autoDetectErrors = true;

    private String codeBundleId;
    private String appType = "android";

    private Delivery delivery;
    private Logger logger;
    private Endpoints endpoints = new Endpoints();
    private int maxBreadcrumbs = DEFAULT_MAX_SIZE;

    /**
     * Construct a new Bugsnag configuration object with the supplied API key
     *
     * @param apiKey The API key to send reports to
     */
    public Configuration(@NonNull String apiKey) {
        if (TextUtils.isEmpty(apiKey)) {
            throw new IllegalArgumentException("You must provide a Bugsnag API key");
        }

        this.apiKey = apiKey;
        this.clientState = new ClientState();
        this.clientState.getMetadata().addObserver(this);
        enabledBreadcrumbTypes.addAll(Arrays.asList(BreadcrumbType.values()));

        try {
            // check if AUTO_DETECT_NDK_CRASHES has been set in bugsnag-android
            // or bugsnag-android-ndk
            Class<?> clz = Class.forName("com.bugsnag.android.BuildConfig");
            Field field = clz.getDeclaredField("AUTO_AUTO_DETECT_NDK_CRASHES");
            autoDetectNdkCrashes = field.getBoolean(null);
        } catch (Throwable exc) {
            autoDetectNdkCrashes = false;
        }

        boolean loggingEnabled = !AppData.RELEASE_STAGE_PRODUCTION.equals(releaseStage);

        if (loggingEnabled) {
            logger = DebugLogger.INSTANCE;
        } else {
            logger = NoopLogger.INSTANCE;
        }
    }

    /**
     * Constructs a new Bugsnag Configuration object by looking for meta-data elements in
     * the AndroidManifest.xml
     *
     * @return a new Configuration object
     */
    @NonNull
    public static Configuration loadConfig(@NonNull Context ctx) {
        return new ManifestConfigLoader().load(ctx);
    }

    /**
     * Respond to an update notification from observed objects, like MetaData
     */
    public void update(@NonNull Observable observable, @NonNull Object arg) {
        if (arg instanceof NativeInterface.Message) {
            setChanged();
            notifyObservers(arg);
        }
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
    @NonNull
    public String getAppVersion() {
        return appVersion;
    }

    /**
     * Set the application version sent to Bugsnag. By default we'll pull this
     * from your AndroidManifest.xml
     *
     * @param appVersion the app version to send
     */
    public void setAppVersion(@NonNull String appVersion) {
        this.appVersion = appVersion;
    }

    /**
     * Gets the version code sent to Bugsnag.
     *
     * @return Version Code
     */
    @Nullable
    public Integer getVersionCode() {
        return versionCode;
    }

    /**
     * Set the version code sent to Bugsnag. By default we'll pull this
     * from your AndroidManifest.xml
     *
     * @param versionCode the version code to send
     */
    public void setVersionCode(@Nullable Integer versionCode) {
        this.versionCode = versionCode;
    }

    /**
     * Gets the context to be sent to Bugsnag.
     *
     * @return Context
     */
    @Nullable
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
    public void setContext(@Nullable String context) {
        this.context = context;
        setChanged();
        notifyObservers(new NativeInterface.Message(
                NativeInterface.MessageType.UPDATE_CONTEXT, context));
    }

    /**
     * Set the endpoints to send data to. By default we'll send error reports to
     * https://notify.bugsnag.com, and sessions to https://sessions.bugsnag.com, but you can
     * override this if you are using Bugsnag Enterprise to point to your own Bugsnag endpoints.
     *
     * @param endpoints the notify and sessions endpoint
     */
    public void setEndpoints(@NonNull Endpoints endpoints) {
        this.endpoints = endpoints;
    }

    /**
     * Retrieves the endpoints to send data to. By default we'll send error reports to
     * https://notify.bugsnag.com, and sessions to https://sessions.bugsnag.com, but you can
     * override this if you are using Bugsnag Enterprise to point to your own Bugsnag endpoints.
     *
     * @return the notify and sessions endpoint
     */
    @NonNull
    public Endpoints getEndpoints() {
        return endpoints;
    }

    /**
     * Get the buildUUID.
     *
     * @return build UUID
     */
    @Nullable
    public String getBuildUuid() {
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
    public void setBuildUuid(@Nullable String buildUuid) {
        this.buildUuid = buildUuid;
    }

    /**
     * Get which keys should be filtered when sending metaData to Bugsnag
     *
     * @return a list of keys that should be redacted from the payload
     */
    @NonNull
    public Collection<String> getRedactKeys() {
        return Collections.unmodifiableSet(clientState.getMetadata().getRedactKeys());
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
     * client.setRedactKeys("password", "credit_card");
     *
     * @param redactKeys a list of keys to redact from metaData
     */
    public void setRedactKeys(@NonNull Collection<String> redactKeys) {
        clientState.getMetadata().setRedactKeys(redactKeys);
    }

    /**
     * Get which exception classes should be ignored (not sent) by Bugsnag.
     *
     * @return Ignore classes
     */
    @NonNull
    public Collection<String> getIgnoreClasses() {
        return Collections.unmodifiableSet(ignoreClasses);
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
    public void setIgnoreClasses(@NonNull Collection<String> ignoreClasses) {
        this.ignoreClasses.clear();
        this.ignoreClasses.addAll(ignoreClasses);
    }

    /**
     * Get for which releaseStages errors should be sent to Bugsnag.
     *
     * @return Enabled release stages
     */
    @NonNull
    public Collection<String> getEnabledReleaseStages() {
        return Collections.unmodifiableSet(enabledReleaseStages);
    }

    /**
     * Set for which releaseStages errors should be sent to Bugsnag.
     * Use this to stop errors from development builds being sent.
     * <p/>
     * For example:
     * <p/>
     * client.setEnabledReleaseStages("production");
     *
     * @param enabledReleaseStages a list of releaseStages to notify for
     * @see #setReleaseStage
     */
    public void setEnabledReleaseStages(@NonNull Collection<String> enabledReleaseStages) {
        this.enabledReleaseStages.clear();
        this.enabledReleaseStages.addAll(enabledReleaseStages);
    }

    @NonNull
    public Set<BreadcrumbType> getEnabledBreadcrumbTypes() {
        return Collections.unmodifiableSet(enabledBreadcrumbTypes);
    }

    public void setEnabledBreadcrumbTypes(@NonNull Set<BreadcrumbType> enabledBreadcrumbTypes) {
        this.enabledBreadcrumbTypes.clear();
        this.enabledBreadcrumbTypes.addAll(enabledBreadcrumbTypes);
    }

    /**
     * Get which packages should be considered part of your application.
     *
     * @return packages
     */
    @NonNull
    public Collection<String> getProjectPackages() {
        return Collections.unmodifiableSet(projectPackages);
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
    public void setProjectPackages(@NonNull Collection<String> projectPackages) {
        this.projectPackages.clear();
        this.projectPackages.addAll(projectPackages);
    }

    /**
     * Get the current "release stage" of your application.
     *
     * @return release stage
     */
    @Nullable
    public String getReleaseStage() {
        return releaseStage;
    }

    /**
     * Set the current "release stage" of your application.
     * By default, we'll set this to "development" for debug builds and
     * "production" for non-debug builds.
     *
     * @param releaseStage the release stage of the app
     * @see #setEnabledReleaseStages
     */
    public void setReleaseStage(@Nullable String releaseStage) {
        this.releaseStage = releaseStage;
    }

    /**
     * Gets whether Bugsnag should automatically capture and report unhandled errors.
     * By default, this value is true.
     */
    public boolean getAutoDetectErrors() {
        return autoDetectErrors;
    }

    /**
     * Sets whether Bugsnag should automatically capture and report unhandled errors.
     * By default, this value is true.
     *
     * @param autoDetectErrors - whether unhandled errors should be reported automatically
     */
    public void setAutoDetectErrors(boolean autoDetectErrors) {
        this.autoDetectErrors = autoDetectErrors;
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
     * @param sendThreads whether thread traces should be sent with reports
     */
    public void setSendThreads(boolean sendThreads) {
        this.sendThreads = sendThreads;
    }

    /**
     * Get whether or not User sessions are captured automatically.
     *
     * @return true if sessions are captured automatically
     */
    public boolean getAutoTrackSessions() {
        return autoTrackSessions;
    }

    /**
     * Sets whether or not Bugsnag should automatically capture and report User sessions whenever
     * the app enters the foreground.
     * <p>
     * By default this behavior is enabled.
     *
     * @param autoTrack whether sessions should be tracked automatically
     */
    public void setAutoTrackSessions(boolean autoTrack) {
        this.autoTrackSessions = autoTrack;
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
    public boolean getAutoCaptureBreadcrumbs() {
        return autoCaptureBreadcrumbs;
    }

    /**
     * By default, we will automatically add breadcrumbs for common application events,
     * such as activity lifecycle events, and system intents.
     * To disable this behavior, set this property to false.
     *
     * @param autoCaptureBreadcrumbs whether breadcrumbs should be automatically captured
     *                                        or not
     */
    public void setAutoCaptureBreadcrumbs(boolean autoCaptureBreadcrumbs) {
        this.autoCaptureBreadcrumbs = autoCaptureBreadcrumbs;
    }

    /**
     * Intended for internal use only - sets the type of the notifier (e.g. Android, React Native)
     * @param appType the notifier type
     */
    public void setAppType(@NonNull String appType) {
        this.appType = appType;
    }

    /**
     * Intended for internal use only - sets the code bundle id for React Native
     * @param codeBundleId the code bundle id
     */
    public void setCodeBundleId(@Nullable String codeBundleId) {
        this.codeBundleId = codeBundleId;
    }

    @Nullable
    public String getCodeBundleId() {
        return codeBundleId;
    }

    @NonNull
    public String getAppType() {
        return appType;
    }

    /**
     * Set the maximum number of breadcrumbs to keep and sent to Bugsnag.
     * By default, we'll keep and send the 25 most recent breadcrumb log
     * messages.
     *
     * @param numBreadcrumbs max number of breadcrumb log messages to send
     */
    public void setMaxBreadcrumbs(int numBreadcrumbs) {
        if (numBreadcrumbs <= 0) {
            numBreadcrumbs = 0;
        }
        if (numBreadcrumbs > 100) {
            numBreadcrumbs = 100;
        }
        this.maxBreadcrumbs = numBreadcrumbs;
    }

    /**
     * Retrieves the maximum number of breadcrumbs to keep and sent to Bugsnag.
     * By default, we'll keep and send the 32 most recent breadcrumb log
     * messages.
     *
     * @return the maximum number of breadcrumb log messages to send
     */
    public int getMaxBreadcrumbs() {
        return maxBreadcrumbs;
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
     * @return whether ANRs will be captured or not
     * @see #setAutoDetectAnrs(boolean)
     */
    public boolean getAutoDetectAnrs() {
        return autoDetectAnrs;
    }

    /**
     * Sets whether <a href="https://developer.android.com/topic/performance/vitals/anr">ANRs</a>
     * should be reported to Bugsnag. When enabled, Bugsnag will record an ANR whenever the main
     * thread has been blocked for 5000 milliseconds or longer.
     * <p/>
     * If you wish to enable ANR detection, you should set this property to true; if you wish to
     * configure the time threshold required to capture an ANR, you should use the
     * {@link #setAnrThresholdMs(long)} property.
     *
     * @param autoDetectAnrs whether ANRs should be captured or not
     * @see #setAnrThresholdMs(long)
     */
    public void setAutoDetectAnrs(boolean autoDetectAnrs) {
        this.autoDetectAnrs = autoDetectAnrs;
    }

    /**
     * @return whether NDK crashes will be reported by bugsnag
     * @see #setAutoDetectNdkCrashes(boolean)
     */
    public boolean getAutoDetectNdkCrashes() {
        return autoDetectNdkCrashes;
    }

    /**
     * Determines whether NDK crashes such as signals and exceptions should be reported by bugsnag.
     *
     * If you are using bugsnag-android this flag is false by default; if you are using
     * bugsnag-android-ndk this flag is true by default.
     *
     * @param autoDetectNdkCrashes whether NDK crashes should be reported
     */
    public void setAutoDetectNdkCrashes(boolean autoDetectNdkCrashes) {
        this.autoDetectNdkCrashes = autoDetectNdkCrashes;
    }

    /**
     * Retrieves the logger used for logging internal messages within the bugsnag SDK
     *
     * @return the logger
     */
    @Nullable
    public Logger getLogger() {
        return logger;
    }

    /**
     * Sets the logger used for logging internal messages within the bugsnag SDK to a custom
     * implementation. If set to null, no log messages will be logged.
     *
     * @param logger the logger, or null
     */
    public void setLogger(@Nullable Logger logger) {
        if (logger == null) {
            this.logger = NoopLogger.INSTANCE;
        } else {
            this.logger = logger;
        }
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
        clientState.addOnError(onError);
    }

    @Override
    public void removeOnError(@NonNull OnError onError) {
        clientState.removeOnError(onError);
    }

    /**
     * Adds an on breadcrumb callback
     *
     * @param onBreadcrumb the on breadcrumb callback
     */
    @Override
    public void addOnBreadcrumb(@NonNull OnBreadcrumb onBreadcrumb) {
        clientState.addOnBreadcrumb(onBreadcrumb);
    }

    /**
     * Removes an on breadcrumb callback
     *
     * @param onBreadcrumb the on breadcrumb callback
     */
    @Override
    public void removeOnBreadcrumb(@NonNull OnBreadcrumb onBreadcrumb) {
        clientState.removeOnBreadcrumb(onBreadcrumb);
    }

    /**
     * Adds an on session callback
     *
     * @param onSession the on session callback
     */
    @Override
    public void addOnSession(@NonNull OnSession onSession) {
        clientState.addOnSession(onSession);
    }

    /**
     * Removes an on session callback
     *
     * @param onSession the on session callback
     */
    @Override
    public void removeOnSession(@NonNull OnSession onSession) {
        clientState.removeOnSession(onSession);
    }

    ClientState getClientState() {
        return clientState;
    }
}
