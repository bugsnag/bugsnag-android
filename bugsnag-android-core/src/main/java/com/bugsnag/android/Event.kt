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
class Event internal constructor(
    internal val config: ImmutableConfig,
    originalError: Throwable,
    private val handledState: HandledState,
    s: Severity,
    session: Session?,
    private val threadState: ThreadState,
    private val metadata: MetaData
) : JsonStream.Streamable, MetaDataAware {

    val session = session
    val originalError = originalError

    /**
     * Set the Severity of this Event.
     *
     * By default, unhandled exceptions will be Severity.ERROR and handled
     * exceptions sent with bugsnag.notify will be Severity.WARNING.
     *
     * @param severity the severity of this error
     * @see Severity
     */
    var severity: Severity = s
        set(value) {
            field = value
            handledState.currentSeverity = severity
        }
    var apiKey: String = config.apiKey

    var app: MutableMap<String, Any> = HashMap()
    var device: MutableMap<String, Any> = HashMap()

    val unhandled = handledState.isUnhandled

    var breadcrumbs: MutableList<Breadcrumb> = emptyList<Breadcrumb>().toMutableList()

    var projectPackages: Collection<String> = config.projectPackages
    var errors: MutableList<Error> =
        Error.createError(originalError, projectPackages).toMutableList()
    var threads: MutableList<Thread> = threadState.threads.toMutableList()

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
     *
     * @param groupingHash a string to use when grouping errors
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
     *
     * @param context what was happening at the time of a crash
     */
    var context: String? = null

    /**
     * @return user information associated with this Event
     */
    private var _user = User()

    protected fun shouldIgnoreClass(): Boolean {
        return config.ignoreClasses.contains(errors[0].errorClass)
    }

    @Throws(IOException::class)
    override fun toStream(writer: JsonStream) {
        // Write error basics
        writer.beginObject()
        writer.name("context").value(context)
        writer.name("metaData").value(metadata)

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

        if (config.sendThreads) {
            writer.name("threads").value(threadState)
        }

        if (session != null) {
            writer.name("session").beginObject()
            writer.name("id").value(session.id)
            writer.name("startedAt").value(DateUtils.toIso8601(session.startedAt))

            writer.name("events").beginObject()
            writer.name("handled").value(session.handledCount.toLong())
            writer.name("unhandled").value(session.unhandledCount.toLong())
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
    fun setUser(id: String?, email: String?, name: String?) {
        _user = User(id, email, name)
    }

    fun getUser() = _user

    fun clearUser() = setUser(null, null, null)

    override fun addMetadata(section: String, value: Any?) = addMetadata(section, null, value)
    override fun addMetadata(section: String, key: String?, value: Any?) =
        metadata.addMetadata(section, key, value)

    override fun clearMetadata(section: String) = clearMetadata(section, null)
    override fun clearMetadata(section: String, key: String?) = metadata.clearMetadata(section, key)

    override fun getMetadata(section: String) = getMetadata(section, null)
    override fun getMetadata(section: String, key: String?) = metadata.getMetadata(section, key)
}
