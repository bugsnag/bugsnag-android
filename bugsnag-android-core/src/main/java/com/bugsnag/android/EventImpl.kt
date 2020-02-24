package com.bugsnag.android

import com.bugsnag.android.Thread.ThreadSendPolicy.*
import java.io.IOException

/**
 * An Event object represents a Throwable captured by Bugsnag and is available as a parameter on
 * an [OnErrorCallback], where individual properties can be mutated before an error report is sent
 * to Bugsnag's API.
 */
internal class EventImpl @JvmOverloads internal constructor(

    /**
     * The Throwable object that caused the event in your application.
     *
     * Manipulating this field does not affect the error information reported to the
     * Bugsnag dashboard. Use [errors] to access and amend the representation of the error
     * that will be sent.
     */
    val originalError: Throwable? = null,
    config: ImmutableConfig,
    private var handledState: HandledState,
    data: Metadata = Metadata()
) : JsonStream.Streamable, MetadataAware, UserAware {

    internal val metadata: Metadata = data.copy()
    private val discardClasses: Set<String> = config.discardClasses.toSet()

    @JvmField
    internal var session: Session? = null

    /**
     * The severity of the event. By default, unhandled exceptions will be [Severity.ERROR]
     * and handled exceptions sent with [Bugsnag.notify] [Severity.WARNING].
     */
    var severity: Severity
        get() = handledState.currentSeverity
        set(value) {
            handledState.currentSeverity = value
        }

    /**
     * The API key used for events sent to Bugsnag. Even though the API key is set when Bugsnag
     * is initialized, you may choose to send certain events to a different Bugsnag project.
     */
    var apiKey: String = config.apiKey

    /**
     * Information set by the notifier about your app can be found in this field. These values
     * can be accessed and amended if necessary.
     */
    lateinit var app: AppWithState

    /**
     * Information set by the notifier about your device can be found in this field. These values
     * can be accessed and amended if necessary.
     */
    lateinit var device: DeviceWithState

    /**
     * Whether the event was a crash (i.e. unhandled) or handled error in which the system
     * continued running.
     */
    val isUnhandled: Boolean = handledState.isUnhandled

    /**
     * A list of breadcrumbs leading up to the event. These values can be accessed and amended
     * if necessary. See [Breadcrumb] for details of the data available.
     */
    var breadcrumbs: MutableList<Breadcrumb> = mutableListOf()

    /**
     * Information extracted from the [Throwable]that caused the event can be found in this field.
     * The list contains at least one [Error] that represents the thrown object
     * with subsequent elements in the list populated from [Throwable.cause].
     *
     * A reference to the actual [Throwable] object that caused the event is available through
     * [originalError].
     */
    var errors: MutableList<Error> = when (originalError) {
        null -> mutableListOf()
        else -> Error.createError(originalError, config.projectPackages, config.logger)
    }

    /**
     * If thread state is being captured along with the event, this field will contain a
     * list of [Thread] objects.
     */
    var threads: MutableList<Thread>

    init {
        val recordThreads = config.sendThreads == ALWAYS || (config.sendThreads == UNHANDLED_ONLY && isUnhandled)

        threads = when {
            recordThreads -> ThreadState(config, if (isUnhandled) originalError else null).threads
            else -> mutableListOf()
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
    var groupingHash: String? = null

    /**
     * Returns the context of the error. The context is a summary what what was occurring in the
     * application at the time of the crash, if available, such as the visible activity.
     */
    var context: String? = null

    /**
     * @return user information associated with this Event
     */
    internal var _user = User(null, null, null)

    protected fun shouldDiscardClass(): Boolean {
        return when {
            errors.isEmpty() -> true
            else -> errors.any { discardClasses.contains(it.errorClass) }
        }
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
        threads.forEach { writer.value(it) }
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

    protected fun updateSeverityInternal(severity: Severity) {
        handledState = HandledState.newInstance(handledState.severityReasonType,
            severity, handledState.attributeValue)
        this.severity = severity
    }

    /**
     * Sets the user associated with the event.
     */
    override fun setUser(id: String?, email: String?, name: String?) {
        _user = User(id, email, name)
    }

    /**
     * Returns the currently set User information.
     */
    override fun getUser() = _user

    /**
     * Adds a map of multiple metadata key-value pairs to the specified section.
     */
    override fun addMetadata(section: String, value: Map<String, Any?>) = metadata.addMetadata(section, value)

    /**
     * Adds the specified key and value in the specified section. The value can be of
     * any primitive type or a collection such as a map, set or array.
     */
    override fun addMetadata(section: String, key: String, value: Any?) =
        metadata.addMetadata(section, key, value)

    /**
     * Removes all the data from the specified section.
     */
    override fun clearMetadata(section: String) = metadata.clearMetadata(section)

    /**
     * Removes data with the specified key from the specified section.
     */
    override fun clearMetadata(section: String, key: String) = metadata.clearMetadata(section, key)

    /**
     * Returns a map of data in the specified section.
     */
    override fun getMetadata(section: String) = metadata.getMetadata(section)

    /**
     * Returns the value of the specified key in the specified section.
     */
    override fun getMetadata(section: String, key: String) = metadata.getMetadata(section, key)
}
