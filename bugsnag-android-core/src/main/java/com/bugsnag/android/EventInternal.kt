package com.bugsnag.android

import com.bugsnag.android.internal.ImmutableConfig
import com.bugsnag.android.internal.InternalMetrics
import com.bugsnag.android.internal.InternalMetricsNoop
import com.bugsnag.android.internal.JsonHelper
import com.bugsnag.android.internal.TrimMetrics
import java.io.IOException
import java.util.Date
import java.util.regex.Pattern

internal class EventInternal : FeatureFlagAware, JsonStream.Streamable, MetadataAware, UserAware {

    @JvmOverloads
    internal constructor(
        originalError: Throwable? = null,
        config: ImmutableConfig,
        severityReason: SeverityReason,
        data: Metadata = Metadata(),
        featureFlags: FeatureFlags = FeatureFlags(),
        captureStacktrace: Boolean = true,
        captureThreads: Boolean = true,
    ) : this(
        config.apiKey,
        config.logger,
        mutableListOf(),
        config.discardClasses.toSet(),
        when (originalError) {
            null -> mutableListOf()
            else -> Error.createError(
                originalError,
                config.projectPackages,
                captureStacktrace,
                config.logger
            )
        },
        data.copy(),
        featureFlags.copy(),
        originalError,
        config.projectPackages,
        severityReason,
        if (captureThreads) {
            ThreadState(originalError, severityReason.unhandled, config).threads
        } else {
            mutableListOf()
        },
        User(),
        config.redactedKeys.toSet(),
        config.attemptDeliveryOnCrash
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
        redactionKeys: Set<Pattern>? = null,
        isAttemptDeliveryOnCrash: Boolean = false
    ) {
        this.logger = logger
        this.apiKey = apiKey
        this.breadcrumbs = breadcrumbs
        this.discardClasses = discardClasses
        this.errors = errors
        this.metadata = metadata
        this.featureFlags = featureFlags
        this.originalError = originalError
        this.projectPackages = projectPackages
        this.severityReason = severityReason
        this.threads = threads
        this.userImpl = user
        this.isAttemptDeliveryOnCrash = isAttemptDeliveryOnCrash
        redactionKeys?.let {
            this.redactedKeys = it
        }
    }

    val originalError: Throwable?
    internal var severityReason: SeverityReason

    val logger: Logger
    val metadata: Metadata
    val featureFlags: FeatureFlags
    val isAttemptDeliveryOnCrash: Boolean

    private val discardClasses: Set<Pattern>
    internal var projectPackages: Collection<String>

    private val jsonStreamer: ObjectJsonStreamer = ObjectJsonStreamer().apply {
        redactedKeys = redactedKeys.toSet()
    }

    @JvmField
    internal var session: Session? = null

    var severity: Severity
        get() = severityReason.currentSeverity
        set(value) {
            severityReason.currentSeverity = value
        }

    var apiKey: String
    lateinit var app: AppWithState
    lateinit var device: DeviceWithState
    var unhandled: Boolean
        get() = severityReason.unhandled
        set(value) {
            severityReason.unhandled = value
        }

    var breadcrumbs: MutableList<Breadcrumb>
    var errors: MutableList<Error>
    var threads: MutableList<Thread>
    var groupingHash: String? = null
    var context: String? = null
    var groupingDiscriminator: String? = null

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
    internal var userImpl: User

    var traceCorrelation: TraceCorrelation? = null

    var deliveryStrategy: DeliveryStrategy? = null

    var request: Request? = null
    var response: Response? = null

    fun getUnhandledOverridden(): Boolean = severityReason.unhandledOverridden

    fun getOriginalUnhandled(): Boolean = severityReason.originalUnhandled

    protected fun shouldDiscardClass(): Boolean {
        return when {
            errors.isEmpty() -> true
            else -> errors.any { error ->
                discardClasses.any { pattern ->
                    pattern.matcher(error.errorClass).matches()
                }
            }
        }
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

    @Throws(IOException::class)
    override fun toStream(writer: JsonStream) {
        val childWriter = JsonStream(writer, jsonStreamer)
        // Write error basics
        childWriter.beginObject()
        childWriter.name("context").value(context)
        childWriter.name("groupingDiscriminator").value(groupingDiscriminator)
        childWriter.name("metaData").value(metadata)

        childWriter.name("severity").value(severity)
        childWriter.name("severityReason").value(severityReason)
        childWriter.name("unhandled").value(severityReason.unhandled)

        // Write exception info
        childWriter.name("exceptions")
        childWriter.beginArray()
        errors.forEach { childWriter.value(it) }
        childWriter.endArray()

        // Write request/response info if it exists
        childWriter.name("request").value(request)
        childWriter.name("response").value(response)

        // Write project packages
        childWriter.name("projectPackages")
        childWriter.beginArray()
        projectPackages.forEach { childWriter.value(it) }
        childWriter.endArray()

        // Write user info
        childWriter.name("user").value(userImpl)

        // Write diagnostics
        childWriter.name("app").value(app)
        childWriter.name("device").value(device)
        childWriter.name("breadcrumbs").value(breadcrumbs)
        childWriter.name("groupingHash").value(groupingHash)
        val usage = internalMetrics.toJsonableMap()
        if (usage.isNotEmpty()) {
            childWriter.name("usage")
            childWriter.beginObject()
            usage.forEach { entry ->
                childWriter.name(entry.key).value(entry.value)
            }
            childWriter.endObject()
        }

        childWriter.name("threads")
        childWriter.beginArray()
        threads.forEach { childWriter.value(it) }
        childWriter.endArray()

        childWriter.name("featureFlags").value(featureFlags)

        traceCorrelation?.let { correlation ->
            childWriter.name("correlation").value(correlation)
        }

        if (session != null) {
            val copy = Session.copySession(session)
            childWriter.name("session").beginObject()
            childWriter.name("id").value(copy.id)
            childWriter.name("startedAt").value(copy.startedAt)
            childWriter.name("events").beginObject()
            childWriter.name("handled").value(copy.handledCount.toLong())
            childWriter.name("unhandled").value(copy.unhandledCount.toLong())
            childWriter.endObject()
            childWriter.endObject()
        }

        childWriter.endObject()
    }

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

    protected fun updateSeverityInternal(severity: Severity) {
        severityReason = SeverityReason(
            severityReason.severityReasonType,
            severity,
            severityReason.unhandled,
            severityReason.unhandledOverridden,
            severityReason.attributeValue,
            severityReason.attributeKey
        )
    }

    protected fun updateSeverityReason(@SeverityReason.SeverityReasonType reason: String) {
        severityReason = SeverityReason(
            reason,
            severityReason.currentSeverity,
            severityReason.unhandled,
            severityReason.unhandledOverridden,
            severityReason.attributeValue,
            severityReason.attributeKey
        )
    }

    fun getSeverityReasonType(): String = severityReason.severityReasonType

    fun trimMetadataStringsTo(maxLength: Int): TrimMetrics {
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

    fun trimBreadcrumbsBy(byteCount: Int): TrimMetrics {
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

    override fun setUser(id: String?, email: String?, name: String?) {
        userImpl = User(id, email, name)
    }

    override fun getUser() = userImpl

    override fun addMetadata(section: String, value: Map<String, Any?>) =
        metadata.addMetadata(section, value)

    override fun addMetadata(section: String, key: String, value: Any?) =
        metadata.addMetadata(section, key, value)

    override fun clearMetadata(section: String) = metadata.clearMetadata(section)

    override fun clearMetadata(section: String, key: String) = metadata.clearMetadata(section, key)

    override fun getMetadata(section: String) = metadata.getMetadata(section)

    override fun getMetadata(section: String, key: String) = metadata.getMetadata(section, key)

    override fun addFeatureFlag(name: String) = featureFlags.addFeatureFlag(name)

    override fun addFeatureFlag(name: String, variant: String?) =
        featureFlags.addFeatureFlag(name, variant)

    override fun addFeatureFlags(featureFlags: Iterable<FeatureFlag>) =
        this.featureFlags.addFeatureFlags(featureFlags)

    override fun clearFeatureFlag(name: String) = featureFlags.clearFeatureFlag(name)

    override fun clearFeatureFlags() = featureFlags.clearFeatureFlags()

    fun addError(thrownError: Throwable?): Error {
        if (thrownError == null) {
            val newError = Error(
                ErrorInternal("null", null, Stacktrace(ArrayList())),
                logger
            )
            errors.add(newError)
            return newError
        } else {
            val newErrors = Error.createError(thrownError, projectPackages, true, logger)
            errors.addAll(newErrors)
            return newErrors.first()
        }
    }

    fun addError(errorClass: String?, errorMessage: String?, errorType: ErrorType?): Error {
        val error = Error(
            ErrorInternal(
                errorClass.toString(),
                errorMessage,
                Stacktrace(ArrayList()),
                errorType ?: ErrorType.ANDROID
            ),
            logger
        )
        errors.add(error)
        return error
    }

    fun addThread(
        id: String?,
        name: String?,
        errorType: ErrorType,
        isErrorReportingThread: Boolean,
        state: String
    ): Thread {
        val thread = Thread(
            ThreadInternal(
                id.toString(),
                name.toString(),
                errorType,
                isErrorReportingThread,
                state,
                Stacktrace(ArrayList())
            ),
            logger
        )
        threads.add(thread)
        return thread
    }

    fun setErrorReportingThread(thread: Thread) {
        setErrorReportingThread { it === thread }
    }

    fun setErrorReportingThread(threadId: Long) {
        val idString = threadId.toString()
        setErrorReportingThread { it.id == idString }
    }

    private inline fun setErrorReportingThread(predicate: (Thread) -> Boolean) {
        var previousErrorReportingThread: Thread? = null
        var foundPredicateMatch = false
        for (thread in threads) {
            if (thread.errorReportingThread && !predicate(thread)) {
                previousErrorReportingThread = thread
                thread.errorReportingThread = false
            } else if (predicate(thread)) {
                thread.errorReportingThread = true
                foundPredicateMatch = true
            }
        }

        if (!foundPredicateMatch) {
            previousErrorReportingThread?.errorReportingThread = true
        }
    }

    fun leaveBreadcrumb(
        message: String?,
        type: BreadcrumbType?,
        metadata: MutableMap<String, Any?>?
    ): Breadcrumb {
        val breadcrumb = Breadcrumb(
            message.toString(),
            type ?: BreadcrumbType.MANUAL,
            metadata,
            Date(),
            logger
        )

        breadcrumbs.add(breadcrumb)
        return breadcrumb
    }
}
