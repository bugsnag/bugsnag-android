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

    private var cachedPayloadBytes: ByteArray? = null

    private val logger: Logger get() = config.logger

    internal val notifier = Notifier(notifier.name, notifier.version, notifier.url).apply {
        dependencies = notifier.dependencies.toMutableList()
    }

    internal fun getErrorTypes(): Set<ErrorType> {
        val event = this.event

        return event?.impl?.getErrorTypesFromStackframes()
            ?: (eventFile?.let { EventFilenameInfo.fromFile(it, config).errorTypes }
                ?: emptySet())
    }

    internal fun decodedEvent(): Event {
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
     * Returns a new EventPayload that will typically encode to less than the specified number of
     * bytes. If this `EventPayload` already encodes to fewer bytes it is returned unchanged.
     */
    @JvmOverloads
    fun trimToSize(maxSizeBytes: Int = DEFAULT_MAX_PAYLOAD_SIZE) {
        var json = toByteArray()
        if (json.size <= maxSizeBytes) {
            return
        }

        val event = decodedEvent()
        val (itemsTrimmed, dataTrimmed) = event.impl.trimMetadataStringsTo(config.maxStringValueLength)
        event.impl.internalMetrics.setMetadataTrimMetrics(
            itemsTrimmed,
            dataTrimmed
        )
        cachedPayloadBytes = null

        json = toByteArray()
        if (json.size <= maxSizeBytes) {
            cachedPayloadBytes = json
            return
        }

        val breadcrumbAndBytesRemovedCounts =
            event.impl.trimBreadcrumbsBy(json.size - maxSizeBytes)
        event.impl.internalMetrics.setBreadcrumbTrimMetrics(
            breadcrumbAndBytesRemovedCounts.itemsTrimmed,
            breadcrumbAndBytesRemovedCounts.dataTrimmed
        )
        cachedPayloadBytes = null
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

    @Throws(IOException::class)
    fun toByteArray(): ByteArray {
        var bytes = cachedPayloadBytes
        if (bytes == null) {
            bytes = JsonHelper.serialize(this)
            cachedPayloadBytes = bytes
        }
        return bytes
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
        // 1MB with some fiddle room in case of encoding overhead
        const val DEFAULT_MAX_PAYLOAD_SIZE = 999700
    }
}
