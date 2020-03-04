package com.bugsnag.android

import java.util.Locale

internal class EventDeserializer(
    private val client: Client
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
        val type = severityReason["type"] as String
        val handledState: HandledState = HandledState.newInstance(type)

        // construct event
        val event = NativeInterface.createEvent(null, client, handledState)
        event.context = map["context"] as String?
        event.groupingHash = map["groupingHash"] as String?
        val severity = map["severity"] as String
        event.updateSeverityInternal(Severity.valueOf(severity.toUpperCase(Locale.US)))

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
