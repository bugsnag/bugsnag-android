package com.bugsnag.android

import android.content.Context
import java.util.Collections

/**
 * User-specified configuration storage object, contains information
 * specified at the client level, api-key and endpoint configuration.
 */
class Configuration(
    /**
     * Changes the API key used for events sent to Bugsnag.
     */
    apiKey: String
) : CallbackAware, MetadataAware, UserAware {

    var apiKey = apiKey
        set(value) {
            require(value.matches(API_KEY_REGEX.toRegex())) { "You must provide a valid Bugsnag API key" }
            field = value
        }
    private var user = User()

    @JvmField
    internal val callbackState: CallbackState

    @JvmField
    internal val metadataState: MetadataState

    /**
     * Sets a unique identifier for the app build to be included in all events sent to Bugsnag.
     *
     * This is used to identify proguard
     * mapping files in the case that you publish multiple different apps with
     * the same appId and versionCode. The default value is read from the
     * com.bugsnag.android.BUILD_UUID meta-data field in your app manifest.
     */
    var buildUuid: String? = null

    /**
     * Set the application version sent to Bugsnag. We'll automatically pull your app version
     * from the versionName field in your AndroidManifest.xml file.
     */
    var appVersion: String? = null

    /**
     * We'll automatically pull your [versionCode] from the versionCode field
     * in your AndroidManifest.xml file. If you'd like to override this you
     * can set this property.
     */
    var versionCode: Int? = 0

    /**
     * If you would like to distinguish between errors that happen in different stages of the
     * application release process (development, production, etc) you can set the [releaseStage]
     * that is reported to Bugsnag.
     *
     * If you are running a debug build, we'll automatically set this to "development",
     * otherwise it is set to "production". You can control whether events are sent for
     * specific release stages using the [enabledReleaseStages] option.
     */
    var releaseStage: String? = null

    /**
     * Controls whether we should capture and serialize the state of all threads at the time
     * of an error.
     *
     * By default [sendThreads] is set to [Thread.ThreadSendPolicy.ALWAYS]. This can be set to
     * [Thread.ThreadSendPolicy.NEVER] to disable or [Thread.ThreadSendPolicy.UNHANDLED_ONLY]
     * to only do so for unhandled errors.
     */
    var sendThreads: Thread.ThreadSendPolicy = Thread.ThreadSendPolicy.ALWAYS

    /**
     * Set whether or not Bugsnag should persist user information between application sessions.
     *
     * If enabled then any user information set will be re-used until the user information is
     * removed manually by calling [Bugsnag.setUser] with null arguments.
     */
    var persistUser: Boolean = false

    /**
     * Sets the threshold in milliseconds for an uncaught error to be considered as a crash on
     * launch. If a crash is detected on launch, Bugsnag will attempt to send the event
     * synchronously.
     *
     * By default, this value is set at 5,000ms. Setting the value to 0 will disable this behaviour.
     */
    var launchCrashThresholdMs: Long = DEFAULT_LAUNCH_CRASH_THRESHOLD_MS
        set(launchCrashThresholdMs) {
            field = when {
                launchCrashThresholdMs <= MIN_LAUNCH_CRASH_THRESHOLD_MS -> MIN_LAUNCH_CRASH_THRESHOLD_MS
                else -> launchCrashThresholdMs
            }
        }

    /**
     * Sets whether or not Bugsnag should automatically capture and report User sessions whenever
     * the app enters the foreground.
     *
     * By default this behavior is enabled.
     */
    var autoTrackSessions: Boolean = true

    /**
     * Bugsnag will automatically detect different types of error in your application.
     * If you wish to control exactly which types are enabled, set this property.
     */
    var enabledErrorTypes: ErrorTypes = ErrorTypes()

    /**
     * If you want to disable automatic detection of all errors, you can set this property to false.
     * By default this property is true.
     *
     * Setting [autoDetectErrors] to false will disable all automatic errors, regardless of the
     * error types enabled by [enabledErrorTypes]
     */
    var autoDetectErrors: Boolean = true

    /**
     * Intended for internal use only - sets the code bundle id for React Native
     */
    var codeBundleId: String? = null

    /**
     * If your app's codebase contains different entry-points/processes, but reports to a single
     * Bugsnag project, you might want to add information denoting the type of process the error
     * came from.
     *
     * This information can be used in the dashboard to filter errors and to determine whether
     * an error is limited to a subset of appTypes.
     *
     * By default, this value is set to 'android'.
     */
    var appType: String? = "android"

    /**
     * By default, the notifier's log messages will be logged using [android.util.Log]
     * with a "Bugsnag" tag unless the [releaseStage] is "production".
     *
     * To override this behavior, an alternative instance can be provided that implements the
     * [Logger] interface.
     */
    var logger: Logger? = null

    /**
     * The Delivery implementation used to make network calls to the Bugsnag
     * [Error Reporting](https://docs.bugsnag.com/api/error-reporting/)
     * and [Sessions API](https://docs.bugsnag.com/api/sessions/).
     *
     * This may be useful if you have requirements such as certificate pinning and rotation,
     * which are not supported by the default implementation.
     *
     * To provide custom delivery functionality, create a class which implements the [Delivery]
     * interface. Please note that request bodies must match the structure specified in the
     * [Error Reporting](https://docs.bugsnag.com/api/error-reporting/) and
     * [Sessions API](https://bugsnagsessiontrackingapi.docs.apiary.io/) documentation.
     *
     * You can use the return type from the `deliver` functions to control the strategy for
     * retrying the transmission at a later date.
     *
     * If [DeliveryStatus.UNDELIVERED] is returned, the notifier will automatically cache
     * the payload and trigger delivery later on. Otherwise, if either [DeliveryStatus.DELIVERED]
     * or [DeliveryStatus.FAILURE] is returned the notifier will removed any cached payload
     * and no further delivery will be attempted.
     */
    var delivery: Delivery? = null

    /**
     * Set the endpoints to send data to. By default we'll send error reports to
     * https://notify.bugsnag.com, and sessions to https://sessions.bugsnag.com, but you can
     * override this if you are using Bugsnag Enterprise to point to your own Bugsnag endpoints.
     */
    var endpoints: EndpointConfiguration = EndpointConfiguration()

    /**
     * Sets the maximum number of breadcrumbs which will be stored. Once the threshold is reached,
     * the oldest breadcrumbs will be deleted.
     *
     * By default, 25 breadcrumbs are stored: this can be amended up to a maximum of 100.
     */
    var maxBreadcrumbs: Int = DEFAULT_MAX_SIZE
        set(numBreadcrumbs) {
            field = when {
                numBreadcrumbs <= MIN_BREADCRUMBS -> MIN_BREADCRUMBS
                numBreadcrumbs > MAX_BREADCRUMBS -> MAX_BREADCRUMBS
                else -> numBreadcrumbs
            }
        }

    /**
     * Bugsnag uses the concept of "contexts" to help display and group your errors. Contexts
     * represent what was happening in your application at the time an error occurs.
     *
     * In an android app the "context" is automatically set as the foreground Activity.
     * If you would like to set this value manually, you should alter this property.
     */
    var context: String? = null

    /**
     * Sets which values should be removed from any Metadata objects before
     * sending them to Bugsnag. Use this if you want to ensure you don't send
     * sensitive data such as passwords, and credit card numbers to our
     * servers. Any keys which contain these strings will be filtered.
     *
     * By default, [redactedKeys] is set to "password"
     */
    var redactedKeys: Set<String>
        get() = Collections.unmodifiableSet(metadataState.metadata.redactedKeys)
        set(redactedKeys) = metadataState.metadata.setRedactedKeys(redactedKeys)

    init {
        require(apiKey.matches(API_KEY_REGEX.toRegex())) { "You must provide a valid Bugsnag API key" }
        this.callbackState = CallbackState()
        this.metadataState = MetadataState()

        enabledErrorTypes.ndkCrashes = try {
            // check if AUTO_DETECT_NDK_CRASHES has been set in bugsnag-android
            // or bugsnag-android-ndk
            val clz = Class.forName("com.bugsnag.android.BuildConfig")
            val field = clz.getDeclaredField("AUTO_DETECT_NDK_CRASHES")
            field.getBoolean(null)
        } catch (exc: Throwable) {
            false
        }
    }

    /**
     * Allows you to specify the fully-qualified name of error classes that will be discarded
     * before being sent to Bugsnag if they are detected. The notifier performs an exact
     * match against the canonical class name.
     */
    var discardClasses: Set<String> = emptySet()

    /**
     * By default, Bugsnag will be notified of events that happen in any [releaseStage].
     * If you would like to change which release stages notify Bugsnag you can set this property.
     */
    var enabledReleaseStages: Set<String>? = null

    /**
     * By default we will automatically add breadcrumbs for common application events such as
     * activity lifecycle events and system intents. To amend this behavior,
     * override the enabled breadcrumb types. All breadcrumbs can be disabled by providing an
     * empty set.
     *
     * The following breadcrumb types can be enabled:
     *
     * - Captured errors: left when an error event is sent to the Bugsnag API.
     * - Manual breadcrumbs: left via the [Bugsnag.leaveBreadcrumb] function.
     * - Navigation changes: left for Activity Lifecycle events to track the user's journey in the app.
     * - State changes: state breadcrumbs are left for system broadcast events. For example:
     * battery warnings, airplane mode, etc.
     * - User interaction: left when the user performs certain system operations.
     */
    var enabledBreadcrumbTypes: Set<BreadcrumbType>? = BreadcrumbType.values().toSet()

    /**
     * Sets which package names Bugsnag should consider as a part of the
     * running application. We mark stacktrace lines as in-project if they
     * originate from any of these packages and this allows us to improve
     * the visual display of the stacktrace on the dashboard.
     *
     * By default, [projectPackages] is set to be the package you called [Bugsnag.start] from.
     */
    var projectPackages: Set<String> = emptySet()

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
    override fun addOnError(onError: OnErrorCallback) = callbackState.addOnError(onError)

    /**
     * Removes a previously added "on error" callback
     * @param onError the callback to remove
     */
    override fun removeOnError(onError: OnErrorCallback) = callbackState.removeOnError(onError)

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
    override fun addOnBreadcrumb(onBreadcrumb: OnBreadcrumbCallback) =
        callbackState.addOnBreadcrumb(onBreadcrumb)

    /**
     * Removes a previously added "on breadcrumb" callback
     * @param onBreadcrumb the callback to remove
     */
    override fun removeOnBreadcrumb(onBreadcrumb: OnBreadcrumbCallback) =
        callbackState.removeOnBreadcrumb(onBreadcrumb)

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
    override fun addOnSession(onSession: OnSessionCallback) = callbackState.addOnSession(onSession)

    /**
     * Removes a previously added "on session" callback
     * @param onSession the callback to remove
     */
    override fun removeOnSession(onSession: OnSessionCallback) = callbackState.removeOnSession(onSession)

    /**
     * Adds a map of multiple metadata key-value pairs to the specified section.
     */
    override fun addMetadata(section: String, value: Map<String, Any?>) =
        metadataState.addMetadata(section, value)

    /**
     * Adds the specified key and value in the specified section. The value can be of
     * any primitive type or a collection such as a map, set or array.
     */
    override fun addMetadata(section: String, key: String, value: Any?) =
        metadataState.addMetadata(section, key, value)

    /**
     * Removes all the data from the specified section.
     */
    override fun clearMetadata(section: String) = metadataState.clearMetadata(section)

    /**
     * Removes data with the specified key from the specified section.
     */
    override fun clearMetadata(section: String, key: String) = metadataState.clearMetadata(section, key)

    /**
     * Returns a map of data in the specified section.
     */
    override fun getMetadata(section: String) = metadataState.getMetadata(section)

    /**
     * Returns the value of the specified key in the specified section.
     */
    override fun getMetadata(section: String, key: String) = metadataState.getMetadata(section, key)

    /**
     * Returns the currently set User information.
     */
    override fun getUser(): User = user

    /**
     * Sets the user associated with the event.
     */
    override fun setUser(id: String?, email: String?, name: String?) {
        user = User(id, email, name)
    }

    companion object {
        private const val DEFAULT_MAX_SIZE = 25
        private const val DEFAULT_LAUNCH_CRASH_THRESHOLD_MS: Long = 5000
        private const val MIN_BREADCRUMBS = 0
        private const val MAX_BREADCRUMBS = 100
        private const val MIN_LAUNCH_CRASH_THRESHOLD_MS: Long = 0
        private const val API_KEY_REGEX = "[A-Fa-f0-9]{32}"

        @JvmStatic
        fun load(context: Context): Configuration = load(context, null)

        @JvmStatic
        protected fun load(context: Context, apiKey: String?): Configuration {
            return ManifestConfigLoader().load(context, apiKey)
        }
    }
}
