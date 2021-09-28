package com.bugsnag.android

import com.bugsnag.android.internal.ImmutableConfig
import java.io.IOException

internal class EventInternal : JsonStream.Streamable, MetadataAware, UserAware {

    @JvmOverloads
    internal constructor(
        originalError: Throwable? = null,
        config: ImmutableConfig,
        severityReason: SeverityReason,
        data: Metadata = Metadata()
    ) : this(
        config.apiKey,
        mutableListOf(),
        config.discardClasses.toSet(),
        when (originalError) {
            null -> mutableListOf()
            else -> Error.createError(originalError, config.projectPackages, config.logger)
        },
        data.copy(),
        originalError,
        config.projectPackages,
        severityReason,
        ThreadState(originalError, severityReason.unhandled, config).threads,
        User()
    )

    internal constructor(
        apiKey: String,
        breadcrumbs: MutableList<Breadcrumb> = mutableListOf(),
        discardClasses: Set<String> = setOf(),
        errors: MutableList<Error> = mutableListOf(),
        metadata: Metadata = Metadata(),
        originalError: Throwable? = null,
        projectPackages: Collection<String> = setOf(),
        severityReason: SeverityReason = SeverityReason.newInstance(SeverityReason.REASON_HANDLED_EXCEPTION),
        threads: MutableList<Thread> = mutableListOf(),
        user: User = User()
    ) {
        this.apiKey = apiKey
        this.breadcrumbs = breadcrumbs
        this.discardClasses = discardClasses
        this.errors = errors
        this.metadata = metadata
        this.originalError = originalError
        this.projectPackages = projectPackages
        this.severityReason = severityReason
        this.threads = threads
        this.userImpl = user
    }

    val originalError: Throwable?
    private var severityReason: SeverityReason

    val metadata: Metadata
    private val discardClasses: Set<String>
    private val projectPackages: Collection<String>

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

    /**
     * @return user information associated with this Event
     */
    internal var userImpl: User

    fun getUnhandledOverridden(): Boolean = severityReason.unhandledOverridden

    fun getOriginalUnhandled(): Boolean = severityReason.originalUnhandled

    protected fun shouldDiscardClass(): Boolean {
        return when {
            errors.isEmpty() -> true
            else -> errors.any { discardClasses.contains(it.errorClass) }
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

        writer.name("threads")
        writer.beginArray()
        threads.forEach { writer.value(it) }
        writer.endArray()

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

    internal fun getErrorTypesFromStackframes(): Set<ErrorType> {
        val errorTypes = errors.mapNotNull(Error::getType).toSet()
        val frameOverrideTypes = errors
            .map { it.stacktrace }
            .flatMap { it.mapNotNull(Stackframe::type) }
        return errorTypes.plus(frameOverrideTypes)
    }

    protected fun updateSeverityInternal(severity: Severity) {
        severityReason = SeverityReason(
            severityReason.severityReasonType,
            severity,
            severityReason.unhandled,
            severityReason.attributeValue
        )
    }

    protected fun updateSeverityReason(@SeverityReason.SeverityReasonType reason: String) {
        severityReason = SeverityReason(
            reason,
            severityReason.currentSeverity,
            severityReason.unhandled,
            severityReason.attributeValue
        )
    }

    fun getSeverityReasonType(): String = severityReason.severityReasonType

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
}
