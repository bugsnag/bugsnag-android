package com.bugsnag.android

import androidx.annotation.VisibleForTesting
import com.bugsnag.android.internal.ImmutableConfig
import com.bugsnag.android.internal.JsonHelper
import java.io.File
import java.io.IOException
import java.security.DigestOutputStream
import java.security.MessageDigest

/**
 * An error report payload.
 *
 * This payload contains an error report and identifies the source application
 * using your API key.
 */
class EventPayload @JvmOverloads internal constructor(
    var apiKey: String?,
    event: Event? = null,
    eventFile: File? = null,
    notifier: Notifier,
    private val config: ImmutableConfig
) : JsonStream.Streamable {

    @VisibleForTesting
    internal var event: Event? = event
        private set

    internal var eventFile: File? = eventFile
        private set

    private var cachedBytes: ByteArray? = null

    private val logger: Logger get() = config.logger

    internal val notifier = Notifier(notifier.name, notifier.version, notifier.url).apply {
        dependencies = notifier.dependencies.toMutableList()
    }

    internal fun getErrorTypes(): Set<ErrorType> {
        val event = this.event

        return event?.impl?.getErrorTypesFromStackframes() ?: (
            eventFile?.let { EventFilenameInfo.fromFile(it, config).errorTypes }
                ?: emptySet()
            )
    }

    private fun decodedEvent(): Event {
        val localEvent = event
        if (localEvent != null) {
            return localEvent
        }

        val eventSource = MarshalledEventSource(eventFile!!, apiKey ?: config.apiKey, logger)
        val decodedEvent = eventSource()

        // cache the decoded Event object
        event = decodedEvent

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

        json = rebuildCachedBytes()
        if (json.size <= maxSizeBytes) {
            return this
        }

        val breadcrumbAndBytesRemovedCounts =
            event.impl.trimBreadcrumbsBy(json.size - maxSizeBytes)
        event.impl.internalMetrics.setBreadcrumbTrimMetrics(
            breadcrumbAndBytesRemovedCounts.itemsTrimmed,
            breadcrumbAndBytesRemovedCounts.dataTrimmed
        )

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
            event != null -> writer.value(event)
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
    fun toByteArray(): ByteArray {
        var payload = cachedBytes
        if (payload == null) {
            payload = JsonHelper.serialize(this)
            cachedBytes = payload
        }
        return payload
    }

    private fun rebuildCachedBytes(): ByteArray {
        cachedBytes = null
        return toByteArray()
    }

    /**
     * The value of the "Bugsnag-Integrity" HTTP header returned as a String. This value is used
     * to validate the payload and is expected by the standard BugSnag servers.
     */
    val integrityToken: String?
        get() {
            runCatching {
                val shaDigest = MessageDigest.getInstance("SHA-1")
                val builder = StringBuilder("sha1 ")

                // Pipe the object through a no-op output stream
                DigestOutputStream(NullOutputStream(), shaDigest).use { stream ->
                    stream.buffered().use { writer ->
                        writer.write(toByteArray())
                    }
                    shaDigest.digest().forEach { byte ->
                        builder.append(String.format("%02x", byte))
                    }
                }
                return builder.toString()
            }.getOrElse { return null }
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
