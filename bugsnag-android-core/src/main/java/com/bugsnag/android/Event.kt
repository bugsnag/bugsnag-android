package com.bugsnag.android

import java.io.IOException
import java.util.HashMap

/**
 * Information and associated diagnostics relating to a handled or unhandled
 * Exception.
 *
 * This object is made available in OnError callbacks, so you can
 * inspect and modify it before it is delivered to Bugsnag.
 *
 * @see OnError
 */
class Event @JvmOverloads internal constructor(
    val originalError: Throwable? = null,
    internal val config: ImmutableConfig,
    private val handledState: HandledState,
    internal val metadata: Metadata = Metadata(),
    private val stackTrace: Array<StackTraceElement>? = null,
    threadState: ThreadState? = if (config.sendThreads) ThreadState(config, originalError?.stackTrace ?: stackTrace!!) else null
) : JsonStream.Streamable, MetadataAware, UserAware {

    var session: Session? = null

    /**
     * Set the Severity of this Event.
     *
     * By default, unhandled exceptions will be Severity.ERROR and handled
     * exceptions sent with bugsnag.notify will be Severity.WARNING.
     * @see Severity
     */
    var severity: Severity
        get() = handledState.currentSeverity
        set(value) {
            handledState.currentSeverity = value
        }

    var apiKey: String = config.apiKey

    var app: MutableMap<String, Any?> = HashMap()
    var device: MutableMap<String, Any?> = HashMap()

    val unhandled = handledState.isUnhandled

    var breadcrumbs: List<Breadcrumb> = emptyList()

    var projectPackages: Collection<String> = config.projectPackages
    var errors: List<Error> = when (originalError) {
        null -> listOf()
        else -> Error.createError(originalError, projectPackages)
    }

    var threads: List<Thread> = threadState?.threads ?: listOf()

    /**
     * Get the grouping hash associated with this Event.
     *
     * @return the grouping hash, if set
     */
    /**
     * Set a custom grouping hash to use when grouping this Event on the
     * Bugsnag dashboard. By default, we use a combination of error class
     * and top-most stacktrace line to calculate this, and we do not recommend
     * you override this.
     */
    var groupingHash: String? = null

    /**
     * Get the context associated with this Event.
     */
    /**
     * Override the context sent to Bugsnag with this Event. By default we'll
     * attempt to detect the name of the top-most Activity when this error
     * occurred, and use this as the context, but sometimes this is not
     * possible.
     */
    var context: String? = null

    /**
     * @return user information associated with this Event
     */
    internal var _user = User(null, null, null)

    protected fun shouldIgnoreClass(): Boolean {
        return config.ignoreClasses.contains(errors[0].errorClass)
    }

    @Throws(IOException::class)
    override fun toStream(writer: JsonStream) {
        // Write error basics
        writer.beginObject()
        writer.name("context").value(context)
        writer.name("metaData").value(metadata, true)

        writer.name("severity").value(severity)
        writer.name("severityReason").value(handledState)
        writer.name("unhandled").value(handledState.isUnhandled)

        writer.name("projectPackages").beginArray()
        projectPackages.forEach { writer.value(it) }
        writer.endArray()

        // Write exception info
        writer.name("exceptions")
        writer.beginArray()
        errors.forEach { writer.value(it) }
        writer.endArray()

        // Write user info
        writer.name("user").value(_user)

        // Write diagnostics
        writer.name("app").value(app)
        writer.name("device").value(device)
        writer.name("breadcrumbs").value(breadcrumbs)
        writer.name("groupingHash").value(groupingHash)

        writer.name("threads")
        writer.beginArray()
        for (thread in threads) {
            writer.value(thread)
        }
        writer.endArray()

        if (session != null) {
            val copy = Session.copySession(session)
            writer.name("session").beginObject()
            writer.name("id").value(copy.id)
            writer.name("startedAt").value(DateUtils.toIso8601(copy.startedAt))

            writer.name("events").beginObject()
            writer.name("handled").value(copy.handledCount.toLong())
            writer.name("unhandled").value(copy.unhandledCount.toLong())
            writer.endObject()
            writer.endObject()
        }

        writer.endObject()
    }

    /**
     * Set user information associated with this Event
     *
     * @param id    the id of the user
     * @param email the email address of the user
     * @param name  the name of the user
     */
    override fun setUser(id: String?, email: String?, name: String?) {
        _user = User(id, email, name)
    }

    override fun getUser() = _user
    override fun setUserId(id: String?) = setUser(id, _user.email, _user.name)
    override fun setUserEmail(email: String?) = setUser(_user.id, email, _user.name)
    override fun setUserName(name: String?) = setUser(_user.id, _user.email, name)

    override fun addMetadata(section: String, value: Any?) = metadata.addMetadata(section, value)
    override fun addMetadata(section: String, key: String?, value: Any?) =
        metadata.addMetadata(section, key, value)

    override fun clearMetadata(section: String) = metadata.clearMetadata(section)
    override fun clearMetadata(section: String, key: String?) = metadata.clearMetadata(section, key)

    override fun getMetadata(section: String) = metadata.getMetadata(section)
    override fun getMetadata(section: String, key: String?) = metadata.getMetadata(section, key)
}
