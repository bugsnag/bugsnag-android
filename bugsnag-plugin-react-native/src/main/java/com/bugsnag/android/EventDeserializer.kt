package com.bugsnag.android

import java.util.Locale

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
        val severityReasonType = severityReason["type"] as String
        val severity = map["severity"] as String
        val unhandled = map["unhandled"] as Boolean
        val handledState = HandledState(
            severityReasonType,
            Severity.valueOf(severity.toUpperCase(Locale.US)),
            unhandled,
            null
        )

        // construct event
        val event = NativeInterface.createEvent(null, client, handledState)
        event.context = map["context"] as String?
        event.groupingHash = map["groupingHash"] as String?

        // app/device
        event.app = appDeserializer.deserialize(map["app"] as MutableMap<String, Any>)
        event.device = deviceDeserializer.deserialize(map["device"] as MutableMap<String, Any>)

        // user
        val user = UserDeserializer().deserialize(map["user"] as MutableMap<String, Any>)
        event.setUser(user.id, user.email, user.name)

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
                val nativeStackDeserializer = NativeStackDeserializer(projectPackages)
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
        return event
    }
}
