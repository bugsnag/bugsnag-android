package com.bugsnag.android

import androidx.annotation.VisibleForTesting
import com.bugsnag.android.EventPayload.Companion.DEFAULT_MAX_PAYLOAD_SIZE
import com.bugsnag.android.internal.ImmutableConfig
import com.bugsnag.android.internal.JsonHelper
import java.io.File
import java.io.IOException

/**
 * An error report payload.
 *
 * This payload contains an error report and identifies the source application
 * using your API key.
 */
class EventPayload @JvmOverloads internal constructor(
    apiKey: String?,
    event: Event? = null,
    eventFile: File? = null,
    notifier: Notifier,
    private val config: ImmutableConfig
) : JsonStream.Streamable, Deliverable {

    private val fallbackApiKey = apiKey ?: config.apiKey
    private var overwrittenApiKey: String? = null
    private var cachedEvent = event

    var apiKey: String
        set(value) {
            overwrittenApiKey = value
            cachedEvent?.apiKey = value
        }
        get() {
            val localApiKey = overwrittenApiKey ?: cachedEvent?.apiKey
            if (localApiKey != null) {
                return localApiKey
            }

            val localEventFile = eventFile
            if (localEventFile != null) {
                return EventFilenameInfo.fromFile(localEventFile, fallbackApiKey).apiKey
            }

            return fallbackApiKey
        }

    var event: Event?
        get() {
            return try {
                decodedEvent()
            } catch (_: Exception) {
                null
            }
        }
        internal set(newEvent) {
            cachedEvent = newEvent
        }

    /**
     * Returns `true` if the `Event` in this `EventPayload` is considered unhandled.
     */
    val isUnhandled: Boolean
        get() {
            return decodedEvent().isUnhandled
        }

    internal val isLaunchCrash: Boolean
        get() = eventFile?.let { EventFilenameInfo.isLaunchCrashReport(it.name) } == true

    internal var eventFile: File? = eventFile
        private set

    private var cachedBytes: ByteArray? = null

    private val logger: Logger get() = config.logger

    internal val notifier = Notifier(notifier.name, notifier.version, notifier.url).apply {
        dependencies = notifier.dependencies.toMutableList()
    }

    internal fun getErrorTypes(): Set<ErrorType> {
        val event = this.cachedEvent
        return event?.impl?.getErrorTypesFromStackframes()
            ?: eventFile?.let { EventFilenameInfo.fromFile(it, config).errorTypes }
            ?: emptySet()
    }

    private fun decodedEvent(): Event {
        val localEvent = cachedEvent
        if (localEvent != null) {
            return localEvent
        }

        val eventSource = MarshalledEventSource(eventFile!!, apiKey, logger)
        val decodedEvent = eventSource()

        // cache the decoded Event object
        cachedEvent = decodedEvent

        return decodedEvent
    }

    /**
     * If required trim this `EventPayload` so that its [encoded data](toByteArray) will usually be
     * less-than or equal to [maxSizeBytes]. This function may make no changes to the payload, and
     * may also not achieve the requested [maxSizeBytes]. The default use of the function is
     * configured to [DEFAULT_MAX_PAYLOAD_SIZE].
     *
     * @return `this` for call chaining
     */
    @JvmOverloads
    fun trimToSize(maxSizeBytes: Int = DEFAULT_MAX_PAYLOAD_SIZE): EventPayload {
        var json = toByteArray()
        if (json.size <= maxSizeBytes) {
            return this
        }

        val event = decodedEvent()
        val (itemsTrimmed, dataTrimmed) = event.impl.trimMetadataStringsTo(config.maxStringValueLength)
        event.impl.internalMetrics.setMetadataTrimMetrics(
            itemsTrimmed,
            dataTrimmed
        )

        val threadCount = event.threads.size
        val maxReportedThreads = config.maxReportedThreads
        if (threadCount > maxReportedThreads) {
            event.threads.subList(maxReportedThreads, threadCount).clear()

            event.threads.add(
                Thread(
                    "",
                    "[${threadCount - maxReportedThreads} threads omitted as the " +
                        "maxReportedThreads limit ($maxReportedThreads) was exceeded]",
                    ErrorType.UNKNOWN,
                    false,
                    Thread.State.UNKNOWN,
                    Stacktrace(
                        arrayOf(StackTraceElement("", "", "-", 0)),
                        config.projectPackages,
                        logger
                    ),
                    logger
                )
            )
        }

        json = rebuildPayloadCache()
        if (json.size <= maxSizeBytes) {
            return this
        }

        val breadcrumbAndBytesRemovedCounts =
            event.impl.trimBreadcrumbsBy(json.size - maxSizeBytes)
        event.impl.internalMetrics.setBreadcrumbTrimMetrics(
            breadcrumbAndBytesRemovedCounts.itemsTrimmed,
            breadcrumbAndBytesRemovedCounts.dataTrimmed
        )

        rebuildPayloadCache()
        return this
    }

    @Throws(IOException::class)
    override fun toStream(writer: JsonStream) {
        writer.beginObject()
        writer.name("apiKey").value(apiKey)
        writer.name("payloadVersion").value("4.0")
        writer.name("notifier").value(notifier)
        writer.name("events").beginArray()

        when {
            cachedEvent != null -> writer.value(cachedEvent)
            eventFile != null -> writer.value(eventFile)
            else -> Unit
        }

        writer.endArray()
        writer.endObject()
    }

    /**
     * Transform this `EventPayload` to a byte array suitable for delivery to a BugSnag event
     * endpoint (typically configured using [EndpointConfiguration.notify]).
     */
    @Throws(IOException::class)
    override fun toByteArray(): ByteArray {
        var payload = cachedBytes
        if (payload == null) {
            payload = JsonHelper.serialize(this)
            cachedBytes = payload
        }
        return payload
    }

    @VisibleForTesting
    internal fun rebuildPayloadCache(): ByteArray {
        cachedBytes = null
        return toByteArray()
    }

    companion object {
        /**
         * The default maximum payload size for [trimToSize], payloads larger than this will
         * typically be rejected by BugSnag.
         */
        // 1MB with some fiddle room in case of encoding overhead
        const val DEFAULT_MAX_PAYLOAD_SIZE = 999700
    }
}
