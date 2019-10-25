package com.bugsnag.android

import java.io.IOException
import java.lang.Thread
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
    exc: Throwable,
    internal val handledState: HandledState,
    private var severity: Severity?,
    val session: Session?,
    internal val threadState: ThreadState,
    val metaData: MetaData
) : JsonStream.Streamable, MetaDataAware {

    var app: MutableMap<String, Any> = HashMap()
    var device: MutableMap<String, Any> = HashMap()

    /**
     * @return user information associated with this Event
     */
    var user = User()
        internal set

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
    var projectPackages: Collection<String>? = null
    internal val exceptions: Exceptions?
    internal var breadcrumbs: Breadcrumbs? = null
    internal val exception: BugsnagException
    var isIncomplete = false

    /**
     * Get the class name from the exception contained in this Event report.
     */
    /**
     * Sets the class name from the exception contained in this Event report.
     */
    var exceptionName: String
        get() = exception.name
        set(exceptionName) {
            exception.name = exceptionName
        }

    /**
     * Get the message from the exception contained in this Event report.
     */
    /**
     * Sets the message from the exception contained in this Event report.
     */
    var exceptionMessage: String
        get() {
            val msg = exception.message
            return msg
        }
        set(exceptionMessage) = exception.setMessage(exceptionMessage)

    init {

        if (exc is BugsnagException) {
            this.exception = exc
        } else {
            this.exception = BugsnagException(exc)
        }

        projectPackages = config.projectPackages
        exceptions = Exceptions(config, exception)
    }

    @Throws(IOException::class)
    override fun toStream(writer: JsonStream) {
        // Write error basics
        writer.beginObject()
        writer.name("context").value(context)
        writer.name("metaData").value(metaData)

        writer.name("severity").value(severity!!)
        writer.name("severityReason").value(handledState)
        writer.name("unhandled").value(handledState.isUnhandled)
        writer.name("incomplete").value(isIncomplete)

        if (projectPackages != null) {
            writer.name("projectPackages").beginArray()
            for (projectPackage in projectPackages!!) {
                writer.value(projectPackage)
            }
            writer.endArray()
        }

        // Write exception info
        writer.name("exceptions").value(exceptions!!)

        // Write user info
        writer.name("user").value(user)

        // Write diagnostics
        writer.name("app").value(app)
        writer.name("device").value(device)
        writer.name("breadcrumbs").value(breadcrumbs!!)
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
     * Set the Severity of this Event.
     *
     *
     * By default, unhandled exceptions will be Severity.ERROR and handled
     * exceptions sent with bugsnag.notify will be Severity.WARNING.
     *
     * @param severity the severity of this error
     * @see Severity
     */
    fun setSeverity(severity: Severity?) {
        if (severity != null) {
            this.severity = severity
            this.handledState.currentSeverity = severity
        }
    }

    /**
     * Get the Severity of this Event.
     *
     * @see Severity
     */
    fun getSeverity(): Severity? {
        return severity
    }

    /**
     * Set user information associated with this Event
     *
     * @param id    the id of the user
     * @param email the email address of the user
     * @param name  the name of the user
     */
    fun setUser(id: String?, email: String?, name: String?) {
        this.user = User(id, email, name)
    }

    /**
     * Set user id associated with this Event
     *
     * @param id the id of the user
     */
    fun setUserId(id: String?) {
        this.user = User(this.user)
        this.user.id = id
    }

    /**
     * Set user email address associated with this Event
     *
     * @param email the email address of the user
     */
    fun setUserEmail(email: String?) {
        this.user = User(this.user)
        this.user.email = email
    }

    /**
     * Set user name associated with this Event
     *
     * @param name the name of the user
     */
    fun setUserName(name: String?) {
        this.user = User(this.user)
        this.user.name = name
    }

    override fun addMetadata(section: String, value: Any?) = addMetadata(section, null, value)
    override fun addMetadata(section: String, key: String?, value: Any?) =
        metaData.addMetadata(section, key, value)

    override fun clearMetadata(section: String) = clearMetadata(section, null)
    override fun clearMetadata(section: String, key: String?) = metaData.clearMetadata(section, key)

    override fun getMetadata(section: String) = getMetadata(section, null)
    override fun getMetadata(section: String, key: String?) = metaData.getMetadata(section, key)

    /**
     * The [exception][Throwable] which triggered this Event report.
     */
    fun getException(): Throwable {
        return exception
    }

    internal fun shouldIgnoreClass(): Boolean {
        return config.ignoreClasses.contains(exceptionName)
    }

    internal fun getProjectPackages(): Collection<String>? {
        return projectPackages
    }

    internal fun setProjectPackages(projectPackages: Collection<String>) {
        this.projectPackages = projectPackages
        if (exceptions != null) {
            exceptions.projectPackages = projectPackages
        }
    }
}
