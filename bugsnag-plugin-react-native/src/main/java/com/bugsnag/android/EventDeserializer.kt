package com.bugsnag.android

import java.util.UUID

internal class EventDeserializer(
    private val client: Client,
    private val projectPackages: Collection<String>
) : MapDeserializer<Event> {

    private val appDeserializer = AppDeserializer()
    private val deviceDeserializer = DeviceDeserializer()
    private val stackframeDeserializer = StackframeDeserializer()
    private val errorDeserializer = ErrorDeserializer(stackframeDeserializer, client.getLogger())
    private val threadDeserializer = ThreadDeserializer(stackframeDeserializer, client.getLogger())
    private val breadcrumbDeserializer = BreadcrumbDeserializer(client.getLogger())

    @Suppress("UNCHECKED_CAST")
    override fun deserialize(map: MutableMap<String, Any?>): Event {
        val severityReason = map["severityReason"] as Map<String, Any>
        val featureFlags = map["featureFlags"] as? List<Map<String, Any?>>
        val severityReasonType = severityReason["type"] as String
        val severity = map["severity"] as String
        val unhandled = map["unhandled"] as Boolean
        val originalUnhandled = getOriginalUnhandled(severityReason, unhandled)

        val handledState = SeverityReason(
            severityReasonType,
            Severity.valueOf(severity.uppercase()),
            unhandled,
            originalUnhandled,
            null,
            null
        )

        // construct event
        val event = NativeInterface.createEvent(null, client, handledState)
        event.context = map["context"] as String?
        event.groupingHash = map["groupingHash"] as String?

        // apiKey if it exists in the map and is not empty
        val apiKey = (map["apiKey"] as? String)?.takeIf { it.isNotEmpty() }
        apiKey?.let { event.apiKey = apiKey }

        // app/device
        event.app = appDeserializer.deserialize(map["app"] as MutableMap<String, Any>)
        event.device = deviceDeserializer.deserialize(map["device"] as MutableMap<String, Any>)

        // user
        val user = UserDeserializer().deserialize(map["user"] as MutableMap<String, Any>)
        event.setUser(user.id, user.email, user.name)

        // featureFlags
        event.clearFeatureFlags() // we discard the featureFlags from Android native
        featureFlags?.forEach { flagMap ->
            event.addFeatureFlag(flagMap["featureFlag"] as String, flagMap["variant"] as String?)
        }

        // errors
        val errors = map["errors"] as List<Map<String, Any?>>
        event.errors.clear()
        event.errors.addAll(errors.map(errorDeserializer::deserialize))

        // if the JS payload has passed down a native stacktrace,
        // construct a second error object from it and append it to the event
        // so both stacktraces are visible to the user
        if (map.containsKey("nativeStack") && event.errors.isNotEmpty()) {
            runCatching {
                val jsError = event.errors.first()
                val nativeStackDeserializer =
                    NativeStackDeserializer(projectPackages, client.config)
                val nativeStack = nativeStackDeserializer.deserialize(map)
                jsError.stacktrace.addAll(0, nativeStack)
            }
        }

        // threads
        val threads = map["threads"] as List<Map<String, Any?>>
        event.threads.clear()
        event.threads.addAll(threads.map(threadDeserializer::deserialize))

        // breadcrumbs
        val breadcrumbs = map["breadcrumbs"] as List<Map<String, Any?>>
        event.breadcrumbs.clear()
        event.breadcrumbs.addAll(breadcrumbs.map(breadcrumbDeserializer::deserialize))

        // metadata
        val metadata = map["metadata"] as Map<String, Any?>
        metadata.forEach {
            event.addMetadata(it.key, it.value as Map<String, Any>)
        }

        val correlation = map["correlation"] as? Map<String, Any?>
        correlation?.let {
            deserializeCorrelation(it, event)
        }

        return event
    }

    private fun deserializeCorrelation(
        correlation: Map<String, Any?>,
        event: Event
    ) {
        val traceId = (correlation["traceId"] as? String)
            ?.takeIf { it.length == TRACE_ID_LENGTH }
            ?.let {
                val mostSigBits = it.substring(0, HEX_LONG_LENGTH).hexToLong()
                val leastSigBits = it.substring(HEX_LONG_LENGTH).hexToLong()

                if (mostSigBits != null && leastSigBits != null) {
                    UUID(mostSigBits, leastSigBits)
                } else {
                    null
                }
            }
        val spanId = (correlation["spanId"] as? String)
            ?.takeIf { it.length == HEX_LONG_LENGTH }
            ?.hexToLong()

        if (traceId != null && spanId != null) {
            event.setTraceCorrelation(traceId, spanId)
        }
    }

    private fun getOriginalUnhandled(
        map: Map<String, Any>,
        unhandled: Boolean
    ): Boolean {
        val unhandledOverridden = (map.getOrElse("unhandledOverridden") { false }) as Boolean
        return when {
            unhandledOverridden -> !unhandled
            else -> unhandled
        }
    }

    @Suppress("MagicNumber")
    private fun String.hexToLong(): Long? {
        if (length != HEX_LONG_LENGTH || this[0] == '-' || this[3] == '-') return null
        val firstByte = this.substring(0, 2).toLongOrNull(HEX_LONG_LENGTH) ?: return null
        val remaining = this.substring(2).toLongOrNull(HEX_LONG_LENGTH) ?: return null
        return (firstByte shl 56) or remaining
    }

    companion object {
        private const val TRACE_ID_LENGTH = 32
        private const val HEX_LONG_LENGTH = 16
    }
}
