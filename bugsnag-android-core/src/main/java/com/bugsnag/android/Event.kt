package com.bugsnag.android

import androidx.annotation.RestrictTo
import com.bugsnag.android.JsonStream.Streamable
import com.bugsnag.android.SeverityReason.SeverityReasonType
import com.bugsnag.android.internal.ImmutableConfig
import com.bugsnag.android.internal.InternalMetrics
import com.bugsnag.android.internal.InternalMetricsNoop
import com.bugsnag.android.internal.JsonHelper
import com.bugsnag.android.internal.TrimMetrics
import java.io.IOException
import java.util.regex.Pattern

/**
 * An Event object represents a Throwable captured by Bugsnag and is available as a parameter on
 * an [OnErrorCallback], where individual properties can be mutated before an error report is
 * sent to Bugsnag's API.
 */
class Event : Streamable, MetadataAware, UserAware, FeatureFlagAware {

    internal var severityReason: SeverityReason
    val logger: Logger
    internal val metadata: Metadata

    private val jsonStreamer: ObjectJsonStreamer = ObjectJsonStreamer().apply {
        redactedKeys = redactedKeys.toSet()
    }

    lateinit var app: AppWithState
    lateinit var device: DeviceWithState

    var redactedKeys: Collection<Pattern>
        get() = jsonStreamer.redactedKeys
        set(value) {
            jsonStreamer.redactedKeys = value.toSet()
            metadata.redactedKeys = value.toSet()
        }
    var internalMetrics: InternalMetrics = InternalMetricsNoop()

    /**
     * @return user information associated with this Event
     */
    var userImpl: User

    internal constructor(
        originalError: Throwable?,
        config: ImmutableConfig,
        severityReason: SeverityReason,
        data: Metadata = Metadata(),
        featureFlags: FeatureFlags = FeatureFlags(),
        logger: Logger = config.logger
    ) : this(
        config.apiKey,
        logger,
        mutableListOf(),
        config.discardClasses.toSet(),
        when (originalError) {
            null -> mutableListOf()
            else -> Error.createError(originalError, config.projectPackages, config.logger)
        },
        data.copy(),
        featureFlags.copy(),
        originalError,
        config.projectPackages,
        severityReason,
        ThreadState(originalError, severityReason.unhandled, config).threads,
        User(),
        config.redactedKeys.toSet()
    )

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
    constructor(
        originalError: Throwable?,
        config: ImmutableConfig,
        severityReason: String,
        logger: Logger
    ) : this(
        originalError,
        config,
        SeverityReason.newInstance(severityReason),
        Metadata(),
        FeatureFlags(),
        logger
    )

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
    internal constructor(
        originalError: Throwable?,
        config: ImmutableConfig,
        severityReason: SeverityReason,
        logger: Logger
    ) : this(
        originalError,
        config,
        severityReason,
        Metadata(),
        FeatureFlags(),
        logger
    )

    internal constructor(
        apiKey: String,
        logger: Logger,
        breadcrumbs: MutableList<Breadcrumb> = mutableListOf(),
        discardClasses: Set<Pattern> = setOf(),
        errors: MutableList<Error> = mutableListOf(),
        metadata: Metadata = Metadata(),
        featureFlags: FeatureFlags = FeatureFlags(),
        originalError: Throwable? = null,
        projectPackages: Collection<String> = setOf(),
        severityReason: SeverityReason = SeverityReason.newInstance(SeverityReason.REASON_HANDLED_EXCEPTION),
        threads: MutableList<Thread> = mutableListOf(),
        user: User = User(),
        redactionKeys: Set<Pattern>? = null
    ) {
        this.logger = logger
        this.apiKey = apiKey
        this.breadcrumbs = breadcrumbs
        this.discardClasses = discardClasses
        this.errors = errors
        this.metadata = metadata
        this._featureFlags = featureFlags
        this.originalError = originalError
        this.projectPackages = projectPackages
        this.severityReason = severityReason
        this.threads = threads
        this.userImpl = user

        redactionKeys?.let {
            this.redactedKeys = it
        }
    }

    private fun logNull(property: String) {
        logger.e("Invalid null value supplied to config.$property, ignoring")
    }

    /**
     * The Throwable object that caused the event in your application.
     *
     * Manipulating this field does not affect the error information reported to the
     * Bugsnag dashboard. Use [Event.getErrors] to access and amend the representation of
     * the error that will be sent.
     */
    val originalError: Throwable?

    /**
     * Information extracted from the [Throwable] that caused the event can be found in this
     * field. The list contains at least one [Error] that represents the thrown object
     * with subsequent elements in the list populated from [Throwable.getCause].
     *
     * A reference to the actual [Throwable] object that caused the event is available
     * through [Event.getOriginalError] ()}.
     */
    var errors: MutableList<Error>

    /**
     * If thread state is being captured along with the event, this field will contain a
     * list of [Thread] objects.
     */
    var threads: MutableList<Thread>

    /**
     * A list of breadcrumbs leading up to the event. These values can be accessed and amended
     * if necessary. See [Breadcrumb] for details of the data available.
     */
    var breadcrumbs: MutableList<Breadcrumb>
        set(breadcrumbs) {
            field = breadcrumbs.toMutableList()
        }

    /**
     * A list of feature flags active at the time of the event.
     * See [FeatureFlag] for details of the data available.
     */
    val featureFlags: List<FeatureFlag>
        get() = _featureFlags.toList()

    private val _featureFlags: FeatureFlags

    /**
     * Information set by the notifier about your app can be found in this field. These values
     * can be accessed and amended if necessary.
     */
    private val discardClasses: Set<Pattern>

    /**
     * Information set by the notifier about your device can be found in this field. These values
     * can be accessed and amended if necessary.
     */
    internal var projectPackages: Collection<String>

    /**
     * The API key used for events sent to Bugsnag. Even though the API key is set when Bugsnag
     * is initialized, you may choose to send certain events to a different Bugsnag project.
     */
    /**
     * The API key used for events sent to Bugsnag. Even though the API key is set when Bugsnag
     * is initialized, you may choose to send certain events to a different Bugsnag project.
     */
    var apiKey: String
        set(apiKey) =
            @Suppress("SENSELESS_COMPARISON")
            if (apiKey != null) {
                field = apiKey
            } else {
                logNull("apiKey")
            }
    /**
     * The severity of the event. By default, unhandled exceptions will be [Severity.ERROR]
     * and handled exceptions sent with [Bugsnag.notify] [Severity.WARNING].
     */
    /**
     * The severity of the event. By default, unhandled exceptions will be [Severity.ERROR]
     * and handled exceptions sent with [Bugsnag.notify] [Severity.WARNING].
     */
    var severity: Severity
        get() = severityReason.currentSeverity
        set(value) {
            @Suppress("SENSELESS_COMPARISON")
            if (value != null) {
                severityReason.currentSeverity = value
            } else {
                logNull("severity")
            }
        }

    /**
     * Set the grouping hash of the event to override the default grouping on the dashboard.
     * All events with the same grouping hash will be grouped together into one error. This is an
     * advanced usage of the library and mis-using it will cause your events not to group properly
     * in your dashboard.
     *
     * As the name implies, this option accepts a hash of sorts.
     */
    /**
     * Set the grouping hash of the event to override the default grouping on the dashboard.
     * All events with the same grouping hash will be grouped together into one error. This is an
     * advanced usage of the library and mis-using it will cause your events not to group properly
     * in your dashboard.
     *
     * As the name implies, this option accepts a hash of sorts.
     */
    var groupingHash: String? = null
    /**
     * Returns the context of the error. The context is a summary what what was occurring in the
     * application at the time of the crash, if available, such as the visible activity.
     */
    /**
     * Sets the context of the error. The context is a summary what what was occurring in the
     * application at the time of the crash, if available, such as the visible activity.
     */
    var context: String? = null

    /**
     * Sets the user associated with the event.
     */
    override fun setUser(id: String?, email: String?, name: String?) {
        userImpl = User(id, email, name)
    }

    /**
     * Returns the currently set User information.
     */
    override fun getUser(): User {
        return userImpl
    }

    /**
     * Adds a map of multiple metadata key-value pairs to the specified section.
     */
    override fun addMetadata(section: String, value: Map<String, Any?>) {
        @Suppress("SENSELESS_COMPARISON")
        if (section != null && value != null) {
            metadata.addMetadata(section, value)
        } else {
            logNull("addMetadata")
        }
    }

    /**
     * Adds the specified key and value in the specified section. The value can be of
     * any primitive type or a collection such as a map, set or array.
     */
    override fun addMetadata(section: String, key: String, value: Any?) {
        @Suppress("SENSELESS_COMPARISON")
        if (section != null && key != null) {
            metadata.addMetadata(section, key, value)
        } else {
            logNull("addMetadata")
        }
    }

    /**
     * Removes all the data from the specified section.
     */
    override fun clearMetadata(section: String) {
        @Suppress("SENSELESS_COMPARISON")
        if (section != null) {
            metadata.clearMetadata(section)
        } else {
            logNull("clearMetadata")
        }
    }

    /**
     * Removes data with the specified key from the specified section.
     */
    override fun clearMetadata(section: String, key: String) {
        @Suppress("SENSELESS_COMPARISON")
        if (section != null && key != null) {
            metadata.clearMetadata(section, key)
        } else {
            logNull("clearMetadata")
        }
    }

    /**
     * Returns a map of data in the specified section.
     */
    override fun getMetadata(section: String): Map<String, Any>? {
        @Suppress("SENSELESS_COMPARISON")
        return if (section != null) {
            metadata.getMetadata(section)
        } else {
            logNull("getMetadata")
            null
        }
    }

    /**
     * Returns the value of the specified key in the specified section.
     */
    override fun getMetadata(section: String, key: String): Any? {
        @Suppress("SENSELESS_COMPARISON")
        return if (section != null && key != null) {
            metadata.getMetadata(section, key)
        } else {
            logNull("getMetadata")
            null
        }
    }

    /**
     * {@inheritDoc}
     */
    override fun addFeatureFlag(name: String) {
        @Suppress("SENSELESS_COMPARISON")
        if (name != null) {
            _featureFlags.addFeatureFlag(name)
        } else {
            logNull("addFeatureFlag")
        }
    }

    /**
     * {@inheritDoc}
     */
    override fun addFeatureFlag(name: String, variant: String?) {
        @Suppress("SENSELESS_COMPARISON")
        if (name != null) {
            _featureFlags.addFeatureFlag(name, variant)
        } else {
            logNull("addFeatureFlag")
        }
    }

    /**
     * {@inheritDoc}
     */
    override fun addFeatureFlags(featureFlags: Iterable<FeatureFlag>) {
        @Suppress("SENSELESS_COMPARISON")
        if (featureFlags != null) {
            this._featureFlags.addFeatureFlags(featureFlags)
        } else {
            logNull("addFeatureFlags")
        }
    }

    /**
     * {@inheritDoc}
     */
    override fun clearFeatureFlag(name: String) {
        @Suppress("SENSELESS_COMPARISON")
        if (name != null) {
            _featureFlags.clearFeatureFlag(name)
        } else {
            logNull("clearFeatureFlag")
        }
    }

    /**
     * {@inheritDoc}
     */
    override fun clearFeatureFlags() {
        _featureFlags.clearFeatureFlags()
    }

    @Throws(IOException::class)
    override fun toStream(parentWriter: JsonStream) {
        val writer = JsonStream(parentWriter, jsonStreamer)
        // Write error basics
        writer.beginObject()
        writer.name("context").value(context)
        writer.name("metaData").value(metadata)

        writer.name("severity").value(severity)
        writer.name("severityReason").value(severityReason)
        writer.name("unhandled").value(severityReason.unhandled)

        // Write exception info
        writer.name("exceptions")
        writer.beginArray()
        errors.forEach { writer.value(it) }
        writer.endArray()

        // Write project packages
        writer.name("projectPackages")
        writer.beginArray()
        projectPackages.forEach { writer.value(it) }
        writer.endArray()

        // Write user info
        writer.name("user").value(userImpl)

        // Write diagnostics
        writer.name("app").value(app)
        writer.name("device").value(device)
        writer.name("breadcrumbs").value(breadcrumbs)
        writer.name("groupingHash").value(groupingHash)
        val usage = internalMetrics.toJsonableMap()
        if (usage.isNotEmpty()) {
            writer.name("usage")
            writer.beginObject()
            usage.forEach { entry ->
                writer.name(entry.key).value(entry.value)
            }
            writer.endObject()
        }

        writer.name("threads")
        writer.beginArray()
        threads.forEach { writer.value(it) }
        writer.endArray()

        writer.name("featureFlags").value(_featureFlags)

        if (session != null) {
            val copy = Session.copySession(session)
            writer.name("session").beginObject()
            writer.name("id").value(copy.id)
            writer.name("startedAt").value(copy.startedAt)
            writer.name("events").beginObject()
            writer.name("handled").value(copy.handledCount.toLong())
            writer.name("unhandled").value(copy.unhandledCount.toLong())
            writer.endObject()
            writer.endObject()
        }

        writer.endObject()
    }

    var isUnhandled: Boolean
        @JvmName("setUnhandled") set(unhandled) {
            severityReason.unhandled = unhandled
        }
        @JvmName("isUnhandled") get() = severityReason.unhandled

    fun shouldDiscardClass(): Boolean {
        return when {
            errors.isEmpty() -> true
            else -> errors.any { error ->
                discardClasses.any { pattern ->
                    pattern.matcher(error.errorClass).matches()
                }
            }
        }
    }

    internal fun trimMetadataStringsTo(maxLength: Int): TrimMetrics {
        var stringCount = 0
        var charCount = 0

        var stringAndCharCounts = metadata.trimMetadataStringsTo(maxLength)
        stringCount += stringAndCharCounts.itemsTrimmed
        charCount += stringAndCharCounts.dataTrimmed
        for (breadcrumb in breadcrumbs) {
            stringAndCharCounts = breadcrumb.impl.trimMetadataStringsTo(maxLength)
            stringCount += stringAndCharCounts.itemsTrimmed
            charCount += stringAndCharCounts.dataTrimmed
        }
        return TrimMetrics(stringCount, charCount)
    }

    internal fun trimBreadcrumbsBy(byteCount: Int): TrimMetrics {
        var removedBreadcrumbCount = 0
        var removedByteCount = 0
        while (removedByteCount < byteCount && breadcrumbs.isNotEmpty()) {
            val breadcrumb = breadcrumbs.removeAt(0)
            removedByteCount += JsonHelper.serialize(breadcrumb).size
            removedBreadcrumbCount++
        }
        when (removedBreadcrumbCount) {
            1 -> breadcrumbs.add(Breadcrumb("Removed to reduce payload size", logger))
            else -> breadcrumbs.add(
                Breadcrumb(
                    "Removed, along with ${removedBreadcrumbCount - 1} older breadcrumbs, to reduce payload size",
                    logger
                )
            )
        }
        return TrimMetrics(removedBreadcrumbCount, removedByteCount)
    }

    protected fun isAnr(event: Event): Boolean {
        val errors = event.errors
        var errorClass: String? = null
        if (errors.isNotEmpty()) {
            val error = errors[0]
            errorClass = error.errorClass
        }
        return "ANR" == errorClass
    }

    fun getUnhandledOverridden(): Boolean = severityReason.unhandledOverridden

    fun getOriginalUnhandled(): Boolean = severityReason.originalUnhandled

    fun getSeverityReasonType(): String = severityReason.severityReasonType

    internal fun getErrorTypesFromStackframes(): Set<ErrorType> {
        val errorTypes = errors.mapNotNull(Error::getType).toSet()
        val frameOverrideTypes = errors
            .map { it.stacktrace }
            .flatMap { it.mapNotNull(Stackframe::type) }
        return errorTypes.plus(frameOverrideTypes)
    }

    internal fun normalizeStackframeErrorTypes() {
        if (getErrorTypesFromStackframes().size == 1) {
            errors.flatMap { it.stacktrace }.forEach {
                it.type = null
            }
        }
    }

    internal fun updateSeverityReasonInternal(severityReason: SeverityReason) {
        this.severityReason = severityReason
    }

    fun updateSeverityInternal(severity: Severity) {
        severityReason = SeverityReason(
            severityReason.severityReasonType,
            severity,
            severityReason.unhandled,
            severityReason.unhandledOverridden,
            severityReason.attributeValue,
            severityReason.attributeKey
        )
    }

    fun updateSeverityReason(@SeverityReasonType reason: String) {
        severityReason = SeverityReason(
            reason,
            severityReason.currentSeverity,
            severityReason.unhandled,
            severityReason.unhandledOverridden,
            severityReason.attributeValue,
            severityReason.attributeKey
        )
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
    var session: Session? = null
        @JvmName("setSession") set
        @JvmName("getSession") get
}
