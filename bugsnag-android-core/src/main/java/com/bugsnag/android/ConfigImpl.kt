package com.bugsnag.android

import android.content.Context
import java.util.Collections

internal class ConfigImpl(apiKey: String) : CallbackAware, MetadataAware, UserAware {

    var apiKey: String = apiKey
        set(value) {
            require(value.matches(API_KEY_REGEX.toRegex())) { "You must provide a valid Bugsnag API key" }
            field = value
        }
    private var user = User()

    @JvmField
    internal val callbackState: CallbackState

    @JvmField
    internal val metadataState: MetadataState = MetadataState()

    var buildUuid: String? = null
    var appVersion: String? = null
    var versionCode: Int? = 0
    var releaseStage: String? = null
    var sendThreads: Thread.ThreadSendPolicy = Thread.ThreadSendPolicy.ALWAYS
    var persistUser: Boolean = false

    var launchCrashThresholdMs: Long = DEFAULT_LAUNCH_CRASH_THRESHOLD_MS
        set(launchCrashThresholdMs) {
            field = when {
                launchCrashThresholdMs <= MIN_LAUNCH_CRASH_THRESHOLD_MS -> MIN_LAUNCH_CRASH_THRESHOLD_MS
                else -> launchCrashThresholdMs
            }
        }

    var autoTrackSessions: Boolean = true
    var enabledErrorTypes: ErrorTypes = ErrorTypes()
    var autoDetectErrors: Boolean = true
    var codeBundleId: String? = null
    var appType: String? = "android"
    var logger: Logger? = null
    var delivery: Delivery? = null
    var endpoints: EndpointConfiguration = EndpointConfiguration()

    var maxBreadcrumbs: Int = DEFAULT_MAX_SIZE
        set(numBreadcrumbs) {
            field = when {
                numBreadcrumbs <= MIN_BREADCRUMBS -> MIN_BREADCRUMBS
                numBreadcrumbs > MAX_BREADCRUMBS -> MAX_BREADCRUMBS
                else -> numBreadcrumbs
            }
        }

    var context: String? = null

    var redactedKeys: Set<String>? = metadataState.metadata.redactedKeys
        set(value) {
            if (value != null) {
                metadataState.metadata.setRedactedKeys(value)
            }
            field = value
        }

    init {
        require(apiKey.matches(API_KEY_REGEX.toRegex())) { "You must provide a valid Bugsnag API key" }
        this.callbackState = CallbackState()


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

    var discardClasses: Set<String>? = emptySet()
    var enabledReleaseStages: Set<String>? = null
    var enabledBreadcrumbTypes: Set<BreadcrumbType>? = BreadcrumbType.values().toSet()
    var projectPackages: Set<String> = emptySet()

    override fun addOnError(onError: OnErrorCallback) = callbackState.addOnError(onError)
    override fun removeOnError(onError: OnErrorCallback) = callbackState.removeOnError(onError)
    override fun addOnBreadcrumb(onBreadcrumb: OnBreadcrumbCallback) =
        callbackState.addOnBreadcrumb(onBreadcrumb)
    override fun removeOnBreadcrumb(onBreadcrumb: OnBreadcrumbCallback) =
        callbackState.removeOnBreadcrumb(onBreadcrumb)
    override fun addOnSession(onSession: OnSessionCallback) = callbackState.addOnSession(onSession)
    override fun removeOnSession(onSession: OnSessionCallback) = callbackState.removeOnSession(onSession)

    override fun addMetadata(section: String, value: Map<String, Any?>) =
        metadataState.addMetadata(section, value)
    override fun addMetadata(section: String, key: String, value: Any?) =
        metadataState.addMetadata(section, key, value)
    override fun clearMetadata(section: String) = metadataState.clearMetadata(section)
    override fun clearMetadata(section: String, key: String) = metadataState.clearMetadata(section, key)
    override fun getMetadata(section: String) = metadataState.getMetadata(section)
    override fun getMetadata(section: String, key: String) = metadataState.getMetadata(section, key)

    override fun getUser(): User = user
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
